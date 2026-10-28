package dev.reimagined.content;

import java.util.Comparator;
import dev.reimagined.magic.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative spells: clients only request use and receive vanilla updates/particles. */
public final class SpellWand extends Item {
    public enum School {
        ELEMENTAL(20, 30), NATURE(25, 100), ARCANE(30, 80), RITUAL(40, 200);
        final int cost;
        final int cooldown;
        School(int cost, int cooldown) { this.cost = cost; this.cooldown = cooldown; }
    }
    private static final String MANA = "reimagined_mana";
    private static final String REGEN = "reimagined_mana_tick";
    private static final String COOLDOWN = "reimagined_spell_ready_";
    private final School school;
    public SpellWand(Properties properties, School school) { super(properties); this.school = school; }
    public static int mana(ServerPlayer player) {
        return player.getPersistentData().contains(MANA) ? ManaRules.clamp(player.getPersistentData().getInt(MANA)) : ManaRules.CAPACITY;
    }
    public static boolean spendMana(ServerPlayer player,int cost) {
        if(cost<0 || cost>ManaRules.CAPACITY || (!player.isCreative() && !ManaRules.canCast(mana(player),cost)))return false;
        if(!player.isCreative())player.getPersistentData().putInt(MANA,mana(player)-cost);
        return true;
    }
    public static void regenerate(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        var data = player.getPersistentData();
        long previous = data.contains(REGEN) ? data.getLong(REGEN) : now;
        // No offline regeneration: game time may have advanced while this player was disconnected.
        int seconds = now >= previous && now - previous <= 40 ? (int) ((now - previous) / 20) : 0;
        data.putInt(MANA, ManaRules.regenerate(mana(player), seconds));
        data.putLong(REGEN, now);
    }
    public Spell selected(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        Spell spell=data==null?null:Spell.parse(data.copyTag().getString("reimagined_spell"));
        return spell!=null && spell.school==school?spell:Spell.basic(school);
    }
    public void select(ItemStack stack,Spell spell) {
        if(spell.school!=school)throw new IllegalArgumentException("Wrong spell school");
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        data.putString("reimagined_spell",spell.id());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);
        Spell spell=selected(stack);
        if(!SpellKnowledge.knows(player,spell)) {
            if(!player.isShiftKeyDown()) {
                player.displayClientMessage(Component.translatable("message.reimagined.spell_locked"),true);
                return InteractionResultHolder.fail(stack);
            }
            spell=Spell.basic(school);
        }
        if(player.isShiftKeyDown()) {
            Spell next=spell;
            for(int step=1;step<=Spell.values().length;step++) {
                var candidate=Spell.values()[(spell.ordinal()+step)%Spell.values().length];
                if(candidate.school==school && SpellKnowledge.knows(player,candidate)) {next=candidate;break;}
            }
            select(stack,next);player.getInventory().setChanged();serverPlayer.containerMenu.broadcastChanges();
            player.displayClientMessage(Component.translatable("message.reimagined.spell_selected",Component.translatable(next.translationKey())),true);
            return InteractionResultHolder.consume(stack);
        }
        long now = serverPlayer.serverLevel().getServer().overworld().getGameTime();
        long ready = player.getPersistentData().getLong(COOLDOWN + school.name());
        if (ready > now) return InteractionResultHolder.fail(stack);
        int current = mana(serverPlayer);
        if (!player.isCreative() && !ManaRules.canCast(current, spell.cost)) {
            player.displayClientMessage(Component.translatable("message.reimagined.mana", current, ManaRules.CAPACITY), true);
            return InteractionResultHolder.fail(stack);
        }
        boolean performed = switch (spell) {
            case SPARK -> elemental(serverPlayer,false);
            case GROWTH -> nature(serverPlayer);
            case BLINK -> arcane(serverPlayer);
            case BLESSING -> ritual(serverPlayer);
            case FROST_LANCE -> elemental(serverPlayer,true);
            case PURIFICATION -> purification(serverPlayer);
            case MAGELIGHT -> magelight(serverPlayer);
            case SANCTUARY -> sanctuary(serverPlayer);
        };
        if (!performed) return InteractionResultHolder.fail(stack);
        if (!player.isCreative()) player.getPersistentData().putInt(MANA, current - spell.cost);
        player.getPersistentData().putLong(COOLDOWN + school.name(), now + spell.cooldown);
        player.getCooldowns().addCooldown(this, spell.cooldown);
        dev.reimagined.progression.PlayerSkills.award(serverPlayer, dev.reimagined.progression.Skill.MAGE, 5);
        player.displayClientMessage(Component.translatable("message.reimagined.mana", mana(serverPlayer), ManaRules.CAPACITY), true);
        return InteractionResultHolder.consume(stack);
    }
    private static boolean elemental(ServerPlayer player,boolean frost) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(16));
        var block = SpellWorldAccess.ray(player,16,ClipContext.Block.COLLIDER);
        final Vec3 wallLimit = block.getLocation();
        Vec3 limit = wallLimit;
        var candidates=new java.util.ArrayList<LivingEntity>(64);
        level.getEntities().get(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),
                player.getBoundingBox().expandTowards(limit.subtract(start)).inflate(1),entity-> {
                    candidates.add(entity);
                    return candidates.size()>=64?net.minecraft.util.AbortableIterationConsumer.Continuation.ABORT:net.minecraft.util.AbortableIterationConsumer.Continuation.CONTINUE;
                });
        var target=candidates.stream().filter(entity->entity!=player && entity.isAlive() && !entity.isAlliedTo(player)
                && (!(entity instanceof Player other) || player.canHarmPlayer(other)))
                .filter(entity->entity.getBoundingBox().inflate(.25).clip(start,wallLimit).isPresent())
                .min(Comparator.comparingDouble(entity->entity.distanceToSqr(player))).orElse(null);
        if (target != null) {
            limit = target.getBoundingBox().getCenter();
            boolean damaged=target.hurt(player.damageSources().indirectMagic(player, player), frost?4.0F:6.0F);
            if(frost && damaged)target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,120,0));
        }
        Vec3 ray = limit.subtract(start);
        int steps = Math.max(1, Math.min(16,(int) ray.length()));
        for (int i = 0; i <= steps; i++) {
            Vec3 pos = start.add(ray.scale((double) i / steps));
            level.sendParticles(frost?ParticleTypes.SNOWFLAKE:ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0);
        }
        return true;
    }
    private static boolean nature(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        var hit = SpellWorldAccess.ray(player,6,ClipContext.Block.OUTLINE);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos();
            var state = new SpellWorldAccess(level).getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock plant && plant.isValidBonemealTarget(level, pos, state)) {
                if (plant.isBonemealSuccess(level, level.random, pos, state)) plant.performBonemeal(level, level.random, pos, state);
                level.levelEvent(1505, pos, 0);
                return true;
            }
        }
        if (player.getHealth() >= player.getMaxHealth()) return false;
        player.heal(4);
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1, player.getZ(), 5, 0.4, 0.5, 0.4, 0);
        return true;
    }
    private static boolean arcane(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.position();
        Vec3 direction = player.getLookAngle();
        Vec3 best = null;
        // Trace every half block to prevent teleporting through walls or into unloaded chunks.
        for (double distance = 0.5; distance <= 8; distance += 0.5) {
            Vec3 candidate = start.add(direction.scale(distance));
            BlockPos feet = BlockPos.containing(candidate);
            if (!SpellWorldAccess.loadedBox(level,player.getBoundingBox().move(candidate.subtract(start)).inflate(1)) || !level.getWorldBorder().isWithinBounds(feet)
                    || candidate.y < level.getMinBuildHeight() || candidate.y + player.getBbHeight() >= level.getMaxBuildHeight()
                    || !level.noCollision(player, player.getBoundingBox().move(candidate.subtract(start)))) break;
            if (new SpellWorldAccess(level).getBlockState(feet.below()).isSolid() && new SpellWorldAccess(level).getFluidState(feet).isEmpty()) best = candidate;
        }
        if (best == null || best.distanceToSqr(start) < 1) return false;
        level.sendParticles(ParticleTypes.PORTAL, start.x, start.y + 1, start.z, 24, 0.3, 0.6, 0.3, 0.2);
        player.teleportTo(best.x, best.y, best.z);
        player.fallDistance = 0;
        return true;
    }
    private static boolean ritual(ServerPlayer player) {
        // Resource consumed even though the wand itself is reusable.
        int shardSlot = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.AMETHYST_SHARD)) { shardSlot = i; break; }
        }
        if (shardSlot < 0 && !player.isCreative()) {
            player.displayClientMessage(Component.translatable("message.reimagined.ritual_shard"), true);
            return false;
        }
        if (!player.isCreative()) player.getInventory().getItem(shardSlot).shrink(1);
        for (Player ally : player.serverLevel().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(6),
                other -> other == player || other.isAlliedTo(player))) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        }
        player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 60, 3, 0.5, 3, 0.5);
        return true;
    }
    private static boolean purification(ServerPlayer player) {
        player.removeEffect(MobEffects.POISON);player.removeEffect(MobEffects.WITHER);player.removeEffect(MobEffects.HUNGER);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,120,0));
        player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,player.getX(),player.getY()+1,player.getZ(),12,.4,.5,.4,0);
        return true;
    }
    private static boolean magelight(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,1200,0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,200,0));return true;
    }
    private static boolean sanctuary(ServerPlayer player) {
        int slot=-1;
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(Items.AMETHYST_SHARD)) {slot=i;break;}
        if(slot<0 && !player.isCreative()) {player.displayClientMessage(Component.translatable("message.reimagined.ritual_shard"),true);return false;}
        if(!player.isCreative())player.getInventory().getItem(slot).shrink(1);
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,600,0));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,600,0));return true;
    }
    /** Keep resources and cooldowns across death; normal persistent data already saves on logout. */
    public static void copyState(Player original, Player replacement) {
        var source = original.getPersistentData();
        var target = replacement.getPersistentData();
        if (source.contains(MANA)) target.putInt(MANA, ManaRules.clamp(source.getInt(MANA)));
        for (School school : School.values()) {
            String key = COOLDOWN + school.name();
            if (source.contains(key)) target.putLong(key, source.getLong(key));
        }
    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reimagined." + school.name().toLowerCase(java.util.Locale.ROOT)));
        Spell spell=selected(stack);
        tooltip.add(Component.translatable(spell.translationKey()));
        tooltip.add(Component.translatable("tooltip.reimagined.mana_cost", spell.cost, spell.cooldown / 20.0));
        tooltip.add(Component.translatable("tooltip.reimagined.spell_cycle"));
    }
}

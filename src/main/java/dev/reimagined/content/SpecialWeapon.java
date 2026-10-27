package dev.reimagined.content;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Left click remains a vanilla sword attack. Sneak + right click performs a bounded special. */
public final class SpecialWeapon extends SwordItem {
    public enum Technique {
        SPEAR(4.5, 8, 80, 1), HAMMER(3, 7, 120, 3), DAGGER(2.5, 6, 40, 1), HALBERD(3.5, 6, 100, 3);
        final double reach;
        final float damage;
        final int cooldown;
        final int maximumTargets;
        Technique(double reach, float damage, int cooldown, int maximumTargets) {
            this.reach = reach; this.damage = damage; this.cooldown = cooldown; this.maximumTargets = maximumTargets;
        }
    }
    private final Technique technique;
    public SpecialWeapon(Properties properties, Technique technique) { super(Tiers.IRON, properties); this.technique = technique; }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack weapon = player.getItemInHand(hand);
        // Passing ordinary use allows the vanilla offhand shield to raise normally.
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return InteractionResultHolder.pass(weapon);
        if (level.isClientSide) return InteractionResultHolder.success(weapon);
        if (!(player instanceof ServerPlayer attacker) || attacker.isSpectator() || attacker.isUsingItem())
            return InteractionResultHolder.fail(weapon);
        if (!CombatState.specialReady(attacker) || attacker.getAttackStrengthScale(0) < 0.9F) {
            attacker.displayClientMessage(Component.translatable("message.reimagined.special_recharging"), true);
            return InteractionResultHolder.fail(weapon);
        }
        var candidates = level.getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(technique.reach),
                target -> eligible(attacker, target) && inArc(attacker, target) && clearPath(attacker, target))
                .stream().sorted(Comparator.comparingDouble(target -> target.distanceToSqr(attacker)))
                .limit(technique.maximumTargets).toList();
        for (LivingEntity target : candidates) {
            if (target.hurt(attacker.damageSources().playerAttack(attacker), technique.damage)) {
                if (technique == Technique.HAMMER) {
                    target.knockback(1.2, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                } else if (technique == Technique.SPEAR) {
                    target.knockback(0.5, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
                }
                attacker.serverLevel().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() / 2,
                        target.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
            }
        }
        if (technique == Technique.DAGGER) attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0));
        CombatState.startSpecial(attacker, technique.cooldown);
        // All weapon icons share this cooldown, matching the server's persistent shared gate.
        for (var item : List.of(Content.SPEAR, Content.HAMMER, Content.DAGGER, Content.HALBERD))
            attacker.getCooldowns().addCooldown(item.get(), technique.cooldown);
        attacker.resetAttackStrengthTicker();
        attacker.swing(hand, true);
        if (!attacker.isCreative()) weapon.hurtAndBreak(2, attacker, EquipmentSlot.MAINHAND);
        level.playSound(null, attacker.blockPosition(), technique == Technique.HAMMER
                ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7F, 1);
        return InteractionResultHolder.consume(weapon);
    }
    private static boolean eligible(ServerPlayer attacker, LivingEntity target) {
        return target != attacker && target.isAlive() && !target.isSpectator() && !target.isAlliedTo(attacker)
                && (!(target instanceof Player other) || attacker.canHarmPlayer(other));
    }
    private boolean inArc(ServerPlayer attacker, LivingEntity target) {
        Vec3 eye = attacker.getEyePosition();
        Vec3 look = attacker.getLookAngle();
        if (technique == Technique.SPEAR || technique == Technique.DAGGER)
            return target.getBoundingBox().inflate(0.15).clip(eye, eye.add(look.scale(technique.reach))).isPresent();
        Vec3 offset = target.getBoundingBox().getCenter().subtract(eye);
        return offset.lengthSqr() <= technique.reach * technique.reach && offset.normalize().dot(look) >= 0.5;
    }
    private static boolean clearPath(ServerPlayer attacker, LivingEntity target) {
        Vec3 start = attacker.getEyePosition();
        Vec3 end = target.getBoundingBox().getCenter();
        return attacker.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker))
                .getType() == HitResult.Type.MISS;
    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reimagined.special." + technique.name().toLowerCase(Locale.ROOT)));
        tooltip.add(Component.translatable("tooltip.reimagined.special_cooldown", technique.cooldown / 20.0));
    }
}

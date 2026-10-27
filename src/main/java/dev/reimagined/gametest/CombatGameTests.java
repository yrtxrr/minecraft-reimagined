package dev.reimagined.gametest;

import com.mojang.authlib.GameProfile;
import dev.reimagined.content.CombatState;
import dev.reimagined.content.Content;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real item use and damage processing, including vanilla shield-facing and warmup checks. */
@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class CombatGameTests {
    private CombatGameTests() {}
    /** In-memory packet sink only; ordinary ServerPlayer tick/hurt and vanilla shield timing remain active. */
    private static final class Fighter extends ServerPlayer {
        Fighter(GameTestHelper helper) {
            super(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "CombatTest"), ClientInformation.createDefault());
            connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), this,
                    CommonListenerCookie.createInitial(getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) {}
                @Override public void send(Packet<?> packet, PacketSendListener listener) {}
            };
            setGameMode(GameType.SURVIVAL);
            setPos(helper.absolutePos(new BlockPos(3, 3, 3)).getCenter().add(0, -0.5, 0));
            setYRot(0); setYHeadRot(0); setXRot(0);
            // End Minecraft's normal login invulnerability through its actual server tick path.
            for (int i = 0; i < 61; i++) tick();
            setPos(helper.absolutePos(new BlockPos(3, 3, 3)).getCenter().add(0, -0.5, 0));
            setYRot(0); setYHeadRot(0); setXRot(0);
            attackStrengthTicker = 100;
        }
        @Override public boolean isFakePlayer() { return true; }
        void recoverAttack() { attackStrengthTicker = 100; }
        void shieldFor(int ticks) {
            setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
            startUsingItem(InteractionHand.OFF_HAND);
            for (int i = 0; i < ticks; i++) updateUsingItem(getUseItem());
        }
    }
    private static Villager target(GameTestHelper helper, int x, int z) {
        Villager target = helper.spawn(EntityType.VILLAGER, new BlockPos(x, 3, z));
        target.setNoAi(true);
        return target;
    }
    private static Cow attacker(GameTestHelper helper, int x, int z) {
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(x, 3, z));
        cow.setNoAi(true);
        return cow;
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void spearSpecialPreservesNormalUseAndSharesCooldown(GameTestHelper helper) {
        Fighter fighter = new Fighter(helper);
        ItemStack spear = new ItemStack(Content.SPEAR.get());
        fighter.setItemInHand(InteractionHand.MAIN_HAND, spear);
        helper.assertTrue(Content.SPEAR.get().use(helper.getLevel(), fighter, InteractionHand.MAIN_HAND).getResult() == InteractionResult.PASS,
                "Ordinary right click must pass so offhand shields can be raised");
        Villager victim = target(helper, 3, 7);
        fighter.setShiftKeyDown(true);
        fighter.resetAttackStrengthTicker();
        helper.assertTrue(Content.SPEAR.get().use(helper.getLevel(), fighter, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL
                && spear.getDamageValue() == 0 && victim.getHealth() == victim.getMaxHealth(),
                "An uncharged vanilla attack meter must reject special without spending durability or damage");
        fighter.recoverAttack();
        helper.assertTrue(Content.SPEAR.get().use(helper.getLevel(), fighter, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                "Charged sneak-use must perform spear special");
        helper.assertTrue(victim.getHealth() < victim.getMaxHealth(), "Spear thrust must reach a real target four blocks away");
        helper.assertTrue(spear.getDamageValue() == 2, "A special must cost two durability even before a normal hit");
        long ready = CombatState.specialReadyAt(fighter);
        fighter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.DAGGER.get()));
        helper.assertTrue(Content.DAGGER.get().use(helper.getLevel(), fighter, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL,
                "Switching to dagger must not bypass shared special cooldown");
        helper.assertTrue(CombatState.specialReadyAt(fighter) == ready, "Rejected special must not extend cooldown");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void spearDoesNotHitThroughWallsOrAllies(GameTestHelper helper) {
        Fighter fighter = new Fighter(helper);
        fighter.setShiftKeyDown(true);
        fighter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.SPEAR.get()));
        Villager victim = target(helper, 3, 6);
        helper.setBlock(new BlockPos(3, 4, 5), Blocks.STONE);
        Content.SPEAR.get().use(helper.getLevel(), fighter, InteractionHand.MAIN_HAND);
        helper.assertTrue(victim.getHealth() == victim.getMaxHealth(), "A stone wall must stop special attack damage");
        helper.setBlock(new BlockPos(3, 4, 5), Blocks.AIR);
        Fighter teammate = new Fighter(helper);
        teammate.setShiftKeyDown(true);
        teammate.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.SPEAR.get()));
        var scoreboard = helper.getLevel().getScoreboard();
        var team = scoreboard.addPlayerTeam("combat_" + UUID.randomUUID().toString().substring(0, 8));
        try {
            scoreboard.addPlayerToTeam(teammate.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(victim.getScoreboardName(), team);
            Content.SPEAR.get().use(helper.getLevel(), teammate, InteractionHand.MAIN_HAND);
            helper.assertTrue(victim.getHealth() == victim.getMaxHealth(), "Special attacks must leave scoreboard allies unharmed");
        } finally { scoreboard.removePlayerTeam(team); }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void hammerAndDaggerHaveDistinctEffects(GameTestHelper helper) {
        Fighter hammer = new Fighter(helper);
        hammer.setShiftKeyDown(true);
        hammer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.HAMMER.get()));
        Villager victim = target(helper, 3, 5);
        Content.HAMMER.get().use(helper.getLevel(), hammer, InteractionHand.MAIN_HAND);
        helper.assertTrue(victim.getHealth() < victim.getMaxHealth() && victim.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "Hammer special must damage and stagger the front target");
        helper.assertTrue(victim.getDeltaMovement().horizontalDistanceSqr() > 0, "Hammer special must apply real knockback");
        victim.discard();
        Fighter dagger = new Fighter(helper);
        dagger.setShiftKeyDown(true);
        dagger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.DAGGER.get()));
        Villager next = target(helper, 3, 5);
        Content.DAGGER.get().use(helper.getLevel(), dagger, InteractionHand.MAIN_HAND);
        helper.assertTrue(next.getHealth() < next.getMaxHealth() && dagger.hasEffect(MobEffects.MOVEMENT_SPEED),
                "Dagger special must combine a short thrust with brief footwork speed");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void halberdSweepsFrontArcWithoutHittingBehind(GameTestHelper helper) {
        Fighter fighter = new Fighter(helper);
        fighter.setShiftKeyDown(true);
        fighter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.HALBERD.get()));
        Villager left = target(helper, 2, 5);
        Villager middle = target(helper, 3, 5);
        Villager right = target(helper, 4, 5);
        Villager behind = target(helper, 3, 1);
        Content.HALBERD.get().use(helper.getLevel(), fighter, InteractionHand.MAIN_HAND);
        helper.assertTrue(left.getHealth() < left.getMaxHealth() && middle.getHealth() < middle.getMaxHealth()
                && right.getHealth() < right.getMaxHealth(), "Halberd must strike three real enemies in its front arc");
        helper.assertTrue(behind.getHealth() == behind.getMaxHealth(), "Halberd special must not strike behind the player");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void specialCooldownSurvivesSaveAndDeathClone(GameTestHelper helper) {
        Fighter original = new Fighter(helper);
        original.setShiftKeyDown(true);
        original.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.SPEAR.get()));
        Content.SPEAR.get().use(helper.getLevel(), original, InteractionHand.MAIN_HAND);
        long ready = CombatState.specialReadyAt(original);
        CompoundTag saved = original.saveWithoutId(new CompoundTag());
        Fighter restored = new Fighter(helper);
        restored.load(saved);
        helper.assertTrue(CombatState.specialReadyAt(restored) == ready && !CombatState.specialReady(restored),
                "Actual player entity save/load must retain the shared special cooldown");
        Fighter replacement = new Fighter(helper);
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(replacement, original, true));
        helper.assertTrue(CombatState.specialReadyAt(replacement) == ready && !CombatState.specialReady(replacement),
                "Actual death clone event must retain cooldown so death cannot reset specials");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void timedShieldParryStaggersAttackerAndPreservesDurability(GameTestHelper helper) {
        Fighter defender = new Fighter(helper);
        defender.shieldFor(6);
        Cow first = attacker(helper, 3, 5);
        helper.assertTrue(defender.isDamageSourceBlocked(defender.damageSources().mobAttack(first)), "Front attack must satisfy vanilla shield checks");
        defender.hurt(defender.damageSources().mobAttack(first), 6);
        helper.assertTrue(defender.getHealth() == defender.getMaxHealth(), "Parry must retain vanilla full shield protection");
        helper.assertTrue(first.hasEffect(MobEffects.WEAKNESS) && first.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "The real shield damage path must trigger attacker stagger");
        helper.assertTrue(defender.getUseItem().getDamageValue() == 0, "A successful timed parry must preserve shield durability");
        Cow second = attacker(helper, 4, 5);
        defender.hurt(defender.damageSources().mobAttack(second), 6);
        helper.assertTrue(!second.hasEffect(MobEffects.WEAKNESS), "Shared parry cooldown must prevent repeated stagger during the same block window");
        helper.assertTrue(defender.getUseItem().getDamageValue() > 0, "Ordinary block during parry cooldown must use normal shield durability");
        Fighter restored = new Fighter(helper);
        restored.load(defender.saveWithoutId(new CompoundTag()));
        helper.assertTrue(!CombatState.parryReady(restored), "Parry lockout must persist in player entity save/load");
        Fighter clone = new Fighter(helper);
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(clone, defender, true));
        helper.assertTrue(!CombatState.parryReady(clone), "Death clone must retain the parry lockout");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void shieldParryHonorsVanillaWarmupFacingAndLateBlocks(GameTestHelper helper) {
        Fighter early = new Fighter(helper);
        early.shieldFor(3);
        Cow front = attacker(helper, 3, 5);
        early.hurt(early.damageSources().mobAttack(front), 6);
        helper.assertTrue(early.getHealth() < early.getMaxHealth() && !front.hasEffect(MobEffects.WEAKNESS),
                "Parry must not remove vanilla shield warmup");
        Fighter late = new Fighter(helper);
        late.shieldFor(15);
        late.hurt(late.damageSources().mobAttack(front), 6);
        helper.assertTrue(late.getHealth() == late.getMaxHealth() && !front.hasEffect(MobEffects.WEAKNESS)
                && late.getUseItem().getDamageValue() > 0, "Holding shield past parry window must still block normally");
        Fighter back = new Fighter(helper);
        back.shieldFor(6);
        Cow rear = attacker(helper, 3, 1);
        back.hurt(back.damageSources().mobAttack(rear), 6);
        helper.assertTrue(back.getHealth() < back.getMaxHealth() && !rear.hasEffect(MobEffects.WEAKNESS),
                "A rear attack must bypass shield and must never trigger parry");
        Fighter magic = new Fighter(helper);
        magic.shieldFor(6);
        magic.hurt(magic.damageSources().magic(), 6);
        helper.assertTrue(magic.getHealth() < magic.getMaxHealth() && CombatState.parryReady(magic),
                "Shield-bypassing magic must damage normally without consuming a parry window");
        helper.succeed();
    }
}

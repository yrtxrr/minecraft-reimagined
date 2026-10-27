package dev.reimagined.content;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/** Rewards a well-timed vanilla shield block; never turns unblockable damage into a block. */
public final class CombatEvents {
    private CombatEvents() {}
    public static void register() { NeoForge.EVENT_BUS.addListener(CombatEvents::parry); }
    private static void parry(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer defender) || !event.getOriginalBlock() || !event.getBlocked()) return;
        int heldTicks = defender.getTicksUsingItem();
        if (heldTicks < 5 || heldTicks > 10 || !CombatState.parryReady(defender)) return;
        if (event.getDamageSource().is(DamageTypeTags.IS_PROJECTILE)
                || !(event.getDamageSource().getDirectEntity() instanceof LivingEntity attacker)
                || attacker.isAlliedTo(defender)
                || attacker instanceof Player other && !defender.canHarmPlayer(other)) return;
        event.setShieldDamage(0);
        attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
        attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
        attacker.knockback(0.8, defender.getX() - attacker.getX(), defender.getZ() - attacker.getZ());
        CombatState.startParry(defender);
        defender.displayClientMessage(Component.translatable("message.reimagined.parry"), true);
        defender.serverLevel().sendParticles(ParticleTypes.CRIT, defender.getX(), defender.getEyeY(), defender.getZ(), 8, 0.25, 0.2, 0.25, 0.05);
    }
}

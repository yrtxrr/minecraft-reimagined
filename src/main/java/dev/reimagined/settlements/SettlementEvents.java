package dev.reimagined.settlements;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Central event wiring for settlements: settler spawning, persistence, trading,
 * schedule-driven movement and rain sheltering. All work is event/local only;
 * no world scans or chunk preloading.
 */
public final class SettlementEvents {
    private SettlementEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SettlementEvents::onJoin);
        NeoForge.EVENT_BUS.addListener(SettlementEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(SettlementEvents::onInteract);

    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Settler settler)) return;
        if (!(event.getLevel() instanceof ServerLevel server)) return;
        var data = SettlementData.get(server);
        var record = data.findSettler(settler.getUUID());
        if (record != null) {
            settler.setHome(record.home);
            settler.setWork(record.work);
        } else {
            settler.setHome(settler.blockPosition());
            settler.setWork(settler.blockPosition());
            data.registerSettler(settler.getUUID(), settler.blockPosition(), settler.blockPosition(), "commoner");
        }
        String profession = record != null ? record.profession : "commoner";
        TradeData.initializeOffersForProfession(server, profession);
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Settler settler)) return;
        if (!(settler.level() instanceof ServerLevel server)) return;
        SettlementData.get(server).removeSettler(settler.getUUID());
        TradeData.get(server).removeStock(settler.getUUID());
    }

    private static void onInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Settler settler) || event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND) return;
        if (!(settler.level() instanceof ServerLevel server)) return;
        Player player = event.getEntity();
        if (player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("message.reimagined.settler_greeting", settler.getDisplayName()), false);
        } else {
            settler.openTradingScreen(player,settler.getDisplayName(),1);
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }

}

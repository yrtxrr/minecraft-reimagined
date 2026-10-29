package dev.reimagined.settlements.network;

import dev.reimagined.settlements.TradingMenu;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class TradePacketHandler {
    public static void handleTradeExecute(TradeExecutePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.containerMenu instanceof TradingMenu menu) {
                    menu.executeTrade(player, payload.tradeIndex(), payload.tradeCount());
                }
            }
        });
    }

    private TradePacketHandler() {}
}
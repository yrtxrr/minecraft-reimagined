package dev.reimagined.settlements.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SettlementNetwork {
    public static void register(IEventBus bus) {
        bus.addListener(SettlementNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                TradeExecutePayload.TYPE,
                TradeExecutePayload.STREAM_CODEC,
                TradePacketHandler::handleTradeExecute
        );
    }

    private SettlementNetwork() {}
}
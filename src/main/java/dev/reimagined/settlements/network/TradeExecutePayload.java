package dev.reimagined.settlements.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TradeExecutePayload(int tradeIndex, int tradeCount) implements CustomPacketPayload {
    public static final Type<TradeExecutePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("reimagined", "trade_execute"));
    public static final StreamCodec<FriendlyByteBuf, TradeExecutePayload> STREAM_CODEC = StreamCodec.of(
            TradeExecutePayload::write,
            TradeExecutePayload::read
    );

    private static void write(FriendlyByteBuf buf, TradeExecutePayload payload) {
        buf.writeVarInt(payload.tradeIndex);
        buf.writeVarInt(payload.tradeCount);
    }

    private static TradeExecutePayload read(FriendlyByteBuf buf) {
        return new TradeExecutePayload(buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
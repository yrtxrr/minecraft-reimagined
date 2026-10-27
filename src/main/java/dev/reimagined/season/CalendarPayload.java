package dev.reimagined.season;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CalendarPayload(long day) implements CustomPacketPayload {
    public static final Type<CalendarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("reimagined", "calendar"));
    public static final StreamCodec<ByteBuf, CalendarPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, CalendarPayload::day, CalendarPayload::new);
    // Pure value cache: no reference to client-only Minecraft classes on a dedicated server.
    public static volatile long clientDay;
    @Override public Type<CalendarPayload> type() { return TYPE; }
}

package dev.reimagined.magic;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import dev.reimagined.research.Discoveries;
import net.neoforged.neoforge.network.PacketDistributor;

/** Owner-only changes/initial snapshot, never a per-tick broadcast or client authority. */
public record MagicProgressPayload(int known,int biomes,int creatures) implements CustomPacketPayload {
    public static final Type<MagicProgressPayload> TYPE=new Type<>(ResourceLocation.parse("reimagined:magic_progress"));
    public static final StreamCodec<ByteBuf,MagicProgressPayload> CODEC=StreamCodec.composite(
            ByteBufCodecs.VAR_INT,MagicProgressPayload::known,ByteBufCodecs.VAR_INT,MagicProgressPayload::biomes,
            ByteBufCodecs.VAR_INT,MagicProgressPayload::creatures,MagicProgressPayload::new);
    public MagicProgressPayload { known &= SpellKnowledge.ALL_ADVANCED;biomes=Math.clamp(biomes,0,256);creatures=Math.clamp(creatures,0,256); }
    private static volatile MagicProgressPayload client=new MagicProgressPayload(0,0,0);
    public static MagicProgressPayload client() { return client; }
    public static void receive(MagicProgressPayload payload) { client=payload; }
    public static void clear() { client=new MagicProgressPayload(0,0,0); }
    public boolean knows(Spell spell) { return spell.basic || (known&(1<<spell.ordinal()))!=0; }
    public static void sync(ServerPlayer player) {
        var discoveries=Discoveries.read(player);
        if(player.connection!=null)PacketDistributor.sendToPlayer(player,new MagicProgressPayload(SpellKnowledge.mask(player),discoveries.biomes().size(),discoveries.creatures().size()));
    }
    @Override public Type<MagicProgressPayload> type() { return TYPE; }
}

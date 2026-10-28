package dev.reimagined.magic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Bounded versioned player state; no registry IDs/world references or unbounded upgrade values. */
public final class SpellKnowledge {
    public static final String KEY="reimagined_spells_v1";
    public static final int ALL_ADVANCED=0xF0;
    private SpellKnowledge() {}
    public static int mask(Player player) {
        var tag=player.getPersistentData().getCompound(KEY);
        return tag.getInt("Version")==1 ? tag.getInt("Known") & ALL_ADVANCED : 0;
    }
    public static boolean knows(Player player,Spell spell) { return spell.basic || (mask(player)&(1<<spell.ordinal()))!=0; }
    public static boolean learn(ServerPlayer player,Spell spell) {
        if(knows(player,spell))return false;
        var tag=new CompoundTag();tag.putInt("Version",1);tag.putInt("Known",mask(player)|(1<<spell.ordinal()));
        player.getPersistentData().put(KEY,tag);MagicProgressPayload.sync(player);return true;
    }
    public static void copy(Player original,Player replacement) {
        if(original.getPersistentData().contains(KEY))replacement.getPersistentData().put(KEY,original.getPersistentData().getCompound(KEY).copy());
    }
}

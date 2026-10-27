package dev.reimagined.progression;

import java.math.BigInteger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative storage automatically saved with player.dat. Never trust client XP. */
public final class PlayerSkills {
    public static final String DATA_KEY = "reimagined_skills_v1";
    private PlayerSkills() {}

    public static SkillProgress get(ServerPlayer player, Skill skill) {
        CompoundTag tree = player.getPersistentData().getCompound(DATA_KEY).getCompound(skill.id());
        if (!tree.contains("xp")) return SkillProgress.EMPTY;
        try {
            return new SkillProgress(new BigInteger(tree.getString("xp")), tree.getInt("rank"));
        } catch (IllegalArgumentException ex) {
            // A malformed tree cannot grant upgrades; do not break player login.
            return SkillProgress.EMPTY;
        }
    }

    public static void set(ServerPlayer player, Skill skill, SkillProgress progress) {
        CompoundTag skills = player.getPersistentData().getCompound(DATA_KEY);
        CompoundTag tree = new CompoundTag();
        tree.putString("xp", progress.experience().toString());
        tree.putInt("rank", progress.rank());
        skills.put(skill.id(), tree);
        player.getPersistentData().put(DATA_KEY, skills);
    }

    public static int rank(ServerPlayer player, Skill skill) { return get(player, skill).rank(); }
    public static double bonusMultiplier(ServerPlayer player, Skill skill) { return get(player, skill).bonusMultiplier(); }

    public static void award(ServerPlayer player, Skill skill, long amount) {
        if (player.isCreative() || player.isSpectator() || amount <= 0) return;
        SkillProgress before = get(player, skill);
        SkillProgress after = before.award(amount);
        set(player, skill, after);
        if (!before.level().equals(after.level())) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.reimagined.skill_level", net.minecraft.network.chat.Component.translatable(skill.translationKey()),
                    after.level().toString()), false);
        }
    }
}

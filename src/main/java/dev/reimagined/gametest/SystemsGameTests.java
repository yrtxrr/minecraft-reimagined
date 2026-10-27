package dev.reimagined.gametest;

import com.mojang.authlib.GameProfile;
import dev.reimagined.content.Content;
import dev.reimagined.content.ManaRules;
import dev.reimagined.content.SpellWand;
import dev.reimagined.progression.PlayerSkills;
import dev.reimagined.progression.Skill;
import dev.reimagined.progression.SkillProgress;
import dev.reimagined.season.CalendarData;
import dev.reimagined.season.SeasonCalendar;
import java.math.BigInteger;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Server integration tests: real registries, NBT, event bus, commands and item use. */
@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class SystemsGameTests {
    private SystemsGameTests() {}

    /** NeoForge's supported ServerPlayer simulator; no real network client is required. */
    private static FakePlayer player(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ReimaginedTest"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(3, 3, 3)).getCenter());
        return player;
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void calendarOffsetSurvivesSavedDataRoundtrip(GameTestHelper helper) {
        var level = helper.getLevel();
        CalendarData calendar = new CalendarData();
        long timeBefore = level.getServer().overworld().getDayTime();
        calendar.setSeason(level, 3);
        helper.assertTrue(calendar.isDirty(), "Changing season must mark SavedData dirty");
        helper.assertTrue(SeasonCalendar.season(calendar.day(level)) == SeasonCalendar.Season.WINTER,
                "Season offset must select winter without changing vanilla time");
        helper.assertTrue(level.getServer().overworld().getDayTime() == timeBefore,
                "Season offset must not change vanilla day time");
        CompoundTag saved = calendar.save(new CompoundTag(), level.registryAccess());
        CalendarData restored = CalendarData.load(saved, level.registryAccess());
        helper.assertTrue(restored.day(level) == calendar.day(level), "Saved calendar must preserve its day offset");
        helper.assertTrue(saved.getInt("Version") == 1, "Calendar save must carry schema version");
        restored.setSeason(level, 0);
        helper.assertTrue(SeasonCalendar.season(restored.day(level)) == SeasonCalendar.Season.SPRING,
                "Restored calendar must remain editable");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void calendarCommandsUseRegisteredWorldData(GameTestHelper helper) throws Exception {
        var level = helper.getLevel().getServer().overworld();
        var storage = level.getDataStorage();
        CompoundTag previous = CalendarData.get(level).save(new CompoundTag(), level.registryAccess());
        try {
            var dispatcher = level.getServer().getCommands().getDispatcher();
            var source = level.getServer().createCommandSourceStack();
            helper.assertTrue(dispatcher.execute("reimagined season set 2", source) == 1,
                    "Registered operator command must execute");
            helper.assertTrue(SeasonCalendar.season(CalendarData.get(level).day(level)) == SeasonCalendar.Season.AUTUMN,
                    "Season command must mutate world-owned calendar data");
            helper.assertTrue(dispatcher.execute("reimagined calendar", source) == 1,
                    "Registered calendar query must execute");
        } finally {
            CalendarData restored = CalendarData.load(previous, level.registryAccess());
            restored.setDirty();
            storage.set("reimagined_calendar", restored);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void playerSkillsSurviveEntitySaveAndDeathClone(GameTestHelper helper) {
        FakePlayer original = player(helper);
        SkillProgress learned = new SkillProgress(BigInteger.valueOf(600), 3);
        PlayerSkills.set(original, Skill.MINER, learned);
        PlayerSkills.set(original, Skill.MAGE, SkillProgress.EMPTY.award(125));
        CompoundTag saved = original.saveWithoutId(new CompoundTag());
        FakePlayer restored = player(helper);
        restored.load(saved);
        helper.assertTrue(PlayerSkills.get(restored, Skill.MINER).equals(learned),
                "Player entity save/load must preserve XP and learned ranks");
        FakePlayer replacement = player(helper);
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(replacement, original, true));
        helper.assertTrue(PlayerSkills.get(replacement, Skill.MINER).equals(learned),
                "Actual clone listener must preserve earned mastery after death");
        helper.assertTrue(PlayerSkills.get(replacement, Skill.MAGE).experience().equals(BigInteger.valueOf(125)),
                "Clone listener must preserve all trees, not only the active tree");
        PlayerSkills.set(original, Skill.MINER, SkillProgress.EMPTY);
        helper.assertTrue(PlayerSkills.get(replacement, Skill.MINER).equals(learned),
                "Cloned skills must not share mutable NBT with the old player");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void natureWandConsumesManaAndEnforcesTimedCooldown(GameTestHelper helper) {
        FakePlayer caster = player(helper);
        caster.setHealth(10);
        var wand = Content.NATURE_WAND.get();
        caster.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(wand));
        helper.assertTrue(SpellWand.mana(caster) == ManaRules.CAPACITY, "New player must begin with full mana");
        var first = wand.use(helper.getLevel(), caster, InteractionHand.MAIN_HAND);
        helper.assertTrue(first.getResult().consumesAction(), "Wounded player must cast nature heal");
        helper.assertTrue(caster.getHealth() == 14, "Nature cast must heal exactly four health");
        helper.assertTrue(SpellWand.mana(caster) == 75, "Successful nature cast must consume 25 mana");
        helper.assertTrue(PlayerSkills.get(caster, Skill.MAGE).experience().equals(BigInteger.valueOf(5)),
                "Successful spell must award server-owned mage experience");
        var repeated = wand.use(helper.getLevel(), caster, InteractionHand.MAIN_HAND);
        helper.assertTrue(repeated.getResult() == InteractionResult.FAIL, "Immediate repeated cast must fail during cooldown");
        helper.assertTrue(SpellWand.mana(caster) == 75 && caster.getHealth() == 14,
                "Rejected cooldown cast must neither spend mana nor heal");
        helper.runAfterDelay(105, () -> {
            var second = wand.use(helper.getLevel(), caster, InteractionHand.MAIN_HAND);
            helper.assertTrue(second.getResult().consumesAction(), "Cast must work after the actual server cooldown expires");
            helper.assertTrue(caster.getHealth() == 18 && SpellWand.mana(caster) == 50,
                    "Second cast must apply healing and spend mana exactly once");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void failedNatureCastDoesNotGrantFreeHealingOrExperience(GameTestHelper helper) {
        FakePlayer caster = player(helper);
        caster.setHealth(10);
        caster.getPersistentData().putInt("reimagined_mana", 0);
        var wand = Content.NATURE_WAND.get();
        caster.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(wand));
        var failed = wand.use(helper.getLevel(), caster, InteractionHand.MAIN_HAND);
        helper.assertTrue(failed.getResult() == InteractionResult.FAIL, "Insufficient mana must reject spell use");
        helper.assertTrue(caster.getHealth() == 10 && SpellWand.mana(caster) == 0,
                "Failed cast must not heal or invent mana");
        helper.assertTrue(PlayerSkills.get(caster, Skill.MAGE).experience().signum() == 0,
                "Failed spell must not grant mage experience");
        helper.succeed();
    }
}

package dev.reimagined.season;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class Seasons {
    private Seasons() {}
    public static void register(IEventBus modBus) {
        modBus.addListener(Seasons::payloads);
        NeoForge.EVENT_BUS.addListener(Seasons::tick);
        NeoForge.EVENT_BUS.addListener(Seasons::login);
        NeoForge.EVENT_BUS.addListener(Seasons::grow);
        NeoForge.EVENT_BUS.addListener(Seasons::commands);
    }
    private static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(CalendarPayload.TYPE, CalendarPayload.CODEC,
            (payload, context) -> CalendarPayload.clientDay = payload.day());
    }
    private static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 100 != 0) return;
        var level = event.getServer().overworld();
        PacketDistributor.sendToAllPlayers(new CalendarPayload(CalendarData.get(level).day(level)));
    }
    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            PacketDistributor.sendToPlayer(player, new CalendarPayload(CalendarData.get(player.serverLevel()).day(player.serverLevel())));
    }
    private static void grow(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        var pos = event.getPos();
        long day = CalendarData.get(level).day(level);
        float temperature = level.getBiome(pos).value().getBaseTemperature();
        int chance = SeasonCalendar.growthChance(day, temperature, false);
        if (chance == 100 || sheltered(level, pos)) return;
        if (level.random.nextInt(100) >= chance) event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
    }
    /** A glass roof within 16 blocks shelters crops. No block outside the crop's loaded column is touched. */
    private static boolean sheltered(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        for (int dy = 1; dy <= 16 && pos.getY() + dy < level.getMaxBuildHeight(); dy++) {
            BlockState state = level.getBlockState(pos.above(dy));
            if (state.getBlock() instanceof AbstractGlassBlock || state.is(Blocks.GLASS_PANE) || state.is(BlockTags.IMPERMEABLE)) return true;
            if (state.canOcclude()) return false;
        }
        return false;
    }
    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("reimagined")
            .then(Commands.literal("calendar").executes(ctx -> {
                var level = ctx.getSource().getServer().overworld();
                long day = CalendarData.get(level).day(level);
                ctx.getSource().sendSuccess(() -> Component.translatable("message.reimagined.calendar",
                    Component.translatable("season.reimagined." + SeasonCalendar.season(day).name().toLowerCase(java.util.Locale.ROOT)),
                    SeasonCalendar.dayOfSeason(day), 24, day), false);
                return 1;
            }))
            .then(Commands.literal("season").requires(source -> source.hasPermission(2))
                .then(Commands.literal("set").then(Commands.argument("season", IntegerArgumentType.integer(0, 3)).executes(ctx -> {
                    var level = ctx.getSource().getServer().overworld();
                    var data = CalendarData.get(level);
                    data.setSeason(level, IntegerArgumentType.getInteger(ctx, "season"));
                    PacketDistributor.sendToAllPlayers(new CalendarPayload(data.day(level)));
                    ctx.getSource().sendSuccess(() -> Component.translatable("message.reimagined.season_set"), true);
                    return 1;
                })))));
    }
}

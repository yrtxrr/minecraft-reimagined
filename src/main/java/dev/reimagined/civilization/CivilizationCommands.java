package dev.reimagined.civilization;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Operator tooling for the prototype. Settlements are world-owned and cannot be founded by players. */
final class CivilizationCommands {
    private CivilizationCommands() {}

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("reimagined").then(Commands.literal("settlement")
                .then(Commands.literal("list").executes(CivilizationCommands::list))
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    String remaining = builder.getRemainingLowerCase();
                                    for (Settlement settlement : data(context.getSource()).all()) {
                                        String id = settlement.id().toString();
                                        if (id.startsWith(remaining)) builder.suggest(id);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(CivilizationCommands::info)))
                .then(Commands.literal("advance").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (Settlement settlement : data(context.getSource()).all()) {
                                        builder.suggest(settlement.id().toString());
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("hours", IntegerArgumentType.integer(1, CivilizationSimulation.MAX_COMMAND_HOURS))
                                        .executes(CivilizationCommands::advance))))
                .then(Commands.literal("found").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (SettlementType type : SettlementType.values()) builder.suggest(type.id);
                                    return builder.buildFuture();
                                })
                                .executes(context -> found(context.getSource(), SettlementType.byId(
                                        StringArgumentType.getString(context, "type")), ""))
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> found(context.getSource(), SettlementType.byId(
                                                        StringArgumentType.getString(context, "type")),
                                                StringArgumentType.getString(context, "name"))))))));
    }

    private static SettlementData data(CommandSourceStack source) {
        return SettlementData.get(source.getServer());
    }

    private static int found(CommandSourceStack source, SettlementType type, String name) {
        if (type == null) {
            source.sendSuccess(() -> Component.translatable("message.reimagined.settlement_unknown_type"), false);
            return 0;
        }
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendSuccess(() -> Component.translatable("message.reimagined.settlement_overworld_only"), false);
            return 0;
        }
        BlockPos position = BlockPos.containing(source.getPosition());
        SettlementData.FoundResult result = data(source).found(level, type, name, position);
        if (result.reason() == SettlementData.Reason.LIMIT) {
            source.sendSuccess(() -> Component.translatable("message.reimagined.settlement_found_limit",
                    SettlementData.MAX_SETTLEMENTS), false);
            return 0;
        }
        if (result.reason() == SettlementData.Reason.TOO_CLOSE) {
            source.sendSuccess(() -> Component.translatable("message.reimagined.settlement_found_too_close",
                    SettlementData.MIN_SPACING), false);
            return 0;
        }
        Settlement settlement = result.settlement();
        CivilizationSimulation.announceFounding(level, settlement);
        source.sendSuccess(() -> Component.translatable("message.reimagined.settlement_found", settlement.name(),
                Component.translatable(type.translationKey()), position.toShortString()), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SettlementData data = data(source);
        if (data.size() == 0) {
            source.sendSuccess(() -> Component.translatable("message.reimagined.settlement_none"), false);
            return 1;
        }
        source.sendSuccess(() -> Component.translatable("message.reimagined.settlement_list_header",
                data.size(), SettlementData.MAX_SETTLEMENTS), false);
        for (Settlement settlement : data.all()) {
            source.sendSuccess(() -> Component.translatable("message.reimagined.settlement_list_entry",
                    settlement.name(), Component.translatable(settlement.type().translationKey()),
                    Component.translatable(settlement.era().translationKey()),
                    settlement.population(), settlement.housing()), false);
        }
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        Settlement settlement = data(context.getSource()).find(StringArgumentType.getString(context, "id")).orElse(null);
        if (settlement == null) {
            context.getSource().sendSuccess(() -> Component.translatable("message.reimagined.settlement_not_found"), false);
            return 0;
        }
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable("message.reimagined.settlement_info_header",
                settlement.name(), settlement.id().toString()), false);
        source.sendSuccess(() -> Component.translatable("message.reimagined.settlement_info_state",
                Component.translatable(settlement.type().translationKey()),
                Component.translatable(settlement.era().translationKey()),
                settlement.population(), settlement.housing(), settlement.housingCap()), false);
        source.sendSuccess(() -> Component.translatable("message.reimagined.settlement_info_stocks",
                settlement.food(), settlement.materials(), settlement.knowledge(), settlement.treasury()), false);
        return 1;
    }

    private static int advance(CommandContext<CommandSourceStack> context) {
        SettlementData data = data(context.getSource());
        Settlement settlement = data.find(StringArgumentType.getString(context, "id")).orElse(null);
        CommandSourceStack source = context.getSource();
        if (settlement == null) {
            source.sendSuccess(() -> Component.translatable("message.reimagined.settlement_not_found"), false);
            return 0;
        }
        int hours = IntegerArgumentType.getInteger(context, "hours");
        CivilizationSimulation.advanceHours(source.getLevel(), settlement, hours);
        source.sendSuccess(() -> Component.translatable("message.reimagined.settlement_advanced",
                settlement.name(), hours), true);
        return 1;
    }
}

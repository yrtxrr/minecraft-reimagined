package dev.reimagined.core;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** No scan/tick worker, network channel or duplicate journal/skills database. */
public final class CoreBootstrap {
    private static final ModConfigSpec SPEC;
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DISABLED;
    static {
        var builder=new ModConfigSpec.Builder();
        DISABLED=builder.comment("Disabled optional adapter mod IDs. Takes effect on restart; never removes registry content.")
            .defineListAllowEmpty("disabledIntegrations",java.util.List.of(),value -> value instanceof String s && s.matches("[a-z][a-z0-9_]{1,63}"));
        SPEC=builder.build();
    }
    private CoreBootstrap() { }
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON,SPEC);
        NeoForge.EVENT_BUS.addListener(CoreBootstrap::clonePlayer);
        NeoForge.EVENT_BUS.addListener(CoreBootstrap::commands);
    }
    public static boolean integrate(String modId, java.util.function.Supplier<? extends Runnable> adapter) {
        return IntegrationGate.run(modId,java.util.Set.copyOf(DISABLED.get()),() -> ModList.get().isLoaded(modId),adapter);
    }
    private static void clonePlayer(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        if (event.getOriginal().getPersistentData().contains(CoreApi.PLAYER_KEY))
            event.getEntity().getPersistentData().put(CoreApi.PLAYER_KEY,event.getOriginal().getPersistentData().getCompound(CoreApi.PLAYER_KEY).copy());
    }
    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("reimagined_core").then(Commands.literal("status").executes(ctx -> {
            var player=ctx.getSource().getPlayerOrException(); var state=CoreApi.progress(player);
            ctx.getSource().sendSuccess(() -> Component.translatable("message.reimagined.core_status",state.knowledge().size(),state.eras().size()),false); return 1;
        })));
    }
}

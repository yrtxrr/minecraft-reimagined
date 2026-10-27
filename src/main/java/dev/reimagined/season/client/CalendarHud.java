package dev.reimagined.season.client;

import dev.reimagined.season.CalendarPayload;
import dev.reimagined.season.SeasonCalendar;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = "reimagined", value = Dist.CLIENT)
public final class CalendarHud {
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null || mc.getDebugOverlay().showDebugScreen()) return;
        long day = CalendarPayload.clientDay;
        Component text = Component.translatable("hud.reimagined.calendar",
            Component.translatable("season.reimagined." + SeasonCalendar.season(day).name().toLowerCase(java.util.Locale.ROOT)),
            SeasonCalendar.dayOfSeason(day));
        event.getGuiGraphics().drawString(mc.font, text, 6, 6, 0xffe3d6ac);
    }
}

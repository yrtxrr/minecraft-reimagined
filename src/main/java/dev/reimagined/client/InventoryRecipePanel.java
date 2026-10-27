package dev.reimagined.client;

import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Client-only event subscriber; dedicated servers never resolve Minecraft client classes. */
@EventBusSubscriber(modid = "reimagined", value = Dist.CLIENT)
public final class InventoryRecipePanel {
    private static final WeakHashMap<AbstractContainerScreen<?>, Panel> PANELS = new WeakHashMap<>();
    private InventoryRecipePanel() {}

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        int x = screen.getGuiLeft() + screen.getXSize() + 4;
        int available = screen.width - x - 4;
        // Every GUI scale still has an entry point; the item panel requires actual free space.
        event.addListener(Button.builder(Component.literal("R"), b ->
                Minecraft.getInstance().setScreen(new RecipeBrowserScreen(screen)))
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("reimagined.browser.title")))
                .bounds(Math.min(screen.width - 22, x), Math.max(2, screen.getGuiTop() - 22), 20, 20).build());
        PANELS.remove(screen);
        if (available < 72) return;
        Panel panel = new Panel(x, Math.max(24, screen.getGuiTop()), Math.min(available, 144), screen.height);
        PANELS.put(screen, panel);
        EditBox search = new EditBox(Minecraft.getInstance().font, x, panel.y, panel.width, 18,
                Component.translatable("reimagined.browser.search"));
        search.setMaxLength(128);
        search.setHint(Component.translatable("reimagined.browser.search"));
        search.setResponder(query -> { panel.items = RecipeCatalog.items(query, RecipeCatalog.Category.ALL); panel.page = 0; });
        panel.search = search;
        event.addListener(search);
        event.addListener(Button.builder(Component.literal("<"), b -> panel.page = Math.max(0, panel.page - 1))
                .bounds(x, panel.bottom, 20, 18).build());
        event.addListener(Button.builder(Component.literal(">"), b -> panel.page = Math.min(panel.pages() - 1, panel.page + 1))
                .bounds(x + panel.width - 20, panel.bottom, 20, 18).build());
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        Panel panel = PANELS.get(screen);
        if (panel == null) return;
        var graphics = event.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        graphics.fill(panel.x - 2, panel.y + 20, panel.x + panel.width + 2, panel.bottom - 2, 0xBB202428);
        ItemStack hovered = ItemStack.EMPTY;
        for (int i = 0; i < panel.size(); i++) {
            int index = panel.page * panel.size() + i;
            if (index >= panel.items.size()) break;
            int x = panel.x + i % panel.columns * 18;
            int y = panel.y + 24 + i / panel.columns * 18;
            var stack = panel.items.get(index);
            graphics.renderItem(stack, x, y);
            if (RecipeBrowserScreen.inside(event.getMouseX(), event.getMouseY(), x, y, 16, 16)) {
                hovered = stack;
            }
        }
        graphics.drawCenteredString(font, (panel.page + 1) + "/" + panel.pages(), panel.x + panel.width / 2, panel.bottom + 5, 0xFFFFFF);
        if (!hovered.isEmpty()) graphics.renderTooltip(font, hovered, event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public static void key(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        Panel panel = PANELS.get(screen);
        if (panel != null && panel.search != null && panel.search.isFocused()) {
            // Container hotkeys (notably E) must not close the inventory while entering a query.
            if (event.getKeyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) panel.search.setFocused(false);
            else panel.search.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
            event.setCanceled(true);
            return;
        }
        var slot = screen.getSlotUnderMouse();
        if (slot != null && slot.hasItem() && (event.getKeyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_R
                || event.getKeyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_U)) {
            Minecraft.getInstance().setScreen(new RecipeBrowserScreen(screen, slot.getItem(),
                    event.getKeyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_U));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (event.getButton() != 0 && event.getButton() != 1) return;
        Panel panel = PANELS.get(screen);
        if (panel == null) return;
        for (int i = 0; i < panel.size(); i++) {
            int index = panel.page * panel.size() + i;
            if (index >= panel.items.size()) break;
            if (RecipeBrowserScreen.inside(event.getMouseX(), event.getMouseY(), panel.x + i % panel.columns * 18,
                    panel.y + 24 + i / panel.columns * 18, 16, 16)) {
                Minecraft.getInstance().setScreen(new RecipeBrowserScreen(screen, panel.items.get(index), event.getButton() == 1));
                event.setCanceled(true);
                return;
            }
        }
    }

    private static final class Panel {
        final int x, y, width, columns, rows, bottom;
        int page;
        EditBox search;
        List<ItemStack> items = RecipeCatalog.items("", RecipeCatalog.Category.ALL);
        Panel(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width;
            columns = Math.max(1, width / 18);
            rows = Math.max(1, (height - y - 50) / 18);
            bottom = y + 26 + rows * 18;
        }
        int size() { return columns * rows; }
        int pages() { return Math.max(1, (items.size() + size() - 1) / size()); }
    }
}

package dev.reimagined.client;

import dev.reimagined.browser.BrowserItems;

import java.util.List;
import java.util.ArrayList;
import dev.reimagined.browser.navigation.BrowserHistory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import dev.reimagined.browser.RecipeDisplay;
import dev.reimagined.browser.navigation.BrowserPages;
import net.minecraft.resources.ResourceLocation;

/** Standalone item/recipe browser. No item spawning or client-side crafting authority. */
public final class RecipeBrowserScreen extends Screen {
    private final Screen parent;
    private List<ItemStack> items = List.of();
    private List<RecipeDisplay> recipes = List.of();
    private BrowserItems.Category category = BrowserItems.Category.ALL;
    private EditBox search;
    private String query = "";
    private ItemStack selected = ItemStack.EMPTY;
    private boolean usages;
    private int page;
    private int recipePage;
    private int columns;
    private int rows;
    private int gridWidth;
    private long catalogRevision = -1;
    private String language = "";
    private final List<ItemHit> recipeItems = new ArrayList<>();
    private final BrowserHistory<Selection> history = new BrowserHistory<>(64);
    private Button historyButton;
    private Button recipesButton;
    private Button usagesButton;

    public RecipeBrowserScreen(Screen parent) {
        super(Component.translatable("reimagined.browser.title"));
        this.parent = parent;
    }

    public RecipeBrowserScreen(Screen parent, ItemStack selected, boolean usages) {
        this(parent);
        this.selected = selected.copy();
        this.usages = usages;
    }

    @Override
    protected void init() {
        gridWidth = Math.max(108, Math.min(width / 2, 216));
        columns = Math.max(1, (gridWidth - 16) / 18);
        rows = Math.max(1, (height - 105) / 18);
        search = addRenderableWidget(new EditBox(font, 8, 26, gridWidth - 16, 18,
                Component.translatable("reimagined.browser.search")));
        search.setMaxLength(128);
        search.setValue(query);
        search.setHint(Component.translatable("reimagined.browser.search"));
        search.setResponder(value -> { query = value; page = 0; refreshItems(); });
        addRenderableWidget(Button.builder(categoryName(), b -> {
            var values = BrowserItems.Category.values();
            category = values[(category.ordinal() + 1) % values.length];
            b.setMessage(categoryName());
            page = 0;
            refreshItems();
        }).bounds(8, 48, gridWidth - 16, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> page = Math.max(0, page - 1))
                .bounds(8, height - 28, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> page = Math.min(pageCount() - 1, page + 1))
                .bounds(gridWidth - 32, height - 28, 24, 20).build());
        recipesButton = addRenderableWidget(Button.builder(Component.translatable("reimagined.browser.recipes"), b -> navigate(selected, false))
                .bounds(gridWidth + 8, 26, Math.max(48, (width - gridWidth - 24) / 2), 20).build());
        usagesButton = addRenderableWidget(Button.builder(Component.translatable("reimagined.browser.usages"), b -> navigate(selected, true))
                .bounds(gridWidth + 12 + Math.max(48, (width - gridWidth - 24) / 2), 26,
                        Math.max(48, (width - gridWidth - 24) / 2), 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> recipePage = Math.max(0, recipePage - 1))
                .bounds(gridWidth + 8, height - 28, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> recipePage = Math.min(Math.max(0, recipes.size() - 1), recipePage + 1))
                .bounds(gridWidth + 36, height - 28, 24, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(width - 66, height - 28, 58, 20).build());
        historyButton = addRenderableWidget(Button.builder(Component.literal("←"), b -> previousSelection())
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("reimagined.browser.previous")))
                .bounds(width - 28, 5, 20, 18).build());
        refreshItems();
        refreshRecipes(false);
        catalogRevision = RecipeCatalog.revision();
        language = minecraft.getLanguageManager().getSelected();
        updateButtons();
    }

    private Component categoryName() {
        return Component.translatable("reimagined.browser.category." + category.name().toLowerCase(java.util.Locale.ROOT));
    }

    private int pageCount() { return BrowserPages.count(items.size(), columns * rows); }
    private void refreshItems() {
        items = RecipeCatalog.items(query, category);
        page = BrowserPages.clamp(page, items.size(), columns * rows);
    }

    private void select(ItemStack stack, boolean uses) {
        selected = stack.copy();
        usages = uses;
        recipePage = 0;
        recipeItems.clear();
        refreshRecipes(true);
    }

    private void navigate(ItemStack stack, boolean uses) {
        if (ItemStack.isSameItemSameComponents(selected, stack) && usages == uses) return;
        if (!selected.isEmpty()) {
            history.push(new Selection(selected.copy(), usages, recipePage));
        }
        select(stack, uses);
        setFocused(null); search.setFocused(false);
        updateButtons();
    }

    private void previousSelection() {
        history.back().ifPresent(previous -> {
            select(previous.item(), previous.usages());
            recipePage = BrowserPages.clamp(previous.recipePage(), recipes.size(), 1);
        });
        updateButtons();
    }

    private void updateButtons() {
        historyButton.active = history.size() > 0;
        recipesButton.active = usages;
        usagesButton.active = !usages;
    }

    private void refreshRecipes(boolean reset) {
        ResourceLocation previous = !reset && !recipes.isEmpty() && recipePage < recipes.size()
                ? recipes.get(recipePage).id() : null;
        recipes = selected.isEmpty() ? List.of() : RecipeCatalog.recipes(selected, usages);
        recipePage = BrowserPages.clamp(recipePage, recipes.size(), 1);
        if (previous != null) {
            for (int i = 0; i < recipes.size(); i++) if (recipes.get(i).id().equals(previous)) { recipePage = i; break; }
        }
        recipeItems.clear();
    }

    @Override public void tick() {
        super.tick();
        long revision = RecipeCatalog.revision();
        String currentLanguage = minecraft.getLanguageManager().getSelected();
        if (revision != catalogRevision || !currentLanguage.equals(language)) {
            catalogRevision = revision; language = currentLanguage;
            refreshItems(); refreshRecipes(false);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(4, 4, width - 4, height - 4, 0xE0202428);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen.render includes the background in1.21.1: calling it after our content
        // would blur all item icons/text a second time. Draw it and its widgets first.
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, title, 8, 10, 0xFFFFFF);
        recipeItems.clear();
        ItemStack hovered = ItemStack.EMPTY;
        int start = page * columns * rows;
        for (int i = 0; i < columns * rows && start + i < items.size(); i++) {
            int x = 8 + (i % columns) * 18;
            int y = 74 + (i / columns) * 18;
            ItemStack stack = items.get(start + i);
            graphics.renderItem(stack, x, y);
            if (inside(mouseX, mouseY, x, y, 16, 16)) hovered = stack;
        }
        graphics.drawCenteredString(font, (page + 1) + "/" + pageCount(), gridWidth / 2, height - 22, 0xFFFFFF);
        int x = gridWidth + 8;
        if (!selected.isEmpty()) {
            graphics.drawString(font, font.plainSubstrByWidth(selected.getHoverName().getString(), width - x - 8), x, 52, 0xFFFFFF);
            graphics.drawString(font, Component.translatable(usages ? "reimagined.browser.usages_count" : "reimagined.browser.recipes_count", recipes.size()), x, 65, 0xBBBBBB);
        } else {
            graphics.drawWordWrap(font, Component.translatable("reimagined.browser.help"), x, 55, Math.max(40, width - x - 8), 0xCCCCCC);
        }
        if (!recipes.isEmpty()) {
            var recipe = recipes.get(recipePage);
            ItemStack station = recipe.station();
            graphics.drawString(font, font.plainSubstrByWidth(station.getHoverName().getString(), width - x - 8), x, 82, 0xFFE0A0);
            if (minecraft.options.advancedItemTooltips) {
                graphics.drawString(font, font.plainSubstrByWidth(recipe.id().toString(), width - x - 8), x, 94, 0x999999);
            }
            int ingredientColumns = recipe.columns() > 0 ? recipe.columns() : Math.max(1, (width - x - 8) / 20);
            var ingredients = recipe.inputs();
            for (int i = 0; i < ingredients.size(); i++) {
                int ix = x + (i % ingredientColumns) * 20;
                int iy = 112 + (i / ingredientColumns) * 20;
                ItemStack stack = recipe.ingredient(i, minecraft.level.getGameTime());
                graphics.fill(ix - 1, iy - 1, ix + 17, iy + 17, 0xFF44484C);
                graphics.renderItem(stack, ix, iy);
                graphics.renderItemDecorations(font, stack, ix, iy);
                if (!stack.isEmpty()) recipeItems.add(new ItemHit(ix, iy, stack));
                if (inside(mouseX, mouseY, ix, iy, 16, 16)) hovered = stack;
            }
            int outputY = 120 + ((ingredients.size() + ingredientColumns - 1) / ingredientColumns) * 20;
            ItemStack output = recipe.result();
            graphics.drawString(font, Component.translatable("reimagined.browser.result"), x, outputY, 0xCCCCCC);
            graphics.renderItem(output, x, outputY + 12);
            graphics.renderItemDecorations(font, output, x, outputY + 12);
            if (!output.isEmpty()) recipeItems.add(new ItemHit(x, outputY + 12, output));
            if (inside(mouseX, mouseY, x, outputY + 12, 16, 16)) hovered = output;
            if (!station.isEmpty()) {
                graphics.renderItem(station, x + 28, outputY + 12);
                recipeItems.add(new ItemHit(x + 28, outputY + 12, station));
                if (inside(mouseX, mouseY, x + 28, outputY + 12, 16, 16)) hovered = station;
            }
            int noteY = outputY + 34;
            if (noteY < height - 32 && !recipe.note().getString().isEmpty()) {
                graphics.enableScissor(x, noteY, width - 8, height - 32);
                graphics.drawWordWrap(font, recipe.note(), x, noteY, Math.max(40, width - x - 8), 0xBBBBBB);
                graphics.disableScissor();
            }
            graphics.drawString(font, (recipePage + 1) + "/" + recipes.size(), gridWidth + 72, height - 22, 0xFFFFFF);
        } else if (!selected.isEmpty()) {
            graphics.drawWordWrap(font, Component.translatable("reimagined.browser.no_recipes"), x, 88,
                    Math.max(40, width - x - 8), 0xCCCCCC);
        }
        if (!hovered.isEmpty()) graphics.renderTooltip(font, hovered, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            for (int i = 0; i < columns * rows; i++) {
                int index = page * columns * rows + i;
                if (index >= items.size()) break;
                if (inside(mouseX, mouseY, 8 + i % columns * 18, 74 + i / columns * 18, 16, 16)) {
                    navigate(items.get(index), button == 1);
                    return true;
                }
            }
            for (ItemHit hit : recipeItems) {
                if (inside(mouseX, mouseY, hit.x(), hit.y(), 16, 16)) {
                    navigate(hit.stack(), button == 1);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (!search.isFocused()) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) { previousSelection(); return true; }
            ItemStack hover = ItemStack.EMPTY;
            double mx = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
            double my = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
            for (ItemHit hit : recipeItems) if (inside(mx, my, hit.x(), hit.y(), 16, 16)) hover = hit.stack();
            for (int i = 0; i < columns * rows && page * columns * rows + i < items.size(); i++) {
                if (inside(mx, my, 8 + i % columns * 18, 74 + i / columns * 18, 16, 16))
                    hover = items.get(page * columns * rows + i);
            }
            if (!hover.isEmpty() && (key == org.lwjgl.glfw.GLFW.GLFW_KEY_R || key == org.lwjgl.glfw.GLFW.GLFW_KEY_U)) {
                navigate(hover, key == org.lwjgl.glfw.GLFW.GLFW_KEY_U); return true;
            }
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    private record ItemHit(int x, int y, ItemStack stack) {}
    private record Selection(ItemStack item, boolean usages, int recipePage) {}
}

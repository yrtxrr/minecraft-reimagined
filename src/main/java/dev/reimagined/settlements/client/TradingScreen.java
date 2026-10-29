package dev.reimagined.settlements.client;

import dev.reimagined.settlements.SettlementContent;
import dev.reimagined.settlements.TradeData;
import dev.reimagined.settlements.TradeOffer;
import dev.reimagined.settlements.TradingMenu;
import dev.reimagined.settlements.network.TradeExecutePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class TradingScreen extends AbstractContainerScreen<TradingMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("reimagined", "textures/gui/trading.png");
    private static final int TRADE_BUTTON_WIDTH = 100;
    private static final int TRADE_BUTTON_HEIGHT = 20;
    private static final int TRADE_LIST_X = 7;
    private static final int TRADE_LIST_Y = 18;
    private static final int MAX_VISIBLE_TRADES = 7;

    private int scrollOffset = 0;
    private Button incrementButton;
    private Button decrementButton;
    private Button executeButton;

    public TradingScreen(TradingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 64;
        this.titleLabelX = 7;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + imageWidth + 4;
        int y = topPos + 18;

        incrementButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            if (menu.getCurrentTradeCount() < menu.getMaxUses() - menu.getUses()
                    && menu.getCurrentTradeCount() < menu.getInputCount()) {
                menu.incrementTradeCount();
            }
        }).bounds(x + 60, y + 30, 20, 20).build());

        decrementButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            if (menu.getCurrentTradeCount() > 1) {
                menu.decrementTradeCount();
            }
        }).bounds(x + 60, y + 10, 20, 20).build());

        executeButton = addRenderableWidget(Button.builder(Component.translatable("gui.reimagined.trade.execute"), b -> {
            if (menu.getSelectedTradeIndex() >= 0 && menu.getCurrentTradeCount() > 0) {
                PacketDistributor.sendToServer(new TradeExecutePayload(menu.getSelectedTradeIndex(), menu.getCurrentTradeCount()));
            }
        }).bounds(x + 10, y + 55, 100, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 176, 166);

        List<TradeOffer> offers = menu.getOffers();
        if (offers.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.reimagined.trade.no_offers").getString(),
                    leftPos + 7, topPos + 18, 0xFF404040);
            return;
        }

        // Render trade list background
        graphics.fill(leftPos + TRADE_LIST_X, topPos + TRADE_LIST_Y,
                leftPos + TRADE_LIST_X + TRADE_BUTTON_WIDTH, topPos + TRADE_LIST_Y + MAX_VISIBLE_TRADES * TRADE_BUTTON_HEIGHT,
                0xFF202020);

        // Render each visible trade
        for (int i = 0; i < MAX_VISIBLE_TRADES && scrollOffset + i < offers.size(); i++) {
            int index = scrollOffset + i;
            TradeOffer offer = offers.get(index);
            int tx = leftPos + TRADE_LIST_X;
            int ty = topPos + TRADE_LIST_Y + i * TRADE_BUTTON_HEIGHT;

            boolean selected = index == menu.getSelectedTradeIndex();
            boolean canTrade = offer.canTrade();

            // Trade button background
            if (selected) {
                graphics.fill(tx, ty, tx + TRADE_BUTTON_WIDTH, ty + TRADE_BUTTON_HEIGHT, 0xFF404080);
            } else if (canTrade) {
                graphics.fill(tx, ty, tx + TRADE_BUTTON_WIDTH, ty + TRADE_BUTTON_HEIGHT, 0xFF303030);
            } else {
                graphics.fill(tx, ty, tx + TRADE_BUTTON_WIDTH, ty + TRADE_BUTTON_HEIGHT, 0xFF1A1A1A);
            }

            // Trade info text
            String tradeText = offer.input.getCount() + "x " + offer.input.getHoverName().getString()
                    + " -> " + offer.output.getCount() + "x " + offer.output.getHoverName().getString();
            if (!canTrade) tradeText += " (" + Component.translatable("gui.reimagined.trade.exhausted").getString() + ")";
            graphics.drawString(font, tradeText, tx + 4, ty + 4, selected ? 0xFFFFFFFF : (canTrade ? 0xFFAAAAAA : 0xFF606060));
        }

        // Render selected trade details
        int detailX = leftPos + TRADE_LIST_X + TRADE_BUTTON_WIDTH + 10;
        int detailY = topPos + TRADE_LIST_Y;

        if (menu.getSelectedTradeIndex() >= 0 && menu.getSelectedTradeIndex() < offers.size()) {
            TradeOffer offer = offers.get(menu.getSelectedTradeIndex());

            // Input item
            graphics.renderFakeItem(offer.input.copy(), detailX, detailY);
            graphics.renderItemDecorations(font, offer.input, detailX, detailY, offer.input.getCount() + "x");

            // Arrow
            graphics.drawString(font, "->", detailX + 24, detailY + 4, 0xFFAAAAAA);

            // Output item
            graphics.renderFakeItem(offer.output.copy(), detailX + 36, detailY);
            graphics.renderItemDecorations(font, offer.output, detailX + 36, detailY, offer.output.getCount() + "x");

            // Uses
            String usesText = Component.translatable("gui.reimagined.trade.uses", offer.uses(), offer.maxUses).getString();
            graphics.drawString(font, usesText, detailX, detailY + 20, 0xFFAAAAAA);

            // Trade count selector
            graphics.drawString(font, Component.translatable("gui.reimagined.trade.count").getString(), detailX, detailY + 38, 0xFFAAAAAA);
            graphics.drawString(font, String.valueOf(menu.getCurrentTradeCount()), detailX + 70, detailY + 38, 0xFFFFFFFF);

            // Required input count
            String requiredText = Component.translatable("gui.reimagined.trade.required",
                    offer.input.getCount() * menu.getCurrentTradeCount()).getString();
            graphics.drawString(font, requiredText, detailX, detailY + 58, 0xFFAAAAAA);
        }

        // Render input/output slots
        graphics.blit(BACKGROUND, leftPos + 26, topPos + 30, 0, 166, 18, 18, 176, 166);
        graphics.blit(BACKGROUND, leftPos + 134, topPos + 30, 0, 166, 18, 18, 176, 166);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);

        List<TradeOffer> offers = menu.getOffers();
        for (int i = 0; i < MAX_VISIBLE_TRADES && scrollOffset + i < offers.size(); i++) {
            int index = scrollOffset + i;
            int tx = leftPos + TRADE_LIST_X;
            int ty = topPos + TRADE_LIST_Y + i * TRADE_BUTTON_HEIGHT;

            if (mouseX >= tx && mouseX < tx + TRADE_BUTTON_WIDTH
                    && mouseY >= ty && mouseY < ty + TRADE_BUTTON_HEIGHT) {
                TradeOffer offer = offers.get(index);
                graphics.renderTooltip(font,
                        Component.literal(offer.input.getCount() + "x " + offer.input.getHoverName().getString()
                                + " -> " + offer.output.getCount() + "x " + offer.output.getHoverName().getString()
                                + " (" + offer.uses() + "/" + offer.maxUses + " uses)"),
                        mouseX, mouseY);
                break;
            }
        }

        // Tooltip for input slot
        if (mouseX >= leftPos + 26 && mouseX < leftPos + 44
                && mouseY >= topPos + 30 && mouseY < topPos + 48) {
            if (!menu.getSlot(0).getItem().isEmpty()) {
                graphics.renderTooltip(font, menu.getSlot(0).getItem(), mouseX, mouseY);
            }
        }

        // Tooltip for output slot
        if (mouseX >= leftPos + 134 && mouseX < leftPos + 152
                && mouseY >= topPos + 30 && mouseY < topPos + 48) {
            if (!menu.getSlot(1).getItem().isEmpty()) {
                graphics.renderTooltip(font, menu.getSlot(1).getItem(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<TradeOffer> offers = menu.getOffers();
        if (offers.size() > MAX_VISIBLE_TRADES) {
            scrollOffset = Math.clamp(scrollOffset - (int) scrollY, 0, offers.size() - MAX_VISIBLE_TRADES);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<TradeOffer> offers = menu.getOffers();
        for (int i = 0; i < MAX_VISIBLE_TRADES && scrollOffset + i < offers.size(); i++) {
            int index = scrollOffset + i;
            int tx = leftPos + TRADE_LIST_X;
            int ty = topPos + TRADE_LIST_Y + i * TRADE_BUTTON_HEIGHT;

            if (mouseX >= tx && mouseX < tx + TRADE_BUTTON_WIDTH
                    && mouseY >= ty && mouseY < ty + TRADE_BUTTON_HEIGHT) {
                menu.selectTrade(index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
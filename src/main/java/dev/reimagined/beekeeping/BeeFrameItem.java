package dev.reimagined.beekeeping;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Hive upgrade item; the actual frame state lives on the vanilla hive block entity. */
public final class BeeFrameItem extends Item {
    public BeeFrameItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reimagined.bee_frame"));
    }
}

package dev.reimagined.content;

import dev.reimagined.research.JournalPages;
import dev.reimagined.research.ResearchEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;

/** A real readable book, rebuilt from the holder's own server-owned observations whenever opened. */
public final class ResearchJournal extends WrittenBookItem {
    public ResearchJournal(Properties properties) { super(properties); }
    @Override public Component getName(ItemStack stack) { return Component.translatable(getDescriptionId()); }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer reader) {
            ResearchEvents.observe(reader);
            stack.set(DataComponents.WRITTEN_BOOK_CONTENT, JournalPages.build(reader));
            reader.containerMenu.broadcastChanges();
            // Vanilla openItemGui checks Items.WRITTEN_BOOK identity. Its client packet handler,
            // however, reads WRITTEN_BOOK_CONTENT on any item. Sync the held slot before opening.
            int slot = hand == InteractionHand.MAIN_HAND ? reader.getInventory().selected : 40;
            reader.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, slot, stack.copy()));
            reader.connection.send(new ClientboundOpenBookPacket(hand));
            reader.awardStat(Stats.ITEM_USED.get(this));
            dev.reimagined.quest.QuestIntegration.mark(reader,"journal_read");
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

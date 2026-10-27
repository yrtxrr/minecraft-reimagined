package dev.reimagined.content;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A small in-world entry point; discoveries and a full journal GUI remain separate systems. */
public final class ResearchJournal extends Item {
    public ResearchJournal(Properties properties) { super(properties); }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            player.sendSystemMessage(Component.translatable("journal.reimagined.introduction"));
            player.sendSystemMessage(Component.translatable("journal.reimagined.controls"));
            player.sendSystemMessage(Component.translatable("journal.reimagined.magic"));
            player.sendSystemMessage(Component.translatable("message.reimagined.mana", SpellWand.mana(serverPlayer), ManaRules.CAPACITY));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}

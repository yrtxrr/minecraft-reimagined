package dev.reimagined.beekeeping;

import dev.reimagined.Reimagined;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Server-authoritative frame installation and frame recovery when a hive is broken. */
@EventBusSubscriber(modid = Reimagined.MOD_ID)
public final class BeekeepingEvents {
    private BeekeepingEvents() {}

    @SubscribeEvent
    public static void installFrame(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        ItemStack stack = event.getItemStack();
        if (!stack.is(BeekeepingContent.BEE_FRAME.get())) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.BEEHIVES)) return;
        if (!(level.getBlockEntity(pos) instanceof BeehiveBlockEntity hive)) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide) return;

        Player player = event.getEntity();
        int frames = BeeFrames.get(hive);
        if (frames >= BeeFrames.MAX_FRAMES) {
            player.displayClientMessage(Component.translatable("message.reimagined.bee_frames_full"), true);
            return;
        }
        BeeFrames.install(hive);
        stack.shrink(1);
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.9F, 0.8F);
        player.displayClientMessage(Component.translatable("message.reimagined.bee_frames_installed",
                frames + 1, BeeFrames.MAX_FRAMES), true);
    }

    /**
     * Runs late so a break cancelled by another listener does not lose the installed frames.
     * Explosions and other non-player removal are outside this event; their frames are lost,
     * matching the fact that vanilla hives release their colony in those cases too.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void returnFrames(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) return;
        BlockEntity hive = level.getBlockEntity(event.getPos());
        int frames = BeeFrames.takeAll(hive);
        if (frames > 0) Block.popResource(level, event.getPos(), new ItemStack(BeekeepingContent.BEE_FRAME.get(), frames));
    }
}

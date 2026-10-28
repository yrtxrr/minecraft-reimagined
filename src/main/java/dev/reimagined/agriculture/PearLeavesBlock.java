package dev.reimagined.agriculture;

import com.mojang.serialization.MapCodec;
import dev.reimagined.season.CalendarData;
import dev.reimagined.season.SeasonCalendar;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Pear foliage. Only the inner canopy (distance 1–2) does seasonal fruit work, so a forest of
 * leaves does not turn into thousands of extra random-tick tasks. Ripe leaves stop ticking until
 * a player harvests them. Decay remains ordinary vanilla {@link LeavesBlock} behaviour.
 */
public final class PearLeavesBlock extends LeavesBlock {
    public static final MapCodec<PearLeavesBlock> CODEC = simpleCodec(PearLeavesBlock::new);
    public static final BooleanProperty FRUIT = BooleanProperty.create("fruit");
    private static final int RIPEN_CHANCE = 12;
    private static final int FRUIT_BEARING_DISTANCE = 2;

    public PearLeavesBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FRUIT, false));
    }

    @Override public MapCodec<PearLeavesBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FRUIT);
    }

    @Override protected boolean isRandomlyTicking(BlockState state) {
        return super.isRandomlyTicking(state)
                || (!state.getValue(FRUIT) && state.getValue(DISTANCE) <= FRUIT_BEARING_DISTANCE);
    }

    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (state.getValue(FRUIT) || state.getValue(DISTANCE) > FRUIT_BEARING_DISTANCE) return;
        if (!canRipen(level) || random.nextInt(RIPEN_CHANCE) != 0) return;
        BlockState current = level.getBlockState(pos);
        if (current.is(this) && !current.getValue(FRUIT)) {
            level.setBlock(pos, current.setValue(FRUIT, true), Block.UPDATE_CLIENTS);
        }
    }

    /** Fruit follows the existing calendar: summer and autumn in the overworld; other dimensions are always mild. */
    public static boolean canRipen(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return true;
        long day = CalendarData.get(level).day(level);
        SeasonCalendar.Season season = SeasonCalendar.season(day);
        return season == SeasonCalendar.Season.SUMMER || season == SeasonCalendar.Season.AUTUMN;
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(FRUIT)) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        return harvest(level, pos, state, player)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!state.getValue(FRUIT)) return super.useWithoutItem(state, level, pos, player, hit);
        return harvest(level, pos, state, player)
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
    }

    /**
     * Server-owned harvest used by both interaction paths. Returns true when the block held ripe fruit,
     * including the client prediction side; the authoritative inventory/block change happens on the server.
     */
    public static boolean harvest(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        if (!state.is(OrchardContent.PEAR_LEAVES.get()) || !state.getValue(FRUIT)) return false;
        if (!(level instanceof ServerLevel server)) return true;
        int count = 1 + server.random.nextInt(2);
        Block.popResource(server, pos, new ItemStack(OrchardContent.PEAR.get(), count));
        server.setBlock(pos, state.setValue(FRUIT, false), Block.UPDATE_CLIENTS);
        server.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS,
                1.0F, 0.9F + server.random.nextFloat() * 0.2F);
        return true;
    }
}

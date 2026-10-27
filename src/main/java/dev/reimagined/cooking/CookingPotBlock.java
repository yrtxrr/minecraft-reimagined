package dev.reimagined.cooking;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class CookingPotBlock extends BaseEntityBlock {
    public static final MapCodec<CookingPotBlock> CODEC = simpleCodec(CookingPotBlock::new);
    public static final BooleanProperty COOKING = BooleanProperty.create("cooking");
    private static final VoxelShape SHAPE = Block.box(2,0,2,14,10,14);
    public CookingPotBlock(BlockBehaviour.Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(COOKING,false)); }
    private static BlockEntity loadedPot(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return null;
        var chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockEntity(pos);
    }
    @Override protected net.minecraft.world.MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return loadedPot(level,pos) instanceof CookingPotBlockEntity pot ? pot : null;
    }
    @Override protected MapCodec<CookingPotBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CookingPotBlockEntity(pos,state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(COOKING); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && loadedPot(level,pos) instanceof CookingPotBlockEntity pot) player.openMenu(pot);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!level.isClientSide && loadedPot(level,pos) instanceof CookingPotBlockEntity pot) pot.wake();
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (loadedPot(level,pos) instanceof CookingPotBlockEntity pot) pot.cookStep();
    }
    @Override protected void onRemove(BlockState old, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!old.is(next.getBlock())) {
            if (loadedPot(level,pos) instanceof CookingPotBlockEntity pot) Containers.dropContents(level,pos,pot);
            super.onRemove(old,level,pos,next,moving); level.updateNeighbourForOutputSignal(pos,this);
        }
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) { return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(loadedPot(level,pos)); }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(COOKING) && random.nextInt(4) == 0)
            level.addParticle(ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+.75,pos.getZ()+.5,0,.025,0);
    }
}

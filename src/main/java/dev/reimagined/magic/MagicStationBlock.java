package dev.reimagined.magic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class MagicStationBlock extends BaseEntityBlock {
    public final boolean research;
    private static final VoxelShape SHAPE=Block.box(0,0,0,16,14,16);
    public MagicStationBlock(BlockBehaviour.Properties properties,boolean research) {super(properties);this.research=research;}
    @Override protected MapCodec<MagicStationBlock> codec() { return simpleCodec(properties->new MagicStationBlock(properties,research)); }
    public static MagicStationBlockEntity loaded(Level level,BlockPos pos) {
        if(!(level instanceof ServerLevel server))return null;
        var chunk=server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return chunk!=null && chunk.getBlockEntity(pos) instanceof MagicStationBlockEntity station?station:null;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new MagicStationBlockEntity(pos,state);}
    @Override protected RenderShape getRenderShape(BlockState state) {return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {return SHAPE;}
    @Override protected MenuProvider getMenuProvider(BlockState state,Level level,BlockPos pos) {return loaded(level,pos);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        var station=loaded(level,pos);if(station!=null)player.openMenu(station);return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock())) {
            var station=loaded(level,pos);if(station!=null)Containers.dropContents(level,pos,station);
            super.onRemove(state,level,pos,next,moving);level.updateNeighbourForOutputSignal(pos,this);
        }
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) {return true;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos) {return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(loaded(level,pos));}
}

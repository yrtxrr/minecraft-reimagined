package dev.reimagined.smithing;

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

public final class ForgeBlock extends BaseEntityBlock {
    public static final MapCodec<ForgeBlock> CODEC=simpleCodec(ForgeBlock::new);
    public ForgeBlock(BlockBehaviour.Properties properties){super(properties);}
    @Override protected MapCodec<ForgeBlock> codec(){return CODEC;}
    public static ForgeBlockEntity loaded(Level level,BlockPos pos){
        if(!(level instanceof ServerLevel server))return null;
        var chunk=server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return chunk!=null && chunk.getBlockEntity(pos) instanceof ForgeBlockEntity forge?forge:null;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ForgeBlockEntity(pos,state);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override protected MenuProvider getMenuProvider(BlockState state,Level level,BlockPos pos){return loaded(level,pos);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){var forge=loaded(level,pos);if(forge!=null)player.openMenu(forge);return InteractionResult.sidedSuccess(level.isClientSide);}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())){var forge=loaded(level,pos);if(forge!=null)Containers.dropContents(level,pos,forge);super.onRemove(state,level,pos,next,moving);level.updateNeighbourForOutputSignal(pos,this);}
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(loaded(level,pos));}
}

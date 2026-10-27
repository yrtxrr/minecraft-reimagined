package dev.reimagined.engineering;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class MechanicalBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private final Kind kind;
    public enum Kind { WATER_WHEEL, SHAFT, MILLSTONE }
    public MechanicalBlock(Properties properties, Kind kind) {
        super(properties); this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(ACTIVE, false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new MechanicalBlock(properties, kind));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AXIS, ACTIVE); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        if (kind == Kind.SHAFT) return switch (state.getValue(AXIS)) {
            case X -> Block.box(0, 6, 6, 16, 10, 10);
            case Y -> Block.box(6, 0, 6, 10, 16, 10);
            case Z -> Block.box(6, 6, 0, 10, 10, 16);
        };
        return super.getShape(state, level, pos, context);
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return kind == Kind.SHAFT ? null : new MachineBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, EngineeringContent.MACHINE.get(), MachineBlockEntity::tick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine && kind == Kind.MILLSTONE)
                machine.interact(player, stack);
            Rotation network = MechanicalNetwork.inspect(level, pos);
            player.displayClientMessage(Component.translatable("message.reimagined.rotation", network.speed(), network.load(), network.capacity()), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level,
            BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine && kind == Kind.MILLSTONE)
                machine.interact(player, ItemStack.EMPTY);
            Rotation network = MechanicalNetwork.inspect(level, pos);
            player.displayClientMessage(Component.translatable("message.reimagined.rotation", network.speed(), network.load(), network.capacity()), true);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) machine.dropContents();
        super.onRemove(state, level, pos, replacement, moving);
    }
}

package com.tfcelectriccooking.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.blockentity.ElectricOvenBlockEntity;

public class ElectricOvenBlock extends Block implements EntityBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    private static final VoxelShape SHAPE = Block.box(1, 1, 1, 15, 15, 15);

    public ElectricOvenBlock()
    {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.5f)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .lightLevel(state -> state.getValue(POWERED) ? 13 : 0));
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new ElectricOvenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        if (level.isClientSide()) return null;
        return type == ModBlocks.ELECTRIC_OVEN_BLOCK_ENTITY.get()
            ? (lvl, pos, st, be) -> ((ElectricOvenBlockEntity) be).serverTick()
            : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (level.getBlockEntity(pos) instanceof ElectricOvenBlockEntity oven)
        {
            if (level.isClientSide())
            {
                return ItemInteractionResult.sidedSuccess(true);
            }

            if (player instanceof ServerPlayer serverPlayer)
            {
                serverPlayer.openMenu(oven, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        if (!state.is(newState.getBlock()))
        {
            if (level.getBlockEntity(pos) instanceof InventoryBlockEntity<?> inv)
            {
                inv.ejectInventory();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

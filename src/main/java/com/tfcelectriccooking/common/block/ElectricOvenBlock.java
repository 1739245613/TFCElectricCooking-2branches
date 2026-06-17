package com.tfcelectriccooking.common.block;

import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.ModSounds;
import com.tfcelectriccooking.common.blockentity.ElectricOvenBlockEntity;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

public class ElectricOvenBlock extends Block implements EntityBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
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
            .setValue(POWERED, false)
            .setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, POWERED, OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new ElectricOvenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        if (level.isClientSide())
        {
            return null;
        }
        return type == ModBlocks.ELECTRIC_OVEN_BLOCK_ENTITY.get()
            ? (lvl, pos, blockState, be) -> ((ElectricOvenBlockEntity) be).serverTick()
            : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (level.getBlockEntity(pos) instanceof ElectricOvenBlockEntity oven)
        {
            if (level.isClientSide())
            {
                return InteractionResult.SUCCESS;
            }

            if (player.isShiftKeyDown())
            {
                final boolean open = !state.getValue(OPEN);
                setOpen(level, pos, state, open);
                player.displayClientMessage(Component.translatable(open ? "tfcelectriccooking.message.container_open" : "tfcelectriccooking.message.container_closed"), true);
                return InteractionResult.SUCCESS;
            }

            if (player instanceof ServerPlayer serverPlayer)
            {
                setOpen(level, pos, state, true);
                Helpers.openScreen(serverPlayer, oven, pos);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static void setOpen(Level level, BlockPos pos, BlockState state, boolean open)
    {
        BlockState currentState = level.getBlockState(pos);
        if (!currentState.is(state.getBlock()))
        {
            currentState = state;
        }

        if (currentState.hasProperty(OPEN) && currentState.getValue(OPEN) != open)
        {
            level.setBlockAndUpdate(pos, currentState.setValue(OPEN, open));
            Helpers.playSound(level, pos, open ? ModSounds.ELECTRIC_OVEN_OPEN.get() : ModSounds.ELECTRIC_OVEN_CLOSE.get());
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof InventoryBlockEntity<?> inv)
        {
            inv.ejectInventory();
            inv.invalidateCapabilities();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}

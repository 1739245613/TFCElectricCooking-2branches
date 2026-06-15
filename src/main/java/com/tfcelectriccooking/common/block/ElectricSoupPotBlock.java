package com.tfcelectriccooking.common.block;

import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;
import net.dries007.tfc.client.particle.TFCParticles;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

public class ElectricSoupPotBlock extends Block implements EntityBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

    public ElectricSoupPotBlock()
    {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.5f)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .lightLevel(state -> state.getValue(POWERED) ? 10 : 0));
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
        return new ElectricSoupPotBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        if (level.isClientSide())
        {
            return null;
        }
        return type == ModBlocks.ELECTRIC_SOUP_POT_BLOCK_ENTITY.get()
            ? (lvl, pos, blockState, be) -> ((ElectricSoupPotBlockEntity) be).serverTick()
            : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random)
    {
        if (level.getBlockEntity(pos) instanceof ElectricSoupPotBlockEntity pot && pot.shouldRenderAsBoiling())
        {
            final double x = pos.getX() + 0.5;
            final double y = pos.getY();
            final double z = pos.getZ() + 0.5;
            for (int i = 0; i < random.nextInt(5) + 4; i++)
            {
                level.addParticle(TFCParticles.BUBBLE.get(), false, x + random.nextFloat() * 0.375 - 0.1875, y + 0.625, z + random.nextFloat() * 0.375 - 0.1875, 0, 0.05D, 0);
            }
            level.addParticle(TFCParticles.STEAM.get(), false, x, y + 0.8, z, Helpers.triangle(random), 0.5, Helpers.triangle(random));
            level.playLocalSound(x, y, z, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.7F + 0.4F, false);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (!(level.getBlockEntity(pos) instanceof ElectricSoupPotBlockEntity pot))
        {
            return InteractionResult.PASS;
        }

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

        final ItemStack held = player.getItemInHand(hand);
        final InteractionResult outputResult = pot.interactWithOutput(player, held);
        if (outputResult != InteractionResult.PASS)
        {
            return outputResult;
        }

        if (pot.handleFluidInteraction(player, hand, held))
        {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer)
        {
            setOpen(level, pos, state, true);
            Helpers.openScreen(serverPlayer, pot, pos);
        }
        return InteractionResult.SUCCESS;
    }

    public static void setOpen(Level level, BlockPos pos, BlockState state, boolean open)
    {
        if (state.hasProperty(OPEN) && state.getValue(OPEN) != open)
        {
            level.setBlockAndUpdate(pos, state.setValue(OPEN, open));
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

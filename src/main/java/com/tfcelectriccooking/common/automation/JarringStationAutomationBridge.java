package com.tfcelectriccooking.common.automation;

import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;
import com.tfcelectriccooking.common.compat.JamJarCompat;
import java.lang.reflect.Field;
import net.dries007.tfc.common.blockentities.TFCBlockEntity;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public final class JarringStationAutomationBridge
{
    private static final int SLOTS = 9;
    private static final int POUR_ANIMATION_TICKS = 45;
    private static final @Nullable Field INVENTORY_FIELD = findInventoryField();
    private static final @Nullable Field POUR_TICKS_FIELD = findJarringStationField("pourTicks");

    private JarringStationAutomationBridge() {}

    public static void tryFillAroundPot(Level level, BlockPos potPos)
    {
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            final BlockPos stationPos = potPos.relative(direction);
            final BlockState stationState = level.getBlockState(stationPos);
            final BlockEntity station = level.getBlockEntity(stationPos);
            if (isJarringStation(station) && stationFacesPot(stationState, stationPos, potPos))
            {
                tryFillFromStation(level, stationPos, stationState, station);
            }
        }
    }

    public static boolean tryFillFromStation(BlockEntity station)
    {
        final Level level = station.getLevel();
        return level != null && tryFillFromStation(level, station.getBlockPos(), station.getBlockState(), station);
    }

    public static void syncStation(BlockEntity station)
    {
        final Level level = station.getLevel();
        if (level != null)
        {
            syncStation(level, station.getBlockPos(), station.getBlockState(), station, false);
        }
    }

    public static boolean tryFillFromStation(Level level, BlockPos pos, BlockState state, Object station)
    {
        if (level.isClientSide || !state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
        {
            return false;
        }

        final Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (!(level.getBlockEntity(pos.relative(facing)) instanceof ElectricSoupPotBlockEntity pot) || !pot.hasOutput())
        {
            return false;
        }

        final IItemHandlerModifiable inventory = getInventory(station);
        if (inventory == null)
        {
            return false;
        }

        boolean changed = false;
        for (int slot = 0; slot < SLOTS && pot.hasOutput(); slot++)
        {
            final ItemStack jar = inventory.getStackInSlot(slot);
            if (!JamJarCompat.isSupportedEmptyJar(jar))
            {
                continue;
            }

            final ItemStack jarred = pot.tryTakeOutputWithJar(jar);
            if (!jarred.isEmpty())
            {
                inventory.setStackInSlot(slot, jarred);
                changed = true;
            }
        }

        if (changed)
        {
            Helpers.playSound(level, pos, SoundEvents.BOTTLE_FILL);
            syncStation(level, pos, state, station, true);
        }
        return changed;
    }

    private static void syncStation(Level level, BlockPos pos, BlockState state, Object station, boolean animate)
    {
        if (level.isClientSide)
        {
            return;
        }
        if (animate)
        {
            setPourTicks(station);
        }
        if (station instanceof TFCBlockEntity blockEntity)
        {
            blockEntity.sendVanillaUpdatePacket();
            blockEntity.setChanged();
        }
        else if (station instanceof BlockEntity blockEntity)
        {
            blockEntity.setChanged();
        }
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    private static boolean stationFacesPot(BlockState state, BlockPos stationPos, BlockPos potPos)
    {
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
            && stationPos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING)).equals(potPos);
    }

    private static boolean isJarringStation(@Nullable Object station)
    {
        return station != null && station.getClass().getName().equals("com.eerussianguy.firmalife.common.blockentities.JarringStationBlockEntity");
    }

    private static @Nullable IItemHandlerModifiable getInventory(Object station)
    {
        if (INVENTORY_FIELD != null)
        {
            try
            {
                final Object value = INVENTORY_FIELD.get(station);
                if (value instanceof IItemHandlerModifiable handler)
                {
                    return handler;
                }
            }
            catch (IllegalAccessException ignored)
            {
            }
        }
        return null;
    }

    private static @Nullable Field findInventoryField()
    {
        Class<?> type;
        try
        {
            type = Class.forName("net.dries007.tfc.common.blockentities.InventoryBlockEntity");
        }
        catch (ClassNotFoundException ignored)
        {
            return null;
        }

        try
        {
            final Field field = type.getDeclaredField("inventory");
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException ignored)
        {
            return null;
        }
    }

    private static @Nullable Field findJarringStationField(String name)
    {
        try
        {
            final Field field = Class.forName("com.eerussianguy.firmalife.common.blockentities.JarringStationBlockEntity").getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException ignored)
        {
            return null;
        }
    }

    private static void setPourTicks(Object station)
    {
        if (POUR_TICKS_FIELD != null)
        {
            try
            {
                POUR_TICKS_FIELD.setInt(station, POUR_ANIMATION_TICKS);
            }
            catch (IllegalAccessException ignored)
            {
            }
        }
    }
}

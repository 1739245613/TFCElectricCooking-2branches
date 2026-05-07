package com.tfcelectriccooking.common.automation;

import com.eerussianguy.firmalife.common.blockentities.JarringStationBlockEntity;
import com.eerussianguy.firmalife.common.blocks.JarringStationBlock;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;
import com.tfcelectriccooking.common.compat.JamJarCompat;
import java.lang.reflect.Field;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public final class JarringStationAutomationBridge
{
    private static final int SLOTS = JarringStationBlockEntity.SLOTS;
    private static final int POUR_ANIMATION_TICKS = 45;
    private static final @Nullable Field POUR_TICKS_FIELD = findJarringStationField("pourTicks");

    private JarringStationAutomationBridge() {}

    public static void tryFillAroundPot(Level level, BlockPos potPos)
    {
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            final BlockPos stationPos = potPos.relative(direction);
            final BlockState stationState = level.getBlockState(stationPos);
            final BlockEntity station = level.getBlockEntity(stationPos);
            if (station instanceof JarringStationBlockEntity jarringStation && stationFacesPot(stationState, stationPos, potPos))
            {
                tryFillFromStation(level, stationPos, stationState, jarringStation);
            }
        }
    }

    public static boolean tryFillFromStation(BlockEntity station)
    {
        final Level level = station.getLevel();
        return station instanceof JarringStationBlockEntity jarringStation
            && level != null
            && tryFillFromStation(level, station.getBlockPos(), station.getBlockState(), jarringStation);
    }

    public static void syncStation(BlockEntity station)
    {
        final Level level = station.getLevel();
        if (level != null && station instanceof JarringStationBlockEntity jarringStation)
        {
            syncStation(level, station.getBlockPos(), station.getBlockState(), jarringStation, false);
        }
    }

    public static boolean tryFillFromStation(Level level, BlockPos pos, BlockState state, JarringStationBlockEntity station)
    {
        if (level.isClientSide || !state.hasProperty(JarringStationBlock.FACING))
        {
            return false;
        }

        final Direction facing = state.getValue(JarringStationBlock.FACING);
        if (!(level.getBlockEntity(pos.relative(facing)) instanceof ElectricSoupPotBlockEntity pot) || !pot.hasOutput())
        {
            return false;
        }

        final IItemHandlerModifiable inventory = station.getInventory();
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

    private static void syncStation(Level level, BlockPos pos, BlockState state, JarringStationBlockEntity station, boolean animate)
    {
        if (level.isClientSide)
        {
            return;
        }
        if (animate)
        {
            setPourTicks(station);
        }
        station.requestModelDataUpdate();
        station.markForSync();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    private static boolean stationFacesPot(BlockState state, BlockPos stationPos, BlockPos potPos)
    {
        return state.hasProperty(JarringStationBlock.FACING)
            && stationPos.relative(state.getValue(JarringStationBlock.FACING)).equals(potPos);
    }

    private static @Nullable Field findJarringStationField(String name)
    {
        try
        {
            final Field field = JarringStationBlockEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException ignored)
        {
            return null;
        }
    }

    private static void setPourTicks(JarringStationBlockEntity station)
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

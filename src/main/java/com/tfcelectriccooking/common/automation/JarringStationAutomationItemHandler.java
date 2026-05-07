package com.tfcelectriccooking.common.automation;

import com.tfcelectriccooking.common.ModConfig;
import com.tfcelectriccooking.common.compat.JamJarCompat;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public final class JarringStationAutomationItemHandler implements IItemHandlerModifiable
{
    private final BlockEntity station;
    private final IItemHandlerModifiable delegate;

    public JarringStationAutomationItemHandler(BlockEntity station, IItemHandlerModifiable delegate)
    {
        this.station = station;
        this.delegate = delegate;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack)
    {
        if (stack.isEmpty())
        {
            delegate.setStackInSlot(slot, stack);
            notifyJarringStation();
        }
        else if (JamJarCompat.isSupportedEmptyJar(stack))
        {
            delegate.setStackInSlot(slot, stack.copyWithCount(Math.min(stack.getCount(), Math.min(getSlotLimit(slot), stack.getMaxStackSize()))));
            notifyJarringStation();
        }
        else if (canInsert(slot, stack))
        {
            delegate.setStackInSlot(slot, stack);
            notifyJarringStation();
        }
    }

    @Override
    public int getSlots()
    {
        return delegate.getSlots();
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return delegate.getStackInSlot(slot);
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        final ItemStack remainder = canInsert(slot, stack)
            ? JamJarCompat.isSupportedEmptyJar(stack) ? insertKnownJar(slot, stack, simulate) : delegate.insertItem(slot, stack, simulate)
            : stack;
        if (!simulate && remainder.getCount() != stack.getCount())
        {
            notifyJarringStation();
        }
        return remainder;
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        final ItemStack stack = delegate.getStackInSlot(slot);
        final ItemStack extracted = canExtract(slot, stack) ? delegate.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        if (!simulate && !extracted.isEmpty())
        {
            notifyJarringStation();
        }
        return extracted;
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return JamJarCompat.isSupportedEmptyJar(stack) || (canInsert(slot, stack) && delegate.isItemValid(slot, stack));
    }

    private boolean canInsert(int slot, ItemStack stack)
    {
        return !stack.isEmpty()
            && (JamJarCompat.isSupportedEmptyJar(stack) || matches(stack, ModConfig.JARRING_STATION_INSERT_WHITELIST.get()));
    }

    private boolean canExtract(int slot, ItemStack stack)
    {
        return !stack.isEmpty()
            && !JamJarCompat.isSupportedEmptyJar(stack)
            && !matches(stack, ModConfig.JARRING_STATION_INSERT_WHITELIST.get())
            && !matches(stack, ModConfig.JARRING_STATION_EXTRACT_BLACKLIST.get());
    }

    private ItemStack insertKnownJar(int slot, ItemStack stack, boolean simulate)
    {
        final ItemStack existing = delegate.getStackInSlot(slot);
        if (!existing.isEmpty())
        {
            return stack;
        }

        final int inserted = Math.min(stack.getCount(), Math.min(getSlotLimit(slot), stack.getMaxStackSize()));
        if (inserted <= 0)
        {
            return stack;
        }

        if (!simulate)
        {
            delegate.setStackInSlot(slot, stack.copyWithCount(inserted));
        }

        final ItemStack remainder = stack.copy();
        remainder.shrink(inserted);
        return remainder;
    }

    private static boolean matches(ItemStack stack, List<? extends String> entries)
    {
        for (String rawEntry : entries)
        {
            final String entry = rawEntry.trim();
            if (entry.isEmpty())
            {
                continue;
            }
            if (entry.charAt(0) == '#')
            {
                final ResourceLocation id = ResourceLocation.tryParse(entry.substring(1));
                if (id != null && stack.is(TagKey.create(Registries.ITEM, id)))
                {
                    return true;
                }
            }
            else
            {
                final ResourceLocation id = ResourceLocation.tryParse(entry);
                final Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
                if (item != null && stack.is(item))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void notifyJarringStation()
    {
        if (!JarringStationAutomationBridge.tryFillFromStation(station))
        {
            JarringStationAutomationBridge.syncStation(station);
        }
    }
}

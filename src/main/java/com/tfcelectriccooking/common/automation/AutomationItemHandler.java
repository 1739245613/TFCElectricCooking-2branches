package com.tfcelectriccooking.common.automation;

import java.util.function.BiPredicate;
import java.util.function.IntPredicate;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

public final class AutomationItemHandler implements IItemHandlerModifiable
{
    private final IItemHandlerModifiable delegate;
    private final BiPredicate<Integer, ItemStack> canInsert;
    private final IntPredicate canExtract;
    private final Runnable onChanged;

    public AutomationItemHandler(IItemHandlerModifiable delegate, BiPredicate<Integer, ItemStack> canInsert, IntPredicate canExtract)
    {
        this(delegate, canInsert, canExtract, () -> {});
    }

    public AutomationItemHandler(IItemHandlerModifiable delegate, BiPredicate<Integer, ItemStack> canInsert, IntPredicate canExtract, Runnable onChanged)
    {
        this.delegate = delegate;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
        this.onChanged = onChanged;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack)
    {
        if (stack.isEmpty() || canInsert.test(slot, stack))
        {
            final ItemStack before = delegate.getStackInSlot(slot).copy();
            delegate.setStackInSlot(slot, stack);
            if (!ItemStack.matches(before, delegate.getStackInSlot(slot)))
            {
                onChanged.run();
            }
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
        final ItemStack remainder = stack.isEmpty() || canInsert.test(slot, stack) ? delegate.insertItem(slot, stack, simulate) : stack;
        if (!simulate && remainder.getCount() != stack.getCount())
        {
            onChanged.run();
        }
        return remainder;
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        final ItemStack extracted = canExtract.test(slot) ? delegate.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        if (!simulate && !extracted.isEmpty())
        {
            onChanged.run();
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
        return canInsert.test(slot, stack) && delegate.isItemValid(slot, stack);
    }
}

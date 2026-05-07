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

    public AutomationItemHandler(IItemHandlerModifiable delegate, BiPredicate<Integer, ItemStack> canInsert, IntPredicate canExtract)
    {
        this.delegate = delegate;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack)
    {
        delegate.setStackInSlot(slot, stack);
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
        return stack.isEmpty() || canInsert.test(slot, stack) ? delegate.insertItem(slot, stack, simulate) : stack;
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return canExtract.test(slot) ? delegate.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
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

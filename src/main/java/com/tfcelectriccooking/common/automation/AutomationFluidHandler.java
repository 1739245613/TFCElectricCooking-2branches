package com.tfcelectriccooking.common.automation;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public final class AutomationFluidHandler implements IFluidHandler
{
    private final IFluidHandler delegate;
    private final Predicate<FluidStack> canFill;
    private final BooleanSupplier canDrain;

    public AutomationFluidHandler(IFluidHandler delegate, Predicate<FluidStack> canFill, BooleanSupplier canDrain)
    {
        this.delegate = delegate;
        this.canFill = canFill;
        this.canDrain = canDrain;
    }

    @Override
    public int getTanks()
    {
        return delegate.getTanks();
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank)
    {
        return delegate.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank)
    {
        return delegate.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack)
    {
        return canFill.test(stack) && delegate.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action)
    {
        return canFill.test(resource) ? delegate.fill(resource, action) : 0;
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action)
    {
        return canDrain.getAsBoolean() ? delegate.drain(resource, action) : FluidStack.EMPTY;
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action)
    {
        return canDrain.getAsBoolean() ? delegate.drain(maxDrain, action) : FluidStack.EMPTY;
    }
}

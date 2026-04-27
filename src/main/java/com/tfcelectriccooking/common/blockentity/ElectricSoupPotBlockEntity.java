package com.tfcelectriccooking.common.blockentity;

import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.container.ElectricSoupPotContainer;
import java.lang.reflect.Field;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.blockentities.PotBlockEntity;
import net.dries007.tfc.common.blockentities.TickableInventoryBlockEntity;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.common.capabilities.DelegateFluidHandler;
import net.dries007.tfc.common.capabilities.DelegateItemHandler;
import net.dries007.tfc.common.capabilities.InventoryItemHandler;
import net.dries007.tfc.common.capabilities.PartialFluidHandler;
import net.dries007.tfc.common.capabilities.PartialItemHandler;
import net.dries007.tfc.common.capabilities.SidedHandler;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.recipes.PotRecipe;
import net.dries007.tfc.common.recipes.RecipeHelpers;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.common.recipes.inventory.EmptyInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ElectricSoupPotBlockEntity extends TickableInventoryBlockEntity<ElectricSoupPotBlockEntity.PotInventory>
{
    public static final int SLOT_EXTRA_INPUT_START = 4;
    public static final int SLOT_EXTRA_INPUT_END = 8;
    public static final int INPUT_SLOT_COUNT = 5;
    public static final int INTERNAL_SLOT_COUNT = 9;
    public static final int PRE_BOIL_TIME = 100;
    public static final int SPEED_MULTIPLIER = 4;
    public static final int ENERGY_CAPACITY = 16000;
    public static final int ENERGY_MAX_IO = 256;
    public static final int ENERGY_PER_TICK = 20;

    private static final @Nullable Field POT_RECIPE_TEMPERATURE_FIELD = findPotRecipeTemperatureField();

    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_MAX_IO, ENERGY_MAX_IO, 0)
    {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate)
        {
            final int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate)
            {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate)
        {
            final int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate)
            {
                setChanged();
            }
            return extracted;
        }
    };
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);
    private final SidedHandler.Builder<IFluidHandler> sidedFluidInventory;
    private final RecipeProxyPotBlockEntity recipeProxy;

    private @Nullable PotRecipe.Output output;
    private @Nullable PotRecipe cachedRecipe;
    private int boilingTicks;
    private int preBoilingTicks;
    private float temperature;
    private int targetTemperature;
    private boolean needsRecipeUpdate = true;
    private int lastRecipeTemperature;
    private int syncedUiProgress;
    private int syncedUiProgressTotal;
    private int syncedUiHasOutput;
    private int syncedUiRecipeTemperature;

    private final ContainerData syncData = new ContainerData()
    {
        @Override
        public int get(int index)
        {
            final boolean clientSide = getLevel() != null && getLevel().isClientSide;
            return switch (index) {
                case 0 -> (int) temperature;
                case 1 -> targetTemperature;
                case 2 -> energyStorage.getEnergyStored();
                case 3 -> clientSide ? syncedUiProgress : getUiProgress();
                case 4 -> clientSide ? syncedUiProgressTotal : getUiProgressTotal();
                case 5 -> clientSide ? syncedUiHasOutput : (hasOutput() ? 1 : 0);
                case 6 -> clientSide ? syncedUiRecipeTemperature : getUiRecipeTemperature();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value)
        {
            switch (index)
            {
                case 0 -> temperature = value;
                case 1 -> targetTemperature = value;
                case 3 -> syncedUiProgress = value;
                case 4 -> syncedUiProgressTotal = value;
                case 5 -> syncedUiHasOutput = value;
                case 6 -> syncedUiRecipeTemperature = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount()
        {
            return 7;
        }
    };

    public ElectricSoupPotBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlocks.ELECTRIC_SOUP_POT_BLOCK_ENTITY.get(), pos, state,
            PotInventory::new,
            Component.translatable("block.tfcelectriccooking.electric_soup_pot"));

        sidedInventory
            .on(new PartialItemHandler(inventory).insert(SLOT_EXTRA_INPUT_START, SLOT_EXTRA_INPUT_START + 1, SLOT_EXTRA_INPUT_START + 2, SLOT_EXTRA_INPUT_START + 3, SLOT_EXTRA_INPUT_END).extract(SLOT_EXTRA_INPUT_START, SLOT_EXTRA_INPUT_START + 1, SLOT_EXTRA_INPUT_START + 2, SLOT_EXTRA_INPUT_START + 3, SLOT_EXTRA_INPUT_END), Direction.Plane.HORIZONTAL)
            .on(new PartialItemHandler(inventory).insert(SLOT_EXTRA_INPUT_START, SLOT_EXTRA_INPUT_START + 1, SLOT_EXTRA_INPUT_START + 2, SLOT_EXTRA_INPUT_START + 3, SLOT_EXTRA_INPUT_END), Direction.UP);

        sidedFluidInventory = new SidedHandler.Builder<>(inventory);
        sidedFluidInventory
            .on(new PartialFluidHandler(inventory).insert(), Direction.UP)
            .on(new PartialFluidHandler(inventory).extract(), Direction.Plane.HORIZONTAL);

        recipeProxy = new RecipeProxyPotBlockEntity(pos, state);
    }

    public void serverTick()
    {
        checkForLastTickSync();
        final Level level = getLevel();
        if (level == null)
        {
            return;
        }

        recipeProxy.setLevel(level);

        if (needsRecipeUpdate)
        {
            needsRecipeUpdate = false;
            updateCachedRecipe();
        }

        handleEnergy();
        handleTemperature();
        handleCooking();
    }

    private void handleEnergy()
    {
        final Level level = getLevel();
        if (level == null)
        {
            return;
        }

        final boolean wasPowered = getBlockState().getValue(com.tfcelectriccooking.common.block.ElectricSoupPotBlock.POWERED);
        final boolean shouldPower = targetTemperature > 0 && energyStorage.getEnergyStored() >= ENERGY_PER_TICK;

        if (shouldPower)
        {
            energyStorage.extractEnergy(ENERGY_PER_TICK, false);
        }

        if (wasPowered != shouldPower)
        {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(com.tfcelectriccooking.common.block.ElectricSoupPotBlock.POWERED, shouldPower));
            markForSync();
        }
    }

    private void handleTemperature()
    {
        final boolean powered = getBlockState().getValue(com.tfcelectriccooking.common.block.ElectricSoupPotBlock.POWERED);
        if (powered && targetTemperature > 0)
        {
            if (temperature < targetTemperature)
            {
                temperature = Math.min(temperature + 3f, targetTemperature);
            }
            else if (temperature > targetTemperature)
            {
                temperature = Math.max(temperature - 3f, targetTemperature);
            }
        }
        else if (temperature > 0)
        {
            temperature = Math.max(temperature - 1.5f, 0f);
        }
    }

    private void handleCooking()
    {
        if (output != null)
        {
            return;
        }

        if (cachedRecipe != null && cachedRecipe.isHotEnough(temperature))
        {
            if (preBoilingTicks < PRE_BOIL_TIME)
            {
                preBoilingTicks++;
                if (preBoilingTicks == PRE_BOIL_TIME)
                {
                    markForSync();
                }
                return;
            }

            boilingTicks += SPEED_MULTIPLIER;
            if (boilingTicks == SPEED_MULTIPLIER)
            {
                markForSync();
            }

            if (boilingTicks >= cachedRecipe.getDuration())
            {
                syncToProxy();
                final PotBlockEntity.PotInventory proxyInventory = recipeProxy.getRecipeInventory();
                final PotRecipe recipe = cachedRecipe;
                final PotRecipe.Output finishedOutput;

                RecipeHelpers.setCraftingInput(proxyInventory, SLOT_EXTRA_INPUT_START, SLOT_EXTRA_INPUT_END + 1);
                try
                {
                    finishedOutput = recipe.getOutput(proxyInventory);
                }
                finally
                {
                    RecipeHelpers.clearCraftingInput();
                }

                proxyInventory.getFluidHandler().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                for (int slot = SLOT_EXTRA_INPUT_START; slot <= SLOT_EXTRA_INPUT_END; slot++)
                {
                    proxyInventory.setStackInSlot(slot, proxyInventory.getStackInSlot(slot).getCraftingRemainingItem());
                }

                finishedOutput.onFinish(proxyInventory);
                syncFromProxy();

                output = finishedOutput.isEmpty() ? null : finishedOutput;
                lastRecipeTemperature = Math.round(readRecipeTemperature(recipe));
                cachedRecipe = null;
                boilingTicks = 0;
                preBoilingTicks = 0;
                needsRecipeUpdate = true;
                markForSync();
            }
        }
        else if (boilingTicks > 0 || preBoilingTicks > 0)
        {
            boilingTicks = 0;
            preBoilingTicks = 0;
            markForSync();
        }
    }

    private void updateCachedRecipe()
    {
        final Level level = getLevel();
        if (level == null)
        {
            return;
        }

        syncToProxy();
        cachedRecipe = level.getRecipeManager().getRecipeFor(TFCRecipeTypes.POT.get(), recipeProxy.getRecipeInventory(), level).orElse(null);

        if (cachedRecipe != null)
        {
            lastRecipeTemperature = Math.round(readRecipeTemperature(cachedRecipe));
        }
        else if (!hasOutput())
        {
            lastRecipeTemperature = 0;
        }
    }

    private void syncToProxy()
    {
        prepareRecipeProxy();
        final PotBlockEntity.PotInventory target = recipeProxy.getRecipeInventory();

        for (int slot = 0; slot < INTERNAL_SLOT_COUNT; slot++)
        {
            target.setStackInSlot(slot, inventory.getStackInSlot(slot).copy());
        }

        target.getFluidHandler().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        final FluidStack fluid = inventory.getFluidInTank(0).copy();
        if (!fluid.isEmpty())
        {
            target.getFluidHandler().fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void prepareRecipeProxy()
    {
        final Level level = getLevel();
        if (level != null && recipeProxy.getLevel() != level)
        {
            recipeProxy.setLevel(level);
        }
    }

    private void syncFromProxy()
    {
        final PotBlockEntity.PotInventory source = recipeProxy.getRecipeInventory();

        for (int slot = 0; slot < INTERNAL_SLOT_COUNT; slot++)
        {
            inventory.setStackInSlot(slot, source.getStackInSlot(slot).copy());
        }
        inventory.setFluid(source.getFluidInTank(0).copy());
        needsRecipeUpdate = true;
    }

    public InteractionResult interactWithOutput(Player player, ItemStack clickedWith)
    {
        if (output == null)
        {
            return InteractionResult.PASS;
        }

        syncToProxy();
        final InteractionResult result = output.onInteract(recipeProxy, player, clickedWith);
        syncFromProxy();

        cleanupOutputState();
        markForSync();
        return result;
    }

    public boolean handleFluidInteraction(Player player, InteractionHand hand, ItemStack stack)
    {
        if (hasRecipeStarted() || hasOutput())
        {
            return false;
        }
        return FluidUtil.interactWithFluidHandler(player, hand, inventory.getFluidHandler());
    }

    public boolean isBoiling()
    {
        return cachedRecipe != null && output == null && cachedRecipe.isHotEnough(temperature);
    }

    public boolean hasRecipeStarted()
    {
        return isBoiling() && preBoilingTicks >= PRE_BOIL_TIME;
    }

    public boolean shouldRenderAsBoiling()
    {
        return boilingTicks > 0;
    }

    public @Nullable PotRecipe.Output getOutput()
    {
        return output;
    }

    public void setTargetTemperature(int temp)
    {
        targetTemperature = Math.max(0, Math.min(1600, temp));
        setChanged();
        markForSync();
    }

    public FluidStack getFluidInTank()
    {
        return inventory.getFluidInTank(0);
    }

    public boolean hasOutput()
    {
        return output != null && !output.isEmpty();
    }

    public EnergyStorage getEnergyStorage()
    {
        return energyStorage;
    }

    public float getTemperature()
    {
        return temperature;
    }

    public int getTargetTemperature()
    {
        return targetTemperature;
    }

    public int getDisplayProgress()
    {
        return hasOutput() ? 1 : getUiProgress();
    }

    public int getDisplayProgressTotal()
    {
        return hasOutput() ? 1 : getUiProgressTotal();
    }

    public int getDisplayRecipeTemperature()
    {
        return getUiRecipeTemperature();
    }

    public ContainerData getSyncData()
    {
        return syncData;
    }

    private int getUiProgress()
    {
        return cachedRecipe != null ? preBoilingTicks + boilingTicks : 0;
    }

    private int getUiProgressTotal()
    {
        return cachedRecipe != null ? PRE_BOIL_TIME + cachedRecipe.getDuration() : 0;
    }

    private int getUiRecipeTemperature()
    {
        return cachedRecipe != null ? Math.round(readRecipeTemperature(cachedRecipe)) : lastRecipeTemperature;
    }

    private void cleanupOutputState()
    {
        if (output != null && output.isEmpty())
        {
            output = null;
            lastRecipeTemperature = 0;
        }
    }

    @Override
    public void setAndUpdateSlots(int slot)
    {
        super.setAndUpdateSlots(slot);
        needsRecipeUpdate = true;
        cleanupOutputState();
        markForSync();
    }

    @Override
    public int getSlotStackLimit(int slot)
    {
        return slot >= SLOT_EXTRA_INPUT_START ? 1 : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return slot >= SLOT_EXTRA_INPUT_START && slot <= SLOT_EXTRA_INPUT_END;
    }

    @Override
    public void loadAdditional(CompoundTag nbt)
    {
        output = nbt.contains("output") ? PotRecipe.Output.read(nbt.getCompound("output")) : null;
        cleanupOutputState();
        boilingTicks = nbt.getInt("boilingTicks");
        preBoilingTicks = nbt.getInt("preBoilingTicks");
        temperature = nbt.getFloat("temperature");
        targetTemperature = nbt.getInt("targetTemperature");
        lastRecipeTemperature = nbt.getInt("lastRecipeTemperature");
        if (nbt.contains("energy"))
        {
            energyStorage.deserializeNBT(nbt.get("energy"));
        }
        needsRecipeUpdate = true;
        super.loadAdditional(nbt);
    }

    @Override
    public void saveAdditional(CompoundTag nbt)
    {
        if (output != null && !output.isEmpty())
        {
            nbt.put("output", PotRecipe.Output.write(output));
        }
        nbt.putInt("boilingTicks", boilingTicks);
        nbt.putInt("preBoilingTicks", preBoilingTicks);
        nbt.putFloat("temperature", temperature);
        nbt.putInt("targetTemperature", targetTemperature);
        nbt.putInt("lastRecipeTemperature", lastRecipeTemperature);
        nbt.put("energy", energyStorage.serializeNBT());
        super.saveAdditional(nbt);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player player)
    {
        return ElectricSoupPotContainer.create(this, playerInv, windowId);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        if (cap == ForgeCapabilities.ENERGY)
        {
            return energyCapability.cast();
        }
        if (cap == Capabilities.FLUID)
        {
            return sidedFluidInventory.getSidedHandler(side).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCapabilities()
    {
        super.invalidateCapabilities();
        sidedFluidInventory.invalidate();
        energyCapability.invalidate();
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        sidedFluidInventory.invalidate();
        energyCapability.invalidate();
    }

    private static @Nullable Field findPotRecipeTemperatureField()
    {
        try
        {
            final Field field = PotRecipe.class.getDeclaredField("minTemp");
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException ignored)
        {
            return null;
        }
    }

    private static float readRecipeTemperature(PotRecipe recipe)
    {
        if (POT_RECIPE_TEMPERATURE_FIELD != null)
        {
            try
            {
                return POT_RECIPE_TEMPERATURE_FIELD.getFloat(recipe);
            }
            catch (IllegalAccessException ignored)
            {
            }
        }
        return 0f;
    }

    public static class PotInventory implements EmptyInventory, DelegateItemHandler, DelegateFluidHandler, INBTSerializable<CompoundTag>
    {
        private final ElectricSoupPotBlockEntity pot;
        private final InventoryItemHandler inventory;
        private final FluidTank tank;

        public PotInventory(InventoryBlockEntity<PotInventory> entity)
        {
            pot = (ElectricSoupPotBlockEntity) entity;
            inventory = new InventoryItemHandler(pot, INTERNAL_SLOT_COUNT);
            tank = new FluidTank(FluidHelpers.BUCKET_VOLUME, fluid -> net.dries007.tfc.util.Helpers.isFluid(fluid.getFluid(), TFCTags.Fluids.USABLE_IN_POT))
            {
                @Override
                protected void onContentsChanged()
                {
                    pot.setAndUpdateSlots(-1);
                }
            };
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return pot.hasRecipeStarted() && slot >= SLOT_EXTRA_INPUT_START ? ItemStack.EMPTY : inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action)
        {
            return pot.hasRecipeStarted() ? FluidStack.EMPTY : tank.drain(maxDrain, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action)
        {
            return pot.hasRecipeStarted() ? FluidStack.EMPTY : tank.drain(resource, action);
        }

        @Override
        public IItemHandlerModifiable getItemHandler()
        {
            return inventory;
        }

        @Override
        public IFluidHandler getFluidHandler()
        {
            return tank;
        }

        public void setFluid(FluidStack stack)
        {
            tank.setFluid(stack);
        }

        @Override
        public CompoundTag serializeNBT()
        {
            final CompoundTag nbt = new CompoundTag();
            nbt.put("inventory", inventory.serializeNBT());
            nbt.put("tank", tank.writeToNBT(new CompoundTag()));
            return nbt;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt)
        {
            inventory.deserializeNBT(nbt.getCompound("inventory"));
            tank.readFromNBT(nbt.getCompound("tank"));
        }

        @Override
        public boolean isEmpty()
        {
            for (int slot = SLOT_EXTRA_INPUT_START; slot <= SLOT_EXTRA_INPUT_END; slot++)
            {
                if (!inventory.getStackInSlot(slot).isEmpty())
                {
                    return false;
                }
            }
            return tank.getFluidInTank(0).isEmpty();
        }
    }

    private static final class RecipeProxyPotBlockEntity extends PotBlockEntity
    {
        private RecipeProxyPotBlockEntity(BlockPos pos, BlockState state)
        {
            super(pos, state);
        }

        private PotBlockEntity.PotInventory getRecipeInventory()
        {
            return inventory;
        }
    }
}

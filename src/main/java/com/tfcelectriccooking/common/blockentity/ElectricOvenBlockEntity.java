package com.tfcelectriccooking.common.blockentity;

import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.ModFoodTraits;
import com.tfcelectriccooking.common.container.ElectricOvenContainer;
import com.tfcelectriccooking.compat.firmalife.FirmaLifeCompat;
import net.dries007.tfc.common.blockentities.TickableInventoryBlockEntity;
import net.dries007.tfc.common.capabilities.InventoryItemHandler;
import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.capabilities.heat.HeatCapability;
import net.dries007.tfc.common.capabilities.heat.IHeat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ElectricOvenBlockEntity extends TickableInventoryBlockEntity<InventoryItemHandler>
{
    public static final int SLOTS = 10;
    public static final int ENERGY_CAPACITY = 16000;
    public static final int ENERGY_MAX_IO = 256;
    public static final int ENERGY_PER_TICK = 20;
    public static final int MAX_TEMPERATURE = 600;

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
    private final FirmaLifeCompat.WrappedHeatingRecipe[] cachedRecipes = new FirmaLifeCompat.WrappedHeatingRecipe[SLOTS];

    private float temperature;
    private int targetTemperature;
    private boolean needsRecipeUpdate = true;

    private final ContainerData syncData = new ContainerData()
    {
        @Override
        public int get(int index)
        {
            return switch (index) {
                case 0 -> (int) temperature;
                case 1 -> targetTemperature;
                case 2 -> energyStorage.getEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value)
        {
            switch (index)
            {
                case 0 -> temperature = value;
                case 1 -> targetTemperature = Math.max(0, Math.min(MAX_TEMPERATURE, value));
                default -> {
                }
            }
        }

        @Override
        public int getCount()
        {
            return 3;
        }
    };

    public ElectricOvenBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlocks.ELECTRIC_OVEN_BLOCK_ENTITY.get(), pos, state,
            self -> new InventoryItemHandler(self, SLOTS),
            Component.translatable("block.tfcelectriccooking.electric_oven"));
    }

    public void serverTick()
    {
        checkForLastTickSync();
        final Level level = getLevel();
        if (level == null)
        {
            return;
        }

        if (needsRecipeUpdate)
        {
            needsRecipeUpdate = false;
            updateCachedRecipes();
        }

        handleEnergy();
        handleTemperature();
        handleCooking(level);
    }

    private void handleEnergy()
    {
        final Level level = getLevel();
        if (level == null)
        {
            return;
        }

        final boolean wasPowered = getBlockState().getValue(com.tfcelectriccooking.common.block.ElectricOvenBlock.POWERED);
        final boolean shouldPower = targetTemperature > 0 && energyStorage.getEnergyStored() >= ENERGY_PER_TICK;

        if (shouldPower)
        {
            energyStorage.extractEnergy(ENERGY_PER_TICK, false);
        }

        if (wasPowered != shouldPower)
        {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(com.tfcelectriccooking.common.block.ElectricOvenBlock.POWERED, shouldPower));
            markForSync();
        }
    }

    private void handleTemperature()
    {
        final boolean powered = getBlockState().getValue(com.tfcelectriccooking.common.block.ElectricOvenBlock.POWERED);
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

    private void handleCooking(Level level)
    {
        if (temperature <= 0)
        {
            return;
        }

        for (int i = 0; i < SLOTS; i++)
        {
            final ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty())
            {
                continue;
            }

            final @Nullable IHeat heat = HeatCapability.get(stack);
            if (heat != null)
            {
                HeatCapability.addTemp(heat, temperature);
            }

            final FirmaLifeCompat.WrappedHeatingRecipe recipe = cachedRecipes[i];
            if (recipe != null && heat != null && recipe.isValidTemperature(heat.getTemperature()))
            {
                final ItemStack result = recipe.assemble(stack, level.registryAccess());
                if (!result.isEmpty())
                {
                    FoodCapability.applyTrait(result, ModFoodTraits.ELECTRIC_OVEN_BAKED);
                    FoodCapability.setCreationDate(result, FoodCapability.getRoundedCreationDate());
                    HeatCapability.setTemperature(result, heat.getTemperature());
                }
                inventory.setStackInSlot(i, result);
                markForSync();
            }
        }
    }

    private void updateCachedRecipes()
    {
        for (int i = 0; i < SLOTS; i++)
        {
            final ItemStack stack = inventory.getStackInSlot(i);
            cachedRecipes[i] = stack.isEmpty() ? null : FirmaLifeCompat.getRecipe(stack);
        }
    }

    public void setTargetTemperature(int temp)
    {
        targetTemperature = Math.max(0, Math.min(MAX_TEMPERATURE, temp));
        setChanged();
        markForSync();
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

    public ContainerData getSyncData()
    {
        return syncData;
    }

    @Override
    public void setAndUpdateSlots(int slot)
    {
        super.setAndUpdateSlots(slot);
        needsRecipeUpdate = true;
    }

    @Override
    public int getSlotStackLimit(int slot)
    {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return FirmaLifeCompat.getRecipe(stack) != null;
    }

    @Override
    public void loadAdditional(CompoundTag nbt)
    {
        temperature = nbt.getFloat("temperature");
        targetTemperature = Math.max(0, Math.min(MAX_TEMPERATURE, nbt.getInt("targetTemperature")));
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
        nbt.putFloat("temperature", temperature);
        nbt.putInt("targetTemperature", targetTemperature);
        nbt.put("energy", energyStorage.serializeNBT());
        super.saveAdditional(nbt);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player player)
    {
        return ElectricOvenContainer.create(this, playerInv, windowId);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side)
    {
        if (cap == ForgeCapabilities.ENERGY)
        {
            return energyCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCapabilities()
    {
        super.invalidateCapabilities();
        energyCapability.invalidate();
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        energyCapability.invalidate();
    }
}

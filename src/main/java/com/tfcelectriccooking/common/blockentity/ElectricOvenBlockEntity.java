package com.tfcelectriccooking.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.blockentities.TickableInventoryBlockEntity;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.heat.IHeat;

import com.eerussianguy.firmalife.common.recipes.WrappedHeatingRecipe;

import com.tfcelectriccooking.TFCElectricCooking;
import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.ModFoodTraits;
import com.tfcelectriccooking.common.automation.AutomationItemHandler;
import com.tfcelectriccooking.common.block.ElectricOvenBlock;
import com.tfcelectriccooking.common.container.ElectricOvenContainer;

public class ElectricOvenBlockEntity extends TickableInventoryBlockEntity<ItemStackHandler>
{
    public static final int SLOTS = 10;
    public static final int ENERGY_CAPACITY = 16000;
    public static final int ENERGY_MAX_IO = 256;
    public static final int ENERGY_PER_TICK = 20;
    public static final int MAX_TEMPERATURE = 600;

    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_MAX_IO, ENERGY_MAX_IO, 0);
    private final WrappedHeatingRecipe[] cachedRecipes = new WrappedHeatingRecipe[SLOTS];
    private final boolean[] completedSlots = new boolean[SLOTS];
    private final AutomationItemHandler automationInventory;
    private float temperature = 0;
    private int targetTemperature = 0;
    private boolean needsRecipeUpdate = true;

    // Synced data for GUI
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
            switch (index) {
                case 0 -> temperature = value;
                case 1 -> targetTemperature = Math.max(0, Math.min(MAX_TEMPERATURE, value));
                case 2 -> {} // energy is read-only from client
            }
        }

        @Override
        public int getCount() { return 3; }
    };

    public ElectricOvenBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlocks.ELECTRIC_OVEN_BLOCK_ENTITY.get(), pos, state, defaultInventory(SLOTS), TFCElectricCooking.MOD_ID);
        automationInventory = new AutomationItemHandler(getInventory(), this::canAutomationInsert, this::canAutomationExtract);
    }

    public static InventoryBlockEntity.InventoryFactory<ItemStackHandler> defaultInventory(int slots)
    {
        return self -> new ItemStackHandler(slots)
        {
            @Override
            public int getSlotLimit(int slot) { return 1; }

            @Override
            protected void onContentsChanged(int slot)
            {
                self.setAndUpdateSlots(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack)
            {
                return WrappedHeatingRecipe.getRecipe(stack) != null;
            }
        };
    }

    public void serverTick()
    {
        checkForLastTickSync();
        final Level level = getLevel();
        if (level == null) return;

        if (needsRecipeUpdate)
        {
            needsRecipeUpdate = false;
            updateCachedRecipes();
        }

        handleEnergy();
        handleTemperature();
        handleCooking();
    }

    private void handleEnergy()
    {
        final Level level = getLevel();
        if (level == null) return;

        boolean wasPowered = getBlockState().getValue(ElectricOvenBlock.POWERED);
        boolean shouldPower = targetTemperature > 0 && energyStorage.getEnergyStored() >= ENERGY_PER_TICK;

        if (shouldPower)
        {
            energyStorage.extractEnergy(ENERGY_PER_TICK, false);
        }

        if (wasPowered != shouldPower)
        {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(ElectricOvenBlock.POWERED, shouldPower));
        }
    }

    private void handleTemperature()
    {
        boolean powered = getBlockState().getValue(ElectricOvenBlock.POWERED);
        if (powered && targetTemperature > 0)
        {
            if (temperature < targetTemperature)
            {
                temperature = Math.min(temperature + 3.0f, targetTemperature);
            }
            else if (temperature > targetTemperature)
            {
                temperature = Math.max(temperature - 3.0f, targetTemperature);
            }
        }
        else
        {
            if (temperature > 0)
            {
                temperature = Math.max(temperature - 1.5f, 0);
            }
        }
    }

    /**
     * Grill-like cooking: heat items towards device temperature, transform instantly
     * when the item's own temperature reaches the recipe's required temperature.
     * Compatible with both HeatingRecipe (grill) and OvenRecipe (oven) via WrappedHeatingRecipe.
     */
    private void handleCooking()
    {
        if (temperature <= 0) return;
        final ItemStackHandler inv = getInventory();

        for (int i = 0; i < SLOTS; i++)
        {
            final ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            // Heat the item towards device temperature (same as grill)
            final @Nullable IHeat heat = HeatCapability.get(stack);
            if (heat != null)
            {
                HeatCapability.addTemp(heat, temperature);
            }

            // Check recipe - transform instantly when item temperature is high enough
            final WrappedHeatingRecipe recipe = cachedRecipes[i];
            if (recipe != null && heat != null && recipe.isValidTemperature(heat.getTemperature()))
            {
                final ItemStack result = recipe.assemble(stack);
                if (!result.isEmpty())
                {
                    FoodCapability.applyTrait(result, ModFoodTraits.ELECTRIC_OVEN_BAKED);
                    FoodCapability.setCreationDate(result, FoodCapability.getRoundedCreationDate());
                    HeatCapability.setTemperature(result, heat.getTemperature());
                }
                inv.setStackInSlot(i, result);
                completedSlots[i] = true;
                markForSync();
            }
        }
    }

    private void updateCachedRecipes()
    {
        final ItemStackHandler inv = getInventory();
        for (int i = 0; i < SLOTS; i++)
        {
            final ItemStack stack = inv.getStackInSlot(i);
            cachedRecipes[i] = stack.isEmpty() ? null : WrappedHeatingRecipe.getRecipe(stack);
        }
    }

    public void setTargetTemperature(int temp)
    {
        targetTemperature = Math.max(0, Math.min(MAX_TEMPERATURE, temp));
        setChanged();
        markForSync();
    }

    public void adjustTargetTemperature(int delta)
    {
        setTargetTemperature(targetTemperature + delta);
    }

    public EnergyStorage getEnergyStorage()
    {
        return energyStorage;
    }

    public IItemHandler getAutomationInventory()
    {
        return automationInventory;
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
        if (slot >= 0 && slot < SLOTS)
        {
            completedSlots[slot] = false;
        }
        super.setAndUpdateSlots(slot);
        needsRecipeUpdate = true;
    }

    @Override
    public int getSlotStackLimit(int slot) { return 1; }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return WrappedHeatingRecipe.getRecipe(stack) != null;
    }

    private boolean canAutomationInsert(int slot, ItemStack stack)
    {
        return slot >= 0 && slot < SLOTS && !completedSlots[slot] && isItemValid(slot, stack);
    }

    private boolean canAutomationExtract(int slot)
    {
        return slot >= 0 && slot < SLOTS && completedSlots[slot];
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.loadAdditional(nbt, provider);
        temperature = nbt.getFloat("temperature");
        targetTemperature = Math.max(0, Math.min(MAX_TEMPERATURE, nbt.getInt("targetTemperature")));
        energyStorage.deserializeNBT(provider, nbt.get("energy"));
        for (int i = 0; i < SLOTS; i++)
        {
            completedSlots[i] = nbt.getBoolean("completedSlot" + i);
        }
        needsRecipeUpdate = true;
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.saveAdditional(nbt, provider);
        nbt.putFloat("temperature", temperature);
        nbt.putInt("targetTemperature", targetTemperature);
        nbt.put("energy", energyStorage.serializeNBT(provider));
        for (int i = 0; i < SLOTS; i++)
        {
            nbt.putBoolean("completedSlot" + i, completedSlots[i]);
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player player)
    {
        return ElectricOvenContainer.create(this, playerInv, windowId);
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.tfcelectriccooking.electric_oven");
    }
}

package com.tfcelectriccooking.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import net.dries007.tfc.common.blockentities.IPotInventory;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.blockentities.TickableInventoryBlockEntity;
import net.dries007.tfc.common.capabilities.DelegateFluidHandler;
import net.dries007.tfc.common.capabilities.DelegateItemHandler;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.recipes.PotRecipe;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.common.recipes.input.NonEmptyInput;
import net.dries007.tfc.common.recipes.outputs.PotOutput;
import net.dries007.tfc.common.capabilities.SidedHandler;

import com.tfcelectriccooking.TFCElectricCooking;
import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.block.ElectricSoupPotBlock;
import com.tfcelectriccooking.common.container.ElectricSoupPotContainer;

public class ElectricSoupPotBlockEntity extends TickableInventoryBlockEntity<ElectricSoupPotBlockEntity.PotInventory>
{
    public static final int SLOT_INPUT_START = 0;
    public static final int SLOT_INPUT_END = 4;
    public static final int INPUT_SLOTS = 5;
    public static final int PRE_BOIL_TIME = 100;
    public static final int SPEED_MULTIPLIER = 4;
    public static final int ENERGY_CAPACITY = 16000;
    public static final int ENERGY_MAX_IO = 256;
    public static final int ENERGY_PER_TICK = 20;
    private static final @Nullable Field POT_RECIPE_TEMPERATURE_FIELD = findPotRecipeTemperatureField();

    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_MAX_IO, ENERGY_MAX_IO, 0);
    private final SidedHandler<IFluidHandler> sidedFluidInventory;

    private @Nullable PotOutput output;
    private @Nullable PotRecipe cachedRecipe;
    private int boilingTicks;
    private int preBoilingTicks;
    private float temperature = 0;
    private int targetTemperature = 0;
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
            switch (index) {
                case 0 -> temperature = value;
                case 1 -> targetTemperature = value;
                case 2 -> {}
                case 3 -> syncedUiProgress = value;
                case 4 -> syncedUiProgressTotal = value;
                case 5 -> syncedUiHasOutput = value;
                case 6 -> syncedUiRecipeTemperature = value;
            }
        }

        @Override
        public int getCount() { return 7; }
    };

    public ElectricSoupPotBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlocks.ELECTRIC_SOUP_POT_BLOCK_ENTITY.get(), pos, state,
            self -> new PotInventory((ElectricSoupPotBlockEntity) self), TFCElectricCooking.MOD_ID);

        sidedFluidInventory = new SidedHandler<>(getInventory());
    }

    public void serverTick()
    {
        checkForLastTickSync();
        final Level level = getLevel();
        if (level == null) return;

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
        if (level == null) return;

        boolean wasPowered = getBlockState().getValue(ElectricSoupPotBlock.POWERED);
        boolean shouldPower = targetTemperature > 0 && energyStorage.getEnergyStored() >= ENERGY_PER_TICK;

        if (shouldPower)
        {
            energyStorage.extractEnergy(ENERGY_PER_TICK, false);
        }

        if (wasPowered != shouldPower)
        {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(ElectricSoupPotBlock.POWERED, shouldPower));
        }
    }

    private void handleTemperature()
    {
        boolean powered = getBlockState().getValue(ElectricSoupPotBlock.POWERED);
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

    private void handleCooking()
    {
        // If we already have output, wait for player extraction
        if (output != null) return;

        if (cachedRecipe != null && cachedRecipe.isHotEnough(temperature))
        {
            preBoilingTicks++;
            if (preBoilingTicks >= PRE_BOIL_TIME)
            {
                // 4x speed: increment by SPEED_MULTIPLIER instead of 1
                boilingTicks += SPEED_MULTIPLIER;
                if (boilingTicks >= cachedRecipe.getDuration())
                {
                    // Recipe complete
                    final PotInventory inv = getInventory();
                    final PotOutput finishedOutput = cachedRecipe.getOutput(inv);
                    finishedOutput.onFinish(inv);
                    output = finishedOutput.isEmpty() ? null : finishedOutput;
                    lastRecipeTemperature = Math.round(readRecipeTemperature(cachedRecipe));

                    // Clear input slots
                    for (int i = SLOT_INPUT_START; i <= SLOT_INPUT_END; i++)
                    {
                        inv.getItemHandler().setStackInSlot(i, ItemStack.EMPTY);
                    }
                    inv.clearFluid();

                    cachedRecipe = null;
                    boilingTicks = 0;
                    preBoilingTicks = 0;
                    needsRecipeUpdate = true;
                    markForSync();
                }
            }
        }
        else
        {
            // Not hot enough or no recipe - reset boiling progress
            if (boilingTicks > 0 || preBoilingTicks > 0)
            {
                boilingTicks = 0;
                preBoilingTicks = 0;
                markForSync();
            }
        }
    }

    private void updateCachedRecipe()
    {
        final Level level = getLevel();
        if (level == null) return;

        final PotInventory inv = getInventory();
        cachedRecipe = level.getRecipeManager()
            .getRecipeFor(TFCRecipeTypes.POT.get(), inv, level)
            .map(r -> r.value())
            .orElse(null);

        if (cachedRecipe != null)
        {
            lastRecipeTemperature = Math.round(readRecipeTemperature(cachedRecipe));
        }
        else if (!hasOutput())
        {
            lastRecipeTemperature = 0;
        }
    }

    public ItemInteractionResult interactWithOutput(Player player, ItemStack clickedWith)
    {
        if (output != null)
        {
            final ItemInteractionResult result = output.onInteract(getInventory(), player, clickedWith);
            cleanupOutputState();
            markForSync();
            return result;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public boolean handleFluidInteraction(Player player, InteractionHand hand, ItemStack stack)
    {
        if (hasRecipeStarted() || hasOutput()) return false;
        return FluidUtil.interactWithFluidHandler(player, hand, getInventory().getFluidHandler());
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

    public int getBoilingTicks()
    {
        return boilingTicks;
    }

    public @Nullable PotOutput getOutput()
    {
        return output;
    }

    public void setTargetTemperature(int temp)
    {
        targetTemperature = Math.max(0, Math.min(1600, temp));
        setChanged();
        markForSync();
    }

    public void adjustTargetTemperature(int delta)
    {
        setTargetTemperature(targetTemperature + delta);
    }

    public FluidStack getFluidInTank()
    {
        return getInventory().getFluidHandler().getFluidInTank(0);
    }

    public boolean hasOutput()
    {
        return output != null && !output.isEmpty();
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

    private static @Nullable Field findPotRecipeTemperatureField()
    {
        try
        {
            final Field field = PotRecipe.class.getDeclaredField("temperature");
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

    @Nullable
    public static IFluidHandler getSidedFluidInventory(ElectricSoupPotBlockEntity be, @Nullable Direction side)
    {
        return be.sidedFluidInventory.get(side);
    }

    @Override
    public void setAndUpdateSlots(int slot)
    {
        super.setAndUpdateSlots(slot);
        needsRecipeUpdate = true;
        cleanupOutputState();
    }

    @Override
    public int getSlotStackLimit(int slot) { return 1; }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return true; // Pot accepts various items; recipe matching handles validation
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.loadAdditional(nbt, provider);
        temperature = nbt.getFloat("temperature");
        targetTemperature = nbt.getInt("targetTemperature");
        energyStorage.deserializeNBT(provider, nbt.get("energy"));
        boilingTicks = nbt.getInt("boilingTicks");
        preBoilingTicks = nbt.getInt("preBoilingTicks");
        lastRecipeTemperature = nbt.getInt("lastRecipeTemperature");
        output = nbt.contains("output") ? PotOutput.read(provider, nbt.getCompound("output")) : null;
        cleanupOutputState();
        needsRecipeUpdate = true;
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.saveAdditional(nbt, provider);
        nbt.putFloat("temperature", temperature);
        nbt.putInt("targetTemperature", targetTemperature);
        nbt.put("energy", energyStorage.serializeNBT(provider));
        nbt.putInt("boilingTicks", boilingTicks);
        nbt.putInt("preBoilingTicks", preBoilingTicks);
        nbt.putInt("lastRecipeTemperature", lastRecipeTemperature);
        if (output != null && !output.isEmpty())
        {
            nbt.put("output", PotOutput.write(provider, output));
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player player)
    {
        return ElectricSoupPotContainer.create(this, playerInv, windowId);
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.tfcelectriccooking.electric_soup_pot");
    }

    /**
     * Inner inventory class implementing IPotInventory for PotRecipe compatibility.
     */
    public static class PotInventory implements IPotInventory, net.neoforged.neoforge.common.util.INBTSerializable<CompoundTag>
    {
        private final ElectricSoupPotBlockEntity pot;
        private final ItemStackHandler inventory;
        private final FluidTank tank;

        public PotInventory(ElectricSoupPotBlockEntity pot)
        {
            this.pot = pot;
            this.inventory = new ItemStackHandler(INPUT_SLOTS)
            {
                @Override
                public int getSlotLimit(int slot) { return 1; }

                @Override
                protected void onContentsChanged(int slot)
                {
                    pot.setAndUpdateSlots(slot);
                }
            };
            this.tank = new FluidTank(FluidHelpers.BUCKET_VOLUME)
            {
                @Override
                protected void onContentsChanged()
                {
                    pot.setAndUpdateSlots(-1);
                }
            };
        }

        @Override
        public int inputStart() { return SLOT_INPUT_START; }

        @Override
        public int inputEnd() { return SLOT_INPUT_END; }

        @Override
        public void clearFluid()
        {
            tank.setFluid(FluidStack.EMPTY);
        }

        // DelegateItemHandler
        @Override
        public IItemHandlerModifiable getItemHandler() { return inventory; }

        // DelegateFluidHandler
        @Override
        public IFluidHandler getFluidHandler() { return tank; }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider)
        {
            final CompoundTag nbt = new CompoundTag();
            nbt.put("inventory", inventory.serializeNBT(provider));
            nbt.put("tank", tank.writeToNBT(provider, new CompoundTag()));
            return nbt;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
        {
            inventory.deserializeNBT(provider, nbt.getCompound("inventory"));
            tank.readFromNBT(provider, nbt.getCompound("tank"));
        }

        // NonEmptyInput - check if any input is non-empty
        @Override
        public boolean isEmpty()
        {
            for (int i = SLOT_INPUT_START; i <= SLOT_INPUT_END; i++)
            {
                if (!inventory.getStackInSlot(i).isEmpty()) return false;
            }
            return tank.getFluidInTank(0).isEmpty();
        }
    }
}

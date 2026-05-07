package com.tfcelectriccooking.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.IPotInventory;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.blockentities.TickableInventoryBlockEntity;
import net.dries007.tfc.common.capabilities.DelegateFluidHandler;
import net.dries007.tfc.common.capabilities.DelegateItemHandler;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.FoodTrait;
import net.dries007.tfc.common.component.food.FoodTraits;
import net.dries007.tfc.common.recipes.JamPotRecipe;
import net.dries007.tfc.common.recipes.PotRecipe;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.common.recipes.outputs.PotOutput;
import net.dries007.tfc.common.capabilities.SidedHandler;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.util.Helpers;

import com.tfcelectriccooking.TFCElectricCooking;
import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.automation.AutomationFluidHandler;
import com.tfcelectriccooking.common.automation.AutomationItemHandler;
import com.tfcelectriccooking.common.automation.JarringStationAutomationBridge;
import com.tfcelectriccooking.common.block.ElectricSoupPotBlock;
import com.tfcelectriccooking.common.compat.JamJarCompat;
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
    public static final int MAX_TEMPERATURE = 600;
    private static final @Nullable Field POT_RECIPE_TEMPERATURE_FIELD = findPotRecipeTemperatureField();
    private static final ResourceLocation FIRMA_LIFE_DRIED_TRAIT = ResourceLocation.fromNamespaceAndPath("firmalife", "dried");
    private static final ResourceLocation FIRMA_LIFE_SUGAR_WATER = ResourceLocation.fromNamespaceAndPath("firmalife", "sugar_water");
    private static final TagKey<Item> SWEETENER = TFCTags.Items.SWEETENERS;
    private static final TagKey<Item> FRUITS = TFCTags.Items.FRUITS;
    private static final TagKey<Fluid> SUGAR_WATER = TagKey.create(Registries.FLUID, FIRMA_LIFE_SUGAR_WATER);

    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_MAX_IO, ENERGY_MAX_IO, 0);
    private final SidedHandler<IFluidHandler> sidedFluidInventory;
    private final AutomationItemHandler automationInventory;
    private final IFluidHandler automationFluidInventory;

    private @Nullable PotOutput output;
    private @Nullable PotRecipe cachedRecipe;
    private @Nullable SpecialRecipe cachedSpecialRecipe;
    private int boilingTicks;
    private int preBoilingTicks;
    private float temperature = 0;
    private int targetTemperature = 0;
    private boolean inventoryOutputReady;
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
                case 1 -> targetTemperature = Math.max(0, Math.min(MAX_TEMPERATURE, value));
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

        automationInventory = new AutomationItemHandler(getInventory().getItemHandler(), this::canAutomationInsertItem, this::canAutomationExtractItem);
        automationFluidInventory = new AutomationFluidHandler(getInventory().getFluidHandler(), this::canAutomationFillFluid, this::canAutomationDrainFluid);
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
        // If we already have output, wait for extraction.
        if (output != null || inventoryOutputReady) return;

        if (cachedSpecialRecipe != null && cachedSpecialRecipe.isHotEnough(temperature))
        {
            handleSpecialCooking(cachedSpecialRecipe);
        }
        else if (cachedRecipe != null && cachedRecipe.isHotEnough(temperature))
        {
            if (preBoilingTicks < PRE_BOIL_TIME)
            {
                preBoilingTicks++;
                return;
            }

            // 4x speed: increment by SPEED_MULTIPLIER instead of 1
            boilingTicks += SPEED_MULTIPLIER;
            if (boilingTicks >= cachedRecipe.getDuration())
            {
                // Recipe complete
                final PotInventory inv = getInventory();
                final PotOutput finishedOutput = cachedRecipe.getOutput(inv);
                finishedOutput.onFinish(inv);
                output = finishedOutput.isEmpty() ? null : finishedOutput;
                inventoryOutputReady = output == null && hasAnyInputItem();
                lastRecipeTemperature = Math.round(readRecipeTemperature(cachedRecipe));

                if (output != null)
                {
                    for (int i = SLOT_INPUT_START; i <= SLOT_INPUT_END; i++)
                    {
                        inv.getItemHandler().setStackInSlot(i, ItemStack.EMPTY);
                    }
                    inv.clearFluid();
                }

                cachedRecipe = null;
                cachedSpecialRecipe = null;
                boilingTicks = 0;
                preBoilingTicks = 0;
                needsRecipeUpdate = true;
                markForSync();
                notifyNearbyJarringStations();
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

    private void handleSpecialCooking(SpecialRecipe recipe)
    {
        if (preBoilingTicks < PRE_BOIL_TIME)
        {
            preBoilingTicks++;
            return;
        }

        boilingTicks += SPEED_MULTIPLIER;
        if (boilingTicks >= recipe.duration)
        {
            if (recipe == SpecialRecipe.SUGAR_WATER)
            {
                finishSugarWaterRecipe();
            }
            else if (recipe == SpecialRecipe.SUGAR_WATER_JAM)
            {
                output = finishSugarWaterJamRecipe();
            }

            lastRecipeTemperature = recipe.temperature;
            cachedRecipe = null;
            cachedSpecialRecipe = null;
            boilingTicks = 0;
            preBoilingTicks = 0;
            needsRecipeUpdate = true;
            cleanupOutputState();
            markForSync();
            notifyNearbyJarringStations();
        }
    }

    private void finishSugarWaterRecipe()
    {
        consumeOneSweetener();

        getInventory().getFluidHandler().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        getInventory().setFluid(new FluidStack(BuiltInRegistries.FLUID.get(FIRMA_LIFE_SUGAR_WATER), FluidHelpers.BUCKET_VOLUME));

        output = null;
        inventoryOutputReady = false;
    }

    private PotOutput finishSugarWaterJamRecipe()
    {
        final FruitBatch batch = getSingleFruitBatch();
        if (batch == null)
        {
            return PotOutput.EMPTY_INSTANCE;
        }

        getInventory().getFluidHandler().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        int remaining = batch.count;
        for (int slot = SLOT_INPUT_START; slot <= SLOT_INPUT_END && remaining > 0; slot++)
        {
            final ItemStack stack = getInventory().getItemHandler().getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getItem() == batch.sample.getItem())
            {
                final int consumed = Math.min(stack.getCount(), remaining);
                stack.shrink(consumed);
                getInventory().getItemHandler().setStackInSlot(slot, stack);
                remaining -= consumed;
            }
        }

        inventoryOutputReady = false;
        return new JamPotRecipe.JamOutput(batch.unsealed, batch.sealed, batch.texture);
    }

    private @Nullable SpecialRecipe findSpecialRecipe()
    {
        if (matchesSugarWaterRecipe())
        {
            return SpecialRecipe.SUGAR_WATER;
        }
        if (matchesSugarWaterJamRecipe())
        {
            return SpecialRecipe.SUGAR_WATER_JAM;
        }
        return null;
    }

    private boolean matchesSugarWaterRecipe()
    {
        final FluidStack fluid = getInventory().getFluidHandler().getFluidInTank(0);
        if (fluid.getAmount() < FluidHelpers.BUCKET_VOLUME || fluid.getFluid() != Fluids.WATER)
        {
            return false;
        }

        boolean foundSweetener = false;
        for (int slot = SLOT_INPUT_START; slot <= SLOT_INPUT_END; slot++)
        {
            final ItemStack stack = getInventory().getItemHandler().getStackInSlot(slot);
            if (stack.isEmpty())
            {
                continue;
            }
            if (!Helpers.isItem(stack, SWEETENER))
            {
                return false;
            }
            foundSweetener = true;
        }
        return foundSweetener && BuiltInRegistries.FLUID.containsKey(FIRMA_LIFE_SUGAR_WATER);
    }

    private boolean matchesSugarWaterJamRecipe()
    {
        final FluidStack fluid = getInventory().getFluidHandler().getFluidInTank(0);
        return fluid.getAmount() >= FluidHelpers.BUCKET_VOLUME
            && isSugarWaterFluid(fluid)
            && getSingleFruitBatch() != null;
    }

    private void consumeOneSweetener()
    {
        for (int slot = SLOT_INPUT_START; slot <= SLOT_INPUT_END; slot++)
        {
            final ItemStack stack = getInventory().getItemHandler().getStackInSlot(slot);
            if (!stack.isEmpty() && Helpers.isItem(stack, SWEETENER))
            {
                stack.shrink(1);
                getInventory().getItemHandler().setStackInSlot(slot, stack);
                return;
            }
        }
    }

    private @Nullable FruitBatch getSingleFruitBatch()
    {
        ItemStack sample = ItemStack.EMPTY;
        int count = 0;
        final List<ItemStack> previous = new ArrayList<>(INPUT_SLOTS);

        for (int slot = SLOT_INPUT_START; slot <= SLOT_INPUT_END; slot++)
        {
            final ItemStack stack = getInventory().getItemHandler().getStackInSlot(slot);
            if (stack.isEmpty())
            {
                continue;
            }
            if (!isUsableJamFruit(stack))
            {
                return null;
            }
            if (sample.isEmpty())
            {
                sample = stack.copyWithCount(1);
            }
            else if (sample.getItem() != stack.getItem())
            {
                return null;
            }
            previous.add(stack.copyWithCount(1));
            count += stack.getCount();
        }

        if (sample.isEmpty())
        {
            return null;
        }

        count = Math.min(count, INPUT_SLOTS);
        final ResourceLocation fruitId = BuiltInRegistries.ITEM.getKey(sample.getItem());
        if (!fruitId.getPath().startsWith("food/"))
        {
            return null;
        }

        final String fruitName = fruitId.getPath().substring("food/".length());
        final Item unsealedItem = JamJarCompat.getUnsealedJamJarItem(fruitId.getNamespace(), fruitName);
        final Item sealedItem = JamJarCompat.getSealedJamJarItem(fruitId.getNamespace(), fruitName);
        if (unsealedItem == null || sealedItem == null)
        {
            return null;
        }

        final ItemStack unsealed = new ItemStack(unsealedItem, count);
        final ItemStack sealed = new ItemStack(sealedItem, count);
        FoodCapability.updateFoodFromAllPrevious(previous, unsealed);
        FoodCapability.updateFoodFromAllPrevious(previous, sealed);
        return new FruitBatch(sample, count, unsealed, sealed, ResourceLocation.fromNamespaceAndPath(fruitId.getNamespace(), "block/jar/" + fruitName));
    }

    private boolean isUsableJamFruit(ItemStack stack)
    {
        if (!Helpers.isItem(stack, FRUITS) || FoodCapability.isRotten(stack))
        {
            return false;
        }
        final Holder<FoodTrait> dried = getFoodTrait(FIRMA_LIFE_DRIED_TRAIT);
        return dried == null || !FoodCapability.hasTrait(stack, dried);
    }

    private void updateCachedRecipe()
    {
        final Level level = getLevel();
        if (level == null) return;

        if (inventoryOutputReady)
        {
            cachedRecipe = null;
            cachedSpecialRecipe = null;
            if (!hasOutput())
            {
                lastRecipeTemperature = 0;
            }
            return;
        }

        cachedSpecialRecipe = findSpecialRecipe();
        if (cachedSpecialRecipe != null)
        {
            cachedRecipe = null;
            lastRecipeTemperature = cachedSpecialRecipe.temperature;
            return;
        }

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
            if (output instanceof JamPotRecipe.JamOutput jamOutput)
            {
                final ItemInteractionResult jamResult = interactWithJamOutput(player, clickedWith, jamOutput);
                if (jamResult != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION)
                {
                    return jamResult;
                }
            }

            final ItemInteractionResult result = output.onInteract(getInventory(), player, clickedWith);
            cleanupOutputState();
            markForSync();
            return result;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private ItemInteractionResult interactWithJamOutput(Player player, ItemStack clickedWith, JamPotRecipe.JamOutput jamOutput)
    {
        final ItemStack result;
        if (JamJarCompat.isEmptyJar(clickedWith))
        {
            if (jamOutput.unsealedStack().isEmpty())
            {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            result = jamOutput.unsealedStack().split(1);
            jamOutput.sealedStack().shrink(1);
        }
        else if (JamJarCompat.isEmptyJarWithLid(clickedWith))
        {
            if (jamOutput.sealedStack().isEmpty())
            {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            result = jamOutput.sealedStack().split(1);
            jamOutput.unsealedStack().shrink(1);
        }
        else
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (result.isEmpty())
        {
            cleanupOutputState();
            markForSync();
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        clickedWith.shrink(1);
        ItemHandlerHelper.giveItemToPlayer(player, result);
        cleanupOutputState();
        markForSync();
        return ItemInteractionResult.sidedSuccess(player.level().isClientSide);
    }

    public ItemStack tryTakeOutputWithJar(ItemStack jar)
    {
        if (!(output instanceof JamPotRecipe.JamOutput jamOutput) || !JamJarCompat.isSupportedEmptyJar(jar))
        {
            return ItemStack.EMPTY;
        }

        final ItemStack result;
        if (JamJarCompat.isEmptyJar(jar))
        {
            if (jamOutput.unsealedStack().isEmpty())
            {
                return ItemStack.EMPTY;
            }
            result = jamOutput.unsealedStack().split(1);
            jamOutput.sealedStack().shrink(1);
        }
        else if (JamJarCompat.isEmptyJarWithLid(jar))
        {
            if (jamOutput.sealedStack().isEmpty())
            {
                return ItemStack.EMPTY;
            }
            result = jamOutput.sealedStack().split(1);
            jamOutput.unsealedStack().shrink(1);
        }
        else
        {
            return ItemStack.EMPTY;
        }

        cleanupOutputState();
        needsRecipeUpdate = true;
        setChanged();
        markForSync();
        return result;
    }

    private void notifyNearbyJarringStations()
    {
        final Level level = getLevel();
        if (level != null && hasOutput())
        {
            JarringStationAutomationBridge.tryFillAroundPot(level, worldPosition);
        }
    }

    public boolean handleFluidInteraction(Player player, InteractionHand hand, ItemStack stack)
    {
        if (!canAcceptManualInput()) return false;
        return FluidUtil.interactWithFluidHandler(player, hand, getInventory().getFluidHandler());
    }

    public boolean isBoiling()
    {
        return output == null
            && ((cachedSpecialRecipe != null && cachedSpecialRecipe.isHotEnough(temperature))
                || (cachedRecipe != null && cachedRecipe.isHotEnough(temperature)));
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
        targetTemperature = Math.max(0, Math.min(MAX_TEMPERATURE, temp));
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

    public boolean canAcceptManualInput()
    {
        return !hasRecipeStarted() && !hasOutput() && !inventoryOutputReady;
    }

    public IItemHandler getAutomationInventory()
    {
        return automationInventory;
    }

    private int getUiProgress()
    {
        return cachedSpecialRecipe != null || cachedRecipe != null ? preBoilingTicks + boilingTicks : 0;
    }

    private int getUiProgressTotal()
    {
        if (cachedSpecialRecipe != null)
        {
            return PRE_BOIL_TIME + cachedSpecialRecipe.duration;
        }
        return cachedRecipe != null ? PRE_BOIL_TIME + cachedRecipe.getDuration() : 0;
    }

    private int getUiRecipeTemperature()
    {
        if (cachedSpecialRecipe != null)
        {
            return cachedSpecialRecipe.temperature;
        }
        return cachedRecipe != null ? Math.round(readRecipeTemperature(cachedRecipe)) : lastRecipeTemperature;
    }

    private void cleanupOutputState()
    {
        if (output != null && output.isEmpty())
        {
            output = null;
            lastRecipeTemperature = 0;
        }
        if (inventoryOutputReady && (hasOnlySweetenerInputs() || !hasAnyInputItem()))
        {
            inventoryOutputReady = false;
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

    private static @Nullable Holder<FoodTrait> getFoodTrait(ResourceLocation id)
    {
        return FoodTraits.REGISTRY.getHolder(ResourceKey.create(FoodTraits.KEY, id)).orElse(null);
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
        return side == null ? be.sidedFluidInventory.get(null) : be.automationFluidInventory;
    }

    public IFluidHandler getAutomationFluidInventory()
    {
        return automationFluidInventory;
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
    public int getSlotStackLimit(int slot) { return 1; }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return true; // Pot accepts various items; recipe matching handles validation
    }

    private boolean canAutomationInsertItem(int slot, ItemStack stack)
    {
        return slot >= SLOT_INPUT_START && slot <= SLOT_INPUT_END && canAcceptManualInput();
    }

    private boolean canAutomationExtractItem(int slot)
    {
        return slot >= SLOT_INPUT_START && slot <= SLOT_INPUT_END && inventoryOutputReady;
    }

    private boolean canAutomationFillFluid(FluidStack stack)
    {
        return !stack.isEmpty() && canAcceptManualInput() && Helpers.isFluid(stack.getFluid(), TFCTags.Fluids.USABLE_IN_POT);
    }

    private boolean canAutomationDrainFluid()
    {
        return !hasRecipeStarted() && !hasOutput();
    }

    private boolean isSugarWaterFluid(FluidStack fluid)
    {
        return !fluid.isEmpty()
            && (fluid.getFluid() == BuiltInRegistries.FLUID.get(FIRMA_LIFE_SUGAR_WATER) || Helpers.isFluid(fluid.getFluid(), SUGAR_WATER));
    }

    private boolean hasOnlySweetenerInputs()
    {
        boolean foundSweetener = false;
        final PotInventory inv = getInventory();
        for (int i = SLOT_INPUT_START; i <= SLOT_INPUT_END; i++)
        {
            final ItemStack stack = inv.getItemHandler().getStackInSlot(i);
            if (stack.isEmpty())
            {
                continue;
            }
            if (!Helpers.isItem(stack, SWEETENER))
            {
                return false;
            }
            foundSweetener = true;
        }
        return foundSweetener;
    }

    private boolean hasAnyInputItem()
    {
        final PotInventory inv = getInventory();
        for (int i = SLOT_INPUT_START; i <= SLOT_INPUT_END; i++)
        {
            if (!inv.getItemHandler().getStackInSlot(i).isEmpty())
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.loadAdditional(nbt, provider);
        temperature = nbt.getFloat("temperature");
        targetTemperature = Math.max(0, Math.min(MAX_TEMPERATURE, nbt.getInt("targetTemperature")));
        energyStorage.deserializeNBT(provider, nbt.get("energy"));
        boilingTicks = nbt.getInt("boilingTicks");
        preBoilingTicks = nbt.getInt("preBoilingTicks");
        inventoryOutputReady = nbt.getBoolean("inventoryOutputReady");
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
        nbt.putBoolean("inventoryOutputReady", inventoryOutputReady);
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

    private enum SpecialRecipe
    {
        SUGAR_WATER(500, 300),
        SUGAR_WATER_JAM(500, 300);

        private final int duration;
        private final int temperature;

        SpecialRecipe(int duration, int temperature)
        {
            this.duration = duration;
            this.temperature = temperature;
        }

        private boolean isHotEnough(float temperature)
        {
            return temperature > this.temperature;
        }
    }

    private record FruitBatch(ItemStack sample, int count, ItemStack unsealed, ItemStack sealed, ResourceLocation texture) {}

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
                public boolean isFluidValid(FluidStack stack)
                {
                    return Helpers.isFluid(stack.getFluid(), TFCTags.Fluids.USABLE_IN_POT);
                }

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

        public void setFluid(FluidStack stack)
        {
            tank.setFluid(stack);
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

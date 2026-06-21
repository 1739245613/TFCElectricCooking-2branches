package com.tfcelectriccooking.common.blockentity;

import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.automation.AutomationFluidHandler;
import com.tfcelectriccooking.common.automation.AutomationItemHandler;
import com.tfcelectriccooking.common.automation.JarringStationAutomationBridge;
import com.tfcelectriccooking.common.compat.JamJarCompat;
import com.tfcelectriccooking.common.container.ElectricSoupPotContainer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
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
import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.recipes.JamPotRecipe;
import net.dries007.tfc.common.recipes.PotRecipe;
import net.dries007.tfc.common.recipes.RecipeHelpers;
import net.dries007.tfc.common.recipes.SimplePotRecipe;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.common.recipes.inventory.EmptyInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
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
    public static final int MAX_TEMPERATURE = 800;
    private static final int SUGAR_WATER_AMOUNT = 500;
    private static final int SUGAR_WATER_PER_JAM = 100;

    private static final @Nullable Field POT_RECIPE_TEMPERATURE_FIELD = findPotRecipeTemperatureField();
    private static final ResourceLocation FIRMA_LIFE_DRIED_TRAIT = new ResourceLocation("firmalife", "dried");
    private static final ResourceLocation FIRMA_LIFE_SUGAR_WATER = new ResourceLocation("firmalife", "sugar_water");
    private static final TagKey<Item> SWEETENER = TagKey.create(Registries.ITEM, new ResourceLocation("tfc", "sweetener"));
    private static final TagKey<Item> FRUITS = TagKey.create(Registries.ITEM, new ResourceLocation("tfc", "foods/fruits"));
    private static final TagKey<Fluid> SUGAR_WATER = TagKey.create(Registries.FLUID, FIRMA_LIFE_SUGAR_WATER);

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
    private final AutomationItemHandler automationInventory;
    private final AutomationFluidHandler automationFluidInventory;
    private final LazyOptional<IItemHandler> automationItemCapability;
    private final LazyOptional<IFluidHandler> automationFluidCapability;
    private final SidedHandler.Builder<IFluidHandler> sidedFluidInventory;
    private final RecipeProxyPotBlockEntity recipeProxy;

    private @Nullable PotRecipe.Output output;
    private @Nullable PotRecipe cachedRecipe;
    private @Nullable SpecialRecipe cachedSpecialRecipe;
    private int boilingTicks;
    private int preBoilingTicks;
    private float temperature;
    private int targetTemperature;
    private boolean inventoryOutputReady;
    private boolean needsRecipeUpdate = true;
    private int lastRecipeTemperature;
    private int syncedUiProgress;
    private int syncedUiProgressTotal;
    private int syncedUiHasOutput;
    private int syncedUiRecipeTemperature;
    private int syncedUiBoilingTicks;

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
                case 7 -> clientSide ? syncedUiBoilingTicks : boilingTicks;
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
                case 2 -> setSyncedEnergy(value);
                case 3 -> syncedUiProgress = value;
                case 4 -> syncedUiProgressTotal = value;
                case 5 -> syncedUiHasOutput = value;
                case 6 -> syncedUiRecipeTemperature = value;
                case 7 -> syncedUiBoilingTicks = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount()
        {
            return 8;
        }
    };

    public ElectricSoupPotBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlocks.ELECTRIC_SOUP_POT_BLOCK_ENTITY.get(), pos, state,
            PotInventory::new,
            Component.translatable("block.tfcelectriccooking.electric_soup_pot"));

        automationInventory = new AutomationItemHandler(inventory.getItemHandler(), this::canAutomationInsertItem, this::canAutomationExtractItem, this::syncAutomationChange);
        automationFluidInventory = new AutomationFluidHandler(inventory.getFluidHandler(), this::canAutomationFillFluid, this::canAutomationDrainFluid, this::syncAutomationChange);
        automationItemCapability = LazyOptional.of(() -> automationInventory);
        automationFluidCapability = LazyOptional.of(() -> automationFluidInventory);

        sidedInventory
            .on(new PartialItemHandler(inventory).insert(SLOT_EXTRA_INPUT_START, SLOT_EXTRA_INPUT_START + 1, SLOT_EXTRA_INPUT_START + 2, SLOT_EXTRA_INPUT_START + 3, SLOT_EXTRA_INPUT_END).extract(SLOT_EXTRA_INPUT_START, SLOT_EXTRA_INPUT_START + 1, SLOT_EXTRA_INPUT_START + 2, SLOT_EXTRA_INPUT_START + 3, SLOT_EXTRA_INPUT_END), Direction.Plane.HORIZONTAL)
            .on(new PartialItemHandler(inventory).insert(SLOT_EXTRA_INPUT_START, SLOT_EXTRA_INPUT_START + 1, SLOT_EXTRA_INPUT_START + 2, SLOT_EXTRA_INPUT_START + 3, SLOT_EXTRA_INPUT_END), Direction.UP);

        sidedFluidInventory = new SidedHandler.Builder<>(inventory);
        sidedFluidInventory
            .on(new PartialFluidHandler(inventory).insert(), Direction.UP)
            .on(new PartialFluidHandler(inventory).extract(), Direction.Plane.HORIZONTAL);

        recipeProxy = new RecipeProxyPotBlockEntity(pos, state);
    }

    private void setSyncedEnergy(int energy)
    {
        energyStorage.deserializeNBT(IntTag.valueOf(Math.max(0, Math.min(ENERGY_CAPACITY, energy))));
    }

    private void syncAutomationChange()
    {
        setChanged();
        needsRecipeUpdate = true;
        cleanupOutputState();
        markForSync();
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
        if (output != null || inventoryOutputReady)
        {
            return;
        }

        if (cachedSpecialRecipe != null && cachedSpecialRecipe.isHotEnough(temperature))
        {
            handleSpecialCooking(cachedSpecialRecipe);
        }
        else if (cachedRecipe != null && cachedRecipe.isHotEnough(temperature))
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

            final int speed = getCookingSpeedMultiplier();
            boilingTicks += speed;
            if (boilingTicks == speed)
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

                if (recipe instanceof SimplePotRecipe simpleRecipe && !simpleRecipe.getDisplayFluid().isEmpty())
                {
                    proxyInventory.getFluidHandler().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                }
                else
                {
                    proxyInventory.getFluidHandler().drain(recipe.getFluidIngredient().amount(), IFluidHandler.FluidAction.EXECUTE);
                }
                for (int slot = SLOT_EXTRA_INPUT_START; slot <= SLOT_EXTRA_INPUT_END; slot++)
                {
                    proxyInventory.setStackInSlot(slot, proxyInventory.getStackInSlot(slot).getCraftingRemainingItem());
                }

                finishedOutput.onFinish(proxyInventory);
                syncFromProxy();

                output = finishedOutput.isEmpty() ? null : finishedOutput;
                inventoryOutputReady = output == null && hasAnyInputItem();
                lastRecipeTemperature = Math.round(readRecipeTemperature(recipe));
                cachedRecipe = null;
                cachedSpecialRecipe = null;
                boilingTicks = 0;
                preBoilingTicks = 0;
                needsRecipeUpdate = true;
                markForSync();
                notifyNearbyJarringStations();
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

    private void handleSpecialCooking(SpecialRecipe recipe)
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

        final int speed = getCookingSpeedMultiplier();
        boilingTicks += speed;
        if (boilingTicks == speed)
        {
            markForSync();
        }

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
        final Fluid fluid = ForgeRegistries.FLUIDS.getValue(FIRMA_LIFE_SUGAR_WATER);
        final int amount = getSugarWaterConversionAmount();
        if (fluid != null && amount > 0)
        {
            consumeSweeteners(amount / SUGAR_WATER_AMOUNT);
            inventory.setFluid(new FluidStack(fluid, amount));
        }

        output = null;
        inventoryOutputReady = false;
    }

    private PotRecipe.Output finishSugarWaterJamRecipe()
    {
        final FruitBatch batch = getSingleFruitBatch();

        if (batch == null)
        {
            return PotRecipe.Output.read(new CompoundTag());
        }

        inventory.getFluidHandler().drain(batch.count * SUGAR_WATER_PER_JAM, IFluidHandler.FluidAction.EXECUTE);
        int remaining = batch.count;
        for (int slot = SLOT_EXTRA_INPUT_START; slot <= SLOT_EXTRA_INPUT_END && remaining > 0; slot++)
        {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getItem() == batch.sample.getItem())
            {
                final int consumed = Math.min(stack.getCount(), remaining);
                stack.shrink(consumed);
                inventory.setStackInSlot(slot, stack);
                remaining -= consumed;
            }
        }

        inventoryOutputReady = false;
        return new JamPotRecipe.JamOutput(batch.jarred, batch.texture);
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
        return getSugarWaterConversionAmount() > 0 && ForgeRegistries.FLUIDS.containsKey(FIRMA_LIFE_SUGAR_WATER);
    }

    private boolean matchesSugarWaterJamRecipe()
    {
        final FluidStack fluid = inventory.getFluidInTank(0);
        final FruitBatch batch = getSingleFruitBatch();
        return fluid.getAmount() >= SUGAR_WATER_PER_JAM
            && isSugarWaterFluid(fluid)
            && batch != null
            && fluid.getAmount() >= batch.count * SUGAR_WATER_PER_JAM;
    }

    private int getSugarWaterConversionAmount()
    {
        final FluidStack fluid = inventory.getFluidInTank(0);
        if (fluid.getAmount() < SUGAR_WATER_AMOUNT || fluid.getAmount() % SUGAR_WATER_AMOUNT != 0 || !fluid.getFluid().isSame(Fluids.WATER))
        {
            return 0;
        }

        final int sweetenerCount = countSweeteners();
        final int requiredSweeteners = fluid.getAmount() / SUGAR_WATER_AMOUNT;
        return sweetenerCount >= requiredSweeteners ? fluid.getAmount() : 0;
    }

    private int countSweeteners()
    {
        int count = 0;
        for (int slot = SLOT_EXTRA_INPUT_START; slot <= SLOT_EXTRA_INPUT_END; slot++)
        {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty())
            {
                continue;
            }
            if (!net.dries007.tfc.util.Helpers.isItem(stack, SWEETENER))
            {
                return -1;
            }
            count += stack.getCount();
        }
        return count;
    }

    private void consumeSweeteners(int amount)
    {
        for (int slot = SLOT_EXTRA_INPUT_START; slot <= SLOT_EXTRA_INPUT_END; slot++)
        {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && net.dries007.tfc.util.Helpers.isItem(stack, SWEETENER))
            {
                final int consumed = Math.min(stack.getCount(), amount);
                stack.shrink(consumed);
                inventory.setStackInSlot(slot, stack);
                amount -= consumed;
                if (amount <= 0)
                {
                    return;
                }
            }
        }
    }

    private @Nullable FruitBatch getSingleFruitBatch()
    {
        ItemStack sample = ItemStack.EMPTY;
        int count = 0;
        final List<ItemStack> previous = new ArrayList<>(INPUT_SLOT_COUNT);

        for (int slot = SLOT_EXTRA_INPUT_START; slot <= SLOT_EXTRA_INPUT_END; slot++)
        {
            final ItemStack stack = inventory.getStackInSlot(slot);
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

        count = Math.min(count, INPUT_SLOT_COUNT);
        final ResourceLocation fruitId = ForgeRegistries.ITEMS.getKey(sample.getItem());
        if (fruitId == null || !fruitId.getPath().startsWith("food/"))
        {
            return null;
        }

        final String fruitName = fruitId.getPath().substring("food/".length());
        final Item jarItem = JamJarCompat.getSealedJamJarItem(fruitId.getNamespace(), fruitName);
        if (jarItem == null)
        {
            return null;
        }

        final ItemStack jarred = new ItemStack(jarItem, count);
        FoodCapability.updateFoodFromAllPrevious(previous, jarred);
        return new FruitBatch(sample, count, jarred, new ResourceLocation(fruitId.getNamespace(), "block/jar/" + fruitName));
    }

    private boolean isUsableJamFruit(ItemStack stack)
    {
        if (!net.dries007.tfc.util.Helpers.isItem(stack, FRUITS) || FoodCapability.isRotten(stack))
        {
            return false;
        }
        final FoodTrait dried = FoodTrait.getTrait(FIRMA_LIFE_DRIED_TRAIT);
        return dried == null || !FoodCapability.hasTrait(stack, dried);
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

        if (output instanceof JamPotRecipe.JamOutput && JamJarCompat.isSupportedEmptyJar(clickedWith))
        {
            final ItemStack jarred = takeJamOutputWithJar(clickedWith);
            if (!jarred.isEmpty())
            {
                clickedWith.shrink(1);
                ItemHandlerHelper.giveItemToPlayer(player, jarred);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        final InteractionResult result = output.onInteract(recipeProxy, player, clickedWith);

        cleanupOutputState();
        markForSync();
        return result;
    }

    public ItemStack tryTakeOutputWithJar(ItemStack jar)
    {
        if (!(output instanceof JamPotRecipe.JamOutput) || !JamJarCompat.isSupportedEmptyJar(jar))
        {
            return ItemStack.EMPTY;
        }
        return takeJamOutputWithJar(jar);
    }

    private ItemStack takeJamOutputWithJar(ItemStack jar)
    {
        if (!(output instanceof JamPotRecipe.JamOutput))
        {
            return ItemStack.EMPTY;
        }

        final CompoundTag outputNbt = PotRecipe.Output.write(output);
        if (!outputNbt.contains("item"))
        {
            return ItemStack.EMPTY;
        }

        final ItemStack outputStack = ItemStack.of(outputNbt.getCompound("item"));
        if (outputStack.isEmpty())
        {
            cleanupOutputState();
            markForSync();
            return ItemStack.EMPTY;
        }

        final ItemStack storedJar = outputStack.copyWithCount(1);
        final ItemStack jarredStack = JamJarCompat.createFilledJar(storedJar, jar);
        if (jarredStack.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        outputStack.shrink(1);
        if (outputStack.isEmpty())
        {
            output = null;
        }
        else
        {
            outputNbt.put("item", outputStack.save(new CompoundTag()));
            output = PotRecipe.Output.read(outputNbt);
        }
        cleanupOutputState();
        needsRecipeUpdate = true;
        setChanged();
        markForSync();
        return jarredStack;
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
        if (!canAcceptManualInput())
        {
            return false;
        }
        return FluidUtil.interactWithFluidHandler(player, hand, inventory.getFluidHandler());
    }

    public boolean isBoiling()
    {
        return output == null && ((cachedSpecialRecipe != null && cachedSpecialRecipe.isHotEnough(temperature)) || (cachedRecipe != null && cachedRecipe.isHotEnough(temperature)));
    }

    public boolean hasRecipeStarted()
    {
        return isBoiling() && preBoilingTicks >= PRE_BOIL_TIME;
    }

    public boolean shouldRenderAsBoiling()
    {
        return isBoiling() || boilingTicks > 0 || preBoilingTicks > 0;
    }

    private int getCookingSpeedMultiplier()
    {
        return SPEED_MULTIPLIER;
    }

    public @Nullable PotRecipe.Output getOutput()
    {
        return output;
    }

    public void setTargetTemperature(int temp)
    {
        targetTemperature = Math.max(0, Math.min(MAX_TEMPERATURE, temp));
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

    public boolean canAcceptManualInput()
    {
        return !hasRecipeStarted() && !hasOutput() && !inventoryOutputReady;
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

    private boolean canAutomationInsertItem(int slot, ItemStack stack)
    {
        return slot >= SLOT_EXTRA_INPUT_START && slot <= SLOT_EXTRA_INPUT_END && canAcceptManualInput();
    }

    private boolean canAutomationExtractItem(int slot)
    {
        return slot >= SLOT_EXTRA_INPUT_START && slot <= SLOT_EXTRA_INPUT_END && inventoryOutputReady;
    }

    private boolean canAutomationFillFluid(FluidStack stack)
    {
        return !stack.isEmpty() && canAcceptManualInput() && net.dries007.tfc.util.Helpers.isFluid(stack.getFluid(), TFCTags.Fluids.USABLE_IN_POT);
    }

    private boolean canAutomationDrainFluid()
    {
        return !hasRecipeStarted() && !hasOutput();
    }

    private boolean isSugarWaterFluid(FluidStack fluid)
    {
        return !fluid.isEmpty() && net.dries007.tfc.util.Helpers.isFluid(fluid.getFluid(), SUGAR_WATER);
    }

    private boolean hasOnlySweetenerInputs()
    {
        boolean foundSweetener = false;
        for (int slot = SLOT_EXTRA_INPUT_START; slot <= SLOT_EXTRA_INPUT_END; slot++)
        {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty())
            {
                continue;
            }
            if (!net.dries007.tfc.util.Helpers.isItem(stack, SWEETENER))
            {
                return false;
            }
            foundSweetener = true;
        }
        return foundSweetener;
    }

    private boolean hasAnyInputItem()
    {
        for (int slot = SLOT_EXTRA_INPUT_START; slot <= SLOT_EXTRA_INPUT_END; slot++)
        {
            if (!inventory.getStackInSlot(slot).isEmpty())
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void loadAdditional(CompoundTag nbt)
    {
        output = nbt.contains("output") ? PotRecipe.Output.read(nbt.getCompound("output")) : null;
        cleanupOutputState();
        boilingTicks = nbt.getInt("boilingTicks");
        preBoilingTicks = nbt.getInt("preBoilingTicks");
        temperature = nbt.getFloat("temperature");
        targetTemperature = Math.max(0, Math.min(MAX_TEMPERATURE, nbt.getInt("targetTemperature")));
        inventoryOutputReady = nbt.getBoolean("inventoryOutputReady");
        lastRecipeTemperature = nbt.getInt("lastRecipeTemperature");
        if (nbt.contains("energy"))
        {
            energyStorage.deserializeNBT(nbt.get("energy"));
        }
        needsRecipeUpdate = true;
        super.loadAdditional(nbt);
        cleanupOutputState();
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
        nbt.putBoolean("inventoryOutputReady", inventoryOutputReady);
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
        if (cap == ForgeCapabilities.ITEM_HANDLER && side != null)
        {
            return automationItemCapability.cast();
        }
        if (cap == Capabilities.FLUID)
        {
            return automationFluidCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCapabilities()
    {
        super.invalidateCapabilities();
        sidedFluidInventory.invalidate();
        energyCapability.invalidate();
        automationItemCapability.invalidate();
        automationFluidCapability.invalidate();
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        sidedFluidInventory.invalidate();
        energyCapability.invalidate();
        automationItemCapability.invalidate();
        automationFluidCapability.invalidate();
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

    private record FruitBatch(ItemStack sample, int count, ItemStack jarred, ResourceLocation texture) {}

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

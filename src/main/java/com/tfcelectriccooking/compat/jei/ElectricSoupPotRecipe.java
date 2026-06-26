package com.tfcelectriccooking.compat.jei;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

public record ElectricSoupPotRecipe(
    List<Ingredient> inputItems,
    FluidStack inputFluid,
    List<ItemStack> outputItems,
    FluidStack outputFluid
) {}

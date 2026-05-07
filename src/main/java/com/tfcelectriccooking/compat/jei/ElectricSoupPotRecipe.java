package com.tfcelectriccooking.compat.jei;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

public record ElectricSoupPotRecipe(
    Component title,
    List<Ingredient> inputItems,
    FluidStack inputFluid,
    List<ItemStack> outputItems,
    FluidStack outputFluid
) {}

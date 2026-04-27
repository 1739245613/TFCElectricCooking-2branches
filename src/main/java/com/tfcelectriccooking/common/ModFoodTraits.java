package com.tfcelectriccooking.common;

import com.tfcelectriccooking.TFCElectricCooking;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.minecraft.resources.ResourceLocation;

public final class ModFoodTraits
{
    public static final FoodTrait ELECTRIC_OVEN_BAKED = FoodTrait.register(
        new ResourceLocation(TFCElectricCooking.MOD_ID, "electric_oven_baked"),
        new FoodTrait(() -> 0.9f, "tfcelectriccooking.tooltip.food_trait.electric_oven_baked")
    );

    private ModFoodTraits() {}

    public static void init() {}
}

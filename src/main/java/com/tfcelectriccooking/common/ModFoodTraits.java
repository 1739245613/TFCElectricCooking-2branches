package com.tfcelectriccooking.common;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.dries007.tfc.common.component.food.FoodTrait;
import net.dries007.tfc.common.component.food.FoodTraits;
import com.tfcelectriccooking.TFCElectricCooking;

public class ModFoodTraits
{
    public static final DeferredRegister<FoodTrait> TRAITS = DeferredRegister.create(FoodTraits.KEY, TFCElectricCooking.MOD_ID);

    public static final DeferredHolder<FoodTrait, FoodTrait> ELECTRIC_OVEN_BAKED = TRAITS.register("electric_oven_baked",
        () -> new FoodTrait(() -> 0.9d, "tfcelectriccooking.tooltip.food_trait.electric_oven_baked"));

    public static void register(IEventBus bus)
    {
        TRAITS.register(bus);
    }
}

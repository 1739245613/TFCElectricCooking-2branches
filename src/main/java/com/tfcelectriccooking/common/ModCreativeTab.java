package com.tfcelectriccooking.common;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.tfcelectriccooking.TFCElectricCooking;

public class ModCreativeTab
{
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TFCElectricCooking.MOD_ID);

    public static final Supplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.tfcelectriccooking"))
        .icon(() -> new ItemStack(ModBlocks.ELECTRIC_OVEN_ITEM.get()))
        .displayItems((params, output) -> {
            output.accept(ModBlocks.ELECTRIC_OVEN_ITEM.get());
            output.accept(ModBlocks.ELECTRIC_SOUP_POT_ITEM.get());
        })
        .build());

    public static void register(IEventBus bus)
    {
        TABS.register(bus);
    }
}

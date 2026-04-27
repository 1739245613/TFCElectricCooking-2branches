package com.tfcelectriccooking.common;

import com.tfcelectriccooking.TFCElectricCooking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTab
{
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TFCElectricCooking.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.tfcelectriccooking"))
        .icon(() -> new ItemStack(ModBlocks.ELECTRIC_OVEN_ITEM.get()))
        .displayItems((parameters, output) -> {
            output.accept(ModBlocks.ELECTRIC_OVEN_ITEM.get());
            output.accept(ModBlocks.ELECTRIC_SOUP_POT_ITEM.get());
        })
        .build());

    private ModCreativeTab() {}

    public static void register(IEventBus bus)
    {
        TABS.register(bus);
    }
}

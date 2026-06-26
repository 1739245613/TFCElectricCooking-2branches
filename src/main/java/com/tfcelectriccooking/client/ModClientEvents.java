package com.tfcelectriccooking.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.ModContainerTypes;
import com.tfcelectriccooking.client.render.ElectricOvenBlockEntityRenderer;
import com.tfcelectriccooking.client.render.ElectricSoupPotBlockEntityRenderer;
import com.tfcelectriccooking.client.screen.ElectricOvenScreen;
import com.tfcelectriccooking.client.screen.ElectricSoupPotScreen;

public class ModClientEvents
{
    public static void register(IEventBus modBus)
    {
        modBus.addListener(ModClientEvents::registerScreens);
        modBus.addListener(ModClientEvents::registerRenderers);
    }

    private static void registerScreens(RegisterMenuScreensEvent event)
    {
        event.register(ModContainerTypes.ELECTRIC_OVEN.get(), ElectricOvenScreen::new);
        event.register(ModContainerTypes.ELECTRIC_SOUP_POT.get(), ElectricSoupPotScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(ModBlocks.ELECTRIC_OVEN_BLOCK_ENTITY.get(), ElectricOvenBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.ELECTRIC_SOUP_POT_BLOCK_ENTITY.get(), ElectricSoupPotBlockEntityRenderer::new);
    }
}

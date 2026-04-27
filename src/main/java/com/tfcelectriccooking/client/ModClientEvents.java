package com.tfcelectriccooking.client;

import com.tfcelectriccooking.client.render.ElectricSoupPotBlockEntityRenderer;
import com.tfcelectriccooking.client.screen.ElectricOvenScreen;
import com.tfcelectriccooking.client.screen.ElectricSoupPotScreen;
import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.ModContainerTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ModClientEvents
{
    private ModClientEvents() {}

    public static void register(IEventBus modBus)
    {
        modBus.addListener(ModClientEvents::clientSetup);
        modBus.addListener(ModClientEvents::registerRenderers);
    }

    private static void clientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> {
            MenuScreens.register(ModContainerTypes.ELECTRIC_OVEN.get(), ElectricOvenScreen::new);
            MenuScreens.register(ModContainerTypes.ELECTRIC_SOUP_POT.get(), ElectricSoupPotScreen::new);
        });
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        BlockEntityRenderers.register(ModBlocks.ELECTRIC_SOUP_POT_BLOCK_ENTITY.get(), ElectricSoupPotBlockEntityRenderer::new);
    }
}

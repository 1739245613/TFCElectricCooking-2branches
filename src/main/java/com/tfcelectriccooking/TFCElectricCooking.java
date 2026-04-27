package com.tfcelectriccooking;

import com.tfcelectriccooking.client.ModClientEvents;
import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.ModContainerTypes;
import com.tfcelectriccooking.common.ModCreativeTab;
import com.tfcelectriccooking.common.ModFoodTraits;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(TFCElectricCooking.MOD_ID)
public final class TFCElectricCooking
{
    public static final String MOD_ID = "tfcelectriccooking";

    public TFCElectricCooking()
    {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModFoodTraits.init();
        ModBlocks.register(modBus);
        ModContainerTypes.register(modBus);
        ModCreativeTab.register(modBus);

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            ModClientEvents.register(modBus);
        }
    }
}

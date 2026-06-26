package com.tfcelectriccooking;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.ModContainerTypes;
import com.tfcelectriccooking.common.ModCreativeTab;
import com.tfcelectriccooking.common.ModFoodTraits;
import com.tfcelectriccooking.common.ModSounds;
import com.tfcelectriccooking.client.ModClientEvents;

@Mod(TFCElectricCooking.MOD_ID)
public class TFCElectricCooking
{
    public static final String MOD_ID = "tfcelectriccooking";

    public TFCElectricCooking(IEventBus modBus)
    {
        ModBlocks.register(modBus);
        ModContainerTypes.register(modBus);
        ModCreativeTab.register(modBus);
        ModFoodTraits.register(modBus);
        ModSounds.register(modBus);

        if (FMLEnvironment.dist.isClient())
        {
            ModClientEvents.register(modBus);
        }

        modBus.addListener(com.tfcelectriccooking.common.ModCapabilities::register);
    }
}

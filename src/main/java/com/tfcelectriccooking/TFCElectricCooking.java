package com.tfcelectriccooking;

import com.tfcelectriccooking.client.ModClientEvents;
import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.ModConfig;
import com.tfcelectriccooking.common.ModContainerTypes;
import com.tfcelectriccooking.common.ModCreativeTab;
import com.tfcelectriccooking.common.ModFoodTraits;
import com.tfcelectriccooking.common.ModSounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(TFCElectricCooking.MOD_ID)
public final class TFCElectricCooking
{
    public static final String MOD_ID = "tfcelectriccooking";

    public TFCElectricCooking()
    {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.COMMON_SPEC);

        ModFoodTraits.init();
        ModBlocks.register(modBus);
        ModContainerTypes.register(modBus);
        ModCreativeTab.register(modBus);
        ModSounds.register(modBus);

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            ModClientEvents.register(modBus);
        }
    }
}

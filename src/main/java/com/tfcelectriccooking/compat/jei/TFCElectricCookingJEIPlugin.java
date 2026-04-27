package com.tfcelectriccooking.compat.jei;

import com.eerussianguy.firmalife.compat.jei.FLJEIPlugin;
import com.tfcelectriccooking.TFCElectricCooking;
import com.tfcelectriccooking.common.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.dries007.tfc.compat.jei.JEIIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class TFCElectricCookingJEIPlugin implements IModPlugin
{
    @Override
    public ResourceLocation getPluginUid()
    {
        return ResourceLocation.fromNamespaceAndPath(TFCElectricCooking.MOD_ID, "jei");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registry)
    {
        final ItemStack oven = new ItemStack(ModBlocks.ELECTRIC_OVEN.get());
        final ItemStack soupPot = new ItemStack(ModBlocks.ELECTRIC_SOUP_POT.get());

        registry.addRecipeCatalyst(oven, JEIIntegration.HEATING);
        registry.addRecipeCatalyst(oven, FLJEIPlugin.OVEN);

        registry.addRecipeCatalyst(soupPot, JEIIntegration.SOUP_POT);
        registry.addRecipeCatalyst(soupPot, JEIIntegration.SIMPLE_POT);
        registry.addRecipeCatalyst(soupPot, JEIIntegration.JAM_POT);
        registry.addRecipeCatalyst(soupPot, FLJEIPlugin.BOWL_POT);
    }
}

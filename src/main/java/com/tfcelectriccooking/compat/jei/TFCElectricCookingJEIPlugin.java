package com.tfcelectriccooking.compat.jei;

import com.tfcelectriccooking.TFCElectricCooking;
import com.tfcelectriccooking.common.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.dries007.tfc.common.recipes.PotRecipe;
import net.dries007.tfc.compat.jei.JEIIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

@JeiPlugin
public final class TFCElectricCookingJEIPlugin implements IModPlugin
{
    private static final RecipeType<PotRecipe> FIRMA_LIFE_BOWL_POT = RecipeType.create("firmalife", "bowl_pot", PotRecipe.class);
    private static final RecipeType<PotRecipe> FIRMA_LIFE_STINKY_SOUP = RecipeType.create("firmalife", "stinky_soup", PotRecipe.class);

    @Override
    public ResourceLocation getPluginUid()
    {
        return new ResourceLocation(TFCElectricCooking.MOD_ID, "jei");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registry)
    {
        final ItemStack oven = new ItemStack(ModBlocks.ELECTRIC_OVEN.get());
        final ItemStack soupPot = new ItemStack(ModBlocks.ELECTRIC_SOUP_POT.get());

        registry.addRecipeCatalyst(oven, JEIIntegration.HEATING);

        registry.addRecipeCatalyst(soupPot, JEIIntegration.SOUP_POT);
        registry.addRecipeCatalyst(soupPot, JEIIntegration.SIMPLE_POT);
        registry.addRecipeCatalyst(soupPot, JEIIntegration.JAM_POT);

        final RecipeType<?> firmalifeOven = getFirmaLifeOvenType();
        if (firmalifeOven != null)
        {
            registry.addRecipeCatalyst(oven, firmalifeOven);
        }
        if (ModList.get().isLoaded("firmalife"))
        {
            registry.addRecipeCatalyst(soupPot, FIRMA_LIFE_BOWL_POT);
            registry.addRecipeCatalyst(soupPot, FIRMA_LIFE_STINKY_SOUP);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static @Nullable RecipeType<?> getFirmaLifeOvenType()
    {
        if (!ModList.get().isLoaded("firmalife"))
        {
            return null;
        }

        try
        {
            final Class ovenRecipeClass = Class.forName("com.eerussianguy.firmalife.common.recipes.OvenRecipe");
            return RecipeType.create("firmalife", "oven", ovenRecipeClass);
        }
        catch (ReflectiveOperationException ignored)
        {
            return null;
        }
    }
}

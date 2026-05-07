package com.tfcelectriccooking.compat.jei;

import com.tfcelectriccooking.TFCElectricCooking;
import com.tfcelectriccooking.common.ModBlocks;
import com.tfcelectriccooking.common.compat.JamJarCompat;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.dries007.tfc.common.recipes.PotRecipe;
import net.dries007.tfc.common.recipes.ingredients.LacksTraitIngredient;
import net.dries007.tfc.common.recipes.ingredients.NotRottenIngredient;
import net.dries007.tfc.compat.jei.JEIIntegration;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

@JeiPlugin
public final class TFCElectricCookingJEIPlugin implements IModPlugin
{
    static final RecipeType<ElectricSoupPotRecipe> ELECTRIC_SOUP_POT = RecipeType.create(TFCElectricCooking.MOD_ID, "electric_soup_pot", ElectricSoupPotRecipe.class);

    private static final ResourceLocation FIRMA_LIFE_DRIED_TRAIT = new ResourceLocation("firmalife", "dried");
    private static final ResourceLocation FIRMA_LIFE_SUGAR_WATER = new ResourceLocation("firmalife", "sugar_water");
    private static final TagKey<Item> SWEETENER = TagKey.create(Registries.ITEM, new ResourceLocation("tfc", "sweetener"));
    private static final TagKey<Item> FRUITS = TagKey.create(Registries.ITEM, new ResourceLocation("tfc", "foods/fruits"));
    private static final RecipeType<PotRecipe> FIRMA_LIFE_BOWL_POT = RecipeType.create("firmalife", "bowl_pot", PotRecipe.class);
    private static final RecipeType<PotRecipe> FIRMA_LIFE_STINKY_SOUP = RecipeType.create("firmalife", "stinky_soup", PotRecipe.class);

    @Override
    public ResourceLocation getPluginUid()
    {
        return new ResourceLocation(TFCElectricCooking.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry)
    {
        registry.addRecipeCategories(new ElectricSoupPotRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registry)
    {
        final List<ElectricSoupPotRecipe> recipes = createElectricSoupPotRecipes();
        if (!recipes.isEmpty())
        {
            registry.addRecipes(ELECTRIC_SOUP_POT, recipes);
        }
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
        registry.addRecipeCatalyst(soupPot, ELECTRIC_SOUP_POT);

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

    private static List<ElectricSoupPotRecipe> createElectricSoupPotRecipes()
    {
        final Fluid sugarWater = ForgeRegistries.FLUIDS.getValue(FIRMA_LIFE_SUGAR_WATER);
        if (!ModList.get().isLoaded("firmalife") || sugarWater == null)
        {
            return List.of();
        }

        final List<ElectricSoupPotRecipe> recipes = new ArrayList<>();
        recipes.add(new ElectricSoupPotRecipe(
            Component.translatable("tfcelectriccooking.jei.sugar_water"),
            List.of(Ingredient.of(SWEETENER)),
            new FluidStack(Fluids.WATER, 1000),
            List.of(),
            new FluidStack(sugarWater, 1000)
        ));

        Helpers.allItems(FRUITS).forEach(foodItem -> addSugarWaterJamRecipes(recipes, sugarWater, foodItem));
        return recipes;
    }

    private static void addSugarWaterJamRecipes(List<ElectricSoupPotRecipe> recipes, Fluid sugarWater, Item foodItem)
    {
        final ResourceLocation foodId = ForgeRegistries.ITEMS.getKey(foodItem);
        if (foodId == null || !foodId.getPath().startsWith("food/"))
        {
            return;
        }

        final String fruitName = foodId.getPath().substring("food/".length());
        final Ingredient fruit = nonDriedFruitIngredient(foodItem);
        for (int count = 1; count <= 5; count++)
        {
            final List<ItemStack> outputs = JamJarCompat.getJeiResults(foodId.getNamespace(), fruitName, count);
            if (outputs.isEmpty())
            {
                continue;
            }

            final List<Ingredient> ingredients = new ArrayList<>(count);
            for (int i = 0; i < count; i++)
            {
                ingredients.add(fruit);
            }
            recipes.add(new ElectricSoupPotRecipe(
                Component.translatable("tfcelectriccooking.jei.sugar_water_jam"),
                ingredients,
                new FluidStack(sugarWater, 1000),
                outputs,
                FluidStack.EMPTY
            ));
        }
    }

    private static Ingredient nonDriedFruitIngredient(Item item)
    {
        final Ingredient base = Ingredient.of(item);
        final FoodTrait dried = FoodTrait.getTrait(FIRMA_LIFE_DRIED_TRAIT);
        return NotRottenIngredient.of(dried == null ? base : LacksTraitIngredient.of(base, dried));
    }
}

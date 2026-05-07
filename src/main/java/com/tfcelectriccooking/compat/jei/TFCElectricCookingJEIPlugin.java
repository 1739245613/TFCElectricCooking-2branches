package com.tfcelectriccooking.compat.jei;

import com.eerussianguy.firmalife.compat.jei.FLJEIPlugin;
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
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.component.food.FoodTrait;
import net.dries007.tfc.common.component.food.FoodTraits;
import net.dries007.tfc.common.recipes.ingredients.AndIngredient;
import net.dries007.tfc.common.recipes.ingredients.LacksTraitIngredient;
import net.dries007.tfc.common.recipes.ingredients.NotRottenIngredient;
import net.dries007.tfc.compat.jei.JEIIntegration;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

@JeiPlugin
public final class TFCElectricCookingJEIPlugin implements IModPlugin
{
    static final RecipeType<ElectricSoupPotRecipe> ELECTRIC_SOUP_POT = RecipeType.create(TFCElectricCooking.MOD_ID, "electric_soup_pot", ElectricSoupPotRecipe.class);

    private static final ResourceLocation FIRMA_LIFE_DRIED_TRAIT = ResourceLocation.fromNamespaceAndPath("firmalife", "dried");
    private static final ResourceLocation FIRMA_LIFE_SUGAR_WATER = ResourceLocation.fromNamespaceAndPath("firmalife", "sugar_water");

    @Override
    public ResourceLocation getPluginUid()
    {
        return ResourceLocation.fromNamespaceAndPath(TFCElectricCooking.MOD_ID, "jei");
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
        registry.addRecipeCatalyst(oven, FLJEIPlugin.OVEN);

        registry.addRecipeCatalyst(soupPot, JEIIntegration.SOUP_POT);
        registry.addRecipeCatalyst(soupPot, JEIIntegration.SIMPLE_POT);
        registry.addRecipeCatalyst(soupPot, JEIIntegration.JAM_POT);
        registry.addRecipeCatalyst(soupPot, FLJEIPlugin.BOWL_POT);
        registry.addRecipeCatalyst(soupPot, FLJEIPlugin.STINKY_SOUP);
        registry.addRecipeCatalyst(soupPot, ELECTRIC_SOUP_POT);
    }

    private static List<ElectricSoupPotRecipe> createElectricSoupPotRecipes()
    {
        final Fluid sugarWater = BuiltInRegistries.FLUID.get(FIRMA_LIFE_SUGAR_WATER);
        if (!BuiltInRegistries.FLUID.containsKey(FIRMA_LIFE_SUGAR_WATER))
        {
            return List.of();
        }

        final List<ElectricSoupPotRecipe> recipes = new ArrayList<>();
        recipes.add(new ElectricSoupPotRecipe(
            Component.translatable("tfcelectriccooking.jei.sugar_water"),
            List.of(Ingredient.of(TFCTags.Items.SWEETENERS)),
            new FluidStack(Fluids.WATER, 1000),
            List.of(),
            new FluidStack(sugarWater, 1000)
        ));

        Helpers.allItems(TFCTags.Items.FRUITS).forEach(foodItem -> addSugarWaterJamRecipes(recipes, sugarWater, foodItem));
        return recipes;
    }

    private static void addSugarWaterJamRecipes(List<ElectricSoupPotRecipe> recipes, Fluid sugarWater, Item foodItem)
    {
        final ResourceLocation foodId = BuiltInRegistries.ITEM.getKey(foodItem);
        if (!foodId.getPath().startsWith("food/"))
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
        final Holder<FoodTrait> dried = getFoodTrait(FIRMA_LIFE_DRIED_TRAIT);
        return dried == null
            ? AndIngredient.of(base, NotRottenIngredient.INSTANCE)
            : AndIngredient.of(base, NotRottenIngredient.INSTANCE, LacksTraitIngredient.of(dried));
    }

    private static @Nullable Holder<FoodTrait> getFoodTrait(ResourceLocation id)
    {
        return FoodTraits.REGISTRY.getHolder(ResourceKey.create(FoodTraits.KEY, id)).orElse(null);
    }
}

package com.tfcelectriccooking.compat.firmalife;

import java.lang.reflect.Method;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.common.recipes.inventory.ItemStackInventory;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class FirmaLifeCompat
{
    private static final String WRAPPED_RECIPE_CLASS = "com.eerussianguy.firmalife.common.recipes.WrappedHeatingRecipe";

    private static boolean reflectionInitialized;
    private static @Nullable Method getRecipeMethod;
    private static @Nullable Method idMethod;
    private static @Nullable Method temperatureMethod;
    private static @Nullable Method durationMethod;
    private static @Nullable Method assembleMethod;

    private FirmaLifeCompat() {}

    @Nullable
    public static WrappedHeatingRecipe getRecipe(ItemStack stack)
    {
        final HeatingRecipe tfcRecipe = HeatingRecipe.getRecipe(stack);
        if (tfcRecipe != null)
        {
            return WrappedHeatingRecipe.of(tfcRecipe);
        }

        if (!ModList.get().isLoaded("firmalife") || !initReflection())
        {
            return null;
        }

        try
        {
            final Object recipe = getRecipeMethod.invoke(null, stack);
            if (recipe == null)
            {
                return null;
            }

            return new WrappedHeatingRecipe(
                (ResourceLocation) idMethod.invoke(recipe),
                ((Number) temperatureMethod.invoke(recipe)).floatValue(),
                ((Number) durationMethod.invoke(recipe)).intValue(),
                (inventory, access) -> {
                    try
                    {
                        return (ItemStack) assembleMethod.invoke(recipe, inventory, access);
                    }
                    catch (ReflectiveOperationException ignored)
                    {
                        return ItemStack.EMPTY;
                    }
                }
            );
        }
        catch (ReflectiveOperationException ignored)
        {
            return null;
        }
    }

    private static boolean initReflection()
    {
        if (reflectionInitialized)
        {
            return getRecipeMethod != null && idMethod != null && temperatureMethod != null && durationMethod != null && assembleMethod != null;
        }

        reflectionInitialized = true;
        try
        {
            final Class<?> wrappedRecipeClass = Class.forName(WRAPPED_RECIPE_CLASS);
            getRecipeMethod = wrappedRecipeClass.getMethod("getRecipe", ItemStack.class);
            idMethod = wrappedRecipeClass.getMethod("id");
            temperatureMethod = wrappedRecipeClass.getMethod("temperature");
            durationMethod = wrappedRecipeClass.getMethod("duration");
            assembleMethod = wrappedRecipeClass.getMethod("assemble", ItemStackInventory.class, RegistryAccess.class);
        }
        catch (ReflectiveOperationException ignored)
        {
            getRecipeMethod = null;
            idMethod = null;
            temperatureMethod = null;
            durationMethod = null;
            assembleMethod = null;
        }
        return getRecipeMethod != null && idMethod != null && temperatureMethod != null && durationMethod != null && assembleMethod != null;
    }

    public record WrappedHeatingRecipe(ResourceLocation id, float temperature, int duration, Assembler assembler)
    {
        public static WrappedHeatingRecipe of(HeatingRecipe recipe)
        {
            return new WrappedHeatingRecipe(recipe.getId(), recipe.getTemperature(), 20 * 50, recipe::assemble);
        }

        public boolean isValidTemperature(float value)
        {
            return value >= temperature;
        }

        public ItemStack assemble(ItemStack stack, RegistryAccess access)
        {
            return assembler.assemble(new ItemStackInventory(stack.copy()), access);
        }
    }

    @FunctionalInterface
    public interface Assembler
    {
        ItemStack assemble(ItemStackInventory inventory, RegistryAccess access);
    }
}

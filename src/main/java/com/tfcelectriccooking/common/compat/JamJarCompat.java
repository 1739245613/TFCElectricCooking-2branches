package com.tfcelectriccooking.common.compat;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class JamJarCompat
{
    private JamJarCompat()
    {
    }

    public static boolean isEmptyJar(ItemStack stack)
    {
        return !stack.isEmpty() && Helpers.isItem(stack, TFCItems.EMPTY_JAR);
    }

    public static boolean isEmptyJarWithLid(ItemStack stack)
    {
        return !stack.isEmpty() && Helpers.isItem(stack, TFCTags.Items.EMPTY_JARS_WITH_LID);
    }

    public static boolean isSupportedEmptyJar(ItemStack stack)
    {
        return isEmptyJar(stack) || isEmptyJarWithLid(stack);
    }

    public static @Nullable Item getUnsealedJamJarItem(String namespace, String fruitName)
    {
        return getItem(ResourceLocation.fromNamespaceAndPath(namespace, "jar/" + fruitName + "_unsealed"));
    }

    public static @Nullable Item getSealedJamJarItem(String namespace, String fruitName)
    {
        return getItem(ResourceLocation.fromNamespaceAndPath(namespace, "jar/" + fruitName));
    }

    public static List<ItemStack> getJeiResults(String namespace, String fruitName, int count)
    {
        final Item unsealed = getUnsealedJamJarItem(namespace, fruitName);
        final Item sealed = getSealedJamJarItem(namespace, fruitName);
        final List<ItemStack> results = new ArrayList<>(2);
        if (unsealed != null)
        {
            results.add(new ItemStack(unsealed, count));
        }
        if (sealed != null)
        {
            results.add(new ItemStack(sealed, count));
        }
        return results;
    }

    private static @Nullable Item getItem(ResourceLocation id)
    {
        return BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }
}

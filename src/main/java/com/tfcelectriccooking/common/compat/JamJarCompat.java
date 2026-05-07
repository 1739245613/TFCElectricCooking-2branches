package com.tfcelectriccooking.common.compat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public final class JamJarCompat
{
    private static final String LITHIC_ADDON = "lithicaddon";

    private static final ResourceLocation TFC_EMPTY_JAR = new ResourceLocation("tfc", "empty_jar");
    private static final ResourceLocation TFC_EMPTY_JAR_WITH_LID = new ResourceLocation("tfc", "empty_jar_with_lid");
    private static final ResourceLocation FIRMALIFE_EMPTY_STAINLESS_JAR = new ResourceLocation("firmalife", "empty_jar_with_stainless_steel_lid");
    private static final ResourceLocation LITHIC_EMPTY_ALUMINUM_JAR = new ResourceLocation(LITHIC_ADDON, "empty_jar_with_aluminum_lid");
    private static final ResourceLocation LITHIC_EMPTY_STAINLESS_JAR = new ResourceLocation(LITHIC_ADDON, "empty_jar_with_stainless_steel_lid");

    private JamJarCompat()
    {
    }

    public static boolean isSupportedEmptyJar(ItemStack stack)
    {
        return getJarKind(stack) != null;
    }

    public static @Nullable Item getSealedJamJarItem(String namespace, String fruitName)
    {
        return getItem(new ResourceLocation(namespace, "jar/" + fruitName));
    }

    public static ItemStack createFilledJar(ItemStack storedJam, ItemStack emptyJar)
    {
        final JarKind kind = getJarKind(emptyJar);
        if (kind == null || storedJam.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        final ResourceLocation storedId = ForgeRegistries.ITEMS.getKey(storedJam.getItem());
        final String fruitName = getFruitName(storedId);
        if (storedId == null || fruitName == null)
        {
            return ItemStack.EMPTY;
        }

        final Item targetItem = getTargetJarItem(storedId.getNamespace(), fruitName, kind);
        if (targetItem == null)
        {
            return ItemStack.EMPTY;
        }

        final ItemStack result = new ItemStack(targetItem);
        if (storedJam.hasTag())
        {
            result.setTag(storedJam.getTag().copy());
        }
        return result;
    }

    public static List<ItemStack> getJeiResults(String namespace, String fruitName, int count)
    {
        final List<ItemStack> results = new ArrayList<>(4);
        addResult(results, getSealedJamJarItem(namespace, fruitName), count);
        addResult(results, getItem(new ResourceLocation(namespace, "jar/" + fruitName + "_unsealed")), count);

        if (isLithicLoaded())
        {
            addResult(results, getItem(new ResourceLocation(LITHIC_ADDON, "aluminum_jar/" + fruitName)), count);
            addResult(results, getItem(new ResourceLocation(LITHIC_ADDON, "stainless_steel_jar/" + fruitName)), count);
        }

        return results;
    }

    private static void addResult(List<ItemStack> results, @Nullable Item item, int count)
    {
        if (item != null)
        {
            results.add(new ItemStack(item, count));
        }
    }

    private static @Nullable Item getTargetJarItem(String namespace, String fruitName, JarKind kind)
    {
        return switch (kind)
            {
                case UNSEALED -> getItem(new ResourceLocation(namespace, "jar/" + fruitName + "_unsealed"));
                case SEALED -> getSealedJamJarItem(namespace, fruitName);
                case LITHIC_ALUMINUM -> getItem(new ResourceLocation(LITHIC_ADDON, "aluminum_jar/" + fruitName));
                case LITHIC_STAINLESS -> getItem(new ResourceLocation(LITHIC_ADDON, "stainless_steel_jar/" + fruitName));
            };
    }

    private static @Nullable JarKind getJarKind(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return null;
        }

        if (isItem(stack, TFC_EMPTY_JAR))
        {
            return JarKind.UNSEALED;
        }
        if (isItem(stack, TFC_EMPTY_JAR_WITH_LID))
        {
            return JarKind.SEALED;
        }
        if (isItem(stack, LITHIC_EMPTY_ALUMINUM_JAR))
        {
            return JarKind.LITHIC_ALUMINUM;
        }
        if (isItem(stack, LITHIC_EMPTY_STAINLESS_JAR) || isItem(stack, FIRMALIFE_EMPTY_STAINLESS_JAR))
        {
            return isLithicLoaded() ? JarKind.LITHIC_STAINLESS : JarKind.SEALED;
        }
        return null;
    }

    private static @Nullable String getFruitName(@Nullable ResourceLocation id)
    {
        if (id == null || !id.getPath().startsWith("jar/"))
        {
            return null;
        }
        final String name = id.getPath().substring("jar/".length());
        return name.endsWith("_unsealed") ? name.substring(0, name.length() - "_unsealed".length()) : name;
    }

    private static boolean isLithicLoaded()
    {
        return ModList.get().isLoaded(LITHIC_ADDON);
    }

    private static boolean isItem(ItemStack stack, ResourceLocation id)
    {
        final Item item = getItem(id);
        return item != null && stack.is(item);
    }

    private static @Nullable Item getItem(ResourceLocation id)
    {
        final Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null || item == Items.AIR ? null : item;
    }

    private enum JarKind
    {
        UNSEALED,
        SEALED,
        LITHIC_ALUMINUM,
        LITHIC_STAINLESS
    }
}

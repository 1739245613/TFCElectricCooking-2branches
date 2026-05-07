package com.tfcelectriccooking.compat.jei;

import com.tfcelectriccooking.common.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public final class ElectricSoupPotRecipeCategory implements IRecipeCategory<ElectricSoupPotRecipe>
{
    private static final Component TITLE = Component.translatable("tfcelectriccooking.jei.electric_soup_pot");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;

    public ElectricSoupPotRecipeCategory(IGuiHelper helper)
    {
        background = helper.createBlankDrawable(176, 74);
        icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.ELECTRIC_SOUP_POT.get()));
        slot = helper.getSlotDrawable();
    }

    @Override
    public RecipeType<ElectricSoupPotRecipe> getRecipeType()
    {
        return TFCElectricCookingJEIPlugin.ELECTRIC_SOUP_POT;
    }

    @Override
    public Component getTitle()
    {
        return TITLE;
    }

    @Override
    public IDrawable getBackground()
    {
        return background;
    }

    @Override
    public IDrawable getIcon()
    {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ElectricSoupPotRecipe recipe, IFocusGroup focuses)
    {
        builder.addSlot(RecipeIngredientRole.CATALYST, 6, 29)
            .setBackground(slot, -1, -1)
            .addItemStack(new ItemStack(ModBlocks.ELECTRIC_SOUP_POT.get()));

        final int inputs = Math.min(recipe.inputItems().size(), 5);
        for (int i = 0; i < inputs; i++)
        {
            builder.addSlot(RecipeIngredientRole.INPUT, 34 + i * 19, 7)
                .setBackground(slot, -1, -1)
                .addIngredients(recipe.inputItems().get(i));
        }

        final FluidStack inputFluid = recipe.inputFluid();
        if (!inputFluid.isEmpty())
        {
            builder.addSlot(RecipeIngredientRole.INPUT, 63, 38)
                .setBackground(slot, -1, -1)
                .setFluidRenderer(1000, false, 16, 16)
                .addFluidStack(inputFluid.getFluid(), inputFluid.getAmount());
        }

        if (!recipe.outputItems().isEmpty())
        {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 151, 7)
                .setBackground(slot, -1, -1)
                .addItemStacks(recipe.outputItems());
        }

        final FluidStack outputFluid = recipe.outputFluid();
        if (!outputFluid.isEmpty())
        {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 151, 38)
                .setBackground(slot, -1, -1)
                .setFluidRenderer(1000, false, 16, 16)
                .addFluidStack(outputFluid.getFluid(), outputFluid.getAmount());
        }
    }

    @Override
    public void draw(ElectricSoupPotRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY)
    {
        final Minecraft minecraft = Minecraft.getInstance();
        graphics.drawString(minecraft.font, recipe.title(), 34, 61, 0x404040, false);
        graphics.drawString(minecraft.font, ">", 126, 30, 0x606060, false);
    }
}

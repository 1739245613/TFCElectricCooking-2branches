package com.tfcelectriccooking.compat.jei;

import com.tfcelectriccooking.common.ModBlocks;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.dries007.tfc.compat.jei.category.BaseRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public final class ElectricSoupPotRecipeCategory implements IRecipeCategory<ElectricSoupPotRecipe>
{
    private static final Component TITLE = Component.translatable("tfcelectriccooking.jei.electric_soup_pot");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawableStatic arrow;
    private final IDrawableAnimated arrowAnimated;

    private @Nullable IRecipeSlotBuilder inputFluidSlot;
    private @Nullable IRecipeSlotBuilder inputItemSlot;
    private @Nullable IRecipeSlotBuilder outputFluidSlot;
    private @Nullable IRecipeSlotBuilder outputItemSlot;

    public ElectricSoupPotRecipeCategory(IGuiHelper helper)
    {
        background = helper.createBlankDrawable(118, 26);
        icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.ELECTRIC_SOUP_POT.get()));
        slot = helper.getSlotDrawable();
        arrow = helper.createDrawable(BaseRecipeCategory.ICONS, 0, 14, 22, 16);
        final IDrawableStatic arrowAnimation = helper.createDrawable(BaseRecipeCategory.ICONS, 22, 14, 22, 16);
        arrowAnimated = helper.createAnimatedDrawable(arrowAnimation, 80, IDrawableAnimated.StartDirection.LEFT, false);
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
        inputFluidSlot = null;
        inputItemSlot = null;
        outputFluidSlot = null;
        outputItemSlot = null;

        final int[] positions = slotPositions();
        final FluidStack inputFluid = recipe.inputFluid();
        if (!inputFluid.isEmpty())
        {
            inputFluidSlot = builder.addSlot(RecipeIngredientRole.INPUT, recipe.inputItems().isEmpty() ? positions[1] : positions[0], 5);
            inputFluidSlot.addFluidStack(inputFluid.getFluid(), inputFluid.getAmount());
            inputFluidSlot.setFluidRenderer(1, false, 16, 16);
            inputFluidSlot.setBackground(slot, -1, -1);
        }

        if (!recipe.inputItems().isEmpty())
        {
            inputItemSlot = builder.addSlot(RecipeIngredientRole.INPUT, positions[1], 5);
            inputItemSlot.addIngredients(recipe.inputItems().get(0));
            inputItemSlot.setBackground(slot, -1, -1);
        }

        final FluidStack outputFluid = recipe.outputFluid();
        if (!outputFluid.isEmpty())
        {
            outputFluidSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, positions[2], 5);
            outputFluidSlot.addFluidStack(outputFluid.getFluid(), outputFluid.getAmount());
            outputFluidSlot.setFluidRenderer(1, false, 16, 16);
            outputFluidSlot.setBackground(slot, -1, -1);
        }

        final List<ItemStack> outputItems = recipe.outputItems();
        if (!outputItems.isEmpty() && !outputItems.stream().allMatch(ItemStack::isEmpty))
        {
            outputItemSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, outputFluid.isEmpty() ? positions[2] : positions[3], 5);
            outputItemSlot.addItemStacks(outputItems);
            outputItemSlot.setBackground(slot, -1, -1);
        }
    }

    @Override
    public void draw(ElectricSoupPotRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY)
    {
        arrow.draw(graphics, 48, 5);
        arrowAnimated.draw(graphics, 48, 5);
    }

    private int[] slotPositions()
    {
        return new int[] {6, 26, 76, 96};
    }
}

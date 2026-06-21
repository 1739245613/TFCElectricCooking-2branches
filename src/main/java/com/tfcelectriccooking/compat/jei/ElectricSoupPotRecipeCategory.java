package com.tfcelectriccooking.compat.jei;

import com.tfcelectriccooking.common.ModBlocks;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.dries007.tfc.compat.jei.JEIIntegration;
import net.dries007.tfc.compat.jei.category.BaseRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public final class ElectricSoupPotRecipeCategory extends BaseRecipeCategory<ElectricSoupPotRecipe>
{
    private static final Component TITLE = Component.translatable("tfcelectriccooking.jei.electric_soup_pot");

    private @Nullable IRecipeSlotBuilder inputFluidSlot;
    private @Nullable IRecipeSlotBuilder inputItemSlot;
    private @Nullable IRecipeSlotBuilder outputFluidSlot;
    private @Nullable IRecipeSlotBuilder outputItemSlot;

    public ElectricSoupPotRecipeCategory(IGuiHelper helper)
    {
        super(TFCElectricCookingJEIPlugin.ELECTRIC_SOUP_POT, helper, helper.createBlankDrawable(118, 26), new ItemStack(ModBlocks.ELECTRIC_SOUP_POT.get()));
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
    public void setRecipe(IRecipeLayoutBuilder builder, ElectricSoupPotRecipe recipe, IFocusGroup focuses)
    {
        inputFluidSlot = null;
        inputItemSlot = null;
        outputFluidSlot = null;
        outputItemSlot = null;

        final int[] positions = slotPositions(recipe);
        final List<FluidStack> inputFluids = recipe.inputFluid().isEmpty() ? List.of() : List.of(recipe.inputFluid());
        final List<ItemStack> outputItems = recipe.outputItems();

        if (!inputFluids.isEmpty())
        {
            inputFluidSlot = builder.addSlot(RecipeIngredientRole.INPUT, recipe.inputItems().isEmpty() ? positions[1] : positions[0], 5);
            inputFluidSlot.addIngredients(JEIIntegration.FLUID_STACK, inputFluids);
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
            outputFluidSlot.addIngredient(JEIIntegration.FLUID_STACK, outputFluid);
            outputFluidSlot.setFluidRenderer(1, false, 16, 16);
            outputFluidSlot.setBackground(slot, -1, -1);
        }

        if (!outputItems.isEmpty() && !outputItems.stream().allMatch(ItemStack::isEmpty))
        {
            outputItemSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, outputFluid.isEmpty() ? positions[2] : positions[3], 5);
            outputItemSlot.addItemStacks(outputItems);
            outputItemSlot.setBackground(slot, -1, -1);
        }
    }

    @Override
    public void draw(ElectricSoupPotRecipe recipe, IRecipeSlotsView recipeSlots, GuiGraphics graphics, double mouseX, double mouseY)
    {
        final int arrowPosition = arrowPosition(recipe);
        arrow.draw(graphics, arrowPosition, 5);
        arrowAnimated.draw(graphics, arrowPosition, 5);
    }

    private int[] slotPositions(ElectricSoupPotRecipe recipe)
    {
        return new int[] {6, 26, 76, 96};
    }

    private int arrowPosition(ElectricSoupPotRecipe recipe)
    {
        return 48;
    }
}

package com.tfcelectriccooking.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.common.recipes.outputs.PotOutput;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;

/**
 * Renders a flat liquid surface inside the electric soup pot.
 */
public class ElectricSoupPotBlockEntityRenderer implements BlockEntityRenderer<ElectricSoupPotBlockEntity>
{
    private static final float FLUID_MIN_X = 5f / 16f;
    private static final float FLUID_MAX_X = 11f / 16f;
    private static final float FLUID_MIN_Z = 5f / 16f;
    private static final float FLUID_MAX_Z = 11f / 16f;
    private static final float FLUID_Y = 11f / 16f;  // liquid surface level
    private static final int OUTPUT_SOUP_COLOR = 0xFFB85C24;

    public ElectricSoupPotBlockEntityRenderer(BlockEntityRendererProvider.Context context)
    {
    }

    @Override
    public void render(ElectricSoupPotBlockEntity pot, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        if (pot.getLevel() == null)
        {
            return;
        }

        final PotOutput output = pot.getOutput();
        FluidStack fluid = pot.getFluidInTank();
        final boolean renderOutput = output != null && !output.isEmpty();
        if (fluid.isEmpty() && !renderOutput && !pot.shouldRenderAsBoiling())
        {
            return;
        }

        if (fluid.isEmpty())
        {
            fluid = new FluidStack(Fluids.WATER, 1);
        }

        float y = FLUID_Y;
        if (!renderOutput && pot.shouldRenderAsBoiling())
        {
            final float time = (System.currentTimeMillis() % 1000L) / 1000f;
            y += (float) Math.sin(time * Math.PI * 4f) * 0.01f;
        }

        final int color = renderOutput ? OUTPUT_SOUP_COLOR : 0xFF4A90E2;
        RenderHelpers.renderFluidFace(
            poseStack,
            fluid,
            buffer,
            color,
            FLUID_MIN_X,
            FLUID_MIN_Z,
            FLUID_MAX_X,
            FLUID_MAX_Z,
            y,
            packedOverlay,
            packedLight
        );
    }
}

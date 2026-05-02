package com.tfcelectriccooking.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;
import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.recipes.PotRecipe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

public class ElectricSoupPotBlockEntityRenderer implements BlockEntityRenderer<ElectricSoupPotBlockEntity>
{
    private static final float FLUID_MIN_X = 5f / 16f;
    private static final float FLUID_MAX_X = 11f / 16f;
    private static final float FLUID_MIN_Z = 5f / 16f;
    private static final float FLUID_MAX_Z = 11f / 16f;
    private static final float DEFAULT_FLUID_Y = 11f / 16f;
    private static final float MIN_OUTPUT_FLUID_Y = 8.75f / 16f;
    private static final int OUTPUT_SOUP_COLOR = 0xFFB85C24;

    public ElectricSoupPotBlockEntityRenderer(BlockEntityRendererProvider.Context context)
    {
    }

    @Override
    public void render(ElectricSoupPotBlockEntity pot, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        if (pot.getLevel() == null)
        {
            return;
        }

        final PotRecipe.Output output = pot.getOutput();
        if (output != null && output.getRenderTexture() != null)
        {
            RenderHelpers.renderTexturedFace(
                poseStack,
                buffer,
                0xFFFFFF,
                FLUID_MIN_X,
                FLUID_MIN_Z,
                FLUID_MAX_X,
                FLUID_MAX_Z,
                Math.max(output.getFluidYLevel(), MIN_OUTPUT_FLUID_Y),
                packedOverlay,
                packedLight,
                output.getRenderTexture(),
                false
            );
            return;
        }

        final boolean useOutputColor = output != null && output.getFluidColor() != -1;
        FluidStack fluid = pot.getFluidInTank();
        if (fluid.isEmpty())
        {
            if (!useOutputColor)
            {
                return;
            }
            fluid = new FluidStack(Fluids.WATER, FluidHelpers.BUCKET_VOLUME);
        }

        float fluidY = output == null ? DEFAULT_FLUID_Y : Math.max(output.getFluidYLevel(), MIN_OUTPUT_FLUID_Y);
        if (output == null && pot.shouldRenderAsBoiling())
        {
            final float time = (System.currentTimeMillis() % 1000L) / 1000f;
            fluidY += (float) Math.sin(time * Math.PI * 4f) * 0.01f;
        }

        final int color = useOutputColor
            ? output.getFluidColor()
            : pot.hasOutput() ? OUTPUT_SOUP_COLOR : RenderHelpers.getFluidColor(fluid);

        RenderHelpers.renderFluidFace(
            poseStack,
            fluid,
            buffer,
            color,
            FLUID_MIN_X,
            FLUID_MIN_Z,
            FLUID_MAX_X,
            FLUID_MAX_Z,
            fluidY,
            packedOverlay,
            packedLight
        );
    }
}

package com.tfcelectriccooking.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tfcelectriccooking.common.block.ElectricSoupPotBlock;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;
import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.recipes.outputs.PotOutput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;

public class ElectricSoupPotBlockEntityRenderer implements BlockEntityRenderer<ElectricSoupPotBlockEntity>
{
    private static final float FLUID_MIN_X = 3f / 16f;
    private static final float FLUID_MAX_X = 13f / 16f;
    private static final float FLUID_MIN_Z = 3f / 16f;
    private static final float FLUID_MAX_Z = 13f / 16f;
    private static final float DEFAULT_FLUID_Y = 11f / 16f;
    private static final float MIN_OUTPUT_FLUID_Y = 8.75f / 16f;
    private static final int OUTPUT_SOUP_COLOR = 0xFFB85C24;
    private static final double[][] SLOT_POSITIONS = {
        {0.500, 7.10 / 16.0, 0.403},
        {0.592, 7.85 / 16.0, 0.470},
        {0.557, 8.60 / 16.0, 0.578},
        {0.443, 9.35 / 16.0, 0.578},
        {0.408, 10.10 / 16.0, 0.470}
    };
    private static final float[] SLOT_ROTATIONS = {
        0F, 72F, 144F, 216F, 288F
    };

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

        renderFluid(pot, poseStack, buffer, packedLight, packedOverlay);
        renderItems(pot, poseStack, buffer, packedLight, packedOverlay);
    }

    private static void renderFluid(ElectricSoupPotBlockEntity pot, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        final PotOutput output = pot.getOutput();
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
                output.getRenderTexture()
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
            fluidY += (float) Math.sin(time * Math.PI * 4f) * 0.0025f;
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

    private static void renderItems(ElectricSoupPotBlockEntity pot, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        final IItemHandler inventory = pot.getInventory().getItemHandler();
        for (int i = 0; i < ElectricSoupPotBlockEntity.INPUT_SLOTS; i++)
        {
            final int slot = ElectricSoupPotBlockEntity.SLOT_INPUT_START + i;
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty())
            {
                renderItem(pot, stack, i, poseStack, buffer, packedLight, packedOverlay);
            }
        }
    }

    private static void renderItem(ElectricSoupPotBlockEntity pot, ItemStack stack, int index, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        final double[] pos = SLOT_POSITIONS[index];
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationFor(pot.getBlockState().getValue(ElectricSoupPotBlock.FACING))));
        poseStack.translate(pos[0] - 0.5, pos[1] - 0.5, pos[2] - 0.5);
        poseStack.scale(0.50f, 0.50f, 0.50f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180F + SLOT_ROTATIONS[index]));
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer, pot.getLevel(), index);
        poseStack.popPose();
    }

    private static float rotationFor(Direction facing)
    {
        return switch (facing) {
            case SOUTH -> 180F;
            case WEST -> 270F;
            case EAST -> 90F;
            default -> 0F;
        };
    }
}

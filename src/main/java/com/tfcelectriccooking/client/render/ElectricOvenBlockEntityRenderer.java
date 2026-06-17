package com.tfcelectriccooking.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tfcelectriccooking.common.block.ElectricOvenBlock;
import com.tfcelectriccooking.common.blockentity.ElectricOvenBlockEntity;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class ElectricOvenBlockEntityRenderer implements BlockEntityRenderer<ElectricOvenBlockEntity>
{
    private static final double LOWER_SHELF_Y = 4.16 / 16.0;
    private static final double UPPER_SHELF_Y = 7.16 / 16.0;
    // The open door sits at z < 2; this keeps items just inside the door-side thin rack.
    private static final double TRAY_Z = 5.9 / 16.0;
    private static final double ITEM_LAYER_Y_OFFSET = 0.0025;
    private static final double ITEM_LAYER_Z_OFFSET = 0.0025;
    private static final double[][] CENTERED_X_POSITIONS = {
        {},
        {8.0 / 16.0},
        {6.25 / 16.0, 9.75 / 16.0},
        {4.75 / 16.0, 8.0 / 16.0, 11.25 / 16.0}
    };

    public ElectricOvenBlockEntityRenderer(BlockEntityRendererProvider.Context context)
    {
    }

    @Override
    public void render(ElectricOvenBlockEntity oven, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        if (oven.getLevel() == null)
        {
            return;
        }

        oven.getCapability(Capabilities.ITEM).ifPresent(cap -> {
            renderShelf(oven, cap, 0, LOWER_SHELF_Y, poseStack, buffer, packedLight, packedOverlay);
            renderShelf(oven, cap, 3, UPPER_SHELF_Y, poseStack, buffer, packedLight, packedOverlay);
        });
    }

    private static void renderShelf(ElectricOvenBlockEntity oven, IItemHandler cap, int firstSlot, double y, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        final int[] slots = new int[3];
        int count = 0;
        for (int offset = 0; offset < 3; offset++)
        {
            final int slot = firstSlot + offset;
            if (slot < ElectricOvenBlockEntity.SLOTS && !cap.getStackInSlot(slot).isEmpty())
            {
                slots[count++] = slot;
            }
        }

        for (int i = 0; i < count; i++)
        {
            final int slot = slots[i];
            final double layeredY = y + i * ITEM_LAYER_Y_OFFSET;
            final double layeredZ = TRAY_Z - i * ITEM_LAYER_Z_OFFSET;
            renderItem(oven, cap.getStackInSlot(slot), slot, CENTERED_X_POSITIONS[count][i], layeredY, layeredZ, poseStack, buffer, packedLight, packedOverlay);
        }
    }

    private static void renderItem(ElectricOvenBlockEntity oven, ItemStack stack, int slot, double x, double y, double z, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationFor(oven.getBlockState().getValue(ElectricOvenBlock.FACING))));
        poseStack.translate(x - 0.5, y - 0.5, z - 0.5);
        poseStack.scale(0.38f, 0.38f, 0.38f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90F));
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer, oven.getLevel(), slot);
        poseStack.popPose();
    }

    private static float rotationFor(Direction facing)
    {
        // Blockstate y rotations and PoseStack positive-Y rotations are opposite for east/west.
        return switch (facing) {
            case SOUTH -> 180F;
            case WEST -> 90F;
            case EAST -> 270F;
            default -> 0F;
        };
    }
}

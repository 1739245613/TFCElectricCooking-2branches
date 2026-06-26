package com.tfcelectriccooking.common.container;

import com.tfcelectriccooking.common.ModContainerTypes;
import com.tfcelectriccooking.common.block.ElectricOvenBlock;
import com.tfcelectriccooking.common.blockentity.ElectricOvenBlockEntity;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.common.container.BlockEntityContainer;
import net.dries007.tfc.common.container.CallbackSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ElectricOvenContainer extends BlockEntityContainer<ElectricOvenBlockEntity>
{
    public static final int TARGET_TEMPERATURE_BUTTON_STEPS = 49;

    public static ElectricOvenContainer create(ElectricOvenBlockEntity oven, Inventory playerInv, int windowId)
    {
        return new ElectricOvenContainer(oven, windowId).init(playerInv, 20);
    }

    private ElectricOvenContainer(ElectricOvenBlockEntity oven, int windowId)
    {
        super(ModContainerTypes.ELECTRIC_OVEN.get(), windowId, oven);
        addDataSlots(oven.getSyncData());
    }

    @Override
    protected void addContainerSlots()
    {
        blockEntity.getCapability(Capabilities.ITEM).ifPresent(handler -> {
            for (int row = 0; row < 2; row++)
            {
                for (int col = 0; col < 3; col++)
                {
                    addSlot(new CallbackSlot(blockEntity, handler, row * 3 + col, 71 + col * 18, 32 + row * 22));
                }
            }
        });
    }

    @Override
    protected boolean moveStack(ItemStack stack, int slotIndex)
    {
        if (slotIndex >= ElectricOvenBlockEntity.SLOTS)
        {
            return !moveItemStackTo(stack, 0, ElectricOvenBlockEntity.SLOTS, false);
        }
        return !moveItemStackTo(stack, ElectricOvenBlockEntity.SLOTS, ElectricOvenBlockEntity.SLOTS + 36, false);
    }

    @Override
    public boolean clickMenuButton(Player player, int id)
    {
        if (id >= 0 && id <= TARGET_TEMPERATURE_BUTTON_STEPS)
        {
            blockEntity.setTargetTemperature(buttonIdToTemperature(id));
            return true;
        }
        return false;
    }

    public static int temperatureToButtonId(int temperature)
    {
        final int clamped = Math.max(0, Math.min(ElectricOvenBlockEntity.MAX_TEMPERATURE, temperature));
        return Math.round(clamped * TARGET_TEMPERATURE_BUTTON_STEPS / (float) ElectricOvenBlockEntity.MAX_TEMPERATURE);
    }

    public static int buttonIdToTemperature(int id)
    {
        final int clamped = Math.max(0, Math.min(TARGET_TEMPERATURE_BUTTON_STEPS, id));
        return Math.round(clamped * ElectricOvenBlockEntity.MAX_TEMPERATURE / (float) TARGET_TEMPERATURE_BUTTON_STEPS);
    }

    @Override
    public void removed(Player player)
    {
        if (!player.level().isClientSide)
        {
            ElectricOvenBlock.setOpen(player.level(), blockEntity.getBlockPos(), blockEntity.getBlockState(), false);
        }
        super.removed(player);
    }
}

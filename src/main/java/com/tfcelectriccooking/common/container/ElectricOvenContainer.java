package com.tfcelectriccooking.common.container;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.dries007.tfc.common.container.BlockEntityContainer;
import net.dries007.tfc.common.container.slot.CallbackSlot;
import com.tfcelectriccooking.common.ModContainerTypes;
import com.tfcelectriccooking.common.blockentity.ElectricOvenBlockEntity;

public class ElectricOvenContainer extends BlockEntityContainer<ElectricOvenBlockEntity>
{
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
        final ElectricOvenBlockEntity oven = getBlockEntity();
        // 10 slots in 2 rows of 5, aligned to the custom oven grid
        for (int row = 0; row < 2; row++)
        {
            for (int col = 0; col < 5; col++)
            {
                addSlot(new CallbackSlot(oven, row * 5 + col, 62 + col * 18, 24 + row * 18));
            }
        }
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
        // Direct temperature setting: id = temperature value (0-1600)
        if (id >= 0 && id <= 1600)
        {
            getBlockEntity().setTargetTemperature(id);
            return true;
        }
        return false;
    }
}

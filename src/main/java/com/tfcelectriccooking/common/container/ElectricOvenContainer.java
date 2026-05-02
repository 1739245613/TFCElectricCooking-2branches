package com.tfcelectriccooking.common.container;

import com.tfcelectriccooking.common.ModContainerTypes;
import com.tfcelectriccooking.common.blockentity.ElectricOvenBlockEntity;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.common.container.BlockEntityContainer;
import net.dries007.tfc.common.container.CallbackSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
        blockEntity.getCapability(Capabilities.ITEM).ifPresent(handler -> {
            for (int row = 0; row < 2; row++)
            {
                for (int col = 0; col < 5; col++)
                {
                    addSlot(new CallbackSlot(blockEntity, handler, row * 5 + col, 62 + col * 18, 24 + row * 18));
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
        if (id >= 0 && id <= ElectricOvenBlockEntity.MAX_TEMPERATURE)
        {
            blockEntity.setTargetTemperature(id);
            return true;
        }
        return false;
    }
}

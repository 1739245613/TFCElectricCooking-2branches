package com.tfcelectriccooking.common.container;

import com.tfcelectriccooking.common.ModContainerTypes;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.common.container.BlockEntityContainer;
import net.dries007.tfc.common.container.CallbackSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ElectricSoupPotContainer extends BlockEntityContainer<ElectricSoupPotBlockEntity>
{
    public static ElectricSoupPotContainer create(ElectricSoupPotBlockEntity pot, Inventory playerInv, int windowId)
    {
        return new ElectricSoupPotContainer(pot, windowId).init(playerInv, 20);
    }

    private ElectricSoupPotContainer(ElectricSoupPotBlockEntity pot, int windowId)
    {
        super(ModContainerTypes.ELECTRIC_SOUP_POT.get(), windowId, pot);
        addDataSlots(pot.getSyncData());
    }

    @Override
    protected void addContainerSlots()
    {
        blockEntity.getCapability(Capabilities.ITEM).ifPresent(handler -> {
            addSlot(new CallbackSlot(blockEntity, handler, ElectricSoupPotBlockEntity.SLOT_EXTRA_INPUT_START, 65, 23));
            addSlot(new CallbackSlot(blockEntity, handler, ElectricSoupPotBlockEntity.SLOT_EXTRA_INPUT_START + 1, 83, 23));
            addSlot(new CallbackSlot(blockEntity, handler, ElectricSoupPotBlockEntity.SLOT_EXTRA_INPUT_START + 2, 56, 41));
            addSlot(new CallbackSlot(blockEntity, handler, ElectricSoupPotBlockEntity.SLOT_EXTRA_INPUT_START + 3, 74, 41));
            addSlot(new CallbackSlot(blockEntity, handler, ElectricSoupPotBlockEntity.SLOT_EXTRA_INPUT_END, 92, 41));
        });
    }

    @Override
    protected boolean moveStack(ItemStack stack, int slotIndex)
    {
        if (slotIndex >= ElectricSoupPotBlockEntity.INPUT_SLOT_COUNT)
        {
            if (blockEntity.canAcceptManualInput())
            {
                return !moveItemStackTo(stack, 0, ElectricSoupPotBlockEntity.INPUT_SLOT_COUNT, false);
            }
            return true;
        }
        return !moveItemStackTo(stack, ElectricSoupPotBlockEntity.INPUT_SLOT_COUNT, ElectricSoupPotBlockEntity.INPUT_SLOT_COUNT + 36, false);
    }

    @Override
    public boolean clickMenuButton(Player player, int id)
    {
        if (id >= 0 && id <= ElectricSoupPotBlockEntity.MAX_TEMPERATURE)
        {
            blockEntity.setTargetTemperature(id);
            return true;
        }
        return false;
    }
}

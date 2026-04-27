package com.tfcelectriccooking.common.container;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.dries007.tfc.common.container.BlockEntityContainer;
import net.dries007.tfc.common.container.slot.CallbackSlot;
import com.tfcelectriccooking.common.ModContainerTypes;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;

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
        final ElectricSoupPotBlockEntity pot = getBlockEntity();
        // 5 ingredient slots in diamond pattern (like TFC clay pot)
        addSlot(new CallbackSlot(pot, 0, 65, 23));
        addSlot(new CallbackSlot(pot, 1, 83, 23));
        addSlot(new CallbackSlot(pot, 2, 56, 41));
        addSlot(new CallbackSlot(pot, 3, 74, 41));
        addSlot(new CallbackSlot(pot, 4, 92, 41));
    }

    @Override
    protected boolean moveStack(ItemStack stack, int slotIndex)
    {
        if (slotIndex >= ElectricSoupPotBlockEntity.INPUT_SLOTS)
        {
            if (!getBlockEntity().hasRecipeStarted())
            {
                return !moveItemStackTo(stack, 0, ElectricSoupPotBlockEntity.INPUT_SLOTS, false);
            }
            return true;
        }
        return !moveItemStackTo(stack, ElectricSoupPotBlockEntity.INPUT_SLOTS, ElectricSoupPotBlockEntity.INPUT_SLOTS + 36, false);
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

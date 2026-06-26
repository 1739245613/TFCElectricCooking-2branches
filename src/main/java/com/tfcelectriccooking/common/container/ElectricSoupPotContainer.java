package com.tfcelectriccooking.common.container;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.dries007.tfc.common.container.BlockEntityContainer;
import net.dries007.tfc.common.container.slot.CallbackSlot;
import com.tfcelectriccooking.common.ModContainerTypes;
import com.tfcelectriccooking.common.block.ElectricSoupPotBlock;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;

public class ElectricSoupPotContainer extends BlockEntityContainer<ElectricSoupPotBlockEntity>
{
    public static final int BUTTON_STOP = 0;
    public static final int BUTTON_RUN = 1;
    private static final int RUNNING_TEMPERATURE = 350;

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
        addSlot(new CallbackSlot(pot, 0, 43, 39));
        addSlot(new CallbackSlot(pot, 1, 61, 39));
        addSlot(new CallbackSlot(pot, 2, 34, 57));
        addSlot(new CallbackSlot(pot, 3, 52, 57));
        addSlot(new CallbackSlot(pot, 4, 70, 57));
    }

    @Override
    protected boolean moveStack(ItemStack stack, int slotIndex)
    {
        if (slotIndex >= ElectricSoupPotBlockEntity.INPUT_SLOTS)
        {
            if (getBlockEntity().canAcceptManualInput())
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
        if (id == BUTTON_STOP || id == BUTTON_RUN)
        {
            getBlockEntity().setTargetTemperature(id == BUTTON_RUN ? RUNNING_TEMPERATURE : 0);
            return true;
        }
        return false;
    }

    @Override
    public void removed(Player player)
    {
        if (!player.level().isClientSide)
        {
            ElectricSoupPotBlock.setOpen(player.level(), getBlockEntity().getBlockPos(), getBlockEntity().getBlockState(), false);
        }
        super.removed(player);
    }
}

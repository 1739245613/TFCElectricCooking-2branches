package com.tfcelectriccooking.common.container;

import com.tfcelectriccooking.common.ModContainerTypes;
import com.tfcelectriccooking.common.block.ElectricSoupPotBlock;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.common.container.BlockEntityContainer;
import net.dries007.tfc.common.container.CallbackSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ElectricSoupPotContainer extends BlockEntityContainer<ElectricSoupPotBlockEntity>
{
    public static final int BUTTON_STOP = 0;
    public static final int BUTTON_RUN = 1;
    private static final int RUNNING_TEMPERATURE = 350;
    private static final int HIDDEN_FIREPIT_SLOT_COUNT = ElectricSoupPotBlockEntity.SLOT_EXTRA_INPUT_START;
    private static final int POT_MENU_SLOT_START = HIDDEN_FIREPIT_SLOT_COUNT;
    private static final int POT_MENU_SLOT_END = POT_MENU_SLOT_START + ElectricSoupPotBlockEntity.INPUT_SLOT_COUNT;

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
        final SimpleContainer hiddenFirepitSlots = new SimpleContainer(HIDDEN_FIREPIT_SLOT_COUNT);
        for (int slot = 0; slot < HIDDEN_FIREPIT_SLOT_COUNT; slot++)
        {
            addSlot(new HiddenSlot(hiddenFirepitSlots, slot));
        }

        blockEntity.getCapability(Capabilities.ITEM).ifPresent(handler -> {
            addSlot(new CallbackSlot(blockEntity, handler, ElectricSoupPotBlockEntity.SLOT_EXTRA_INPUT_START, 43, 39));
            addSlot(new CallbackSlot(blockEntity, handler, ElectricSoupPotBlockEntity.SLOT_EXTRA_INPUT_START + 1, 61, 39));
            addSlot(new CallbackSlot(blockEntity, handler, ElectricSoupPotBlockEntity.SLOT_EXTRA_INPUT_START + 2, 34, 57));
            addSlot(new CallbackSlot(blockEntity, handler, ElectricSoupPotBlockEntity.SLOT_EXTRA_INPUT_START + 3, 52, 57));
            addSlot(new CallbackSlot(blockEntity, handler, ElectricSoupPotBlockEntity.SLOT_EXTRA_INPUT_END, 70, 57));
        });
    }

    @Override
    protected boolean moveStack(ItemStack stack, int slotIndex)
    {
        if (slotIndex >= POT_MENU_SLOT_END)
        {
            if (blockEntity.canAcceptManualInput())
            {
                return !moveItemStackTo(stack, POT_MENU_SLOT_START, POT_MENU_SLOT_END, false);
            }
            return true;
        }
        if (slotIndex >= POT_MENU_SLOT_START)
        {
            return !moveItemStackTo(stack, POT_MENU_SLOT_END, POT_MENU_SLOT_END + 36, false);
        }
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id)
    {
        if (id == BUTTON_STOP || id == BUTTON_RUN)
        {
            blockEntity.setTargetTemperature(id == BUTTON_RUN ? RUNNING_TEMPERATURE : 0);
            return true;
        }
        return false;
    }

    @Override
    public void removed(Player player)
    {
        if (!player.level().isClientSide)
        {
            ElectricSoupPotBlock.setOpen(player.level(), blockEntity.getBlockPos(), blockEntity.getBlockState(), false);
        }
        super.removed(player);
    }

    private static final class HiddenSlot extends Slot
    {
        private HiddenSlot(SimpleContainer container, int slot)
        {
            super(container, slot, -1000, -1000);
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return false;
        }

        @Override
        public boolean mayPickup(Player player)
        {
            return false;
        }

        @Override
        public boolean isActive()
        {
            return false;
        }
    }
}

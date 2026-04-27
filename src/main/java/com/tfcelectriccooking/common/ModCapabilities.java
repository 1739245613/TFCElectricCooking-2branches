package com.tfcelectriccooking.common;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;

public class ModCapabilities
{
    public static void register(RegisterCapabilitiesEvent event)
    {
        // Electric Oven - energy + items
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlocks.ELECTRIC_OVEN_BLOCK_ENTITY.get(),
            (be, side) -> be.getEnergyStorage()
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            ModBlocks.ELECTRIC_OVEN_BLOCK_ENTITY.get(),
            InventoryBlockEntity::getSidedInventory
        );

        // Electric Soup Pot - energy + items + fluids
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlocks.ELECTRIC_SOUP_POT_BLOCK_ENTITY.get(),
            (be, side) -> be.getEnergyStorage()
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            ModBlocks.ELECTRIC_SOUP_POT_BLOCK_ENTITY.get(),
            InventoryBlockEntity::getSidedInventory
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            ModBlocks.ELECTRIC_SOUP_POT_BLOCK_ENTITY.get(),
            ElectricSoupPotBlockEntity::getSidedFluidInventory
        );
    }
}

package com.tfcelectriccooking.common;

import com.tfcelectriccooking.TFCElectricCooking;
import com.tfcelectriccooking.common.blockentity.ElectricOvenBlockEntity;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;
import com.tfcelectriccooking.common.container.ElectricOvenContainer;
import com.tfcelectriccooking.common.container.ElectricSoupPotContainer;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.container.BlockEntityContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModContainerTypes
{
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TFCElectricCooking.MOD_ID);

    public static final RegistryObject<MenuType<ElectricOvenContainer>> ELECTRIC_OVEN = ModContainerTypes.<ElectricOvenBlockEntity, ElectricOvenContainer>registerBlock("electric_oven",
        ModBlocks.ELECTRIC_OVEN_BLOCK_ENTITY, ElectricOvenContainer::create);

    public static final RegistryObject<MenuType<ElectricSoupPotContainer>> ELECTRIC_SOUP_POT = ModContainerTypes.<ElectricSoupPotBlockEntity, ElectricSoupPotContainer>registerBlock("electric_soup_pot",
        ModBlocks.ELECTRIC_SOUP_POT_BLOCK_ENTITY, ElectricSoupPotContainer::create);

    private ModContainerTypes() {}

    private static <T extends InventoryBlockEntity<?>, C extends BlockEntityContainer<T>> RegistryObject<MenuType<C>> registerBlock(
        String name, RegistryObject<BlockEntityType<T>> type, BlockEntityContainer.Factory<T, C> factory)
    {
        return MENUS.register(name, () -> IForgeMenuType.create((windowId, playerInventory, buffer) -> {
            final Level level = playerInventory.player.level();
            final BlockPos pos = buffer.readBlockPos();
            final T entity = level.getBlockEntity(pos, type.get()).orElseThrow();
            return factory.create(entity, playerInventory, windowId);
        }));
    }

    public static void register(IEventBus bus)
    {
        MENUS.register(bus);
    }
}

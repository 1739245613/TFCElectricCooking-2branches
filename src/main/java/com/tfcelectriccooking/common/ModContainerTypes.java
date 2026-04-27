package com.tfcelectriccooking.common;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.container.BlockEntityContainer;
import com.tfcelectriccooking.TFCElectricCooking;
import com.tfcelectriccooking.common.blockentity.ElectricOvenBlockEntity;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;
import com.tfcelectriccooking.common.container.ElectricOvenContainer;
import com.tfcelectriccooking.common.container.ElectricSoupPotContainer;

public class ModContainerTypes
{
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(Registries.MENU, TFCElectricCooking.MOD_ID);

    public static final Supplier<MenuType<ElectricOvenContainer>> ELECTRIC_OVEN = ModContainerTypes.<ElectricOvenBlockEntity, ElectricOvenContainer>registerBlock("electric_oven",
        ModBlocks.ELECTRIC_OVEN_BLOCK_ENTITY, ElectricOvenContainer::create);

    public static final Supplier<MenuType<ElectricSoupPotContainer>> ELECTRIC_SOUP_POT = ModContainerTypes.<ElectricSoupPotBlockEntity, ElectricSoupPotContainer>registerBlock("electric_soup_pot",
        ModBlocks.ELECTRIC_SOUP_POT_BLOCK_ENTITY, ElectricSoupPotContainer::create);

    @SuppressWarnings("unchecked")
    private static <T extends InventoryBlockEntity<?>, C extends BlockEntityContainer<T>> DeferredHolder<MenuType<?>, MenuType<C>> registerBlock(
        String name, Supplier<BlockEntityType<T>> type, BlockEntityContainer.Factory<T, C> factory)
    {
        return CONTAINERS.register(name, () -> IMenuTypeExtension.create((windowId, playerInventory, buffer) -> {
            final Level level = playerInventory.player.level();
            final BlockPos pos = buffer.readBlockPos();
            final T entity = level.getBlockEntity(pos, type.get()).orElseThrow();
            return factory.create(entity, playerInventory, windowId);
        }));
    }

    public static void register(IEventBus bus)
    {
        CONTAINERS.register(bus);
    }
}

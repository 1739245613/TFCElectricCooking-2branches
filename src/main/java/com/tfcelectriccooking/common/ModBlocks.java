package com.tfcelectriccooking.common;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.tfcelectriccooking.TFCElectricCooking;
import com.tfcelectriccooking.common.block.ElectricOvenBlock;
import com.tfcelectriccooking.common.block.ElectricSoupPotBlock;
import com.tfcelectriccooking.common.blockentity.ElectricOvenBlockEntity;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;

public class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, TFCElectricCooking.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, TFCElectricCooking.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TFCElectricCooking.MOD_ID);

    // Blocks
    public static final Supplier<ElectricOvenBlock> ELECTRIC_OVEN = BLOCKS.register("electric_oven", ElectricOvenBlock::new);
    public static final Supplier<ElectricSoupPotBlock> ELECTRIC_SOUP_POT = BLOCKS.register("electric_soup_pot", ElectricSoupPotBlock::new);

    // Block Items
    public static final Supplier<BlockItem> ELECTRIC_OVEN_ITEM = ITEMS.register("electric_oven",
        () -> new BlockItem(ELECTRIC_OVEN.get(), new Item.Properties()));
    public static final Supplier<BlockItem> ELECTRIC_SOUP_POT_ITEM = ITEMS.register("electric_soup_pot",
        () -> new BlockItem(ELECTRIC_SOUP_POT.get(), new Item.Properties()));

    // Block Entities
    @SuppressWarnings("ConstantConditions")
    public static final Supplier<BlockEntityType<ElectricOvenBlockEntity>> ELECTRIC_OVEN_BLOCK_ENTITY = BLOCK_ENTITIES.register("electric_oven",
        () -> BlockEntityType.Builder.of(ElectricOvenBlockEntity::new, ELECTRIC_OVEN.get()).build(null));

    @SuppressWarnings("ConstantConditions")
    public static final Supplier<BlockEntityType<ElectricSoupPotBlockEntity>> ELECTRIC_SOUP_POT_BLOCK_ENTITY = BLOCK_ENTITIES.register("electric_soup_pot",
        () -> BlockEntityType.Builder.of(ElectricSoupPotBlockEntity::new, ELECTRIC_SOUP_POT.get()).build(null));

    public static void register(IEventBus bus)
    {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}

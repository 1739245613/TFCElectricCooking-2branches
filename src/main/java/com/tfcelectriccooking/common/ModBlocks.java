package com.tfcelectriccooking.common;

import com.tfcelectriccooking.TFCElectricCooking;
import com.tfcelectriccooking.common.block.ElectricOvenBlock;
import com.tfcelectriccooking.common.block.ElectricSoupPotBlock;
import com.tfcelectriccooking.common.blockentity.ElectricOvenBlockEntity;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TFCElectricCooking.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TFCElectricCooking.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TFCElectricCooking.MOD_ID);

    public static final RegistryObject<ElectricOvenBlock> ELECTRIC_OVEN = BLOCKS.register("electric_oven", ElectricOvenBlock::new);
    public static final RegistryObject<ElectricSoupPotBlock> ELECTRIC_SOUP_POT = BLOCKS.register("electric_soup_pot", ElectricSoupPotBlock::new);

    public static final RegistryObject<BlockItem> ELECTRIC_OVEN_ITEM = ITEMS.register("electric_oven",
        () -> new BlockItem(ELECTRIC_OVEN.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> ELECTRIC_SOUP_POT_ITEM = ITEMS.register("electric_soup_pot",
        () -> new BlockItem(ELECTRIC_SOUP_POT.get(), new Item.Properties()));

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<ElectricOvenBlockEntity>> ELECTRIC_OVEN_BLOCK_ENTITY = BLOCK_ENTITIES.register("electric_oven",
        () -> BlockEntityType.Builder.of(ElectricOvenBlockEntity::new, ELECTRIC_OVEN.get()).build(null));

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<ElectricSoupPotBlockEntity>> ELECTRIC_SOUP_POT_BLOCK_ENTITY = BLOCK_ENTITIES.register("electric_soup_pot",
        () -> BlockEntityType.Builder.of(ElectricSoupPotBlockEntity::new, ELECTRIC_SOUP_POT.get()).build(null));

    private ModBlocks() {}

    public static void register(IEventBus bus)
    {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}

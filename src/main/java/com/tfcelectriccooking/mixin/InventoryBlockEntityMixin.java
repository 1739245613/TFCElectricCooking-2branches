package com.tfcelectriccooking.mixin;

import com.tfcelectriccooking.common.automation.JarringStationAutomationItemHandler;
import java.lang.reflect.Field;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InventoryBlockEntity.class, remap = false)
public abstract class InventoryBlockEntityMixin
{
    @Unique
    private static final @Nullable Field tfcelectriccooking$inventoryField = tfcelectriccooking$findInventoryField();

    @Unique
    private JarringStationAutomationItemHandler tfcelectriccooking$jarringStationAutomation;

    @Unique
    private LazyOptional<IItemHandler> tfcelectriccooking$jarringStationAutomationCapability = LazyOptional.empty();

    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true)
    private <T> void tfcelectriccooking$getAutomationCapability(Capability<T> cap, @Nullable Direction side, CallbackInfoReturnable<LazyOptional<T>> cir)
    {
        if (side != null && cap == Capabilities.ITEM && tfcelectriccooking$isFirmaLifeJarringStation())
        {
            if (!tfcelectriccooking$jarringStationAutomationCapability.isPresent())
            {
                final IItemHandlerModifiable inventory = tfcelectriccooking$getInventory();
                if (inventory == null)
                {
                    return;
                }
                tfcelectriccooking$jarringStationAutomation = new JarringStationAutomationItemHandler((BlockEntity) (Object) this, inventory);
                tfcelectriccooking$jarringStationAutomationCapability = LazyOptional.of(() -> tfcelectriccooking$jarringStationAutomation);
            }
            cir.setReturnValue(tfcelectriccooking$jarringStationAutomationCapability.cast());
        }
    }

    @Inject(method = "invalidateCapabilities", at = @At("HEAD"))
    private void tfcelectriccooking$invalidateAutomationCapability(CallbackInfo ci)
    {
        tfcelectriccooking$jarringStationAutomationCapability.invalidate();
        tfcelectriccooking$jarringStationAutomationCapability = LazyOptional.empty();
        tfcelectriccooking$jarringStationAutomation = null;
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void tfcelectriccooking$refreshJarringStationModelData(CompoundTag nbt, CallbackInfo ci)
    {
        if (tfcelectriccooking$isFirmaLifeJarringStation())
        {
            final BlockEntity blockEntity = (BlockEntity) (Object) this;
            final Level level = blockEntity.getLevel();
            if (level != null && level.isClientSide)
            {
                blockEntity.requestModelDataUpdate();
                level.sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Unique
    private boolean tfcelectriccooking$isFirmaLifeJarringStation()
    {
        return ((Object) this).getClass().getName().equals("com.eerussianguy.firmalife.common.blockentities.JarringStationBlockEntity");
    }

    @Unique
    private @Nullable IItemHandlerModifiable tfcelectriccooking$getInventory()
    {
        if (tfcelectriccooking$inventoryField != null)
        {
            try
            {
                final Object value = tfcelectriccooking$inventoryField.get(this);
                if (value instanceof IItemHandlerModifiable handler)
                {
                    return handler;
                }
            }
            catch (IllegalAccessException ignored)
            {
            }
        }
        return null;
    }

    @Unique
    private static @Nullable Field tfcelectriccooking$findInventoryField()
    {
        try
        {
            final Field field = InventoryBlockEntity.class.getDeclaredField("inventory");
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException ignored)
        {
            return null;
        }
    }
}

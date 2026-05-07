package com.tfcelectriccooking.mixin;

import com.eerussianguy.firmalife.common.blockentities.JarringStationBlockEntity;
import com.tfcelectriccooking.common.automation.JarringStationAutomationItemHandler;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.IItemHandler;
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
    private JarringStationAutomationItemHandler tfcelectriccooking$jarringStationAutomation;

    @Inject(method = "getSidedInventory", at = @At("HEAD"), cancellable = true)
    private void tfcelectriccooking$getJarringStationAutomation(@Nullable Direction context, CallbackInfoReturnable<IItemHandler> cir)
    {
        if (context != null && (Object) this instanceof JarringStationBlockEntity station)
        {
            if (tfcelectriccooking$jarringStationAutomation == null)
            {
                tfcelectriccooking$jarringStationAutomation = new JarringStationAutomationItemHandler(station, station.getInventory());
            }
            cir.setReturnValue(tfcelectriccooking$jarringStationAutomation);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void tfcelectriccooking$refreshJarringStationModelData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci)
    {
        if ((Object) this instanceof JarringStationBlockEntity station)
        {
            final Level level = station.getLevel();
            if (level != null && level.isClientSide)
            {
                station.requestModelDataUpdate();
                level.sendBlockUpdated(station.getBlockPos(), station.getBlockState(), station.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }
}

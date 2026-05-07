package com.tfcelectriccooking.mixin;

import com.eerussianguy.firmalife.common.blockentities.JarringStationBlockEntity;
import com.tfcelectriccooking.common.automation.JarringStationAutomationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = JarringStationBlockEntity.class, remap = false)
public abstract class JarringStationBlockEntityMixin
{
    @Inject(method = "tick", at = @At("TAIL"))
    private static void tfcelectriccooking$tickElectricSoupPot(Level level, BlockPos pos, BlockState state, JarringStationBlockEntity station, CallbackInfo ci)
    {
        if (level.getGameTime() % 10 == 0)
        {
            JarringStationAutomationBridge.tryFillFromStation(level, pos, state, station);
        }
    }
}

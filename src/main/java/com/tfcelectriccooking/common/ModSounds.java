package com.tfcelectriccooking.common;

import com.tfcelectriccooking.TFCElectricCooking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds
{
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, TFCElectricCooking.MOD_ID);

    public static final RegistryObject<SoundEvent> ELECTRIC_OVEN_OPEN = register("block.electric_oven.open");
    public static final RegistryObject<SoundEvent> ELECTRIC_OVEN_CLOSE = register("block.electric_oven.close");
    public static final RegistryObject<SoundEvent> ELECTRIC_SOUP_POT_OPEN = register("block.electric_soup_pot.open");
    public static final RegistryObject<SoundEvent> ELECTRIC_SOUP_POT_CLOSE = register("block.electric_soup_pot.close");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> register(String name)
    {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(TFCElectricCooking.MOD_ID, name)));
    }

    public static void register(IEventBus bus)
    {
        SOUND_EVENTS.register(bus);
    }
}

package com.tfcelectriccooking.compat.jade;

import com.tfcelectriccooking.TFCElectricCooking;
import com.tfcelectriccooking.common.block.ElectricOvenBlock;
import com.tfcelectriccooking.common.block.ElectricSoupPotBlock;
import com.tfcelectriccooking.common.blockentity.ElectricOvenBlockEntity;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;
import net.dries007.tfc.config.TFCConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.IProgressStyle;

@WailaPlugin
public final class TFCElectricCookingJadePlugin implements IWailaPlugin
{
    private static final ResourceLocation ELECTRIC_OVEN_UID = new ResourceLocation(TFCElectricCooking.MOD_ID, "electric_oven");
    private static final ResourceLocation ELECTRIC_SOUP_POT_UID = new ResourceLocation(TFCElectricCooking.MOD_ID, "electric_soup_pot");
    private static final String CURRENT_TEMPERATURE = "CurrentTemperature";
    private static final String RECIPE_TEMPERATURE = "RecipeTemperature";
    private static final String PROGRESS = "Progress";
    private static final String PROGRESS_TOTAL = "ProgressTotal";
    private static final String HAS_OUTPUT = "HasOutput";

    @Override
    public void register(IWailaCommonRegistration registry)
    {
        registry.registerBlockDataProvider(OvenComponentProvider.INSTANCE, ElectricOvenBlockEntity.class);
        registry.registerBlockDataProvider(SoupPotComponentProvider.INSTANCE, ElectricSoupPotBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registry)
    {
        registry.registerBlockComponent(OvenComponentProvider.INSTANCE, ElectricOvenBlock.class);
        registry.registerBlockComponent(SoupPotComponentProvider.INSTANCE, ElectricSoupPotBlock.class);
    }

    private static void addTemperatureLines(ITooltip tooltip, float actualTemperature)
    {
        final MutableComponent heat = TFCConfig.CLIENT.heatTooltipStyle.get().formatColored(actualTemperature);
        if (heat != null)
        {
            tooltip.add(heat);
        }
        tooltip.add(Component.translatable("tfcelectriccooking.tooltip.current_temperature", Math.round(actualTemperature)));
    }

    private enum OvenComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor>
    {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor access, IPluginConfig config)
        {
            final CompoundTag serverData = access.getServerData();
            if (!serverData.isEmpty())
            {
                addTemperatureLines(tooltip, serverData.getFloat(CURRENT_TEMPERATURE));
            }
            else if (access.getBlockEntity() instanceof ElectricOvenBlockEntity oven)
            {
                addTemperatureLines(tooltip, oven.getTemperature());
            }
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor access)
        {
            if (access.getBlockEntity() instanceof ElectricOvenBlockEntity oven)
            {
                data.putFloat(CURRENT_TEMPERATURE, oven.getTemperature());
            }
        }

        @Override
        public ResourceLocation getUid()
        {
            return ELECTRIC_OVEN_UID;
        }
    }

    private enum SoupPotComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor>
    {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor access, IPluginConfig config)
        {
            final CompoundTag serverData = access.getServerData();
            final float currentTemperature;
            final int recipeTemperature;
            final int total;
            final int progressValue;
            final boolean hasOutput;

            if (!serverData.isEmpty())
            {
                currentTemperature = serverData.getFloat(CURRENT_TEMPERATURE);
                recipeTemperature = serverData.getInt(RECIPE_TEMPERATURE);
                total = serverData.getInt(PROGRESS_TOTAL);
                progressValue = serverData.getInt(PROGRESS);
                hasOutput = serverData.getBoolean(HAS_OUTPUT);
            }
            else if (access.getBlockEntity() instanceof ElectricSoupPotBlockEntity soupPot)
            {
                currentTemperature = soupPot.getTemperature();
                recipeTemperature = soupPot.getDisplayRecipeTemperature();
                total = soupPot.getDisplayProgressTotal();
                progressValue = soupPot.getDisplayProgress();
                hasOutput = soupPot.hasOutput();
            }
            else
            {
                return;
            }

            addTemperatureLines(tooltip, currentTemperature);

            if (recipeTemperature > 0)
            {
                tooltip.add(Component.translatable("tfcelectriccooking.jade.recipe_temperature", recipeTemperature));
            }

            final float progress = hasOutput ? 1f : total > 0 ? Mth.clamp((float) progressValue / total, 0f, 1f) : 0f;
            final Component progressText = hasOutput
                ? Component.translatable("tfcelectriccooking.jade.done_short")
                : progress > 0f ? Component.literal(Mth.floor(progress * 100f) + "%") : Component.translatable("tfcelectriccooking.jade.not_started");
            final IProgressStyle style = IElementHelper.get().progressStyle()
                .color(hasOutput ? 0xFFB85C24 : 0xFF4CAF50, hasOutput ? 0xFFE07B39 : 0xFF86C36B)
                .textColor(0xFFFFFFFF);
            tooltip.add(IElementHelper.get().progress(progress, progressText, style, BoxStyle.DEFAULT, false));
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor access)
        {
            if (access.getBlockEntity() instanceof ElectricSoupPotBlockEntity soupPot)
            {
                data.putFloat(CURRENT_TEMPERATURE, soupPot.getTemperature());
                data.putInt(RECIPE_TEMPERATURE, soupPot.getDisplayRecipeTemperature());
                data.putInt(PROGRESS, soupPot.getDisplayProgress());
                data.putInt(PROGRESS_TOTAL, soupPot.getDisplayProgressTotal());
                data.putBoolean(HAS_OUTPUT, soupPot.hasOutput());
            }
        }

        @Override
        public ResourceLocation getUid()
        {
            return ELECTRIC_SOUP_POT_UID;
        }
    }
}

package com.tfcelectriccooking.client.screen;

import com.tfcelectriccooking.TFCElectricCooking;
import com.tfcelectriccooking.common.blockentity.ElectricOvenBlockEntity;
import com.tfcelectriccooking.common.container.ElectricOvenContainer;
import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.common.capabilities.heat.Heat;
import net.dries007.tfc.config.TFCConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ElectricOvenScreen extends AbstractContainerScreen<ElectricOvenContainer>
{
    private static final ResourceLocation BACKGROUND = new ResourceLocation(TFCElectricCooking.MOD_ID, "textures/gui/electric_oven.png");
    private static final ResourceLocation TEMPERATURE_BAR = new ResourceLocation(TFCElectricCooking.MOD_ID, "textures/gui/temperature_bar.png");
    private static final ResourceLocation TEMPERATURE_INDICATOR = new ResourceLocation(TFCElectricCooking.MOD_ID, "textures/gui/temperature_indicator.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 186;

    private static final int TARGET_SLIDER_X = 8;
    private static final int TARGET_SLIDER_Y = 21;
    private static final int TARGET_SLIDER_WIDTH = 14;
    private static final int TARGET_SLIDER_HEIGHT = 66;
    private static final int TARGET_SLIDER_RANGE = 49;
    private static final int TARGET_SLIDER_BOTTOM = 71;

    private static final int TARGET_HANDLE_X = 9;
    private static final int TARGET_HANDLE_Y = 22;
    private static final int TARGET_HANDLE_WIDTH = 12;
    private static final int TARGET_HANDLE_HEIGHT = 15;
    private static final float TARGET_HANDLE_CENTER_OFFSET = TARGET_HANDLE_HEIGHT / 2f;
    private static final int TARGET_SLIDER_HIT_X = TARGET_SLIDER_X - 2;
    private static final int TARGET_SLIDER_HIT_Y = TARGET_SLIDER_BOTTOM - TARGET_SLIDER_RANGE;
    private static final int TARGET_SLIDER_HIT_WIDTH = TARGET_SLIDER_WIDTH + 4;
    private static final int TARGET_SLIDER_HIT_HEIGHT = TARGET_SLIDER_RANGE + TARGET_HANDLE_HEIGHT;

    private static final int TEMPERATURE_X = 25;
    private static final int TEMPERATURE_Y = 21;
    private static final int TEMPERATURE_WIDTH = 17;
    private static final int TEMPERATURE_HEIGHT = 62;
    private static final int TEMPERATURE_TEXTURE_Y = 15;
    private static final int TEMPERATURE_TEXTURE_HEIGHT = 74;
    private static final int TEMPERATURE_RANGE = 51;
    private static final int TEMPERATURE_MARKER_X = 26;
    private static final int TEMPERATURE_MARKER_BOTTOM = 79;

    private static final int ENERGY_X = 150;
    private static final int ENERGY_Y = 21;
    private static final int ENERGY_WIDTH = 17;
    private static final int ENERGY_HEIGHT = 62;
    private static final int ENERGY_FILL_X = 155;
    private static final int ENERGY_FILL_Y = 26;
    private static final int ENERGY_FILL_WIDTH = 7;
    private static final int ENERGY_FILL_HEIGHT = 52;

    private static final int COIL_X = 67;
    private static final int COIL_Y = 77;
    private static final int COIL_WIDTH = 60;
    private static final int COIL_HEIGHT = 14;
    private static final int PENDING_TARGET_CONFIRM_TICKS = 100;

    private boolean draggingTargetTemperature;
    private int sliderTemperature;
    private int pendingTargetTemperature = -1;
    private int pendingTargetTicks;

    public ElectricOvenScreen(ElectricOvenContainer container, Inventory playerInv, Component title)
    {
        super(container, playerInv, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
        inventoryLabelY = imageHeight - 94;
        sliderTemperature = getSyncedTargetTemperature(container);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        drawTargetSlider(graphics);
        drawTemperature(graphics);
        drawEnergy(graphics);
        drawCoil(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        // The art uses no persistent labels.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderCustomTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        if (draggingTargetTemperature)
        {
            return;
        }

        final int syncedTargetTemperature = getSyncedTargetTemperature();
        if (pendingTargetTemperature >= 0)
        {
            if (syncedTargetTemperature == pendingTargetTemperature || ++pendingTargetTicks > PENDING_TARGET_CONFIRM_TICKS)
            {
                pendingTargetTemperature = -1;
                pendingTargetTicks = 0;
                sliderTemperature = syncedTargetTemperature;
            }
        }
        else
        {
            sliderTemperature = syncedTargetTemperature;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0 && isInside(mouseX, mouseY, TARGET_SLIDER_HIT_X, TARGET_SLIDER_HIT_Y, TARGET_SLIDER_HIT_WIDTH, TARGET_SLIDER_HIT_HEIGHT))
        {
            draggingTargetTemperature = true;
            updateSliderTemperature(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (button == 0 && draggingTargetTemperature)
        {
            updateSliderTemperature(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (button == 0 && draggingTargetTemperature)
        {
            draggingTargetTemperature = false;
            sendTargetTemperature(sliderTemperature);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void drawTargetSlider(GuiGraphics graphics)
    {
        graphics.blit(BACKGROUND, leftPos + TARGET_SLIDER_X, topPos + TARGET_SLIDER_Y, 242, 39, TARGET_SLIDER_WIDTH, TARGET_SLIDER_HEIGHT);

        final int targetTemperature = getDisplayedTargetTemperature();
        final int handleY = TARGET_SLIDER_BOTTOM - Math.round(TARGET_SLIDER_RANGE * targetTemperature / (float) ElectricOvenBlockEntity.MAX_TEMPERATURE);
        graphics.blit(BACKGROUND, leftPos + TARGET_HANDLE_X, topPos + handleY, 228, 40, TARGET_HANDLE_WIDTH, TARGET_HANDLE_HEIGHT);
    }

    private void drawTemperature(GuiGraphics graphics)
    {
        graphics.blit(TEMPERATURE_BAR, leftPos + TEMPERATURE_X, topPos + TEMPERATURE_TEXTURE_Y, 0, 0, TEMPERATURE_WIDTH, TEMPERATURE_TEXTURE_HEIGHT, TEMPERATURE_WIDTH, TEMPERATURE_TEXTURE_HEIGHT);

        final int temperature = getSyncedTemperature();
        final int markerY = TEMPERATURE_MARKER_BOTTOM - Math.min(TEMPERATURE_RANGE, Heat.scaleTemperatureForGui(temperature));
        if (temperature > 0)
        {
            graphics.blit(TEMPERATURE_INDICATOR, leftPos + TEMPERATURE_MARKER_X, topPos + markerY, 0, 0, 15, 5, 15, 5);
        }
    }

    private void drawEnergy(GuiGraphics graphics)
    {
        graphics.blit(BACKGROUND, leftPos + ENERGY_X, topPos + ENERGY_Y, 208, 39, ENERGY_WIDTH, ENERGY_HEIGHT);

        final int energy = getSyncedEnergy();
        final int fillHeight = Math.min(ENERGY_FILL_HEIGHT, Math.round(ENERGY_FILL_HEIGHT * energy / (float) ElectricOvenBlockEntity.ENERGY_CAPACITY));
        if (fillHeight > 0)
        {
            final int sourceY = 44 + (ENERGY_FILL_HEIGHT - fillHeight);
            final int destY = ENERGY_FILL_Y + (ENERGY_FILL_HEIGHT - fillHeight);
            graphics.blit(BACKGROUND, leftPos + ENERGY_FILL_X, topPos + destY, 195, sourceY, ENERGY_FILL_WIDTH, fillHeight);
        }
    }

    private void drawCoil(GuiGraphics graphics)
    {
        final int energy = getSyncedEnergy();
        final boolean running = getDisplayedTargetTemperature() > 0 && energy >= ElectricOvenBlockEntity.ENERGY_PER_TICK;
        graphics.blit(BACKGROUND, leftPos + COIL_X, topPos + COIL_Y, 193, running ? 19 : 0, COIL_WIDTH, COIL_HEIGHT);
    }

    private void updateSliderTemperature(double mouseY)
    {
        final int top = TARGET_SLIDER_BOTTOM - TARGET_SLIDER_RANGE;
        final int handleTop = Mth.clamp((int) Math.round(mouseY - topPos - TARGET_HANDLE_CENTER_OFFSET), top, TARGET_SLIDER_BOTTOM);
        final int relative = TARGET_SLIDER_BOTTOM - handleTop;
        sliderTemperature = Math.round(relative * ElectricOvenBlockEntity.MAX_TEMPERATURE / (float) TARGET_SLIDER_RANGE);
    }

    private void sendTargetTemperature(int temperature)
    {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null)
        {
            return;
        }

        final int clamped = Mth.clamp(temperature, 0, ElectricOvenBlockEntity.MAX_TEMPERATURE);
        sliderTemperature = clamped;
        pendingTargetTemperature = clamped;
        pendingTargetTicks = 0;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ElectricOvenContainer.temperatureToButtonId(clamped));
    }

    private void renderCustomTooltips(GuiGraphics graphics, int mouseX, int mouseY)
    {
        if (isInside(mouseX, mouseY, TARGET_SLIDER_HIT_X, TARGET_SLIDER_HIT_Y, TARGET_SLIDER_HIT_WIDTH, TARGET_SLIDER_HIT_HEIGHT))
        {
            final int targetTemperature = getDisplayedTargetTemperature();
            graphics.renderTooltip(font, Component.translatable("tfcelectriccooking.tooltip.target_temperature", targetTemperature), mouseX, mouseY);
        }
        else if (isInside(mouseX, mouseY, TEMPERATURE_X, TEMPERATURE_Y, TEMPERATURE_WIDTH, TEMPERATURE_HEIGHT))
        {
            final var text = TFCConfig.CLIENT.heatTooltipStyle.get().formatColored(getSyncedTemperature());
            if (text != null)
            {
                graphics.renderTooltip(font, text, mouseX, mouseY);
            }
        }
        else if (isInside(mouseX, mouseY, ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT))
        {
            final int energy = getSyncedEnergy();
            graphics.renderTooltip(font, Component.translatable("tfcelectriccooking.tooltip.energy", energy, ElectricOvenBlockEntity.ENERGY_CAPACITY), mouseX, mouseY);
        }
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height)
    {
        return RenderHelpers.isInside((int) mouseX, (int) mouseY, leftPos + x, topPos + y, width, height);
    }

    private int getDisplayedTargetTemperature()
    {
        if (draggingTargetTemperature)
        {
            return sliderTemperature;
        }
        if (pendingTargetTemperature >= 0)
        {
            return pendingTargetTemperature;
        }
        return getSyncedTargetTemperature();
    }

    private int getSyncedTemperature()
    {
        return menu.getBlockEntity().getSyncData().get(ElectricOvenBlockEntity.DATA_TEMPERATURE);
    }

    private int getSyncedTargetTemperature()
    {
        return getSyncedTargetTemperature(menu);
    }

    private static int getSyncedTargetTemperature(ElectricOvenContainer container)
    {
        return container.getBlockEntity().getSyncData().get(ElectricOvenBlockEntity.DATA_TARGET_TEMPERATURE);
    }

    private int getSyncedEnergy()
    {
        final var data = menu.getBlockEntity().getSyncData();
        final int low = data.get(ElectricOvenBlockEntity.DATA_ENERGY_LOW) & 0xFFFF;
        final int high = data.get(ElectricOvenBlockEntity.DATA_ENERGY_HIGH) & 0xFFFF;
        return (high << 16) | low;
    }
}

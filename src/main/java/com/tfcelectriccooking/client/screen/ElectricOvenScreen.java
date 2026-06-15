package com.tfcelectriccooking.client.screen;

import com.tfcelectriccooking.TFCElectricCooking;
import com.tfcelectriccooking.common.blockentity.ElectricOvenBlockEntity;
import com.tfcelectriccooking.common.container.ElectricOvenContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ElectricOvenScreen extends AbstractContainerScreen<ElectricOvenContainer>
{
    private static final ResourceLocation BACKGROUND = new ResourceLocation(TFCElectricCooking.MOD_ID, "textures/gui/electric_oven.png");
    private static final int SLOT_SIZE = 18;
    private static final int CONTROL_X = 8;
    private static final int INPUT_Y = 68;
    private static final int BUTTON_Y = 86;
    private static final int TEMPERATURE_PANEL_X = 21;
    private static final int TEMPERATURE_PANEL_Y = 21;
    private static final int TEMPERATURE_PANEL_WIDTH = 14;
    private static final int TEMPERATURE_PANEL_HEIGHT = 44;
    private static final int TEMPERATURE_BAR_X = TEMPERATURE_PANEL_X + 4;
    private static final int TEMPERATURE_BAR_WIDTH = 6;
    private static final int TEMPERATURE_BAR_BOTTOM = TEMPERATURE_PANEL_Y + TEMPERATURE_PANEL_HEIGHT - 3;
    private static final int TEMPERATURE_BAR_MAX_HEIGHT = 34;
    private static final int TEMPERATURE_TEXT_Y = 12;

    private EditBox temperatureInput;

    public ElectricOvenScreen(ElectricOvenContainer container, Inventory playerInv, Component title)
    {
        super(container, playerInv, title);
        imageWidth = 176;
        imageHeight = 196;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init()
    {
        super.init();

        temperatureInput = new EditBox(font, leftPos + CONTROL_X, topPos + INPUT_Y, 40, 14, Component.empty());
        temperatureInput.setMaxLength(3);
        temperatureInput.setValue(String.valueOf(menu.getBlockEntity().getSyncData().get(1)));
        temperatureInput.setTextColor(0xFFFFFF);
        addRenderableWidget(temperatureInput);

        addRenderableWidget(Button.builder(Component.translatable("tfcelectriccooking.gui.set"), button -> sendTemperature())
            .bounds(leftPos + CONTROL_X, topPos + BUTTON_Y, 40, 12)
            .build());
    }

    private void sendTemperature()
    {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null)
        {
            return;
        }

        try
        {
            int temp = Integer.parseInt(temperatureInput.getValue().trim());
            temp = Math.max(0, Math.min(ElectricOvenBlockEntity.MAX_TEMPERATURE, temp));
            temperatureInput.setValue(String.valueOf(temp));
            if (menu.clickMenuButton(minecraft.player, temp))
            {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, temp);
            }
        }
        catch (NumberFormatException ignored)
        {
            temperatureInput.setValue("0");
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == 257 && temperatureInput.isFocused())
        {
            sendTemperature();
            return true;
        }
        if (temperatureInput.isFocused())
        {
            return temperatureInput.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        for (int row = 0; row < 2; row++)
        {
            for (int col = 0; col < 3; col++)
            {
                drawSlot(graphics, leftPos + 70 + col * SLOT_SIZE, topPos + 23 + row * SLOT_SIZE);
            }
        }

        final int temp = menu.getBlockEntity().getSyncData().get(0);
        drawPanel(graphics, leftPos + TEMPERATURE_PANEL_X, topPos + TEMPERATURE_PANEL_Y, TEMPERATURE_PANEL_WIDTH, TEMPERATURE_PANEL_HEIGHT);
        final int barHeight = Math.min(TEMPERATURE_BAR_MAX_HEIGHT, (int) (TEMPERATURE_BAR_MAX_HEIGHT * temp / (float) ElectricOvenBlockEntity.MAX_TEMPERATURE));
        if (barHeight > 0)
        {
            graphics.fill(leftPos + TEMPERATURE_BAR_X, topPos + TEMPERATURE_BAR_BOTTOM - barHeight, leftPos + TEMPERATURE_BAR_X + TEMPERATURE_BAR_WIDTH, topPos + TEMPERATURE_BAR_BOTTOM, 0xFFFF7A1A);
        }

        final int energy = menu.getBlockEntity().getSyncData().get(2);
        drawPanel(graphics, leftPos + 153, topPos + 23, 12, 48);
        final int energyBarHeight = Math.min(44, (int) (44f * energy / ElectricOvenBlockEntity.ENERGY_CAPACITY));
        if (energyBarHeight > 0)
        {
            graphics.fill(leftPos + 155, topPos + 69 - energyBarHeight, leftPos + 163, topPos + 69, 0xFF4CAF50);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        final int currentTemp = menu.getBlockEntity().getSyncData().get(0);
        final String temperatureText = currentTemp + "\u00B0C";
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 6, 0x404040, false);
        graphics.drawString(font, temperatureText, getTemperatureCenterX() - font.width(temperatureText) / 2, TEMPERATURE_TEXT_Y, 0xD96817, false);

        final int energy = menu.getBlockEntity().getSyncData().get(2);
        final String energyText = energy + " FE";
        graphics.drawString(font, energyText, imageWidth - 8 - font.width(energyText), 76, 0x2E8B57, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y)
    {
        drawPanel(graphics, x, y, SLOT_SIZE, SLOT_SIZE);
    }

    private int getTemperatureCenterX()
    {
        return TEMPERATURE_PANEL_X + TEMPERATURE_PANEL_WIDTH / 2;
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height)
    {
        graphics.fill(x, y, x + width, y + height, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + width - 2, y + height - 2, 0xFFC6C6C6);
        graphics.fill(x + 2, y + 2, x + width - 1, y + height - 1, 0xFF555555);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF171717);
    }
}

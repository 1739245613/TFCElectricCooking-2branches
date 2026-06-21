package com.tfcelectriccooking.client.screen;

import com.tfcelectriccooking.TFCElectricCooking;
import com.tfcelectriccooking.common.blockentity.ElectricSoupPotBlockEntity;
import com.tfcelectriccooking.common.container.ElectricSoupPotContainer;
import java.util.ArrayList;
import java.util.List;
import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.common.capabilities.heat.Heat;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.recipes.JamPotRecipe;
import net.dries007.tfc.common.recipes.PotRecipe;
import net.dries007.tfc.compat.jade.common.BlockEntityTooltip;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Tooltips;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fluids.FluidStack;

public class ElectricSoupPotScreen extends AbstractContainerScreen<ElectricSoupPotContainer>
{
    private static final ResourceLocation BACKGROUND = new ResourceLocation(TFCElectricCooking.MOD_ID, "textures/gui/electric_soup_pot.png");
    private static final ResourceLocation TEMPERATURE_BAR = new ResourceLocation(TFCElectricCooking.MOD_ID, "textures/gui/temperature_bar.png");
    private static final ResourceLocation TEMPERATURE_INDICATOR = new ResourceLocation(TFCElectricCooking.MOD_ID, "textures/gui/temperature_indicator.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 186;
    private static final int POWER_ON_TEMPERATURE = 350;

    private static final int POWER_BUTTON_X = 8;
    private static final int POWER_BUTTON_Y = 80;
    private static final int POWER_BUTTON_WIDTH = 17;
    private static final int POWER_BUTTON_HEIGHT = 17;

    private static final int TEMPERATURE_X = 8;
    private static final int TEMPERATURE_Y = 16;
    private static final int TEMPERATURE_WIDTH = 17;
    private static final int TEMPERATURE_HEIGHT = 62;
    private static final int TEMPERATURE_TEXTURE_Y = 10;
    private static final int TEMPERATURE_TEXTURE_HEIGHT = 74;
    private static final int TEMPERATURE_RANGE = 51;
    private static final int TEMPERATURE_MARKER_X = 9;
    private static final int TEMPERATURE_MARKER_BOTTOM = 74;

    private static final int ENERGY_X = 150;
    private static final int ENERGY_Y = 16;
    private static final int ENERGY_WIDTH = 17;
    private static final int ENERGY_HEIGHT = 62;
    private static final int ENERGY_FILL_X = 155;
    private static final int ENERGY_FILL_Y = 21;
    private static final int ENERGY_FILL_WIDTH = 7;
    private static final int ENERGY_FILL_HEIGHT = 52;
    private static final int ENERGY_FILL_SRC_X = 213;
    private static final int ENERGY_FILL_SRC_Y = 4;

    private static final int POT_CONTENT_X = 102;
    private static final int POT_CONTENT_Y = 40;
    private static final int POT_CONTENT_WIDTH = 38;
    private static final int POT_CONTENT_HEIGHT = 34;
    private static final int POT_CONTENT_SRC_X = 218;
    private static final int POT_CONTENT_SRC_Y = 95;
    private static final int POT_CONTENT_VISIBLE_HEIGHT = POT_CONTENT_HEIGHT;
    private static final int POT_CONTENT_STEPS = 10;

    private static final int BUBBLES_X = 109;
    private static final int BUBBLES_Y = 16;
    private static final int BUBBLE_COLUMN_GAP = 14;
    private static final int BUBBLE_COLUMN_WIDTH = 14;
    private static final int BUBBLE_COLUMN_HEIGHT = 22;
    private static final int BOILING_BUBBLES_SRC_X = 226;
    private static final int BOILING_BUBBLES_SRC_Y = 66;
    private static final int IDLE_BUBBLES_SRC_X = 240;
    private static final int IDLE_BUBBLES_SRC_Y = 66;

    private static final int OUTPUT_TEXT_CENTER_X = 120;
    private static final int OUTPUT_TEXT_Y = 79;
    private static final int OUTPUT_TEXT_MAX_WIDTH = 78;
    private static final String JAM_SUFFIX = "\u679c\u9171";
    private static final String[] JAM_DISPLAY_PREFIXES = {
        "\u9521\u76d6\u5bc6\u5c01\u7684",
        "\u94a2\u76d6\u5bc6\u5c01\u7684",
        "\u94dd\u76d6\u5bc6\u5c01\u7684",
        "\u94c1\u76d6\u5bc6\u5c01\u7684",
        "\u94dc\u76d6\u5bc6\u5c01\u7684",
        "\u672a\u5bc6\u5c01\u7684",
        "\u5df2\u5bc6\u5c01\u7684",
        "\u5bc6\u5c01\u7684",
        "\u5c01\u597d\u7684",
        "\u5c01\u53e3\u7684",
        "\u5e26\u76d6\u7684",
        "\u6709\u76d6\u7684",
        "\u65e0\u76d6\u7684",
        "\u9521\u76d6\u7684",
        "\u94a2\u76d6\u7684",
        "\u94dd\u76d6\u7684",
        "\u94c1\u76d6\u7684",
        "\u94dc\u76d6\u7684",
        "\u9521\u76d6",
        "\u94a2\u76d6",
        "\u94dd\u76d6",
        "\u94c1\u76d6",
        "\u94dc\u76d6",
        "\u5bc6\u5c01"
    };

    public ElectricSoupPotScreen(ElectricSoupPotContainer container, Inventory playerInv, Component title)
    {
        super(container, playerInv, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        drawTemperature(graphics);
        drawEnergy(graphics);
        drawPowerButton(graphics);
        drawPotContents(graphics);
        drawBubbles(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        final Component outputText = getOutputText();
        if (!outputText.getString().isEmpty())
        {
            drawCenteredTrimmedString(graphics, outputText, OUTPUT_TEXT_CENTER_X, OUTPUT_TEXT_Y, OUTPUT_TEXT_MAX_WIDTH, 0x404040);
        }
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
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0 && isInside(mouseX, mouseY, POWER_BUTTON_X, POWER_BUTTON_Y, POWER_BUTTON_WIDTH, POWER_BUTTON_HEIGHT))
        {
            final int targetTemperature = menu.getBlockEntity().getSyncData().get(1);
            sendTargetTemperature(targetTemperature > 0 ? 0 : POWER_ON_TEMPERATURE);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawTemperature(GuiGraphics graphics)
    {
        graphics.blit(TEMPERATURE_BAR, leftPos + TEMPERATURE_X, topPos + TEMPERATURE_TEXTURE_Y, 0, 0, TEMPERATURE_WIDTH, TEMPERATURE_TEXTURE_HEIGHT, TEMPERATURE_WIDTH, TEMPERATURE_TEXTURE_HEIGHT);

        final int temperature = menu.getBlockEntity().getSyncData().get(0);
        if (temperature > 0)
        {
            final int markerY = TEMPERATURE_MARKER_BOTTOM - Math.min(TEMPERATURE_RANGE, Heat.scaleTemperatureForGui(temperature));
            graphics.blit(TEMPERATURE_INDICATOR, leftPos + TEMPERATURE_MARKER_X, topPos + markerY, 0, 0, 15, 5, 15, 5);
        }
    }

    private void drawEnergy(GuiGraphics graphics)
    {
        graphics.blit(BACKGROUND, leftPos + ENERGY_X, topPos + ENERGY_Y, 221, 0, ENERGY_WIDTH, ENERGY_HEIGHT);

        final int energy = menu.getBlockEntity().getSyncData().get(2);
        final int fillHeight = Math.min(ENERGY_FILL_HEIGHT, Math.round(ENERGY_FILL_HEIGHT * energy / (float) ElectricSoupPotBlockEntity.ENERGY_CAPACITY));
        if (fillHeight > 0)
        {
            final int sourceY = ENERGY_FILL_SRC_Y + (ENERGY_FILL_HEIGHT - fillHeight);
            final int destY = ENERGY_FILL_Y + (ENERGY_FILL_HEIGHT - fillHeight);
            graphics.blit(BACKGROUND, leftPos + ENERGY_FILL_X, topPos + destY, ENERGY_FILL_SRC_X, sourceY, ENERGY_FILL_WIDTH, fillHeight);
        }
    }

    private void drawPowerButton(GuiGraphics graphics)
    {
        final int targetTemperature = menu.getBlockEntity().getSyncData().get(1);
        graphics.blit(BACKGROUND, leftPos + POWER_BUTTON_X, topPos + POWER_BUTTON_Y, 239, targetTemperature > 0 ? 19 : 0, POWER_BUTTON_WIDTH, POWER_BUTTON_HEIGHT);
    }

    private void drawPotContents(GuiGraphics graphics)
    {
        final PotRecipe.Output output = menu.getBlockEntity().getOutput();
        final FluidStack fluid = menu.getBlockEntity().getFluidInTank();

        final int steps = getPotContentSteps(output, fluid);
        if (steps > 0)
        {
            final int fillHeight = Math.max(1, Math.round(POT_CONTENT_VISIBLE_HEIGHT * steps / (float) POT_CONTENT_STEPS));
            final int sourceY = POT_CONTENT_SRC_Y + POT_CONTENT_HEIGHT - fillHeight;
            final int destY = POT_CONTENT_Y + POT_CONTENT_HEIGHT - fillHeight;
            if (output != null && !output.isEmpty())
            {
                final int color = getFallbackPotContentColor(output, fluid);
                if (color != -1)
                {
                    RenderHelpers.setShaderColor(graphics, color);
                    graphics.blit(BACKGROUND, leftPos + POT_CONTENT_X, topPos + destY, POT_CONTENT_SRC_X, sourceY, POT_CONTENT_WIDTH, fillHeight);
                    RenderHelpers.setShaderColor(graphics, 0xFFFFFFFF);
                }
            }
            else if (!drawFluidPotContent(graphics, fluid, leftPos + POT_CONTENT_X, topPos + destY, POT_CONTENT_WIDTH, fillHeight))
            {
                final int color = getFallbackPotContentColor(output, fluid);
                if (color != -1)
                {
                    RenderHelpers.setShaderColor(graphics, color);
                    graphics.blit(BACKGROUND, leftPos + POT_CONTENT_X, topPos + destY, POT_CONTENT_SRC_X, sourceY, POT_CONTENT_WIDTH, fillHeight);
                    RenderHelpers.setShaderColor(graphics, 0xFFFFFFFF);
                }
            }
        }
    }

    private boolean drawFluidPotContent(GuiGraphics graphics, FluidStack fluid, int x, int y, int width, int height)
    {
        if (fluid.isEmpty())
        {
            return false;
        }
        try
        {
            final var sprite = RenderHelpers.getAndBindFluidSprite(fluid);
            final int firstRow = y - (topPos + POT_CONTENT_Y);
            for (int row = 0; row < height; row++)
            {
                final int sourceRow = firstRow + row;
                final int[] bounds = getPotContentRowBounds(sourceRow);
                final int rowWidth = bounds[1] - bounds[0] + 1;
                if (rowWidth > 0)
                {
                    drawFluidSpriteRow(graphics, sprite, x + bounds[0], y + row, rowWidth, bounds[0], sourceRow);
                }
            }
            RenderHelpers.setShaderColor(graphics, 0xFFFFFFFF);
            return true;
        }
        catch (RuntimeException ignored)
        {
            RenderHelpers.setShaderColor(graphics, 0xFFFFFFFF);
        }
        return false;
    }

    private void drawFluidSpriteRow(GuiGraphics graphics, TextureAtlasSprite sprite, int x, int y, int width, int sourceX, int sourceY)
    {
        int remaining = width;
        int destX = x;
        int tileX = Math.floorMod(sourceX, 16);
        final int tileY = Math.floorMod(sourceY, 16);

        while (remaining > 0)
        {
            final int drawWidth = Math.min(remaining, 16 - tileX);
            RenderHelpers.blit(
                graphics,
                destX,
                y,
                drawWidth,
                1,
                sprite.getU(tileX),
                sprite.getU(tileX + drawWidth),
                sprite.getV(tileY),
                sprite.getV(tileY + 1)
            );
            destX += drawWidth;
            remaining -= drawWidth;
            tileX = 0;
        }
    }

    private int[] getPotContentRowBounds(int row)
    {
        final int inset = Math.max(0, row - (POT_CONTENT_HEIGHT - 5));
        return new int[] {inset, POT_CONTENT_WIDTH - 1 - inset};
    }

    private int getFallbackPotContentColor(PotRecipe.Output output, FluidStack fluid)
    {
        if (output != null && !output.isEmpty())
        {
            final int color = output.getFluidColor();
            return color == -1 ? 0xFFB85C24 : color;
        }
        if (!fluid.isEmpty())
        {
            try
            {
                return RenderHelpers.getFluidColor(fluid);
            }
            catch (RuntimeException ignored)
            {
                return 0xFFFFFFFF;
            }
        }
        return -1;
    }

    private void drawBubbles(GuiGraphics graphics)
    {
        final PotRecipe.Output output = menu.getBlockEntity().getOutput();
        final FluidStack fluid = menu.getBlockEntity().getFluidInTank();
        if ((output == null || output.isEmpty()) && fluid.isEmpty() && menu.getBlockEntity().getSyncData().get(4) <= 0)
        {
            return;
        }

        drawBubbleColumns(graphics, IDLE_BUBBLES_SRC_X, IDLE_BUBBLES_SRC_Y, BUBBLES_Y, BUBBLE_COLUMN_HEIGHT);

        final int boilingTicks = menu.getBlockEntity().getSyncData().get(7);
        if (boilingTicks > 0)
        {
            final int bubbleHeight = Math.max(1, Math.min(BUBBLE_COLUMN_HEIGHT, (boilingTicks % 35) * BUBBLE_COLUMN_HEIGHT / 35));
            final int sourceY = BOILING_BUBBLES_SRC_Y + (BUBBLE_COLUMN_HEIGHT - bubbleHeight);
            final int destY = BUBBLES_Y + (BUBBLE_COLUMN_HEIGHT - bubbleHeight);
            drawBubbleColumns(graphics, BOILING_BUBBLES_SRC_X, sourceY, destY, bubbleHeight);
        }
    }

    private void drawBubbleColumns(GuiGraphics graphics, int sourceX, int sourceY, int destY, int height)
    {
        graphics.blit(BACKGROUND, leftPos + BUBBLES_X, topPos + destY, sourceX, sourceY, BUBBLE_COLUMN_WIDTH, height);
        graphics.blit(BACKGROUND, leftPos + BUBBLES_X + BUBBLE_COLUMN_GAP, topPos + destY, sourceX, sourceY, BUBBLE_COLUMN_WIDTH, height);
    }

    private void sendTargetTemperature(int temperature)
    {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null)
        {
            return;
        }

        final int clamped = Mth.clamp(temperature, 0, ElectricSoupPotBlockEntity.MAX_TEMPERATURE);
        if (menu.clickMenuButton(minecraft.player, clamped))
        {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, clamped);
        }
    }

    private Component getOutputText()
    {
        return getRecipeOutputText();
    }

    private Component getRecipeOutputText()
    {
        final PotRecipe.Output output = menu.getBlockEntity().getOutput();
        if (output != null && !output.isEmpty())
        {
            final BlockEntityTooltip tooltip = output.getTooltip();
            if (tooltip != null && menu.getBlockEntity().getLevel() != null)
            {
                final List<Component> fakeTooltip = new ArrayList<>();
                tooltip.display(menu.getBlockEntity().getLevel(), menu.getBlockEntity().getBlockState(), menu.getBlockEntity().getBlockPos(), menu.getBlockEntity(), fakeTooltip::add);
                if (!fakeTooltip.isEmpty())
                {
                    return formatOutputText(output, fakeTooltip.get(0));
                }
            }
            return Component.translatable("tfcelectriccooking.gui.done");
        }
        return Component.empty();
    }

    private Component formatOutputText(PotRecipe.Output output, Component component)
    {
        if (output instanceof JamPotRecipe.JamOutput)
        {
            final String text = component.getString();
            final String cleaned = stripJamDisplayPrefix(text);
            if (!cleaned.equals(text))
            {
                return Component.literal(cleaned).withStyle(component.getStyle());
            }
        }
        return component;
    }

    private String stripJamDisplayPrefix(String text)
    {
        if (!text.contains(JAM_SUFFIX))
        {
            return text;
        }

        int index = 0;
        while (index < text.length() && Character.isWhitespace(text.charAt(index)))
        {
            index++;
        }

        final int countStart = index;
        while (index < text.length() && Character.isDigit(text.charAt(index)))
        {
            index++;
        }

        boolean removedCount = false;
        if (index > countStart)
        {
            int afterCount = index;
            while (afterCount < text.length() && Character.isWhitespace(text.charAt(afterCount)))
            {
                afterCount++;
            }
            if (afterCount < text.length() && isCountSeparator(text.charAt(afterCount)))
            {
                afterCount++;
                while (afterCount < text.length() && Character.isWhitespace(text.charAt(afterCount)))
                {
                    afterCount++;
                }
                removedCount = true;
                index = afterCount;
            }
            else
            {
                index = countStart;
            }
        }

        final String originalName = text.substring(index).stripLeading();
        String name = originalName;
        boolean removedPrefix = false;
        boolean stripped;
        do
        {
            stripped = false;
            for (String prefix : JAM_DISPLAY_PREFIXES)
            {
                if (name.startsWith(prefix))
                {
                    name = name.substring(prefix.length()).stripLeading();
                    removedPrefix = true;
                    stripped = true;
                    break;
                }
            }
        } while (stripped);

        if (name.isEmpty() || !name.contains(JAM_SUFFIX) || (!removedCount && !removedPrefix))
        {
            return text;
        }
        return name;
    }

    private boolean isCountSeparator(char value)
    {
        return value == 'x' || value == 'X' || value == '\u00d7';
    }

    private int getPotContentSteps(PotRecipe.Output output, FluidStack fluid)
    {
        if (output != null && !output.isEmpty())
        {
            return POT_CONTENT_STEPS;
        }
        if (!fluid.isEmpty())
        {
            return Mth.clamp((fluid.getAmount() + FluidHelpers.BUCKET_VOLUME / POT_CONTENT_STEPS - 1) / (FluidHelpers.BUCKET_VOLUME / POT_CONTENT_STEPS), 1, POT_CONTENT_STEPS);
        }
        return 0;
    }

    private void drawCenteredTrimmedString(GuiGraphics graphics, Component component, int centerX, int y, int maxWidth, int color)
    {
        String text = component.getString();
        while (!text.isEmpty() && font.width(text) > maxWidth)
        {
            text = text.substring(0, text.length() - 1);
        }
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private void renderCustomTooltips(GuiGraphics graphics, int mouseX, int mouseY)
    {
        if (isInside(mouseX, mouseY, POWER_BUTTON_X, POWER_BUTTON_Y, POWER_BUTTON_WIDTH, POWER_BUTTON_HEIGHT))
        {
            final int targetTemperature = menu.getBlockEntity().getSyncData().get(1);
            graphics.renderTooltip(font, Component.translatable(targetTemperature > 0 ? "tfcelectriccooking.tooltip.running" : "tfcelectriccooking.tooltip.stopped"), mouseX, mouseY);
        }
        else if (isInside(mouseX, mouseY, TEMPERATURE_X, TEMPERATURE_Y, TEMPERATURE_WIDTH, TEMPERATURE_HEIGHT))
        {
            final var text = TFCConfig.CLIENT.heatTooltipStyle.get().formatColored(menu.getBlockEntity().getSyncData().get(0));
            if (text != null)
            {
                graphics.renderTooltip(font, text, mouseX, mouseY);
            }
        }
        else if (isInside(mouseX, mouseY, ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT))
        {
            final int energy = menu.getBlockEntity().getSyncData().get(2);
            graphics.renderTooltip(font, Component.translatable("tfcelectriccooking.tooltip.energy", energy, ElectricSoupPotBlockEntity.ENERGY_CAPACITY), mouseX, mouseY);
        }
        else if (isInside(mouseX, mouseY, POT_CONTENT_X, POT_CONTENT_Y, POT_CONTENT_WIDTH, POT_CONTENT_HEIGHT))
        {
            final PotRecipe.Output output = menu.getBlockEntity().getOutput();
            final FluidStack fluid = menu.getBlockEntity().getFluidInTank();
            if ((output == null || output.isEmpty()) && !fluid.isEmpty())
            {
                graphics.renderTooltip(font, Tooltips.fluidUnitsAndCapacityOf(fluid, FluidHelpers.BUCKET_VOLUME), mouseX, mouseY);
            }
        }
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height)
    {
        return RenderHelpers.isInside((int) mouseX, (int) mouseY, leftPos + x, topPos + y, width, height);
    }
}

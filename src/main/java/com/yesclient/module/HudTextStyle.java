package com.yesclient.module;

import java.awt.Color;

import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ColorSetting;
import com.yesclient.module.settings.ModeSetting;
import com.yesclient.module.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

 
public final class HudTextStyle {

    private final Module owner;
    private final BooleanSetting showLabel;
    private final ModeSetting brackets;
    private final ColorSetting textColor;
    private final BooleanSetting valueColoring;
    private final NumberSetting lowValue;
    private final NumberSetting highValue;
    private final ColorSetting lowColor;
    private final ColorSetting highColor;

    public HudTextStyle(Module owner, boolean showLabelByDefault) {
        this(owner, showLabelByDefault, false, 0.0D, 100.0D, 0xFFFF5555, 0xFF55FF55);
    }

    public HudTextStyle(Module owner, boolean showLabelByDefault, double lowDefault, double highDefault,
            double maxValue, int lowColorDefault, int highColorDefault) {
        this(owner, showLabelByDefault, true, lowDefault, highDefault, maxValue, lowColorDefault, highColorDefault,
                false);
    }

    public HudTextStyle(Module owner, boolean showLabelByDefault, double lowDefault, double highDefault,
            double maxValue, int lowColorDefault, int highColorDefault, boolean colorByValueDefault) {
        this(owner, showLabelByDefault, true, lowDefault, highDefault, maxValue, lowColorDefault, highColorDefault,
                colorByValueDefault);
    }

    private HudTextStyle(Module owner, boolean showLabelByDefault, boolean supportsValueColor,
            double lowDefault, double highDefault, int lowColorDefault, int highColorDefault) {
        this(owner, showLabelByDefault, supportsValueColor, lowDefault, highDefault,
                Math.max(Math.max(Math.abs(lowDefault), Math.abs(highDefault)) * 4.0D, 100.0D),
                lowColorDefault, highColorDefault, false);
    }

    private HudTextStyle(Module owner, boolean showLabelByDefault, boolean supportsValueColor,
            double lowDefault, double highDefault, double maxValue, int lowColorDefault, int highColorDefault,
            boolean colorByValueDefault) {
        this.owner = owner;
        showLabel = new BooleanSetting("Show Label", "Show the HUD item's name before its value", showLabelByDefault);
        brackets = new ModeSetting("Brackets", "Wrap the HUD text in brackets", "None",
                "None", "Square", "Round", "Curly");
        textColor = new ColorSetting("Text Color", "HUD text color", new Color(170, 170, 170, 255));
        owner.addSetting(showLabel);
        owner.addSetting(brackets);
        owner.addSetting(textColor);

        if (supportsValueColor) {
            valueColoring = new BooleanSetting("Color by Value", "Blend between colors based on the current value",
                    colorByValueDefault);
            lowValue = new NumberSetting("Low Color At", "Value that uses the low color", lowDefault, 0.0D, maxValue, 1.0D);
            highValue = new NumberSetting("High Color At", "Value that uses the high color", highDefault, 1.0D, maxValue, 1.0D);
            lowColor = new ColorSetting("Low Value Color", "Color at the low end of the range",
                    new Color(lowColorDefault, true));
            highColor = new ColorSetting("High Value Color", "Color at the high end of the range",
                    new Color(highColorDefault, true));
            owner.addSetting(valueColoring);
            owner.addSetting(lowValue);
            owner.addSetting(highValue);
            owner.addSetting(lowColor);
            owner.addSetting(highColor);
            lowValue.setVisibility(() -> valueColoring.isEnabled());
            highValue.setVisibility(() -> valueColoring.isEnabled());
            lowColor.setVisibility(() -> valueColoring.isEnabled());
            highColor.setVisibility(() -> valueColoring.isEnabled());
        } else {
            valueColoring = null;
            lowValue = null;
            highValue = null;
            lowColor = null;
            highColor = null;
        }
    }

    public String format(String label, String value, String suffix) {
        String content = value == null ? "" : value;
        if (suffix != null) content += suffix;
        if (showLabel.isEnabled() && label != null && !label.isEmpty()) content = label + ": " + content;
        switch (brackets.getValue()) {
            case "Square": return "[" + content + "]";
            case "Round": return "(" + content + ")";
            case "Curly": return "{" + content + "}";
            default: return content;
        }
    }

    public int getColor(double value) {
        if (valueColoring == null || !valueColoring.isEnabled() || Double.isNaN(value)
                || Double.isInfinite(value)) {
            return textColor.getRGB();
        }
        double low = lowValue.getValue();
        double high = Math.max(low + 1.0D, highValue.getValue());
        float amount = (float) Math.max(0.0D, Math.min(1.0D, (value - low) / (high - low)));
        return blend(lowColor.getRGB(), highColor.getRGB(), amount);
    }

    public int draw(String label, String value, String suffix, double metric, int x, int y) {
        return drawFixedColor(label, value, suffix, getColor(metric), x, y);
    }

    public int drawFixedColor(String label, String value, String suffix, int color, int x, int y) {
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        String text = format(label, value, suffix);
        font.drawStringWithShadow(text, x, y, color);
        owner.setWidth(font.getStringWidth(text));
        owner.setHeight(font.FONT_HEIGHT);
        return color;
    }

    private static int blend(int first, int second, float amount) {
        int a = mix(first >>> 24 & 255, second >>> 24 & 255, amount);
        int r = mix(first >>> 16 & 255, second >>> 16 & 255, amount);
        int g = mix(first >>> 8 & 255, second >>> 8 & 255, amount);
        int b = mix(first & 255, second & 255, amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int mix(int first, int second, float amount) {
        return Math.round(first + (second - first) * amount);
    }
}

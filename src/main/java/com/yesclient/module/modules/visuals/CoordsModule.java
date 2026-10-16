package com.yesclient.module.modules.visuals;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.HudTextStyle;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.HudDesignerModule;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ModeSetting;
import net.minecraft.client.gui.ScaledResolution;

 


public class CoordsModule extends Module {

    private final HudScale hudScale = new HudScale(this);
    private final BooleanSetting simplified = new BooleanSetting("Simplified Coordinates",
            "Round coordinates to whole numbers", false);
    private final ModeSetting layout = new ModeSetting("Layout", "Arrange coordinates on one line or stacked",
            "Inline", "Inline", "Stacked");
    private final HudTextStyle textStyle = new HudTextStyle(this, true);

    public CoordsModule() {
        super("Coords", "Shows your XYZ coordinates", Category.VISUALS, ModuleMode.BOTH);
        addSetting(simplified);
        addSetting(layout);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null)
            return;

        ScaledResolution sr = new ScaledResolution(mc);
         
        HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getCoordsX() : 5;
        int y = designer != null ? designer.getCoordsY(sr.getScaledHeight()) : sr.getScaledHeight() - 25;

        hudScale.beginAt(x, y);
        if (layout.is("Stacked")) {
            String[] axes = {"X", "Y", "Z"};
            double[] values = {mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ};
            int maxWidth = 0;
            int lineHeight = mc.fontRendererObj.FONT_HEIGHT + 1;
            for (int i = 0; i < axes.length; i++) {
                String value = formatCoordinate(values[i]);
                String text = textStyle.format(axes[i], value, "");
                mc.fontRendererObj.drawStringWithShadow(text, x, y + i * lineHeight, textStyle.getColor(Double.NaN));
                maxWidth = Math.max(maxWidth, mc.fontRendererObj.getStringWidth(text));
            }
            setWidth(maxWidth);
            setHeight(lineHeight * 3 - 1);
        } else {
            String coordinates = formatCoordinate(mc.thePlayer.posX) + " / "
                    + formatCoordinate(mc.thePlayer.posY) + " / " + formatCoordinate(mc.thePlayer.posZ);
            textStyle.draw("XYZ", coordinates, "", Double.NaN, x, y);
        }
        hudScale.end();
    }

    private String formatCoords() {
        if (simplified.isEnabled()) {
            return String.format("XYZ: %.0f, %.0f, %.0f",
                    mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        }
         
        return String.format("XYZ: %.3f / %.3f / %.3f",
                mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
    }

    private String formatCoordinate(double coordinate) {
        return simplified.isEnabled() ? String.format("%.0f", coordinate) : String.format("%.3f", coordinate);
    }

    @Override
    public String getDisplayInfo() {
        if (mc.thePlayer == null) {
            return "";
        }
        if (simplified.isEnabled()) {
            return String.format("%.0f, %.0f, %.0f",
                    mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        }
        return String.format("%.3f / %.3f / %.3f",
                mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
    }
}

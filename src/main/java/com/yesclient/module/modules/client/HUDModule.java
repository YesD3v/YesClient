package com.yesclient.module.modules.client;
import java.awt.Color;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ColorSetting;
import com.yesclient.module.settings.ModeSetting;
import com.yesclient.module.settings.NumberSetting;


public class HUDModule extends Module {


     
    private final ModeSetting moduleListPos = new ModeSetting("Position", "Module list position", "Top Right",
            "Top Right", "Top Left", "Bottom Right", "Bottom Left");
    private final ModeSetting moduleListSort = new ModeSetting("Sort", "How to sort modules", "Length", "Length",
            "Alphabetical");
    private final BooleanSetting moduleListBackground = new BooleanSetting("Background",
            "Show background behind list", true);
    private final NumberSetting backgroundPadding = new NumberSetting("Background Size",
            "Padding around module text", 3.5, 0.0, 10.0, 0.5);
    private final ColorSetting primaryColor = new ColorSetting("Primary Color", "Main text color",
            new Color(255, 255, 255));
    private final ColorSetting secondaryColor = new ColorSetting("Secondary Color", "Accent color",
            new Color(170, 170, 170));

    public HUDModule() {
        super("Module List", "Display enabled modules on screen", Category.CLIENT, ModuleMode.BOTH);



        addSetting(moduleListPos);
        addSetting(moduleListSort);
        addSetting(moduleListBackground);
        addSetting(backgroundPadding);
        addSetting(primaryColor);
        addSetting(secondaryColor);

         
        backgroundPadding.setVisibility(() -> moduleListBackground.isEnabled());

        setEnabled(true);  
    }


    public String getModuleListPos() {
        return moduleListPos.getValue();
    }

    public String getModuleListSort() {
        return moduleListSort.getValue();
    }

    public boolean showModuleListBackground() {
        return moduleListBackground.isEnabled();
    }

    public double getBackgroundPadding() {
        return backgroundPadding.getValue();
    }

    public Color getPrimaryColor() {
        if (primaryColor.getRGB() == new Color(0, 200, 255).getRGB()) {
            primaryColor.setValue(new Color(255, 255, 255));
        }
        if (secondaryColor.getRGB() == new Color(200, 0, 255).getRGB()) {
            secondaryColor.setValue(new Color(170, 170, 170));
        }
        return primaryColor.getValue();
    }

    public Color getSecondaryColor() {
        return secondaryColor.getValue();
    }
}

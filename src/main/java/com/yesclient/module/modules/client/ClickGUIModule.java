package com.yesclient.module.modules.client;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ModeSetting;
import org.lwjgl.input.Keyboard;

public class ClickGUIModule extends Module {
    private final HudScale watermarkScale;

    private final BooleanSetting sounds = new BooleanSetting("Click Sounds",
            "Play polished clicks and toggle sounds across the interface", true);
    private final BooleanSetting snapToGrid = new BooleanSetting("Snap to Grid", "Snap panels to grid", false);
    private final BooleanSetting notifications = new BooleanSetting("Notifications", "Show toggle notifications", true);
    private final BooleanSetting blatantMode = new BooleanSetting("Blatant Mode", "Enable risky modules", false);
    private final BooleanSetting watermark = new BooleanSetting("Watermark", "Show YesClient watermark", true);
    private final ModeSetting fontMode = new ModeSetting("Font", "The font to use", "Fancy", "Regular", "Fancy");

    public ClickGUIModule() {
        super("ClickGUI", "Displays this menu", Category.CLIENT, ModuleMode.BOTH, Keyboard.KEY_RSHIFT);
        
        addSetting(sounds);
        addSetting(snapToGrid);
        addSetting(notifications);
        blatantMode.setOnValueChanged(enabled -> {
            if (enabled || YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null) {
                return;
            }
            com.yesclient.module.modules.exploit.ProjectileAimbotModule aimbot = YesClient.getInstance()
                    .getModuleManager().getModule(com.yesclient.module.modules.exploit.ProjectileAimbotModule.class);
            if (aimbot != null) {
                aimbot.setAssistMode();
            }
        });
        addSetting(blatantMode);
        addSetting(watermark);
        watermarkScale = new HudScale(this, "Watermark Size");
        addSetting(fontMode);
    }
    
    @Override
    protected void onEnable() {
        mc.displayGuiScreen(new com.yesclient.gui.ClickGUI());
        toggle();
    }
    
    public boolean isBlurEnabled() { return true; }
    public boolean isSoundsEnabled() { return sounds.isEnabled(); }
    public boolean isSnapToGrid() { return snapToGrid.isEnabled(); }
    public boolean isNotificationsEnabled() { return notifications.isEnabled(); }
    public boolean isBlatantMode() { return blatantMode.isEnabled(); }
    public boolean isWatermarkEnabled() { return watermark.isEnabled(); }
    public boolean showWatermark() { return watermark.isEnabled(); }
    public boolean isFancyFont() { return fontMode.is("Fancy"); }
    public boolean isTooltipsEnabled() { return true; }
    public boolean isBlatantModeEnabled() { return blatantMode.isEnabled(); }
    public HudScale getWatermarkScale() { return watermarkScale; }
}

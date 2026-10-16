package com.yesclient.module.modules.qol;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.NumberSetting;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class ZoomModule extends Module {

    private final NumberSetting zoomAmount = new NumberSetting("Zoom Amount", "FOV divisor (higher = more zoom)",
            2.0, 1.5, 20.0, 0.5);
    private final BooleanSetting smoothCamera = new BooleanSetting("Smooth Camera",
            "Enable smooth camera while zooming", true);
    private final BooleanSetting scrollZoom = new BooleanSetting("Scroll to Zoom",
            "Use scroll wheel to adjust zoom level", true);
    private final BooleanSetting hideNotification = new BooleanSetting("Hide Notification", "Don't show toggle alert",
            true);

    private boolean originalSmoothCam;
    private boolean smoothCameraSaved;
     
    private double activeZoom;

    public ZoomModule() {
        super("Zoom", "Zoom in view (Hold Key)", Category.QOL, ModuleMode.BOTH);
        addSetting(zoomAmount);
        addSetting(smoothCamera);
        addSetting(scrollZoom);
        addSetting(hideNotification);
    }

    @Override
    protected void onEnable() {
        if (mc.gameSettings != null) {
            originalSmoothCam = mc.gameSettings.smoothCamera;
            smoothCameraSaved = true;
        }
         
        activeZoom = zoomAmount.getValue();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void onDisable() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
        restoreGameSettingsForShutdown();
    }

    public void restoreGameSettingsForShutdown() {
        if (smoothCameraSaved && mc.gameSettings != null) {
            mc.gameSettings.smoothCamera = originalSmoothCam;
            smoothCameraSaved = false;
        }
    }

    @Override
    public void onTick() {
         
         
        if (isEnabled() && getKeybind() != 0) {
            if (!Keyboard.isKeyDown(getKeybind())) {
                toggle();  
            }
        }
    }

    @SubscribeEvent
    public void onFOVModifier(EntityViewRenderEvent.FOVModifier event) {
        if (!isEnabled())
            return;

         
        event.setFOV((float) (event.getFOV() / (activeZoom * 0.5)));

        if (smoothCamera.isEnabled()) {
            mc.gameSettings.smoothCamera = true;
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (!isEnabled() || !scrollZoom.isEnabled())
            return;

        int dWheel = event.dwheel;
        if (dWheel != 0) {
             
            if (dWheel > 0) {
                activeZoom = Math.min(activeZoom + 0.5, 30.0);
            } else {
                activeZoom = Math.max(activeZoom - 0.5, 1.5);
            }
             
            event.setCanceled(true);
        }
    }

    @Override
    protected boolean shouldShowNotification() {
        return !hideNotification.isEnabled();
    }
}

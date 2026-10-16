package com.yesclient.module.modules.visuals;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class FreeLookModule extends Module {

    private final BooleanSetting scrollDistance = new BooleanSetting("Scroll Changes Distance", "Use the mouse wheel to adjust camera distance", true);
    private final NumberSetting distance = new NumberSetting("Camera Distance", "Starting orbit distance", 5.0, 1.0, 10.0, 0.1);
    private final BooleanSetting disableOnRelease = new BooleanSetting("Disable on release", "Hold the module key to keep free look active", true);

    private float cameraYaw;
    private float cameraPitch;
    private double sessionDistance = 5.0D;
    private int previousPerspective;
    private boolean perspectiveSaved;
    private boolean activatedByHold;

    public FreeLookModule() {
        super("FreeLook", "Orbit the camera around your character", Category.VISUALS, ModuleMode.BOTH);
        addSetting(scrollDistance);
        addSetting(distance);
        addSetting(disableOnRelease);
    }

    @Override
    protected void onEnable() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            setEnabled(false);
            return;
        }

         
         
        cameraYaw = mc.thePlayer.rotationYaw;
        cameraPitch = mc.thePlayer.rotationPitch;
        sessionDistance = distance.getValue();
        previousPerspective = mc.gameSettings.thirdPersonView;
        perspectiveSaved = true;
        mc.gameSettings.thirdPersonView = 1;
    }

    @Override
    protected void onDisable() {
        activatedByHold = false;
        restoreGameSettingsForShutdown();
    }

    public void restoreGameSettingsForShutdown() {
        if (perspectiveSaved && mc.gameSettings != null) {
            mc.gameSettings.thirdPersonView = previousPerspective;
            perspectiveSaved = false;
        }
    }

    @Override
    public boolean handleKeybindPress(int key) {
        if (key == 0 || key != getKeybind()) return false;
        if (!disableOnRelease.isEnabled()) {
            toggle();
            return true;
        }
        if (isEnabled()) {
            setEnabled(false);
        } else {
            activatedByHold = true;
            setEnabled(true);
        }
        return true;
    }

    @Override
    public void onTick() {
        if (activatedByHold && (mc.currentScreen != null || !isBoundKeyDown())) {
            setEnabled(false);
        }
    }

    public void redirectMouseLook(Entity player, float mouseX, float mouseY) {
        if (!isEnabled() || player != mc.thePlayer) {
            player.setAngles(mouseX, mouseY);
            return;
        }

         
         
        cameraYaw += mouseX * 0.15F;
        cameraPitch -= mouseY * 0.15F;
        cameraPitch = Math.max(-90.0F, Math.min(90.0F, cameraPitch));
    }

    public float cameraYaw(Entity entity, float vanillaYaw) {
        return isEnabled() && entity == mc.thePlayer ? cameraYaw : vanillaYaw;
    }

    public float cameraPitch(Entity entity, float vanillaPitch) {
        return isEnabled() && entity == mc.thePlayer ? cameraPitch : vanillaPitch;
    }
    public Vec3 cameraLook(Entity entity, float partialTicks) {
        if (!isEnabled() || entity != mc.thePlayer) return entity.getLook(partialTicks);
        double yaw = Math.toRadians(cameraYaw);
        double pitch = Math.toRadians(cameraPitch);
        double horizontal = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * horizontal, -Math.sin(pitch), Math.cos(yaw) * horizontal);
    }

    public double getCameraDistance() {
        return sessionDistance;
    }

    public boolean handleScrollWheel(int wheel) {
        if (!isEnabled() || mc.currentScreen != null || !scrollDistance.isEnabled() || wheel == 0) return false;
        sessionDistance = Math.max(distance.getMin(), Math.min(distance.getMax(),
                sessionDistance + (wheel > 0 ? -0.1D : 0.1D)));
        return true;
    }

    private boolean isBoundKeyDown() {
        int key = getKeybind();
        if (key == 0) return false;
        return key < 0 ? Mouse.isButtonDown(-key - 100) : Keyboard.isKeyDown(key);
    }
}

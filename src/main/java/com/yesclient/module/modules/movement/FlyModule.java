package com.yesclient.module.modules.movement;
import java.util.List;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.ClickGUIModule;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ModeSetting;
import com.yesclient.module.settings.NumberSetting;

public class FlyModule extends Module {

    private final ModeSetting flightMode = new ModeSetting("Mode", "How flight movement speed is applied", "Vanilla",
            "Vanilla", "Constant");
    private final NumberSetting speed = new NumberSetting("Speed", "Fly multiplier or constant blocks per tick", 1.0,
            0.1, 20.0, 0.1);
    private final NumberSetting sprintSpeed = new NumberSetting("Sprint Speed", "Speed when sprinting", 2.0, 0.5, 50.0,
            0.1);
    private final BooleanSetting doubleTap = new BooleanSetting("Double Tap", "Double tap space to toggle fly", true);
    private final BooleanSetting strictMode = new BooleanSetting("Strict Mode", "Instantly stop motion", false);
    private final BooleanSetting limitEnabled = new BooleanSetting("Limit Height", "Limit vertical fly range", false);
    private final NumberSetting limitHeight = new NumberSetting("Height Limit", "Max blocks up/down", 3.5, 1.0, 20.0,
            0.5);

    private float originalFlySpeed;
    private double startY;

     



    private boolean serverAllowedFlying = false;

    public FlyModule() {
        super("Creative Flight", "Creative-mode flight with double-tap space", Category.MOVEMENT, ModuleMode.BOTH);

        addSetting(flightMode);
        addSetting(speed);
        addSetting(sprintSpeed);
        addSetting(doubleTap);
        addSetting(strictMode);
        addSetting(limitEnabled);
        addSetting(limitHeight);
    }

    private boolean isBlatantModeEnabled() {
        if (YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null) {
            return false;
        }
        ClickGUIModule clickGUI = YesClient.getInstance().getModuleManager().getModule(ClickGUIModule.class);
        return clickGUI != null && clickGUI.isBlatantModeEnabled();
    }

    @Override
    public String getDisplayName() {
        if (!isBlatantModeEnabled()) {
            return "Flight Speed";
        }
        return super.getDisplayName();
    }

    @Override
    public String getDisplayInfo() {
        return mc.thePlayer != null && mc.thePlayer.capabilities.isFlying ? "Active" : "Ready";
    }

    @Override
    protected void onEnable() {
        if (mc.thePlayer != null) {
            originalFlySpeed = mc.thePlayer.capabilities.getFlySpeed();
            startY = mc.thePlayer.posY;

             
            serverAllowedFlying = mc.thePlayer.capabilities.allowFlying;

             
            if (isBlatantModeEnabled() || mc.thePlayer.capabilities.isCreativeMode) {
                if (!doubleTap.isEnabled()) {
                     
                    mc.thePlayer.capabilities.allowFlying = true;
                    mc.thePlayer.capabilities.isFlying = true;

                     
                    if (isBlatantModeEnabled() && flightMode.is("Constant")) {
                        mc.thePlayer.capabilities.setFlySpeed(0.0f);
                        applyConstantVelocity();
                    } else {
                        float flySpeed = (float) (speed.getValue() * 0.05f);
                        mc.thePlayer.capabilities.setFlySpeed(flySpeed);
                    }

                     
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.1;
                        startY = mc.thePlayer.posY + 0.1;
                    }
                }
            }
             
             
        }
    }

    @Override
    protected void onDisable() {
        if (mc.thePlayer != null) {
             
            mc.thePlayer.capabilities.setFlySpeed(originalFlySpeed);

            if (isBlatantModeEnabled() && flightMode.is("Constant")) {
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionZ = 0;
            }

             
            if (!isBlatantModeEnabled()) {
                if (mc.thePlayer.capabilities.isCreativeMode) {
                     
                    mc.thePlayer.capabilities.allowFlying = true;
                } else {
                     
                    mc.thePlayer.capabilities.allowFlying = serverAllowedFlying;
                    if (!serverAllowedFlying) {
                        mc.thePlayer.capabilities.isFlying = false;
                    }
                }
                return;
            }

             
            if (mc.thePlayer.capabilities.isFlying) {
                mc.thePlayer.motionY = -0.1;
            }

            if (strictMode.isEnabled()) {
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionZ = 0;
            }

            if (mc.thePlayer.capabilities.isCreativeMode) {
                mc.thePlayer.capabilities.allowFlying = true;
            } else {
                mc.thePlayer.capabilities.isFlying = false;
                mc.thePlayer.capabilities.allowFlying = false;
            }
        }
    }

     




    public void updateServerFlyState(boolean allowFlying) {
        this.serverAllowedFlying = allowFlying;
    }

    private void updateSettingsVisibility() {
        if (!isBlatantModeEnabled()) {
             
            flightMode.setVisible(false);
            speed.setVisible(true);
            sprintSpeed.setVisible(true);
            doubleTap.setVisible(false);
            strictMode.setVisible(false);
            limitEnabled.setVisible(false);
            limitHeight.setVisible(false);
        } else {
             
            flightMode.setVisible(true);
            speed.setVisible(true);
            sprintSpeed.setVisible(!flightMode.is("Constant"));
            doubleTap.setVisible(true);
            strictMode.setVisible(!flightMode.is("Constant"));
            limitEnabled.setVisible(true);
            limitHeight.setVisible(true);
        }
    }

    @Override
    public List<com.yesclient.module.settings.Setting<?>> getSettings() {
        updateSettingsVisibility();
        return super.getSettings();
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

         
        if (!isBlatantModeEnabled()) {
            updateSettingsVisibility();

            if (mc.thePlayer.capabilities.isCreativeMode) {
                 
                mc.thePlayer.capabilities.allowFlying = true;

                float flySpeed = (float) (speed.getValue() * 0.05f);
                if (mc.thePlayer.isSprinting()) {
                    flySpeed = (float) (sprintSpeed.getValue() * 0.05f);
                }
                mc.thePlayer.capabilities.setFlySpeed(flySpeed);
            } else if (serverAllowedFlying) {
                 
                 
                mc.thePlayer.capabilities.allowFlying = true;

                float flySpeed = (float) (speed.getValue() * 0.05f);
                if (mc.thePlayer.isSprinting()) {
                    flySpeed = (float) (sprintSpeed.getValue() * 0.05f);
                }
                mc.thePlayer.capabilities.setFlySpeed(flySpeed);
            } else {
                 
                 
                mc.thePlayer.capabilities.setFlySpeed(0.05f);  
            }
            return;
        }

         
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

         
        mc.thePlayer.capabilities.allowFlying = true;

        boolean isFlying = mc.thePlayer.capabilities.isFlying;

         
        if (mc.thePlayer.onGround) {
            startY = mc.thePlayer.posY;
        }

         
        if (!doubleTap.isEnabled()) {
            mc.thePlayer.capabilities.isFlying = true;
            isFlying = true;

            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionY = 0.04;
                startY = mc.thePlayer.posY;
            }
        }

        if (isFlying || !doubleTap.isEnabled()) {
            if (flightMode.is("Constant")) {
                 
                 
                mc.thePlayer.capabilities.setFlySpeed(0.0f);
                applyConstantVelocity();
            } else {
                float flySpeed = (float) (speed.getValue() * 0.05f);
                if (mc.thePlayer.isSprinting()) {
                    flySpeed = (float) (sprintSpeed.getValue() * 0.05f);
                }
                mc.thePlayer.capabilities.setFlySpeed(flySpeed);

                 
                if (strictMode.isEnabled()) {
                    if (mc.thePlayer.movementInput.moveForward == 0 && mc.thePlayer.movementInput.moveStrafe == 0) {
                        mc.thePlayer.motionX = 0;
                        mc.thePlayer.motionZ = 0;
                    }

                    if (!mc.thePlayer.movementInput.jump && !mc.thePlayer.movementInput.sneak) {
                        mc.thePlayer.motionY = 0;
                    }
                }
            }

             
            if (limitEnabled.isEnabled()) {
                double limit = limitHeight.getValue();
                double maxY = startY + limit;
                double minY = startY - limit;

                if (mc.thePlayer.posY > maxY) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, maxY, mc.thePlayer.posZ);
                    mc.thePlayer.motionY = 0;
                } else if (mc.thePlayer.posY < minY) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, minY, mc.thePlayer.posZ);
                    mc.thePlayer.motionY = 0;
                }
            }
        }
    }

     




    private void applyConstantVelocity() {
        double forward = mc.thePlayer.movementInput.moveForward;
        double strafe = mc.thePlayer.movementInput.moveStrafe;
        double inputLength = Math.sqrt(forward * forward + strafe * strafe);
        double blocksPerTick = speed.getValue();

        if (inputLength > 0.0) {
            forward /= inputLength;
            strafe /= inputLength;

            double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
            double sin = Math.sin(yaw);
            double cos = Math.cos(yaw);

            mc.thePlayer.motionX = (strafe * cos - forward * sin) * blocksPerTick;
            mc.thePlayer.motionZ = (forward * cos + strafe * sin) * blocksPerTick;
        } else {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }

        if (mc.thePlayer.movementInput.jump == mc.thePlayer.movementInput.sneak) {
            mc.thePlayer.motionY = 0.0;
        } else {
            mc.thePlayer.motionY = mc.thePlayer.movementInput.jump ? blocksPerTick : -blocksPerTick;
        }
    }

}

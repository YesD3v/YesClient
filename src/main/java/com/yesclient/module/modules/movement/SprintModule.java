package com.yesclient.module.modules.movement;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;

public class SprintModule extends Module {

    private final BooleanSetting keepSprint = new BooleanSetting("Keep Sprint",
            "Don't stop sprinting when hitting entities", true);
    private final BooleanSetting omniSprint = new BooleanSetting("Omni Sprint", "Sprint in all directions", false);
    private final BooleanSetting ignoreHunger = new BooleanSetting("Ignore Hunger", "Sprint even with low food", true);

    public SprintModule() {
        super("Sprint", "Always sprint without holding the key", Category.MOVEMENT, ModuleMode.BOTH);

        addSetting(keepSprint);
        addSetting(omniSprint);
        addSetting(ignoreHunger);
    }

    @Override
    public void onTick() {
        updateSprint();
    }

    @Override
    public void onUpdate(net.minecraft.entity.EntityLivingBase entity) {
        updateSprint();
    }

    private void updateSprint() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (mc.currentScreen != null)
            return;

         
         
        if (mc.thePlayer.isSneaking() || mc.thePlayer.isUsingItem()) {
            mc.thePlayer.setSprinting(false);
            return;
        }

         
        if (mc.thePlayer.getFoodStats().getFoodLevel() <= 6 && !ignoreHunger.isEnabled()) {
            mc.thePlayer.setSprinting(false);
            return;
        }

         
        float forward = mc.thePlayer.movementInput == null ? mc.thePlayer.moveForward
                : mc.thePlayer.movementInput.moveForward;
        float strafe = mc.thePlayer.movementInput == null ? mc.thePlayer.moveStrafing
                : mc.thePlayer.movementInput.moveStrafe;
        boolean moving = forward != 0 || strafe != 0;
        boolean allowed = !mc.thePlayer.isCollidedHorizontally
                && (omniSprint.isEnabled() ? moving : forward > 0);
        if (!allowed) {
            mc.thePlayer.setSprinting(false);
            return;
        }
        mc.thePlayer.setSprinting(true);
    }

    @Override
    public void onJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
         
        if (mc.thePlayer == null || !ignoreHunger.isEnabled())
            return;

         
        if (mc.thePlayer.getFoodStats().getFoodLevel() > 6)
            return;

         
        float forward = mc.thePlayer.movementInput == null ? mc.thePlayer.moveForward
                : mc.thePlayer.movementInput.moveForward;
        float strafe = mc.thePlayer.movementInput == null ? mc.thePlayer.moveStrafing
                : mc.thePlayer.movementInput.moveStrafe;
        boolean moving = forward != 0 || strafe != 0;

        if (moving && !mc.thePlayer.isSneaking() && !mc.thePlayer.isUsingItem()) {
             
            float f = mc.thePlayer.rotationYaw * 0.017453292F;
            mc.thePlayer.motionX -= (double) (net.minecraft.util.MathHelper.sin(f) * 0.2F);
            mc.thePlayer.motionZ += (double) (net.minecraft.util.MathHelper.cos(f) * 0.2F);
        }
    }

    public boolean shouldKeepSprint() {
        return isEnabled() && keepSprint.isEnabled();
    }
}

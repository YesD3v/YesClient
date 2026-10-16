package com.yesclient.module.modules.misc;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

 



public class ReachModule extends Module {

    private final NumberSetting leftReach = new NumberSetting("Left Reach", "Left click reach distance", 3.0, 0.0, 8.0,
            0.1);
    private final NumberSetting rightReach = new NumberSetting("Right Reach", "Right click reach distance", 4.5, 0.0,
            8.0, 0.1);

    public ReachModule() {
        super("Reach", "Modify attack and interaction reach", Category.MISC, ModuleMode.BOTH);

        addSetting(leftReach);
        addSetting(rightReach);
    }

    public double getLeftReach() {
        if (!isEnabled())
            return 3.0;  
        return leftReach.getValue();
    }

    public double getRightReach() {
        if (!isEnabled())
            return 4.5;  
        return rightReach.getValue();
    }

     

    public boolean isInAttackReach(Entity target) {
        if (mc.thePlayer == null || target == null)
            return false;
        double distance = mc.thePlayer.getDistanceToEntity(target);
        return distance <= getLeftReach();
    }

     


    public boolean isInInteractReach(Entity target) {
        if (mc.thePlayer == null || target == null)
            return false;
        double distance = mc.thePlayer.getDistanceToEntity(target);
        return distance <= getRightReach();
    }

    @Override
    public String getDisplayInfo() {
        return String.format("L:%.1f R:%.1f", leftReach.getValue(), rightReach.getValue());
    }
}

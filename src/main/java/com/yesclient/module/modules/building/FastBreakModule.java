package com.yesclient.module.modules.building;
import java.lang.reflect.Field;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.NumberSetting;


 









public class FastBreakModule extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", "Ticks between hits (0 = fastest)", 0, 0, 5);

    private Field blockHitDelayField;
    private boolean fieldFound = false;

    public FastBreakModule() {
        super("FastBreak", "Break blocks faster", Category.MISC, ModuleMode.BOTH);
        addSetting(delay);

         
        try {
            Class<?> controllerClass = net.minecraft.client.multiplayer.PlayerControllerMP.class;

             
            try {
                blockHitDelayField = controllerClass.getDeclaredField("blockHitDelay");
                blockHitDelayField.setAccessible(true);
                fieldFound = true;
            } catch (NoSuchFieldException e) {
                 
                try {
                    blockHitDelayField = controllerClass.getDeclaredField("field_78781_i");
                    blockHitDelayField.setAccessible(true);
                    fieldFound = true;
                } catch (NoSuchFieldException e2) {
                     
                    for (Field field : controllerClass.getDeclaredFields()) {
                        if (field.getType() == int.class) {
                            field.setAccessible(true);
                            blockHitDelayField = field;
                            fieldFound = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
             
        }
    }

    @Override
    public void onTick() {
        if (!isEnabled())
            return;
        if (mc.thePlayer == null || mc.playerController == null)
            return;

         
        if (fieldFound && blockHitDelayField != null) {
            try {
                blockHitDelayField.setInt(mc.playerController, delay.getIntValue());
            } catch (Exception e) {
                 
            }
        }
    }

    @Override
    public String getDisplayInfo() {
        return delay.getIntValue() + "t";
    }
}

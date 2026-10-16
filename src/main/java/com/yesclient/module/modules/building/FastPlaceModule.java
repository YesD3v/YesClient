package com.yesclient.module.modules.building;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.NumberSetting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.MovingObjectPosition;


 








public class FastPlaceModule extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", "Ticks between clicks", 0, 0, 4);
    private final NumberSetting cps = new NumberSetting("Clicks/Tick", "Right-clicks per tick (at 0 delay)", 1, 1, 5);
    private final BooleanSetting blocksOnly = new BooleanSetting("Blocks Only", "Only affect block placement", false);

    private Field rightClickDelayField;
    private Method rightClickMethod;
    private boolean delayFieldFound = false;
    private boolean methodFound = false;

     
    private Item lastHeldItem = null;
    private int lastHeldMeta = -1;
    private int swapCooldown = 0;
    private int tickCounter = 0;

    public FastPlaceModule() {
        super("FastPlace", "Universal fast right-click", Category.MISC, ModuleMode.BOTH);

        addSetting(delay);
        addSetting(cps);
        addSetting(blocksOnly);

         
        try {
            try {
                rightClickDelayField = mc.getClass().getDeclaredField("rightClickDelayTimer");
                rightClickDelayField.setAccessible(true);
                delayFieldFound = true;
            } catch (NoSuchFieldException e) {
                try {
                    rightClickDelayField = mc.getClass().getDeclaredField("field_71467_ac");
                    rightClickDelayField.setAccessible(true);
                    delayFieldFound = true;
                } catch (NoSuchFieldException e2) {
                     
                    for (Field field : mc.getClass().getDeclaredFields()) {
                        if (field.getType() == int.class) {
                            field.setAccessible(true);
                            try {
                                int val = field.getInt(mc);
                                if (val >= 0 && val <= 6) {
                                    rightClickDelayField = field;
                                    delayFieldFound = true;
                                    break;
                                }
                            } catch (Exception ex) {
                                 
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
             
        }

         
         
        try {
            try {
                rightClickMethod = mc.getClass().getDeclaredMethod("rightClickMouse");
                rightClickMethod.setAccessible(true);
                methodFound = true;
            } catch (NoSuchMethodException e) {
                try {
                     
                    rightClickMethod = mc.getClass().getDeclaredMethod("func_147121_ag");
                    rightClickMethod.setAccessible(true);
                    methodFound = true;
                } catch (NoSuchMethodException e2) {
                     
                    for (Method m : mc.getClass().getDeclaredMethods()) {
                        if (m.getParameterCount() == 0 && m.getReturnType() == void.class) {
                             
                             
                        }
                    }
                }
            }
        } catch (Exception e) {
             
        }
    }

    @Override
    protected void onEnable() {
        lastHeldItem = null;
        lastHeldMeta = -1;
        swapCooldown = 0;
        tickCounter = 0;
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
         
        if (mc.currentScreen != null) {
            boolean allow = false;
            if (mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiEditSign) {
                com.yesclient.module.Module signFill = com.yesclient.YesClient.getInstance()
                        .getModuleManager().getModule(com.yesclient.module.modules.exploit.SignFillModule.class);
                if (signFill != null && signFill.isEnabled()) {
                    allow = true;
                }
            }
            if (!allow)
                return;
        }

         
        if (blocksOnly.isEnabled()) {
            boolean holdingBlock = mc.thePlayer.getHeldItem() != null
                    && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock;
            boolean lookingAtBlock = mc.objectMouseOver != null
                    && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
            if (!holdingBlock || !lookingAtBlock) {
                return;
            }
        }

         
        if (swapCooldown > 0) {
            swapCooldown--;
        }

         
         
        if (!mc.gameSettings.keyBindUseItem.isKeyDown())
            return;

         
        Item currentItem = mc.thePlayer.getHeldItem() != null ? mc.thePlayer.getHeldItem().getItem() : null;
        int currentMeta = mc.thePlayer.getHeldItem() != null ? mc.thePlayer.getHeldItem().getMetadata() : -1;
        if (currentItem != lastHeldItem || currentMeta != lastHeldMeta) {
            lastHeldItem = currentItem;
            lastHeldMeta = currentMeta;
            swapCooldown = 2;
             
            resetDelayTimer();
            return;
        }

         
        boolean bypassCooldown = false;
        if (currentItem instanceof net.minecraft.item.ItemSign) {
            com.yesclient.module.Module signFill = com.yesclient.YesClient.getInstance()
                    .getModuleManager().getModule(com.yesclient.module.modules.exploit.SignFillModule.class);
            if (signFill != null && signFill.isEnabled()) {
                bypassCooldown = true;
            }
        }

        if (swapCooldown > 0 && !bypassCooldown) {
            return;
        }

         
        int delayVal = delay.getIntValue();
        if (delayVal > 0) {
            tickCounter++;
            if (tickCounter < delayVal) {
                 
                 
                resetDelayTimer();
                return;
            }
            tickCounter = 0;
        }

         
         
        resetDelayTimer();

         
         
         
        if (methodFound && rightClickMethod != null) {
            int extraClicks = cps.getIntValue() - 1;
            for (int i = 0; i < extraClicks; i++) {
                try {
                    rightClickMethod.invoke(mc);
                } catch (Exception e) {
                     
                    break;
                }
            }
        }
    }
    private void resetDelayTimer() {
        if (delayFieldFound && rightClickDelayField != null) {
            try {
                rightClickDelayField.setInt(mc, 0);
            } catch (Exception e) {
                 
            }
        }
    }

    @Override
    public String getDisplayInfo() {
        int d = delay.getIntValue();
        int c = cps.getIntValue();
        if (d == 0 && c > 1) {
            return c + "x";
        }
        return d + "t";
    }
}

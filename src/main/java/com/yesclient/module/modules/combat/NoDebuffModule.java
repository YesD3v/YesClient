package com.yesclient.module.modules.combat;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.List;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.ClickGUIModule;
import com.yesclient.module.settings.BooleanSetting;
import net.minecraft.potion.Potion;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.SharedMonsterAttributes;

import java.util.ArrayList;
import java.util.Collection;

 




















public class NoDebuffModule extends Module {

    private final BooleanSetting blindness = new BooleanSetting("Blindness", "Remove blindness effect", true);
    private final BooleanSetting slowness = new BooleanSetting("Slowness", "Remove slowness effect", true);
    private final BooleanSetting nausea = new BooleanSetting("Nausea", "Remove nausea effect", true);

    public NoDebuffModule() {
        super("NoDebuff", "Cancel negative potion effects", Category.MISC, ModuleMode.BOTH);
        addSetting(blindness);
        addSetting(slowness);
        addSetting(nausea);
    }

    private boolean isBlatantModeEnabled() {
        if (YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null) {
            return false;
        }
        ClickGUIModule clickGUI = YesClient.getInstance().getModuleManager().getModule(ClickGUIModule.class);
        return clickGUI != null && clickGUI.isBlatantModeEnabled();
    }

    private void updateSettingsVisibility() {
        boolean blatant = isBlatantModeEnabled();
         
        slowness.setVisible(blatant);
         
        blindness.setVisible(true);
        nausea.setVisible(true);
    }

    @Override
    public List<com.yesclient.module.settings.Setting<?>> getSettings() {
        updateSettingsVisibility();
        return super.getSettings();
    }

    @Override
    protected void onEnable() {
        cleanEffects();
    }

    @Override
    public void onTick() {
        cleanEffects();
    }

    private void cleanEffects() {
        if (mc.thePlayer == null)
            return;

        boolean blatant = isBlatantModeEnabled();

         
        if (blindness.isEnabled() && mc.thePlayer.isPotionActive(Potion.blindness)) {
            if (blatant) {
                 
                mc.thePlayer.removePotionEffect(Potion.blindness.id);
            }
             
             
        }

         
        if (nausea.isEnabled() && mc.thePlayer.isPotionActive(Potion.confusion)) {
            if (blatant) {
                 
                mc.thePlayer.removePotionEffect(Potion.confusion.id);
            }
             
        }

         
        if (blatant && slowness.isEnabled()) {
             
            if (mc.thePlayer.isPotionActive(Potion.moveSlowdown)) {
                mc.thePlayer.removePotionEffect(Potion.moveSlowdown.id);
            }

             
            IAttributeInstance speedAttr = mc.thePlayer.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
            if (speedAttr != null) {
                try {
                     
                     
                     
                    Method getModifiersMethod = speedAttr.getClass().getMethod("func_111122_c");
                    Method removeModifierMethod = speedAttr.getClass().getMethod("func_111124_b",
                            AttributeModifier.class);

                    if (getModifiersMethod != null && removeModifierMethod != null) {
                        @SuppressWarnings("unchecked")
                        Collection<AttributeModifier> modifiers = new ArrayList<>(
                                (Collection<AttributeModifier>) getModifiersMethod.invoke(speedAttr));
                        for (AttributeModifier modifier : modifiers) {
                            if (modifier.getAmount() < 0) {
                                YesClient.LOGGER.info("Removing negative attribute modifier: " + modifier.getName()
                                        + " (" + modifier.getAmount() + ")");
                                removeModifierMethod.invoke(speedAttr, modifier);
                            }
                        }
                    }
                } catch (Exception e) {
                     
                    try {
                        for (Method m : speedAttr.getClass().getMethods()) {
                            if ((m.getName().equals("getModifiers") || m.getName().equals("func_111122_c"))
                                    && m.getReturnType().equals(Collection.class)) {
                                @SuppressWarnings("unchecked")
                                Collection<AttributeModifier> modifiers = new ArrayList<>(
                                        (Collection<AttributeModifier>) m.invoke(speedAttr));
                                for (AttributeModifier modifier : modifiers) {
                                    if (modifier.getAmount() < 0) {
                                        speedAttr.removeModifier(modifier);
                                    }
                                }
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

             
            if (mc.thePlayer.capabilities.getWalkSpeed() < 0.1f) {
                mc.thePlayer.capabilities.setPlayerWalkSpeed(0.1f);
            }
        }

         
         
         
        if (!blatant) {
            suppressVisualEffects();
        }
    }

     



    private void suppressVisualEffects() {
        if (mc.thePlayer == null)
            return;

         
         
         
        if (blindness.isEnabled() && mc.thePlayer.isPotionActive(Potion.blindness)) {
             
             
             
             
            try {
                net.minecraft.potion.PotionEffect effect = mc.thePlayer.getActivePotionEffect(Potion.blindness);
                if (effect != null) {
                     
                     
                    Field amplifierField = null;
                    for (Field f : effect.getClass().getDeclaredFields()) {
                        if (f.getType() == int.class) {
                            f.setAccessible(true);
                            int val = f.getInt(effect);
                             
                             
                            if (f.getName().equals("amplifier") || f.getName().equals("field_76461_c")) {
                                amplifierField = f;
                                break;
                            }
                        }
                    }
                    if (amplifierField != null) {
                        amplifierField.setInt(effect, 0);
                    }

                     
                     
                    Field durationField = null;
                    for (Field f : effect.getClass().getDeclaredFields()) {
                        if (f.getType() == int.class) {
                            f.setAccessible(true);
                            if (f.getName().equals("duration") || f.getName().equals("field_76460_b")) {
                                durationField = f;
                                break;
                            }
                        }
                    }
                    if (durationField != null) {
                        durationField.setInt(effect, 1);
                    }
                }
            } catch (Exception ignored) {
                 
            }
        }

         
        if (nausea.isEnabled() && mc.thePlayer.isPotionActive(Potion.confusion)) {
            try {
                net.minecraft.potion.PotionEffect effect = mc.thePlayer.getActivePotionEffect(Potion.confusion);
                if (effect != null) {
                    Field durationField = null;
                    for (Field f : effect.getClass().getDeclaredFields()) {
                        if (f.getType() == int.class) {
                            f.setAccessible(true);
                            if (f.getName().equals("duration") || f.getName().equals("field_76460_b")) {
                                durationField = f;
                                break;
                            }
                        }
                    }
                    if (durationField != null) {
                        durationField.setInt(effect, 1);
                    }
                }
            } catch (Exception ignored) {
                 
            }
        }
    }

     





    public boolean shouldRemoveEffect(int potionId) {
         
        if (!isBlatantModeEnabled()) {
            return false;
        }

         
        if (Potion.blindness != null && potionId == Potion.blindness.id && blindness.isEnabled())
            return true;
        if (Potion.moveSlowdown != null && potionId == Potion.moveSlowdown.id && slowness.isEnabled())
            return true;
        if (Potion.confusion != null && potionId == Potion.confusion.id && nausea.isEnabled())
            return true;
        return false;
    }

     


    public boolean isSlownessRemovalEnabled() {
        return isBlatantModeEnabled() && slowness.isEnabled();
    }
}

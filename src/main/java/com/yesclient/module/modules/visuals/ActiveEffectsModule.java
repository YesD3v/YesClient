package com.yesclient.module.modules.visuals;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.HudTextStyle;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.HudDesignerModule;
import com.yesclient.module.settings.BooleanSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;

public class ActiveEffectsModule extends Module {

    private final HudScale hudScale = new HudScale(this);
    private final HudTextStyle textStyle = new HudTextStyle(this, false);
    private final BooleanSetting hideInventoryEffects = new BooleanSetting("Hide Inventory Effects",
            "Remove the vanilla potion effect cards next to the opened inventory", false);

    private static final ResourceLocation inventoryBackground = new ResourceLocation(
            "textures/gui/container/inventory.png");

    public ActiveEffectsModule() {
        super("ActiveEffects", "Display active potion effects on HUD", Category.VISUALS, ModuleMode.BOTH);
        addSetting(hideInventoryEffects);
    }

    /** True while the module is on with "Hide Inventory Effects" enabled. */
    public static boolean shouldHideInventoryEffects() {
        YesClient client = YesClient.getInstance();
        if (client == null || client.getModuleManager() == null) {
            return false;
        }
        ActiveEffectsModule module = client.getModuleManager().getModule(ActiveEffectsModule.class);
        return module != null && module.isEnabled() && module.hideInventoryEffects.isEnabled();
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        Collection<PotionEffect> effects = mc.thePlayer.getActivePotionEffects();
        if (effects.isEmpty())
            return;

        HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getActiveEffectsX() : 10;
        int y = designer != null ? designer.getActiveEffectsY() : 100;

        int initialY = y;
        hudScale.beginAt(x, y);

        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(inventoryBackground);

            if (potion.hasStatusIcon()) {
                int index = potion.getStatusIconIndex();
                 
                mc.ingameGUI.drawTexturedModalRect(x, y, 0 + index % 8 * 18, 198 + index / 8 * 18, 18, 18);
            }

             
            String s = I18n.format(potion.getName());
            if (effect.getAmplifier() == 1) {
                s = s + " " + I18n.format("enchantment.level.2");
            } else if (effect.getAmplifier() == 2) {
                s = s + " " + I18n.format("enchantment.level.3");
            } else if (effect.getAmplifier() == 3) {
                s = s + " " + I18n.format("enchantment.level.4");
            }

            String effectName = textStyle.format(null, s, "");
            mc.fontRendererObj.drawStringWithShadow(effectName, x + 22, y + 2, textStyle.getColor(Double.NaN));

             
            String s1 = Potion.getDurationString(effect);
            String duration = textStyle.format(null, s1, "");
            mc.fontRendererObj.drawStringWithShadow(duration, x + 22, y + 12,
                    (textStyle.getColor(Double.NaN) & 0x00FFFFFF) | 0x7F000000);

            y += 24;  
        }

         
        setWidth(100);
        setHeight(y - initialY);
        hudScale.end();
    }

}

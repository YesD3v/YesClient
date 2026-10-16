package com.yesclient.module;

import com.yesclient.module.settings.NumberSetting;
import org.lwjgl.opengl.GL11;
import java.util.Map;
import java.util.WeakHashMap;

 
public final class HudScale {
    public static final float MIN_SCALE = 0.5F;
    public static final float MAX_SCALE = 2.0F;
    private static final Map<Module, HudScale> SCALES = new WeakHashMap<>();

    private final NumberSetting setting;

    public HudScale(Module owner) {
        this(owner, "HUD Scale");
    }

    public HudScale(Module owner, String settingName) {
        setting = new NumberSetting(settingName, "Scale this HUD element", 1.0D,
                MIN_SCALE, MAX_SCALE, 0.1D);
        owner.addSetting(setting);
        SCALES.put(owner, this);
    }

    public float getValue() {
        return setting.getValue().floatValue();
    }

    public void setValue(float value) {
        setting.setValue((double) Math.max(MIN_SCALE, Math.min(MAX_SCALE, value)));
    }

    public void beginAt(int x, int y) {
        float scale = getValue();
        GL11.glPushMatrix();
        GL11.glTranslatef(x * (1.0F - scale), y * (1.0F - scale), 0.0F);
        GL11.glScalef(scale, scale, 1.0F);
    }

     
    public void beginAtLocal(int x, int y) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0.0F);
        GL11.glScalef(getValue(), getValue(), 1.0F);
    }

    public void end() {
        GL11.glPopMatrix();
    }

    public int scaled(int size) {
        return Math.max(1, Math.round(size * getValue()));
    }

    public static HudScale of(Module module) {
        if (module == null) return null;
        return SCALES.get(module);
    }
}

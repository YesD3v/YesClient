package com.yesclient.utils;

import java.util.concurrent.ThreadLocalRandom;

import com.yesclient.YesClient;
import com.yesclient.module.ModuleManager;
import com.yesclient.module.modules.client.ClickGUIModule;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

 
public final class InterfaceSounds {
    private static final long CLICK_DEBOUNCE_NANOS = 35_000_000L;
    private static final long SLIDER_DEBOUNCE_NANOS = 65_000_000L;
    private static volatile long lastClickAt;
    private static volatile long lastSliderTickAt;

    private InterfaceSounds() {
    }

    public static boolean isEnabled() {
        try {
            YesClient client = YesClient.getInstance();
            if (client == null) return true;
            ModuleManager manager = client.getModuleManager();
            if (manager == null) return true;
            ClickGUIModule clickGui = manager.getModule(ClickGUIModule.class);
            return clickGui == null || clickGui.isSoundsEnabled();
        } catch (Throwable ignored) {
            return true;
        }
    }

     
    public static boolean isClientUiScreen() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            return mc != null && mc.currentScreen instanceof com.yesclient.gui.ClickGUI;
        } catch (Throwable ignored) {
            return false;
        }
    }

     
    public static void playClick() {
        if (!isEnabled()) return;
        long now = System.nanoTime();
        if (now - lastClickAt < CLICK_DEBOUNCE_NANOS) return;
        lastClickAt = now;
        float pitch = 0.99F + ThreadLocalRandom.current().nextFloat() * 0.06F;
        play("gui.button.press", 0.16F, pitch);
    }

     
    public static void playModuleToggle(boolean enabled) {
        if (!isEnabled()) return;
        float pitch = enabled ? 1.30F : 0.86F;
        play("note.pling", enabled ? 0.15F : 0.13F, pitch);
    }

     
    public static void playSettingToggle(boolean enabled) {
        if (!isEnabled()) return;
        float pitch = enabled ? 1.17F : 0.93F;
        play("note.harp", 0.10F, pitch);
    }

     
    public static void playSliderTick() {
        if (!isEnabled()) return;
        long now = System.nanoTime();
        if (now - lastSliderTickAt < SLIDER_DEBOUNCE_NANOS) return;
        lastSliderTickAt = now;
        float pitch = 1.18F + ThreadLocalRandom.current().nextFloat() * 0.22F;
        play("gui.button.press", 0.09F, pitch);
    }

    private static void play(String eventName, float volume, float pitch) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getSoundHandler() == null) return;
            mc.getSoundHandler().playSound(new ClientUiSound(new ResourceLocation(eventName), volume, pitch));
        } catch (Throwable ignored) {
             
        }
    }
}

package com.yesclient.module.modules.qol;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class GuiMoveModule extends Module {

    public GuiMoveModule() {
        super("GUI Move", "Walk while container screens are open", Category.QOL, ModuleMode.BOTH);
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || !(mc.currentScreen instanceof GuiContainer)) {
            return;
        }
        
        if (mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainerCreative) {
            net.minecraft.client.gui.inventory.GuiContainerCreative creative = (net.minecraft.client.gui.inventory.GuiContainerCreative) mc.currentScreen;
            if (creative.getSelectedTabIndex() == net.minecraft.creativetab.CreativeTabs.tabAllSearch.getTabIndex()) {
                onDisable();  
                return;
            }
        }

        syncKey(mc.gameSettings.keyBindForward);
        syncKey(mc.gameSettings.keyBindBack);
        syncKey(mc.gameSettings.keyBindLeft);
        syncKey(mc.gameSettings.keyBindRight);
        syncKey(mc.gameSettings.keyBindJump);
    }

    @Override
    protected void onDisable() {
        releaseKey(mc.gameSettings.keyBindForward);
        releaseKey(mc.gameSettings.keyBindBack);
        releaseKey(mc.gameSettings.keyBindLeft);
        releaseKey(mc.gameSettings.keyBindRight);
        releaseKey(mc.gameSettings.keyBindJump);
    }

    private void syncKey(KeyBinding keyBinding) {
        int keyCode = keyBinding.getKeyCode();
        if (keyCode != 0) {
            KeyBinding.setKeyBindState(keyCode, isPhysicallyDown(keyCode));
        }
    }

    private boolean isPhysicallyDown(int keyCode) {
        if (keyCode < 0) {
            return Mouse.isButtonDown(keyCode + 100);
        }
        return Keyboard.isKeyDown(keyCode);
    }

    private void releaseKey(KeyBinding keyBinding) {
        int keyCode = keyBinding.getKeyCode();
        if (keyCode != 0) {
            KeyBinding.setKeyBindState(keyCode, false);
        }
    }
}

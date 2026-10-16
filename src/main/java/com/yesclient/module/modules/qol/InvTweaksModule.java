package com.yesclient.module.modules.qol;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class InvTweaksModule extends Module {

    private GuiContainer activeGui;
    private Slot oldSelectedSlot;
    private boolean leftDragActive;

    public InvTweaksModule() {
        super("Inv Tweaks", "Mouse Tweaks-style held inventory dragging", Category.QOL, ModuleMode.BOTH);
    }

    public static void onMouseClicked(GuiContainer gui, Slot slot, int mouseButton) {
        InvTweaksModule module = getModule();
        if (module != null) {
            module.handleMouseClicked(gui, slot, mouseButton);
        }
    }

    public static void onMouseClickMove(GuiContainer gui, Slot slot, int mouseButton) {
        InvTweaksModule module = getModule();
        if (module != null) {
            module.handleMouseClickMove(gui, slot, mouseButton);
        }
    }

    public static void onMouseReleased(GuiContainer gui, int mouseButton) {
        InvTweaksModule module = getModule();
        if (module != null) {
            module.handleMouseReleased(gui, mouseButton);
        }
    }

    @Override
    public void onTick() {
        if (activeGui != null && (mc.currentScreen != activeGui
                || (!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1)))) {
            resetDragState();
        }
    }

    @Override
    protected void onDisable() {
        resetDragState();
    }

    private void handleMouseClicked(GuiContainer gui, Slot slot, int mouseButton) {
        if (mc.thePlayer == null || mc.currentScreen != gui) {
            return;
        }

        if (activeGui != gui) {
            resetDragState();
            activeGui = gui;
        }

        oldSelectedSlot = slot;
        leftDragActive = mouseButton == 0;

         
        if (mouseButton == 0 && isShiftDown() && slot != null
                && slot.getHasStack() && slot.canTakeStack(mc.thePlayer)) {
            clickSlot(gui, slot, 0, 1);
        }
    }

    private void handleMouseClickMove(GuiContainer gui, Slot selectedSlot, int mouseButton) {
        if (mc.thePlayer == null || mc.currentScreen != gui || !leftDragActive || mouseButton != 0) {
            return;
        }

        if (selectedSlot == oldSelectedSlot) {
            return;
        }

        oldSelectedSlot = selectedSlot;
        if (selectedSlot == null) {
            return;
        }

        ItemStack targetStack = selectedSlot.getStack();
        if (targetStack == null || !selectedSlot.canTakeStack(mc.thePlayer)) {
            return;
        }

        ItemStack stackOnMouse = mc.thePlayer.inventory.getItemStack();
        if (isShiftDown()) {
            clickSlot(gui, selectedSlot, 0, 1);
            return;
        }

        if (stackOnMouse == null || !areStacksCompatible(stackOnMouse, targetStack)) {
            return;
        }

        int maxStackSize = Math.min(stackOnMouse.getMaxStackSize(), selectedSlot.getItemStackLimit(stackOnMouse));
        if (stackOnMouse.stackSize + targetStack.stackSize > maxStackSize) {
            return;
        }

        clickSlot(gui, selectedSlot, 0, 0);
        clickSlot(gui, selectedSlot, 0, 0);
    }

    private void handleMouseReleased(GuiContainer gui, int mouseButton) {
        if (activeGui == gui && (mouseButton == 0 || mouseButton == 1)) {
            resetDragState();
        }
    }

    private void clickSlot(GuiContainer gui, Slot slot, int mouseButton, int clickType) {
        if (mc.playerController != null && mc.thePlayer != null && gui.inventorySlots != null) {
            mc.playerController.windowClick(
                    gui.inventorySlots.windowId,
                    slot.slotNumber,
                    mouseButton,
                    clickType,
                    mc.thePlayer
            );
        }
    }

    private boolean isShiftDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    private boolean areStacksCompatible(ItemStack first, ItemStack second) {
        return first != null && second != null
                && ItemStack.areItemsEqual(first, second)
                && ItemStack.areItemStackTagsEqual(first, second);
    }

    private void resetDragState() {
        activeGui = null;
        oldSelectedSlot = null;
        leftDragActive = false;
    }

    private static InvTweaksModule getModule() {
        if (YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null) {
            return null;
        }
        InvTweaksModule module = YesClient.getInstance().getModuleManager().getModule(InvTweaksModule.class);
        return module != null && module.isEnabled() ? module : null;
    }
}

package com.yesclient.mixin;

import com.yesclient.module.modules.qol.InvTweaksModule;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer {

    @Shadow(remap = false)
    protected int field_147003_i;

    @Shadow(remap = false)
    protected int field_147009_r;

    @Shadow(remap = false)
    public Container field_147002_h;

    @Inject(method = "func_73864_a", at = @At("HEAD"))
    private void yesclient$mouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        InvTweaksModule.onMouseClicked((GuiContainer) (Object) this, getSlotAtPosition(mouseX, mouseY), mouseButton);
    }

    @Inject(method = "func_146273_a", at = @At("HEAD"))
    private void yesclient$mouseClickMove(int mouseX, int mouseY, int clickedMouseButton,
            long timeSinceLastClick, CallbackInfo ci) {
        InvTweaksModule.onMouseClickMove((GuiContainer) (Object) this,
                getSlotAtPosition(mouseX, mouseY), clickedMouseButton);
    }

    @Inject(method = "func_146286_b", at = @At("HEAD"))
    private void yesclient$mouseReleased(int mouseX, int mouseY, int state, CallbackInfo ci) {
        InvTweaksModule.onMouseReleased((GuiContainer) (Object) this, state);
    }

    private Slot getSlotAtPosition(int mouseX, int mouseY) {
        if (field_147002_h == null) {
            return null;
        }

        for (Object object : field_147002_h.inventorySlots) {
            Slot slot = (Slot) object;
            int left = field_147003_i + slot.xDisplayPosition;
            int top = field_147009_r + slot.yDisplayPosition;
            if (mouseX >= left - 1 && mouseX < left + 17
                    && mouseY >= top - 1 && mouseY < top + 17) {
                return slot;
            }
        }
        return null;
    }
}

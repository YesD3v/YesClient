package com.yesclient.mixin;

import com.yesclient.utils.InterfaceSounds;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiButton.class)
public abstract class MixinGuiButton {
    @Inject(method = "func_146113_a", at = @At("HEAD"), cancellable = true)
    private void yesclient$useConfiguredButtonSound(SoundHandler soundHandler, CallbackInfo ci) {
        InterfaceSounds.playClick();
        ci.cancel();
    }
}

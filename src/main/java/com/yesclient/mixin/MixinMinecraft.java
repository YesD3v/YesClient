package com.yesclient.mixin;

import com.yesclient.YesClient;
import com.yesclient.module.modules.visuals.FreeLookModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "func_71405_e", at = @At("HEAD"))
    private void yesclient$saveStateAndRestoreTemporarySettings(CallbackInfo ci) {
        if (YesClient.getInstance() != null) {
            YesClient.getInstance().onShutdown();
        }
    }


    @Redirect(method = "func_71407_l", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;func_78765_e()V"))
    private void nullCheckUpdateController(PlayerControllerMP instance) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            instance.updateController();
        }
    }

    @Redirect(method = "func_71407_l", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"))
    private int yesclient$redirectFreeLookScroll() {
        int wheel = Mouse.getEventDWheel();
        if (YesClient.getInstance() != null && YesClient.getInstance().getModuleManager() != null) {
            FreeLookModule freeLook = YesClient.getInstance().getModuleManager().getModule(FreeLookModule.class);
            if (freeLook != null && freeLook.handleScrollWheel(wheel)) return 0;
        }
        return wheel;
    }
}

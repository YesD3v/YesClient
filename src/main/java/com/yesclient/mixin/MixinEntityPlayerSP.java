package com.yesclient.mixin;

import com.yesclient.YesClient;
import com.yesclient.module.modules.visuals.FancyTextModule;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

 



@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {

    @org.spongepowered.asm.mixin.injection.Inject(method = "func_71165_d", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (com.yesclient.command.ChatCommandHandler.isCommand(message)) {
            boolean handled = com.yesclient.command.ChatCommandHandler.handleChatMessage(message);
            if (handled) {
                ci.cancel();
            }
        }
    }

    @ModifyVariable(method = "func_71165_d", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String revertFancyCommands(String message) {
        try {
            FancyTextModule fancyText = YesClient.getInstance().getModuleManager()
                    .getModule(FancyTextModule.class);
            if (fancyText != null && fancyText.isEnabled()) {
                if (fancyText.isRevertCommands() && FancyTextModule.isCommand(message)) {
                    return FancyTextModule.revertToNormal(message);
                } else if (!FancyTextModule.isCommand(message)) {
                     
                    return FancyTextModule.convertForChat(message);
                }
            }
        } catch (Exception e) {
             
        }
        return message;
    }
}

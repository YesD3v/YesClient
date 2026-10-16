package com.yesclient.mixin;

import com.yesclient.YesClient;
import com.yesclient.module.modules.client.HudDesignerModule;
import com.yesclient.module.modules.visuals.ScoreboardModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 







@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame {

     





    @Inject(method = "func_180475_a", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboard(ScoreObjective objective, ScaledResolution sr, CallbackInfo ci) {
         
        if (YesClient.getInstance() == null ||
                YesClient.getInstance().getModuleManager() == null) {
            return;
        }

        ScoreboardModule scoreboard = YesClient.getInstance()
                .getModuleManager()
                .getModule(ScoreboardModule.class);

        if (scoreboard != null && scoreboard.isEnabled()) {
             
            ci.cancel();
        }
    }
}

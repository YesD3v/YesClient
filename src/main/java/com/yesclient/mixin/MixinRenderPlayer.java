package com.yesclient.mixin;

import com.yesclient.YesClient;
import com.yesclient.module.modules.visuals.NametagsModule;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayer {

    @Redirect(
            method = "func_177069_a(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDLjava/lang/String;FD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/Scoreboard;func_96539_a(I)Lnet/minecraft/scoreboard/ScoreObjective;", remap = false),
            remap = false)
    private ScoreObjective yesclient$suppressDuplicateNametagHealth(Scoreboard scoreboard, int displaySlot) {
        if (displaySlot == 2 && YesClient.getInstance() != null
                && YesClient.getInstance().getModuleManager() != null) {
            NametagsModule nametags = YesClient.getInstance().getModuleManager().getModule(NametagsModule.class);
            if (nametags != null && nametags.suppressVanillaHealthLabel()) {
                return null;
            }
        }
        return scoreboard.getObjectiveInDisplaySlot(displaySlot);
    }
}

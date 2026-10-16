package com.yesclient.mixin;
import java.util.List;

import com.yesclient.utils.YesClientUserManager;
import com.yesclient.utils.MinecraftFormatting;
import com.yesclient.utils.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(GuiPlayerTabOverlay.class)
public class MixinGuiPlayerTabOverlay {

    @Inject(method = "func_175243_a", at = @At("RETURN"), cancellable = true)
    public void onGetPlayerName(NetworkPlayerInfo networkPlayerInfoIn, CallbackInfoReturnable<String> cir) {
        if (networkPlayerInfoIn == null || networkPlayerInfoIn.getGameProfile() == null) return;
        String name = networkPlayerInfoIn.getGameProfile().getName();
        try {
            if (com.yesclient.YesClient.instance != null && com.yesclient.YesClient.instance.getModuleManager() != null) {
                com.yesclient.module.modules.moderation.NickDetectorModule nickMod = 
                    (com.yesclient.module.modules.moderation.NickDetectorModule) com.yesclient.YesClient.instance.getModuleManager().getModule(com.yesclient.module.modules.moderation.NickDetectorModule.class);
                if (nickMod != null && nickMod.isEnabled() && nickMod.isNicked(name)) {
                    cir.setReturnValue(MinecraftFormatting.appendPreservingFormatting(
                            cir.getReturnValue(), "\u00A7c~"));
                }
            }
        } catch (Exception e) {}
    }

    @Redirect(method = "func_175249_a", at = @At(value = "INVOKE", target = "Ljava/util/List;subList(II)Ljava/util/List;", remap = false))
    public List<NetworkPlayerInfo> onSublist(List<NetworkPlayerInfo> instance, int fromIndex, int toIndex) {
         
        return com.yesclient.module.modules.visuals.NickHiderModule.reorderTabList(instance);
    }

    @Redirect(method = "func_175249_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;func_175063_a(Ljava/lang/String;FFI)I"))
    public int onDrawStringWithShadow(FontRenderer instance, String text, float x, float y, int color) {
         
        if (YesClientUserManager.getInstance().isRainbowName(text)) {
             
            return RenderUtils.drawRainbowString(text, x, y, true);
        }

         
        return instance.drawStringWithShadow(text, x, y, color);
    }
}

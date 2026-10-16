package com.yesclient.mixin;
import java.util.List;

import com.yesclient.YesClient;
import com.yesclient.module.modules.visuals.HideEntitiesModule;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntitySignRenderer.class)
public class MixinTileEntitySignRenderer {

    @Inject(method = "func_180535_a", at = @At("HEAD"), cancellable = true)
    public void onRenderTileEntityAt(TileEntitySign te, double x, double y, double z, float partialTicks,
            int destroyStage, CallbackInfo ci) {
        HideEntitiesModule hideEntities = YesClient.getInstance().getModuleManager()
                .getModule(HideEntitiesModule.class);
        if (hideEntities != null && hideEntities.isEnabled() && hideEntities.isHideSignsEnabled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "func_180535_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiUtilRenderComponents;func_178908_a(Lnet/minecraft/util/IChatComponent;ILnet/minecraft/client/gui/FontRenderer;ZZ)Ljava/util/List;"))
    public List<net.minecraft.util.IChatComponent> onSplitText(net.minecraft.util.IChatComponent str,
            int maxTextLenght, net.minecraft.client.gui.FontRenderer fontRendererIn, boolean p_178908_3_,
            boolean forceTextColor) {
        HideEntitiesModule hideEntities = YesClient.getInstance().getModuleManager()
                .getModule(HideEntitiesModule.class);
        if (hideEntities != null && hideEntities.isEnabled() && hideEntities.isHideSignTextEnabled()) {
            return null;
        }
        return net.minecraft.client.gui.GuiUtilRenderComponents.splitText(str, maxTextLenght, fontRendererIn,
                p_178908_3_, forceTextColor);
    }
}
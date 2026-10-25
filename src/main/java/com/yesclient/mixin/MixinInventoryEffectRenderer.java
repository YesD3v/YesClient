package com.yesclient.mixin;

import com.yesclient.module.modules.visuals.ActiveEffectsModule;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Active Effects "Hide Inventory Effects": no vanilla effect cards, and the inventory stays centered. */
@Mixin(InventoryEffectRenderer.class)
public abstract class MixinInventoryEffectRenderer extends GuiContainer {

    @Shadow(remap = false)
    private boolean field_147045_u; // hasActivePotionEffects

    public MixinInventoryEffectRenderer(Container container) {
        super(container);
    }

    @Inject(method = "func_175378_g", at = @At("HEAD"), cancellable = true)
    private void yesclient$hideEffectCards(CallbackInfo ci) {
        if (ActiveEffectsModule.shouldHideInventoryEffects()) {
            guiLeft = (width - xSize) / 2;
            field_147045_u = false;
            ci.cancel();
        }
    }
}

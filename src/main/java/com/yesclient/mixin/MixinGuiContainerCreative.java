package com.yesclient.mixin;

import com.yesclient.YesClient;
import com.yesclient.module.modules.visuals.ItemDisguiserModule;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 



@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreative {

    @Inject(method = "func_73869_a", at = @At("HEAD"), cancellable = true)
    private void yesclient$handleSavedHotbarKeys(char typedChar, int keyCode, CallbackInfo ci) {
        if (YesClient.getInstance() == null || YesClient.getInstance().getSavedHotbarStorage() == null) return;
        if (YesClient.getInstance().getSavedHotbarStorage().handleCreativeKeyPress(keyCode)) ci.cancel();
    }

    @ModifyVariable(method = "func_146285_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack yesclient$replaceCreativeSearchTooltipStack(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }
}

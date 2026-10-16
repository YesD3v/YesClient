package com.yesclient.mixin;

import com.yesclient.module.modules.visuals.ItemDisguiserModule;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 



@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @Shadow(aliases = "field_78453_b", remap = false)
    private ItemStack itemToRender;

    @Unique
    private ItemStack yesclient$realFirstPersonStack;

    @Unique
    private boolean yesclient$firstPersonStackReplaced;

    @Inject(method = "func_78440_a", at = @At("HEAD"))
    private void yesclient$useVisualFirstPersonStack(float partialTicks, CallbackInfo ci) {
        ItemStack visual = ItemDisguiserModule.getVisualStack(itemToRender);
        if (visual != itemToRender) {
            yesclient$realFirstPersonStack = itemToRender;
            yesclient$firstPersonStackReplaced = true;
            itemToRender = visual;
        }
    }

    @Inject(method = "func_78440_a", at = @At("RETURN"))
    private void yesclient$restoreRealFirstPersonStack(float partialTicks, CallbackInfo ci) {
        if (yesclient$firstPersonStackReplaced) {
            itemToRender = yesclient$realFirstPersonStack;
            yesclient$realFirstPersonStack = null;
            yesclient$firstPersonStackReplaced = false;
        }
    }

    @ModifyVariable(method = "func_178099_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack yesclient$replaceHeldItemStack(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }
}

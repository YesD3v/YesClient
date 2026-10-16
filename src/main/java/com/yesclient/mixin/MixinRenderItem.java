package com.yesclient.mixin;

import com.yesclient.module.modules.visuals.ItemDisguiserModule;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

 



@Mixin(RenderItem.class)
public abstract class MixinRenderItem {

    @ModifyVariable(method = "func_180454_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack yesclient$replaceRenderedStack(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }

    @ModifyVariable(method = "func_180453_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack yesclient$replaceOverlayStack(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }

    @ModifyVariable(method = "func_175049_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack yesclient$replaceEntityModelStack(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }
}

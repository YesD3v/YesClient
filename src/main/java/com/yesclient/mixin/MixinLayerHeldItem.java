package com.yesclient.mixin;

import com.yesclient.module.modules.visuals.ItemDisguiserModule;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

 



@Mixin(LayerHeldItem.class)
public abstract class MixinLayerHeldItem {

    @ModifyVariable(method = "func_177141_a", at = @At("STORE"), ordinal = 0)
    private ItemStack yesclient$useVisualHeldItemTransform(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }
}

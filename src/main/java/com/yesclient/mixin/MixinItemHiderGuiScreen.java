package com.yesclient.mixin;

import com.yesclient.module.modules.visuals.ItemDisguiserModule;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

 


@Mixin(GuiScreen.class)
public abstract class MixinItemHiderGuiScreen {

    @ModifyVariable(method = "func_146285_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack yesclient$replaceTooltipStack(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }
}

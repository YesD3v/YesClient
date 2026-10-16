package com.yesclient.mixin;

import com.yesclient.module.modules.visuals.ItemDisguiserModule;
import com.yesclient.module.modules.visuals.NickHiderModule;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FontRenderer.class)
public class MixinFontRenderer {

     



    @ModifyVariable(method = "func_180455_b", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String hideAccountName(String text) {
        return NickHiderModule.replaceOwnName(ItemDisguiserModule.replaceHeldItemName(text));
    }

     



    @ModifyVariable(method = "func_78256_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String measureHiddenAccountName(String text) {
        return NickHiderModule.replaceOwnName(ItemDisguiserModule.replaceHeldItemName(text));
    }
}

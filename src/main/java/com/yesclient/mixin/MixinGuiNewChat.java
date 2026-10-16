package com.yesclient.mixin;

import com.yesclient.module.modules.client.ChatModule;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {
    @ModifyVariable(method = "func_146230_a", at = @At("STORE"), index = 17)
    private String highlightRenderedChatLine(String text) {
        return ChatModule.highlightKeywordText(text);
    }

    @ModifyArg(method = "func_146230_a", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiNewChat;func_73734_a(IIIII)V", ordinal = 0), index = 4)
    private int applyBackgroundOpacity(int color) {
        ChatModule module = ChatModule.getActiveModule();
        if (module == null || !module.isEnabled()) return color;
        int alpha = (color >>> 24) & 0xFF;
        alpha = alpha * module.getBackgroundOpacityPercent() / 100;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    @Redirect(method = "func_146230_a", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;func_175063_a(Ljava/lang/String;FFI)I"))
    private int applyTextShadowSetting(FontRenderer renderer, String text, float x, float y, int color) {
        ChatModule module = ChatModule.getActiveModule();
        if (module != null && module.isEnabled() && !module.isTextShadowEnabled()) {
            return renderer.drawString(text, x, y, color, false);
        }
        return renderer.drawStringWithShadow(text, x, y, color);
    }
}

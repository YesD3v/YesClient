package com.yesclient.mixin;

import com.yesclient.YesClient;
import com.yesclient.module.modules.exploit.SignFillModule;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiEditSign.class)
public abstract class MixinGuiEditSign extends GuiScreen {

    @Shadow(remap = false)
    private TileEntitySign field_146848_f;  

    @Shadow(remap = false)
    private int field_146851_h;  

    @Inject(method = "func_73866_w_", at = @At("RETURN"))
    private void onInitGui(CallbackInfo ci) {
        if (YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null) {
            return;
        }

        SignFillModule signFill = YesClient.getInstance().getModuleManager().getModule(SignFillModule.class);
        if (signFill == null || !signFill.isEnabled()) {
            return;
        }

        TileEntitySign sign = this.field_146848_f;
        if (sign == null) {
            return;
        }

        for (int i = 0; i < 4; i++) {
            String lineText = signFill.getLine(i);
            if (lineText == null)
                lineText = "";
            sign.signText[i] = new ChatComponentText(lineText);
        }

        sign.markDirty();

        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                mc.displayGuiScreen(null);
            }
        });
    }

    @Inject(method = "func_73869_a", at = @At("HEAD"), cancellable = true)
    private void onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (isCtrlKeyDown() && (keyCode == 47)) {
            String clipboard = getClipboardString();
            if (clipboard != null && !clipboard.isEmpty()) {
                TileEntitySign sign = this.field_146848_f;
                int line = this.field_146851_h;
                if (sign != null && line >= 0 && line < 4) {
                    String current = sign.signText[line] != null
                            ? sign.signText[line].getUnformattedText()
                            : "";
                    String pasteText = clipboard.split("\n")[0].split("\r")[0];
                    String newText = current + pasteText;
                    if (newText.length() > 90) {
                        newText = newText.substring(0, 90);
                    }
                    sign.signText[line] = new ChatComponentText(newText);
                }
                ci.cancel();
            }
        }
    }
}

package com.yesclient.mixin;

import com.yesclient.YesClient;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameMenu.class)
public class MixinGuiIngameMenu extends GuiScreen {

    @Inject(method = "func_73866_w_", at = @At("RETURN"))
    private void onInitGui(CallbackInfo ci) {
         
        this.buttonList.add(new GuiButton(6969, this.width - 24, this.height - 24, 20, 20, ""));
    }

    @Inject(method = "func_146284_a", at = @At("HEAD"), cancellable = true)
    private void onActionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == 6969) {
            this.mc.displayGuiScreen(new com.yesclient.gui.GuiContentManager(this));
            ci.cancel();
            return;
        }
    }

    @Inject(method = "func_73863_a", at = @At("TAIL"))
    private void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        for (GuiButton button : this.buttonList) {
            if (button.id == 6969) {
                 
                net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                this.mc.getRenderItem().renderItemIntoGUI(new net.minecraft.item.ItemStack(net.minecraft.init.Items.book), button.xPosition + 2, button.yPosition + 2);
                net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            }
        }
    }
}

package com.yesclient.mixin;

import com.yesclient.YesClient;
import com.yesclient.module.Module;
import com.yesclient.module.modules.client.FancyTextModule;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 











@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    @Inject(method = "func_146282_l", at = @At("HEAD"))
    private void onHandleKeyboardInput(CallbackInfo ci) {
        try {
            GuiScreen self = (GuiScreen) (Object) this;
             
             
            if (!(self instanceof net.minecraft.client.gui.inventory.GuiChest))
                return;

            if (!Keyboard.getEventKeyState())
                return;  
            int key = Keyboard.getEventKey();
            if (key <= 0)
                return;

             
            if (YesClient.getInstance() != null && YesClient.getInstance().getModuleManager() != null) {
                for (Module module : YesClient.getInstance().getModuleManager().getModules()) {
                    module.handleKeybindPress(key);
                }
            }
        } catch (Throwable t) {
             
        }
    }

     




    @ModifyArg(method = "func_146282_l", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;func_73869_a(CI)V"), index = 0)
    private char modifyTypedChar(char original) {
         
        if (original < 32 && original != 0) {
            return original;
        }

         
        GuiScreen self = (GuiScreen) (Object) this;

         
        if (self instanceof com.yesclient.gui.ClickGUI) {
            return original;
        }

         
        if (self instanceof com.yesclient.gui.ItemSelectorGUI) {
            return original;
        }

         
        if (self instanceof GuiContainerCreative) {
            return original;
        }

        try {
            FancyTextModule fancyText = YesClient.getInstance().getModuleManager()
                    .getModule(FancyTextModule.class);
            if (fancyText != null && fancyText.isEnabled()) {
                return fancyText.convertChar(original);
            }
        } catch (Exception e) {
             
        }
        return original;
    }
}

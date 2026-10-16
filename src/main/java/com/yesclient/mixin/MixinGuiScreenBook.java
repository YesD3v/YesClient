package com.yesclient.mixin;
import java.lang.reflect.Field;

import com.yesclient.YesClient;
import com.yesclient.module.modules.exploit.AutoBegModule;

import net.minecraft.nbt.NBTTagList;
import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.GuiScreenBook")
public class MixinGuiScreenBook {

    @Inject(method = "func_73866_w_", at = @At("RETURN"), remap = false)
    public void onInitGui(CallbackInfo ci) {
        try {
            AutoBegModule autobeg = YesClient.getInstance().getModuleManager().getModule(AutoBegModule.class);
            if (autobeg != null && autobeg.isEnabled()) {
                 
                 
                NBTTagList bookPages = null;
                for (Field f : this.getClass().getDeclaredFields()) {
                    if (f.getType().equals(NBTTagList.class)) {
                        f.setAccessible(true);
                        bookPages = (NBTTagList) f.get(this);
                        break;
                    }
                }

                if (bookPages == null) {
                     
                    for (Field f : this.getClass().getSuperclass().getDeclaredFields()) {
                        if (f.getType().equals(NBTTagList.class)) {
                            f.setAccessible(true);
                            bookPages = (NBTTagList) f.get(this);
                            break;
                        }
                    }
                }

                if (bookPages != null) {
                    autobeg.checkAndAcceptGift(bookPages);
                }
            }
        } catch (Exception e) {
             
        }
    }
}

package com.yesclient.mixin;
import java.io.IOException;

import com.yesclient.YesClient;
import com.yesclient.module.modules.client.ClickGUIModule;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(SimpleReloadableResourceManager.class)
public class MixinSimpleReloadableResourceManager {

    @Inject(method = "func_110536_a", at = @At("HEAD"), cancellable = true)
    private void onGetResource(ResourceLocation location, CallbackInfoReturnable<IResource> cir) {
        if (location != null && location.getResourceDomain().equals("minecraft") && location.getResourcePath().startsWith("textures/font/")) {
            boolean isFancy = true;
            YesClient client = YesClient.getInstance();
            if (client != null && client.getModuleManager() != null) {
                ClickGUIModule settings = client.getModuleManager().getModule(ClickGUIModule.class);
                if (settings != null) {
                    isFancy = settings.isFancyFont();
                }
            }
            
            if (isFancy) {
                String fileName = location.getResourcePath().substring("textures/font/".length());
                ResourceLocation custom = new ResourceLocation("yesclient", "textures/font/slightly_improved/" + fileName);
                try {
                    IResource resource = ((SimpleReloadableResourceManager) (Object) this).getResource(custom);
                    cir.setReturnValue(resource);
                } catch (IOException e) {
                     
                }
            }
        }
    }
}

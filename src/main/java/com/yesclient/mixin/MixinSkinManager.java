package com.yesclient.mixin;
import java.util.HashMap;
import java.util.Map;

import com.google.common.cache.LoadingCache;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.resources.SkinManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(SkinManager.class)
public class MixinSkinManager {

    @Redirect(method = "func_152788_a", at = @At(value = "INVOKE", target = "Lcom/google/common/cache/LoadingCache;getUnchecked(Ljava/lang/Object;)Ljava/lang/Object;", remap = false))
    private Object catchSkinCrash(LoadingCache<Object, Map<Type, MinecraftProfileTexture>> cache, Object key) {
        try {
            return cache.getUnchecked(key);
        } catch (Exception e) {
             
            return new HashMap<Type, MinecraftProfileTexture>();
        }
    }
}

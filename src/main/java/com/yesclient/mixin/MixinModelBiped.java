package com.yesclient.mixin;

import com.yesclient.YesClient;
import com.yesclient.module.modules.exploit.ProjectileAimbotModule;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBiped.class)
public abstract class MixinModelBiped {

    @Shadow(remap = false) private ModelRenderer field_78116_c;

    @Inject(method = "func_78087_a", at = @At("RETURN"))
    private void yesclient$aimLocalHead(float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
        if (YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null) return;
        ProjectileAimbotModule aimbot = YesClient.getInstance().getModuleManager().getModule(ProjectileAimbotModule.class);
        if (aimbot != null && aimbot.shouldAdjustLocalHeadPitch(entity)) {
            field_78116_c.rotateAngleX = (float) Math.toRadians(aimbot.getTargetPitch());
        }
    }
}

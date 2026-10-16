package com.yesclient.mixin;

import com.yesclient.YesClient;
import com.yesclient.module.modules.visuals.FreeLookModule;
import com.yesclient.module.modules.visuals.HideEntitiesModule;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderManager.class)
public class MixinRenderManager {

    @Inject(method = "func_147939_a", at = @At("HEAD"), cancellable = true)
    private void onDoRenderEntity(Entity entity, double x, double y, double z, float entityYaw,
            float partialTicks, boolean hideDebugBox, CallbackInfoReturnable<Boolean> cir) {
        if (YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null)
            return;
        HideEntitiesModule hideEntities = YesClient.getInstance().getModuleManager()
                .getModule(HideEntitiesModule.class);
        if (hideEntities != null && hideEntities.isEnabled() && hideEntities.isEntityHidden(entity.getEntityId())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "func_180597_a", at = @At("TAIL"))
    private void yesclient$faceBillboardsWithFreeLook(World world, FontRenderer fontRenderer,
            Entity renderViewEntity, Entity pointedEntity, GameSettings options, float partialTicks,
            CallbackInfo ci) {
        if (renderViewEntity == null || YesClient.getInstance() == null
                || YesClient.getInstance().getModuleManager() == null) {
            return;
        }

        FreeLookModule freeLook = YesClient.getInstance().getModuleManager().getModule(FreeLookModule.class);
        if (freeLook == null || !freeLook.isEnabled()) {
            return;
        }

        RenderManager renderManager = (RenderManager) (Object) this;
        renderManager.playerViewY = freeLook.cameraYaw(renderViewEntity, renderViewEntity.rotationYaw);
        renderManager.playerViewX = freeLook.cameraPitch(renderViewEntity, renderViewEntity.rotationPitch);
    }
}

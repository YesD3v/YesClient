package com.yesclient.mixin;

import com.yesclient.YesClient;
import com.yesclient.module.modules.visuals.HideEntitiesModule;
import com.yesclient.module.modules.visuals.ItemDisguiserModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.RenderItemFrame;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 



@Mixin(RenderItemFrame.class)
public abstract class MixinRenderItemFrame {

    private static final ModelResourceLocation YESCLIENT_EMPTY_FRAME_MODEL =
            new ModelResourceLocation("item_frame", "normal");

    @Inject(method = "func_76986_a", at = @At("HEAD"), cancellable = true)
    private void yesclient$renderSafeFrame(EntityItemFrame entity, double x, double y, double z,
            float entityYaw, float partialTicks, CallbackInfo ci) {
        HideEntitiesModule hideEntities = yesclient$getHideEntities();
        if (hideEntities == null || !hideEntities.isEnabled()
                || !hideEntities.shouldHideItemFrameContents()) {
            return;
        }

         
        if (hideEntities.isHideItemFramesEnabled()) {
            ci.cancel();
            return;
        }

         



        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        BlockPos blockPos = entity.getHangingPosition();
        double renderX = (double) blockPos.getX() - entity.posX + x;
        double renderY = (double) blockPos.getY() - entity.posY + y;
        double renderZ = (double) blockPos.getZ() - entity.posZ + z;
        GlStateManager.translate(renderX + 0.5D, renderY + 0.5D, renderZ + 0.5D);
        GlStateManager.rotate(180.0F - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

        BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
        ModelManager modelManager = dispatcher.getBlockModelShapes().getModelManager();
        IBakedModel frameModel = modelManager.getModel(YESCLIENT_EMPTY_FRAME_MODEL);
        GlStateManager.pushMatrix();
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        dispatcher.getBlockModelRenderer().renderModelBrightnessColor(
                frameModel, 1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
        GlStateManager.popMatrix();
        ci.cancel();
    }

     
    @ModifyVariable(method = "func_82402_b", at = @At("STORE"), ordinal = 0)
    private ItemStack yesclient$getVisualFramedItem(ItemStack stack) {
        HideEntitiesModule hideEntities = yesclient$getHideEntities();
        if (hideEntities != null && hideEntities.isEnabled()
                && hideEntities.shouldHideItemFrameContents()) {
            return null;
        }
        return ItemDisguiserModule.getVisualStack(stack);
    }

    private static HideEntitiesModule yesclient$getHideEntities() {
        if (YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null) {
            return null;
        }
        return YesClient.getInstance().getModuleManager().getModule(HideEntitiesModule.class);
    }
}

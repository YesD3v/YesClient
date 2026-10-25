package com.yesclient.mixin;

import com.yesclient.module.modules.misc.ReachModule;
import com.yesclient.YesClient;
import com.yesclient.module.modules.visuals.FreeLookModule;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    private FreeLookModule yesclient$freeLook() {
        if (YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null) return null;
        return YesClient.getInstance().getModuleManager().getModule(FreeLookModule.class);
    }

    @Redirect(method = "func_181560_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;func_70082_c(FF)V"))
    private void yesclient$redirectFreeLookMouse(EntityPlayerSP player, float mouseX, float mouseY) {
        FreeLookModule freeLook = yesclient$freeLook();
        if (freeLook != null) freeLook.redirectMouseLook(player, mouseX, mouseY);
        else player.setAngles(mouseX, mouseY);
    }

    @Redirect(method = "func_78467_g", at = @At(value = "FIELD",
            target = "Lnet/minecraft/entity/Entity;field_70126_B:F", opcode = Opcodes.GETFIELD))
    private float yesclient$cameraPrevYaw(Entity entity) {
        FreeLookModule freeLook = yesclient$freeLook();
        return freeLook == null ? entity.prevRotationYaw : freeLook.cameraYaw(entity, entity.prevRotationYaw);
    }

    @Redirect(method = "func_78467_g", at = @At(value = "FIELD",
            target = "Lnet/minecraft/entity/Entity;field_70177_z:F", opcode = Opcodes.GETFIELD))
    private float yesclient$cameraYaw(Entity entity) {
        FreeLookModule freeLook = yesclient$freeLook();
        return freeLook == null ? entity.rotationYaw : freeLook.cameraYaw(entity, entity.rotationYaw);
    }

    @Redirect(method = "func_78467_g", at = @At(value = "FIELD",
            target = "Lnet/minecraft/entity/Entity;field_70127_C:F", opcode = Opcodes.GETFIELD))
    private float yesclient$cameraPrevPitch(Entity entity) {
        FreeLookModule freeLook = yesclient$freeLook();
        return freeLook == null ? entity.prevRotationPitch : freeLook.cameraPitch(entity, entity.prevRotationPitch);
    }

    @Redirect(method = "func_78467_g", at = @At(value = "FIELD",
            target = "Lnet/minecraft/entity/Entity;field_70125_A:F", opcode = Opcodes.GETFIELD))
    private float yesclient$cameraPitch(Entity entity) {
        FreeLookModule freeLook = yesclient$freeLook();
        return freeLook == null ? entity.rotationPitch : freeLook.cameraPitch(entity, entity.rotationPitch);
    }

    @ModifyArg(method = "func_78467_g", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;func_179109_b(FFF)V", ordinal = 2), index = 2)
    private float yesclient$cameraDistance(float vanillaZ) {
        FreeLookModule freeLook = yesclient$freeLook();
        return freeLook != null && freeLook.isEnabled() ? (float) -freeLook.getCameraDistance() : vanillaZ;
    }

    @Redirect(method = "func_78473_a", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;func_174822_a(DF)Lnet/minecraft/util/MovingObjectPosition;"))
    private MovingObjectPosition yesclient$freeLookBlockRay(Entity entity, double reach, float partialTicks) {
        return entity.rayTrace(reach, partialTicks);
    }

    @Redirect(method = "func_78473_a", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;func_70676_i(F)Lnet/minecraft/util/Vec3;"))
    private Vec3 yesclient$freeLookPickVector(Entity entity, float partialTicks) {
        return entity.getLook(partialTicks);
    }

     





    @Redirect(method = "func_78473_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;func_78757_d()F"))
    private float redirectGetBlockReachDistance(PlayerControllerMP controller) {
        if (com.yesclient.YesClient.getInstance() != null
                && com.yesclient.YesClient.getInstance().getModuleManager() != null) {
            ReachModule reach = (ReachModule) com.yesclient.YesClient.getInstance().getModuleManager()
                    .getModule(ReachModule.class);
            if (reach != null && reach.isEnabled()) {
                 
                return (float) Math.max(reach.getLeftReach(), reach.getRightReach());
            }
        }
        return controller.getBlockReachDistance();
    }
}

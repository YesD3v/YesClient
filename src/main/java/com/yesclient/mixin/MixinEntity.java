package com.yesclient.mixin;
import java.util.List;

import com.yesclient.YesClient;
import com.yesclient.module.modules.movement.SafeWalkModule;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Redirect(method = "func_70091_d", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;func_70093_af()Z"))
    private boolean safeWalkCheck(Entity entity) {
        if (entity.isSneaking()) return true;
        
        SafeWalkModule safeWalk = YesClient.getInstance().getModuleManager().getModule(SafeWalkModule.class);
        if (safeWalk != null && safeWalk.isEnabled() && entity == Minecraft.getMinecraft().thePlayer && Minecraft.getMinecraft().thePlayer.onGround) {
            return true;
        }
        
        return false;
    }

    @Redirect(method = "func_70091_d", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;func_72945_a(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;"))
    private List<net.minecraft.util.AxisAlignedBB> redirectGetCollidingBoundingBoxes(net.minecraft.world.World world, Entity entity, net.minecraft.util.AxisAlignedBB bb) {
        List<net.minecraft.util.AxisAlignedBB> originalList = world.getCollidingBoundingBoxes(entity, bb);
        
        SafeWalkModule safeWalk = YesClient.getInstance().getModuleManager().getModule(SafeWalkModule.class);
        if (safeWalk != null && safeWalk.isEnabled() && entity == Minecraft.getMinecraft().thePlayer && !entity.isSneaking()) {
             
            if (Math.abs(bb.minY - (entity.getEntityBoundingBox().minY - 1.0D)) < 0.01) {
                if (originalList.isEmpty()) {
                    int threshold = safeWalk.threshold.getIntValue();
                    if (threshold > 1) {
                        net.minecraft.util.AxisAlignedBB deepBb = bb.addCoord(0, -(threshold - 1), 0);
                        List<net.minecraft.util.AxisAlignedBB> deepList = world.getCollidingBoundingBoxes(entity, deepBb);
                        if (!deepList.isEmpty()) {
                            return deepList;
                        }
                    }
                }
            }
        }
        return originalList;
    }
}

package com.yesclient.mixin;

import com.yesclient.utils.ItemFramePacketFilter;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 
@Mixin(S13PacketDestroyEntities.class)
public abstract class MixinS13PacketDestroyEntities {

    @Inject(method = "func_148837_a", at = @At("RETURN"))
    private void yesclient$forgetDestroyedFrames(PacketBuffer buffer, CallbackInfo ci) {
        int[] entityIds = ((S13PacketDestroyEntities) (Object) this).getEntityIDs();
        if (entityIds == null) {
            return;
        }
        for (int entityId : entityIds) {
            ItemFramePacketFilter.remove(entityId);
        }
    }
}

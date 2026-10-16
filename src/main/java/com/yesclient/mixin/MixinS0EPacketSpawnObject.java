package com.yesclient.mixin;

import com.yesclient.utils.ItemFramePacketFilter;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 
@Mixin(S0EPacketSpawnObject.class)
public abstract class MixinS0EPacketSpawnObject {

    @Inject(method = "func_148837_a", at = @At("RETURN"))
    private void yesclient$recordObjectType(PacketBuffer buffer, CallbackInfo ci) {
        S0EPacketSpawnObject packet = (S0EPacketSpawnObject) (Object) this;
        ItemFramePacketFilter.recordObjectSpawn(packet.getEntityID(), packet.getType());
    }
}

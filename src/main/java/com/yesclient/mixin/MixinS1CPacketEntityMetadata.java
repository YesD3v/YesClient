package com.yesclient.mixin;
import java.io.IOException;
import java.util.List;

import com.yesclient.utils.ItemFramePacketFilter;
import net.minecraft.entity.DataWatcher;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;

 



@Mixin(S1CPacketEntityMetadata.class)
public abstract class MixinS1CPacketEntityMetadata {

    @Redirect(method = "func_148837_a", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/DataWatcher;func_151508_b(Lnet/minecraft/network/PacketBuffer;)Ljava/util/List;"))
    private List<DataWatcher.WatchableObject> yesclient$discardHiddenFrameMetadata(PacketBuffer buffer)
            throws IOException {
        int entityId = ((S1CPacketEntityMetadata) (Object) this).getEntityId();
        if (ItemFramePacketFilter.shouldDiscardMetadata(entityId)) {
             
             
            buffer.skipBytes(buffer.readableBytes());
            return Collections.emptyList();
        }
        return DataWatcher.readWatchedListFromPacketBuffer(buffer);
    }
}

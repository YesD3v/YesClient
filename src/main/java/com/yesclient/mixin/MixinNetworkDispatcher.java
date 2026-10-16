package com.yesclient.mixin;
import java.lang.reflect.Method;

import com.yesclient.YesClient;
import com.yesclient.utils.BungeePacketQueue;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraftforge.fml.common.network.handshake.NetworkDispatcher", remap = false)
public class MixinNetworkDispatcher {

     













    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onChannelRead0(ChannelHandlerContext ctx, Packet<?> msg, CallbackInfo ci) {
        if (msg instanceof S3FPacketCustomPayload) {
            S3FPacketCustomPayload packet = (S3FPacketCustomPayload) msg;

             
             
            String channel = "";
            try {
                channel = packet.getChannelName();
            } catch (Throwable t) {
                try {
                    Method m = packet.getClass().getMethod("func_149176_c");
                    channel = (String) m.invoke(packet);
                } catch (Throwable t2) {
                     
                    return;
                }
            }

            if ("REGISTER".equals(channel) || "UNREGISTER".equals(channel)) {
                net.minecraft.network.INetHandler handler = YesClient.getMinecraft().getNetHandler();

                 
                 
                 
                boolean isNotReady = (handler == null)
                        || (handler instanceof net.minecraft.network.login.INetHandlerLoginClient)
                        || (handler.getClass().getName().contains("NetHandlerLoginClient"));

                if (isNotReady) {
                    BungeePacketQueue.delayPacketQueue.add(msg);
                    ci.cancel();
                }
            }
        }
    }
}

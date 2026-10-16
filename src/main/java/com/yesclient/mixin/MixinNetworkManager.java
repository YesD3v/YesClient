package com.yesclient.mixin;
import java.lang.reflect.Field;

import com.yesclient.YesClient;

import com.yesclient.module.modules.exploit.BlinkModule;
import com.yesclient.module.modules.exploit.CustomAuthorModule;
import com.yesclient.module.modules.exploit.SilentNukerModule;
import com.yesclient.module.modules.misc.CommandCheckerModule;
import com.yesclient.module.modules.exploit.PacketMultiplierModule;
import com.yesclient.module.modules.exploit.ProjectileAimbotModule;
import com.yesclient.module.modules.visuals.FreeCamModule;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S3APacketTabComplete;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {

     



    @Inject(method = "func_179290_a", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {

        try {
            ProjectileAimbotModule.handleOutboundUse(packet);
        } catch (Throwable ignored) {
        }

         
         
        try {
            if (SilentNukerModule.handleOutbound(packet)) {
                ci.cancel();
                return;
            }
        } catch (Throwable t) {
             
        }

         
        try {
            if (CustomAuthorModule.handleOutbound(packet)) {
                ci.cancel();
                return;
            }
        } catch (Throwable t) {
             
        }

         
        try {
            BlinkModule blink = YesClient.getInstance().getModuleManager().getModule(BlinkModule.class);
            if (blink != null && BlinkModule.shouldCancelPacket(packet)) {
                ci.cancel();
            }
        } catch (Throwable t) {
             
        }

         
        try {
            if (PacketMultiplierModule.shouldMultiplyPacket(packet)) {
                ci.cancel();
            }
        } catch (Throwable t) {
             
        }

         
         
        try {
            if (packet instanceof C0BPacketEntityAction) {
                FreeCamModule freeCam = YesClient.getInstance().getModuleManager().getModule(FreeCamModule.class);
                if (freeCam != null && freeCam.isEnabled()) {
                    C0BPacketEntityAction actionPacket = (C0BPacketEntityAction) packet;
                    C0BPacketEntityAction.Action action = actionPacket.getAction();
                    if (action == C0BPacketEntityAction.Action.START_SNEAKING
                            || action == C0BPacketEntityAction.Action.STOP_SNEAKING) {
                        ci.cancel();
                    }
                }
            }
        } catch (Throwable t) {
             
        }

         
        try {
            if (packet instanceof C02PacketUseEntity) {
                FreeCamModule freeCam = YesClient.getInstance().getModuleManager().getModule(FreeCamModule.class);
                if (freeCam != null && freeCam.isEnabled()) {
                    C02PacketUseEntity usePacket = (C02PacketUseEntity) packet;
                    int targetId = -1;

                    try {
                         
                        Field f = usePacket.getClass().getDeclaredField("field_149567_a");
                        f.setAccessible(true);
                        targetId = f.getInt(usePacket);
                    } catch (Exception e1) {
                        try {
                             
                            Field f = usePacket.getClass().getDeclaredField("entityId");
                            f.setAccessible(true);
                            targetId = f.getInt(usePacket);
                        } catch (Exception e2) {
                             
                        }
                    }

                    if (targetId != -1 && targetId == YesClient.getMinecraft().thePlayer.getEntityId()) {
                        ci.cancel();
                    }
                }
            }
        } catch (Throwable t) {
             
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void onChannelRead0(io.netty.channel.ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
         
         
         
        try {
            if (packet instanceof S08PacketPlayerPosLook) {
                if (BlinkModule.handleServerPositionCorrection((S08PacketPlayerPosLook) packet)) {
                    ci.cancel();
                    return;
                }
                SilentNukerModule.onServerPositionCorrection();
            }
        } catch (Throwable t) {
             
        }

         
        try {
            if (packet instanceof S3APacketTabComplete) {
                CommandCheckerModule checker = YesClient.getInstance().getModuleManager().getModule(CommandCheckerModule.class);
                if (checker != null && checker.isEnabled()) {
                    checker.onTabCompleteResponse((S3APacketTabComplete) packet);
                }
            }
        } catch (Throwable t) {
             
        }
        NetworkManager manager = (NetworkManager) (Object) this;
        final net.minecraft.network.INetHandler handler = manager.getNetHandler();

         
         
        if (handler instanceof net.minecraft.client.network.NetHandlerLoginClient) {
            String className = packet.getClass().getName();

             
            if (className.startsWith("net.minecraft.network.play.server.")) {
                com.yesclient.utils.BungeePacketQueue.delayPacketQueue.add(packet);
                ci.cancel();
            }
             
             
            else if (className.equals("net.minecraftforge.fml.common.network.internal.FMLProxyPacket")) {
                net.minecraftforge.fml.common.network.internal.FMLProxyPacket proxyPacket = (net.minecraftforge.fml.common.network.internal.FMLProxyPacket) packet;
                String channel = proxyPacket.channel();

                 
                 
                if (!"FML|HS".equals(channel)) {
                    com.yesclient.utils.BungeePacketQueue.delayPacketQueue.add(packet);
                    ci.cancel();
                }
            }
        }
    }
}

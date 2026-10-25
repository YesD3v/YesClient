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
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S3APacketTabComplete;
import net.minecraft.entity.Entity;
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
            FreeCamModule freeCam = YesClient.getInstance().getModuleManager().getModule(FreeCamModule.class);
            if (freeCam == null || !freeCam.isEnabled()) return;

            if (packet instanceof C07PacketPlayerDigging) {
                if (!freeCam.canInteractWithBlock(((C07PacketPlayerDigging) packet).getPosition())) {
                    ci.cancel();
                    return;
                }
            } else if (packet instanceof C08PacketPlayerBlockPlacement) {
                net.minecraft.util.BlockPos pos = ((C08PacketPlayerBlockPlacement) packet).getPosition();
                boolean airUse = pos == null || (pos.getX() == -1 && pos.getY() == -1 && pos.getZ() == -1);
                if (airUse ? !freeCam.canUseItemFromCamera() : !freeCam.canInteractWithBlock(pos)) {
                    ci.cancel();
                    return;
                }
            } else if (packet instanceof C02PacketUseEntity) {
                C02PacketUseEntity usePacket = (C02PacketUseEntity) packet;
                int targetId = -1;
                try {
                    Field field = usePacket.getClass().getDeclaredField("field_149567_a");
                    field.setAccessible(true);
                    targetId = field.getInt(usePacket);
                } catch (Exception ignored) {
                    try {
                        Field field = usePacket.getClass().getDeclaredField("entityId");
                        field.setAccessible(true);
                        targetId = field.getInt(usePacket);
                    } catch (Exception ignoredAgain) {
                    }
                }
                net.minecraft.client.Minecraft minecraft = YesClient.getMinecraft();
                Entity target = minecraft == null || minecraft.theWorld == null || targetId < 0
                        ? null : minecraft.theWorld.getEntityByID(targetId);
                if (!freeCam.canInteractWithEntity(target)) {
                    ci.cancel();
                    return;
                }
            } else if (packet instanceof C0BPacketEntityAction && !freeCam.canUseItemFromCamera()) {
                ci.cancel();
                return;
            }
        } catch (Throwable ignored) {
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

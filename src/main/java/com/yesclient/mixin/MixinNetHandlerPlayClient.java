package com.yesclient.mixin;
import java.util.List;
import java.util.Iterator;

import com.yesclient.YesClient;
import com.yesclient.module.modules.exploit.BlatantFlyModule;
import com.yesclient.module.modules.misc.NoDebuffModule;
import com.yesclient.module.modules.visuals.HideEntitiesModule;
import com.yesclient.utils.ItemFramePacketFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S20PacketEntityProperties;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import com.yesclient.module.modules.visuals.TPSModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Inject(method = "func_147282_a", at = @At("HEAD"))
    private void yesclient$clearFramePacketFilterOnJoin(S01PacketJoinGame packet, CallbackInfo ci) {
        if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            ItemFramePacketFilter.clear();
        }
    }

    @Inject(method = "func_147280_a", at = @At("HEAD"))
    private void yesclient$clearFramePacketFilterOnRespawn(S07PacketRespawn packet, CallbackInfo ci) {
        if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            ItemFramePacketFilter.clear();
        }
    }

     
    @Inject(method = "func_147235_a", at = @At("HEAD"), cancellable = true)
    private void yesclient$discardItemFrameSpawn(S0EPacketSpawnObject packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.isCallingFromMinecraftThread() || packet.getType() != 71) {
            return;
        }

        HideEntitiesModule hideEntities = yesclient$getHideEntities();
        if (hideEntities != null && hideEntities.isEnabled()
                && hideEntities.isHideItemFramesEnabled()) {
            ci.cancel();
        }
    }

     
    @Inject(method = "func_147284_a", at = @At("HEAD"), cancellable = true)
    private void yesclient$discardItemFrameMetadata(S1CPacketEntityMetadata packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.isCallingFromMinecraftThread() || mc.theWorld == null) {
            return;
        }

        HideEntitiesModule hideEntities = yesclient$getHideEntities();
        if (hideEntities == null || !hideEntities.isEnabled()
                || !hideEntities.shouldHideItemFrameContents()) {
            return;
        }

        Entity entity = mc.theWorld.getEntityByID(packet.getEntityId());
        if (!(entity instanceof EntityItemFrame)) {
            return;
        }

        if (hideEntities.isHideItemFramesEnabled()) {
            ci.cancel();
            return;
        }

        List<DataWatcher.WatchableObject> metadata = packet.func_149376_c();
        if (metadata == null) {
            return;
        }

        Iterator<DataWatcher.WatchableObject> iterator = metadata.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getDataValueId() == 8) {
                iterator.remove();
            }
        }
    }

    @Inject(method = "func_147270_a", at = @At("HEAD"), cancellable = true)
    public void handlePlayerAbilities(S39PacketPlayerAbilities packet, CallbackInfo ci) {
        if (YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null) return;

        NoDebuffModule noDebuff = YesClient.getInstance().getModuleManager().getModule(NoDebuffModule.class);
        if (noDebuff != null && noDebuff.isEnabled() && noDebuff.isSlownessRemovalEnabled()) {
             
            if (packet.getWalkSpeed() < 0.1f) {
                packet.setWalkSpeed(0.1f);
            }
        }

        BlatantFlyModule fly = YesClient.getInstance().getModuleManager().getModule(BlatantFlyModule.class);
        Minecraft minecraft = Minecraft.getMinecraft();
        if (fly != null && fly.isEnabled() && minecraft.thePlayer != null
                && !minecraft.thePlayer.capabilities.isCreativeMode) {
            packet.setAllowFlying(true);
            packet.setFlying(minecraft.thePlayer.capabilities.isFlying);
        }

    }

    @Inject(method = "func_147260_a", at = @At("HEAD"), cancellable = true)
    public void handleEntityEffect(S1DPacketEntityEffect packet, CallbackInfo ci) {
        if (Minecraft.getMinecraft().thePlayer != null
                && packet.getEntityId() == Minecraft.getMinecraft().thePlayer.getEntityId()) {
            NoDebuffModule noDebuff = YesClient.getInstance()
                    .getModuleManager().getModule(NoDebuffModule.class);
            if (noDebuff != null && noDebuff.isEnabled() && noDebuff.shouldRemoveEffect(packet.getEffectId())) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "func_147290_a", at = @At("HEAD"), cancellable = true)
    public void handleEntityProperties(S20PacketEntityProperties packet, CallbackInfo ci) {
        if (Minecraft.getMinecraft().thePlayer != null
                && packet.getEntityId() == Minecraft.getMinecraft().thePlayer.getEntityId()) {
            NoDebuffModule noDebuff = YesClient.getInstance()
                    .getModuleManager().getModule(NoDebuffModule.class);
            if (noDebuff != null && noDebuff.isEnabled() && noDebuff.isSlownessRemovalEnabled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "func_147285_a", at = @At("HEAD"))
    public void handleTimeUpdate(S03PacketTimeUpdate packet, CallbackInfo ci) {
        TPSModule tpsModule = YesClient.getInstance().getModuleManager().getModule(TPSModule.class);
        if (tpsModule != null) {
            tpsModule.onTimeUpdate();
        }
    }

    @Inject(method = "func_147236_a", at = @At("HEAD"), cancellable = true)
    public void handleEntityStatus(S19PacketEntityStatus packet, CallbackInfo ci) {
        if (packet.getOpCode() == 17) {  
            net.minecraft.entity.Entity entity = packet.getEntity(Minecraft.getMinecraft().theWorld);
            if (entity instanceof net.minecraft.entity.item.EntityFireworkRocket) {
                com.yesclient.module.modules.visuals.HideEntitiesModule hideEntities = (com.yesclient.module.modules.visuals.HideEntitiesModule) YesClient
                        .getInstance().getModuleManager()
                        .getModule(com.yesclient.module.modules.visuals.HideEntitiesModule.class);

                if (hideEntities != null && hideEntities.isEnabled() && hideEntities.shouldHideEntity(entity)) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "func_147234_a", at = @At("HEAD"))
    public void handleBlockChange(net.minecraft.network.play.server.S23PacketBlockChange packet, CallbackInfo ci) {
        com.yesclient.module.modules.moderation.GrieferDetectorModule detector = (com.yesclient.module.modules.moderation.GrieferDetectorModule) YesClient
                .getInstance().getModuleManager()
                .getModule(com.yesclient.module.modules.moderation.GrieferDetectorModule.class);
        if (detector != null && detector.isEnabled()) {
            detector.onPacketReceive(packet);
        }
    }

    @Inject(method = "func_147287_a", at = @At("HEAD"))
    public void handleMultiBlockChange(net.minecraft.network.play.server.S22PacketMultiBlockChange packet,
            CallbackInfo ci) {
        com.yesclient.module.modules.moderation.GrieferDetectorModule detector = (com.yesclient.module.modules.moderation.GrieferDetectorModule) YesClient
                .getInstance().getModuleManager()
                .getModule(com.yesclient.module.modules.moderation.GrieferDetectorModule.class);
        if (detector != null && detector.isEnabled()) {
            detector.onPacketReceive(packet);
        }
    }

    @Inject(method = "func_147268_a", at = @At("HEAD"), cancellable = true)
    public void handleSignEditorOpen(net.minecraft.network.play.server.S36PacketSignEditorOpen packet,
            CallbackInfo ci) {
        com.yesclient.module.modules.exploit.SignFillModule signFill = (com.yesclient.module.modules.exploit.SignFillModule) com.yesclient.YesClient
                .getInstance()
                .getModuleManager().getModule(com.yesclient.module.modules.exploit.SignFillModule.class);

        if (signFill != null && signFill.isEnabled()) {
            net.minecraft.util.BlockPos pos = packet.getSignPosition();
            net.minecraft.util.IChatComponent[] lines = new net.minecraft.util.IChatComponent[4];
            lines[0] = new net.minecraft.util.ChatComponentText(signFill.getLine(0));
            lines[1] = new net.minecraft.util.ChatComponentText(signFill.getLine(1));
            lines[2] = new net.minecraft.util.ChatComponentText(signFill.getLine(2));
            lines[3] = new net.minecraft.util.ChatComponentText(signFill.getLine(3));

            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld != null) {
                net.minecraft.tileentity.TileEntity te = mc.theWorld.getTileEntity(pos);
                if (te instanceof net.minecraft.tileentity.TileEntitySign) {
                    net.minecraft.tileentity.TileEntitySign sign = (net.minecraft.tileentity.TileEntitySign) te;
                    sign.signText[0] = lines[0];
                    sign.signText[1] = lines[1];
                    sign.signText[2] = lines[2];
                    sign.signText[3] = lines[3];
                }
            }

            mc.getNetHandler().addToSendQueue(new net.minecraft.network.play.client.C12PacketUpdateSign(pos, lines));
            ci.cancel();
        }
    }

    private static HideEntitiesModule yesclient$getHideEntities() {
        if (YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null) {
            return null;
        }
        return YesClient.getInstance().getModuleManager().getModule(HideEntitiesModule.class);
    }
}

package com.yesclient.event;

import com.yesclient.YesClient;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class EventManager {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (YesClient.getMinecraft().thePlayer == null)
            return;
        if (YesClient.getMinecraft().theWorld == null)
            return;

        YesClient.getInstance().getModuleManager().onTick();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (YesClient.getMinecraft().thePlayer == null)
            return;
        if (YesClient.getMinecraft().theWorld == null)
            return;

        YesClient.getInstance().getModuleManager().onRender3D(event.partialTicks);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT)
            return;
        if (YesClient.getMinecraft().thePlayer == null)
            return;

        YesClient.getInstance().getModuleManager().onRender();
    }

    @SubscribeEvent
    public void onCameraSetup(net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup event) {
        com.yesclient.module.modules.visuals.FreeCamModule freeCam = YesClient.getInstance().getModuleManager()
                .getModule(com.yesclient.module.modules.visuals.FreeCamModule.class);

        if (freeCam != null && freeCam.isEnabled()) {
             
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent event) {
        if (event.entity.worldObj.isRemote && event.entity.equals(YesClient.getMinecraft().thePlayer)) {
            if (event.entity instanceof net.minecraft.entity.EntityLivingBase) {
                YesClient.getInstance().getModuleManager()
                        .onUpdate((net.minecraft.entity.EntityLivingBase) event.entity);
            }
        }
    }

    @SubscribeEvent
    public void onLivingJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (event.entity.worldObj.isRemote && event.entity.equals(YesClient.getMinecraft().thePlayer)) {
            YesClient.getInstance().getModuleManager().onJump(event);
        }
    }
}

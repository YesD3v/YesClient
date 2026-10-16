package com.yesclient.module.modules.visuals;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.NumberSetting;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

 










public class FreeCamModule extends Module {

    private final NumberSetting speed = new NumberSetting("Speed", "Camera flight speed", 1.0, 0.1, 5.0, 0.1);
    private final BooleanSetting showPlayer = new BooleanSetting("Show Player", "Show your body at real position",
            true);
    private final BooleanSetting allowInteractions = new BooleanSetting("Allow Interactions",
            "Allow interacting with the world from camera position", true);

     
    private EntityOtherPlayerMP cameraEntity;

     
    private EntityOtherPlayerMP ghostEntity;

     
    private double savedX, savedY, savedZ;
    private float savedYaw, savedPitch;

     
    private Entity originalRenderEntity;

     
    private float cameraYaw, cameraPitch;

     
     
    private float lastPlayerYaw, lastPlayerPitch;

     
    private boolean savedSneaking;

    public FreeCamModule() {
        super("FreeCam", "True detached camera (anticheat safe)", Category.VISUALS, ModuleMode.BOTH);
        addSetting(speed);
        addSetting(showPlayer);
        addSetting(allowInteractions);
    }

    @Override
    protected void onEnable() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            setEnabled(false);
            return;
        }

         
        savedX = mc.thePlayer.posX;
        savedY = mc.thePlayer.posY;
        savedZ = mc.thePlayer.posZ;
        savedYaw = mc.thePlayer.rotationYaw;
        savedPitch = mc.thePlayer.rotationPitch;

         
        cameraYaw = savedYaw;
        cameraPitch = savedPitch;

         
        lastPlayerYaw = savedYaw;
        lastPlayerPitch = savedPitch;

         
        savedSneaking = mc.thePlayer.isSneaking();

         
        originalRenderEntity = mc.getRenderViewEntity();

         
        cameraEntity = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        cameraEntity.copyLocationAndAnglesFrom(mc.thePlayer);
        cameraEntity.rotationYawHead = mc.thePlayer.rotationYawHead;
        cameraEntity.noClip = true;
        cameraEntity.inventory.copyInventory(mc.thePlayer.inventory);

        mc.theWorld.addEntityToWorld(-1338, cameraEntity);
        mc.setRenderViewEntity(cameraEntity);

         
        if (showPlayer.isEnabled()) {
            ghostEntity = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
            ghostEntity.copyLocationAndAnglesFrom(mc.thePlayer);
            ghostEntity.rotationYawHead = mc.thePlayer.rotationYawHead;
            ghostEntity.renderYawOffset = mc.thePlayer.renderYawOffset;
            ghostEntity.inventory.copyInventory(mc.thePlayer.inventory);
            mc.theWorld.addEntityToWorld(-1337, ghostEntity);
        }

        lastFrameTime = 0;
    }

    @Override
    protected void onDisable() {
        if (originalRenderEntity != null) {
            mc.setRenderViewEntity(originalRenderEntity);
        } else if (mc.thePlayer != null) {
            mc.setRenderViewEntity(mc.thePlayer);
        }

         
        if (mc.thePlayer != null) {
            mc.thePlayer.rotationYaw = savedYaw;
            mc.thePlayer.rotationPitch = savedPitch;
            mc.thePlayer.prevRotationYaw = savedYaw;
            mc.thePlayer.prevRotationPitch = savedPitch;
            mc.thePlayer.rotationYawHead = savedYaw;
            mc.thePlayer.renderArmYaw = savedYaw;
            mc.thePlayer.prevRenderArmYaw = savedYaw;
            mc.thePlayer.renderArmPitch = savedPitch;
            mc.thePlayer.prevRenderArmPitch = savedPitch;
        }

        if (cameraEntity != null && mc.theWorld != null) {
            mc.theWorld.removeEntity(cameraEntity);
            cameraEntity = null;
        }

        if (ghostEntity != null && mc.theWorld != null) {
            mc.theWorld.removeEntity(ghostEntity);
            ghostEntity = null;
        }
    }

     



    @SubscribeEvent
    public void onWorldUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if (isEnabled()) {
             
            cameraEntity = null;
            ghostEntity = null;
            originalRenderEntity = null;
            setEnabled(false);
        }
    }

     



    @SubscribeEvent
    public void onUpdate(net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || cameraEntity == null)
            return;
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END)
            return;

         
        mc.thePlayer.setPosition(savedX, savedY, savedZ);
        mc.thePlayer.prevPosX = savedX;
        mc.thePlayer.prevPosY = savedY;
        mc.thePlayer.prevPosZ = savedZ;
        mc.thePlayer.lastTickPosX = savedX;
        mc.thePlayer.lastTickPosY = savedY;
        mc.thePlayer.lastTickPosZ = savedZ;
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionY = 0;
        mc.thePlayer.motionZ = 0;

         
        mc.thePlayer.setSneaking(savedSneaking);

         
        cameraEntity.inventory.currentItem = mc.thePlayer.inventory.currentItem;
        cameraEntity.inventory.copyInventory(mc.thePlayer.inventory);

        if (ghostEntity != null) {
            ghostEntity.inventory.copyInventory(mc.thePlayer.inventory);
        }
    }

    private long lastFrameTime = 0;

     


    @SubscribeEvent
    public void onRenderTick(net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || cameraEntity == null)
            return;
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START)
            return;

         
         
         
         
        float yawDelta = mc.thePlayer.rotationYaw - lastPlayerYaw;
        float pitchDelta = mc.thePlayer.rotationPitch - lastPlayerPitch;

         
        cameraYaw += yawDelta;
        cameraPitch += pitchDelta;
        cameraPitch = Math.max(-90, Math.min(90, cameraPitch));

         
        long currentTime = System.nanoTime();
        if (lastFrameTime == 0)
            lastFrameTime = currentTime;
        float dt = (currentTime - lastFrameTime) / 1_000_000_000f;
        lastFrameTime = currentTime;
        if (dt > 0.1f)
            dt = 0.1f;

        float baseSpeed = speed.getFloatValue() * 10.0f;
        float moveSpeed = baseSpeed * dt;
        if (mc.thePlayer.isSprinting()) {
            moveSpeed *= 1.3f;
        }

        float forward = 0, strafe = 0, vertical = 0;

        if (mc.currentScreen == null) {
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()))
                forward += 1;
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()))
                forward -= 1;
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()))
                strafe += 1;
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()))
                strafe -= 1;
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()))
                vertical += 1;
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()))
                vertical -= 1;
        }

        double moveX = 0, moveZ = 0;
        if (forward != 0 || strafe != 0) {
            float yaw = cameraYaw;
            if (forward != 0 && strafe != 0) {
                yaw += (forward > 0 ? -45 : 45) * (strafe > 0 ? 1 : -1);
                strafe = 0;
            }

            double rad = Math.toRadians(yaw + 90);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);

            if (forward != 0) {
                moveX = forward * moveSpeed * cos;
                moveZ = forward * moveSpeed * sin;
            }
            if (strafe != 0) {
                moveX = strafe * moveSpeed * sin;
                moveZ = -strafe * moveSpeed * cos;
            }
        }

         
        cameraEntity.setPosition(
                cameraEntity.posX + moveX,
                cameraEntity.posY + vertical * moveSpeed,
                cameraEntity.posZ + moveZ);

         
        cameraEntity.rotationYaw = cameraYaw;
        cameraEntity.rotationPitch = cameraPitch;
        cameraEntity.prevRotationYaw = cameraYaw;
        cameraEntity.prevRotationPitch = cameraPitch;
        cameraEntity.rotationYawHead = cameraYaw;
        cameraEntity.prevRotationYawHead = cameraYaw;
         
        cameraEntity.renderYawOffset = cameraYaw;
        cameraEntity.prevRenderYawOffset = cameraYaw;

         
        cameraEntity.prevPosX = cameraEntity.posX;
        cameraEntity.prevPosY = cameraEntity.posY;
        cameraEntity.prevPosZ = cameraEntity.posZ;
        cameraEntity.lastTickPosX = cameraEntity.posX;
        cameraEntity.lastTickPosY = cameraEntity.posY;
        cameraEntity.lastTickPosZ = cameraEntity.posZ;

         
        cameraEntity.setSprinting(mc.thePlayer.isSprinting());
        cameraEntity.capabilities.isFlying = mc.thePlayer.capabilities.isFlying;
        try {
            double playerSpeed = mc.thePlayer
                    .getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed).getAttributeValue();
            cameraEntity.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed)
                    .setBaseValue(playerSpeed);
        } catch (Exception ignored) {
        }

         
         
         
        if (allowInteractions.isEnabled()) {
            mc.thePlayer.rotationYaw = cameraYaw;
            mc.thePlayer.rotationPitch = cameraPitch;
            mc.thePlayer.rotationYawHead = cameraYaw;
            mc.thePlayer.prevRotationYaw = cameraYaw;
            mc.thePlayer.prevRotationPitch = cameraPitch;
             
             
            lastPlayerYaw = cameraYaw;
            lastPlayerPitch = cameraPitch;
        } else {
             
            mc.thePlayer.rotationYaw = savedYaw;
            mc.thePlayer.rotationPitch = savedPitch;
            mc.thePlayer.prevRotationYaw = savedYaw;
            mc.thePlayer.prevRotationPitch = savedPitch;
            lastPlayerYaw = savedYaw;
            lastPlayerPitch = savedPitch;
        }

         
         
         
         
         
        mc.thePlayer.renderArmYaw = cameraYaw;
        mc.thePlayer.prevRenderArmYaw = cameraYaw;
        mc.thePlayer.renderArmPitch = cameraPitch;
        mc.thePlayer.prevRenderArmPitch = cameraPitch;
    }

     
    public double getSavedX() {
        return savedX;
    }

    public double getSavedY() {
        return savedY;
    }
    public double getSavedZ() {
        return savedZ;
    }

    public float getSavedYaw() {
        return savedYaw;
    }

    public float getSavedPitch() {
        return savedPitch;
    }

    public boolean isAllowInteractions() {
        return allowInteractions.isEnabled();
    }
}

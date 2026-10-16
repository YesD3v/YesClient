package com.yesclient.mixin;

import com.yesclient.module.modules.combat.ReachModule;
import com.yesclient.YesClient;
import com.yesclient.module.modules.exploit.ProjectileAimbotModule;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    @Unique private EntityPlayer yesclient$aimPlayer;
    @Unique private float yesclient$restoreYaw;
    @Unique private float yesclient$restorePitch;

    @Inject(method = "func_78764_a", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(EntityPlayer playerIn, Entity targetEntity, CallbackInfo ci) {
        if (com.yesclient.YesClient.getInstance() != null
                && com.yesclient.YesClient.getInstance().getModuleManager() != null) {
            ReachModule reach = (ReachModule) com.yesclient.YesClient.getInstance().getModuleManager()
                    .getModule(ReachModule.class);
            if (reach != null && reach.isEnabled()) {
                if (!reach.isInAttackReach(targetEntity)) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "func_78769_a", at = @At("HEAD"))
    private void yesclient$aimLocalProjectileUse(EntityPlayer player, net.minecraft.world.World world,
            net.minecraft.item.ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        yesclient$beginSilentAim(player);
    }

    @Inject(method = "func_78769_a", at = @At("RETURN"))
    private void yesclient$restoreAfterLocalProjectileUse(EntityPlayer player, net.minecraft.world.World world,
            net.minecraft.item.ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        yesclient$restoreAimRotation();
    }

    @Inject(method = "func_78766_c", at = @At("HEAD"))
    private void yesclient$aimLocalBowRelease(EntityPlayer player, CallbackInfo ci) {
        yesclient$beginSilentAim(player);
    }

    @Inject(method = "func_78766_c", at = @At("RETURN"))
    private void yesclient$restoreAfterLocalBowRelease(EntityPlayer player, CallbackInfo ci) {
        yesclient$restoreAimRotation();
    }

    @Unique
    private void yesclient$beginSilentAim(EntityPlayer player) {
        if (player == null || player != net.minecraft.client.Minecraft.getMinecraft().thePlayer) return;
        if (YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null) return;
        ProjectileAimbotModule aimbot = YesClient.getInstance().getModuleManager()
                .getModule(ProjectileAimbotModule.class);
        if (aimbot == null || !aimbot.shouldApplySilentAim()) return;

        yesclient$aimPlayer = player;
        yesclient$restoreYaw = player.rotationYaw;
        yesclient$restorePitch = player.rotationPitch;
        player.rotationYaw = aimbot.getTargetYaw();
        player.rotationPitch = aimbot.getTargetPitch();
    }

    @Unique
    private void yesclient$restoreAimRotation() {
        if (yesclient$aimPlayer == null) return;
        yesclient$aimPlayer.rotationYaw = yesclient$restoreYaw;
        yesclient$aimPlayer.rotationPitch = yesclient$restorePitch;
        yesclient$aimPlayer = null;
    }
}

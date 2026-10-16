package com.yesclient.mixin;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.util.List;

import com.yesclient.YesClient;
import com.yesclient.module.modules.qol.AntiVoidLagModule;
import net.minecraft.util.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CopyOnWriteArrayList;

 






@Mixin(World.class)
public abstract class MixinWorld {

    private boolean yesclient_listsSwapped = false;

     







    @Inject(method = "func_72939_s", at = @At("HEAD"))
    private void onUpdateEntities(CallbackInfo ci) {
        if (!yesclient_listsSwapped) {
            yesclient_listsSwapped = true;
            try {
                World world = (World) (Object) this;
                 
                swapListField(world, "field_72996_f", "loadedEntityList");
                 
                swapListField(world, "field_73010_i", "playerEntities");
            } catch (Exception ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    private void swapListField(World world, String srgName, String deobfName) {
        Field field = null;
         
        try {
            field = World.class.getDeclaredField(srgName);
        } catch (NoSuchFieldException ignored) {}
         
        if (field == null) {
            try {
                field = World.class.getDeclaredField(deobfName);
            } catch (NoSuchFieldException ignored) {}
        }
        if (field == null) return;

        try {
            field.setAccessible(true);
            Object currentList = field.get(world);
            if (currentList instanceof List && !(currentList instanceof CopyOnWriteArrayList)) {
                 
                try {
                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                } catch (Exception ignored) {}
                field.set(world, new CopyOnWriteArrayList<>((List<?>) currentList));
            }
        } catch (Exception ignored) {}
    }

     





    @Inject(method = "func_180500_c", at = @At("HEAD"), cancellable = true)
    private void onCheckLightFor(EnumSkyBlock lightType, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (lightType != EnumSkyBlock.SKY) return;
        if (YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null) return;
        AntiVoidLagModule antiVoidLag = YesClient.getInstance().getModuleManager().getModule(AntiVoidLagModule.class);
        if (antiVoidLag == null || !antiVoidLag.isEnabled()) return;
        if (pos.getY() < 5) {
            cir.setReturnValue(false);
            return;
        }
        if (antiVoidLag.shouldSkipLightUpdate()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "func_180501_a", at = @At("HEAD"))
    private void onSetBlockState(BlockPos pos, net.minecraft.block.state.IBlockState state, int flags, CallbackInfoReturnable<Boolean> cir) {
        if (YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null) return;
        com.yesclient.module.modules.moderation.PatternDetectionModule pdm = YesClient.getInstance().getModuleManager().getModule(com.yesclient.module.modules.moderation.PatternDetectionModule.class);
        if (pdm != null && pdm.isEnabled()) {
            pdm.onBlockChange(pos, state);
        }
    }
}

package com.yesclient.mixin;

import com.yesclient.YesClient;
import com.yesclient.module.modules.exploit.WearableItemsModule;
import com.yesclient.module.modules.visuals.RainbowArmorModule;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.inventory.ContainerPlayer$1")
public class MixinSlotArmor {

    @Inject(method = { "isItemValid", "func_75214_a" }, at = @At("HEAD"), cancellable = true)
    private void onIsItemValid(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        WearableItemsModule wearable = YesClient.getInstance().getModuleManager()
                .getModule(WearableItemsModule.class);
        if (wearable != null && wearable.isEnabled()) {
            cir.setReturnValue(true);
            return;
        }

        RainbowArmorModule yesArmor = YesClient.getInstance().getModuleManager().getModule(RainbowArmorModule.class);
        if (yesArmor != null && yesArmor.allowsHeadWearable(stack)) {
            cir.setReturnValue(true);
        }
    }
}
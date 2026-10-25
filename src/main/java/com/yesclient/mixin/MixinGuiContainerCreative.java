package com.yesclient.mixin;

import com.yesclient.YesClient;
import com.yesclient.module.modules.visuals.ItemDisguiserModule;
import com.yesclient.storage.ItemVaultStorage;
import com.yesclient.storage.ItemVaultTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;





@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreative {

    @Shadow(remap = false)
    private static InventoryBasic field_147060_v; // the creative item-list buffer

    /** setCurrentCreativeTab (private, so shadowed by its runtime name; this project has no refmap). */
    @Shadow(remap = false)
    private void func_147050_b(CreativeTabs tab) {
    }

    private int selectedTabIndex() {
        return ((GuiContainerCreative) (Object) this).getSelectedTabIndex();
    }

    /** Rebuilds the vault tab's item list after a store/remove. */
    private void refreshVaultTab() {
        func_147050_b(ItemVaultTab.INSTANCE);
    }

    /**
     * Item Vault tab: Ctrl+Click a stored item removes it; clicking with an item on the cursor stores a copy.
     * Any other click falls through to vanilla, which copies the stored item onto the cursor (so it multiplies).
     */
    @Inject(method = "func_146984_a", at = @At("HEAD"), cancellable = true)
    private void yesclient$itemVaultClick(Slot slot, int slotId, int clickButton, int clickType, CallbackInfo ci) {
        if (ItemVaultTab.INSTANCE == null
                || CreativeTabs.creativeTabArray[selectedTabIndex()] != ItemVaultTab.INSTANCE) {
            return;
        }
        if (slot == null || slot.inventory != field_147060_v) {
            return; // only the vault's item grid, not the player's own inventory slots
        }
        if (YesClient.getInstance() == null || YesClient.getInstance().getItemVaultStorage() == null) {
            return;
        }
        ItemVaultStorage storage = YesClient.getInstance().getItemVaultStorage();
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            return;
        }
        ItemStack cursor = player.inventory.getItemStack();

        if (GuiScreen.isCtrlKeyDown() && slot.getHasStack()) {
            storage.removeMatching(slot.getStack());
            refreshVaultTab();
            ci.cancel();
            return;
        }
        if (cursor != null) {
            storage.add(cursor);
            refreshVaultTab();
            ci.cancel();
        }
    }

    @ModifyVariable(method = "func_146285_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack yesclient$replaceCreativeSearchTooltipStack(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }
}

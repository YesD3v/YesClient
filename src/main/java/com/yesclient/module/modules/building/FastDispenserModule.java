package com.yesclient.module.modules.building;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.NumberSetting;
import com.yesclient.utils.ChatUtils;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

 










public class FastDispenserModule extends Module {

    private final NumberSetting placeDelay = new NumberSetting("Place Delay",
            "Ticks to wait before placing (0=instant)", 0,
            0, 5);

    private int lastDispenserSlot = -1;
    private int ticksSinceFound = 0;
    private boolean readyToPlace = false;
    private int placementCooldown = 0;  

    public FastDispenserModule() {
        super("Fast Dispenser", "Auto-place dispensers instantly when detected", Category.EXPLOIT, ModuleMode.BOTH);
        addSetting(placeDelay);
    }

    @Override
    protected void onEnable() {
        lastDispenserSlot = -1;
        ticksSinceFound = 0;
        readyToPlace = false;
        placementCooldown = 0;
        ChatUtils.sendClientMessage("\u00A7aFast Dispenser enabled - get a dispenser to auto-place!");
    }

    @Override
    protected void onDisable() {
        lastDispenserSlot = -1;
        readyToPlace = false;
        placementCooldown = 0;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (!isEnabled())
            return;
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
         

         
        if (placementCooldown > 0) {
            placementCooldown--;
            return;
        }

         
        int dispenserSlot = findDispenserInHotbar();

        if (dispenserSlot != -1) {
             
            if (lastDispenserSlot == -1) {
                 
                lastDispenserSlot = dispenserSlot;
                ticksSinceFound = 0;
                readyToPlace = true;
            }

            if (readyToPlace) {
                 
                if (ticksSinceFound >= placeDelay.getIntValue()) {
                    placeDispenser();
                    readyToPlace = false;
                    lastDispenserSlot = -1;
                    ticksSinceFound = 0;
                    placementCooldown = 20;  
                } else {
                    ticksSinceFound++;
                }
            }
        } else {
             
            lastDispenserSlot = -1;
            readyToPlace = false;
            ticksSinceFound = 0;
        }
    }

    private void placeDispenser() {
        if (lastDispenserSlot == -1)
            return;

        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(lastDispenserSlot);
        if (stack == null || !isDispenser(stack)) {
            ChatUtils.sendClientMessage("\u00A7cDispenser was removed before placement!");
            return;
        }

        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            ChatUtils.sendClientMessage("\u00A7cNot looking at a block!");
            return;
        }

        BlockPos targetPos = mop.getBlockPos();
        EnumFacing face = mop.sideHit;

        BlockPos placePos = targetPos.offset(face);
        Block blockAtPlace = mc.theWorld.getBlockState(placePos).getBlock();
        if (blockAtPlace != Blocks.air && !blockAtPlace.isReplaceable(mc.theWorld, placePos)) {
            ChatUtils.sendClientMessage("\u00A7cCannot place there!");
            return;
        }

         
        int originalSlot = mc.thePlayer.inventory.currentItem;
        if (originalSlot != lastDispenserSlot) {
            mc.thePlayer.inventory.currentItem = lastDispenserSlot;
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(lastDispenserSlot));
        }

         
        Vec3 hitVec = new Vec3(
                targetPos.getX() + 0.5 + face.getFrontOffsetX() * 0.5,
                targetPos.getY() + 0.5 + face.getFrontOffsetY() * 0.5,
                targetPos.getZ() + 0.5 + face.getFrontOffsetZ() * 0.5);

        mc.playerController.onPlayerRightClick(
                mc.thePlayer,
                mc.theWorld,
                stack,
                targetPos,
                face,
                hitVec);

        mc.thePlayer.swingItem();
        ChatUtils.sendClientMessage("\u00A7aDispenser placed!");
    }

    private int findDispenserInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (isDispenser(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isDispenser(ItemStack stack) {
        if (stack == null)
            return false;
        if (!(stack.getItem() instanceof ItemBlock))
            return false;
        return ((ItemBlock) stack.getItem()).getBlock() == Blocks.dispenser;
    }

    @Override
    public String getDisplayInfo() {
        if (readyToPlace)
            return "Placing...";
        return findDispenserInHotbar() != -1 ? "Found" : "Waiting";
    }
}

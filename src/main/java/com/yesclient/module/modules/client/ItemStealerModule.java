package com.yesclient.module.modules.client;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.utils.ChatUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

 




public class ItemStealerModule extends Module {

    private final BooleanSetting stealHeldItem = new BooleanSetting("Steal Held Item", "Copy the item in player's hand",
            true);
    private final BooleanSetting stealHelmet = new BooleanSetting("Steal Helmet", "Copy the player's helmet", true);
    private final BooleanSetting stealChestplate = new BooleanSetting("Steal Chestplate",
            "Copy the player's chestplate", true);
    private final BooleanSetting stealLeggings = new BooleanSetting("Steal Leggings", "Copy the player's leggings",
            true);
    private final BooleanSetting stealBoots = new BooleanSetting("Steal Boots", "Copy the player's boots", true);
    private final BooleanSetting stealFromItemFrames = new BooleanSetting("Steal from Item Frames",
            "Copy items displayed in item frames", true);
    private final BooleanSetting stealFromArmorStands = new BooleanSetting("Steal from Armor Stands",
            "Copy items from armor stands/NPCs", true);

    public ItemStealerModule() {
        super("Item Stealer", "Copy items from players, armor stands, and item frames (keybind only)", Category.EXPLOIT,
                ModuleMode.BOTH);

        addSetting(stealHeldItem);
        addSetting(stealHelmet);
        addSetting(stealChestplate);
        addSetting(stealLeggings);
        addSetting(stealBoots);
        addSetting(stealFromItemFrames);
        addSetting(stealFromArmorStands);
    }

    @Override
    public void toggle() {
         
        if (getKeybind() == 0) {
            ChatUtils.sendClientMessage("\u00A7cItem Stealer requires a keybind to work! Set one in the GUI.");
            return;
        }
         
        stealFromLookedPlayer();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled && getKeybind() == 0) {
            ChatUtils.sendClientMessage("\u00A7cItem Stealer requires a keybind to work! Set one in the GUI.");
            return;
        }
        if (enabled) {
            stealFromLookedPlayer();
        }
         
    }

    @Override
    public boolean isEnabled() {
        return false;  
    }

    private void stealFromLookedPlayer() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        if (!mc.thePlayer.capabilities.isCreativeMode) {
            ChatUtils.sendClientMessage("\u00A7cItem Stealer only works in Creative Mode!");
            return;
        }

         
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.entityHit == null) {
            ChatUtils.sendClientMessage("\u00A7cNot looking at an entity!");
            return;
        }

        Entity entity = mop.entityHit;

         
        if (entity instanceof EntityItemFrame) {
            if (!stealFromItemFrames.isEnabled()) {
                ChatUtils.sendClientMessage("\u00A7cItem Frame stealing is disabled in settings!");
                return;
            }
            EntityItemFrame frame = (EntityItemFrame) entity;
            ItemStack displayedItem = frame.getDisplayedItem();
            if (displayedItem != null) {
                giveItem(displayedItem, "Item Frame");
                ChatUtils.sendClientMessage("\u00A7aStole item from Item Frame!");
            } else {
                ChatUtils.sendClientMessage("\u00A7eItem Frame is empty!");
            }
            return;
        }

         
        if (entity instanceof EntityArmorStand) {
            if (!stealFromArmorStands.isEnabled()) {
                ChatUtils.sendClientMessage("\u00A7cArmor Stand stealing is disabled in settings!");
                return;
            }
            EntityArmorStand stand = (EntityArmorStand) entity;
            int stolenCount = stealFromLivingEntity(stand, "Armor Stand");
            if (stolenCount > 0) {
                ChatUtils.sendClientMessage("\u00A7aStole " + stolenCount + " item(s) from Armor Stand/NPC!");
            } else {
                ChatUtils.sendClientMessage("\u00A7eNo items to steal from this Armor Stand/NPC");
            }
            return;
        }

         
        if (entity instanceof EntityPlayer) {
            EntityPlayer target = (EntityPlayer) entity;
            int stolenCount = stealFromLivingEntity(target, target.getName());
            if (stolenCount > 0) {
                ChatUtils.sendClientMessage("\u00A7aStole " + stolenCount + " item(s) from " + target.getName() + "!");
            } else {
                ChatUtils.sendClientMessage("\u00A7eNo items to steal from " + target.getName());
            }
            return;
        }

         
        if (entity instanceof EntityLivingBase) {
            if (!stealFromArmorStands.isEnabled()) {
                ChatUtils.sendClientMessage("\u00A7cNPC/Entity stealing is disabled in settings!");
                return;
            }
            EntityLivingBase living = (EntityLivingBase) entity;
            int stolenCount = stealFromLivingEntity(living, entity.getName());
            if (stolenCount > 0) {
                ChatUtils.sendClientMessage("\u00A7aStole " + stolenCount + " item(s) from " + entity.getName() + "!");
            } else {
                ChatUtils.sendClientMessage("\u00A7eNo items to steal from " + entity.getName());
            }
            return;
        }

        ChatUtils.sendClientMessage("\u00A7cCannot steal from this entity type!");
    }

    private int stealFromLivingEntity(EntityLivingBase target, String sourceName) {
        int stolenCount = 0;

         
        if (stealHeldItem.isEnabled()) {
            ItemStack heldItem = target.getHeldItem();
            if (heldItem != null) {
                giveItem(heldItem, "Held Item");
                stolenCount++;
            }
        }

         
        if (stealHelmet.isEnabled()) {
            ItemStack helmet = target.getCurrentArmor(3);
            if (helmet != null) {
                giveItem(helmet, "Helmet");
                stolenCount++;
            }
        }

         
        if (stealChestplate.isEnabled()) {
            ItemStack chestplate = target.getCurrentArmor(2);
            if (chestplate != null) {
                giveItem(chestplate, "Chestplate");
                stolenCount++;
            }
        }

         
        if (stealLeggings.isEnabled()) {
            ItemStack leggings = target.getCurrentArmor(1);
            if (leggings != null) {
                giveItem(leggings, "Leggings");
                stolenCount++;
            }
        }

         
        if (stealBoots.isEnabled()) {
            ItemStack boots = target.getCurrentArmor(0);
            if (boots != null) {
                giveItem(boots, "Boots");
                stolenCount++;
            }
        }

        if (stolenCount > 0) {
             
            System.out.println("[ItemStealer] Stole " + stolenCount + " items from " + sourceName);
        }

        return stolenCount;
    }

    private void giveItem(ItemStack item, String type) {
         
        com.yesclient.storage.ItemStealerStorage storage = com.yesclient.YesClient.getInstance()
                .getItemStealerStorage();

        if (storage != null) {
            storage.addItem(item.copy());
        }

         
        com.yesclient.itemlog.ItemLogManager logManager = com.yesclient.itemlog.ItemLogManager.getInstance();
        if (logManager != null) {
            logManager.addItem(item.copy());
        }

         
        if (mc.thePlayer.capabilities.isCreativeMode) {
            ItemStack clonedItem = item.copy();
             
            boolean added = mc.thePlayer.inventory.addItemStackToInventory(clonedItem);
            if (added) {
                 
                for (int i = 0; i < mc.thePlayer.inventory.mainInventory.length; i++) {
                    ItemStack slotStack = mc.thePlayer.inventory.mainInventory[i];
                    if (slotStack == clonedItem) {
                         
                         
                         
                        int networkSlot = i < 9 ? 36 + i : i;
                        mc.thePlayer.sendQueue.addToSendQueue(
                                new net.minecraft.network.play.client.C10PacketCreativeInventoryAction(
                                        networkSlot, clonedItem));
                        break;
                    }
                }
            }
        }
    }
}

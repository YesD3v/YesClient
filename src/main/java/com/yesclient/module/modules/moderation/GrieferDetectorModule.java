package com.yesclient.module.modules.moderation;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;
import java.util.UUID;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.NumberSetting;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Set;

public class GrieferDetectorModule extends Module {

    private final BooleanSetting blockDestruction = new BooleanSetting("Block Destruction",
            "Detect rapid block breaking", true);
    private final NumberSetting threshold = new NumberSetting("Threshold", "Blocks per second to trigger alert", 10, 1,
            50, 1);

     
    private final BooleanSetting bonemealDetector = new BooleanSetting("Bonemeal Detector",
            "Detects when anyone holds bonemeal", true);
    private final BooleanSetting dispenserDetector = new BooleanSetting("Dispenser Detector",
            "Detects when anyone holds a dispenser", true);
    private final BooleanSetting stickyPistonDetector = new BooleanSetting("Sticky Piston Detector",
            "Detects when anyone holds a sticky piston", true);
    private final BooleanSetting pistonDetector = new BooleanSetting("Piston Detector",
            "Detects when anyone holds a piston", true);
    private final BooleanSetting slimeDetector = new BooleanSetting("Slime Detector", "Detects when anyone holds slime",
            true);
    private final BooleanSetting flintAndSteelDetector = new BooleanSetting("Flint and Steel",
            "Detects when anyone holds flint and steel", true);
    private final BooleanSetting fireChargeDetector = new BooleanSetting("Fire Charge",
            "Detects when anyone holds a fire charge", true);
    private final BooleanSetting saplingsDetector = new BooleanSetting("Saplings", "Detects when anyone holds saplings",
            true);
    private final BooleanSetting jukeboxDetector = new BooleanSetting("Jukebox", "Detects when anyone holds a jukebox",
            true);
    private final BooleanSetting noteBlockDetector = new BooleanSetting("Note Block",
            "Detects when anyone holds a note block", true);
    private final BooleanSetting musicDiscDetector = new BooleanSetting("Music Disc",
            "Detects when anyone holds a music disc", true);
    private final BooleanSetting splashPotionsDetector = new BooleanSetting("Splash Potions",
            "Detects when anyone holds splash potions", true);
    private final BooleanSetting boatDetector = new BooleanSetting("Boat", "Detects when anyone holds a boat", true);
    private final BooleanSetting spawnEggsDetector = new BooleanSetting("Spawn Eggs",
            "Detects when anyone holds spawn eggs", true);
    private final BooleanSetting fireworksRocketDetector = new BooleanSetting("Fireworks Rocket",
            "Detects when anyone holds a fireworks rocket", true);
    private final BooleanSetting waterBucketDetector = new BooleanSetting("Water Bucket",
            "Detects when anyone holds a water bucket", true);
    private final BooleanSetting lavaBucketDetector = new BooleanSetting("Lava Bucket",
            "Detects when anyone holds a lava bucket", true);
    private final BooleanSetting barrierDetector = new BooleanSetting("Barrier",
            "Detects when anyone holds a barrier block", true);

    private final Map<UUID, Integer> breakCounts = new HashMap<>();

     
     
     
    private final Map<UUID, Set<String>> alertedItems = new HashMap<>();
    private final Map<UUID, Map<String, ItemAlertState>> itemAlertStates = new HashMap<>();
    private int nextAlertChatLineId = Integer.MIN_VALUE;

    private int ticks = 0;

    public GrieferDetectorModule() {
        super("Griefer Detector", "Alerts when a player flags griefer checks", Category.MODERATION,
                com.yesclient.module.ModuleMode.BOTH);
        addSetting(blockDestruction);
        addSetting(threshold);
        addSetting(bonemealDetector);
        addSetting(dispenserDetector);
        addSetting(stickyPistonDetector);
        addSetting(pistonDetector);
        addSetting(slimeDetector);
        addSetting(flintAndSteelDetector);
        addSetting(fireChargeDetector);
        addSetting(saplingsDetector);
        addSetting(jukeboxDetector);
        addSetting(noteBlockDetector);
        addSetting(musicDiscDetector);
        addSetting(splashPotionsDetector);
        addSetting(boatDetector);
        addSetting(spawnEggsDetector);
        addSetting(fireworksRocketDetector);
        addSetting(waterBucketDetector);
        addSetting(lavaBucketDetector);
        addSetting(barrierDetector);

         
        threshold.setVisibility(() -> blockDestruction.isEnabled());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        breakCounts.clear();
        alertedItems.clear();
        itemAlertStates.clear();
        nextAlertChatLineId = Integer.MIN_VALUE;
        ticks = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null)
            return;

        ticks++;

         
        if (ticks % 5 == 0) {
            checkPlayerItems();
        }

         
        if (ticks >= 20) {
            if (blockDestruction.isEnabled()) {
                checkThresholds();
            }
            breakCounts.clear();
            ticks = 0;
        }
    }

    private void checkPlayerItems() {
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer)
                continue;  

            ItemStack heldItem = player.getHeldItem();
            UUID uuid = player.getUniqueID();

             
            Set<String> currentlyHolding = new HashSet<>();
            Set<String> alerted = alertedItems.computeIfAbsent(uuid, k -> new HashSet<>());

            if (heldItem != null) {
                Item item = heldItem.getItem();
                String itemId = Item.itemRegistry.getNameForObject(item).toString();  
                int meta = heldItem.getMetadata();
                String displayName = heldItem.getDisplayName();

                String flagName = null;
                boolean isSevere = false;

                 
                if (bonemealDetector.isEnabled() && itemId.equals("minecraft:dye") && meta == 15) {
                    flagName = "Bonemeal";
                }
                 
                else if (dispenserDetector.isEnabled() && itemId.equals("minecraft:dispenser")) {
                    flagName = "Dispenser";
                }
                 
                else if (stickyPistonDetector.isEnabled() && itemId.equals("minecraft:sticky_piston")) {
                    flagName = "Sticky Piston";
                }
                 
                else if (pistonDetector.isEnabled() && itemId.equals("minecraft:piston")) {
                    flagName = "Piston";
                }
                 
                else if (slimeDetector.isEnabled()
                        && (itemId.equals("minecraft:slime") || itemId.equals("minecraft:slime_ball"))) {
                    flagName = "Slime";
                }
                 
                else if (flintAndSteelDetector.isEnabled() && itemId.equals("minecraft:flint_and_steel")) {
                    flagName = "Flint and Steel";
                }
                 
                else if (fireChargeDetector.isEnabled() && itemId.equals("minecraft:fire_charge")) {
                    flagName = "Fire Charge";
                }
                 
                else if (saplingsDetector.isEnabled() && itemId.equals("minecraft:sapling")) {
                    flagName = displayName;  
                }
                 
                else if (jukeboxDetector.isEnabled() && itemId.equals("minecraft:jukebox")) {
                    flagName = "Jukebox";
                }
                 
                else if (noteBlockDetector.isEnabled() && itemId.equals("minecraft:noteblock")) {
                    flagName = "Note Block";
                }
                 
                else if (musicDiscDetector.isEnabled() && itemId.startsWith("minecraft:record_")) {
                    flagName = displayName;  
                }
                 
                else if (splashPotionsDetector.isEnabled() && itemId.equals("minecraft:potion")
                        && net.minecraft.item.ItemPotion.isSplash(meta)) {
                    flagName = "Splash " + displayName;  
                }
                 
                else if (boatDetector.isEnabled() && itemId.equals("minecraft:boat")) {
                    flagName = "Boat";
                }
                 
                else if (spawnEggsDetector.isEnabled() && itemId.equals("minecraft:spawn_egg")) {
                    flagName = displayName;  
                    if (meta == 56) {  
                        isSevere = true;
                    }
                }
                 
                else if (fireworksRocketDetector.isEnabled() && itemId.equals("minecraft:fireworks")) {
                    flagName = "Fireworks Rocket";
                }
                 
                else if (waterBucketDetector.isEnabled() && itemId.equals("minecraft:water_bucket")) {
                    flagName = "Water Bucket";
                }
                 
                else if (lavaBucketDetector.isEnabled() && itemId.equals("minecraft:lava_bucket")) {
                    flagName = "Lava Bucket";
                }
                 
                else if (barrierDetector.isEnabled() && itemId.equals("minecraft:barrier")) {
                    flagName = "Barrier";
                }

                if (flagName != null) {
                    currentlyHolding.add(flagName);
                     
                    if (!alerted.contains(flagName)) {
                        alertItem(player, flagName, isSevere);
                        alerted.add(flagName);
                    }
                }
            }

             
             
            alerted.retainAll(currentlyHolding);
        }
    }

    private void alertItem(EntityPlayer player, String type, boolean severe) {
        updateAlert(player.getUniqueID(), player.getName(), type, severe, 1);
    }

    private void updateAlert(UUID uuid, String name, String type, boolean severe, int amount) {
        Map<String, ItemAlertState> states = itemAlertStates.computeIfAbsent(uuid, key -> new HashMap<String, ItemAlertState>());
        ItemAlertState state = states.get(type);
        if (state == null) {
            state = new ItemAlertState(nextAlertChatLineId++);
            states.put(type, state);
        }
        state.count += amount;
        state.severe = severe;

        String color = state.severe ? "\u00A7c" : "\u00A7f";
        net.minecraft.util.ChatComponentText message = new net.minecraft.util.ChatComponentText(
                "\u00A7c[Griefer Detector] \u00A7f" + name + " is flagging ["
                        + color + type + "\u00A7f] \u00A77(x" + state.count + ")");
        mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(message, state.chatLineId);
    }

    private static class ItemAlertState {
        private final int chatLineId;
        private int count;
        private boolean severe;

        private ItemAlertState(int chatLineId) {
            this.chatLineId = chatLineId;
        }
    }

    public void onPacketReceive(net.minecraft.network.Packet<?> packet) {
        if (!blockDestruction.isEnabled())
            return;

        if (packet instanceof S23PacketBlockChange) {
            S23PacketBlockChange p = (S23PacketBlockChange) packet;
             
            try {
                Field f = S23PacketBlockChange.class.getDeclaredField("field_148883_d");
                f.setAccessible(true);
                IBlockState state = (IBlockState) f.get(p);
                if (state.getBlock() == net.minecraft.init.Blocks.air) {
                    processBlockBreak(p.getBlockPosition());
                }
            } catch (Exception e) {
                 
                try {
                    Field f = S23PacketBlockChange.class.getDeclaredField("blockState");
                    f.setAccessible(true);
                    IBlockState state = (IBlockState) f.get(p);
                    if (state.getBlock() == net.minecraft.init.Blocks.air) {
                        processBlockBreak(p.getBlockPosition());
                    }
                } catch (Exception e2) {
                     
                }
            }
        } else if (packet instanceof S22PacketMultiBlockChange) {
            S22PacketMultiBlockChange p = (S22PacketMultiBlockChange) packet;
            for (S22PacketMultiBlockChange.BlockUpdateData data : p.getChangedBlocks()) {
                if (data.getBlockState().getBlock() == net.minecraft.init.Blocks.air) {
                    processBlockBreak(data.getPos());
                }
            }
        }
    }

    private void processBlockBreak(BlockPos pos) {
        double range = 6.0;
        EntityPlayer suspect = null;
        double closestDistSq = range * range;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityPlayer && entity != mc.thePlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                double distSq = player.getDistanceSq(pos);

                if (distSq <= range * range) {
                    if (isLookingAt(player, pos)) {
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            suspect = player;
                        }
                    }
                }
            }
        }

        if (suspect != null) {
            UUID id = suspect.getUniqueID();
            breakCounts.put(id, breakCounts.getOrDefault(id, 0) + 1);
        }
    }

    private boolean isLookingAt(EntityPlayer player, BlockPos target) {
        Vec3 eyePos = player.getPositionEyes(1.0f);
        Vec3 lookVec = player.getLook(1.0f);

        Vec3 targetVec = new Vec3(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        Vec3 diff = targetVec.subtract(eyePos);
        diff = diff.normalize();

        double dot = lookVec.dotProduct(diff);
        return dot > 0.95;
    }

    private void checkThresholds() {
        int limit = threshold.getIntValue();
        for (Map.Entry<UUID, Integer> entry : breakCounts.entrySet()) {
            if (entry.getValue() > limit) {
                EntityPlayer player = mc.theWorld.getPlayerEntityByUUID(entry.getKey());
                String name = (player != null) ? player.getName() : "Unknown";
                updateAlert(entry.getKey(), name, "Block Destruction", false, entry.getValue());
            }
        }
    }
}

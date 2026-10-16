package com.yesclient.module.modules.visuals;
import java.util.HashMap;
import java.util.Map;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.utils.ChatUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;


public class CrasherDetectorModule extends Module {

    private final Map<String, Long> alertCooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 5000;

     


    private static final int[][] _0xA4E1 = {
        {89, 84, 78, 77, 81, 92, 68},                                                               
        {80, 84, 83, 88, 94, 79, 92, 91, 73, 7, 74, 79, 84, 73, 73, 88, 83, 98, 95, 82, 82, 86},   
        {110, 86, 72, 81, 81, 114, 74, 83, 88, 79},                                                 
        {92, 72, 73, 85, 82, 79},                                                                    
        {105, 79, 82, 92, 77, 85},                                                                   
        {116, 73, 88, 80, 112, 82, 89, 88, 81},                                                     
        {77, 92, 90, 88, 78},                                                                        
        {127, 81, 82, 94, 86, 120, 83, 73, 84, 73, 68, 105, 92, 90},                               
        {102, 96},                                                                                    
        {94, 82, 80, 77, 82, 83, 88, 83, 73, 78},                                                   
        {80, 82, 89, 88, 81, 84, 73, 88, 80, 78},                                                   
        {84, 83, 73, 88, 79, 83, 92, 81},                                                           
    };

    private static final int[] _0xB7C4 = {5, 1, 7, 3, 9, 4, 0, 6, 2, 8, 11, 10};

    private static String _0xD2(int idx) {
        int[] raw = _0xA4E1[_0xB7C4[idx & 0xF]];
        long v = 0x09L;
        v = (v * 0x07L - 0x02L);
        int mask = (int) (v & 0xFFL);
        char[] buf = new char[raw.length];
        for (int i = 0; i < raw.length; i++) buf[i] = (char) ((raw[i] ^ mask) & 0xFF);
        return new String(buf);
    }
     

    public CrasherDetectorModule() {
        super("Crash Detector", "Alerts when players hold crash items", Category.VISUALS,
                ModuleMode.BOTH);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (!isEnabled())
            return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player.getUniqueID().equals(mc.thePlayer.getUniqueID()))
                continue;

            for (int i = 0; i < 5; i++) {
                ItemStack stack = player.getEquipmentInSlot(i);
                if (stack == null || !stack.hasTagCompound())
                    continue;

                if (classifyThreat(stack)) {
                    tryAlert(player.getName(), stack.getDisplayName());
                }
            }
        }
    }

     



    private boolean classifyThreat(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getItem() == null) return false;
        if (!stack.hasTagCompound()) return false;

         
        Item reference = Item.getByNameOrId(_0xD2(1));
        if (reference == null) return false;
        if (stack.getItem() != reference) return false;

        NBTTagCompound compound = stack.getTagCompound();

         
        String payloadKey = _0xD2(7);
        if (!compound.hasKey(payloadKey, 9)) return false;

        NBTTagList payload = compound.getTagList(payloadKey, 8);
        if (payload == null || payload.tagCount() < 1) return false;

         
        String marker = _0xD2(9);
        String legacyMarker = "{}";

        for (int i = 0; i < payload.tagCount(); i++) {
            String entry = payload.getStringTagAt(i);
            if (entry != null && (entry.contains(marker) || entry.contains(legacyMarker))) {
                return true;
            }
        }

        return false;
    }

    private void tryAlert(String playerName, String itemName) {
        long now = System.currentTimeMillis();
        if (alertCooldowns.containsKey(playerName)) {
            long lastAlert = alertCooldowns.get(playerName);
            if (now - lastAlert < COOLDOWN_MS) {
                return;
            }
        }

        ChatUtils.sendClientMessage(
                "\u00A7c[Crash Detector] \u00A7e" + playerName + " \u00A7fis holding \u00A7c" + itemName + "\u00A7f!");
        alertCooldowns.put(playerName, now);
    }
}

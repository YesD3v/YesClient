package com.yesclient.module.modules.moderation;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.utils.ChatUtils;
import com.yesclient.utils.MinecraftFormatting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

 










public class NickDetectorModule extends Module {

     
    private final Map<String, Boolean> nickCache = new ConcurrentHashMap<>();

     
    private final Queue<String> lookupQueue = new ConcurrentLinkedQueue<>();

     
    private final Set<String> queuedNames = Collections.synchronizedSet(new HashSet<>());

     
    private long lastLookupTime = 0;
    private static final long LOOKUP_INTERVAL_MS = 1200;  

     
    private ExecutorService lookupExecutor;

     
    private int tickCounter = 0;

    public NickDetectorModule() {
        super("Nick Detector", "Detects nicked players via API", Category.MODERATION, ModuleMode.BOTH);
    }

    @Override
    protected void onEnable() {
        nickCache.clear();
        lookupQueue.clear();
        queuedNames.clear();
        lastLookupTime = 0;
        tickCounter = 0;
        lookupExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "NickDetector-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    protected void onDisable() {
        nickCache.clear();
        lookupQueue.clear();
        queuedNames.clear();
        if (lookupExecutor != null) {
            lookupExecutor.shutdownNow();
            lookupExecutor = null;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.getNetHandler() == null) return;

        tickCounter++;

         
        if (tickCounter % 40 == 0) {
            scanTabList();
        }

         
        long now = System.currentTimeMillis();
        if (!lookupQueue.isEmpty() && now - lastLookupTime >= LOOKUP_INTERVAL_MS) {
            String name = lookupQueue.poll();
            if (name != null && lookupExecutor != null && !lookupExecutor.isShutdown()) {
                lastLookupTime = now;
                final String nameFinal = name;
                lookupExecutor.submit(() -> {
                    boolean nicked = checkIfNicked(nameFinal);
                    nickCache.put(nameFinal.toLowerCase(), nicked);
                });
            }
        }
    }

     


    @SubscribeEvent(priority = EventPriority.LOW)
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!isEnabled()) return;
        if (event.message == null) return;
         
        if (event.type == 2) return;

        String formattedText = event.message.getFormattedText();
        boolean modified = false;

         
        for (Map.Entry<String, Boolean> entry : nickCache.entrySet()) {
            if (!entry.getValue()) continue;  
            String nickedName = entry.getKey();

            String markedText = MinecraftFormatting.appendAfterIgnoreCase(
                    formattedText, nickedName, "\u00A7c~");
            if (!markedText.equals(formattedText)) {
                formattedText = markedText;
                modified = true;
            }
        }

        if (modified) {
            event.message = new ChatComponentText(formattedText);
        }
    }

     



    private void scanTabList() {
        if (mc.getNetHandler() == null) return;

        Collection<NetworkPlayerInfo> players = mc.getNetHandler().getPlayerInfoMap();
        for (NetworkPlayerInfo info : players) {
            String name = info.getGameProfile().getName();
            if (name == null || name.isEmpty()) continue;

             
            if (name.equals(mc.thePlayer.getName())) continue;

             
            String lower = name.toLowerCase();
            if (nickCache.containsKey(lower) || queuedNames.contains(lower)) continue;

             
            String displayName = "";
            if (info.getDisplayName() != null) {
                displayName = info.getDisplayName().getFormattedText();
            } else {
                displayName = ScorePlayerTeam.formatPlayerName(
                        info.getPlayerTeam(), info.getGameProfile().getName());
            }

             
            if (!displayName.contains("\u00A7")) continue;

             
            if (!shouldCheckPlayer(displayName, name)) continue;

             
            queuedNames.add(lower);
            lookupQueue.add(name);
        }
    }

     













    private boolean shouldCheckPlayer(String displayName, String playerName) {
         
        String stripped = displayName;

         
         
        if (stripped.contains("++")) {
            return false;  
        }

         
        int nameIdx = stripped.indexOf(playerName);
        if (nameIdx < 0) return false;  

         
        char nameColorCode = '7';  
        for (int i = nameIdx - 1; i >= 1; i--) {
            if (stripped.charAt(i - 1) == '\u00A7') {
                char code = Character.toLowerCase(stripped.charAt(i));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    nameColorCode = code;
                    break;
                }
            }
        }

         
        if (nameColorCode == '7') {
            return true;
        }

         
        if (nameColorCode == 'a') {
            return true;
        }

         
        if (nameColorCode == 'b') {
             
             
            String beforeName = stripped.substring(0, nameIdx);

             
            int plusIdx = beforeName.lastIndexOf('+');
            if (plusIdx < 0) {
                 
                return true;
            }

             
             
            for (int i = plusIdx - 1; i >= 1; i--) {
                if (stripped.charAt(i - 1) == '\u00A7') {
                    char plusColor = Character.toLowerCase(stripped.charAt(i));
                    if ((plusColor >= '0' && plusColor <= '9') || (plusColor >= 'a' && plusColor <= 'f')) {
                         
                        if (plusColor == 'c') {
                            return true;
                        }
                         
                        return false;
                    }
                    break;
                }
            }
             
            return true;
        }

         
        return false;
    }

     



    private boolean checkIfNicked(String username) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "YesClient/1.0");

            int code = conn.getResponseCode();

            if (code == 200) {
                 
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && line.contains("\"id\"")) {
                        return false;  
                    }
                }
                return false;
            } else if (code == 204 || code == 404) {
                 
                return true;  
            } else {
                 
                System.out.println("[NickDetector] API returned " + code + " for " + username);
                return false;
            }
        } catch (Exception e) {
            System.out.println("[NickDetector] Error checking " + username + ": " + e.getMessage());
            return false;  
        }
    }

     


    public boolean isNicked(String username) {
        Boolean cached = nickCache.get(username.toLowerCase());
        return cached != null && cached;
    }

    @Override
    public String getDisplayInfo() {
        long nickedCount = nickCache.values().stream().filter(b -> b).count();
        if (nickedCount > 0) {
            return nickedCount + " nicked";
        }
        return null;
    }
}

package com.yesclient.module.modules.moderation;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.utils.ChatUtils;
import com.yesclient.utils.MinecraftFormatting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NickDetectorModule extends Module {

    private final Map<String, Boolean> nickCache = new ConcurrentHashMap<>();
    private World trackedWorld;

    public NickDetectorModule() {
        super("Nick Detector", "Detects nicked players from their server UUID", Category.MODERATION, ModuleMode.BOTH);
    }

    @Override
    protected void onEnable() {
        nickCache.clear();
        trackedWorld = mc.theWorld;
    }

    @Override
    protected void onDisable() {
        nickCache.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !isEnabled()
                || mc.thePlayer == null || mc.theWorld == null || mc.getNetHandler() == null) return;

        if (trackedWorld != mc.theWorld) {
            trackedWorld = mc.theWorld;
            nickCache.clear();
        }

        scanTabList();
    }

    private void scanTabList() {
        Collection<NetworkPlayerInfo> players = mc.getNetHandler().getPlayerInfoMap();
        UUID ownId = mc.thePlayer.getUniqueID();

        for (NetworkPlayerInfo info : players) {
            if (info == null || info.getGameProfile() == null) continue;

            UUID playerId = info.getGameProfile().getId();
            String username = info.getGameProfile().getName();
            if (playerId == null || username == null || playerId.equals(ownId)) continue;

            String cacheKey = username.toLowerCase(Locale.ROOT);
            int uuidVersion = playerId.version();
            if (uuidVersion == 1) {
                Boolean alreadyNicked = nickCache.put(cacheKey, true);
                if (!Boolean.TRUE.equals(alreadyNicked)) {
                    ChatUtils.sendClientMessage("\u00A7c[Nick Detector] \u00A7f" + username + " is nicked!");
                }
            } else {
                nickCache.remove(cacheKey);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!isEnabled() || event.message == null || event.type == 2) return;

        String formattedText = event.message.getFormattedText();
        boolean modified = false;
        for (Map.Entry<String, Boolean> entry : nickCache.entrySet()) {
            if (!entry.getValue()) continue;

            String markedText = MinecraftFormatting.appendAfterIgnoreCase(
                    formattedText, entry.getKey(), "\u00A7c~");
            if (!markedText.equals(formattedText)) {
                formattedText = markedText;
                modified = true;
            }
        }

        if (modified) event.message = new ChatComponentText(formattedText);
    }

    public boolean isNicked(String username) {
        if (username == null) return false;
        Boolean cached = nickCache.get(username.toLowerCase(Locale.ROOT));
        return cached != null && cached;
    }

    @Override
    public String getDisplayInfo() {
        long nickedCount = nickCache.values().stream().filter(Boolean::booleanValue).count();
        return nickedCount > 0 ? nickedCount + " nicked" : null;
    }
}

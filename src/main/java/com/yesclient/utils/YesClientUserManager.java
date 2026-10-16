package com.yesclient.utils;
import java.util.HashSet;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

 



public class YesClientUserManager {

    private static YesClientUserManager instance;

     
    private final Set<UUID> yesClientUsers = ConcurrentHashMap.newKeySet();

     
    private final Set<String> yesClientNames = ConcurrentHashMap.newKeySet();

     
    private final Set<String> rainbowNameCache = ConcurrentHashMap.newKeySet();

    private YesClientUserManager() {
        System.out.println("[YesClient] YesClientUserManager initialized!");
    }

    public static YesClientUserManager getInstance() {
        if (instance == null) {
            instance = new YesClientUserManager();
        }
        return instance;
    }

     


    public boolean isYesClientUser(EntityPlayer player) {
        if (player == null)
            return false;
        return yesClientUsers.contains(player.getUniqueID());
    }

     


    public boolean isYesClientUser(UUID uuid) {
        if (uuid == null)
            return false;
        return yesClientUsers.contains(uuid);
    }

     



    public boolean isYesClientUserByName(String name) {
        if (name == null || name.isEmpty())
            return false;
        return yesClientNames.contains(name.toLowerCase().trim());
    }

     


    public Set<UUID> getYesClientUsers() {
        return new HashSet<>(yesClientUsers);
    }

     


    public void onServerConnect() {
        yesClientUsers.clear();
        yesClientNames.clear();
    }

     


    public boolean isRainbowName(String displayName) {
        if (displayName == null)
            return false;
        return rainbowNameCache.contains(displayName);
    }

     



    public void updateRainbowNameCache() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.getNetHandler() instanceof net.minecraft.client.network.NetHandlerPlayClient))
            return;

        Set<String> newCache = new HashSet<>();

         
        for (net.minecraft.client.network.NetworkPlayerInfo info : ((net.minecraft.client.network.NetHandlerPlayClient) mc
                .getNetHandler()).getPlayerInfoMap()) {
            if (info.getGameProfile() != null && isYesClientUser(info.getGameProfile().getId())) {
                 
                String name = info.getDisplayName() != null ? info.getDisplayName().getFormattedText()
                        : net.minecraft.scoreboard.ScorePlayerTeam.formatPlayerName(info.getPlayerTeam(),
                                info.getGameProfile().getName());

                newCache.add(name);
            }
        }

        rainbowNameCache.clear();
        rainbowNameCache.addAll(newCache);
    }
}

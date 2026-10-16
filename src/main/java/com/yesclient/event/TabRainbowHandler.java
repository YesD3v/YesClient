package com.yesclient.event;
import java.util.Comparator;
import java.util.List;
import java.awt.Color;
import java.util.UUID;

import com.yesclient.utils.YesClientUserManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.ArrayList;
import java.util.Collections;

 




public class TabRainbowHandler {

    public static int yOffset = 0;

    private static final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderPlayerList(RenderGameOverlayEvent.Post event) {
         
        if (event.type != RenderGameOverlayEvent.ElementType.PLAYER_LIST)
            return;
        if (mc.thePlayer == null || mc.getNetHandler() == null)
            return;
        if (!mc.gameSettings.keyBindPlayerList.isKeyDown())
            return;

        renderRainbowNames();
    }

    private void renderRainbowNames() {
        NetHandlerPlayClient netHandler = mc.getNetHandler();
        if (netHandler == null)
            return;

        List<NetworkPlayerInfo> playerList = new ArrayList<>(netHandler.getPlayerInfoMap());
         
        Collections.sort(playerList, new Comparator<NetworkPlayerInfo>() {
            @Override
            public int compare(NetworkPlayerInfo p1, NetworkPlayerInfo p2) {
                ScorePlayerTeam team1 = p1.getPlayerTeam();
                ScorePlayerTeam team2 = p2.getPlayerTeam();
                String name1 = team1 != null ? team1.getRegisteredName() : "";
                String name2 = team2 != null ? team2.getRegisteredName() : "";
                int teamCompare = name1.compareTo(name2);
                if (teamCompare != 0)
                    return teamCompare;
                return p1.getGameProfile().getName().compareToIgnoreCase(p2.getGameProfile().getName());
            }
        });
        playerList = com.yesclient.module.modules.visuals.NickHiderModule.reorderTabList(playerList);

        if (playerList.isEmpty())
            return;

        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRendererObj;
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();

         
        int maxPlayers = playerList.size();
        int columns = 1;
        while (columns < 4 && maxPlayers > columns * 20) {
            columns++;
        }
        int rows = (maxPlayers + columns - 1) / columns;

         
        int maxNameWidth = 0;
        for (int i = 0; i < maxPlayers; i++) {
            NetworkPlayerInfo info = playerList.get(i);
            String name = info.getGameProfile().getName();
            ScorePlayerTeam team = info.getPlayerTeam();
            String displayName = ScorePlayerTeam.formatPlayerName(team, name);
            int strWidth = fr.getStringWidth(displayName);
            if (strWidth > maxNameWidth) {
                maxNameWidth = strWidth;
            }
        }
         
        maxNameWidth = Math.max(maxNameWidth + 13, 80);

         
        IChatComponent header = null;
        IChatComponent footer = null;
        try {
            GuiPlayerTabOverlay tabOverlay = mc.ingameGUI.getTabList();

             
            try {
                 
                header = ReflectionHelper.getPrivateValue(GuiPlayerTabOverlay.class, tabOverlay, "field_175256_i",
                        "field_175256_h", "header", "h");
                footer = ReflectionHelper.getPrivateValue(GuiPlayerTabOverlay.class, tabOverlay, "field_175255_h",
                        "field_175255_i", "footer", "i");
            } catch (Exception ex) {
                System.out.println(
                        "[TabRainbow] CRITICAL: Failed to get header/footer via reflection! " + ex.getMessage());
            }

             
             

        } catch (Exception e) {
            e.printStackTrace();
        }

         
        int totalTableWidth = maxNameWidth * columns + (columns - 1) * 5;
        int startX = width / 2 - totalTableWidth / 2;

        int totalContentHeight = rows * 9;
         
         
         
        if (header != null) {
            List<String> headerLines = fr.listFormattedStringToWidth(header.getFormattedText(), width - 50);
            totalContentHeight += headerLines.size() * 9 + 20;
        }
        if (footer != null) {
            List<String> footerLines = fr.listFormattedStringToWidth(footer.getFormattedText(), width - 50);
            totalContentHeight += footerLines.size() * 9 + 20;
        }

         
        int startY = height / 2 - totalContentHeight / 2;

         
        if (header != null) {
            List<String> headerLines = fr.listFormattedStringToWidth(header.getFormattedText(), width - 50);
            startY += headerLines.size() * 9 + 20;
        }

         
        int playerIndex = 0;
        for (NetworkPlayerInfo info : playerList) {
            if (playerIndex >= maxPlayers)
                break;

            UUID playerUUID = info.getGameProfile().getId();
            boolean isYesClientUser = playerUUID != null
                    && YesClientUserManager.getInstance().isYesClientUser(playerUUID);

            if (isYesClientUser) {
                int column = playerIndex / rows;
                int row = playerIndex % rows;

                 
                int x = startX + column * maxNameWidth + column * 5;
                int y = startY + row * 9 + yOffset;

                ScorePlayerTeam team = info.getPlayerTeam();
                String displayName = ScorePlayerTeam.formatPlayerName(team, info.getGameProfile().getName());
                displayName = com.yesclient.module.modules.visuals.NickHiderModule.replaceOwnName(displayName);

                 
                 
                int nameWidth = fr.getStringWidth(displayName);

                 
                Gui.drawRect(x + 10, y, x + 10 + nameWidth, y + 9, 0xFF000000);

                 
                drawRainbowString(fr, displayName, x + 10, y);
            }
            playerIndex++;
        }
    }

     


    private void drawRainbowString(FontRenderer fr, String text, int x, int y) {
        long time = System.currentTimeMillis();
        float speed = 2000f;
        float offset = (time % (long) speed) / speed;

        float currentX = x;
        int charIndex = 0;
        int totalVisibleChars = getVisibleCharCount(text);

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

             
            if (c == '\u00A7' && i + 1 < text.length()) {
                i++;  
                continue;
            }

             
            float hue = ((float) charIndex / Math.max(totalVisibleChars, 1) + offset) % 1.0f;
            Color rainbow = Color.getHSBColor(hue, 0.8f, 1.0f);
            int rainbowColor = rainbow.getRGB();

             
            String charStr = String.valueOf(c);
            fr.drawStringWithShadow(charStr, currentX, y, rainbowColor);
            currentX += fr.getCharWidth(c);
            charIndex++;
        }
    }

    private int getVisibleCharCount(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00A7' && i + 1 < text.length()) {
                i++;
                continue;
            }
            count++;
        }
        return count;
    }
}

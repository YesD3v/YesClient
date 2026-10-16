package com.yesclient.module.modules.visuals;
import java.awt.Color;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.FriendsModule;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ColorSetting;
import com.yesclient.module.settings.NumberSetting;
import com.yesclient.utils.YesClientUserManager;
import com.yesclient.utils.RenderUtils;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.entity.player.EntityPlayer;


public class TracersModule extends Module {

    private final BooleanSetting players = new BooleanSetting("Players", "Trace to players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", "Trace to hostile mobs", false);
    private final BooleanSetting animals = new BooleanSetting("Animals", "Trace to passive animals", false);
    private final BooleanSetting npcs = new BooleanSetting("NPCs", "Trace to NPCs", false);
    private final BooleanSetting friendsOnly = new BooleanSetting("Friends Only", "Only trace to friends", false);
    private final NumberSetting width = new NumberSetting("Width", "Line width", 1.0, 0.5, 5.0, 0.5);

    private final BooleanSetting rankColors = new BooleanSetting("Rank Colors", "Color tracers by Hypixel rank", true);
    private final BooleanSetting roleColors = new BooleanSetting("Role Colors", "Color tracers by Hypixel role", false);

    private boolean lastRank = true;
    private boolean lastRole = false;

     
    private final ColorSetting playerColor = new ColorSetting("Player Color", "Color for players",
            new Color(255, 50, 50));
    private final ColorSetting mobColor = new ColorSetting("Mob Color", "Color for mobs", new Color(255, 100, 100));
    private final ColorSetting animalColor = new ColorSetting("Animal Color", "Color for animals",
            new Color(100, 255, 100));
    private final ColorSetting npcColor = new ColorSetting("NPC Color", "Color for NPCs", new Color(255, 200, 0));

    public TracersModule() {
        super("Tracers", "Draw lines to entities", Category.VISUALS, ModuleMode.BOTH);
        addSetting(players);
        addSetting(mobs);
        addSetting(animals);
        addSetting(npcs);
        addSetting(friendsOnly);
        addSetting(width);
        addSetting(rankColors);
        addSetting(roleColors);
        addSetting(playerColor);
        addSetting(mobColor);
        addSetting(animalColor);
        addSetting(npcColor);
    }

    @Override
    public void onTick() {
        if (rankColors.isEnabled() && !lastRank) {
            roleColors.setValue(false);
        }
        if (roleColors.isEnabled() && !lastRole) {
            rankColors.setValue(false);
        }
        lastRank = rankColors.isEnabled();
        lastRole = roleColors.isEnabled();
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (mc.theWorld == null || mc.thePlayer == null)
            return;

         
         
        net.minecraft.util.Vec3 cameraPos = RenderUtils.getCrosshairPos(partialTicks);

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer)
                continue;

            if (shouldRender(entity)) {
                 
                if (entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) entity;
                    if (YesClientUserManager.getInstance().isYesClientUser(player)) {
                        renderRainbowTracer(entity, partialTicks, cameraPos);
                        continue;
                    }
                }

                Color color = getColor(entity);
                renderTracer(entity, color, partialTicks, cameraPos);
            }
        }

    }

    private boolean shouldRender(Entity entity) {
         
        if (entity.getEntityId() < 0)
            return false;

        if (entity instanceof EntityPlayer) {
             
            EntityPlayer player = (EntityPlayer) entity;
            Color color = null;
            if (rankColors.isEnabled()) {
                color = extractColorFromName(getTabDisplayName(player), player.getName());
            }
             
             
             
             
            if (color == null) {
                color = extractColorFromName(getTabDisplayName(player), player.getName());
            }

             
            boolean isBot = color == null ||
                    (color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 255) ||
                    (color.getRed() == 255 && color.getGreen() == 85 && color.getBlue() == 85);

            if (isBot)
                return npcs.isEnabled();

             
            if (friendsOnly.isEnabled() && !FriendsModule.isFriend(player.getName())) {
                return false;
            }

            return players.isEnabled();
        }
        if (entity instanceof EntityMob)
            return mobs.isEnabled();
        if (entity instanceof EntityAnimal)
            return animals.isEnabled();
        if (entity instanceof EntityVillager || entity instanceof EntityArmorStand)
            return npcs.isEnabled();
        return false;
    }

    private Color getColor(Entity entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;

             
            if (FriendsModule.isFriend(entity.getName()))
                return new Color(0, 255, 100);

             
            if (roleColors.isEnabled()) {
                String displayName = getTabDisplayName(player);
                Color roleColor = extractRoleColor(displayName);
                if (roleColor != null) {
                    return roleColor;
                }
            }

             
            if (rankColors.isEnabled()) {
                 
                net.minecraft.scoreboard.Team team = player.getTeam();
                if (team instanceof ScorePlayerTeam) {
                    ScorePlayerTeam sbTeam = (ScorePlayerTeam) team;
                    String prefix = sbTeam.getColorPrefix();
                    Color teamColor = extractColorFromPrefix(prefix);
                    if (teamColor != null) {
                        return teamColor;
                    }
                }

                 
                String displayName = getTabDisplayName(player);
                Color nameColor = extractColorFromName(displayName, player.getName());
                if (nameColor != null) {
                    return nameColor;
                }
            }

            return playerColor.getValue();
        }
        if (entity instanceof EntityMob)
            return mobColor.getValue();
        if (entity instanceof EntityAnimal)
            return animalColor.getValue();
        return npcColor.getValue();
    }

    private Color extractColorFromPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty())
            return null;
         
        for (int i = prefix.length() - 2; i >= 0; i--) {
            if (prefix.charAt(i) == '\u00A7') {
                char code = Character.toLowerCase(prefix.charAt(i + 1));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    return getColorFromCode(code);
                }
            }
        }
        return null;
    }

     


    private Color extractColorFromName(String displayName, String playerName) {
        if (displayName == null)
            return null;

         
        if (playerName != null && !playerName.isEmpty()) {
            int nameIndex = displayName.indexOf(playerName);
            if (nameIndex > -1) {
                for (int i = nameIndex - 1; i >= 0; i--) {
                    if (displayName.charAt(i) == '\u00A7' && i + 1 < displayName.length()) {
                        char code = Character.toLowerCase(displayName.charAt(i + 1));
                        if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                             
                             
                             
                             
                            return getColorFromCode(code);
                        }
                    }
                }
            }
        }

         
        for (int i = 0; i < displayName.length() - 1; i++) {
            if (displayName.charAt(i) == '\u00A7') {
                char code = Character.toLowerCase(displayName.charAt(i + 1));
                 
                 
                if ((code >= '0' && code <= '6') || code == '9' || (code >= 'a' && code <= 'f')) {
                    return getColorFromCode(code);
                }
            }
        }
        return null;
    }

    private Color getColorFromCode(char code) {
        switch (code) {
            case '0':
                return new Color(0, 0, 0);
            case '1':
                return new Color(0, 0, 170);
            case '2':
                return new Color(0, 170, 0);
            case '3':
                return new Color(0, 170, 170);
            case '4':
                return new Color(170, 0, 0);
            case '5':
                return new Color(170, 0, 170);
            case '6':
                return new Color(255, 170, 0);
            case '7':
                return new Color(170, 170, 170);
            case '8':
                return new Color(85, 85, 85);
            case '9':
                return new Color(85, 85, 255);
            case 'a':
                return new Color(85, 255, 85);
            case 'b':
                return new Color(85, 255, 255);
            case 'c':
                return new Color(255, 85, 85);
            case 'd':
                return new Color(255, 85, 255);
            case 'e':
                return new Color(255, 255, 85);
            case 'f':
                return new Color(255, 255, 255);
            default:
                return null;
        }
    }

    private Color extractRoleColor(String displayName) {
        if (displayName == null)
            return null;
         
        for (int i = displayName.length() - 2; i >= 0; i--) {
            if (displayName.charAt(i) == '\u00A7') {
                char code = Character.toLowerCase(displayName.charAt(i + 1));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    return getColorFromCode(code);
                }
            }
        }
        return null;
    }

    private String getTabDisplayName(EntityPlayer player) {
        if (mc.getNetHandler() != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
            if (info != null) {
                if (info.getDisplayName() != null) {
                    return info.getDisplayName().getFormattedText();
                }
                return mc.ingameGUI.getTabList().getPlayerName(info);
            }
        }
        return player.getName();
    }

    private void renderTracer(Entity entity, Color color, float partialTicks, net.minecraft.util.Vec3 cameraPos) {
         
        RenderUtils.drawTracer(entity, partialTicks, color.getRGB(), width.getFloatValue(), cameraPos);
    }

    private void renderRainbowTracer(Entity entity, float partialTicks, net.minecraft.util.Vec3 cameraPos) {
        RenderManager rm = mc.getRenderManager();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rm.viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - rm.viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rm.viewerPosZ;

         
        RenderUtils.drawRainbowLine(cameraPos.xCoord - rm.viewerPosX, cameraPos.yCoord - rm.viewerPosY,
                cameraPos.zCoord - rm.viewerPosZ, x, y + entity.height / 2, z,
                width.getFloatValue(), 2000f);
    }
}

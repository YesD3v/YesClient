package com.yesclient.module.modules.visuals;
import java.awt.Color;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.FriendsModule;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.NumberSetting;
import com.yesclient.utils.YesClientUserManager;
import com.yesclient.utils.RenderUtils;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;


public class ESPModule extends Module {

    private final BooleanSetting players = new BooleanSetting("Players", "Show players", true);
    private final BooleanSetting npcs = new BooleanSetting("NPCs", "Show NPCs (villagers, armor stands)", false);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", "Show hostile mobs", false);
    private final BooleanSetting animals = new BooleanSetting("Animals", "Show passive animals", false);
    private final BooleanSetting itemFrames = new BooleanSetting("Item Frames", "Show item frames", false);
    private final BooleanSetting self = new BooleanSetting("Self", "Show yourself", false);

    private final BooleanSetting healthBar = new BooleanSetting("Health Bar", "Show health bar", false);
    private final BooleanSetting rankColors = new BooleanSetting("Rank Colors", "Use player rank colors", true);
    private final BooleanSetting roleColors = new BooleanSetting("Role Colors", "Use player role colors (last color)",
            false);

    private boolean lastRank = true;
    private boolean lastRole = false;

    private final NumberSetting lineWidth = new NumberSetting("Line Width", "Width of ESP lines", 2.0, 0.5, 5.0, 0.5);

    public ESPModule() {
        super("ESP", "See entities through walls", Category.VISUALS, ModuleMode.BOTH);

        addSetting(players);
        addSetting(npcs);
        addSetting(mobs);
        addSetting(animals);
        addSetting(itemFrames);
        addSetting(self);
        addSetting(healthBar);
        addSetting(rankColors);
        addSetting(roleColors);
        addSetting(lineWidth);
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

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!shouldRender(entity))
                continue;

            FreeCamModule freeCam = com.yesclient.YesClient.getInstance().getModuleManager()
                    .getModule(FreeCamModule.class);
            if (freeCam != null && freeCam.isEnabled() && freeCam.isAllowInteractions()
                    && freeCam.isEntityWithinInteractionRadius(entity)) continue;

             
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (YesClientUserManager.getInstance().isYesClientUser(player)) {
                    renderRainbowESP(entity, partialTicks);
                    continue;
                }
            }

            Color color = getColorForEntity(entity);
            renderESP(entity, color, partialTicks);
        }
    }

    private boolean shouldRender(Entity entity) {
         
        if (entity.getEntityId() < 0) {
            return false;
        }

         
        if (com.yesclient.module.modules.render.HideHykiaEntitiesModule.shouldHide(entity)) {
            return false;
        }

         
        if (entity == mc.thePlayer) {
            return self.isEnabled();
        }

         
         
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
             
            Color color = extractColorFromName(getTabDisplayName(player), player.getName());
             
            boolean isBot = color == null ||
                    (color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 255) ||
                    (color.getRed() == 255 && color.getGreen() == 85 && color.getBlue() == 85);

            if (isBot) {
                return npcs.isEnabled();  
            }

            return players.isEnabled();
        } else if (entity instanceof EntityArmorStand || entity instanceof EntityVillager) {
            return npcs.isEnabled();
        } else if (entity instanceof EntityMob) {
            return mobs.isEnabled();
        } else if (entity instanceof EntityAnimal) {
            return animals.isEnabled();
        } else if (entity instanceof EntityItemFrame) {
            return itemFrames.isEnabled();
        }

        return false;
    }

    private Color getColorForEntity(Entity entity) {
        com.yesclient.module.modules.exploit.ProjectileAimbotModule aimbot =
                com.yesclient.YesClient.getInstance().getModuleManager()
                        .getModule(com.yesclient.module.modules.exploit.ProjectileAimbotModule.class);
        if (aimbot != null && aimbot.isLockedTarget(entity)) {
            return new Color(255, 35, 35, aimbot.isTargetOccluded(entity) ? 105 : 255);
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;

             
            if (FriendsModule.isFriend(player.getName())) {
                return new Color(0, 255, 100);  
            }

             
            if (rankColors.isEnabled()) {
                String displayName = getTabDisplayName(player);
                 
                Color nameColor = extractColorFromName(displayName, player.getName());
                if (nameColor != null) {
                    return nameColor;
                }
            }
            if (roleColors.isEnabled()) {
                String displayName = getTabDisplayName(player);
                Color roleColor = extractRoleColor(displayName);
                if (roleColor != null) {
                    return roleColor;
                }
            }

            return new Color(255, 50, 50);  
        }
        if (entity instanceof EntityVillager || entity instanceof EntityArmorStand) {
            return new Color(255, 200, 0);  
        }
        if (entity instanceof EntityMob) {
            return new Color(255, 100, 100);  
        }
        if (entity instanceof EntityAnimal) {
            return new Color(100, 255, 100);  
        }
        if (entity instanceof EntityItemFrame) {
            return new Color(255, 215, 0);  
        }
        return new Color(255, 255, 255);  
    }

    private String getTabDisplayName(EntityPlayer player) {
        if (mc.getNetHandler() == null)
            return player.getName();

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        if (info != null) {
             
            if (info.getDisplayName() != null) {
                return info.getDisplayName().getFormattedText();
            }
             
            return mc.ingameGUI.getTabList().getPlayerName(info);
        }
        return player.getName();
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
            } else {
                 
                 
            }
        }

         
        for (int i = 0; i < displayName.length() - 1; i++) {
            if (displayName.charAt(i) == '\u00A7') {
                char code = Character.toLowerCase(displayName.charAt(i + 1));
                 
                 
                 
                if ((code >= '0' && code <= '6') || code == '9' || (code >= 'a' && code <= 'f')) {
                    Color color = getColorFromCode(code);
                    if (color != null) {
                        return color;
                    }
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

    private void renderESP(Entity entity, Color color, float partialTicks) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        x -= mc.getRenderManager().viewerPosX;
        y -= mc.getRenderManager().viewerPosY;
        z -= mc.getRenderManager().viewerPosZ;

        AxisAlignedBB bb = entity.getEntityBoundingBox();
        bb = bb.offset(-entity.posX, -entity.posY, -entity.posZ).offset(x, y, z);

         
        float distance = mc.thePlayer.getDistanceToEntity(entity);
        float scaledWidth = lineWidth.getFloatValue();

         
         
        if (distance > 10) {
            scaledWidth = Math.max(0.1f, scaledWidth * (10f / distance));
        }

        RenderUtils.drawOutlinedBox(bb, color, scaledWidth);

         
        if (healthBar.isEnabled() && entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            float healthPercent = living.getHealth() / living.getMaxHealth();

             
            double barX = bb.minX - 0.15;
            double barHeight = bb.maxY - bb.minY;
            double currentHealthHeight = barHeight * healthPercent;

            Color healthColor;
            if (healthPercent > 0.66f)
                healthColor = new Color(0, 255, 0, 200);
            else if (healthPercent > 0.33f)
                healthColor = new Color(255, 255, 0, 200);
            else
                healthColor = new Color(255, 0, 0, 200);

             
            AxisAlignedBB bgBar = new AxisAlignedBB(barX - 0.08, bb.minY, bb.minZ, barX, bb.maxY, bb.minZ + 0.08);
            RenderUtils.drawFilledBox(bgBar, new Color(0, 0, 0, 150));

             
            AxisAlignedBB hpBar = new AxisAlignedBB(barX - 0.06, bb.minY + 0.02, bb.minZ + 0.02,
                    barX - 0.02, bb.minY + currentHealthHeight - 0.02, bb.minZ + 0.06);
            RenderUtils.drawFilledBox(hpBar, healthColor);
        }
    }




    private void renderRainbowESP(Entity entity, float partialTicks) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        x -= mc.getRenderManager().viewerPosX;
        y -= mc.getRenderManager().viewerPosY;
        z -= mc.getRenderManager().viewerPosZ;

        AxisAlignedBB bb = entity.getEntityBoundingBox();
        bb = bb.offset(-entity.posX, -entity.posY, -entity.posZ).offset(x, y, z);

         
        float distance = mc.thePlayer.getDistanceToEntity(entity);
        float scaledWidth = lineWidth.getFloatValue();

         
        if (distance > 10) {
            scaledWidth = Math.max(0.5f, scaledWidth * (10f / distance));
        }

         
        RenderUtils.drawRainbowOutlinedBox(bb, scaledWidth, 2000f);

         
        if (healthBar.isEnabled() && entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            float healthPercent = living.getHealth() / living.getMaxHealth();

            double barX = bb.minX - 0.15;
            double barHeight = bb.maxY - bb.minY;
            double currentHealthHeight = barHeight * healthPercent;

            Color healthColor;
            if (healthPercent > 0.66f)
                healthColor = new Color(0, 255, 0, 200);
            else if (healthPercent > 0.33f)
                healthColor = new Color(255, 255, 0, 200);
            else
                healthColor = new Color(255, 0, 0, 200);

            AxisAlignedBB bgBar = new AxisAlignedBB(barX - 0.08, bb.minY, bb.minZ, barX, bb.maxY, bb.minZ + 0.08);
            RenderUtils.drawFilledBox(bgBar, new Color(0, 0, 0, 150));

            AxisAlignedBB hpBar = new AxisAlignedBB(barX - 0.06, bb.minY + 0.02, bb.minZ + 0.02,
                    barX - 0.02, bb.minY + currentHealthHeight - 0.02, bb.minZ + 0.06);
            RenderUtils.drawFilledBox(hpBar, healthColor);
        }
    }
}

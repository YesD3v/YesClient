package com.yesclient.module.modules.visuals;
import java.util.List;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.HudTextStyle;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.FancyTextModule;
import com.yesclient.module.modules.client.HudDesignerModule;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.NumberSetting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;

 



public class LoadedPlayersModule extends Module {

    private final HudScale hudScale = new HudScale(this);
    private final BooleanSetting showCountOnly = new BooleanSetting("Count Only", "Show only the player count", false);
    private final BooleanSetting showDistance = new BooleanSetting("Show Distance", "Display distance to each player",
            false);
    private final BooleanSetting normalizeNames = new BooleanSetting("Normalize Names",
            "Convert fancy/unicode characters in names to normal letters", false);
    private final BooleanSetting customMaxDistance = new BooleanSetting("Custom Max Distance",
            "Limit listed players by distance", false);
    private final NumberSetting maxDistance = new NumberSetting("Max Distance", "Maximum distance when custom limit is on",
            80.0, 16.0, 128.0, 8.0);
    private final HudTextStyle textStyle = new HudTextStyle(this, true, 0.0D, 30.0D, 500.0D,
            0xFFFF5555, 0xFF55FF55);

    private List<EntityPlayer> loadedPlayers = new ArrayList<>();

    public LoadedPlayersModule() {
        super("Loaded Players", "Shows players currently rendered around you", Category.VISUALS, ModuleMode.BOTH);

        addSetting(showCountOnly);
        addSetting(showDistance);
        addSetting(normalizeNames);
        addSetting(customMaxDistance);
        addSetting(maxDistance);

        maxDistance.setVisibility(() -> customMaxDistance.isEnabled());
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        loadedPlayers.clear();
        boolean limitDistance = customMaxDistance.isEnabled();
        double maxDist = maxDistance.getValue();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer)
                continue;

             
            if (isBot(player))
                continue;

            if (limitDistance && mc.thePlayer.getDistanceToEntity(player) > maxDist) {
                continue;
            }

            loadedPlayers.add(player);
        }
    }

    private boolean isBot(EntityPlayer player) {
        String displayName = getTabDisplayName(player);
         
        if (!displayName.contains("\u00A7")) {
            return true;  
        }
         
        int idx = displayName.indexOf("\u00A7");
        if (idx + 1 < displayName.length()) {
            char code = Character.toLowerCase(displayName.charAt(idx + 1));
            if (code == 'f' || code == '8' || code == 'c') {
                return true;  
            }
        }
        return false;
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

         
        HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getLoadedPlayersX() : 5;
        int y = designer != null ? designer.getLoadedPlayersY() : 50;

        int maxWidth = 0;
        int totalHeight = 0;
        hudScale.beginAt(x, y);

        if (showCountOnly.isEnabled()) {
            String text = textStyle.format("Loaded", String.valueOf(loadedPlayers.size()), "");
            mc.fontRendererObj.drawStringWithShadow(text, x, y, textStyle.getColor(loadedPlayers.size()));

            maxWidth = mc.fontRendererObj.getStringWidth(text);
            totalHeight = mc.fontRendererObj.FONT_HEIGHT;
        } else {
             
            String header = textStyle.format("Loaded Players", String.valueOf(loadedPlayers.size()), "");
            mc.fontRendererObj.drawStringWithShadow(header, x, y, textStyle.getColor(loadedPlayers.size()));

            maxWidth = mc.fontRendererObj.getStringWidth(header);
            totalHeight += 12;  
            y += 12;

             
            for (EntityPlayer player : loadedPlayers) {
                String displayName = getTabDisplayName(player);
                if (normalizeNames.isEnabled()) {
                    displayName = FancyTextModule.revertToNormal(displayName);
                }
                if (showDistance.isEnabled()) {
                    double dist = mc.thePlayer.getDistanceToEntity(player);
                    displayName = displayName + " \u00A77[" + String.format("%.1f", dist) + "m]";
                }
                mc.fontRendererObj.drawStringWithShadow(displayName, x + 4, y,
                        textStyle.getColor(loadedPlayers.size()));

                int nameWidth = mc.fontRendererObj.getStringWidth(displayName) + 4;
                if (nameWidth > maxWidth)
                    maxWidth = nameWidth;

                totalHeight += 10;
                y += 10;
            }
        }

         
        this.setWidth(maxWidth);
        this.setHeight(totalHeight);
        hudScale.end();
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

    @Override
    public String getDisplayInfo() {
        return String.valueOf(loadedPlayers.size());
    }
}

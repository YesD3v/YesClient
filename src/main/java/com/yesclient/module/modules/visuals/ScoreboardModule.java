package com.yesclient.module.modules.visuals;
import java.util.List;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.HudDesignerModule;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.NumberSetting;
import com.yesclient.utils.RenderUtils;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.*;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collection;
import java.util.stream.Collectors;

 












public class ScoreboardModule extends Module {

    private final HudScale hudScale = new HudScale(this);
    private final BooleanSetting hideNumbers = new BooleanSetting("Hide Numbers",
            "Hide the red score numbers", true);
    private final BooleanSetting cleanBackground = new BooleanSetting("Clean Background",
            "Use cleaner dark background", true);
    private final BooleanSetting roundedCorners = new BooleanSetting("Rounded Corners",
            "Use rounded corners on background", true);
    private final NumberSetting opacity = new NumberSetting("Background Opacity",
            "Background opacity %", 30, 0, 100, 5);

    public ScoreboardModule() {
        super("Scoreboard", "Custom moveable scoreboard with cleaner look", Category.VISUALS, ModuleMode.BOTH);
        addSetting(hideNumbers);
        addSetting(cleanBackground);
        addSetting(roundedCorners);
        addSetting(opacity);
    }

     

     


    @SubscribeEvent
    public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        if (!isEnabled())
            return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL)
            return;
        if (mc.theWorld == null || mc.thePlayer == null)
            return;
        if (mc.gameSettings.showDebugInfo)
            return;  

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);

        if (objective == null)
            return;

        renderCustomScoreboard(objective, new ScaledResolution(mc));
    }

    private void renderCustomScoreboard(ScoreObjective objective, ScaledResolution sr) {
        Scoreboard scoreboard = objective.getScoreboard();
        Collection<Score> scores = scoreboard.getSortedScores(objective);

         
        List<Score> filteredScores = scores.stream()
                .filter(score -> score.getPlayerName() != null && !score.getPlayerName().startsWith("#"))
                .collect(Collectors.toList());

         
        if (filteredScores.size() > 15) {
            filteredScores = Lists.newArrayList(Iterables.skip(filteredScores, filteredScores.size() - 15));
        }

        if (filteredScores.isEmpty())
            return;

        FontRenderer fr = mc.fontRendererObj;
        int fontHeight = fr.FONT_HEIGHT;

         
        int maxWidth = fr.getStringWidth(objective.getDisplayName());

        for (Score score : filteredScores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String text = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());

            int lineWidth = fr.getStringWidth(text);
            if (!hideNumbers.isEnabled()) {
                lineWidth += fr.getStringWidth(": " + score.getScorePoints());
            }
            maxWidth = Math.max(maxWidth, lineWidth);
        }

        int linesCount = filteredScores.size();
        int totalHeight = linesCount * fontHeight + fontHeight + 4;  
        int width = maxWidth + 6;

         
        int x, y;
        HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        if (designer != null && designer.getScoreboardX() >= 0) {
            x = designer.getScoreboardX();
            y = designer.getScoreboardY();
        } else {
             
            x = sr.getScaledWidth() - width - 3;
            y = (sr.getScaledHeight() - totalHeight) / 2;
        }

        int displayWidth = hudScale.scaled(width);
        int displayHeight = hudScale.scaled(totalHeight);
        if (designer == null || designer.getScoreboardX() < 0) {
            x = sr.getScaledWidth() - displayWidth - 3;
            y = (sr.getScaledHeight() - displayHeight) / 2;
        }
        hudScale.beginAt(x, y);

         
        int alpha = (int) (opacity.getValue() * 255.0 / 100.0);
        if (alpha == 0 && opacity.getValue() == 0)
            alpha = 1;  
        int bgColor = (alpha << 24) | 0x000000;

         
        if (roundedCorners.isEnabled()) {
            RenderUtils.drawRoundedRect(x - 2, y - 2, width + 4, totalHeight + 2, 4, bgColor);
        } else {
            Gui.drawRect(x - 2, y - 2, x + width + 2, y + totalHeight, bgColor);
        }

         
        String title = objective.getDisplayName();
        int titleX = x + (width - fr.getStringWidth(title)) / 2;
        fr.drawStringWithShadow(title, titleX, y, 0xFFFFFFFF);

         
        int lineY = y + fontHeight + 2;

         
        for (int i = filteredScores.size() - 1; i >= 0; i--) {
            Score score = filteredScores.get(i);
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String playerName = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());

             
            fr.drawStringWithShadow(playerName, x, lineY, 0xFFFFFFFF);

             
            if (!hideNumbers.isEnabled()) {
                String scoreText = "" + score.getScorePoints();
                int scoreX = x + width - fr.getStringWidth(scoreText);
                fr.drawStringWithShadow(scoreText, scoreX, lineY, 0xFFFF5555);
            }

            lineY += fontHeight;
        }
        setWidth(width + 4);
        setHeight(totalHeight + 2);
        hudScale.end();
    }

     


    public int[] getBounds() {
        if (mc.theWorld == null)
            return new int[] { 0, 0, 100, 100 };

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);

        if (objective == null)
            return new int[] { 0, 0, 100, 100 };

         
        List<Score> filteredScores = scoreboard.getSortedScores(objective).stream()
                .filter(score -> score.getPlayerName() != null && !score.getPlayerName().startsWith("#"))
                .collect(Collectors.toList());
        if (filteredScores.size() > 15) {
            filteredScores = Lists.newArrayList(Iterables.skip(filteredScores, filteredScores.size() - 15));
        }
        FontRenderer fr = mc.fontRendererObj;
        int maxWidth = fr.getStringWidth(objective.getDisplayName());
        for (Score score : filteredScores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String text = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            int textWidth = fr.getStringWidth(text);
            if (!hideNumbers.isEnabled()) textWidth += fr.getStringWidth(": " + score.getScorePoints());
            maxWidth = Math.max(maxWidth, textWidth);
        }
        int height = filteredScores.size() * fr.FONT_HEIGHT + fr.FONT_HEIGHT + 6;
        return new int[] { 0, 0, maxWidth + 10, height };
    }
}

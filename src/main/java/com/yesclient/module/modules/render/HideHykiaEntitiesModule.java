package com.yesclient.module.modules.render;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collection;

public class HideHykiaEntitiesModule extends Module {

    private boolean inHykiaContext = false;
    private int tickTimer = 0;

     
    private static final double MIN_X = -86;
    private static final double MAX_X = -37;
    private static final double MIN_Y = 47;
    private static final double MAX_Y = 90;
    private static final double MIN_Z = -62;
    private static final double MAX_Z = 23;

    private static HideHykiaEntitiesModule INSTANCE;

    public HideHykiaEntitiesModule() {
        super("Hide Hykia Entities", "Hides entities in Hykia lobby region", Category.RENDER, ModuleMode.BOTH);
        INSTANCE = this;
    }

    public static boolean shouldHide(Entity entity) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !INSTANCE.inHykiaContext)
            return false;
        return INSTANCE.isInZone(entity);
    }

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre<?> event) {
        if (!inHykiaContext)
            return;

        if (isInZone(event.entity)) {
            event.setCanceled(true);
        }
    }

    private boolean isInZone(Entity entity) {
        return entity.posX >= MIN_X && entity.posX <= MAX_X &&
                entity.posY >= MIN_Y && entity.posY <= MAX_Y &&
                entity.posZ >= MIN_Z && entity.posZ <= MAX_Z;
    }

    @Override
    public void onTick() {
        if (mc.theWorld == null) {
            inHykiaContext = false;
            return;
        }

         
        if (tickTimer++ < 20 && inHykiaContext) {
             
             
             
            return;
        }
        tickTimer = 0;
        boolean newContext = checkHykiaContext();
        if (newContext != inHykiaContext) {
            inHykiaContext = newContext;
            if (inHykiaContext) {
                 
                 
            } else {
                 
                 
            }
        }
    }

    @SubscribeEvent
    public void onRenderPlayer(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
        if (!inHykiaContext)
            return;
        if (isInZone(event.entity))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRenderLivingSpecials(RenderLivingEvent.Specials.Pre<?> event) {
        if (!inHykiaContext)
            return;
        if (isInZone(event.entity))
            event.setCanceled(true);
    }

    private boolean checkHykiaContext() {
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null)
            return false;

        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null)
            return false;

        boolean housingFound = false;
        String serverId = null;

        String title = stripColor(objective.getDisplayName());
         
        if (title.toUpperCase().contains("HOUSING")) {
            housingFound = true;
        }

        Collection<Score> scores = scoreboard.getSortedScores(objective);
        Pattern idPattern = Pattern.compile("([LlMm][a-zA-Z0-9]{3,9})$");

        for (Score score : scores) {
            String playerName = score.getPlayerName();
            ScorePlayerTeam team = scoreboard.getPlayersTeam(playerName);
            String line = (team != null) ? team.getColorPrefix() + playerName + team.getColorSuffix() : playerName;

            String stripped = stripColor(line).trim();
            if (stripped.isEmpty())
                continue;

             
            if (stripped.toUpperCase().contains("HOUSING")) {
                housingFound = true;
            }

             
             
            if (stripped.matches("\\d{1,2}/\\d{1,2}/\\d{2,4}\\s+\\S+")) {
                String[] parts = stripped.split("\\s+");
                if (parts.length >= 2) {
                    String id = parts[parts.length - 1];
                    if (id.startsWith("L"))
                        serverId = id;
                }
            }
             
            Matcher matcher = idPattern.matcher(stripped);
            if (matcher.find()) {
                String id = matcher.group(1);
                if (id.startsWith("L"))
                    serverId = id;
            }
             
            if (stripped.matches("[a-zA-Z0-9]{4,10}") && stripped.startsWith("L")) {
                serverId = stripped;
            }
        }

         
        if (!housingFound || serverId == null) {
             
        } else {
             
        }

        return housingFound && serverId != null;
    }

    private String stripColor(String input) {
        return input.replaceAll("\u00A7.", "");
    }
}

package com.yesclient.utils;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.yesclient.YesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collection;

public class HousingDetector {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    private boolean inHousing = false;
    private boolean isOwner = false;
    private boolean isCoOwner = false;
    private String currentPlotName = "";
    private String currentOwner = "";
    private int visitorCount = 0;
    
    private static final Pattern HOUSING_PATTERN = Pattern.compile("(?:Your|\\w+'s) Housing");
    private static final Pattern OWNER_PATTERN = Pattern.compile("Owner: (\\w+)");
    private static final Pattern VISITORS_PATTERN = Pattern.compile("Visitors: (\\d+)");
    
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
         
        checkScoreboard();
    }
    
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String message = ChatUtils.stripColorCodes(event.message.getUnformattedText());
        
         
        if (message.contains("You are now visiting")) {
            isOwner = false;
            isCoOwner = false;
            YesClient.getInstance().setOwnerMode(false);
        } else if (message.contains("Welcome to your Housing!") || 
                   message.contains("Teleported to your Housing")) {
            isOwner = true;
            YesClient.getInstance().setOwnerMode(true);
        } else if (message.contains("You are now a co-owner")) {
            isCoOwner = true;
            YesClient.getInstance().setOwnerMode(true);
        }
        
         
        Matcher visitorMatcher = VISITORS_PATTERN.matcher(message);
        if (visitorMatcher.find()) {
            visitorCount = Integer.parseInt(visitorMatcher.group(1));
        }
    }
    
    private void checkScoreboard() {
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        
        if (objective == null) {
            inHousing = false;
            return;
        }
        
        String title = ChatUtils.stripColorCodes(objective.getDisplayName());
        
         
        if (!title.contains("HYPIXEL")) {
            inHousing = false;
            return;
        }
        
         
        List<String> lines = getScoreboardLines(scoreboard, objective);
        
        for (String line : lines) {
            String cleanLine = ChatUtils.stripColorCodes(line);
            
             
            if (cleanLine.contains("Housing") || cleanLine.contains("housing")) {
                inHousing = true;
            }
            
             
            Matcher ownerMatcher = OWNER_PATTERN.matcher(cleanLine);
            if (ownerMatcher.find()) {
                currentOwner = ownerMatcher.group(1);
                
                 
                if (mc.thePlayer != null && 
                    currentOwner.equalsIgnoreCase(mc.thePlayer.getName())) {
                    isOwner = true;
                    YesClient.getInstance().setOwnerMode(true);
                }
            }
        }
    }
    
    private List<String> getScoreboardLines(Scoreboard scoreboard, ScoreObjective objective) {
        List<String> lines = new ArrayList<>();
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        
        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            lines.add(line);
        }
        
        return lines;
    }
    
    public boolean isInHousing() {
        return inHousing;
    }
    
    public boolean isOwner() {
        return isOwner;
    }
    
    public boolean isCoOwner() {
        return isCoOwner;
    }
    
    public boolean hasOwnerPermissions() {
        return isOwner || isCoOwner;
    }
    
    public String getCurrentPlotName() {
        return currentPlotName;
    }
    
    public String getCurrentOwner() {
        return currentOwner;
    }
    
    public int getVisitorCount() {
        return visitorCount;
    }
    
    public void setInHousing(boolean inHousing) {
        this.inHousing = inHousing;
    }
    
    public void setOwner(boolean owner) {
        this.isOwner = owner;
        YesClient.getInstance().setOwnerMode(owner || isCoOwner);
    }
}


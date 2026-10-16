package com.yesclient.event;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.yesclient.altmanager.CookieAltManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.util.IChatComponent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


 




public class BanDetectionHandler {

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityPlayer && event.entity == Minecraft.getMinecraft().thePlayer) {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (!mc.isSingleplayer() && mc.getCurrentServerData() != null) {
                    String ip = mc.getCurrentServerData().serverIP.toLowerCase();
                    if (ip.contains("hypixel.net")) {
                        String currentUser = mc.getSession().getUsername();
                        CookieAltManager manager = CookieAltManager.getInstance();
                        if (manager != null) {
                            for (CookieAltManager.AltAccount acc : manager.getAccounts()) {
                                if (acc.username != null && acc.username.equalsIgnoreCase(currentUser)) {
                                    acc.autoUnban();
                                    manager.save();
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                 
            }
        }
    }

    private IChatComponent getField(GuiDisconnected screen, String fieldName) {
        try {
            Field f = screen.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object value = f.get(screen);
            if (value instanceof IChatComponent) {
                return (IChatComponent) value;
            }
        } catch (Exception e) {
             
        }
        return null;
    }

    private IChatComponent findIChatComponentField(GuiDisconnected screen) {
        try {
             
            for (Field f : screen.getClass().getDeclaredFields()) {
                if (IChatComponent.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (IChatComponent) f.get(screen);
                }
            }
             
            Class<?> superClass = screen.getClass().getSuperclass();
            while (superClass != null && superClass != Object.class) {
                for (Field f : superClass.getDeclaredFields()) {
                    if (IChatComponent.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        return (IChatComponent) f.get(screen);
                    }
                }
                superClass = superClass.getSuperclass();
            }
        } catch (Exception e) {
             
        }
        return null;
    }

    private long parseExpiryToTimestamp(String text) {
        try {
            Pattern pattern = Pattern.compile(
                    "(?:(\\d+)\\s*d(?:ays?)?)?\\s*(?:(\\d+)\\s*h)?\\s*(?:(\\d+)\\s*m)?\\s*(?:(\\d+)\\s*s)?"
            );
            Matcher matcher = pattern.matcher(text.replace("temporarily banned for ", ""));
            
            long totalMs = 0;
             
             
            while (matcher.find()) {
                String d = matcher.group(1);
                String h = matcher.group(2);
                String m = matcher.group(3);
                String s = matcher.group(4);
                
                boolean foundSomething = false;
                if (d != null) { totalMs += Long.parseLong(d) * 86400000L; foundSomething = true; }
                if (h != null) { totalMs += Long.parseLong(h) * 3600000L; foundSomething = true; }
                if (m != null) { totalMs += Long.parseLong(m) * 60000L; foundSomething = true; }
                if (s != null) { totalMs += Long.parseLong(s) * 1000L; foundSomething = true; }
                
                if (foundSomething) {
                    return System.currentTimeMillis() + totalMs;
                }
            }
        } catch (Exception e) {
             
        }
        return 0;
    }
}

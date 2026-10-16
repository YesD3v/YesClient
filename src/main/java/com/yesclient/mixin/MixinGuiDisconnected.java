package com.yesclient.mixin;

import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.yesclient.altmanager.CookieAltManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(GuiDisconnected.class)
public class MixinGuiDisconnected {
    @Shadow(aliases = "field_146304_f")
    private IChatComponent message;

    @Inject(method = "func_73866_w_", at = @At("HEAD"))
    private void onInitGui(CallbackInfo ci) {
        if (message == null) return;
        String text = message.getUnformattedText();
        
        CookieAltManager manager = CookieAltManager.getInstance();
        String currentName = manager.getCurrentUsername();
        if (currentName == null) return;
        
        CookieAltManager.AltAccount currentAcc = null;
        for (CookieAltManager.AltAccount acc : manager.getAccounts()) {
            if (currentName.equalsIgnoreCase(acc.username)) {
                currentAcc = acc;
                break;
            }
        }
        if (currentAcc == null) return;
        
        if (net.minecraft.client.Minecraft.getMinecraft().getCurrentServerData() == null || 
            !net.minecraft.client.Minecraft.getMinecraft().getCurrentServerData().serverIP.toLowerCase().contains("hypixel.net")) {
            return;
        }

        if (text.contains("permanently banned") || text.contains("Permanently banned") || text.contains("Banned by Watchdog")) {
            currentAcc.setBan("BANNED", -1);
            manager.save();
        } else if (text.contains("temporarily banned") || text.contains("Temporarily banned") || text.contains("Time left:")) {
            long durationMs = parseTimeLeft(text);
            if (durationMs > 0) {
                currentAcc.setBan("TEMP_BANNED", System.currentTimeMillis() + durationMs);
                manager.save();
            } else {
                currentAcc.setBan("TEMP_BANNED", System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000);  
                manager.save();
            }
        }
    }

    private long parseTimeLeft(String text) {
        Pattern pattern = Pattern.compile("(?:Time left:|for)\\s*(?:(\\d+)d)?\\s*(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            long ms = 0;
            if (matcher.group(1) != null) ms += Long.parseLong(matcher.group(1)) * 24 * 60 * 60 * 1000L;
            if (matcher.group(2) != null) ms += Long.parseLong(matcher.group(2)) * 60 * 60 * 1000L;
            if (matcher.group(3) != null) ms += Long.parseLong(matcher.group(3)) * 60 * 1000L;
            if (matcher.group(4) != null) ms += Long.parseLong(matcher.group(4)) * 1000L;
            return ms;
        }
        return 0;
    }
}

package com.yesclient.module.modules.client;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.utils.DiscordRPCClient;

 
public class DiscordRPCModule extends Module {

    private final DiscordRPCClient rpc = new DiscordRPCClient();
    private final BooleanSetting showUsername = new BooleanSetting("Show Username",
            "Show the current Minecraft username in Discord", true);
    private boolean lastShowUsername;

    public DiscordRPCModule() {
        super("Discord RPC", "Show YesClient activity on Discord", Category.CLIENT, ModuleMode.BOTH);
        addSetting(showUsername);
        setEnabled(true);
    }

    @Override
    protected void onEnable() {
        lastShowUsername = showUsername.isEnabled();
        rpc.start();
    }

    @Override
    protected void onDisable() {
        rpc.stop();
    }

    @Override
    public void onTick() {
        boolean includeUsername = showUsername.isEnabled();
        String state = "with YesClient";
        if (includeUsername && YesClient.getMinecraft().thePlayer != null) {
            state += " • " + YesClient.getMinecraft().thePlayer.getName();
        }
        rpc.setActivity("Playing Minecraft", state);

         
         
        if (includeUsername != lastShowUsername) {
            lastShowUsername = includeUsername;
            rpc.restart();
        }
    }
}

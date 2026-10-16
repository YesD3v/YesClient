package com.yesclient.module.modules.visuals;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.HudTextStyle;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.HudDesignerModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetHandlerPlayClient;

 


public class PingModule extends Module {

    private final HudScale hudScale = new HudScale(this);
    private final HudTextStyle textStyle = new HudTextStyle(this, true, 0.0D, 250.0D, 1000.0D,
            0xFF55FF55, 0xFFFF5555);

    public PingModule() {
        super("Ping", "Shows your ping to the server", Category.VISUALS, ModuleMode.BOTH);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null)
            return;

        int ping = getCurrentPing();
        String value = ping < 0 ? "?" : String.valueOf(ping);

         
        HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getPingX() : 5;
        int y = designer != null ? designer.getPingY() : 112;

        hudScale.beginAt(x, y);
        if (ping < 0) {
            textStyle.drawFixedColor("Ping", value, "ms", 0xFF888888, x, y);
        } else {
            textStyle.draw("Ping", value, "ms", ping, x, y);
        }
        hudScale.end();
    }

    public static int getCurrentPing() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.getNetHandler() == null) return -1;

        NetHandlerPlayClient netHandler = mc.getNetHandler();
        NetworkPlayerInfo info = netHandler.getPlayerInfo(mc.thePlayer.getUniqueID());
        if (info == null && mc.thePlayer.getName() != null) {
             
             
            info = netHandler.getPlayerInfo(mc.thePlayer.getName());
        }
        int playerListPing = info == null ? -1 : info.getResponseTime();
        if (playerListPing > 1) return playerListPing;

        ServerData server = mc.getCurrentServerData();
        if (server != null && server.pingToServer > 1L && server.pingToServer <= Integer.MAX_VALUE) {
            return (int) server.pingToServer;
        }

         
         
        if (playerListPing == 1) return 1;
        if (server != null && server.pingToServer == 1L) return 1;
        return -1;
    }
}

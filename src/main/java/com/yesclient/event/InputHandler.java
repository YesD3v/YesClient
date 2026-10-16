package com.yesclient.event;

import com.yesclient.YesClient;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class InputHandler {

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

         
        if (mc.theWorld == null && YesClient.getInstance() != null) {
             
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
         
         
        if (event.button == 0 && event.buttonstate) {
            if (YesClient.getInstance() != null && YesClient.getInstance().getHud() != null) {
                YesClient.getInstance().getHud().registerLeftClick();
            }
        } else if (event.button == 1 && event.buttonstate) {
            if (YesClient.getInstance() != null && YesClient.getInstance().getHud() != null) {
                YesClient.getInstance().getHud().registerRightClick();
            }
        }
    }
}

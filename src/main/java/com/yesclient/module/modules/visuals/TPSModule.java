package com.yesclient.module.modules.visuals;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.HudTextStyle;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.HudDesignerModule;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

 











public class TPSModule extends Module {

    private final HudScale hudScale = new HudScale(this);
    private static final int SAMPLE_SIZE = 5;

    private final long[] intervals = new long[SAMPLE_SIZE];
    private int sampleIndex = 0;
    private int sampleCount = 0;
    private long lastPacketNanos = -1;
    private double currentTPS = 20.0;
    private final HudTextStyle textStyle = new HudTextStyle(this, true, 10.0D, 20.0D, 20.0D,
            0xFFFF5555, 0xFF55FF55);

    public TPSModule() {
        super("TPS", "Shows server TPS", Category.VISUALS, ModuleMode.BOTH);
    }

     




    public void onTimeUpdate() {
        long now = System.nanoTime();

        if (lastPacketNanos != -1) {
            long diffMs = (now - lastPacketNanos) / 1_000_000L;

             
             
             
            if (diffMs >= 50 && diffMs <= 10000) {
                intervals[sampleIndex] = diffMs;
                sampleIndex = (sampleIndex + 1) % SAMPLE_SIZE;
                if (sampleCount < SAMPLE_SIZE) {
                    sampleCount++;
                }
                recalculate();
            }
             
             
        }

        lastPacketNanos = now;
    }

    private void recalculate() {
        if (sampleCount == 0) {
            currentTPS = 20.0;
            return;
        }

        long sum = 0;
        for (int i = 0; i < sampleCount; i++) {
            sum += intervals[i];
        }
        double avgMs = (double) sum / sampleCount;

        if (avgMs > 0) {
             
             
            currentTPS = Math.min(20.0, 20000.0 / avgMs);
        }
    }

    @SubscribeEvent
    public void onWorldJoin(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        resetTPS();
    }

    @Override
    protected void onEnable() {
         
    }

     


    public void resetTPS() {
        lastPacketNanos = -1;
        currentTPS = 20.0;
        sampleIndex = 0;
        sampleCount = 0;
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            intervals[i] = 0;
        }
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

         
        HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getTpsX() : 5;
        int y = designer != null ? designer.getTpsY() : 25;

        String value = String.format("%.1f", currentTPS);
        String tpsText = textStyle.format("TPS", value, "");
        hudScale.beginAt(x, y);
        mc.fontRendererObj.drawStringWithShadow(tpsText, x, y, textStyle.getColor(currentTPS));

         
        setWidth(mc.fontRendererObj.getStringWidth(tpsText));
        setHeight(mc.fontRendererObj.FONT_HEIGHT);
        hudScale.end();
    }

    @Override
    public String getDisplayInfo() {
        return String.format("%.1f", currentTPS);
    }

    public double getCurrentTPS() {
        return currentTPS;
    }
}

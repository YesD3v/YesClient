package com.yesclient.module.modules.visuals;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.HudTextStyle;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.HudDesignerModule;
import net.minecraft.client.Minecraft;

 


public class FPSModule extends Module {

    private final HudScale hudScale = new HudScale(this);
    private final HudTextStyle textStyle = new HudTextStyle(this, true, 30.0D, 120.0D, 500.0D,
            0xFFFF5555, 0xFF55FF55);

    public FPSModule() {
        super("FPS", "Shows your frames per second", Category.VISUALS, ModuleMode.BOTH);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null)
            return;

        int fps = Minecraft.getDebugFPS();

         
        HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getFpsX() : 5;
        int y = designer != null ? designer.getFpsY() : 88;

        hudScale.beginAt(x, y);
        textStyle.draw("FPS", String.valueOf(fps), "", fps, x, y);
        hudScale.end();
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(Minecraft.getDebugFPS());
    }
}

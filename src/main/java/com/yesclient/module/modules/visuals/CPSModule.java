package com.yesclient.module.modules.visuals;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.HudTextStyle;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.HudDesignerModule;

 


public class CPSModule extends Module {

    private final HudScale hudScale = new HudScale(this);
    private final HudTextStyle textStyle = new HudTextStyle(this, true, 0.0D, 20.0D, 100.0D,
            0xFFFF5555, 0xFF55FF55);

    public CPSModule() {
        super("CPS", "Shows your clicks per second", Category.VISUALS, ModuleMode.BOTH);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null)
            return;

        int leftCPS = YesClient.getInstance().getHud().getLeftCPS();
        int rightCPS = YesClient.getInstance().getHud().getRightCPS();
        String value = leftCPS + " | " + rightCPS;

         
        HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getCpsX() : 5;
        int y = designer != null ? designer.getCpsY() : 100;

        hudScale.beginAt(x, y);
        textStyle.draw("CPS", value, "", (leftCPS + rightCPS) / 2.0D, x, y);
        hudScale.end();
    }

    @Override
    public String getDisplayInfo() {
        return null;  
    }
}

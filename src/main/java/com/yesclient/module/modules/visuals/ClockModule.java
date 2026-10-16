package com.yesclient.module.modules.visuals;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.HudTextStyle;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.HudDesignerModule;
import com.yesclient.module.settings.ModeSetting;

import java.text.SimpleDateFormat;
import java.util.Date;

 


public class ClockModule extends Module {

    private final HudScale hudScale = new HudScale(this);
    private final ModeSetting timeFormat = new ModeSetting("Format", "Time format", "24h", "24h", "12h");
    private final HudTextStyle textStyle = new HudTextStyle(this, false);

    public ClockModule() {
        super("Clock", "Shows the current time", Category.VISUALS, ModuleMode.BOTH);
        addSetting(timeFormat);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null)
            return;

         
        String pattern = timeFormat.getValue().equals("12h") ? "hh:mm:ss a" : "HH:mm:ss";
        String time = new SimpleDateFormat(pattern).format(new Date());

         
        HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getTimeX() : 5;
        int y = designer != null ? designer.getTimeY() : 5;

        hudScale.beginAt(x, y);
        textStyle.draw("Time", time, "", Double.NaN, x, y);
        hudScale.end();
    }

    @Override
    public String getDisplayInfo() {
         
         
        return timeFormat.getValue();
    }
}

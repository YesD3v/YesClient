package com.yesclient.module.modules.visuals;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.HudTextStyle;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.HudDesignerModule;

 


public class BiomeModule extends Module {

    private final HudScale hudScale = new HudScale(this);
    private final HudTextStyle textStyle = new HudTextStyle(this, true);

    public BiomeModule() {
        super("Biome", "Shows the current biome", Category.VISUALS, ModuleMode.BOTH);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        String biome = mc.theWorld.getBiomeGenForCoords(mc.thePlayer.getPosition()).biomeName;

         
        HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getBiomeX() : 5;
        int y = designer != null ? designer.getBiomeY() : 136;

        hudScale.beginAt(x, y);
        textStyle.draw("Biome", biome, "", Double.NaN, x, y);
        hudScale.end();
    }

    @Override
    public String getDisplayInfo() {
        return null;
    }
}

package com.yesclient.module.modules.visuals;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.ModeSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

 


public class WeatherModule extends Module {

    private final ModeSetting weatherMode = new ModeSetting("Weather", "Select weather type", "Clear",
            "Clear", "Rain", "Thunder");

    public WeatherModule() {
        super("Weather", "Customize rendered weather", Category.VISUALS, ModuleMode.BOTH);
        addSetting(weatherMode);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null)
            return;

        String mode = weatherMode.getValue();

        switch (mode) {
            case "Clear":
                mc.theWorld.setRainStrength(0);
                mc.theWorld.setThunderStrength(0);
                mc.theWorld.getWorldInfo().setRaining(false);
                mc.theWorld.getWorldInfo().setThundering(false);
                break;
            case "Rain":
                mc.theWorld.setRainStrength(1);
                mc.theWorld.setThunderStrength(0);
                mc.theWorld.getWorldInfo().setRaining(true);
                mc.theWorld.getWorldInfo().setThundering(false);
                break;
            case "Thunder":
                mc.theWorld.setRainStrength(1);
                mc.theWorld.setThunderStrength(1);
                mc.theWorld.getWorldInfo().setRaining(true);
                mc.theWorld.getWorldInfo().setThundering(true);
                break;
        }
    }

    @Override
    public String getDisplayInfo() {
        return weatherMode.getValue();
    }
}

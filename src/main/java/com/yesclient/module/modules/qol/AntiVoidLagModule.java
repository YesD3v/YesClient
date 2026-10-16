package com.yesclient.module.modules.qol;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;

 












public class AntiVoidLagModule extends Module {

    private static final int MAX_UPDATES_PER_TICK = 100;

     
    private int updatesThisTick = 0;
    private long lastTickTime = 0;

    public AntiVoidLagModule() {
        super("Anti Void Lag", "Prevents lag when void holes are filled/broken", Category.QOL, ModuleMode.BOTH);
    }

    @Override
    public void onTick() {
         
        long now = System.currentTimeMillis();
        if (now - lastTickTime > 50) {  
            updatesThisTick = 0;
            lastTickTime = now;
        }
    }

     








    public boolean shouldSkipLightUpdate() {
        if (!isEnabled()) {
            return false;
        }

        updatesThisTick++;

         
        if (updatesThisTick > MAX_UPDATES_PER_TICK) {
            return true;
        }

        return false;
    }
}

package com.yesclient.module.modules.movement;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.NumberSetting;

public class SafeWalkModule extends Module {
    
    public final NumberSetting threshold = new NumberSetting("Block Threshold", "Fall if drop is less than this", 2.0, 1.0, 10.0, 1.0);

    public SafeWalkModule() {
        super("SafeWalk", "Prevents walking off edges", Category.MOVEMENT, ModuleMode.BOTH);
        addSetting(threshold);
    }
}

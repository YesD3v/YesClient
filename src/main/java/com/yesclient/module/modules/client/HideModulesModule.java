package com.yesclient.module.modules.client;
import java.util.Locale;
import java.util.Map;
import java.util.List;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;

import java.util.LinkedHashMap;

 



public class HideModulesModule extends Module {

    private static HideModulesModule instance;

    private final Map<String, BooleanSetting> hiddenModuleSettings = new LinkedHashMap<>();

    public HideModulesModule() {
        super("Hide Modules", "Choose enabled modules to hide from the on-screen module list",
                Category.CLIENT, ModuleMode.BOTH);
        instance = this;
    }

     



    public void initializeModuleSettings(List<Module> modules) {
        if (!hiddenModuleSettings.isEmpty()) {
            return;
        }

        for (Module module : modules) {
            String name = module.getName();
            if (isAlwaysExcludedFromModuleList(name)) {
                continue;
            }

            BooleanSetting setting = new BooleanSetting(
                    name,
                    "Hide " + name + " from the on-screen module list",
                    false);
            hiddenModuleSettings.put(normalize(name), setting);
            addSetting(setting);
        }
    }

    public static boolean shouldHide(Module module) {
        if (module == null || instance == null || !instance.isEnabled()) {
            return false;
        }

        BooleanSetting setting = instance.hiddenModuleSettings.get(normalize(module.getName()));
        return setting != null && setting.isEnabled();
    }

    private static boolean isAlwaysExcludedFromModuleList(String name) {
        return "Module List".equalsIgnoreCase(name)
                || "ClickGUI".equalsIgnoreCase(name)
                || "HUD Designer".equalsIgnoreCase(name);
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}

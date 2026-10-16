package com.yesclient.module;
import java.util.Comparator;
import java.util.List;
import java.nio.charset.StandardCharsets;

import com.yesclient.YesClient;
import com.yesclient.module.modules.building.*;
import com.yesclient.module.modules.client.*;
import com.yesclient.module.modules.combat.*;
import com.yesclient.module.modules.freebuild.*;
import com.yesclient.module.modules.movement.*;
import com.yesclient.module.modules.visuals.*;
import com.yesclient.module.modules.render.*;
import com.yesclient.module.modules.items.*;
import com.yesclient.module.modules.exploit.*;

import com.yesclient.module.modules.moderation.*;
import com.yesclient.module.modules.miscellaneous.*;

import com.yesclient.module.modules.qol.*;
import com.yesclient.module.settings.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<Module>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File modulesFile;
     
     
     
    private final ClickGUIModule clientSettings = new ClickGUIModule();

    public ModuleManager() {
        modulesFile = new File(YesClient.getInstance().getDataDir(), "modules.json");
        registerModules();
        loadModuleStates();
        if (!clientSettings.isBlatantModeEnabled()) {
            ProjectileAimbotModule aimbot = getModule(ProjectileAimbotModule.class);
            if (aimbot != null) {
                aimbot.setAssistMode();
            }
        }
    }

    private void registerModules() {
         
        modules.add(new FlyModule());
        modules.add(new SprintModule());
        modules.add(new SafeWalkModule());
        modules.add(new SpeedModule());

         
        modules.add(new com.yesclient.module.modules.visuals.CrosshairModule());
        modules.add(new FullbrightModule());
        modules.add(new FreeCamModule());
        modules.add(new FreeLookModule());
        modules.add(new TrueSightModule());
        modules.add(new NametagsModule());  
        modules.add(new StorageESPModule());
        modules.add(new ESPModule());
        modules.add(new ZoomModule());
        modules.add(new TracersModule());
        modules.add(new SearchModule());
         
        modules.add(new LoadedPlayersModule());
        modules.add(new RainbowArmorModule());
        modules.add(new TPSModule());
        modules.add(new HideEntitiesModule());
        modules.add(new CPSModule());
        modules.add(new FPSModule());
        modules.add(new PingModule());
        modules.add(new CoordsModule());
        modules.add(new DirectionModule());
        modules.add(new BiomeModule());
        modules.add(new ClockModule());
        modules.add(new MediaOverlayModule());
        modules.add(new ScoreboardModule());
        modules.add(new ActiveEffectsModule());
        modules.add(new WeatherModule());
        modules.add(new NickHiderModule());
        modules.add(new ItemDisguiserModule());
        modules.add(new MailboxESPModule());

         
        modules.add(new HideHykiaEntitiesModule());

         
        modules.add(new GrieferDetectorModule());
        modules.add(new NickDetectorModule());
        modules.add(new com.yesclient.module.modules.moderation.PatternDetectionModule());

         
        modules.add(new AutoclickerModule());
        
        modules.add(new ReachModule());
        modules.add(new NoDebuffModule());

         
        modules.add(new FastPlaceModule());
        modules.add(new GhostBlocksModule());
         
        modules.add(new NukerModule());
        modules.add(new FastBreakModule());

         
        modules.add(new AntiVoidLagModule());

         
        modules.add(new HUDModule());
        modules.add(new DiscordRPCModule());
        HideModulesModule hideModules = new HideModulesModule();
        modules.add(hideModules);
        modules.add(new FriendsModule());
        modules.add(new HudDesignerModule());
        modules.add(new ChatModule());
        
        modules.add(new CrasherDetectorModule());
        modules.add(new FancyTextModule());

        modules.add(new ItemStealerModule());

         
         

         
        modules.add(new NBTLoggerModule());
        modules.add(new PlayerCrasherModule());
        modules.add(new ServerMatcherModule());
        modules.add(new BlinkModule());
        modules.add(new BypassBlacklistModule());
        modules.add(new ProjectileAimbotModule());
        modules.add(new BlatantFlyModule());
        modules.add(new SilentNukerModule());
        modules.add(new CustomAuthorModule());
        modules.add(new WearableItemsModule());

        modules.add(new SignFillModule());
        modules.add(new ImageToNBTModule());
        modules.add(new GhostDiscModule());
        modules.add(new PacketMultiplierModule());

         
        modules.add(new AutoBegModule());
        modules.add(new ChestStealerModule());
        modules.add(new CommandCheckerModule());
        modules.add(new GuiMoveModule());
        modules.add(new InvTweaksModule());

        
        hideModules.initializeModuleSettings(modules);

        YesClient.LOGGER.info("Registered " + modules.size() + " modules.");
    }

    public List<Module> getModules() {
        return modules;
    }

    public Module getModule(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public Module getModuleByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        for (Module module : modules) {
            if (module.getName().toLowerCase().contains(name.toLowerCase())) {
                return module;
            }
        }
        String noSpaces = name.replace(" ", "").toLowerCase();
        for (Module module : modules) {
            if (module.getName().replace(" ", "").toLowerCase().equals(noSpaces)) {
                return module;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        if (clazz.isInstance(clientSettings)) {
            return (T) clientSettings;
        }
        for (Module module : modules) {
            if (clazz.isInstance(module)) {
                return (T) module;
            }
        }
        return null;
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());
    }

    public List<Module> getEnabledModules() {
        return modules.stream()
                .filter(Module::isEnabled)
                .collect(Collectors.toList());
    }

    public List<Module> getVisibleModules() {
        return modules.stream()
                .filter(Module::isVisible)
                .filter(Module::isEnabled)
                .collect(Collectors.toList());
    }

    public List<Module> getVisibleModulesSorted() {
        return modules.stream()
                .filter(Module::isVisible)
                .filter(Module::isEnabled)
                .sorted(Comparator.comparingInt(m -> -m.getName().length()))
                .collect(Collectors.toList());
    }

    public void disableAll() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.setEnabled(false);
            }
        }
    }

    public void saveModuleStates() {
        try {
            JsonObject json = new JsonObject();

            for (Module module : modules) {
                writeModuleState(json, module);
            }
             
             
            writeModuleState(json, clientSettings);

             
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(modulesFile),
                    StandardCharsets.UTF_8)) {
                gson.toJson(json, writer);
            }
        } catch (IOException e) {
            YesClient.LOGGER.error("Failed to save module states", e);
        }
    }

    private void writeModuleState(JsonObject json, Module module) {
        JsonObject moduleJson = new JsonObject();
        moduleJson.addProperty("enabled", module.isEnabled());
        moduleJson.addProperty("keybind", module.getKeybind());
        moduleJson.addProperty("visible", module.isVisible());

        JsonObject settingsJson = new JsonObject();
        for (Setting<?> setting : module.getSettings()) {
            settingsJson.add(setting.getName(), setting.toJson());
        }
        moduleJson.add("settings", settingsJson);
        json.add(module.getName(), moduleJson);
    }

    public void loadModuleStates() {
        if (!modulesFile.exists()) {
             
            for (Module module : modules) {
                if (module instanceof com.yesclient.module.modules.client.ChatModule) {
                    module.setEnabled(true);
                    break;
                }
            }
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(modulesFile),
                StandardCharsets.UTF_8)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json == null) {
                 
                for (Module module : modules) {
                    if (module instanceof com.yesclient.module.modules.client.ChatModule) {
                        module.setEnabled(true);
                        break;
                    }
                }
                return;
            }

            boolean chatFound = false;

            for (Module module : modules) {
                String savedModuleName = module.getName();
                if (module instanceof ItemDisguiserModule
                        && !json.has(savedModuleName)
                        && json.has("Item Hider")) {
                    savedModuleName = "Item Hider";
                }
                if (module instanceof MailboxESPModule
                        && !json.has(savedModuleName)
                        && json.has("Mailbox Spammer")) {
                    savedModuleName = "Mailbox Spammer";
                }
                if (json.has(savedModuleName)) {
                    JsonObject moduleJson = json.getAsJsonObject(savedModuleName);

                    if (module instanceof com.yesclient.module.modules.client.ChatModule) {
                        chatFound = true;
                    }

                    if (moduleJson.has("keybind")) {
                        module.setKeybind(moduleJson.get("keybind").getAsInt());
                    }
                    if (moduleJson.has("visible")) {
                        module.setVisible(moduleJson.get("visible").getAsBoolean());
                    }

                    if (moduleJson.has("settings")) {
                        JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                        for (Setting<?> setting : module.getSettings()) {
                            if (settingsJson.has(setting.getName())) {
                                setting.fromJson(settingsJson.get(setting.getName()));
                            }
                        }
                    }

                    if (moduleJson.has("enabled") && moduleJson.get("enabled").getAsBoolean()) {
                        module.setEnabled(true);
                    }
                }
            }

             
             
             
            if (json.has(clientSettings.getName())) {
                JsonObject settingsModule = json.getAsJsonObject(clientSettings.getName());
                if (settingsModule.has("settings")) {
                    JsonObject settingsJson = settingsModule.getAsJsonObject("settings");
                    for (Setting<?> setting : clientSettings.getSettings()) {
                        if (settingsJson.has(setting.getName())) {
                            setting.fromJson(settingsJson.get(setting.getName()));
                        } else if ("Watermark Size".equals(setting.getName())) {
                            if (settingsJson.has("HUD Scale")) {
                                setting.fromJson(settingsJson.get("HUD Scale"));
                            } else if (settingsJson.has("HUD Size")) {
                                setting.fromJson(settingsJson.get("HUD Size"));
                            }
                        } else if ("Click Sounds".equals(setting.getName()) && settingsJson.has("Sounds")) {
                             
                             
                            setting.fromJson(new com.google.gson.JsonPrimitive(true));
                        }
                    }
                }
            }

             
            if (!chatFound) {
                for (Module module : modules) {
                    if (module instanceof com.yesclient.module.modules.client.ChatModule) {
                        module.setEnabled(true);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            YesClient.LOGGER.error("Failed to load module states", e);
        }
    }

    public void onTick() {
        boolean safeMode = YesClient.getInstance().isSafeMode();
        clientSettings.onTick();
        for (Module module : modules) {
            if (safeMode && module.isBlatant() && module.isEnabled()) {
                module.setEnabled(false);
            }
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }

    public void onRender() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onRender();
            }
        }
    }

    public void onRender3D(float partialTicks) {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onRender3D(partialTicks);
            }
        }
    }

     



    public void onRender3DPreBobbing(float partialTicks) {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onRender3DPreBobbing(partialTicks);
            }
        }
    }

    public void onUpdate(net.minecraft.entity.EntityLivingBase entity) {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onUpdate(entity);
            }
        }
    }

    public void onJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onJump(event);
            }
        }
    }
}

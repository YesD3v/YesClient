package com.yesclient;
import java.util.Map;
import java.lang.reflect.Field;

import com.yesclient.config.ConfigManager;
import com.yesclient.config.ProfileManager;
import com.yesclient.gui.ClickGUI;
import com.yesclient.gui.HUD;
import com.yesclient.itemlog.ItemLogManager;
import com.yesclient.storage.ItemLogTab;
import com.yesclient.module.ModuleManager;
import com.yesclient.module.modules.client.ClickGUIModule;
import com.yesclient.storage.CreativeTabStorage;
import com.yesclient.storage.ItemStealerStorage;
import com.yesclient.storage.SavedHotbarStorage;
import com.yesclient.storage.SavedHotbarTab;

import com.yesclient.utils.HousingDetector;
import com.yesclient.utils.KeybindManager;
import com.yesclient.event.ChatSendHandler;
import com.yesclient.event.InputHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import com.yesclient.command.CommandManager;
import com.yesclient.command.ChatCommandHandler;
import com.yesclient.event.EventManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.File;

@Mod(modid = YesClient.MODID, name = YesClient.NAME, version = YesClient.VERSION, clientSideOnly = true)
public class YesClient {

    public static final String MODID = "yesclient";
    public static final String NAME = "YesClient";
    public static final String VERSION = "1.2.1";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static YesClient instance;

    private static Minecraft mc;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private ProfileManager profileManager;
    private KeybindManager keybindManager;
    private CommandManager commandManager;
    private EventManager eventManager;
    private CreativeTabStorage creativeTabStorage;
    private ItemStealerStorage itemStealerStorage;
    private SavedHotbarStorage savedHotbarStorage;
    private ItemLogManager itemLogManager;

    private HousingDetector housingDetector;
    private ClickGUI clickGUI;
    private HUD hud;

     
    private KeyBinding guiKeyBinding;

    private File dataDir;
    private boolean safeMode = false;
    private boolean isOwnerMode = false;
    private boolean shutdownSaved = false;

     
    private String lastChatInput = "";
    private int tickCounter = 0;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        mc = Minecraft.getMinecraft();
        dataDir = new File(mc.mcDataDir, "yesclient");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        LOGGER.info("===========================================");
        LOGGER.info("YesClient v" + VERSION + " - Pre-Init");
         
        configManager = new ConfigManager(dataDir);
        configManager.loadConfig();
        LOGGER.info("YesClient data directory: {}", dataDir.getAbsolutePath());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("YesClient - Initializing...");

         
        guiKeyBinding = new KeyBinding("Open YesClient GUI", Keyboard.KEY_RSHIFT, "YesClient");
        ClientRegistry.registerKeyBinding(guiKeyBinding);

         
        eventManager = new EventManager();
        moduleManager = new ModuleManager();
        profileManager = new ProfileManager(dataDir);
        keybindManager = new KeybindManager();
        commandManager = new CommandManager();
        creativeTabStorage = new CreativeTabStorage(dataDir);
        itemStealerStorage = new ItemStealerStorage(dataDir);
        itemStealerStorage.load();
        savedHotbarStorage = new SavedHotbarStorage(dataDir);
        savedHotbarStorage.load();
        itemLogManager = new ItemLogManager();
        new ItemLogTab();
        new com.yesclient.storage.ItemStealerTab();  
        new SavedHotbarTab();
        housingDetector = new HousingDetector();

         
        com.yesclient.utils.font.FontManager.getInstance().loadFonts();

         
        clickGUI = new ClickGUI();
        hud = new HUD();

         
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(eventManager);

        MinecraftForge.EVENT_BUS.register(keybindManager);
        MinecraftForge.EVENT_BUS.register(housingDetector);
        MinecraftForge.EVENT_BUS.register(hud);
        MinecraftForge.EVENT_BUS.register(new InputHandler());  
        MinecraftForge.EVENT_BUS.register(new ChatSendHandler());
        MinecraftForge.EVENT_BUS.register(new com.yesclient.event.BanDetectionHandler());
         
        profileManager.loadProfiles();
        creativeTabStorage.load();
        keybindManager.loadKeybinds();

        LOGGER.info("YesClient - Initialization complete!");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("===========================================");
        LOGGER.info("YesClient loaded successfully!");
        if (moduleManager != null) {
            LOGGER.info("Loaded " + moduleManager.getModules().size() + " modules");
        } else {
            LOGGER.warn("ModuleManager is null — init phase may have been skipped!");
        }
        LOGGER.info("Press RIGHT SHIFT to open the GUI");
        LOGGER.info("Use . commands in chat (e.g. .help)");
        LOGGER.info("===========================================");

         
        injectTranslations();

        LOGGER.info("--- Creative Tabs Dump ---");
        for (net.minecraft.creativetab.CreativeTabs tab : net.minecraft.creativetab.CreativeTabs.creativeTabArray) {
            if (tab != null) {
                LOGGER.info("Index: " + tab.getTabIndex() + " Label: " + tab.getTabLabel() + " Class: "
                        + tab.getClass().getName());
            } else {
                LOGGER.info("Index: NULL SLOT");
            }
        }
        LOGGER.info("--------------------------");
    }

     




    private void injectTranslations() {
        try {
             
            Field localeField;
            try {
                localeField = net.minecraft.client.resources.I18n.class.getDeclaredField("i18nLocale");
            } catch (NoSuchFieldException e) {
                 
                localeField = net.minecraft.client.resources.I18n.class.getDeclaredField("field_135054_a");
            }
            localeField.setAccessible(true);
            net.minecraft.client.resources.Locale locale = (net.minecraft.client.resources.Locale) localeField
                    .get(null);

            if (locale == null) {
                LOGGER.warn("I18n locale is null, translations not injected");
                return;
            }

             
            Field propsField = null;
            for (Field f : net.minecraft.client.resources.Locale.class.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) {
                    propsField = f;
                    break;
                }
            }

            if (propsField != null) {
                propsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, String> langMap = (Map<String, String>) propsField.get(locale);

                if (langMap != null) {
                     
                    langMap.put("itemGroup.yesclient_nbt_logger", "NBT Logger");
                    langMap.put("itemGroup.itemstealer", "Item Stealer");
                    langMap.put("itemGroup.savedhotbars", "Saved Hotbars");

                    LOGGER.info("Injected creative tab translations successfully!");
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to inject translations: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKeyState() && savedHotbarStorage != null
                && savedHotbarStorage.handleCreativeKeyPress(Keyboard.getEventKey())) {
            return;
        }

        if (mc.theWorld == null || mc.thePlayer == null)
            return;

         
        if (guiKeyBinding.isPressed()) {
            clickGUI.reset();
            mc.displayGuiScreen(clickGUI);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

         
        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;
            if (mc.thePlayer != null) {
                com.yesclient.utils.YesClientUserManager.getInstance().updateRainbowNameCache();
            }
        }

         
         
        if (mc.getNetHandler() instanceof net.minecraft.client.network.NetHandlerPlayClient) {
            while (!com.yesclient.utils.BungeePacketQueue.delayPacketQueue.isEmpty()) {
                net.minecraft.network.Packet<?> p = com.yesclient.utils.BungeePacketQueue.delayPacketQueue.poll();
                try {
                    @SuppressWarnings("unchecked")
                    net.minecraft.network.Packet<net.minecraft.network.INetHandler> rawPacket = (net.minecraft.network.Packet<net.minecraft.network.INetHandler>) p;
                    rawPacket.processPacket(mc.getNetHandler());
                } catch (Exception e) {
                    LOGGER.error("[YesClient] Error processing BungeeCord queued packet: ", e);
                }
            }
        }

        if (mc.thePlayer == null)
            return;

         
        moduleManager.onTick();

         
        if (mc.currentScreen instanceof GuiChat) {
            try {

                Field inputField = GuiChat.class.getDeclaredField("inputField");
                inputField.setAccessible(true);
                net.minecraft.client.gui.GuiTextField textField = (net.minecraft.client.gui.GuiTextField) inputField
                        .get(mc.currentScreen);

                if (textField != null) {
                    lastChatInput = textField.getText();
                }
            } catch (Exception e) {
                 
                try {
                    Field inputField = GuiChat.class.getDeclaredField("field_146415_a");
                    inputField.setAccessible(true);
                    net.minecraft.client.gui.GuiTextField textField = (net.minecraft.client.gui.GuiTextField) inputField
                            .get(mc.currentScreen);

                    if (textField != null) {
                        lastChatInput = textField.getText();
                    }
                } catch (Exception ex) {
                     
                }
            }
        }
    }

     




    public boolean onChatMessage(String message) {
        if (ChatCommandHandler.isCommand(message)) {
            return ChatCommandHandler.handleChatMessage(message);
        }
        return false;
    }

    public static Minecraft getMinecraft() {
        return mc;
    }

    public static YesClient getInstance() {
        return instance;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }


    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public KeybindManager getKeybindManager() {
        return keybindManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public CreativeTabStorage getCreativeTabStorage() {
        return creativeTabStorage;
    }

    public ItemStealerStorage getItemStealerStorage() {
        return itemStealerStorage;
    }

    public SavedHotbarStorage getSavedHotbarStorage() {
        return savedHotbarStorage;
    }

    public HousingDetector getHousingDetector() {
        return housingDetector;
    }

    public ClickGUI getClickGUI() {
        return clickGUI;
    }

    public HUD getHud() {
        return hud;
    }

    public File getDataDir() {
        return dataDir;
    }

    public boolean isSafeMode() {
        if (moduleManager == null)
            return false;
        ClickGUIModule mod = moduleManager.getModule(ClickGUIModule.class);
        return mod != null && !mod.isBlatantModeEnabled();
    }

     
     
    @Deprecated
    public void setSafeMode(boolean safeMode) {
         
         
    }

    public void toggleSafeMode() {
         
    }

    public boolean isOwnerMode() {
        return isOwnerMode;
    }

    public void setOwnerMode(boolean ownerMode) {
        this.isOwnerMode = ownerMode;
    }

    public void saveAll() {
        if (configManager != null) configManager.saveConfig();
        if (profileManager != null) profileManager.saveProfiles();
        if (creativeTabStorage != null) creativeTabStorage.save();
        if (savedHotbarStorage != null) savedHotbarStorage.save();
        if (keybindManager != null) keybindManager.saveKeybinds();
        if (moduleManager != null) moduleManager.saveModuleStates();
    }

    public void onShutdown() {
        if (shutdownSaved) return;
        shutdownSaved = true;

        if (moduleManager != null) {
            com.yesclient.module.modules.visuals.FullbrightModule fullbright =
                    moduleManager.getModule(com.yesclient.module.modules.visuals.FullbrightModule.class);
            if (fullbright != null) fullbright.restoreGameSettingsForShutdown();

            com.yesclient.module.modules.visuals.FreeLookModule freeLook =
                    moduleManager.getModule(com.yesclient.module.modules.visuals.FreeLookModule.class);
            if (freeLook != null) freeLook.restoreGameSettingsForShutdown();

            com.yesclient.module.modules.qol.ZoomModule zoom =
                    moduleManager.getModule(com.yesclient.module.modules.qol.ZoomModule.class);
            if (zoom != null) zoom.restoreGameSettingsForShutdown();
        }
        saveAll();
        LOGGER.info("YesClient shutdown complete.");
    }
}

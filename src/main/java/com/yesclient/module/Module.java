package com.yesclient.module;
import java.util.List;

import com.yesclient.YesClient;

import com.yesclient.module.settings.Setting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;

public abstract class Module {

    protected static final Minecraft mc = Minecraft.getMinecraft();

    private final String name;
    private final String description;
    private final Category category;
    private final ModuleMode mode;
    private boolean enabled;
    private int keybind;
    private final int defaultKeybind;
    private boolean visible = true;
    private boolean isBlatant = false;

    private net.minecraft.util.ResourceLocation icon;
     
    private int width = 0;
    private int height = 0;

    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String description, Category category, ModuleMode mode) {
        this(name, description, category, mode, 0);
    }

    public Module(String name, String description, Category category, ModuleMode mode, int defaultKeybind) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.mode = mode;
        this.enabled = false;
        this.keybind = defaultKeybind;
        this.defaultKeybind = defaultKeybind;
        
        try {
            String iconName = name.toLowerCase(java.util.Locale.ROOT).replace(" ", "");
            iconName = getIconAlias(iconName);
            net.minecraft.util.ResourceLocation loc = new net.minecraft.util.ResourceLocation("yesclient", "textures/icons/" + iconName + ".png");
            Minecraft.getMinecraft().getResourceManager().getResource(loc);
            this.icon = loc;
        } catch (Exception e) {
            this.icon = new net.minecraft.util.ResourceLocation("yesclient", "textures/icons/generic.png");
        }
    }

    private static String getIconAlias(String iconName) {
        switch (iconName) {
            case "clickgui": return "huddesigner";
            case "uniquecrashitem": return "itemduper";
            case "crashegg": return "itemduper";
            case "loreeditor": return "nbteditor";
            case "rename": return "fancytext";
            case "unbreakable": return "yesarmor";
            case "clip": return "freecam";
            case "sandnoclip": return "ghostblocks";
            case "scaffold": return "blocks";
            default: return iconName;
        }
    }

    public int getDefaultKeybind() {
        return defaultKeybind;
    }

    public void toggle() {
        boolean wasEnabled = enabled;
        setEnabled(!wasEnabled);
        if (enabled != wasEnabled && !"ClickGUI".equalsIgnoreCase(name)
                && com.yesclient.utils.InterfaceSounds.isClientUiScreen()) {
            com.yesclient.utils.InterfaceSounds.playModuleToggle(enabled);
        }
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled)
            return;

         
        if (enabled && isBlatant() && YesClient.getInstance().isSafeMode()) {
            return;  
        }

        this.enabled = enabled;

         
        if (mc.theWorld != null && shouldShowNotification()) {
            com.yesclient.module.modules.client.ClickGUIModule clickGui = YesClient.getInstance()
                    .getModuleManager().getModule(
                            com.yesclient.module.modules.client.ClickGUIModule.class);
            if (clickGui != null && clickGui.isNotificationsEnabled()) {
                com.yesclient.gui.ToggleNotification.addNotification(this, enabled);
            }
        }

        if (enabled) {
            onEnable();
            MinecraftForge.EVENT_BUS.register(this);
        } else {
            MinecraftForge.EVENT_BUS.unregister(this);
            onDisable();
        }
    }

    public boolean isAvailable() {
        return true;
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public void onTick() {
    }

    public void onUpdate(net.minecraft.entity.EntityLivingBase entity) {
    }

    public void onJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
    }

    public void onRender() {
    }

    public void onRender3D(float partialTicks) {
    }

     





    public void onRender3DPreBobbing(float partialTicks) {
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return name;
    }

    public net.minecraft.util.ResourceLocation getIcon() {
        return icon;
    }

    public Category getCategory() {
        return category;
    }

    public ModuleMode getMode() {
        return mode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getKeybind() {
        return keybind;
    }

    public void setKeybind(int keybind) {
        this.keybind = keybind;
         
        if (YesClient.getInstance() != null && YesClient.getInstance().getKeybindManager() != null) {
            YesClient.getInstance().getKeybindManager().saveKeybinds();
        }
    }

     


    public boolean allowsModuleKeybind() {
        return true;
    }

     




    public boolean hasPriorityKeybind(int key) {
        return false;
    }

     


    public String getKeybindDisplay() {
        if (getKeybind() == 0) {
            return "[None]";
        }
        return "[" + com.yesclient.utils.KeybindManager.getKeyName(getKeybind()) + "]";
    }

     




    public boolean handleKeybindPress(int key) {
        if (key != 0 && getKeybind() == key) {
            toggle();
            return true;
        }
        return false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    protected <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public Setting<?> getSetting(String name) {
        for (Setting<?> setting : settings) {
            if (setting.getName().equalsIgnoreCase(name)) {
                return setting;
            }
        }
        return null;
    }

    public String getDisplayInfo() {
        return null;
    }

    public boolean isBlatant() {
        return isBlatant;
    }

    public void setBlatant(boolean blatant) {
        this.isBlatant = blatant;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    protected boolean shouldShowNotification() {
        return true;
    }
}

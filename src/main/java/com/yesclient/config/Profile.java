package com.yesclient.config;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class Profile {
    
    private final String name;
    private String iconItem = "minecraft:flint";
    private boolean iconGlint = false;
    private final Map<String, Boolean> moduleStates = new HashMap<>();
    private final Map<String, JsonObject> moduleSettings = new HashMap<>();
    
    private final Map<String, Integer> moduleKeybinds = new HashMap<>();
    
    public Profile(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public String getIconItem() {
        return iconItem;
    }
    
    public void setIconItem(String iconItem) {
        this.iconItem = iconItem;
    }
    
    public boolean hasIconGlint() {
        return iconGlint;
    }
    
    public void setIconGlint(boolean iconGlint) {
        this.iconGlint = iconGlint;
    }
    
    public Boolean getModuleState(String moduleName) {
        return moduleStates.get(moduleName);
    }
    
    public void setModuleState(String moduleName, boolean enabled) {
        moduleStates.put(moduleName, enabled);
    }
    
    public Integer getModuleKeybind(String moduleName) {
        return moduleKeybinds.get(moduleName);
    }
    
    public void setModuleKeybind(String moduleName, int keybind) {
        moduleKeybinds.put(moduleName, keybind);
    }
    
    public JsonObject getModuleSettings(String moduleName) {
        return moduleSettings.get(moduleName);
    }
    
    public void setModuleSettings(String moduleName, JsonObject settings) {
        moduleSettings.put(moduleName, settings);
    }
    
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("iconItem", iconItem);
        json.addProperty("iconGlint", iconGlint);
        
        JsonObject states = new JsonObject();
        for (Map.Entry<String, Boolean> entry : moduleStates.entrySet()) {
            states.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("moduleStates", states);
        
        JsonObject keybinds = new JsonObject();
        for (Map.Entry<String, Integer> entry : moduleKeybinds.entrySet()) {
            keybinds.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("moduleKeybinds", keybinds);
        
        JsonObject settings = new JsonObject();
        for (Map.Entry<String, JsonObject> entry : moduleSettings.entrySet()) {
            settings.add(entry.getKey(), entry.getValue());
        }
        json.add("moduleSettings", settings);
        
        return json;
    }
    
    public static Profile fromJson(JsonObject json) {
        String name = json.has("name") ? json.get("name").getAsString() : "Unknown";
        Profile profile = new Profile(name);
        
        if (json.has("iconItem")) profile.setIconItem(json.get("iconItem").getAsString());
        if (json.has("iconGlint")) profile.setIconGlint(json.get("iconGlint").getAsBoolean());
        
        if (json.has("moduleStates")) {
            JsonObject states = json.getAsJsonObject("moduleStates");
            for (Map.Entry<String, JsonElement> entry : states.entrySet()) {
                profile.setModuleState(entry.getKey(), entry.getValue().getAsBoolean());
            }
        }
        
        if (json.has("moduleKeybinds")) {
            JsonObject keybinds = json.getAsJsonObject("moduleKeybinds");
            for (Map.Entry<String, JsonElement> entry : keybinds.entrySet()) {
                profile.setModuleKeybind(entry.getKey(), entry.getValue().getAsInt());
            }
        }
        
        if (json.has("moduleSettings")) {
            JsonObject settings = json.getAsJsonObject("moduleSettings");
            for (Map.Entry<String, JsonElement> entry : settings.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    profile.setModuleSettings(entry.getKey(), entry.getValue().getAsJsonObject());
                }
            }
        }
        
        return profile;
    }
}


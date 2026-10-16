package com.yesclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.yesclient.YesClient;
import com.yesclient.module.Module;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ProfileManager {
    
    private final File profilesDir;
    private final File activeProfileFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private final Map<String, Profile> profiles = new LinkedHashMap<>();
    private String activeProfile = "Default";
    
    public ProfileManager(File dataDir) {
        this.profilesDir = new File(dataDir, "profiles");
        this.activeProfileFile = new File(profilesDir, "active-profile.txt");
        if (!profilesDir.exists()) {
            profilesDir.mkdirs();
        }
    }
    
    public void loadProfiles() {
        profiles.clear();
        String savedActiveProfile = readActiveProfile();
        
        File[] files = profilesDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject json = gson.fromJson(reader, JsonObject.class);
                    if (json != null) {
                        Profile profile = Profile.fromJson(json);
                        profiles.put(profile.getName(), profile);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        if (profiles.isEmpty()) {
            Profile defaultProfile = new Profile("Default");
            profiles.put("Default", defaultProfile);
            activeProfile = "Default";
            saveCurrentToProfile();
        }
        
        if (savedActiveProfile != null && profiles.containsKey(savedActiveProfile)) {
            activeProfile = savedActiveProfile;
            applyProfile(profiles.get(activeProfile));
        } else {
            Profile recoveredProfile = findBestMatchingLoadedProfile();
            if (recoveredProfile != null) {
                 
                 
                 
                activeProfile = recoveredProfile.getName();
                applyProfile(recoveredProfile);
            } else {
                if (!profiles.containsKey(activeProfile) && !profiles.isEmpty()) {
                    activeProfile = profiles.containsKey("Default") ? "Default" : profiles.keySet().iterator().next();
                }
                 
                 
                saveCurrentToProfile();
            }
        }
        saveActiveProfile();
    }

     




    private Profile findBestMatchingLoadedProfile() {
        if (profiles.isEmpty() || YesClient.getInstance() == null
                || YesClient.getInstance().getModuleManager() == null) return null;

        Profile best = null;
        int bestMatches = -1;
        int bestComparisons = 0;
        double bestScore = -1.0D;
        for (Profile profile : profiles.values()) {
            int matches = 0;
            int comparisons = 0;
            for (Module module : YesClient.getInstance().getModuleManager().getModules()) {
                Boolean savedState = profile.getModuleState(module.getName());
                if (savedState != null) {
                    comparisons++;
                    if (savedState.booleanValue() == module.isEnabled()) matches++;
                }

                Integer savedKeybind = profile.getModuleKeybind(module.getName());
                if (savedKeybind != null) {
                    comparisons++;
                    if (savedKeybind.intValue() == module.getKeybind()) matches++;
                }

                JsonObject savedSettings = profile.getModuleSettings(module.getName());
                if (savedSettings != null) {
                    for (com.yesclient.module.settings.Setting<?> setting : module.getSettings()) {
                        if (savedSettings.has(setting.getName())) {
                            comparisons++;
                            if (setting.toJson().equals(savedSettings.get(setting.getName()))) matches++;
                        }
                    }
                }
            }

            double score = comparisons == 0 ? 0.0D : matches / (double) comparisons;
            if (score > bestScore || (score == bestScore && comparisons > bestComparisons)) {
                best = profile;
                bestMatches = matches;
                bestComparisons = comparisons;
                bestScore = score;
            }
        }

         
         
        int minimumComparisons = Math.max(20, YesClient.getInstance().getModuleManager().getModules().size());
        if (best == null || bestComparisons < minimumComparisons
                || bestMatches / (double) bestComparisons < 0.75D) return null;
        return best;
    }
    
    public void saveProfiles() {
        saveCurrentToProfile();
        for (Profile profile : profiles.values()) {
            saveProfile(profile);
        }
        saveActiveProfile();
    }
    
    private void saveProfile(Profile profile) {
        File profileFile = new File(profilesDir, profile.getName().toLowerCase() + ".json");
        try (FileWriter writer = new FileWriter(profileFile)) {
            gson.toJson(profile.toJson(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readActiveProfile() {
        if (!activeProfileFile.isFile()) return null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(activeProfileFile), StandardCharsets.UTF_8))) {
            String name = reader.readLine();
            return name == null || name.trim().isEmpty() ? null : name.trim();
        } catch (IOException e) {
            YesClient.LOGGER.error("Failed to load active YesClient profile", e);
            return null;
        }
    }

    private void saveActiveProfile() {
        try {
            if (!profilesDir.exists()) profilesDir.mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(activeProfileFile), StandardCharsets.UTF_8)) {
                writer.write(activeProfile);
                writer.write(System.lineSeparator());
            }
        } catch (IOException e) {
            YesClient.LOGGER.error("Failed to save active YesClient profile", e);
        }
    }
    
    public void applyProfile(Profile profile) {
        for (Module module : YesClient.getInstance().getModuleManager().getModules()) {
            Boolean state = profile.getModuleState(module.getName());
            boolean shouldBeEnabled;
            
            if (state != null) {
                shouldBeEnabled = state;
            } else {
                shouldBeEnabled = module.getName().equals("Module List") || module.getName().equals("Discord RPC");
            }
            
            if (shouldBeEnabled != module.isEnabled()) {
                module.setEnabled(shouldBeEnabled);
            }
            
            Integer keybind = profile.getModuleKeybind(module.getName());
            if (keybind != null) {
                module.setKeybind(keybind);
            } else {
                module.setKeybind(module.getDefaultKeybind());
            }
            
            JsonObject settingsJson = profile.getModuleSettings(module.getName());
            if (settingsJson != null) {
                for (com.yesclient.module.settings.Setting<?> setting : module.getSettings()) {
                    if (settingsJson.has(setting.getName())) {
                        setting.fromJson(settingsJson.get(setting.getName()));
                    } else {
                        setting.reset();
                    }
                }
            } else {
                for (com.yesclient.module.settings.Setting<?> setting : module.getSettings()) {
                    setting.reset();
                }
            }
        }
    }
    
    public void saveCurrentToProfile() {
        Profile profile = profiles.get(activeProfile);
        if (profile != null) {
            for (Module module : YesClient.getInstance().getModuleManager().getModules()) {
                profile.setModuleState(module.getName(), module.isEnabled());
                profile.setModuleKeybind(module.getName(), module.getKeybind());
                
                JsonObject settingsJson = new JsonObject();
                for (com.yesclient.module.settings.Setting<?> setting : module.getSettings()) {
                    settingsJson.add(setting.getName(), setting.toJson());
                }
                profile.setModuleSettings(module.getName(), settingsJson);
            }
            saveProfile(profile);
        }
    }
    
    public Collection<Profile> getProfiles() {
        return profiles.values();
    }
    
    public Profile getProfile(String name) {
        return profiles.get(name);
    }
    
    public void createProfile(String name) {
        if (!profiles.containsKey(name)) {
            Profile profile = new Profile(name);
            profiles.put(name, profile);
            saveProfile(profile);
        }
    }
    
    public void deleteProfile(String name) {
        if (profiles.containsKey(name) && !"Default".equals(name)) {
            profiles.remove(name);
            File profileFile = new File(profilesDir, name.toLowerCase() + ".json");
            if (profileFile.exists()) {
                profileFile.delete();
            }
            
            if (activeProfile.equals(name)) {
                if (profiles.isEmpty()) {
                    Profile fallbackProfile = new Profile("Default");
                    profiles.put("Default", fallbackProfile);
                }
                activeProfile = profiles.containsKey("Default") ? "Default" : profiles.keySet().iterator().next();
                Profile fallback = profiles.get(activeProfile);
                if (fallback != null) {
                    if (fallback.toJson().has("moduleStates")
                            && !fallback.toJson().getAsJsonObject("moduleStates").entrySet().isEmpty()) {
                        applyProfile(fallback);
                    } else {
                        saveCurrentToProfile();
                    }
                }
                saveProfile(fallback);
                saveActiveProfile();
            }
        }
    }
    
    public String getActiveProfileName() {
        return activeProfile;
    }
    
    public void setActiveProfile(String name) {
        if (profiles.containsKey(name)) {
            saveCurrentToProfile();
            activeProfile = name;
            saveActiveProfile();
            applyProfile(profiles.get(name));
            YesClient.getInstance().saveAll();
        }
    }
}

package com.yesclient.module.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

 
public class ActionSetting extends Setting<Boolean> {

    private final String buttonLabel;
    private final Runnable action;

    public ActionSetting(String name, String description, String buttonLabel, Runnable action) {
        super(name, description, false);
        this.buttonLabel = buttonLabel;
        this.action = action;
    }

    public String getButtonLabel() {
        return buttonLabel;
    }

    public void run() {
        if (action != null) action.run();
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(false);
    }

    @Override
    public void fromJson(JsonElement element) {
    }
}

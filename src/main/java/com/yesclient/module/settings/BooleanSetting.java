package com.yesclient.module.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.function.Consumer;

public class BooleanSetting extends Setting<Boolean> {

     
    public float animation = 0f;
    private Consumer<Boolean> valueChangeListener;

    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
        this.animation = defaultValue ? 1.0f : 0.0f;
    }

    public boolean isEnabled() {
        return getValue();
    }

    public BooleanSetting setOnValueChanged(Consumer<Boolean> listener) {
        this.valueChangeListener = listener;
        return this;
    }

    @Override
    public void setValue(Boolean value) {
        boolean changed = getValue().booleanValue() != value.booleanValue();
        super.setValue(value);
        if (changed && valueChangeListener != null) {
            valueChangeListener.accept(value);
        }
    }

    public void toggle() {
        boolean nextValue = !getValue();
        if (com.yesclient.utils.InterfaceSounds.isClientUiScreen()) {
            com.yesclient.utils.InterfaceSounds.playSettingToggle(nextValue);
        }
        setValue(nextValue);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive()) {
            setValue(element.getAsBoolean());
        }
    }
}

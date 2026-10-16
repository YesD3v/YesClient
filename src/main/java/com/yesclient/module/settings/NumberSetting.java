package com.yesclient.module.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
    
    private final double min;
    private final double max;
    private final double increment;
    private final boolean onlyInt;
    
    public NumberSetting(String name, String description, double defaultValue, double min, double max, double increment) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.onlyInt = false;
    }
    
    public NumberSetting(String name, String description, int defaultValue, int min, int max) {
        super(name, description, (double) defaultValue);
        this.min = min;
        this.max = max;
        this.increment = 1;
        this.onlyInt = true;
    }
    
    @Override
    public void setValue(Double value) {
        value = Math.max(min, Math.min(max, value));
        if (onlyInt) {
            value = (double) Math.round(value);
        } else {
            value = Math.round(value / increment) * increment;
        }
        super.setValue(value);
    }
    
     



    public void setValueUnclamped(double value) {
        if (onlyInt) {
            value = (double) Math.round(value);
        } else {
             
            value = Math.round(value * 100.0) / 100.0;
        }
        super.setValue(value);
    }
    
    public double getMin() {
        return min;
    }
    
    public double getMax() {
        return max;
    }
    
    public double getIncrement() {
        return increment;
    }
    
    public boolean isOnlyInt() {
        return onlyInt;
    }
    
    public int getIntValue() {
        return getValue().intValue();
    }
    
    public float getFloatValue() {
        return getValue().floatValue();
    }
    
    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }
    
    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive()) {
             
            setValueUnclamped(element.getAsDouble());
        }
    }
}

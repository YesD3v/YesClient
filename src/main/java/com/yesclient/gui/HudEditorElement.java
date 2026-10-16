package com.yesclient.gui;

import com.yesclient.module.Module;

 
public final class HudEditorElement {
    String name;
    String id;
    int x, y, width, height;
    Module module;

    HudEditorElement(String name, int x, int y, int width, int height, String id, Module module) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.id = id;
        this.module = module;
    }
}

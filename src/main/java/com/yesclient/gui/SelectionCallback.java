package com.yesclient.gui;

import net.minecraft.item.ItemStack;

@FunctionalInterface
public interface SelectionCallback {
    void onSelect(ItemStack stack);
}

package com.yesclient.storage;
import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.yesclient.itemlog.ItemLogManager;

 




public class ItemLogTab extends CreativeTabs {

    public static ItemLogTab INSTANCE;

    public ItemLogTab() {
        super("yesclient_nbt_logger");
        INSTANCE = this;
         
        setIconStackViaReflection(new ItemStack(Items.diamond));
    }

    private void setIconStackViaReflection(ItemStack stack) {
        try {
            Field field = null;
            for (Field f : CreativeTabs.class.getDeclaredFields()) {
                if (f.getType() == ItemStack.class) {
                    field = f;
                    break;
                }
            }
            if (field != null) {
                field.setAccessible(true);
                field.set(this, stack);
            }
        } catch (Exception e) {
             
        }
    }

    @Override
    public Item getTabIconItem() {
        return Items.diamond;
    }

    @Override
    public ItemStack getIconItemStack() {
        return new ItemStack(Items.diamond);
    }

    @Override
    public String getTabLabel() {
        return "yesclient_nbt_logger";
    }

    @Override
    public void displayAllReleventItems(List<ItemStack> items) {
        ItemLogManager manager = ItemLogManager.getInstance();
        if (manager != null) {
            List<ItemStack> logged = manager.getLoggedItems();
            for (ItemStack stack : logged) {
                if (stack != null && stack.getItem() != null) {
                    items.add(stack.copy());
                }
            }
        }
    }

    public boolean hasItems() {
        ItemLogManager manager = ItemLogManager.getInstance();
        return manager != null && manager.getItemCount() > 0;
    }

     


    @Override
    public boolean hasSearchBar() {
        return false;
    }
}

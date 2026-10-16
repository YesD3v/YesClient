package com.yesclient.storage;
import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import com.yesclient.YesClient;


 




public class ItemStealerTab extends CreativeTabs {

    public static ItemStealerTab INSTANCE;

    public ItemStealerTab() {
        super("itemstealer");
        INSTANCE = this;
        setIconStackViaReflection(new ItemStack(Items.chest_minecart));
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
        return Items.chest_minecart;
    }

    @Override
    public ItemStack getIconItemStack() {
        return new ItemStack(Items.chest_minecart);
    }

    @Override
    public String getTabLabel() {
        return "itemstealer";
    }

    @Override
    public void displayAllReleventItems(List<ItemStack> items) {
        ItemStealerStorage storage = YesClient.getInstance().getItemStealerStorage();
        if (storage != null) {
            for (ItemStack stack : storage.getItems()) {
                if (stack != null && stack.getItem() != null) {
                    items.add(stack.copy());
                }
            }
        }
    }

    public boolean hasItems() {
        ItemStealerStorage storage = YesClient.getInstance().getItemStealerStorage();
        return storage != null && storage.getItems().length > 0;
    }

    @Override
    public boolean hasSearchBar() {
        return false;
    }
}

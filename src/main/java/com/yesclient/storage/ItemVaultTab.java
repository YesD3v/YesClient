package com.yesclient.storage;

import java.lang.reflect.Field;
import java.util.List;

import com.yesclient.YesClient;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * The Item Vault creative tab: an ender chest tab alongside Item Stealer and NBT Logger. It lists everything in the
 * {@link ItemVaultStorage}; taking an item copies it (so the stored copy stays), and the search bar / scrollbar are
 * the creative inventory's own. Storing and Ctrl+Click removal are handled in MixinGuiContainerCreative.
 */
public class ItemVaultTab extends CreativeTabs {

    public static ItemVaultTab INSTANCE;

    public ItemVaultTab() {
        super("itemvault");
        INSTANCE = this;
        setBackgroundImageName("item_search.png"); // the default background has no dark search box behind the field
        setIconStackViaReflection(new ItemStack(Blocks.ender_chest));
    }

    private void setIconStackViaReflection(ItemStack stack) {
        try {
            for (Field f : CreativeTabs.class.getDeclaredFields()) {
                if (f.getType() == ItemStack.class) {
                    f.setAccessible(true);
                    f.set(this, stack);
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public Item getTabIconItem() {
        return Item.getItemFromBlock(Blocks.ender_chest);
    }

    @Override
    public ItemStack getIconItemStack() {
        return new ItemStack(Blocks.ender_chest);
    }

    @Override
    public String getTabLabel() {
        return "itemvault";
    }

    @Override
    public String getTranslatedTabLabel() {
        return "Item Vault"; // shown on hover; avoids depending on the lang-key injection
    }

    @Override
    public void displayAllReleventItems(List<ItemStack> items) {
        if (YesClient.getInstance() == null || YesClient.getInstance().getItemVaultStorage() == null) {
            return;
        }
        for (ItemStack stack : YesClient.getInstance().getItemVaultStorage().getItems()) {
            if (stack != null && stack.getItem() != null) {
                items.add(stack.copy());
            }
        }
    }

    @Override
    public boolean hasSearchBar() {
        return true;
    }
}

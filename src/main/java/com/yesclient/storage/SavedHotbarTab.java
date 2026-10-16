package com.yesclient.storage;

import java.util.List;

import com.yesclient.YesClient;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SavedHotbarTab extends CreativeTabs {
    public static SavedHotbarTab INSTANCE;

    public SavedHotbarTab() {
        super("savedhotbars");
        INSTANCE = this;
    }

    @Override
    public Item getTabIconItem() {
        return Item.getItemFromBlock(Blocks.bookshelf);
    }

    @Override
    public ItemStack getIconItemStack() {
        return new ItemStack(Blocks.bookshelf);
    }

    @Override
    public String getTabLabel() {
        return "savedhotbars";
    }

    @Override
    public void displayAllReleventItems(List<ItemStack> items) {
        if (YesClient.getInstance() == null || YesClient.getInstance().getSavedHotbarStorage() == null) return;
        SavedHotbarStorage storage = YesClient.getInstance().getSavedHotbarStorage();
        for (int row = 0; row < SavedHotbarStorage.HOTBAR_COUNT; row++) {
            for (int column = 0; column < SavedHotbarStorage.HOTBAR_COUNT; column++) {
                if (row == column) {
                     
                     
                    items.add(storage.createPaper(row));
                } else {
                    ItemStack preview = storage.getSavedStack(row, column);
                    items.add(preview == null ? null : addSlotHint(preview, row, column));
                }
            }
        }

         
         
        for (int hotbar = 0; hotbar < SavedHotbarStorage.HOTBAR_COUNT; hotbar++) {
            if (!storage.isSaved(hotbar)) continue;
            ItemStack diagonalPreview = storage.getSavedStack(hotbar, hotbar);
            if (diagonalPreview != null) {
                items.add(addSlotHint(diagonalPreview, hotbar, hotbar));
            }
        }
    }

    @Override
    public boolean hasSearchBar() {
        return false;
    }

    private ItemStack addSlotHint(ItemStack stack, int hotbar, int slot) {
        net.minecraft.nbt.NBTTagCompound root = stack.getTagCompound();
        if (root == null) root = new net.minecraft.nbt.NBTTagCompound();
        net.minecraft.nbt.NBTTagCompound display = root.getCompoundTag("display");
        net.minecraft.nbt.NBTTagList lore = display.getTagList("Lore", 8);
        net.minecraft.nbt.NBTTagList updatedLore = new net.minecraft.nbt.NBTTagList();
        for (int i = 0; i < lore.tagCount(); i++) {
            updatedLore.appendTag(lore.get(i).copy());
        }
        updatedLore.appendTag(new net.minecraft.nbt.NBTTagString("§8Saved Hotbar " + (hotbar + 1)
                + " - Slot " + (slot + 1)));
        display.setTag("Lore", updatedLore);
        root.setTag("display", display);
        stack.setTagCompound(root);
        return stack;
    }
}

package com.yesclient.storage;
import java.io.IOException;

import com.yesclient.YesClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

 



public class ItemStealerStorage {

     
    public static final int ROWS = 6;
    public static final int COLS = 9;
    public static final int SIZE = ROWS * COLS;

    private final ItemStack[] items = new ItemStack[SIZE];
    private final File storageFile;
    private final File legacyStorageFile;

    public ItemStealerStorage(File dataDir) {
        this.storageFile = new File(dataDir, "item-stealer-items.dat");
        this.legacyStorageFile = new File(dataDir, "itemstealer.db");
    }

    public void load() {
        File sourceFile = storageFile;
        if (!sourceFile.isFile() && legacyStorageFile.isFile()) {
            if (legacyStorageFile.renameTo(storageFile)) {
                sourceFile = storageFile;
            } else {
                sourceFile = legacyStorageFile;
            }
        }
        if (!sourceFile.isFile()) {
            return;
        }

        boolean loadedLegacyFile = sourceFile.equals(legacyStorageFile);
        try (FileInputStream fis = new FileInputStream(sourceFile)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(fis);

            if (root.hasKey("items")) {
                NBTTagList itemList = root.getTagList("items", 10);  

                for (int i = 0; i < itemList.tagCount(); i++) {
                    NBTTagCompound itemNbt = itemList.getCompoundTagAt(i);
                    int slot = itemNbt.getInteger("slot");

                    if (slot >= 0 && slot < SIZE) {
                        items[slot] = ItemStack.loadItemStackFromNBT(itemNbt);
                    }
                }
            }

            YesClient.LOGGER.info("Loaded " + getItemCount() + " items from Item Stealer storage.");
        } catch (IOException e) {
            YesClient.LOGGER.error("Failed to load Item Stealer storage", e);
            return;
        }
        if (loadedLegacyFile) {
            save();
        }
    }

    public void save() {
        try {
            NBTTagCompound root = new NBTTagCompound();
            NBTTagList itemList = new NBTTagList();

            for (int i = 0; i < SIZE; i++) {
                if (items[i] != null) {
                    NBTTagCompound itemNbt = new NBTTagCompound();
                    items[i].writeToNBT(itemNbt);
                    itemNbt.setInteger("slot", i);
                    itemList.appendTag(itemNbt);
                }
            }

            root.setTag("items", itemList);

            try (FileOutputStream fos = new FileOutputStream(storageFile)) {
                CompressedStreamTools.writeCompressed(root, fos);
            }

            if (legacyStorageFile.isFile() && !legacyStorageFile.delete()) {
                YesClient.LOGGER.warn("Could not remove migrated Item Stealer storage file.");
            }

            YesClient.LOGGER.info("Saved " + getItemCount() + " items to Item Stealer storage.");
        } catch (IOException e) {
            YesClient.LOGGER.error("Failed to save Item Stealer storage", e);
        }
    }

    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= SIZE)
            return null;
        return items[slot];
    }

    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SIZE)
            return;
        items[slot] = stack != null ? stack.copy() : null;
    }

    public void clearSlot(int slot) {
        if (slot >= 0 && slot < SIZE) {
            items[slot] = null;
        }
    }

    public void clearAll() {
        for (int i = 0; i < SIZE; i++) {
            items[i] = null;
        }
    }

    public int getItemCount() {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null)
                count++;
        }
        return count;
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public boolean hasItem(int slot) {
        return slot >= 0 && slot < SIZE && items[slot] != null;
    }

    public ItemStack[] getItems() {
        return items;
    }

     
    public int findEmptySlot() {
        for (int i = 0; i < SIZE; i++) {
            if (items[i] == null) {
                return i;
            }
        }
        return -1;
    }

     
    public boolean addItem(ItemStack stack) {
        int slot = findEmptySlot();
        if (slot != -1) {
            setItem(slot, stack);
            save();  
            return true;
        }
        return false;
    }
}

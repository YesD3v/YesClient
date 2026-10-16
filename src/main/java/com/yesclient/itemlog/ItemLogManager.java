package com.yesclient.itemlog;
import java.util.List;

import com.yesclient.YesClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.*;
import java.util.ArrayList;

 



public class ItemLogManager {

    private static ItemLogManager instance;
    private final List<ItemStack> loggedItems = new ArrayList<>();
    private final File saveFile;

    public ItemLogManager() {
        instance = this;
        saveFile = new File(YesClient.getInstance().getDataDir(), "item_log.dat");
        loadItems();
    }

    public static ItemLogManager getInstance() {
        return instance;
    }

     





    public boolean addItem(ItemStack stack) {
        if (stack == null)
            return false;

         
        for (ItemStack existing : loggedItems) {
            if (areItemsEqual(existing, stack)) {
                return false;  
            }
        }

         
        loggedItems.add(stack.copy());
        saveItems();
        return true;
    }

     


    private boolean areItemsEqual(ItemStack a, ItemStack b) {
        if (a == null || b == null)
            return a == b;
        if (a.getItem() != b.getItem())
            return false;
        if (a.getItemDamage() != b.getItemDamage())
            return false;

        NBTTagCompound nbtA = a.getTagCompound();
        NBTTagCompound nbtB = b.getTagCompound();

        if (nbtA == null && nbtB == null)
            return true;
        if (nbtA == null || nbtB == null)
            return false;

        return nbtA.equals(nbtB);
    }

     


    public List<ItemStack> getLoggedItems() {
        return new ArrayList<>(loggedItems);
    }

     


    public void clearItems() {
        loggedItems.clear();
        saveItems();
    }

     


    public void saveItems() {
        try {
            NBTTagCompound root = new NBTTagCompound();
            net.minecraft.nbt.NBTTagList itemList = new net.minecraft.nbt.NBTTagList();

            for (ItemStack stack : loggedItems) {
                if (stack != null) {
                    NBTTagCompound itemNbt = new NBTTagCompound();
                    stack.writeToNBT(itemNbt);
                    itemList.appendTag(itemNbt);
                }
            }

            root.setTag("Items", itemList);

            try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                net.minecraft.nbt.CompressedStreamTools.writeCompressed(root, fos);
            }
        } catch (IOException e) {
            YesClient.LOGGER.error("Failed to save item log", e);
        }
    }

     


    public void loadItems() {
        if (!saveFile.exists()) {
             
             
            return;
        }

        try (FileInputStream fis = new FileInputStream(saveFile)) {
            NBTTagCompound root = net.minecraft.nbt.CompressedStreamTools.readCompressed(fis);

            if (root.hasKey("Items")) {
                net.minecraft.nbt.NBTTagList itemList = root.getTagList("Items", 10);  

                loggedItems.clear();

                for (int i = 0; i < itemList.tagCount(); i++) {
                    NBTTagCompound itemNbt = itemList.getCompoundTagAt(i);
                    ItemStack stack = ItemStack.loadItemStackFromNBT(itemNbt);
                    if (stack != null) {
                        loggedItems.add(stack);
                    }
                }

                YesClient.LOGGER.info("Loaded " + loggedItems.size() + " items from item log");
            }
        } catch (IOException e) {
            YesClient.LOGGER.error("Failed to load item log", e);
        }
    }

     


    public int getItemCount() {
        return loggedItems.size();
    }
}

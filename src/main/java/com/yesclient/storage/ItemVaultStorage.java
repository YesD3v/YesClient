package com.yesclient.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.yesclient.YesClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * The Item Vault: an unlimited list of saved items. Taking an item out gives a copy, so stored items stay until they
 * are removed (Ctrl+Click in the vault panel).
 */
public class ItemVaultStorage {

    private final List<ItemStack> items = new ArrayList<ItemStack>();
    private final File storageFile;

    public ItemVaultStorage(File dataDir) {
        this.storageFile = new File(dataDir, "item-vault.dat");
    }

    public void load() {
        items.clear();
        if (!storageFile.isFile()) {
            return;
        }
        try (FileInputStream in = new FileInputStream(storageFile)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(in);
            NBTTagList list = root.getTagList("items", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                ItemStack stack = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
                if (stack != null) {
                    items.add(stack);
                }
            }
            YesClient.LOGGER.info("Loaded " + items.size() + " items from the item vault.");
        } catch (IOException e) {
            YesClient.LOGGER.error("Failed to load the item vault", e);
        }
    }

    public void save() {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (ItemStack stack : items) {
            NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            list.appendTag(tag);
        }
        root.setTag("items", list);
        try (FileOutputStream out = new FileOutputStream(storageFile)) {
            CompressedStreamTools.writeCompressed(root, out);
        } catch (IOException e) {
            YesClient.LOGGER.error("Failed to save the item vault", e);
        }
    }

    /** Stored items in order. The stacks themselves are the stored ones; copy before handing them out. */
    public List<ItemStack> getItems() {
        return Collections.unmodifiableList(items);
    }

    /** Stores a copy of the stack unless the same item (item, damage and NBT) is already stored. */
    public boolean add(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        for (ItemStack stored : items) {
            if (ItemStack.areItemsEqual(stored, stack) && ItemStack.areItemStackTagsEqual(stored, stack)) {
                return false;
            }
        }
        items.add(stack.copy());
        save();
        return true;
    }

    /** Removes this exact stored stack (as returned by {@link #getItems()}). */
    public boolean remove(ItemStack stored) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == stored) {
                items.remove(i);
                save();
                return true;
            }
        }
        return false;
    }

    /** Removes the first stored item equal (item, damage and NBT) to the given stack. */
    public boolean removeMatching(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < items.size(); i++) {
            ItemStack stored = items.get(i);
            if (ItemStack.areItemsEqual(stored, stack) && ItemStack.areItemStackTagsEqual(stored, stack)) {
                items.remove(i);
                save();
                return true;
            }
        }
        return false;
    }
}

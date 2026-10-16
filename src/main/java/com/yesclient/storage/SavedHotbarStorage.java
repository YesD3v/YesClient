package com.yesclient.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.yesclient.YesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public class SavedHotbarStorage {
    public static final int HOTBAR_COUNT = 9;
    private final ItemStack[][] hotbars = new ItemStack[HOTBAR_COUNT][9];
    private final boolean[] saved = new boolean[HOTBAR_COUNT];
    private final KeyBinding saveBinding;
    private final KeyBinding loadBinding;
    private final File file;

    public SavedHotbarStorage(File dataDirectory) {
        file = new File(dataDirectory, "saved-hotbars.dat");
        saveBinding = new KeyBinding("Save Hotbar", org.lwjgl.input.Keyboard.KEY_C,
                "key.categories.inventory");
        loadBinding = new KeyBinding("Load Hotbar", org.lwjgl.input.Keyboard.KEY_X,
                "key.categories.inventory");
        ClientRegistry.registerKeyBinding(saveBinding);
        ClientRegistry.registerKeyBinding(loadBinding);
    }

    public void load() {
        if (!file.isFile()) return;
        try (FileInputStream input = new FileInputStream(file)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(input);
            NBTTagList stacks = root.getTagList("stacks", 10);
            for (int i = 0; i < HOTBAR_COUNT; i++) saved[i] = root.getBoolean("saved" + i);
            for (int i = 0; i < stacks.tagCount(); i++) {
                NBTTagCompound entry = stacks.getCompoundTagAt(i);
                int hotbar = entry.getInteger("hotbar");
                int slot = entry.getInteger("slot");
                if (hotbar >= 0 && hotbar < HOTBAR_COUNT && slot >= 0 && slot < 9) {
                    hotbars[hotbar][slot] = ItemStack.loadItemStackFromNBT(entry.getCompoundTag("stack"));
                }
            }
        } catch (IOException e) {
            YesClient.LOGGER.error("Failed to load saved hotbars", e);
        }
    }

    public void save() {
        try {
            NBTTagCompound root = new NBTTagCompound();
            NBTTagList stacks = new NBTTagList();
            for (int hotbar = 0; hotbar < HOTBAR_COUNT; hotbar++) {
                root.setBoolean("saved" + hotbar, saved[hotbar]);
                for (int slot = 0; slot < 9; slot++) {
                    ItemStack stack = hotbars[hotbar][slot];
                    if (stack == null) continue;
                    NBTTagCompound entry = new NBTTagCompound();
                    NBTTagCompound stackTag = new NBTTagCompound();
                    stack.writeToNBT(stackTag);
                    entry.setInteger("hotbar", hotbar);
                    entry.setInteger("slot", slot);
                    entry.setTag("stack", stackTag);
                    stacks.appendTag(entry);
                }
            }
            root.setTag("stacks", stacks);
            try (FileOutputStream output = new FileOutputStream(file)) {
                CompressedStreamTools.writeCompressed(root, output);
            }
        } catch (IOException e) {
            YesClient.LOGGER.error("Failed to save saved hotbars", e);
        }
    }

    public void saveHotbar(int index) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!validIndex(index) || mc.thePlayer == null) return;
        InventoryPlayer inventory = mc.thePlayer.inventory;
        ItemStack[] replacement = new ItemStack[9];
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            replacement[slot] = stack == null ? null : stack.copy();
        }
        hotbars[index] = replacement;
        saved[index] = true;
        save();
    }

    public ItemStack getSavedStack(int hotbarIndex, int slot) {
        if (!validIndex(hotbarIndex) || slot < 0 || slot >= 9 || hotbars[hotbarIndex][slot] == null) return null;
        return hotbars[hotbarIndex][slot].copy();
    }

    public boolean loadHotbar(int index) {
        if (!validIndex(index) || !saved[index]) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = hotbars[index][slot];
            ItemStack copy = stack == null ? null : stack.copy();
            mc.thePlayer.inventory.mainInventory[slot] = copy;
            mc.playerController.sendSlotPacket(copy, 36 + slot);
        }
        mc.thePlayer.inventoryContainer.detectAndSendChanges();
        return true;
    }

    public boolean isSaved(int index) {
        return validIndex(index) && saved[index];
    }

    public ItemStack createPaper(int index) {
        if (!validIndex(index)) return null;
        ItemStack paper = new ItemStack(Items.paper);
        paper.setStackDisplayName(saved[index] ? "\u00A7aSaved Hotbar " + (index + 1) : "Hotbar " + (index + 1));
        NBTTagCompound root = paper.getTagCompound();
        if (root == null) root = new NBTTagCompound();
        NBTTagCompound display = root.getCompoundTag("display");
        NBTTagList lore = new NBTTagList();
        if (saved[index]) {
            lore.appendTag(new NBTTagString("X + " + (index + 1) + " to load"));
            lore.appendTag(new NBTTagString("C + " + (index + 1) + " to overwrite"));
        } else {
            lore.appendTag(new NBTTagString("C + " + (index + 1) + " to save"));
            lore.appendTag(new NBTTagString("X + " + (index + 1) + " to load"));
        }
        display.setTag("Lore", lore);
        root.setTag("display", display);
        paper.setTagCompound(root);
        return paper;
    }

    public boolean handleCreativeKeyPress(int key) {
        Minecraft mc = Minecraft.getMinecraft();
         
         
        saveBinding.isPressed();
        loadBinding.isPressed();

        if (mc.thePlayer == null) return false;
        boolean creativeInventoryOpen = mc.currentScreen instanceof GuiContainerCreative;
        boolean gameplayKeybindAllowed = mc.currentScreen == null || creativeInventoryOpen;
        int index = numberKeyIndex(key);
        if (index < 0) return false;
        if (!gameplayKeybindAllowed) return false;

        if (isBindingDown(saveBinding)) {
            saveHotbar(index);
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§aSaved hotbar " + (index + 1)));
            return true;
        }
        if (isBindingDown(loadBinding)) {
            if (loadHotbar(index)) {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§aLoaded hotbar " + (index + 1)));
            } else {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§cHotbar " + (index + 1) + " is empty"));
            }
            return true;
        }
        return false;
    }

    private int numberKeyIndex(int key) {
        if (key >= org.lwjgl.input.Keyboard.KEY_1 && key <= org.lwjgl.input.Keyboard.KEY_9) {
            return key - org.lwjgl.input.Keyboard.KEY_1;
        }
        if (key >= org.lwjgl.input.Keyboard.KEY_NUMPAD1 && key <= org.lwjgl.input.Keyboard.KEY_NUMPAD9) {
            return key - org.lwjgl.input.Keyboard.KEY_NUMPAD1;
        }
        return -1;
    }

    private boolean isBindingDown(KeyBinding binding) {
        int keyCode = binding.getKeyCode();
        return binding.isKeyDown() || (keyCode > 0 && org.lwjgl.input.Keyboard.isKeyDown(keyCode));
    }

    private boolean validIndex(int index) {
        return index >= 0 && index < HOTBAR_COUNT;
    }
}

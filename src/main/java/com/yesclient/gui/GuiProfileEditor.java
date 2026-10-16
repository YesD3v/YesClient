package com.yesclient.gui;

import com.yesclient.YesClient;
import com.yesclient.config.Profile;
import com.yesclient.utils.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiProfileEditor extends GuiScreen {

    private final GuiScreen parent;
    private GuiTextField searchField;
    private GuiTextField nameField;
    private final List<ItemStack> allItems = new ArrayList<>();
    private final List<ItemStack> filteredItems = new ArrayList<>();

    private static final int TOP_BAR_HEIGHT = 45;
    private static final int PADDING = 10;
    private static final int MIN_ITEM_SIZE = 12;
    private static final int MAX_ITEM_SIZE = 24;
    private static final int MIN_GAP = 2;

    private int itemSize = 18;
    private int gap = 3;
    private int itemsPerRow = 1;
    private int gridStartX = 0;
    private int gridStartY = TOP_BAR_HEIGHT;

    private boolean glint = false;
    private ItemStack selectedIcon = null;
    private String editingName = null;

    public GuiProfileEditor(GuiScreen parent) {
        this(parent, null);
    }
    
    public GuiProfileEditor(GuiScreen parent, String editingName) {
        this.parent = parent;
        this.editingName = editingName;
        loadItems();
    }

    private void loadItems() {
        allItems.clear();
        for (Item item : Item.itemRegistry) {
            if (item == null) continue;
            
            if (item == Item.getItemFromBlock(Blocks.farmland) ||
                item == Item.getItemFromBlock(Blocks.lit_furnace) ||
                item == Item.getItemFromBlock(Blocks.lit_redstone_ore) ||
                item == Item.getItemFromBlock(Blocks.furnace)) {
                continue;
            }

            List<ItemStack> subItems = new ArrayList<>();
            item.getSubItems(item, null, subItems);
            if (subItems.isEmpty()) {
                allItems.add(new ItemStack(item));
            } else {
                allItems.addAll(subItems);
            }
        }
        filteredItems.addAll(allItems);
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        int middleX = width / 2;

        nameField = new GuiTextField(0, fontRendererObj, middleX - 160, 15, 120, 20);
        nameField.setFocused(true);
        nameField.setCanLoseFocus(true);
        
        if (editingName != null) {
            nameField.setText(editingName);
            com.yesclient.config.Profile p = YesClient.getInstance().getProfileManager().getProfile(editingName);
            if (p != null) {
                glint = p.hasIconGlint();
                String iconStr = p.getIconItem();
                String[] parts = iconStr.split(":");
                try {
                    Item item = Item.getByNameOrId(parts.length > 1 ? parts[0] + ":" + parts[1] : iconStr);
                    if (item != null) {
                        int meta = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                        selectedIcon = new ItemStack(item, 1, meta);
                    }
                } catch(Exception ignored) {}
            }
        } else {
            nameField.setText("New Profile");
        }

        searchField = new GuiTextField(1, fontRendererObj, middleX - 30, 15, 120, 20);
        searchField.setFocused(false);
        searchField.setCanLoseFocus(true);
        searchField.setText("");
        
        filterItems("");
    }
    
    private void filterItems(String q) {
        filteredItems.clear();
        String query = q.toLowerCase();
        for (ItemStack stack : allItems) {
            if (query.isEmpty() || stack.getDisplayName().toLowerCase().contains(query) || Item.itemRegistry.getNameForObject(stack.getItem()).toString().toLowerCase().contains(query)) {
                filteredItems.add(stack);
            }
        }
        calculateLayout();
    }

    private void calculateLayout() {
        if (filteredItems.isEmpty()) {
            itemsPerRow = 1;
            return;
        }

        int availableWidth = width - (PADDING * 2);
        int availableHeight = height - TOP_BAR_HEIGHT - PADDING;
        int itemCount = filteredItems.size();

        for (int testSize = MAX_ITEM_SIZE; testSize >= MIN_ITEM_SIZE; testSize--) {
            int testGap = Math.max(MIN_GAP, testSize / 6);
            int cellSize = testSize + testGap;
            int cols = availableWidth / cellSize;
            if (cols < 1) cols = 1;

            int rows = (int) Math.ceil((double) itemCount / cols);
            int totalHeight = rows * cellSize;

            if (totalHeight <= availableHeight) {
                itemSize = testSize;
                gap = testGap;
                itemsPerRow = cols;
                int gridWidth = cols * cellSize;
                gridStartX = PADDING + (availableWidth - gridWidth) / 2;
                gridStartY = TOP_BAR_HEIGHT;
                return;
            }
        }

        itemSize = MIN_ITEM_SIZE;
        gap = MIN_GAP;
        int cellSize = itemSize + gap;
        itemsPerRow = Math.max(1, availableWidth / cellSize);
        int gridWidth = itemsPerRow * cellSize;
        gridStartX = PADDING + (availableWidth - gridWidth) / 2;
        gridStartY = TOP_BAR_HEIGHT;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        nameField.updateCursorCounter();
        searchField.updateCursorCounter();
    }

    private void drawCentered(String text, int centerX, int y, int color) {
        fontRendererObj.drawString(text, centerX - fontRendererObj.getStringWidth(text) / 2, y, color);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        nameField.drawTextBox();
        if (nameField.getText().isEmpty() && !nameField.isFocused()) {
            fontRendererObj.drawString("Profile Name", nameField.xPosition + 4, nameField.yPosition + 6, 0xFFAAAAAA);
        }

        searchField.drawTextBox();
        if (searchField.getText().isEmpty() && !searchField.isFocused()) {
            fontRendererObj.drawString("Search Icon...", searchField.xPosition + 4, searchField.yPosition + 6, 0xFFAAAAAA);
        }

        int doneX = width - 70;
        boolean doneHover = mouseX >= doneX && mouseX < doneX + 60 && mouseY >= 15 && mouseY < 35;
        RenderUtils.drawRoundedRect(doneX, 15, 60, 20, 5, doneHover ? 0xFF3A3E45 : 0xFF2A2D32);
        drawCentered("DONE", doneX + 30, 21, 0xFFD0D3D8);
        
        int glintX = doneX - 70;
        boolean glintHover = mouseX >= glintX && mouseX < glintX + 60 && mouseY >= 15 && mouseY < 35;
        RenderUtils.drawRoundedRect(glintX, 15, 60, 20, 5, glintHover ? 0xFF3A3E45 : (glint ? 0xFF2A2D32 : 0xFF1A1D22));
        drawCentered("GLINT " + (glint ? "ON" : "OFF"), glintX + 30, 21, glint ? 0xFF55FF55 : 0xFFD0D3D8);
        
        int cancelX = 10;
        boolean cancelHover = mouseX >= cancelX && mouseX < cancelX + 60 && mouseY >= 15 && mouseY < 35;
        RenderUtils.drawRoundedRect(cancelX, 15, 60, 20, 5, cancelHover ? 0xFF3A3E45 : 0xFF2A2D32);
        drawCentered("CANCEL", cancelX + 30, 21, 0xFFD0D3D8);
        
        if (editingName != null && YesClient.getInstance().getProfileManager().getProfiles().size() > 1) {
            int delX = cancelX + 70;
            boolean delHover = mouseX >= delX && mouseX < delX + 60 && mouseY >= 15 && mouseY < 35;
            RenderUtils.drawRoundedRect(delX, 15, 60, 20, 5, delHover ? 0xFF552222 : 0xFF331111);
            drawCentered("DELETE", delX + 30, 21, 0xFFFF5555);
        }

        int cellSize = itemSize + gap;
        int hoveredIndex = -1;

        RenderHelper.enableGUIStandardItemLighting();

        for (int i = 0; i < filteredItems.size(); i++) {
            int col = i % itemsPerRow;
            int row = i / itemsPerRow;
            int x = gridStartX + col * cellSize;
            int y = gridStartY + row * cellSize;

            if (y + itemSize < TOP_BAR_HEIGHT || y > height) continue;

            ItemStack stack = filteredItems.get(i);
            boolean isSelected = selectedIcon != null && stack.getItem() == selectedIcon.getItem() && stack.getMetadata() == selectedIcon.getMetadata();
            boolean hovered = mouseX >= x && mouseX < x + itemSize && mouseY >= y && mouseY < y + itemSize;

            if (hovered) {
                hoveredIndex = i;
                RenderUtils.drawRect(x - 1, y - 1, itemSize + 2, itemSize + 2, 0x80FFFFFF);
            } else if (isSelected) {
                RenderUtils.drawRect(x - 1, y - 1, itemSize + 2, itemSize + 2, 0x8055FF55);
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 0);
            float scale = itemSize / 16.0f;
            GlStateManager.scale(scale, scale, 1.0f);
            
            ItemStack renderStack = stack.copy();
            if (glint && isSelected) {
                NBTTagCompound tag = renderStack.hasTagCompound() ? renderStack.getTagCompound() : new NBTTagCompound();
                tag.setTag("ench", new NBTTagList());
                renderStack.setTagCompound(tag);
            }

            mc.getRenderItem().renderItemAndEffectIntoGUI(renderStack, 0, 0);
            GlStateManager.popMatrix();
        }

        RenderHelper.disableStandardItemLighting();

        if (hoveredIndex >= 0 && hoveredIndex < filteredItems.size()) {
            ItemStack stack = filteredItems.get(hoveredIndex);
            List<String> lines = new ArrayList<>();
            lines.add(stack.getDisplayName());
            drawHoveringText(lines, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        nameField.mouseClicked(mouseX, mouseY, mouseButton);
        searchField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0) {
            int doneX = width - 70;
            if (mouseX >= doneX && mouseX < doneX + 60 && mouseY >= 15 && mouseY < 35) {
                finish();
                return;
            }
            
            int glintX = doneX - 70;
            if (mouseX >= glintX && mouseX < glintX + 60 && mouseY >= 15 && mouseY < 35) {
                glint = !glint;
                return;
            }
            
            int cancelX = 10;
            if (mouseX >= cancelX && mouseX < cancelX + 60 && mouseY >= 15 && mouseY < 35) {
                mc.displayGuiScreen(parent);
                return;
            }
            
            if (editingName != null && YesClient.getInstance().getProfileManager().getProfiles().size() > 1) {
                int delX = cancelX + 70;
                if (mouseX >= delX && mouseX < delX + 60 && mouseY >= 15 && mouseY < 35) {
                    YesClient.getInstance().getProfileManager().deleteProfile(editingName);
                    mc.displayGuiScreen(parent);
                    return;
                }
            }
        }

        if (mouseY >= TOP_BAR_HEIGHT && mouseButton == 0) {
            int cellSize = itemSize + gap;
            for (int i = 0; i < filteredItems.size(); i++) {
                int col = i % itemsPerRow;
                int row = i / itemsPerRow;
                int x = gridStartX + col * cellSize;
                int y = gridStartY + row * cellSize;

                if (mouseX >= x && mouseX < x + itemSize && mouseY >= y && mouseY < y + itemSize) {
                    selectedIcon = filteredItems.get(i);
                    return;
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (nameField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (searchField.textboxKeyTyped(typedChar, keyCode)) {
            filterItems(searchField.getText());
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void finish() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) return;
        
        com.yesclient.config.ProfileManager pm = YesClient.getInstance().getProfileManager();
        if (editingName != null && !name.equals(editingName)) {
            com.yesclient.config.Profile oldP = pm.getProfile(editingName);
            pm.createProfile(name);
            com.yesclient.config.Profile newP = pm.getProfile(name);
            if (oldP != null && newP != null) {
                for (com.yesclient.module.Module module : YesClient.getInstance().getModuleManager().getModules()) {
                    Boolean st = oldP.getModuleState(module.getName());
                    if (st != null) newP.setModuleState(module.getName(), st);
                }
            }
            if (pm.getActiveProfileName().equals(editingName)) {
                pm.setActiveProfile(name);
            }
            pm.deleteProfile(editingName);
        } else if (editingName == null) {
            pm.createProfile(name);
        }
        
        com.yesclient.config.Profile p = pm.getProfile(name);
        if (p != null) {
            if (selectedIcon != null) {
                p.setIconItem(Item.itemRegistry.getNameForObject(selectedIcon.getItem()).toString() + ":" + selectedIcon.getMetadata());
            }
            p.setIconGlint(glint);
            if (editingName == null || pm.getActiveProfileName().equals(name)) {
                pm.setActiveProfile(name);
            } else {
                YesClient.getInstance().saveAll();
            }
        }
        
        mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

package com.yesclient.gui;
import java.io.IOException;
import java.util.Locale;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.modules.client.ClickGUIModule;
import com.yesclient.module.modules.client.HudDesignerModule;
import com.yesclient.module.settings.ActionSetting;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ColorSetting;
import com.yesclient.module.settings.ItemSetting;
import com.yesclient.module.settings.ListSetting;
import com.yesclient.module.settings.ModeSetting;
import com.yesclient.module.settings.NumberSetting;
import com.yesclient.module.settings.Setting;
import com.yesclient.module.settings.StringSetting;
import com.yesclient.utils.KeybindManager;
import com.yesclient.utils.MarqueeText;
import com.yesclient.utils.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;

 



public class ClickGUI extends GuiScreen {

    private static final ResourceLocation BRAND_ICON = new ResourceLocation("yesclient",
            "textures/gui/yesclient.png");

    private static final int SIDEBAR_WIDTH = 132;
    private static final int HEADER_HEIGHT = 58;
    private static final int CARD_HEIGHT = 64;
    private static final int CARD_GAP = 8;
    private static final int SETTING_HEIGHT = 50;
    private static final int CORNER_RADIUS = 12;

     
    private static final int BACKDROP = 0xA90A0B0D;
    private static final int PANEL = 0xF015171A;
    private static final int SIDEBAR = 0xEE191B1F;
    private static final int CARD = 0xFF22252A;
    private static final int CARD_HOVER = 0xFF2A2E34;
    private static final int CONTROL = 0xFF30343A;
    private static final int LINE = 0xFF34383F;
    private static final int TEXT = 0xFFE7E9ED;
    private static final int TEXT_MUTED = 0xFF9B9FA8;
    private static final int TEXT_DIM = 0xFF696E77;
    private static final int ACCENT = 0xFFC8CDD5;
    private static final int ACCENT_DARK = 0xFF2A2E34;
    private static final Category[] SIDEBAR_CATEGORIES = new Category[] {
            Category.MOVEMENT, Category.VISUALS, Category.MISC, Category.EXPLOIT,
            Category.CLIENT, Category.RENDER, Category.QOL, Category.MODERATION
    };

    private Category selectedCategory;
    private Module selectedModule;
    private Module bindingModule;
    private ItemSetting bindingItemSetting;
    private boolean appearancePage;
    private GuiTextField searchField;
    private StringSetting editingString;
    private GuiTextField stringField;
    private GuiScreen returnScreen;
    private int searchBarX;
    private int searchBarY;
    private int searchBarW;
    private NumberSetting draggingNumber;
    private int draggingControlX;
    private int draggingControlW;

    private float moduleScroll;
    private float targetModuleScroll;
    private float settingsScroll;
    private float targetSettingsScroll;
    private long lastScrollFrameNanos;

     
    private float openAnimation;           
    private long openTime;
    private HashMap<Module, Float> cardHoverAnim = new HashMap<Module, Float>();
    private HashMap<String, Float> settingHoverAnim = new HashMap<String, Float>();
    private float sidebarIndicatorY = -1;   
    private float pageTransition;           

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public ClickGUI() {
    }

    @Override
    public void initGui() {
        super.initGui();
        wasBlurEnabled = false;  
        openTime = System.currentTimeMillis();
        openAnimation = 0f;
        pageTransition = selectedModule != null ? 1f : 0f;

        String previous = searchField == null ? "" : searchField.getText();
        searchField = new GuiTextField(11, fontRendererObj, 0, 0, 140, 18);
        searchField.setEnableBackgroundDrawing(false);
        searchField.setMaxStringLength(80);
        searchField.setText(previous);
        searchField.setCanLoseFocus(true);
        searchField.setFocused(false);
        Keyboard.enableRepeatEvents(true);
    }

    public void reset() {
        selectedCategory = null;
        selectedModule = null;
        bindingModule = null;
        bindingItemSetting = null;
        appearancePage = false;
        editingString = null;
        stringField = null;
        moduleScroll = 0;
        targetModuleScroll = 0;
        settingsScroll = 0;
        targetSettingsScroll = 0;
        lastScrollFrameNanos = System.nanoTime();
        if (searchField != null) {
            searchField.setText("");
            searchField.setFocused(false);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchField != null) searchField.updateCursorCounter();
        if (stringField != null) stringField.updateCursorCounter();
         
        float targetPage = selectedModule != null ? 1f : 0f;
        pageTransition += (targetPage - pageTransition) * 0.3f;
        if (Math.abs(pageTransition - targetPage) < 0.01f) pageTransition = targetPage;
    }

    @Override
    public void onGuiClosed() {
        if (mc.entityRenderer.getShaderGroup() != null) mc.entityRenderer.stopUseShader();
        Keyboard.enableRepeatEvents(false);
        editingString = null;
        stringField = null;
        bindingModule = null;
        bindingItemSetting = null;
        draggingNumber = null;
        YesClient.getInstance().saveAll();
        super.onGuiClosed();
    }

    private int currentSidebar = 0xEE191B1F;
    private int currentLine = 0xFF34383F;

    private float currentAnimScale = 1.0f;  
    private boolean profilesOpen = false;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateLayout();
        updateScrollAnimation();
        
         
        float elapsed = (System.currentTimeMillis() - openTime) / 200f;
        openAnimation = Math.min(1f, elapsed);
        
        ClickGUIModule config = getClickGuiModule();
        int baseOpacity = config != null ? (int) (config.getSetting("Opacity") != null ? ((NumberSetting) config.getSetting("Opacity")).getValue() : 75) : 75;
        float easeOut = 1.0f - (float) Math.pow(1.0f - openAnimation, 3);
        int opacity = (int) (baseOpacity * easeOut);
        
         
        int panelAlpha = Math.max(1, (int) (opacity * 2.55));
        int sidebarAlpha = Math.max(1, (int) (opacity * 2.38));
        int lineAlpha = Math.max(1, (int) (opacity * 2.55));
        int shadowAlpha = Math.max(1, (int) (0x50 * easeOut));
        
        int currentPanel = (panelAlpha << 24) | 0x15171A;
        currentSidebar = (sidebarAlpha << 24) | 0x191B1F;
        currentLine = (lineAlpha << 24) | 0x34383F;
        int currentShadow = (shadowAlpha << 24) | 0x000000;

        GL11.glPushMatrix();
         
        currentAnimScale = 0.95f + (0.05f * easeOut);
        GL11.glTranslatef(width / 2f, height / 2f, 0);
        GL11.glScalef(currentAnimScale, currentAnimScale, 1f);
        GL11.glTranslatef(-width / 2f, -height / 2f, 0);

        RenderUtils.drawRoundedRect(panelX + 4, panelY + 5, panelW, panelH, CORNER_RADIUS, currentShadow);
        RenderUtils.drawRoundedRect(panelX, panelY, panelW, panelH, CORNER_RADIUS, currentPanel);
        RenderUtils.drawRoundedRectOutline(panelX, panelY, panelW, panelH, CORNER_RADIUS, currentLine, 1.0f);

        int contentMouseX = profilesOpen ? -1 : mouseX;
        int contentMouseY = profilesOpen ? -1 : mouseY;
        
        drawHeader(mouseX, mouseY);
        drawSidebar(contentMouseX, contentMouseY);
        if (selectedModule == null) drawModulePage(contentMouseX, contentMouseY);
        else drawSettingsPage(contentMouseX, contentMouseY);
        
        drawProfilesDropdown(mouseX, mouseY);
        
        GL11.glPopMatrix();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void updateScrollAnimation() {
        long now = System.nanoTime();
        float deltaSeconds = lastScrollFrameNanos == 0L ? 1.0F / 60.0F
                : Math.min(0.05F, Math.max(0.0F, (now - lastScrollFrameNanos) / 1000000000.0F));
        lastScrollFrameNanos = now;
        float amount = (float) (1.0D - Math.exp(-9.0D * deltaSeconds));

        moduleScroll += (targetModuleScroll - moduleScroll) * amount;
        settingsScroll += (targetSettingsScroll - settingsScroll) * amount;
        if (Math.abs(moduleScroll - targetModuleScroll) < 0.03F) moduleScroll = targetModuleScroll;
        if (Math.abs(settingsScroll - targetSettingsScroll) < 0.03F) settingsScroll = targetSettingsScroll;
    }

    private boolean wasBlurEnabled = false;

    private void updateLayout() {
        panelW = Math.min(680, Math.max(320, width - 48));
        panelH = Math.min(420, Math.max(240, height - 28));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        
        ClickGUIModule config = getClickGuiModule();
        boolean blurEnabled = config != null && config.isBlurEnabled();
        if (blurEnabled != wasBlurEnabled) {
            wasBlurEnabled = blurEnabled;
            if (blurEnabled && OpenGlHelper.shadersSupported && mc.getRenderViewEntity() instanceof EntityPlayer) {
                if (mc.entityRenderer.getShaderGroup() != null) mc.entityRenderer.getShaderGroup().deleteShaderGroup();
                try {
                    mc.entityRenderer.loadShader(new ResourceLocation("shaders/post/blur.json"));
                } catch (Exception ignored) {}
            } else {
                if (mc.entityRenderer.getShaderGroup() != null) mc.entityRenderer.stopUseShader();
            }
        }
    }

    private void doScissor(int x, int y, int w, int h) {
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        
        float cx = this.width / 2f;
        float cy = this.height / 2f;
        
        float x1 = (x - cx) * currentAnimScale + cx;
        float y1 = (y - cy) * currentAnimScale + cy;
        float x2 = (x + w - cx) * currentAnimScale + cx;
        float y2 = (y + h - cy) * currentAnimScale + cy;
        
        int scX = (int) (x1 * scale);
        int scY = (int) (mc.displayHeight - (y2 * scale));
        int scW = (int) ((x2 - x1) * scale);
        int scH = (int) ((y2 - y1) * scale);
        
        GL11.glScissor(scX, scY, scW, scH);
    }

    private void drawHeader(int mouseX, int mouseY) {
        GlStateManager.color(1, 1, 1, 1);
        drawTexture(BRAND_ICON, panelX + 17, panelY + 14, 30, 30);
        fontRendererObj.drawString("YesClient", panelX + 55, panelY + 16, TEXT);
        fontRendererObj.drawString("by yes.dev", panelX + 55, panelY + 31, 0xFFD0D3D8);

        int hudX = panelX + panelW - 60;
        int settingsX = hudX - 74;

         
        com.yesclient.config.Profile activeP = YesClient.getInstance().getProfileManager().getProfile(YesClient.getInstance().getProfileManager().getActiveProfileName());
        String activeProfile = activeP != null ? activeP.getName() : "Default";
        String label = activeProfile.toUpperCase();
        int btnWidth = fontRendererObj.getStringWidth(label) + 48;
        int profilesX = settingsX - btnWidth - 10;

        drawHeaderTab(profilesX, panelY + 18, btnWidth, "", profilesOpen, mouseX, mouseY);

         
        if (activeP != null) {
            try {
                String iconStr = activeP.getIconItem();
                String[] parts = iconStr.split(":");
                net.minecraft.item.Item item = net.minecraft.item.Item.getByNameOrId(parts.length > 1 ? parts[0] + ":" + parts[1] : iconStr);
                if (item != null) {
                    int meta = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                    net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item, 1, meta);
                    if (activeP.hasIconGlint()) {
                        net.minecraft.nbt.NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new net.minecraft.nbt.NBTTagCompound();
                        tag.setTag("ench", new net.minecraft.nbt.NBTTagList());
                        stack.setTagCompound(tag);
                    }
                    net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(profilesX + 6, panelY + 21, 0);
                    mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
                    GlStateManager.popMatrix();
                    net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
                }
            } catch (Exception ignored) {}
        }

        drawCentered(label, profilesX + 26 + (btnWidth - 40) / 2, panelY + 25, profilesOpen ? TEXT : TEXT_MUTED);

        drawHeaderTab(settingsX, panelY + 18, 66, "SETTINGS", appearancePage, mouseX, mouseY);
        drawHeaderTab(hudX, panelY + 18, 48, "HUD", false, mouseX, mouseY);
        RenderUtils.drawRect(panelX + 1, panelY + HEADER_HEIGHT, panelW - 2, 1, currentLine);
    }
    
    private void drawProfilesDropdown(int mouseX, int mouseY) {
        if (!profilesOpen) return;
        
        int hudX = panelX + panelW - 60;
        int settingsX = hudX - 74;
        String activeProfileStr = YesClient.getInstance().getProfileManager().getActiveProfileName();
        int btnWidth = fontRendererObj.getStringWidth(activeProfileStr.toUpperCase()) + 48;
        int profilesX = settingsX - btnWidth - 10;
        
        int y = panelY + 42;
        int w = Math.max(btnWidth, 120);  
        
        java.util.Collection<com.yesclient.config.Profile> profiles = YesClient.getInstance().getProfileManager().getProfiles();
        int h = (profiles.size() + 1) * 22 + 4;
        
        RenderUtils.drawRoundedRect(profilesX, y, w, h, 6, 0xFF15171A);
        RenderUtils.drawRoundedRectOutline(profilesX, y, w, h, 6, currentLine, 1.0f);
        
        int currentY = y + 2;
        for (com.yesclient.config.Profile p : profiles) {
            boolean active = p.getName().equals(YesClient.getInstance().getProfileManager().getActiveProfileName());
            boolean hover = inside(mouseX, mouseY, profilesX + 2, currentY, w - 4, 22);
            if (hover) RenderUtils.drawRoundedRect(profilesX + 2, currentY, w - 4, 22, 4, CARD_HOVER);
            
             
            try {
                String[] iconParts = p.getIconItem().split(":");
                net.minecraft.item.Item item = net.minecraft.item.Item.getByNameOrId(iconParts.length > 1 ? iconParts[0] + ":" + iconParts[1] : p.getIconItem());
                if (item != null) {
                    int meta = iconParts.length > 2 ? Integer.parseInt(iconParts[2]) : 0;
                    net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item, 1, meta);
                    
                    if (p.hasIconGlint()) {
                        net.minecraft.nbt.NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new net.minecraft.nbt.NBTTagCompound();
                        tag.setTag("ench", new net.minecraft.nbt.NBTTagList());
                        stack.setTagCompound(tag);
                    }
                    
                    net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(profilesX + 6, currentY + 3, 0);
                    float scale = 1.0f;  
                    GlStateManager.scale(scale, scale, 1.0f);
                    mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
                    GlStateManager.popMatrix();
                    net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
                }
            } catch (Exception ignored) {}
            
            fontRendererObj.drawString(trim(p.getName(), w - 34), profilesX + 28, currentY + 7, active ? ACCENT : TEXT_MUTED);
            
             
            if (!"Default".equals(p.getName()) && hover) {
                boolean editHover = inside(mouseX, mouseY, profilesX + w - 20, currentY + 2, 16, 18);
                GlStateManager.color(1, 1, 1, 1);
                fontRendererObj.drawString("x", profilesX + w - 16, currentY + 7, editHover ? 0xFFFF5555 : 0xFFAAAAAA);
            }
            
            currentY += 22;
        }
        
        boolean addHover = inside(mouseX, mouseY, profilesX + 2, currentY, w - 4, 22);
        if (addHover) RenderUtils.drawRoundedRect(profilesX + 2, currentY, w - 4, 22, 4, CARD_HOVER);
        drawCentered("+ ADD", profilesX + w / 2, currentY + 7, addHover ? TEXT : TEXT_MUTED);
    }

    private void drawHeaderTab(int x, int y, int w, String label, boolean active, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, w, 22);
        if (active || hover) RenderUtils.drawRoundedRect(x, y, w, 22, 6, active ? CONTROL : CARD_HOVER);
        drawCentered(label, x + w / 2, y + 7, active ? TEXT : TEXT_MUTED);
    }

    private void drawSidebar(int mouseX, int mouseY) {
        RenderUtils.drawRoundedRect(panelX, panelY + HEADER_HEIGHT, SIDEBAR_WIDTH,
                panelH - HEADER_HEIGHT, CORNER_RADIUS, currentSidebar, false, false, false, true);
        fontRendererObj.drawString("CATEGORIES", panelX + 16, panelY + 70, TEXT_DIM);

        int startY = panelY + 82;
        float targetIndicatorY = -1;
        
         
        if (inside(mouseX, mouseY, panelX + 10, startY, SIDEBAR_WIDTH - 20, 22)) {
            RenderUtils.drawRoundedRect(panelX + 10, startY, SIDEBAR_WIDTH - 20, 22, 6, CARD_HOVER);
        }
        
        if (selectedCategory == null) targetIndicatorY = startY;

        int calcY = startY + 24;
        for (Category category : SIDEBAR_CATEGORIES) {
            if (calcY + 22 > panelY + panelH - 8) break;
            
             
            if (inside(mouseX, mouseY, panelX + 10, calcY, SIDEBAR_WIDTH - 20, 22)) {
                RenderUtils.drawRoundedRect(panelX + 10, calcY, SIDEBAR_WIDTH - 20, 22, 6, CARD_HOVER);
            }
            
            if (selectedCategory == category) {
                targetIndicatorY = calcY;
            }
            calcY += 24;
        }

         
        if (targetIndicatorY != -1) {
            if (sidebarIndicatorY == -1) sidebarIndicatorY = targetIndicatorY;
            else sidebarIndicatorY += (targetIndicatorY - sidebarIndicatorY) * 0.35f;
            
            RenderUtils.drawRoundedRect(panelX + 10, sidebarIndicatorY, SIDEBAR_WIDTH - 20, 22, 6, CONTROL);
            RenderUtils.drawCircle(panelX + 22, sidebarIndicatorY + 11, 3, ACCENT);
        }

        int y = startY;
        drawCategoryRow(y, null, "All modules", mouseX, mouseY);
        y += 24;
        for (Category category : SIDEBAR_CATEGORIES) {
            if (y + 22 > panelY + panelH - 8) break;
            drawCategoryRow(y, category, category.getDisplayName(), mouseX, mouseY);
            y += 24;
        }
    }

    private void drawCategoryRow(int y, Category category, String label, int mouseX, int mouseY) {
        boolean active = selectedCategory == category;
        
        if (!active) {
            RenderUtils.drawCircle(panelX + 22, y + 11, 3, TEXT_DIM);
        }
        fontRendererObj.drawString(label, panelX + 32, y + 7, active ? TEXT : TEXT_MUTED);
        int count = 0;
        for (com.yesclient.module.Module m : YesClient.getInstance().getModuleManager().getModules()) {
            if (m.isVisible() && (category == null || m.getCategory() == category)) {
                count++;
            }
        }
        String number = String.valueOf(count);
        fontRendererObj.drawString(number, panelX + SIDEBAR_WIDTH - 16 - fontRendererObj.getStringWidth(number), y + 7,
                TEXT_DIM);
    }

    private void drawModulePage(int mouseX, int mouseY) {
        int x = panelX + SIDEBAR_WIDTH + 20;
        int w = panelW - SIDEBAR_WIDTH - 36;
        List<Module> modules = filteredModules();

        fontRendererObj.drawString(selectedCategory == null ? "Modules" : selectedCategory.getDisplayName(), x,
                panelY + 70, TEXT);
        fontRendererObj.drawString(modules.size() + " modules", x, panelY + 85, TEXT_DIM);
        int searchWidth = Math.min(154, Math.max(100, w));
        drawSearch(x + w - searchWidth, panelY + 64, searchWidth, mouseX, mouseY);
        drawModuleCards(x, panelY + 108, w, modules, mouseX, mouseY);
    }

    private void drawSearch(int x, int y, int w, int mouseX, int mouseY) {
        searchBarX = x;
        searchBarY = y;
        searchBarW = w;
        boolean hover = inside(mouseX, mouseY, x, y, w, 22);
        RenderUtils.drawRoundedRect(x, y, w, 22, 7, hover ? CARD_HOVER : CARD);
         
         
         
        int fieldWidth = w - 16;
        searchField.xPosition = x + 8;
         
         
        searchField.yPosition = y + 7;
        searchField.width = fieldWidth;
        searchField.height = 10;
        searchField.setTextColor(TEXT);
        searchField.drawTextBox();
        if (searchField.getText().isEmpty() && !searchField.isFocused()) {
            fontRendererObj.drawString("Search modules", x + 8, y + 7, TEXT_DIM);
        }
    }

    private void drawModuleCards(int x, int y, int w, List<Module> modules, int mouseX, int mouseY) {
        int columns = w >= 500 ? 2 : 1;
        int cardW = (w - (columns - 1) * CARD_GAP) / columns;
        int viewport = Math.max(1, panelY + panelH - y - 22);
        int rows = (modules.size() + columns - 1) / columns;
        int totalHeight = Math.max(0, rows * (CARD_HEIGHT + CARD_GAP) - CARD_GAP);
        setModuleScrollBounds(totalHeight, viewport);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        doScissor(x, y, w, viewport);
        float wholeScroll = (float) Math.floor(moduleScroll);
        float fraction = moduleScroll - wholeScroll;
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, -fraction, 0.0F);
        for (int i = 0; i < modules.size(); i++) {
            int cardX = x + (i % columns) * (cardW + CARD_GAP);
            int cardY = y + (i / columns) * (CARD_HEIGHT + CARD_GAP) - (int) wholeScroll;
            drawModuleCard(modules.get(i), cardX, cardY, cardW, mouseX, mouseY);
        }
        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawModuleCard(Module module, int x, int y, int w, int mouseX, int mouseY) {
        if (y + CARD_HEIGHT < panelY + 108 || y > panelY + panelH) return;
        boolean locked = isBlatantLocked(module);
        
        boolean hasKeybindBtn = module.getSettings().isEmpty() && module.allowsModuleKeybind();
        boolean keybindHover = hasKeybindBtn && inside(mouseX, mouseY, x + w - 48, y + 39, 40, 18);
        boolean hover = !locked && inside(mouseX, mouseY, x, y, w, CARD_HEIGHT) && !keybindHover;
        
        float anim = cardHoverAnim.containsKey(module) ? cardHoverAnim.get(module) : 0f;
        anim += ((hover ? 1f : 0f) - anim) * 0.2f;
        cardHoverAnim.put(module, anim);
        
        int bg = locked ? 0xFF1A1C20 : RenderUtils.blendColors(CARD, CARD_HOVER, anim);
        
        RenderUtils.drawRoundedRect(x, y, w, CARD_HEIGHT, 8, bg);
        if (module.isEnabled() && !locked) {
            RenderUtils.drawRoundedRect(x, y, 4, CARD_HEIGHT, 4, ACCENT, true, false, false, true);
        }
        
        if (module.getIcon() != null) {
            GlStateManager.color(1, 1, 1, locked ? 0.3f : 1.0f);
            drawTexture(module.getIcon(), x + w - 38, y + 8, 28, 28);
            GlStateManager.color(1, 1, 1, 1);
        }

        int titleColor = locked ? TEXT_DIM : TEXT;
        int descriptionColor = locked ? 0xFF555A62 : TEXT_MUTED;
        fontRendererObj.drawString(module.getDisplayName(), x + 12, y + 11, titleColor);
        fontRendererObj.drawString(locked ? "BLATANT MODULE" : module.getCategory().getDisplayName().toUpperCase(),
                x + 12, y + 26, locked ? 0xFF777C84 : TEXT_DIM);
        fontRendererObj.drawString(trim(module.getDescription(), Math.max(70, w - 78)), x + 12, y + 44,
                descriptionColor);

        if (locked) drawCentered("LOCKED", x + w - 25, y + 44, 0xFF555A62);
        else if (!module.getSettings().isEmpty()) drawChevronButton(x + w - 39, y + 39, mouseX, mouseY);
        else if (module.allowsModuleKeybind()) drawKeybindControl(x + w - 48, y + 39, 40, 18, module, mouseX, mouseY);
    }

    private void drawChevronButton(int x, int y, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, 28, 16);
        fontRendererObj.drawString(">", x + 12, y + 4, hover ? ACCENT : TEXT_MUTED);
    }

    private void drawKeybindControl(int x, int y, int w, int h, Module module, int mouseX, int mouseY) {
        boolean active = bindingModule == module;
        boolean hover = !isBlatantLocked(module) && inside(mouseX, mouseY, x, y, w, h);
        RenderUtils.drawRoundedRect(x, y, w, h, 6, active ? ACCENT : (hover ? CARD_HOVER : CONTROL));
        String label = active ? "..." : module.getKeybindDisplay().replace("[", "").replace("]", "").toUpperCase();
        label = MarqueeText.fit(label, fontRendererObj, w - 10, module);
        drawCentered(label, x + w / 2, y + (h / 2) - 3, active ? ACCENT_DARK : TEXT_MUTED);
    }

    private boolean isBlatantLocked(Module module) {
        return module != null && module.isBlatant() && YesClient.getInstance().isSafeMode();
    }

    private void drawSettingsPage(int mouseX, int mouseY) {
        int x = panelX + SIDEBAR_WIDTH + 20;
        int w = panelW - SIDEBAR_WIDTH - 36;
        String heading = appearancePage ? "Settings" : selectedModule.getDisplayName();
        String description = appearancePage ? "Interface preferences" : trim(selectedModule.getDescription(), w - 8);

        boolean backHover = inside(mouseX, mouseY, x, panelY + 64, 52, 20);
        if (backHover) RenderUtils.drawRoundedRect(x, panelY + 64, 52, 20, 6, CARD_HOVER);
        drawCentered("< BACK", x + 26, panelY + 70, backHover ? TEXT : TEXT_MUTED);
        fontRendererObj.drawString(heading, x, panelY + 94, TEXT);
        fontRendererObj.drawString(description, x, panelY + 108, TEXT_DIM);

        int summaryY = panelY + 124;
        RenderUtils.drawRoundedRect(x, summaryY, w, 48, 8, CARD);
        if (!appearancePage && selectedModule.isEnabled() && !isBlatantLocked(selectedModule)) {
            RenderUtils.drawRoundedRect(x, summaryY, 4, 48, 4, ACCENT, true, false, false, true);
        }
        fontRendererObj.drawString(appearancePage ? "Global interface" : selectedModule.getDisplayName(), x + 12,
                summaryY + 12, TEXT);
        fontRendererObj.drawString(appearancePage ? "YesClient visual system" : selectedModule.getCategory().getDisplayName(),
                x + 12, summaryY + 28, TEXT_DIM);
        if (!appearancePage && selectedModule.allowsModuleKeybind()) {
            drawKeybindControl(x + w - 72, summaryY + 13, 64, 22, selectedModule, mouseX, mouseY);
        }

        List<Setting<?>> settings = visibleSettings(selectedModule);
        int top = summaryY + 58;
        int viewport = Math.max(1, panelY + panelH - top - 12);
        int total = 0;
        for (Setting<?> s : settings) total += rowHeight(s) + 6;
        setSettingsScrollBounds(total, viewport);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        doScissor(x, top, w, viewport);
        float wholeScroll = (float) Math.floor(settingsScroll);
        float fraction = settingsScroll - wholeScroll;
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, -fraction, 0.0F);
        int rowY = top - (int) wholeScroll;
        for (Setting<?> s : settings) {
            int h = rowHeight(s);
            drawSettingRow(s, x, rowY, w, h, mouseX, mouseY);
            rowY += h + 6;
        }
        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

     

    private static final int PICKER_HEIGHT = 128;
    private static final int SWATCH = 14;
    private static final int[] PRESET_COLORS = new int[] {
            0xFFFFFFFF, 0xFFAAAAAA, 0xFF000000, 0xFFFF3B3B, 0xFFFF8C1A, 0xFFFFE135, 0xFF7CFF4F,
            0xFF1FB84A, 0xFF2EE6E6, 0xFF2F6BFF, 0xFF8A3BFF, 0xFFFF3BD8, 0xFFFF8FB8
    };
    private final java.util.Set<ColorSetting> expandedColors = new java.util.HashSet<ColorSetting>();
    private final HashMap<ColorSetting, float[]> colorHsb = new HashMap<ColorSetting, float[]>();
    private ColorSetting draggingColor;
    private int draggingColorPart;  
    private int dragPickX, dragPickY, dragPickW, dragPickH;

    private int rowHeight(Setting<?> setting) {
        boolean open = setting instanceof ColorSetting && expandedColors.contains(setting) && setting.isAvailable();
        return SETTING_HEIGHT + (open ? PICKER_HEIGHT : 0);
    }

     
    private float[] hsbOf(ColorSetting color) {
        int rgb = color.getValue().getRGB() & 0xFFFFFF;
        float[] cached = colorHsb.get(color);
        if (cached == null || (int) cached[3] != rgb) {
            float[] hsb = java.awt.Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, null);
            float hue = cached != null && (hsb[1] == 0 || hsb[2] == 0) ? cached[0] : hsb[0];
            cached = new float[] { hue, hsb[1], hsb[2], rgb };
            colorHsb.put(color, cached);
        }
        return cached;
    }

    private void setColorHsb(ColorSetting color, float h, float s, float b) {
        int rgb = java.awt.Color.HSBtoRGB(h, s, b) & 0xFFFFFF;
        color.setRainbow(false);
        color.setValue(new java.awt.Color(rgb | (color.getAlpha() << 24), true));
        colorHsb.put(color, new float[] { h, s, b, rgb });
    }

    private int pickerSvW(int w) {
        return Math.max(80, Math.min(180, w - 130));
    }

    private void drawColorPicker(ColorSetting color, int x, int y, int w, int mouseX, int mouseY) {
        int baseY = y + SETTING_HEIGHT - 4;
        RenderUtils.drawRect(x + 12, baseY - 3, w - 24, 1, LINE);

         
        int count = Math.min(PRESET_COLORS.length, Math.max(1, (w - 24) / (SWATCH + 5) - 1));
        int sx = x + 12;
        int sy = baseY + 4;
        int current = color.getValue().getRGB() | 0xFF000000;
        for (int i = 0; i < count; i++) {
            int c = PRESET_COLORS[i];
            boolean sel = !color.isRainbow() && current == c;
            boolean hov = inside(mouseX, mouseY, sx, sy, SWATCH, SWATCH);
            if (sel || hov) RenderUtils.drawRoundedRect(sx - 2, sy - 2, SWATCH + 4, SWATCH + 4, 5, sel ? TEXT : TEXT_DIM);
            RenderUtils.drawRoundedRect(sx, sy, SWATCH, SWATCH, 4, c);
            sx += SWATCH + 5;
        }
        boolean rbHov = inside(mouseX, mouseY, sx, sy, SWATCH, SWATCH);
        if (color.isRainbow() || rbHov) RenderUtils.drawRoundedRect(sx - 2, sy - 2, SWATCH + 4, SWATCH + 4, 5, color.isRainbow() ? TEXT : TEXT_DIM);
        for (int i = 0; i < SWATCH; i++) {
            int c = java.awt.Color.HSBtoRGB(i / (float) SWATCH, 0.8f, 1f);
            RenderUtils.drawRect(sx + i, sy, 1, SWATCH, c);
        }

        float[] hsb = hsbOf(color);
        int svX = x + 12, svY = sy + SWATCH + 8, svW = pickerSvW(w), svH = 92;
        int hueColor = java.awt.Color.HSBtoRGB(hsb[0], 1f, 1f) | 0xFF000000;
        drawHorizontalGradient(svX, svY, svW, svH, 0xFFFFFFFF, hueColor);
        drawGradientRect(svX, svY, svX + svW, svY + svH, 0x00000000, 0xFF000000);
        int mx = svX + Math.round(hsb[1] * svW);
        int my = svY + Math.round((1 - hsb[2]) * svH);
        RenderUtils.drawCircle(mx, my, 4, 0xFF000000);
        RenderUtils.drawCircle(mx, my, 3, 0xFFFFFFFF);
        RenderUtils.drawCircle(mx, my, 2, current);

        int hueX = svX + svW + 8, hueW = 12;
        int seg = 6;
        for (int i = 0; i < seg; i++) {
            int top = svY + svH * i / seg;
            int bottom = svY + svH * (i + 1) / seg;
            int c1 = java.awt.Color.HSBtoRGB(i / (float) seg, 1f, 1f) | 0xFF000000;
            int c2 = java.awt.Color.HSBtoRGB((i + 1) / (float) seg, 1f, 1f) | 0xFF000000;
            drawGradientRect(hueX, top, hueX + hueW, bottom, c1, c2);
        }
        int hy = svY + Math.round(hsb[0] * svH);
        RenderUtils.drawRect(hueX - 2, hy - 1, hueW + 4, 3, 0xFF000000);
        RenderUtils.drawRect(hueX - 1, hy, hueW + 2, 1, 0xFFFFFFFF);

        int infoX = hueX + hueW + 14;
        RenderUtils.drawRoundedRect(infoX, svY, 34, 34, 6, color.getRGB() | 0xFF000000);
        fontRendererObj.drawString(color.isRainbow() ? "RAINBOW" : hex(color), infoX, svY + 42, TEXT_MUTED);
        fontRendererObj.drawString("Click header", infoX, svY + svH - 18, TEXT_DIM);
        fontRendererObj.drawString("to close", infoX, svY + svH - 8, TEXT_DIM);
    }

     
    private boolean handleColorPickerClick(ColorSetting color, int x, int y, int w, int mouseX, int mouseY) {
        int baseY = y + SETTING_HEIGHT - 4;
        int count = Math.min(PRESET_COLORS.length, Math.max(1, (w - 24) / (SWATCH + 5) - 1));
        int sx = x + 12, sy = baseY + 4;
        for (int i = 0; i < count; i++) {
            if (inside(mouseX, mouseY, sx, sy, SWATCH, SWATCH)) {
                int c = PRESET_COLORS[i];
                color.setRainbow(false);
                color.setValue(new java.awt.Color((c & 0xFFFFFF) | (color.getAlpha() << 24), true));
                return true;
            }
            sx += SWATCH + 5;
        }
        if (inside(mouseX, mouseY, sx, sy, SWATCH, SWATCH)) {
            color.setRainbow(!color.isRainbow());
            return true;
        }
        int svX = x + 12, svY = sy + SWATCH + 8, svW = pickerSvW(w), svH = 92;
        if (inside(mouseX, mouseY, svX, svY, svW, svH)) {
            beginColorDrag(color, 1, svX, svY, svW, svH, mouseX, mouseY);
            return true;
        }
        int hueX = svX + svW + 8;
        if (inside(mouseX, mouseY, hueX - 2, svY, 16, svH)) {
            beginColorDrag(color, 2, hueX, svY, 12, svH, mouseX, mouseY);
            return true;
        }
        return true;  
    }

    private void beginColorDrag(ColorSetting color, int part, int px, int py, int pw, int ph, int mouseX, int mouseY) {
        draggingColor = color;
        draggingColorPart = part;
        dragPickX = px;
        dragPickY = py;
        dragPickW = pw;
        dragPickH = ph;
        updateColorDrag(mouseX, mouseY);
    }

    private void updateColorDrag(int mouseX, int mouseY) {
        if (draggingColor == null) return;
        float[] hsb = hsbOf(draggingColor);
        float fx = Math.max(0f, Math.min(1f, (mouseX - dragPickX) / (float) dragPickW));
        float fy = Math.max(0f, Math.min(1f, (mouseY - dragPickY) / (float) dragPickH));
        if (draggingColorPart == 1) setColorHsb(draggingColor, hsb[0], fx, 1f - fy);
        else setColorHsb(draggingColor, Math.min(fy, 0.999f), hsb[1], hsb[2]);
    }

    private static String hex(ColorSetting color) {
        return String.format("#%06X", color.getValue().getRGB() & 0xFFFFFF);
    }

    private void drawHorizontalGradient(int x, int y, int w, int h, int left, int right) {
        net.minecraft.client.renderer.Tessellator tess = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer wr = tess.getWorldRenderer();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        wr.begin(GL11.GL_QUADS, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x, y, 0).color(left >> 16 & 255, left >> 8 & 255, left & 255, left >>> 24).endVertex();
        wr.pos(x, y + h, 0).color(left >> 16 & 255, left >> 8 & 255, left & 255, left >>> 24).endVertex();
        wr.pos(x + w, y + h, 0).color(right >> 16 & 255, right >> 8 & 255, right & 255, right >>> 24).endVertex();
        wr.pos(x + w, y, 0).color(right >> 16 & 255, right >> 8 & 255, right & 255, right >>> 24).endVertex();
        tess.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    private void drawChevron(int cx, int cy, boolean up, int color) {
        int d = up ? -1 : 1;
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.disableTexture2D();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_LINE_SMOOTH);
        org.lwjgl.opengl.GL11.glLineWidth(1.5f);
        net.minecraft.client.renderer.GlStateManager.color(red, green, blue, alpha);
        
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINES);
        org.lwjgl.opengl.GL11.glVertex2f(cx - 4, cy - 2 * d);
        org.lwjgl.opengl.GL11.glVertex2f(cx, cy + 2 * d);
        org.lwjgl.opengl.GL11.glVertex2f(cx, cy + 2 * d);
        org.lwjgl.opengl.GL11.glVertex2f(cx + 4, cy - 2 * d);
        org.lwjgl.opengl.GL11.glEnd();
        
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LINE_SMOOTH);
        net.minecraft.client.renderer.GlStateManager.enableTexture2D();
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }

     

    private void drawSettingRow(Setting<?> setting, int x, int y, int w, int height, int mouseX, int mouseY) {
        if (y + height < panelY + 182 || y > panelY + panelH) return;
        boolean available = setting.isAvailable();
        boolean hover = available && inside(mouseX, mouseY, x, y, w, Math.min(height, SETTING_HEIGHT));

        String key = setting.getName() + hashCode();
        float anim = settingHoverAnim.containsKey(key) ? settingHoverAnim.get(key) : 0f;
        anim += ((hover ? 1f : 0f) - anim) * 0.2f;
        settingHoverAnim.put(key, anim);

        int bg = RenderUtils.blendColors(CARD, CARD_HOVER, anim);

        RenderUtils.drawRoundedRect(x, y, w, height, 7, bg);
        if (available && setting instanceof BooleanSetting && ((BooleanSetting) setting).isEnabled()) {
            RenderUtils.drawRoundedRect(x, y, 4, SETTING_HEIGHT, 4, ACCENT, true, false, false, true);
        }
        fontRendererObj.drawString(setting.getName(), x + 12, y + 10, available ? TEXT : TEXT_DIM);
        String desc = available ? setting.getDescription() : "Not used by this preset";
        if (!available && setting instanceof ColorSetting) desc = "Disabled by another option";
        fontRendererObj.drawString(trim(desc, Math.max(60, w - 200)), x + 12, y + 26, available ? TEXT_DIM : 0xFF50545B);
        drawSettingControl(setting, x + w - 148, y + 10, 136, 30, mouseX, mouseY);
        if (setting instanceof ColorSetting && height > SETTING_HEIGHT) {
            drawColorPicker((ColorSetting) setting, x, y, w, mouseX, mouseY);
        }
        if (!available) {
             
            RenderUtils.drawRoundedRect(x, y, w, height, 7, 0x9015171A);
        }
    }

    private void drawSettingControl(Setting<?> setting, int x, int y, int w, int h, int mouseX, int mouseY) {
        if (setting instanceof BooleanSetting) {
             
             
        } else if (setting instanceof ActionSetting) {
            ActionSetting action = (ActionSetting) setting;
            boolean hov = setting.isAvailable() && inside(mouseX, mouseY, x + 56, y, w - 56, h);
            RenderUtils.drawRoundedRect(x + 56, y, w - 56, h, 6, hov ? CARD_HOVER : CONTROL);
            if (hov) RenderUtils.drawRoundedRectOutline(x + 56, y, w - 56, h, 6, ACCENT, 1.0f);
            drawCentered(action.getButtonLabel(), x + 56 + (w - 56) / 2, y + 11, hov ? TEXT : TEXT_MUTED);
        } else if (setting instanceof ModeSetting) {
            RenderUtils.drawRoundedRect(x + 16, y, w - 16, h, 6, CONTROL);
            drawCentered(trim(((ModeSetting) setting).getValue(), 16).toUpperCase(), x + 16 + (w - 16) / 2,
                    y + 11, TEXT_MUTED);
        } else if (setting instanceof NumberSetting) {
            NumberSetting number = (NumberSetting) setting;
            double range = Math.max(0.0001, number.getMax() - number.getMin());
            double progress = Math.max(0, Math.min(1, (number.getValue() - number.getMin()) / range));
            RenderUtils.drawRoundedRect(x + 4, y + 13, w - 4, 4, 2, CONTROL);
            RenderUtils.drawRoundedRect(x + 4, y + 13, (w - 4) * progress, 4, 2, ACCENT);
            RenderUtils.drawCircle(x + 4 + (w - 4) * progress, y + 15, 4, TEXT);
            drawCentered(format(number.getValue()), x + w - 20, y + 1, TEXT_MUTED);
        } else if (setting instanceof ColorSetting) {
            ColorSetting color = (ColorSetting) setting;
            boolean open = expandedColors.contains(color) && color.isAvailable();
            RenderUtils.drawRoundedRect(x + 16, y, w - 16, h, 6, CONTROL);
            RenderUtils.drawRoundedRect(x + 22, y + 5, 26, 20, 5, color.getRGB() | 0xFF000000);
            fontRendererObj.drawString(color.isRainbow() ? "RAINBOW" : hex(color), x + 56, y + 11, TEXT_MUTED);
            drawChevron(x + w - 12, y + 15, open, TEXT_MUTED);
        } else if (setting instanceof com.yesclient.module.settings.EntityListSetting) {
            com.yesclient.module.settings.EntityListSetting list = (com.yesclient.module.settings.EntityListSetting) setting;
            String val = list.getValue();
            int count = val.isEmpty() ? 0 : val.split(",").length;
            int controlX = x + w - 82;
            RenderUtils.drawRoundedRect(controlX, y, 82, h, 6, CONTROL);
            fontRendererObj.drawString(count > 0 ? count + " Entities" : "SELECT", controlX + 8, y + 11, count > 0 ? TEXT_MUTED : TEXT_DIM);
        } else if (setting instanceof StringSetting) {
            StringSetting string = (StringSetting) setting;
            RenderUtils.drawRoundedRect(x, y, w, h, 6, CONTROL);
            if (editingString == string && stringField != null) {
                stringField.xPosition = x + 6;
                stringField.yPosition = y + 5;
                stringField.width = w - 12;
                stringField.height = 20;
                stringField.drawTextBox();
            } else drawCentered(trim(string.getValue().isEmpty() ? "Click to edit" : string.getValue(), 18), x + w / 2,
                    y + 11, string.getValue().isEmpty() ? TEXT_DIM : TEXT_MUTED);
        } else if (setting instanceof ItemSetting) {
            ItemSetting item = (ItemSetting) setting;
            boolean selected = item.getValue() != null;
            RenderUtils.drawRoundedRect(x, y, 82, h, 6, CONTROL);
            fontRendererObj.drawString(selected ? trim(item.getValue().getDisplayName(), 11) : "SELECT ITEM",
                    x + 8, y + 11, selected ? TEXT_MUTED : TEXT_DIM);
            
            if (item.hasKeybind()) {
                boolean active = bindingItemSetting == item;
                RenderUtils.drawRoundedRect(x + 88, y, 48, h, 6, active ? ACCENT : CONTROL);
                String key = active ? "..." : (item.getKeybind() == 0 ? "NONE" : KeybindManager.getKeyName(item.getKeybind()));
                key = MarqueeText.fit(key.toUpperCase(), fontRendererObj, 40, item);
                drawCentered(key, x + 112, y + 11, active ? ACCENT_DARK : TEXT_MUTED);
            }
        } else if (setting instanceof ListSetting) {
            drawCentered(((ListSetting) setting).size() + " ENTRIES", x + w / 2, y + 11, TEXT_DIM);
        } else drawCentered("CONFIGURE", x + w / 2, y + 11, TEXT_DIM);
    }

    private List<Module> filteredModules() {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        List<Module> result = new ArrayList<Module>();
        for (Module module : YesClient.getInstance().getModuleManager().getModules()) {
            if (!module.isVisible()) continue;
            if (selectedCategory != null && module.getCategory() != selectedCategory) continue;
            if (!query.isEmpty() && !module.getDisplayName().toLowerCase().contains(query)
                    && !module.getDescription().toLowerCase().contains(query)) continue;
            result.add(module);
        }
        Collections.sort(result, new Comparator<Module>() {
            @Override
            public int compare(Module a, Module b) {
                return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
            }
        });
        return result;
    }

    private List<Setting<?>> visibleSettings(Module module) {
        List<Setting<?>> result = new ArrayList<Setting<?>>();
        if (module == null) return result;
        for (Setting<?> setting : module.getSettings()) if (setting.isVisible()) result.add(setting);
        return result;
    }

    private void setModuleScrollBounds(int totalHeight, int viewport) {
        float max = Math.max(0, totalHeight - viewport);
        targetModuleScroll = Math.max(0, Math.min(targetModuleScroll, max));
        moduleScroll = Math.max(0, Math.min(moduleScroll, max));
    }

    private void setSettingsScrollBounds(int totalHeight, int viewport) {
        float max = Math.max(0, totalHeight - viewport);
        targetSettingsScroll = Math.max(0, Math.min(targetSettingsScroll, max));
        settingsScroll = Math.max(0, Math.min(settingsScroll, max));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        updateLayout();
        
        int hudX = panelX + panelW - 60;
        int settingsX = hudX - 74;
        String activeProfileStr = YesClient.getInstance().getProfileManager().getActiveProfileName();
        int btnWidth = fontRendererObj.getStringWidth(activeProfileStr.toUpperCase()) + 48;
        int profilesX = settingsX - btnWidth - 10;

        if (profilesOpen) {
            int y = panelY + 42;
            int w = Math.max(btnWidth, 120);
            java.util.Collection<com.yesclient.config.Profile> profiles = YesClient.getInstance().getProfileManager().getProfiles();
            int h = (profiles.size() + 1) * 22 + 4;
            
            if (inside(mouseX, mouseY, profilesX, y, w, h)) {
                int currentY = y + 2;
                for (com.yesclient.config.Profile p : profiles) {
                    if (inside(mouseX, mouseY, profilesX + 2, currentY, w - 4, 22)) {
                        boolean editHover = inside(mouseX, mouseY, profilesX + w - 20, currentY + 2, 16, 18);
                        
                        if (!"Default".equals(p.getName()) && editHover && mouseButton == 0) {
                            com.yesclient.utils.InterfaceSounds.playClick();
                            YesClient.getInstance().getProfileManager().deleteProfile(p.getName());
                            profilesOpen = false;
                            return;
                        } else if (mouseButton == 0) {
                            com.yesclient.utils.InterfaceSounds.playClick();
                            YesClient.getInstance().getProfileManager().setActiveProfile(p.getName());
                            profilesOpen = false;
                            return;
                        }
                    }
                    currentY += 22;
                }
                if (inside(mouseX, mouseY, profilesX + 2, currentY, w - 4, 22)) {
                    if (mouseButton == 0) com.yesclient.utils.InterfaceSounds.playClick();
                    mc.displayGuiScreen(new com.yesclient.gui.GuiProfileEditor(this));
                    profilesOpen = false;
                    return;
                }
            }
            profilesOpen = false;
            if (inside(mouseX, mouseY, profilesX, panelY + 18, btnWidth, 22)) return;
        }
        
        if (inside(mouseX, mouseY, profilesX, panelY + 18, btnWidth, 22)) {
            if (mouseButton == 0) com.yesclient.utils.InterfaceSounds.playClick();
            profilesOpen = true;
            return;
        }

        if (selectedModule == null && !appearancePage && searchField != null) {
            boolean wasSearchFocused = searchField.isFocused();
            searchField.xPosition = searchBarX + 8;
            searchField.yPosition = searchBarY + 7;
            searchField.width = searchBarW - 16;
            searchField.height = 10;
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
             
             
            if (inside(mouseX, mouseY, searchBarX, searchBarY, searchBarW, 22)) {
                if (!wasSearchFocused && !searchField.getText().isEmpty()) {
                    searchField.setText("");
                    targetModuleScroll = moduleScroll = 0;
                }
                searchField.setFocused(true);
                return;
            } else {
                searchField.setFocused(false);
            }
        }

        
        if (inside(mouseX, mouseY, settingsX, panelY + 18, 66, 22)) {
            if (mouseButton == 0) com.yesclient.utils.InterfaceSounds.playClick();
            Module module = getClickGuiModule();
            if (module != null) openSettings(module, true);
            return;
        }
        if (inside(mouseX, mouseY, hudX, panelY + 18, 48, 22)) {
            if (mouseButton == 0) com.yesclient.utils.InterfaceSounds.playClick();
            HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
            if (designer != null) mc.displayGuiScreen(new HudEditorGUI(designer));
            return;
        }

         
         
        if (handleSidebarClick(mouseX, mouseY, mouseButton)) return;

        if (selectedModule != null) {
            handleSettingsClick(mouseX, mouseY, mouseButton);
            return;
        }

        List<Module> modules = filteredModules();
        int x = panelX + SIDEBAR_WIDTH + 20;
        int w = panelW - SIDEBAR_WIDTH - 36;
        int columns = w >= 500 ? 2 : 1;
        int cardW = (w - (columns - 1) * CARD_GAP) / columns;
        int contentY = panelY + 108;
        for (int i = 0; i < modules.size(); i++) {
            int cardX = x + (i % columns) * (cardW + CARD_GAP);
            int cardY = (int) (contentY + (i / columns) * (CARD_HEIGHT + CARD_GAP) - moduleScroll);
            if (!inside(mouseX, mouseY, cardX, cardY, cardW, CARD_HEIGHT)) continue;
            Module module = modules.get(i);
            if (isBlatantLocked(module)) return;
            boolean setup = !module.getSettings().isEmpty() && inside(mouseX, mouseY, cardX + cardW - 43, cardY + 36, 34, 20);
            boolean keybindClick = module.getSettings().isEmpty() && module.allowsModuleKeybind() && inside(mouseX, mouseY, cardX + cardW - 48, cardY + 39, 40, 18);
            if (setup || (mouseButton == 1 && !module.getSettings().isEmpty())) {
                if (mouseButton == 0) com.yesclient.utils.InterfaceSounds.playClick();
                openSettings(module, false);
            } else if (keybindClick) {
                if (mouseButton == 0) {
                    com.yesclient.utils.InterfaceSounds.playClick();
                    bindingModule = module;
                } else if (mouseButton == 1) module.setKeybind(module.getDefaultKeybind());
            } else if (mouseButton == 0) {
                module.toggle();
            }
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean handleSidebarClick(int mouseX, int mouseY, int mouseButton) {
        int categoryY = panelY + 82;
        if (inside(mouseX, mouseY, panelX + 10, categoryY, SIDEBAR_WIDTH - 20, 22)) {
            if (mouseButton == 0) com.yesclient.utils.InterfaceSounds.playClick();
            selectCategory(null);
            return true;
        }
        categoryY += 24;
        for (Category category : SIDEBAR_CATEGORIES) {
            if (categoryY + 22 > panelY + panelH - 8) break;
            if (inside(mouseX, mouseY, panelX + 10, categoryY, SIDEBAR_WIDTH - 20, 22)) {
                if (mouseButton == 0) com.yesclient.utils.InterfaceSounds.playClick();
                selectCategory(category);
                return true;
            }
            categoryY += 24;
        }
        return false;
    }

    private void selectCategory(Category category) {
        selectedCategory = category;
        selectedModule = null;
        appearancePage = false;
        bindingModule = null;
        bindingItemSetting = null;
        clearStringEditor();
        targetModuleScroll = moduleScroll = 0;
        if (searchField != null) searchField.setFocused(false);
    }

    private void handleSettingsClick(int mouseX, int mouseY, int mouseButton) {
        int x = panelX + SIDEBAR_WIDTH + 20;
        int w = panelW - SIDEBAR_WIDTH - 36;
        if (inside(mouseX, mouseY, x, panelY + 64, 52, 20)) {
            if (returnScreen != null) {
                returnToPreviousScreen();
                return;
            }
            if (mouseButton == 0) com.yesclient.utils.InterfaceSounds.playClick();
            selectedModule = null;
            appearancePage = false;
            bindingModule = null;
            bindingItemSetting = null;
            clearStringEditor();
            if (searchField != null) searchField.setFocused(false);
            return;
        }

        int summaryY = panelY + 124;
        if (!appearancePage && selectedModule.allowsModuleKeybind()
                && inside(mouseX, mouseY, x + w - 72, summaryY + 10, 64, 28)) {
            if (!isBlatantLocked(selectedModule) && selectedModule.allowsModuleKeybind()) {
                if (mouseButton == 0) com.yesclient.utils.InterfaceSounds.playClick();
                bindingModule = selectedModule;
                bindingItemSetting = null;
            }
            return;
        }
        if (!appearancePage && inside(mouseX, mouseY, x, summaryY, w, 48)) {
            if (!isBlatantLocked(selectedModule)) selectedModule.toggle();
            return;
        }

        List<Setting<?>> settings = visibleSettings(selectedModule);
        int top = summaryY + 58;
        if (mouseY < top) return;
        int nextY = (int) (top - settingsScroll);
        for (int i = 0; i < settings.size(); i++) {
            Setting<?> setting = settings.get(i);
            int rowY = nextY;
            int rowH = rowHeight(setting);
            nextY += rowH + 6;
            if (!inside(mouseX, mouseY, x, rowY, w, rowH)) continue;
            if (!setting.isAvailable()) return;  
            if (setting instanceof ColorSetting) {
                ColorSetting color = (ColorSetting) setting;
                if (mouseY <= rowY + SETTING_HEIGHT) {
                    if (mouseButton == 0) com.yesclient.utils.InterfaceSounds.playClick();
                    if (!expandedColors.remove(color)) expandedColors.add(color);
                } else {
                    handleColorPickerClick(color, x, rowY, w, mouseX, mouseY);
                }
                return;
            }
            if (setting instanceof ActionSetting) {
                if (mouseButton == 0) com.yesclient.utils.InterfaceSounds.playClick();
                ((ActionSetting) setting).run();
                return;
            }
            if (setting instanceof BooleanSetting) ((BooleanSetting) setting).toggle();
            else if (setting instanceof ModeSetting) {
                com.yesclient.utils.InterfaceSounds.playClick();
                if (mouseButton == 1) ((ModeSetting) setting).cycleReverse();
                else ((ModeSetting) setting).cycle();
            }
            else if (setting instanceof NumberSetting) {
                NumberSetting number = (NumberSetting) setting;
                int controlX = x + w - 148;
                if (mouseButton == 0 && inside(mouseX, mouseY, controlX, rowY + 6, 136, 38)) {
                    updateNumberSetting(number, mouseX, controlX, 136);
                    draggingNumber = number;
                    draggingControlX = controlX;
                    draggingControlW = 136;
                }
            } else if (setting instanceof com.yesclient.module.settings.EntityListSetting) {
                int controlX = x + w - 82;
                if (inside(mouseX, mouseY, controlX, rowY + 10, 82, 30)) {
                    if (mouseButton == 1) {
                        com.yesclient.utils.InterfaceSounds.playClick();
                        ((com.yesclient.module.settings.EntityListSetting) setting).setValue("");
                    } else if (selectedModule instanceof com.yesclient.module.modules.exploit.ProjectileAimbotModule) {
                        if (mouseButton == 0) com.yesclient.utils.InterfaceSounds.playClick();
                        mc.displayGuiScreen(new com.yesclient.gui.exploit.GuiEntityFilter(this, (com.yesclient.module.modules.exploit.ProjectileAimbotModule) selectedModule));
                    }
                }
            } else if (setting instanceof StringSetting) beginStringEdit((StringSetting) setting);
            else if (setting instanceof ItemSetting) {
                ItemSetting item = (ItemSetting) setting;
                int controlX = x + w - 148;
                if (item.hasKeybind() && inside(mouseX, mouseY, controlX + 88, rowY + 10, 48, 30)) {
                    if (mouseButton == 0) com.yesclient.utils.InterfaceSounds.playClick();
                    bindingItemSetting = item;
                    bindingModule = null;
                } else if (inside(mouseX, mouseY, controlX, rowY + 10, 82, 30)) {
                    if (mouseButton == 1 && item.getValue() != null) {
                        com.yesclient.utils.InterfaceSounds.playClick();
                        item.setValue(null);
                    } else {
                        if (mouseButton == 0) com.yesclient.utils.InterfaceSounds.playClick();
                        mc.displayGuiScreen(new ItemSelectorGUI(this, item));
                    }
                }
            }
            return;
        }
        clearStringEditor();
    }

    private void updateNumberSetting(NumberSetting number, int mouseX, int controlX, int controlW) {
        double previousValue = number.getValue();
        double ratio = Math.max(0.0, Math.min(1.0, (mouseX - controlX) / (double) controlW));
        number.setValue(number.getMin() + (number.getMax() - number.getMin()) * ratio);
        if (Double.compare(previousValue, number.getValue()) != 0) {
            com.yesclient.utils.InterfaceSounds.playSliderTick();
        }
    }

    private void beginStringEdit(StringSetting setting) {
        editingString = setting;
        if (searchField != null) searchField.setFocused(false);
        stringField = new GuiTextField(12, fontRendererObj, 0, 0, 120, 20);
        stringField.setMaxStringLength(setting.getMaxLength());
        stringField.setText(setting.getValue());
        stringField.setEnableBackgroundDrawing(false);
        stringField.setFocused(true);
    }

    private void clearStringEditor() {
        editingString = null;
        stringField = null;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (bindingModule != null) {
                bindingModule.setKeybind(0);
                bindingModule = null;
                return;
            }
            if (bindingItemSetting != null) {
                bindingItemSetting.setKeybind(0);
                bindingItemSetting = null;
                YesClient.getInstance().saveAll();
                return;
            }
            if (returnScreen != null) {
                returnToPreviousScreen();
                return;
            }
            if (selectedModule != null) {
                selectedModule = null;
                appearancePage = false;
                clearStringEditor();
                if (searchField != null) searchField.setFocused(false);
                return;
            }
            mc.displayGuiScreen(null);
            return;
        }
        if (bindingItemSetting != null) {
            bindingItemSetting.setKeybind(keyCode);
            bindingItemSetting = null;
            YesClient.getInstance().saveAll();
            return;
        }
        if (bindingModule != null) {
            bindingModule.setKeybind(keyCode);
            bindingModule = null;
            return;
        }
        if (editingString != null && stringField != null && stringField.isFocused()) {
            stringField.textboxKeyTyped(typedChar, keyCode);
            editingString.setValue(stringField.getText());
            return;
        }
        if (searchField != null && searchField.isFocused()) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                searchField.setText("");
                searchField.setFocused(false);
                targetModuleScroll = moduleScroll = 0;
                return;
            }
            searchField.textboxKeyTyped(typedChar, keyCode);
            targetModuleScroll = moduleScroll = 0;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingNumber != null && clickedMouseButton == 0) {
            updateNumberSetting(draggingNumber, mouseX, draggingControlX, draggingControlW);
        }
        if (draggingColor != null && clickedMouseButton == 0) {
            updateColorDrag(mouseX, mouseY);
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            draggingNumber = null;
            draggingColor = null;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        float scrollDelta = Math.min(120, Math.abs(wheel)) * (42.0F / 120.0F);
        if (selectedModule != null) targetSettingsScroll -= Math.signum(wheel) * scrollDelta;
        else targetModuleScroll -= Math.signum(wheel) * scrollDelta;
    }

    private void openSettings(Module module, boolean appearance) {
        selectedModule = module;
        bindingModule = null;
        bindingItemSetting = null;
        draggingNumber = null;
        appearancePage = appearance;
        targetSettingsScroll = settingsScroll = 0;
        clearStringEditor();
        if (searchField != null) searchField.setFocused(false);
    }

    public void openHudElementSettings(Module module, GuiScreen previousScreen) {
        if (module == null) return;
        returnScreen = previousScreen;
        openSettings(module, false);
         
         
         
        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(this);
    }

    private void returnToPreviousScreen() {
        GuiScreen destination = returnScreen;
        returnScreen = null;
        selectedModule = null;
        appearancePage = false;
        if (destination != null) mc.displayGuiScreen(destination);
        else mc.displayGuiScreen(null);
    }

    private ClickGUIModule getClickGuiModule() {
        if (YesClient.getInstance() == null || YesClient.getInstance().getModuleManager() == null) return null;
        return YesClient.getInstance().getModuleManager().getModule(ClickGUIModule.class);
    }

    private void drawTexture(net.minecraft.util.ResourceLocation texture, int x, int y, int w, int h) {
        mc.getTextureManager().bindTexture(texture);
        GlStateManager.enableBlend();
        drawModalRectWithCustomSizedTexture(x, y, 0, 0, w, h, w, h);
    }

    private void drawCentered(String text, int centerX, int y, int color) {
        fontRendererObj.drawString(text, centerX - fontRendererObj.getStringWidth(text) / 2, y, color);
    }

    private static String trim(String text, int width) {
        if (text == null) return "";
        int maxChars = width < 30 ? width : Math.max(8, width / 5);
        return text.length() > maxChars ? text.substring(0, Math.max(1, maxChars - 3)) + "..." : text;
    }

    private static String format(double value) {
        if (Math.abs(value - Math.round(value)) < 0.01) return String.valueOf((int) Math.round(value));
        return String.format(Locale.US, "%.2f", value);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

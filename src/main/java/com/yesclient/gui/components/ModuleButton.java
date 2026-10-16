package com.yesclient.gui.components;
import java.util.List;

import com.yesclient.gui.theme.Theme;
import com.yesclient.module.Module;
import com.yesclient.module.settings.*;
import com.yesclient.utils.RenderUtils;
import com.yesclient.utils.MarqueeText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import com.yesclient.YesClient;
import com.yesclient.module.modules.client.ClickGUIModule;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;

 


public class ModuleButton {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final FontRenderer fontRenderer = mc.fontRendererObj;

    private final Module module;
    private final Panel parent;
    private final Theme theme;

    private int x, y, width;
    private static final int HEIGHT = 20;

    private boolean expanded = false;
    private boolean hovered = false;
    private boolean bindingKey = false;
    private boolean infoHovered = false;

    private static final ResourceLocation INFO_ICON = new ResourceLocation("yesclient",
            "textures/gui/moduleinfo.png");
    private static final int INFO_ICON_SIZE = 10;

     
    private float enableAnimation = 0;
    private float expandAnimation = 0;
    private float hoverAnimation = 0;

    private final List<SettingComponent> settingComponents = new ArrayList<SettingComponent>();

    public ModuleButton(Module module, Panel parent, Theme theme) {
        this.module = module;
        this.parent = parent;
        this.theme = theme;

        initializeSettings();
    }

    private void initializeSettings() {
        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) {
                settingComponents.add(new CheckboxComponent((BooleanSetting) setting, theme));
            } else if (setting instanceof NumberSetting) {
                settingComponents.add(new SliderComponent((NumberSetting) setting, theme));
            } else if (setting instanceof ModeSetting) {
                settingComponents.add(new ModeComponent((ModeSetting) setting, theme));
            }
        }
    }

    public void setPosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    private int getBindButtonWidth() {
        return module.getSettings().isEmpty() ? 48 : 38;
    }

    private int getBindButtonX() {
        return x + width - 4 - getBindButtonWidth();
    }

    private int getExpandIndicatorX() {
        return module.allowsModuleKeybind() ? getBindButtonX() - 8 : x + width - 14;
    }

    private boolean isExpandIndicatorHit(int mouseX, int mouseY) {
        int indicatorX = getExpandIndicatorX();
        return mouseX >= indicatorX - 5 && mouseX <= indicatorX + 5
                && mouseY >= y + 2 && mouseY < y + HEIGHT - 2;
    }

    public void draw(int mouseX, int mouseY) {
         
        float targetEnable = module.isEnabled() ? 1 : 0;
        enableAnimation += (targetEnable - enableAnimation) * 0.25f;

        float targetExpand = expanded ? 1 : 0;
        expandAnimation += (targetExpand - expandAnimation) * 0.2f;

        int bindButtonWidth = getBindButtonWidth();
        int bindButtonX = getBindButtonX();
        boolean bindHovered = module.allowsModuleKeybind()
                && mouseX >= bindButtonX - 2 && mouseX < bindButtonX + bindButtonWidth
                && mouseY >= y + 2 && mouseY < y + HEIGHT - 2;
        hovered = isMouseOver(mouseX, mouseY) && !bindHovered;
        float targetHover = hovered ? 1 : 0;
        hoverAnimation += (targetHover - hoverAnimation) * 0.3f;

         
        int bgColor;
        if (module.isEnabled()) {
             
            bgColor = theme.getEnabledBackgroundColor();
            if (hoverAnimation > 0.01f) {
                bgColor = Theme.brighter(bgColor, 0.1f * hoverAnimation);
            }
        } else {
             
            bgColor = theme.getModuleColor();
            if (hoverAnimation > 0.01f) {
                bgColor = Theme.blend(bgColor, theme.getModuleColorHover(), hoverAnimation);
            }
        }

         
        if (enableAnimation > 0.5f) {
            RenderUtils.drawGlow(x - 1, y - 1, width + 2, HEIGHT + 2,
                    (int) (4 * enableAnimation), theme.getAccentColor());
        }

         
        RenderUtils.drawRoundedRect(x, y, width, HEIGHT, 4, bgColor);

         
        com.yesclient.module.modules.client.ClickGUIModule clickGuiModule = YesClient.getInstance()
                .getModuleManager().getModule(com.yesclient.module.modules.client.ClickGUIModule.class);
        if (clickGuiModule == null) {
            Module m = YesClient.getInstance().getModuleManager().getModule("ClickGUI");
            if (m instanceof com.yesclient.module.modules.client.ClickGUIModule) {
                clickGuiModule = (com.yesclient.module.modules.client.ClickGUIModule) m;
            }
        }
        boolean tooltipsEnabled = clickGuiModule != null && clickGuiModule.isTooltipsEnabled();

        if (tooltipsEnabled) {
             
            int infoX = x + 4;
            int infoY = y + (HEIGHT - INFO_ICON_SIZE) / 2;
            infoHovered = mouseX >= infoX && mouseX <= infoX + INFO_ICON_SIZE && mouseY >= infoY
                    && mouseY <= infoY + INFO_ICON_SIZE;

            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE,
                    GL11.GL_ZERO);
            GlStateManager.color(1f, 1f, 1f, infoHovered ? 1.0f : 0.5f);
            mc.getTextureManager().bindTexture(INFO_ICON);
             
             
            Gui.drawScaledCustomSizeModalRect(infoX, infoY, 0, 0, 512, 512, INFO_ICON_SIZE, INFO_ICON_SIZE, 512, 512);
            GlStateManager.color(1f, 1f, 1f, 1f);  
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        } else {
            infoHovered = false;
        }

         
        String name = module.getDisplayName();
        int textColor = module.isEnabled() ? 0xFF1A1A22 : 0xFFCCCCCC;  
        int nameX = x + 4 + INFO_ICON_SIZE + 4;  
        if (module.isEnabled()) {
             
            fontRenderer.drawString(name, nameX, y + 6, textColor);
        } else {
            fontRenderer.drawStringWithShadow(name, nameX, y + 6, textColor);
        }

         
        String keybindText = bindingKey ? "..." : module.getKeybindDisplay().replace("[", "").replace("]", "").toUpperCase();
        if (module.allowsModuleKeybind()) {
            int bindX = bindButtonX;
            int bindY = y + 3;
            RenderUtils.drawRoundedRect(bindX, bindY, bindButtonWidth, 15, 4,
                    bindingKey ? Theme.ACCENT : (module.isEnabled() ? 0x6682829A : 0x6633333D));
            GlStateManager.pushMatrix();
            GlStateManager.translate(bindX + bindButtonWidth / 2, bindY + 7, 0);
            GlStateManager.scale(0.78f, 0.78f, 1.0f);
            keybindText = MarqueeText.fit(keybindText, fontRenderer,
                    Math.max(1, (int) ((bindButtonWidth - 6) / 0.78f)), module);
            int keybindWidth = fontRenderer.getStringWidth(keybindText);
            int keybindColor = bindingKey ? 0xFFFFFFFF : (module.isEnabled() ? 0xFF2A2A35 : 0xFFAAAAAA);
            fontRenderer.drawString(keybindText, -keybindWidth / 2, -4, keybindColor);
            GlStateManager.popMatrix();
        }

         
        if (!module.getSettings().isEmpty()) {
            int dotColor = module.isEnabled() ? 0xFF2A2A35 : 0xFF888888;
            int dotX = getExpandIndicatorX();
            int dotY = y + HEIGHT / 2;
            int dotSize = 2;
            int dotSpacing = 4;

             
            RenderUtils.drawCircle(dotX, dotY - dotSpacing, dotSize, dotColor);
            RenderUtils.drawCircle(dotX, dotY, dotSize, dotColor);
            RenderUtils.drawCircle(dotX, dotY + dotSpacing, dotSize, dotColor);
        }

         
        if (expanded && expandAnimation > 0.1f) {
            drawSettings(mouseX, mouseY);
        }

         
        if (infoHovered && module.getDescription() != null && !module.getDescription().isEmpty()) {
            ClickGUIModule clickGui = (ClickGUIModule) com.yesclient.YesClient.getInstance().getModuleManager()
                    .getModule(ClickGUIModule.class);
            if (clickGui != null && clickGui.isTooltipsEnabled()) {
                drawInfoTooltip(mouseX, mouseY);
            }
        }
    }

    private void drawInfoTooltip(int mouseX, int mouseY) {
        String desc = module.getDescription();
        int tooltipWidth = fontRenderer.getStringWidth(desc) + 8;
        int tooltipHeight = 12;
        int tooltipX = mouseX + 8;
        int tooltipY = mouseY - 4;

         
        int screenWidth = mc.currentScreen != null ? mc.currentScreen.width : 400;
        if (tooltipX + tooltipWidth > screenWidth - 5) {
            tooltipX = mouseX - tooltipWidth - 8;
        }

         
        RenderUtils.drawRoundedRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 3, 0xEE1A1A20);
        RenderUtils.drawRoundedRectOutline(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 3, theme.getAccentColor(),
                1.0f);

         
        fontRenderer.drawStringWithShadow(desc, tooltipX + 4, tooltipY + 2, 0xFFCCCCCC);
    }

    private void drawSettings(int mouseX, int mouseY) {
        int settingY = y + HEIGHT + 2;
        int settingX = x + 4;
        int settingWidth = width - 8;

         
        int totalHeight = 0;
        for (SettingComponent component : settingComponents) {
            if (component.getSetting().isVisible()) {
                totalHeight += component.getHeight() + 2;
            }
        }

        if (totalHeight > 0) {
            RenderUtils.drawRoundedRect(x, y + HEIGHT, width, totalHeight + 4, 4, theme.getSettingsBackground());
        }

        for (SettingComponent component : settingComponents) {
            if (!component.getSetting().isVisible())
                continue;

            component.setPosition(settingX, settingY, settingWidth);
            component.draw(mouseX, mouseY);
            settingY += component.getHeight() + 2;
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
         
        if (bindingKey && button >= 3 && module.allowsModuleKeybind()) {
            module.setKeybind(-(button + 100));
            bindingKey = false;
            return;
        }

        if (!isMouseOver(mouseX, mouseY) && !isMouseOverSettings(mouseX, mouseY)) {
            return;
        }

        if (isMouseOver(mouseX, mouseY)) {
             
            if (!module.getSettings().isEmpty() && isExpandIndicatorHit(mouseX, mouseY)) {
                expanded = !expanded;
                return;
            }

             
            if (module.allowsModuleKeybind()) {
                int bindButtonWidth = getBindButtonWidth();
                int bindButtonX = getBindButtonX();
                if (mouseX >= bindButtonX - 2 && mouseX < bindButtonX + bindButtonWidth) {
                    bindingKey = !bindingKey;
                    return;
                }
            }

            if (button == 0) {
                 
                module.toggle();
            } else if (button == 1) {
                 
                if (!module.getSettings().isEmpty()) {
                    expanded = !expanded;
                }
            }
            return;
        }

         
        if (expanded) {
            for (SettingComponent component : settingComponents) {
                component.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int button) {
        if (expanded) {
            for (SettingComponent component : settingComponents) {
                component.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public boolean keyTyped(int keyCode) {
        if (bindingKey) {
            if (!module.allowsModuleKeybind()) {
                bindingKey = false;
                return true;
            }
            if (keyCode == Keyboard.KEY_ESCAPE) {
                module.setKeybind(0);
            } else if (keyCode != Keyboard.KEY_RETURN) {
                module.setKeybind(keyCode);
            }
            bindingKey = false;
            return true;
        }

        if (expanded) {
            for (SettingComponent component : settingComponents) {
                if (component instanceof SliderComponent) {
                    if (((SliderComponent) component).keyTyped(keyCode)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + HEIGHT;
    }

    private boolean isMouseOverSettings(int mouseX, int mouseY) {
        if (!expanded)
            return false;

        int totalHeight = 0;
        for (SettingComponent component : settingComponents) {
            if (component.getSetting().isVisible()) {
                totalHeight += component.getHeight() + 2;
            }
        }

        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y + HEIGHT && mouseY <= y + HEIGHT + totalHeight + 4;
    }

    public int getTotalHeight() {
        if (!expanded)
            return HEIGHT;

        int settingsHeight = 4;  
        for (SettingComponent component : settingComponents) {
            if (component.getSetting().isVisible()) {
                settingsHeight += component.getHeight() + 2;
            }
        }

        return HEIGHT + settingsHeight;
    }

    public Module getModule() {
        return module;
    }

    public boolean isBindingKey() {
        return bindingKey;
    }
}

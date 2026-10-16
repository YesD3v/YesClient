package com.yesclient.gui.components;

import com.yesclient.gui.theme.Theme;
import com.yesclient.module.settings.ModeSetting;
import com.yesclient.module.settings.Setting;
import com.yesclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

 


public class ModeComponent extends SettingComponent {

    private final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
    private final ModeSetting setting;

    private static final int HEIGHT = 18;

    private float hoverAnimation = 0;

    public ModeComponent(ModeSetting setting, Theme theme) {
        super(theme);
        this.setting = setting;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
         
        boolean hovered = isMouseOver(mouseX, mouseY);
        float targetHover = hovered ? 1 : 0;
        hoverAnimation += (targetHover - hoverAnimation) * 0.3f;

         
        fontRenderer.drawStringWithShadow(setting.getName(), x, y + 4, theme.getTextSecondary());

         
        String mode = setting.getValue();
        int modeWidth = fontRenderer.getStringWidth(mode) + 20;  
        int modeX = x + width - modeWidth;
        int modeY = y + 1;
        int modeHeight = HEIGHT - 2;

         
        int bgColor = Theme.blend(theme.getModuleColor(), theme.getHoverColor(), hoverAnimation * 0.5f);
        RenderUtils.drawRoundedRect(modeX, modeY, modeWidth, modeHeight, 3, bgColor);

         
        String leftArrow = "\u25C0";  
        int arrowColor = Theme.withAlpha(theme.getTextSecondary(), (int) (150 + 105 * hoverAnimation));
        fontRenderer.drawStringWithShadow(leftArrow, modeX + 3, y + 4, arrowColor);

         
        int textX = modeX + (modeWidth - fontRenderer.getStringWidth(mode)) / 2;
        int textColor = hovered ? theme.getTextColor() : theme.getTextSecondary();
        fontRenderer.drawStringWithShadow(mode, textX, y + 4, textColor);

         
        String rightArrow = "\u25B6";  
        fontRenderer.drawStringWithShadow(rightArrow, modeX + modeWidth - 10, y + 4, arrowColor);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY))
            return;

         
        String mode = setting.getValue();
        int modeWidth = fontRenderer.getStringWidth(mode) + 20;
        int modeX = x + width - modeWidth;

        if (mouseX >= modeX) {
             
            if (mouseX < modeX + modeWidth / 2) {
                 
                setting.cycleReverse();
            } else {
                 
                setting.cycle();
            }
        } else if (button == 0) {
            setting.cycle();
        } else if (button == 1) {
            setting.cycleReverse();
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
    }

    private boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + HEIGHT;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public Setting<?> getSetting() {
        return setting;
    }
}

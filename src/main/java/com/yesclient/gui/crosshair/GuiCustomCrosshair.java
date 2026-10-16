package com.yesclient.gui.crosshair;

import com.yesclient.YesClient;
import com.yesclient.module.modules.qol.CrosshairModule;
import com.yesclient.utils.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

 





public class GuiCustomCrosshair extends GuiScreen {

    private static final int R = CrosshairModule.GRID_RADIUS;
    private static final int CELLS = R * 2 + 1;
    private static final int CELL = 12;

    private static final int PANEL = 0xF015171A;
    private static final int CARD = 0xFF22252A;
    private static final int CONTROL = 0xFF30343A;
    private static final int CONTROL_HOVER = 0xFF3A3F46;
    private static final int TEXT = 0xFFE7E9ED;
    private static final int TEXT_DIM = 0xFF8A8F98;

    private final GuiScreen parent;
    private final CrosshairModule module;
    private final Set<Long> pixels;

     
    private int paintMode;
    private long lastCell = Long.MIN_VALUE;

    public GuiCustomCrosshair(GuiScreen parent, CrosshairModule module) {
        this.parent = parent;
        this.module = module;
        this.pixels = new LinkedHashSet<>(CrosshairModule.parseCustom(module.customData.getValue()));
    }

    private int gridSize() {
        return CELLS * CELL;
    }

    private int gridX() {
        return width / 2 - gridSize() / 2 - 50;
    }

    private int gridY() {
        return height / 2 - gridSize() / 2 - 6;
    }

    private int panelX() {
        return gridX() - 14;
    }

    private int panelY() {
        return gridY() - 36;
    }

    private int panelW() {
        return gridSize() + 28 + 100;
    }

    private int panelH() {
        return gridSize() + 36 + 44;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0x99000000);
        int px = panelX(), py = panelY(), pw = panelW(), ph = panelH();
        RenderUtils.drawRoundedRect(px, py, pw, ph, 10, PANEL);

        fontRendererObj.drawString("Custom Crosshair", px + 14, py + 10, TEXT);
        fontRendererObj.drawString("Click to toggle a pixel, drag to paint", px + 14, py + 22, TEXT_DIM);

        int gx = gridX(), gy = gridY();
        RenderUtils.drawRect(gx - 1, gy - 1, gridSize() + 2, gridSize() + 2, 0xFF0C0D0F);
        for (int cx = -R; cx <= R; cx++) {
            for (int cy = -R; cy <= R; cy++) {
                int x = gx + (cx + R) * CELL;
                int y = gy + (cy + R) * CELL;
                boolean filled = pixels.contains(CrosshairModule.key(cx, cy));
                boolean center = cx == 0 && cy == 0;
                boolean axis = cx == 0 || cy == 0;
                int c;
                if (filled) c = center ? 0xFFFFFFFF : 0xFFE6E6E6;
                else if (center) c = 0xFF6A707A;           
                else if (axis) c = 0xFF2A2D33;             
                else c = 0xFF1E2024;
                RenderUtils.drawRect(x, y, CELL - 1, CELL - 1, c);
            }
        }
         
        long hover = cellAt(mouseX, mouseY);
        if (hover != Long.MIN_VALUE) {
            int x = gx + (CrosshairModule.keyX(hover) + R) * CELL;
            int y = gy + (CrosshairModule.keyY(hover) + R) * CELL;
            RenderUtils.drawRect(x, y, CELL - 1, 1, 0xFF9AA0AA);
            RenderUtils.drawRect(x, y + CELL - 2, CELL - 1, 1, 0xFF9AA0AA);
            RenderUtils.drawRect(x, y, 1, CELL - 1, 0xFF9AA0AA);
            RenderUtils.drawRect(x + CELL - 2, y, 1, CELL - 1, 0xFF9AA0AA);
        }

         
        int prevX = gx + gridSize() + 14;
        int prevY = gy;
        int prevW = 86;
        fontRendererObj.drawString("Preview", prevX, prevY, TEXT_DIM);
        RenderUtils.drawRoundedRect(prevX, prevY + 12, prevW, 60, 6, 0xFF5B7FA6);
        RenderUtils.drawRoundedRect(prevX, prevY + 78, prevW, 60, 6, 0xFF3C3F44);
        drawPreview(prevX + prevW / 2, prevY + 12 + 30, 0xFFFFFFFF);
        drawPreview(prevX + prevW / 2, prevY + 78 + 30, 0xFFFFFFFF);
        fontRendererObj.drawString(pixels.size() + " pixels", prevX, prevY + 146, TEXT_DIM);

         
        int by = gy + gridSize() + 12;
        drawButton("Done", gx, by, 70, mouseX, mouseY, 0xFF3D6B4A, 0xFF4A8259);
        drawButton("Cancel", gx + 76, by, 50, mouseX, mouseY, CONTROL, CONTROL_HOVER);
        drawButton("Clear", gx + gridSize() - 50, by, 50, mouseX, mouseY, CONTROL, CONTROL_HOVER);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPreview(int centerX, int centerY, int color) {
        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f / sf, 1.0f / sf, 1f);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.color((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1f);
        CrosshairModule.drawPixels(pixels, centerX * sf, centerY * sf, module.getScale());
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    private void drawButton(String label, int x, int y, int w, int mouseX, int mouseY, int bg, int bgHover) {
        boolean hover = inside(mouseX, mouseY, x, y, w, 20);
        RenderUtils.drawRoundedRect(x, y, w, 20, 6, hover ? bgHover : bg);
        fontRendererObj.drawString(label, x + w / 2 - fontRendererObj.getStringWidth(label) / 2, y + 6, TEXT);
    }

    private long cellAt(int mouseX, int mouseY) {
        int gx = gridX(), gy = gridY();
        if (mouseX < gx || mouseY < gy || mouseX >= gx + gridSize() || mouseY >= gy + gridSize()) {
            return Long.MIN_VALUE;
        }
        int cx = (mouseX - gx) / CELL - R;
        int cy = (mouseY - gy) / CELL - R;
        return CrosshairModule.key(cx, cy);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int gx = gridX(), by = gridY() + gridSize() + 12;
        if (mouseButton == 0) {
            if (inside(mouseX, mouseY, gx, by, 70, 20)) {
                finish(true);
                return;
            }
            if (inside(mouseX, mouseY, gx + 76, by, 70, 20)) {
                finish(false);
                return;
            }
            if (inside(mouseX, mouseY, gx + gridSize() - 50, by, 50, 20)) {
                pixels.clear();
                return;
            }
        }

        long cell = cellAt(mouseX, mouseY);
        if (cell == Long.MIN_VALUE) return;
        if (mouseButton == 1) {
            paintMode = 2;
        } else if (mouseButton == 0) {
            paintMode = pixels.contains(cell) ? 2 : 1;  
        } else return;
        apply(cell);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (paintMode == 0) return;
        long cell = cellAt(mouseX, mouseY);
        if (cell != Long.MIN_VALUE && cell != lastCell) apply(cell);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        paintMode = 0;
        lastCell = Long.MIN_VALUE;
        super.mouseReleased(mouseX, mouseY, state);
    }

    private void apply(long cell) {
        lastCell = cell;
        if (paintMode == 1) pixels.add(cell);
        else if (paintMode == 2) pixels.remove(cell);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) finish(false);
        else if (keyCode == Keyboard.KEY_RETURN) finish(true);
    }

    private void finish(boolean save) {
        if (save) {
            module.customData.setValue(CrosshairModule.serializeCustom(pixels));
            try {
                YesClient.getInstance().saveAll();
            } catch (Exception ignored) {
            }
        }
        mc.displayGuiScreen(parent);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

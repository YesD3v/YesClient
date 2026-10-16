package com.yesclient.gui;
import java.io.IOException;
import java.util.List;
import java.awt.Color;

import com.yesclient.YesClient;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleManager;
import com.yesclient.module.HudScale;
import com.yesclient.module.modules.client.ClickGUIModule;
import com.yesclient.module.modules.client.HudDesignerModule;
import com.yesclient.module.modules.client.HUDModule;

import com.yesclient.module.modules.render.DirectionModule;
import com.yesclient.module.modules.visuals.*;
import com.yesclient.module.modules.exploit.BlinkModule;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;

 




public class HudEditorGUI extends GuiScreen {

    private static final int RESIZE_HANDLE_SIZE = 12;
    private static final int RESIZE_HANDLE_GAP = 2;

    private final HudDesignerModule hudDesigner;
    private final ModuleManager moduleManager;

    private List<HudEditorElement> elements = new ArrayList<>();
    private HudEditorElement draggedElement = null;
    private HudEditorElement resizingElement = null;
    private int dragOffsetX, dragOffsetY;
    private int resizeMouseStartX, resizeMouseStartY, resizeStartWidth, resizeStartHeight;
    private int resizeStartX, resizeStartY;
    private int resizeDirectionX = 1, resizeDirectionY = 1;
    private float resizeStartScale = 1.0F;

    public HudEditorGUI(HudDesignerModule hudDesigner) {
        this.hudDesigner = hudDesigner;
        this.moduleManager = YesClient.getInstance().getModuleManager();
    }

    @Override
    public void initGui() {
        super.initGui();
        refreshElements();
    }

    private void refreshElements() {
        elements.clear();

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

         
        HUDModule hudModule = moduleManager.getModule(HUDModule.class);
        if (hudModule != null && hudModule.isEnabled()) {
             
             
            elements.add(new HudEditorElement("Module List", hudDesigner.getModuleListX(screenWidth),
                    hudDesigner.getModuleListY(), 100, 100, "moduleList", hudModule));
        }

        ClickGUIModule clickGui = moduleManager.getModule(ClickGUIModule.class);
        if (clickGui != null && clickGui.showWatermark()) {
            int wmWidth = Math.max(fontRendererObj.getStringWidth("Yes"), fontRendererObj.getStringWidth("Client"));
            int wmHeight = (fontRendererObj.FONT_HEIGHT + 1) * 2;
            float scale = clickGui.getWatermarkScale().getValue();
            elements.add(new HudEditorElement("Watermark", hudDesigner.getWatermarkX(), hudDesigner.getWatermarkY(),
                    Math.round(wmWidth * scale), Math.round(wmHeight * scale), "watermark", clickGui));
        }

         
        addModuleElementIfEnabled(CoordsModule.class, "Coords", hudDesigner.getCoordsX(),
                hudDesigner.getCoordsY(screenHeight), 100, 15, "coords");
        addModuleElementIfEnabled(FPSModule.class, "FPS", hudDesigner.getFpsX(), hudDesigner.getFpsY(), 40, 15, "fps");
        addModuleElementIfEnabled(CPSModule.class, "CPS", hudDesigner.getCpsX(), hudDesigner.getCpsY(), 60, 15, "cps");
        addModuleElementIfEnabled(PingModule.class, "Ping", hudDesigner.getPingX(), hudDesigner.getPingY(), 50, 15,
                "ping");
        addModuleElementIfEnabled(DirectionModule.class, "Direction", hudDesigner.getDirX(), hudDesigner.getDirY(), 160,
                18, "dir");
        addModuleElementIfEnabled(BiomeModule.class, "Biome", hudDesigner.getBiomeX(), hudDesigner.getBiomeY(), 70, 15,
                "biome");
        addModuleElementIfEnabled(ClockModule.class, "Time", hudDesigner.getTimeX(), hudDesigner.getTimeY(), 50, 15,
                "time");
        addModuleElementIfEnabled(TPSModule.class, "TPS", hudDesigner.getTpsX(), hudDesigner.getTpsY(), 50, 15, "tps");
        addModuleElementIfEnabled(LoadedPlayersModule.class, "Loaded Players", hudDesigner.getLoadedPlayersX(),
                hudDesigner.getLoadedPlayersY(), 100, 15, "loadedPlayers");
        addModuleElementIfEnabled(ActiveEffectsModule.class, "Active Effects", hudDesigner.getActiveEffectsX(),
                hudDesigner.getActiveEffectsY(), 100, 50, "activeEffects");
        addModuleElementIfEnabled(MediaOverlayModule.class, "Media Overlay", hudDesigner.getMediaOverlayX(),
                hudDesigner.getMediaOverlayY(screenHeight), 310, 82, "mediaOverlay");
        addModuleElementIfEnabled(HideEntitiesModule.class, "Entities Hidden", hudDesigner.getHiddenEntitiesX(),
                hudDesigner.getHiddenEntitiesY(), 100, 15, "hiddenEntities");
        addModuleElementIfEnabled(com.yesclient.module.modules.exploit.AutoBegModule.class, "Auto Beg",
                hudDesigner.getAutoBegX(),
                hudDesigner.getAutoBegY(), 100, 15, "autobeg");

         
         
        int sbX = hudDesigner.getScoreboardX();
        int sbY = hudDesigner.getScoreboardY();
        if (sbX < 0)
            sbX = screenWidth - 100;  
        if (sbY < 0)
            sbY = screenHeight / 2 - 50;  
        com.yesclient.module.modules.visuals.ScoreboardModule scoreboard =
                moduleManager.getModule(com.yesclient.module.modules.visuals.ScoreboardModule.class);
        int[] scoreboardBounds = scoreboard == null ? new int[] { 0, 0, 100, 100 } : scoreboard.getBounds();
        HudScale scoreboardHudScale = HudScale.of(scoreboard);
        float scoreboardScale = scoreboardHudScale == null ? 1.0F : scoreboardHudScale.getValue();
        elements.add(new HudEditorElement("Scoreboard", sbX, sbY, Math.round(scoreboardBounds[2] * scoreboardScale),
                Math.round(scoreboardBounds[3] * scoreboardScale), "scoreboard", scoreboard));

         
         
        BlinkModule blink = moduleManager.getModule(BlinkModule.class);
        if (blink != null && blink.isDisplayTimerEnabled()) {
            HudScale blinkHudScale = HudScale.of(blink);
            float blinkScale = blinkHudScale == null ? 1.0F : blinkHudScale.getValue();
            elements.add(new HudEditorElement("Blink Timer", hudDesigner.getBlinkTimerX(), hudDesigner.getBlinkTimerY(),
                    Math.round(Math.max(60, blink.getWidth()) * blinkScale),
                    Math.round(Math.max(30, blink.getHeight()) * blinkScale), "blinkTimer", blink));
        }
         
        for (HudEditorElement el : elements) {
            el.x = Math.max(0, Math.min(screenWidth, el.x));
            el.y = Math.max(0, Math.min(screenHeight, el.y));
        }
    }

    private void addModuleElementIfEnabled(Class<? extends Module> clazz, String name, int x, int y, int defaultW,
            int defaultH, String id) {
        Module m = moduleManager.getModule(clazz);
        if (m != null && m.isEnabled()) {
            int w = m.getWidth() > 0 ? m.getWidth() : defaultW;
            int h = m.getHeight() > 0 ? m.getHeight() : defaultH;
            if (m instanceof MediaOverlayModule) {
                MediaOverlayModule media = (MediaOverlayModule) m;
                media.setOverlaySize(media.getOverlayWidth(), media.getOverlayHeight());
                w = media.getOverlayWidth();
                h = media.getOverlayHeight();
            }
            HudScale scale = HudScale.of(m);
            if (scale != null) {
                w = Math.round(w * scale.getValue());
                h = Math.round(h * scale.getValue());
            }
            elements.add(new HudEditorElement(name, x, y, w, h, id, m));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         
         
        if (resizingElement != null) {
            if (org.lwjgl.input.Mouse.isButtonDown(0)) {
                int newWidth = resizeStartWidth + resizeDirectionX * (mouseX - resizeMouseStartX);
                int newHeight = resizeStartHeight + resizeDirectionY * (mouseY - resizeMouseStartY);
                HudScale scale = HudScale.of(resizingElement.module);
                if (scale != null) {
                    float widthFactor = newWidth / (float) Math.max(1, resizeStartWidth);
                    float heightFactor = newHeight / (float) Math.max(1, resizeStartHeight);
                    scale.setValue(resizeStartScale * Math.max(0.1F, (widthFactor + heightFactor) / 2.0F));
                    float ratio = scale.getValue() / resizeStartScale;
                    resizingElement.width = Math.max(20, Math.round(resizeStartWidth * ratio));
                    resizingElement.height = Math.max(12, Math.round(resizeStartHeight * ratio));
                    resizingElement.x = resizeDirectionX < 0
                            ? resizeStartX - (resizingElement.width - resizeStartWidth) : resizeStartX;
                    resizingElement.y = resizeDirectionY < 0
                            ? resizeStartY - (resizingElement.height - resizeStartHeight) : resizeStartY;
                    updateModulePosition(resizingElement);
                }
            } else {
                resizingElement = null;
            }
        }

        if (draggedElement != null) {
            if (org.lwjgl.input.Mouse.isButtonDown(0)) {
                draggedElement.x = mouseX - dragOffsetX;
                draggedElement.y = mouseY - dragOffsetY;

                 
                draggedElement.x = Math.max(0, Math.min(width, draggedElement.x));
                draggedElement.y = Math.max(0, Math.min(height, draggedElement.y));

                 
                updateModulePosition(draggedElement);
            } else {
                 
                draggedElement = null;
            }
        }

         
        drawRect(0, 0, width, height, new Color(0, 0, 0, 100).getRGB());

         
         
         
         

        if (elements.isEmpty()) {
            drawCenteredString(fontRendererObj, "\u00A7cNo HUD modules are enabled!", width / 2, height / 2, -1);
            drawCenteredString(fontRendererObj, "\u00A77Enable modules in the Click GUI first", width / 2,
                    height / 2 + 15, -1);
        }

         
        for (HudEditorElement element : elements) {
             
            if (element.module != null) {
                int newWidth = element.module.getWidth();
                int newHeight = element.module.getHeight();

                HudScale scale = HudScale.of(element.module);
                if (scale != null) {
                    newWidth = Math.round(newWidth * scale.getValue());
                    newHeight = Math.round(newHeight * scale.getValue());
                }

                if ("blinkTimer".equals(element.id)) {
                    newWidth = Math.round(Math.max(60, element.module.getWidth())
                            * (scale == null ? 1.0F : scale.getValue()));
                    newHeight = Math.round(Math.max(30, element.module.getHeight())
                            * (scale == null ? 1.0F : scale.getValue()));
                }

                 
                if (newWidth > 0)
                    element.width = newWidth;
                if (newHeight > 0)
                    element.height = newHeight;
            }

            int drawX = element.x;

             
             
             
             
             
            if ("moduleList".equals(element.id)) {
                ScaledResolution sr = new ScaledResolution(mc);
                if (element.x > sr.getScaledWidth() / 2) {
                    drawX = element.x - element.width;
                }
            }

            drawElement(element, drawX, element.y, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private int getRenderX(HudEditorElement element) {
         
         
        if ("moduleList".equals(element.id)) {
            ScaledResolution sr = new ScaledResolution(mc);
             
            if (element.x > sr.getScaledWidth() / 2) {
                return element.x - element.width;
            }
        }
        return element.x;
    }

    private void drawElement(HudEditorElement element, int x, int y, int mouseX, int mouseY) {
        boolean hovered = isMouseOver(x, y, element.width, element.height, mouseX, mouseY);
        boolean dragging = draggedElement == element || resizingElement == element;

         
        int borderColor = hovered || dragging ? new Color(255, 255, 255, 220).getRGB()
                : new Color(200, 200, 200, 100).getRGB();

         
        drawHorizontalLine(x, x + element.width - 1, y, borderColor);
        drawHorizontalLine(x, x + element.width - 1, y + element.height - 1, borderColor);
        drawVerticalLine(x, y, y + element.height - 1, borderColor);
        drawVerticalLine(x + element.width - 1, y, y + element.height - 1, borderColor);

         
        drawCenteredString(fontRendererObj, element.name,
                x + element.width / 2,
                y + (element.height - 8) / 2,
                -1);

        if (element.module != null && !"moduleList".equals(element.id)) {
            HudResizeHandle handle = getResizeHandle(element, x, y, element.width, element.height);
            drawFourWayHandle(handle.x, handle.y, hovered || dragging ? 0xFFE7E9ED : 0xFF9B9FA8);
        }

        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawFourWayHandle(int x, int y, int color) {
         
        drawRect(x + 5, y, x + 6, y + 3, color);
        drawRect(x + 4, y + 1, x + 7, y + 3, color);
        drawRect(x + 5, y + 3, x + 6, y + 6, color);

        drawRect(x + 5, y + 6, x + 6, y + 9, color);
        drawRect(x + 4, y + 8, x + 7, y + 10, color);
        drawRect(x + 5, y + 9, x + 6, y + 12, color);

        drawRect(x, y + 5, x + 3, y + 6, color);
        drawRect(x + 1, y + 4, x + 3, y + 7, color);
        drawRect(x + 3, y + 5, x + 6, y + 6, color);

        drawRect(x + 6, y + 5, x + 9, y + 6, color);
        drawRect(x + 8, y + 4, x + 11, y + 7, color);
        drawRect(x + 9, y + 5, x + 12, y + 6, color);
    }

    private HudResizeHandle getResizeHandle(HudEditorElement element, int x, int y, int elementWidth, int elementHeight) {
        int[][] outsideCandidates = new int[][] {
                { x + elementWidth + RESIZE_HANDLE_GAP, y + elementHeight + RESIZE_HANDLE_GAP, 1, 1 },
                { x + elementWidth + RESIZE_HANDLE_GAP, y - RESIZE_HANDLE_GAP - RESIZE_HANDLE_SIZE, 1, -1 },
                { x - RESIZE_HANDLE_GAP - RESIZE_HANDLE_SIZE, y + elementHeight + RESIZE_HANDLE_GAP, -1, 1 },
                { x - RESIZE_HANDLE_GAP - RESIZE_HANDLE_SIZE, y - RESIZE_HANDLE_GAP - RESIZE_HANDLE_SIZE, -1, -1 }
        };

        for (int[] candidate : outsideCandidates) {
            if (isHandleOnScreen(candidate[0], candidate[1])
                    && !isHandleBlocked(element, candidate[0], candidate[1])) {
                return new HudResizeHandle(candidate[0], candidate[1], candidate[2], candidate[3]);
            }
        }

         
        int[][] insideCandidates = new int[][] {
                { x + elementWidth - RESIZE_HANDLE_SIZE, y + elementHeight - RESIZE_HANDLE_SIZE, 1, 1 },
                { x + elementWidth - RESIZE_HANDLE_SIZE, y, 1, -1 },
                { x, y + elementHeight - RESIZE_HANDLE_SIZE, -1, 1 },
                { x, y, -1, -1 }
        };
        for (int[] candidate : insideCandidates) {
            if (isHandleOnScreen(candidate[0], candidate[1])
                    && !isHandleBlocked(element, candidate[0], candidate[1])) {
                return new HudResizeHandle(candidate[0], candidate[1], candidate[2], candidate[3]);
            }
        }

         
        int[] fallback = insideCandidates[0];
        int handleX = Math.max(0, Math.min(width - RESIZE_HANDLE_SIZE, fallback[0]));
        int handleY = Math.max(0, Math.min(height - RESIZE_HANDLE_SIZE, fallback[1]));
        return new HudResizeHandle(handleX, handleY, fallback[2], fallback[3]);
    }

    private boolean isHandleOnScreen(int x, int y) {
        return x >= 0 && y >= 0 && x + RESIZE_HANDLE_SIZE <= width && y + RESIZE_HANDLE_SIZE <= height;
    }

    private boolean isHandleBlocked(HudEditorElement owner, int handleX, int handleY) {
        for (HudEditorElement other : elements) {
            if (other == owner) continue;
            int otherX = getRenderX(other);
            if (handleX < otherX + other.width && handleX + RESIZE_HANDLE_SIZE > otherX
                    && handleY < other.y + other.height && handleY + RESIZE_HANDLE_SIZE > other.y) {
                return true;
            }
        }
        return false;
    }

    private boolean isMouseOver(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private boolean isMouseOver(HudEditorElement element, int mouseX, int mouseY) {
        int renderX = getRenderX(element);
        return isMouseOver(renderX, element.y, element.width, element.height, mouseX, mouseY);
    }

     
    private void drawElement(HudEditorElement element, int mouseX, int mouseY) {
        drawElement(element, element.x, element.y, mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 1) {
            for (int i = elements.size() - 1; i >= 0; i--) {
                HudEditorElement element = elements.get(i);
                if (isMouseOver(element, mouseX, mouseY)) {
                    if (element.module != null) {
                        new ClickGUI().openHudElementSettings(element.module, this);
                    }
                    return;
                }
            }
        }

        if (mouseButton == 0) {
             
             
            for (int i = elements.size() - 1; i >= 0; i--) {
                HudEditorElement el = elements.get(i);
                if (el.module != null && !"moduleList".equals(el.id)) {
                    int renderX = getRenderX(el);
                    HudResizeHandle handle = getResizeHandle(el, renderX, el.y, el.width, el.height);
                    if (isMouseOver(handle.x, handle.y, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE, mouseX, mouseY)) {
                        resizingElement = el;
                        resizeMouseStartX = mouseX;
                        resizeMouseStartY = mouseY;
                        resizeStartWidth = el.width;
                        resizeStartHeight = el.height;
                        resizeStartX = el.x;
                        resizeStartY = el.y;
                        resizeDirectionX = handle.directionX;
                        resizeDirectionY = handle.directionY;
                        HudScale scale = HudScale.of(el.module);
                        resizeStartScale = scale == null ? 1.0F : scale.getValue();
                        return;
                    }
                }
            }

            for (int i = elements.size() - 1; i >= 0; i--) {
                HudEditorElement el = elements.get(i);
                if (isMouseOver(el, mouseX, mouseY)) {
                    draggedElement = el;
                    dragOffsetX = mouseX - el.x;
                    dragOffsetY = mouseY - el.y;
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggedElement = null;
        resizingElement = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
         
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

     
    private void updateModulePosition(HudEditorElement element) {
        switch (element.id) {
            case "watermark":
                hudDesigner.setWatermarkPosition(element.x, element.y);
                break;
            case "moduleList":
                hudDesigner.setModuleListPosition(element.x, element.y);
                break;
            case "coords":
                hudDesigner.setCoordsPosition(element.x, element.y);
                break;
            case "fps":
                hudDesigner.setFpsPosition(element.x, element.y);
                break;
            case "cps":
                hudDesigner.setCpsPosition(element.x, element.y);
                break;
            case "ping":
                hudDesigner.setPingPosition(element.x, element.y);
                break;
            case "dir":
                hudDesigner.setDirPosition(element.x, element.y);
                break;
            case "biome":
                hudDesigner.setBiomePosition(element.x, element.y);
                break;
            case "time":
                hudDesigner.setTimePosition(element.x, element.y);
                break;
            case "tps":
                hudDesigner.setTpsPosition(element.x, element.y);
                break;
            case "loadedPlayers":
                hudDesigner.setLoadedPlayersPosition(element.x, element.y);
                break;
            case "scoreboard":
                hudDesigner.setScoreboardPosition(element.x, element.y);
                break;
            case "blinkTimer":
                hudDesigner.setBlinkTimerPosition(element.x, element.y);
                break;
            case "activeEffects":
                hudDesigner.setActiveEffectsPosition(element.x, element.y);
                break;
            case "hiddenEntities":
                hudDesigner.setHiddenEntitiesPosition(element.x, element.y);
                break;
            case "autobeg":
                hudDesigner.setAutoBegPosition(element.x, element.y);
                break;
            case "mediaOverlay":
                hudDesigner.setMediaOverlayPosition(element.x, element.y);
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {  
            savePositions();
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        savePositions();
        super.onGuiClosed();
    }

    private void savePositions() {
         
        YesClient.getInstance().getModuleManager().saveModuleStates();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

}

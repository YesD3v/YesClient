package com.yesclient.gui;
import java.util.List;
import java.awt.Color;

import com.yesclient.YesClient;
import com.yesclient.gui.theme.Theme;
import com.yesclient.module.Module;
import com.yesclient.module.modules.client.HideModulesModule;
import com.yesclient.module.modules.client.HUDModule;
import com.yesclient.module.modules.client.HudDesignerModule;
import com.yesclient.module.settings.ModeSetting;
import com.yesclient.module.settings.Setting;
import com.yesclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

 


public class HUD {

    private final Minecraft mc = Minecraft.getMinecraft();
    private FontRenderer fontRenderer;
    private final List<Long> leftClicks = new ArrayList<Long>();
    private final List<Long> rightClicks = new ArrayList<Long>();
    private HUDModule hudModule;
    private HudDesignerModule hudDesigner;

    public HUD() {
    }

    private HUDModule getHudModule() {
        if (hudModule == null && YesClient.getInstance() != null
                && YesClient.getInstance().getModuleManager() != null) {
            hudModule = YesClient.getInstance().getModuleManager().getModule(HUDModule.class);
        }
        return hudModule;
    }

    private HudDesignerModule getHudDesigner() {
        if (hudDesigner == null && YesClient.getInstance() != null
                && YesClient.getInstance().getModuleManager() != null) {
            hudDesigner = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        }
        return hudDesigner;
    }

    @SubscribeEvent
    public void onRenderHotbar(RenderGameOverlayEvent.Pre event) {
         
    }

    private void renderCustomHotbar(int screenWidth, int screenHeight, String style) {
        int slotSize = 20;
        int slots = 9;
        int hotbarWidth = slots * slotSize + 4;
        int hotbarHeight = slotSize + 4;
        int x = (screenWidth - hotbarWidth) / 2;
        int y = screenHeight - hotbarHeight - 2;

         
        switch (style) {
            case "Dark":
                RenderUtils.drawRoundedRect(x, y, hotbarWidth, hotbarHeight, 6, 0xD0101015);
                RenderUtils.drawRoundedRectOutline(x, y, hotbarWidth, hotbarHeight, 6, 0x40ffffff, 1);
                break;
            case "Gradient":
                RenderUtils.drawRoundedRect(x, y, hotbarWidth, hotbarHeight, 6, 0xD0101015);
                 
                RenderUtils.drawGradientRect(x + 6, y + hotbarHeight - 3, hotbarWidth - 12, 2,
                        Theme.ACCENT, Theme.ACCENT_BLUE, true);
                break;
            case "Minimal":
                RenderUtils.drawRoundedRect(x, y, hotbarWidth, hotbarHeight, 4, 0x80000000);
                break;
        }

         
        for (int i = 0; i < slots; i++) {
            int slotX = x + 2 + i * slotSize;
            int slotY = y + 2;

            boolean selected = (i == mc.thePlayer.inventory.currentItem);

            if (selected) {
                 
                RenderUtils.drawRoundedRect(slotX, slotY, slotSize, slotSize, 4, 0x40ffffff);
                if (!style.equals("Minimal")) {
                    RenderUtils.drawRoundedRectOutline(slotX, slotY, slotSize, slotSize, 4, Theme.ACCENT, 1.5f);
                }
            } else {
                 
                RenderUtils.drawRoundedRect(slotX, slotY, slotSize, slotSize, 3, 0x20ffffff);
            }

             
            net.minecraft.item.ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null) {
                net.minecraft.client.renderer.GlStateManager.enableRescaleNormal();
                net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(stack, slotX + 2, slotY + 2);
                mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, slotX + 2, slotY + 2);
                net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
                net.minecraft.client.renderer.GlStateManager.disableRescaleNormal();
            }
        }

         
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL)
            return;
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (mc.currentScreen instanceof ClickGUI) {
             
        }
        if (fontRenderer == null)
            fontRenderer = mc.fontRendererObj;

        ScaledResolution sr = new ScaledResolution(mc);
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();

         
        ToggleNotification.render();

        HUDModule hud = getHudModule();

         
        com.yesclient.module.modules.client.ClickGUIModule clickGui = com.yesclient.YesClient
                .getInstance().getModuleManager()
                .getModule(com.yesclient.module.modules.client.ClickGUIModule.class);
        if (clickGui != null && clickGui.showWatermark()) {
            HudDesignerModule watermarkDesigner = getHudDesigner();
            int watermarkX = watermarkDesigner != null ? watermarkDesigner.getWatermarkX() : 5;
            int watermarkY = watermarkDesigner != null ? watermarkDesigner.getWatermarkY() : 5;
            drawWatermark(watermarkX, watermarkY, clickGui);
        }

        if (hud == null || !hud.isEnabled())
            return;

        HudDesignerModule designer = getHudDesigner();

        Color primary = hud.getPrimaryColor();
        Color secondary = hud.getSecondaryColor();
        int mlX = designer != null ? designer.getModuleListX(width) : width - 100;
        int mlY = designer != null ? designer.getModuleListY() : 2;

         
        if (mlX < 0) mlX = 0;
        if (mlX > width) mlX = width;
        if (mlY < 0) mlY = 0;
        if (mlY > height) mlY = height;

        drawModuleList(width, height, mlX, mlY, primary, secondary, hud.showModuleListBackground(),
                hud.getBackgroundPadding());

         
         
         
         

         
         
         
        com.yesclient.module.modules.exploit.BlinkModule blink = com.yesclient.YesClient.getInstance()
                .getModuleManager().getModule(com.yesclient.module.modules.exploit.BlinkModule.class);
        if (blink != null && blink.isDisplayTimerEnabled()) {
            blink.onRender();
        }

         
         
    }

    private void drawModuleList(int screenWidth, int screenHeight, int xPos, int yPos, Color primary, Color secondary,
            boolean showBg, double bgPadding) {
        List<Module> enabledModules = YesClient.getInstance().getModuleManager().getVisibleModules();
        if (enabledModules.isEmpty())
            return;

         
        List<Module> filtered = new ArrayList<>();
        for (Module m : enabledModules) {
            if (HideModulesModule.shouldHide(m)) {
                continue;
            }

            String name = m.getName().toLowerCase();
            if (name.equals("module list") || name.equals("clickgui") || name.equals("hud designer")) {
                continue;
            }
            filtered.add(m);
        }

         
        boolean safeMode = YesClient.getInstance().isSafeMode();
        if (safeMode) {
            filtered.removeIf(Module::isBlatant);
        }
        if (filtered.isEmpty()) {
             
             
             
            return;
        }

         
        filtered.sort((a, b) -> {
            String aText = getModuleDisplayText(a);
            String bText = getModuleDisplayText(b);
            return fontRenderer.getStringWidth(bText) - fontRenderer.getStringWidth(aText);
        });

         
        int maxModuleWidth = 0;
        int totalModuleHeight = 0;

         
        GlStateManager.pushMatrix();
        float scale = 0.85f;
        GlStateManager.scale(scale, scale, 1.0f);

         
         
        int scaledX = (int) (xPos / scale);
        int scaledY = (int) (yPos / scale);
        int scaledScreenWidth = (int) (screenWidth / scale);

        int y = scaledY;
        int colorOffset = 0;

        for (Module module : filtered) {
            String name = module.getDisplayName().toLowerCase();
            String modeInfo = getModeModeInfo(module);

            String fullText = modeInfo.isEmpty() ? name : name + " " + modeInfo;
            int textWidth = fontRenderer.getStringWidth(fullText);

            int x = scaledX;

             
            boolean rightAlign = scaledX > scaledScreenWidth / 2;
            if (rightAlign) {
                x = scaledX - textWidth;  
            }

             
             
             
            float gradientProgress = filtered.size() <= 1 ? 0.0f
                    : colorOffset / (float) (filtered.size() - 1);
            int nameColor = blendColor(primary.getRGB(), secondary.getRGB(), gradientProgress);

             
             
             
             
            int pad = (int) bgPadding;
            int vPad = Math.max(0, pad - 2);  

             
            int itemWidth = textWidth + (pad * 2);
            int itemHeight = 8 + (vPad * 2);

            if (itemWidth > maxModuleWidth)
                maxModuleWidth = itemWidth;
            totalModuleHeight += itemHeight;

             
            if (showBg) {
                 
                RenderUtils.drawRect(x - pad, y - vPad, textWidth + (pad * 2), 8 + (vPad * 2), 0x50000000);
            }

             
            fontRenderer.drawStringWithShadow(name, x, y, nameColor);

             
            if (!modeInfo.isEmpty()) {
                int nameDisplayWidth = fontRenderer.getStringWidth(name + " ");
                fontRenderer.drawStringWithShadow(modeInfo, x + nameDisplayWidth, y, 0xFFFFFFFF);
            }

             
            y += itemHeight;
            colorOffset++;
        }

        GlStateManager.popMatrix();

         
        HUDModule hud = getHudModule();
        if (hud != null) {
             
            hud.setWidth((int) (maxModuleWidth * scale));
            hud.setHeight((int) (totalModuleHeight * scale));
        }
    }

     


    private String getModeModeInfo(Module module) {
         
        String displayInfo = module.getDisplayInfo();
        if (displayInfo != null && !displayInfo.isEmpty()) {
             
            try {
                Double.parseDouble(displayInfo);
                 
            } catch (NumberFormatException e) {
                 
                if (!displayInfo.contains(" ") && displayInfo.length() <= 15) {
                    return displayInfo.toLowerCase();
                }
            }
        }

         
        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof ModeSetting) {
                String value = ((ModeSetting) setting).getValue();
                if (value != null && !value.isEmpty() && value.length() <= 15) {
                    return value.toLowerCase();
                }
            }
        }

        return "";
    }

    private String getModuleDisplayText(Module module) {
        String info = getModeModeInfo(module);
        if (!info.isEmpty()) {
            return module.getDisplayName().toLowerCase() + " " + info;
        }
        return module.getDisplayName().toLowerCase();
    }

    private void drawCoordinates(int x, int y) {
        if (mc.thePlayer == null)
            return;

         
        String coords = String.format("XYZ: %.0f, %.0f, %.0f",
                mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);

        fontRenderer.drawStringWithShadow(coords, x, y, 0xFFAAAAAA);
    }

    private void drawFPS(int x, int y) {
        int fps = Minecraft.getDebugFPS();
        fontRenderer.drawStringWithShadow("FPS: " + fps, x, y, 0xFFAAAAAA);
    }

    private void drawCPS(int x, int y) {
        long now = System.currentTimeMillis();
        while (!leftClicks.isEmpty() && now - leftClicks.get(0) > 1000)
            leftClicks.remove(0);
        while (!rightClicks.isEmpty() && now - rightClicks.get(0) > 1000)
            rightClicks.remove(0);

        String cps = "CPS: " + leftClicks.size() + " | " + rightClicks.size();
        fontRenderer.drawStringWithShadow(cps, x, y, 0xFFAAAAAA);
    }

    private void drawPing(int x, int y) {
        int ping = com.yesclient.module.modules.visuals.PingModule.getCurrentPing();
        fontRenderer.drawStringWithShadow(ping < 0 ? "Ping: ?ms" : "Ping: " + ping + "ms", x, y,
                ping < 0 ? 0xFF888888 : 0xFFAAAAAA);
    }

    private void drawDirection(int x, int y) {
        float yaw = mc.thePlayer.rotationYaw % 360;
        if (yaw < 0)
            yaw += 360;
        String dir = (yaw >= 315 || yaw < 45) ? "S" : (yaw < 135) ? "W" : (yaw < 225) ? "N" : "E";

        fontRenderer.drawStringWithShadow("Facing: " + dir, x, y, 0xFFAAAAAA);
    }

    private void drawBiome(int x, int y) {
        if (mc.theWorld == null)
            return;

        String biome = mc.theWorld.getBiomeGenForCoords(mc.thePlayer.getPosition()).biomeName;
        fontRenderer.drawStringWithShadow("Biome: " + biome, x, y, 0xFFAAAAAA);
    }

    private void drawTime(int x, int y) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        fontRenderer.drawStringWithShadow(time, x, y, 0xFFAAAAAA);
    }

    private void drawWatermark(int x, int y, com.yesclient.module.modules.client.ClickGUIModule clickGui) {
         
        String line1 = "\u00A7lYes";
        String line2 = "\u00A7lClient";
        int lineHeight = fontRenderer.FONT_HEIGHT + 1;
        clickGui.getWatermarkScale().beginAt(x, y);
        fontRenderer.drawStringWithShadow(line1, x, y, 0xFFFFFFFF);
        fontRenderer.drawStringWithShadow(line2, x, y + lineHeight, 0xFF9EA3AB);
        clickGui.setWidth(Math.max(fontRenderer.getStringWidth(line1), fontRenderer.getStringWidth(line2)));
        clickGui.setHeight(lineHeight * 2);
        clickGui.getWatermarkScale().end();
    }

    private int blendColor(int first, int second, float amount) {
        amount = Math.max(0.0f, Math.min(1.0f, amount));
        int a = (int) (((first >>> 24) & 255) + (((second >>> 24) & 255) - ((first >>> 24) & 255)) * amount);
        int r = (int) (((first >>> 16) & 255) + (((second >>> 16) & 255) - ((first >>> 16) & 255)) * amount);
        int g = (int) (((first >>> 8) & 255) + (((second >>> 8) & 255) - ((first >>> 8) & 255)) * amount);
        int b = (int) ((first & 255) + ((second & 255) - (first & 255)) * amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private void drawVersionInfo(int screenWidth, int screenHeight) {
         
        String version = "Version: " + YesClient.VERSION;
        String user = "User: " + mc.thePlayer.getName();
        String combined = version + "  " + user;

        int w = fontRenderer.getStringWidth(combined);
        fontRenderer.drawStringWithShadow(combined, screenWidth - w - 2, screenHeight - 10, 0xFF666666);
    }

    public void registerLeftClick() {
        leftClicks.add(System.currentTimeMillis());
    }

    public void registerRightClick() {
        rightClicks.add(System.currentTimeMillis());
    }

    public int getLeftCPS() {
        long n = System.currentTimeMillis();
        while (!leftClicks.isEmpty() && n - leftClicks.get(0) > 1000)
            leftClicks.remove(0);
        return leftClicks.size();
    }

    public int getRightCPS() {
        long n = System.currentTimeMillis();
        while (!rightClicks.isEmpty() && n - rightClicks.get(0) > 1000)
            rightClicks.remove(0);
        return rightClicks.size();
    }
}

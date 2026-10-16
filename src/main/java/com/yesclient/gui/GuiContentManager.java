package com.yesclient.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.input.Mouse;
import java.io.File;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class GuiContentManager extends GuiScreen {

    private final GuiScreen parent;
    private final List<File> contentFiles = new ArrayList<>();
    private int scrollOffset = 0;
    private int selectedIndex = -1;
    
    private static final int COLS = 3;
    private static final int PAD = 15;
    
    private final Map<File, ResourceLocation> imageCache = new HashMap<>();

    private long lastClickTime = 0;
    private int lastClickedIndex = -1;
    
    private File fullscreenFile = null;
    private ResourceLocation fullscreenLoc = null;
    private float fullscreenRatio = 1.777f;

    public GuiContentManager(GuiScreen parent) {
        this.parent = parent;
    }
    
    private int getItemWidth() {
        return (this.width - PAD * 4) / COLS;
    }

    private int getImageHeight() {
        return getItemWidth() * 9 / 16;
    }

    private int getItemHeight() {
        return getImageHeight() + 18;  
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 150, this.height - 30, 70, 20, "Back"));
        this.buttonList.add(new GuiButton(1, this.width / 2 - 75, this.height - 30, 70, 20, "Open File"));
        this.buttonList.add(new GuiButton(2, this.width / 2 + 5, this.height - 30, 70, 20, "Copy Path"));
        this.buttonList.add(new GuiButton(3, this.width / 2 + 80, this.height - 30, 70, 20, "Delete"));
        
        loadFiles();
    }

    private void loadFiles() {
        contentFiles.clear();
        File screenshots = new File(Minecraft.getMinecraft().mcDataDir, "screenshots");
        if (screenshots.exists() && screenshots.isDirectory()) {
            File[] files = screenshots.listFiles((dir, name) -> name.endsWith(".png"));
            if (files != null) contentFiles.addAll(Arrays.asList(files));
        }
        
        contentFiles.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        if (selectedIndex >= contentFiles.size()) selectedIndex = -1;
    }
    
    private void queueTextureLoad(File f) {
        if (imageCache.containsKey(f) || !f.getName().endsWith(".png")) return;
        imageCache.put(f, null);  
        new Thread(() -> {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img != null) {
                    int newW = 320;
                    int newH = 180;
                    BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = scaled.createGraphics();
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(img, 0, 0, newW, newH, null);
                    g2.dispose();
                    
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        DynamicTexture tex = new DynamicTexture(scaled);
                        ResourceLocation loc = new ResourceLocation("yesclient", "content_" + f.hashCode());
                        Minecraft.getMinecraft().getTextureManager().loadTexture(loc, tex);
                        imageCache.put(f, loc);
                    });
                }
            } catch (Exception e) {}
        }).start();
    }
    
    private void openFullscreen(File f) {
        fullscreenFile = f;
        fullscreenLoc = null;
        lastClickTime = 0;
        new Thread(() -> {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img != null) {
                    final float ratio = (float) img.getWidth() / img.getHeight();
                    
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        fullscreenRatio = ratio;
                        DynamicTexture tex = new DynamicTexture(img);
                        fullscreenLoc = new ResourceLocation("yesclient", "fullscreen_" + f.hashCode());
                        Minecraft.getMinecraft().getTextureManager().loadTexture(fullscreenLoc, tex);
                    });
                }
            } catch (Exception e) {}
        }).start();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (fullscreenFile != null) return;
        
        if (button.id == 0) {
            this.mc.displayGuiScreen(parent);
        } else if (button.id == 1 && selectedIndex != -1) {
            try {
                Desktop.getDesktop().open(contentFiles.get(selectedIndex));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (button.id == 2 && selectedIndex != -1) {
            String path = contentFiles.get(selectedIndex).getAbsolutePath();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(path), null);
        } else if (button.id == 3 && selectedIndex != -1) {
            File f = contentFiles.get(selectedIndex);
            if (f.delete()) {
                loadFiles();
            }
        }
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        if (fullscreenFile != null) return;  
        
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            if (scroll > 0) scrollOffset -= 40;
            else scrollOffset += 40;
            if (scrollOffset < 0) scrollOffset = 0;
            int rows = (int) Math.ceil((double) contentFiles.size() / COLS);
            int maxScroll = Math.max(0, rows * (getItemHeight() + PAD) - (this.height - 80));
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (fullscreenFile != null) {
            fullscreenFile = null;
            fullscreenLoc = null;
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            int itemW = getItemWidth();
            int itemH = getItemHeight();
            int startX = PAD;
            int startY = 40;
            
            for (int i = 0; i < contentFiles.size(); i++) {
                int col = i % COLS;
                int row = i / COLS;
                int xPos = startX + col * (itemW + PAD);
                int yPos = startY + row * (itemH + PAD) - scrollOffset;
                
                if (yPos >= 40 && yPos <= this.height - 40) {
                    if (mouseX >= xPos && mouseX <= xPos + itemW && mouseY >= yPos && mouseY < yPos + itemH) {
                        if (lastClickedIndex == i && System.currentTimeMillis() - lastClickTime < 300) {
                            File f = contentFiles.get(i);
                            if (f.getName().endsWith(".png")) {
                                openFullscreen(f);
                            }
                        }
                        selectedIndex = i;
                        lastClickedIndex = i;
                        lastClickTime = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (fullscreenFile != null) {
            fullscreenFile = null;
            fullscreenLoc = null;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
    
    private void drawImagePreview(ResourceLocation loc, int x, int y, int width, int height) {
        if (loc == null) return;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(loc);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + height, 0.0D).tex(0.0D, 1.0D).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).tex(1.0D, 1.0D).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).tex(1.0D, 0.0D).endVertex();
        worldrenderer.pos(x, y, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, "Content Manager", this.width / 2, 15, 0xFFFFFF);
        
        this.buttonList.get(1).enabled = selectedIndex != -1;
        this.buttonList.get(2).enabled = selectedIndex != -1;
        this.buttonList.get(3).enabled = selectedIndex != -1;

        int itemW = getItemWidth();
        int itemH = getItemHeight();
        int imgH = getImageHeight();
        
        int startX = PAD;
        int startY = 40;
        
        int scale = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(0, 40 * scale, this.width * scale, (this.height - 80) * scale);

        for (int i = 0; i < contentFiles.size(); i++) {
            File f = contentFiles.get(i);
            int col = i % COLS;
            int row = i / COLS;
            
            int xPos = startX + col * (itemW + PAD);
            int yPos = startY + row * (itemH + PAD) - scrollOffset;
            
            if (yPos < 40 - itemH || yPos > this.height - 40) continue;
            
            if (!imageCache.containsKey(f) && f.getName().endsWith(".png")) {
                queueTextureLoad(f);
            }

            int color = (i == selectedIndex) ? 0xAA55FF55 : 0x88000000;
            drawRect(xPos, yPos, xPos + itemW, yPos + itemH, color);
            
            if (imageCache.get(f) != null) {
                drawImagePreview(imageCache.get(f), xPos + 2, yPos + 2, itemW - 4, imgH);
            } else {
                drawRect(xPos + 2, yPos + 2, xPos + itemW - 2, yPos + 2 + imgH, 0x44FFFFFF);
            }
            
            String name = f.getName();
            int textW = this.fontRendererObj.getStringWidth(name);
            if (textW > itemW - 8) {
                while (name.length() > 0 && this.fontRendererObj.getStringWidth(name + "...") > itemW - 8) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name + "...";
                textW = this.fontRendererObj.getStringWidth(name);
            }
            
            this.drawString(this.fontRendererObj, name, xPos + itemW/2 - textW/2, yPos + imgH + 5, 0xFFFFFF);
        }

        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);

        super.drawScreen(mouseX, mouseY, partialTicks);
        
        if (fullscreenFile != null) {
            drawRect(0, 0, this.width, this.height, 0xEE000000);
            if (fullscreenLoc != null) {
                int maxW = this.width - 40;
                int maxH = this.height - 40;
                int w = maxW;
                int h = (int)(w / fullscreenRatio);
                if (h > maxH) {
                    h = maxH;
                    w = (int)(h * fullscreenRatio);
                }
                
                int x = this.width / 2 - w / 2;
                int y = this.height / 2 - h / 2;
                
                drawImagePreview(fullscreenLoc, x, y, w, h);
            } else {
                this.drawCenteredString(this.fontRendererObj, "Loading High-Res Image...", this.width / 2, this.height / 2, 0xFFFFFF);
            }
        }
    }
}

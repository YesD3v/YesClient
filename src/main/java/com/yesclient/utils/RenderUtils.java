package com.yesclient.utils;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.lang.reflect.Method;
import java.awt.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;


 


public class RenderUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

     
    private static boolean originalViewBobbing;
    private static Method setupCameraTransformMethod;

     












    public static void enableStableMatrix(float partialTicks) {
        originalViewBobbing = mc.gameSettings.viewBobbing;
        if (!originalViewBobbing)
            return;

        mc.gameSettings.viewBobbing = false;

        try {
             
             
            GlStateManager.pushMatrix();

             
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();

            if (setupCameraTransformMethod == null) {
                 
                try {
                    setupCameraTransformMethod = net.minecraft.client.renderer.EntityRenderer.class
                            .getDeclaredMethod("setupCameraTransform", float.class, int.class);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();  
                     
                    setupCameraTransformMethod = net.minecraft.client.renderer.EntityRenderer.class
                            .getDeclaredMethod("func_78479_a", float.class, int.class);
                }
                setupCameraTransformMethod.setAccessible(true);
            }
            setupCameraTransformMethod.invoke(mc.entityRenderer, partialTicks, 0);

        } catch (Exception e) {
            e.printStackTrace();  
        }
    }

     



    public static void disableStableMatrix() {
        if (mc.gameSettings.viewBobbing != originalViewBobbing) {
            mc.gameSettings.viewBobbing = originalViewBobbing;
            GlStateManager.popMatrix();  
        }
    }

     





    public static void enableBobbedViewMatrix(float partialTicks) {
        try {
            GlStateManager.pushMatrix();

            if (setupCameraTransformMethod == null) {
                 
                try {
                    setupCameraTransformMethod = net.minecraft.client.renderer.EntityRenderer.class
                            .getDeclaredMethod("setupCameraTransform", float.class, int.class);
                } catch (NoSuchMethodException e) {
                     
                    setupCameraTransformMethod = net.minecraft.client.renderer.EntityRenderer.class
                            .getDeclaredMethod("func_78479_a", float.class, int.class);
                }
                setupCameraTransformMethod.setAccessible(true);
            }
             
             
            setupCameraTransformMethod.invoke(mc.entityRenderer, partialTicks, 2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void disableBobbedViewMatrix() {
        GlStateManager.popMatrix();
    }

     



     
    private static final FloatBuffer modelview = org.lwjgl.BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer projection = org.lwjgl.BufferUtils.createFloatBuffer(16);
    private static final IntBuffer viewport = org.lwjgl.BufferUtils.createIntBuffer(16);
    private static final FloatBuffer screenCoords = org.lwjgl.BufferUtils.createFloatBuffer(3);

     



    public static double[] toScreen(double x, double y, double z) {
        modelview.clear();
        projection.clear();
        viewport.clear();
        screenCoords.clear();

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

         
         
         
        modelview.rewind();
        projection.rewind();
        viewport.rewind();

        boolean result = org.lwjgl.util.glu.GLU.gluProject((float) x, (float) y, (float) z, modelview, projection,
                viewport, screenCoords);

        if (result) {
            return new double[] { screenCoords.get(0), mc.displayHeight - screenCoords.get(1), screenCoords.get(2) };
        }
        return null;
    }

    public static void draw2DLine(double x1, double y1, double x2, double y2, int color, float width) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();  
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

         
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, mc.displayWidth, mc.displayHeight, 0, 0, 1000);

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();  

        GlStateManager.color(red, green, blue, alpha);
        GL11.glLineWidth(width);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        wr.pos(x1, y1, 0).endVertex();
        wr.pos(x2, y2, 0).endVertex();
        tessellator.draw();

         
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);

        GlStateManager.enableDepth();  
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

     

    public static void drawRect(double x, double y, double width, double height, int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        if (alpha == 0)
            alpha = 1.0f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        wr.pos(x, y + height, 0).endVertex();
        wr.pos(x + width, y + height, 0).endVertex();
        wr.pos(x + width, y, 0).endVertex();
        wr.pos(x, y, 0).endVertex();
        tessellator.draw();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

     


    public static void drawRoundedRect(double x, double y, double width, double height, double radius, int color) {
        if (radius <= 0 || radius > Math.min(width, height) / 2) {
            drawRect(x, y, width, height, color);
            return;
        }

        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        if (alpha == 0)
            alpha = 1.0f;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
         
         
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

         
        wr.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION);

         
         
        for (int i = 0; i <= 90; i += 2) {
            double angle = Math.toRadians(360 - i);  
            wr.pos(x + width - radius + Math.cos(angle) * radius,
                    y + radius + Math.sin(angle) * radius, 0).endVertex();
        }

         
        for (int i = 0; i <= 90; i += 2) {
            double angle = Math.toRadians(270 - i);  
            wr.pos(x + radius + Math.cos(angle) * radius,
                    y + radius + Math.sin(angle) * radius, 0).endVertex();
        }

         
        for (int i = 0; i <= 90; i += 2) {
            double angle = Math.toRadians(180 - i);  
            wr.pos(x + radius + Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius, 0).endVertex();
        }

         
        for (int i = 0; i <= 90; i += 2) {
            double angle = Math.toRadians(90 - i);  
            wr.pos(x + width - radius + Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius, 0).endVertex();
        }

        tessellator.draw();

        GlStateManager.enableCull();  
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

     


    public static void drawRoundedRect(double x, double y, double width, double height, double radius, int color,
            boolean tl, boolean tr, boolean br, boolean bl) {
        if (radius <= 0) {
            drawRect(x, y, width, height, color);
            return;
        }

        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        if (alpha == 0)
            alpha = 1.0f;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        wr.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION);

         
        if (tr) {
            for (int i = 0; i <= 90; i += 2) {
                double angle = Math.toRadians(360 - i);
                wr.pos(x + width - radius + Math.cos(angle) * radius, y + radius + Math.sin(angle) * radius, 0)
                        .endVertex();
            }
        } else {
            wr.pos(x + width, y, 0).endVertex();
        }

         
        if (tl) {
            for (int i = 0; i <= 90; i += 2) {
                double angle = Math.toRadians(270 - i);
                wr.pos(x + radius + Math.cos(angle) * radius, y + radius + Math.sin(angle) * radius, 0).endVertex();
            }
        } else {
            wr.pos(x, y, 0).endVertex();
        }

         
        if (bl) {
            for (int i = 0; i <= 90; i += 2) {
                double angle = Math.toRadians(180 - i);
                wr.pos(x + radius + Math.cos(angle) * radius, y + height - radius + Math.sin(angle) * radius, 0)
                        .endVertex();
            }
        } else {
            wr.pos(x, y + height, 0).endVertex();
        }

         
        if (br) {
            for (int i = 0; i <= 90; i += 2) {
                double angle = Math.toRadians(90 - i);
                wr.pos(x + width - radius + Math.cos(angle) * radius, y + height - radius + Math.sin(angle) * radius, 0)
                        .endVertex();
            }
        } else {
            wr.pos(x + width, y + height, 0).endVertex();
        }

        tessellator.draw();

        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

     


    public static void drawLeftRoundedRect(double x, double y, double width, double height, double radius, int color) {
        if (radius <= 0) {
            drawRect(x, y, width, height, color);
            return;
        }

        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
         
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION);

         
        for (int i = 0; i <= 90; i += 2) {
            double angle = Math.toRadians(270 - i);
            wr.pos(x + radius + Math.cos(angle) * radius,
                    y + radius + Math.sin(angle) * radius, 0).endVertex();
        }

         
        for (int i = 0; i <= 90; i += 2) {
            double angle = Math.toRadians(180 - i);
            wr.pos(x + radius + Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius, 0).endVertex();
        }

         
        wr.pos(x + width, y + height, 0).endVertex();

         
        wr.pos(x + width, y, 0).endVertex();

        tessellator.draw();

         
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

     


    public static void drawBottomRoundedRect(double x, double y, double width, double height, double radius,
            int color) {
        if (radius <= 0) {
            drawRect(x, y, width, height, color);
            return;
        }

        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION);

         
        wr.pos(x + width, y, 0).endVertex();

         
        wr.pos(x, y, 0).endVertex();

         
        for (int i = 0; i <= 90; i += 2) {
            double angle = Math.toRadians(180 - i);
            wr.pos(x + radius + Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius, 0).endVertex();
        }

         
        for (int i = 0; i <= 90; i += 2) {
            double angle = Math.toRadians(90 - i);
            wr.pos(x + width - radius + Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius, 0).endVertex();
        }

        tessellator.draw();

        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void drawFilledArcInternal(WorldRenderer wr, Tessellator tessellator,
            double cx, double cy, double radius, int startAngle, int endAngle) {
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        wr.pos(cx, cy, 0).endVertex();

        for (int i = startAngle; i <= endAngle; i += 2) {
            double angle = Math.toRadians(i);
            wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).endVertex();
        }

        tessellator.draw();
    }

    public static void drawFilledArc(double cx, double cy, double radius, int startAngle, int endAngle) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

         
         
         
         
         
         
         
         
         
         
         

        drawFilledArcInternal(wr, tessellator, cx, cy, radius, startAngle, endAngle);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedBorderedRect(double x, double y, double width, double height,
            double radius, int fillColor, int borderColor, float borderWidth) {
        drawRoundedRect(x, y, width, height, radius, fillColor);
        if (borderWidth > 0) {
            drawRoundedRectOutline(x, y, width, height, radius, borderColor, borderWidth);
        }
    }

    public static void drawAnimatedGradientBorder(double x, double y, double width, double height, double radius,
            float lineWidth) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glLineWidth(lineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);

        long time = System.currentTimeMillis();
         
        double perimeter = 2 * (width + height);
        double currentDist = 0;

         
         
        for (double ix = radius; ix <= width - radius; ix += 5) {
            drawVertexWithColor(x + ix, y, currentDist + (ix - radius), perimeter, time, wr);
        }
        currentDist += (width - 2 * radius);

         
        for (int i = 270; i <= 360; i += 2) {
            double angle = Math.toRadians(i);
            double px = x + width - radius + Math.cos(angle) * radius;
            double py = y + radius + Math.sin(angle) * radius;
            double arcProgress = ((i - 270) / 90.0) * (Math.PI * radius / 2.0);
            drawVertexWithColor(px, py, currentDist + arcProgress, perimeter, time, wr);
        }
        currentDist += (Math.PI * radius / 2.0);

         
        for (double iy = radius; iy <= height - radius; iy += 5) {
            drawVertexWithColor(x + width, y + iy, currentDist + (iy - radius), perimeter, time, wr);
        }
        currentDist += (height - 2 * radius);

         
        for (int i = 0; i <= 90; i += 2) {
            double angle = Math.toRadians(i);
            double px = x + width - radius + Math.cos(angle) * radius;
            double py = y + height - radius + Math.sin(angle) * radius;
            double arcProgress = (i / 90.0) * (Math.PI * radius / 2.0);
            drawVertexWithColor(px, py, currentDist + arcProgress, perimeter, time, wr);
        }
        currentDist += (Math.PI * radius / 2.0);

         
        for (double ix = width - radius; ix >= radius; ix -= 5) {
            drawVertexWithColor(x + ix, y + height, currentDist + (width - radius - ix), perimeter, time, wr);
        }
        currentDist += (width - 2 * radius);

         
        for (int i = 90; i <= 180; i += 2) {
            double angle = Math.toRadians(i);
            double px = x + radius + Math.cos(angle) * radius;
            double py = y + height - radius + Math.sin(angle) * radius;
            double arcProgress = ((i - 90) / 90.0) * (Math.PI * radius / 2.0);
            drawVertexWithColor(px, py, currentDist + arcProgress, perimeter, time, wr);
        }
        currentDist += (Math.PI * radius / 2.0);

         
        for (double iy = height - radius; iy >= radius; iy -= 5) {
            drawVertexWithColor(x, y + iy, currentDist + (height - radius - iy), perimeter, time, wr);
        }
        currentDist += (height - 2 * radius);

         
        for (int i = 180; i <= 270; i += 2) {
            double angle = Math.toRadians(i);
            double px = x + radius + Math.cos(angle) * radius;
            double py = y + radius + Math.sin(angle) * radius;
            double arcProgress = ((i - 180) / 90.0) * (Math.PI * radius / 2.0);
            drawVertexWithColor(px, py, currentDist + arcProgress, perimeter, time, wr);
        }

        tessellator.draw();

        GL11.glLineWidth(1.0f);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void drawVertexWithColor(double x, double y, double dist, double perimeter, long time,
            WorldRenderer wr) {
        float progress = (float) (dist / perimeter);
         
        float animSpeed = 3000f;
        float offset = (time % (long) animSpeed) / animSpeed;

        float hue = (progress - offset);
        if (hue < 0)
            hue += 1.0f;

         
         
        float subHue = hue * 2;
        if (subHue > 1)
            subHue = 2 - subHue;

        float finalHue = 0.5f + (subHue * 0.25f);  

        int color = Color.HSBtoRGB(finalHue, 0.8f, 1.0f);

        float alpha = 1.0f;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        wr.pos(x, y, 0).color(r, g, b, alpha).endVertex();
    }

    public static void drawRoundedRectOutline(double x, double y, double width, double height,
            double radius, int color, float lineWidth) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        if (alpha == 0)
            alpha = 1.0f;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);
        GL11.glLineWidth(lineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);

         
        for (int i = 0; i <= 90; i += 2) {
            double angle = Math.toRadians(i);
            wr.pos(x + width - radius + Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius, 0).endVertex();
        }

         
        for (int i = 90; i <= 180; i += 2) {
            double angle = Math.toRadians(i);
            wr.pos(x + radius + Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius, 0).endVertex();
        }

         
        for (int i = 180; i <= 270; i += 2) {
            double angle = Math.toRadians(i);
            wr.pos(x + radius + Math.cos(angle) * radius,
                    y + radius + Math.sin(angle) * radius, 0).endVertex();
        }

         
        for (int i = 270; i <= 360; i += 2) {
            double angle = Math.toRadians(i);
            wr.pos(x + width - radius + Math.cos(angle) * radius,
                    y + radius + Math.sin(angle) * radius, 0).endVertex();
        }

        tessellator.draw();

        GL11.glLineWidth(1.0f);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawCircle(double cx, double cy, double radius, int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        if (alpha == 0)
            alpha = 1.0f;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        wr.pos(cx, cy, 0).endVertex();
        for (int i = 0; i <= 360; i += 3) {
            double angle = Math.toRadians(i);
            wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).endVertex();
        }
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawGlow(double x, double y, double width, double height, int glowSize, int glowColor) {
        int baseAlpha = (glowColor >> 24) & 0xFF;
        if (baseAlpha == 0)
            baseAlpha = 60;

        for (int i = glowSize; i > 0; i--) {
            float progress = (float) i / glowSize;
            int alpha = (int) (baseAlpha * (1 - progress) * 0.6f);
            int color = (alpha << 24) | (glowColor & 0x00FFFFFF);
            drawRoundedRect(x - i, y - i, width + i * 2, height + i * 2, 4, color);
        }
    }

     

    public static void drawGradientRect(double x, double y, double width, double height,
            int startColor, int endColor, boolean horizontal) {
        float startAlpha = (startColor >> 24 & 0xFF) / 255.0F;
        float startRed = (startColor >> 16 & 0xFF) / 255.0F;
        float startGreen = (startColor >> 8 & 0xFF) / 255.0F;
        float startBlue = (startColor & 0xFF) / 255.0F;

        float endAlpha = (endColor >> 24 & 0xFF) / 255.0F;
        float endRed = (endColor >> 16 & 0xFF) / 255.0F;
        float endGreen = (endColor >> 8 & 0xFF) / 255.0F;
        float endBlue = (endColor & 0xFF) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        if (horizontal) {
            wr.pos(x, y + height, 0).color(startRed, startGreen, startBlue, startAlpha).endVertex();
            wr.pos(x + width, y + height, 0).color(endRed, endGreen, endBlue, endAlpha).endVertex();
            wr.pos(x + width, y, 0).color(endRed, endGreen, endBlue, endAlpha).endVertex();
            wr.pos(x, y, 0).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        } else {
            wr.pos(x, y + height, 0).color(endRed, endGreen, endBlue, endAlpha).endVertex();
            wr.pos(x + width, y + height, 0).color(endRed, endGreen, endBlue, endAlpha).endVertex();
            wr.pos(x + width, y, 0).color(startRed, startGreen, startBlue, startAlpha).endVertex();
            wr.pos(x, y, 0).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        }

        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static int blendColors(int color1, int color2, float ratio) {
        float ir = 1.0f - ratio;
        int a = (int) (((color1 >> 24) & 0xFF) * ir + ((color2 >> 24) & 0xFF) * ratio);
        int r = (int) (((color1 >> 16) & 0xFF) * ir + ((color2 >> 16) & 0xFF) * ratio);
        int g = (int) (((color1 >> 8) & 0xFF) * ir + ((color2 >> 8) & 0xFF) * ratio);
        int b = (int) ((color1 & 0xFF) * ir + (color2 & 0xFF) * ratio);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void scissor(int x, int y, int width, int height) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        int screenHeight = mc.displayHeight;
        GL11.glScissor(x * scale, screenHeight - (y + height) * scale, width * scale, height * scale);
    }

    public static void scissorCustomScale(int x, int y, int width, int height, double scaleFactor) {
        int screenHeight = mc.displayHeight;
        int scX = (int) (x * scaleFactor);
        int scY = (int) (screenHeight - (y + height) * scaleFactor);
        int scW = (int) (width * scaleFactor);
        int scH = (int) (height * scaleFactor);
        GL11.glScissor(scX, scY, scW, scH);
    }

     

    public static void drawBlockESP(BlockPos pos, Color color, float lineWidth) {
        RenderManager rm = mc.getRenderManager();
        double x = pos.getX() - rm.viewerPosX;
        double y = pos.getY() - rm.viewerPosY;
        double z = pos.getZ() - rm.viewerPosZ;
        AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);
        drawOutlinedBox(bb, color, lineWidth);
    }

    public static void drawBlockESPFilled(BlockPos pos, Color fillColor, Color outlineColor, float lineWidth) {
        RenderManager rm = mc.getRenderManager();
        double x = pos.getX() - rm.viewerPosX;
        double y = pos.getY() - rm.viewerPosY;
        double z = pos.getZ() - rm.viewerPosZ;
        AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);
        drawFilledBox(bb, fillColor);
        drawOutlinedBox(bb, outlineColor, lineWidth);
    }

    public static void drawOutlinedBox(AxisAlignedBB bb, Color color, float lineWidth) {
        float alpha = color.getAlpha() / 255.0F;
        float red = color.getRed() / 255.0F;
        float green = color.getGreen() / 255.0F;
        float blue = color.getBlue() / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glLineWidth(lineWidth);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        GlStateManager.disableLighting();

         
        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
         
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
         
        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();

        tessellator.draw();

        GL11.glLineWidth(1.0f);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawFilledBox(AxisAlignedBB bb, int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

         
        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();

        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();

        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();

        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();

        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();

        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();

        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawFilledBox(AxisAlignedBB bb, Color color) {
        drawFilledBox(bb, color.getRGB());
    }

    public static void drawNametagAt(String text, double x, double y, double z) {
        RenderManager rm = mc.getRenderManager();

        double renderX = x - rm.viewerPosX;
        double renderY = y - rm.viewerPosY;
        double renderZ = z - rm.viewerPosZ;

        float scale = 0.02666667f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(renderX, renderY, renderZ);
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        int textWidth = mc.fontRendererObj.getStringWidth(text);
        drawRect(-textWidth / 2 - 2, -2, textWidth + 4, mc.fontRendererObj.FONT_HEIGHT + 4, 0x80000000);

        GlStateManager.enableTexture2D();
        mc.fontRendererObj.drawString(text, -textWidth / 2, 0, 0xFFFFFFFF);

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2,
            int color, float lineWidth) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glLineWidth(lineWidth);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        GlStateManager.disableLighting();
        wr.pos(x1, y1, z1).endVertex();
        wr.pos(x2, y2, z2).endVertex();
        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL11.glLineWidth(1.0f);
        GlStateManager.popMatrix();
    }

    public static Vec3 getCameraPos() {
        modelview.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        modelview.rewind();
        float m0 = modelview.get(0), m1 = modelview.get(1), m2 = modelview.get(2);
        float m4 = modelview.get(4), m5 = modelview.get(5), m6 = modelview.get(6);
        float m8 = modelview.get(8), m9 = modelview.get(9), m10 = modelview.get(10);
        float tx = modelview.get(12), ty = modelview.get(13), tz = modelview.get(14);

        double x = -(m0 * tx + m1 * ty + m2 * tz);
        double y = -(m4 * tx + m5 * ty + m6 * tz);
        double z = -(m8 * tx + m9 * ty + m10 * tz);
        return new Vec3(x, y, z);
    }

     




    public static Vec3 getCrosshairPos(float partialTicks) {
        Entity viewEntity = mc.getRenderViewEntity();
        if (viewEntity == null) {
            viewEntity = mc.thePlayer;
        }
        return viewEntity == null ? new Vec3(0.0D, 0.0D, 0.0D) : viewEntity.getPositionEyes(partialTicks);
    }

     





    public static Vec3 getCameraPos(double zOffset) {
        modelview.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        modelview.rewind();
        float m0 = modelview.get(0), m1 = modelview.get(1), m2 = modelview.get(2);
        float m4 = modelview.get(4), m5 = modelview.get(5), m6 = modelview.get(6);
        float m8 = modelview.get(8), m9 = modelview.get(9), m10 = modelview.get(10);
        float tx = modelview.get(12), ty = modelview.get(13), tz = modelview.get(14);

         
        double x = -(m0 * tx + m1 * ty + m2 * tz);
        double y = -(m4 * tx + m5 * ty + m6 * tz);
        double z = -(m8 * tx + m9 * ty + m10 * tz);

         
         
         
         
        x += m8 * zOffset;
        y += m9 * zOffset;
        z += m10 * zOffset;

        return new Vec3(x, y, z);
    }

    public static void drawTracer(Entity entity, float partialTicks, int color, float lineWidth, Vec3 startPos) {
        RenderManager rm = mc.getRenderManager();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rm.viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - rm.viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rm.viewerPosZ;

         
        drawLine(startPos.xCoord - rm.viewerPosX, startPos.yCoord - rm.viewerPosY,
                startPos.zCoord - rm.viewerPosZ, x, y + entity.height / 2, z, color, lineWidth);
    }

     
    public static void drawTracer(Entity entity, float partialTicks, int color, float lineWidth) {
        drawTracer(entity, partialTicks, color, lineWidth, getCrosshairPos(partialTicks));
    }

    public static void drawTracer(double x, double y, double z, Color color, float lineWidth) {
         
         
         
        drawLine(0, 0, 0, x, y, z, color.getRGB(), lineWidth);
    }

     

    public static void drawShadow(double x, double y, double width, double height, int shadowSize, int shadowColor) {
        int baseAlpha = (shadowColor >> 24) & 0xFF;
        if (baseAlpha == 0)
            baseAlpha = 40;

        for (int i = shadowSize; i > 0; i--) {
            float progress = (float) (shadowSize - i) / shadowSize;
            int alpha = (int) (baseAlpha * (1 - progress * progress));
            int color = (alpha << 24) | (shadowColor & 0x00FFFFFF);
            drawRoundedRect(x - i, y - i, width + i * 2, height + i * 2, 8 + i, color);
        }
    }

    public static void drawHorizontalLine(double x, double y, double width, double height, int color) {
        drawRect(x, y, width, height, color);
    }

    public static void drawVerticalLine(double x, double y, double height, double width, int color) {
        drawRect(x, y, width, height, color);
    }

    public static void drawPill(double x, double y, double width, double height, int color) {
        double radius = height / 2;
        drawRoundedRect(x, y, width, height, radius, color);
    }

    public static void drawBorderedRect(double x, double y, double width, double height,
            double borderWidth, int fillColor, int borderColor) {
        drawRect(x, y, width, height, borderColor);
        drawRect(x + borderWidth, y + borderWidth, width - borderWidth * 2, height - borderWidth * 2, fillColor);
    }

    public static ScaledResolution getScaledResolution() {
        return new ScaledResolution(mc);
    }

    public static void drawGlowingText(FontRenderer fr, String text, int x, int y) {
         
        String clean = net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(text);

        long time = System.currentTimeMillis();

         
        int currentX = x;
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            String ch = String.valueOf(c);

             
            double offset = (i * 0.1) + (time / 1000.0);
            double val = Math.sin(offset);  
             
            float hue = 0.5f + (float) (val * 0.2f);
            int color = Color.HSBtoRGB(hue, 0.7f, 1.0f);

             
             
             
             

            int glowAlpha = 0x15000000;  
            int glowColor = (color & 0x00FFFFFF) | glowAlpha;

             
             
            fr.drawString("\u00A7l" + ch, currentX, y - 0.5f, glowColor, false);
            fr.drawString("\u00A7l" + ch, currentX, y + 0.5f, glowColor, false);
            fr.drawString("\u00A7l" + ch, currentX - 0.5f, y, glowColor, false);
            fr.drawString("\u00A7l" + ch, currentX + 0.5f, y, glowColor, false);

             
            fr.drawString("\u00A7l" + ch, currentX - 0.35f, y - 0.35f, glowColor, false);
            fr.drawString("\u00A7l" + ch, currentX + 0.35f, y - 0.35f, glowColor, false);
            fr.drawString("\u00A7l" + ch, currentX - 0.35f, y + 0.35f, glowColor, false);
            fr.drawString("\u00A7l" + ch, currentX + 0.35f, y + 0.35f, glowColor, false);

             
            fr.drawString("\u00A7l" + ch, currentX, y, color, false);

             
            currentX += fr.getStringWidth("\u00A7l" + ch);
        }
    }

     

     






    public static Color getRainbowColor(float offset) {
        long time = System.currentTimeMillis();
        float speed = 2000f;  
        float hue = ((time % (long) speed) / speed + offset) % 1.0f;
        return Color.getHSBColor(hue, 0.8f, 1.0f);
    }

     






    public static Color getRainbowColor(float offset, float speedMs) {
        long time = System.currentTimeMillis();
        float hue = ((time % (long) speedMs) / speedMs + offset) % 1.0f;
        return Color.getHSBColor(hue, 0.8f, 1.0f);
    }

     







    public static Color getRainbowGradient(double progress, float animSpeed) {
        long time = System.currentTimeMillis();
        float offset = (time % (long) animSpeed) / animSpeed;
        float hue = (float) ((progress + offset) % 1.0);
        return Color.getHSBColor(hue, 0.8f, 1.0f);
    }

     





    public static int getRainbowRGB(double progress) {
        return getRainbowGradient(progress, 2000f).getRGB();
    }

     



    public static void drawRainbowLine(double x1, double y1, double z1, double x2, double y2, double z2,
            float lineWidth, float animSpeed) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glLineWidth(lineWidth);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GlStateManager.disableLighting();

         
        Color startColor = getRainbowGradient(0.0, animSpeed);
         
        Color endColor = getRainbowGradient(0.5, animSpeed);

        wr.pos(x1, y1, z1).color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), 255).endVertex();
        wr.pos(x2, y2, z2).color(endColor.getRed(), endColor.getGreen(), endColor.getBlue(), 255).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL11.glLineWidth(1.0f);
        GlStateManager.popMatrix();
    }

     



    public static void drawRainbowOutlinedBox(AxisAlignedBB bb, float lineWidth, float animSpeed) {
        long time = System.currentTimeMillis();
        float offset = (time % (long) animSpeed) / animSpeed;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glLineWidth(lineWidth);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

         
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        addRainbowVertex(wr, bb.minX, bb.minY, bb.minZ, offset + 0.0f);
        addRainbowVertex(wr, bb.maxX, bb.minY, bb.minZ, offset + 0.0833f);
        addRainbowVertex(wr, bb.maxX, bb.minY, bb.maxZ, offset + 0.1666f);
        addRainbowVertex(wr, bb.minX, bb.minY, bb.maxZ, offset + 0.25f);
        tessellator.draw();

         
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        addRainbowVertex(wr, bb.minX, bb.maxY, bb.minZ, offset + 0.333f);
        addRainbowVertex(wr, bb.maxX, bb.maxY, bb.minZ, offset + 0.416f);
        addRainbowVertex(wr, bb.maxX, bb.maxY, bb.maxZ, offset + 0.5f);
        addRainbowVertex(wr, bb.minX, bb.maxY, bb.maxZ, offset + 0.583f);
        tessellator.draw();

         
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
         
        addRainbowVertex(wr, bb.minX, bb.minY, bb.minZ, offset + 0.666f);
        addRainbowVertex(wr, bb.minX, bb.maxY, bb.minZ, offset + 0.75f);
         
        addRainbowVertex(wr, bb.maxX, bb.minY, bb.minZ, offset + 0.75f);
        addRainbowVertex(wr, bb.maxX, bb.maxY, bb.minZ, offset + 0.833f);
         
        addRainbowVertex(wr, bb.maxX, bb.minY, bb.maxZ, offset + 0.833f);
        addRainbowVertex(wr, bb.maxX, bb.maxY, bb.maxZ, offset + 0.916f);
         
        addRainbowVertex(wr, bb.minX, bb.minY, bb.maxZ, offset + 0.916f);
        addRainbowVertex(wr, bb.minX, bb.maxY, bb.maxZ, offset + 1.0f);
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GL11.glLineWidth(1.0f);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void addRainbowVertex(WorldRenderer wr, double x, double y, double z, float hueOffset) {
        float hue = hueOffset % 1.0f;
        Color c = Color.getHSBColor(hue, 0.8f, 1.0f);
        wr.pos(x, y, z).color(c.getRed(), c.getGreen(), c.getBlue(), 255).endVertex();
    }

     







    public static int drawRainbowString(String text, float x, float y, boolean shadow) {
        text = com.yesclient.module.modules.visuals.NickHiderModule.replaceOwnName(text);
        FontRenderer fr = mc.fontRendererObj;
        float currentX = x;

         
        String cleanText = net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(text);
        if (cleanText == null)
            cleanText = "";

         
        long time = System.currentTimeMillis();
        float hueSpeed = 3000f;  
        float timeOffset = (time % (long) hueSpeed) / hueSpeed;

        for (int i = 0; i < cleanText.length(); i++) {
            char c = cleanText.charAt(i);
            String charStr = String.valueOf(c);

             
             
            float hue = timeOffset - (i * 0.05f);
            if (hue < 0)
                hue += 1.0f;

            int color = Color.HSBtoRGB(hue, 0.8f, 1.0f);

            if (shadow) {
                fr.drawStringWithShadow(charStr, currentX, y, color);
            } else {
                fr.drawString(charStr, (int) currentX, (int) y, color);
            }

            currentX += fr.getStringWidth(charStr);
        }

        return (int) currentX;
    }
}

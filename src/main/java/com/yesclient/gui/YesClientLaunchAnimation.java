package com.yesclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedString;

public final class YesClientLaunchAnimation {
    private static final float CUBE_VIEW_X = -7.0F;
    private static final float CUBE_VIEW_Y = -10.5F;
    private static final float CUBE_VIEW_WIDTH = 194.0F;
    private static final float CUBE_VIEW_HEIGHT = 221.0F;
    private static final float CUBE_STROKE = 13.0F;
    private static final float TITLE_SIZE = 42.0F;
    private static final float TITLE_GAP = 18.0F;
    private static final float TOTAL_DURATION = 11.0F;
    private static final float[][] OUTER_CUBE = {
            {90.0F, 0.0F}, {176.6F, 50.0F}, {176.6F, 150.0F}, {90.0F, 200.0F},
            {3.4F, 150.0F}, {3.4F, 50.0F}
    };
    private static final float[][] OUTER_LEFT = {
            {90.0F, 0.0F}, {3.4F, 50.0F}, {3.4F, 150.0F}, {90.0F, 200.0F}
    };
    private static final float[][] OUTER_RIGHT = {
            {90.0F, 0.0F}, {176.6F, 50.0F}, {176.6F, 150.0F}, {90.0F, 200.0F}
    };
    private static final float[][][] INNER_CUBE = {
            {{3.4F, 50.0F}, {90.0F, 100.0F}, {176.6F, 50.0F}},
            {{90.0F, 100.0F}, {90.0F, 200.0F}}
    };

    private static boolean started;
    private static boolean finished;
    private static long startedAt;
    private static int textureScale = -1;
    private static int textureScreenWidth = -1;
    private static int textureScreenHeight = -1;
    private static BufferedImage logoImage;
    private static BufferedImage titleImage;
    private static DynamicTexture logoTexture;
    private static DynamicTexture titleTexture;
    private static ResourceLocation logoLocation;
    private static ResourceLocation titleLocation;
    private static Font archivoFont;

    private YesClientLaunchAnimation() {
    }

    public static void begin() {
        if (started) return;
        started = true;
        startedAt = System.nanoTime();
    }

    public static boolean isActive() {
        if (!started || finished) return false;
        if (elapsedSeconds() >= TOTAL_DURATION) {
            finished = true;
            return false;
        }
        return true;
    }

    public static void render(int screenWidth, int screenHeight) {
        if (!isActive()) return;

        float elapsed = elapsedSeconds();
        float fade = elapsed <= 10.5F ? 1.0F : 1.0F - ease((elapsed - 10.5F) / 0.5F);
        Gui.drawRect(0, 0, screenWidth, screenHeight, Math.round(clamp(fade) * 255.0F) << 24);

        if (elapsed < 2.0F && com.yesclient.YesClient.getInstance() != null
                && com.yesclient.YesClient.getInstance().getClickGUI() != null) {
            com.yesclient.YesClient.getInstance().getClickGUI().prepareForFirstOpen(12);
        }

        float logoScale = Math.min(0.82F,
                Math.min(screenWidth * 0.68F / CUBE_VIEW_WIDTH, screenHeight * 0.42F / CUBE_VIEW_HEIGHT));
        int sampleScale = Math.max(4, new net.minecraft.client.gui.ScaledResolution(Minecraft.getMinecraft()).getScaleFactor());
        ensureTextures(sampleScale, screenWidth, screenHeight, logoScale);

        float logoWidth = CUBE_VIEW_WIDTH * logoScale;
        float logoHeight = CUBE_VIEW_HEIGHT * logoScale;
        float titleHeight = titleImage.getHeight() / (float) sampleScale;
        float contentHeight = logoHeight + TITLE_GAP + titleHeight;
        float logoTop = (screenHeight - contentHeight) * 0.5F;
        float logoLeft = (screenWidth - logoWidth) * 0.5F;

        if (elapsed >= 2.0F && elapsed < 10.5F) {
            float outerProgress;
            float innerProgress;
            if (elapsed < 4.0F) {
                outerProgress = ease((elapsed - 2.0F) / 1.4F);
                innerProgress = ease((elapsed - 2.6F) / 1.4F);
            } else if (elapsed < 8.0F) {
                outerProgress = 1.0F;
                innerProgress = 1.0F;
            } else {
                innerProgress = 1.0F - ease((elapsed - 8.0F) / 1.4F);
                outerProgress = 1.0F - ease((elapsed - 8.9F) / 1.6F);
            }
            updateLogoTexture(outerProgress, innerProgress, logoScale, sampleScale, elapsed >= 8.0F);
            drawTexture(logoLocation, logoLeft, logoTop, logoWidth, logoHeight, 1.0F);
        }

        float titleAlpha = 0.0F;
        if (elapsed >= 4.0F && elapsed < 4.5F) {
            titleAlpha = ease((elapsed - 4.0F) / 0.5F);
        } else if (elapsed >= 4.5F && elapsed < 7.5F) {
            titleAlpha = 1.0F;
        } else if (elapsed >= 7.5F && elapsed < 8.0F) {
            titleAlpha = 1.0F - ease((elapsed - 7.5F) / 0.5F);
        }
        if (titleAlpha > 0.0F) {
            float titleWidth = titleImage.getWidth() / (float) sampleScale;
            float titleX = (screenWidth - titleWidth) * 0.5F;
            drawTexture(titleLocation, titleX, logoTop + logoHeight + TITLE_GAP,
                    titleWidth, titleHeight, titleAlpha);
        }
    }

    private static float elapsedSeconds() {
        return started ? (System.nanoTime() - startedAt) / 1_000_000_000.0F : 0.0F;
    }

    private static void ensureTextures(int sampleScale, int screenWidth, int screenHeight, float logoScale) {
        if (textureScale == sampleScale && textureScreenWidth == screenWidth && textureScreenHeight == screenHeight
                && logoTexture != null && titleTexture != null) return;

        textureScale = sampleScale;
        textureScreenWidth = screenWidth;
        textureScreenHeight = screenHeight;
        int width = Math.max(1, Math.round(CUBE_VIEW_WIDTH * logoScale * sampleScale));
        int height = Math.max(1, Math.round(CUBE_VIEW_HEIGHT * logoScale * sampleScale));
        logoImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        logoTexture = new DynamicTexture(logoImage);
        titleImage = createTitleImage(sampleScale);
        titleTexture = new DynamicTexture(titleImage);

        Minecraft minecraft = Minecraft.getMinecraft();
        logoLocation = minecraft.getTextureManager().getDynamicTextureLocation("yesclient-launch-cube", logoTexture);
        titleLocation = minecraft.getTextureManager().getDynamicTextureLocation("yesclient-launch-wordmark", titleTexture);
    }

    private static BufferedImage createTitleImage(int sampleScale) {
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measure = scratch.createGraphics();
        configureQuality(measure);
        Font font = loadArchivoFont();
        if (font == null) font = new Font("Arial", Font.BOLD, Math.round(TITLE_SIZE * sampleScale));
        font = font.deriveFont(TITLE_SIZE * sampleScale);
        AttributedString attributed = new AttributedString("YesClient");
        attributed.addAttribute(TextAttribute.FONT, font);
        attributed.addAttribute(TextAttribute.TRACKING, -0.025F);
        TextLayout layout = new TextLayout(attributed.getIterator(), measure.getFontRenderContext());
        int padding = 4 * sampleScale;
        int width = Math.max(1, (int) Math.ceil(layout.getAdvance() + padding * 2.0F));
        int height = Math.max(1, (int) Math.ceil(measure.getFontMetrics(font).getHeight() + padding * 2.0F));
        measure.dispose();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        configureQuality(graphics);
        graphics.setColor(Color.WHITE);
        graphics.setFont(font);
        layout.draw(graphics, padding, padding + graphics.getFontMetrics(font).getAscent());
        graphics.dispose();
        return image;
    }

    private static Font loadArchivoFont() {
        if (archivoFont != null) return archivoFont;
        try (InputStream input = YesClientLaunchAnimation.class.getResourceAsStream(
                "/assets/yesclient/fonts/Archivo-ExtraBold-Expanded.ttf")) {
            if (input != null) archivoFont = Font.createFont(Font.TRUETYPE_FONT, input);
        } catch (FontFormatException | IOException ignored) {
        }
        return archivoFont;
    }

    private static void updateLogoTexture(float outerProgress, float innerProgress, float logoScale, int sampleScale, boolean erasing) {
        Graphics2D graphics = logoImage.createGraphics();
        configureQuality(graphics);
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, logoImage.getWidth(), logoImage.getHeight());
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setColor(Color.WHITE);
        float rasterScale = logoScale * sampleScale;
        graphics.translate(-CUBE_VIEW_X * rasterScale, -CUBE_VIEW_Y * rasterScale);
        graphics.scale(rasterScale, rasterScale);
        graphics.setStroke(new BasicStroke(CUBE_STROKE, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

        if (outerProgress >= 1.0F) {
            graphics.draw(partialPath(OUTER_CUBE, 1.0F, true));
        } else if (outerProgress > 0.0F) {
            drawForkProgress(graphics, OUTER_LEFT, OUTER_RIGHT, outerProgress, erasing);
        }
        drawSubpaths(graphics, INNER_CUBE, innerProgress, erasing);
        graphics.dispose();

        int[] source = ((DataBufferInt) logoImage.getRaster().getDataBuffer()).getData();
        int[] target = logoTexture.getTextureData();
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
        logoTexture.updateDynamicTexture();
    }

    private static void drawSubpaths(Graphics2D graphics, float[][][] paths, float progress, boolean erasing) {
        float totalLength = 0.0F;
        for (float[][] path : paths) totalLength += pathLength(path);
        float targetLength = totalLength * clamp(progress);
        float passed = 0.0F;
        for (float[][] path : paths) {
            float length = pathLength(path);
            float pathProgress = clamp((targetLength - passed) / length);
            if (pathProgress > 0.0F) drawProgressStroke(graphics, path, pathProgress, erasing);
            passed += length;
        }
    }

    private static void drawProgressStroke(Graphics2D graphics, float[][] points, float progress, boolean erasing) {
        float clampedProgress = clamp(progress);
        if (clampedProgress <= 0.0F) return;
        float drawnLength = pathLength(points) * clampedProgress;
        if (drawnLength < CUBE_STROKE) return;
        float strokeWidth = CUBE_STROKE;
        java.awt.Stroke previousStroke = graphics.getStroke();
        graphics.setStroke(new BasicStroke(strokeWidth,
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        graphics.draw(partialPath(points, clampedProgress, false));
        graphics.setStroke(previousStroke);
        if (clampedProgress < 1.0F && drawnLength >= CUBE_STROKE) {
            float[] endpoint = pointAlongPath(points, clampedProgress);
            float radius = strokeWidth * 0.5F;
            graphics.fill(new java.awt.geom.Ellipse2D.Float(endpoint[0] - radius, endpoint[1] - radius,
                    radius * 2.0F, radius * 2.0F));
        }
    }

    private static void drawForkProgress(Graphics2D graphics, float[][] left, float[][] right, float progress, boolean erasing) {
        float clampedProgress = clamp(progress);
        if (clampedProgress <= 0.0F) return;
        float drawnLength = pathLength(left) * clampedProgress;
        if (drawnLength < CUBE_STROKE) return;
        float[][] leftPoints = progressPoints(left, clampedProgress);
        float[][] rightPoints = progressPoints(right, clampedProgress);
        java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();
        float[] leftEnd = leftPoints[leftPoints.length - 1];
        path.moveTo(leftEnd[0], leftEnd[1]);
        for (int i = leftPoints.length - 2; i >= 0; i--) path.lineTo(leftPoints[i][0], leftPoints[i][1]);
        for (int i = 1; i < rightPoints.length; i++) path.lineTo(rightPoints[i][0], rightPoints[i][1]);
        float strokeWidth = CUBE_STROKE;
        java.awt.Stroke previousStroke = graphics.getStroke();
        graphics.setStroke(new BasicStroke(strokeWidth,
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        graphics.draw(path);
        graphics.setStroke(previousStroke);
        if (clampedProgress < 1.0F && drawnLength >= CUBE_STROKE) {
            float radius = strokeWidth * 0.5F;
            float[] rightEnd = rightPoints[rightPoints.length - 1];
            graphics.fill(new java.awt.geom.Ellipse2D.Float(leftEnd[0] - radius, leftEnd[1] - radius,
                    radius * 2.0F, radius * 2.0F));
            graphics.fill(new java.awt.geom.Ellipse2D.Float(rightEnd[0] - radius, rightEnd[1] - radius,
                    radius * 2.0F, radius * 2.0F));
        }
    }

    private static float[][] progressPoints(float[][] points, float progress) {
        float targetLength = pathLength(points) * clamp(progress);
        java.util.ArrayList<float[]> result = new java.util.ArrayList<float[]>();
        result.add(points[0]);
        float passed = 0.0F;
        for (int i = 1; i < points.length; i++) {
            float dx = points[i][0] - points[i - 1][0];
            float dy = points[i][1] - points[i - 1][1];
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (targetLength >= passed + length) {
                result.add(points[i]);
                passed += length;
            } else {
                float fraction = length <= 0.0F ? 0.0F : (targetLength - passed) / length;
                if (fraction > 0.0F) {
                    result.add(new float[] { points[i - 1][0] + dx * fraction, points[i - 1][1] + dy * fraction });
                }
                break;
            }
        }
        return result.toArray(new float[result.size()][]);
    }

    private static float[] pointAlongPath(float[][] points, float progress) {
        float remaining = pathLength(points) * clamp(progress);
        for (int i = 1; i < points.length; i++) {
            float dx = points[i][0] - points[i - 1][0];
            float dy = points[i][1] - points[i - 1][1];
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (remaining <= length || i == points.length - 1) {
                float fraction = length <= 0.0F ? 0.0F : Math.min(1.0F, remaining / length);
                return new float[] { points[i - 1][0] + dx * fraction, points[i - 1][1] + dy * fraction };
            }
            remaining -= length;
        }
        return new float[] { points[points.length - 1][0], points[points.length - 1][1] };
    }

    private static java.awt.geom.Path2D.Float partialPath(float[][] points, float progress, boolean closed) {
        java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();
        path.moveTo(points[0][0], points[0][1]);
        float normalizedProgress = clamp(progress);
        if (closed && normalizedProgress >= 1.0F) {
            for (int i = 1; i < points.length; i++) path.lineTo(points[i][0], points[i][1]);
            path.closePath();
            return path;
        }
        float remaining = (closed ? closedPathLength(points) : pathLength(points)) * normalizedProgress;
        for (int i = 1; i < points.length && remaining > 0.0F; i++) {
            float dx = points[i][0] - points[i - 1][0];
            float dy = points[i][1] - points[i - 1][1];
            float segmentLength = (float) Math.sqrt(dx * dx + dy * dy);
            if (remaining >= segmentLength) {
                path.lineTo(points[i][0], points[i][1]);
                remaining -= segmentLength;
            } else {
                float fraction = remaining / segmentLength;
                path.lineTo(points[i - 1][0] + dx * fraction, points[i - 1][1] + dy * fraction);
                remaining = 0.0F;
            }
        }
        if (closed && remaining > 0.0F) {
            float dx = points[0][0] - points[points.length - 1][0];
            float dy = points[0][1] - points[points.length - 1][1];
            float segmentLength = (float) Math.sqrt(dx * dx + dy * dy);
            if (remaining >= segmentLength) path.lineTo(points[0][0], points[0][1]);
            else if (segmentLength > 0.0F) {
                float fraction = remaining / segmentLength;
                path.lineTo(points[points.length - 1][0] + dx * fraction,
                        points[points.length - 1][1] + dy * fraction);
            }
        }
        return path;
    }

    private static float closedPathLength(float[][] points) {
        float length = pathLength(points);
        float dx = points[0][0] - points[points.length - 1][0];
        float dy = points[0][1] - points[points.length - 1][1];
        return length + (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static float pathLength(float[][] points) {
        float length = 0.0F;
        for (int i = 1; i < points.length; i++) {
            float dx = points[i][0] - points[i - 1][0];
            float dy = points[i][1] - points[i - 1][1];
            length += (float) Math.sqrt(dx * dx + dy * dy);
        }
        return length;
    }

    private static void drawTexture(ResourceLocation location, float x, float y, float width, float height, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.disableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, clamp(alpha));
        Minecraft.getMinecraft().getTextureManager().bindTexture(location);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2f(x, y + height);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2f(x + width, y);
        GL11.glEnd();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private static void configureQuality(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static float ease(float value) {
        float x = clamp(value);
        float low = 0.0F;
        float high = 1.0F;
        for (int i = 0; i < 12; i++) {
            float t = (low + high) * 0.5F;
            if (bezier(t, 0.6F, 0.2F) < x) low = t;
            else high = t;
        }
        return bezier((low + high) * 0.5F, 0.0F, 1.0F);
    }

    private static float bezier(float t, float firstControl, float secondControl) {
        float inverse = 1.0F - t;
        return 3.0F * inverse * inverse * t * firstControl
                + 3.0F * inverse * t * t * secondControl + t * t * t;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}

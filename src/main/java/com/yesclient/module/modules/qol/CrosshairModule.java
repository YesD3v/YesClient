package com.yesclient.module.modules.qol;

import com.yesclient.gui.crosshair.GuiCustomCrosshair;
import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.ActionSetting;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ColorSetting;
import com.yesclient.module.settings.ModeSetting;
import com.yesclient.module.settings.NumberSetting;
import com.yesclient.module.settings.StringSetting;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Set;

 




public class CrosshairModule extends Module {

    public static final int GRID_RADIUS = 7;  

    private final ModeSetting preset = new ModeSetting("Preset", "Crosshair shape",
            "Cross", "Cross", "Cross + Dot", "Dot", "Caret", "Custom");
    private final ActionSetting editCustom = new ActionSetting("Draw Custom",
            "Draw your own crosshair on a grid", "EDIT", this::openEditor);

    private final NumberSetting scale = new NumberSetting("Scale",
            "Size of each crosshair pixel", 2, 1, 5);
    private final NumberSetting length = new NumberSetting("Arm Length",
            "How long the arms are", 4, 1, 7);
    private final NumberSetting gap = new NumberSetting("Gap",
            "Empty space between the center and the arms", 1, 0, 5);
    private final NumberSetting thickness = new NumberSetting("Thickness",
            "How thick the arms are (1 = thinnest)", 1, 1, 3);
    private final NumberSetting dotSize = new NumberSetting("Dot Size",
            "How big the center dot is (1 = single pixel)", 1, 1, 3);

    private final BooleanSetting autoContrast = new BooleanSetting("Auto Contrast",
            "Inverts against what's behind it so it's always visible", true);
    private final ColorSetting color = new ColorSetting("Color", "Crosshair color", 255, 255, 255, 255);
    private final BooleanSetting entityHighlight = new BooleanSetting("Entity Highlight",
            "Changes color when aiming at an entity", true);
    private final ColorSetting entityColor = new ColorSetting("Entity Color",
            "Color used while aiming at an entity", 255, 60, 60, 255);

     
    public final StringSetting customData = new StringSetting("Custom Data", "Custom grid pixels", "", 8192);

    public CrosshairModule() {
        super("Crosshair", "Custom crosshair editor", Category.QOL, ModuleMode.BOTH);

        editCustom.setAvailability(() -> preset.is("Custom"));
        length.setAvailability(() -> preset.is("Cross") || preset.is("Cross + Dot") || preset.is("Caret"));
        gap.setAvailability(() -> preset.is("Cross") || preset.is("Cross + Dot"));
        thickness.setAvailability(() -> preset.is("Cross") || preset.is("Cross + Dot"));
        dotSize.setAvailability(() -> preset.is("Dot") || preset.is("Cross + Dot"));
        color.setAvailability(() -> !autoContrast.getValue());
        entityColor.setAvailability(() -> entityHighlight.getValue());
        customData.setVisibility(() -> false);

        addSetting(preset);
        addSetting(editCustom);
        addSetting(scale);
        addSetting(length);
        addSetting(gap);
        addSetting(thickness);
        addSetting(dotSize);
        addSetting(autoContrast);
        addSetting(color);
        addSetting(entityHighlight);
        addSetting(entityColor);
        addSetting(customData);
    }

    private void openEditor() {
        mc.displayGuiScreen(new GuiCustomCrosshair(mc.currentScreen, this));
    }

    public int getScale() {
        return scale.getIntValue();
    }

     
    public Set<Long> buildShape() {
        Set<Long> pts = new LinkedHashSet<>();
        String mode = preset.getValue();
        int len = length.getIntValue();

        if (mode.equals("Cross") || mode.equals("Cross + Dot")) {
            int g = gap.getIntValue();
            int h = thickness.getIntValue() - 1;  
            for (int i = g + 1; i <= g + len; i++) {
                for (int t = -h; t <= h; t++) {
                    add(pts, i, t);
                    add(pts, -i, t);
                    add(pts, t, i);
                    add(pts, t, -i);
                }
            }
            if (g == 0 && mode.equals("Cross")) add(pts, 0, 0);
        }
        if (mode.equals("Dot") || mode.equals("Cross + Dot")) {
            int d = dotSize.getIntValue() - 1;
            for (int x = -d; x <= d; x++)
                for (int y = -d; y <= d; y++) add(pts, x, y);
        }
        if (mode.equals("Caret")) {
             
            add(pts, 0, 0);
            for (int r = 1; r <= len; r++) {
                add(pts, -r, r);
                add(pts, -(r - 1), r);
                add(pts, r - 1, r);
                add(pts, r, r);
            }
        }
        if (mode.equals("Custom")) {
            pts.addAll(parseCustom(customData.getValue()));
        }
        return pts;
    }

    public static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    public static int keyX(long k) {
        return (int) (k >> 32);
    }

    public static int keyY(long k) {
        return (int) k;
    }

    private static void add(Set<Long> pts, int x, int y) {
        pts.add(key(x, y));
    }

    public static Set<Long> parseCustom(String data) {
        Set<Long> pts = new LinkedHashSet<>();
        if (data == null || data.isEmpty()) return pts;
        for (String p : data.split(";")) {
            String[] c = p.split(",");
            if (c.length != 2) continue;
            try {
                int x = Integer.parseInt(c[0].trim());
                int y = Integer.parseInt(c[1].trim());
                if (Math.abs(x) <= GRID_RADIUS && Math.abs(y) <= GRID_RADIUS) add(pts, x, y);
            } catch (NumberFormatException ignored) {
            }
        }
        return pts;
    }

    public static String serializeCustom(Set<Long> pts) {
        StringBuilder sb = new StringBuilder();
        for (long k : pts) sb.append(keyX(k)).append(',').append(keyY(k)).append(';');
        return sb.toString();
    }

     



    public static void drawPixels(Set<Long> pts, int cx, int cy, int px) {
        if (pts.isEmpty()) return;
        int half = px / 2;
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        for (long k : pts) {
            double l = cx + keyX(k) * px - half;
            double t = cy + keyY(k) * px - half;
            double r = l + px;
            double b = t + px;
            wr.pos(l, b, 0).endVertex();
            wr.pos(r, b, 0).endVertex();
            wr.pos(r, t, 0).endVertex();
            wr.pos(l, t, 0).endVertex();
        }
        tess.draw();
    }

    @SubscribeEvent
    public void onRenderCrosshair(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        event.setCanceled(true);
        if (mc.gameSettings.thirdPersonView != 0) return;  

        boolean onEntity = entityHighlight.getValue() && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY;
        boolean invert = autoContrast.getValue() && !onEntity;
        Color c = onEntity ? entityColor.getColor() : color.getColor();

        ScaledResolution sr = new ScaledResolution(mc);
        float sf = sr.getScaleFactor();

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f / sf, 1.0f / sf, 1.0f);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        if (invert) {
            GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR,
                    GL11.GL_ONE, GL11.GL_ZERO);
            GlStateManager.color(1f, 1f, 1f, 1f);
        } else {
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ZERO);
            GlStateManager.color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f,
                    Math.max(0.1f, c.getAlpha() / 255f));
        }

        drawPixels(buildShape(), mc.displayWidth / 2, mc.displayHeight / 2, getScale());

        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }
}

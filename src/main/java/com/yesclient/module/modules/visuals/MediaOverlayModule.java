package com.yesclient.module.modules.visuals;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.AlphaComposite;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.imageio.ImageIO;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yesclient.YesClient;
import com.yesclient.gui.HudEditorGUI;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.HudDesignerModule;
import com.yesclient.module.settings.ColorSetting;
import com.yesclient.module.settings.NumberSetting;
import com.yesclient.utils.MarqueeText;
import com.yesclient.utils.RenderUtils;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

 
public class MediaOverlayModule extends Module {

    private final HudScale hudScale = new HudScale(this);
    private static final int MIN_WIDTH = 160;
    private static final int MAX_WIDTH = 560;
    private static final int MIN_HEIGHT = 56;
    private static final int MAX_HEIGHT = 150;
    private static final int MAX_COVER_BYTES = 1024 * 1024;

    private final NumberSetting overlayWidth = new NumberSetting("Width", "Media card width", 310, MIN_WIDTH, MAX_WIDTH);
    private final NumberSetting overlayHeight = new NumberSetting("Height", "Media card height", 82, MIN_HEIGHT, MAX_HEIGHT);
    private final ColorSetting backgroundColor = new ColorSetting("Background Color", "Media card background", 18, 21, 27, 255);
    private final NumberSetting opacity = new NumberSetting("Opacity", "Media card background opacity", 90.0D, 0.0D, 100.0D, 5.0D);
    private final ColorSetting titleColor = new ColorSetting("Title Color", "Song title color", 244, 245, 247, 255);
    private final ColorSetting artistColor = new ColorSetting("Artist Color", "Artist text color", 156, 163, 173, 255);
    private final ColorSetting progressColor = new ColorSetting("Progress Color", "Playback line and indicator color", 255, 255, 255, 255);
    private final Object titleMarquee = new Object();
    private final Object artistMarquee = new Object();

    private volatile boolean readerRunning;
    private volatile int readerGeneration;
    private volatile Process readerProcess;
    private Thread readerThread;
    private volatile MediaSnapshot snapshot = MediaSnapshot.EMPTY;
    private volatile long missingMediaSince;

    private String textureTrackKey;
    private int textureArtSize;
    private byte[] textureCoverBytes;
    private ResourceLocation coverTextureLocation;
    private final Thread shutdownHook = new Thread(new Runnable() {
        @Override
        public void run() {
            readerRunning = false;
            Process process = readerProcess;
            if (process != null) {
                process.destroy();
            }
        }
    }, "YesClient-MediaOverlay-Shutdown");

    public MediaOverlayModule() {
        super("Media Overlay", "Shows the current song and playback progress", Category.VISUALS, ModuleMode.BOTH);
        addSetting(overlayWidth);
        addSetting(overlayHeight);
        addSetting(backgroundColor);
        addSetting(opacity);
        addSetting(titleColor);
        addSetting(artistColor);
        addSetting(progressColor);
        setWidth(overlayWidth.getIntValue());
        setHeight(overlayHeight.getIntValue());
        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
             
        }
    }

    @Override
    protected void onEnable() {
        setOverlaySize(overlayWidth.getIntValue(), overlayHeight.getIntValue());
        startMediaReader();
    }

    @Override
    protected void onDisable() {
        readerRunning = false;
        readerGeneration++;
        Process process = readerProcess;
        readerProcess = null;
        if (process != null) {
            process.destroy();
        }
        Thread thread = readerThread;
        readerThread = null;
        if (thread != null) {
            thread.interrupt();
        }
        snapshot = MediaSnapshot.EMPTY;
        missingMediaSince = 0L;
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

         
         
         
        if (mc.currentScreen instanceof HudEditorGUI) {
            return;
        }

        MediaSnapshot current = snapshot;
        if (!current.active) {
            return;
        }

         
         
         
        setOverlaySize(overlayWidth.getIntValue(), overlayHeight.getIntValue());
        int cardWidth = getOverlayWidth();
        int cardHeight = getOverlayHeight();

        ScaledResolution resolution = new ScaledResolution(mc);
        int x = 8;
        int y = 8;
        HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        if (designer != null) {
            x = designer.getMediaOverlayX();
            y = designer.getMediaOverlayY(resolution.getScaledHeight());
        }

        x = Math.max(0, Math.min(resolution.getScaledWidth() - hudScale.scaled(cardWidth), x));
        y = Math.max(0, Math.min(resolution.getScaledHeight() - hudScale.scaled(cardHeight), y));

        int padding = 8;
        updateCoverTexture(current, Math.max(1, cardHeight - padding * 2));
        hudScale.beginAt(x, y);
        drawCard(x, y, cardWidth, cardHeight, current);
        hudScale.end();
    }

    public int getOverlayWidth() {
        return overlayWidth.getIntValue();
    }
    public int getOverlayHeight() {
        return overlayHeight.getIntValue();
    }

    public void setOverlaySize(int width, int height) {
        overlayWidth.setValue((double) Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width)));
        overlayHeight.setValue((double) Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, height)));
        setWidth(overlayWidth.getIntValue());
        setHeight(overlayHeight.getIntValue());
    }

    private void drawCard(int x, int y, int width, int height, MediaSnapshot media) {
        int cardBackground = applyOpacity(backgroundColor.getRGB(), opacity.getIntValue());
        RenderUtils.drawRoundedRect(x, y, width, height, 10, cardBackground);
        RenderUtils.drawRoundedRectOutline(x, y, width, height, 10,
                applyOpacity(0x3AFFFFFF, opacity.getIntValue()), 1.0F);

        int padding = 8;
        int artSize = height - padding * 2;
        int artX = x + padding;
        int artY = y + padding;
        RenderUtils.drawRoundedRect(artX, artY, artSize, artSize, 6,
                applyOpacity(0xFF252A32, opacity.getIntValue()));

        ResourceLocation texture = coverTextureLocation;
        if (texture != null) {
            drawTexture(texture, artX, artY, artSize, artSize);
        } else {
            String missingCover = "?";
            int markX = artX + (artSize - mc.fontRendererObj.getStringWidth(missingCover)) / 2;
            int markY = artY + (artSize - mc.fontRendererObj.FONT_HEIGHT) / 2;
            mc.fontRendererObj.drawStringWithShadow(missingCover, markX, markY, 0xFFB6BBC4);
        }
        RenderUtils.drawRoundedRectOutline(artX, artY, artSize, artSize, 6, 0x35FFFFFF, 1.0F);

        int textX = artX + artSize + 11;
        int textWidth = Math.max(8, x + width - padding - textX);
        String title = media.active ? media.title : "Nothing playing";
        String artist = media.active ? media.artist : "Windows media session";
        if (title == null || title.trim().isEmpty()) title = "Unknown title";
        if (artist == null || artist.trim().isEmpty()) artist = "Unknown artist";

        String visibleTitle = MarqueeText.fit(title, mc.fontRendererObj, textWidth, titleMarquee, "\u00A7l");
        String visibleArtist = MarqueeText.fit(artist, mc.fontRendererObj, textWidth, artistMarquee);
        mc.fontRendererObj.drawStringWithShadow("\u00A7l" + visibleTitle, textX, y + 10, titleColor.getRGB());
        mc.fontRendererObj.drawStringWithShadow(visibleArtist, textX, y + 28, artistColor.getRGB());

        double position = media.positionSeconds;
        if (media.playing) {
            position += Math.max(0L, System.currentTimeMillis() - media.receivedAt) / 1000.0D;
        }
        double progress = media.transitioning ? 1.0D : (media.durationSeconds > 0.0D
                ? Math.max(0.0D, Math.min(1.0D, position / media.durationSeconds))
                : 0.0D);
        String remaining = media.transitioning
                ? "0:00"
                : media.durationSeconds > 0.0D
                ? formatDuration(Math.max(0.0D, media.durationSeconds - position))
                : "--:--";
        int timeWidth = mc.fontRendererObj.getStringWidth(remaining);
        int progressX = textX;
        int progressY = y + height - 14;
        int progressWidth = Math.max(16, x + width - padding - timeWidth - 9 - progressX);
        int progressArgb = progressColor.getRGB();
        RenderUtils.drawPill(progressX, progressY, progressWidth, 2.0D, applyOpacity(progressArgb, 35));
        double filledWidth = progressWidth * progress;
        if (filledWidth > 0.0D) {
            RenderUtils.drawPill(progressX, progressY, filledWidth, 2.0D, progressArgb);
        }
        RenderUtils.drawCircle(progressX + filledWidth, progressY + 1.0D, 3.0D, progressArgb);

        mc.fontRendererObj.drawStringWithShadow(remaining,
                x + width - padding - mc.fontRendererObj.getStringWidth(remaining), y + height - 19, 0xFFB6BBC4);

         
         
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private int applyOpacity(int argb, int opacityPercent) {
        int sourceAlpha = (argb >>> 24) & 0xFF;
        int alpha = Math.max(1, sourceAlpha * Math.max(0, Math.min(100, opacityPercent)) / 100);
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    private void drawTexture(ResourceLocation texture, int x, int y, int width, int height) {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE,
                GL11.GL_ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(texture);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        renderer.pos(x, y + height, 0.0D).tex(0.0D, 1.0D).endVertex();
        renderer.pos(x + width, y + height, 0.0D).tex(1.0D, 1.0D).endVertex();
        renderer.pos(x + width, y, 0.0D).tex(1.0D, 0.0D).endVertex();
        renderer.pos(x, y, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
    }

    private void updateCoverTexture(MediaSnapshot media, int artSize) {
        if (media.trackKey.equals(textureTrackKey) && artSize == textureArtSize
                && media.coverBytes == textureCoverBytes) {
            return;
        }
        textureTrackKey = media.trackKey;
        textureArtSize = artSize;
        textureCoverBytes = media.coverBytes;
        if (coverTextureLocation != null) {
            mc.getTextureManager().deleteTexture(coverTextureLocation);
        }
        coverTextureLocation = null;
        if (media.coverBytes == null || media.coverBytes.length == 0) {
            return;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(media.coverBytes));
            if (image != null) {
                BufferedImage roundedImage = createRoundedCover(image, artSize);
                DynamicTexture dynamicTexture = new DynamicTexture(roundedImage);
                coverTextureLocation = mc.getTextureManager()
                        .getDynamicTextureLocation("media_overlay_cover", dynamicTexture);
            }
        } catch (Exception ignored) {
            coverTextureLocation = null;
        }
    }

    private BufferedImage createRoundedCover(BufferedImage source, int size) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage rounded = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = rounded.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setComposite(AlphaComposite.Src);
            float arcWidth = 12.0F * width / Math.max(1.0F, size);
            float arcHeight = 12.0F * height / Math.max(1.0F, size);
            graphics.setClip(new RoundRectangle2D.Float(0.0F, 0.0F, width, height, arcWidth, arcHeight));
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rounded;
    }

    private void startMediaReader() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win") || readerRunning) {
            return;
        }

        readerRunning = true;
        final int generation = ++readerGeneration;
        readerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (readerRunning && readerGeneration == generation) {
                    Process process = null;
                    try {
                        process = launchPowerShellReader();
                        readerProcess = process;
                        BufferedReader output = new BufferedReader(
                                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                        String line;
                        while (readerRunning && readerGeneration == generation && (line = output.readLine()) != null) {
                            acceptSnapshot(line);
                        }
                    } catch (Exception ignored) {
                         
                    } finally {
                        if (process != null) {
                            process.destroy();
                        }
                        if (readerProcess == process) {
                            readerProcess = null;
                        }
                    }

                    if (readerRunning && readerGeneration == generation) {
                        try {
                            Thread.sleep(2500L);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
        }, "YesClient-MediaOverlay");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private Process launchPowerShellReader() throws Exception {
        InputStream stream = getClass().getResourceAsStream("/assets/yesclient/media-session.ps1");
        if (stream == null) {
            throw new IllegalStateException("Media session helper resource is missing");
        }

        byte[] scriptBytes;
        try {
            scriptBytes = readAll(stream);
        } finally {
            stream.close();
        }

        String script = new String(scriptBytes, StandardCharsets.UTF_8);
        final String encoded;
        try {
            encoded = Base64.getEncoder().encodeToString(script.getBytes("UTF-16LE"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }

        ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive",
                "-WindowStyle", "Hidden", "-EncodedCommand", encoded);
        builder.redirectErrorStream(true);
        return builder.start();
    }

    private byte[] readAll(InputStream input) throws Exception {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private void acceptSnapshot(String line) {
        try {
            JsonObject json = new JsonParser().parse(line).getAsJsonObject();
            boolean active = json.has("active") && json.get("active").getAsBoolean();
            String title = jsonString(json, "title");
            String artist = jsonString(json, "artist");
            String trackKey = jsonString(json, "trackKey");
            boolean playing = json.has("playing") && json.get("playing").getAsBoolean();
            double position = json.has("position") ? json.get("position").getAsDouble() : 0.0D;
            double duration = json.has("duration") ? json.get("duration").getAsDouble() : 0.0D;
            long receivedAt = System.currentTimeMillis();
            long timelineUpdatedAt = json.has("updatedAt") ? json.get("updatedAt").getAsLong() : 0L;
            String encodedCover = jsonString(json, "cover");

            MediaSnapshot previous = snapshot;
            if (!active || title.trim().isEmpty()) {
                if (previous.active && !previous.trackKey.isEmpty()) {
                    if (missingMediaSince == 0L) {
                        missingMediaSince = receivedAt;
                    }
                    if (receivedAt - missingMediaSince <= 3000L) {
                        snapshot = new MediaSnapshot(true, previous.title, previous.artist, previous.trackKey,
                                previous.coverBytes, false, previous.durationSeconds, previous.durationSeconds,
                                receivedAt, previous.timelineUpdatedAt, true);
                        return;
                    }
                }
                snapshot = MediaSnapshot.EMPTY;
                return;
            }
            missingMediaSince = 0L;

            byte[] coverBytes = null;
            if (!encodedCover.isEmpty()) {
                byte[] decoded = Base64.getDecoder().decode(encodedCover);
                if (decoded.length <= MAX_COVER_BYTES) {
                    coverBytes = decoded;
                }
            } else if (active && trackKey.equals(snapshot.trackKey)) {
                coverBytes = snapshot.coverBytes;
            }

            position = Math.max(0.0D, position);
            duration = Math.max(0.0D, duration);
            if (playing) {
                if (timelineUpdatedAt > 0L) {
                    position += Math.max(0L, receivedAt - timelineUpdatedAt) / 1000.0D;
                }
                if (previous.active && previous.playing && trackKey.equals(previous.trackKey)) {
                    double previousPosition = previous.positionSeconds
                            + Math.max(0L, receivedAt - previous.receivedAt) / 1000.0D;
                    boolean movedBack = timelineUpdatedAt > previous.timelineUpdatedAt
                            && position + 1.5D < previousPosition;
                    if (!movedBack) {
                        position = Math.max(position, previousPosition);
                    }
                }
            }
            snapshot = new MediaSnapshot(true, title, artist, trackKey, coverBytes, playing,
                    position, duration, receivedAt, timelineUpdatedAt, false);
        } catch (Exception ignored) {
             
        }
    }

    private String jsonString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }

    private String formatDuration(double seconds) {
        long total = Math.max(0L, (long) seconds);
        long minutes = total / 60L;
        long remainder = total % 60L;
        return minutes + ":" + (remainder < 10L ? "0" : "") + remainder;
    }

    private static final class MediaSnapshot {
        private static final MediaSnapshot EMPTY = new MediaSnapshot(false, "", "", "", null, false, 0.0D,
                0.0D, 0L, 0L, false);

        private final boolean active;
        private final String title;
        private final String artist;
        private final String trackKey;
        private final byte[] coverBytes;
        private final boolean playing;
        private final double positionSeconds;
        private final double durationSeconds;
        private final long receivedAt;
        private final long timelineUpdatedAt;
        private final boolean transitioning;

        private MediaSnapshot(boolean active, String title, String artist, String trackKey, byte[] coverBytes,
                boolean playing, double positionSeconds, double durationSeconds, long receivedAt,
                long timelineUpdatedAt, boolean transitioning) {
            this.active = active;
            this.title = title;
            this.artist = artist;
            this.trackKey = trackKey;
            this.coverBytes = coverBytes;
            this.playing = playing;
            this.positionSeconds = positionSeconds;
            this.durationSeconds = durationSeconds;
            this.receivedAt = receivedAt;
            this.timelineUpdatedAt = timelineUpdatedAt;
            this.transitioning = transitioning;
        }
    }
}

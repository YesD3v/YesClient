package com.yesclient.module.modules.render;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.HudScale;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ColorSetting;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.FriendsModule;
import com.yesclient.module.modules.client.HudDesignerModule;
import com.yesclient.utils.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

 




public class DirectionModule extends Module {

    private final HudScale hudScale = new HudScale(this);
    private static final int WIDTH = 160;
    private static final int HEIGHT = 24;  
    private final BooleanSetting showBackground = new BooleanSetting("Background", "Show the compass background", true);
    private final ColorSetting textColor = new ColorSetting("Text Color", "Compass direction color", new java.awt.Color(255, 255, 255, 255));
    private final ColorSetting tickColor = new ColorSetting("Tick Color", "Compass tick color", new java.awt.Color(170, 170, 170, 255));
    private final ColorSetting backgroundColor = new ColorSetting("Background Color", "Compass background color", new java.awt.Color(0, 0, 0, 102));
    private final ColorSetting teammateColor = new ColorSetting("Teammate Color", "Color around teammate heads", new java.awt.Color(0, 255, 0, 255));

    public DirectionModule() {
        super("Direction", "Compass HUD with teammate tracking", Category.RENDER, ModuleMode.BOTH);
        addSetting(showBackground);
        addSetting(textColor);
        addSetting(tickColor);
        addSetting(backgroundColor);
        addSetting(teammateColor);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

         
        HudDesignerModule designer = YesClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getDirX() : 5;
        int y = designer != null ? designer.getDirY() : 25;
        hudScale.beginAt(x, y);

         
         
         
         
         
         
        int centerX = x + WIDTH / 2;

         
        if (showBackground.isEnabled()) {
            RenderUtils.drawRoundedRect(x, y, WIDTH, HEIGHT, 4, backgroundColor.getRGB());
        }

         
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.scissor(x, y, hudScale.scaled(WIDTH), hudScale.scaled(HEIGHT));

        float yaw = mc.thePlayer.rotationYaw;
        float wrapYaw = MathHelper.wrapAngleTo180_float(yaw);  

         
        for (int i = 0; i < 360; i += 15) {
            boolean isCardinal = (i % 90 == 0);
            boolean isOrdinal = (i % 45 == 0) && !isCardinal;
            drawTick(x, y, wrapYaw, i - 180, isCardinal, isOrdinal);
        }

         
        drawDirection(x, y, wrapYaw, 0, "S");
        drawDirection(x, y, wrapYaw, -90, "E");
        drawDirection(x, y, wrapYaw, 90, "W");
        drawDirection(x, y, wrapYaw, 180, "N");
        drawDirection(x, y, wrapYaw, -180, "N");

         
        drawOrdinal(x, y, wrapYaw, -45, "SE");
        drawOrdinal(x, y, wrapYaw, 45, "SW");
        drawOrdinal(x, y, wrapYaw, 135, "NW");
        drawOrdinal(x, y, wrapYaw, -135, "NE");

         
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer)
                continue;

            String name = player.getName();
            if (FriendsModule.isFriend(name)) {
                drawTeammate(x, y, wrapYaw, player);
            }
        }

         
        RenderUtils.drawRect(x + WIDTH / 2, y + 15, 1, 8, textColor.getRGB());

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

         
        RenderUtils.drawRoundedRectOutline(x, y, WIDTH, HEIGHT, 4,
                (textColor.getRGB() & 0x00FFFFFF) | 0x40000000, 1.0f);
        setWidth(WIDTH);
        setHeight(HEIGHT);
        hudScale.end();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawDirection(int x, int y, float playerYaw, float markerYaw, String text) {
        float angle = markerYaw - playerYaw;

         
        while (angle < -180)
            angle += 360;
        while (angle >= 180)
            angle -= 360;

         
         
         
        float pxPerDeg = (float) WIDTH / 100.0f;

        float offset = angle * pxPerDeg;

        float textX = x + WIDTH / 2 + offset;

         
        if (textX > x - 10 && textX < x + WIDTH + 10) {
             
            int alpha = 255;
            float dist = Math.abs(offset);
            if (dist > (WIDTH / 2 - 20)) {
                alpha = (int) Math.max(0, 255 * (1.0f - (dist - (WIDTH / 2 - 20)) / 20.0f));
            }
            int color = (alpha << 24) | (textColor.getRGB() & 0x00FFFFFF);

             
            int strWidth = mc.fontRendererObj.getStringWidth(text);
             
             
            mc.fontRendererObj.drawStringWithShadow(text, textX - strWidth / 2.0f, y + 2, color);
        }
    }

    private void drawTick(int x, int y, float playerYaw, float markerYaw, boolean isCardinal, boolean isOrdinal) {
        float angle = markerYaw - playerYaw;
        while (angle < -180)
            angle += 360;
        while (angle >= 180)
            angle -= 360;

        float pxPerDeg = (float) WIDTH / 100.0f;
        float offset = angle * pxPerDeg;
        float tickX = x + WIDTH / 2 + offset;

        if (tickX > x - 2 && tickX < x + WIDTH + 2) {
             
            int alpha = 255;
            float dist = Math.abs(offset);
            if (dist > (WIDTH / 2 - 20)) {
                alpha = (int) Math.max(0, 255 * (1.0f - (dist - (WIDTH / 2 - 20)) / 20.0f));
            }
            int color = (alpha << 24) | (tickColor.getRGB() & 0x00FFFFFF);

             
             
            int h = isCardinal ? 7 : (isOrdinal ? 5 : 3);
            int yPos = y + HEIGHT - h - 1;
            RenderUtils.drawRect((int) tickX, yPos, 1, h, color);
        }
    }

     


    private void drawOrdinal(int x, int y, float playerYaw, float markerYaw, String text) {
        float angle = markerYaw - playerYaw;
        while (angle < -180)
            angle += 360;
        while (angle >= 180)
            angle -= 360;

        float pxPerDeg = (float) WIDTH / 100.0f;
        float offset = angle * pxPerDeg;
        float textX = x + WIDTH / 2 + offset;

        if (textX > x - 10 && textX < x + WIDTH + 10) {
            int alpha = 255;
            float dist = Math.abs(offset);
            if (dist > (WIDTH / 2 - 20)) {
                alpha = (int) Math.max(0, 255 * (1.0f - (dist - (WIDTH / 2 - 20)) / 20.0f));
            }
            int color = (alpha << 24) | (textColor.getRGB() & 0x00FFFFFF);

             
             
            int strWidth = mc.fontRendererObj.getStringWidth(text);
            mc.fontRendererObj.drawStringWithShadow(text, textX - strWidth / 2.0f, y + 3, color);
        }
    }

    private void drawTeammate(int x, int y, float playerYaw, EntityPlayer teammate) {
        double dX = teammate.posX - mc.thePlayer.posX;
        double dZ = teammate.posZ - mc.thePlayer.posZ;

         
         
         
         
         

        double degrees = Math.toDegrees(Math.atan2(dZ, dX)) - 90;  
        float teammateYaw = (float) degrees;

        float angle = teammateYaw - playerYaw;
        while (angle < -180)
            angle += 360;
        while (angle >= 180)
            angle -= 360;

        float pxPerDeg = (float) WIDTH / 100.0f;
        float offset = angle * pxPerDeg;

        float headX = x + WIDTH / 2 + offset;
        float headSize = 12;

        if (headX > x - headSize && headX < x + WIDTH + headSize) {
             
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(teammate.getUniqueID());
            ResourceLocation skin = (info != null) ? info.getLocationSkin()
                    : new ResourceLocation("textures/entity/steve.png");  

            mc.getTextureManager().bindTexture(skin);
            GlStateManager.color(1, 1, 1, 1);

             
             
            Gui.drawScaledCustomSizeModalRect((int) (headX - headSize / 2), y + 3, 8, 8, 8, 8, (int) headSize,
                    (int) headSize, 64, 64);
             
            Gui.drawScaledCustomSizeModalRect((int) (headX - headSize / 2), y + 3, 40, 8, 8, 8, (int) headSize,
                    (int) headSize, 64, 64);

             
            RenderUtils.drawRoundedRectOutline((int) (headX - headSize / 2), y + 3, (int) headSize, (int) headSize, 0,
                    teammateColor.getRGB(), 1.0f);
        }
    }

    @Override
    public String getDisplayInfo() {
        return null;  
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }
}

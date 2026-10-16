package com.yesclient.module.modules.visuals;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.modules.client.FriendsModule;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.utils.MinecraftFormatting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NametagsModule extends Module {

    private static final Pattern DISTANCE_SUFFIX = Pattern.compile(" \\u00A77\\[\\d+(?:[.,]\\d+)?m\\]$");
    private static final Pattern HEALTH_SUFFIX = Pattern.compile(" \\u00A7[ace6]\\d+(?:[.,]\\d+)?\\u00A7c\\u2764$");
    private static final Pattern NICK_SUFFIX = Pattern.compile("\\u00A7c~$");

    private final BooleanSetting showHealth = new BooleanSetting("Show Health", "Display player health", true);
    private final BooleanSetting showDistance = new BooleanSetting("Show Distance", "Display distance to player", true);
    private final BooleanSetting friendHighlight = new BooleanSetting("Friend Highlight", "Highlight friends in green", true);
    private final BooleanSetting invisibles = new BooleanSetting("Invisibles", "Show invisible player nametags", true);
    private final BooleanSetting self = new BooleanSetting("Self", "Show your custom nametag in second and third person", false);
    private final BooleanSetting normalizeNames = new BooleanSetting("Normalize Names", "Convert fancy/unicode characters in names to normal letters", false);

    public NametagsModule() {
        super("Nametags", "Enhanced native player nametags", Category.VISUALS, ModuleMode.BOTH);

        addSetting(showHealth);
        addSetting(showDistance);
        addSetting(friendHighlight);
        addSetting(invisibles);
        addSetting(self);
        addSetting(normalizeNames);
    }

    private String getFormattedName(EntityPlayer player, String baseName) {
        String name = removeClientSuffixes(baseName == null ? player.getName() : baseName);
        if (name.isEmpty()) name = player.getName();
        
        if (normalizeNames.isEnabled()) {
            name = FancyTextModule.revertToNormal(name);
        }

        boolean isFriend = FriendsModule.isFriend(player.getName());
        if (isFriend && friendHighlight.isEnabled()) {
            name = "\u00A7a" + net.minecraft.util.StringUtils.stripControlCodes(name);
        }

        try {
            com.yesclient.module.modules.moderation.NickDetectorModule nickMod = (com.yesclient.module.modules.moderation.NickDetectorModule) YesClient.getInstance().getModuleManager().getModule(com.yesclient.module.modules.moderation.NickDetectorModule.class);
            if (nickMod != null && nickMod.isEnabled() && nickMod.isNicked(player.getName())) {
                name = name + "\u00A7c~";
            }
        } catch (Exception ignored) {}

        if (showHealth.isEnabled()) {
            float health = player.getHealth();
            String healthColor = "\u00A7c";
            if (health > 15) healthColor = "\u00A7a";
            else if (health > 10) healthColor = "\u00A7e";
            else if (health > 5) healthColor = "\u00A76";
            
            name = name + " " + healthColor + String.format(Locale.ROOT, "%.1f", health) + "\u00A7c\u2764";
        }

        if (showDistance.isEnabled() && mc.thePlayer != null && player != mc.thePlayer) {
            double dist = mc.thePlayer.getDistanceToEntity(player);
            name = name + " \u00A77[" + String.format(Locale.ROOT, "%.1f", dist) + "m]";
        }
        
        return name;
    }

     




    private String removeClientSuffixes(String name) {
        if (name == null) return "";

        String cleaned = name;
        boolean removed;
        do {
            int oldLength = cleaned.length();
            cleaned = stripTrailingMatch(DISTANCE_SUFFIX, cleaned);
            cleaned = stripTrailingMatch(HEALTH_SUFFIX, cleaned);
            cleaned = stripTrailingMatch(NICK_SUFFIX, cleaned);
            removed = cleaned.length() != oldLength;
        } while (removed);
        return cleaned;
    }

    private String stripTrailingMatch(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? value.substring(0, matcher.start()) : value;
    }

     
    public boolean suppressVanillaHealthLabel() {
        return isEnabled();
    }

    @SubscribeEvent
    public void onNameFormat(PlayerEvent.NameFormat event) {
        if (!isEnabled()) return;
        EntityPlayer player = event.entityPlayer;
        if (player == null) return;
        
        event.displayname = getFormattedName(player, event.displayname);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || mc.thePlayer == null) return;
        
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            boolean isSelf = player == mc.thePlayer;
            if (isSelf && (!self.isEnabled() || mc.gameSettings.thirdPersonView == 0)) continue;
             
             
            if (!isSelf && (!player.isInvisible() || !invisibles.isEnabled())) continue;
            
            String text = getFormattedName(player, player.getDisplayName().getFormattedText());
            renderNametag(player, text, event.partialTicks);
        }
    }

    private void renderNametag(EntityPlayer player, String text, float partialTicks) {
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;
        
        float f = 1.6F;
        float f1 = 0.016666668F * f;
        boolean isThirdPersonFront = mc.gameSettings.thirdPersonView == 2;
        
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate((float)x, (float)y + player.height + 0.5F, (float)z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);

            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(mc.getRenderManager().playerViewX * (isThirdPersonFront ? -1 : 1), 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(-f1, -f1, f1);

            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            int j = mc.fontRendererObj.getStringWidth(text) / 2;
            GlStateManager.disableTexture2D();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldrenderer.pos((double)(-j - 1), (double)(-1), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos((double)(-j - 1), (double)(8), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos((double)(j + 1), (double)(8), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos((double)(j + 1), (double)(-1), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            tessellator.draw();

            GlStateManager.enableTexture2D();
            mc.fontRendererObj.drawString(text, -mc.fontRendererObj.getStringWidth(text) / 2, 0, 553648127);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            mc.fontRendererObj.drawString(text, -mc.fontRendererObj.getStringWidth(text) / 2, 0, -1);
        } finally {
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }
    
    @Override
    public void onRender3D(float partialTicks) {
    }
}

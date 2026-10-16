package com.yesclient.gui;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import com.yesclient.gui.theme.Theme;
import com.yesclient.altmanager.CookieAltManager;
import com.yesclient.altmanager.CookieAltManager.AltAccount;
import com.yesclient.gui.alt.GuiAltManager;
import com.yesclient.module.Module;
import com.yesclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public class ToggleNotification {

    private static final List<Notification> notifications = new ArrayList<>();
    private static final int MAX_NOTIFICATIONS = 3;
    private static final int MARGIN = 12;
    private static final long DISPLAY_TIME = 2000;
    private static final long FADE_TIME = 300;

    private static String lastModuleName = "";
    private static long lastNotificationTime = 0;
    private static final long DEBOUNCE_TIME = 100;
    private static final int ACCOUNT_NOTICE_WIDTH = 320;
    private static final int ACCOUNT_NOTICE_HEIGHT = 66;
    private static final long ACCOUNT_NOTICE_SLIDE_MS = 240L;
    private static final long ACCOUNT_NOTICE_TERMINAL_MS = 6500L;
    private static volatile AccountNotice accountNotice;

    private enum AccountNoticeState {
        EXPIRED, LOGGING_IN, ERROR, CONNECTED, CANCELLED
    }

    public static void addNotification(Module module, boolean enabled) {
        long now = System.currentTimeMillis();
        String displayName = module.getDisplayName();
        if (displayName.equals(lastModuleName) && (now - lastNotificationTime) < DEBOUNCE_TIME) {
            return;
        }
        lastModuleName = displayName;
        lastNotificationTime = now;

        while (notifications.size() >= MAX_NOTIFICATIONS) {
            notifications.remove(0);
        }

        notifications.add(new Notification(displayName, enabled));
    }

    public static void addCustomNotification(String title, String message, ResourceLocation icon, boolean isError) {
        while (notifications.size() >= MAX_NOTIFICATIONS) {
            notifications.remove(0);
        }
        notifications.add(new Notification(title, message, icon, isError));
    }

    public static void showAccountExpired(AltAccount account) {
        if (account != null) {
            accountNotice = new AccountNotice(account);
        }
    }

     
    public static boolean handleAccountNoticeClick(int mouseX, int mouseY) {
        AccountNotice notice = accountNotice;
        if (notice == null) return false;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        AccountNoticeBounds bounds = getAccountNoticeBounds(sr.getScaledWidth(), notice);
        if (!inside(mouseX, mouseY, bounds.x, bounds.y, bounds.width, bounds.height)) return false;

        if (notice.state != AccountNoticeState.LOGGING_IN
                && inside(mouseX, mouseY, bounds.x + bounds.width - 17, bounds.y + 3, 14, 14)) {
            accountNotice = null;
            return true;
        }

        if (inside(mouseX, mouseY, bounds.buttonX, bounds.buttonY, bounds.buttonWidth, bounds.buttonHeight)) {
            if (notice.state == AccountNoticeState.LOGGING_IN) {
                CookieAltManager.getInstance().cancelLogin();
                notice.state = AccountNoticeState.CANCELLED;
                notice.detail = "Microsoft sign-in cancelled";
                notice.terminalAt = System.currentTimeMillis();
            } else if (notice.state == AccountNoticeState.CONNECTED) {
                accountNotice = null;
            } else {
                notice.detail = "Starting Microsoft sign-in...";
                notice.state = AccountNoticeState.LOGGING_IN;
                notice.terminalAt = 0L;
                if (!CookieAltManager.getInstance().startAccountRelogin(notice.account)) {
                    notice.state = AccountNoticeState.ERROR;
                    notice.detail = "Another sign-in is already in progress";
                    notice.terminalAt = System.currentTimeMillis();
                }
            }
        }
        return true;
    }

    public static void render() {
        render(false);
    }

     
    public static void renderFromAccountManager() {
        render(true);
    }

    private static void render(boolean fromAccountManager) {
        if (!fromAccountManager && Minecraft.getMinecraft().currentScreen instanceof GuiAltManager) return;
        if (notifications.isEmpty() && accountNotice == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null && mc.currentScreen == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRendererObj;

        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        renderAccountNotice(fr, screenWidth, screenHeight);

        Iterator<Notification> iterator = notifications.iterator();
        int index = 0;
        int currentYOffset = 0;

        while (iterator.hasNext()) {
            Notification notif = iterator.next();
            notif.update();

            if (notif.isExpired()) {
                iterator.remove();
                continue;
            }

            int spacing = 8;
            int h = notif.isCustom ? 36 : 22;
            int bottomOffset = MARGIN;
            
            int targetY = screenHeight - bottomOffset - currentYOffset - h;

            float slideProgress = notif.getSlideProgress();
            int slideOffset = (int) ((notif.width + MARGIN + 20) * (1 - slideProgress));
            int x = screenWidth - MARGIN - notif.width + slideOffset;

            float alpha = notif.getAlpha();
            renderNotification(fr, x, targetY, notif, alpha);
            
            currentYOffset += h + spacing;
            index++;
        }
    }

    private static void renderAccountNotice(FontRenderer fr, int screenWidth, int screenHeight) {
        AccountNotice notice = accountNotice;
        if (notice == null) return;
        refreshAccountNotice(notice);
        if (accountNotice != notice) return;

        long now = System.currentTimeMillis();
        if (notice.terminalAt > 0L && now - notice.terminalAt >= ACCOUNT_NOTICE_TERMINAL_MS) {
            accountNotice = null;
            return;
        }

        AccountNoticeBounds bounds = getAccountNoticeBounds(screenWidth, notice);
        float alpha = getAccountNoticeEnterProgress(notice);
        if (notice.terminalAt > 0L && now - notice.terminalAt > ACCOUNT_NOTICE_TERMINAL_MS - 350L) {
            alpha *= Math.max(0.0F, (ACCOUNT_NOTICE_TERMINAL_MS - (now - notice.terminalAt)) / 350.0F);
        }
         
         
        int alphaByte = Math.max(1, Math.min(255, Math.round(alpha * 255.0F)));
        int background = (alphaByte << 24) | 0x15171A;
        int border = (alphaByte << 24) | 0x373B42;
        float pulse = notice.state == AccountNoticeState.LOGGING_IN
                ? 0.78F + 0.22F * (0.5F + 0.5F * (float) Math.sin(now / 180.0D)) : 1.0F;
        int accentAlpha = Math.round(alphaByte * pulse);
        int accent = notice.state == AccountNoticeState.EXPIRED || notice.state == AccountNoticeState.ERROR
                ? (accentAlpha << 24) | 0xE74C3C : (accentAlpha << 24) | 0x2ECC71;

        RenderUtils.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, background);
         
        RenderUtils.drawRoundedRect(bounds.x + 1, bounds.y + 1, 4, bounds.height - 2, 3, accent,
                true, false, false, true);
        RenderUtils.drawRoundedRectOutline(bounds.x, bounds.y, bounds.width, bounds.height, 8, border, 1.0F);

        int headX = bounds.x + 11;
        int headY = bounds.y + 17;
        ResourceLocation head = GuiAltManager.getCachedHead(notice.account);
        if (head == null) {
            GuiAltManager.loadHeadTexture(Minecraft.getMinecraft(), notice.account);
            RenderUtils.drawRoundedRect(headX, headY, 32, 32, 6, 0xFF30343A);
            String initial = notice.account.username == null || notice.account.username.isEmpty()
                    ? "?" : notice.account.username.substring(0, 1).toUpperCase();
            fr.drawString(initial, headX + (32 - fr.getStringWidth(initial)) / 2, headY + 12, 0xFFD7D9DE);
        } else {
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            Minecraft.getMinecraft().getTextureManager().bindTexture(head);
            net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(headX, headY, 0, 0, 32, 32, 32, 32);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        String title = getAccountNoticeTitle(notice);
        String detail = getAccountNoticeDetail(notice);
        fr.drawStringWithShadow("\u00A7l" + title, bounds.x + 51, bounds.y + 11, Theme.withAlpha(0xF0F1F3, alphaByte));
        int detailWidth = Math.max(20, bounds.buttonX - (bounds.x + 51) - 8);
        String fitDetail = fr.trimStringToWidth(detail, detailWidth);
        fr.drawStringWithShadow(fitDetail, bounds.x + 51, bounds.y + 25, Theme.withAlpha(0xB6BAC2, alphaByte));

        int buttonColor;
        int buttonTextColor = 0xFFFFFFFF;
        switch (notice.state) {
            case LOGGING_IN:
                buttonColor = (alphaByte << 24) | 0xC0392B;
                break;
            case CONNECTED:
                buttonColor = (alphaByte << 24) | 0x26734D;
                break;
            default:
                buttonColor = (alphaByte << 24) | 0x278B54;
                break;
        }
        RenderUtils.drawRoundedRect(bounds.buttonX, bounds.buttonY, bounds.buttonWidth, bounds.buttonHeight, 5,
                buttonColor);
        String buttonText = getAccountNoticeButton(notice);
        fr.drawString(buttonText, bounds.buttonX + (bounds.buttonWidth - fr.getStringWidth(buttonText)) / 2,
                bounds.buttonY + 6, buttonTextColor);

        if (notice.state != AccountNoticeState.LOGGING_IN) {
            fr.drawString("x", bounds.x + bounds.width - 14, bounds.y + 5, Theme.withAlpha(0x8A8F98, alphaByte));
        } else {
            drawAccountNoticeSpinner(bounds.x + bounds.width - 17, bounds.y + 16, alpha, now);
        }
    }

    private static float getAccountNoticeEnterProgress(AccountNotice notice) {
        long elapsed = Math.max(0L, System.currentTimeMillis() - notice.createdAt);
        float progress = Math.min(1.0F, elapsed / (float) ACCOUNT_NOTICE_SLIDE_MS);
        return 1.0F - (float) Math.pow(1.0F - progress, 3.0D);
    }

    private static void drawAccountNoticeSpinner(int centerX, int centerY, float alpha, long now) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(0.86F, 0.90F, 0.96F, alpha);
        GL11.glLineWidth(1.5F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        double rotation = (now % 720L) * Math.PI * 2.0D / 720.0D;
        for (int i = 0; i <= 24; i++) {
            double angle = rotation + Math.PI * (0.15D + 1.5D * i / 24.0D);
            GL11.glVertex2d(centerX + Math.cos(angle) * 5.0D, centerY + Math.sin(angle) * 5.0D);
        }
        GL11.glEnd();
        GL11.glLineWidth(1.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void refreshAccountNotice(AccountNotice notice) {
        if (notice.state != AccountNoticeState.LOGGING_IN) return;
        CookieAltManager manager = CookieAltManager.getInstance();
        CookieAltManager.Status status = manager.getStatus();
        if (status == CookieAltManager.Status.SUCCESS) {
            notice.state = AccountNoticeState.CONNECTED;
            notice.detail = manager.getCurrentUsername() + " is connected";
            notice.terminalAt = System.currentTimeMillis();
        } else if (status == CookieAltManager.Status.FAILED) {
            notice.state = AccountNoticeState.ERROR;
            notice.detail = manager.getLastError().isEmpty() ? "Microsoft sign-in failed" : manager.getLastError();
            notice.terminalAt = System.currentTimeMillis();
        } else if (status == CookieAltManager.Status.IDLE && !manager.isMicrosoftLoginInProgress()) {
            notice.state = AccountNoticeState.CANCELLED;
            notice.detail = "Microsoft sign-in cancelled";
            notice.terminalAt = System.currentTimeMillis();
        } else if (status == CookieAltManager.Status.MICROSOFT_LOGIN) {
            String code = manager.getMicrosoftUserCode();
            notice.detail = code == null || code.isEmpty()
                    ? "Opening Microsoft sign-in..." : "Microsoft code: " + code;
        } else if (status != CookieAltManager.Status.IDLE) {
            notice.detail = stripFormatting(status.getMessage());
        }
    }

    private static String getAccountNoticeTitle(AccountNotice notice) {
        switch (notice.state) {
            case LOGGING_IN: return "Microsoft sign-in";
            case ERROR: return "Sign-in failed";
            case CONNECTED: return "Connected";
            case CANCELLED: return "Sign-in cancelled";
            default: return "Account expired";
        }
    }

    private static String getAccountNoticeDetail(AccountNotice notice) {
        if (notice.detail != null && !notice.detail.isEmpty()) return notice.detail;
        if (notice.state == AccountNoticeState.EXPIRED) {
            return (notice.account.username == null ? "Account" : notice.account.username)
                    + " needs Microsoft sign-in";
        }
        return "Microsoft sign-in required";
    }

    private static String getAccountNoticeButton(AccountNotice notice) {
        switch (notice.state) {
            case LOGGING_IN: return "Cancel";
            case CONNECTED: return "Done";
            case ERROR: return "Try again";
            case CANCELLED: return "Try again";
            default: return "Log back in";
        }
    }

    private static AccountNoticeBounds getAccountNoticeBounds(int screenWidth, AccountNotice notice) {
        int width = Math.min(ACCOUNT_NOTICE_WIDTH, Math.max(236, screenWidth - MARGIN * 2));
        int height = ACCOUNT_NOTICE_HEIGHT;
        float ease = getAccountNoticeEnterProgress(notice);
        int x = screenWidth - MARGIN - width + Math.round((width + MARGIN) * (1.0F - ease));
        return new AccountNoticeBounds(x, MARGIN, width, height);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static String stripFormatting(String text) {
        return text == null ? "" : text.replaceAll("\u00A7.", "");
    }

    private static void renderNotification(FontRenderer fr, int x, int y, Notification notif, float alpha) {
        int bgAlpha = (int) (160 * alpha);
        int textAlpha = (int) (255 * alpha);
        int bgColor = (bgAlpha << 24) | 0x111111;

        if (notif.isCustom) {
            RenderUtils.drawRoundedRect(x, y, notif.width, 36, 4, bgColor);
            if (notif.icon != null) {
                GlStateManager.color(1.0f, 1.0f, 1.0f, alpha);
                Minecraft.getMinecraft().getTextureManager().bindTexture(notif.icon);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
                net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 6, 0, 0, 24, 24, 24, 24);
            }
            int titleColor = notif.isError ? 0xff5555 : 0x55ff55;
            fr.drawStringWithShadow(notif.customTitle, x + 36, y + 8, Theme.withAlpha(titleColor, textAlpha));
            fr.drawStringWithShadow(notif.customMessage, x + 36, y + 20, Theme.withAlpha(0xffffff, textAlpha));
        } else {
            RenderUtils.drawRoundedRect(x, y, notif.width, 22, 4, bgColor);
            String prefix = notif.enabled ? "Enabled " : "Disabled ";
            int prefixColor = notif.enabled ? Theme.withAlpha(0x55ff55, textAlpha) : Theme.withAlpha(0xff5555, textAlpha);
            fr.drawStringWithShadow(prefix, x + 12, y + 7, prefixColor);
            fr.drawStringWithShadow(notif.moduleName, x + 12 + fr.getStringWidth(prefix), y + 7, Theme.withAlpha(0xffffff, textAlpha));
        }
    }

    private static class Notification {
        final String moduleName;
        final boolean enabled;
        
        final boolean isCustom;
        final String customTitle;
        final String customMessage;
        final ResourceLocation icon;
        final boolean isError;
        
        final long startTime;
        final int width;
        float slideAnimation = 0;

        Notification(String moduleName, boolean enabled) {
            this.isCustom = false;
            this.moduleName = moduleName;
            this.enabled = enabled;
            this.customTitle = null;
            this.customMessage = null;
            this.icon = null;
            this.isError = false;
            this.startTime = System.currentTimeMillis();
            this.width = Minecraft.getMinecraft().fontRendererObj.getStringWidth((enabled ? "Enabled " : "Disabled ") + moduleName) + 24;
        }

        Notification(String title, String message, ResourceLocation icon, boolean isError) {
            this.isCustom = true;
            this.moduleName = null;
            this.enabled = false;
            this.customTitle = title;
            this.customMessage = message;
            this.icon = icon;
            this.isError = isError;
            this.startTime = System.currentTimeMillis();
            int w1 = Minecraft.getMinecraft().fontRendererObj.getStringWidth(title);
            int w2 = Minecraft.getMinecraft().fontRendererObj.getStringWidth(message);
            this.width = Math.max(w1, w2) + 48;
        }

        void update() {
            if (slideAnimation < 1) {
                slideAnimation = Math.min(1, slideAnimation + 0.15f);
            }
        }

        boolean isExpired() {
            return System.currentTimeMillis() - startTime > DISPLAY_TIME + FADE_TIME;
        }

        float getSlideProgress() {
            float t = slideAnimation;
            return 1 - (1 - t) * (1 - t) * (1 - t);
        }

        float getAlpha() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed < DISPLAY_TIME) {
                return 1.0f;
            }
            float fadeProgress = (elapsed - DISPLAY_TIME) / (float) FADE_TIME;
            return Math.max(0, 1 - fadeProgress);
        }
    }

    private static class AccountNotice {
        final AltAccount account;
        final long createdAt = System.currentTimeMillis();
        volatile AccountNoticeState state = AccountNoticeState.EXPIRED;
        volatile String detail = "";
        volatile long terminalAt;

        AccountNotice(AltAccount account) {
            this.account = account;
        }
    }

    private static class AccountNoticeBounds {
        final int x, y, width, height;
        final int buttonX, buttonY, buttonWidth = 88, buttonHeight = 20;

        AccountNoticeBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.buttonX = x + width - 98;
            this.buttonY = y + 38;
        }
    }
}

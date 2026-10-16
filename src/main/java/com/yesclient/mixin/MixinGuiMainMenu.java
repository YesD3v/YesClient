package com.yesclient.mixin;

import com.yesclient.altmanager.CookieAltManager;
import com.yesclient.gui.alt.GuiAltManager;
import com.yesclient.utils.RenderUtils;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.yesclient.gui.ToggleNotification;

import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Random;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu extends GuiScreen {

    private static final ArrayDeque<String> RECENT_SPLASHES = new ArrayDeque<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void yesclient$chooseSplash(CallbackInfo ci) {
        List<String> choices = new ArrayList<>();
        try {
            ResourceLocation location = new ResourceLocation("minecraft", "texts/splashes.txt");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    mc.getResourceManager().getResource(location).getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) choices.add(line);
                }
            }
        } catch (Exception ignored) {
        }
        if (choices.isEmpty()) return;
        List<String> fresh = new ArrayList<>();
        for (String choice : choices) if (!RECENT_SPLASHES.contains(choice)) fresh.add(choice);
        List<String> pool = fresh.isEmpty() ? choices : fresh;
        String selected = pool.get(new Random().nextInt(pool.size())).replace("&lt;", "<").replace("&", "and");
        selected = selected.replaceAll("\\s+", " ").trim();
        if (selected.isEmpty()) return;
        setSplashText(selected);
        RECENT_SPLASHES.addLast(selected);
        while (RECENT_SPLASHES.size() > 5) RECENT_SPLASHES.removeFirst();
    }

    private void setSplashText(String text) {
        GuiMainMenu menu = (GuiMainMenu) (Object) this;
        try {
            Field fallback = null;
            for (Field field : GuiMainMenu.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) continue;
                if (field.getName().equals("splashText")) {
                    field.setAccessible(true);
                    field.set(menu, text);
                    return;
                }
                fallback = field;
            }
            if (fallback != null) {
                fallback.setAccessible(true);
                fallback.set(menu, text);
            }
        } catch (Exception ignored) {
        }
    }

    private static final int ACCOUNT_X = 12;
    private static final int ACCOUNT_Y = 12;
    private static final int ACCOUNT_WIDTH = 210;
    private static final int ACCOUNT_HEIGHT = 30;
    private static final int ACCOUNT_HEAD_SIZE = 22;
    private static final int ROW_HEIGHT = 28;
    private static final int ACCENT_BLUE = 0xFF3498DB;
    private static final int ACCENT_GREEN = 0xFF2ECC71;
    private static final int TEXT_WHITE = 0xFFF0F0F0;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int TEXT_DIM = 0xFF666666;

    private boolean accountDropdownOpen;
    private String lastLoggedInUsername = null;
    private CookieAltManager.AltAccount targetLoginAccount = null;
    private boolean savedAccountRestoreStarted;
    private boolean restoringSavedAccount;

    @Inject(method = "func_73876_c", at = @At("HEAD"))
    private void onUpdateScreen(CallbackInfo ci) {
        CookieAltManager manager = CookieAltManager.getInstance();
        if (!savedAccountRestoreStarted && targetLoginAccount == null) {
            savedAccountRestoreStarted = true;
            CookieAltManager.AltAccount lastAccount = manager.getLastAccount();
            if (lastAccount != null && !manager.isCurrentAccount(lastAccount)
                    && ((lastAccount.cookies != null && !lastAccount.cookies.isEmpty())
                    || (lastAccount.microsoftRefreshToken != null && !lastAccount.microsoftRefreshToken.isEmpty()))) {
                targetLoginAccount = lastAccount;
                restoringSavedAccount = true;
                manager.prepareAccountLogin();
                new Thread(() -> manager.loginWithAccount(lastAccount), "YesClient-RestoreLastAccount").start();
            }
        }

        if (targetLoginAccount != null) {
            CookieAltManager.Status status = manager.getStatus();
            
            if (status == CookieAltManager.Status.SUCCESS) {
                ToggleNotification.addCustomNotification("Logged in", manager.getCurrentUsername(), GuiAltManager.getCachedHead(targetLoginAccount), false);
                targetLoginAccount = null;
                lastLoggedInUsername = manager.getCurrentUsername();
                restoringSavedAccount = false;
            } else if (status == CookieAltManager.Status.FAILED || (!restoringSavedAccount && !targetLoginAccount.valid)) {
                ToggleNotification.showAccountExpired(targetLoginAccount);
                targetLoginAccount = null;
                restoringSavedAccount = false;
            }
        }
    }

    @Inject(method = "func_73866_w_", at = @At("RETURN"))
    private void onInitGui(CallbackInfo ci) {
        this.buttonList.add(new GuiButton(6969, this.width - 24, this.height - 24, 20, 20, ""));
    }

    @Redirect(method = "func_73863_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiMainMenu;func_73731_b(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V"))
    private void redirectDrawString(GuiMainMenu instance, net.minecraft.client.gui.FontRenderer fontRendererIn, String text, int x, int y, int color) {
        if (text != null && text.contains("Copyright Mojang AB")) {
            return;
        }
        instance.drawString(fontRendererIn, text, x, y, color);
    }

    @Inject(method = "func_73863_a", at = @At("RETURN"))
    private void drawYesClientAccount(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        for (GuiButton button : this.buttonList) {
            if (button.id == 6969) {
                net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                this.mc.getRenderItem().renderItemIntoGUI(new net.minecraft.item.ItemStack(net.minecraft.init.Items.book), button.xPosition + 2, button.yPosition + 2);
                net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            }
        }
        CookieAltManager manager = CookieAltManager.getInstance();
        CookieAltManager.Status status = manager.getStatus();
        boolean isLoggingIn = status != CookieAltManager.Status.IDLE && status != CookieAltManager.Status.SUCCESS && status != CookieAltManager.Status.FAILED;

        for (GuiButton btn : this.buttonList) {
            if (btn.id == 2) {
                btn.enabled = !isLoggingIn;
            }
        }

        String username = manager.getCurrentUsername();
        if (username == null || username.trim().isEmpty()) username = "Offline";

        boolean hovered = inside(mouseX, mouseY, ACCOUNT_X, ACCOUNT_Y, ACCOUNT_WIDTH, ACCOUNT_HEIGHT);
        RenderUtils.drawRoundedRect(ACCOUNT_X, ACCOUNT_Y, ACCOUNT_WIDTH, ACCOUNT_HEIGHT, 7,
                hovered || accountDropdownOpen ? 0xFF282828 : 0xFF1E1E1E);
        CookieAltManager.AltAccount current = findCurrent(manager, username);
        if (current == null) {
            current = new CookieAltManager.AltAccount(username, currentUuid(), null);
            current.valid = false;
        }
        
        boolean currentLoggingIn = isLoggingIn && targetLoginAccount != null;
        CookieAltManager.AltAccount faceAccount = currentLoggingIn ? targetLoginAccount : current;
        drawAccountHead(faceAccount, ACCOUNT_X + 5, ACCOUNT_Y + 4, ACCOUNT_HEAD_SIZE, currentLoggingIn);
        
        if (currentLoggingIn && GuiAltManager.getCachedHead(faceAccount) != null) {
            drawSpinner(ACCOUNT_X + 5 + ACCOUNT_HEAD_SIZE / 2, ACCOUNT_Y + 4 + ACCOUNT_HEAD_SIZE / 2);
        }

        fontRendererObj.drawString(username, ACCOUNT_X + 36, ACCOUNT_Y + 7, TEXT_WHITE);
        fontRendererObj.drawString("ACCOUNT", ACCOUNT_X + 36, ACCOUNT_Y + 18, TEXT_DIM);
        String caret = accountDropdownOpen ? "^" : "v";
        int caretY = ACCOUNT_Y + 10 + (accountDropdownOpen ? 3 : -1);
        drawCentered(caret, ACCOUNT_X + ACCOUNT_WIDTH - 12, caretY, TEXT_GRAY);

        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        String watermark = "\u00A7f\u00A7lYes\u00A77\u00A7lClient \u00A7fby \u00A7nyes.dev";
        int watermarkWidth = fontRendererObj.getStringWidth(watermark);
        fontRendererObj.drawStringWithShadow(watermark, sr.getScaledWidth() - watermarkWidth - 5, 5, 0xFFFFFF);

        ToggleNotification.render();

        if (!accountDropdownOpen) return;

        List<CookieAltManager.AltAccount> accounts = new ArrayList<>(manager.getAccounts());
        final String finalUsername = username;
        accounts.removeIf(a -> a.username != null && a.username.equalsIgnoreCase(finalUsername));
        int menuY = ACCOUNT_Y + ACCOUNT_HEIGHT + 4;
        int menuH = 8 + Math.max(1, accounts.size()) * ROW_HEIGHT;
        RenderUtils.drawRoundedRect(ACCOUNT_X, menuY, ACCOUNT_WIDTH, menuH, 8, 0xF01A1A1A);
        RenderUtils.drawRoundedRectOutline(ACCOUNT_X, menuY, ACCOUNT_WIDTH, menuH, 8, 0xFF333333, 1.0f);

        if (accounts.isEmpty()) {
            fontRendererObj.drawString("No saved accounts", ACCOUNT_X + 12, menuY + 12, TEXT_DIM);
            return;
        }

        for (int i = 0; i < accounts.size(); i++) {
            CookieAltManager.AltAccount account = accounts.get(i);
            int rowY = menuY + 4 + i * ROW_HEIGHT;
            if (inside(mouseX, mouseY, ACCOUNT_X + 4, rowY, ACCOUNT_WIDTH - 8, ROW_HEIGHT)) {
                RenderUtils.drawRoundedRect(ACCOUNT_X + 4, rowY, ACCOUNT_WIDTH - 8, ROW_HEIGHT, 5, 0xFF292929);
            }
            boolean isThisLoggingIn = isLoggingIn && targetLoginAccount == account;
            drawAccountHead(account, ACCOUNT_X + 12, rowY + 3, ACCOUNT_HEAD_SIZE, isThisLoggingIn);
            if (isThisLoggingIn && GuiAltManager.getCachedHead(account) != null) {
                drawSpinner(ACCOUNT_X + 12 + ACCOUNT_HEAD_SIZE / 2, rowY + 3 + ACCOUNT_HEAD_SIZE / 2);
            }
            
            String name = account.username == null || account.username.isEmpty() ? "Unknown" : account.username;
            fontRendererObj.drawString(name, ACCOUNT_X + 42, rowY + 9, TEXT_WHITE);
        }
    }

    private void drawSpinner(int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        long time = System.currentTimeMillis();
        float angle = (time % 1000) * 360f / 1000f;
        GlStateManager.rotate(angle, 0, 0, 1);
        
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        
        GL11.glLineWidth(2.5f);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        
        for (int i = 0; i <= 270; i += 10) {
            double rad = Math.toRadians(i);
            worldrenderer.pos(Math.cos(rad) * 6, Math.sin(rad) * 6, 0.0D).color(255, 255, 255, 255).endVertex();
        }
        
        tessellator.draw();
        
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    @Inject(method = "func_73864_a", at = @At("HEAD"), cancellable = true)
    private void clickYesClientAccount(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (mouseButton != 0) return;
        if (ToggleNotification.handleAccountNoticeClick(mouseX, mouseY)) {
            ci.cancel();
            return;
        }

        CookieAltManager manager = CookieAltManager.getInstance();
        CookieAltManager.Status status = manager.getStatus();
        boolean isLoggingIn = status != CookieAltManager.Status.IDLE && status != CookieAltManager.Status.SUCCESS && status != CookieAltManager.Status.FAILED;

        if (inside(mouseX, mouseY, ACCOUNT_X, ACCOUNT_Y, ACCOUNT_WIDTH, ACCOUNT_HEIGHT)) {
            if (isLoggingIn) {
                manager.cancelLogin();
            } else {
                accountDropdownOpen = !accountDropdownOpen;
            }
            ci.cancel();
            return;
        }

        if (isLoggingIn) return;
        if (!accountDropdownOpen) return;

        List<CookieAltManager.AltAccount> accounts = new ArrayList<>(manager.getAccounts());
        accounts.removeIf(a -> a.username != null && a.username.equalsIgnoreCase(manager.getCurrentUsername()));
        int menuY = ACCOUNT_Y + ACCOUNT_HEIGHT + 4;
        for (int i = 0; i < accounts.size(); i++) {
            if (!inside(mouseX, mouseY, ACCOUNT_X + 4, menuY + 4 + i * ROW_HEIGHT,
                    ACCOUNT_WIDTH - 8, ROW_HEIGHT)) continue;
            CookieAltManager.AltAccount account = accounts.get(i);
            accountDropdownOpen = false;
            
            targetLoginAccount = account;
            if (!account.valid) {
                manager.prepareAccountLogin();
                targetLoginAccount = null;
                ToggleNotification.showAccountExpired(account);
            } else {
                manager.prepareAccountLogin();
                new Thread(() -> manager.loginWithAccount(account), "YesClient-AccountSwitch").start();
            }
            ci.cancel();
            return;
        }

        accountDropdownOpen = false;
        ci.cancel();
    }

    @Inject(method = "func_146284_a", at = @At("HEAD"), cancellable = true)
    private void onActionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == 6969) {
            this.mc.displayGuiScreen(new com.yesclient.gui.GuiContentManager(this));
            ci.cancel();
            return;
        }
        CookieAltManager.Status status = CookieAltManager.getInstance().getStatus();
        boolean isLoggingIn = status != CookieAltManager.Status.IDLE && status != CookieAltManager.Status.SUCCESS && status != CookieAltManager.Status.FAILED;
        if (isLoggingIn && (button.id == 1 || button.id == 2)) {
            ci.cancel();
        }
    }

    private CookieAltManager.AltAccount findCurrent(CookieAltManager manager, String username) {
        for (CookieAltManager.AltAccount account : manager.getAccounts()) {
            if (account.username != null && account.username.equalsIgnoreCase(username)) return account;
        }
        return null;
    }

    private String currentUuid() {
        try {
            if (mc.getSession().getProfile() != null && mc.getSession().getProfile().getId() != null) {
                return mc.getSession().getProfile().getId().toString();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private void drawAccountHead(CookieAltManager.AltAccount account, int x, int y, int size, boolean grayOut) {
        ResourceLocation head = GuiAltManager.getCachedHead(account);
        if (head != null) {
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            mc.getTextureManager().bindTexture(head);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            if (grayOut) {
                GlStateManager.color(0.3f, 0.3f, 0.3f, 1.0f); 
            } else {
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            }
            net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f); 
            return;
        }

        GuiAltManager.loadHeadTexture(mc, account);

        drawSpinner(x + size / 2, y + size / 2);
        if (account.uuid != null && !account.uuid.isEmpty()) GuiAltManager.loadHeadTexture(mc, account);
    }

    private void drawCentered(String text, int centerX, int y, int color) {
        fontRendererObj.drawStringWithShadow(text,
                centerX - fontRendererObj.getStringWidth(text) / 2, y, color);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}

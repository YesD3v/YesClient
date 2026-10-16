package com.yesclient.gui.alt;

import com.yesclient.altmanager.CookieAltManager;
import com.yesclient.gui.ToggleNotification;
import com.yesclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

public class GuiAltManager extends GuiScreen {

    private final GuiScreen previousScreen;

    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 340;
    private static final int HEADER_HEIGHT = 36;
    private static final int FOOTER_HEIGHT = 52;
    private static final int CARD_HEIGHT = 36;
    private static final int CARD_MARGIN = 14;
    private static final int CARD_GAP = 4;
    private static final int HEAD_SIZE = 20;
    private static final int HEAD_RADIUS = 4;

    private static final int ACCENT_BLUE = 0xFF3498DB;
    private static final int ACCENT_GREEN = 0xFF2ECC71;
    private static final int ACCENT_RED = 0xFFE74C3C;
    private static final int TEXT_WHITE = 0xFFF0F0F0;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int TEXT_DIM = 0xFF666666;

    private static final int BG_COLOR = 0xF2141414;
    private static final int CARD_BG = 0xFF1E1E1E;
    private static final int CARD_HOVER = 0xFF282828;
    private static final int CARD_SELECTED = 0xFF2C3E50;
    private static final int CARD_INVALID = 0xFF2A1C1C;
    private static final int INVALID_DOT = 0xFFE74C3C;

    private static final int BTN_WIDTH = 90;
    private static final int BTN_HEIGHT = 26;
    private static final int BTN_GAP = 10;
    private static final int BTN_LOGIN = 0xFF27AE60;
    private static final int BTN_LOGIN_HOVER = 0xFF2ECC71;
    private static final int BTN_ADD = 0xFF2980B9;
    private static final int BTN_ADD_HOVER = 0xFF3498DB;
    private static final int BTN_DELETE = 0xFFC0392B;
    private static final int BTN_DELETE_HOVER = 0xFFE74C3C;
    private static final int BTN_BACK = 0xFF555555;
    private static final int BTN_BACK_HOVER = 0xFF777777;
    private static final int BTN_DISABLED = 0xFF333333;
    private static final int BTN_DISABLED_TEXT = 0xFF888888;

    private int panelX;
    private int panelY;

    private float scrollOffset = 0;
    private float targetScroll = 0;

    private String searchQuery = "";
    private boolean isSearching = false;

    private List<CookieAltManager.AltAccount> filteredAccounts;
    private CookieAltManager.AltAccount selectedAccount;
    private CookieAltManager.AltAccount loginTarget;

    private long lastClickTime = 0;
    private boolean confirmDelete = false;
    private long confirmDeleteTime = 0;

    private int accountSnapshot = Integer.MIN_VALUE;

    private static final Map<String, ResourceLocation> headCache = new ConcurrentHashMap<>();
    private static final Set<String> headLoading = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> headRetryAfter = new ConcurrentHashMap<>();

    public GuiAltManager(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
        this.filteredAccounts = new ArrayList<>();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        panelX = (this.width - PANEL_WIDTH) / 2;
        panelY = (this.height - PANEL_HEIGHT) / 2;

        for (CookieAltManager.AltAccount account : CookieAltManager.getInstance().getAccounts()) {
            loadHeadTexture(account);
        }

        updateSearch();
    }

    private void updateButtons() {
    }

    public static ResourceLocation getCachedHead(CookieAltManager.AltAccount account) {
        if (account == null || account.uuid == null || account.uuid.isEmpty()) return null;
        return headCache.get(account.uuid);
    }

    private void loadHeadTexture(CookieAltManager.AltAccount account) {
        loadHeadTexture(mc, account);
    }

    public static void loadHeadTexture(Minecraft minecraft, CookieAltManager.AltAccount account) {
        if (account == null) return;
        String uuid = account.uuid;

        if (uuid == null || uuid.isEmpty()) {
            resolveUuid(minecraft, account);
            return;
        }

        Long retryAfter = headRetryAfter.get(uuid);
        if (headCache.containsKey(uuid) || (retryAfter != null && System.currentTimeMillis() < retryAfter)
                || !headLoading.add(uuid)) return;

        new Thread(() -> {
            try {
                String cleanUuid = uuid.replace("-", "");
                BufferedImage image = null;

                image = fetchImage("https://api.mineatar.io/face/" + cleanUuid + "?scale=32");
                if (image == null)
                    image = fetchImage("https://mc-heads.net/avatar/" + cleanUuid + "/64");
                if (image == null)
                    image = fetchImage("https://crafatar.com/avatars/" + cleanUuid + "?size=64&overlay");

                if (image != null) {
                    final BufferedImage rounded = makeRounded(image, 12);
                    minecraft.addScheduledTask(() -> {
                        DynamicTexture tex = new DynamicTexture(rounded);
                        ResourceLocation loc = minecraft.getTextureManager().getDynamicTextureLocation("head_" + cleanUuid, tex);
                        headCache.put(uuid, loc);
                        headRetryAfter.remove(uuid);
                        headLoading.remove(uuid);
                    });
                } else {
                    headRetryAfter.put(uuid, System.currentTimeMillis() + 30000L);
                    headLoading.remove(uuid);
                }
            } catch (Exception e) {
                headRetryAfter.put(uuid, System.currentTimeMillis() + 30000L);
                headLoading.remove(uuid);
            }
        }).start();
    }

    private static void resolveUuid(Minecraft minecraft, CookieAltManager.AltAccount account) {
        if (account.username == null) return;
        String resolveKey = "resolve_" + account.username;
        Long retryAfter = headRetryAfter.get(resolveKey);
        if ((retryAfter != null && System.currentTimeMillis() < retryAfter) || !headLoading.add(resolveKey)) return;
        new Thread(() -> {
            try {
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + account.username);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null)
                        sb.append(line);
                    br.close();
                    com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(sb.toString())
                            .getAsJsonObject();
                    if (json.has("id")) {
                        account.uuid = json.get("id").getAsString();
                        loadHeadTexture(minecraft, account);
                    }
                }
            } catch (Exception e) {
            } finally {
                headRetryAfter.put(resolveKey, System.currentTimeMillis() + 30000L);
                headLoading.remove(resolveKey);
            }
        }).start();
    }

    private static BufferedImage fetchImage(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoInput(true);
            conn.connect();
            if (conn.getResponseCode() == 200) {
                return ImageIO.read(conn.getInputStream());
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static BufferedImage makeRounded(BufferedImage image, int radius) {
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, image.getWidth(), image.getHeight(), radius, radius));
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return output;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        CookieAltManager.getInstance().save();
    }

    @Override
    public void updateScreen() {
        scrollOffset += (targetScroll - scrollOffset) * 0.3f;
        int currentSnapshot = getAccountSnapshot();
        if (currentSnapshot != accountSnapshot) {
            updateSearch();
            accountSnapshot = currentSnapshot;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        CookieAltManager manager = CookieAltManager.getInstance();

        RenderUtils.drawRoundedRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 10, BG_COLOR);
        RenderUtils.drawRoundedRectOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 10, 0xFF333333, 1.5f);

        drawHeader(mouseX, mouseY);
        drawAccountList(mouseX, mouseY);

        int fy = panelY + PANEL_HEIGHT - FOOTER_HEIGHT;
        RenderUtils.drawRoundedRect(panelX, fy, PANEL_WIDTH, FOOTER_HEIGHT, 0, 0xFF181818);

        drawFooter(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
        ToggleNotification.renderFromAccountManager();
    }

    private void drawHeader(int mouseX, int mouseY) {
        fontRendererObj.drawStringWithShadow("\u00A7lAccount Manager", panelX + CARD_MARGIN, panelY + 20, TEXT_WHITE);
        
        CookieAltManager manager = CookieAltManager.getInstance();
        String currentUser = manager.getCurrentUsername();
        String currentStatus = "Offline";
        if (currentUser != null && !currentUser.isEmpty()) {
            currentStatus = "\u00A7aLogged in: \u00A7f" + currentUser;
        }

        CookieAltManager.Status status = manager.getStatus();
        if (status != CookieAltManager.Status.IDLE && status != CookieAltManager.Status.SUCCESS) {
            currentStatus = status.toString();
        } else if (manager.isMicrosoftLoginInProgress()) {
            currentStatus = "\u00A7eWaiting for Microsoft auth...";
        }
        
        int statusW = fontRendererObj.getStringWidth(currentStatus);
        fontRendererObj.drawStringWithShadow(currentStatus, panelX + PANEL_WIDTH - CARD_MARGIN - statusW, panelY + 20, TEXT_WHITE);
    }

    private void drawAccountList(int mouseX, int mouseY) {
        int listX = panelX + CARD_MARGIN;
        int listY = panelY + HEADER_HEIGHT + 4;
        int listW = PANEL_WIDTH - CARD_MARGIN * 2;
        int listH = PANEL_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 10;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.scissor(listX, listY, listW, listH);

        CookieAltManager manager = CookieAltManager.getInstance();
        String currentUser = manager.getCurrentUsername();
        CookieAltManager.Status loginStatus = manager.getStatus();
        boolean isLoggingIn = loginStatus != CookieAltManager.Status.IDLE
                && loginStatus != CookieAltManager.Status.SUCCESS && loginStatus != CookieAltManager.Status.FAILED;

        float currentY = listY - scrollOffset;

        for (int i = 0; i < filteredAccounts.size(); i++) {
            CookieAltManager.AltAccount account = filteredAccounts.get(i);
            float cardY = currentY + i * (CARD_HEIGHT + CARD_GAP);

            if (cardY + CARD_HEIGHT < listY || cardY > listY + listH)
                continue;

            boolean isSelected = (account == selectedAccount);
            boolean isHovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT;
            boolean isActive = currentUser.equals(account.username);

            int bg = CARD_BG;
            if (isSelected) bg = CARD_SELECTED;
            else if (isHovered) bg = CARD_HOVER;
            
            if (!account.valid && !isActive) {
                bg = CARD_INVALID;
                if (isSelected) bg = 0xFF352020;
                else if (isHovered) bg = 0xFF301818;
            }

            RenderUtils.drawRoundedRect(listX, cardY, listW, CARD_HEIGHT, 6, bg);
            RenderUtils.drawRoundedRectOutline(listX, cardY, listW, CARD_HEIGHT, 6, 
                isSelected ? ACCENT_BLUE : (isHovered ? 0xFF444444 : 0xFF252525), isSelected ? 1.5f : 1.0f);

            if (isActive) {
                RenderUtils.drawRoundedRect(listX, cardY, 3, CARD_HEIGHT, 3, ACCENT_GREEN, true, false, false, true);
            }

            int headX = listX + 8;
            int headY = (int) cardY + (CARD_HEIGHT - HEAD_SIZE) / 2;

            ResourceLocation head = getCachedHead(account);
            if (head != null) {
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                if (!account.valid && !isActive) GlStateManager.color(0.5f, 0.5f, 0.5f, 1.0f);  
                
                mc.getTextureManager().bindTexture(head);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
                net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(headX, headY, 0, 0, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE);
            } else {
                drawLoadingSpinner(headX + HEAD_SIZE / 2, headY + HEAD_SIZE / 2, Math.max(5, HEAD_SIZE / 3));
                loadHeadTexture(account);
            }
            if (isLoggingIn && account == loginTarget && head != null) {
                drawLoadingSpinner(headX + HEAD_SIZE / 2, headY + HEAD_SIZE / 2, Math.max(5, HEAD_SIZE / 3));
            }

            int textX = headX + HEAD_SIZE + 10;
            int textY = (int) cardY + (CARD_HEIGHT - 8) / 2;
            int nameColor = isActive ? ACCENT_BLUE : (account.valid ? TEXT_WHITE : INVALID_DOT);
            fontRendererObj.drawStringWithShadow(account.username == null ? "?" : account.username, textX, textY, nameColor);

            String status = account.getBanStatus();
            String banLabel = "UNKNOWN";
            int banColor = 0xFF555555;
            int textColor = TEXT_WHITE;

            if (!account.valid) {
                banLabel = "EXPIRED";
                banColor = 0xFFAA0000;
            } else if ("UNBANNED".equals(status)) {
                banLabel = "UNBANNED";
                banColor = 0xFF2ECC71;
            } else if ("BANNED".equals(status)) {
                banLabel = "BANNED";
                banColor = 0xFFE74C3C;
            } else if ("TEMP_BANNED".equals(status)) {
                long left = account.banExpiry - System.currentTimeMillis();
                if (left <= 0) {
                    account.autoUnban();
                    banLabel = "UNBANNED";
                    banColor = 0xFF2ECC71;
                } else {
                    long days = left / (24 * 60 * 60 * 1000L);
                    long hours = (left % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L);
                    long mins = (left % (60 * 60 * 1000L)) / (60 * 1000L);
                    long secs = (left % (60 * 1000L)) / 1000L;
                    StringBuilder sb = new StringBuilder("TEMP: ");
                    if (days > 0) sb.append(days).append("d ");
                    if (hours > 0 || days > 0) sb.append(hours).append("h ");
                    sb.append(mins).append("m ");
                    sb.append(secs).append("s");
                    banLabel = sb.toString().trim();
                    banColor = 0xFFE67E22;  
                }
            }

            if (banLabel != null) {
                int tagW = fontRendererObj.getStringWidth(banLabel) + 12;
                int tagH = 14;
                int tagX = listX + listW - tagW - 10;
                int tagY = (int) cardY + (CARD_HEIGHT - tagH) / 2;

                RenderUtils.drawRoundedRect(tagX, tagY, tagW, tagH, 3, banColor);
                fontRendererObj.drawString(banLabel, tagX + 6, tagY + 3, textColor);
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        float maxScroll = Math.max(0, filteredAccounts.size() * (CARD_HEIGHT + CARD_GAP) - listH);
        if (maxScroll > 0) {
            int scrollbarX = listX + listW + 4;
            int scrollbarW = 4;
            int scrollbarH = (int) (listH * (listH / (float) (filteredAccounts.size() * (CARD_HEIGHT + CARD_GAP))));
            int scrollbarY = listY + (int) ((scrollOffset / maxScroll) * (listH - scrollbarH));
            RenderUtils.drawRoundedRect(scrollbarX, scrollbarY, scrollbarW, scrollbarH, 2, 0xFF444444);
        }
    }

    private void drawFooter(int mouseX, int mouseY) {
        int fy = panelY + PANEL_HEIGHT - FOOTER_HEIGHT;
        int totalBtnW = BTN_WIDTH * 4 + BTN_GAP * 3;
        int btnX = panelX + (PANEL_WIDTH - totalBtnW) / 2;
        int btnY = fy + (FOOTER_HEIGHT - BTN_HEIGHT) / 2 + 1;

        boolean hasSelection = selectedAccount != null;
        CookieAltManager manager = CookieAltManager.getInstance();
        boolean isLoggingIn = manager.getStatus() != CookieAltManager.Status.SUCCESS && manager.getStatus() != CookieAltManager.Status.FAILED && manager.getStatus() != CookieAltManager.Status.IDLE;

        if (isLoggingIn) {
            drawPillButton(btnX, btnY, BTN_WIDTH, BTN_HEIGHT, "Cancel", ACCENT_RED, 0xFFFF5544, TEXT_WHITE, mouseX, mouseY, true);
        } else {
            drawPillButton(btnX, btnY, BTN_WIDTH, BTN_HEIGHT, "Login", BTN_LOGIN, BTN_LOGIN_HOVER, hasSelection ? TEXT_WHITE : BTN_DISABLED_TEXT, mouseX, mouseY, hasSelection);
        }
        
        btnX += BTN_WIDTH + BTN_GAP;
        drawPillButton(btnX, btnY, BTN_WIDTH, BTN_HEIGHT, "Add Account", BTN_ADD, BTN_ADD_HOVER, TEXT_WHITE, mouseX, mouseY, true);
        btnX += BTN_WIDTH + BTN_GAP;
        String deleteLabel = confirmDelete ? "Confirm?" : "Delete";
        int delBg = confirmDelete ? ACCENT_RED : (hasSelection ? BTN_DELETE : BTN_DISABLED);
        int delHover = confirmDelete ? 0xFFFF5544 : (hasSelection ? BTN_DELETE_HOVER : BTN_DISABLED);
        drawPillButton(btnX, btnY, BTN_WIDTH, BTN_HEIGHT, deleteLabel, delBg, delHover, hasSelection ? TEXT_WHITE : BTN_DISABLED_TEXT, mouseX, mouseY, hasSelection);
        btnX += BTN_WIDTH + BTN_GAP;
        drawPillButton(btnX, btnY, BTN_WIDTH, BTN_HEIGHT, "Back", BTN_BACK, BTN_BACK_HOVER, TEXT_WHITE, mouseX, mouseY, true);

        if (confirmDelete && System.currentTimeMillis() - confirmDeleteTime > 3000) {
            confirmDelete = false;
        }
    }

    private void drawPillButton(int x, int y, int w, int h, String text, int colorNormal, int colorHover,
            int textColor, int mouseX, int mouseY, boolean enabled) {
        boolean hovered = enabled && isInButton(mouseX, mouseY, x, y);
        int color = hovered ? colorHover : colorNormal;
        RenderUtils.drawRoundedRect(x, y, w, h, h / 2, color);
        fontRendererObj.drawString(text, x + (w - fontRendererObj.getStringWidth(text)) / 2, y + (h - 8) / 2,
                textColor);
    }

    private boolean isInButton(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + BTN_WIDTH && mouseY >= y && mouseY <= y + BTN_HEIGHT;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;
        if (ToggleNotification.handleAccountNoticeClick(mouseX, mouseY)) return;

        int listX = panelX + CARD_MARGIN;
        int listY = panelY + HEADER_HEIGHT + 4;
        int listW = PANEL_WIDTH - CARD_MARGIN * 2;
        int listH = PANEL_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 10;

        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            float currentY = listY - scrollOffset;
            for (int i = 0; i < filteredAccounts.size(); i++) {
                float cardY = currentY + i * (CARD_HEIGHT + CARD_GAP);
                if (mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT && cardY + CARD_HEIGHT > listY && cardY < listY + listH) {
                    CookieAltManager.AltAccount clicked = filteredAccounts.get(i);

                    if (!clicked.valid) {
                        selectedAccount = clicked;
                        ToggleNotification.showAccountExpired(clicked);
                        confirmDelete = false;
                        return;
                    }

                    long now = System.currentTimeMillis();
                    if (selectedAccount == clicked && now - lastClickTime < 400) {
                        loginSelected();
                    }
                    lastClickTime = now;
                    selectedAccount = clicked;
                    confirmDelete = false;
                    return;
                }
            }
        }

        int fy = panelY + PANEL_HEIGHT - FOOTER_HEIGHT;
        int totalBtnW = BTN_WIDTH * 4 + BTN_GAP * 3;
        int btnX = panelX + (PANEL_WIDTH - totalBtnW) / 2;
        int btnY = fy + (FOOTER_HEIGHT - BTN_HEIGHT) / 2 + 1;

        boolean hasSelection = selectedAccount != null;
        CookieAltManager manager = CookieAltManager.getInstance();
        boolean isLoggingIn = manager.getStatus() != CookieAltManager.Status.SUCCESS && manager.getStatus() != CookieAltManager.Status.FAILED && manager.getStatus() != CookieAltManager.Status.IDLE;

        if (isInButton(mouseX, mouseY, btnX, btnY)) {
            if (isLoggingIn) {
                manager.cancelLogin();
                return;
            } else if (hasSelection) {
                loginSelected();
                return;
            }
        }

        btnX += BTN_WIDTH + BTN_GAP;
        if (isInButton(mouseX, mouseY, btnX, btnY)) {
            mc.displayGuiScreen(new GuiCookieLogin(this));
            return;
        }

        btnX += BTN_WIDTH + BTN_GAP;
        if (hasSelection && isInButton(mouseX, mouseY, btnX, btnY)) {
            if (confirmDelete) {
                CookieAltManager.getInstance().removeAccount(selectedAccount);
                CookieAltManager.getInstance().save();
                selectedAccount = null;
                confirmDelete = false;
            } else {
                confirmDelete = true;
                confirmDeleteTime = System.currentTimeMillis();
            }
            return;
        }

        btnX += BTN_WIDTH + BTN_GAP;
        if (isInButton(mouseX, mouseY, btnX, btnY)) {
            mc.displayGuiScreen(previousScreen);
            return;
        }
    }

    private void loginSelected() {
        if (selectedAccount != null) {
            if (!selectedAccount.valid) {
                ToggleNotification.showAccountExpired(selectedAccount);
                return;
            }
            final CookieAltManager.AltAccount target = selectedAccount;
            final CookieAltManager manager = CookieAltManager.getInstance();
            loginTarget = target;
            new Thread(() -> {
                manager.prepareAccountLogin();
                boolean success = manager.loginWithAccount(target);
                mc.addScheduledTask(() -> {
                    if (loginTarget == target) loginTarget = null;
                    if (success) {
                        ToggleNotification.addCustomNotification("Logged in", manager.getCurrentUsername(),
                                getCachedHead(target), false);
                    } else if (!target.valid) {
                        ToggleNotification.showAccountExpired(target);
                    } else {
                        ToggleNotification.addCustomNotification("Login failed", manager.getLastError(),
                                getCachedHead(target), true);
                    }
                });
            }, "YesClient-AccountSwitch").start();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(previousScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            float maxScroll = Math.max(0, filteredAccounts.size() * (CARD_HEIGHT + CARD_GAP)
                    - (PANEL_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 10));
            targetScroll -= Math.signum(scroll) * (CARD_HEIGHT + CARD_GAP);
            if (targetScroll < 0) targetScroll = 0;
            if (targetScroll > maxScroll) targetScroll = maxScroll;
        }
    }

    private void updateSearch() {
        filteredAccounts.clear();
        String lowerQuery = searchQuery.toLowerCase();
        for (CookieAltManager.AltAccount account : CookieAltManager.getInstance().getAccounts()) {
            if (account.username != null && account.username.toLowerCase().contains(lowerQuery)) {
                filteredAccounts.add(account);
            }
        }
        
        String currentUser = CookieAltManager.getInstance().getCurrentUsername();
        filteredAccounts.sort((a, b) -> {
            boolean aActive = currentUser != null && currentUser.equals(a.username);
            boolean bActive = currentUser != null && currentUser.equals(b.username);
            if (aActive != bActive) return aActive ? -1 : 1;
            if (a.valid != b.valid) return a.valid ? -1 : 1;
            return a.username.compareToIgnoreCase(b.username);
        });
    }

    private int getAccountSnapshot() {
        int snapshot = 1;
        for (CookieAltManager.AltAccount account : CookieAltManager.getInstance().getAccounts()) {
            snapshot = 31 * snapshot + (account.username != null ? account.username.hashCode() : 0);
            snapshot = 31 * snapshot + (account.valid ? 1 : 0);
        }
        return snapshot;
    }

    public static void drawLoadingSpinner(int centerX, int centerY, int radius) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.rotate((System.currentTimeMillis() % 900L) * 360f / 900f, 0, 0, 1);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glLineWidth(2.0f);
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_LINE_STRIP, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        for (int angle = 0; angle <= 275; angle += 15) {
            double radians = Math.toRadians(angle);
            int alpha = 255 - angle / 2;
            renderer.pos(Math.cos(radians) * radius, Math.sin(radians) * radius, 0)
                    .color(235, 235, 245, alpha).endVertex();
        }
        tessellator.draw();
        GL11.glLineWidth(1.0f);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}

package com.yesclient.module.modules.client;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.yesclient.YesClient;
import com.yesclient.gui.CustomGuiNewChat;
import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ModeSetting;
import com.yesclient.module.settings.NumberSetting;
import com.yesclient.module.settings.StringSetting;
import net.minecraft.client.gui.*;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;


 




public class ChatModule extends Module {

    private final NumberSetting maxLines = new NumberSetting("Max Lines", "Maximum chat lines to keep", 5000, 500,
            10000);
    private final BooleanSetting customTabCompletion = new BooleanSetting(
            "Custom Tab Completion", "Enable player name tab completion for custom commands", true);
    private final ModeSetting chatCopyModifier = new ModeSetting("Copy Modifier", "Key to hold while copying chat", "None", "None", "Control", "Shift", "Alt");
    private final ModeSetting chatCopyButton = new ModeSetting("Copy Button", "Mouse button to copy chat", "Right Click", "Left Click", "Right Click", "Middle Click");
    private final NumberSetting backgroundOpacity = new NumberSetting("Background Opacity", "Scale the chat background opacity from 0% to 100%", 100, 0, 100);
    private final BooleanSetting textShadow = new BooleanSetting("Text Shadow", "Draw a shadow behind chat text", true);
    private final StringSetting keywords = new StringSetting("Keywords", "Comma-separated words or phrases; matches whole words only", "", 512);
    private final ModeSetting keywordColor = new ModeSetting("Keyword Color", "Minecraft chat color used for matching keywords", "Yellow",
            "Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray",
            "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White");
    private final BooleanSetting keywordHighlight = new BooleanSetting("Highlight Keywords", "Color matching words and phrases in chat", true);
    private final BooleanSetting boldKeywords = new BooleanSetting("Bold Keywords", "Make highlighted keywords bold", false);
    private final BooleanSetting keywordBell = new BooleanSetting("Keyword Bell", "Play an orb sound when a keyword appears in chat", true);
    private final NumberSetting keywordBellPitch = new NumberSetting("Keyword Bell Pitch", "Pitch of the keyword alert sound", 1.5, 0.0, 2.0, 0.1);

    private GuiNewChat originalChat;
    private CustomGuiNewChat customChat;
    private String cachedKeywordConfig;
    private Pattern cachedKeywordPattern;
    private Set<String> cachedUsernames = new HashSet<String>();
    private Set<String> cachedOwnUsernames = new HashSet<String>();

     
    private static final String[] INPUT_FIELD_NAMES = new String[] { "inputField", "field_146415_a", "a" };

    public ChatModule() {
        super("Chat", "Increases chat history and input limit", Category.CLIENT, ModuleMode.BOTH);
        addSetting(maxLines);
        addSetting(customTabCompletion);
        addSetting(chatCopyModifier);
        addSetting(chatCopyButton);
        addSetting(backgroundOpacity);
        addSetting(textShadow);
        addSetting(keywords);
        addSetting(keywordColor);
        addSetting(keywordHighlight);
        addSetting(boldKeywords);
        addSetting(keywordBell);
        addSetting(keywordBellPitch);
         
         
    }

     


    public boolean isCustomTabCompletionEnabled() {
        return customTabCompletion.isEnabled();
    }

    public static ChatModule getActiveModule() {
        try {
            YesClient client = YesClient.getInstance();
            if (client == null || client.getModuleManager() == null) return null;
            return client.getModuleManager().getModule(ChatModule.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public int getBackgroundOpacityPercent() {
        return backgroundOpacity.getIntValue();
    }

    public boolean isTextShadowEnabled() {
        return textShadow.isEnabled();
    }

    public static String highlightKeywordText(String text) {
        ChatModule module = getActiveModule();
        if (text == null || module == null || !module.isEnabled() || !module.keywordHighlight.isEnabled()) return text;
        module.refreshKeywordConfig();
        if (module.cachedKeywordPattern == null || module.isSelfMessage(text)) return text;

        Matcher matcher = module.cachedKeywordPattern.matcher(text);
        StringBuilder result = new StringBuilder(text.length() + 16);
        String activeFormatting = "\u00a7r";
        int cursor = 0;
        String colorCode = "\u00a7" + getMinecraftColorCode(module.keywordColor.getValue());
        while (matcher.find()) {
            String prefix = text.substring(cursor, matcher.start());
            result.append(prefix);
            activeFormatting = updateFormattingState(prefix, activeFormatting);
            if (isUsername(matcher.group(), module.cachedUsernames)) {
                result.append(matcher.group());
            } else {
                result.append(colorCode);
                if (module.boldKeywords.isEnabled()) result.append("\u00a7l");
                result.append(matcher.group()).append(activeFormatting);
            }
            cursor = matcher.end();
        }
        result.append(text.substring(cursor));
        return result.toString();
    }

    public static void playKeywordBellIfMatched(String text) {
        ChatModule module = getActiveModule();
        if (text == null || module == null || !module.isEnabled() || !module.keywordBell.isEnabled()) return;
        if (module.getMatchingKeyword(text) == null) return;
        try {
            if (mc != null && mc.getSoundHandler() != null) {
                mc.getSoundHandler().playSound(new com.yesclient.utils.ClientUiSound(
                        new net.minecraft.util.ResourceLocation("random.orb"), 0.72F, module.keywordBellPitch.getFloatValue()));
            }
        } catch (Throwable ignored) {
        }
    }

    private List<String> getKeywords() {
        List<String> result = new ArrayList<String>();
        String configured = keywords.getValue();
        if (configured == null || configured.trim().isEmpty()) return result;
        for (String part : configured.split("[,;\\r\\n]+")) {
            String term = stripFormattingCodes(part.trim());
            if (!term.isEmpty() && !result.contains(term)) result.add(term);
        }
        return result;
    }

    private String getMatchingKeyword(String text) {
        refreshKeywordConfig();
        if (cachedKeywordPattern == null || isSelfMessage(text)) return null;
        Matcher matcher = cachedKeywordPattern.matcher(stripFormattingCodes(text));
        while (matcher.find()) {
            if (!isUsername(matcher.group(), cachedUsernames)) return matcher.group();
        }
        return null;
    }

    private void refreshKeywordConfig() {
        String configured = keywords.getValue() == null ? "" : keywords.getValue();
        if (!configured.equals(cachedKeywordConfig)) {
            cachedKeywordConfig = configured;
            List<String> terms = getKeywords();
            Collections.sort(terms, new Comparator<String>() {
                @Override
                public int compare(String first, String second) {
                    return Integer.compare(second.length(), first.length());
                }
            });
            if (terms.isEmpty()) {
                cachedKeywordPattern = null;
            } else {
                StringBuilder expression = new StringBuilder();
                for (String term : terms) {
                    if (expression.length() > 0) expression.append('|');
                    expression.append(Pattern.quote(term));
                }
                String boundaries = "(?<![\\p{L}\\p{N}_])(?:" + expression + ")(?![\\p{L}\\p{N}_])";
                cachedKeywordPattern = Pattern.compile(boundaries, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            }
        }
    }

    private void refreshKeywordCache() {
        refreshKeywordConfig();
        cachedUsernames = getKnownUsernames();
        cachedOwnUsernames = getOwnUsernames();
    }

    private static Set<String> getKnownUsernames() {
        Set<String> usernames = new HashSet<String>();
        if (mc == null) return usernames;
        if (mc.thePlayer != null && mc.thePlayer.getName() != null) {
            usernames.add(mc.thePlayer.getName().toLowerCase(Locale.ROOT));
        }
        try {
            if (mc.getSession() != null && mc.getSession().getUsername() != null) {
                usernames.add(mc.getSession().getUsername().toLowerCase(Locale.ROOT));
            }
            if (mc.getNetHandler() != null) {
                for (net.minecraft.client.network.NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                    if (info.getGameProfile() != null && info.getGameProfile().getName() != null) {
                        usernames.add(info.getGameProfile().getName().toLowerCase(Locale.ROOT));
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return usernames;
    }

    private static Set<String> getOwnUsernames() {
        Set<String> ownNames = new HashSet<String>();
        if (mc == null) return ownNames;
        try {
            if (mc.thePlayer != null && mc.thePlayer.getName() != null) {
                ownNames.add(mc.thePlayer.getName().toLowerCase(Locale.ROOT));
            }
            if (mc.getSession() != null && mc.getSession().getUsername() != null) {
                ownNames.add(mc.getSession().getUsername().toLowerCase(Locale.ROOT));
            }
        } catch (Throwable ignored) {
        }
        return ownNames;
    }

    private static boolean isUsername(String text, Set<String> usernames) {
        return usernames.contains(stripFormattingCodes(text).trim().toLowerCase(Locale.ROOT));
    }

    private boolean isSelfMessage(String text) {
        if (text == null || mc == null) return false;
        String plainText = stripFormattingCodes(text).trim();
        int separator = plainText.indexOf(':');
        if (separator < 1 || separator > 80) return false;
        String prefix = plainText.substring(0, separator).trim().toLowerCase(Locale.ROOT);
        for (String ownName : cachedOwnUsernames) {
            if (prefix.equals(ownName) || prefix.endsWith(" " + ownName)) return true;
        }
        return false;
    }

    private static String stripFormattingCodes(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '\u00a7' && i + 1 < text.length()) {
                i++;
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }

    private static String updateFormattingState(String text, String currentState) {
        String color = "";
        StringBuilder decorations = new StringBuilder();
        String combined = currentState + text;
        for (int i = 0; i + 1 < combined.length(); i++) {
            if (combined.charAt(i) != '\u00a7') continue;
            char code = Character.toLowerCase(combined.charAt(++i));
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                color = "\u00a7" + code;
                decorations.setLength(0);
            } else if (code == 'r') {
                color = "\u00a7r";
                decorations.setLength(0);
            } else if (code >= 'k' && code <= 'o' && decorations.indexOf("\u00a7" + code) < 0) {
                decorations.append('\u00a7').append(code);
            }
        }
        return color.isEmpty() ? "\u00a7r" : color + decorations.toString();
    }

    private static char getMinecraftColorCode(String color) {
        if ("Black".equals(color)) return '0';
        if ("Dark Blue".equals(color)) return '1';
        if ("Dark Green".equals(color)) return '2';
        if ("Dark Aqua".equals(color)) return '3';
        if ("Dark Red".equals(color)) return '4';
        if ("Dark Purple".equals(color)) return '5';
        if ("Gold".equals(color)) return '6';
        if ("Gray".equals(color)) return '7';
        if ("Dark Gray".equals(color)) return '8';
        if ("Blue".equals(color)) return '9';
        if ("Green".equals(color)) return 'a';
        if ("Aqua".equals(color)) return 'b';
        if ("Red".equals(color)) return 'c';
        if ("Light Purple".equals(color)) return 'd';
        if ("Yellow".equals(color)) return 'e';
        return 'f';
    }

    @Override
    protected void onEnable() {
        refreshKeywordCache();
        if (mc.ingameGUI == null)
            return;

         
        originalChat = mc.ingameGUI.getChatGUI();
        if (!(originalChat instanceof CustomGuiNewChat)) {
            customChat = new CustomGuiNewChat(mc);
            updateCustomChatSettings();

             
            migrateChatLines(originalChat, customChat);

            setPersistentChatGUI(customChat);
        }
    }

    @Override
    public void onTick() {
        refreshKeywordCache();
        if (customChat != null) {
            updateCustomChatSettings();
        }

         
        if (mc.ingameGUI != null && isEnabled()) {
            if (!(mc.ingameGUI.getChatGUI() instanceof CustomGuiNewChat)) {
                if (customChat == null)
                    customChat = new CustomGuiNewChat(mc);
                updateCustomChatSettings();
                originalChat = mc.ingameGUI.getChatGUI();  
                migrateChatLines(originalChat, customChat);
                setPersistentChatGUI(customChat);
            }
        }
    }

    private void updateCustomChatSettings() {
        if (customChat != null) {
            customChat.setMaxLines(maxLines.getIntValue());
        }
    }

    @Override
    protected void onDisable() {
        if (originalChat != null && mc.ingameGUI != null) {
            setPersistentChatGUI(originalChat);
        }
    }

    private void migrateChatLines(GuiNewChat oldChat, CustomGuiNewChat newChat) {
        try {
            Field chatLinesField = ReflectionHelper.findField(GuiNewChat.class, "chatLines", "field_146252_h");
            Field drawnChatLinesField = ReflectionHelper.findField(GuiNewChat.class, "drawnChatLines",
                    "field_146253_i");
            chatLinesField.setAccessible(true);
            drawnChatLinesField.setAccessible(true);

            List<ChatLine> oldLines = (List<ChatLine>) chatLinesField.get(oldChat);
            List<ChatLine> oldDrawn = (List<ChatLine>) drawnChatLinesField.get(oldChat);

            List<ChatLine> newLines = (List<ChatLine>) chatLinesField.get(newChat);
            List<ChatLine> newDrawn = (List<ChatLine>) drawnChatLinesField.get(newChat);

            newLines.clear();
            newLines.addAll(oldLines);

            newDrawn.clear();
            newDrawn.addAll(oldDrawn);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setPersistentChatGUI(GuiNewChat chat) {
        try {
             
            Field field = ReflectionHelper.findField(GuiIngame.class, "persistantChatGUI", "field_73840_e");
            field.setAccessible(true);

             
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(mc.ingameGUI, chat);
        } catch (Exception e) {
            e.printStackTrace();
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiChat) {
            GuiChat guiChat = (GuiChat) event.gui;
            try {
                Field inputField = ReflectionHelper.findField(GuiChat.class, INPUT_FIELD_NAMES);
                inputField.setAccessible(true);
                GuiTextField textField = (GuiTextField) inputField.get(guiChat);
                if (textField != null) {
                    textField.setMaxStringLength(5000);  
                }
            } catch (Exception e) {
                 
            }
        }
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(maxLines.getIntValue());
    }

     


    public boolean checkCopyModifiers() {
        if (!isEnabled()) return false;
        String mod = chatCopyModifier.getValue();
        if (mod.equals("None")) return true;
        if (mod.equals("Control")) return org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LCONTROL) || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RCONTROL);
        if (mod.equals("Shift")) return org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT) || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
        if (mod.equals("Alt")) return org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LMENU) || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RMENU);
        return false;
    }

    public boolean checkCopyButton(int button) {
        if (!isEnabled()) return false;
        String btn = chatCopyButton.getValue();
        if (btn.equals("Left Click")) return button == 0;
        if (btn.equals("Right Click")) return button == 1;
        if (btn.equals("Middle Click")) return button == 2;
        return false;
    }
}

package com.yesclient.mixin;
import java.lang.reflect.Field;
import java.util.List;

import com.yesclient.YesClient;
import com.yesclient.command.ChatCommandHandler;
import com.yesclient.module.modules.client.ChatModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collections;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen {

     
    private static List<String> currentCompletions = new ArrayList<>();
    private static int completionIndex = 0;
    private static String originalPrefix = "";
    private static String lastTextBeforeTab = "";

     



    @Inject(method = "func_73869_a", at = @At("HEAD"), cancellable = true)
    private void onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
         
        if (keyCode == 15) {
            GuiTextField inputField = getInputField();
            if (inputField == null) {
                return;
            }

            String text = inputField.getText();

             
            if (ChatCommandHandler.isCommand(text)) {
                if (handleDotCommandTab(inputField, ChatCommandHandler.getCleanCommand(text).trim())) {
                    ci.cancel();
                }
                return;
            }

             
            if (text.startsWith("/")) {
                if (handleSlashCommandTab(inputField, text)) {
                    ci.cancel();
                }
            }
        }
    }

     



    @Inject(method = "func_73864_a", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        try {
            ChatModule chatModule = YesClient.getInstance().getModuleManager().getModule(ChatModule.class);
            if (chatModule == null || !chatModule.isEnabled())
                return;

            if (chatModule.checkCopyModifiers() && chatModule.checkCopyButton(mouseButton)) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.ingameGUI != null && mc.ingameGUI.getChatGUI() != null) {
                    net.minecraft.util.IChatComponent component = mc.ingameGUI.getChatGUI().getChatComponent(org.lwjgl.input.Mouse.getX(), org.lwjgl.input.Mouse.getY());
                    if (component != null) {
                        String text = net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(component.getUnformattedTextForChat());
                        if (text != null && !text.trim().isEmpty()) {
                            StringSelection selection = new StringSelection(text);
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                            com.yesclient.utils.ChatUtils.sendClientMessage("\u00A7aCopied chat message to clipboard!");
                            ci.cancel();
                        }
                    }
                }
            }
        } catch (Throwable t) {
             
        }
    }

     











    private String getChatLineAtMouse(int mouseY) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            GuiNewChat chatGui = mc.ingameGUI.getChatGUI();
            if (!chatGui.getChatOpen())
                return null;

            float chatScale = chatGui.getChatScale();

             
             
             
             
             
            int chatAreaBottom = this.height - 41;
            int distFromBottom = chatAreaBottom - mouseY;

             
            int k = net.minecraft.util.MathHelper.floor_float((float) distFromBottom / chatScale);

            if (k < 0)
                return null;

             
            Field drawnField = null;
            Field scrollField = null;
            try {
                drawnField = GuiNewChat.class.getDeclaredField("drawnChatLines");
            } catch (NoSuchFieldException e) {
                drawnField = GuiNewChat.class.getDeclaredField("field_146253_i");
            }
            try {
                scrollField = GuiNewChat.class.getDeclaredField("scrollPos");
            } catch (NoSuchFieldException e) {
                scrollField = GuiNewChat.class.getDeclaredField("field_146250_j");
            }
            drawnField.setAccessible(true);
            scrollField.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<ChatLine> drawnChatLines = (List<ChatLine>) drawnField.get(chatGui);
            int scrollPos = scrollField.getInt(chatGui);

             
            int visibleLines = Math.min(chatGui.getLineCount(), drawnChatLines.size());
            int fontHeight = mc.fontRendererObj.FONT_HEIGHT;
            if (k >= fontHeight * visibleLines + visibleLines)
                return null;

             
            int lineIndex = k / fontHeight + scrollPos;

            if (lineIndex < 0 || lineIndex >= drawnChatLines.size())
                return null;

            ChatLine chatLine = drawnChatLines.get(lineIndex);
            if (chatLine == null || chatLine.getChatComponent() == null)
                return null;

             
            String text = chatLine.getChatComponent().getUnformattedText();
            text = text.replaceAll("\u00A7.", "");
            return text;
        } catch (Exception e) {
            return null;
        }
    }

     


    private GuiTextField getInputField() {
        try {
            GuiChat guiChat = (GuiChat) (Object) this;
            Field field = null;

            try {
                field = GuiChat.class.getDeclaredField("inputField");
            } catch (NoSuchFieldException e) {
                field = GuiChat.class.getDeclaredField("field_146415_a");
            }

            field.setAccessible(true);
            return (GuiTextField) field.get(guiChat);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean handleDotCommandTab(GuiTextField inputField, String text) {
        String[] parts = text.split(" ");
        String lastWord = parts.length > 0 ? parts[parts.length - 1] : "";
        if (text.endsWith(" "))
            lastWord = "";

        List<String> matches = new ArrayList<>();

        if (parts.length <= 1 && !text.endsWith(" ")) {
            matches.addAll(ChatCommandHandler.getCommandSuggestions(text));
        } else {
            matches.addAll(getPlayerNameMatches(lastWord));
        }

        if (!matches.isEmpty()) {
            applyCompletion(inputField, text, lastWord, matches);
            return true;
        }
        return false;
    }

    private boolean handleSlashCommandTab(GuiTextField inputField, String text) {
        String[] parts = text.split(" ");

         
        if (parts.length < 2 && !text.endsWith(" ")) {
            return false;  
        }

        String lastWord = parts.length > 0 ? parts[parts.length - 1] : "";
        if (text.endsWith(" "))
            lastWord = "";

        List<String> matches = getPlayerNameMatches(lastWord);

        if (!matches.isEmpty()) {
            applyCompletion(inputField, text, lastWord, matches);
            return true;
        }
        return false;
    }

     




    private void applyCompletion(GuiTextField inputField, String text, String prefix, List<String> matches) {
        Collections.sort(matches, String.CASE_INSENSITIVE_ORDER);

         
        String textBeforeLastWord = text.substring(0, text.length() - prefix.length());

        if (text.equals(lastTextBeforeTab) && !currentCompletions.isEmpty()) {
             
            completionIndex = (completionIndex + 1) % currentCompletions.size();
        } else {
             
            currentCompletions = new ArrayList<>(matches);
            completionIndex = 0;
            originalPrefix = prefix;
        }

         
        String completion = currentCompletions.get(completionIndex);
        String newText = textBeforeLastWord + completion;

        inputField.setText(newText);
        inputField.setCursorPositionEnd();

         
        lastTextBeforeTab = newText;
    }

    private List<String> getPlayerNameMatches(String prefix) {
        List<String> matches = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase();

        if (Minecraft.getMinecraft().getNetHandler() != null) {
            for (NetworkPlayerInfo info : Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap()) {
                if (info.getGameProfile() != null) {
                    String name = info.getGameProfile().getName();
                    if (name.toLowerCase().startsWith(lowerPrefix)) {
                        matches.add(name);
                    }
                }
            }
        }
        return matches;
    }
}

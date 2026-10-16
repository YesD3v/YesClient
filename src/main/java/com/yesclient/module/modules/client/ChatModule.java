package com.yesclient.module.modules.client;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.util.List;

import com.yesclient.gui.CustomGuiNewChat;
import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ModeSetting;
import com.yesclient.module.settings.NumberSetting;
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

    private GuiNewChat originalChat;
    private CustomGuiNewChat customChat;

     
    private static final String[] INPUT_FIELD_NAMES = new String[] { "inputField", "field_146415_a", "a" };

    public ChatModule() {
        super("Chat", "Increases chat history and input limit", Category.CLIENT, ModuleMode.BOTH);
        addSetting(maxLines);
        addSetting(customTabCompletion);
        addSetting(chatCopyModifier);
        addSetting(chatCopyButton);         
         
         
    }

     


    public boolean isCustomTabCompletionEnabled() {
        return customTabCompletion.isEnabled();
    }

    @Override
    protected void onEnable() {
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

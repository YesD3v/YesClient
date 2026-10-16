package com.yesclient.event;
import java.lang.reflect.Field;

import com.yesclient.command.ChatCommandHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;


public class ChatSendHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private long lastTime = 0;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!(event.gui instanceof GuiChat))
            return;

        if (System.currentTimeMillis() - lastTime < 100)
            return;

         
        if (Keyboard.getEventKey() != Keyboard.KEY_RETURN && Keyboard.getEventKey() != Keyboard.KEY_NUMPADENTER)
            return;
        if (!Keyboard.getEventKeyState())
            return;

        try {
            GuiChat guiChat = (GuiChat) event.gui;
            GuiTextField inputField = getInputField(guiChat);

            if (inputField != null) {
                String message = inputField.getText().trim();

                if (ChatCommandHandler.isCommand(message)) {
                     
                    boolean handled = ChatCommandHandler.handleChatMessage(message);

                    if (handled) {
                        lastTime = System.currentTimeMillis();
                         
                        event.setCanceled(true);
                        mc.displayGuiScreen(null);
                    }
                }
            }
        } catch (Exception e) {
             
        }
    }

    private GuiTextField getInputField(GuiChat guiChat) {
         
        String[] fieldNames = { "inputField", "field_146415_a", "a" };

        for (String fieldName : fieldNames) {
            try {
                Field field = GuiChat.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                return (GuiTextField) field.get(guiChat);
            } catch (Exception e) {
                 
            }
        }

        return null;
    }
}

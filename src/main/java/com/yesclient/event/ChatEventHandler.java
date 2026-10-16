package com.yesclient.event;

import com.yesclient.command.ChatCommandHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatEventHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

     



    public static boolean onChatSend(String message) {
        if (ChatCommandHandler.isCommand(message)) {
            return ChatCommandHandler.handleChatMessage(message);
        }
        return false;
    }
}

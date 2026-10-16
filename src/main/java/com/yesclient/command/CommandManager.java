package com.yesclient.command;
import java.util.List;
import java.util.Arrays;

import com.yesclient.utils.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;

public class CommandManager {

    private final List<Command> commands = new ArrayList<>();
    private String prefix = ".";

    public CommandManager() {
        registerCommands();
    }

    private void registerCommands() {
        commands.add(new CommandScreenshot());
        commands.add(new Command("fly", "Toggle Fly module", "fly") {
            @Override
            public void execute(String[] args) {
                com.yesclient.module.Module flyModule = com.yesclient.YesClient.getInstance().getModuleManager().getModule(com.yesclient.module.modules.exploit.BlatantFlyModule.class);
                if (flyModule != null) {
                    flyModule.toggle();
                    com.yesclient.utils.ChatUtils.sendClientMessage("\u00A7aFly has been " + (flyModule.isEnabled() ? "enabled" : "disabled"));
                } else {
                    com.yesclient.utils.ChatUtils.sendClientMessage("\u00A7cFly module not found!");
                }
            }
        });
        
        
        
        
        commands.add(new Command("clip", "Teleport in a direction", "clip <up/down/forward/backward> <distance>") {
            @Override
            public void execute(String[] args) {
                if (args.length < 2) {
                    ChatUtils.sendClientMessage("\u00A7cUsage: .clip <direction> <distance>");
                    return;
                }

                double distance;
                try {
                    distance = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    ChatUtils.sendClientMessage("\u00A7cInvalid distance: " + args[1]);
                    return;
                }

                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer == null)
                    return;

                double x = mc.thePlayer.posX;
                double y = mc.thePlayer.posY;
                double z = mc.thePlayer.posZ;

                String dir = args[0].toLowerCase();

                switch (dir) {
                    case "up":
                    case "u":
                        y += distance;
                        break;
                    case "down":
                    case "d":
                        y -= distance;
                        break;
                    case "forward":
                    case "f":
                        float yaw = mc.thePlayer.rotationYaw * (float) Math.PI / 180f;
                        x -= MathHelper.sin(yaw) * distance;
                        z += MathHelper.cos(yaw) * distance;
                        break;
                    case "back":
                    case "backward":
                    case "b":
                        yaw = mc.thePlayer.rotationYaw * (float) Math.PI / 180f;
                        x += MathHelper.sin(yaw) * distance;
                        z -= MathHelper.cos(yaw) * distance;
                        break;
                    case "left":
                    case "l":
                        yaw = (mc.thePlayer.rotationYaw - 90) * (float) Math.PI / 180f;
                        x -= MathHelper.sin(yaw) * distance;
                        z += MathHelper.cos(yaw) * distance;
                        break;
                    case "right":
                    case "r":
                        yaw = (mc.thePlayer.rotationYaw + 90) * (float) Math.PI / 180f;
                        x -= MathHelper.sin(yaw) * distance;
                        z += MathHelper.cos(yaw) * distance;
                        break;
                    default:
                        ChatUtils.sendClientMessage("\u00A7cInvalid direction: " + dir);
                        return;
                }

                mc.thePlayer.setPosition(x, y, z);
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                ChatUtils
                        .sendClientMessage("\u00A7aClipped " + dir + " " + String.format("%.1f", distance) + " blocks");
            }
        });
    }

    public boolean handleCommand(String message) {
        if (!message.startsWith(prefix))
            return false;

        String[] parts = message.substring(prefix.length()).split(" ");
        if (parts.length == 0)
            return false;

        String commandName = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        for (Command command : commands) {
            if (command.getName().equalsIgnoreCase(commandName)) {
                command.execute(args);
                return true;
            }
        }

        return false;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        if (prefix != null && !prefix.trim().isEmpty()) {
            this.prefix = prefix;
        }
    }
}

package com.yesclient.mixin;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ScreenShotHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import net.minecraft.client.shader.Framebuffer;

@Mixin(ScreenShotHelper.class)
public class MixinScreenShotHelper {
    
    @Inject(method = "func_148259_a", at = @At("RETURN"), cancellable = true, remap = false)
    private static void onSaveScreenshot(File gameDirectory, String screenshotName, int width, int height, Framebuffer buffer, CallbackInfoReturnable<IChatComponent> cir) {
        IChatComponent returnValue = cir.getReturnValue();
        if (returnValue instanceof ChatComponentTranslation) {
            ChatComponentTranslation translation = (ChatComponentTranslation) returnValue;
            if ("screenshot.success".equals(translation.getKey())) {
                Object[] args = translation.getFormatArgs();
                if (args.length > 0 && args[0] instanceof IChatComponent) {
                    IChatComponent fileComponent = (IChatComponent) args[0];
                    String fileName = fileComponent.getUnformattedText();
                    String filePath = "";
                    if (fileComponent.getChatStyle() != null && fileComponent.getChatStyle().getChatClickEvent() != null) {
                        filePath = fileComponent.getChatStyle().getChatClickEvent().getValue();
                    }
                    
                    if (filePath.isEmpty()) {
                        File screenshotsDir = new File(gameDirectory, "screenshots");
                        filePath = new File(screenshotsDir, fileName).getAbsolutePath();
                    }

                    String prefix = com.yesclient.command.ChatCommandHandler.getPrefix();
                    
                    ChatComponentText custom = new ChatComponentText("");
                    
                    ChatComponentText nameComp = new ChatComponentText("\u00A7f\u00A7n" + fileName);
                    nameComp.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, filePath));
                    nameComp.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("\u00A7aOpen File")));
                    custom.appendSibling(nameComp);
                    
                    custom.appendSibling(new ChatComponentText(" \u00A7r\u00A7fsaved! "));
                    
                    ChatComponentText copyComp = new ChatComponentText("\u00A79\u00A7l[Copy]");
                    copyComp.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, prefix + "ss copy " + filePath));
                    copyComp.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("\u00A79Copy image to clipboard")));
                    custom.appendSibling(copyComp);
                    
                    custom.appendSibling(new ChatComponentText(" "));
                    
                    ChatComponentText delComp = new ChatComponentText("\u00A7c\u00A7l[Delete]");
                    delComp.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, prefix + "ss delete " + filePath));
                    delComp.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("\u00A7cDelete image")));
                    custom.appendSibling(delComp);
                    
                    cir.setReturnValue(custom);
                }
            }
        }
    }
}

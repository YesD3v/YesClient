package com.yesclient.command;
import java.util.Arrays;

import com.yesclient.utils.ChatUtils;
import java.io.File;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class CommandScreenshot extends Command {

    public CommandScreenshot() {
        super("ss", "Manage screenshots", "ss <copy/delete> <path>");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) return;
        
        String action = args[0].toLowerCase();
        String path = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        File file = new File(path);
        
        if (!file.exists()) {
            ChatUtils.sendClientMessage("\u00A7cScreenshot not found!");
            return;
        }
        
        if (action.equals("copy")) {
            new Thread(() -> {
                try {
                    BufferedImage img = ImageIO.read(file);
                    Transferable trans = new Transferable() {
                        public DataFlavor[] getTransferDataFlavors() {
                            return new DataFlavor[] { DataFlavor.imageFlavor };
                        }
                        public boolean isDataFlavorSupported(DataFlavor flavor) {
                            return DataFlavor.imageFlavor.equals(flavor);
                        }
                        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                            if (isDataFlavorSupported(flavor)) return img;
                            throw new UnsupportedFlavorException(flavor);
                        }
                    };
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(trans, null);
                    ChatUtils.sendClientMessage("\u00A7aScreenshot copied to clipboard!");
                } catch (Exception e) {
                    ChatUtils.sendClientMessage("\u00A7cFailed to copy screenshot: " + e.getMessage());
                }
            }, "YesClient-ScreenshotCopy").start();
        } else if (action.equals("delete")) {
            if (file.delete()) {
                ChatUtils.sendClientMessage("\u00A7cScreenshot deleted.");
            } else {
                ChatUtils.sendClientMessage("\u00A7cFailed to delete screenshot.");
            }
        }
    }
}

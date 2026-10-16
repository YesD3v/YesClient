package com.yesclient.gui;
import java.util.List;

import com.yesclient.imagetonbt.ImageProcessor;
import com.yesclient.imagetonbt.ItemGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.*;

public class ImageToNBTScreen extends GuiScreen {

     
    private static String lastItemName = "Custom_NBT";
    private static String lastItemType = "paper";
    private static String lastDirectory = System.getProperty("user.home");

    private GuiTextField itemNameField;
    private GuiTextField itemTypeField;
    private GuiButton browseButton;
    private GuiButton pasteButton;
    private GuiButton normalButton;
    private GuiButton contrastButton;
    private GuiButton brightButton;

    private BufferedImage selectedImage = null;
    private boolean showVariants = false;

     
    private String[] normalVariant;
    private String[] contrastVariant;
    private String[] brightVariant;

     
    private String[] hoverPreview = null;
    private String statusMessage = "";

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int centerX = width / 2;
        int centerY = height / 2;
        int startY = centerY - 70;

         
        itemNameField = new GuiTextField(0, fontRendererObj, centerX - 100, startY + 12, 200, 18);
        itemNameField.setMaxStringLength(50);
        itemNameField.setText(lastItemName);

         
        itemTypeField = new GuiTextField(1, fontRendererObj, centerX - 100, startY + 50, 200, 18);
        itemTypeField.setMaxStringLength(50);
        itemTypeField.setText(lastItemType);

         
        browseButton = new GuiButton(1, centerX - 105, startY + 80, 100, 20, "Browse File");
        pasteButton = new GuiButton(2, centerX + 5, startY + 80, 100, 20, "Paste");
        buttonList.add(browseButton);
        buttonList.add(pasteButton);

         
        normalButton = new GuiButton(3, centerX - 155, startY + 110, 100, 20, "Normal");
        contrastButton = new GuiButton(4, centerX - 50, startY + 110, 100, 20, "High Contrast");
        brightButton = new GuiButton(5, centerX + 55, startY + 110, 100, 20, "Bright");

        normalButton.visible = false;
        contrastButton.visible = false;
        brightButton.visible = false;

        buttonList.add(normalButton);
        buttonList.add(contrastButton);
        buttonList.add(brightButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int centerX = width / 2;
        int centerY = height / 2;
        int startY = centerY - 70;

         
        drawCenteredString(fontRendererObj, "\u00A7l\u00A7eImage to NBT", centerX, startY - 20, 0xFFFFFF);

         
        drawString(fontRendererObj, "Item Name:", centerX - 100, startY, 0xA0A0A0);
        itemNameField.drawTextBox();

         
        drawString(fontRendererObj, "Item Type:", centerX - 100, startY + 38, 0xA0A0A0);
        itemTypeField.drawTextBox();

         
        if (!statusMessage.isEmpty()) {
            drawCenteredString(fontRendererObj, statusMessage, centerX, startY + 140, 0xFFFF55);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

         
        hoverPreview = null;
        if (showVariants) {
            if (normalButton.isMouseOver() && normalVariant != null) {
                hoverPreview = normalVariant;
            } else if (contrastButton.isMouseOver() && contrastVariant != null) {
                hoverPreview = contrastVariant;
            } else if (brightButton.isMouseOver() && brightVariant != null) {
                hoverPreview = brightVariant;
            }
        }

         
        if (hoverPreview != null) {
            drawTooltipPreview(mouseX, mouseY, hoverPreview);
        }
    }

     


    private void drawTooltipPreview(int mouseX, int mouseY, String[] lines) {
        if (lines == null || lines.length == 0)
            return;

        String itemName = convertColorCodes(itemNameField.getText().trim());
        if (itemName.isEmpty())
            itemName = "Custom_NBT";

        int previewX = mouseX + 12;
        int previewY = mouseY - 12;

        int maxWidth = fontRendererObj.getStringWidth(itemName);
        for (String line : lines) {
            int lineWidth = fontRendererObj.getStringWidth(line);
            if (lineWidth > maxWidth)
                maxWidth = lineWidth;
        }

        int maxLines = Math.min(200, lines.length);
        int lineHeight = 10;
        int totalHeight = (maxLines + 1) * lineHeight + 8;

        if (previewX + maxWidth + 8 > width) {
            previewX = mouseX - maxWidth - 20;
        }
        if (previewY + totalHeight > height) {
            previewY = height - totalHeight - 5;
        }
        if (previewY < 5)
            previewY = 5;

        int bgColor = 0xF0100010;
        int borderStart = 0x505000FF;
        int borderEnd = 0x5028007F;

        drawGradientRect(previewX - 3, previewY - 4, previewX + maxWidth + 3, previewY - 3, bgColor, bgColor);
        drawGradientRect(previewX - 3, previewY + totalHeight - 4, previewX + maxWidth + 3, previewY + totalHeight - 3,
                bgColor, bgColor);
        drawGradientRect(previewX - 3, previewY - 3, previewX + maxWidth + 3, previewY + totalHeight - 4, bgColor,
                bgColor);
        drawGradientRect(previewX - 4, previewY - 3, previewX - 3, previewY + totalHeight - 4, bgColor, bgColor);
        drawGradientRect(previewX + maxWidth + 3, previewY - 3, previewX + maxWidth + 4, previewY + totalHeight - 4,
                bgColor, bgColor);

        drawGradientRect(previewX - 3, previewY - 3, previewX - 2, previewY + totalHeight - 4, borderStart, borderEnd);
        drawGradientRect(previewX + maxWidth + 2, previewY - 3, previewX + maxWidth + 3, previewY + totalHeight - 4,
                borderStart, borderEnd);
        drawGradientRect(previewX - 3, previewY - 3, previewX + maxWidth + 3, previewY - 2, borderStart, borderStart);
        drawGradientRect(previewX - 3, previewY + totalHeight - 5, previewX + maxWidth + 3, previewY + totalHeight - 4,
                borderEnd, borderEnd);

        fontRendererObj.drawStringWithShadow(itemName, previewX, previewY, 0xFFFFFF);

        for (int i = 0; i < maxLines; i++) {
            fontRendererObj.drawStringWithShadow(lines[i], previewX, previewY + (i + 1) * lineHeight, 0xFFFFFF);
        }

        if (lines.length > maxLines) {
            fontRendererObj.drawStringWithShadow("\u00A77... (" + (lines.length - maxLines) + " more lines)",
                    previewX, previewY + (maxLines + 1) * lineHeight, 0x808080);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == browseButton) {
            openModernFilePicker();
        } else if (button == pasteButton) {
            pasteFromClipboard();
        } else if (button == normalButton && normalVariant != null) {
            giveItem(normalVariant);
        } else if (button == contrastButton && contrastVariant != null) {
            giveItem(contrastVariant);
        } else if (button == brightButton && brightVariant != null) {
            giveItem(brightVariant);
        }
    }

     


    private void openModernFilePicker() {
        browseButton.enabled = false;
        pasteButton.enabled = false;
        statusMessage = "Opening file picker...";

        new Thread(() -> {
            File selectedFile = null;
            try {
                 
                String psScript = "Add-Type -AssemblyName System.Windows.Forms; " +
                        "$dialog = New-Object System.Windows.Forms.OpenFileDialog; " +
                        "$dialog.Title = 'Select an Image'; " +
                        "$dialog.Filter = 'Image Files|*.png;*.jpg;*.jpeg;*.gif;*.bmp|All Files|*.*'; " +
                        "$dialog.InitialDirectory = '" + lastDirectory.replace("'", "''") + "'; " +
                        "if ($dialog.ShowDialog() -eq 'OK') { Write-Output $dialog.FileName }";

                ProcessBuilder pb = new ProcessBuilder(
                        "powershell", "-Command", psScript);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                process.waitFor();

                if (line != null && !line.isEmpty()) {
                    selectedFile = new File(line.trim());
                    if (selectedFile.exists() && selectedFile.getParentFile() != null) {
                        lastDirectory = selectedFile.getParentFile().getAbsolutePath();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final File finalFile = selectedFile;
            Minecraft.getMinecraft().addScheduledTask(() -> {
                browseButton.enabled = true;
                pasteButton.enabled = true;

                if (finalFile != null) {
                    processFile(finalFile);
                } else {
                    statusMessage = "";
                }
            });
        }).start();
    }

    private void pasteFromClipboard() {
        try {
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                Image image = (Image) transferable.getTransferData(DataFlavor.imageFlavor);

                BufferedImage buffered;
                if (image instanceof BufferedImage) {
                    buffered = (BufferedImage) image;
                } else {
                    buffered = new BufferedImage(
                            image.getWidth(null),
                            image.getHeight(null),
                            BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = buffered.createGraphics();
                    g.drawImage(image, 0, 0, null);
                    g.dispose();
                }

                selectedImage = buffered;
                statusMessage = "Generating...";
                generateVariants();
            } else if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                 
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (!files.isEmpty()) {
                    processFile(files.get(0));
                }
            } else {
                statusMessage = "\u00A7cNo image in clipboard!";
            }
        } catch (Exception e) {
            statusMessage = "\u00A7cFailed to paste: " + e.getMessage();
            e.printStackTrace();
        }
    }

     


    public void handleFileDrop(File file) {
        if (file != null && file.exists()) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                    name.endsWith(".gif") || name.endsWith(".bmp")) {
                processFile(file);
            }
        }
    }

    private void processFile(File file) {
        statusMessage = "Processing...";

        new Thread(() -> {
            try {
                selectedImage = javax.imageio.ImageIO.read(file);

                if (selectedImage == null) {
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        statusMessage = "\u00A7cFailed to read image!";
                    });
                    return;
                }

                Minecraft.getMinecraft().addScheduledTask(() -> {
                    statusMessage = "Generating...";
                    generateVariants();
                });
            } catch (Exception e) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    statusMessage = "\u00A7cError: " + e.getMessage();
                });
            }
        }).start();
    }

    private void generateVariants() {
        if (selectedImage == null)
            return;

        new Thread(() -> {
            try {
                normalVariant = ImageProcessor.processImageWithAdjustment(selectedImage, 0, 0);
                contrastVariant = ImageProcessor.processImageWithAdjustment(selectedImage, 30, 0);
                brightVariant = ImageProcessor.processImageWithAdjustment(selectedImage, 0, 40);

                Minecraft.getMinecraft().addScheduledTask(() -> {
                    showVariants = true;
                    normalButton.visible = true;
                    contrastButton.visible = true;
                    brightButton.visible = true;
                    statusMessage = "";
                });
            } catch (Exception e) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    statusMessage = "\u00A7cFailed to generate variants!";
                });
                e.printStackTrace();
            }
        }).start();
    }

     


    private void giveItem(String[] loreLines) {
        if (loreLines == null)
            return;

        lastItemName = itemNameField.getText().trim();
        if (lastItemName.isEmpty())
            lastItemName = "Custom_NBT";

        lastItemType = itemTypeField.getText().trim().toLowerCase();
        if (lastItemType.isEmpty())
            lastItemType = "paper";

        String coloredName = convertColorCodes(lastItemName);

        EntityPlayer player = Minecraft.getMinecraft().thePlayer;

        boolean success = ItemGenerator.giveImageItem(player, coloredName, lastItemType, loreLines);

        if (success) {
            player.addChatMessage(new net.minecraft.util.ChatComponentText(
                    "\u00A7a[Image to NBT] \u00A7fItem '" + coloredName + "\u00A7f' added!"));
        } else {
            player.addChatMessage(new net.minecraft.util.ChatComponentText(
                    "\u00A7c[Image to NBT] \u00A7fFailed to create item!"));
        }

        mc.displayGuiScreen(null);
    }

    private String convertColorCodes(String input) {
        if (input == null)
            return "";
        return input.replace("&", "\u00A7");
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }

        if (keyCode == Keyboard.KEY_V && isCtrlKeyDown()) {
            pasteFromClipboard();
            return;
        }

        if (keyCode == Keyboard.KEY_TAB) {
            if (itemNameField.isFocused()) {
                itemNameField.setFocused(false);
                itemTypeField.setFocused(true);
            } else {
                itemTypeField.setFocused(false);
                itemNameField.setFocused(true);
            }
            return;
        }

        itemNameField.textboxKeyTyped(typedChar, keyCode);
        itemTypeField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        itemNameField.mouseClicked(mouseX, mouseY, mouseButton);
        itemTypeField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        itemNameField.updateCursorCounter();
        itemTypeField.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        lastItemName = itemNameField.getText().trim();
        if (lastItemName.isEmpty())
            lastItemName = "Custom_NBT";
        lastItemType = itemTypeField.getText().trim().toLowerCase();
        if (lastItemType.isEmpty())
            lastItemType = "paper";
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

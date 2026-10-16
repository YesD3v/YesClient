package com.yesclient.imagetonbt;
import java.nio.file.Files;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ImageProcessor {
     
    private static final int MAX_NBT_BYTES = 6500;

     
     
    private static final int MAX_WIDTH = 200;
    private static final int MAX_HEIGHT = 100;

     
    private static final Map<Character, Integer> CHAR_WIDTHS = new HashMap<>();

    static {
         
        for (char c : "@#%&$WMB80QHNAXZUKRDGES965320CPYVbdpqhnmwuaegosczxvyrFTJL74+=*?/<>:;-_".toCharArray()) {
            CHAR_WIDTHS.put(c, 6);
        }
         
        for (char c : "fk".toCharArray()) {
            CHAR_WIDTHS.put(c, 5);
        }
         
        for (char c : "tI[](){}".toCharArray()) {
            CHAR_WIDTHS.put(c, 4);
        }
         
        CHAR_WIDTHS.put('l', 3);
         
        for (char c : "i!.,':;`".toCharArray()) {
            CHAR_WIDTHS.put(c, 2);
        }
         
        CHAR_WIDTHS.put('|', 1);
    }

     
     
    private static final char[] BRAILLE_CHARS;

    static {
         
        Character[] chars = new Character[256];
        for (int i = 0; i < 256; i++) {
            chars[i] = (char) (0x2800 + i);
        }

         
        Arrays.sort(chars, (a, b) -> {
            int bitsA = Integer.bitCount(a - 0x2800);
            int bitsB = Integer.bitCount(b - 0x2800);
            return bitsA - bitsB;
        });

        BRAILLE_CHARS = new char[256];
        for (int i = 0; i < 256; i++) {
            BRAILLE_CHARS[i] = chars[i];
        }
    }

     
    private static final int CELL_WIDTH = 2;

     
     
     
    private static final double CHAR_ASPECT_CORRECTION = 3.0;

    public static String[] processImage(File imageFile) {
        BufferedImage originalImage = null;
        File tempFile = null;

        String fileName = imageFile.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        String extension = ".png";
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            extension = fileName.substring(lastDotIndex).toLowerCase();
        }

        System.out.println("[ImageToNBT] File: " + fileName);

        try {
            if (imageFile.getAbsolutePath().toLowerCase().contains("onedrive")) {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                            "powershell", "-Command",
                            "Get-Content -Path '" + imageFile.getAbsolutePath() + "' -ReadCount 0 | Out-Null");
                    pb.redirectErrorStream(true);
                    pb.start().waitFor();
                } catch (Exception e) {
                }
            }

            tempFile = File.createTempFile("imagetonbt_", extension);
            tempFile.deleteOnExit();
            Files.copy(imageFile.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            originalImage = ImageIO.read(tempFile);

            if (originalImage == null) {
                System.err.println("[ImageToNBT] Failed to read image");
                return null;
            }
        } catch (Exception e) {
            System.err.println("[ImageToNBT] Error: " + e.getMessage());
            return null;
        } finally {
            if (tempFile != null && tempFile.exists())
                tempFile.delete();
        }

         
        return processImageWithAdjustment(originalImage, 0, 0);
    }

     



    private static int calculateByteSize(String[] lines) {
        if (lines == null)
            return 0;
        int total = 0;
        for (String line : lines) {
            if (line != null) {
                 
                total += 2 + line.getBytes(StandardCharsets.UTF_8).length;
            }
        }
         
         
        total += 50;
        return total;
    }

     



    private static String[] generateAtScale(BufferedImage originalImage, double scaleFactor,
            int contrastAdjust, int brightnessAdjust) {
        int srcW = originalImage.getWidth();
        int srcH = originalImage.getHeight();
        double imgRatio = (double) srcW / srcH;

         
        int baseHeight = (int) Math.round(MAX_HEIGHT * scaleFactor);
        int baseWidth = (int) Math.round(baseHeight * imgRatio * CHAR_ASPECT_CORRECTION);

         
        int targetWidth, targetHeight;
        if (baseWidth <= MAX_WIDTH * scaleFactor) {
            targetHeight = baseHeight;
            targetWidth = baseWidth;
        } else {
            targetWidth = (int) Math.round(MAX_WIDTH * scaleFactor);
            targetHeight = (int) Math.round(targetWidth / imgRatio / CHAR_ASPECT_CORRECTION);
        }

         
        targetWidth = Math.max(1, targetWidth);
        targetHeight = Math.max(1, targetHeight);

        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setComposite(AlphaComposite.Src);
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

         
        double contrastFactor = (100.0 + contrastAdjust) / 100.0;
        contrastFactor = contrastFactor * contrastFactor;

        String bgColor = calculateContrastingBgColor(scaled);
        String[] result = new String[targetHeight];

        for (int y = 0; y < targetHeight; y++) {
            StringBuilder line = new StringBuilder();
            String lastColor = null;

            for (int x = 0; x < targetWidth; x++) {
                int rgb = scaled.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                int r = (rgb >> 16) & 0xFF;
                int g2 = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                 
                r = Math.max(0, Math.min(255, r + brightnessAdjust));
                g2 = Math.max(0, Math.min(255, g2 + brightnessAdjust));
                b = Math.max(0, Math.min(255, b + brightnessAdjust));

                 
                r = (int) Math.max(0, Math.min(255, ((r - 128) * contrastFactor) + 128));
                g2 = (int) Math.max(0, Math.min(255, ((g2 - 128) * contrastFactor) + 128));
                b = (int) Math.max(0, Math.min(255, ((b - 128) * contrastFactor) + 128));

                String colorCode;
                char ch;

                if (alpha < 128) {
                    colorCode = bgColor;
                    ch = BRAILLE_CHARS[BRAILLE_CHARS.length - 1];
                } else {
                    colorCode = ColorMapper.getColorCode(r, g2, b);
                    ch = getDensityChar(r, g2, b);
                }

                if (!colorCode.equals(lastColor)) {
                    line.append(colorCode);
                    lastColor = colorCode;
                }
                line.append(ch);
            }

            result[y] = line.toString();
        }

        return result;
    }

     



    public static String[] processImageWithAdjustment(BufferedImage originalImage, int contrastAdjust,
            int brightnessAdjust) {
        if (originalImage == null)
            return null;

        try {
             
            double lowScale = 0.1;
            double highScale = 1.0;
            String[] bestResult = null;
            int bestSize = 0;

             
            String[] fullScale = generateAtScale(originalImage, highScale, contrastAdjust, brightnessAdjust);
            int fullSize = calculateByteSize(fullScale);

            if (fullSize <= MAX_NBT_BYTES) {
                System.out.println("[ImageToNBT] Full scale fits! Size: " + fullSize + " bytes, dims: " +
                        fullScale[0].length() + "x" + fullScale.length);
                return fullScale;
            }

             
            for (int i = 0; i < 10; i++) {
                double midScale = (lowScale + highScale) / 2.0;
                String[] result = generateAtScale(originalImage, midScale, contrastAdjust, brightnessAdjust);
                int size = calculateByteSize(result);

                if (size <= MAX_NBT_BYTES) {
                     
                    bestResult = result;
                    bestSize = size;
                    lowScale = midScale;
                } else {
                     
                    highScale = midScale;
                }
            }

             
            if (bestResult == null) {
                bestResult = generateAtScale(originalImage, lowScale, contrastAdjust, brightnessAdjust);
                bestSize = calculateByteSize(bestResult);
            }

            System.out.println("[ImageToNBT] Dynamic size: " + bestSize + " bytes (limit: " + MAX_NBT_BYTES +
                    "), dims: " + (bestResult.length > 0 ? bestResult[0].length() : 0) + "x" + bestResult.length);

            return bestResult;

        } catch (Exception e) {
            System.err.println("[ImageToNBT] Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

     


    public static String[][] processImageWithSize(BufferedImage originalImage, int maxWidth, int maxHeight) {
        if (originalImage == null)
            return null;

         
        String[] lines = processImageWithAdjustment(originalImage, 0, 0);
        if (lines == null)
            return null;

        String[][] result = new String[lines.length][1];
        for (int i = 0; i < lines.length; i++) {
            result[i][0] = lines[i];
        }
        return result;
    }

    private static int getCharWidth(char c) {
        return CHAR_WIDTHS.getOrDefault(c, 6);
    }

    private static char getDensityChar(int r, int g, int b) {
        double lum = 0.299 * r + 0.587 * g + 0.114 * b;
        double ratio = lum / 255.0;
        int idx = (int) Math.floor(ratio * (BRAILLE_CHARS.length - 1));
        idx = Math.max(0, Math.min(idx, BRAILLE_CHARS.length - 1));
        return BRAILLE_CHARS[idx];
    }

    private static String calculateContrastingBgColor(BufferedImage img) {
        long tr = 0, tg = 0, tb = 0;
        int cnt = 0;

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                if (((rgb >> 24) & 0xFF) >= 128) {
                    tr += (rgb >> 16) & 0xFF;
                    tg += (rgb >> 8) & 0xFF;
                    tb += rgb & 0xFF;
                    cnt++;
                }
            }
        }

        if (cnt == 0)
            return "\u00A7f";
        double lum = 0.299 * (tr / cnt) + 0.587 * (tg / cnt) + 0.114 * (tb / cnt);
        return lum > 128 ? "\u00A70" : "\u00A7f";
    }
}

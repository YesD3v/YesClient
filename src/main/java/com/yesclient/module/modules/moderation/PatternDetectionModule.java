package com.yesclient.module.modules.moderation;
import java.util.Comparator;
import java.util.List;
import java.awt.Color;
import java.util.Iterator;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.NumberSetting;
import com.yesclient.utils.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PatternDetectionModule extends Module {

    private static final double MIN_BACKGROUND_AIR_RATIO = 0.65D;
    private static final int MAX_CHANGED_BLOCKS_PER_TICK = 2;
    private static final int MAX_FOUND_VALIDATIONS_PER_TICK = 12;
    private static final int MAX_FOUND_PATTERNS = 128;
    private static final int MAX_QUEUED_BLOCKS = 4096;

    private final BooleanSetting chatNotifications = new BooleanSetting("Chat Notifications", "Send chat alerts when found", true);
    private final BooleanSetting esp = new BooleanSetting("ESP", "Highlight detected patterns", true);
    private final BooleanSetting tracers = new BooleanSetting("Tracers", "Draw lines to detected patterns", true);
    private final NumberSetting range = new NumberSetting("Range", "Scan range in blocks", 64, 16, 128, 1);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", "Width of ESP lines", 2.0, 0.5, 5.0, 0.5);

    private final List<ShapeDef> shapes = new ArrayList<>();
    private final List<Match> foundPatterns = new ArrayList<>();
    private final List<Reported> reportedOrigins = new ArrayList<>();
    private int foundValidationIndex;

    public PatternDetectionModule() {
        super("Pattern Detection", "Detects inappropriate block builds", Category.MODERATION, ModuleMode.BOTH);

        addSetting(chatNotifications);
        addSetting(esp);
        addSetting(tracers);
        addSetting(range);
        addSetting(lineWidth);

        initShapes();
    }

    private void initShapes() {
         
         
         
        String[] swastikaV1 = {
                ".......",
                ".X.XXX.",
                ".X.X...",
                ".XXXXX.",
                "...X.X.",
                ".XXX.X."
        };
        String[] swastikaV2 = {
                ".......",
                ".XXX.X.",
                "...X.X.",
                ".XXXXX.",
                ".X.X...",
                ".X.XXX."
        };
        String[] swastikaV3 = {  
                ".......",
                "...XXX.",
                "...X...",
                "...XXX.",
                "...X.X.",
                ".XXX.X."
        };
        addVerticalShape("Swastika", swastikaV1);
        addVerticalShape("Swastika", swastikaV2);
        addVerticalShape("Swastika", swastikaV3);

         
         
         
        String[] swastikaH1 = {
                ".......",
                ".X.XXX.",
                ".X.X...",
                ".XXXXX.",
                "...X.X.",
                ".XXX.X.",
                "......."
        };
        String[] swastikaH2 = {
                ".......",
                ".XXX.X.",
                "...X.X.",
                ".XXXXX.",
                ".X.X...",
                ".X.XXX.",
                "......."
        };
        addHorizontalShape("Swastika", swastikaH1);
        addHorizontalShape("Swastika", swastikaH2);

         
         
         
        String[] ppV5 = { ".....", "..X..", "..X..", "..X..", "..X..", ".XXX." };  
        String[] ppV7 = { ".....", "..X..", "..X..", "..X..", "..X..", "..X..", ".XXX." };  
        addVerticalShape("PP", ppV5);
        addVerticalShape("PP", ppV7);

         
         
         
        String[] ppH5 = { ".....", "..X..", "..X..", "..X..", "..X..", ".XXX.", "....." };
        String[] ppH7 = { ".....", "..X..", "..X..", "..X..", "..X..", "..X..", ".XXX.", "....." };
        addHorizontalShape("PP", ppH5);
        addHorizontalShape("PP", ppH7);
    }

    private void addVerticalShape(String name, String[] pattern) {
        int h = pattern.length;
        int w = pattern[0].length();

        ShapeDef shapeX = new ShapeDef(name);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                char c = pattern[h - 1 - y].charAt(x);
                if (c == 'X') shapeX.solid.add(new BlockPos(x, y, 0));
                else if (c == '.') shapeX.air.add(new BlockPos(x, y, 0));
            }
        }
        shapeX.calculateBoundsAndAnchor();
        shapes.add(shapeX);

        ShapeDef shapeZ = new ShapeDef(name);
        for (int y = 0; y < h; y++) {
            for (int z = 0; z < w; z++) {
                char c = pattern[h - 1 - y].charAt(z);
                if (c == 'X') shapeZ.solid.add(new BlockPos(0, y, z));
                else if (c == '.') shapeZ.air.add(new BlockPos(0, y, z));
            }
        }
        shapeZ.calculateBoundsAndAnchor();
        shapes.add(shapeZ);
    }
    private void addHorizontalShape(String name, String[] pattern) {
        int h = pattern.length;
        int w = pattern[0].length();

         
        ShapeDef shape0 = new ShapeDef(name);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                if (pattern[z].charAt(x) == 'X') shape0.solid.add(new BlockPos(x, 0, z));
                else if (pattern[z].charAt(x) == '.') shape0.air.add(new BlockPos(x, 0, z));
            }
        }
        shape0.calculateBoundsAndAnchor();
        shapes.add(shape0);

         
        ShapeDef shape90 = new ShapeDef(name);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                if (pattern[z].charAt(x) == 'X') shape90.solid.add(new BlockPos(-z, 0, x));
                else if (pattern[z].charAt(x) == '.') shape90.air.add(new BlockPos(-z, 0, x));
            }
        }
        shape90.calculateBoundsAndAnchor();
        shapes.add(shape90);

         
        ShapeDef shape180 = new ShapeDef(name);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                if (pattern[z].charAt(x) == 'X') shape180.solid.add(new BlockPos(-x, 0, -z));
                else if (pattern[z].charAt(x) == '.') shape180.air.add(new BlockPos(-x, 0, -z));
            }
        }
        shape180.calculateBoundsAndAnchor();
        shapes.add(shape180);

         
        ShapeDef shape270 = new ShapeDef(name);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                if (pattern[z].charAt(x) == 'X') shape270.solid.add(new BlockPos(z, 0, -x));
                else if (pattern[z].charAt(x) == '.') shape270.air.add(new BlockPos(z, 0, -x));
            }
        }
        shape270.calculateBoundsAndAnchor();
        shapes.add(shape270);
    }

    @Override
    protected void onEnable() {
        foundPatterns.clear();
        reportedOrigins.clear();
        blocksToScan.clear();
        foundValidationIndex = 0;
    }

    @Override
    protected void onDisable() {
        foundPatterns.clear();
        reportedOrigins.clear();
        blocksToScan.clear();
        foundValidationIndex = 0;
    }

    private final Set<BlockPos> blocksToScan = ConcurrentHashMap.newKeySet();

    public void onBlockChange(BlockPos pos, net.minecraft.block.state.IBlockState state) {
        if (mc.thePlayer == null || state == null || !isSolidBlock(state.getBlock())) return;
        if (pos.distanceSq(mc.thePlayer.getPosition()) <= range.getIntValue() * range.getIntValue()
                && (blocksToScan.contains(pos) || blocksToScan.size() < MAX_QUEUED_BLOCKS)) {
            blocksToScan.add(pos);
        }
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

         
         
         
         
         
        validateFoundPatterns();

        if (blocksToScan.isEmpty()) return;

        Iterator<BlockPos> it = blocksToScan.iterator();
        int scannedCount = 0;

        while (it.hasNext() && scannedCount < MAX_CHANGED_BLOCKS_PER_TICK) {
            BlockPos changedPos = it.next();
            it.remove();

            for (ShapeDef shape : shapes) {
                if (shape.anchorOffset == null) continue;
                for (BlockPos offset : shape.solid) {
                    BlockPos origin = changedPos.add(-offset.getX(), -offset.getY(), -offset.getZ());
                    if (matchesPattern(origin, shape)) {
                        handleMatch(origin, shape);
                    }
                }
            }
            scannedCount++;
        }
    }

    private void validateFoundPatterns() {
        int checks = Math.min(MAX_FOUND_VALIDATIONS_PER_TICK, foundPatterns.size());
        for (int i = 0; i < checks && !foundPatterns.isEmpty(); i++) {
            if (foundValidationIndex >= foundPatterns.size()) foundValidationIndex = 0;
            Match match = foundPatterns.get(foundValidationIndex);
            if (!matchesPattern(match.origin, match.shape)) {
                foundPatterns.remove(foundValidationIndex);
            } else {
                foundValidationIndex++;
            }
        }
    }

    private boolean matchesPattern(BlockPos origin, ShapeDef shape) {
        Block shapeBlock = null;
        boolean uniform = true;
        for (BlockPos offset : shape.solid) {
            BlockPos pos = origin.add(offset);
            if (pos.getY() < 0 || pos.getY() > 255 || !mc.theWorld.isBlockLoaded(pos)) return false;
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            if (!isSolidBlock(block)) return false;
            if (shapeBlock == null) shapeBlock = block;
            else if (shapeBlock != block) uniform = false;
        }

        int backgroundAir = 0;
        boolean backgroundMatchesMaterial = false;
        for (BlockPos offset : shape.air) {
            BlockPos pos = origin.add(offset);
            if (pos.getY() < 0 || pos.getY() > 255 || !mc.theWorld.isBlockLoaded(pos)) return false;
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            if (isAirBlock(block)) backgroundAir++;
            else if (block == shapeBlock) backgroundMatchesMaterial = true;
        }
        boolean standalone = backgroundAir == shape.air.size();
        return standalone || (uniform && !backgroundMatchesMaterial
                && !shape.air.isEmpty()
                && backgroundAir / (double) shape.air.size() >= MIN_BACKGROUND_AIR_RATIO);
    }

    private boolean isSolidBlock(Block block) {
        return block != net.minecraft.init.Blocks.air
                && !block.getMaterial().isLiquid()
                && !block.getMaterial().isReplaceable();
    }

    private boolean isAirBlock(Block block) {
        return block == net.minecraft.init.Blocks.air
                || block.getMaterial().isReplaceable();
    }

    private void handleMatch(BlockPos pos, ShapeDef shape) {
        if (foundPatterns.size() >= MAX_FOUND_PATTERNS) return;
        for (Match match : foundPatterns) {
            if (match.origin.distanceSq(pos) < 9) {
                return;  
            }
        }

         
        boolean recentlyReported = false;
        for (Reported reported : reportedOrigins) {
            if (reported.pos.distanceSq(pos) < 9 && (System.currentTimeMillis() - reported.time) < 300000) {
                recentlyReported = true;
                break;
            }
        }

        if (!recentlyReported) {
            reportedOrigins.add(new Reported(pos, System.currentTimeMillis()));
            sendHoverMessage(pos, shape.name);
        }
        
        AxisAlignedBB bb = new AxisAlignedBB(
                pos.getX() + shape.minX, pos.getY() + shape.minY, pos.getZ() + shape.minZ,
                pos.getX() + shape.maxX + 1, pos.getY() + shape.maxY + 1, pos.getZ() + shape.maxZ + 1
        );
        
        foundPatterns.add(new Match(pos, shape, bb));
    }

    private void sendHoverMessage(BlockPos pos, String shapeName) {
        if (!chatNotifications.isEnabled()) return;

        List<net.minecraft.entity.player.EntityPlayer> players = new ArrayList<>(mc.theWorld.playerEntities);
        players.sort(Comparator.comparingDouble(p -> p.getDistanceSq(pos)));

        StringBuilder hoverText = new StringBuilder("\u00A7eClosest Players:\n");
        int count = 0;
        for (net.minecraft.entity.player.EntityPlayer p : players) {
            if (count >= 5) break;
            double dist = Math.sqrt(p.getDistanceSq(pos));
            hoverText.append("\u00A77- \u00A7f").append(p.getName())
                     .append(" \u00A77(").append(String.format("%.1f", dist)).append("m)\n");
            count++;
        }
        if (count == 0) {
            hoverText.append("\u00A77None");
        } else {
            hoverText.setLength(hoverText.length() - 1);
        }

        net.minecraft.util.ChatComponentText message = new net.minecraft.util.ChatComponentText(
                "\u00A7c[Moderation] \u00A7fDetected \u00A7c" + shapeName + " \u00A7f" +
                "at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
        );

        net.minecraft.util.ChatStyle style = new net.minecraft.util.ChatStyle();
        style.setChatHoverEvent(new net.minecraft.event.HoverEvent(
                net.minecraft.event.HoverEvent.Action.SHOW_TEXT,
                new net.minecraft.util.ChatComponentText(hoverText.toString())
        ));
        message.setChatStyle(style);

        mc.thePlayer.addChatMessage(message);
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (mc.thePlayer == null || mc.theWorld == null || foundPatterns.isEmpty()) return;
        if (!esp.isEnabled() && !tracers.isEnabled()) return;

        Color outlineColor = new Color(255, 50, 50, 255);
        Color fillColor = new Color(255, 50, 50, 100);
        float width = lineWidth.getFloatValue();

        double renderPosX = mc.getRenderManager().viewerPosX;
        double renderPosY = mc.getRenderManager().viewerPosY;
        double renderPosZ = mc.getRenderManager().viewerPosZ;

        for (Match match : foundPatterns) {
            AxisAlignedBB bb = match.bounds;
            AxisAlignedBB renderBB = new AxisAlignedBB(
                    bb.minX - renderPosX, bb.minY - renderPosY, bb.minZ - renderPosZ,
                    bb.maxX - renderPosX, bb.maxY - renderPosY, bb.maxZ - renderPosZ
            );

            if (esp.isEnabled()) {
                RenderUtils.drawFilledBox(renderBB, fillColor);
                RenderUtils.drawOutlinedBox(renderBB, outlineColor, width);
            }

            if (tracers.isEnabled()) {
                double centerX = renderBB.minX + (renderBB.maxX - renderBB.minX) / 2.0;
                double centerY = renderBB.minY + (renderBB.maxY - renderBB.minY) / 2.0;
                double centerZ = renderBB.minZ + (renderBB.maxZ - renderBB.minZ) / 2.0;
                net.minecraft.util.Vec3 startPos = RenderUtils.getCrosshairPos(partialTicks);
                net.minecraft.client.renderer.entity.RenderManager renderManager = mc.getRenderManager();
                RenderUtils.drawLine(startPos.xCoord - renderManager.viewerPosX,
                        startPos.yCoord - renderManager.viewerPosY, startPos.zCoord - renderManager.viewerPosZ,
                        centerX, centerY, centerZ, outlineColor.getRGB(), width);
            }
        }
    }

    @Override
    public String getDisplayInfo() {
        if (foundPatterns.isEmpty()) return null;
        return foundPatterns.size() + " Found";
    }

    private static class ShapeDef {
        String name;
        List<BlockPos> solid = new ArrayList<>();
        List<BlockPos> air = new ArrayList<>();
        int minX, minY, minZ, maxX, maxY, maxZ;
        BlockPos anchorOffset;

        public ShapeDef(String name) { this.name = name; }

        public void calculateBoundsAndAnchor() {
            minX = minY = minZ = Integer.MAX_VALUE;
            maxX = maxY = maxZ = Integer.MIN_VALUE;
            for (BlockPos p : solid) {
                minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
                maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
            }
            if (!solid.isEmpty()) anchorOffset = solid.get(0);
        }
    }

    private static class Match {
        BlockPos origin;
        String name;
        ShapeDef shape;
        AxisAlignedBB bounds;

        public Match(BlockPos origin, ShapeDef shape, AxisAlignedBB bounds) {
            this.origin = origin; this.name = shape.name; this.shape = shape; this.bounds = bounds;
        }
    }

    private static class Reported {
        BlockPos pos;
        long time;
        public Reported(BlockPos pos, long time) {
            this.pos = pos; this.time = time;
        }
    }
}

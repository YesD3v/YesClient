package com.yesclient.module.modules.visuals;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ColorSetting;
import com.yesclient.module.settings.ItemSetting;
import com.yesclient.module.settings.NumberSetting;
import com.yesclient.utils.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

public class SearchModule extends Module {

    private static final int SCAN_BUDGET_PER_TICK = 8192;
    private static final int MAX_TRACKED_BLOCKS = 2048;
    private static final int MAX_RENDERED_BLOCKS = 256;

    private final ItemSetting blockToSearch = new ItemSetting("Block", "Block to search for", true, false);
    private final NumberSetting range = new NumberSetting("Range", "Search range in blocks", 32, 8, 100);
    private final BooleanSetting filled = new BooleanSetting("Filled", "Render filled boxes", true);
    private final BooleanSetting outline = new BooleanSetting("Outline", "Render outlines", true);
    private final BooleanSetting tracers = new BooleanSetting("Tracers", "Draw lines to blocks", false);
    private final ColorSetting highlightColor = new ColorSetting("Color", "Highlight color",
            new Color(255, 200, 0, 150));
    private final NumberSetting lineWidth = new NumberSetting("Line Width", "Width of outline", 2.0, 0.5, 5.0, 0.5);

    private final Set<BlockPos> foundBlocks = new LinkedHashSet<>();
    private int lastBlockId = -1;
    private int lastMeta = -1;
    private BlockPos scanCenter;
    private long scanIndex;
    private long scanVolume;
    private int scanRange = -1;
    private int scanVerticalRange;
    private net.minecraft.world.World scannedWorld;

    public SearchModule() {
        super("Search", "Highlight blocks in the world", Category.VISUALS, ModuleMode.BOTH);
        addSetting(blockToSearch);
        addSetting(range);
        addSetting(filled);
        addSetting(outline);
        addSetting(tracers);
        addSetting(highlightColor);
        addSetting(lineWidth);
    }

    @Override
    public String getDisplayName() {
        return "Block ESP";
    }

    @Override
    protected void onEnable() {
        resetScan(true);
    }

    @Override
    protected void onDisable() {
        foundBlocks.clear();
        resetScan(false);
    }

    private void resetScan(boolean clearMatches) {
        if (clearMatches) foundBlocks.clear();
        scanCenter = null;
        scanIndex = 0;
        scanVolume = 0;
        scanRange = -1;
        scannedWorld = null;
    }

    private int[] getTargetBlockInfo() {
        ItemStack stack = blockToSearch.getValue();
        if (stack == null || !(stack.getItem() instanceof ItemBlock)) return null;
        Block block = Block.getBlockFromItem(stack.getItem());
        return block == null ? null : new int[] { Block.getIdFromBlock(block), stack.getMetadata() };
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        int[] info = getTargetBlockInfo();
        if (info == null || info[0] <= 0) {
            foundBlocks.clear();
            resetScan(false);
            return;
        }

        int radius = range.getIntValue();
        int verticalRadius = Math.max(8, radius / 2);
        BlockPos playerPos = mc.thePlayer.getPosition();
        boolean targetChanged = info[0] != lastBlockId || info[1] != lastMeta;
        boolean centerChanged = scanCenter == null || scanCenter.distanceSq(playerPos) > 4.0D;
        boolean scanChanged = scanRange != radius || scanVerticalRange != verticalRadius || scannedWorld != mc.theWorld;

        if (targetChanged || centerChanged || scanChanged) {
            if (targetChanged || scannedWorld != mc.theWorld) foundBlocks.clear();
            lastBlockId = info[0];
            lastMeta = info[1];
            scanCenter = playerPos;
            scanRange = radius;
            scanVerticalRange = verticalRadius;
            scannedWorld = mc.theWorld;
            scanIndex = 0;
            long diameter = radius * 2L + 1L;
            scanVolume = diameter * diameter * (verticalRadius * 2L + 1L);
        }

         
         
        for (int checked = 0; checked < SCAN_BUDGET_PER_TICK && scanIndex < scanVolume; checked++, scanIndex++) {
            BlockPos pos = positionForIndex(scanIndex, radius, verticalRadius, scanCenter);
            if (!mc.theWorld.isBlockLoaded(pos)) continue;
            net.minecraft.block.state.IBlockState state = mc.theWorld.getBlockState(pos);
            Block block = state.getBlock();
            if (Block.getIdFromBlock(block) == info[0] && block.getMetaFromState(state) == info[1]) {
                if (foundBlocks.size() < MAX_TRACKED_BLOCKS || foundBlocks.contains(pos)) foundBlocks.add(pos);
            }
        }

         
         
        Iterator<BlockPos> iterator = foundBlocks.iterator();
        double maxDistanceSq = radius * (double) radius;
        while (iterator.hasNext()) {
            if (iterator.next().distanceSq(playerPos) > maxDistanceSq) iterator.remove();
        }
    }

    private BlockPos positionForIndex(long index, int radius, int verticalRadius, BlockPos center) {
        int xSize = radius * 2 + 1;
        int ySize = verticalRadius * 2 + 1;
        int yIndex = (int) (index % ySize);
        int zIndex = (int) ((index / ySize) % xSize);
        int xIndex = (int) (index / (ySize * (long) xSize));
        return center.add(centerOut(xIndex, radius), centerOut(yIndex, verticalRadius), centerOut(zIndex, radius));
    }

    private int centerOut(int index, int radius) {
        if (index == 0) return 0;
        int distance = (index + 1) / 2;
        return (index & 1) == 1 ? distance : -distance;
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (mc.thePlayer == null || mc.theWorld == null || foundBlocks.isEmpty()) return;

        Color color = highlightColor.getValue();
        Color outlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 2);
        float baseLineWidth = lineWidth.getFloatValue();
        double playerX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double playerY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
        double playerZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;
        double maxDistanceSq = range.getIntValue() * (double) range.getIntValue();
        int rendered = 0;

        Iterator<BlockPos> matches = foundBlocks.iterator();
        while (matches.hasNext()) {
            BlockPos pos = matches.next();
            if (!mc.theWorld.isBlockLoaded(pos)) {
                matches.remove();
                continue;
            }
            net.minecraft.block.state.IBlockState currentState = mc.theWorld.getBlockState(pos);
            Block currentBlock = currentState.getBlock();
            if (Block.getIdFromBlock(currentBlock) != lastBlockId || currentBlock.getMetaFromState(currentState) != lastMeta) {
                matches.remove();
                continue;
            }
            if (rendered >= MAX_RENDERED_BLOCKS) break;
            double dx = pos.getX() + 0.5 - playerX;
            double dy = pos.getY() + 0.5 - playerY;
            double dz = pos.getZ() + 0.5 - playerZ;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > maxDistanceSq) continue;
            float distance = (float) Math.sqrt(distanceSq);
            float scaledLineWidth = (float) Math.min(baseLineWidth * 4, Math.max(0.5, baseLineWidth * (8.0 / Math.max(distance, 1.0))));

            if (filled.isEnabled()) RenderUtils.drawBlockESPFilled(pos, fillColor, outlineColor, scaledLineWidth);
            else if (outline.isEnabled()) RenderUtils.drawBlockESP(pos, outlineColor, scaledLineWidth);

            if (tracers.isEnabled()) {
                RenderManager renderManager = mc.getRenderManager();
                Vec3 start = RenderUtils.getCrosshairPos(partialTicks);
                RenderUtils.drawLine(
                        start.xCoord - renderManager.viewerPosX,
                        start.yCoord - renderManager.viewerPosY,
                        start.zCoord - renderManager.viewerPosZ,
                        pos.getX() + 0.5D - renderManager.viewerPosX,
                        pos.getY() + 0.5D - renderManager.viewerPosY,
                        pos.getZ() + 0.5D - renderManager.viewerPosZ,
                        outlineColor.getRGB(), scaledLineWidth);
            }
            rendered++;
        }
    }

    @Override
    public String getDisplayInfo() {
        int[] info = getTargetBlockInfo();
        if (info == null) return "None";
        Block block = Block.getBlockById(info[0]);
        String blockName = block != null ? block.getLocalizedName() : "Unknown";
        return foundBlocks.size() + " (" + blockName + ")";
    }
}

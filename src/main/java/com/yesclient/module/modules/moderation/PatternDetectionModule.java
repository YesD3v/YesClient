package com.yesclient.module.modules.moderation;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.NumberSetting;
import com.yesclient.utils.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 * Finds flat block drawings of known offensive shapes.
 *
 * How it works:
 *  - Every loaded chunk in range is scanned once (and again if the server resends it), plus every block
 *    change is rescanned around the changed position.
 *  - For each solid block, the connected same-material region is flood-filled inside each of the three
 *    axis planes (XY, ZY, XZ). "Same material" includes colour, so black wool on a white wool wall counts.
 *  - The region's exact shape is looked up in a precomputed table of every size / thickness / rotation /
 *    mirror of each template. The region must equal a template exactly - no extra or missing blocks.
 *  - The match must also stand out from its surroundings (no same-material block in the gaps or the
 *    1-block border) and be a flat drawing rather than a slice through a 3D object.
 */
public class PatternDetectionModule extends Module {

    private static final int MAX_SPAN = 32;
    private static final int MIN_CELLS = 5;
    private static final int SMALL_PP_CELLS = 6;
    private static final int MAX_EXTRUSION = 3;

    private static final int SIDE_INVALID = 0;
    private static final int SIDE_OPEN = 1;
    private static final int SIDE_BACKED = 2;
    private static final int MAX_FOUND_PATTERNS = 128;
    private static final int MAX_DIRTY_POSITIONS = 8192;
    private static final int MAX_VALIDATIONS_PER_TICK = 16;
    private static final int DISCOVERY_INTERVAL_TICKS = 10;
    private static final long REPORT_COOLDOWN_MS = 10 * 60 * 1000L;

    private static final int KEY_UNLOADED = -1;
    private static final int KEY_NONE = 0;

    private static final byte VISITED_SMALL = 1;
    private static final byte VISITED_BIG = 2;

    private static final int PLANE_XY = 0;
    private static final int PLANE_ZY = 1;
    private static final int PLANE_XZ = 2;

    private static final String SWASTIKA = "Swastika";
    private static final String PP = "PP";

    private static final int[][] NEIGHBORS_4 = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
    private static final int[][] NEIGHBORS_8 = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
            { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };

    private final BooleanSetting detectSwastikas = new BooleanSetting("Swastikas", "Detect swastikas (any size, thickness, rotation, incl. 45 degree)", true);
    private final BooleanSetting detectPP = new BooleanSetting("PP", "Detect PP shapes", true);
    private final BooleanSetting chatNotifications = new BooleanSetting("Chat Notifications", "Send chat alerts when found", true);
    private final BooleanSetting esp = new BooleanSetting("ESP", "Highlight detected patterns", true);
    private final BooleanSetting tracers = new BooleanSetting("Tracers", "Draw lines to detected patterns", true);
    private final NumberSetting range = new NumberSetting("Range", "Scan range in blocks", 96, 16, 256, 1);
    private final NumberSetting scanBudget = new NumberSetting("Scan Budget", "Milliseconds per tick spent scanning", 4, 1, 15, 1);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", "Width of ESP lines", 2.0, 0.5, 5.0, 0.5);

    /** Exact shape tables, one for 4-connected (axis aligned) and one for 8-connected (45 degree) shapes. */
    private final Map<ShapeKey, String> cardinalTemplates = new HashMap<>();
    private final Map<ShapeKey, String> diagonalTemplates = new HashMap<>();
    /** Template orientations that are unrotated (a PP with its base at the bottom). */
    private final Set<ShapeKey> uprightTemplates = new HashSet<>();
    /** PP bodies whose shaft tip is a different block (like PP Builder's preset), keyed by the body's shape. */
    private final Map<ShapeKey, List<TipTemplate>> tippedPPTemplates = new HashMap<>();
    private int maxTemplateCells;

    private final IdentityHashMap<IBlockState, Integer> materialKeys = new IdentityHashMap<>();

    private final List<Match> foundPatterns = new ArrayList<>();
    private final Map<String, Long> reportedAt = new HashMap<>();
    private int validationIndex;

    private final Map<Long, Chunk> scannedChunks = new HashMap<>();
    private final ArrayDeque<int[]> sectionQueue = new ArrayDeque<>();
    private final Set<Long> queuedSections = new HashSet<>();
    private final LinkedHashSet<BlockPos> dirtyPositions = new LinkedHashSet<>();
    private SectionJob currentJob;
    private WorldClient lastWorld;
    private int tickCounter;

    // Flood-fill scratch space, reused between calls (everything runs on the client thread).
    private int[] queueU = new int[0];
    private int[] queueV = new int[0];
    private final Set<Long> component = new HashSet<>();

    private Chunk cachedChunk;
    private int cachedChunkX = Integer.MIN_VALUE;
    private int cachedChunkZ = Integer.MIN_VALUE;

    public PatternDetectionModule() {
        super("Pattern Detection", "Detects inappropriate block builds", Category.MODERATION, ModuleMode.BOTH);

        addSetting(detectSwastikas);
        addSetting(detectPP);
        addSetting(chatNotifications);
        addSetting(esp);
        addSetting(tracers);
        addSetting(range);
        addSetting(scanBudget);
        addSetting(lineWidth);

        buildTemplates();
    }

    // ------------------------------------------------------------------------------------------------
    // Templates
    // ------------------------------------------------------------------------------------------------

    private void buildTemplates() {
        // Axis aligned swastikas: arm length L, hook length H, stroke width t, gap unit width g.
        for (int armLength = 2; armLength <= 8; armLength++) {
            for (int hookLength = 2; hookLength <= armLength; hookLength++) {
                List<int[]> skeleton = swastikaSkeleton(armLength, hookLength, false);
                for (int stroke = 1; stroke <= 4; stroke++) {
                    for (int gap = 1; gap <= 4; gap++) {
                        if (3 * stroke + (2 * armLength - 2) * gap > MAX_SPAN) continue;
                        registerAllSymmetries(cardinalTemplates, expandBands(skeleton, armLength, stroke, gap), SWASTIKA);
                    }
                }
            }
        }

        // 45 degree swastikas (like the flag version), single block diagonal strokes.
        for (int armLength = 2; armLength <= 8; armLength++) {
            for (int hookLength = 2; hookLength <= armLength; hookLength++) {
                if (2 * (armLength + hookLength) + 1 > MAX_SPAN) continue;
                registerAllSymmetries(diagonalTemplates, swastikaSkeleton(armLength, hookLength, true), SWASTIKA);
            }
        }

        // PP shapes: shaft width w and length s on top of a base ("balls") of width w + 2b and height hb.
        for (int shaftWidth = 1; shaftWidth <= 3; shaftWidth++) {
            for (int ballWidth = 1; ballWidth <= shaftWidth; ballWidth++) {
                for (int baseHeight = ballWidth; baseHeight <= ballWidth + 1; baseHeight++) {
                    // Total height 3..10 blocks for a 1-wide shaft, scaled up for thicker ones.
                    for (int shaftLength = 2 * shaftWidth; shaftLength + baseHeight <= 10 * shaftWidth; shaftLength++) {
                        if (shaftLength + baseHeight > MAX_SPAN) continue;
                        List<int[]> cells = ppCells(shaftWidth, shaftLength, ballWidth, baseHeight);
                        registerAllSymmetries(cardinalTemplates, cells, PP);
                        for (int tipLength = 1; tipLength < shaftLength && tipLength <= Math.max(1, shaftLength / 2); tipLength++) {
                            registerTippedSymmetries(cells, baseHeight + shaftLength - tipLength);
                        }
                    }
                }
            }
        }
    }

    /** Arms from the centre in four directions, each ending in a hook turned the same rotational way. */
    private static List<int[]> swastikaSkeleton(int armLength, int hookLength, boolean diagonal) {
        int[][] arms = diagonal
                ? new int[][] { { 1, 1 }, { 1, -1 }, { -1, -1 }, { -1, 1 } }
                : new int[][] { { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 } };
        Set<Long> seen = new HashSet<>();
        List<int[]> cells = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int[] arm = arms[i];
            int[] hook = arms[(i + 1) % 4];
            for (int k = 0; k <= armLength; k++) {
                addCell(cells, seen, arm[0] * k, arm[1] * k);
            }
            for (int k = 1; k <= hookLength; k++) {
                addCell(cells, seen, arm[0] * armLength + hook[0] * k, arm[1] * armLength + hook[1] * k);
            }
        }
        return normalize(cells);
    }

    /**
     * Stretches a unit skeleton (spanning 0..2L) so stroke rows/columns (0, L, 2L) are {@code stroke} wide and
     * every other row/column is {@code gap} wide.
     */
    private static List<int[]> expandBands(List<int[]> skeleton, int armLength, int stroke, int gap) {
        int size = 2 * armLength + 1;
        int[] offset = new int[size];
        int[] width = new int[size];
        int position = 0;
        for (int i = 0; i < size; i++) {
            width[i] = (i == 0 || i == armLength || i == 2 * armLength) ? stroke : gap;
            offset[i] = position;
            position += width[i];
        }
        List<int[]> cells = new ArrayList<>();
        for (int[] cell : skeleton) {
            for (int du = 0; du < width[cell[0]]; du++) {
                for (int dv = 0; dv < width[cell[1]]; dv++) {
                    cells.add(new int[] { offset[cell[0]] + du, offset[cell[1]] + dv });
                }
            }
        }
        return cells;
    }

    private static List<int[]> ppCells(int shaftWidth, int shaftLength, int ballWidth, int baseHeight) {
        List<int[]> cells = new ArrayList<>();
        int baseWidth = shaftWidth + 2 * ballWidth;
        for (int u = 0; u < baseWidth; u++) {
            for (int v = 0; v < baseHeight; v++) {
                cells.add(new int[] { u, v });
            }
        }
        for (int u = ballWidth; u < ballWidth + shaftWidth; u++) {
            for (int v = baseHeight; v < baseHeight + shaftLength; v++) {
                cells.add(new int[] { u, v });
            }
        }
        return cells;
    }

    private static void addCell(List<int[]> cells, Set<Long> seen, int u, int v) {
        if (seen.add(packUV(u, v))) cells.add(new int[] { u, v });
    }

    private void registerAllSymmetries(Map<ShapeKey, String> table, List<int[]> cells, String name) {
        for (int transform = 0; transform < 8; transform++) {
            List<int[]> transformed = new ArrayList<>(cells.size());
            for (int[] cell : cells) transformed.add(transformCell(cell, transform));
            ShapeKey key = ShapeKey.of(normalize(transformed));
            if (!table.containsKey(key)) table.put(key, name);
            if ((transform & 3) == 0) uprightTemplates.add(key);
            maxTemplateCells = Math.max(maxTemplateCells, cells.size());
        }
    }

    /**
     * Registers a PP split into body and tip (cells with v >= {@code tipStart}), so a two-tone PP is found from its
     * body. Tip cells are stored relative to the body's own corner.
     */
    private void registerTippedSymmetries(List<int[]> cells, int tipStart) {
        for (int transform = 0; transform < 8; transform++) {
            List<int[]> body = new ArrayList<>();
            List<int[]> tip = new ArrayList<>();
            for (int[] cell : cells) (cell[1] >= tipStart ? tip : body).add(transformCell(cell, transform));
            int minU = Integer.MAX_VALUE, minV = Integer.MAX_VALUE;
            for (int[] cell : body) {
                minU = Math.min(minU, cell[0]);
                minV = Math.min(minV, cell[1]);
            }
            for (int[] cell : body) { cell[0] -= minU; cell[1] -= minV; }
            for (int[] cell : tip) { cell[0] -= minU; cell[1] -= minV; }
            tippedPPTemplates.computeIfAbsent(ShapeKey.of(body), k -> new ArrayList<>())
                    .add(new TipTemplate(tip, (transform & 3) == 0));
        }
    }

    /** Transforms 0-3 rotate by 90 degree steps, 4-7 do the same after mirroring. */
    private static int[] transformCell(int[] cell, int transform) {
        int u = transform >= 4 ? -cell[0] : cell[0];
        int v = cell[1];
        for (int r = 0; r < (transform & 3); r++) {
            int tmp = u;
            u = v;
            v = -tmp;
        }
        return new int[] { u, v };
    }

    private static List<int[]> normalize(List<int[]> cells) {
        int minU = Integer.MAX_VALUE, minV = Integer.MAX_VALUE;
        for (int[] cell : cells) {
            minU = Math.min(minU, cell[0]);
            minV = Math.min(minV, cell[1]);
        }
        List<int[]> result = new ArrayList<>(cells.size());
        for (int[] cell : cells) result.add(new int[] { cell[0] - minU, cell[1] - minV });
        return result;
    }

    // ------------------------------------------------------------------------------------------------
    // Lifecycle / events
    // ------------------------------------------------------------------------------------------------

    @Override
    protected void onEnable() {
        resetState();
    }

    @Override
    protected void onDisable() {
        resetState();
    }

    private void resetState() {
        foundPatterns.clear();
        reportedAt.clear();
        scannedChunks.clear();
        sectionQueue.clear();
        queuedSections.clear();
        dirtyPositions.clear();
        materialKeys.clear();
        currentJob = null;
        validationIndex = 0;
        tickCounter = 0;
        cachedChunk = null;
        cachedChunkX = cachedChunkZ = Integer.MIN_VALUE;
        lastWorld = mc.theWorld;
    }

    /** Called from MixinWorld before every client-side block change (placed, broken or replaced). */
    public void onBlockChange(BlockPos pos, IBlockState state) {
        if (mc.thePlayer == null || !mc.isCallingFromMinecraftThread()) return;
        int maxRange = range.getIntValue() + MAX_SPAN;
        if (pos.distanceSq(mc.thePlayer.getPosition()) > (double) maxRange * maxRange) return;

        if (dirtyPositions.size() < MAX_DIRTY_POSITIONS) {
            dirtyPositions.add(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        } else {
            // Mass edits (pastes, fills): fall back to rescanning the whole section.
            queueSection(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4, true);
        }
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.theWorld != lastWorld) resetState();

        // Block reads are cached per chunk; drop the cache every tick so unloaded chunks are never read.
        cachedChunk = null;
        cachedChunkX = cachedChunkZ = Integer.MIN_VALUE;

        if (tickCounter++ % DISCOVERY_INTERVAL_TICKS == 0) discoverChunks();
        validateFoundPatterns();

        long deadline = System.nanoTime() + scanBudget.getIntValue() * 1_000_000L;

        if (!dirtyPositions.isEmpty()) {
            Map<Long, Byte>[] visited = newVisitedMaps();
            Iterator<BlockPos> it = dirtyPositions.iterator();
            while (it.hasNext() && System.nanoTime() < deadline) {
                BlockPos pos = it.next();
                it.remove();
                revalidateNear(pos);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            scanSeed(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz, visited);
                        }
                    }
                }
            }
        }

        while (System.nanoTime() < deadline) {
            if (currentJob == null) {
                int[] section = sectionQueue.poll();
                if (section == null) break;
                queuedSections.remove(packSection(section[0], section[1], section[2]));
                currentJob = new SectionJob(section[0], section[1], section[2]);
            }
            int end = Math.min(4096, currentJob.index + 256);
            for (; currentJob.index < end; currentJob.index++) {
                int i = currentJob.index;
                scanSeed((currentJob.chunkX << 4) + (i & 15), (currentJob.sectionY << 4) + (i >> 8),
                        (currentJob.chunkZ << 4) + ((i >> 4) & 15), currentJob.visited);
            }
            if (currentJob.index >= 4096) currentJob = null;
        }
    }

    /** Queues every non-empty section of newly loaded (or re-sent) chunks in range, nearest first. */
    private void discoverChunks() {
        WorldClient world = mc.theWorld;
        int playerChunkX = (int) Math.floor(mc.thePlayer.posX) >> 4;
        int playerChunkZ = (int) Math.floor(mc.thePlayer.posZ) >> 4;
        int radius = (range.getIntValue() >> 4) + 1;

        Iterator<Map.Entry<Long, Chunk>> it = scannedChunks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Chunk> entry = it.next();
            Chunk chunk = entry.getValue();
            if (world.getChunkFromChunkCoords(chunk.xPosition, chunk.zPosition) != chunk) it.remove();
        }

        List<Chunk> fresh = new ArrayList<>();
        for (int cx = playerChunkX - radius; cx <= playerChunkX + radius; cx++) {
            for (int cz = playerChunkZ - radius; cz <= playerChunkZ + radius; cz++) {
                Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                if (chunk == null || chunk.isEmpty()) continue;
                if (scannedChunks.get(packChunk(cx, cz)) == chunk) continue;
                fresh.add(chunk);
            }
        }
        fresh.sort(Comparator.comparingInt(c -> Math.max(Math.abs(c.xPosition - playerChunkX), Math.abs(c.zPosition - playerChunkZ))));

        for (Chunk chunk : fresh) {
            scannedChunks.put(packChunk(chunk.xPosition, chunk.zPosition), chunk);
            ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
            for (int sy = 0; sy < storage.length; sy++) {
                if (storage[sy] != null && !storage[sy].isEmpty()) queueSection(chunk.xPosition, sy, chunk.zPosition, false);
            }
        }
    }

    private void queueSection(int chunkX, int sectionY, int chunkZ, boolean priority) {
        if (sectionY < 0 || sectionY > 15) return;
        if (!queuedSections.add(packSection(chunkX, sectionY, chunkZ))) return;
        int[] section = { chunkX, sectionY, chunkZ };
        if (priority) sectionQueue.addFirst(section);
        else sectionQueue.addLast(section);
    }

    // ------------------------------------------------------------------------------------------------
    // Detection
    // ------------------------------------------------------------------------------------------------

    private void scanSeed(int x, int y, int z, Map<Long, Byte>[] visited) {
        int material = keyAt(x, y, z);
        if (material <= KEY_NONE) return;
        // Buried blocks can't start a match: every valid shape has an edge block that will seed it instead.
        if (keyAt(x + 1, y, z) == material && keyAt(x - 1, y, z) == material && keyAt(x, y + 1, z) == material
                && keyAt(x, y - 1, z) == material && keyAt(x, y, z + 1) == material && keyAt(x, y, z - 1) == material) return;
        for (int plane = 0; plane < 3; plane++) {
            Detection detection = analyze(x, y, z, plane, false, visited[plane]);
            if (detection != null) handleDetection(detection);
            if (detectSwastikas.isEnabled()) {
                detection = analyze(x, y, z, plane, true, visited[plane + 3]);
                if (detection != null) handleDetection(detection);
            }
        }
    }

    /**
     * Flood-fills the same-material region containing the seed within one plane and checks whether it is
     * exactly one of the templates. {@code visited} (nullable) lets a scan skip regions it already handled.
     *
     * Blocks that are part of a solid 3D mass (same material directly in front of and behind them) never join
     * the region, so a drawing standing on or growing out of a platform of the same block is still isolated.
     */
    private Detection analyze(int x, int y, int z, int plane, boolean diagonal, Map<Long, Byte> visited) {
        int material = keyAt(x, y, z);
        if (material <= KEY_NONE) return null;
        if (visited != null && visited.containsKey(packPos(x, y, z))) return null;

        int seedU = toU(plane, x, y, z);
        int seedV = toV(plane, x, y, z);
        int depth = toD(plane, x, y, z);
        if (thickness(plane, seedU, seedV, depth, material) != 0) return null;
        int[][] neighbors = diagonal ? NEIGHBORS_8 : NEIGHBORS_4;

        if (queueU.length < maxTemplateCells + 2) {
            queueU = new int[maxTemplateCells + 2];
            queueV = new int[maxTemplateCells + 2];
        }
        Set<Long> component = this.component;
        component.clear();
        component.add(packUV(seedU, seedV));
        queueU[0] = seedU;
        queueV[0] = seedV;
        int head = 0, tail = 1;
        int minU = seedU, maxU = seedU, minV = seedV, maxV = seedV;
        boolean big = false;

        flood:
        while (head < tail) {
            int cu = queueU[head], cv = queueV[head];
            head++;
            for (int[] n : neighbors) {
                int nu = cu + n[0], nv = cv + n[1];
                long uv = packUV(nu, nv);
                if (component.contains(uv)) continue;
                int key = keyAt(plane, nu, nv, depth);
                if (key == KEY_UNLOADED) return null; // can't see the whole region yet; chunk load will rescan
                if (key != material) continue;
                int thick = thickness(plane, nu, nv, depth, material);
                if (thick == KEY_UNLOADED) return null;
                if (thick == 1) continue; // part of a 3D mass (ground, wall), not of the drawing
                if (visited != null && visited.get(worldLong(plane, nu, nv, depth)) != null) {
                    big = true; // touches a region that already turned out to be too large
                    break flood;
                }
                component.add(uv);
                minU = Math.min(minU, nu); maxU = Math.max(maxU, nu);
                minV = Math.min(minV, nv); maxV = Math.max(maxV, nv);
                if (component.size() > maxTemplateCells || maxU - minU >= MAX_SPAN || maxV - minV >= MAX_SPAN) {
                    big = true;
                    break flood;
                }
                queueU[tail] = nu;
                queueV[tail] = nv;
                tail++;
            }
        }

        if (visited != null) {
            byte mark = big ? VISITED_BIG : VISITED_SMALL;
            for (long uv : component) visited.put(worldLong(plane, (int) (uv >> 32), (int) uv, depth), mark);
        }
        if (big) return null;

        int width = maxU - minU + 1;
        int height = maxV - minV + 1;
        BitSet bits = new BitSet(width * height);
        for (long uv : component) bits.set(((int) uv - minV) * width + ((int) (uv >> 32) - minU));
        ShapeKey shapeKey = new ShapeKey(width, height, bits);
        String name = component.size() >= MIN_CELLS ? (diagonal ? diagonalTemplates : cardinalTemplates).get(shapeKey) : null;
        boolean upright = uprightTemplates.contains(shapeKey);

        // Two-tone PP: the body is one block and the shaft tip another (PP Builder's preset is white clay with a red tip).
        Set<Long> tip = Collections.emptySet();
        int tipMaterial = KEY_NONE;
        if (name == null && !diagonal && detectPP.isEnabled()) {
            List<TipTemplate> candidates = tippedPPTemplates.get(shapeKey);
            if (candidates != null) {
                int bodyMinU = minU, bodyMinV = minV;
                for (TipTemplate candidate : candidates) {
                    int key = tipMaterialAt(plane, depth, material, bodyMinU, bodyMinV, candidate);
                    if (key == KEY_UNLOADED) return null;
                    if (key == KEY_NONE) continue;
                    tip = new HashSet<>();
                    for (int[] cell : candidate.cells) {
                        int u = bodyMinU + cell[0], v = bodyMinV + cell[1];
                        tip.add(packUV(u, v));
                        minU = Math.min(minU, u); maxU = Math.max(maxU, u);
                        minV = Math.min(minV, v); maxV = Math.max(maxV, v);
                    }
                    if (component.size() + tip.size() < MIN_CELLS) return null;
                    tipMaterial = key;
                    upright = candidate.upright;
                    name = PP;
                    break;
                }
            }
        }
        if (name == null || !isTypeEnabled(name)) return null;
        // On walls (vertical planes) a PP has to stand upright; upside-down / sideways Ts are signposts and beams.
        if (PP.equals(name) && plane != PLANE_XZ && !upright) return null;

        // Small PPs are just a T, which shows up all over normal builds, so they need open space around them.
        boolean needsOpenSpace = PP.equals(name) && component.size() + tip.size() <= SMALL_PP_CELLS;

        // Contrast: nothing of the same material in the gaps or the 1-block border, except the row directly under
        // a shape on a wall, which may be the solid platform it stands on. Same-material blocks beside or above
        // it mean it is part of a bigger structure.
        for (int u = minU - 1; u <= maxU + 1; u++) {
            for (int v = minV - 1; v <= maxV + 1; v++) {
                long uv = packUV(u, v);
                if (component.contains(uv) || tip.contains(uv)) continue;
                int key = keyAt(plane, u, v, depth);
                if (key == KEY_UNLOADED) return null;
                boolean ground = plane != PLANE_XZ && v == minV - 1;
                if (key == material) {
                    if (!ground || thickness(plane, u, v, depth, material) != 1) return null;
                } else if (tipMaterial != KEY_NONE && key == tipMaterial) {
                    return null;
                } else if (needsOpenSpace && key != KEY_NONE && !ground) {
                    return null; // the ground it stands on doesn't count against open space
                }
            }
        }

        // Flatness: a drawing is a flat layer (possibly a few layers thick). Behind it on each side there must be
        // either open space or a solid surface; anything in between means it is a slice through a 3D object such
        // as a lamp post, building or terrain.
        int backedSides = 0;
        for (int side = -1; side <= 1; side += 2) {
            int result = sideKind(plane, depth, side, material, minU, minV, maxU, maxV);
            if (result == SIDE_INVALID) return null;
            if (result == SIDE_BACKED) backedSides++;
        }
        if (backedSides == 2) return null; // material on both sides: buried inside something, not a drawing
        if (backedSides == 1) {
            // A raised drawing on a surface of its own material only reads as a shape if its gaps are open.
            for (int u = minU; u <= maxU; u++) {
                for (int v = minV; v <= maxV; v++) {
                    long uv = packUV(u, v);
                    if (!component.contains(uv) && !tip.contains(uv) && keyAt(plane, u, v, depth) != KEY_NONE) return null;
                }
            }
        }

        return new Detection(name, plane, diagonal, x, y, z, minU, minV, maxU, maxV, depth);
    }

    /**
     * What is behind the shape on one side: {@link #SIDE_OPEN} (none of its material, or the shape is copied a
     * few layers deep and then stops cleanly), {@link #SIDE_BACKED} (a solid surface of its material covering the
     * whole shape area) or {@link #SIDE_INVALID} (anything else, or a chunk not loaded).
     */
    private int sideKind(int plane, int depth, int side, int material, int minU, int minV, int maxU, int maxV) {
        int touching = touchingCount(plane, depth + side, material);
        if (touching < 0) return SIDE_INVALID;
        if (touching * 10 <= component.size()) return SIDE_OPEN;
        if (touching != component.size()) return SIDE_INVALID;

        // The same outline repeated layer after layer is a beam or a wall seen end-on, not a drawing.
        int run = extrusionRun(plane, depth, side, material, minU, minV, maxU, maxV);
        if (run < 0 || run > MAX_EXTRUSION) return SIDE_INVALID;
        int behind = depth + side * (run + 1);
        if (run > 0) {
            int touchingEnd = touchingCount(plane, behind, material);
            if (touchingEnd < 0) return SIDE_INVALID;
            if (touchingEnd == 0) return SIDE_OPEN;
        }
        for (int u = minU; u <= maxU; u++) {
            for (int v = minV; v <= maxV; v++) {
                if (keyAt(plane, u, v, behind) != material) return SIDE_INVALID;
            }
        }
        return SIDE_BACKED;
    }

    /**
     * The one material (other than the body's) filling every cell of a tip template, as a flat layer.
     * {@link #KEY_NONE} if the cells don't form such a tip, {@link #KEY_UNLOADED} if a chunk is not loaded.
     */
    private int tipMaterialAt(int plane, int depth, int material, int minU, int minV, TipTemplate tip) {
        int tipMaterial = KEY_NONE;
        for (int[] cell : tip.cells) {
            int u = minU + cell[0], v = minV + cell[1];
            int key = keyAt(plane, u, v, depth);
            if (key == KEY_UNLOADED) return KEY_UNLOADED;
            if (key <= KEY_NONE || key == material || (tipMaterial != KEY_NONE && key != tipMaterial)) return KEY_NONE;
            tipMaterial = key;
            int thick = thickness(plane, u, v, depth, key);
            if (thick == KEY_UNLOADED) return KEY_UNLOADED;
            if (thick == 1) return KEY_NONE;
        }
        return tipMaterial;
    }

    /** How many cells of the component have the material at {@code layer}; -1 if a chunk is not loaded. */
    private int touchingCount(int plane, int layer, int material) {
        int touching = 0;
        for (long uv : component) {
            int key = keyAt(plane, (int) (uv >> 32), (int) uv, layer);
            if (key == KEY_UNLOADED) return -1;
            if (key == material) touching++;
        }
        return touching;
    }

    /**
     * Number of consecutive layers on {@code side} that repeat the component exactly (same cells of the material,
     * nothing of the material elsewhere in the bordered box). -1 if a needed chunk is not loaded.
     */
    private int extrusionRun(int plane, int depth, int side, int material, int minU, int minV, int maxU, int maxV) {
        int run = 0;
        for (int layer = depth + side; run <= MAX_EXTRUSION; layer += side) {
            for (int u = minU - 1; u <= maxU + 1; u++) {
                for (int v = minV - 1; v <= maxV + 1; v++) {
                    int key = keyAt(plane, u, v, layer);
                    if (key == KEY_UNLOADED) return -1;
                    if ((key == material) != component.contains(packUV(u, v))) return run;
                }
            }
            run++;
        }
        return run;
    }

    /** 1 if the same material is directly in front of and behind the block (part of a 3D mass), else 0. */
    private int thickness(int plane, int u, int v, int depth, int material) {
        int front = keyAt(plane, u, v, depth - 1);
        int back = keyAt(plane, u, v, depth + 1);
        if (front == KEY_UNLOADED || back == KEY_UNLOADED) return KEY_UNLOADED;
        return front == material && back == material ? 1 : 0;
    }

    private boolean isTypeEnabled(String name) {
        return SWASTIKA.equals(name) ? detectSwastikas.isEnabled() : detectPP.isEnabled();
    }

    private void handleDetection(Detection detection) {
        if (foundPatterns.size() >= MAX_FOUND_PATTERNS) return;
        for (Match match : foundPatterns) {
            if (match.detection.id.equals(detection.id)) return;
            // Extruded / multi-plane copies of a build already flagged.
            if (match.detection.name.equals(detection.name) && match.bounds.expand(2, 2, 2).intersectsWith(detection.bounds)) return;
        }
        foundPatterns.add(new Match(detection));

        long now = System.currentTimeMillis();
        Long last = reportedAt.get(detection.id);
        if (last == null || now - last > REPORT_COOLDOWN_MS) {
            reportedAt.put(detection.id, now);
            sendHoverMessage(detection);
        }
    }

    private void validateFoundPatterns() {
        int checks = Math.min(MAX_VALIDATIONS_PER_TICK, foundPatterns.size());
        for (int i = 0; i < checks && !foundPatterns.isEmpty(); i++) {
            if (validationIndex >= foundPatterns.size()) validationIndex = 0;
            if (stillValid(foundPatterns.get(validationIndex))) validationIndex++;
            else foundPatterns.remove(validationIndex);
        }
    }

    private void revalidateNear(BlockPos pos) {
        AxisAlignedBB around = new AxisAlignedBB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2);
        foundPatterns.removeIf(match -> match.bounds.intersectsWith(around) && !stillValid(match));
    }

    private boolean stillValid(Match match) {
        Detection d = match.detection;
        Detection again = analyze(d.seedX, d.seedY, d.seedZ, d.plane, d.diagonal, null);
        return again != null && again.id.equals(d.id);
    }

    // ------------------------------------------------------------------------------------------------
    // Block access
    // ------------------------------------------------------------------------------------------------

    private int keyAt(int plane, int u, int v, int d) {
        switch (plane) {
            case PLANE_XY: return keyAt(u, v, d);
            case PLANE_ZY: return keyAt(d, v, u);
            default: return keyAt(u, d, v);
        }
    }

    /** Material key of the block (block + colour/variant), {@link #KEY_NONE} for non-drawing blocks. */
    private int keyAt(int x, int y, int z) {
        if (y < 0 || y > 255) return KEY_NONE;
        int chunkX = x >> 4, chunkZ = z >> 4;
        if (chunkX != cachedChunkX || chunkZ != cachedChunkZ) {
            cachedChunk = mc.theWorld.getChunkFromChunkCoords(chunkX, chunkZ);
            cachedChunkX = chunkX;
            cachedChunkZ = chunkZ;
        }
        if (cachedChunk == null || cachedChunk.isEmpty()) return KEY_UNLOADED;
        ExtendedBlockStorage storage = cachedChunk.getBlockStorageArray()[y >> 4];
        if (storage == null) return KEY_NONE;
        return materialKey(storage.get(x & 15, y & 15, z & 15));
    }

    private int materialKey(IBlockState state) {
        Integer cached = materialKeys.get(state);
        if (cached != null) return cached;

        int key = KEY_NONE;
        Block block = state.getBlock();
        Material material = block.getMaterial();
        // Clear glass is see-through (windows), so it never forms a visible shape; stained glass still counts.
        boolean drawable = block != Blocks.air && block != Blocks.glass && block != Blocks.glass_pane
                && !material.isLiquid() && !material.isReplaceable()
                && (block.isFullCube() || block instanceof BlockCarpet || block instanceof BlockSlab
                    || block instanceof BlockStairs || block instanceof BlockPane);
        if (drawable && isSoil(block)) {
            key = (Block.getIdFromBlock(Blocks.dirt) + 1) * 512; // grass under a block turns into dirt
        } else if (drawable) {
            // Colour / wood type / stone type matter; orientation (stairs facing, log axis, slab half) does not.
            int variant = 0;
            for (IProperty property : state.getPropertyNames()) {
                String propertyName = property.getName();
                if (propertyName.equals("color") || propertyName.equals("variant") || propertyName.equals("type")) {
                    @SuppressWarnings("unchecked")
                    Comparable<?> value = state.getValue(property);
                    variant = 1 + (value instanceof Enum ? ((Enum<?>) value).ordinal() : value.hashCode() & 0xFF);
                    break;
                }
            }
            key = (Block.getIdFromBlock(block) + 1) * 512 + variant;
        }
        materialKeys.put(state, key);
        return key;
    }

    private static boolean isSoil(Block block) {
        return block == Blocks.grass || block == Blocks.dirt || block == Blocks.mycelium || block == Blocks.farmland;
    }

    // ------------------------------------------------------------------------------------------------
    // Output
    // ------------------------------------------------------------------------------------------------

    private void sendHoverMessage(Detection detection) {
        if (!chatNotifications.isEnabled()) return;
        BlockPos pos = detection.center;

        List<EntityPlayer> players = new ArrayList<>(mc.theWorld.playerEntities);
        players.sort(Comparator.comparingDouble(p -> p.getDistanceSq(pos)));

        StringBuilder hoverText = new StringBuilder("§eClosest Players:\n");
        int count = 0;
        for (EntityPlayer p : players) {
            if (count >= 5) break;
            double dist = Math.sqrt(p.getDistanceSq(pos));
            hoverText.append("§7- §f").append(p.getName())
                     .append(" §7(").append(String.format("%.1f", dist)).append("m)\n");
            count++;
        }
        if (count == 0) {
            hoverText.append("§7None");
        } else {
            hoverText.setLength(hoverText.length() - 1);
        }

        net.minecraft.util.ChatComponentText message = new net.minecraft.util.ChatComponentText(
                "§c[Moderation] §fDetected §c" + detection.name + " §7(" + detection.sizeLabel + ") §f" +
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
                RenderUtils.drawLine(startPos.xCoord - renderPosX, startPos.yCoord - renderPosY,
                        startPos.zCoord - renderPosZ, centerX, centerY, centerZ, outlineColor.getRGB(), width);
            }
        }
    }

    @Override
    public String getDisplayInfo() {
        if (foundPatterns.isEmpty()) return null;
        return foundPatterns.size() + " Found";
    }

    // ------------------------------------------------------------------------------------------------
    // Coordinates
    // ------------------------------------------------------------------------------------------------

    private static int toU(int plane, int x, int y, int z) { return plane == PLANE_ZY ? z : x; }
    private static int toV(int plane, int x, int y, int z) { return plane == PLANE_XZ ? z : y; }
    private static int toD(int plane, int x, int y, int z) { return plane == PLANE_XY ? z : plane == PLANE_ZY ? x : y; }

    private static int worldX(int plane, int u, int v, int d) { return plane == PLANE_ZY ? d : u; }
    private static int worldY(int plane, int u, int v, int d) { return plane == PLANE_XZ ? d : v; }
    private static int worldZ(int plane, int u, int v, int d) { return plane == PLANE_XY ? d : plane == PLANE_ZY ? u : v; }

    private static long worldLong(int plane, int u, int v, int d) {
        return packPos(worldX(plane, u, v, d), worldY(plane, u, v, d), worldZ(plane, u, v, d));
    }

    private static long packPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    private static long packUV(int u, int v) {
        return ((long) u << 32) | (v & 0xFFFFFFFFL);
    }

    private static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private static long packSection(int chunkX, int sectionY, int chunkZ) {
        return ((long) (chunkX & 0x3FFFFFF) << 30) | ((long) (chunkZ & 0x3FFFFFF) << 4) | (sectionY & 15);
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, Byte>[] newVisitedMaps() {
        Map<Long, Byte>[] maps = new Map[6];
        for (int i = 0; i < maps.length; i++) maps[i] = new HashMap<>();
        return maps;
    }

    // ------------------------------------------------------------------------------------------------
    // Types
    // ------------------------------------------------------------------------------------------------

    private static final class ShapeKey {
        final int width, height;
        final BitSet bits;

        ShapeKey(int width, int height, BitSet bits) {
            this.width = width; this.height = height; this.bits = bits;
        }

        static ShapeKey of(List<int[]> normalizedCells) {
            int width = 0, height = 0;
            for (int[] cell : normalizedCells) {
                width = Math.max(width, cell[0] + 1);
                height = Math.max(height, cell[1] + 1);
            }
            BitSet bits = new BitSet(width * height);
            for (int[] cell : normalizedCells) bits.set(cell[1] * width + cell[0]);
            return new ShapeKey(width, height, bits);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ShapeKey)) return false;
            ShapeKey other = (ShapeKey) o;
            return width == other.width && height == other.height && bits.equals(other.bits);
        }

        @Override
        public int hashCode() {
            return (width * 31 + height) * 31 + bits.hashCode();
        }
    }

    private static final class TipTemplate {
        final List<int[]> cells;
        final boolean upright;

        TipTemplate(List<int[]> cells, boolean upright) {
            this.cells = cells; this.upright = upright;
        }
    }

    private static final class Detection {
        final String name;
        final int plane;
        final boolean diagonal;
        final int seedX, seedY, seedZ;
        final AxisAlignedBB bounds;
        final BlockPos center;
        final String sizeLabel;
        final String id;

        Detection(String name, int plane, boolean diagonal, int seedX, int seedY, int seedZ,
                  int minU, int minV, int maxU, int maxV, int depth) {
            this.name = name;
            this.plane = plane;
            this.diagonal = diagonal;
            this.seedX = seedX; this.seedY = seedY; this.seedZ = seedZ;

            int x1 = worldX(plane, minU, minV, depth), x2 = worldX(plane, maxU, maxV, depth);
            int y1 = worldY(plane, minU, minV, depth), y2 = worldY(plane, maxU, maxV, depth);
            int z1 = worldZ(plane, minU, minV, depth), z2 = worldZ(plane, maxU, maxV, depth);
            this.bounds = new AxisAlignedBB(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                    Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, Math.max(z1, z2) + 1);
            this.center = new BlockPos((x1 + x2) / 2, (y1 + y2) / 2, (z1 + z2) / 2);
            this.sizeLabel = (maxU - minU + 1) + "x" + (maxV - minV + 1);
            this.id = name + '|' + plane + '|' + diagonal + '|' + x1 + ',' + y1 + ',' + z1 + '|' + sizeLabel;
        }
    }

    private static final class Match {
        final Detection detection;
        final AxisAlignedBB bounds;

        Match(Detection detection) {
            this.detection = detection;
            this.bounds = detection.bounds;
        }
    }

    private static final class SectionJob {
        final int chunkX, sectionY, chunkZ;
        final Map<Long, Byte>[] visited = newVisitedMaps();
        int index;

        SectionJob(int chunkX, int sectionY, int chunkZ) {
            this.chunkX = chunkX; this.sectionY = sectionY; this.chunkZ = chunkZ;
        }
    }
}

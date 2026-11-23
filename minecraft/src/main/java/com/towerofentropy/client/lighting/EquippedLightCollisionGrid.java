package com.towerofentropy.client.lighting;

import java.lang.ref.WeakReference;
import java.util.BitSet;
import java.util.IdentityHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Caches collider bounds in a moving region in world coordinates. */
@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class EquippedLightCollisionGrid {
    static final int SIDE = 23, CELLS = SIDE * SIDE * SIDE;
    private static final int SLICE = SIDE * SIDE;
    private static final long FULL_BLOCK = 0xffffff000000L;
    private static final boolean FULL_SCAN = Boolean.getBoolean("toe.equippedLight.fullScan");
    private static final EquippedLightCollisionGrid INSTANCE = new EquippedLightCollisionGrid();
    private static WeakReference<ClientLevel> trackedLevel = new WeakReference<>(null);

    private final IdentityHashMap<BlockState, Long> shapes = new IdentityHashMap<>();
    private final BitSet dirty = new BitSet(CELLS);
    private final BitSet entering = new BitSet(CELLS);
    private BitSet dynamic = new BitSet(CELLS), shiftedDynamic = new BitSet(CELLS);
    private long[] cells = new long[CELLS], shifted = new long[CELLS];
    private final BlockPos.MutableBlockPos query = new BlockPos.MutableBlockPos();
    private BlockPos center, origin;
    private ClientLevel world;
    private long worldTick;
    private int firstChanged, lastChanged;
    private long queries;

    private EquippedLightCollisionGrid() {}

    static EquippedLightCollisionGrid instance() { return INSTANCE; }

    public static void enable(ClientLevel level) { trackedLevel = new WeakReference<>(level); }

    public static void blockChanged(ClientLevel level, BlockPos position) {
        int x = position.getX(), y = position.getY(), z = position.getZ();
        if (Minecraft.getInstance().isSameThread()) {
            INSTANCE.invalidate(level, x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
        } else {
            Minecraft.getInstance().execute(() -> INSTANCE.invalidate(level, x - 1, y - 1, z - 1, x + 1, y + 1, z + 1));
        }
    }

    @SubscribeEvent
    public static void chunkChanged(ChunkEvent event) {
        if (!(event.getLevel() instanceof ClientLevel level)) return;
        var pos = event.getChunk().getPos();
        int x = pos.getMinBlockX(), z = pos.getMinBlockZ();
        if (Minecraft.getInstance().isSameThread()) {
            INSTANCE.invalidate(level, x - 1, Integer.MIN_VALUE, z - 1, x + 16, Integer.MAX_VALUE, z + 16);
        } else {
            Minecraft.getInstance().execute(() ->
                    INSTANCE.invalidate(level, x - 1, Integer.MIN_VALUE, z - 1, x + 16, Integer.MAX_VALUE, z + 16));
        }
    }

    @SubscribeEvent
    public static void tagsChanged(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
            Minecraft.getInstance().execute(INSTANCE::reset);
        }
    }

    void reset() {
        world = null;
        center = origin = null;
        dirty.clear();
        dynamic.clear();
        shapes.clear();
    }

    void releaseOtherLevel(ClientLevel level) {
        if (world != null && world != level) reset();
    }

    private void invalidate(ClientLevel level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (world != level || origin == null) return;
        int startX = Math.max(minX, origin.getX()), endX = Math.min(maxX, origin.getX() + SIDE - 1);
        if (startX > endX) return;
        for (int z = Math.max(minZ, origin.getZ()); z <= Math.min(maxZ, origin.getZ() + SIDE - 1); z++) {
            for (int y = Math.max(minY, origin.getY()); y <= Math.min(maxY, origin.getY() + SIDE - 1); y++) {
                int start = index(startX - origin.getX(), y - origin.getY(), z - origin.getZ());
                dirty.set(start, start + endX - startX + 1);
            }
        }
    }

    void update(ClientLevel level, BlockPos playerCell) {
        firstChanged = CELLS;
        lastChanged = -1;
        if (world != level) reset();
        world = level;
        long tick = level.getGameTime();
        var base = playerCell.equals(center) ? origin : playerCell.offset(-SIDE / 2, -SIDE / 2, -SIDE / 2);
        if (origin == null) {
            dirty.set(0, CELLS);
            firstChanged = 0;
            lastChanged = CELLS - 1;
        } else if (!base.equals(origin)) {
            shift(base.getX() - origin.getX(), base.getY() - origin.getY(), base.getZ() - origin.getZ());
        }
        if (tick != worldTick) {
            if (FULL_SCAN || trackedLevel.get() != level) dirty.set(0, CELLS);
            else dirty.or(dynamic);
        }
        center = playerCell;
        origin = base;
        worldTick = tick;
        int checkedChunks = 0, loadedChunks = 0;
        int startChunkX = base.getX() >> 4, startChunkZ = base.getZ() >> 4;
        for (int i = dirty.nextSetBit(0); i >= 0; i = dirty.nextSetBit(i + 1)) {
            int x = base.getX() + i % SIDE;
            int y = base.getY() + (i / SIDE) % SIDE;
            int z = base.getZ() + i / SLICE;
            long shape = FULL_BLOCK;
            boolean dynamicShape = false;
            int chunkX = x >> 4, chunkZ = z >> 4;
            int chunk = 1 << (chunkX - startChunkX + 3 * (chunkZ - startChunkZ));
            if ((checkedChunks & chunk) == 0) {
                checkedChunks |= chunk;
                if (level.hasChunk(chunkX, chunkZ)) loadedChunks |= chunk;
            }
            if ((loadedChunks & chunk) != 0) {
                query.set(x, y, z);
                var state = level.getBlockState(query);
                dynamicShape = state.getBlock().hasDynamicShape();
                shape = bounds(level, state, dynamicShape);
            }
            dynamic.set(i, dynamicShape);
            queries++;
            if (cells[i] != shape) {
                cells[i] = shape;
                markChanged(i);
            }
        }
        dirty.clear();
    }

    private void shift(int dx, int dy, int dz) {
        if (Math.abs(dx) >= SIDE || Math.abs(dy) >= SIDE || Math.abs(dz) >= SIDE) {
            dirty.set(0, CELLS);
            dynamic.clear();
            return;
        }
        shiftedDynamic.clear();
        entering.clear();
        for (int z = 0; z < SIDE; z++) {
            for (int y = 0; y < SIDE; y++) {
                for (int x = 0; x < SIDE; x++) {
                    int i = index(x, y, z);
                    int oldX = x + dx, oldY = y + dy, oldZ = z + dz;
                    if (inside(oldX) && inside(oldY) && inside(oldZ)) {
                        int previous = index(oldX, oldY, oldZ);
                        shifted[i] = cells[previous];
                        shiftedDynamic.set(i, dynamic.get(previous));
                        entering.set(i, dirty.get(previous));
                        if (cells[i] != shifted[i]) markChanged(i);
                    } else {
                        shifted[i] = cells[i];
                        entering.set(i);
                    }
                }
            }
        }
        var previous = cells;
        cells = shifted;
        shifted = previous;
        var previousDynamic = dynamic;
        dynamic = shiftedDynamic;
        shiftedDynamic = previousDynamic;
        dirty.clear();
        dirty.or(entering);
    }

    private static boolean inside(int cell) { return cell >= 0 && cell < SIDE; }

    private static int index(int x, int y, int z) { return (z * SIDE + y) * SIDE + x; }

    private void markChanged(int i) {
        firstChanged = Math.min(firstChanged, i);
        lastChanged = Math.max(lastChanged, i);
    }

    int firstChanged() { return firstChanged; }
    int lastChanged() { return lastChanged; }
    BlockPos origin() { return origin; }
    long cell(int index) { return cells[index]; }
    long queries() { return queries; }

    private long bounds(ClientLevel level, BlockState state, boolean dynamicShape) {
        if (state.isAir()) return 0;
        if (!dynamicShape) {
            Long cached = shapes.get(state);
            if (cached != null) return cached;
        }
        long result = 0;
        if (!state.is(Tags.Blocks.GLASS) && !state.is(Tags.Blocks.GLASS_PANES) && !state.is(BlockTags.LEAVES)) {
            var collision = state.getCollisionShape(level, query);
            if (!collision.isEmpty()) {
                var box = collision.bounds();
                result = quantize(box.minX, false) | quantize(box.minY, false) << 8 | quantize(box.minZ, false) << 16
                       | quantize(box.maxX, true) << 24 | quantize(box.maxY, true) << 32 | quantize(box.maxZ, true) << 40;
            }
        }
        if (!dynamicShape) shapes.put(state, result);
        return result;
    }

    private static long quantize(double coordinate, boolean upper) {
        // Clip bounds to their voxel. Collision above fences is approximated.
        return (long) Math.max(0, Math.min(255, upper ? Math.ceil(coordinate * 255) : Math.floor(coordinate * 255)));
    }
}

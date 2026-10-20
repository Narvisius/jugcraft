package io.github.jimbozoomer.jugcraft.town;

import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;

/** Vanilla signs and a locked, north-up town plan. No client dependency or chunk scanning. */
public final class TownWayfinding {
    private TownWayfinding() {}

    /** Called only after this chunk's geometry is built; never rewrites an existing town. */
    public static void chunkBuilt(ServerLevel level, ChunkPos chunk, BlockPos origin) {
        for (var entry : TownData.get().signs) {
            BlockPos pos = origin.offset(entry.pos());
            if (!ChunkPos.containing(pos).equals(chunk)) continue;
            if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
                List<Component> lines = entry.lines().stream().map(Component::literal).map(c -> (Component)c).toList();
                for (var slot : SignTextSlot.values()) sign.setText(new SignText(lines, lines, DyeColor.WHITE, false), slot);
                sign.setWaxed(true);
                sign.setChanged();
                level.sendBlockUpdated(pos, sign.getBlockState(), sign.getBlockState(), 3);
            }
        }
        for (var entry : TownData.get().mapBoards) {
            BlockPos pos = origin.offset(entry.pos());
            if (!ChunkPos.containing(pos).equals(chunk)) continue;
            // Idempotent if an operator retries construction of this chunk.
            if (!level.getEntitiesOfClass(ItemFrame.class, new AABB(pos).inflate(0.5),
                    frame -> frame.getItem().is(Items.FILLED_MAP)).isEmpty()) continue;
            ItemFrame frame = new ItemFrame(level, pos, entry.facing());
            frame.setItem(map(level));
            frame.setPermanentlyInvulnerable(true);
            level.addFreshEntity(frame);
        }
    }

    /** All copies share one saved, locked map ID; requesting maps cannot allocate unlimited saved data. */
    public static ItemStack map(ServerLevel level) {
        TownState state = TownState.get(level);
        BlockPos origin = state.origin();
        if (origin == null) return ItemStack.EMPTY;
        MapId id = new MapId(state.townMap());
        if (state.townMap() < 0 || level.getMapData(id) == null) {
            var fresh = MapItemSavedData.createFresh(origin.getX() + 96, origin.getZ() + 96,
                    (byte)1, true, true, level.dimension());
            // Vanilla's factory snaps to a global grid. Its public codec lets the
            // authored town plan use its exact centre, including negative origins.
            var encoded = MapItemSavedData.CODEC.encodeStart(JsonOps.INSTANCE, fresh).getOrThrow().getAsJsonObject();
            encoded.addProperty("xCenter", origin.getX() + 96);
            encoded.addProperty("zCenter", origin.getZ() + 96);
            var plan = MapItemSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
            TownData data = TownData.get();
            for (int z = 0; z < 128; z++) for (int x = 0; x < 128; x++) {
                int tx = (x - 64) * 2 + 96, tz = (z - 64) * 2 + 96;
                MapColor colour = MapColor.SAND;
                if (data.mask(tx, tz) != 0) {
                    for (int y = data.yMin + data.height - 1; y >= 0; y--) {
                        var block = data.state(data.index(tx, y, tz));
                        MapColor candidate = block.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                        if (candidate != MapColor.NONE) { colour = candidate; break; }
                    }
                }
                plan.setColor(x, z, colour.getPackedId(MapColor.Brightness.NORMAL));
            }
            label(plan, 96, 62, new String[]{"010", "110", "010", "010", "111"});
            label(plan, 64, 106, new String[]{"110", "001", "010", "100", "111"});
            label(plan, 128, 107, new String[]{"110", "001", "010", "001", "110"});
            var shop = data.spots.stream().filter(s -> "general".equals(s.shop())).findFirst().orElseThrow();
            label(plan, shop.pos().getX(), shop.pos().getZ(), new String[]{"101", "101", "111", "001", "001"});
            id = level.getFreeMapId();
            level.setMapData(id, plan.locked());
            state.setTownMap(id.id());
        }
        ItemStack stack = new ItemStack(Items.FILLED_MAP);
        stack.set(DataComponents.MAP_ID, id);
        stack.set(DataComponents.ITEM_NAME, Component.translatable("filled_map.jugcraft.town"));
        return stack;
    }

    private static void label(MapItemSavedData map, int tx, int tz, String[] glyph) {
        int x = (tx - 96) / 2 + 63, z = (tz - 96) / 2 + 62;
        for (int dy = -1; dy <= 5; dy++) for (int dx = -1; dx <= 3; dx++) {
            boolean ink = dy >= 0 && dy < 5 && dx >= 0 && dx < 3 && glyph[dy].charAt(dx) == '1';
            map.setColor(x + dx, z + dy, (ink ? MapColor.COLOR_BLACK : MapColor.SNOW).getPackedId(MapColor.Brightness.NORMAL));
        }
    }

    /** A free personal copy, but never dropped on the ground or duplicated in the same inventory. */
    public static void giveMap(ServerPlayer player) {
        ItemStack stack = map(player.level());
        if (stack.isEmpty()) return;
        MapId id = stack.get(DataComponents.MAP_ID);
        boolean has = player.getInventory().contains(item -> id.equals(item.get(DataComponents.MAP_ID)));
        String result = has ? "have_map" : player.getInventory().add(stack) ? "map_given" : "map_full";
        player.sendSystemMessage(Component.translatable("message.jugcraft.town." + result));
    }
}

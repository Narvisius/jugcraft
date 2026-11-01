package io.github.jimbozoomer.jugcraft.building;

import com.google.gson.*;
import java.util.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Cached mesh-derived boxes, including authored offsets outside one block. */
final class OwnerAuthoredShapes {
    private final Map<BlockState, VoxelShape> states = new HashMap<>();
    OwnerAuthoredShapes(Block block, JsonObject variants) {
        Map<JsonArray, VoxelShape> compiled = new HashMap<>();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            VoxelShape shape = Shapes.empty(); int matches = 0;
            for (var entry : variants.entrySet()) {
                boolean match = true;
                for (String pair : entry.getKey().split(",")) {
                    if (pair.isEmpty()) continue;
                    String[] bits = pair.split("=", 2);
                    var property = block.getStateDefinition().getProperty(bits[0]);
                    if (property == null || property.getValue(bits[1]).isEmpty()
                            || !state.getValue(property).equals(property.getValue(bits[1]).orElseThrow())) { match = false; break; }
                }
                if (!match) continue;
                VoxelShape part = compiled.computeIfAbsent(entry.getValue().getAsJsonArray(), OwnerAuthoredShapes::compile);
                shape = matches++ == 0 ? part : Shapes.joinUnoptimized(shape, part, BooleanOp.OR);
            }
            if (matches == 0) throw new IllegalStateException("No supplied architectural shape for " + state);
            states.put(state, matches == 1 ? shape : shape.optimize());
        }
    }
    private static VoxelShape compile(JsonArray boxes) {
        VoxelShape shape = Shapes.empty();
        for (JsonElement element : boxes) {
            JsonArray box = element.getAsJsonArray();
            shape = Shapes.joinUnoptimized(shape, Block.box(box.get(0).getAsDouble(), box.get(1).getAsDouble(), box.get(2).getAsDouble(),
                box.get(3).getAsDouble(), box.get(4).getAsDouble(), box.get(5).getAsDouble()), BooleanOp.OR);
        }
        // Optimize once, not after every mesh cuboid. Manual/connection states
        // sharing a supplied mesh also share the immutable compiled shape.
        return shape.optimize();
    }
    VoxelShape get(BlockState state) { return states.getOrDefault(state, Shapes.block()); }
}

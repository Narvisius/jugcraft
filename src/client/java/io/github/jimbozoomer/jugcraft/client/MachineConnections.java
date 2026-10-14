package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.CableBlock;
import io.github.jimbozoomer.jugcraft.fluid.FluidPipeBlock;
import io.github.jimbozoomer.jugcraft.machine.EnlargedMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.Footprint;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.PipeBlock;

/** Additional fittings on legacy all-face machines, visible only at an attached cable.
 * Uses the existing quad renderer; no extra block entities, saved states or server ticking. */
public final class MachineConnections {
    private record Port(Vec3i offset, Direction face, QuadModel model) {}
    public record Fitting(QuadModel model, float rotation, int light) {}
    private record Cached(long tick, List<Fitting> fittings) {}
    private static Map<String, List<Port>> models;
    private static final Map<MachineBlockEntity, Cached> CACHE = new WeakHashMap<>();

    private MachineConnections() {}

    /** Block entity renderer construction also happens on resource reload. */
    static void reset() { models = null; CACHE.clear(); }

    public static List<Fitting> extract(MachineBlockEntity machine) {
        var level = machine.getLevel();
        var state = machine.getBlockState();
        if (level == null || state.hasProperty(EnlargedMachineBlock.COMPACT)
                && state.getValue(EnlargedMachineBlock.COMPACT)) return List.of();
        if (models == null) models = load();
        var ports = models.get(machine.kind().id);
        if (ports == null) return List.of();
        long now = level.getGameTime();
        Cached cached = CACHE.get(machine);
        if (cached != null && now >= cached.tick && now - cached.tick < 4) return cached.fittings;
        var facing = state.getValue(MachineBlock.FACING);
        float rotation = switch (facing) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
        List<Fitting> fittings = new ArrayList<>();
        for (Port port : ports) {
            var at = machine.getBlockPos().offset(Footprint.rotate(port.offset, facing));
            var side = Footprint.rotate(port.face, facing);
            var cable = level.getBlockState(at.relative(side));
            if ((cable.getBlock() instanceof CableBlock || cable.getBlock() instanceof FluidPipeBlock)
                    && cable.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side.getOpposite()))) {
                fittings.add(new Fitting(port.model, rotation, LightCoordsUtil.getLightCoords(level, at.relative(side))));
            }
        }
        var result = List.copyOf(fittings);
        CACHE.put(machine, new Cached(now, result));
        return result;
    }

    public static void submit(List<Fitting> fittings, PoseStack pose, SubmitNodeCollector collector) {
        for (Fitting fitting : fittings) {
            pose.pushPose();
            pose.translate(.5F, 0, .5F);
            pose.rotateDegrees(Axis.YP, -fitting.rotation);
            pose.translate(-.5F, 0, -.5F);
            fitting.model.submit(pose, collector, fitting.light);
            pose.popPose();
        }
    }

    private static Map<String, List<Port>> load() {
        Map<String, List<Port>> result = new HashMap<>();
        var file = Jugcraft.id("machine_connection_quads.json");
        var resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) return result;
        try (Reader reader = resource.get().openAsReader()) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var entry : root.entrySet()) {
                List<Port> ports = new ArrayList<>();
                for (var value : entry.getValue().getAsJsonArray()) {
                    var data = value.getAsJsonObject();
                    var xyz = data.getAsJsonArray("offset");
                    var face = Direction.valueOf(data.get("face").getAsString().toUpperCase(java.util.Locale.ROOT));
                    ports.add(new Port(new Vec3i(xyz.get(0).getAsInt(), xyz.get(1).getAsInt(), xyz.get(2).getAsInt()),
                            face, QuadModel.parse(data.getAsJsonArray("quads"))));
                }
                result.put(entry.getKey(), List.copyOf(ports));
            }
        } catch (Exception e) {
            Jugcraft.LOGGER.error("Could not load machine cable fittings: {}", file, e);
            result.clear();
        }
        return result;
    }
}

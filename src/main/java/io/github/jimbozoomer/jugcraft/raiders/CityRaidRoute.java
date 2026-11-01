package io.github.jimbozoomer.jugcraft.raiders;

import io.github.jimbozoomer.jugcraft.town.Town;
import io.github.jimbozoomer.jugcraft.town.TownData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Navigation only, through tools/town.py's authored gates. Never edits blocks or loads chunks. */
public final class CityRaidRoute {
    private CityRaidRoute() {}
    public record Gate(BlockPos passage, int dx, int dz) {
        public BlockPos outside() { return passage.offset(dx * 16, 0, dz * 16); }
        public BlockPos inside() { return passage.offset(-dx * 14, 0, -dz * 14); }
    }
    public static List<Gate> gates(BlockPos origin) {
        BlockPos c = Town.centre(origin).above();
        return List.of(new Gate(c.offset(76, 0, 0), 1, 0),
                new Gate(c.offset(0, 0, 71), 0, 1), new Gate(c.offset(-76, 0, 0), -1, 0));
    }
    public static Gate nearest(BlockPos origin, Vec3 from) {
        return gates(origin).stream().min(Comparator.comparingDouble(g ->
                Vec3.atBottomCenterOf(g.outside()).subtract(from).horizontalDistanceSqr())).orElseThrow();
    }
    public static List<BlockPos> approach(BlockPos origin, Vec3 from) {
        Gate gate = nearest(origin, from);
        Vec3 c = Vec3.atBottomCenterOf(Town.centre(origin));
        int lx = (int)Math.floor(from.x) - origin.getX(), lz = (int)Math.floor(from.z) - origin.getZ();
        if (TownData.get().mask(lx, lz) == TownData.INSIDE
                && from.subtract(c).horizontalDistance() < 60) return List.of(gate.inside());
        List<BlockPos> route = new ArrayList<>();
        // The Styx estate starts at local X=192 and occupies Z=52..100.
        // Keep the ring and east forecourt on the connecting road, west of it.
        // An older raid arriving beyond the estate goes around its perimeter.
        double approachX = from.x, approachZ = from.z;
        if (from.x > origin.getX() + 188) {
            double low = origin.getZ() + 44, high = origin.getZ() + 108;
            boolean outsideEstate = from.x >= origin.getX() + 259 || from.z < origin.getZ()+52 || from.z > origin.getZ()+100;
            if (outsideEstate) {
                approachZ = from.z < (low+high)/2 ? Math.min(from.z, low) : Math.max(from.z, high);
                route.add(BlockPos.containing(from.x, origin.getY()+1, approachZ));
                approachX = origin.getX()+188;
                route.add(BlockPos.containing(approachX, origin.getY()+1, approachZ));
            }
        }
        double start = Math.atan2(approachZ - c.z, approachX - c.x);
        double finish = Math.atan2(gate.dz(), gate.dx());
        double arc = Math.atan2(Math.sin(finish - start), Math.cos(finish - start));
        // Move around the outside, not diagonally across the curtain wall.
        int steps = Math.max(1, (int)Math.ceil(Math.abs(arc) / .18));
        for (int i = 0; i <= steps; i++) {
            double a = start + arc * i / steps;
            route.add(BlockPos.containing(c.x + Math.cos(a) * 92, origin.getY() + 1,
                    c.z + Math.sin(a) * 92));
        }
        route.add(gate.outside()); route.add(gate.passage()); route.add(gate.inside());
        return List.copyOf(route);
    }
}

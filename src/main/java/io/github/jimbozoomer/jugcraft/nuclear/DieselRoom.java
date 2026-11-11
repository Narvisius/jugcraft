package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * A diesel generator room's own plant (round 31 part 3; Justin: "Diesel Part 3"; tools/diesel_room.py): the jacket
 * water and lube oil keep-warm skids, the expansion tank, the jacket water cooler, the neutral grounding resistor, the
 * generator's terminal and CT cubicle, the CO2 cylinder bank and the monorail hoist. Each joins the nearest diesel
 * generator within {@link #REACH} blocks, as the starting air does (StartingAir): the client reads that generator's
 * state for their gauges, lamps and sight glass (Justin: keep-warm skids "Looks and live gauges only", the cooler "Live
 * gauges, no trip"); the generator's CO2 system needs its bank (Co2System).
 */
public final class DieselRoom {
	/** How far (blocks) from a generator's anchor its room's pieces may stand. */
	public static final int REACH = 40;

	private DieselRoom() {
	}

	/** Each second (spread by position): the generator finds its room's pieces again when the plant changed, and joins them. */
	public static void tick(ServerLevel level, EquipmentBlockEntity edg, long now) {
		if (Math.floorMod(now + edg.getBlockPos().asLong() + 7, 20) != 0) {
			return;
		}
		EquipmentRegistry registry = EquipmentRegistry.of(level);
		if (edg.roomVersion != registry.version() || now - edg.roomScanned >= StartingAir.RESCAN) {
			edg.roomVersion = registry.version();
			edg.roomScanned = now;
			edg.roomPieces = find(registry, edg.getBlockPos());
		}
		for (BlockPos at : edg.roomPieces) {
			if (level.isLoaded(at) && level.getBlockEntity(at) instanceof EquipmentBlockEntity piece && piece.kind().dieselRoom()
					&& !edg.getBlockPos().equals(piece.linked)) {
				piece.linked = edg.getBlockPos().immutable();
				piece.changed();
			}
		}
	}

	/** The room's pieces within {@link #REACH} of a generator that have no other generator nearer. */
	static List<BlockPos> find(EquipmentRegistry registry, BlockPos at) {
		Map<BlockPos, EquipmentRegistry.Entry> anchors = registry.anchors();
		List<BlockPos> edgs = new ArrayList<>();
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : anchors.entrySet()) {
			if (e.getValue().kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR) {
				edgs.add(e.getKey());
			}
		}
		List<BlockPos> out = new ArrayList<>();
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : anchors.entrySet()) {
			if (!e.getValue().kind().dieselRoom()) {
				continue;
			}
			double d = e.getKey().distSqr(at);
			BlockPos piece = e.getKey();
			if (d <= (double) REACH * REACH && edgs.stream().noneMatch(o -> !o.equals(at) && o.distSqr(piece) < d)) {
				out.add(piece.immutable());
			}
		}
		return out;
	}

	/** The pieces of one kind a generator has (nearest first). */
	public static List<EquipmentBlockEntity> pieces(ServerLevel level, EquipmentBlockEntity edg, EquipmentKind kind) {
		List<EquipmentBlockEntity> out = new ArrayList<>();
		for (BlockPos at : edg.roomPieces) {
			if (level.isLoaded(at) && level.getBlockEntity(at) instanceof EquipmentBlockEntity piece && piece.kind() == kind) {
				out.add(piece);
			}
		}
		out.sort(java.util.Comparator.comparingDouble(p -> p.getBlockPos().distSqr(edg.getBlockPos())));
		return out;
	}

	/** A piece's own check (each five seconds): its generator still there and still has it, or it stands alone. */
	public static void tickPiece(ServerLevel level, EquipmentBlockEntity piece, long now) {
		if (piece.linked == null || Math.floorMod(now + piece.getBlockPos().asLong(), 100) != 0 || !level.isLoaded(piece.linked)) {
			return;
		}
		if (!(level.getBlockEntity(piece.linked) instanceof EquipmentBlockEntity edg) || edg.kind() != EquipmentKind.EMERGENCY_DIESEL_GENERATOR
				|| edg.roomVersion != -1 && !edg.roomPieces.contains(piece.getBlockPos())) {
			piece.linked = null;
			piece.changed();
		}
	}

	/** A piece's label: 1A or 1B (its anchor's variant). */
	public static String label(EquipmentBlockEntity piece) {
		return StartingAir.label(piece) == 1 ? "1B" : "1A";
	}
}

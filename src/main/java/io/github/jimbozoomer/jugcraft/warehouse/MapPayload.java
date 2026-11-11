package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Part W5: the warehouse's floor map (the terminal's LAYOUT page and the Status Board; TerminalBlockEntity.map): each
 * floor's cells (lane, walkway, crossing, dock, rack, piece, from x0, z0, w across, d deep), its pieces' footprints
 * and names, and its rack columns (the places one above another at a cell: their code, how many are in use, how many
 * hold a part pallet, when the column was last counted). The drones come from the terminal's fleet (their legs).
 */
public record MapPayload(BlockPos terminal, int version, List<Floor> floors) implements CustomPacketPayload {
	public static final Type<MapPayload> TYPE = new Type<>(Jugcraft.id("warehouse_map"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MapPayload> CODEC = StreamCodec.of(MapPayload::write, MapPayload::read);
	/** The cells' codes. */
	public static final byte NONE = 0, LANE = 1, WALKWAY = 2, CROSSING = 3, DOCK = 4, RACK = 5, PIECE = 6;

	public record Floor(int y, int x0, int z0, int w, int d, byte[] cells, List<Piece> pieces, List<Column> racks) {
		public byte cell(int x, int z) {
			int i = (z - z0) * w + (x - x0);
			return x < x0 || z < z0 || x >= x0 + w || z >= z0 + d || i < 0 || i >= cells.length ? NONE : cells[i];
		}
	}

	/** A piece's footprint (world x, z) and the name the map shows on it. */
	public record Piece(String name, int x0, int z0, int x1, int z1) {
	}

	/** A rack column: its cell, its code (2B-04), places in use, part pallets, places in all, the oldest count (0: never). */
	public record Column(int x, int z, String label, int used, int parts, int levels, long counted) {
	}

	private static void write(RegistryFriendlyByteBuf buf, MapPayload p) {
		buf.writeBlockPos(p.terminal);
		buf.writeVarInt(p.version);
		// (As many as the reader takes: 64 floors, 512 pieces and 8,192 rack columns a floor.)
		List<Floor> floors = p.floors.subList(0, Math.min(64, p.floors.size()));
		buf.writeVarInt(floors.size());
		for (Floor f : floors) {
			buf.writeVarInt(f.y);
			buf.writeVarInt(f.x0);
			buf.writeVarInt(f.z0);
			buf.writeVarInt(f.w);
			buf.writeVarInt(f.d);
			buf.writeByteArray(f.cells);
			List<Piece> pieces = f.pieces.subList(0, Math.min(512, f.pieces.size()));
			buf.writeVarInt(pieces.size());
			for (Piece pc : pieces) {
				buf.writeUtf(pc.name);
				buf.writeVarInt(pc.x0);
				buf.writeVarInt(pc.z0);
				buf.writeVarInt(pc.x1);
				buf.writeVarInt(pc.z1);
			}
			List<Column> racks = f.racks.subList(0, Math.min(8192, f.racks.size()));
			buf.writeVarInt(racks.size());
			for (Column c : racks) {
				buf.writeVarInt(c.x);
				buf.writeVarInt(c.z);
				buf.writeUtf(c.label);
				buf.writeVarInt(c.used);
				buf.writeVarInt(c.parts);
				buf.writeVarInt(c.levels);
				buf.writeVarLong(c.counted);
			}
		}
	}

	private static MapPayload read(RegistryFriendlyByteBuf buf) {
		BlockPos terminal = buf.readBlockPos();
		int version = buf.readVarInt();
		int n = Math.min(64, buf.readVarInt());
		List<Floor> floors = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			int y = buf.readVarInt(), x0 = buf.readVarInt(), z0 = buf.readVarInt(), w = buf.readVarInt(), d = buf.readVarInt();
			byte[] cells = buf.readByteArray(1 << 16);
			int np = Math.min(512, buf.readVarInt());
			List<Piece> pieces = new ArrayList<>();
			for (int k = 0; k < np; k++) {
				pieces.add(new Piece(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
			}
			int nc = Math.min(8192, buf.readVarInt());
			List<Column> racks = new ArrayList<>();
			for (int k = 0; k < nc; k++) {
				racks.add(new Column(buf.readVarInt(), buf.readVarInt(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarLong()));
			}
			floors.add(new Floor(y, x0, z0, w, d, cells, pieces, racks));
		}
		return new MapPayload(terminal, version, floors);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

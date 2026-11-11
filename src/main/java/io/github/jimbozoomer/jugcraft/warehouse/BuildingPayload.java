package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * The terminal's BUILDING page (part W3; server to a player with the terminal's screen open, every second;
 * WarehouseBuilding.view): which pre-made warehouse it is (empty kind: not one), its floors with what is on each and
 * their pallet places, its lifts, ADD A FLOOR (state, the staked floor's blocks in place, what it still needs), the
 * height it takes and the floors that still fit, and what it was built from.
 */
public record BuildingPayload(BlockPos terminal, String kind, String name, int width, int depth, int storey, int floors, String facing,
		List<FloorLine> lines, int lifts, int state, int done, int total, List<Need> needs, int y0, int y1, int room, String builtFrom,
		boolean party, String note) implements CustomPacketPayload {
	/** Not a pre-made warehouse; ready to add a floor; a floor staked and building; a floor left half built; no room above. */
	public static final int NOT_PREMADE = 0, READY = 1, ADDING = 2, INCOMPLETE = 3, FULL = 4;
	public static final Type<BuildingPayload> TYPE = new Type<>(Jugcraft.id("warehouse_building"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BuildingPayload> CODEC = StreamCodec.of(BuildingPayload::write, BuildingPayload::read);

	/** One floor: its number, what is on it, its pallet places in use and in all. */
	public record FloorLine(int n, String what, int used, int total) {
	}

	/** A block the staked (or next) floor still needs, and how many. */
	public record Need(ItemStack item, int count) {
	}

	private static void write(RegistryFriendlyByteBuf buf, BuildingPayload p) {
		buf.writeBlockPos(p.terminal);
		buf.writeUtf(p.kind);
		buf.writeUtf(p.name);
		buf.writeVarInt(p.width);
		buf.writeVarInt(p.depth);
		buf.writeVarInt(p.storey);
		buf.writeVarInt(p.floors);
		buf.writeUtf(p.facing);
		buf.writeVarInt(p.lines.size());
		for (FloorLine l : p.lines) {
			buf.writeVarInt(l.n);
			buf.writeUtf(l.what);
			buf.writeVarInt(l.used);
			buf.writeVarInt(l.total);
		}
		buf.writeVarInt(p.lifts);
		buf.writeVarInt(p.state);
		buf.writeVarInt(p.done);
		buf.writeVarInt(p.total);
		buf.writeVarInt(p.needs.size());
		for (Need n : p.needs) {
			ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, n.item);
			buf.writeVarInt(n.count);
		}
		buf.writeVarInt(p.y0);
		buf.writeVarInt(p.y1);
		buf.writeVarInt(p.room);
		buf.writeUtf(p.builtFrom);
		buf.writeBoolean(p.party);
		buf.writeUtf(p.note);
	}

	private static BuildingPayload read(RegistryFriendlyByteBuf buf) {
		BlockPos terminal = buf.readBlockPos();
		String kind = buf.readUtf(), name = buf.readUtf();
		int width = buf.readVarInt(), depth = buf.readVarInt(), storey = buf.readVarInt(), floors = buf.readVarInt();
		String facing = buf.readUtf();
		int n = Math.min(64, buf.readVarInt());
		List<FloorLine> lines = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			lines.add(new FloorLine(buf.readVarInt(), buf.readUtf(), buf.readVarInt(), buf.readVarInt()));
		}
		int lifts = buf.readVarInt(), state = buf.readVarInt(), done = buf.readVarInt(), total = buf.readVarInt();
		int k = Math.min(64, buf.readVarInt());
		List<Need> needs = new ArrayList<>(k);
		for (int i = 0; i < k; i++) {
			needs.add(new Need(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readVarInt()));
		}
		int y0 = buf.readVarInt(), y1 = buf.readVarInt(), room = buf.readVarInt();
		String builtFrom = buf.readUtf();
		boolean party = buf.readBoolean();
		String note = buf.readUtf();
		return new BuildingPayload(terminal, kind, name, width, depth, storey, floors, facing, lines, lifts, state, done, total, needs, y0, y1,
				room, builtFrom, party, note);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

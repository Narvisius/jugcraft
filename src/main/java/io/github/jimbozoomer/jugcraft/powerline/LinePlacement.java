package io.github.jimbozoomer.jugcraft.powerline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Building power lines: placing a structure (its footprint, its footing on uneven ground), wiring it to the line
 * being built, joining and cutting wires by hand, upgrading supports in place and taking a structure down. All of it
 * runs on the server.
 */
public final class LinePlacement {
	/** Set while a structure removes its own frame, so the frame blocks don't try to break it again. */
	public static final ThreadLocal<Boolean> CLEARING = ThreadLocal.withInitial(() -> false);
	/** Set while a support is swapped for a better one, so its wires survive. */
	public static final ThreadLocal<Boolean> UPGRADING = ThreadLocal.withInitial(() -> false);
	/** Lattice and bigger towers stand legs on uneven ground, up to this much height between their feet. */
	public static final int MAX_SLOPE = 12;

	/** The structure each player picked first when joining two by hand. */
	private static final Map<UUID, GlobalPos> SELECTED = new HashMap<>();

	private LinePlacement() {
	}

	/** Where a structure of this kind would stand if placed at {@code pos} (lifted to its highest foot), or null. */
	public static @Nullable BlockPos footing(Level level, BlockPos pos, LineKind kind) {
		return footing(level, pos, kind, prospect(level, pos, kind));
	}

	/**
	 * How a structure of this kind would stand placed at {@code pos}: wired to the structure it would wire itself to
	 * (turned to it, its angle body at the line's end), or alone.
	 */
	public static LineGeometry.Layout prospect(Level level, BlockPos pos, LineKind kind) {
		BlockPos partner = autoPartner(level, pos, kind);
		return LineGeometry.layout(kind, pos, partner == null ? List.of() : List.of(partner), LineWires.kinds(level));
	}

	/** {@link #footing(Level, BlockPos, LineKind)} for a layout: a tower's feet are where its body's footings are. */
	public static @Nullable BlockPos footing(Level level, BlockPos pos, LineKind kind, LineGeometry.Layout layout) {
		List<double[]> corners = layout.legCorners();
		if (kind.isBox() || corners.isEmpty()) {
			return pos;
		}
		int lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE;
		for (double[] c : corners) {
			int ground = groundBelow(level, pos.offset(layout.cellOf(c[0], 0, c[1])));
			if (ground == Integer.MIN_VALUE) {
				return null;
			}
			lowest = Math.min(lowest, ground);
			highest = Math.max(highest, ground);
		}
		if (highest - lowest > MAX_SLOPE) {
			return null;
		}
		return new BlockPos(pos.getX(), Math.max(pos.getY(), highest + 1), pos.getZ());
	}

	/** The top solid block under (or up to {@link #MAX_SLOPE} above) a foot, or MIN_VALUE if there is none in range. */
	private static int groundBelow(Level level, BlockPos foot) {
		for (int dy = MAX_SLOPE; dy >= -MAX_SLOPE; dy--) {
			BlockPos p = foot.offset(0, dy, 0);
			BlockState state = level.getBlockState(p);
			if (!state.canBeReplaced() && !state.getCollisionShape(level, p).isEmpty() && level.getBlockState(p.above()).canBeReplaced()) {
				return p.getY();
			}
		}
		return Integer.MIN_VALUE;
	}

	/** Whether the anchor and every frame block of a structure here are free (ignoring blocks of {@code ignore}). */
	public static boolean fits(Level level, BlockPos anchor, LineKind kind, @Nullable BlockPos ignore) {
		return fits(level, anchor, LineGeometry.frames(prospect(level, anchor, kind)), ignore);
	}

	/** Whether the anchor and these frame cells (offsets) are free (ignoring blocks of {@code ignore}). */
	public static boolean fits(Level level, BlockPos anchor, List<BlockPos> frames, @Nullable BlockPos ignore) {
		List<BlockPos> cells = new ArrayList<>(frames);
		cells.add(BlockPos.ZERO);
		for (BlockPos offset : cells) {
			BlockPos p = anchor.offset(offset);
			if (level.isOutsideBuildHeight(p)) {
				return false;
			}
			if (ignore != null && ignore.equals(LineRegistry.of(level).anchorOf(p))) {
				continue;
			}
			if (!level.getBlockState(p).canBeReplaced()) {
				return false;
			}
		}
		return true;
	}

	/** Places a structure's frame blocks (the anchor is already placed) where nothing else stands; returns those placed. */
	static List<BlockPos> placeFrames(Level level, BlockPos anchor, List<BlockPos> frames) {
		BlockState frame = JugcraftPowerLines.FRAME.defaultBlockState();
		List<BlockPos> placed = new ArrayList<>();
		for (BlockPos offset : frames) {
			BlockPos p = anchor.offset(offset);
			if (!level.isOutsideBuildHeight(p) && (level.getBlockState(p).canBeReplaced() || level.getBlockState(p).is(JugcraftPowerLines.FRAME))) {
				level.setBlock(p, frame, Block.UPDATE_ALL);
				placed.add(offset);
			}
		}
		return placed;
	}

	/** Removes a structure's frame blocks quietly (no drops; it is the anchor that drops the item). */
	static void clearFrames(ServerLevel level, BlockPos anchor, List<BlockPos> frames) {
		boolean was = CLEARING.get();
		CLEARING.set(true);
		try {
			for (BlockPos offset : frames) {
				BlockPos p = anchor.offset(offset);
				if (level.getBlockState(p).is(JugcraftPowerLines.FRAME) && anchor.equals(LineRegistry.of(level).anchorOf(p))) {
					level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		} finally {
			CLEARING.set(was);
		}
	}

	/** Places a structure from its item. Returns the anchor, or null with the reason sent to the player. */
	public static @Nullable BlockPos place(ServerLevel level, BlockPos at, LineKind kind, @Nullable Player player) {
		LineGeometry.Layout layout = player != null ? prospect(level, at, kind) : LineGeometry.layout(kind, at, List.of(), LineWires.kinds(level));
		BlockPos anchor = footing(level, at, kind, layout);
		if (anchor == null) {
			tell(player, "message.jugcraft.power_line.too_steep");
			return null;
		}
		List<BlockPos> frames = LineGeometry.frames(layout);
		if (!fits(level, anchor, frames, null)) {
			tell(player, "message.jugcraft.power_line.no_room");
			return null;
		}
		level.setBlock(anchor, JugcraftPowerLines.ANCHORS.get(kind).defaultBlockState(), Block.UPDATE_ALL);
		if (level.getBlockEntity(anchor) instanceof LineAnchorBlockEntity placed) {
			placed.setFrames(frames);
			placed.setFrames(placeFrames(level, anchor, frames));
		}
		if (player != null) {
			BlockPos partner = autoPartner(level, anchor, null);
			if (partner != null) {
				join(level, anchor, partner, player);
			}
		}
		LineNetworks.invalidate(level);
		return anchor;
	}

	/**
	 * Who a newly placed structure wires itself to: the nearest structure it can be wired to (the client's placement
	 * preview shows the same one). With {@code kind} set, the structure is not placed yet and is judged as that kind.
	 */
	public static @Nullable BlockPos autoPartner(Level level, BlockPos anchor, @Nullable LineKind kind) {
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Map.Entry<BlockPos, LineKind> entry : LineRegistry.of(level).anchors().entrySet()) {
			BlockPos other = entry.getKey();
			double d = other.distSqr(anchor);
			if (d < bestDistance && !other.equals(anchor) && (kind == null ? canJoin(level, anchor, other) : canJoinNew(level, anchor, kind, other)) == null) {
				best = other;
				bestDistance = d;
			}
		}
		return best;
	}

	/** Why two structures cannot be wired together (a message key), or null if they can. */
	public static @Nullable String canJoin(Level level, BlockPos a, BlockPos b) {
		if (a.equals(b) || !(level.getBlockEntity(a) instanceof LineAnchorBlockEntity ea) || !(level.getBlockEntity(b) instanceof LineAnchorBlockEntity eb)) {
			return "message.jugcraft.power_line.not_a_structure";
		}
		if (ea.connections().contains(b)) {
			return "message.jugcraft.power_line.already_wired";
		}
		return check(level, a, ea.kind(), ea.connections(), b, eb.kind(), eb.connections());
	}

	/** {@link #canJoin} for a structure of {@code kind} about to be placed at {@code a} (for the preview and placing). */
	public static @Nullable String canJoinNew(Level level, BlockPos a, LineKind kind, BlockPos b) {
		if (a.equals(b) || !(level.getBlockEntity(b) instanceof LineAnchorBlockEntity eb)) {
			return "message.jugcraft.power_line.not_a_structure";
		}
		return check(level, a, kind, List.of(), b, eb.kind(), eb.connections());
	}

	private static @Nullable String check(Level level, BlockPos a, LineKind ka, List<BlockPos> ca, BlockPos b, LineKind kb, List<BlockPos> cb) {
		if (!LineKind.compatible(ka, kb)) {
			return "message.jugcraft.power_line.incompatible";
		}
		double reach = Math.min(ka.reach, kb.reach);
		double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
		if (dx * dx + dz * dz > reach * reach || Math.abs(a.getY() - b.getY()) > reach / 2.0) {
			return "message.jugcraft.power_line.too_far";
		}
		if (ca.size() >= LineAnchorBlockEntity.MAX_CONNECTIONS || cb.size() >= LineAnchorBlockEntity.MAX_CONNECTIONS) {
			return "message.jugcraft.power_line.full";
		}
		if (!pathClear(level, a, ka, b, kb, ca, cb)) {
			return "message.jugcraft.power_line.blocked";
		}
		return null;
	}

	/** Whether the wires a new span would hang run through nothing solid (leaves and trees block them too). */
	static boolean pathClear(Level level, BlockPos a, LineKind ka, BlockPos b, LineKind kb, List<BlockPos> ca, List<BlockPos> cb) {
		List<BlockPos> withB = new ArrayList<>(ca);
		withB.add(b);
		List<BlockPos> withA = new ArrayList<>(cb);
		withA.add(a);
		LineWires.Span span = LineWires.span(level, a, ka, withB, b, kb, withA);
		List<LineWires.Wire> all = new ArrayList<>(span.conductors());
		all.addAll(span.shields());
		for (LineWires.Wire wire : all) {
			double length = wire.a().distanceTo(wire.b());
			int steps = Math.max(2, (int) Math.ceil(length * 2));
			for (int i = 1; i < steps; i++) {
				Vec3 p = wire.at(i / (double) steps);
				BlockPos cell = BlockPos.containing(p);
				BlockPos owner = LineRegistry.of(level).anchorOf(cell);
				if (a.equals(owner) || b.equals(owner)) {
					continue;
				}
				BlockState state = level.getBlockState(cell);
				if (!state.getCollisionShape(level, cell).isEmpty() || state.is(net.minecraft.tags.BlockTags.LEAVES)) {
					return false;
				}
			}
		}
		return true;
	}

	private static void join(ServerLevel level, BlockPos a, BlockPos b, @Nullable Player player) {
		if (level.getBlockEntity(a) instanceof LineAnchorBlockEntity ea && level.getBlockEntity(b) instanceof LineAnchorBlockEntity eb) {
			ea.connect(b);
			eb.connect(a);
			LineNetworks.invalidate(level);
			tell(player, "message.jugcraft.power_line.wired");
		}
	}

	private static void cut(ServerLevel level, BlockPos a, BlockPos b, @Nullable Player player) {
		if (level.getBlockEntity(a) instanceof LineAnchorBlockEntity ea) {
			ea.disconnect(b);
		}
		if (level.getBlockEntity(b) instanceof LineAnchorBlockEntity eb) {
			eb.disconnect(a);
		}
		LineNetworks.invalidate(level);
		tell(player, "message.jugcraft.power_line.cut");
	}

	/**
	 * Empty-hand use: sneaking picks the first structure, then the second, joining them (or cutting the wire if they
	 * are already joined); otherwise shows the line's readout.
	 */
	public static InteractionResult useStructure(Level level, BlockPos anchor, Player player) {
		if (!(level instanceof ServerLevel server) || !(server.getBlockEntity(anchor) instanceof LineAnchorBlockEntity entity)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			GlobalPos first = SELECTED.remove(player.getUUID());
			if (first == null || !first.dimension().equals(server.dimension()) || first.pos().equals(anchor)
					|| !(server.getBlockEntity(first.pos()) instanceof LineAnchorBlockEntity)) {
				SELECTED.put(player.getUUID(), GlobalPos.of(server.dimension(), anchor));
				tell(player, "message.jugcraft.power_line.selected");
				return InteractionResult.SUCCESS;
			}
			if (entity.connections().contains(first.pos())) {
				cut(server, anchor, first.pos(), player);
			} else {
				String why = canJoin(server, first.pos(), anchor);
				if (why == null) {
					join(server, first.pos(), anchor, player);
				} else {
					tell(player, why);
				}
			}
			return InteractionResult.SUCCESS;
		}
		LineNetworks.Network network = LineNetworks.networkOf(server, anchor);
		long rate = network == null ? entity.kind().capacity : network.rate();
		int structures = network == null ? 1 : network.size();
		player.sendOverlayMessage(Component.translatable("message.jugcraft.power_line.readout",
				Component.translatable(JugcraftPowerLines.ANCHORS.get(entity.kind()).getDescriptionId()), String.format("%,d", rate),
				entity.load() * 100 / 255, structures, entity.connections().size()));
		return InteractionResult.SUCCESS;
	}

	/**
	 * Swaps a support for a better one of the same family, keeping its wires; the old support's item goes back to the
	 * player. Returns PASS if the item is not an upgrade for this structure.
	 */
	public static InteractionResult upgrade(ServerLevel level, BlockPos anchor, Player player, ItemStack stack, LineKind to) {
		if (!(level.getBlockEntity(anchor) instanceof LineAnchorBlockEntity entity)) {
			return InteractionResult.PASS;
		}
		LineKind from = entity.kind();
		if (from.isBox() || from.family != to.family || to.tier <= from.tier) {
			return InteractionResult.PASS;
		}
		if (!fits(level, anchor, LineGeometry.frames(LineGeometry.layout(to, anchor, entity.connections(), LineWires.kinds(level))), anchor)) {
			tell(player, "message.jugcraft.power_line.no_room");
			return InteractionResult.FAIL;
		}
		List<BlockPos> wires = List.copyOf(entity.connections());
		UPGRADING.set(true);
		try {
			level.setBlock(anchor, JugcraftPowerLines.ANCHORS.get(to).defaultBlockState(), Block.UPDATE_ALL);
		} finally {
			UPGRADING.set(false);
		}
		if (level.getBlockEntity(anchor) instanceof LineAnchorBlockEntity upgraded) {
			upgraded.setFrames(List.of());
			upgraded.setConnections(wires);
		}
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
			ItemStack old = new ItemStack(JugcraftPowerLines.ITEMS.get(from));
			player.getInventory().placeItemBackInInventory(old, net.minecraft.util.Prediction.SERVER_ONLY);
		}
		LineNetworks.invalidate(level);
		tell(player, "message.jugcraft.power_line.upgraded");
		return InteractionResult.SUCCESS;
	}

	/** Forgets a player's selection (on logout). */
	public static void forget(UUID player) {
		SELECTED.remove(player);
	}

	private static void tell(@Nullable Player player, String key) {
		if (player != null) {
			player.sendOverlayMessage(Component.translatable(key));
		}
	}
}

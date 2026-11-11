package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintStacks;
import io.github.jimbozoomer.jugcraft.blueprint.BuildSite;
import io.github.jimbozoomer.jugcraft.blueprint.JugcraftBlueprints;
import io.github.jimbozoomer.jugcraft.blueprint.SurveyStakeBlockEntity;
import io.github.jimbozoomer.jugcraft.exterior.JugcraftExterior;
import io.github.jimbozoomer.jugcraft.exterior.WallPanelBlock;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A pre-made warehouse as a building (part W3; the terminal's BUILDING page): which one it is and how many floors it
 * has, found from where its terminal stands and what is built round it (so it works for one staked from a blueprint,
 * the medium nuclear plant's, or one whose floors were added since), and ADD A FLOOR (Justin, 2026-10-17: "on one of
 * the terminals that controls the warehouse, do an option that says add a floor").
 *
 * <p>The building is a stack (BlueprintStacks): its base (floor 1), middle floors and the top (the last floor and the
 * roof). A floor is added by staking {@code <kind>@add} (a middle floor and a new top) on the old top floor: what the
 * old top floor had and a middle floor doesn't (the ladder up to the roof hatch, the lift's drive) comes off and is
 * given back first, the roof's band is turned into plain wall, and the stake's drones swap the roof and parapet for
 * the new floor and build it, the roof going back on top. When the stake is done the terminal finds the new floor.
 */
public final class WarehouseBuilding {
	/** How much of a part's blocks must be there for it to count (sampled). A middle floor is 74 % like a top one. */
	private static final double TOP = 0.88, MIDDLE = 0.85, BASE = 0.8;

	/** A pre-made warehouse found round a terminal: its stack, the turn, its stake, its floors (incomplete: the top's roof is not on). */
	public record Found(BlueprintStacks.Stack stack, Rotation rotation, BlockPos terminal, BlockPos stake, int floors, boolean incomplete) {
		/** A position in the stack's parts' frame (the base's y) in the world. */
		public BlockPos world(int x, int y, int z) {
			BlockPos t = stack.terminal();
			return terminal.offset(new BlockPos(x - t.getX(), y - t.getY(), z - t.getZ()).rotate(rotation));
		}

		/** Where the stake of the floor added to this building stands. */
		public BlockPos addStake() {
			BlockPos a = stack.addAnchor();
			return world(a.getX(), a.getY() + (floors - 1) * stack.storey(), a.getZ());
		}

		public Direction facing() {
			// The blueprint's front (its stake's side) is south as drawn.
			return rotation.rotate(Direction.SOUTH);
		}
	}

	private WarehouseBuilding() {
	}

	public static void register() {
		BlueprintStacks.COMPLETED.add(WarehouseBuilding::completed);
	}

	/** The pre-made warehouse whose terminal this is, or null. */
	public static @Nullable Found detect(ServerLevel level, BlockPos terminal) {
		BlockState state = level.getBlockState(terminal);
		if (!(state.getBlock() instanceof PieceBlock) || !state.hasProperty(PieceBlock.FACING)) {
			return null;
		}
		Direction facing = state.getValue(PieceBlock.FACING);
		Rotation rotation = Rotation.NONE;
		for (Rotation r : Rotation.values()) {
			if (r.rotate(Direction.NORTH) == facing) {
				rotation = r;
			}
		}
		for (BlueprintStacks.Stack stack : BlueprintStacks.all()) {
			Blueprint base = Blueprint.get(stack.base(), false), mid = Blueprint.get(stack.middle(), false), top = Blueprint.get(stack.top(), false);
			if (base == null || mid == null || top == null) {
				continue;
			}
			BlockPos a = stack.anchor();
			Found f = new Found(stack, rotation, terminal, BlockPos.ZERO, 0, false);
			if (match(level, f, base, 0, 6) < BASE) {
				continue;
			}
			BlockPos stake = f.world(a.getX(), a.getY(), a.getZ());
			int k = 1;
			while (k < BlueprintStacks.MAX) {
				int dy = k * stack.storey();
				if (f.world(0, dy + stack.topHeight() - 1, 0).getY() > level.getMaxY()) {
					break;
				}
				if (match(level, f, top, dy, 3) >= TOP) {
					return new Found(stack, rotation, terminal, stake, k + 1, false);
				}
				if (match(level, f, mid, dy, 4) < MIDDLE) {
					break;
				}
				k++;
			}
			// No roof found: the floors under it stand (an added floor left half built, or the roof taken off).
			return k >= 2 ? new Found(stack, rotation, terminal, stake, k, true) : null;
		}
		return null;
	}

	/** The share of a part's blocks (every step-th) that are there, the part's bottom dy over the base's. */
	private static double match(ServerLevel level, Found f, Blueprint part, int dy, int step) {
		int seen = 0, hits = 0;
		for (int i = 0; i < part.size(); i += step) {
			BlockPos o = part.offset(i, Rotation.NONE);
			BlockPos w = f.world(o.getX() + part.anchorX(), o.getY() + part.anchorY() + dy, o.getZ() + part.anchorZ());
			if (!level.isLoaded(w)) {
				return 0;
			}
			seen++;
			if (level.getBlockState(w).is(part.state(i, Rotation.NONE).getBlock())) {
				hits++;
			}
		}
		return seen == 0 ? 0 : hits / (double) seen;
	}

	/** The staked floor's stake, if it is still there (else the terminal forgets it). */
	static @Nullable SurveyStakeBlockEntity pendingStake(ServerLevel level, TerminalBlockEntity t) {
		BlockPos at = t.pendingFloor();
		if (at == null) {
			return null;
		}
		if (level.isLoaded(at) && level.getBlockEntity(at) instanceof SurveyStakeBlockEntity stake && BlueprintStacks.isAdd(stake.blueprintId())) {
			return stake;
		}
		if (level.isLoaded(at)) {
			t.setPendingFloor(null, 0);
		}
		return null;
	}

	// ------------------------------------------------------------------ ADD A FLOOR

	public static boolean addFloor(ServerLevel level, TerminalBlockEntity t, ServerPlayer player, boolean party) {
		if (pendingStake(level, t) != null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.floor_busy"));
			return false;
		}
		Found f = detect(level, t.getBlockPos());
		if (f == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.not_premade"));
			return false;
		}
		BlueprintStacks.Stack stack = f.stack();
		if (f.stake().getY() + stack.topAbove(f.floors() + 1) > level.getMaxY()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.floor_too_tall", level.getMaxY() + 1));
			return false;
		}
		Blueprint add = Blueprint.get(stack.addId(), false), top = Blueprint.get(stack.top(), false);
		BlockPos stakePos = f.addStake();
		if (add == null || top == null) {
			return false;
		}
		if (!level.isLoaded(stakePos) || !level.getBlockState(stakePos).canBeReplaced()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.floor_no_room"));
			return false;
		}
		BuildSite site = new BuildSite(add, f.rotation(), stakePos);
		// The old top floor: what a middle floor hasn't comes off and goes back to the player; its roof band's wall
		// panels turn plain (the same block: drones would leave them).
		Map<Item, Integer> back = new LinkedHashMap<>();
		Set<BlockPos> pieces = new HashSet<>();
		int dy = (f.floors() - 1) * stack.storey();
		for (int i = 0; i < top.size(); i++) {
			BlockPos o = top.offset(i, Rotation.NONE);
			BlockPos w = f.world(o.getX() + top.anchorX(), o.getY() + top.anchorY() + dy, o.getZ() + top.anchorZ());
			BlockState now = level.getBlockState(w);
			if (!level.isLoaded(w) || !now.is(top.state(i, Rotation.NONE).getBlock())) {
				continue;
			}
			int k = site.find(w);
			BlockState want = k < 0 ? null : site.state(k);
			if (now.getBlock() instanceof PieceBlock piece && (want == null || !want.is(now.getBlock()))) {
				BlockPos origin = piece.origin(w, now);
				if (pieces.add(origin)) {
					back.merge(now.getBlock().asItem(), 1, Integer::sum);
					level.removeBlock(origin, false);
				}
			} else if (want == null) {
				back.merge(now.getBlock().asItem(), 1, Integer::sum);
				level.removeBlock(w, false);
			} else if (want.is(now.getBlock()) && now.is(JugcraftExterior.WALL_PANEL) && !want.equals(now)) {
				level.setBlock(w, want.setValue(WallPanelBlock.LIT, now.getValue(WallPanelBlock.LIT)), Block.UPDATE_ALL);
			}
		}
		back.remove(Items.AIR);
		level.setBlockAndUpdate(stakePos, JugcraftBlueprints.STAKE.defaultBlockState());
		if (!(level.getBlockEntity(stakePos) instanceof SurveyStakeBlockEntity stake)) {
			return false;
		}
		stake.setup(stack.addId(), f.rotation(), player.getUUID());
		stake.setMode(party ? UseMode.PARTY : UseMode.PERSONAL);
		stake.setLink(t.getBlockPos());
		t.setPendingFloor(stakePos, f.floors());
		List<String> given = new ArrayList<>();
		for (Map.Entry<Item, Integer> e : back.entrySet()) {
			give(player, new ItemStack(e.getKey(), e.getValue()));
			given.add(e.getValue() + " " + new ItemStack(e.getKey()).getHoverName().getString());
		}
		if (!given.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.warehouse.floor_refund", String.join(", ", given)));
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.warehouse.floor_staked", f.floors() + 1, party ? "PARTY" : "PERSONAL"));
		return true;
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		while (!stack.isEmpty()) {
			ItemStack part = stack.split(Math.max(1, stack.getMaxStackSize()));
			if (!player.getInventory().add(part)) {
				net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), part);
			}
		}
	}

	public static boolean cancel(ServerLevel level, TerminalBlockEntity t, ServerPlayer player) {
		SurveyStakeBlockEntity stake = pendingStake(level, t);
		if (stake == null || !stake.mayChange(player)) {
			return false;
		}
		int floor = t.pendingFrom() + 1;
		stake.remove(player);
		t.setPendingFloor(null, 0);
		player.sendSystemMessage(Component.translatable("message.jugcraft.warehouse.floor_cancelled", floor));
		return true;
	}

	/** BUILD WITH: the staked floor's drones from its owner's own items or the party's. */
	public static boolean setMode(ServerLevel level, TerminalBlockEntity t, ServerPlayer player, boolean party) {
		SurveyStakeBlockEntity stake = pendingStake(level, t);
		if (stake == null || !stake.mayChange(player)) {
			return false;
		}
		stake.setMode(party ? UseMode.PARTY : UseMode.PERSONAL);
		return true;
	}

	/** A stake's build done: an added floor tells its terminal; a whole pre-made warehouse tells the one in it who staked it. */
	private static void completed(ServerLevel level, SurveyStakeBlockEntity stake) {
		String id = stake.blueprintId();
		BlueprintStacks.Stack stack = BlueprintStacks.of(id);
		if (stack == null) {
			return;
		}
		String who = "";
		if (stake.owner() != null && level.getServer().getPlayerList().getPlayer(stake.owner()) instanceof ServerPlayer p) {
			who = p.getName().getString();
		}
		if (BlueprintStacks.isAdd(id)) {
			if (stake.link() != null && level.getBlockEntity(stake.link()) instanceof TerminalBlockEntity t) {
				int floors = t.pendingFrom() + 1;
				t.setPendingFloor(null, 0);
				t.markChanged();
				if (stake.owner() != null && level.getServer().getPlayerList().getPlayer(stake.owner()) instanceof ServerPlayer p) {
					p.sendSystemMessage(Component.translatable("message.jugcraft.warehouse.floor_built", floors, floors));
				}
			}
			return;
		}
		BlockPos a = stack.anchor(), tp = stack.terminal();
		BlockPos at = stake.getBlockPos().offset(new BlockPos(tp.getX() - a.getX(), tp.getY() - a.getY(), tp.getZ() - a.getZ()).rotate(stake.rotation()));
		if (level.getBlockEntity(at) instanceof TerminalBlockEntity t) {
			t.setBuiltFrom(who, BlueprintStacks.floors(id));
		}
	}

	// ------------------------------------------------------------------ the page

	/** The BUILDING page as it is now. */
	public static BuildingPayload view(ServerLevel level, TerminalBlockEntity t, Network network, Set<BlockPos> occupied) {
		Found f = t.building(level);
		SurveyStakeBlockEntity stake = pendingStake(level, t);
		List<BuildingPayload.FloorLine> lines = floors(network, occupied);
		Set<Long> liftColumns = new HashSet<>();
		for (Network.Landing l : network.landings) {
			liftColumns.add(BlockPos.asLong(l.origin().getX(), 0, l.origin().getZ()));
		}
		if (f == null) {
			return new BuildingPayload(t.getBlockPos(), "", "", 0, 0, 0, Math.max(1, network.floors.size()), "", lines, liftColumns.size(),
					BuildingPayload.NOT_PREMADE, 0, 0, List.of(), 0, 0, 0, "", false, "");
		}
		BlueprintStacks.Stack s = f.stack();
		int floors = f.floors();
		int state = stake != null ? BuildingPayload.ADDING : f.incomplete() ? BuildingPayload.INCOMPLETE : BuildingPayload.READY;
		int room = s.maxFloors(f.stake().getY(), level.getMaxY()) - floors;
		if (state == BuildingPayload.READY && room <= 0) {
			state = BuildingPayload.FULL;
		}
		// The staked floor's progress and what it still needs (or what the next one would).
		int done = 0, total = 0;
		Map<Item, Integer> need = new LinkedHashMap<>();
		BuildSite site = stake != null ? stake.site() : t.nextFloorSite(level, f);
		if (site != null) {
			total = site.size();
			for (int i = 0; i < site.size(); i++) {
				BlockState now = level.getBlockState(site.blockPos(i));
				if (site.matches(now, i)) {
					done++;
				} else {
					need.merge(site.state(i).getBlock().asItem(), 1, Integer::sum);
				}
			}
		}
		need.remove(Items.AIR);
		List<BuildingPayload.Need> needs = new ArrayList<>();
		need.entrySet().stream().sorted(Map.Entry.<Item, Integer>comparingByValue().reversed()).limit(14)
				.forEach(e -> needs.add(new BuildingPayload.Need(new ItemStack(e.getKey()), e.getValue())));
		int y0 = f.stake().getY() - s.anchor().getY();
		int y1 = f.stake().getY() + s.topAbove(floors + (stake != null ? 1 : 0));
		String from = t.builtBy().isEmpty() ? s.name() + " (found by its layout)"
				: s.name() + " blueprint  -  staked by " + t.builtBy() + "  -  " + t.floorsChosen() + " floors chosen";
		return new BuildingPayload(t.getBlockPos(), s.kind(), s.name(), s.hallWidth(), s.hallDepth(), s.storey(), floors,
				f.facing().getSerializedName().toUpperCase(Locale.ROOT), lines, liftColumns.size(), state, done, total, needs, y0, y1, Math.max(0, room),
				from, stake != null && stake.mode() == UseMode.PARTY, "");
	}

	/** Each floor's line: its aisles, docks and the machines on it; its pallet places in use and in all. */
	private static List<BuildingPayload.FloorLine> floors(Network network, Set<BlockPos> occupied) {
		int n = Math.max(1, network.floors.size());
		List<BuildingPayload.FloorLine> out = new ArrayList<>();
		for (int fl = 0; fl < n && fl < 64; fl++) {
			int used = 0, all = 0;
			for (Network.Place p : network.places) {
				if (network.floor(p.pos().getY()) == fl) {
					all++;
					if (occupied.contains(p.pos())) {
						used++;
					}
				}
			}
			List<String> what = new ArrayList<>();
			List<String> aisles = new ArrayList<>();
			for (Network.Aisle a : network.aisles) {
				if (a.floor() == fl) {
					aisles.add(a.name());
				}
			}
			if (!aisles.isEmpty()) {
				what.add("aisles " + aisles.get(0) + (aisles.size() > 1 ? "-" + aisles.get(aisles.size() - 1) : ""));
			}
			final int floor = fl;
			long docks = network.docks.stream().filter(d -> network.floor(d.getY()) == floor).count();
			if (docks > 0) {
				what.add(docks + (docks == 1 ? " dock" : " docks"));
			}
			long intakes = network.intakes.stream().filter(i -> network.floor(i.origin().getY()) == floor).count();
			long stations = network.stations.stream().filter(i -> network.floor(i.origin().getY()) == floor).count();
			long vlms = network.vlms.stream().filter(i -> network.floor(i.origin().getY()) == floor).count();
			long landings = network.landings.stream().filter(i -> network.floor(i.origin().getY()) == floor).count();
			if (intakes > 0) {
				what.add(intakes == 1 ? "intake" : intakes + " intakes");
			}
			if (stations > 0) {
				what.add(stations == 1 ? "1 station" : stations + " stations");
			}
			if (vlms > 0) {
				what.add(vlms == 1 ? "Ready Storage" : vlms + " Ready Storages");
			}
			if (landings > 0) {
				what.add(landings == 1 ? "lift landing" : landings + " lift landings");
			}
			out.add(new BuildingPayload.FloorLine(fl + 1, String.join("  -  ", what), used, all));
		}
		return out;
	}
}

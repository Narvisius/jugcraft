package io.github.jimbozoomer.jugcraft.tower.crew;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerRegistry;
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;

/**
 * Keeps each Drone Tower's crew. New hands and replacements don't just appear (the drop ship sheet, approved 6 November
 * 2026): they come in on a crew drop ship ({@link CrewShip}), on the half hour ({@link #SHIP_EVERY}), when at least
 * {@link #BATCH} are waiting, or a whole job's staff is gone (all the janitors, all the security), or the manager is
 * needed; a new tower's crew, or a new tier's, come a minute after it's built ({@link #NEW_DELAY}). One ship at a time.
 * With nobody near the tower (no player within {@link #NEAR} blocks) there's no ship: posts are filled as before, at
 * once for a new post and {@link #REPLACE_TICKS} after one falls empty, in a face nobody in the tower wears. Only towers
 * whose posts are in loaded, ticking chunks are seen to (the core keeps its own chunks loaded), and nothing is hired
 * while a tower is being rebuilt as a new design.
 */
public final class TowerCrewCare {
	/** How often (ticks) the towers' posts are looked at. */
	public static final int CHECK_TICKS = 100;
	/** How long a post stands empty before the tower hires again (ticks: five minutes). */
	public static final int REPLACE_TICKS = 6000;
	/** How far from a post a crew member who went missing is looked for before someone new is hired. */
	public static final double SEARCH = 64;
	/** The drop ship's schedule (ticks: half an hour), the least it brings unless a whole staff or the manager is
	 * waiting, the wait for a new tower's (or tier's) crew, and how near a player must be for a ship to come at all. */
	public static final int SHIP_EVERY = 36000, BATCH = 5, NEW_DELAY = 1200, NEAR = 160;
	/** Since when each tower (its core) has had a post never staffed, waiting on the new-tower ship (not saved). */
	private static final java.util.Map<Long, Long> NEW_SINCE = new java.util.HashMap<>();

	private TowerCrewCare() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(TowerCrewCare::tick);
		CrewShipLights.register();
		// the towers' walk maps (CrewRoutes) are this world's: another world's towers are other towers
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> CrewRoutes.clear());
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(
				net.minecraft.commands.Commands.literal("crewship").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
						.executes(context -> callShip(context.getSource()))));
	}

	/** /crewship: sends a drop ship to the nearest tower now with whoever it is waiting for (maybe nobody: a fly-in to
	 * watch), to its landing spot or its roof. */
	private static int callShip(net.minecraft.commands.CommandSourceStack source) {
		ServerLevel level = source.getLevel();
		BlockPos best = null;
		double bestD = Double.MAX_VALUE;
		for (BlockPos core : TowerRegistry.get(level).allCores()) {
			double d = core.distToCenterSqr(source.getPosition());
			if (d < bestD) {
				bestD = d;
				best = core;
			}
		}
		if (best == null || !(level.getBlockEntity(best) instanceof TowerCoreBlockEntity tower)) {
			source.sendFailure(net.minecraft.network.chat.Component.literal("No Drone Tower found in this dimension (a tower lists itself "
					+ "once its core has loaded: go to it and try again)."));
			return 0;
		}
		if (tower.layout().crew().isEmpty()) {
			source.sendFailure(net.minecraft.network.chat.Component.literal("This tower's design has no crew yet (it is being refitted: try again "
					+ "when it's done)."));
			return 0;
		}
		if (CrewShip.active(level, best) != null) {
			source.sendFailure(net.minecraft.network.chat.Component.literal("A ship is already on its way."));
			return 0;
		}
		List<String> due = due(level, tower).stream().map(CrewPost::id).toList();
		CrewShip ship = dispatch(level, tower, due);
		if (ship == null) {
			source.sendFailure(net.minecraft.network.chat.Component.literal("Nowhere for the ship to set down."));
			return 0;
		}
		source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Drop ship inbound with " + due.size() + " crew"
				+ (ship.roof() ? " (to the roof)." : ".")), true);
		return 1;
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % CHECK_TICKS != 0) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			for (BlockPos core : TowerRegistry.get(level).allCores()) {
				if (level.isLoaded(core) && level.isPositionEntityTicking(core) && level.getBlockEntity(core) instanceof TowerCoreBlockEntity tower) {
					staff(level, tower);
				}
			}
		}
	}

	/** Whether a post's part of the tower is built. */
	public static boolean open(TowerCoreBlockEntity tower, CrewPost post) {
		if (post.step() == 0) {
			return tower.foundationState() == 2 || tower.tier() >= 1;
		}
		return tower.tier() >= post.step();
	}

	/** Sees to one tower's posts: those never staffed or fallen empty are filled, by drop ship or (nobody near) at once. */
	public static void staff(ServerLevel level, TowerCoreBlockEntity tower) {
		if (tower.converting()) {
			return;
		}
		List<CrewPost> posts = tower.layout().crew();
		if (posts.isEmpty()) {
			return;
		}
		TowerCrewState state = TowerCrewState.get(level);
		BlockPos core = tower.getBlockPos();
		long now = level.getGameTime();
		List<CrewPost> due = due(level, tower);
		boolean fresh = false;
		for (CrewPost post : due) {
			fresh |= state.slot(core, post.id()) == null;
		}
		if (level.getNearestPlayer(core.getX() + 0.5, core.getY(), core.getZ() + 0.5, NEAR, false) == null) {
			hireAtOnce(level, tower, due);
			return;
		}
		if (due.isEmpty() || CrewShip.active(level, core) != null) {
			return;
		}
		boolean go = false;
		if (fresh) {
			long since = NEW_SINCE.computeIfAbsent(core.asLong(), k -> now);
			go = now - since >= NEW_DELAY;
		}
		if (!go && now % SHIP_EVERY < CHECK_TICKS) {
			go = flies(due, posts, tower);
		}
		if (go) {
			NEW_SINCE.remove(core.asLong());
			if (dispatch(level, tower, due.stream().map(CrewPost::id).toList()) == null) {
				for (CrewPost post : due) {                 // nowhere to land (no roof yet either): hired as before
					TowerCrewState.Slot slot = state.slot(core, post.id());
					hire(level, core, post, state, slot == null ? 0 : slot.hires());
				}
			}
		}
	}

	/**
	 * With nobody near to see a ship come: as before, a new post filled at once, an empty one after a while
	 * ({@link #REPLACE_TICKS}). The game tests hire this way (a test's mock players stand near other tests' towers).
	 */
	public static void hireAtOnce(ServerLevel level, TowerCoreBlockEntity tower) {
		if (!tower.converting()) {
			hireAtOnce(level, tower, due(level, tower));
		}
	}

	private static void hireAtOnce(ServerLevel level, TowerCoreBlockEntity tower, List<CrewPost> due) {
		TowerCrewState state = TowerCrewState.get(level);
		BlockPos core = tower.getBlockPos();
		long now = level.getGameTime();
		NEW_SINCE.remove(core.asLong());
		for (CrewPost post : due) {
			TowerCrewState.Slot slot = state.slot(core, post.id());
			if (slot == null || now - slot.vacant() >= REPLACE_TICKS) {
				hire(level, core, post, state, slot == null ? 0 : slot.hires());
			}
		}
	}

	/** The drop ship's rule: it flies for {@link #BATCH} or more, a whole job's staff, or the manager. */
	public static boolean flies(List<CrewPost> due, List<CrewPost> posts, TowerCoreBlockEntity tower) {
		return flies(due, posts, p -> open(tower, p));
	}

	/** The rule, with which posts are open given. */
	public static boolean flies(List<CrewPost> due, List<CrewPost> posts, java.util.function.Predicate<CrewPost> open) {
		if (due.size() >= BATCH) {
			return true;
		}
		for (CrewPost d : due) {
			if (d.job().equals("manager")) {
				return true;
			}
			boolean whole = true;
			for (CrewPost p : posts) {
				if (p.job().equals(d.job()) && open.test(p) && !due.contains(p)) {
					whole = false;
					break;
				}
			}
			if (whole) {
				return true;
			}
		}
		return false;
	}

	/** The tower's open posts with nobody standing for them (marking those whose hand went missing as empty). */
	public static List<CrewPost> due(ServerLevel level, TowerCoreBlockEntity tower) {
		TowerCrewState state = TowerCrewState.get(level);
		BlockPos core = tower.getBlockPos();
		long now = level.getGameTime();
		List<CrewPost> out = new java.util.ArrayList<>();
		for (CrewPost post : tower.layout().crew()) {
			BlockPos home = core.offset(post.home());
			if (!open(tower, post) || !level.isPositionEntityTicking(home)) {
				continue;
			}
			TowerCrewState.Slot slot = state.slot(core, post.id());
			if (slot != null && slot.crew().isPresent()) {
				Entity entity = level.getEntity(slot.crew().get());
				if (!(entity instanceof TowerCrew crew && crew.isAlive())) {
					// not to be found where they should be: the post counts as empty from now
					state.vacate(core, post.id(), null, now);
				}
				continue;
			}
			TowerCrew back = find(level, core, post, home);
			if (back != null) {
				// the hand on record turned up after all (or one was never recorded): they keep the post
				if (slot == null) {
					state.hired(core, post.id(), back.getUUID(), back.skin());
				} else {
					state.found(core, post.id(), back.getUUID());
				}
				continue;
			}
			out.add(post);
		}
		return out;
	}

	/** Sends a drop ship for the tower bringing {@code posts} (security first in the file, {@link #file}; who comes for each
	 * chosen now, so they ride in seated, {@link #hires}); null if it has nowhere to set down. */
	public static @Nullable CrewShip dispatch(ServerLevel level, TowerCoreBlockEntity tower, List<String> posts) {
		CrewShipSite.Site site = CrewShipSite.find(level, tower.getBlockPos(), tower.tier());
		if (site == null) {
			return null;
		}
		CrewShip ship = JugcraftTower.CREW_SHIP.create(level, EntitySpawnReason.EVENT);
		if (ship == null) {
			return null;
		}
		List<String> filed = file(tower, posts);
		ship.setUp(tower.getBlockPos(), site, filed, hires(level, tower, filed), level.getGameTime());
		return level.addFreshEntity(ship) ? ship : null;
	}

	/** Who comes for each of these posts (in order), each in a face nobody in the tower wears nor anyone before them aboard. */
	static List<CrewRoster.Hire> hires(ServerLevel level, TowerCoreBlockEntity tower, List<String> posts) {
		BlockPos core = tower.getBlockPos();
		TowerCrewState state = TowerCrewState.get(level);
		java.util.Set<String> worn = new java.util.HashSet<>(state.worn(core));
		List<CrewRoster.Hire> out = new java.util.ArrayList<>();
		for (String id : posts) {
			CrewPost post = tower.layout().crew().stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
			if (post == null) {
				out.add(CrewRoster.get().hire("operator", id, 0, core.asLong(), worn));   // (a post gone from the design: never boards)
				continue;
			}
			TowerCrewState.Slot slot = state.slot(core, id);
			CrewRoster.Hire hire = CrewRoster.get().hire(post.job(), id, slot == null ? 0 : slot.hires(), core.asLong(), worn);
			worn.add(hire.look().skin());
			out.add(hire);
		}
		return out;
	}

	/** The order they sit and come off in (C4): all the security first (the seats nearest the ramp; off first, to cover it), then the rest. */
	static List<String> file(TowerCoreBlockEntity tower, List<String> posts) {
		List<String> guards = new java.util.ArrayList<>(), rest = new java.util.ArrayList<>();
		for (String id : posts) {
			boolean guard = tower.layout().crew().stream().anyMatch(p -> p.id().equals(id) && p.job().equals("security"));
			(guard ? guards : rest).add(id);
		}
		guards.addAll(rest);
		return guards;
	}

	/**
	 * Hires {@code hire} for post {@code postId} aboard a drop ship as it touches down: they're put in their seat at
	 * {@code at} facing {@code yaw} ({@code seated}: on a seat; else standing in the aisle) to wait till the ship calls them
	 * off ({@link TowerCrew#disembark}); null if the post has someone after all. A hire not chosen beforehand is chosen now.
	 */
	public static @Nullable TowerCrew board(ServerLevel level, TowerCoreBlockEntity tower, String postId, CrewRoster.@Nullable Hire hire,
			net.minecraft.world.phys.Vec3 at, float yaw, boolean seated, java.util.UUID ship) {
		CrewPost post = tower.layout().crew().stream().filter(p -> p.id().equals(postId)).findFirst().orElse(null);
		if (post == null) {
			return null;
		}
		BlockPos core = tower.getBlockPos();
		TowerCrewState state = TowerCrewState.get(level);
		TowerCrewState.Slot slot = state.slot(core, post.id());
		if (slot != null && slot.crew().isPresent() && level.getEntity(slot.crew().get()) instanceof TowerCrew c && c.isAlive()) {
			return null;
		}
		TowerCrew crew = JugcraftTower.TOWER_CREW.create(level, EntitySpawnReason.EVENT);
		if (crew == null) {
			return null;
		}
		if (hire == null) {
			hire = CrewRoster.get().hire(post.job(), post.id(), slot == null ? 0 : slot.hires(), core.asLong(), state.worn(core));
		}
		crew.snapTo(at.x, at.y, at.z, yaw, 0.0F);
		crew.setUp(core, post, hire);
		crew.board(at, yaw, seated, ship);
		if (!level.addFreshEntity(crew)) {
			return null;
		}
		state.hired(core, post.id(), crew.getUUID(), hire.look().skin());
		crew.seatAboard();
		return crew;
	}

	private static @Nullable TowerCrew find(ServerLevel level, BlockPos core, CrewPost post, BlockPos home) {
		List<TowerCrew> found = level.getEntitiesOfClass(TowerCrew.class, new AABB(home).inflate(SEARCH),
				c -> c.isAlive() && c.core().equals(core) && c.postId().equals(post.id()));
		return found.isEmpty() ? null : found.getFirst();
	}

	/** Hires someone new for a post: they appear at its home (beside the chair, or the first spot). */
	public static @Nullable TowerCrew hire(ServerLevel level, BlockPos core, CrewPost post, TowerCrewState state, int hires) {
		TowerCrew crew = JugcraftTower.TOWER_CREW.create(level, EntitySpawnReason.STRUCTURE);
		if (crew == null) {
			return null;
		}
		CrewRoster.Hire hire = CrewRoster.get().hire(post.job(), post.id(), hires, core.asLong(), state.worn(core));
		BlockPos home = post.outside() ? TowerCrew.ground(level, core.offset(post.home())) : core.offset(post.home());
		crew.snapTo(home.getX() + 0.5, TowerCrew.floor(level, home), home.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
		crew.setUp(core, post, hire);
		if (!level.addFreshEntity(crew)) {
			Jugcraft.LOGGER.warn("Tower crew: could not hire for {} at {}", post.id(), home);
			return null;
		}
		state.hired(core, post.id(), crew.getUUID(), hire.look().skin());
		return crew;
	}
}

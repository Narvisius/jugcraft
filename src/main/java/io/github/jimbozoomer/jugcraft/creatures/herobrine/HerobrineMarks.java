package io.github.jimbozoomer.jugcraft.creatures.herobrine;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.town.Town;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Herobrine's marks (docs/features/herobrine.md): what he leaves behind after a confirmed distant sighting, and the
 * saved record of every block a mark changed, so an operator can take any mark back. A mark is planned by
 * {@link HerobrineMarkPlans} only in untouched wilderness, then built {@value #BLOCKS_PER_TICK} blocks a tick (shared
 * fairly among the marks building), each block checked again just before it changes: if its chunk is not loaded it
 * waits; if the land became claimed or town, the block is no longer what was planned on, or it holds a block entity,
 * the mark stops there and never overwrites what changed. Undoing a mark restores only blocks still exactly as the mark left them, every
 * property and their contents; anything a player changed since stays as they left it.
 */
public final class HerobrineMarks extends SavedData {
	/** How many blocks all marks together change in one tick. */
	public static final int BLOCKS_PER_TICK = 8;
	/** The chance a confirmed distant sighting leaves a mark. */
	public static final double MARK_CHANCE = 0.35;
	/** How many marks may be building at once. */
	public static final int MAX_JOBS = 2;
	/** How many ticks a mark waits for an unloaded chunk or someone standing in the way before giving up. */
	public static final int MAX_WAITS = 200;
	/** How many sites one mark may weigh before giving up (each is a bounded scan of loaded blocks). */
	public static final int MAX_PLAN_TRIES = 8;
	/** The chance a sighting moves a player along a trail, once one may begin or continue. */
	public static final double TRAIL_CHANCE = 0.5;
	/** The chance any other sighting near the shore raises a pyramid. */
	public static final double PYRAMID_CHANCE = 0.03;
	/** How far a trail's signpost may point to a pyramid, in blocks. */
	public static final int POINT_RANGE = 1024;

	/**
	 * One block a mark changed: what it was, what the mark made it, and (for a block that keeps contents) what it held
	 * when the mark finished it: the lines it wrote on a sign ({@link #asWritten} checks both sides, style, dye, glow and
	 * wax against them), or {@value #JOURNAL} for a lectern holding the journal.
	 */
	public record Change(BlockPos pos, BlockState before, BlockState after, String content) {
		static final Codec<Change> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(Change::pos),
				BlockState.CODEC.fieldOf("before").forGetter(Change::before),
				BlockState.CODEC.fieldOf("after").forGetter(Change::after),
				Codec.STRING.optionalFieldOf("content", "").forGetter(Change::content)).apply(instance, Change::new));
	}

	/** A lectern's recorded content: it holds the pyramid's journal. */
	public static final String JOURNAL = "journal";

	/**
	 * A mark's record: its kind, where he stood, when (game time), every block it changed, in order, and whether it was
	 * {@code finished}: built to its last block. A mark stopped part-built (by a claim, a player's block, a long wait or
	 * an undo) or partly undone is not finished; a trail never counts it and never points to it.
	 */
	public record Mark(String type, BlockPos at, long time, List<Change> changes, boolean finished) {
		static final Codec<Mark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("type").forGetter(Mark::type),
				BlockPos.CODEC.fieldOf("at").forGetter(Mark::at),
				Codec.LONG.fieldOf("time").forGetter(Mark::time),
				Change.CODEC.listOf().fieldOf("changes").forGetter(Mark::changes),
				Codec.BOOL.optionalFieldOf("finished", false).forGetter(Mark::finished)).apply(instance, Mark::new));

		public Mark {
			changes = new ArrayList<>(changes);
		}
	}

	/**
	 * Land an operator approved for the marks that take blocks away (groves and tunnels): a circle of {@code radius}
	 * blocks round {@code centre}, with who approved it and when (game time).
	 */
	public record Wild(BlockPos centre, int radius, String by, long time) {
		static final Codec<Wild> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BlockPos.CODEC.fieldOf("centre").forGetter(Wild::centre),
				Codec.INT.fieldOf("radius").forGetter(Wild::radius),
				Codec.STRING.fieldOf("by").forGetter(Wild::by),
				Codec.LONG.fieldOf("time").forGetter(Wild::time)).apply(instance, Wild::new));

		/** Whether a column lies inside this circle. */
		public boolean covers(int x, int z) {
			long dx = x - centre.getX();
			long dz = z - centre.getZ();
			return dx * dx + dz * dz <= (long) radius * radius;
		}
	}

	/** The smallest and largest radius an operator may approve, in blocks. */
	public static final int MIN_WILD_RADIUS = 16;
	public static final int MAX_WILD_RADIUS = 256;

	public static final Codec<HerobrineMarks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Codec.STRING, Mark.CODEC).fieldOf("marks").forGetter(marks -> marks.marks),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("trails", Map.of()).forGetter(marks -> marks.trails),
			Wild.CODEC.listOf().optionalFieldOf("wilds", List.of()).forGetter(marks -> marks.wilds))
			.apply(instance, HerobrineMarks::new));
	private static final SavedDataType<HerobrineMarks> TYPE = new SavedDataType<>(Jugcraft.id("herobrine_marks"),
			HerobrineMarks::new, CODEC, null);

	/** What an undo did: blocks put back, blocks left because a player changed them, and blocks waiting on unloaded land. */
	public record Undo(int restored, int kept, int waiting) {
	}

	private static final class Job {
		final String id;
		final ServerLevel level;
		final Deque<HerobrineMarkPlans.Step> steps;
		final Map<BlockPos, List<String>> signs;
		final List<BlockPos> lecterns;
		int waits;
		/** The player whose trail this mark moves on, to {@link #trailStep}, once it is finished; or null. */
		@Nullable UUID trailPlayer;
		int trailStep;

		Job(String id, ServerLevel level, HerobrineMarkPlans.Plan plan) {
			this.id = id;
			this.level = level;
			this.steps = new ArrayDeque<>(plan.steps());
			this.signs = plan.signs();
			this.lecterns = plan.lecterns();
		}
	}

	private enum Result {
		DONE, SKIP, WAIT, STOP
	}

	private static final List<Job> JOBS = new ArrayList<>();
	private final Map<String, Mark> marks;
	/** How far each player (by UUID) has followed a trail: 1 after the grove, 2 after the tunnel's signpost. */
	private final Map<String, Integer> trails;

	/** Land approved for groves and tunnels, oldest first. */
	private final List<Wild> wilds;

	public HerobrineMarks() {
		this(Map.of(), Map.of(), List.of());
	}

	private HerobrineMarks(Map<String, Mark> marks, Map<String, Integer> trails, List<Wild> wilds) {
		this.marks = new HashMap<>(marks);
		this.trails = new HashMap<>(trails);
		this.wilds = new ArrayList<>(wilds);
	}

	/** Land approved for groves and tunnels, oldest first. */
	public List<Wild> wilds() {
		return Collections.unmodifiableList(wilds);
	}

	/**
	 * Whether the box from {@code min} to {@code max}, with the plans' margin round it, lies wholly inside one circle an
	 * operator approved. Groves and tunnels fail closed without it: unclaimed land may still hold a plain stone or log
	 * build, or trees a player grew, which look natural.
	 */
	public static boolean approved(ServerLevel level, BlockPos min, BlockPos max) {
		int pad = HerobrineMarkPlans.MARGIN;
		int x0 = Math.min(min.getX(), max.getX()) - pad;
		int x1 = Math.max(min.getX(), max.getX()) + pad;
		int z0 = Math.min(min.getZ(), max.getZ()) - pad;
		int z1 = Math.max(min.getZ(), max.getZ()) + pad;
		return get(level.getServer()).wilds.stream().anyMatch(wild -> wild.covers(x0, z0) && wild.covers(x0, z1)
				&& wild.covers(x1, z0) && wild.covers(x1, z1));
	}

	/** Approves the circle of {@code radius} blocks (clamped) round {@code centre} for groves and tunnels. */
	public static Wild approve(ServerLevel level, BlockPos centre, int radius, String by) {
		HerobrineMarks marks = get(level.getServer());
		Wild wild = new Wild(centre.immutable(), Math.clamp(radius, MIN_WILD_RADIUS, MAX_WILD_RADIUS), by, level.getGameTime());
		marks.wilds.add(wild);
		marks.setDirty();
		return wild;
	}

	/** Withdraws an approval; marks already made stay until undone. Whether it was there. */
	public static boolean revoke(ServerLevel level, Wild wild) {
		HerobrineMarks marks = get(level.getServer());
		boolean removed = marks.wilds.remove(wild);
		marks.setDirty();
		return removed;
	}

	/** How far {@code player} has followed a trail (0 if they have not begun one). */
	public int trail(UUID player) {
		return trails.getOrDefault(player.toString(), 0);
	}

	public static HerobrineMarks get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	/** Every mark on record, by id. */
	public Map<String, Mark> marks() {
		return Collections.unmodifiableMap(marks);
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(HerobrineMarks::work);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> JOBS.clear());
		CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> commands(dispatcher));
	}

	/** Whether he leaves marks ({@code herobrine.marks}, on unless set to off). Undo works either way. */
	public static boolean enabled() {
		return !"off".equalsIgnoreCase(JugcraftConfig.textOption("herobrine.marks").trim());
	}

	/** Marks still being built. */
	public static int building() {
		return JOBS.size();
	}

	/** Whether mark {@code id} is still being built. */
	public static boolean building(String id) {
		return JOBS.stream().anyMatch(job -> job.id.equals(id));
	}

	/**
	 * After a confirmed distant sighting: now and then ({@value #MARK_CHANCE}) he leaves a mark where he stood. A sighting
	 * several players shared may leave "you brought someone".
	 */
	static void afterSighting(ServerLevel level, BlockPos where, ServerPlayer player, int witnesses) {
		RandomSource random = level.getRandom();
		if (!enabled() || JOBS.size() >= MAX_JOBS) {
			return;
		}
		HerobrineMarks marks = get(level.getServer());
		int trail = marks.trail(player.getUUID());
		// One trail mark at a time: while one is building, its step is not yet the player's.
		boolean trailing = JOBS.stream().anyMatch(job -> player.getUUID().equals(job.trailPlayer));
		if (!trailing && trail == 0 && acrossWater(level, player.blockPosition(), where) && random.nextDouble() < TRAIL_CHANCE) {
			beginTrail(level, where, player, random);
		} else if (!trailing && trail == 1 && random.nextDouble() < TRAIL_CHANCE) {
			continueTrail(level, where, player, random);
		} else if (random.nextDouble() < PYRAMID_CHANCE) {
			HerobrineMarkPlans.Plan plan = HerobrinePyramids.near(level, where, player.blockPosition(), random);
			if (plan != null) {
				start(level, plan);
			}
		} else if (random.nextDouble() < (level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				where.getX(), where.getZ()) > where.getY() + 8 ? 0.75 : MARK_CHANCE)) {
			leave(level, where, player, witnesses > 1, random, null, null);
		}
	}

	/**
	 * A trail's first mark: a bare grove where he stood (or, out of the trees or without approved land, a shrine) reading
	 * "YOU TOOK THE LONG WAY". The player's trail moves to 1 only once it is finished. Returns the mark's id, or null.
	 */
	public static @Nullable String beginTrail(ServerLevel level, BlockPos where, ServerPlayer player, RandomSource random) {
		List<String> text = HerobrineMarkPlans.MESSAGES.getFirst();
		String id = leave(level, where, player, false, random, HerobrineMarkPlans.Type.GROVE, text);
		if (id == null) {
			id = leave(level, where, player, false, random, HerobrineMarkPlans.Type.SHRINE, text);
		}
		return id == null ? null : onTrail(id, player, 1);
	}

	/**
	 * A trail's second mark: a torch-lit tunnel (or, without approved land, a lone signpost) whose sign gives the way and
	 * distance to the nearest finished pyramid within {@value #POINT_RANGE} blocks. With none finished it leaves no
	 * signpost: it raises a pyramid by the shore (unless one is already rising) and the trail waits for a later sighting,
	 * when that pyramid is finished (one stopped part-built is never pointed to; another is raised then). The player's
	 * trail moves to 2 only once the signpost is finished. Returns the signpost mark's id, or null.
	 */
	public static @Nullable String continueTrail(ServerLevel level, BlockPos where, ServerPlayer player, RandomSource random) {
		BlockPos pyramid = HerobrinePyramids.nearest(level, where, POINT_RANGE);
		if (pyramid == null) {
			if (!HerobrinePyramids.rising(level, where, POINT_RANGE)) {
				HerobrineMarkPlans.Plan plan = HerobrinePyramids.near(level, player.blockPosition(), where, random);
				if (plan != null) {
					start(level, plan);
				}
			}
			return null;
		}
		List<String> post = signpost(where, pyramid);
		String id = leave(level, where, player, false, random, HerobrineMarkPlans.Type.TUNNEL, post);
		if (id == null) {
			id = leave(level, where, player, false, random, HerobrineMarkPlans.Type.SIGN, post);
		}
		return id == null ? null : onTrail(id, player, 2);
	}

	/** Has the building mark {@code id} move {@code player}'s trail to {@code step} once it is finished. */
	private static String onTrail(String id, ServerPlayer player, int step) {
		for (Job job : JOBS) {
			if (job.id.equals(id)) {
				job.trailPlayer = player.getUUID();
				job.trailStep = step;
			}
		}
		return id;
	}

	/** Whether water lies at the surface somewhere on the straight way between {@code from} and {@code to}. */
	static boolean acrossWater(ServerLevel level, BlockPos from, BlockPos to) {
		int dx = to.getX() - from.getX();
		int dz = to.getZ() - from.getZ();
		int steps = (int) (Math.sqrt(dx * dx + dz * dz) / 2.0);
		for (int step = 2; step < steps - 1; step++) {
			int x = from.getX() + dx * step / steps;
			int z = from.getZ() + dz * step / steps;
			if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
				return false;
			}
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			if (!level.getBlockState(new BlockPos(x, y - 1, z)).getFluidState().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** A trail's signpost: "the water remembers", the way to {@code pyramid} on the compass, and how far. */
	static List<String> signpost(BlockPos from, BlockPos pyramid) {
		int dx = pyramid.getX() - from.getX();
		int dz = pyramid.getZ() - from.getZ();
		String[] compass = {"EAST", "SOUTH-EAST", "SOUTH", "SOUTH-WEST", "WEST", "NORTH-WEST", "NORTH", "NORTH-EAST"};
		int index = Math.floorMod((int) Math.round(Math.atan2(dz, dx) / (Math.PI / 4.0)), 8);
		int distance = (int) Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
		return List.of("THE WATER", "REMEMBERS", compass[index], distance + " BLOCKS");
	}

	/**
	 * Leaves a mark at or near {@code where} (where he stood), facing {@code player}; of {@code only} if given, otherwise
	 * of a kind that suits the place: a tunnel underground, a sand pyramid on sand, a bare grove under trees, otherwise a
	 * shrine, a mossy pyramid or a tunnel into a hillside, and a lone sign if nothing else fits; at most
	 * {@value #MAX_PLAN_TRIES} sites are weighed. Returns the mark's id, or null if nowhere near was wild enough.
	 */
	public static @Nullable String leave(ServerLevel level, BlockPos where, ServerPlayer player, boolean company, RandomSource random,
			HerobrineMarkPlans.@Nullable Type only, @Nullable List<String> text) {
		if (JOBS.size() >= MAX_JOBS || !level.hasChunkAt(where)) {
			return null;
		}
		List<String> message = text != null ? text : company ? HerobrineMarkPlans.BROUGHT_SOMEONE
				: HerobrineMarkPlans.MESSAGES.get(random.nextInt(HerobrineMarkPlans.MESSAGES.size()));
		BlockPos toward = player.blockPosition();
		List<HerobrineMarkPlans.Type> kinds = new ArrayList<>();
		if (only != null) {
			kinds.add(only);
		} else {
			boolean underground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, where.getX(), where.getZ()) > where.getY() + 1;
			boolean underTrees = !underground && !level.canSeeSky(where.above());
			if (underground) {
				kinds.add(HerobrineMarkPlans.Type.UNDERGROUND);
				kinds.add(HerobrineMarkPlans.Type.TUNNEL);
			} else if (level.getBlockState(where.below()).is(BlockTags.SAND)) {
				kinds.add(HerobrineMarkPlans.Type.SAND_PYRAMID);
			} else if (underTrees) {
				kinds.add(HerobrineMarkPlans.Type.GROVE);
			} else {
				List<HerobrineMarkPlans.Type> open = new ArrayList<>(List.of(HerobrineMarkPlans.Type.SHRINE,
						HerobrineMarkPlans.Type.MOSSY_PYRAMID, HerobrineMarkPlans.Type.TUNNEL));
				Collections.shuffle(open, new java.util.Random(random.nextLong()));
				kinds.addAll(open);
			}
			kinds.add(HerobrineMarkPlans.Type.SIGN);
		}
		Direction away = HerobrineMarkPlans.toward(toward, where);
		int tries = 0;
		for (HerobrineMarkPlans.Type kind : kinds) {
			boolean wide = kind == HerobrineMarkPlans.Type.GROVE || kind == HerobrineMarkPlans.Type.TUNNEL
					|| kind == HerobrineMarkPlans.Type.PYRAMID || kind == HerobrineMarkPlans.Type.UNDERGROUND
					|| kind == HerobrineMarkPlans.Type.UNDERWATER || kind == HerobrineMarkPlans.Type.SKY_ISLAND
					|| kind == HerobrineMarkPlans.Type.SURFACE_RUIN;
			for (BlockPos feet : wide ? List.of(where) : near(level, where)) {
				if (++tries > MAX_PLAN_TRIES) {
					return null;
				}
				HerobrineMarkPlans.Plan plan = switch (kind) {
					case SHRINE -> HerobrineMarkPlans.shrine(level, feet, toward, message);
					case MOSSY_PYRAMID -> HerobrineMarkPlans.pyramid(level, feet, false, toward, message);
					case SAND_PYRAMID -> HerobrineMarkPlans.pyramid(level, feet, true, toward, message);
					case SIGN -> HerobrineMarkPlans.sign(level, feet, toward, message);
					case GROVE -> HerobrineMarkPlans.grove(level, feet, toward, message, random);
					case TUNNEL -> HerobrineMarkPlans.tunnel(level, feet, away, toward, message);
					case PYRAMID -> HerobrinePyramids.near(level, feet, toward, random);
					case UNDERGROUND -> HerobrineDepths.find(level, feet, false);
					case UNDERWATER -> HerobrineDepths.find(level, feet, true);
					case SKY_ISLAND -> HerobrineSkyIslands.find(level, feet);
					case SURFACE_RUIN -> HerobrineSurfacePlans.find(level, feet);
				};
				if (plan != null) {
					return start(level, plan);
				}
			}
		}
		return null;
	}

	/** Where he stood, then four places three blocks round it, each at a standable height within a block. */
	private static List<BlockPos> near(ServerLevel level, BlockPos where) {
		List<BlockPos> places = new ArrayList<>();
		places.add(where);
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos side = where.relative(direction, 3);
			for (int dy = 1; dy >= -1; dy--) {
				if (HerobrineEncounters.standable(level, side.above(dy))) {
					places.add(side.above(dy));
					break;
				}
			}
		}
		return places;
	}

	/** Starts building {@code plan}, recording it under a new id. Null if two marks are already building. */
	public static @Nullable String start(ServerLevel level, HerobrineMarkPlans.Plan plan) {
		if (JOBS.size() >= MAX_JOBS) {
			return null;
		}
		HerobrineMarks marks = get(level.getServer());
		String id = newId(marks);
		marks.marks.put(id, new Mark(plan.type().name().toLowerCase(Locale.ROOT), plan.at(), level.getGameTime(), List.of(), false));
		marks.setDirty();
		JOBS.add(new Job(id, level, plan));
		return id;
	}

	/**
	 * Builds {@code plan} at once rather than over ticks, block by block through the same checks as any mark, recorded
	 * and undoable the same way; it stops where a check stops it (an unloaded chunk or someone in the way stops it too,
	 * rather than waiting). For game tests; it does not take a build slot. Returns the mark's id.
	 */
	public static String buildNow(ServerLevel level, HerobrineMarkPlans.Plan plan) {
		HerobrineMarks marks = get(level.getServer());
		String id = newId(marks);
		Mark mark = new Mark(plan.type().name().toLowerCase(Locale.ROOT), plan.at(), level.getGameTime(), List.of(), false);
		marks.marks.put(id, mark);
		marks.setDirty();
		Job job = new Job(id, level, plan);
		while (!job.steps.isEmpty()) {
			Result result = apply(job, job.steps.peekFirst(), mark, marks);
			if (result == Result.WAIT || result == Result.STOP) {
				break;
			}
			job.steps.removeFirst();
		}
		done(marks, job, mark, job.steps.isEmpty());
		return id;
	}

	private static String newId(HerobrineMarks marks) {
		String id;
		do {
			id = UUID.randomUUID().toString().substring(0, 8);
		} while (marks.marks.containsKey(id));
		return id;
	}

	private static void work(MinecraftServer server) {
		if (JOBS.isEmpty()) {
			return;
		}
		HerobrineMarks marks = get(server);
		int share = Math.max(1, BLOCKS_PER_TICK / JOBS.size());
		for (Iterator<Job> jobs = JOBS.iterator(); jobs.hasNext(); ) {
			Job job = jobs.next();
			int budget = share;
			Mark mark = marks.marks.get(job.id);
			boolean over = mark == null;
			while (!over && budget > 0 && !job.steps.isEmpty()) {
				Result result = apply(job, job.steps.peekFirst(), mark, marks);
				if (result == Result.WAIT) {
					over = ++job.waits > MAX_WAITS;
					break;
				}
				if (result == Result.STOP) {
					over = true;
					break;
				}
				job.steps.removeFirst();
				budget -= result == Result.DONE ? 1 : 0;
			}
			if (over || job.steps.isEmpty()) {
				jobs.remove();
				done(marks, job, mark, !over);
			}
		}
	}

	/**
	 * A job is over: a mark that changed nothing is forgotten; one built to its last block ({@code whole}) is recorded
	 * finished and moves on the trail it carries. One stopped part-built stays recorded and undoable, unfinished.
	 */
	private static void done(HerobrineMarks marks, Job job, @Nullable Mark mark, boolean whole) {
		if (mark == null) {
			return;
		}
		if (mark.changes().isEmpty()) {
			marks.marks.remove(job.id);
		} else if (whole) {
			marks.marks.put(job.id, new Mark(mark.type(), mark.at(), mark.time(), mark.changes(), true));
			if (job.trailPlayer != null) {
				marks.trails.merge(job.trailPlayer.toString(), job.trailStep, Math::max);
			}
		}
		marks.setDirty();
	}

	private static Result apply(Job job, HerobrineMarkPlans.Step step, Mark mark, HerobrineMarks marks) {
		ServerLevel level = job.level;
		BlockPos pos = step.pos();
		if (!level.hasChunkAt(pos)) {
			return Result.WAIT;
		}
		if (LandRegistry.isClaimed(level, pos) || Town.isProtected(level, pos)
                || mark.type().equals("sky_island") && HerobrineSkyIslands.protectedTownColumn(level, pos)) {
			return Result.STOP;
		}
		// Taking a block away still needs the operator's approval, block by block: a revoked approval stops a grove or tunnel.
		if (step.state().isAir() && !approved(level, pos, pos)) {
			return Result.STOP;
		}
		BlockState now = level.getBlockState(pos);
		// Source-water formations never consume lava or a later flowing-water state.
		if (mark.type().equals("underwater") && (!now.is(Blocks.WATER) || !now.getFluidState().isSource())) return Result.STOP;
		// New excavation/lining plans may replace natural stone only inside approved wilderness, rechecked at apply time.
		if (mark.type().equals("underground") && !HerobrineMarkPlans.open(now) && !approved(level, pos, pos)) return Result.STOP;
		if (!now.is(step.expect()) || now.hasBlockEntity() || !HerobrineMarkPlans.natural(now)) {
			return Result.STOP;
		}
		if (step.state().isAir()) {
			// Never open a way for water or lava: a leaf by the water stays; a tunnel that meets it stops.
			for (Direction direction : Direction.values()) {
				BlockPos next = pos.relative(direction);
				if (!level.hasChunkAt(next) || !level.getBlockState(next).getFluidState().isEmpty()) {
					return now.is(BlockTags.LEAVES) ? Result.SKIP : Result.STOP;
				}
			}
		} else if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) {
			return Result.WAIT;
		}
		level.setBlock(pos, step.state(), Block.UPDATE_ALL);
		String content = "";
		List<String> text = job.signs.get(pos);
		if (text != null && level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
			List<Component> lines = text.stream().map(line -> (Component) Component.literal(line)).toList();
			for (SignTextSlot slot : SignTextSlot.values()) {
				sign.setText(new SignText(lines, lines, DyeColor.BLACK, false), slot);
			}
			sign.setWaxed(true);
			sign.setChanged();
			level.sendBlockUpdated(pos, sign.getBlockState(), sign.getBlockState(), Block.UPDATE_ALL);
			content = written(text);
		}
		if (job.lecterns.contains(pos) && LecternBlock.tryPlaceBook(null, level, pos, level.getBlockState(pos),
				HerobrinePyramids.journal())) {
			content = JOURNAL;
		}
		// What the mark left, contents and all: undo puts back only what is still exactly this.
		mark.changes().add(new Change(pos, now, level.getBlockState(pos), content));
		marks.setDirty();
		return Result.DONE;
	}

	/**
	 * Takes mark {@code id} back, newest block first: a block still exactly as the mark left it ({@link #unchanged}) is
	 * put back as it was; a block a player changed since, in any way, is left alone and forgotten; a block in an
	 * unloaded chunk, or with someone standing where it would go back, waits in the record for another undo. Stops the mark building, if it still is. Null if
	 * there is no such mark.
	 */
	public static @Nullable Undo undo(ServerLevel level, String id) {
		HerobrineMarks marks = get(level.getServer());
		Mark mark = marks.marks.get(id);
		if (mark == null) {
			return null;
		}
		JOBS.removeIf(job -> job.id.equals(id));
		int restored = 0;
		int kept = 0;
		List<Change> waiting = new ArrayList<>();
		List<Change> changes = mark.changes();
		for (int i = changes.size() - 1; i >= 0; i--) {
			Change change = changes.get(i);
			if (!level.hasChunkAt(change.pos())
					|| !change.before().isAir() && !level.getEntitiesOfClass(LivingEntity.class, new AABB(change.pos())).isEmpty()) {
				waiting.add(0, change);
			} else if (unchanged(level, change)) {
				if (change.content().equals(JOURNAL) && level.getBlockEntity(change.pos()) instanceof LecternBlockEntity lectern) {
					// Still the pyramid's own journal: it goes with the pyramid, emptied first so the lectern cannot drop it.
					lectern.clearContent();
				}
				level.setBlock(change.pos(), change.before(), Block.UPDATE_ALL);
				restored++;
			} else {
				kept++;
			}
		}
		if (waiting.isEmpty()) {
			marks.marks.remove(id);
		} else {
			// What is left waits for a later undo, and is no longer a finished mark.
			marks.marks.put(id, new Mark(mark.type(), mark.at(), mark.time(), waiting, false));
		}
		marks.setDirty();
		return new Undo(restored, kept, waiting.size());
	}

	/**
	 * Whether the block a mark changed is still exactly as the mark left it: the same block in the same state (every
	 * property), holding the same contents. Only these changes the world makes by itself are allowed: a redstone torch
	 * lit or unlit by power, a glass pane's sides joining its neighbours, and a lectern's pulse when a page is turned.
	 * Anything else (a turned, waterlogged or swapped block, an edited sign, a taken or swapped book) is a player's
	 * change, and undo leaves it be.
	 */
	static boolean unchanged(ServerLevel level, Change change) {
		BlockState now = level.getBlockState(change.pos());
		BlockState after = change.after();
		if (after.isAir()) {
			return now.isAir();
		}
		if (!now.is(after.getBlock())) {
			return false;
		}
		for (Property<?> property : after.getProperties()) {
			if (!settles(after, property) && !now.getValue(property).equals(after.getValue(property))) {
				return false;
			}
		}
		if (change.content().isEmpty()) {
			// A visitor may adopt an empty shelter furnace: never remove containers through generic undo.
			return !(level.getBlockEntity(change.pos()) instanceof net.minecraft.world.Container);
		}
		BlockEntity entity = level.getBlockEntity(change.pos());
		if (change.content().equals(JOURNAL)) {
			return entity instanceof LecternBlockEntity lectern
					&& ItemStack.isSameItemSameComponents(lectern.getBook(), HerobrinePyramids.journal());
		}
		return entity instanceof SignBlockEntity sign && asWritten(sign, change.content());
	}

	/** The properties the world changes on its own, one block at a time: never a player's doing. */
	private static boolean settles(BlockState state, Property<?> property) {
		return state.is(Blocks.REDSTONE_TORCH) && property == BlockStateProperties.LIT
				|| state.is(Blocks.GLASS_PANE) && (property == BlockStateProperties.NORTH || property == BlockStateProperties.EAST
						|| property == BlockStateProperties.SOUTH || property == BlockStateProperties.WEST)
				|| state.is(Blocks.LECTERN) && property == BlockStateProperties.POWERED;
	}

	/** A sign's recorded content: the four lines a mark wrote on it, each ended by a newline. */
	private static String written(List<String> text) {
		StringBuilder content = new StringBuilder();
		for (String line : text) {
			content.append(line).append('\n');
		}
		return content.toString();
	}

	/**
	 * Whether a sign is still exactly as a mark wrote it ({@link #written}): on both sides, filtered and not, the same
	 * plain unstyled lines, in black, without glow, and still waxed. Any other difference (a line, its style, the back,
	 * the dye, glow ink, the wax) is someone's edit, and undo keeps the sign.
	 */
	private static boolean asWritten(SignBlockEntity sign, String content) {
		String[] lines = content.split("\n", -1);
		if (!sign.isWaxed() || lines.length < 4) {
			return false;
		}
		for (SignTextSlot slot : SignTextSlot.values()) {
			SignText text = sign.getText(slot);
			if (text.getColor() != DyeColor.BLACK || text.hasGlowingText()) {
				return false;
			}
			for (boolean filtered : new boolean[] {false, true}) {
				List<Component> messages = text.getMessages(filtered);
				if (messages.size() < 4) {
					return false;
				}
				for (int line = 0; line < 4; line++) {
					if (!messages.get(line).equals(Component.literal(lines[line]))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static void commands(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft").then(Commands.literal("herobrine")
				.requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
				.then(Commands.literal("marks").executes(context -> list(context.getSource()))
						.then(Commands.literal("undo").then(Commands.argument("id", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										get(context.getSource().getServer()).marks.keySet(), builder))
								.executes(context -> undo(context.getSource(), StringArgumentType.getString(context, "id"))))))
				.then(Commands.literal("wilds").executes(context -> listWilds(context.getSource()))
						.then(Commands.literal("approve").then(Commands.argument("radius", IntegerArgumentType.integer(MIN_WILD_RADIUS, MAX_WILD_RADIUS))
								.executes(context -> approveHere(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))))
						.then(Commands.literal("revoke").then(Commands.argument("number", IntegerArgumentType.integer(1))
								.executes(context -> revoke(context.getSource(), IntegerArgumentType.getInteger(context, "number"))))))
				.then(Commands.literal("mark").then(Commands.argument("player", EntityArgument.player())
						.then(Commands.argument("kind", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(markNames(), builder))
								.executes(context -> force(context.getSource(), EntityArgument.getPlayer(context, "player"),
										StringArgumentType.getString(context, "kind"))))))));
	}

	private static int listWilds(CommandSourceStack source) {
		List<Wild> all = get(source.getServer()).wilds;
		if (all.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("commands.jugcraft.herobrine.wilds.none"), false);
		}
		for (int i = 0; i < all.size(); i++) {
			Wild wild = all.get(i);
			int number = i + 1;
			source.sendSuccess(() -> Component.translatable("commands.jugcraft.herobrine.wilds.entry", number, wild.radius(),
					wild.centre().getX(), wild.centre().getY(), wild.centre().getZ(), wild.by()), false);
		}
		return all.size();
	}

	private static int approveHere(CommandSourceStack source, int radius) {
		// Approvals are Overworld coordinates: he haunts and marks nowhere else.
		if (source.getLevel().dimension() != Level.OVERWORLD) {
			source.sendFailure(Component.translatable("commands.jugcraft.herobrine.wilds.overworld"));
			return 0;
		}
		BlockPos centre = BlockPos.containing(source.getPosition());
		Wild wild = approve(source.getServer().overworld(), centre, radius, source.getTextName());
		source.sendSuccess(() -> Component.translatable("commands.jugcraft.herobrine.wilds.approved", wild.radius(),
				centre.getX(), centre.getY(), centre.getZ()), true);
		return 1;
	}

	private static int revoke(CommandSourceStack source, int number) {
		List<Wild> all = get(source.getServer()).wilds;
		if (number > all.size()) {
			source.sendFailure(Component.translatable("commands.jugcraft.herobrine.wilds.unknown", number));
			return 0;
		}
		revoke(source.getServer().overworld(), all.get(number - 1));
		source.sendSuccess(() -> Component.translatable("commands.jugcraft.herobrine.wilds.revoked", number), true);
		return 1;
	}

	private static int list(CommandSourceStack source) {
		Map<String, Mark> all = get(source.getServer()).marks;
		if (all.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("commands.jugcraft.herobrine.marks.none"), false);
		}
		all.entrySet().stream().sorted(Map.Entry.comparingByValue((a, b) -> Long.compare(a.time(), b.time()))).forEach(entry -> {
			Mark mark = entry.getValue();
			source.sendSuccess(() -> Component.translatable("commands.jugcraft.herobrine.marks.entry", entry.getKey(), mark.type(),
					mark.at().getX(), mark.at().getY(), mark.at().getZ(), mark.changes().size()), false);
		});
		return all.size();
	}

	private static int undo(CommandSourceStack source, String id) {
		Undo undo = undo(source.getServer().overworld(), id);
		if (undo == null) {
			source.sendFailure(Component.translatable("commands.jugcraft.herobrine.marks.unknown", id));
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("commands.jugcraft.herobrine.marks.undone", id, undo.restored(), undo.kept(),
				undo.waiting()), true);
		return undo.restored();
	}

	/** An operator's mark: a fixed kind, about six blocks in front of {@code player}. */
	private static int force(CommandSourceStack source, ServerPlayer player, String kind) {
		if (player.level().dimension() != Level.OVERWORLD) {
			source.sendFailure(Component.literal("Herobrine landmarks are Overworld-only."));
			return 0;
		}
		if (HerobrineDepths.CAVES.contains(kind) || HerobrineDepths.WATER.contains(kind)
				|| HerobrineSkyIslands.VARIANTS.contains(kind) || HerobrineSurfacePlans.VARIANTS.contains(kind)) {
			var level = source.getServer().overworld();
			BlockPos site = player.blockPosition().relative(player.getDirection(), 6);
			HerobrineMarkPlans.Plan plan;
			if (HerobrineSkyIslands.VARIANTS.contains(kind)) {
				Integer top = HerobrineSkyIslands.lowestTop(level, site.getX(), site.getZ(), kind);
				plan = top == null ? null : HerobrineSkyIslands.plan(level, new BlockPos(site.getX(), top + 8, site.getZ()), kind);
			} else if (HerobrineSurfacePlans.VARIANTS.contains(kind)) {
				plan = HerobrineSurfacePlans.plan(level, site, kind);
			} else {
				plan = HerobrineDepths.plan(level, site, kind);
			}
			String id = plan == null ? null : start(source.getServer().overworld(), plan);
			if (id == null) { source.sendFailure(Component.translatable("commands.jugcraft.herobrine.marks.no_site", kind)); return 0; }
			source.sendSuccess(() -> Component.translatable("commands.jugcraft.herobrine.marks.left", kind, id), true);
			return 1;
		}
		HerobrineMarkPlans.Type type;
		try {
			type = HerobrineMarkPlans.Type.valueOf(kind.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			source.sendFailure(Component.translatable("commands.jugcraft.herobrine.marks.unknown_kind", kind));
			return 0;
		}
		BlockPos ahead = player.blockPosition().relative(player.getDirection(), 6);
		String id = leave(source.getServer().overworld(), ahead, player, false, player.getRandom(), type, null);
		if (id == null) {
			source.sendFailure(Component.translatable("commands.jugcraft.herobrine.marks.no_site", kind));
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("commands.jugcraft.herobrine.marks.left", kind, id), true);
		return 1;
	}

	private static List<String> markNames() {
		List<String> names = new ArrayList<>(java.util.Arrays.stream(HerobrineMarkPlans.Type.values())
				.map(type -> type.name().toLowerCase(Locale.ROOT)).toList());
		names.addAll(HerobrineDepths.CAVES);
		names.addAll(HerobrineDepths.WATER);
		names.addAll(HerobrineSkyIslands.VARIANTS);
		names.addAll(HerobrineSurfacePlans.VARIANTS);
		return names;
	}
}

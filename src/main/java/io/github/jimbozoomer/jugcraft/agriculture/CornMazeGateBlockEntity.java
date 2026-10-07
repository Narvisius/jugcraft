package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A corn maze, kept by its gate. Before it is planted the gate holds the size chosen; planting carves a maze from a new
 * seed ({@link CornMaze}), puts the finish post at its exit and plants maze corn along its walls, {@value #PLANT_PER_TICK}
 * stalks a tick, only where the ground is solid and the three blocks above are clear (it never replaces a block).
 *
 * <p>A run starts when a player steps out through the gate and ends at the finish post. The server follows each runner
 * every tick: a run is void if they fly (or glide), climb above the corn, leave the maze, take longer than
 * {@value #MAX_RUN} ticks, or reach the finish having walked less than {@value #SHORTCUT} of the shortest way through
 * (a shortcut, say through a broken wall). A finished run's time goes on the gate's board (the best of each runner, the
 * top {@value #BOARD}), with a ribbon the first time a runner places, and Amazing for finishing at all. At most
 * {@value #MAX_RUNNERS} run at once.
 */
public class CornMazeGateBlockEntity extends BlockEntity {
	public static final int PLANT_PER_TICK = 32;
	public static final int MAX_RUN = 12000;
	public static final double SHORTCUT = 0.8;
	public static final int BOARD = HarvestScaleBlockEntity.BOARD;
	public static final int MAX_RUNNERS = 16;
	public static final int REMEMBERED = 64;

	/** One place on the board: a runner's best time in ticks. */
	public record Entry(UUID runner, String name, int ticks) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("runner").forGetter(Entry::runner),
				Codec.STRING.fieldOf("name").forGetter(Entry::name),
				Codec.INT.fieldOf("ticks").forGetter(Entry::ticks)).apply(i, Entry::new));
	}

	/** A run in progress (kept in memory only): when it began, where the runner last was, how far they have walked. */
	private static final class Run {
		long start;
		Vec3 last;
		double walked;

		Run(long start, Vec3 at) {
			this.start = start;
			this.last = at;
		}
	}

	private int size = 1;
	private long seed;
	private boolean planted;
	private int cells;
	private int shortest;
	private final List<BlockPos> pending = new ArrayList<>();
	private final List<Entry> board = new ArrayList<>();
	private final Deque<UUID> awarded = new ArrayDeque<>();
	private final Map<UUID, Run> runs = new HashMap<>();
	private @Nullable CornMaze cached;
	private long cachedSeed;

	public CornMazeGateBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.CORN_MAZE_GATE_ENTITY, pos, state);
	}

	public int size() {
		return size;
	}

	public boolean planted() {
		return planted;
	}

	public int shortest() {
		return shortest;
	}

	public List<Entry> board() {
		return List.copyOf(board);
	}

	public boolean running(UUID runner) {
		return runs.containsKey(runner);
	}

	/** Steps the size on to the next (tiny, small, medium, large); only before planting. */
	public boolean nextSize() {
		if (planted) {
			return false;
		}
		size = (size + 1) % CornMaze.CELLS.length;
		setChanged();
		return true;
	}

	private Direction facing() {
		return getBlockState().getValue(CornMazeGateBlock.FACING);
	}

	/** The maze this gate keeps (or would plant). */
	public CornMaze maze() {
		int wanted = planted ? cells : CornMaze.CELLS[size];
		if (cached == null || cached.cells() != wanted || cachedSeed != seed) {
			cached = CornMaze.carve(wanted, seed);
			cachedSeed = seed;
		}
		return cached;
	}

	/** Whether maze corn can be planted at {@code pos}: solid ground under it and three clear blocks. */
	public static boolean plantable(Level level, BlockPos pos) {
		if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
			return false;
		}
		for (int section = 0; section < 3; section++) {
			if (!level.getBlockState(pos.above(section)).canBeReplaced()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Plans the maze from a new {@code seed} and returns where its walls can be planted (it plants nothing yet), or null
	 * if the finish post's square isn't clear.
	 */
	public @Nullable List<BlockPos> survey(ServerLevel level, long newSeed) {
		CornMaze maze = CornMaze.carve(CornMaze.CELLS[size], newSeed);
		BlockPos finish = maze.finish(worldPosition, facing());
		if (!plantable(level, finish)) {
			return null;
		}
		List<BlockPos> columns = new ArrayList<>();
		for (int[] square : maze.walls()) {
			BlockPos pos = maze.at(worldPosition, facing(), square[0], square[1]);
			if (plantable(level, pos)) {
				columns.add(pos);
			}
		}
		return columns;
	}

	/** Plants the maze surveyed from {@code newSeed}: places the finish post, and plants the walls over the next ticks. */
	public void plant(ServerLevel level, long newSeed, List<BlockPos> columns) {
		seed = newSeed;
		cells = CornMaze.CELLS[size];
		CornMaze maze = maze();
		shortest = maze.shortest();
		planted = true;
		BlockPos finish = maze.finish(worldPosition, facing());
		level.setBlock(finish, JugcraftAgriculture.block("corn_maze_finish").defaultBlockState()
				.setValue(CornMazeGateBlock.FACING, facing().getOpposite()), Block.UPDATE_ALL);
		pending.clear();
		pending.addAll(columns);
		board.clear();
		setChanged();
	}

	public BlockPos finishPos() {
		return maze().finish(worldPosition, facing());
	}

	/** The maze's ground area, a block wider all round, from the ground up past the corn. */
	public AABB bounds() {
		CornMaze maze = maze();
		BlockPos a = maze.at(worldPosition, facing(), 0, 0);
		BlockPos b = maze.at(worldPosition, facing(), maze.side() - 1, maze.side() - 1);
		return AABB.encapsulatingFullBlocks(a, b).inflate(1.5, 0.0, 1.5).expandTowards(0.0, 4.0, 0.0);
	}

	public void serverTick(ServerLevel level) {
		if (!pending.isEmpty()) {
			plantSome(level);
		}
		if (!planted) {
			return;
		}
		long now = level.getGameTime();
		for (Player player : level.getEntitiesOfClass(Player.class, new AABB(worldPosition).expandTowards(0.0, 1.0, 0.0))) {
			if (player instanceof ServerPlayer runner && !runner.isSpectator()) {
				start(runner, now);
			}
		}
		if (!runs.isEmpty()) {
			track(level, now);
			for (Player player : level.getEntitiesOfClass(Player.class, new AABB(finishPos()).expandTowards(0.0, 1.0, 0.0))) {
				if (player instanceof ServerPlayer runner) {
					finish(runner, now);
				}
			}
		}
	}

	private void plantSome(ServerLevel level) {
		int planted = 0;
		while (!pending.isEmpty() && planted < PLANT_PER_TICK) {
			BlockPos pos = pending.removeLast();
			if (plantable(level, pos)) {
				BlockState corn = JugcraftAgriculture.block("maze_corn").defaultBlockState();
				for (int section = 0; section < 3; section++) {
					level.setBlock(pos.above(section), corn.setValue(MazeCornBlock.SECTION, section), Block.UPDATE_ALL);
				}
			}
			planted++;
		}
		if (pending.isEmpty()) {
			level.playSound(null, worldPosition, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 0.9F);
		}
		setChanged();
	}

	/** Starts (or, while they stand in the gate, restarts) {@code runner}'s run. */
	public void start(ServerPlayer runner, long now) {
		Run run = runs.get(runner.getUUID());
		if (run == null) {
			if (runs.size() >= MAX_RUNNERS) {
				return;
			}
			runner.sendOverlayMessage(Component.translatable("message.jugcraft.corn_maze.go"));
			runs.put(runner.getUUID(), new Run(now, runner.position()));
		} else if (now - run.start < MAX_RUN) {
			run.start = now;
			run.last = runner.position();
			run.walked = 0.0;
		}
	}

	/** Follows every runner for one tick, voiding the runs of any who fly, climb out, leave or take too long. */
	public void track(ServerLevel level, long now) {
		AABB bounds = bounds();
		// Feet two blocks up is on top of the corn, not a jump.
		double top = worldPosition.getY() + 2.0;
		runs.entrySet().removeIf(entry -> {
			Player player = level.getPlayerByUUID(entry.getKey());
			if (!(player instanceof ServerPlayer runner) || runner.isRemoved() || runner.level() != level) {
				return true;
			}
			Run run = entry.getValue();
			String voided = runner.getAbilities().flying || runner.isFallFlying() || runner.isSpectator() ? "flew"
					: runner.getY() > top ? "climbed" : !bounds.contains(runner.position()) ? "left" : now - run.start > MAX_RUN ? "slow" : null;
			if (voided != null) {
				runner.sendOverlayMessage(Component.translatable("message.jugcraft.corn_maze.void." + voided));
				return true;
			}
			Vec3 at = runner.position();
			run.walked += Math.sqrt((at.x - run.last.x) * (at.x - run.last.x) + (at.z - run.last.z) * (at.z - run.last.z));
			run.last = at;
			return false;
		});
	}

	/**
	 * {@code runner} has reached the finish post: if their run is good, records it (telling them their time, keeping their
	 * best, a ribbon the first time they place, Amazing) and returns their place (0 for first), else -1.
	 */
	public int finish(ServerPlayer runner, long now) {
		Run run = runs.remove(runner.getUUID());
		if (run == null) {
			return -1;
		}
		if (run.walked < shortest * SHORTCUT) {
			runner.sendSystemMessage(Component.translatable("message.jugcraft.corn_maze.shortcut"));
			return -1;
		}
		int ticks = (int) (now - run.start);
		runner.sendSystemMessage(Component.translatable("message.jugcraft.corn_maze.finished", RegattaFlagBlockEntity.time(ticks)));
		TrickOrTreat.award(runner, "amazing");
		UUID id = runner.getUUID();
		Entry best = board.stream().filter(entry -> entry.runner().equals(id)).findFirst().orElse(null);
		if (best == null || ticks < best.ticks()) {
			board.removeIf(entry -> entry.runner().equals(id));
			board.add(new Entry(id, runner.getName().getString(), ticks));
			board.sort(Comparator.comparingInt(Entry::ticks));
			while (board.size() > BOARD) {
				board.removeLast();
			}
		}
		int place = -1;
		for (int i = 0; i < board.size(); i++) {
			if (board.get(i).runner().equals(id)) {
				place = i;
			}
		}
		if (place >= 0 && !awarded.contains(id)) {
			awarded.addLast(id);
			while (awarded.size() > REMEMBERED) {
				awarded.removeFirst();
			}
			ItemStack ribbon = new ItemStack(JugcraftAgriculture.item(HarvestScaleBlockEntity.RIBBONS.get(place)));
			runner.sendSystemMessage(Component.translatable("message.jugcraft.harvest_scale.ribbon", ribbon.getHoverName()));
			if (!runner.getInventory().add(ribbon)) {
				Block.popResource(runner.level(), runner.blockPosition(), ribbon);
			}
		}
		runner.level().playSound(null, runner.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.2F);
		showBoard(runner);
		setChanged();
		return place;
	}

	/** Tells a player the maze's size and the board. */
	public void showBoard(ServerPlayer player) {
		CornMaze maze = maze();
		player.sendSystemMessage(Component.translatable("message.jugcraft.corn_maze." + (planted ? "planted" : "plan"),
				Component.translatable("message.jugcraft.corn_maze.size." + CornMaze.SIZES[planted ? java.util.Arrays.binarySearch(CornMaze.CELLS, cells) : size]),
				maze.side(), maze.side()));
		for (int i = 0; i < board.size(); i++) {
			Entry entry = board.get(i);
			player.sendSystemMessage(Component.translatable("message.jugcraft.regatta.board", i + 1, entry.name(), RegattaFlagBlockEntity.time(entry.ticks())));
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		size = Math.floorMod(input.getIntOr("size", 1), CornMaze.CELLS.length);
		seed = input.getLongOr("seed", 0L);
		planted = input.getBooleanOr("planted", false);
		cells = input.getIntOr("cells", CornMaze.CELLS[size]);
		shortest = input.getIntOr("shortest", 0);
		pending.clear();
		input.read("pending", BlockPos.CODEC.listOf()).ifPresent(pending::addAll);
		board.clear();
		input.read("board", Entry.CODEC.listOf()).ifPresent(entries -> entries.stream().limit(BOARD).forEach(board::add));
		awarded.clear();
		input.read("awarded", UUIDUtil.CODEC.listOf()).ifPresent(ids -> ids.stream().limit(REMEMBERED).forEach(awarded::addLast));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("size", size);
		output.putLong("seed", seed);
		output.putBoolean("planted", planted);
		output.putInt("cells", cells);
		output.putInt("shortest", shortest);
		output.store("pending", BlockPos.CODEC.listOf(), List.copyOf(pending));
		output.store("board", Entry.CODEC.listOf(), List.copyOf(board));
		output.store("awarded", UUIDUtil.CODEC.listOf(), List.copyOf(awarded));
	}
}

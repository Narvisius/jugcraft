package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.creatures.scary.ScaryMod;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The server load benchmark (docs/features/server-load-benchmark.md): what a representative load of Jugcraft's own
 * server work costs a tick, timed with {@code System.nanoTime} and written to the log as "Server load: ...", which the
 * record copies from CI:
 * <ul>
 * <li>a factory of {@value #MACHINES_PER_KIND} each of five working processing machines (electric furnaces, crushers,
 * pulverizers, sawmills and sieves), charged and fed, ticked through their own block entity tickers;</li>
 * <li>a crowd of {@value #MOBS_PER_KIND} each of the four costumed villains, free to think and move.</li>
 * </ul>
 * Each load is warmed up for {@value #WARM_UP} rounds and then timed over {@value #ROUNDS} rounds; a round is one tick of
 * the whole load, on top of the ticks the server gives it anyway. The times are a CI runner's, shared with every other
 * test running at once: an indication, not a profile. The bounds asserted are a whole server tick (50 ms) for each load's
 * round, there to catch a change that makes the work grow, not to time the runner.
 */
public class ServerLoadGameTests {
	public static final int MACHINES_PER_KIND = 16;
	public static final int MOBS_PER_KIND = 6;
	public static final int WARM_UP = 20;
	public static final int ROUNDS = 100;
	/** A whole server tick, in microseconds: the budget for one round of each load. */
	private static final long TICK_MICROS = 50_000;

	private record Fed(MachineKind kind, Item input) {
	}

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	@GameTest(structure = "jugcraft-test:tatterlace_arena", maxTicks = 100)
	public void measuredServerLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 48; x++) {
			for (int z = 0; z < 48; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}

		// The factory: five rows of machines, each charged full and fed a stack of something it works.
		List<Fed> kinds = List.of(new Fed(MachineKind.ELECTRIC_FURNACE, Items.RAW_IRON), new Fed(MachineKind.CRUSHER, item("tin_ore")),
				new Fed(MachineKind.PULVERIZER, item("tin_ore")), new Fed(MachineKind.SAWMILL, Items.OAK_LOG),
				new Fed(MachineKind.SIEVE, Items.GRAVEL));
		List<BlockPos> machines = new ArrayList<>();
		for (int row = 0; row < kinds.size(); row++) {
			Fed fed = kinds.get(row);
			for (int i = 0; i < MACHINES_PER_KIND; i++) {
				BlockPos pos = new BlockPos(2 + i * 2, 1, 2 + row * 3);
				helper.setBlock(pos, JugcraftMachines.MACHINES.get(fed.kind()).defaultBlockState());
				EnergyStorage storage = EnergyStorage.SIDED.find(level, helper.absolutePos(pos), Direction.UP);
				if (storage instanceof SimpleEnergyStorage simple) {
					simple.setAmount(storage.getCapacity());
				}
				helper.getBlockEntity(pos, MachineBlockEntity.class).setItem(0, new ItemStack(fed.input(), 64));
				machines.add(helper.absolutePos(pos));
			}
		}
		List<Runnable> factory = new ArrayList<>();
		for (BlockPos pos : machines) {
			factory.add(() -> tick(level, pos));
		}

		// The crowd: the four costumed villains, in a pen at the far end, thinking and moving.
		List<EntityType<? extends Mob>> villains = List.of(ScaryMod.SPACE_KOOK, ScaryMod.CAPTAIN_CUTLER, ScaryMod.PHANTOM_SHADOW,
				ScaryMod.BLACK_KNIGHT);
		List<Runnable> crowd = new ArrayList<>();
		for (int kind = 0; kind < villains.size(); kind++) {
			for (int i = 0; i < MOBS_PER_KIND; i++) {
				Mob mob = helper.spawn(villains.get(kind), new BlockPos(4 + i * 6, 1, 26 + kind * 5));
				mob.setPersistenceRequired();
				crowd.add(mob::tick);
			}
		}

		long factoryMicros = time(factory);
		long crowdMicros = time(crowd);
		int busy = 0;
		for (BlockPos pos : machines) {
			if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine
					&& (machine.processingProgress() > 0 || !machine.getItem(machine.kind().outputSlot()).isEmpty())) {
				busy++;
			}
		}
		Jugcraft.LOGGER.info(String.format(Locale.ROOT, "Server load: factory of %d machines %d us a tick (%d us a machine, %d of them "
				+ "working or done), crowd of %d costumed villains %d us a tick (%d us a mob), over %d timed rounds after %d warm-up rounds",
				machines.size(), factoryMicros, factoryMicros / machines.size(), busy, crowd.size(), crowdMicros,
				crowdMicros / crowd.size(), ROUNDS, WARM_UP));
		helper.assertTrue(busy > 0, "Some of the factory's machines should be working or done");
		helper.assertTrue(factoryMicros < TICK_MICROS, "The factory's tick stays within a whole server tick: " + factoryMicros + " us");
		helper.assertTrue(crowdMicros < TICK_MICROS, "The crowd's tick stays within a whole server tick: " + crowdMicros + " us");
		helper.succeed();
	}

	/** One round's average time over {@link #ROUNDS} rounds, after {@link #WARM_UP} untimed ones, in microseconds. */
	private static long time(List<Runnable> load) {
		for (int round = 0; round < WARM_UP; round++) {
			load.forEach(Runnable::run);
		}
		long start = System.nanoTime();
		for (int round = 0; round < ROUNDS; round++) {
			load.forEach(Runnable::run);
		}
		return (System.nanoTime() - start) / 1000L / ROUNDS;
	}

	/** Ticks the block entity at {@code pos} once through its own ticker, as the level does. */
	@SuppressWarnings("unchecked")
	private static <T extends BlockEntity> void tick(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		BlockEntity entity = level.getBlockEntity(pos);
		if (entity == null) {
			return;
		}
		BlockEntityTicker<T> ticker = state.getTicker(level, (BlockEntityType<T>) entity.getType());
		if (ticker != null) {
			ticker.tick(level, pos, state, (T) entity);
		}
	}
}

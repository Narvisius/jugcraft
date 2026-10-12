package io.github.jimbozoomer.jugcraft.raiders;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.town.TownBuilder;
import io.github.jimbozoomer.jugcraft.town.TownState;
import java.util.ArrayDeque;
import java.util.Deque;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Raider camps (raider extras): rare camps out in the plains, savanna and badlands, to find and clear. One newly
 * generated overworld chunk in {@value #RARITY} (chosen from the world seed, so the same seed gives the same camps) is
 * marked; the camp is built at its centre on the next server tick, if the ground there is flat and dry, the town is
 * not there and it is at least {@value #SPAWN_CLEARANCE} blocks from the world spawn. Inside a ring of sandbags (with
 * four gaps) a campfire burns between two olive tents, with a supply barrel (loot table
 * {@code jugcraft:chests/raider_camp}), held by an officer, a grunt, a gunner (a grunt while the guns are switched off) and a grenadier who stay put until someone comes. Its raiders belong to no raid, never despawn and do not come back once killed. Chunks generated
 * before this existed (or with the raiders feature off) never get one.
 */
public final class RaiderCamps {
	public static final int RADIUS = 6;
	public static final int MAX_SLOPE = 2;
	public static final int RARITY = 400;
	/** No camp within this many blocks of the world spawn, so a new player does not start beside one. */
	public static final int SPAWN_CLEARANCE = 512;
	public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("chests/raider_camp"));
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
	private static final Deque<ChunkPos> QUEUE = new ArrayDeque<>();

	private RaiderCamps() {
	}

	public static void register() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
			if (generated && level == level.getServer().overworld() && JugcraftConfig.isFeatureEnabled(JugcraftRaiders.FEATURE)
					&& chosen(level.getSeed(), chunk.getPos())) {
				QUEUE.add(chunk.getPos());
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(RaiderCamps::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> QUEUE.clear());
	}

	/** Whether a chunk of this world is one of the few to hold a camp. */
	public static boolean chosen(long seed, ChunkPos pos) {
		return RandomSource.create(seed ^ (pos.pack() * 0x9E3779B97F4A7C15L) ^ 0x5241494445524CL).nextInt(RARITY) == 0;
	}

	private static void tick(MinecraftServer server) {
		ChunkPos pos = QUEUE.poll();
		if (pos == null) {
			return;
		}
		ServerLevel level = server.overworld();
		if (level.getChunkSource().getChunkNow(pos.x(), pos.z()) == null) {
			return;
		}
		int x = (pos.x() << 4) + 8;
		int z = (pos.z() << 4) + 8;
		BlockPos origin = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
		BlockPos town = TownState.get(level).origin();
		BlockPos spawn = level.getRespawnData().pos();
		double fromSpawn = Math.hypot(x - spawn.getX(), z - spawn.getZ());
		if (fromSpawn >= SPAWN_CLEARANCE && camping(level.getBiome(origin)) && (town == null || !TownBuilder.overlaps(town, pos))) {
			build(level, origin, level.getRandom());
		}
	}

	private static boolean camping(Holder<Biome> biome) {
		return biome.is(ConventionalBiomeTags.IS_PLAINS) || biome.is(ConventionalBiomeTags.IS_SAVANNA) || biome.is(ConventionalBiomeTags.IS_BADLANDS);
	}

	/** Builds a camp on the ground at {@code origin} (the surface there); false (and nothing built) if it is not flat and dry. */
	public static boolean build(ServerLevel level, BlockPos origin, RandomSource random) {
		int y = origin.getY();
		for (int dx = -RADIUS; dx <= RADIUS; dx += RADIUS) {
			for (int dz = -RADIUS; dz <= RADIUS; dz += RADIUS) {
				int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, origin.getX() + dx, origin.getZ() + dz);
				if (Math.abs(ground - y) > MAX_SLOPE || !level.getFluidState(new BlockPos(origin.getX() + dx, ground - 1, origin.getZ() + dz)).isEmpty()) {
					return false;
				}
			}
		}
		// Level the ground: coarse dirt and gravel underfoot, clear air above.
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				if (dx * dx + dz * dz > (RADIUS + 0.5) * (RADIUS + 0.5)) {
					continue;
				}
				set(level, origin.offset(dx, -1, dz), (dx + dz) % 3 == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
				for (int dy = 0; dy < 5; dy++) {
					set(level, origin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
				}
			}
		}
		// The sandbag ring, with a gap to each side.
		Block sandbags = BuiltInRegistries.BLOCK.getValue(Jugcraft.id("sandbags"));
		BlockState bags = sandbags == Blocks.AIR ? Blocks.MUD_BRICKS.defaultBlockState() : sandbags.defaultBlockState();
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > RADIUS - 0.5 && d <= RADIUS + 0.5 && Math.abs(dx) > 1 && Math.abs(dz) > 1) {
					set(level, origin.offset(dx, 0, dz), bags);
				}
			}
		}
		set(level, origin, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		tent(level, origin.offset(-3, 0, -3));
		tent(level, origin.offset(2, 0, -3));
		BlockPos barrel = origin.offset(3, 0, 2);
		set(level, barrel, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
		RandomizableContainer.setBlockEntityLootTable(level, random, barrel, LOOT);
		set(level, origin.offset(-3, 0, 3), Blocks.CAULDRON.defaultBlockState());
		// The garrison.
		garrison(level, JugcraftRaiders.OFFICER, origin.offset(0, 0, 2), random);
		garrison(level, JugcraftRaiders.GRUNT, origin.offset(-2, 0, 0), random);
		garrison(level, JugcraftRaiders.armed(JugcraftRaiders.GUNNER), origin.offset(2, 0, 0), random);
		garrison(level, JugcraftRaiders.GRENADIER, origin.offset(0, 0, -1), random);
		return true;
	}

	/** An olive A-frame tent, three blocks deep, open to the south. */
	private static void tent(ServerLevel level, BlockPos corner) {
		BlockState cloth = vanilla("green_wool");
		for (int dz = 0; dz < 3; dz++) {
			set(level, corner.offset(0, 0, dz), cloth);
			set(level, corner.offset(2, 0, dz), cloth);
			set(level, corner.offset(1, 1, dz), cloth);
		}
		set(level, corner.offset(1, 0, 0), cloth);
		set(level, corner.offset(1, 0, 1), vanilla("brown_carpet"));
	}

	private static BlockState vanilla(String name) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(name)).defaultBlockState();
	}

	private static void garrison(ServerLevel level, EntityType<? extends Mob> type, BlockPos at, RandomSource random) {
		Mob raider = type.create(level, EntitySpawnReason.STRUCTURE);
		if (raider == null) {
			return;
		}
		raider.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		raider.setPersistenceRequired();
		level.addFreshEntity(raider);
	}

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, FLAGS);
	}
}

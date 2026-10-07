package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Where wild turkeys ({@link Turkey}) come from: woods and meadows by day (biome tag {@code jugcraft:turkey_habitat}).
 * Every {@value #SPAWN_TICKS} ticks, for each player in the overworld, {@link #SPAWN_CHANCE} of the time the server looks at
 * one spot {@value #MIN_DISTANCE} to {@value #MAX_DISTANCE} blocks away, in a loaded chunk; if it is grass under the open
 * sky in turkey country, a flock of {@value #FLOCK_MIN} to {@value #FLOCK_MAX} (a tom among them) wanders in there. Fewer
 * than {@value #NEAR_CAP} turkeys may be near a player, and at most {@value #LEVEL_CAP} in the world. Each try reads a
 * few blocks and never loads a chunk. Turkeys come only while mobs spawn (the {@code spawn_mobs} game rule). Like any
 * farm animal, they stay once they have come.
 */
public final class Turkeys {
	public static final int SPAWN_TICKS = 400;
	public static final float SPAWN_CHANCE = 0.25F;
	public static final int MIN_DISTANCE = 24;
	public static final int MAX_DISTANCE = 48;
	public static final int FLOCK_MIN = 3;
	public static final int FLOCK_MAX = 5;
	public static final int NEAR_CAP = 10;
	public static final int NEAR_RANGE = 64;
	public static final int LEVEL_CAP = 40;
	public static final TagKey<Biome> HABITAT = TagKey.create(Registries.BIOME, Jugcraft.id("turkey_habitat"));

	private Turkeys() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % SPAWN_TICKS != 0) {
				return;
			}
			ServerLevel level = server.overworld();
			if (!Crows.day(level) || !level.getGameRules().get(GameRules.SPAWN_MOBS) || !JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE)) {
				return;
			}
			int total = level.getEntities(JugcraftAgriculture.TURKEY, turkey -> true).size();
			for (ServerPlayer player : level.players()) {
				if (total >= LEVEL_CAP) {
					return;
				}
				if (!player.isSpectator() && level.getRandom().nextFloat() < SPAWN_CHANCE) {
					total += trySpawn(level, player.blockPosition(), level.getRandom());
				}
			}
		});
	}

	/** Tries to bring a flock into turkey country around {@code near}; returns how many turkeys came. */
	public static int trySpawn(ServerLevel level, BlockPos near, RandomSource random) {
		if (level.getEntitiesOfClass(Turkey.class, new AABB(near).inflate(NEAR_RANGE)).size() >= NEAR_CAP) {
			return 0;
		}
		double angle = random.nextDouble() * Math.PI * 2.0;
		int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
		BlockPos spot = ground(level, near.getX() + (int) Math.round(Math.cos(angle) * distance),
				near.getZ() + (int) Math.round(Math.sin(angle) * distance));
		return spot == null ? 0 : spawnFlock(level, spot, random);
	}

	/** The block above open grass in turkey country at (x, z), in a loaded chunk, or null. */
	public static @Nullable BlockPos ground(ServerLevel level, int x, int z) {
		if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))) {
			return null;
		}
		BlockPos spot = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
		if (!level.getBlockState(spot.below()).is(Blocks.GRASS_BLOCK) || !level.getBiome(spot).is(HABITAT)) {
			return null;
		}
		return spot;
	}

	/** Puts a flock of turkeys on the ground at {@code spot}, the first a tom; returns how many. */
	public static int spawnFlock(ServerLevel level, BlockPos spot, RandomSource random) {
		int count = FLOCK_MIN + random.nextInt(FLOCK_MAX - FLOCK_MIN + 1);
		int spawned = 0;
		for (int i = 0; i < count; i++) {
			Turkey turkey = JugcraftAgriculture.TURKEY.create(level, EntitySpawnReason.NATURAL);
			if (turkey == null) {
				continue;
			}
			int x = spot.getX() + random.nextIntBetweenInclusive(-2, 2);
			int z = spot.getZ() + random.nextIntBetweenInclusive(-2, 2);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
			if (Math.abs(y - spot.getY()) > 2) {
				x = spot.getX();
				z = spot.getZ();
				y = spot.getY();
			}
			turkey.setTom(i == 0 || random.nextInt(3) == 0);
			turkey.snapTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
			if (level.addFreshEntity(turkey)) {
				spawned++;
			}
		}
		return spawned;
	}
}

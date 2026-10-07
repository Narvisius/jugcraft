package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Where crows ({@link Crow}) come from: fields by day. Every {@value #SPAWN_TICKS} ticks, for each player in the overworld,
 * {@link #SPAWN_CHANCE} of the time the server looks at one spot {@value #MIN_DISTANCE} to {@value #MAX_DISTANCE} blocks
 * away, in a loaded chunk; if a ripe crop open to the sky (crops under a roof are safe) is within {@value #FIELD_RADIUS}
 * blocks of it (a few spots sampled), a flock of {@value #FLOCK_MIN} to {@value #FLOCK_MAX} crows arrives in the sky
 * above. Fewer than {@value #NEAR_CAP} crows may be near a player, and at most {@value #LEVEL_CAP} in the world. Each try
 * reads a few dozen blocks and never loads a chunk. Crows come only while mobs spawn (the {@code spawn_mobs} game rule),
 * and whether or not they may peck (the {@code mob_griefing} rule decides only that).
 */
public final class Crows {
	public static final int SPAWN_TICKS = 200;
	public static final float SPAWN_CHANCE = 0.3F;
	public static final int MIN_DISTANCE = 16;
	public static final int MAX_DISTANCE = 40;
	public static final int FIELD_RADIUS = 6;
	public static final int FIELD_TRIES = 16;
	public static final int FLOCK_MIN = 2;
	public static final int FLOCK_MAX = 3;
	public static final int NEAR_CAP = 6;
	public static final int NEAR_RANGE = 48;
	public static final int LEVEL_CAP = 32;
	/** Day on the overworld clock, when crows are about: from dawn to dusk. */
	public static final int DAY_END = 12000;

	private Crows() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % SPAWN_TICKS != 0) {
				return;
			}
			ServerLevel level = server.overworld();
			if (!day(level) || !level.getGameRules().get(GameRules.SPAWN_MOBS) || !JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE)) {
				return;
			}
			int total = level.getEntities(JugcraftAgriculture.CROW, crow -> true).size();
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

	/** Whether crows are about: day on the overworld clock. */
	public static boolean day(Level level) {
		return Math.floorMod(level.getOverworldClockTime(), TrickOrTreat.DAY) < DAY_END;
	}

	/** Tries to bring a flock to a field around {@code near}; returns how many crows came. */
	public static int trySpawn(ServerLevel level, BlockPos near, RandomSource random) {
		if (level.getEntitiesOfClass(Crow.class, new AABB(near).inflate(NEAR_RANGE)).size() >= NEAR_CAP) {
			return 0;
		}
		double angle = random.nextDouble() * Math.PI * 2.0;
		int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
		int x = near.getX() + (int) Math.round(Math.cos(angle) * distance);
		int z = near.getZ() + (int) Math.round(Math.sin(angle) * distance);
		BlockPos field = findField(level, new BlockPos(x, near.getY(), z), random);
		return field == null ? 0 : spawnFlock(level, field, random);
	}

	/** A ripe crop within {@value #FIELD_RADIUS} blocks of {@code around}, from a few sampled spots, or null. */
	public static @Nullable BlockPos findField(ServerLevel level, BlockPos around, RandomSource random) {
		for (int i = 0; i < FIELD_TRIES; i++) {
			int x = around.getX() + random.nextIntBetweenInclusive(-FIELD_RADIUS, FIELD_RADIUS);
			int z = around.getZ() + random.nextIntBetweenInclusive(-FIELD_RADIUS, FIELD_RADIUS);
			if (!level.isLoaded(new BlockPos(x, around.getY(), z))) {
				continue;
			}
			BlockPos spot = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			if (Crow.tempting(level.getBlockState(spot))) {
				return spot;
			}
		}
		return null;
	}

	/** Puts a flock of crows in the sky above {@code field}; returns how many. */
	public static int spawnFlock(ServerLevel level, BlockPos field, RandomSource random) {
		int count = FLOCK_MIN + random.nextInt(FLOCK_MAX - FLOCK_MIN + 1);
		int spawned = 0;
		for (int i = 0; i < count; i++) {
			Crow crow = JugcraftAgriculture.CROW.create(level, EntitySpawnReason.NATURAL);
			if (crow == null) {
				continue;
			}
			crow.snapTo(field.getX() + 0.5 + random.nextDouble() * 4 - 2, field.getY() + 6 + random.nextDouble() * 3,
					field.getZ() + 0.5 + random.nextDouble() * 4 - 2, random.nextFloat() * 360.0F, 0.0F);
			if (level.addFreshEntity(crow)) {
				spawned++;
			}
		}
		return spawned;
	}
}

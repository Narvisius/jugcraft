package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Where will-o'-wisps ({@link WillOWisp}) appear: on Halloween nights (the event running, the overworld between
 * {@link HarvestMoon#DUSK} and {@link HarvestMoon#DAWN}), over swamps and cornfields. Every {@link #SPAWN_TICKS}
 * ticks, for each player in the overworld, {@link #SPAWN_CHANCE} of the time the server tries one spot
 * {@link #MIN_DISTANCE} to {@link #MAX_DISTANCE} blocks away, in a loaded chunk, open to the sky: a swamp, or within
 * {@link #CORN_RADIUS} blocks of a corn plant. Fewer than {@link #NEAR_CAP} wisps may be near a player, and at most
 * {@link #LEVEL_CAP} in the world. Each try reads at most a few dozen blocks and never loads a chunk.
 */
public final class Wisps {
	public static final int SPAWN_TICKS = 100;
	public static final float SPAWN_CHANCE = 0.5F;
	public static final int MIN_DISTANCE = 10;
	public static final int MAX_DISTANCE = 32;
	public static final int CORN_RADIUS = 2;
	public static final int NEAR_CAP = 4;
	public static final int NEAR_RANGE = 48;
	public static final int LEVEL_CAP = 64;

	private Wisps() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % SPAWN_TICKS != 0) {
				return;
			}
			ServerLevel level = server.overworld();
			if (!night(level) || !JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE)) {
				return;
			}
			int total = level.getEntities(JugcraftAgriculture.WILL_O_WISP, wisp -> true).size();
			for (ServerPlayer player : level.players()) {
				if (total >= LEVEL_CAP) {
					return;
				}
				if (!player.isSpectator() && level.getRandom().nextFloat() < SPAWN_CHANCE && trySpawn(level, player.blockPosition(), level.getRandom())) {
					total++;
				}
			}
		});
	}

	/** Whether wisps are about: the event runs and it is night on the overworld clock. */
	public static boolean night(Level level) {
		return night(level.getOverworldClockTime());
	}

	/** Whether wisps are about at {@code dayTime} on the overworld clock. */
	public static boolean night(long dayTime) {
		long hour = Math.floorMod(dayTime, TrickOrTreat.DAY);
		return HalloweenSeason.active() && hour >= HarvestMoon.DUSK && hour < HarvestMoon.DAWN;
	}

	/** Whether nothing that blocks movement (leaves included) is above {@code pos}: open to the sky, by the heightmap. */
	public static boolean openSky(ServerLevel level, BlockPos pos) {
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY();
	}

	/** Tries to put one wisp somewhere around {@code near}; returns whether it did. */
	public static boolean trySpawn(ServerLevel level, BlockPos near, RandomSource random) {
		if (level.getEntitiesOfClass(WillOWisp.class, new AABB(near).inflate(NEAR_RANGE)).size() >= NEAR_CAP) {
			return false;
		}
		double angle = random.nextDouble() * Math.PI * 2.0;
		int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
		int x = near.getX() + (int) Math.round(Math.cos(angle) * distance);
		int z = near.getZ() + (int) Math.round(Math.sin(angle) * distance);
		if (!level.isLoaded(new BlockPos(x, near.getY(), z))) {
			return false;
		}
		BlockPos spot = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
		// Crops do not block motion: rise above a field's plants to the first free block.
		for (int i = 0; i < 4 && !level.getBlockState(spot).isAir(); i++) {
			spot = spot.above();
		}
		if (!canSpawnAt(level, spot)) {
			return false;
		}
		WillOWisp wisp = JugcraftAgriculture.WILL_O_WISP.create(level, EntitySpawnReason.NATURAL);
		if (wisp == null) {
			return false;
		}
		wisp.snapTo(x + 0.5, spot.getY() + 1.0 + random.nextDouble(), z + 0.5, random.nextFloat() * 360.0F, 0.0F);
		return level.addFreshEntity(wisp);
	}

	/** Whether {@code spot} (the first free block above the ground) suits a wisp: open sky, over a swamp or by corn. */
	public static boolean canSpawnAt(ServerLevel level, BlockPos spot) {
		if (!level.getBlockState(spot).isAir() || !openSky(level, spot)) {
			return false;
		}
		if (level.getBiome(spot).is(ConventionalBiomeTags.IS_SWAMP)) {
			return true;
		}
		for (BlockPos pos : BlockPos.betweenClosed(spot.offset(-CORN_RADIUS, -4, -CORN_RADIUS), spot.offset(CORN_RADIUS, 0, CORN_RADIUS))) {
			if (level.getBlockState(pos).getBlock() instanceof TallCropBlock tall && tall.crop() == TallCrop.CORN) {
				return true;
			}
		}
		return false;
	}
}

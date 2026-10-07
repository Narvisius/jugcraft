package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Where werewolves ({@link Werewolf}) come from (fall addition 23), and what keeps them off: only on full-moon nights,
 * only in the Overworld's woods (biome tag {@code jugcraft:werewolf_haunts}). Every {@value #SPAWN_TICKS} ticks, for
 * each player, {@link #SPAWN_CHANCE} of the time the server looks at one spot {@value #MIN_DISTANCE} to
 * {@value #MAX_DISTANCE} blocks away in a loaded chunk; if it is open earth in werewolf country with no wolfsbane within
 * {@value #WARD_REACH} blocks, a werewolf steps out there, howling. Fewer than {@value #NEAR_CAP} may be near a player and
 * at most {@value #LEVEL_CAP} in the world; none come in peaceful, or while mobs don't spawn.
 *
 * <p>Which kind ({@link #kindFor}): a shadow werewolf {@value #SHADOW_HAUNT_CHANCE} of the time in its haunts (biome
 * tag {@code jugcraft:shadow_werewolf_haunts}: dark forests, the pale garden, the Gloomweald and the ghost forest) and
 * {@value #SHADOW_CHANCE} elsewhere; otherwise a snow werewolf in snowy woods ({@code jugcraft:snow_werewolf_haunts})
 * and a brown one anywhere else.
 *
 * <p>Wolfsbane ({@link #warded}) wards a creature off them: holding a sprig (not against a shadow werewolf), or
 * standing within {@value #WARD_REACH} blocks of a planted or potted one.
 */
public final class Werewolves {
	public static final int SPAWN_TICKS = 200;
	public static final float SPAWN_CHANCE = 0.3F;
	public static final int MIN_DISTANCE = 24;
	public static final int MAX_DISTANCE = 40;
	public static final int NEAR_CAP = 2;
	public static final int NEAR_RANGE = 64;
	public static final int LEVEL_CAP = 8;
	public static final int WARD_REACH = 6;
	public static final String WOLFSBANE = "wolfsbane";
	/** How long wolfsbane in suspicious stew poisons, in seconds. */
	public static final float STEW_SECONDS = 8.0F;
	public static final String SILVER_ARROW = "silver_arrow";
	public static final TagKey<Biome> HAUNTS = TagKey.create(Registries.BIOME, Jugcraft.id("werewolf_haunts"));
	public static final TagKey<Biome> SNOW_HAUNTS = TagKey.create(Registries.BIOME, Jugcraft.id("snow_werewolf_haunts"));
	public static final TagKey<Biome> SHADOW_HAUNTS = TagKey.create(Registries.BIOME, Jugcraft.id("shadow_werewolf_haunts"));
	public static final float SHADOW_CHANCE = 0.08F;
	public static final float SHADOW_HAUNT_CHANCE = 0.5F;

	private Werewolves() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % SPAWN_TICKS != 0) {
				return;
			}
			ServerLevel level = server.overworld();
			if (!canHaunt(level) || !JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE)) {
				return;
			}
			int total = level.getEntities(JugcraftAgriculture.WEREWOLF, werewolf -> true).size();
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

	/** Whether it is a full-moon night (by the Overworld's moon). */
	public static boolean fullMoon(ServerLevel level) {
		return MooncakeItem.fullMoonNight(level.getOverworldClockTime());
	}

	/** Whether werewolves may come out now: a full-moon night, mobs spawning, and not peaceful. */
	public static boolean canHaunt(ServerLevel level) {
		return fullMoon(level) && level.getGameRules().get(GameRules.SPAWN_MOBS) && level.getDifficulty() != Difficulty.PEACEFUL;
	}

	/** Tries to bring a werewolf out of the woods around {@code near}; returns how many came (0 or 1). */
	public static int trySpawn(ServerLevel level, BlockPos near, RandomSource random) {
		if (level.getEntitiesOfClass(Werewolf.class, new AABB(near).inflate(NEAR_RANGE)).size() >= NEAR_CAP) {
			return 0;
		}
		double angle = random.nextDouble() * Math.PI * 2.0;
		int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
		BlockPos spot = ground(level, near.getX() + (int) Math.round(Math.cos(angle) * distance),
				near.getZ() + (int) Math.round(Math.sin(angle) * distance));
		if (spot == null || wardNear(level, spot)) {
			return 0;
		}
		Werewolf werewolf = JugcraftAgriculture.WEREWOLF.create(level, EntitySpawnReason.NATURAL);
		if (werewolf == null) {
			return 0;
		}
		werewolf.setKind(kindFor(level, spot, random));
		werewolf.setHealth(werewolf.getMaxHealth());
		werewolf.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		if (!level.addFreshEntity(werewolf)) {
			return 0;
		}
		werewolf.howl(level);
		return 1;
	}

	/** The kind of werewolf that comes out at {@code spot}, by its biome. */
	public static Werewolf.Kind kindFor(ServerLevel level, BlockPos spot, RandomSource random) {
		Holder<Biome> biome = level.getBiome(spot);
		return kindFor(biome.is(SNOW_HAUNTS), biome.is(SHADOW_HAUNTS), random.nextFloat());
	}

	/**
	 * The kind that comes out where it is {@code snowy} or {@code shadowy} (a shadow werewolf's haunt) for a {@code roll}
	 * between 0 and 1.
	 */
	public static Werewolf.Kind kindFor(boolean snowy, boolean shadowy, float roll) {
		if (roll < (shadowy ? SHADOW_HAUNT_CHANCE : SHADOW_CHANCE)) {
			return Werewolf.Kind.SHADOW;
		}
		return snowy ? Werewolf.Kind.SNOW : Werewolf.Kind.BROWN;
	}

	/** The block above open woodland floor in werewolf country at (x, z), in a loaded chunk, or null. */
	public static @Nullable BlockPos ground(ServerLevel level, int x, int z) {
		if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))) {
			return null;
		}
		BlockPos spot = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
		if (!woodlandFloor(level, spot) || !level.getBiome(spot).is(HAUNTS)) {
			return null;
		}
		return spot;
	}

	/**
	 * Whether the block under {@code spot} is woodland floor: earth a tree could grow on (grass, dirt, podzol, moss and
	 * the like), as an oak sapling judges it.
	 */
	public static boolean woodlandFloor(LevelReader level, BlockPos spot) {
		return Blocks.OAK_SAPLING.defaultBlockState().canSurvive(level, spot);
	}

	public static Item wolfsbane() {
		return JugcraftAgriculture.item(WOLFSBANE);
	}

	/** Whether {@code state} is wolfsbane, planted or potted. */
	public static boolean isWard(BlockState state) {
		Block plant = JugcraftAgriculture.block(WOLFSBANE);
		return state.is(plant) || state.is(JugcraftAgriculture.block("potted_" + WOLFSBANE));
	}

	/** Whether wolfsbane grows or stands potted within {@value #WARD_REACH} blocks of {@code pos} (and 3 up or down). */
	public static boolean wardNear(ServerLevel level, BlockPos pos) {
		for (BlockPos at : BlockPos.betweenClosed(pos.offset(-WARD_REACH, -3, -WARD_REACH), pos.offset(WARD_REACH, 3, WARD_REACH))) {
			if (isWard(level.getBlockState(at))) {
				return true;
			}
		}
		return false;
	}

	/** Whether wolfsbane wards {@code target} off: a sprig in either hand, or wolfsbane near them. */
	public static boolean warded(ServerLevel level, LivingEntity target) {
		return warded(level, target, true);
	}

	/** Whether wolfsbane wards {@code target} off: a sprig in either hand (if {@code sprigs} count), or wolfsbane near them. */
	public static boolean warded(ServerLevel level, LivingEntity target, boolean sprigs) {
		return sprigs && (target.getMainHandItem().is(wolfsbane()) || target.getOffhandItem().is(wolfsbane())) || wardNear(level, target.blockPosition());
	}
}

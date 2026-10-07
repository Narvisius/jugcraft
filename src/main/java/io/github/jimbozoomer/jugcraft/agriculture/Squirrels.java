package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Where squirrels ({@link Squirrel}) come from, and acorns (fall addition 24). Squirrels come to the woods (biome tag
 * {@code jugcraft:squirrel_habitat}) by day: every {@value #SPAWN_TICKS} ticks, for each player in the Overworld,
 * {@link #SPAWN_CHANCE} of the time the server tries one spot {@value #MIN_DISTANCE} to {@value #MAX_DISTANCE} blocks
 * away, in a loaded chunk; on open earth in squirrel country, one or two come. None come while {@value #NEAR_CAP} are
 * within {@value #NEAR_RANGE} blocks of the player or {@value #LEVEL_CAP} are in the world, nor while mobs don't spawn.
 * Like any wild animal, they stay.
 *
 * <p>Oak and dark oak leaves drop an acorn {@link #ACORN_CHANCE} of the time they are broken or decay, as they drop
 * apples.
 */
public final class Squirrels {
	public static final int SPAWN_TICKS = 400;
	public static final float SPAWN_CHANCE = 0.25F;
	public static final int MIN_DISTANCE = 20;
	public static final int MAX_DISTANCE = 40;
	public static final int NEAR_CAP = 6;
	public static final int NEAR_RANGE = 64;
	public static final int LEVEL_CAP = 30;
	public static final float ACORN_CHANCE = 0.05F;
	public static final List<String> ACORN_LEAVES = List.of("oak_leaves", "dark_oak_leaves");
	public static final TagKey<Biome> HABITAT = TagKey.create(Registries.BIOME, Jugcraft.id("squirrel_habitat"));

	private Squirrels() {
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
			int total = level.getEntities(JugcraftAgriculture.SQUIRREL, squirrel -> true).size();
			for (ServerPlayer player : level.players()) {
				if (total >= LEVEL_CAP) {
					return;
				}
				if (!player.isSpectator() && level.getRandom().nextFloat() < SPAWN_CHANCE) {
					total += trySpawn(level, player.blockPosition(), level.getRandom());
				}
			}
		});
		for (String leaves : ACORN_LEAVES) {
			ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("minecraft", "blocks/" + leaves));
			LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
				if (source.isBuiltin() && key.equals(table) && JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE)) {
					builder.withPool(LootPool.lootPool().when(LootItemRandomChanceCondition.randomChance(ACORN_CHANCE))
							.add(LootItem.lootTableItem(Squirrel.acorn())));
				}
			});
		}
	}

	/** Tries to bring a squirrel or two into squirrel country around {@code near}; returns how many came. */
	public static int trySpawn(ServerLevel level, BlockPos near, RandomSource random) {
		if (level.getEntitiesOfClass(Squirrel.class, new AABB(near).inflate(NEAR_RANGE)).size() >= NEAR_CAP) {
			return 0;
		}
		double angle = random.nextDouble() * Math.PI * 2.0;
		int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
		BlockPos spot = ground(level, near.getX() + (int) Math.round(Math.cos(angle) * distance),
				near.getZ() + (int) Math.round(Math.sin(angle) * distance));
		if (spot == null) {
			return 0;
		}
		int count = 1 + random.nextInt(2);
		int spawned = 0;
		for (int i = 0; i < count; i++) {
			Squirrel squirrel = JugcraftAgriculture.SQUIRREL.create(level, EntitySpawnReason.NATURAL);
			if (squirrel == null) {
				continue;
			}
			squirrel.snapTo(spot.getX() + 0.5 + random.nextInt(3) - 1, spot.getY(), spot.getZ() + 0.5 + random.nextInt(3) - 1,
					random.nextFloat() * 360.0F, 0.0F);
			if (level.addFreshEntity(squirrel)) {
				spawned++;
			}
		}
		return spawned;
	}

	/** The block above open earth in squirrel country at (x, z), in a loaded chunk, or null. */
	public static @Nullable BlockPos ground(ServerLevel level, int x, int z) {
		if (!level.isLoaded(new BlockPos(x, level.getSeaLevel(), z))) {
			return null;
		}
		BlockPos spot = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
		if (!Werewolves.woodlandFloor(level, spot) || !level.getBiome(spot).is(HABITAT)) {
			return null;
		}
		return spot;
	}
}

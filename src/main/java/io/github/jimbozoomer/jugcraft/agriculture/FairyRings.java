package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fairy rings: wild mushrooms, at least {@value #RING_MUSHROOMS} of them, in a circle between {@value #INNER} and
 * {@value #OUTER} blocks from a centre (a block above or below counts). Wild mushrooms sprout them on full-moon nights
 * ({@link WildMushroomBlock}), and anyone can plant one. On a full-moon night, a player standing at a ring's centre in the
 * Overworld is blessed once a night: Luck II for {@value #LUCK_TICKS} ticks, a shimmer round the ring and the advancement
 * Away with the Fairies. The server looks at each Overworld player every {@value #CHECK_TICKS} ticks, on full-moon nights
 * only; which players were blessed tonight is the server's own record, kept in memory.
 */
public final class FairyRings {
	public static final int RING_MUSHROOMS = 8;
	public static final double INNER = 2.5;
	public static final double OUTER = 3.6;
	public static final int CHECK_TICKS = 20;
	public static final int LUCK_TICKS = 6000;
	/** The circle a sprouting mushroom plants round, radius three: a fairy ring of eight. */
	static final int[][] RING = {{3, 0}, {2, 2}, {0, 3}, {-2, 2}, {-3, 0}, {-2, -2}, {0, -3}, {2, -2}};
	private static final int DAY = 24000;
	private static final Map<UUID, Long> BLESSED = new HashMap<>();

	private FairyRings() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_TICKS != 0) {
				return;
			}
			ServerLevel overworld = server.overworld();
			long time = overworld.getOverworldClockTime();
			if (!MooncakeItem.fullMoonNight(time)) {
				return;
			}
			long night = Math.floorDiv(time, (long) DAY);
			BLESSED.values().removeIf(blessed -> blessed != night);
			for (ServerPlayer player : overworld.players()) {
				if (!BLESSED.containsKey(player.getUUID()) && ring(overworld, player.blockPosition())) {
					bless(overworld, player, night);
				}
			}
		});
	}

	/** How many wild mushrooms stand in the circle round {@code centre}. */
	public static int count(Level level, BlockPos centre) {
		int found = 0;
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				double distance = Math.sqrt(dx * dx + dz * dz);
				if (distance < INNER || distance > OUTER) {
					continue;
				}
				for (int dy = -1; dy <= 1; dy++) {
					if (level.getBlockState(centre.offset(dx, dy, dz)).getBlock() instanceof WildMushroomBlock) {
						found++;
						break;
					}
				}
			}
		}
		return found;
	}

	/** Whether {@code centre} is the middle of a fairy ring. */
	public static boolean ring(Level level, BlockPos centre) {
		return count(level, centre) >= RING_MUSHROOMS;
	}

	/** Blesses {@code player} at a fairy ring (once tonight, {@code night}). */
	public static void bless(ServerLevel level, ServerPlayer player, long night) {
		BLESSED.put(player.getUUID(), night);
		player.addEffect(new MobEffectInstance(MobEffects.LUCK, LUCK_TICKS, 1));
		player.sendOverlayMessage(Component.translatable("message.jugcraft.fairy_ring.blessed"));
		level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
		for (int[] point : RING) {
			level.sendParticles(ParticleTypes.END_ROD, player.getX() + point[0], player.getY() + 0.4, player.getZ() + point[1], 6, 0.2, 0.4, 0.2, 0.01);
		}
		TrickOrTreat.award(player, "fairy_ring");
	}

	/** Whether {@code player} was blessed at a ring tonight (tests). */
	public static boolean blessedTonight(ServerPlayer player) {
		return BLESSED.containsKey(player.getUUID());
	}

	/** Forgets tonight's blessings (tests). */
	public static void forget(ServerPlayer player) {
		BLESSED.remove(player.getUUID());
	}

	/**
	 * A mushroom at {@code pos} sprouts a fairy ring of its kind: a circle of radius three it stands on, its centre on the
	 * far side of it. A mushroom goes wherever the ground takes one (a block up or down for slopes); nothing sprouts where a
	 * ring already stands.
	 */
	public static int sprout(ServerLevel level, BlockPos pos, BlockState mushroom, RandomSource random) {
		int[] at = RING[random.nextInt(RING.length)];
		BlockPos centre = pos.offset(-at[0], 0, -at[1]);
		if (ring(level, centre)) {
			return 0;
		}
		int planted = 0;
		for (int[] point : RING) {
			for (int dy : new int[] {0, 1, -1}) {
				BlockPos spot = centre.offset(point[0], dy, point[1]);
				if (level.getBlockState(spot).getBlock() instanceof WildMushroomBlock) {
					break;
				}
				if (level.isEmptyBlock(spot) && mushroom.canSurvive(level, spot)) {
					level.setBlock(spot, mushroom, Block.UPDATE_ALL);
					planted++;
					break;
				}
			}
		}
		return planted;
	}
}

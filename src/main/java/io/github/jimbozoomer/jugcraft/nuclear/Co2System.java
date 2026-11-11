package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A diesel generator room's CO2 fire suppression (round 31 part 3; Justin: "Discharge that hurts"). Pull the release
 * station's handle (Co2StationBlock): {@link #COUNTDOWN} seconds of horns and strobes (ABORT stops it), then the
 * cylinder bank dumps into the room: the room fogs, the engine shuts down (and won't start until the system is reset)
 * and anyone inside chokes ({@link #DAMAGE} a second, slowed and sick) until they get out. RESET at the station after a
 * discharge runs the room's exhaust fans for the purge ({@link #PURGE} seconds, the fog clearing), then all is normal.
 *
 * <p>Its state is the generator's (saved and sent to the players for the station's lamps, countdown, strobes and the
 * fans): {@link EquipmentBlockEntity#co2Phase} and since when. The room is the open space found from in front of the
 * cylinder bank (DieselRoom), bounded by full blocks and doors, no farther than {@link #REACH} across and
 * {@link #HIGH} up or down.
 */
public final class Co2System {
	public static final int NORMAL = 0, COUNTING = 1, DISCHARGED = 2, PURGING = 3;
	/** Seconds of horns and strobes before the discharge, of the purge, and the choking (health a second). */
	public static final int COUNTDOWN = 30, PURGE = 60;
	public static final float DAMAGE = 2.0F;
	/** How far the room may reach from in front of the bank (blocks across, up or down), and its most cells. */
	static final int REACH = 26, HIGH = 16, MAX_CELLS = 40_000;
	public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, Jugcraft.id("co2"));

	private Co2System() {
	}

	/** Seconds left of the countdown at game time {@code now} (0 once discharged). */
	public static int secondsLeft(EquipmentBlockEntity edg, double now) {
		return edg.co2Phase == COUNTING ? (int) Math.max(0, Math.ceil(COUNTDOWN - (now - edg.co2At) / 20.0)) : 0;
	}

	/** The station's handle pulled: the countdown starts (the generator's room must have its CO2 bank). */
	public static void pull(ServerLevel level, EquipmentBlockEntity edg, BlockPos station, @Nullable Player player) {
		long now = level.getGameTime();
		EquipmentBlockEntity bank = bank(level, edg);
		if (bank == null) {
			tell(player, Component.translatable("message.jugcraft.nuclear.co2_no_bank", DieselPanels.name(edg)));
			return;
		}
		switch (edg.co2Phase) {
			case NORMAL -> {
				edg.co2Phase = COUNTING;
				edg.co2At = now;
				edg.co2Station = station.immutable();
				edg.changed();
				horn(level, edg, bank);
				tell(player, Component.translatable("message.jugcraft.nuclear.co2_pulled", DieselPanels.name(edg), COUNTDOWN));
			}
			case COUNTING -> tell(player, Component.translatable("message.jugcraft.nuclear.co2_counting", secondsLeft(edg, now)));
			default -> tell(player, Component.translatable("message.jugcraft.nuclear.co2_already", DieselPanels.name(edg)));
		}
	}

	/** ABORT: the countdown stops (nothing else to stop). */
	public static void abort(ServerLevel level, EquipmentBlockEntity edg, @Nullable Player player) {
		if (edg.co2Phase == COUNTING) {
			edg.co2Phase = NORMAL;
			edg.co2At = level.getGameTime();
			edg.changed();
			tell(player, Component.translatable("message.jugcraft.nuclear.co2_aborted", DieselPanels.name(edg)));
		} else {
			tell(player, Component.translatable("message.jugcraft.nuclear.co2_no_abort"));
		}
	}

	/** RESET: after a discharge, the purge (the room's fans); in the countdown it is ABORT that stops it. */
	public static void reset(ServerLevel level, EquipmentBlockEntity edg, @Nullable Player player) {
		long now = level.getGameTime();
		switch (edg.co2Phase) {
			case DISCHARGED -> {
				edg.co2Phase = PURGING;
				edg.co2At = now;
				edg.changed();
				tell(player, Component.translatable("message.jugcraft.nuclear.co2_purge", PURGE));
			}
			case PURGING -> tell(player, Component.translatable("message.jugcraft.nuclear.co2_purging",
					(int) Math.ceil(PURGE - (now - edg.co2At) / 20.0)));
			case COUNTING -> tell(player, Component.translatable("message.jugcraft.nuclear.co2_use_abort"));
			default -> tell(player, Component.translatable("message.jugcraft.nuclear.co2_normal", DieselPanels.name(edg)));
		}
	}

	/** Each tick for a generator: the countdown's horns, the discharge, the choking and the fog, the purge. */
	public static void tick(ServerLevel level, EquipmentBlockEntity edg, long now) {
		long t = now - edg.co2At;
		switch (edg.co2Phase) {
			case COUNTING -> {
				EquipmentBlockEntity bank = bank(level, edg);
				if (bank == null) {
					// (Its bank taken away: nothing to discharge.)
					edg.co2Phase = NORMAL;
					edg.changed();
				} else if (t >= COUNTDOWN * 20L) {
					discharge(level, edg, bank, now);
				} else if (t % 20 == 0) {
					horn(level, edg, bank);
				}
			}
			case DISCHARGED -> {
				Set<BlockPos> room = room(level, edg);
				if (t % 20 == 0) {
					choke(level, room);
				}
				if (t % 5 == 0) {
					fog(level, room, 14);
				}
			}
			case PURGING -> {
				if (t >= PURGE * 20L) {
					edg.co2Phase = NORMAL;
					edg.co2At = now;
					edg.co2Room = null;
					edg.changed();
				} else if (t % 5 == 0) {
					// The fog clears as the fans draw it out.
					fog(level, room(level, edg), (int) Math.round(14 * (1 - t / (PURGE * 20.0))));
				}
			}
			default -> {
			}
		}
	}

	/** The discharge: the room fogs, the engine is shut down, the bank's roar. */
	static void discharge(ServerLevel level, EquipmentBlockEntity edg, EquipmentBlockEntity bank, long now) {
		edg.co2Phase = DISCHARGED;
		edg.co2At = now;
		edg.co2Room = flood(level, start(bank));
		edg.changed();
		if (edg.running) {
			EquipmentPlacement.dieselSwitch(level, edg, null, false, DieselPanels.FORCE);
		}
		play(level, bank.middle(), JugcraftNuclear.CO2_DISCHARGE, 2.0F, 1.0F);
		play(level, edg.middle(), JugcraftNuclear.CO2_DISCHARGE, 2.0F, 0.9F);
		fog(level, edg.co2Room, 60);
	}

	/** The room as found (again after a restart), or empty with no bank. */
	static Set<BlockPos> room(ServerLevel level, EquipmentBlockEntity edg) {
		if (edg.co2Room == null) {
			EquipmentBlockEntity bank = bank(level, edg);
			edg.co2Room = bank == null ? Set.of() : flood(level, start(bank));
		}
		return edg.co2Room;
	}

	/** Where the room is looked for from: the cell in front of the bank's anchor. */
	public static BlockPos start(EquipmentBlockEntity bank) {
		return bank.getBlockPos().offset(EquipmentKind.toWorld(new BlockPos(0, 0, -1), bank.facing()));
	}

	/** The open cells reachable from {@code from}: not through full blocks or doors, within reach. */
	public static Set<BlockPos> flood(ServerLevel level, BlockPos from) {
		Set<BlockPos> seen = new HashSet<>();
		if (!open(level, from)) {
			return seen;
		}
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(from);
		queue.add(from);
		while (!queue.isEmpty() && seen.size() < MAX_CELLS) {
			BlockPos at = queue.poll();
			for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
				BlockPos n = at.relative(d);
				if (Math.abs(n.getX() - from.getX()) > REACH || Math.abs(n.getZ() - from.getZ()) > REACH || Math.abs(n.getY() - from.getY()) > HIGH
						|| seen.contains(n) || !level.isLoaded(n) || !open(level, n)) {
					continue;
				}
				seen.add(n);
				queue.add(n);
			}
		}
		return seen;
	}

	private static boolean open(ServerLevel level, BlockPos at) {
		BlockState state = level.getBlockState(at);
		return !(state.getBlock() instanceof DoorBlock) && (state.isAir() || !state.isCollisionShapeFullBlock(level, at));
	}

	/** Each second of the discharge: everyone breathing in the room chokes (creative and spectating players apart). */
	static void choke(ServerLevel level, Set<BlockPos> room) {
		if (room.isEmpty()) {
			return;
		}
		AABB box = bounds(room);
		DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE_TYPE));
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
			if (e instanceof Player p && (p.isCreative() || p.isSpectator()) || !room.contains(BlockPos.containing(e.getEyePosition()))) {
				continue;
			}
			e.hurtServer(level, source, DAMAGE);
			e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1, false, false));
			e.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0, false, false));
		}
	}

	/** Puffs of the fog through the room ({@code puffs} of them, each a little cloud). */
	static void fog(ServerLevel level, Set<BlockPos> room, int puffs) {
		if (room.isEmpty() || puffs <= 0) {
			return;
		}
		List<BlockPos> cells = new ArrayList<>(room);
		net.minecraft.util.RandomSource random = level.getRandom();
		for (int k = 0; k < puffs; k++) {
			BlockPos at = cells.get(random.nextInt(cells.size()));
			level.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 6, 0.8, 0.5, 0.8, 0.005);
		}
	}

	static AABB bounds(Set<BlockPos> room) {
		int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
		for (BlockPos p : room) {
			x0 = Math.min(x0, p.getX());
			y0 = Math.min(y0, p.getY());
			z0 = Math.min(z0, p.getZ());
			x1 = Math.max(x1, p.getX());
			y1 = Math.max(y1, p.getY());
			z1 = Math.max(z1, p.getZ());
		}
		return new AABB(x0, y0, z0, x1 + 1, y1 + 1, z1 + 1);
	}

	/** One blast of the horns: at the bank, the station and the generator (heard across the room). */
	static void horn(ServerLevel level, EquipmentBlockEntity edg, EquipmentBlockEntity bank) {
		play(level, bank.middle(), JugcraftNuclear.CO2_HORN, 1.6F, 1.0F);
		play(level, edg.middle(), JugcraftNuclear.CO2_HORN, 1.6F, 1.0F);
		if (edg.co2Station != null) {
			play(level, Vec3.atCenterOf(edg.co2Station), JugcraftNuclear.CO2_HORN, 1.4F, 1.0F);
		}
	}

	private static void play(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, at.x, at.y, at.z, sound, SoundSource.BLOCKS, volume, pitch);
	}

	/** The generator's CO2 bank (its room's nearest), or null. */
	static @Nullable EquipmentBlockEntity bank(ServerLevel level, EquipmentBlockEntity edg) {
		List<EquipmentBlockEntity> banks = DieselRoom.pieces(level, edg, EquipmentKind.EDG_CO2_BANK);
		return banks.isEmpty() ? null : banks.get(0);
	}

	private static void tell(@Nullable Player player, Component message) {
		if (player != null) {
			player.sendOverlayMessage(message);
		}
	}

	/** What the CO2 system's state is called (the set's status). */
	public static String state(EquipmentBlockEntity edg) {
		return switch (edg.co2Phase) {
			case COUNTING -> ", CO2 COUNTDOWN";
			case DISCHARGED -> ", CO2 DISCHARGED";
			case PURGING -> ", CO2 PURGE";
			default -> "";
		};
	}
}

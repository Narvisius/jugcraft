package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Cydonian Sanctum (docs/features/cydonian-sanctum.md): where its parts lie, relative to its template's corner (the
 * numbers of tools/cydonian_sanctum.py, checked against it by tools/check_mod_data.py; an instance's part is at
 * {@code Lair.CYDONIAN_SANCTUM.origin(slot)} offset by these), and its two rules.
 *
 * <ul>
 * <li><b>Thin air:</b> everyone in the Sanctum jumps higher (Jump Boost {@value #JUMP_AMPLIFIER} + 1, kept up while they
 * are there, its icon shown) and takes {@value #FALL_DAMAGE} of a fall's damage. Looked at once a second for every player:
 * whoever is not in the Sanctum loses both.</li>
 * <li><b>The dust vents:</b> across the Dust Gallery's floor, in lines. Every {@value #VENT_PERIOD} ticks each line hisses
 * and sends up a wisp for {@value #VENT_HISS} ticks (the tell), then puffs for {@value #VENT_PUFF}: whoever stands on it
 * takes {@value #VENT_DAMAGE} damage and is thrown back south, towards the gate chamber. Neighbouring lines puff half a
 * period apart. Run for the open instances only.</li>
 * </ul>
 * The star dials and the Star Door are {@link StarDialBlock} and {@link StarDoorBlock}.
 */
public final class CydonianSanctum {
	public static final int PAVE = 6;
	public static final int STAND = 7;
	/** The crater's middle (x and z, block corners) and its floor's radius. */
	public static final double CENTRE_X = 36.5;
	public static final double CENTRE_Z = 46.5;
	public static final double CRATER = 22.0;
	/** The Star Door: the gallery's cross-section at DOOR_Z, DOOR_WEST to DOOR_EAST, DOOR_HEIGHT high (its top row the middle three). */
	public static final int DOOR_Z = 75;
	public static final int DOOR_WEST = 34;
	public static final int DOOR_EAST = 38;
	public static final int DOOR_HEIGHT = 5;
	/** The door's constellations, in its middle row, west to east in the order of {@link StarDoorBlock.Figure}. */
	public static final int DOOR_FIGURE_Y = 9;
	public static final int[] FIGURE_X = {35, 36, 37};
	/** The star dials on the gallery's walls, in the order of tools/cydonian_sanctum.py DIALS. */
	public static final BlockPos[] DIALS = {new BlockPos(33, 8, 111), new BlockPos(39, 8, 99), new BlockPos(33, 8, 87)};
	/** The dust vents' lines (z), north to south, each from VENT_WEST to VENT_EAST, and the tick each one puffs at. */
	public static final int[] VENT_LINES = {84, 90, 96, 102, 108};
	public static final int[] VENT_PHASES = {0, 30, 0, 30, 0};
	public static final int VENT_WEST = 34;
	public static final int VENT_EAST = 38;
	public static final int VENT_PERIOD = 60;
	public static final int VENT_HISS = 20;
	public static final int VENT_PUFF = 6;
	public static final float VENT_DAMAGE = 3.0F;
	public static final double VENT_PUSH = 0.9;
	public static final double VENT_LIFT = 0.35;
	/** The Red Mist in the gate chamber's south wall: its columns and row, three high from STAND. */
	public static final int EXIT_WEST = 35;
	public static final int EXIT_EAST = 37;
	public static final int EXIT_Z = 129;
	/**
	 * The Cydonian Sentinel's fight (lair/sentinel): where it waits on its plinth (WAIT_X, WAIT_Y, WAIT_Z) and where it
	 * comes down to the crater's floor as it wakes (WOKE_X, WOKE_Z); the rim's wall (its radius from the middle, RIM_WALL);
	 * the four standing stones (their middle cells, STONE_SIZE square and STONE_HEIGHT high); the small craters its
	 * sentries rise from (their middle cells); and the Red Mist it leaves in the crater's middle when it falls (its
	 * columns and row, three high from STAND). All in tools/cydonian_sanctum.py and tools/cydonian_sentinel.py.
	 */
	public static final double WAIT_X = 36.5;
	public static final double WAIT_Y = 8.0;
	public static final double WAIT_Z = 20.5;
	public static final double WOKE_X = 36.5;
	public static final double WOKE_Z = 28.5;
	public static final double RIM_WALL = 25.0;
	public static final int[][] STONES = {{44, 38}, {28, 38}, {28, 54}, {44, 54}};
	public static final int STONE_SIZE = 3;
	public static final int STONE_HEIGHT = 5;
	public static final int[][] PITS = {{26, 46}, {46, 46}, {31, 31}, {41, 31}, {31, 61}, {41, 61}};
	public static final int CRATER_MIST_WEST = 35;
	public static final int CRATER_MIST_EAST = 37;
	public static final int CRATER_MIST_Z = 46;
	/** The thin air: Jump Boost's amplifier, and how much of a fall's damage is taken. */
	public static final int JUMP_AMPLIFIER = 0;
	public static final double FALL_DAMAGE = 0.5;
	/** How long the thin air's Jump Boost lasts, renewed each second while its keeper is in the Sanctum (ticks). */
	private static final int JUMP_TICKS = 50;
	private static final Identifier THIN_AIR = Jugcraft.id("thin_air");
	public static final ResourceKey<DamageType> VENT_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, Jugcraft.id("dust_vent"));
	private static final DustParticleOptions RED_DUST = new DustParticleOptions(0xB85A36, 1.4F);

	private CydonianSanctum() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(CydonianSanctum::tick);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % Lairs.CHECK_TICKS == 0) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				air(player);
			}
		}
		ServerLevel level = server.getLevel(Lair.CYDONIAN_SANCTUM.dimension);
		if (level == null) {
			return;
		}
		for (LairInstance instance : Lairs.open(Lair.CYDONIAN_SANCTUM)) {
			vents(level, instance.origin(), level.getGameTime());
		}
	}

	// ---------------------------------------------------------------- the thin air

	/** Whether {@code player} is in the Sanctum (its dimension). */
	public static boolean inSanctum(Player player) {
		return player.level().dimension() == Lair.CYDONIAN_SANCTUM.dimension;
	}

	/** Gives {@code player} the thin air in the Sanctum, and takes it away anywhere else. */
	public static void air(ServerPlayer player) {
		AttributeInstance fall = player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
		if (!inSanctum(player) || !player.isAlive()) {
			if (fall != null) {
				fall.removeModifier(THIN_AIR);
			}
			return;
		}
		if (fall != null && !fall.hasModifier(THIN_AIR)) {
			fall.addTransientModifier(new AttributeModifier(THIN_AIR, FALL_DAMAGE - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		MobEffectInstance jump = player.getEffect(MobEffects.JUMP_BOOST);
		// A stronger or longer jump of the player's own (a potion) is left alone.
		if (jump == null || jump.getAmplifier() == JUMP_AMPLIFIER && jump.getDuration() <= JUMP_TICKS) {
			player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, JUMP_TICKS, JUMP_AMPLIFIER, true, false, true));
		}
	}

	// ---------------------------------------------------------------- the dust vents

	/** Where a vent line stands in its rhythm at game time {@code now}: 0 to VENT_PERIOD - 1, the hiss first, then the puff. */
	public static int phase(int line, long now) {
		return (int) Math.floorMod(now - VENT_PHASES[line], (long) VENT_PERIOD);
	}

	/** Whether line {@code line} is puffing at game time {@code now}. */
	public static boolean puffing(int line, long now) {
		int phase = phase(line, now);
		return phase >= VENT_HISS && phase < VENT_HISS + VENT_PUFF;
	}

	/** The space over a vent line, in the world: anyone in it is on the vents. */
	public static AABB overLine(BlockPos origin, int line) {
		int z = VENT_LINES[line];
		return new AABB(origin.getX() + VENT_WEST, origin.getY() + STAND, origin.getZ() + z, origin.getX() + VENT_EAST + 1,
				origin.getY() + STAND + 2, origin.getZ() + z + 1);
	}

	/** One tick of an instance's vents at game time {@code now}: the hiss and its wisps, then the puff. */
	public static void vents(ServerLevel level, BlockPos origin, long now) {
		for (int line = 0; line < VENT_LINES.length; line++) {
			int phase = phase(line, now);
			double z = origin.getZ() + VENT_LINES[line] + 0.5;
			double middle = origin.getX() + (VENT_WEST + VENT_EAST) / 2.0 + 0.5;
			double y = origin.getY() + STAND;
			if (phase == 0) {
				level.playSound(null, middle, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 0.6F);
			}
			if (phase < VENT_HISS && phase % 4 == 0) {
				for (int x = VENT_WEST; x <= VENT_EAST; x++) {
					level.sendParticles(ParticleTypes.WHITE_SMOKE, origin.getX() + x + 0.5, y + 0.1, z, 1, 0.15, 0.05, 0.15, 0.01);
				}
			}
			if (phase == VENT_HISS) {
				level.playSound(null, middle, y, z, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 0.5F);
				for (int x = VENT_WEST; x <= VENT_EAST; x++) {
					level.sendParticles(RED_DUST, origin.getX() + x + 0.5, y + 0.6, z, 6, 0.25, 0.6, 0.25, 0.05);
					level.sendParticles(ParticleTypes.CLOUD, origin.getX() + x + 0.5, y + 0.4, z, 2, 0.2, 0.4, 0.2, 0.02);
				}
			}
			if (puffing(line, now)) {
				for (Player player : level.getEntitiesOfClass(Player.class, overLine(origin, line), p -> p.isAlive() && !p.isSpectator())) {
					puff(level, player);
				}
			}
		}
	}

	/** A puff strikes {@code player}: its damage (once: a struck player is spared the rest of the puff), and a throw south. */
	public static boolean puff(ServerLevel level, Player player) {
		Holder<DamageType> type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(VENT_DAMAGE_TYPE);
		if (!player.hurtServer(level, new DamageSource(type), VENT_DAMAGE)) {
			return false;
		}
		player.push(0.0, VENT_LIFT, VENT_PUSH);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
		}
		return true;
	}

	// ---------------------------------------------------------------- the dials and the door

	/** Which instance's origin a position in the Sanctum belongs to (the slot it falls in). */
	public static BlockPos originAt(BlockPos pos) {
		return Lair.CYDONIAN_SANCTUM.origin(Lair.CYDONIAN_SANCTUM.slotAt(pos.getX() + 0.5));
	}

	/** The Star Door's cells, from an instance's origin. */
	public static List<BlockPos> doorCells(BlockPos origin) {
		List<BlockPos> out = new ArrayList<>();
		for (int y = STAND; y < STAND + DOOR_HEIGHT; y++) {
			for (int x = DOOR_WEST; x <= DOOR_EAST; x++) {
				if (y < STAND + DOOR_HEIGHT - 1 || x > DOOR_WEST && x < DOOR_EAST) {
					out.add(origin.offset(x, y, DOOR_Z));
				}
			}
		}
		return out;
	}

	/**
	 * The cell of the door's figure {@code figure}, from an instance's origin. {@link #FIGURE_X} holds the carved figures
	 * only, in {@link StarDoorBlock.Figure}'s order after {@code PLAIN}; a plain block has no cell of its own.
	 */
	public static BlockPos figure(BlockPos origin, StarDoorBlock.Figure figure) {
		if (figure == StarDoorBlock.Figure.PLAIN) {
			throw new IllegalArgumentException("A plain block of the Star Door has no figure's cell");
		}
		return origin.offset(FIGURE_X[figure.ordinal() - 1], DOOR_FIGURE_Y, DOOR_Z);
	}

	/**
	 * A dial was turned in the instance whose origin is {@code origin}: each of the door's figures burns while a dial shows
	 * it, and when all three burn, the door grinds open (each of its blocks crumbles a row at a time, from the top). Once
	 * open, it stays open. Returns whether the door opened now.
	 */
	public static boolean dialsChanged(ServerLevel level, BlockPos origin, @Nullable ServerPlayer player) {
		List<StarDialBlock.Stars> shown = new ArrayList<>();
		for (BlockPos dial : DIALS) {
			BlockState state = level.getBlockState(origin.offset(dial));
			if (state.getBlock() instanceof StarDialBlock) {
				shown.add(state.getValue(StarDialBlock.STARS));
			}
		}
		return match(level, origin, shown, player);
	}

	/**
	 * The dials of the instance whose origin is {@code origin} show {@code shown}: each of the door's figures burns while
	 * one of them shows it (told to {@code player}, if any, as it lights), and when all three burn the door grinds open.
	 * Returns whether the door opened now.
	 */
	public static boolean match(ServerLevel level, BlockPos origin, List<StarDialBlock.Stars> shown, @Nullable ServerPlayer player) {
		BlockPos firstFigure = figure(origin, StarDoorBlock.Figure.HUNTER);
		if (!(level.getBlockState(firstFigure).getBlock() instanceof StarDoorBlock)
				|| level.getBlockState(firstFigure).getValue(StarDoorBlock.OPENING)) {
			return false; // the door is already opening, open or gone
		}
		boolean all = true;
		for (StarDoorBlock.Figure figure : StarDoorBlock.Figure.values()) {
			if (figure == StarDoorBlock.Figure.PLAIN) {
				continue;
			}
			boolean lit = shown.contains(figure.stars());
			BlockPos at = figure(origin, figure);
			BlockState state = level.getBlockState(at);
			if (state.getBlock() instanceof StarDoorBlock && state.getValue(StarDoorBlock.LIT) != lit) {
				level.setBlock(at, state.setValue(StarDoorBlock.LIT, lit), Block.UPDATE_ALL);
				if (lit && player != null) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.dial.matched", figure.stars().display()));
				}
				if (lit) {
					level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.8F);
				}
			}
			all &= lit;
		}
		if (!all) {
			return false;
		}
		open(level, origin);
		return true;
	}

	/** Grinds the Star Door open: its blocks are marked opening and crumble a row at a time, the top row first. */
	public static void open(ServerLevel level, BlockPos origin) {
		for (BlockPos at : doorCells(origin)) {
			BlockState state = level.getBlockState(at);
			if (state.getBlock() instanceof StarDoorBlock && !state.getValue(StarDoorBlock.OPENING)) {
				level.setBlock(at, state.setValue(StarDoorBlock.OPENING, true), Block.UPDATE_CLIENTS);
				int row = origin.getY() + STAND + DOOR_HEIGHT - 1 - at.getY();
				level.scheduleTick(at, state.getBlock(), StarDoorBlock.FIRST_ROW_TICKS + row * StarDoorBlock.ROW_TICKS);
			}
		}
		BlockPos middle = origin.offset((DOOR_WEST + DOOR_EAST) / 2, STAND + 2, DOOR_Z);
		level.playSound(null, middle, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.4F, 0.4F);
		for (ServerPlayer near : level.getEntitiesOfClass(ServerPlayer.class, new AABB(middle).inflate(48.0))) {
			near.sendOverlayMessage(Component.translatable("message.jugcraft.lair.door.opened"));
		}
	}

	/** Whether the Star Door of the instance whose origin is {@code origin} is open: none of its blocks still stands. */
	public static boolean doorOpen(Level level, BlockPos origin) {
		for (BlockPos at : doorCells(origin)) {
			if (level.getBlockState(at).getBlock() instanceof StarDoorBlock) {
				return false;
			}
		}
		return true;
	}

	/** The cells of the Red Mist in the gate chamber's south wall, from an instance's origin. */
	public static List<BlockPos> exitCells(BlockPos origin) {
		List<BlockPos> out = new ArrayList<>();
		for (int y = STAND; y < STAND + 3; y++) {
			for (int x = EXIT_WEST; x <= EXIT_EAST; x++) {
				out.add(origin.offset(x, y, EXIT_Z));
			}
		}
		return out;
	}

	/** The cells of the Red Mist the Sentinel leaves in the crater's middle when it falls, from an instance's origin. */
	public static List<BlockPos> craterMistCells(BlockPos origin) {
		List<BlockPos> out = new ArrayList<>();
		for (int y = STAND; y < STAND + 3; y++) {
			for (int x = CRATER_MIST_WEST; x <= CRATER_MIST_EAST; x++) {
				out.add(origin.offset(x, y, CRATER_MIST_Z));
			}
		}
		return out;
	}

	/**
	 * Whether a point is over the crater laid out from {@code origin}: within the rim's wall of its middle, and from a
	 * small crater's floor to a few blocks over the floor.
	 */
	public static boolean inCrater(BlockPos origin, double x, double y, double z) {
		double dx = x - origin.getX() - CENTRE_X;
		double dz = z - origin.getZ() - CENTRE_Z;
		double up = y - origin.getY() - STAND;
		return dx * dx + dz * dz <= RIM_WALL * RIM_WALL && up >= -2.0 && up <= 6.0;
	}

	/** Whether a point is on the crater's floor itself (within its radius), as a foe stepping down onto it is. */
	public static boolean onCraterFloor(BlockPos origin, double x, double y, double z) {
		double dx = x - origin.getX() - CENTRE_X;
		double dz = z - origin.getZ() - CENTRE_Z;
		double up = y - origin.getY() - STAND;
		return dx * dx + dz * dz <= CRATER * CRATER && up >= -2.0 && up <= 4.0;
	}

	/** The crater's middle, on its floor, from an instance's origin. */
	public static Vec3 centre() {
		return new Vec3(CENTRE_X, STAND, CENTRE_Z);
	}
}

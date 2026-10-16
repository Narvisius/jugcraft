package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.agriculture.Werewolves;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The Howling Totem's howl, which opens the Moonlit Ring (docs/features/moonlit-ring.md): the totem used in the Overworld
 * at night, in werewolf country (the werewolves' haunts, {@code #jugcraft:werewolf_haunts}: the woods), under the open
 * sky, standing on woodland floor (earth a tree could grow on, as the werewolves' own spawning judges it), with no
 * wolfsbane growing or potted within the werewolves' ward reach ({@link Werewolves#wardNear}). The totem howls at the
 * moon and something far off answers; Grey Mist (a {@link MistGateEntity}) rises where it stood; the totem is used up
 * and the howler comes to in the ring's camp. For {@code lairs.gate_seconds} the mist is the gate: anyone who uses it
 * follows, while there is room. Nobody is taken in by standing near it.
 *
 * <p>When a step is missing (outside the Overworld, by day, out of the woods, under a roof, off the woodland floor,
 * wolfsbane near, a mist already risen within {@value #MIST_RANGE} blocks) the totem only says which and nothing is used
 * up; when every instance of the ring is taken, the same. Any night will do, not only a full moon's, and the Alpha
 * Werewolf is no Witching Season boss: the totem works all year, whatever {@code lairs.off_season} says.
 */
public final class HowlingTotemRite {
	public static final int COOLDOWN = 40;
	/** How near another open mist into the ring must not be (blocks). */
	public static final int MIST_RANGE = 3;

	private HowlingTotemRite() {
	}

	/** What the howl found missing here, in the order it is checked, or null when it is answered. */
	public enum Missing {
		DISABLED, NOT_OVERWORLD, NOT_NIGHT, NOT_WOODS, ROOFED, NOT_GROUND, WOLFSBANE, MISTING, FULL
	}

	/** The result of a howl: what was missing (or null), and the instance opened. */
	public record Howl(@Nullable Missing missing, @Nullable LairInstance instance) {
	}

	/** Uses {@code totem} where {@code player} stands. */
	public static Howl howl(ServerLevel level, ServerPlayer player, ItemStack totem) {
		BlockPos feet = feet(player);
		return howl(level, player, totem, !level.isBrightOutside(), level.getBiome(feet).is(Werewolves.HAUNTS));
	}

	/**
	 * Uses {@code totem} where {@code player} stands, {@code night} and whether the land is werewolf country ({@code woods})
	 * as given (game tests set them). Opens an instance of the ring and takes the player in, or says what is missing and
	 * uses nothing.
	 */
	public static Howl howl(ServerLevel level, ServerPlayer player, ItemStack totem, boolean night, boolean woods) {
		BlockPos feet = feet(player);
		level.playSound(null, feet, sound("entity.wolf.howl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.PLAYERS, 1.5F, 0.8F);
		Missing missing = check(level, feet, night, woods);
		if (missing != null) {
			player.sendOverlayMessage(message(missing));
			return new Howl(missing, null);
		}
		LairInstance instance = Lairs.open(level.getServer(), Lair.MOONLIT_RING);
		if (instance == null) {
			player.sendOverlayMessage(message(Missing.FULL));
			return new Howl(Missing.FULL, null);
		}
		totem.consume(1, player);
		// Something far off answers: a deeper howl, then the mist rising.
		level.playSound(null, feet, sound("entity.wolf.howl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 3.0F, 0.45F);
		level.sendParticles(ParticleTypes.CLOUD, player.getX(), feet.getY() + 0.2, player.getZ(), 30, 0.9, 0.2, 0.9, 0.01);
		level.sendParticles(ParticleTypes.WHITE_ASH, player.getX(), feet.getY() + 1.5, player.getZ(), 20, 1.0, 0.8, 1.0, 0.0);
		MistGateEntity.open(level, feet.below(), instance, Lairs.gateTicks());
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.totem.opened"));
		follow(player, instance);
		return new Howl(null, instance);
	}

	/** Checks a howl where someone stands with their feet in {@code feet}, without changing anything. */
	public static @Nullable Missing check(ServerLevel level, BlockPos feet, boolean night, boolean woods) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			return Missing.DISABLED;
		}
		if (level.dimension() != Level.OVERWORLD) {
			return Missing.NOT_OVERWORLD;
		}
		if (!night) {
			return Missing.NOT_NIGHT;
		}
		if (!woods) {
			return Missing.NOT_WOODS;
		}
		if (roofed(level, feet)) {
			return Missing.ROOFED;
		}
		if (!Werewolves.woodlandFloor(level, feet)) {
			return Missing.NOT_GROUND;
		}
		if (Werewolves.wardNear(level, feet)) {
			return Missing.WOLFSBANE;
		}
		if (mist(level, feet) != null) {
			return Missing.MISTING;
		}
		return null;
	}

	public static Component message(Missing missing) {
		return switch (missing) {
			case DISABLED -> Component.translatable("message.jugcraft.lair.rite.disabled");
			case NOT_OVERWORLD -> Component.translatable("message.jugcraft.lair.totem.not_overworld");
			case NOT_NIGHT -> Component.translatable("message.jugcraft.lair.totem.not_night");
			case NOT_WOODS -> Component.translatable("message.jugcraft.lair.totem.not_woods");
			case ROOFED -> Component.translatable("message.jugcraft.lair.totem.roofed");
			case NOT_GROUND -> Component.translatable("message.jugcraft.lair.totem.not_ground");
			case WOLFSBANE -> Component.translatable("message.jugcraft.lair.totem.wolfsbane");
			case MISTING -> Component.translatable("message.jugcraft.lair.totem.misting");
			case FULL -> Component.translatable("message.jugcraft.lair.full", Component.translatable("lair.jugcraft.moonlit_ring"));
		};
	}

	/**
	 * The block a player's feet are in, taken a little above where they stand: on a moss carpet or a thin snow layer they
	 * stand inside its cell, and their feet are there.
	 */
	public static BlockPos feet(Player player) {
		return BlockPos.containing(player.getX(), player.getY() + 0.2, player.getZ());
	}

	/**
	 * Whether someone with their feet in {@code feet} is under a roof: anything over their head that would stop a fall
	 * (a roof, a floor, leaves, water). Their own cell (a snow layer, a carpet) does not count, and nor does a barrier,
	 * which cannot be seen and hides no moon. The column is looked up only as high as the highest such block.
	 */
	public static boolean roofed(Level level, BlockPos feet) {
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, feet.getX(), feet.getZ());
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(feet.getX(), feet.getY(), feet.getZ());
		for (int y = feet.getY() + 1; y < top; y++) {
			BlockState state = level.getBlockState(at.setY(y));
			if (!state.is(Blocks.BARRIER) && (!state.getCollisionShape(level, at).isEmpty() || !state.getFluidState().isEmpty())) {
				return true;
			}
		}
		return false;
	}

	/** An open mist into the Moonlit Ring within {@value #MIST_RANGE} blocks of {@code at}, or null. */
	public static @Nullable MistGateEntity mist(ServerLevel level, BlockPos at) {
		AABB near = new AABB(at.getX() - MIST_RANGE, at.getY() - MIST_RANGE, at.getZ() - MIST_RANGE, at.getX() + MIST_RANGE + 1,
				at.getY() + MIST_RANGE + 1, at.getZ() + MIST_RANGE + 1);
		for (MistGateEntity gate : level.getEntitiesOfClass(MistGateEntity.class, near, gate -> gate.lair() == Lair.MOONLIT_RING)) {
			if (gate.instance() != null) {
				return gate;
			}
		}
		return null;
	}

	/** Takes {@code player} into the ring through the mist: it closes round them, and they come to in the camp. */
	static boolean follow(ServerPlayer player, LairInstance instance) {
		ServerLevel from = (ServerLevel) player.level();
		BlockPos at = player.blockPosition();
		if (!Lairs.enter(player, instance)) {
			return false;
		}
		from.playSound(null, at, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.6F, 0.6F);
		from.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 12, 0.3, 0.5, 0.3, 0.02);
		return true;
	}

	/** A wolf's sound by its id (wolves' sounds vary by kind), or {@code fallback}. */
	static SoundEvent sound(String id, SoundEvent fallback) {
		return BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.withDefaultNamespace(id)).orElse(fallback);
	}
}

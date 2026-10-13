package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlockEntity;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A prick of the finger, which opens the Spindle Loft (docs/features/spindle-loft.md): a Cursed Spindle used on a Spinning
 * Wheel in the Overworld at night. The wheel spins wild and its thread winds round the player, who pricks their finger,
 * falls asleep and wakes in the loft, blind for a moment ({@value #WAKING_TICKS} ticks); the spindle is used up. For
 * {@code lairs.gate_seconds} the wheel keeps spinning and is the gate: anyone who uses it with an empty hand follows, while
 * there is room. Nobody is taken in by standing near it. When a step is missing (by day, outside the Overworld, out of
 * season, the wheel already spinning) the spindle says which and nothing is used up; when every instance of the loft is
 * taken, the same. Any other use of the wheel works it as ever.
 */
public final class SpindleRite {
	public static final int WAKING_TICKS = 40;
	/**
	 * Wheels whose loft closed while their chunk was not loaded, each with the game time it stops spinning wild by itself:
	 * each calms when its chunk is loaded again before then. At most {@value Lairs#MAX_INSTANCES}; a wheel left out stops
	 * when its time is up.
	 */
	private static final Map<GlobalPos, Long> UNCALMED = new HashMap<>();

	private SpindleRite() {
	}

	/** What using the spindle here found missing, in the order it is checked, or null when the rite can go on. */
	public enum Missing {
		DISABLED, NOT_OVERWORLD, NOT_NIGHT, OFF_SEASON, SPINNING, FULL
	}

	/** The result of using the spindle: what was missing (or null), and the instance opened. */
	public record Prick(@Nullable Missing missing, @Nullable LairInstance instance) {
	}

	static void register() {
		UseBlockCallback.EVENT.register(SpindleRite::useBlock);
		ServerTickEvents.END_SERVER_TICK.register(SpindleRite::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> UNCALMED.clear());
		Lairs.onClosed(Lair.SPINDLE_LOFT, SpindleRite::calm);
	}

	/**
	 * Using a Spinning Wheel: with the Cursed Spindle, the rite (on the server; the client only sends the use); with an
	 * empty hand while the wheel is a gate, following into the loft. Anything else is the wheel's own business.
	 */
	public static InteractionResult useBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		if (player.isSpectator() || !(level.getBlockState(pos).getBlock() instanceof SpinningWheelBlock)) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(JugcraftLairs.CURSED_SPINDLE)) {
			if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
				prick(server, serverPlayer, pos, stack, !server.isBrightOutside());
			}
			return InteractionResult.SUCCESS;
		}
		if (hand != InteractionHand.MAIN_HAND || !stack.isEmpty() || !(level instanceof ServerLevel server)
				|| !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}
		LairInstance gate = gate(server, pos);
		if (gate == null) {
			return InteractionResult.PASS;
		}
		follow(serverPlayer, gate);
		return InteractionResult.SUCCESS;
	}

	/**
	 * The rite: {@code player} uses {@code spindle} on the wheel at {@code wheel}, {@code night} as given (game tests set
	 * it). Opens an instance of the loft and takes the player in, or says what is missing and uses nothing.
	 */
	public static Prick prick(ServerLevel level, ServerPlayer player, BlockPos wheel, ItemStack spindle, boolean night) {
		Missing missing = check(level, wheel, night);
		if (missing != null) {
			player.sendOverlayMessage(message(missing));
			return new Prick(missing, null);
		}
		LairInstance instance = Lairs.open(level.getServer(), Lair.SPINDLE_LOFT);
		if (instance == null) {
			player.sendOverlayMessage(message(Missing.FULL));
			return new Prick(Missing.FULL, null);
		}
		spindle.consume(1, player);
		long now = level.getGameTime();
		int ticks = Lairs.gateTicks();
		instance.site = GlobalPos.of(level.dimension(), wheel.immutable());
		instance.gateUntil = now + ticks;
		if (level.getBlockEntity(wheel) instanceof SpinningWheelBlockEntity entity) {
			entity.spinWild(now, ticks);
		}
		level.playSound(null, wheel, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.6F);
		level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.8, 0.4, 0.6);
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.spindle.opened"));
		follow(player, instance);
		return new Prick(null, instance);
	}

	/** Checks the rite at the wheel at {@code wheel} without changing anything, {@code night} as given. */
	public static @Nullable Missing check(ServerLevel level, BlockPos wheel, boolean night) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			return Missing.DISABLED;
		}
		if (level.dimension() != Level.OVERWORLD) {
			return Missing.NOT_OVERWORLD;
		}
		if (!night) {
			return Missing.NOT_NIGHT;
		}
		if (!Lairs.offSeason() && !HalloweenSeason.active()) {
			return Missing.OFF_SEASON;
		}
		if (gate(level, wheel) != null) {
			return Missing.SPINNING;
		}
		return null;
	}

	public static Component message(Missing missing) {
		return switch (missing) {
			case DISABLED -> Component.translatable("message.jugcraft.lair.rite.disabled");
			case NOT_OVERWORLD -> Component.translatable("message.jugcraft.lair.spindle.not_overworld");
			case NOT_NIGHT -> Component.translatable("message.jugcraft.lair.spindle.not_night");
			case OFF_SEASON -> Component.translatable("message.jugcraft.lair.spindle.off_season");
			case SPINNING -> Component.translatable("message.jugcraft.lair.spindle.spinning");
			case FULL -> Component.translatable("message.jugcraft.lair.full", Component.translatable("lair.jugcraft.spindle_loft"));
		};
	}

	/** The open instance of the loft whose gate the wheel at {@code wheel} is, while its gate is open, or null. */
	public static @Nullable LairInstance gate(ServerLevel level, BlockPos wheel) {
		GlobalPos site = GlobalPos.of(level.dimension(), wheel.immutable());
		long now = level.getGameTime();
		for (LairInstance instance : Lairs.open(Lair.SPINDLE_LOFT)) {
			if (site.equals(instance.site) && now < instance.gateUntil) {
				return instance;
			}
		}
		return null;
	}

	/** Takes {@code player} into the loft through the wheel: a prick of the finger, sleep, and waking blind for a moment. */
	static boolean follow(ServerPlayer player, LairInstance instance) {
		ServerLevel from = (ServerLevel) player.level();
		BlockPos at = player.blockPosition();
		if (!Lairs.enter(player, instance)) {
			return false;
		}
		from.playSound(null, at, SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 0.8F, 1.3F);
		player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, WAKING_TICKS, 0, false, false, true));
		return true;
	}

	/**
	 * Five times a second, each open gate (at most {@code lairs.instances}) shows itself where its chunk is loaded: glyphs
	 * drawn into the wheel, and now and then a chime. A wheel whose loft closed while its chunk was not loaded calms here
	 * once it is.
	 */
	private static void tick(MinecraftServer server) {
		int tick = server.getTickCount();
		if (tick % 4 != 0) {
			return;
		}
		if (!UNCALMED.isEmpty()) {
			UNCALMED.entrySet().removeIf(wheel -> calm(server, wheel.getKey(), wheel.getValue()));
		}
		for (LairInstance instance : Lairs.open(Lair.SPINDLE_LOFT)) {
			GlobalPos site = instance.site;
			ServerLevel level = site == null ? null : server.getLevel(site.dimension());
			if (level == null || level.getGameTime() >= instance.gateUntil || !level.isLoaded(site.pos())) {
				continue;
			}
			BlockPos pos = site.pos();
			level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.6);
			if (tick % 40 == 0) {
				level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.0);
				level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 0.6F);
			}
		}
	}

	/**
	 * An instance has closed: its wheel stops spinning wild, at once if its chunk is loaded, or else when the chunk is loaded
	 * again (before the wheel would stop by itself).
	 */
	static void calm(MinecraftServer server, LairInstance instance) {
		GlobalPos site = instance.site;
		if (site != null && !calm(server, site, instance.gateUntil) && UNCALMED.size() < Lairs.MAX_INSTANCES) {
			UNCALMED.put(site, instance.gateUntil);
		}
	}

	/**
	 * Calms the wheel at {@code site}, which would stop by itself at {@code until}, if its chunk is loaded and it is not the
	 * gate of a loft opened since. Returns false only while its chunk is not loaded and its time is not up.
	 */
	private static boolean calm(MinecraftServer server, GlobalPos site, long until) {
		ServerLevel level = server.getLevel(site.dimension());
		if (level == null || level.getGameTime() >= until) {
			return true;
		}
		if (!level.isLoaded(site.pos())) {
			return false;
		}
		if (gate(level, site.pos()) == null && level.getBlockEntity(site.pos()) instanceof SpinningWheelBlockEntity wheel) {
			wheel.calm();
		}
		return true;
	}
}

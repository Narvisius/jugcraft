package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.world.AdamOndiAhman;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Setting the Altar Keystone into the Ancient Altar of Adam-ondi-Ahman, which opens the Cydonian Sanctum on Mars
 * (docs/features/cydonian-sanctum.md): in the Overworld, at night, all year. The keystone crumbles to dust, and the three
 * altars under the Ancient Altar kindle with red fire one after another, {@value #KINDLE_TICKS} ticks apart (the lowest
 * first); then a column of red light rises and the red mist opens over the altar (a {@link MistGateEntity}), the gate for
 * {@code lairs.gate_seconds}. Whoever set the keystone steps through, if they are still within {@value #REACH_BLOCKS}
 * blocks of the altar; anyone who uses the mist, or uses the altar with an empty hand while it is open, follows while there
 * is room. Nobody is taken in by standing near it.
 *
 * <p>The setting is heard before the altar's own use ({@link UseBlockCallback}). When a step is missing (outside the
 * Overworld, by day, the altar already burning red or its mist open within {@value #MIST_RANGE} blocks) the keystone says
 * which and nothing is used up; when every instance of the Sanctum is taken, the same. Every setting, answered or not,
 * rests the keystone for {@value #COOLDOWN} ticks. Not a Witching Season ritual: it works all year, whatever
 * {@code lairs.off_season} says. Its switches are the lairs' and Adam-ondi-Ahman's.
 */
public final class AltarKeystoneRite {
	public static final int COOLDOWN = 40;
	public static final int MIST_RANGE = 3;
	public static final int KINDLE_TICKS = 20;
	public static final int REACH_BLOCKS = 8;
	/** The altars under the Ancient Altar: each one's top (blocks under it) and half-width, the lowest first (tools/lairs.py). */
	public static final int[][] ALTARS = {{3, 5}, {2, 3}, {1, 1}};
	private static final DustParticleOptions RED_FIRE = new DustParticleOptions(0xE0401C, 1.2F);
	/** The kindlings under way, by the altar's place. Never saved: a restart closes the Sanctum's instances too. */
	private static final Map<GlobalPos, Kindling> KINDLINGS = new HashMap<>();

	private AltarKeystoneRite() {
	}

	/** What setting the keystone here found missing, in the order it is checked, or null when the Sanctum opens. */
	public enum Missing {
		DISABLED, NOT_OVERWORLD, NOT_NIGHT, MISTING, FULL
	}

	/** The result of a setting: what was missing (or null), and the instance opened. */
	public record Setting(@Nullable Missing missing, @Nullable LairInstance instance) {
	}

	/** A kindling under way: the altar, the instance it opens, who set the keystone, and since when. */
	private record Kindling(BlockPos altar, LairInstance instance, UUID setter, long started) {
	}

	static void register() {
		UseBlockCallback.EVENT.register(AltarKeystoneRite::useBlock);
		ServerTickEvents.END_SERVER_TICK.register(AltarKeystoneRite::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> KINDLINGS.clear());
	}

	/**
	 * Using the Ancient Altar: with the keystone, the setting (on the server; the client only sends the use); with an empty
	 * main hand while its red mist is open, following. Anything else is the altar's own business (it says it is dormant).
	 */
	public static InteractionResult useBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		if (player.isSpectator() || AdamOndiAhman.ANCIENT_ALTAR == null || !state.is(AdamOndiAhman.ANCIENT_ALTAR)) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(JugcraftLairs.ALTAR_KEYSTONE)) {
			if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer
					&& !player.getCooldowns().isOnCooldown(stack)) {
				player.getCooldowns().addCooldown(stack, COOLDOWN);
				set(server, serverPlayer, pos, stack);
			}
			return InteractionResult.SUCCESS;
		}
		if (hand != InteractionHand.MAIN_HAND || !stack.isEmpty() || !(level instanceof ServerLevel server)
				|| !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}
		MistGateEntity mist = mist(server, pos);
		LairInstance open = mist == null ? null : mist.instance();
		if (open == null) {
			return InteractionResult.PASS;
		}
		follow(serverPlayer, open);
		return InteractionResult.SUCCESS;
	}

	/** Sets {@code keystone} into the Ancient Altar at {@code altar}, at night if the sky over the Overworld is dark. */
	public static Setting set(ServerLevel level, ServerPlayer player, BlockPos altar, ItemStack keystone) {
		return set(level, player, altar, keystone, !level.isBrightOutside());
	}

	/**
	 * Sets {@code keystone} into the Ancient Altar at {@code altar}, {@code night} as given (game tests set it). Opens an
	 * instance of the Sanctum and kindles the altars, or says what is missing and uses nothing.
	 */
	public static Setting set(ServerLevel level, ServerPlayer player, BlockPos altar, ItemStack keystone, boolean night) {
		level.playSound(null, altar, SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.0F, 0.6F);
		Missing missing = check(level, altar, night);
		if (missing != null) {
			player.sendOverlayMessage(message(missing));
			return new Setting(missing, null);
		}
		LairInstance instance = Lairs.open(level.getServer(), Lair.CYDONIAN_SANCTUM);
		if (instance == null) {
			player.sendOverlayMessage(message(Missing.FULL));
			return new Setting(Missing.FULL, null);
		}
		keystone.consume(1, player);
		KINDLINGS.put(GlobalPos.of(level.dimension(), altar.immutable()), new Kindling(altar.immutable(), instance, player.getUUID(),
				level.getGameTime()));
		level.sendParticles(ParticleTypes.ASH, altar.getX() + 0.5, altar.getY() + 1.0, altar.getZ() + 0.5, 20, 0.3, 0.2, 0.3, 0.02);
		kindle(level, altar, 0);
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.keystone.kindled"));
		return new Setting(null, instance);
	}

	/** Checks a setting into the Ancient Altar at {@code altar}, without changing anything. */
	public static @Nullable Missing check(ServerLevel level, BlockPos altar, boolean night) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE) || !JugcraftConfig.isFeatureEnabled(AdamOndiAhman.FEATURE)) {
			return Missing.DISABLED;
		}
		if (level.dimension() != Level.OVERWORLD) {
			return Missing.NOT_OVERWORLD;
		}
		if (!night) {
			return Missing.NOT_NIGHT;
		}
		if (kindling(level, altar) || mist(level, altar) != null) {
			return Missing.MISTING;
		}
		return null;
	}

	public static Component message(Missing missing) {
		return switch (missing) {
			case DISABLED -> Component.translatable("message.jugcraft.lair.rite.disabled");
			case NOT_OVERWORLD -> Component.translatable("message.jugcraft.lair.keystone.not_overworld");
			case NOT_NIGHT -> Component.translatable("message.jugcraft.lair.keystone.not_night");
			case MISTING -> Component.translatable("message.jugcraft.lair.keystone.misting");
			case FULL -> Component.translatable("message.jugcraft.lair.full", Component.translatable("lair.jugcraft.cydonian_sanctum"));
		};
	}

	/** Whether the altar at {@code altar} is kindling (its keystone set, its mist not yet open). */
	public static boolean kindling(ServerLevel level, BlockPos altar) {
		return KINDLINGS.containsKey(GlobalPos.of(level.dimension(), altar));
	}

	/** An open mist into the Sanctum within {@value #MIST_RANGE} blocks of {@code at}, or null. */
	public static @Nullable MistGateEntity mist(ServerLevel level, BlockPos at) {
		AABB near = new AABB(at).inflate(MIST_RANGE);
		for (MistGateEntity gate : level.getEntitiesOfClass(MistGateEntity.class, near, gate -> gate.lair() == Lair.CYDONIAN_SANCTUM)) {
			if (gate.instance() != null) {
				return gate;
			}
		}
		return null;
	}

	/** Takes {@code player} through the red mist onto Mars. */
	static boolean follow(ServerPlayer player, LairInstance instance) {
		ServerLevel from = (ServerLevel) player.level();
		BlockPos at = player.blockPosition();
		if (!Lairs.enter(player, instance)) {
			return false;
		}
		from.playSound(null, at, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 0.6F);
		from.sendParticles(RED_FIRE, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 16, 0.3, 0.6, 0.3, 0.02);
		return true;
	}

	/** The altar {@code step} (0 the lowest) under the Ancient Altar at {@code altar} catches red fire round its edge. */
	static void kindle(ServerLevel level, BlockPos altar, int step) {
		int[] tier = ALTARS[step];
		double y = altar.getY() - tier[0] + 1.05;
		int half = tier[1];
		for (int i = -half; i <= half; i++) {
			for (int[] edge : new int[][] {{i, -half}, {i, half}, {-half, i}, {half, i}}) {
				double x = altar.getX() + 0.5 + edge[0];
				double z = altar.getZ() + 0.5 + edge[1];
				level.sendParticles(RED_FIRE, x, y, z, 2, 0.2, 0.1, 0.2, 0.0);
				level.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 1, 0.15, 0.05, 0.15, 0.01);
			}
		}
		level.playSound(null, altar.below(tier[0]), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.7F, 0.5F + step * 0.15F);
	}

	/** Each tick, the kindlings: the next altar catches, the kindled ones burn on, and after the third the mist opens. */
	private static void tick(MinecraftServer server) {
		if (KINDLINGS.isEmpty()) {
			return;
		}
		KINDLINGS.entrySet().removeIf(entry -> step(server, entry.getKey(), entry.getValue()));
	}

	/** One tick of a kindling; whether it is done (its mist open, or the kindling gone). */
	private static boolean step(MinecraftServer server, GlobalPos site, Kindling kindling) {
		ServerLevel level = server.getLevel(site.dimension());
		if (level == null || !kindling.instance().isOpen()) {
			return true;
		}
		long elapsed = level.getGameTime() - kindling.started();
		int kindled = (int) Math.min(ALTARS.length, elapsed / KINDLE_TICKS + 1);
		if (elapsed > 0 && elapsed % KINDLE_TICKS == 0 && elapsed / KINDLE_TICKS < ALTARS.length) {
			kindle(level, kindling.altar(), (int) (elapsed / KINDLE_TICKS));
		} else if (elapsed % 4 == 0) {
			for (int step = 0; step < kindled; step++) {
				burn(level, kindling.altar(), step);
			}
		}
		if (elapsed < (long) ALTARS.length * KINDLE_TICKS) {
			return false;
		}
		if (!level.isLoaded(kindling.altar())) {
			// Nobody is left near the altar to see it open: the Sanctum closes again, and the keystone is spent.
			Lairs.close(server, kindling.instance());
			return true;
		}
		open(level, kindling);
		return true;
	}

	/** A kindled altar's fire burns on: a few flames at random round its edge. */
	private static void burn(ServerLevel level, BlockPos altar, int step) {
		int[] tier = ALTARS[step];
		int half = tier[1];
		for (int i = 0; i < 3; i++) {
			int along = level.getRandom().nextInt(2 * half + 1) - half;
			int side = level.getRandom().nextInt(4);
			int dx = side < 2 ? along : side == 2 ? -half : half;
			int dz = side < 2 ? (side == 0 ? -half : half) : along;
			level.sendParticles(RED_FIRE, altar.getX() + 0.5 + dx, altar.getY() - tier[0] + 1.1, altar.getZ() + 0.5 + dz, 1, 0.2, 0.15, 0.2, 0.0);
		}
	}

	/** The mist opens over the altar: a column of red light, the gate, and whoever set the keystone steps through. */
	private static void open(ServerLevel level, Kindling kindling) {
		BlockPos altar = kindling.altar();
		level.playSound(null, altar, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2F, 0.6F);
		level.playSound(null, altar, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.5F);
		MistGateEntity.open(level, altar, kindling.instance(), Lairs.gateTicks());
		ServerPlayer setter = level.getServer().getPlayerList().getPlayer(kindling.setter());
		if (setter == null || setter.level() != level || !setter.isAlive()) {
			return;
		}
		setter.sendSystemMessage(Component.translatable("message.jugcraft.lair.keystone.opened"));
		if (setter.distanceToSqr(altar.getX() + 0.5, altar.getY() + 0.5, altar.getZ() + 0.5) <= REACH_BLOCKS * REACH_BLOCKS) {
			follow(setter, kindling.instance());
		}
	}

	/** Forgets every kindling (tests). */
	public static void reset() {
		KINDLINGS.clear();
	}
}

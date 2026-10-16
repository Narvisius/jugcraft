package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.building.FieldTelephoneBlock;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Sealed Orders, which open the Foundry Trench (docs/features/foundry-trench.md): the orders used on a Field Telephone
 * in the Overworld that stands in a trench: under the open sky, with at least {@value #TRENCH_BLOCKS} trench-works blocks
 * ({@code #jugcraft:trench_works}: sandbags, timber revetment, duckboards, and their slabs and stairs) within
 * {@value #TRENCH_RANGE} blocks of it. Any hour, all year. The bell rings, a whistle shrills down the line, the orders are
 * used up, and the one who used them goes over the top into the trench's dugout. For {@code lairs.gate_seconds} the line
 * stays open (a {@link MistGateEntity} over the telephone, crackling, its bell ringing every {@value #BELL_TICKS} ticks):
 * anyone who uses it, or uses the telephone with an empty hand, follows while there is room. Nobody is taken in by
 * standing near it. Then the line goes dead.
 *
 * <p>The call is heard before the telephone answers its own use ({@link UseBlockCallback}), so orders in hand never set
 * its channel. When a step is missing (outside the Overworld, under a roof, no trench round it, a line already open within
 * {@value #LINE_RANGE} blocks) the orders say which and nothing is used up; when every instance of the trench is taken,
 * the same. Every use, answered or not, rests the orders for {@value #COOLDOWN} ticks. The Iron Dreadnought is no
 * Witching Season boss: the orders work all year, whatever {@code lairs.off_season} says.
 */
public final class OrdersRite {
	public static final TagKey<Block> TRENCH_WORKS = TagKey.create(Registries.BLOCK, Jugcraft.id("trench_works"));
	public static final int COOLDOWN = 40;
	/** How near another open line into the trench must not be (blocks). */
	public static final int LINE_RANGE = 3;
	/** How far round the telephone its trench is looked for (blocks, a cube), and how many trench-works blocks it needs. */
	public static final int TRENCH_RANGE = 3;
	public static final int TRENCH_BLOCKS = 4;
	/** Ticks between rings of an open line's bell. */
	public static final int BELL_TICKS = 40;

	private OrdersRite() {
	}

	/** What using the orders here found missing, in the order it is checked, or null when the trench opens. */
	public enum Missing {
		DISABLED, NOT_OVERWORLD, ROOFED, NO_TRENCH, RINGING, FULL
	}

	/** The result of a call: what was missing (or null), and the instance opened. */
	public record Call(@Nullable Missing missing, @Nullable LairInstance instance) {
	}

	static void register() {
		UseBlockCallback.EVENT.register(OrdersRite::useBlock);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> DischargeBreakerBlock.forget());
	}

	/**
	 * Using a Field Telephone: with the orders, the call (on the server; the client only sends the use); with an empty main
	 * hand while its line into the trench is open, following. Anything else is the telephone's own business.
	 */
	public static InteractionResult useBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		if (player.isSpectator() || !(level.getBlockState(pos).getBlock() instanceof FieldTelephoneBlock)) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(JugcraftLairs.SEALED_ORDERS)) {
			if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer
					&& !player.getCooldowns().isOnCooldown(stack)) {
				player.getCooldowns().addCooldown(stack, COOLDOWN);
				call(server, serverPlayer, pos, stack);
			}
			return InteractionResult.SUCCESS;
		}
		if (hand != InteractionHand.MAIN_HAND || !stack.isEmpty() || !(level instanceof ServerLevel server)
				|| !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}
		MistGateEntity line = line(server, pos);
		LairInstance open = line == null ? null : line.instance();
		if (open == null) {
			return InteractionResult.PASS;
		}
		follow(serverPlayer, open);
		return InteractionResult.SUCCESS;
	}

	/**
	 * Uses {@code orders} on the telephone at {@code phone}. Opens an instance of the trench and takes the player over the
	 * top, or says what is missing and uses nothing.
	 */
	public static Call call(ServerLevel level, ServerPlayer player, BlockPos phone, ItemStack orders) {
		level.playSound(null, phone, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
		Missing missing = check(level, phone);
		if (missing != null) {
			player.sendOverlayMessage(message(missing));
			return new Call(missing, null);
		}
		LairInstance instance = Lairs.open(level.getServer(), Lair.FOUNDRY_TRENCH);
		if (instance == null) {
			player.sendOverlayMessage(message(Missing.FULL));
			return new Call(Missing.FULL, null);
		}
		orders.consume(1, player);
		level.playSound(null, phone, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0F, 1.8F);
		level.playSound(null, phone, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.BLOCKS, 1.2F, 2.0F);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, phone.getX() + 0.5, phone.getY() + 0.7, phone.getZ() + 0.5, 16, 0.3, 0.2, 0.3,
				0.1);
		MistGateEntity.open(level, phone, instance, Lairs.gateTicks());
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.orders.opened"));
		follow(player, instance);
		return new Call(null, instance);
	}

	/** Checks a call on the telephone at {@code phone}, without changing anything. */
	public static @Nullable Missing check(ServerLevel level, BlockPos phone) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			return Missing.DISABLED;
		}
		if (level.dimension() != Level.OVERWORLD) {
			return Missing.NOT_OVERWORLD;
		}
		if (!level.canSeeSky(phone.above())) {
			return Missing.ROOFED;
		}
		if (trenchWorks(level, phone) < TRENCH_BLOCKS) {
			return Missing.NO_TRENCH;
		}
		if (line(level, phone) != null) {
			return Missing.RINGING;
		}
		return null;
	}

	/** How many trench-works blocks stand within {@value #TRENCH_RANGE} blocks of {@code phone} (a cube). */
	public static int trenchWorks(Level level, BlockPos phone) {
		int found = 0;
		for (BlockPos at : BlockPos.betweenClosed(phone.offset(-TRENCH_RANGE, -TRENCH_RANGE, -TRENCH_RANGE),
				phone.offset(TRENCH_RANGE, TRENCH_RANGE, TRENCH_RANGE))) {
			if (level.getBlockState(at).is(TRENCH_WORKS)) {
				found++;
			}
		}
		return found;
	}

	public static Component message(Missing missing) {
		return switch (missing) {
			case DISABLED -> Component.translatable("message.jugcraft.lair.rite.disabled");
			case NOT_OVERWORLD -> Component.translatable("message.jugcraft.lair.orders.not_overworld");
			case ROOFED -> Component.translatable("message.jugcraft.lair.orders.roofed");
			case NO_TRENCH -> Component.translatable("message.jugcraft.lair.orders.no_trench");
			case RINGING -> Component.translatable("message.jugcraft.lair.orders.ringing");
			case FULL -> Component.translatable("message.jugcraft.lair.full", Component.translatable("lair.jugcraft.foundry_trench"));
		};
	}

	/** An open line into the trench within {@value #LINE_RANGE} blocks of {@code at}, or null. */
	public static @Nullable MistGateEntity line(ServerLevel level, BlockPos at) {
		AABB near = new AABB(at.getX() - LINE_RANGE, at.getY() - LINE_RANGE, at.getZ() - LINE_RANGE, at.getX() + LINE_RANGE + 1,
				at.getY() + LINE_RANGE + 1, at.getZ() + LINE_RANGE + 1);
		for (MistGateEntity gate : level.getEntitiesOfClass(MistGateEntity.class, near, gate -> gate.lair() == Lair.FOUNDRY_TRENCH)) {
			if (gate.instance() != null) {
				return gate;
			}
		}
		return null;
	}

	/** Takes {@code player} over the top into the trench, down the open line. */
	static boolean follow(ServerPlayer player, LairInstance instance) {
		ServerLevel from = (ServerLevel) player.level();
		BlockPos at = player.blockPosition();
		if (!Lairs.enter(player, instance)) {
			return false;
		}
		from.playSound(null, at, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 0.9F, 2.0F);
		from.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 8, 0.3, 0.5, 0.3, 0.02);
		return true;
	}
}

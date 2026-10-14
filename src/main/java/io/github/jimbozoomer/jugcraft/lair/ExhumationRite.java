package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.LongDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SarcophagusBlock;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Serving the Writ of Exhumation, which opens the Archive of Last Names (docs/features/crypt-archive.md): the writ served
 * on a stone sarcophagus ({@code #jugcraft:writ_tombs}) in the Overworld, in a crypt: under a roof, so that no sky can be
 * seen over either half of it. Any hour, all year. The lid grinds aside (the sarcophagus's own {@link SarcophagusBlock#OPEN}),
 * cold blue fire and grey mist well up from the dark inside (a {@link MistGateEntity} over its head), the writ is used up,
 * and the one who served it goes down the stair to the archive's landing. For {@code lairs.gate_seconds} the mist is the
 * gate: anyone who uses it, or uses the open sarcophagus with an empty hand, follows while there is room. Nobody is taken
 * in by standing near it. When the mist is gone (its time up, or the archive closed), the lid grinds shut.
 *
 * <p>The serving is heard before the sarcophagus opens its own lid ({@link UseBlockCallback}), so a writ in hand never
 * opens the tomb's slots. When a step is missing (outside the Overworld, under the open sky, a mist already rising within
 * {@value #MIST_RANGE} blocks) the writ says which and nothing is used up; when every instance of the archive is taken,
 * the same. Every serving, answered or not, rests the writ for {@value #COOLDOWN} ticks. The Crypt Lich is no Witching
 * Season boss: the writ works all year, whatever {@code lairs.off_season} says.
 */
public final class ExhumationRite {
	public static final TagKey<Block> TOMBS = TagKey.create(Registries.BLOCK, Jugcraft.id("writ_tombs"));
	public static final int COOLDOWN = 40;
	/** How near another open mist into the archive must not be (blocks). */
	public static final int MIST_RANGE = 3;
	/** How long past its time an open lid whose chunk is not loaded is still remembered, to be shut when it is (ticks). */
	private static final long REMEMBER = 24000L;
	/**
	 * The lids ground aside, each with the game time its mist closes: each is shut when its mist is gone (its time up, or its
	 * archive closed), once its chunk is loaded.
	 */
	private static final Map<GlobalPos, Long> OPEN_LIDS = new HashMap<>();

	private ExhumationRite() {
	}

	/** What serving the writ here found missing, in the order it is checked, or null when the archive opens. */
	public enum Missing {
		DISABLED, NOT_OVERWORLD, OPEN_SKY, MISTING, FULL
	}

	/** The result of a serving: what was missing (or null), and the instance opened. */
	public record Serving(@Nullable Missing missing, @Nullable LairInstance instance) {
	}

	static void register() {
		UseBlockCallback.EVENT.register(ExhumationRite::useBlock);
		ServerTickEvents.END_SERVER_TICK.register(ExhumationRite::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			OPEN_LIDS.clear();
			GraveSealBlock.forget();
			ArchiveLecternBlock.forget();
		});
	}

	/**
	 * Using a stone sarcophagus: with the writ, the serving (on the server; the client only sends the use); with an empty
	 * main hand while its mist into the archive is rising, following. Anything else is the sarcophagus's own business.
	 */
	public static InteractionResult useBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		if (player.isSpectator() || !state.is(TOMBS) || !(state.getBlock() instanceof LongDecorationBlock)) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(JugcraftLairs.EXHUMATION_WRIT)) {
			if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer
					&& !player.getCooldowns().isOnCooldown(stack)) {
				player.getCooldowns().addCooldown(stack, COOLDOWN);
				serve(server, serverPlayer, pos, stack);
			}
			return InteractionResult.SUCCESS;
		}
		if (hand != InteractionHand.MAIN_HAND || !stack.isEmpty() || !(level instanceof ServerLevel server)
				|| !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}
		MistGateEntity mist = mist(server, head(state, pos));
		LairInstance open = mist == null ? null : mist.instance();
		if (open == null) {
			return InteractionResult.PASS;
		}
		follow(serverPlayer, open);
		return InteractionResult.SUCCESS;
	}

	/**
	 * Serves {@code writ} on the sarcophagus one of whose halves is at {@code tomb}. Opens an instance of the archive and
	 * takes the player down, or says what is missing and uses nothing.
	 */
	public static Serving serve(ServerLevel level, ServerPlayer player, BlockPos tomb, ItemStack writ) {
		BlockState state = level.getBlockState(tomb);
		BlockPos head = head(state, tomb);
		level.playSound(null, head, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.6F);
		Missing missing = check(level, head);
		if (missing != null) {
			player.sendOverlayMessage(message(missing));
			return new Serving(missing, null);
		}
		LairInstance instance = Lairs.open(level.getServer(), Lair.CRYPT_ARCHIVE);
		if (instance == null) {
			player.sendOverlayMessage(message(Missing.FULL));
			return new Serving(Missing.FULL, null);
		}
		writ.consume(1, player);
		int ticks = Lairs.gateTicks();
		setLid(level, head, true);
		OPEN_LIDS.put(GlobalPos.of(level.dimension(), head.immutable()), level.getGameTime() + ticks);
		level.playSound(null, head, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, 0.5F);
		level.playSound(null, head, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 1.2F, 0.6F);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, head.getX() + 0.5, head.getY() + 0.9, head.getZ() + 0.5, 16, 0.4, 0.2, 0.4, 0.02);
		level.sendParticles(ParticleTypes.CLOUD, head.getX() + 0.5, head.getY() + 1.0, head.getZ() + 0.5, 20, 0.5, 0.3, 0.5, 0.01);
		MistGateEntity.open(level, head, instance, ticks);
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.writ.opened"));
		follow(player, instance);
		return new Serving(null, instance);
	}

	/** Checks a serving on the sarcophagus whose head is at {@code head}, without changing anything. */
	public static @Nullable Missing check(ServerLevel level, BlockPos head) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE)) {
			return Missing.DISABLED;
		}
		if (level.dimension() != Level.OVERWORLD) {
			return Missing.NOT_OVERWORLD;
		}
		BlockState state = level.getBlockState(head);
		BlockPos foot = state.getBlock() instanceof LongDecorationBlock ? LongDecorationBlock.other(state, head) : head;
		if (level.canSeeSky(head.above()) || level.canSeeSky(foot.above())) {
			return Missing.OPEN_SKY;
		}
		if (mist(level, head) != null) {
			return Missing.MISTING;
		}
		return null;
	}

	public static Component message(Missing missing) {
		return switch (missing) {
			case DISABLED -> Component.translatable("message.jugcraft.lair.rite.disabled");
			case NOT_OVERWORLD -> Component.translatable("message.jugcraft.lair.writ.not_overworld");
			case OPEN_SKY -> Component.translatable("message.jugcraft.lair.writ.open_sky");
			case MISTING -> Component.translatable("message.jugcraft.lair.writ.misting");
			case FULL -> Component.translatable("message.jugcraft.lair.full", Component.translatable("lair.jugcraft.crypt_archive"));
		};
	}

	/** The head half of the sarcophagus one of whose halves is at {@code pos} (or {@code pos}, if it is no sarcophagus). */
	public static BlockPos head(BlockState state, BlockPos pos) {
		if (!(state.getBlock() instanceof LongDecorationBlock) || state.getValue(LongDecorationBlock.PART) == BedPart.HEAD) {
			return pos;
		}
		return LongDecorationBlock.other(state, pos);
	}

	/** An open mist into the archive within {@value #MIST_RANGE} blocks of {@code at}, or null. */
	public static @Nullable MistGateEntity mist(ServerLevel level, BlockPos at) {
		AABB near = new AABB(at.getX() - MIST_RANGE, at.getY() - MIST_RANGE, at.getZ() - MIST_RANGE, at.getX() + MIST_RANGE + 1,
				at.getY() + MIST_RANGE + 1, at.getZ() + MIST_RANGE + 1);
		for (MistGateEntity gate : level.getEntitiesOfClass(MistGateEntity.class, near, gate -> gate.lair() == Lair.CRYPT_ARCHIVE)) {
			if (gate.instance() != null) {
				return gate;
			}
		}
		return null;
	}

	/** Grinds the lid of the sarcophagus whose head is at {@code head} aside, or shut: both halves, if it still lies there. */
	public static boolean setLid(Level level, BlockPos head, boolean open) {
		BlockState state = level.getBlockState(head);
		if (!state.is(TOMBS) || !state.hasProperty(SarcophagusBlock.OPEN) || state.getValue(SarcophagusBlock.OPEN) == open) {
			return false;
		}
		level.setBlock(head, state.setValue(SarcophagusBlock.OPEN, open), Block.UPDATE_ALL);
		BlockPos foot = LongDecorationBlock.other(state, head);
		BlockState footState = level.getBlockState(foot);
		if (footState.is(state.getBlock()) && footState.hasProperty(SarcophagusBlock.OPEN)) {
			level.setBlock(foot, footState.setValue(SarcophagusBlock.OPEN, open), Block.UPDATE_ALL);
		}
		return true;
	}

	/** Takes {@code player} down into the archive through the open sarcophagus. */
	static boolean follow(ServerPlayer player, LairInstance instance) {
		ServerLevel from = (ServerLevel) player.level();
		BlockPos at = player.blockPosition();
		if (!Lairs.enter(player, instance)) {
			return false;
		}
		from.playSound(null, at, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.8F, 0.8F);
		from.sendParticles(ParticleTypes.SOUL, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 8, 0.3, 0.5, 0.3, 0.02);
		return true;
	}

	/** Once a second, the lids whose mist is gone grind shut, where their chunks are loaded. */
	private static void tick(MinecraftServer server) {
		if (OPEN_LIDS.isEmpty() || server.getTickCount() % 20 != 0) {
			return;
		}
		OPEN_LIDS.entrySet().removeIf(lid -> shut(server, lid.getKey(), lid.getValue()));
	}

	/**
	 * Shuts the lid at {@code site}, whose mist closes at {@code until}, if its mist is gone (that time come, or its archive
	 * closed); whether it is done with.
	 */
	private static boolean shut(MinecraftServer server, GlobalPos site, long until) {
		ServerLevel level = server.getLevel(site.dimension());
		if (level == null || level.getGameTime() >= until + REMEMBER) {
			return true;
		}
		if (!level.isLoaded(site.pos()) || level.getGameTime() < until && mist(level, site.pos()) != null) {
			return false;
		}
		if (setLid(level, site.pos(), false)) {
			level.playSound(null, site.pos(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 0.4F);
		}
		return true;
	}
}

package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Drone Tower Core's placement warning (Justin, 28 October 2026: "When somebody ... attempt[s] to put down the drone
 * tower core, ... make a pop-up on their screen that says, warning, be sure this is the location you want to select for
 * your drone tower. It is a extremely large structure, and then give them the dimensions of the tier 9 building";
 * approved mockup tower_core_warning_mockup).
 * <p>
 * Using the core on the ground places nothing at first. The usual checks run (room for the block, another of the
 * player's towers too close); then the server asks the player's client ({@link Ask}), which shows the warning with the
 * finished tower's width, length and height (client/TowerPlaceScreen). PLACE TOWER HERE ({@link Answer}) places the
 * core where they clicked (the server checks the request, the reach and the core in hand again, and the placement runs
 * as a normal use, so claims and spawn protection still apply) and opens its first-layer loading screen
 * ({@link TowerLoadMenu}); CANCEL or Esc places nothing. A player whose client can't show it (none on a Jugcraft
 * server; a test's mock player) is told the size instead and places it by using it on the same spot again within
 * {@link #AGAIN_TICKS}. Dispensers and automation's stand-in players (concordance/Authority.isStandIn) place it without asking.
 */
public final class TowerPlacement {
	/** How long an unanswered warning stays valid (ticks). */
	public static final int ANSWER_TICKS = 2 * 60 * 20;
	/** The fallback: how soon the same spot must be used again to place it (ticks). */
	public static final int AGAIN_TICKS = 10 * 20;
	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	/** The player and spot a confirmed placement is running for (consumed by {@link #confirmed}). */
	private static final Map<UUID, BlockPos> CONFIRMED = new HashMap<>();
	private static int nextId = 1;

	private TowerPlacement() {
	}

	/** One warning waiting for its answer: the use to run again, where and when. */
	private record Pending(int id, ResourceKey<Level> level, InteractionHand hand, BlockHitResult hit, BlockPos placed, boolean secondary,
			long time, boolean asked) {
	}

	/** Server to client: show the warning for a core that would stand at {@code core}. */
	public record Ask(int id, BlockPos core) implements CustomPacketPayload {
		public static final Type<Ask> TYPE = new Type<>(Jugcraft.id("tower_place_ask"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Ask> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Ask::id, BlockPos.STREAM_CODEC, Ask::core, Ask::new);

		@Override
		public Type<Ask> type() {
			return TYPE;
		}
	}

	/** Client to server: the warning's answer, PLACE TOWER HERE ({@code place}) or CANCEL. */
	public record Answer(int id, boolean place) implements CustomPacketPayload {
		public static final Type<Answer> TYPE = new Type<>(Jugcraft.id("tower_place_answer"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Answer> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Answer::id, ByteBufCodecs.BOOL, Answer::place, Answer::new);

		@Override
		public Type<Answer> type() {
			return TYPE;
		}
	}

	static void register() {
		PayloadTypeRegistry.clientboundPlay().register(Ask.TYPE, Ask.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Answer.TYPE, Answer.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Answer.TYPE, (payload, context) -> answer(context.player(), payload.id(), payload.place()));
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getPlayer().getUUID()));
	}

	/**
	 * Called from the core's item once the usual checks have passed: may it be placed now? True for a placement the
	 * player confirmed, a dispenser's and a fake player's; otherwise it asks (or, for a client that can't show the warning,
	 * counts the first use) and says no.
	 */
	public static boolean confirmed(BlockPlaceContext context) {
		Player player = context.getPlayer();
		if (player == null || io.github.jimbozoomer.jugcraft.concordance.Authority.isStandIn(player)) {
			return true;
		}
		Level level = context.getLevel();
		if (level.isClientSide()) {
			// the server decides, and places it itself once it is confirmed
			return false;
		}
		BlockPos placed = context.getClickedPos().immutable();
		UUID id = player.getUUID();
		BlockPos ok = CONFIRMED.get(id);
		if (ok != null && ok.equals(placed)) {
			CONFIRMED.remove(id);
			return true;
		}
		long now = level.getGameTime();
		Pending pending = PENDING.get(id);
		boolean canAsk = player instanceof ServerPlayer server && canSend(server);
		if (!canAsk) {
			// no warning screen: the size in a message, and the same spot used again soon places it
			if (pending != null && !pending.asked() && pending.placed().equals(placed) && pending.level().equals(level.dimension())
					&& now - pending.time() <= AGAIN_TICKS) {
				PENDING.remove(id);
				return true;
			}
			PENDING.put(id, new Pending(0, level.dimension(), context.getHand(), hit(context), placed, player.isSecondaryUseActive(), now, false));
			TowerData.Extent size = TowerData.get(TowerData.CURRENT_LAYOUT).extent();
			player.sendSystemMessage(Component.translatable("message.jugcraft.tower.place_again", size.width(), size.length(), size.height(),
					AGAIN_TICKS / 20));
			return false;
		}
		// holding the button down repeats the use: one warning per spot, not one every few ticks
		if (pending != null && pending.asked() && pending.placed().equals(placed) && now - pending.time() < 20) {
			return false;
		}
		int ask = nextId++;
		PENDING.put(id, new Pending(ask, level.dimension(), context.getHand(), hit(context), placed, player.isSecondaryUseActive(), now, true));
		BlockPos core = TowerCoreBlock.sinks(level, placed) ? placed.below() : placed;
		ServerPlayNetworking.send((ServerPlayer) player, new Ask(ask, core));
		return false;
	}

	private static boolean canSend(ServerPlayer player) {
		try {
			return ServerPlayNetworking.canSend(player, Ask.TYPE);
		} catch (RuntimeException notConnected) {
			return false;
		}
	}

	private static BlockHitResult hit(BlockPlaceContext context) {
		return new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos().relative(
				context.getClickedFace().getOpposite(), context.replacingClickedOnBlock() ? 0 : 1), context.isInside());
	}

	/**
	 * The answer to warning {@code id}: on PLACE TOWER HERE, places the core as the use that asked would have (if it is
	 * still the player's latest, recent, in reach and the core still in that hand) and opens its loading screen. Returns
	 * whether a core was placed.
	 */
	public static boolean answer(ServerPlayer player, int id, boolean place) {
		Pending pending = PENDING.get(player.getUUID());
		if (pending == null || pending.id() != id || !pending.asked()) {
			return false;
		}
		PENDING.remove(player.getUUID());
		ServerLevel level = player.level();
		if (!place || !level.dimension().equals(pending.level()) || level.getGameTime() - pending.time() > ANSWER_TICKS
				|| !player.isAlive() || player.isSpectator()) {
			return false;
		}
		ItemStack stack = player.getItemInHand(pending.hand());
		BlockPos clicked = pending.hit().getBlockPos();
		if (!stack.is(JugcraftTower.CORE.asItem()) || !player.isWithinBlockInteractionRange(clicked, 1.0) || !level.mayInteract(player, clicked)) {
			return false;
		}
		BlockPos placed = pending.placed();
		CONFIRMED.put(player.getUUID(), placed);
		boolean wasShift = player.isShiftKeyDown();
		InteractionResult result;
		try {
			// the use that asked, again: sneaking as it was (a sneak-click on a usable block places against it)
			player.setShiftKeyDown(pending.secondary());
			result = player.gameMode.useItemOn(player, level, stack, pending.hand(), pending.hit());
		} finally {
			player.setShiftKeyDown(wasShift);
			CONFIRMED.remove(player.getUUID());
		}
		TowerCoreBlockEntity core = coreAt(level, placed);
		if (!(result instanceof InteractionResult.Success) || core == null) {
			return false;
		}
		TowerLoadMenu.openFirstLayer(player, core);
		return true;
	}

	/** The core placed at {@code placed}: there, or one lower when it set itself into the ground. */
	static @Nullable TowerCoreBlockEntity coreAt(Level level, BlockPos placed) {
		if (level.getBlockEntity(placed) instanceof TowerCoreBlockEntity core) {
			return core;
		}
		return level.getBlockEntity(placed.below()) instanceof TowerCoreBlockEntity sunk ? sunk : null;
	}

	/** Forgets a player's open warning (they left). */
	public static void forget(UUID player) {
		PENDING.remove(player);
		CONFIRMED.remove(player);
	}
}

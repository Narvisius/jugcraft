package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

/**
 * Carving pumpkins with the Carving Knife, on the server. Using the knife on the side of a pumpkin (a
 * vanilla pumpkin, one of Jugcraft's varieties, a hand-carved one or a full-grown giant pumpkin) opens a
 * carving session for that side and sends the screen to the player; the finished face comes back as a
 * {@link CarvePayload} and is checked here before anything changes:
 * <ul>
 * <li>the player opened a session for exactly that block and side, in this dimension, in the last five
 * minutes, for a face of that size (16 pixels, or 48 on a giant pumpkin, whose master block is the target);</li>
 * <li>they still hold a Carving Knife, are within reach and may build there;</li>
 * <li>the block is still such a pumpkin, the face is valid for its size, and it only carves deeper (a knife cannot put skin back);</li>
 * <li>with {@code carving.free_draw=false}, the face is one of the {@link CarvingTemplates}.</li>
 * </ul>
 * One session allows one carve, so a client cannot carve faster than it uses the knife. The first carve
 * into a plain pumpkin scoops it: its seeds come out as with shears, and the scooping table
 * ({@value #SCOOP_TABLE}) adds pumpkin guts and sometimes a giant pumpkin seed. It turns the pumpkin
 * into a {@link CarvedPumpkinBlock} of its kind, facing that side.
 */
public final class PumpkinCarvings {
	/** How long an opened carving screen stays valid: five minutes. */
	public static final long SESSION_TICKS = 6000;
	/** Extra reach to a giant pumpkin's master block, which can be a corner up to three blocks away from the side carved. */
	public static final double GIANT_REACH = 3.5;
	/** What the first cut scoops out of any pumpkin, besides its seeds (data/jugcraft/loot_table/gameplay/scoop_pumpkin.json). */
	public static final String SCOOP_TABLE = "gameplay/scoop_pumpkin";
	public static final ResourceKey<LootTable> SCOOP = ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id(SCOOP_TABLE));
	/** What hollowing a giant pumpkin into a boat scoops out (data/jugcraft/loot_table/gameplay/hollow_giant_pumpkin.json). */
	public static final String HOLLOW_TABLE = "gameplay/hollow_giant_pumpkin";
	public static final ResourceKey<LootTable> HOLLOW = ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id(HOLLOW_TABLE));
	/** Server switch: false allows only the starter faces. Tests may change it. */
	public static boolean freeDraw = true;

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();

	private record Session(ResourceKey<Level> dimension, BlockPos pos, Direction side, int size, long expires) {
	}

	public enum Result {
		CARVED, UNCHANGED, NO_SESSION, NO_KNIFE, TOO_FAR, NOT_ALLOWED, NOT_A_PUMPKIN, INVALID, UNCARVING, NOT_A_TEMPLATE
	}

	public enum HollowResult {
		HOLLOWED, NO_KNIFE, NOT_ALLOWED, TOO_SMALL, NOT_A_PUMPKIN
	}

	private PumpkinCarvings() {
	}

	static void register() {
		freeDraw = JugcraftConfig.option("carving.free_draw");
		PayloadTypeRegistry.clientboundPlay().register(OpenCarvingPayload.TYPE, OpenCarvingPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CarvePayload.TYPE, CarvePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CarvePayload.TYPE, (payload, context) -> {
			Result result = carve(context.player(), payload.pos(), payload.side(), payload.face().size(), payload.face().face());
			if (result != Result.CARVED && result != Result.UNCHANGED) {
				context.player().sendOverlayMessage(Component.translatable("message.jugcraft.carving." + result.name().toLowerCase()));
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SESSIONS.remove(handler.getPlayer().getUUID()));
		// A new world (singleplayer) starts without the last one's sessions.
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.clear());
	}

	/** Whether a block can be carved: a plain pumpkin of any kind, one already carved by hand, or a full-grown giant pumpkin. */
	public static boolean isCarvable(BlockState state) {
		if (state.getBlock() instanceof GiantPumpkinBlock) {
			return state.getValue(GiantPumpkinBlock.SIZE) == GiantPumpkinBlock.MAX_SIZE;
		}
		return JugcraftAgriculture.carvedFrom(state.getBlock()) != null || state.getBlock() instanceof CarvedPumpkinBlock;
	}

	/** Opens a carving session on one side of a pumpkin and sends the player the carving screen. */
	public static void open(ServerPlayer player, BlockPos pos, Direction side) {
		ServerLevel level = player.level();
		BlockState state = level.getBlockState(pos);
		BlockPos target = pos.immutable();
		int size = PumpkinCarving.SIZE;
		if (state.getBlock() instanceof GiantPumpkinBlock) {
			target = GiantPumpkinBlock.masterPos(pos, state).immutable();
			size = GiantPumpkinBlockEntity.FACE_SIZE;
		}
		startSession(player, target, side, size);
		if (ServerPlayNetworking.canSend(player, OpenCarvingPayload.TYPE)) {
			ServerPlayNetworking.send(player, new OpenCarvingPayload(target, side, new CarvingFace.Sized(size, currentFace(level, target, side)),
					freeDraw, stencil(player)));
		}
	}

	/** Records a carving session for {@code side} of the pumpkin at {@code pos}, replacing any earlier one. */
	public static void startSession(ServerPlayer player, BlockPos pos, Direction side) {
		startSession(player, pos, side, PumpkinCarving.SIZE);
	}

	public static void startSession(ServerPlayer player, BlockPos pos, Direction side, int size) {
		ServerLevel level = player.level();
		SESSIONS.put(player.getUUID(), new Session(level.dimension(), pos.immutable(), side, size, level.getGameTime() + SESSION_TICKS));
	}

	/** The design of a Pumpkin Stencil in the player's off hand, which the screen offers to press in. */
	public static Optional<int[]> stencil(Player player) {
		ItemStack held = player.getOffhandItem();
		PumpkinCarving design = held.get(JugcraftAgriculture.STENCIL);
		return design == null || design.isBlank(0) ? Optional.empty() : Optional.of(design.face(0));
	}

	/** What is carved into one side of a pumpkin now (all skin for a plain pumpkin); for a giant pumpkin, {@code pos} is its master. */
	public static int[] currentFace(Level level, BlockPos pos, Direction side) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof CarvedPumpkinBlock && level.getBlockEntity(pos) instanceof CarvedPumpkinBlockEntity pumpkin) {
			return pumpkin.carving().face(PumpkinCarving.faceIndex(state.getValue(CarvedPumpkinBlock.FACING), side));
		}
		if (state.getBlock() instanceof GiantPumpkinBlock && level.getBlockEntity(pos) instanceof GiantPumpkinBlockEntity giant) {
			return giant.face(side);
		}
		return new int[PumpkinCarving.SIZE];
	}

	/** Checks and applies a finished 16x16 face. */
	public static Result carve(ServerPlayer player, BlockPos pos, Direction side, int[] face) {
		return carve(player, pos, side, PumpkinCarving.SIZE, face);
	}

	/** Checks and applies a finished face of {@code size}; see the class comment for every check. */
	public static Result carve(ServerPlayer player, BlockPos pos, Direction side, int size, int[] face) {
		Session session = SESSIONS.remove(player.getUUID());
		ServerLevel level = player.level();
		if (session == null || !session.pos().equals(pos) || session.side() != side || session.dimension() != level.dimension()
				|| session.size() != size || level.getGameTime() > session.expires()) {
			return Result.NO_SESSION;
		}
		InteractionHand hand = knifeHand(player);
		if (hand == null) {
			return Result.NO_KNIFE;
		}
		boolean giantFace = size == GiantPumpkinBlockEntity.FACE_SIZE;
		if (!player.isWithinBlockInteractionRange(pos, giantFace ? GIANT_REACH : 1.0)) {
			return Result.TOO_FAR;
		}
		ItemStack knife = player.getItemInHand(hand);
		if (!player.mayUseItemAt(pos, side, knife) || !level.mayInteract(player, pos)) {
			return Result.NOT_ALLOWED;
		}
		BlockState state = level.getBlockState(pos);
		boolean giant = state.getBlock() instanceof GiantPumpkinBlock;
		boolean fits = giant ? giantFace && state.getValue(GiantPumpkinBlock.PART) == 0 && isCarvable(state) : !giantFace && isCarvable(state);
		if (!fits || side.getAxis() == Direction.Axis.Y) {
			return Result.NOT_A_PUMPKIN;
		}
		if (!CarvingFace.isValid(face, size)) {
			return Result.INVALID;
		}
		int[] before = currentFace(level, pos, side);
		if (Arrays.equals(before, face)) {
			return Result.UNCHANGED;
		}
		if (!CarvingFace.deepensOnly(before, face, size)) {
			return Result.UNCARVING;
		}
		if (!freeDraw && !CarvingTemplates.isTemplate(face, size)) {
			return Result.NOT_A_TEMPLATE;
		}

		if (giant) {
			if (!(level.getBlockEntity(pos) instanceof GiantPumpkinBlockEntity master)) {
				return Result.NOT_A_PUMPKIN;
			}
			master.setFace(side, face, player);
		} else {
			Block carved = JugcraftAgriculture.carvedFrom(state.getBlock());
			if (carved != null) {
				// The first cut opens the pumpkin: its seeds come out, and the scooping table adds guts and sometimes a giant seed.
				ResourceKey<LootTable> seeds = state.is(Blocks.PUMPKIN) ? BuiltInLootTables.CARVE_PUMPKIN : JugcraftAgriculture.carveLoot(state.getBlock());
				for (ResourceKey<LootTable> table : new ResourceKey[] {seeds, SCOOP}) {
					Block.dropFromBlockInteractLootTable(level, table, pos, state, level.getBlockEntity(pos), knife, player,
							(serverLevel, stack) -> drop(serverLevel, pos, side, stack));
				}
				state = carved.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, side);
				level.setBlock(pos, state, Block.UPDATE_ALL);
			}
			if (!(level.getBlockEntity(pos) instanceof CarvedPumpkinBlockEntity pumpkin)) {
				return Result.NOT_A_PUMPKIN;
			}
			PumpkinCarving carving = pumpkin.carving().withFace(PumpkinCarving.faceIndex(state.getValue(CarvedPumpkinBlock.FACING), side), face);
			pumpkin.setCarving(carving, player);
			if (state.getValue(CarvedPumpkinBlock.GLOW) != carving.glow()) {
				level.setBlock(pos, state.setValue(CarvedPumpkinBlock.GLOW, carving.glow()), Block.UPDATE_ALL);
			}
		}
		knife.hurtAndBreak(1, player, hand.asEquipmentSlot());
		level.playSound(null, pos, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, giant ? 0.8F : 1.0F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		player.awardStat(Stats.ITEM_USED.get(knife.getItem()));
		return Result.CARVED;
	}

	/**
	 * Hollows out a giant pumpkin 2 or 3 blocks wide into a boat, for a player using a Carving Knife on its
	 * top (the knife item asks for sneaking first, so a prize pumpkin is not hollowed by accident). A
	 * full-grown 3x3x3 pumpkin becomes a {@link PumpkinBoat.Kind#BARGE} keeping its weight, carving and torch;
	 * a 2x2x2 one a {@link PumpkinBoat.Kind#RACER} weighing {@link PumpkinBoat#RACER_BASE_WEIGHT} kg plus its
	 * growth. The pumpkin is removed without its usual drops; instead the hollowing table ({@value #HOLLOW_TABLE})
	 * gives its guts (and, from a full-grown one, its giant seeds). The player must still hold the knife and
	 * may build there; reach was checked when the block was used.
	 */
	public static HollowResult hollow(ServerPlayer player, BlockPos pos, InteractionHand hand) {
		ServerLevel level = player.level();
		ItemStack knife = player.getItemInHand(hand);
		if (!(knife.getItem() instanceof CarvingKnifeItem)) {
			return HollowResult.NO_KNIFE;
		}
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof GiantPumpkinBlock)) {
			return HollowResult.NOT_A_PUMPKIN;
		}
		int size = state.getValue(GiantPumpkinBlock.SIZE);
		if (size < 2) {
			return HollowResult.TOO_SMALL;
		}
		BlockPos master = GiantPumpkinBlock.masterPos(pos, state);
		if (!player.mayUseItemAt(pos, Direction.UP, knife) || !level.mayInteract(player, master)) {
			return HollowResult.NOT_ALLOWED;
		}
		if (!(level.getBlockEntity(master) instanceof GiantPumpkinBlockEntity giant)) {
			return HollowResult.NOT_A_PUMPKIN;
		}
		PumpkinBoat.Kind kind = size == GiantPumpkinBlock.MAX_SIZE ? PumpkinBoat.Kind.BARGE : PumpkinBoat.Kind.RACER;
		PumpkinBoatData data;
		if (kind == PumpkinBoat.Kind.BARGE) {
			int[][] faces = new int[4][];
			for (Direction side : Direction.Plane.HORIZONTAL) {
				faces[side.get2DDataValue()] = giant.face(side);
			}
			data = PumpkinBoatData.of(giant.weight(), giant.lit(), faces);
		} else {
			data = PumpkinBoatData.plain(PumpkinBoat.RACER_BASE_WEIGHT + giant.points() * GiantPumpkinBlockEntity.WEIGHT_PER_POINT);
		}
		ItemStack boat = new ItemStack(JugcraftAgriculture.item(kind.id));
		boat.set(JugcraftAgriculture.PUMPKIN_BOAT, data);
		BlockState masterState = level.getBlockState(master);
		BlockPos top = master.offset(size / 2, size - 1, size / 2);
		Block.dropFromBlockInteractLootTable(level, HOLLOW, master, masterState, giant, knife, player,
				(serverLevel, stack) -> Block.popResource(serverLevel, top, stack));
		// Removing the master removes the rest of the pumpkin, without drops.
		level.removeBlock(master, false);
		if (!player.getInventory().add(boat)) {
			Block.popResource(level, top, boat);
		}
		knife.hurtAndBreak(1, player, hand.asEquipmentSlot());
		level.playSound(null, top, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 0.6F);
		level.gameEvent(player, GameEvent.BLOCK_DESTROY, top);
		player.awardStat(Stats.ITEM_USED.get(knife.getItem()));
		return HollowResult.HOLLOWED;
	}

	private static @Nullable InteractionHand knifeHand(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			if (player.getItemInHand(hand).getItem() instanceof CarvingKnifeItem) {
				return hand;
			}
		}
		return null;
	}

	/** Drops an item in front of the carved side, as vanilla does when shears carve a pumpkin. */
	private static void drop(ServerLevel level, BlockPos pos, Direction side, ItemStack stack) {
		ItemEntity item = new ItemEntity(level, pos.getX() + 0.5 + side.getStepX() * 0.65, pos.getY() + 0.1,
				pos.getZ() + 0.5 + side.getStepZ() * 0.65, stack);
		item.setDeltaMovement(0.05 * side.getStepX() + level.getRandom().nextDouble() * 0.02, 0.05,
				0.05 * side.getStepZ() + level.getRandom().nextDouble() * 0.02);
		level.addFreshEntity(item);
	}
}

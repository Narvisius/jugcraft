package io.github.jimbozoomer.jugcraft.wallrun;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.CableBlock;
import io.github.jimbozoomer.jugcraft.fluid.FluidPipeBlock;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Pipes and cables inside made walls, as in RimWorld (docs/features/wall-runs.md). Sneak-right-click a made block (a
 * full, solid, man-made block: concrete, bricks, planks, metal; not dirt, stone, sand, ores) holding a pipe or cable
 * and the run goes inside it; sneak-right-click a laid pipe or cable holding a made block and the block goes round it.
 * The wall keeps its look (any block, drawn from its own model) and how hard it is to break; where the run comes out
 * into the open a wall sleeve shows; inside the wall nothing does. Broken, it drops the block and the pipe; Ctrl +
 * right-click on it holding a block (the wall's, a pipe, a cable, any) or sneak-right-click with the brass wrench pulls
 * just the pipe out and leaves the wall.
 */
public final class WallRuns {
	/** Each plain pipe or cable, and its run in a wall. */
	public static final Map<Block, Block> WALL_OF = new LinkedHashMap<>();
	/** Blocks that are never walls for a run: natural ground and rock. */
	public static final TagKey<Block> EXCLUDED = TagKey.create(Registries.BLOCK, Jugcraft.id("wall_run_excluded"));
	/** The look of a run whose wall is not known yet. */
	public static final BlockState FALLBACK = Blocks.SMOOTH_STONE.defaultBlockState();
	public static BlockEntityType<WallRunBlockEntity> ENTITY;

	/** Ctrl + right-click on a run in a wall (the client sees Ctrl; the server does the rest): pull the run out. */
	public record PullPayload(BlockPos pos, boolean mainHand) implements CustomPacketPayload {
		public static final Type<PullPayload> TYPE = new Type<>(Jugcraft.id("wall_run_pull"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PullPayload> CODEC = StreamCodec.of((buf, p) -> {
			buf.writeBlockPos(p.pos);
			buf.writeBoolean(p.mainHand);
		}, buf -> new PullPayload(buf.readBlockPos(), buf.readBoolean()));

		@Override
		public Type<PullPayload> type() {
			return TYPE;
		}
	}

	private WallRuns() {
	}

	public static void register() {
		wall(JugcraftFluids.BRONZE_FLUID_PIPE, p -> new WallFluidPipeBlock(p, FluidPipeBlock.BRONZE_RATE_MB, JugcraftFluids.BRONZE_FLUID_PIPE));
		wall(JugcraftFluids.STEEL_FLUID_PIPE, p -> new WallFluidPipeBlock(p, FluidPipeBlock.STEEL_RATE_MB, JugcraftFluids.STEEL_FLUID_PIPE));
		wall(JugcraftLogistics.BRASS_ITEM_PIPE, p -> new WallItemPipeBlock(p, JugcraftLogistics.BRASS_ITEM_PIPE));
		wall(JugcraftMachines.COPPER_CABLE, p -> new WallCableBlock(p, CableBlock.COPPER_RATE, JugcraftMachines.COPPER_CABLE));
		wall(JugcraftMachines.SILVER_CABLE, p -> new WallCableBlock(p, CableBlock.SILVER_RATE, JugcraftMachines.SILVER_CABLE));
		wall(JugcraftMachines.ALUMINUM_CABLE, p -> new WallCableBlock(p, CableBlock.ALUMINUM_RATE, JugcraftMachines.ALUMINUM_CABLE));
		ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("wall_run"),
				FabricBlockEntityTypeBuilder.create(WallRunBlockEntity::new, WALL_OF.values().toArray(Block[]::new)).build());
		UseBlockCallback.EVENT.register(WallRuns::use);
		PayloadTypeRegistry.serverboundPlay().register(PullPayload.TYPE, PullPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PullPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			Level level = player.level();
			BlockPos pos = payload.pos();
			ItemStack held = player.getItemInHand(payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
			if (!io.github.jimbozoomer.jugcraft.placement.StructurePermissions.mayChange(level, pos, player) || !level.isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 8 * 8
					|| !(level.getBlockState(pos).getBlock() instanceof WallRun) || !canPull(held)
					|| !player.mayUseItemAt(pos, Direction.UP, held)) {
				return;
			}
			pullOut(level, pos, player);
		});
	}

	/** What Ctrl + right-click pulls a run out with: a block in hand (the wall's own, a pipe or cable, any block). */
	public static boolean canPull(ItemStack held) {
		return held.getItem() instanceof BlockItem;
	}

	private static void wall(Block carrier, Function<BlockBehaviour.Properties, Block> factory) {
		String name = "wall_" + BuiltInRegistries.BLOCK.getKey(carrier).getPath();
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
				.strength(1.5F, 6.0F).sound(SoundType.STONE).setId(key)));
		WALL_OF.put(carrier, block);
	}

	// ------------------------------------------------------------------ what a wall is

	/**
	 * A made block a run can go in: a full, solid, plain block (no inventory, no light, not falling, breakable), not
	 * natural ground or rock ({@link #EXCLUDED}).
	 */
	public static boolean isWall(BlockState state, BlockGetter level, BlockPos pos) {
		Block block = state.getBlock();
		return !(block instanceof WallRun) && !(block instanceof EntityBlock) && !(block instanceof FallingBlock) && !state.hasBlockEntity()
				&& state.getRenderShape() == RenderShape.MODEL && state.canOcclude() && state.isCollisionShapeFullBlock(level, pos)
				&& Block.isShapeFullBlock(state.getOcclusionShape()) && state.getLightEmission() == 0 && state.getFluidState().isEmpty()
				&& state.getDestroySpeed(level, pos) >= 0 && !state.is(EXCLUDED);
	}

	// ------------------------------------------------------------------ putting a run in, taking it out

	private static InteractionResult use(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (player.isSpectator() || !player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (!(stack.getItem() instanceof BlockItem item)) {
			return InteractionResult.PASS;
		}
		BlockPos pos = hit.getBlockPos();
		BlockState there = level.getBlockState(pos);
		if (!io.github.jimbozoomer.jugcraft.placement.StructurePermissions.mayChange(level, pos, player) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) {
			return InteractionResult.PASS;
		}
		// A pipe or cable into a made block.
		Block run = WALL_OF.get(item.getBlock());
		if (run != null && isWall(there, level, pos)) {
			if (!level.isClientSide()) {
				put(level, pos, run, there);
				used(player, stack);
				SoundType sound = item.getBlock().defaultBlockState().getSoundType();
				level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8F);
			}
			return InteractionResult.SUCCESS;
		}
		// A made block round a laid pipe or cable.
		run = WALL_OF.get(there.getBlock());
		if (run != null) {
			BlockState look = item.getBlock().getStateForPlacement(new BlockPlaceContext(player, hand, stack, hit));
			if (look != null && isWall(look, level, pos)) {
				if (!level.isClientSide()) {
					put(level, pos, run, look);
					used(player, stack);
					SoundType sound = look.getSoundType();
					level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8F);
				}
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}

	private static void used(Player player, ItemStack stack) {
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
	}

	/** Makes {@code pos} a run of {@code run} in a wall of {@code look}. */
	public static void put(Level level, BlockPos pos, Block run, BlockState look) {
		BlockState state = Block.updateFromNeighbourShapes(run.defaultBlockState(), level, pos);
		level.setBlock(pos, state, Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof WallRunBlockEntity entity) {
			entity.setDisguise(look);
		}
	}

	/** The wrench: the pipe comes out, the wall stays. */
	public static void pullOut(Level level, BlockPos pos, Player player) {
		if (!io.github.jimbozoomer.jugcraft.placement.StructurePermissions.mayChange(level, pos, player)) return;
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof WallRun run) || !(level.getBlockEntity(pos) instanceof WallRunBlockEntity entity)) {
			return;
		}
		BlockState look = entity.disguise();
		level.setBlock(pos, look, Block.UPDATE_ALL);
		if (!player.getAbilities().instabuild) {
			Block.popResource(level, pos, new ItemStack(run.carrier()));
		}
		SoundType sound = run.carrier().defaultBlockState().getSoundType();
		level.playSound(null, pos, sound.getBreakSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8F);
	}

	// ------------------------------------------------------------------ shared by the run blocks

	/** A sleeve shows on a side the run comes out of into the open: joined there, to something not itself in a wall. */
	static boolean sleeve(WallRun run, BlockGetter level, BlockPos pos, Direction direction, BlockState neighbor) {
		return !(neighbor.getBlock() instanceof WallRun) && run.joins(level, pos, direction, neighbor);
	}

	static BlockState sleeves(WallRun run, BlockState state, BlockGetter level, BlockPos pos) {
		for (Map.Entry<Direction, net.minecraft.world.level.block.state.properties.BooleanProperty> entry : PipeBlock.PROPERTY_BY_DIRECTION.entrySet()) {
			Direction d = entry.getKey();
			state = state.setValue(entry.getValue(), sleeve(run, level, pos, d, level.getBlockState(pos.relative(d))));
		}
		return state;
	}

	/** As hard to break as the wall it is in. */
	static float destroyProgress(float own, Player player, BlockGetter level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof WallRunBlockEntity entity && entity.hasDisguise()) {
			return entity.disguise().getDestroyProgress(player, level, pos);
		}
		return own;
	}

	/** Broken: the pipe (its own loot), and the wall's block if the tool would have dropped it. */
	static List<ItemStack> drops(List<ItemStack> own, LootParams.Builder builder) {
		BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (!(entity instanceof WallRunBlockEntity run) || !run.hasDisguise()) {
			return own;
		}
		BlockState look = run.disguise();
		Object tool = builder.getOptionalParameter(LootContextParams.TOOL);
		if (look.requiresCorrectToolForDrops() && !(tool instanceof ItemStack stack && stack.isCorrectToolForDrops(look))) {
			return own;
		}
		List<ItemStack> all = new ArrayList<>(own);
		all.addAll(look.getDrops(builder));
		return all;
	}

	/** Pick-block gives the wall's block. */
	static ItemStack clone(LevelReader level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof WallRunBlockEntity entity ? new ItemStack(entity.disguise().getBlock()) : ItemStack.EMPTY;
	}

	/** Placed without a look (a blueprint build): it takes the look of the wall round it a tick later. */
	static void placed(Level level, BlockPos pos, Block block) {
		if (!level.isClientSide()) {
			level.scheduleTick(pos, block, 1);
		}
	}

	static void tick(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof WallRunBlockEntity entity) || entity.hasDisguise()) {
			return;
		}
		entity.setDisguise(lookAround(level, pos));
	}

	/** The commonest made block round {@code pos} (sideways first), or smooth stone. */
	public static BlockState lookAround(Level level, BlockPos pos) {
		Map<BlockState, Integer> counts = new HashMap<>();
		for (Direction d : Direction.values()) {
			BlockPos q = pos.relative(d);
			BlockState s = level.getBlockState(q);
			if (isWall(s, level, q)) {
				counts.merge(s, d.getAxis().isHorizontal() ? 3 : 2, Integer::sum);
			}
		}
		return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(FALLBACK);
	}
}

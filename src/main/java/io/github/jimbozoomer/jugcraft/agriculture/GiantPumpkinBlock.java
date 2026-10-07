package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A giant pumpkin: a cube of {@link #SIZE} blocks a side (1, then 2, then 3) grown from a
 * {@link GiantPumpkinVineBlock}. Every block of it is this block; {@link #PART} says where in the cube it
 * is, counted from the lowest north-west corner (the master, part 0, which alone has the
 * {@link GiantPumpkinBlockEntity} and ticks). Full grown, each side can be carved as one 48x48 face, and a torch
 * inside lights every block of it ({@link #LIGHT}); a soul torch lights it blue, at most a soul torch's light.
 *
 * <p>It is one prop, like a large machine: breaking any block breaks the whole pumpkin and drops it as one Giant
 * Pumpkin item that keeps its size, weight, carving and torch ({@link GiantPumpkinData}); placing the item puts the
 * whole cube back, away from the player and centred on where they aimed. Cut from its vine it no longer grows. An axe
 * chops it up instead, for its pumpkins and, full grown, seeds (loot table {@code gameplay/chop_giant_pumpkin}).
 */
public class GiantPumpkinBlock extends BaseEntityBlock implements BonemealableBlock {
	public static final int MAX_SIZE = 3;
	public static final IntegerProperty SIZE = IntegerProperty.create("size", 1, MAX_SIZE);
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, MAX_SIZE * MAX_SIZE * MAX_SIZE - 1);
	/** The light every block gives: the carving's glow while a torch is inside, else 0. */
	public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
	/** What chopping a giant pumpkin with an axe gives (by its size). */
	public static final ResourceKey<LootTable> CHOP_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("gameplay/chop_giant_pumpkin"));

	public GiantPumpkinBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(SIZE, 1).setValue(PART, 0).setValue(LIGHT, 0));
	}

	public static int light(BlockState state) {
		return state.getValue(LIGHT);
	}

	/** The part number of the block at {@code (dx, dy, dz)} from the master of a pumpkin {@code size} blocks wide. */
	public static int part(int size, int dx, int dy, int dz) {
		return dx + size * (dz + size * dy);
	}

	/** The offset of a part from its master: {@code {dx, dy, dz}}. */
	public static int[] offset(int size, int part) {
		return new int[] {part % size, part / (size * size), (part / size) % size};
	}

	/** Where the master of the giant pumpkin this block belongs to is. */
	public static BlockPos masterPos(BlockPos pos, BlockState state) {
		int[] offset = offset(state.getValue(SIZE), state.getValue(PART));
		return pos.offset(-offset[0], -offset[1], -offset[2]);
	}

	public static @Nullable GiantPumpkinBlockEntity master(Level level, BlockPos pos, BlockState state) {
		return level.getBlockEntity(masterPos(pos, state)) instanceof GiantPumpkinBlockEntity master ? master : null;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PART) == 0 ? new GiantPumpkinBlockEntity(pos, state) : null;
	}

	/** Only the master ticks: it grows the pumpkin, then puts on weight. */
	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(PART) == 0;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof GiantPumpkinBlockEntity master) {
			master.tick(level, random);
		}
	}

	/** The blocks a pumpkin {@code size} wide placed at {@code pos} by a player looking {@code facing} fills, master first. */
	public static List<BlockPos> footprint(BlockPos pos, Direction facing, int size) {
		Direction side = facing.getClockWise();
		int from = size == 3 ? -1 : 0;
		int minX = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		for (int k = 0; k < size; k++) {
			for (int j = from; j < from + size; j++) {
				BlockPos at = pos.relative(facing, k).relative(side, j);
				minX = Math.min(minX, at.getX());
				minZ = Math.min(minZ, at.getZ());
			}
		}
		List<BlockPos> blocks = new ArrayList<>();
		for (int part = 0; part < size * size * size; part++) {
			int[] offset = offset(size, part);
			blocks.add(new BlockPos(minX + offset[0], pos.getY() + offset[1], minZ + offset[2]));
		}
		return blocks;
	}

	/** Placing a Giant Pumpkin item: every block of its cube must be free (or replaceable, like grass). */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		int size = context.getItemInHand().getOrDefault(JugcraftAgriculture.GIANT_PUMPKIN, GiantPumpkinData.FULL_GROWN).size();
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		List<BlockPos> blocks = footprint(clicked, context.getHorizontalDirection(), size);
		for (BlockPos pos : blocks) {
			if (!pos.equals(clicked) && (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
					|| !level.getBlockState(pos).canBeReplaced(BlockPlaceContext.at(context, pos, Direction.UP)))) {
				return null;
			}
		}
		return defaultBlockState().setValue(SIZE, size).setValue(PART, blocks.indexOf(clicked));
	}

	/** Fills the rest of the cube, then gives the master what the item carried and lights it if a torch is inside. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		int size = state.getValue(SIZE);
		BlockPos master = masterPos(pos, state);
		for (int part = 0; part < size * size * size; part++) {
			int[] offset = offset(size, part);
			BlockPos partPos = master.offset(offset[0], offset[1], offset[2]);
			if (!partPos.equals(pos)) {
				level.setBlock(partPos, state.setValue(PART, part), Block.UPDATE_ALL);
			}
		}
		if (level.getBlockEntity(master) instanceof GiantPumpkinBlockEntity pumpkin) {
			pumpkin.applyComponentsFromItemStack(stack);
			pumpkin.plant(master, placer != null ? placer.getDirection() : Direction.NORTH);
			pumpkin.setLit(pumpkin.lit());
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return; // Grown or lit, not removed.
		}
		int size = state.getValue(SIZE);
		BlockPos master = masterPos(pos, state);
		BlockState masterState = level.getBlockState(master);
		if (state.getValue(PART) != 0 && masterState.is(this) && masterState.getValue(SIZE) == size && masterState.getValue(PART) == 0) {
			// Only the master knows the pumpkin: break it with drops (one Giant Pumpkin item), which clears the rest.
			level.destroyBlock(master, true);
			return;
		}
		// The master was broken and dropped the pumpkin; remove the rest of it without drops.
		for (int part = 0; part < size * size * size; part++) {
			int[] offset = offset(size, part);
			BlockPos partPos = master.offset(offset[0], offset[1], offset[2]);
			BlockState other = level.getBlockState(partPos);
			if (!partPos.equals(pos) && other.is(this) && other.getValue(SIZE) == size && other.getValue(PART) == part) {
				level.removeBlock(partPos, false);
			}
		}
	}

	/** In creative, breaking any block takes the whole pumpkin away without dropping it. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.getAbilities().instabuild && state.getValue(PART) != 0) {
			BlockPos master = masterPos(pos, state);
			if (level.getBlockState(master).is(this)) {
				level.removeBlock(master, false);
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/**
	 * A torch or soul torch lights a full-grown, carved giant pumpkin (every block of it then glows); an axe chops it up;
	 * bone meal says so when the pumpkin has no room left to grow.
	 */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		GiantPumpkinBlockEntity master = master(level, pos, state);
		if (master != null && stack.is(ItemTags.AXES)) {
			if (level instanceof ServerLevel server) {
				if (!player.mayBuild() || !level.mayInteract(player, pos)) {
					return InteractionResult.FAIL;
				}
				chop(server, masterPos(pos, state), player, stack);
				stack.hurtAndBreak(1, player, hand);
			}
			return InteractionResult.SUCCESS;
		}
		if (master != null && stack.is(Items.BONE_MEAL) && master.attached(level) && master.stuck(level)) {
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.sendOverlayMessage(Component.translatable("message.jugcraft.giant_pumpkin.no_room"));
			}
			return InteractionResult.SUCCESS;
		}
		if (!CarvedPumpkinBlock.isTorch(stack) || master == null || master.lit() || master.glow() == 0) {
			return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			master.setLit(true, stack.is(Items.SOUL_TORCH));
			stack.consume(1, player);
			level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Chops up the pumpkin whose master is at {@code master}: its pumpkins and seeds (and the torch, if one was inside)
	 * fall where it stood, and it is gone.
	 */
	public static void chop(ServerLevel level, BlockPos master, Player player, ItemStack tool) {
		BlockState state = level.getBlockState(master);
		if (!(level.getBlockEntity(master) instanceof GiantPumpkinBlockEntity pumpkin)) {
			return;
		}
		int size = state.getValue(SIZE);
		Vec3 middle = Vec3.atLowerCornerOf(master).add(size / 2.0, size / 2.0, size / 2.0);
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, middle)
				.withParameter(LootContextParams.TOOL, tool).withParameter(LootContextParams.BLOCK_STATE, state)
				.withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.BLOCK);
		List<ItemStack> drops = new ArrayList<>(level.getServer().reloadableRegistries().getLootTable(CHOP_LOOT).getRandomItems(params));
		if (pumpkin.lit()) {
			drops.add(CarvedPumpkinBlock.torch(pumpkin.soul()));
		}
		level.removeBlock(master, false);
		for (ItemStack drop : drops) {
			Containers.dropItemStack(level, middle.x, master.getY() + 0.5, middle.z, drop);
		}
		level.playSound(null, BlockPos.containing(middle), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
		level.gameEvent(player, GameEvent.BLOCK_DESTROY, master);
	}

	/** An empty hand takes the torch (or soul torch) back out. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		GiantPumpkinBlockEntity master = master(level, pos, state);
		if (master == null || !master.lit()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			ItemStack torch = CarvedPumpkinBlock.torch(master.soul());
			master.setLit(false, false);
			if (!player.getInventory().add(torch)) {
				Block.popResourceFromFace(level, pos, hit.getDirection(), torch);
			}
			level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Bone meal feeds a giant pumpkin its vine still holds; once cut from the vine, carved, at its heaviest or out of
	 * room to grow, it has no effect.
	 */
	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return level.getBlockEntity(masterPos(pos, state)) instanceof GiantPumpkinBlockEntity master && master.canGrow() && master.attached(level)
				&& !master.stuck(level);
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		GiantPumpkinBlockEntity master = master(level, pos, state);
		if (master != null) {
			master.feed(level, GiantPumpkinBlockEntity.BONE_MEAL_POINTS, random);
		}
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		ItemStack stack = new ItemStack(this);
		if (level.getBlockEntity(masterPos(pos, state)) instanceof GiantPumpkinBlockEntity master) {
			GiantPumpkinData data = master.data();
			stack.set(JugcraftAgriculture.GIANT_PUMPKIN, includeData ? data : new GiantPumpkinData(data.size(), 0,
					GiantPumpkinBlockEntity.START_WEIGHT, Optional.empty(), false, Map.of(), Optional.empty(), "", false));
		}
		return stack;
	}

	/** Turning one block of a pumpkin alone would tear it apart, so structures leave it as it is. */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SIZE, PART, LIGHT);
	}

	/** Under the Harvest Moon a lit giant pumpkin throws off sparks from its top. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (HarvestMoon.clientActive && state.getValue(LIGHT) > 0 && random.nextInt(6) == 0) {
			level.addParticle(random.nextBoolean() ? ParticleTypes.SMALL_FLAME : ParticleTypes.WAX_ON, pos.getX() + random.nextDouble(),
					pos.getY() + 1.0, pos.getZ() + random.nextDouble(), 0.0, 0.04, 0.0);
		}
	}
}

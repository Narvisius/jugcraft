package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Harvest Effigy (Halloween decorations batch 20, Pumpkin Night): a giant wicker-and-straw man three blocks tall, arms
 * out on a cross-pole, sheaves of corn at his feet, in a tattered cloak (dye it). Like the Scarecrow he wears any carved
 * pumpkin as his head. At night, flint and steel, a fire charge or a torch lights him: he burns for
 * {@value #BURN_TICKS} ticks in a blaze that climbs to his head ({@link #LIT}, drawn by the client: no fire blocks are
 * placed and nothing catches), crows near scatter, and every player near gets Harvest Cheer once a night
 * ({@link HarvestCheer}). Rain puts him out. Burnt through, he collapses into Effigy Ashes and drops his head.
 */
public class HarvestEffigyBlock extends MultiDecorationBlock implements EntityBlock {
	public static final int BURN_TICKS = 600;
	public static final int CHEER_RADIUS = 16;
	public static final int CROW_RADIUS = 24;
	public static final int CHECK_TICKS = 20;
	public static final int LIGHT = 15;
	private static final int[][] CELLS = {{0, 0}, {0, 1}, {0, 2}};
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, CELLS.length - 1);
	public static final EnumProperty<DyeColor> CLOAK = EnumProperty.create("cloak", DyeColor.class);
	private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

	public HarvestEffigyBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(LIT, false).setValue(CLOAK, DyeColor.GREEN));
	}

	@Override
	public int[][] cells() {
		return CELLS;
	}

	@Override
	public IntegerProperty partProperty() {
		return PART;
	}

	/** Burning, every part shines. */
	public static int light(BlockState state) {
		return state.getValue(LIT) ? LIGHT : 0;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Sets every part of the effigy with its first block at {@code master} to {@code changed}'s light and cloak. */
	static void setAll(Level level, BlockPos master, BlockState changed) {
		if (!(changed.getBlock() instanceof HarvestEffigyBlock effigy)) {
			return;
		}
		Direction facing = changed.getValue(FACING);
		for (int part = 0; part < CELLS.length; part++) {
			BlockPos at = effigy.partPos(master, facing, part);
			BlockState there = level.getBlockState(at);
			if (there.is(effigy) && effigy.part(there) == part) {
				level.setBlock(at, there.setValue(LIT, changed.getValue(LIT)).setValue(CLOAK, changed.getValue(CLOAK)), Block.UPDATE_ALL);
			}
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		BlockPos master = masterPos(pos, state);
		if (!(level.getBlockEntity(master) instanceof HarvestEffigyBlockEntity effigy)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		boolean burning = state.getValue(LIT);
		if (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE) || stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH)) {
			if (burning) {
				return InteractionResult.PASS;
			}
			if (level instanceof ServerLevel server) {
				String refused = refusal(MourningAngelBlock.night(level), rainedOn(level, master, state));
				if (refused != null) {
					player.sendOverlayMessage(Component.translatable(refused));
					return InteractionResult.FAIL;
				}
				if (!level.mayInteract(player, pos)) {
					return InteractionResult.FAIL;
				}
				if (stack.is(Items.FLINT_AND_STEEL)) {
					stack.hurtAndBreak(1, player, hand);
				} else if (stack.is(Items.FIRE_CHARGE)) {
					stack.consume(1, player);
				}
				ignite(server, master);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			}
			return InteractionResult.SUCCESS;
		}
		if (burning) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (stack.is(JugcraftAgriculture.SCARECROW_HEADS)) {
			if (!level.isClientSide()) {
				ItemStack old = effigy.head();
				effigy.setHead(stack.split(1));
				if (!old.isEmpty()) {
					player.getInventory().placeItemBackInInventory(old, Prediction.SERVER_ONLY);
				}
				level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 0.8F, 0.9F);
			}
			return InteractionResult.SUCCESS;
		}
		DyeColor dye = ScarecrowBlock.dyeColor(stack);
		if (dye != null && dye != state.getValue(CLOAK)) {
			if (!level.isClientSide()) {
				setAll(level, master, level.getBlockState(master).setValue(CLOAK, dye));
				stack.consume(1, player);
				level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	/** An empty hand takes his head back (not while he burns). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos master = masterPos(pos, state);
		if (state.getValue(LIT) || !(level.getBlockEntity(master) instanceof HarvestEffigyBlockEntity effigy) || effigy.head().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			player.getInventory().placeItemBackInInventory(effigy.head(), Prediction.SERVER_ONLY);
			effigy.setHead(ItemStack.EMPTY);
		}
		return InteractionResult.SUCCESS;
	}

	/** Why he won't catch now (the message to show): by day, or with rain on him; null if he will. */
	public static @Nullable String refusal(boolean night, boolean wet) {
		return !night ? "message.jugcraft.effigy.day" : wet ? "message.jugcraft.effigy.wet" : null;
	}

	/** Whether rain falls on the effigy with its first block at {@code master} (on its head). */
	static boolean rainedOn(Level level, BlockPos master, BlockState state) {
		return level.isRainingAt(master.above(CELLS.length));
	}

	/** Sets him alight now: the blaze, a roar, and the crows near scatter. */
	public static void ignite(ServerLevel level, BlockPos master) {
		BlockState state = level.getBlockState(master);
		if (!(level.getBlockEntity(master) instanceof HarvestEffigyBlockEntity effigy) || state.getValue(LIT)) {
			return;
		}
		effigy.ignite(level.getGameTime());
		setAll(level, master, state.setValue(LIT, true));
		level.playSound(null, master.above(), JugcraftAgriculture.EFFIGY_BURN, SoundSource.BLOCKS, 2.0F, 0.8F);
		level.playSound(null, master, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.7F);
		for (Crow crow : level.getEntitiesOfClass(Crow.class, new AABB(master).inflate(CROW_RADIUS))) {
			crow.flee(level, Vec3.atCenterOf(master));
		}
	}

	/** Puts him out (rain): he stands, scorched no further, to be lit again. */
	public static void extinguish(ServerLevel level, BlockPos master) {
		BlockState state = level.getBlockState(master);
		if (state.getBlock() instanceof HarvestEffigyBlock && state.getValue(LIT)) {
			setAll(level, master, state.setValue(LIT, false));
			level.playSound(null, master.above(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
		}
	}

	/** Burnt through: his head drops, and he falls into Effigy Ashes. */
	static void collapse(ServerLevel level, BlockPos master) {
		if (level.getBlockEntity(master) instanceof HarvestEffigyBlockEntity effigy && !effigy.head().isEmpty()) {
			Block.popResource(level, master.above(CELLS.length - 1), effigy.head());
			effigy.setHead(ItemStack.EMPTY);
		}
		level.setBlock(master, JugcraftAgriculture.block(JugcraftAgriculture.EFFIGY_ASHES).defaultBlockState(), Block.UPDATE_ALL);
		level.playSound(null, master, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, master.getX() + 0.5, master.getY() + 1.0, master.getZ() + 0.5, 30, 0.4, 0.8, 0.4, 0.02);
	}

	/** Burning, sparks spiral up and smoke rolls off the top; he crackles. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		for (int i = 0; i < 3; i++) {
			level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + random.nextDouble(),
					pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.03, 0.0);
		}
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
		}
		if (part(state) == CELLS.length - 1) {
			level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 0.0, 0.08, 0.0);
			if (random.nextInt(8) == 0) {
				level.playLocalSound(pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.5F, 0.8F + random.nextFloat() * 0.3F, false);
			}
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return part(state) == MASTER ? new HarvestEffigyBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.HARVEST_EFFIGY_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((HarvestEffigyBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos, tickState);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(CLOAK);
	}
}

package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Horned Skull Cauldron (Halloween decorations batch 17, tools/decor17.py): a black-iron pot on clawed feet with a
 * ram's skull on its front. It holds water or up to {@value #LEVELS} bottles of one potion ({@link #LEVEL}; what it is,
 * in its {@link HornedSkullCauldronBlockEntity}): a water bucket fills it with water, a bucket takes a full pot of water
 * back, a potion goes in if it is empty or holds that potion, and a glass bottle draws one level back out as it went in.
 * Nothing is gained or lost.
 *
 * <p>Over a heat source ({@link #HEATED}, the {@code jugcraft:heat_sources} block tag below it, lit) it bubbles, and its
 * fumes rise (drawn by the client). Stirred there with the Brew Ladle, a potion with lasting effects <b>wafts</b>: up to
 * {@value #WAFT_PLAYERS} players within {@value #WAFT_RANGE} blocks each get its lasting effects for
 * {@value #WAFT_FRACTION} of their duration, for one level. Up to {@value #FLOATERS} things that float (the item tag
 * {@code jugcraft:cauldron_floaters}) can be dropped in; an empty hand or the ladle fishes the last one out. Light: a
 * potion gives {@value #POTION_LIGHT} plus one a level, {@value #HEAT_LIGHT} more while heated. Changing it needs build
 * rights.
 */
public class HornedSkullCauldronBlock extends BaseEntityBlock {
	public static final int LEVELS = 3;
	public static final int FLOATERS = 3;
	public static final int POTION_LIGHT = 6;
	public static final int HEAT_LIGHT = 2;
	public static final int WAFT_PLAYERS = 4;
	public static final double WAFT_RANGE = 3.0;
	public static final double WAFT_FRACTION = 0.25;
	/** An effect lasting no longer than this (ticks) counts as instant, and isn't wafted. */
	public static final int INSTANT_TICKS = 20;
	/** The brew's surface for each level, in pixels (the inside's floor is at 8.5). */
	public static final float[] SURFACE = {8.5F, 9.6F, 10.8F, 12.0F};
	public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, LEVELS);
	/** Whether it holds a potion (not water): it glows. */
	public static final BooleanProperty POTION = BooleanProperty.create("potion");
	public static final BooleanProperty HEATED = BooleanProperty.create("heated");
	public static final TagKey<Item> FLOATS = TagKey.create(Registries.ITEM, Jugcraft.id("cauldron_floaters"));
	/** Which way the skull on its front looks: at whoever placed it. */
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final VoxelShape SHAPE = Shapes.or(Block.box(1.4, 2.4, 1.4, 14.6, 13.6, 14.6), Block.box(2.2, 0.0, 2.2, 13.8, 2.4, 13.8));

	public HornedSkullCauldronBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LEVEL, 0).setValue(POTION, false).setValue(HEATED, false));
	}

	public static int light(BlockState state) {
		if (!state.getValue(POTION) || state.getValue(LEVEL) == 0) {
			return 0;
		}
		return POTION_LIGHT + state.getValue(LEVEL) + (state.getValue(HEATED) ? HEAT_LIGHT : 0);
	}

	/** Whether {@code below} heats a pot over it: a heat source, lit if it can be. */
	public static boolean heats(BlockState below) {
		return below.is(CookingPotBlockEntity.HEAT_SOURCES) && (!below.hasProperty(BlockStateProperties.LIT) || below.getValue(BlockStateProperties.LIT));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HornedSkullCauldronBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(HEATED, heats(context.getLevel().getBlockState(context.getClickedPos().below())));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == Direction.DOWN ? state.setValue(HEATED, heats(neighbor)) : state;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof HornedSkullCauldronBlockEntity pot) || !player.mayBuild()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		int filled = state.getValue(LEVEL);
		if (stack.is(Items.WATER_BUCKET)) {
			if (filled > 0 && !pot.isWater() || filled == LEVELS) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!level.isClientSide()) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
				fill(level, pos, state, pot, new PotionContents(Potions.WATER), LEVELS, player);
				level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.BUCKET)) {
			if (filled != LEVELS || !pot.isWater()) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!level.isClientSide()) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET)));
				fill(level, pos, state, pot, PotionContents.EMPTY, 0, player);
				level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.POTION)) {
			PotionContents potion = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
			if (filled == LEVELS || filled > 0 && !potion.equals(pot.contents())) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!level.isClientSide()) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
				fill(level, pos, state, pot, potion, filled + 1, player);
				level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.GLASS_BOTTLE)) {
			if (filled == 0) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!level.isClientSide()) {
				ItemStack drawn = new ItemStack(Items.POTION);
				drawn.set(DataComponents.POTION_CONTENTS, pot.contents());
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, drawn));
				fill(level, pos, state, pot, filled > 1 ? pot.contents() : PotionContents.EMPTY, filled - 1, player);
				level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(JugcraftAgriculture.item(JugcraftAgriculture.BREW_LADLE))) {
			if (!level.isClientSide()) {
				stir(level, pos, state, pot, player);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(FLOATS) && filled > 0) {
			if (pot.floating().size() >= FLOATERS) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!level.isClientSide()) {
				pot.addFloating(stack);
				stack.consume(1, player);
				level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.4F);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	/** An empty hand fishes out the last thing dropped in. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof HornedSkullCauldronBlockEntity pot) || pot.floating().isEmpty()
				|| !player.mayBuild()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			giveBack(player, pot.takeFloating());
			level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.4F, 1.8F);
		}
		return InteractionResult.SUCCESS;
	}

	private static void giveBack(Player player, ItemStack stack) {
		if (!stack.isEmpty() && !player.getInventory().add(stack)) {
			player.spawnAtLocation((ServerLevel) player.level(), stack);
		}
	}

	private static void fill(Level level, BlockPos pos, BlockState state, HornedSkullCauldronBlockEntity pot, PotionContents contents, int filled,
			Player player) {
		pot.setContents(filled == 0 ? PotionContents.EMPTY : contents);
		boolean potion = filled > 0 && !contents.is(Potions.WATER);
		level.setBlock(pos, state.setValue(LEVEL, filled).setValue(POTION, potion), Block.UPDATE_ALL);
		level.gameEvent(player, filled > state.getValue(LEVEL) ? GameEvent.FLUID_PLACE : GameEvent.FLUID_PICKUP, pos);
	}

	/**
	 * The Brew Ladle: over heat, a potion with lasting effects wafts onto those near (one level used); otherwise it fishes
	 * out the last thing floating, or only stirs.
	 */
	private static void stir(Level level, BlockPos pos, BlockState state, HornedSkullCauldronBlockEntity pot, Player stirrer) {
		ServerLevel server = (ServerLevel) level;
		int filled = state.getValue(LEVEL);
		Vec3 centre = Vec3.atCenterOf(pos).add(0.0, 0.3, 0.0);
		if (filled > 0 && state.getValue(HEATED) && state.getValue(POTION)) {
			List<MobEffectInstance> effects = wafted(pot.contents());
			if (!effects.isEmpty()) {
				for (Player player : nearest(level, pos)) {
					for (MobEffectInstance effect : effects) {
						player.addEffect(new MobEffectInstance(effect), stirrer);
					}
				}
				fill(level, pos, state, pot, pot.contents(), filled - 1, stirrer);
				server.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | pot.color()), centre.x, centre.y + 0.4,
						centre.z, 40, 1.2, 0.4, 1.2, 0.02);
				server.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 0.7F);
				return;
			}
		}
		if (!pot.floating().isEmpty()) {
			giveBack(stirrer, pot.takeFloating());
			server.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.4F, 1.8F);
			return;
		}
		if (filled > 0) {
			server.sendParticles(ParticleTypes.BUBBLE_POP, centre.x, pos.getY() + SURFACE[filled] / 16, centre.z, 8, 0.25, 0.02, 0.25, 0.02);
			server.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.8F, 0.6F);
		}
	}

	/** The lasting effects a wafted potion gives: each at {@value #WAFT_FRACTION} of its duration. */
	public static List<MobEffectInstance> wafted(PotionContents contents) {
		List<MobEffectInstance> out = new ArrayList<>();
		for (MobEffectInstance effect : contents.getAllEffects()) {
			if (effect.getDuration() > INSTANT_TICKS) {
				out.add(new MobEffectInstance(effect.getEffect(), Math.max(1, (int) (effect.getDuration() * WAFT_FRACTION)), effect.getAmplifier(),
						effect.isAmbient(), effect.isVisible(), effect.showIcon()));
			}
		}
		return out;
	}

	/** Up to {@value #WAFT_PLAYERS} players within {@value #WAFT_RANGE} blocks of the pot, nearest first. */
	public static List<Player> nearest(Level level, BlockPos pos) {
		Vec3 centre = Vec3.atCenterOf(pos);
		return level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(WAFT_RANGE),
						p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(centre) <= WAFT_RANGE * WAFT_RANGE).stream()
				.sorted(Comparator.comparingDouble(p -> p.distanceToSqr(centre))).limit(WAFT_PLAYERS).toList();
	}

	/** Over heat, its brew heaves and pops, and now and then spits a drop of brew over the rim. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		int filled = state.getValue(LEVEL);
		if (filled == 0 || !state.getValue(HEATED)) {
			return;
		}
		double y = pos.getY() + SURFACE[filled] / 16;
		double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
		double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
		level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0.0, 0.02, 0.0);
		if (random.nextInt(14) == 0 && level.getBlockEntity(pos) instanceof HornedSkullCauldronBlockEntity pot) {
			level.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | pot.color()), x, y + 0.1, z,
					(random.nextDouble() - 0.5) * 0.2, 0.25, (random.nextDouble() - 0.5) * 0.2);
		}
		if (random.nextInt(10) == 0) {
			level.playLocalSound(x, y, z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.5F, 0.7F + random.nextFloat() * 0.4F, false);
		}
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LEVEL, POTION, HEATED);
	}
}

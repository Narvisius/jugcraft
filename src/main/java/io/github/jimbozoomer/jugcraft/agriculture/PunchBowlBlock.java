package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Witch's Brew Punch Bowl: a glass bowl on a stand. A berry (item tag {@link #INGREDIENTS}) brews
 * {@value #PER_BERRY} servings of glowing green punch, up to {@value #SERVINGS} ({@link #SERVINGS_LEFT}); a glass bottle
 * ladles out one Witch's Brew Punch. While there is punch in it, it glows (light {@value #LIGHT}) and dry-ice fog rolls
 * over its rim and down its sides.
 */
public class PunchBowlBlock extends Block {
	public static final int SERVINGS = 12;
	public static final int PER_BERRY = 2;
	public static final int LIGHT = 6;
	public static final IntegerProperty SERVINGS_LEFT = IntegerProperty.create("servings", 0, SERVINGS);
	public static final TagKey<Item> INGREDIENTS = TagKey.create(Registries.ITEM, Jugcraft.id("witchs_brew_ingredients"));
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 10.0, 15.0);

	public PunchBowlBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(SERVINGS_LEFT, 0));
	}

	public static int light(BlockState state) {
		return state.getValue(SERVINGS_LEFT) > 0 ? LIGHT : 0;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		int servings = state.getValue(SERVINGS_LEFT);
		if (stack.is(INGREDIENTS)) {
			if (servings >= SERVINGS) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!level.isClientSide()) {
				level.setBlock(pos, state.setValue(SERVINGS_LEFT, Math.min(SERVINGS, servings + PER_BERRY)), Block.UPDATE_ALL);
				level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.2F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
				stack.consume(1, player);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.GLASS_BOTTLE)) {
			if (servings <= 0) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!level.isClientSide()) {
				level.setBlock(pos, state.setValue(SERVINGS_LEFT, servings - 1), Block.UPDATE_ALL);
				level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
				ItemStack punch = new ItemStack(JugcraftAgriculture.item("witchs_brew_punch"));
				stack.consume(1, player);
				if (!player.getInventory().add(punch)) {
					Block.popResource(level, pos.above(), punch);
				}
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	/** Now and then a puff of dry-ice fog spilling over the rim and down the sides, or a bubble. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(SERVINGS_LEFT) <= 0) {
			return;
		}
		if (random.nextBoolean()) {
			double angle = random.nextDouble() * Math.PI * 2;
			double x = pos.getX() + 0.5 + Math.cos(angle) * 0.42;
			double z = pos.getZ() + 0.5 + Math.sin(angle) * 0.42;
			level.addParticle(JugcraftAgriculture.FOG, x, pos.getY() + 0.62, z, Math.cos(angle) * 0.015, -0.01, Math.sin(angle) * 0.015);
		}
		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.58,
					pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.0, 0.0);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SERVINGS_LEFT);
	}
}

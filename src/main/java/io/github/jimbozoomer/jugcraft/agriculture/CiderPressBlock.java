package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Cider Press: a slatted oak basket on a trough, under a beam with an iron screw, and a crank-turned grinder (the
 * scratter) on its side ({@link CiderPressBlockEntity} holds what is in it). Use it:
 * <ul>
 * <li>holding apples ({@code jugcraft:cider_apples}), to fill the grinder's hopper, one at a time;</li>
 * <li>with an empty hand while there are apples in the hopper, to turn the crank: an apple is ground into the basket;</li>
 * <li>with an empty hand once the hopper is empty, to turn the screw down on the ground apples: each turn presses out its
 * share of the juice into the trough, and the last knocks out the spent pomace;</li>
 * <li>holding a glass bottle, to draw a serving of juice (Sweet Cider);</li>
 * <li>sneaking with an empty hand, to see what is in it.</li>
 * </ul>
 * Comparators read how much juice is in the trough.
 */
public class CiderPressBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	/** What the grinder takes. */
	public static final TagKey<Item> APPLES = TagKey.create(Registries.ITEM, Jugcraft.id("cider_apples"));
	private static final String MESSAGES = "message.jugcraft.cider_press.";
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);

	public CiderPressBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CiderPressBlockEntity(pos, state);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		boolean apple = stack.is(APPLES);
		if (!apple && !stack.is(Items.GLASS_BOTTLE)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CiderPressBlockEntity press)) {
			return InteractionResult.SUCCESS;
		}
		if (apple) {
			if (press.hasRoom()) {
				press.addApple();
				stack.consume(1, player);
				level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 0.8F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			} else {
				player.sendOverlayMessage(Component.translatable(MESSAGES + (press.pressing() ? "pressing" : "full")));
			}
			return InteractionResult.SUCCESS;
		}
		if (press.juice() <= 0) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "no_juice"));
			return InteractionResult.SUCCESS;
		}
		press.draw();
		player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(JugcraftAgriculture.item("sweet_cider"))));
		level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
		return InteractionResult.SUCCESS;
	}

	/** An empty hand turns the crank, or the screw once the hopper is empty; sneaking, it shows what is in the press. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CiderPressBlockEntity press)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive() || press.apples() == 0 && press.pulp() == 0) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "status", press.apples(), press.pulp(), press.juice(),
					CiderPressBlockEntity.TROUGH));
			return InteractionResult.SUCCESS;
		}
		long now = level.getGameTime();
		if (!press.ready(now)) {
			return InteractionResult.SUCCESS;
		}
		ServerLevel server = (ServerLevel) level;
		if (press.apples() > 0 && !press.pressing()) {
			press.grind(now);
			level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6F, 1.3F);
			server.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.15, 0.05, 0.15, 0.05);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			return InteractionResult.SUCCESS;
		}
		if (press.juice() + press.nextRelease() > CiderPressBlockEntity.TROUGH) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "trough_full"));
			return InteractionResult.SUCCESS;
		}
		int pomace = press.turn(now);
		level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.7F, 0.6F);
		level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.4F, 1.4F);
		server.sendParticles(ParticleTypes.DRIPPING_HONEY, pos.getX() + 0.5, pos.getY() + 0.35, pos.getZ() + 0.5, 4, 0.2, 0.0, 0.2, 0.0);
		if (pomace > 0) {
			Block.popResource(level, pos.above(), new ItemStack(JugcraftAgriculture.item("apple_pomace"), pomace));
			player.sendOverlayMessage(Component.translatable(MESSAGES + "pressed_out", pomace));
		} else {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "turned", press.turns(), CiderPressBlockEntity.TURNS));
		}
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** How much juice is in the trough: 0 when dry, 15 when full. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		int juice = level.getBlockEntity(pos) instanceof CiderPressBlockEntity press ? press.juice() : 0;
		return juice == 0 ? 0 : 1 + (juice - 1) * 14 / (CiderPressBlockEntity.TROUGH - 1);
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
		builder.add(FACING);
	}
}

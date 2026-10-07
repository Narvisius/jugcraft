package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Bobbing for Apples Tub: a banded wooden tub of water. Drop apples in (up to {@value #MAX_APPLES}; they float),
 * then use it with an empty hand to duck for one: the server decides, one try in {@value #CHANCE} catches an apple
 * (into the bobber's inventory), and either way the water splashes for {@value #SPLASH_TICKS} ticks before anyone may
 * try again. It only gives back apples that players put in.
 */
public class BobbingTubBlock extends Block {
	public static final int MAX_APPLES = 4;
	public static final int CHANCE = 3;
	public static final int SPLASH_TICKS = 20;
	public static final IntegerProperty APPLES = IntegerProperty.create("apples", 0, MAX_APPLES);
	public static final BooleanProperty SPLASHING = BooleanProperty.create("splashing");
	private static final VoxelShape SHAPE = Shapes.or(Block.box(1.0, 0.0, 1.0, 15.0, 1.0, 15.0), Block.box(1.0, 1.0, 1.0, 15.0, 10.0, 2.0),
			Block.box(1.0, 1.0, 14.0, 15.0, 10.0, 15.0), Block.box(1.0, 1.0, 2.0, 2.0, 10.0, 14.0), Block.box(14.0, 1.0, 2.0, 15.0, 10.0, 14.0));

	public BobbingTubBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(APPLES, 0).setValue(SPLASHING, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** An apple goes in to float with the others, while there is room. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(Items.APPLE)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (state.getValue(APPLES) >= MAX_APPLES) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			stack.consume(1, player);
			level.setBlock(pos, state.setValue(APPLES, state.getValue(APPLES) + 1), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.4F, 1.4F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** Ducking for an apple: one try in {@link #CHANCE} catches one; then the tub splashes a moment. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (state.getValue(SPLASHING)) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		int apples = state.getValue(APPLES);
		if (apples == 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.bobbing_tub.empty"));
			return InteractionResult.SUCCESS;
		}
		boolean caught = server.getRandom().nextInt(CHANCE) == 0;
		if (caught) {
			ItemStack apple = new ItemStack(Items.APPLE);
			if (!player.getInventory().add(apple)) {
				Block.popResource(server, pos.above(), apple);
			}
		}
		server.setBlock(pos, state.setValue(APPLES, caught ? apples - 1 : apples).setValue(SPLASHING, true), Block.UPDATE_ALL);
		server.scheduleTick(pos, this, SPLASH_TICKS);
		server.playSound(null, pos, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.5F, caught ? 1.3F : 1.0F);
		server.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.65, pos.getZ() + 0.5, 12, 0.25, 0.05, 0.25, 0.1);
		server.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		player.sendOverlayMessage(Component.translatable(caught ? "message.jugcraft.bobbing_tub.caught" : "message.jugcraft.bobbing_tub.missed"));
		return InteractionResult.SUCCESS;
	}

	/** The water settles. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(SPLASHING)) {
			level.setBlock(pos, state.setValue(SPLASHING, false), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(APPLES, SPLASHING);
	}
}

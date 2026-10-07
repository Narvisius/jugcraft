package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A pumpkin carved by hand with the Carving Knife: any design on any of its four sides, kept in
 * {@link CarvedPumpkinBlockEntity} and drawn by the client. A torch inside lights it ({@link #LIT}); it
 * then gives {@link #GLOW} light, more the more is carved out ({@link PumpkinCarving#glow}). A soul torch
 * lights it instead with a cold blue flame ({@link #SOUL}), at most {@value #SOUL_LIGHT} like the soul torch
 * itself. Using it with an empty hand takes the torch back out.
 */
public class CarvedPumpkinBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, 15);
	/** Whether the torch inside is a soul torch (only while {@link #LIT}). */
	public static final BooleanProperty SOUL = BooleanProperty.create("soul");
	/** The most light a soul flame gives, a soul torch's own. */
	public static final int SOUL_LIGHT = 10;

	public CarvedPumpkinBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(GLOW, 0).setValue(SOUL, false));
	}

	/** The light a carved pumpkin gives: its glow while a torch is inside (a soul torch's at most), none without. */
	public static int light(BlockState state) {
		return state.getValue(LIT) ? flameLight(state.getValue(GLOW), state.getValue(SOUL)) : 0;
	}

	/** The light a carving that would glow {@code glow} gives with a torch inside: all of it, or a soul torch's at most. */
	public static int flameLight(int glow, boolean soul) {
		return soul ? Math.min(SOUL_LIGHT, glow) : glow;
	}

	/** Whether {@code stack} is a torch a carving can be lit with: a torch or a soul torch. */
	public static boolean isTorch(ItemStack stack) {
		return stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH);
	}

	/** The torch that comes back out of a carving. */
	public static ItemStack torch(boolean soul) {
		return new ItemStack(soul ? Items.SOUL_TORCH : Items.TORCH);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT, GLOW, SOUL);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		PumpkinCarving carving = context.getItemInHand().getOrDefault(JugcraftAgriculture.CARVING, PumpkinCarving.BLANK);
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(GLOW, carving.glow());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		// The design came with the item; the glow follows it, whatever the item claimed.
		if (level.getBlockEntity(pos) instanceof CarvedPumpkinBlockEntity pumpkin && state.getValue(GLOW) != pumpkin.carving().glow()) {
			level.setBlock(pos, state.setValue(GLOW, pumpkin.carving().glow()), Block.UPDATE_ALL);
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CarvedPumpkinBlockEntity(pos, state);
	}

	/**
	 * A torch (or a soul torch) lights a carved pumpkin that has something carved out to shine through. A spark (a Wisp in
	 * a Jar or Ectoplasm) wakes one with a face into a {@link Pumpkling}.
	 */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.is(Pumpkling.SPARKS) && player.mayBuild() && Pumpkling.wakeable(level, pos)) {
			if (level instanceof ServerLevel server) {
				Pumpkling.wake(server, pos, player, hand);
			}
			return InteractionResult.SUCCESS;
		}
		if (!isTorch(stack) || state.getValue(LIT) || state.getValue(GLOW) == 0) {
			// Only a truly empty hand takes the torch out; anything else (the knife, a block) does its own thing.
			return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(LIT, true).setValue(SOUL, stack.is(Items.SOUL_TORCH)), Block.UPDATE_ALL);
			stack.consume(1, player);
			level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** An empty hand takes the torch (or soul torch) back out. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!state.getValue(LIT)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(LIT, false).setValue(SOUL, false), Block.UPDATE_ALL);
			ItemStack torch = torch(state.getValue(SOUL));
			if (!player.getInventory().add(torch)) {
				Block.popResourceFromFace(level, pos, hit.getDirection(), torch);
			}
			level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8F, 1.2F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** Under the Harvest Moon a lit carving throws off sparks (blue ones from a soul flame). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (HarvestMoon.clientActive && state.getValue(LIT) && random.nextInt(3) == 0) {
			level.addParticle(random.nextBoolean() ? (state.getValue(SOUL) ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMALL_FLAME) : ParticleTypes.WAX_ON, pos.getX() + random.nextDouble(),
					pos.getY() + 0.9 + random.nextDouble() * 0.3, pos.getZ() + random.nextDouble(), 0.0, 0.03, 0.0);
		}
	}
}

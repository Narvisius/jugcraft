package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Coffin: two blocks long, dark planks lined in red velvet. Use it to lift the lid on a
 * {@value CoffinBlockEntity#SLOTS}-slot chest (kept in the head half, {@link CoffinBlockEntity}); the lid stays up
 * while anyone has it open. Sneak-use it with an empty hand to lie down in it like a bed: it sets your spawn and
 * passes the night under vanilla's bed rules. Where a bed would explode (the Nether, the End) the coffin only refuses.
 */
public class CoffinBlock extends AbstractBedBlock implements EntityBlock {
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	private static final Map<Direction.Axis, VoxelShape> SHAPES = Map.of(
			Direction.Axis.Z, Block.box(1.0, 0.0, 0.0, 15.0, 10.0, 16.0), Direction.Axis.X, Block.box(0.0, 0.0, 1.0, 16.0, 10.0, 15.0));

	public CoffinBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT).setValue(OCCUPIED, false)
				.setValue(OPEN, false));
	}

	@Override
	protected EnvironmentAttribute<BedRule> getBedEnvironmentAttribute() {
		return EnvironmentAttributes.BED_RULE;
	}

	/** Where a bed would blow up, the coffin just won't have you. */
	@Override
	protected InteractionResult destroyOnUse(BlockState state, Level level, BlockPos pos, Player player) {
		player.sendOverlayMessage(Component.translatable("message.jugcraft.coffin.restless"));
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void destroyOnLeave(Level level, BlockPos pos) {
	}

	/** The head half's position, from either half. */
	public static BlockPos head(BlockState state, BlockPos pos) {
		return state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.isSecondaryUseActive()) {
			return super.useWithoutItem(state, level, pos, player, hit);
		}
		if (!level.isClientSide() && level.getBlockEntity(head(state, pos)) instanceof CoffinBlockEntity coffin) {
			player.openMenu(coffin);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING).getAxis());
	}

	/** Re-checks who still has the lid up (scheduled by the openers' counter). */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof CoffinBlockEntity coffin) {
			coffin.recheckOpen();
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PART) == BedPart.HEAD ? new CoffinBlockEntity(pos, state) : null;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(OPEN);
	}
}

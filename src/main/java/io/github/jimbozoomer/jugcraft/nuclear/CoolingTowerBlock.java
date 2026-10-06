package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Cooling Tower Controller: the valve pit in the basin of a cooling tower (docs/features/nuclear.md, "Cooling
 * towers"). It is part of each cooling tower blueprint and only works inside a finished tower of its type. A condenser
 * whose Circulating Water Conduit reaches it sends its warm water up the tower instead of back to the lake, so it only
 * needs make-up water; the tower then steams.
 */
public class CoolingTowerBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Tower> TOWER = EnumProperty.create("tower", Tower.class);

	/**
	 * The tower kinds, matching the cooling tower blueprints (tools/nuclear_plants.py): the blueprint the controller
	 * checks its tower against, how much of a standard unit's heat it can take, and where its plume comes out (blocks
	 * above the controller, the plume's radius, and the fans' offsets along the tower for a mechanical bank).
	 */
	public enum Tower implements StringRepresentable {
		NATURAL("natural", "ct_natural", 1.0, 149, 28, new int[] {0}),
		LARGE("large", "ct_natural_large", 0.9, 174, 33, new int[] {0}),
		MECHANICAL("mechanical", "ct_mechanical", 0.4, 20, 5, new int[] {-40, -24, -8, 8, 24, 40});

		public final String name;
		public final String blueprint;
		public final double capacity;
		public final int plumeHeight;
		public final int plumeRadius;
		public final int[] fans;

		Tower(String name, String blueprint, double capacity, int plumeHeight, int plumeRadius, int[] fans) {
			this.name = name;
			this.blueprint = blueprint;
			this.capacity = capacity;
			this.plumeHeight = plumeHeight;
			this.plumeRadius = plumeRadius;
			this.fans = fans;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public CoolingTowerBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(TOWER, Tower.NATURAL));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, TOWER);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CoolingTowerBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, JugcraftNuclear.COOLING_TOWER_ENTITY, level instanceof ServerLevel
				? (l, pos, s, tower) -> tower.serverTick((ServerLevel) l)
				: (l, pos, s, tower) -> tower.clientTick(l));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof CoolingTowerBlockEntity tower) {
			player.sendOverlayMessage(tower.formed()
					? Component.translatable("message.jugcraft.nuclear.tower_status", Math.round(tower.load() * 100), String.format("%.1f", state.getValue(TOWER).capacity))
					: Component.translatable("message.jugcraft.nuclear.tower_incomplete"));
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}

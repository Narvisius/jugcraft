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
		//          name          blueprint            cap  plume r   fans                               fill r     fan
		NATURAL("natural", "ct_natural", 1.0, 150, 28, new int[] {0}, 12, 47, 0),
		LARGE("large", "ct_natural_large", 0.9, 175, 33, new int[] {0}, 12, 57, 0),
		MECHANICAL("mechanical", "ct_mechanical", 0.4, 21, 5, new int[] {-40, -24, -8, 8, 24, 40}, 10, 6, 14);

		public final String name;
		public final String blueprint;
		public final double capacity;
		/** Blocks from the controller (under the basin floor) up to the top of the shell or the fan stacks. */
		public final int plumeHeight;
		public final int plumeRadius;
		/** Along the bank: each cell's offset (a natural tower is one "cell" at 0). */
		public final int[] fans;
		/** The underside of the fill above the controller, and how far it spreads (radius, or a mechanical cell's half width). */
		public final int fillHeight;
		public final int fillRadius;
		/** A mechanical cell's fan, above the controller (0: no fans). */
		public final int fanHeight;

		Tower(String name, String blueprint, double capacity, int plumeHeight, int plumeRadius, int[] fans, int fillHeight, int fillRadius,
				int fanHeight) {
			this.name = name;
			this.blueprint = blueprint;
			this.capacity = capacity;
			this.plumeHeight = plumeHeight;
			this.plumeRadius = plumeRadius;
			this.fans = fans;
			this.fillHeight = fillHeight;
			this.fillRadius = fillRadius;
			this.fanHeight = fanHeight;
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

	/** Turns with a blueprint (or a structure) like any facing block. */
	@Override
	protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
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

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
 * The Water Works Controller (towers and pump houses build, 19 October 2026): the anchor of a pump house built as a
 * compiled site, under the grass where its buried lines leave for the plant. The lake intake's (make-up and service
 * water: WaterWorksBlockEntity) and the circulating water pump house's (beside the plant's conduit trench, whose
 * condensers count its running pumps). It is part of the medium plant's blueprint; clicking it tells its state.
 */
public class WaterWorksBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Works> WORKS = EnumProperty.create("works", Works.class);

	/** Which pump house: its site and how many pumps it has. */
	public enum Works implements StringRepresentable {
		INTAKE("intake", "lake_intake", 6),
		CIRC("circ", "cw_pump_house", 4);

		public final String name;
		public final String site;
		public final int pumps;

		Works(String name, String site, int pumps) {
			this.name = name;
			this.site = site;
			this.pumps = pumps;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public WaterWorksBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(WORKS, Works.INTAKE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WORKS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

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
		return new WaterWorksBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel
				? createTickerHelper(type, JugcraftNuclear.WATER_WORKS_ENTITY, (l, pos, s, works) -> works.serverTick((ServerLevel) l))
				: null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof WaterWorksBlockEntity works) {
			player.sendOverlayMessage(works.status());
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	/** A short line of its state (the controller clicked). */
	static Component line(String key, Object... args) {
		return Component.translatable("message.jugcraft.nuclear." + key, args);
	}
}

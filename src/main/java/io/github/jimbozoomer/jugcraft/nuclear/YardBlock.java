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
 * The Yard Controller (transformer yard and switchyard build, 20 October 2026): the anchor of the medium plant's main
 * transformer yard or of its 345 kV switchyard, each built as a compiled site (tools/yard_sites.py), under the grass.
 * The transformer yard's takes the generator's power from its isolated phase bus (ReactorUnit finds it there) through
 * the main transformers to the switchyard, and the house load through the unit auxiliary transformers to the plant's
 * load centres; the switchyard's breaker and disconnect pass it on to line 1. Clicking it tells its state.
 */
public class YardBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Works> WORKS = EnumProperty.create("works", Works.class);

	/** Which yard: its site. */
	public enum Works implements StringRepresentable {
		YARD("yard", "transformer_yard"),
		SWITCHYARD("switchyard", "switchyard");

		public final String name;
		public final String site;

		Works(String name, String site) {
			this.name = name;
			this.site = site;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public YardBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(WORKS, Works.YARD));
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
		return new YardBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, JugcraftNuclear.YARD_ENTITY, level instanceof ServerLevel
				? (l, pos, s, yard) -> yard.serverTick((ServerLevel) l)
				: (l, pos, s, yard) -> yard.clientTick(l));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof YardBlockEntity yard) {
			player.sendOverlayMessage(yard.status());
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	/** A short line of its state. */
	static Component line(String key, Object... args) {
		return Component.translatable("message.jugcraft.nuclear." + key, args);
	}
}

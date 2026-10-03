package io.github.jimbozoomer.jugcraft.powerline;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The anchor of a power line structure: its footing (or, for a box, the terminal its cables plug into). The
 * structure itself is drawn by the client from its kind; invisible {@link LineFrameBlock}s give it a body. Right-click
 * with an empty hand to read the line (rate, load, structures); sneak-right-click two structures in turn with an
 * empty hand to join or cut the wire between them; right-click it with a better support of the same family to
 * upgrade it in place.
 */
public class LineAnchorBlock extends BaseEntityBlock {
	public final LineKind kind;

	public LineAnchorBlock(LineKind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LineAnchorBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return LineFrameBlock.shapeFor(kind, BlockPos.ZERO);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || !kind.isBox()) {
			return null;
		}
		return createTickerHelper(type, JugcraftPowerLines.ANCHOR_ENTITY, (l, pos, s, anchor) -> anchor.serverTick((ServerLevel) l));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return LinePlacement.useStructure(level, pos, player);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		// Holding anything: the item acts (a line item upgrades or wires up; anything else places against it as usual).
		return InteractionResult.PASS;
	}

	/** Tooltip-style readout name. */
	public Component title() {
		return Component.translatable(getDescriptionId());
	}

	static Block anchorBlock(LineKind kind) {
		return JugcraftPowerLines.ANCHORS.get(kind);
	}
}

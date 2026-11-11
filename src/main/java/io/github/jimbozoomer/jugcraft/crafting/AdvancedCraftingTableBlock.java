package io.github.jimbozoomer.jugcraft.crafting;

import io.github.jimbozoomer.jugcraft.tower.RoomKit;
import io.github.jimbozoomer.jugcraft.tower.RoomKitBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Advanced Crafting Table: a room kit piece (3 wide, 2 high, placed whole facing the player, broken as one) whose
 * anchor holds the grid ({@link AdvancedCraftingTableBlockEntity}). Using any of its blocks opens the 6 x 6 grid. Nothing
 * ticks: the grid's items, the result's hologram, the lamp and the touchscreen are drawn on the clients from what the
 * table holds (client/AdvancedCraftingTableRenderer).
 */
public class AdvancedCraftingTableBlock extends RoomKitBlock implements EntityBlock {
	public static AdvancedCraftingTableBlock create(RoomKit.Spec spec, Properties properties) {
		return (AdvancedCraftingTableBlock) RoomKitBlock.create(spec, properties, AdvancedCraftingTableBlock::new);
	}

	protected AdvancedCraftingTableBlock(RoomKit.Spec spec, Properties properties) {
		super(spec, properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return isAnchor(state) ? new AdvancedCraftingTableBlockEntity(JugcraftCrafting.TABLE_ENTITY, pos, state) : null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(anchor(pos, state)) instanceof AdvancedCraftingTableBlockEntity table) {
			player.openMenu(table);
		}
		return InteractionResult.SUCCESS;
	}

	/** Which way the table faces (its models and live parts turned from north). */
	public Direction facingOf(BlockState state) {
		return state.hasProperty(FACING_PROP) ? state.getValue(FACING_PROP) : Direction.NORTH;
	}
}

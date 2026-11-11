package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The block entity of a warehouse piece (at its origin block): what using any of its blocks does, what it drops. */
public interface PieceEntity {
	InteractionResult use(Level level, BlockPos clicked, Player player, ItemStack stack, InteractionHand hand);

	/** Drops what it holds (the intake's bay, the station's pallets); once. */
	default void dropContents() {
	}
}

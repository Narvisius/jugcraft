package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds nothing: it lets the client draw a Haunted Portrait's moving pupils (client/HauntedPortraitRenderer.java). */
public class HauntedPortraitBlockEntity extends BlockEntity {
	public HauntedPortraitBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.HAUNTED_PORTRAIT_ENTITY, pos, state);
	}
}

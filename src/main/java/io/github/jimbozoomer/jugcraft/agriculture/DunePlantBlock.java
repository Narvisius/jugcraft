package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A two-block plant of the dunes (tools/plants.py kind "dune_plant"), like vanilla's tall grass but standing on sand as
 * well as soil: wherever vanilla's dry grass can (#minecraft:supports_dry_vegetation). Sea oats are one.
 */
public class DunePlantBlock extends DoublePlantBlock {
	private static final TagKey<Block> SUPPORTS = TagKey.create(Registries.BLOCK, Identifier.parse("minecraft:supports_dry_vegetation"));

	public DunePlantBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(SUPPORTS);
	}
}

package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Cinnamon Tree's log (garden crops, herbs and spices, part b; tools/spices.py CINNAMON). An axe strips it as any
 * Jugcraft log ({@link StrippableLogBlock}), and the bark it peels off is the spice: {@value #BARK_MIN} to
 * {@value #BARK_MAX} Cinnamon, dropped from the face that was cut. A stripped log has no bark left to give.
 */
public class CinnamonLogBlock extends StrippableLogBlock {
	public static final String BARK = "cinnamon";
	public static final int BARK_MIN = 1;
	public static final int BARK_MAX = 2;

	public CinnamonLogBlock(Properties properties, String strippedId) {
		super(properties, strippedId);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		// Read before stripping: the strip may wear the axe out.
		boolean axe = stack.is(ItemTags.AXES);
		InteractionResult result = super.useItemOn(stack, state, level, pos, player, hand, hit);
		if (axe && !level.isClientSide()) {
			int count = BARK_MIN + level.getRandom().nextInt(BARK_MAX - BARK_MIN + 1);
			Block.popResourceFromFace(level, pos, hit.getDirection(), new ItemStack(JugcraftAgriculture.item(BARK), count));
		}
		return result;
	}
}

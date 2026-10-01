package io.github.jimbozoomer.jugcraft.chemistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;

/**
 * Superphosphate fertilizer: used on the ground or a crop, it gives every crop in the 5x5 area around it (one block up
 * or down too) two doses of bone meal, using vanilla's growth so every crop grows by its own rules. One fertilizer is
 * used if anything grew. Only crops (the {@code minecraft:crops} tag), never grass or saplings.
 */
public class FertilizerItem extends Item {
	/** Blocks out from the centre: 2 makes a 5x5 area. */
	public static final int RADIUS = 2;
	/** Bone meal doses each crop gets. */
	public static final int DOSES = 2;

	public FertilizerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		int grown = fertilize(server, context.getClickedPos());
		if (grown == 0) {
			return InteractionResult.PASS;
		}
		context.getItemInHand().consume(1, context.getPlayer());
		return InteractionResult.SUCCESS;
	}

	/** Grows the crops around {@code center}; returns how many grew. Also used by game tests. */
	public static int fertilize(ServerLevel level, BlockPos center) {
		int grown = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -1, -RADIUS), center.offset(RADIUS, 1, RADIUS))) {
			if (!level.getBlockState(pos).is(BlockTags.CROPS)) {
				continue;
			}
			boolean any = false;
			for (int dose = 0; dose < DOSES; dose++) {
				// A throwaway stack: growCrop uses one up on success, and fertilizer is spent once for the whole area.
				if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, pos.immutable())) {
					any = true;
				}
			}
			if (any) {
				level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos.immutable(), 15);
				grown++;
			}
		}
		return grown;
	}
}

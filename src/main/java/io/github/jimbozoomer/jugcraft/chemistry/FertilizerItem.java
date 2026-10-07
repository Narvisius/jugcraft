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
 * used if anything grew. Only crops (the {@code minecraft:crops} tag), never grass or saplings. Other fertilizers (bat
 * guano) use the same rule over their own area and doses.
 */
public class FertilizerItem extends Item {
	/** Blocks out from the centre: 2 makes a 5x5 area. */
	public static final int RADIUS = 2;
	/** Bone meal doses each crop gets. */
	public static final int DOSES = 2;

	private final int radius;
	private final int doses;

	public FertilizerItem(Properties properties) {
		this(properties, RADIUS, DOSES);
	}

	/** A fertilizer reaching {@code radius} blocks out, giving each crop {@code doses} doses. */
	public FertilizerItem(Properties properties, int radius, int doses) {
		super(properties);
		this.radius = radius;
		this.doses = doses;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		int grown = fertilize(server, context.getClickedPos(), radius, doses);
		if (grown == 0) {
			return InteractionResult.PASS;
		}
		context.getItemInHand().consume(1, context.getPlayer());
		return InteractionResult.SUCCESS;
	}

	/** Grows the crops around {@code center} as superphosphate does; returns how many grew. Also used by game tests. */
	public static int fertilize(ServerLevel level, BlockPos center) {
		return fertilize(level, center, RADIUS, DOSES);
	}

	/** Gives the crops within {@code radius} of {@code center} {@code doses} doses each; returns how many grew. */
	public static int fertilize(ServerLevel level, BlockPos center, int radius, int doses) {
		int grown = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
			if (!level.getBlockState(pos).is(BlockTags.CROPS)) {
				continue;
			}
			boolean any = false;
			for (int dose = 0; dose < doses; dose++) {
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

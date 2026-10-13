package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A control room HVAC isolation damper (round 26, #11): a section of duct with its blades, actuator, position plates and
 * hanging tag. Its variant is 2 x its label (HvacRoom.DAMPER_TAGS: CR-D-1A to CR-D-3B) + open; the room's HVAC panel opens
 * and shuts it (HvacRoom). Right-clicking it says its tag, duty and position; sneaking and right-clicking it with an
 * empty hand steps it to the next tag (so dampers put down by hand can be told apart, as the plants' are).
 */
public class DamperBlock extends VariantFurnitureBlock {
	public static final int LABELS = 6;
	public static final IntegerProperty VARIANT = VariantFurnitureBlock.property(2 * LABELS);

	public static DamperBlock create(Properties properties, double[] shape) {
		preparing(2 * LABELS);
		return new DamperBlock(properties, shape);
	}

	private DamperBlock(Properties properties, double[] shape) {
		super(properties, 2 * LABELS, shape);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// Put down by hand: CR-D-1A, open (the normal outside air damper of the running train).
		return super.getStateForPlacement(context).setValue(VARIANT, 1);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		int v = state.getValue(VARIANT);
		if (player.isShiftKeyDown()) {
			int next = ((v / 2 + 1) % LABELS) * 2 + (v & 1);
			level.setBlock(pos, state.setValue(VARIANT, next), Block.UPDATE_CLIENTS);
			level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.6F, 1.2F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.relay_relabel", HvacRoom.DAMPER_TAGS[next / 2]));
			return InteractionResult.SUCCESS_SERVER;
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.hvac_damper", HvacRoom.describeDamper(v)));
		return InteractionResult.SUCCESS_SERVER;
	}
}

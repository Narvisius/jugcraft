package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The cable spreading room's trays, bundles and risers (punch list #15, tools/spreading_room.py): a piece's variant is
 * its safety train (0 Train A orange, 1 Train B blue, 2 non-safety grey) and, for the trays, 3 more with a trapeze
 * hanger and the tray's tag. Placed, it takes the train of a tray, bundle or riser it is put beside (else Train A);
 * used with an empty hand it steps through its variants.
 */
public class TrainFurnitureBlock extends VariantFurnitureBlock {
	private final int count;

	public static TrainFurnitureBlock create(Properties properties, int count, double[]... boxes) {
		preparing(count);
		return new TrainFurnitureBlock(properties, count, boxes);
	}

	protected TrainFurnitureBlock(Properties properties, int count, double[]... boxes) {
		super(properties, count, boxes);
		this.count = count;
	}

	/** The variant's train (0 A, 1 B, 2 non-safety), or -1 for a piece that has none (the gaps' barrier boards). */
	public int train(BlockState state) {
		return count >= 3 ? state.getValue(property(count)) % 3 : -1;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		int train = 0;
		if (count >= 3) {
			for (Direction d : Direction.values()) {
				BlockState next = context.getLevel().getBlockState(context.getClickedPos().relative(d));
				if (next.getBlock() instanceof TrainFurnitureBlock other && other.train(next) >= 0) {
					train = other.train(next);
					break;
				}
			}
		}
		return state.setValue(property(count), train);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		int v = (state.getValue(property(count)) + 1) % count;
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(property(count), v), 3);
			level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6F, 1.3F);
			int train = count >= 3 ? v % 3 : -1;
			if (train >= 0) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.csr_train_" + train, count < 6 ? Component.empty()
						: Component.translatable(v >= 3 ? "message.jugcraft.nuclear.csr_hanger" : "message.jugcraft.nuclear.csr_no_hanger")));
			}
		}
		return InteractionResult.SUCCESS;
	}
}

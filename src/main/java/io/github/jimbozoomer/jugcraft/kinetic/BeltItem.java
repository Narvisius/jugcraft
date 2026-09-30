package io.github.jimbozoomer.jugcraft.kinetic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A leather drive belt. Use it on one belt pulley, then on another with the same axis (level with the
 * first along it, up to 16 blocks away): the belt links them and the second turns with the first.
 * The pulley chosen first is remembered per player until the second click, or until another first click.
 */
public class BeltItem extends Item {
	private static final Map<UUID, BlockPos> FIRST = new HashMap<>();

	public BeltItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
		if (!(level.getBlockEntity(pos) instanceof BeltPulleyBlockEntity) || player == null) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockPos first = FIRST.remove(player.getUUID());
		if (first == null || first.equals(pos)) {
			FIRST.put(player.getUUID(), pos.immutable());
			player.sendOverlayMessage(Component.translatable("message.jugcraft.belt.first"));
			return InteractionResult.SUCCESS;
		}
		String problem = BeltPulleyBlockEntity.cannotLink(level, first, pos);
		if (problem != null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.belt." + problem));
			return InteractionResult.FAIL;
		}
		BeltPulleyBlockEntity.connect(level, first, pos);
		if (!player.getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.belt.linked"));
		return InteractionResult.SUCCESS;
	}
}

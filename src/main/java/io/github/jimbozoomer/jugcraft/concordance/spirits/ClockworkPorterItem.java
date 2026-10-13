package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.concordance.worker.Body;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A Clockwork Porter, folded (roadmap step 17): used on the top of a block by someone who understands Binding Arts, it
 * unfolds into a porter they keep, with a whole body and no charge (fuel it at a pylon by its source).
 */
public class ClockworkPorterItem extends Item {
	public ClockworkPorterItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
			return InteractionResult.SUCCESS;
		}
		if (!Workers.enabled() || !Workers.knows(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.unknown"));
			return InteractionResult.FAIL;
		}
		WorkerDefinition.Construct terms = ClockworkPorterEntity.terms();
		ClockworkPorterEntity porter = Workers.CLOCKWORK_PORTER.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (terms == null || porter == null) {
			return InteractionResult.FAIL;
		}
		BlockPos at = context.getClickedPos().relative(context.getClickedFace());
		porter.setOwner(player.getUUID());
		porter.setBody(new Body(terms.integrity(), 0L));
		porter.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYRot() + 180.0F, 0.0F);
		level.addFreshEntity(porter);
		WorkerRoster.of(level.getServer()).note(player.getUUID(), porter.getUUID(), porter.kind(), porter.status(),
				level.dimension().identifier().toString(), at);
		context.getItemInHand().consume(1, player);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.clockwork_porter").withStyle(ChatFormatting.GRAY));
	}
}

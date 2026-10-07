package io.github.jimbozoomer.jugcraft.airship;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** The zeppelin as an item: use it on the ground to set the airship down there, facing the way you face. */
public class ZeppelinItem extends Item {
	public ZeppelinItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos at = context.getClickedPos().relative(context.getClickedFace());
		Vec3 spot = Vec3.atBottomCenterOf(at);
		Player player = context.getPlayer();
		Zeppelin zeppelin = new Zeppelin(JugcraftAirships.ZEPPELIN, level);
		zeppelin.snapTo(spot.x, spot.y, spot.z, player != null ? player.getYRot() : 0.0F, 0.0F);
		if (!level.noCollision(zeppelin, zeppelin.getBoundingBox())) {
			if (player != null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.zeppelin.no_room"));
			}
			return InteractionResult.FAIL;
		}
		level.addFreshEntity(zeppelin);
		if (player == null || !player.getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.zeppelin").withStyle(ChatFormatting.GRAY));
	}
}

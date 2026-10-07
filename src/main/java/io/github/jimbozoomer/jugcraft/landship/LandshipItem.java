package io.github.jimbozoomer.jugcraft.landship;

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

/** The Landship as an item: use it on the ground to set the landship down there, facing the way you face. */
public class LandshipItem extends Item {
	public LandshipItem(Properties properties) {
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
		Landship landship = new Landship(JugcraftLandships.LANDSHIP, level);
		landship.snapTo(spot.x, spot.y, spot.z, player != null ? player.getYRot() : 0.0F, 0.0F);
		if (!level.noCollision(landship, landship.getBoundingBox())) {
			if (player != null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.landship.no_room"));
			}
			return InteractionResult.FAIL;
		}
		level.addFreshEntity(landship);
		if (player == null || !player.getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.landship").withStyle(ChatFormatting.GRAY));
	}
}

package io.github.jimbozoomer.jugcraft.rocketry;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * The line-throwing rocket (batch 40, docs/features/zipline.md). Stand by a free zipline anchor (within
 * {@link ZiplineAnchorBlockEntity#REACH} blocks), look at another one up to {@link ZiplineAnchorBlockEntity#RANGE}
 * blocks away and use it: the rocket carries a steel line across and the two anchors are joined. Used up only when the
 * line is strung.
 */
public class LineRocketItem extends Item {
	public LineRocketItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		HitResult hit = player.pick(ZiplineAnchorBlockEntity.RANGE, 1.0F, false);
		if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK
				|| !(server.getBlockEntity(block.getBlockPos()) instanceof ZiplineAnchorBlockEntity)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.line_rocket.no_target"));
			return InteractionResult.FAIL;
		}
		BlockPos target = block.getBlockPos();
		BlockPos from = nearestFreeAnchor(server, player, target);
		if (from == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.line_rocket.no_anchor"));
			return InteractionResult.FAIL;
		}
		if (!player.mayBuild() || !server.mayInteract(player, from) || !server.mayInteract(player, target)) {
			return InteractionResult.FAIL;
		}
		ZiplineAnchorBlockEntity.Result result = ZiplineAnchorBlockEntity.connect(server, from, target, player);
		if (result != ZiplineAnchorBlockEntity.Result.OK) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.line_rocket."
					+ result.name().toLowerCase(java.util.Locale.ROOT)));
			return InteractionResult.FAIL;
		}
		player.getItemInHand(hand).consume(1, player);
		server.playSound(null, from, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.5F, 0.8F);
		server.playSound(null, target, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.line_rocket.strung",
				(int) Math.round(Math.sqrt(from.distSqr(target)))));
		return InteractionResult.SUCCESS;
	}

	/** The free anchor nearest the player, within {@link ZiplineAnchorBlockEntity#REACH} blocks, other than {@code target}. */
	private static @Nullable BlockPos nearestFreeAnchor(ServerLevel level, Player player, BlockPos target) {
		int reach = ZiplineAnchorBlockEntity.REACH;
		BlockPos centre = player.blockPosition();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-reach, -reach, -reach), centre.offset(reach, reach, reach))) {
			if (!pos.equals(target) && level.getBlockEntity(pos) instanceof ZiplineAnchorBlockEntity anchor && anchor.link() == null) {
				double distance = net.minecraft.world.phys.Vec3.atCenterOf(pos).distanceToSqr(player.position());
				if (distance < bestDistance) {
					bestDistance = distance;
					best = pos.immutable();
				}
			}
		}
		return best;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.line_rocket").withStyle(ChatFormatting.GRAY));
	}
}

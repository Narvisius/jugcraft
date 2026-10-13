package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The Porter Key (roadmap step 17): sets a Clockwork Porter's route. Used on a container it records it as the source,
 * then as the target (component {@code jugcraft:porter_route}); used on a porter its keeper's party commands, it gives
 * the porter that route. Sneaking with it on a container starts again. Roadmap step 18: used on a Courier Post its
 * owner's party may use, it records the post instead (component {@code jugcraft:courier_post}), and used on a porter
 * then, the porter serves that post's requests.
 */
public class PorterKeyItem extends Item {
	public PorterKeyItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos pos = context.getClickedPos();
		ItemStack key = context.getItemInHand();
		if (level.getBlockEntity(pos) instanceof CourierPostBlockEntity post) {
			// Roadmap step 18: bound to a Courier Post, the key sets a porter to serve it.
			if (!post.mayUse(player.getUUID())) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.courier.not_yours"));
				return InteractionResult.FAIL;
			}
			key.set(Couriers.KEY_POST, GlobalPos.of(level.dimension(), pos.immutable()));
			key.remove(Workers.PORTER_ROUTE);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.courier.key_post", pos.getX(), pos.getY(), pos.getZ()));
			return InteractionResult.SUCCESS;
		}
		if (!(level.getBlockEntity(pos) instanceof Container)) {
			return InteractionResult.PASS;
		}
		// Roadmap step 28: a route only between containers its maker could use by hand (the porter asks again each trip).
		if (!Authority.mayChange(level, player, pos)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.key_refused"));
			return InteractionResult.FAIL;
		}
		key.remove(Couriers.KEY_POST);
		ClockworkPorterEntity.Route route = key.get(Workers.PORTER_ROUTE);
		String dimension = level.dimension().identifier().toString();
		if (route == null || player.isShiftKeyDown() || !route.dimension().equals(dimension) || !route.source().equals(route.target())) {
			key.set(Workers.PORTER_ROUTE, new ClockworkPorterEntity.Route(pos.immutable(), pos.immutable(), dimension));
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.key_source", pos.getX(), pos.getY(), pos.getZ()));
		} else {
			key.set(Workers.PORTER_ROUTE, new ClockworkPorterEntity.Route(route.source(), pos.immutable(), dimension));
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.key_target", pos.getX(), pos.getY(), pos.getZ()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (!(target instanceof ClockworkPorterEntity porter) || !(player instanceof ServerPlayer server)) {
			return InteractionResult.PASS;
		}
		ClockworkPorterEntity.Route route = stack.get(Workers.PORTER_ROUTE);
		if (!porter.mayServe(server)) {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.not_yours"));
			return InteractionResult.FAIL;
		}
		GlobalPos post = stack.get(Couriers.KEY_POST);
		if (post != null) {
			if (!porter.setPost(post)) {
				server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.courier.still_carrying"));
				return InteractionResult.FAIL;
			}
			server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.courier.assigned"));
			return InteractionResult.SUCCESS;
		}
		if (route == null || route.source().equals(route.target())) {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.key_unset"));
			return InteractionResult.FAIL;
		}
		porter.setRoute(route);
		server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.routed"));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		GlobalPos post = stack.get(Couriers.KEY_POST);
		if (post != null) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.concordance.porter_key.post", post.pos().toShortString())
					.withStyle(ChatFormatting.GOLD));
		}
		ClockworkPorterEntity.Route route = stack.get(Workers.PORTER_ROUTE);
		if (route != null) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.concordance.porter_key.route", route.source().toShortString(),
					route.source().equals(route.target()) ? "?" : route.target().toShortString()).withStyle(ChatFormatting.GOLD));
		}
		tooltip.accept(Component.translatable("tooltip.jugcraft.porter_key").withStyle(ChatFormatting.GRAY));
	}
}

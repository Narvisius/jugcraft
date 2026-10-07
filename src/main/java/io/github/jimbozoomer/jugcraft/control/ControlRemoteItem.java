package io.github.jimbozoomer.jugcraft.control;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The control remote (batch 37): a handheld switch for one channel of one logic controller.
 * <ul>
 * <li>Use it on a logic controller to bind it.</li>
 * <li>Sneak and use it in the air to pick the next channel.</li>
 * <li>Use it in the air to flip that channel on the bound controller, which switches its relays and alarms at once
 * (its rules may switch it back the next time their condition holds). The controller must be in the same dimension,
 * loaded, and within {@link #RANGE} blocks.</li>
 * </ul>
 */
public class ControlRemoteItem extends Item {
	public static final int RANGE = 256;

	public ControlRemoteItem(Properties properties) {
		super(properties);
	}

	public static @Nullable GlobalPos link(ItemStack stack) {
		return stack.get(JugcraftControl.REMOTE_LINK);
	}

	public static DyeColor channel(ItemStack stack) {
		return DyeColor.values()[Math.floorMod(stack.getOrDefault(JugcraftControl.REMOTE_CHANNEL, 0), Channels.COUNT)];
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!(level.getBlockEntity(pos) instanceof LogicControllerBlockEntity)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide() && context.getPlayer() != null) {
			context.getItemInHand().set(JugcraftControl.REMOTE_LINK, GlobalPos.of(level.dimension(), pos.immutable()));
			context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.control_remote.bound",
					pos.getX(), pos.getY(), pos.getZ()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive()) {
			int next = (channel(stack).ordinal() + 1) % Channels.COUNT;
			stack.set(JugcraftControl.REMOTE_CHANNEL, next);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.channel", Channels.name(DyeColor.values()[next])));
			return InteractionResult.SUCCESS;
		}
		GlobalPos link = link(stack);
		if (link == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.control_remote.unbound"));
			return InteractionResult.FAIL;
		}
		if (!link.dimension().equals(server.dimension()) || !server.isLoaded(link.pos())
				|| link.pos().distSqr(player.blockPosition()) > (double) RANGE * RANGE
				|| !(server.getBlockEntity(link.pos()) instanceof LogicControllerBlockEntity controller)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.control_remote.out_of_range"));
			return InteractionResult.FAIL;
		}
		if (!player.mayBuild() || !server.mayInteract(player, link.pos())) {
			return InteractionResult.FAIL;
		}
		DyeColor channel = channel(stack);
		boolean on = controller.toggle(server, channel);
		server.playSound(null, player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 0.5F, on ? 1.4F : 0.9F);
		player.sendOverlayMessage(Component.translatable(on ? "message.jugcraft.control_remote.on" : "message.jugcraft.control_remote.off",
				Channels.name(channel)));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.control_remote.channel", Channels.name(channel(stack)))
				.withStyle(ChatFormatting.AQUA));
		GlobalPos link = link(stack);
		tooltip.accept(link == null
				? Component.translatable("tooltip.jugcraft.control_remote.unbound").withStyle(ChatFormatting.GRAY)
				: Component.translatable("tooltip.jugcraft.control_remote.bound", link.pos().getX(), link.pos().getY(),
						link.pos().getZ()).withStyle(ChatFormatting.GRAY));
	}
}

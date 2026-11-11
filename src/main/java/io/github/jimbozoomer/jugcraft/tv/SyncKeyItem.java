package io.github.jimbozoomer.jugcraft.tv;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Sync Key (Justin, 27 October 2026: "right click on a speaker and manually sync it to a screen, just in case people
 * put multiple different screens close to each other"): links surround speakers to a screen, either way round.
 * <ul>
 * <li>Right-click a screen: the key is set to it; then right-click speakers to link each to it.</li>
 * <li>Right-click a speaker first: the key remembers it; then right-click a screen to link it (and the key is set to
 * that screen for the next ones).</li>
 * <li>Sneak + right-click a speaker: unlinks it. Sneak + right-click the air: clears the key.</li>
 * </ul>
 * A speaker links only to a screen in its own dimension within {@link TvSpeakerBlockEntity#MAX_LINK} blocks.
 */
public class SyncKeyItem extends Item {
	public SyncKeyItem(Properties properties) {
		super(properties);
	}

	public static @Nullable GlobalPos screen(ItemStack stack) {
		return stack.get(JugcraftTv.SYNC_SCREEN);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return stack.has(JugcraftTv.SYNC_SCREEN) || stack.has(JugcraftTv.SYNC_SPEAKER);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		Player player = context.getPlayer();
		boolean tv = state.getBlock() instanceof FlatTvBlock, speakerBlock = state.getBlock() instanceof TvSpeakerBlock;
		if (!tv && !speakerBlock) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel server) || player == null) {
			return InteractionResult.SUCCESS;
		}
		if (!server.mayInteract(player, pos)) {
			return InteractionResult.FAIL;
		}
		ItemStack stack = context.getItemInHand();
		if (tv) {
			BlockPos controller = FlatTvBlock.controllerOf(server, pos, state).immutable();
			GlobalPos pending = stack.get(JugcraftTv.SYNC_SPEAKER);
			stack.set(JugcraftTv.SYNC_SCREEN, GlobalPos.of(server.dimension(), controller));
			stack.remove(JugcraftTv.SYNC_SPEAKER);
			if (pending != null && pending.dimension().equals(server.dimension()) && server.isLoaded(pending.pos())
					&& server.getBlockEntity(pending.pos()) instanceof TvSpeakerBlockEntity speaker) {
				link(server, player, speaker, controller);
			} else {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.sync_key.screen", controller.getX(), controller.getY(), controller.getZ()));
				server.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
			}
			return InteractionResult.SUCCESS;
		}
		BlockPos lower = TvSpeakerBlock.lower(pos, state);
		if (!(server.getBlockEntity(lower) instanceof TvSpeakerBlockEntity speaker)) {
			return InteractionResult.FAIL;
		}
		if (player.isSecondaryUseActive()) {
			speaker.link(null);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tv_speaker.unlinked"));
			server.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.5F, 0.7F);
			return InteractionResult.SUCCESS;
		}
		GlobalPos screen = screen(stack);
		if (screen == null || !screen.dimension().equals(server.dimension())) {
			stack.set(JugcraftTv.SYNC_SPEAKER, GlobalPos.of(server.dimension(), lower.immutable()));
			player.sendOverlayMessage(Component.translatable("message.jugcraft.sync_key.speaker"));
			server.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.5F, 1.2F);
			return InteractionResult.SUCCESS;
		}
		if (!server.isLoaded(screen.pos()) || !(server.getBlockState(screen.pos()).getBlock() instanceof FlatTvBlock)) {
			stack.remove(JugcraftTv.SYNC_SCREEN);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.sync_key.gone"));
			return InteractionResult.FAIL;
		}
		BlockState ss = server.getBlockState(screen.pos());
		link(server, player, speaker, FlatTvBlock.controllerOf(server, screen.pos(), ss).immutable());
		return InteractionResult.SUCCESS;
	}

	/** Links the speaker to the screen if it's near enough, and says so. */
	private static void link(ServerLevel level, Player player, TvSpeakerBlockEntity speaker, BlockPos controller) {
		if (!(level.getBlockEntity(controller) instanceof TvBlockEntity tv)
				|| tv.screenDistance(speaker.cabinet()) > TvSpeakerBlockEntity.MAX_LINK) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.sync_key.far", TvSpeakerBlockEntity.MAX_LINK));
			return;
		}
		speaker.link(controller);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.tv_speaker.linked", controller.getX(), controller.getY(), controller.getZ()));
		level.playSound(null, speaker.getBlockPos().above(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.4F, 1.6F);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isSecondaryUseActive() && (stack.has(JugcraftTv.SYNC_SCREEN) || stack.has(JugcraftTv.SYNC_SPEAKER))) {
			if (!level.isClientSide()) {
				stack.remove(JugcraftTv.SYNC_SCREEN);
				stack.remove(JugcraftTv.SYNC_SPEAKER);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.sync_key.cleared"));
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		GlobalPos s = screen(stack);
		GlobalPos sp = stack.get(JugcraftTv.SYNC_SPEAKER);
		if (s != null) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.sync_key.screen", s.pos().getX(), s.pos().getY(), s.pos().getZ())
					.withStyle(ChatFormatting.AQUA));
		} else if (sp != null) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.sync_key.speaker", sp.pos().getX(), sp.pos().getY(), sp.pos().getZ())
					.withStyle(ChatFormatting.AQUA));
		} else {
			tooltip.accept(Component.translatable("tooltip.jugcraft.sync_key.empty").withStyle(ChatFormatting.GRAY));
		}
	}
}

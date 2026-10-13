package io.github.jimbozoomer.jugcraft.concordance.smithy;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Substrate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Artificer's Bench (roadmap step 19): used with a substrate's ingots in the main hand it forges a Resonant Ring;
 * used with a ring in the main hand it enhances it by what the other hand holds (see {@link Artificery#enhance}).
 * Everything is decided on the server; the bench keeps no state of its own.
 */
public class ArtificerBenchBlock extends Block {
	public ArtificerBenchBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		if (!RateGate.allow(server, "artifice", 6)) {
			return InteractionResult.FAIL;
		}
		use(server, serverLevel, stack, player.getOffhandItem());
		return InteractionResult.SUCCESS;
	}

	/** One use of the bench (tests call this directly). */
	public static void use(ServerPlayer player, ServerLevel level, ItemStack main, ItemStack other) {
		if (!Artificery.enabled()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return;
		}
		if (!Artificery.knows(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.artifice.refused",
					Component.translatable("compose.jugcraft.artifice.refusal.unknown")));
			return;
		}
		if (main.is(Artificery.RESONANT_RING)) {
			Artificery.enhance(player, level, main, other);
			return;
		}
		Substrate substrate = Artificery.catalog().substrateOf(Artificery.id(main.getItem()));
		if (substrate != null) {
			Artificery.forge(player, level, main, substrate);
		}
	}
}

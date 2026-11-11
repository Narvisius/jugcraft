package io.github.jimbozoomer.jugcraft.client.tv;

import io.github.jimbozoomer.jugcraft.tv.FlatTvBlock;
import io.github.jimbozoomer.jugcraft.tv.TvBlockEntity;
import io.github.jimbozoomer.jugcraft.tv.TvPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Right-clicking a flat screen TV from further off than the game's reach (Justin, 27 October 2026: a big screen is
 * watched from well back, and the window didn't always come up). With an empty hand or a block in it, a right-click
 * that reaches nothing within the game's own reach looks on as far as {@link FlatTvBlock#FAR_REACH} blocks; if the
 * first thing it meets is a TV, the server is asked for that screen's window ({@link TvPayloads.Ask}; it checks the
 * distance too). Sneaking, or holding anything else (a bow, food, a shield in the main hand), leaves the click to the
 * game. Called by mixin/client/TvFarUseMixin at the start of the game's right-click.
 * <p>Sneaking with an empty hand, a right-click on a screen that's on, from as far as {@link TvFullScreen#REACH}
 * blocks (near ones too), puts its video over the whole window ({@link TvFullScreen}); sneaking with a block in hand is
 * left to the game (building a screen out).
 */
public final class TvFarUse {
	private TvFarUse() {
	}

	/** Whether this right-click put a screen's video full screen (the game's own right-click is then skipped). */
	public static boolean tryFullscreen(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.gui.screen() != null || player.isSpectator() || !player.isShiftKeyDown()
				|| !player.getMainHandItem().isEmpty()) {
			return false;
		}
		HitResult hit = player.pick(TvFullScreen.REACH, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), false);
		if (!(hit instanceof BlockHitResult b) || hit.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		BlockState state = mc.level.getBlockState(b.getBlockPos());
		if (!(state.getBlock() instanceof FlatTvBlock)) {
			return false;
		}
		BlockPos controller = FlatTvBlock.controllerOf(mc.level, b.getBlockPos(), state);
		if (!(mc.level.getBlockEntity(controller) instanceof TvBlockEntity tv) || !tv.isController() || tv.url().isEmpty()) {
			return false;   // (off: nothing to watch; the game's own sneak right-click)
		}
		mc.gui.setScreen(new TvFullScreen(controller));
		return true;
	}

	/** Whether this right-click asked for a TV's window (the game's own right-click is then skipped). */
	public static boolean tryOpen(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.gui.screen() != null || player.isSpectator() || player.isShiftKeyDown()) {
			return false;
		}
		HitResult near = mc.hitResult;
		if (near != null && near.getType() != HitResult.Type.MISS) {
			return false;   // something in reach: the game's own right-click
		}
		ItemStack held = player.getMainHandItem();
		if (!held.isEmpty() && !(held.getItem() instanceof BlockItem)) {
			return false;
		}
		if (!ClientPlayNetworking.canSend(TvPayloads.Ask.TYPE)) {
			return false;
		}
		HitResult far = player.pick(FlatTvBlock.FAR_REACH, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), false);
		if (!(far instanceof BlockHitResult hit) || far.getType() != HitResult.Type.BLOCK
				|| !(mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof FlatTvBlock)) {
			return false;
		}
		ClientPlayNetworking.send(new TvPayloads.Ask(hit.getBlockPos()));
		return true;
	}
}

package io.github.jimbozoomer.jugcraft.nuclear;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Things used on a formed storage tank (its outlet, inlet, a shell plate or the roof hatch): a named name tag titles the
 * tank's label; a flame arrester re-rates an empty tank for flammable liquids and a rubber lining for corrosive ones
 * (Justin, Oct 7: "re-rate a placed tank while it is empty"). Re-rating repaints the shell and swaps the fittings
 * (TankFittings). A cryogenic tank is built cold and keeps its rating.
 */
public final class TankUpgrades {
	private TankUpgrades() {
	}

	/** Handles {@code stack} used on the tank part at {@code pos}, or returns null when it is nothing of these. */
	public static @Nullable InteractionResult use(ItemStack stack, Level level, BlockPos pos, Player player) {
		boolean nameTag = stack.is(Items.NAME_TAG) && stack.has(DataComponents.CUSTOM_NAME);
		TankRating to = stack.is(StorageTanks.FLAME_ARRESTER) ? TankRating.FLAMMABLE : stack.is(StorageTanks.RUBBER_LINING) ? TankRating.CORROSIVE : null;
		if (!nameTag && to == null) {
			return null;
		}
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		StorageTankPortBlockEntity tank = tankAt(server, pos);
		if (tank == null || !tank.formed()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.storage_tank.not_formed"));
			return InteractionResult.SUCCESS;
		}
		if (nameTag) {
			tank.setTitle(stack.getHoverName().getString());
			if (!player.hasInfiniteMaterials()) {
				stack.shrink(1);
			}
			level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.0F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.storage_tank.titled", tank.title()));
			return InteractionResult.SUCCESS;
		}
		Component refused = rerate(server, tank, to);
		if (refused != null) {
			player.sendOverlayMessage(refused);
			return InteractionResult.SUCCESS;
		}
		if (!player.hasInfiniteMaterials()) {
			stack.shrink(1);
		}
		level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.2F);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.storage_tank.rerated", name(to)));
		return InteractionResult.SUCCESS;
	}

	/** The formed tank's outlet for any part of it (a nozzle, a plate, the hatch), or null. */
	public static @Nullable StorageTankPortBlockEntity tankAt(Level level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof StorageTankPortBlockEntity port) {
			return port.tank();
		}
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof StorageTankPlateBlock) {
			BlockPos outlet = StorageTanks.outletOf(level, pos);
			if (outlet != null && level.getBlockEntity(outlet) instanceof StorageTankPortBlockEntity port) {
				return port.tank();
			}
		}
		return null;
	}

	/** Re-rates a formed, empty tank (repaints it, swaps its fittings); returns why not, or null when done. */
	public static @Nullable Component rerate(ServerLevel level, StorageTankPortBlockEntity tank, TankRating to) {
		TankRating from = tank.rating();
		if (from == to) {
			return Component.translatable("message.jugcraft.storage_tank.already_rated", name(to));
		}
		if (from == TankRating.CRYOGENIC || to == TankRating.CRYOGENIC) {
			return Component.translatable("message.jugcraft.storage_tank.cryogenic_fixed");
		}
		if (!tank.fluid.isResourceBlank() && tank.fluid.amount > 0) {
			return Component.translatable("message.jugcraft.storage_tank.empty_first", name(to));
		}
		BlockState state = tank.getBlockState();
		level.setBlock(tank.getBlockPos(), state.setValue(StorageTankOutletBlock.RATING, to), Block.UPDATE_ALL);
		if (level.getBlockEntity(tank.getBlockPos()) instanceof StorageTankPortBlockEntity fresh) {
			fresh.check(level);
			TankFittings.refit(level, fresh, to);
		}
		return null;
	}

	/** "flammable liquids" and so on, for messages. */
	public static Component name(TankRating rating) {
		return Component.translatable("tank.jugcraft.rating." + rating.getSerializedName());
	}

	/**
	 * Why a tank won't take a liquid: the rating it needs (or that no storage tank takes it), or null when the tank
	 * takes it.
	 */
	public static @Nullable Component refusal(TankRating rating, FluidVariant variant) {
		TankProducts.Kind kind = TankProducts.kind(variant);
		if (rating.takes(kind)) {
			return null;
		}
		Component liquid = FluidVariantAttributes.getName(variant);
		TankRating needed = TankRating.needed(kind);
		if (needed == null) {
			return Component.translatable(variant.getFluid() instanceof io.github.jimbozoomer.jugcraft.chemistry.GasFluid
					? "message.jugcraft.storage_tank.refused_gas" : "message.jugcraft.storage_tank.refused_lava", liquid);
		}
		return Component.translatable("message.jugcraft.storage_tank.refused", name(rating), liquid, name(needed));
	}
}

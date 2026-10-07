package io.github.jimbozoomer.jugcraft.fluid;

import io.github.jimbozoomer.jugcraft.chemistry.GasFluid;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGear;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGrapple;
import io.github.jimbozoomer.jugcraft.gear.PneumaticGrappleItem;
import io.github.jimbozoomer.jugcraft.gear.ScubaTankItem;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.ChatFormatting;
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
import org.jspecify.annotations.Nullable;

/**
 * The gas cylinder (batch 35, docs/features/gas-storage.md): {@link #CAPACITY} mB of any one gas, kept in the
 * {@code jugcraft:stored_fluid} component. It is a Fabric item fluid storage ({@link #storage}), so machines, tanks and
 * the fluid filter take and give gas through it as they do with buckets. Used on a block it fills from it, or empties
 * into it while sneaking; used in the air it tops up a scuba tank (oxygen) or a grapple (nitrogen) in the other hand.
 */
public class GasCylinderItem extends Item {
	public static final int CAPACITY = 8_000;
	private static final long CAPACITY_DROPLETS = CAPACITY * FluidNetworks.DROPLETS_PER_MB;

	public GasCylinderItem(Properties properties) {
		super(properties);
	}

	/** What a cylinder holds, or null when it is empty. */
	public static @Nullable StoredFluid contents(ItemStack stack) {
		StoredFluid stored = stack.get(JugcraftFluids.STORED_FLUID);
		return stored == null || stored.amount() <= 0 || stored.variant().isBlank() ? null : stored;
	}

	/** {@code stack} holding {@code amount} droplets of {@code gas} (empty, with no component, at 0). */
	public static ItemStack filled(ItemStack stack, FluidVariant gas, long amount) {
		ItemStack out = stack.copy();
		if (amount <= 0 || gas.isBlank()) {
			out.remove(JugcraftFluids.STORED_FLUID);
		} else {
			out.set(JugcraftFluids.STORED_FLUID, new StoredFluid(gas, amount));
		}
		return out;
	}

	public static boolean isGas(FluidVariant variant) {
		return variant.getFluid() instanceof GasFluid;
	}

	/** The cylinder in {@code context} as a fluid storage: one gas at a time, at most {@link #CAPACITY} mB. */
	public static Storage<FluidVariant> storage(ContainerItemContext context) {
		return new SingleSlotStorage<>() {
			private @Nullable StoredFluid stored() {
				return contents(context.getItemVariant().toStack());
			}

			@Override
			public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				if (resource.isBlank() || maxAmount <= 0 || !isGas(resource)) {
					return 0;
				}
				StoredFluid stored = stored();
				if (stored != null && !stored.variant().equals(resource)) {
					return 0;
				}
				long held = stored == null ? 0 : stored.amount();
				long moved = Math.min(maxAmount, CAPACITY_DROPLETS - held);
				return moved > 0 && exchange(resource, held + moved, transaction) ? moved : 0;
			}

			@Override
			public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				StoredFluid stored = stored();
				if (stored == null || maxAmount <= 0 || !stored.variant().equals(resource)) {
					return 0;
				}
				long moved = Math.min(maxAmount, stored.amount());
				return exchange(resource, stored.amount() - moved, transaction) ? moved : 0;
			}

			/** Swaps one cylinder in the context for one holding {@code amount}. */
			private boolean exchange(FluidVariant gas, long amount, TransactionContext transaction) {
				ItemStack current = context.getItemVariant().toStack();
				if (!(current.getItem() instanceof GasCylinderItem)) {
					return false;
				}
				return context.exchange(ItemVariant.of(filled(current.copyWithCount(1), gas, amount)), 1, transaction) == 1;
			}

			@Override
			public boolean isResourceBlank() {
				return stored() == null;
			}

			@Override
			public FluidVariant getResource() {
				StoredFluid stored = stored();
				return stored == null ? FluidVariant.blank() : stored.variant();
			}

			@Override
			public long getAmount() {
				StoredFluid stored = stored();
				return stored == null ? 0 : stored.amount();
			}

			@Override
			public long getCapacity() {
				return CAPACITY_DROPLETS;
			}
		};
	}

	/** Fills from (or, sneaking, empties into) whatever fluid storage was clicked. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		Storage<FluidVariant> block = FluidStorage.SIDED.find(level, context.getClickedPos(), context.getClickedFace());
		if (block == null) {
			return InteractionResult.PASS;
		}
		Storage<FluidVariant> cylinder = storage(ContainerItemContext.ofPlayerHand(player, context.getHand()));
		long moved;
		try (Transaction transaction = Transaction.openOuter()) {
			moved = player.isSecondaryUseActive()
					? StorageUtil.move(cylinder, block, variant -> true, Long.MAX_VALUE, transaction)
					: StorageUtil.move(block, cylinder, GasCylinderItem::isGas, Long.MAX_VALUE, transaction);
			transaction.commit();
		}
		if (moved <= 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.gas_cylinder.nothing"));
			return InteractionResult.SUCCESS;
		}
		level.playSound(null, context.getClickedPos(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.6F, 1.6F);
		report(player, player.getItemInHand(context.getHand()));
		return InteractionResult.SUCCESS;
	}

	/** Used in the air: tops up a scuba tank with oxygen, or a pneumatic grapple with nitrogen, in the other hand. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		ItemStack other = player.getItemInHand(otherHand);
		boolean scuba = other.is(JugcraftGear.SCUBA_TANK);
		boolean grapple = other.is(JugcraftGrapple.PNEUMATIC_GRAPPLE);
		ItemStack cylinder = player.getItemInHand(hand);
		StoredFluid stored = contents(cylinder);
		if (!(scuba || grapple) || stored == null) {
			return InteractionResult.PASS;
		}
		FluidVariant wanted = FluidVariant.of(scuba ? PetroFluids.OXYGEN.fluid() : PetroFluids.NITROGEN.fluid());
		if (!stored.variant().equals(wanted)) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		int held = scuba ? ScubaTankItem.oxygen(other) : PneumaticGrappleItem.nitrogen(other);
		int room = (scuba ? ScubaTankItem.CAPACITY : JugcraftGrapple.CAPACITY) - held;
		// Whole millibuckets only, so nothing is lost in the rounding.
		long mb = Math.min(room, stored.amount() / FluidNetworks.DROPLETS_PER_MB);
		if (mb <= 0) {
			return InteractionResult.PASS;
		}
		player.setItemInHand(hand, filled(cylinder, wanted, stored.amount() - mb * FluidNetworks.DROPLETS_PER_MB));
		if (scuba) {
			ScubaTankItem.setOxygen(other, held + (int) mb);
		} else {
			PneumaticGrappleItem.setNitrogen(other, held + (int) mb);
		}
		server.playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.6F, 1.8F);
		report(player, player.getItemInHand(hand));
		return InteractionResult.SUCCESS;
	}

	private static void report(Player player, ItemStack cylinder) {
		StoredFluid stored = contents(cylinder);
		player.sendOverlayMessage(stored == null ? Component.translatable("message.jugcraft.gas_cylinder.filled", "0", "-")
				: Component.translatable("message.jugcraft.gas_cylinder.filled",
						String.format("%,d", stored.amount() / FluidNetworks.DROPLETS_PER_MB),
						FluidVariantAttributes.getName(stored.variant())));
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return contents(stack) != null;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		StoredFluid stored = contents(stack);
		return stored == null ? 0 : (int) Math.round(13.0 * stored.amount() / CAPACITY_DROPLETS);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		StoredFluid stored = contents(stack);
		int color = stored == null ? 0 : PetroFluids.gaugeColor(stored.variant().getFluid());
		return color == 0 ? 0xFFB0B0B0 : color;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.gas_cylinder").withStyle(ChatFormatting.GRAY));
	}
}

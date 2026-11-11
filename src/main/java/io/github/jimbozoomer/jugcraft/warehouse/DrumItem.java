package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.fluid.StoredFluid;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/**
 * A drum (part W4): empty, it is loaded into a Drum Filling Station's magazine. It is also a fluid container in its own
 * right ({@link #storage}), holding {@link Drums#CAPACITY} mB of one liquid of its kind in {@code jugcraft:stored_fluid}
 * (a full drum dropped out of the warehouse keeps its liquid), so it can be emptied into the Liquids In panel's funnel
 * or filled at a tank like a bucket.
 */
public class DrumItem extends Item {
	private static final long CAPACITY_DROPLETS = Drums.droplets(Drums.CAPACITY);
	public final Drums.Kind kind;

	public DrumItem(Drums.Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public static @Nullable StoredFluid contents(ItemStack stack) {
		StoredFluid stored = stack.get(JugcraftFluids.STORED_FLUID);
		return stored == null || stored.amount() <= 0 || stored.variant().isBlank() ? null : stored;
	}

	public static ItemStack filled(ItemStack stack, FluidVariant liquid, long amount) {
		ItemStack out = stack.copy();
		if (amount <= 0 || liquid.isBlank()) {
			out.remove(JugcraftFluids.STORED_FLUID);
		} else {
			out.set(JugcraftFluids.STORED_FLUID, new StoredFluid(liquid, amount));
		}
		return out;
	}

	/** The drum in a context as a fluid storage: one liquid of its kind, up to 20 buckets. */
	public static Storage<FluidVariant> storage(ContainerItemContext context) {
		return new SingleSlotStorage<>() {
			private @Nullable StoredFluid stored() {
				return contents(context.getItemVariant().toStack());
			}

			private Drums.@Nullable Kind kind() {
				return Drums.kindOf(context.getItemVariant().toStack());
			}

			@Override
			public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				if (resource.isBlank() || maxAmount <= 0 || kind() == null || Drums.kindFor(resource) != kind()) {
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

			private boolean exchange(FluidVariant liquid, long amount, TransactionContext transaction) {
				ItemStack current = context.getItemVariant().toStack();
				if (!(current.getItem() instanceof DrumItem)) {
					return false;
				}
				return context.exchange(ItemVariant.of(filled(current.copyWithCount(1), liquid, amount)), 1, transaction) == 1;
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

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		StoredFluid stored = contents(stack);
		if (stored != null) {
			out.accept(Component.literal(Drums.buckets(stored.amount() / FluidNetworks.DROPLETS_PER_MB) + " ").append(Drums.name(stored.variant()))
					.withStyle(ChatFormatting.AQUA));
		}
		out.accept(Component.translatable("tooltip.jugcraft.warehouse.drum_for",
				Component.translatable("tooltip.jugcraft.warehouse.drum_kind." + kind.model)).withStyle(ChatFormatting.GRAY));
		out.accept(Component.translatable("tooltip.jugcraft.warehouse.drum").withStyle(ChatFormatting.DARK_GRAY));
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
		return 0x4AA8E0;
	}
}

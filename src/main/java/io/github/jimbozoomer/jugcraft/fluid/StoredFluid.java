package io.github.jimbozoomer.jugcraft.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

/**
 * The fluid a broken tank carries as an item (the {@code jugcraft:stored_fluid} component): its block entity puts it
 * on the drop and takes it back when the tank is placed again, so a tank keeps how full it is.
 *
 * @param amount droplets, as in Fabric's fluid storages
 */
public record StoredFluid(FluidVariant variant, long amount) {
	public static final Codec<StoredFluid> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			FluidVariant.CODEC.fieldOf("fluid").forGetter(StoredFluid::variant),
			Codec.LONG.fieldOf("amount").forGetter(StoredFluid::amount)
	).apply(instance, StoredFluid::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, StoredFluid> STREAM_CODEC = StreamCodec.composite(
			FluidVariant.PACKET_CODEC, StoredFluid::variant,
			ByteBufCodecs.VAR_LONG, StoredFluid::amount,
			StoredFluid::new);

	/** What a storage holds, or null when it is empty (an empty tank's item carries nothing, so it stacks). */
	public static @Nullable StoredFluid of(SingleFluidStorage storage) {
		return storage.isResourceBlank() || storage.amount <= 0 ? null : new StoredFluid(storage.variant, storage.amount);
	}

	/** Fills a storage from this, up to its capacity for the fluid. */
	public void restore(SingleFluidStorage storage) {
		storage.variant = variant;
		storage.amount = Math.min(amount, storage.getCapacity());
	}

	/** "Water: 3,000 mB", for the item's tooltip. */
	public Component describe() {
		return Component.translatable("tooltip.jugcraft.stored_fluid", FluidVariantAttributes.getName(variant),
				String.format("%,d", amount / FluidNetworks.DROPLETS_PER_MB));
	}
}

package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.town.TownShops;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The first block of a {@link FarmStandBlock}: who owns it, its {@value #CRATES} crates (a stack each) and the price
 * chalked for each, in Jugs. Only the {@link FarmStandMenu} changes them, and only after the server's checks; clients are
 * sent the crates and prices to draw.
 */
public class FarmStandBlockEntity extends BlockEntity {
	public static final int CRATES = 6;
	private @Nullable UUID owner;
	private String ownerName = "";
	private final SimpleContainer crates = new SimpleContainer(CRATES) {
		@Override
		public void setChanged() {
			super.setChanged();
			changed();
		}
	};
	private final long[] prices = {1, 1, 1, 1, 1, 1};

	public FarmStandBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.FARM_STAND_ENTITY, pos, state);
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public String ownerName() {
		return ownerName;
	}

	/** Whether {@code player} owns the stand (a stand with no owner, from a structure or a command, belongs to no one). */
	public boolean isOwner(Player player) {
		return owner != null && owner.equals(player.getUUID());
	}

	public void setOwner(Player player) {
		owner = player.getUUID();
		ownerName = player.getName().getString();
		changed();
	}

	public SimpleContainer crates() {
		return crates;
	}

	public ItemStack crate(int crate) {
		return crates.getItem(crate);
	}

	public long price(int crate) {
		return prices[crate];
	}

	/** The highest price a crate may ask: the town's maximum balance. */
	public static long maxPrice() {
		return TownShops.get().maxBalance;
	}

	/** Chalks a new price on a crate, kept between 1 and {@link #maxPrice()}. */
	public void setPrice(int crate, long price) {
		prices[crate] = Math.max(1, Math.min(maxPrice(), price));
		changed();
	}

	private void changed() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Broken (only by its owner), it spills what its crates held. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && !level.isClientSide()) {
			Containers.dropContents(level, pos, crates);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		ownerName = input.getStringOr("owner_name", "");
		crates.clearContent();
		ContainerHelper.loadAllItems(input, crates.getItems());
		List<Long> saved = input.read("prices", Codec.LONG.listOf()).orElse(List.of());
		for (int i = 0; i < CRATES; i++) {
			prices[i] = i < saved.size() ? Math.max(1, saved.get(i)) : 1;
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.putString("owner_name", ownerName);
		ContainerHelper.saveAllItems(output, crates.getItems());
		output.store("prices", Codec.LONG.listOf(), java.util.Arrays.stream(prices).boxed().toList());
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}

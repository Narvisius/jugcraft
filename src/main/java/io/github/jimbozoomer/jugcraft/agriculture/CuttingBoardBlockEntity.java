package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What lies on a Cutting Board: one item, kept and sent to clients for the board to show; breaking the board drops it.
 */
public class CuttingBoardBlockEntity extends BlockEntity {
	public final local.peepo.KitchenCompanionPort companionKitchen = new local.peepo.KitchenCompanionPort(this);
	private ItemStack item = ItemStack.EMPTY;
	/** Only called inside a companion transfer, including rollback. */
	public void companionPut(ItemStack stack){item=stack.copyWithCount(stack.isEmpty()?0:1);}

	public CuttingBoardBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.CUTTING_BOARD_ENTITY, pos, state);
	}

	public ItemStack item() {
		return item;
	}

	/** Sets one of {@code stack} on the empty board. */
	public boolean put(ItemStack stack) {
		if (!item.isEmpty() || stack.isEmpty()) {
			return false;
		}
		item = stack.copyWithCount(1);
		changed();
		return true;
	}

	/** Takes what is on the board off it. */
	public ItemStack take() {
		ItemStack out = item;
		item = ItemStack.EMPTY;
		if (!out.isEmpty()) {
			changed();
		}
		return out;
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		companionKitchen.drop();
		if (level != null) {
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), item);
			item = ItemStack.EMPTY;
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		companionKitchen.load(input);
		item = input.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		companionKitchen.save(output);
		output.store("item", ItemStack.OPTIONAL_CODEC, item);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}

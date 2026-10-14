package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The coat hanging in a locker (drawn by client/LockerRenderer.java). A new locker holds its kind of coat when it is
 * the top of its stack (where the rail is); the lower blocks of a stack hold nothing.
 */
public class LockerBlockEntity extends BlockEntity {
	private ItemStack coat;

	public LockerBlockEntity(BlockPos pos, BlockState state) {
		super(PlantInterior.LOCKER_ENTITY, pos, state);
		coat = state.getBlock() instanceof LockerBlock locker && !state.getValue(LockerBlock.UP)
				? new ItemStack(PlantInterior.uniform(locker.kind)) : ItemStack.EMPTY;
	}

	public ItemStack coat() {
		return coat;
	}

	public void setCoat(ItemStack stack) {
		coat = stack;
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	/** Breaking the locker drops the coat in it. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && !level.isClientSide() && !coat.isEmpty()) {
			Block.popResource(level, pos, coat);
		}
		coat = ItemStack.EMPTY;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		if (input.getBooleanOr("has_coat", true)) {
			NonNullList<ItemStack> slot = NonNullList.withSize(1, ItemStack.EMPTY);
			ContainerHelper.loadAllItems(input, slot);
			if (!slot.get(0).isEmpty()) {
				coat = slot.get(0);
			}
		} else {
			coat = ItemStack.EMPTY;
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("has_coat", !coat.isEmpty());
		ContainerHelper.saveAllItems(output, NonNullList.of(ItemStack.EMPTY, coat));
	}
}

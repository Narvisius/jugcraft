package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.List;
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
 * What a Pumpkin Crate holds: up to {@value #CAPACITY} pieces of produce ({@link PumpkinCrateBlock#PRODUCE}), one
 * each, in the order they went in. Saved with the world and sent to clients, which draw them in the crate.
 */
public class PumpkinCrateBlockEntity extends BlockEntity {
	public static final int CAPACITY = 4;

	private final List<ItemStack> produce = new ArrayList<>();

	public PumpkinCrateBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.PUMPKIN_CRATE_ENTITY, pos, state);
	}

	public List<ItemStack> produce() {
		return List.copyOf(produce);
	}

	public boolean isFull() {
		return produce.size() >= CAPACITY;
	}

	public boolean isEmpty() {
		return produce.isEmpty();
	}

	/** Sets one piece of produce in, if it is produce and there is room. */
	public boolean add(ItemStack one) {
		if (one.isEmpty() || !one.is(PumpkinCrateBlock.PRODUCE) || isFull()) {
			return false;
		}
		produce.add(one.copyWithCount(1));
		sync();
		return true;
	}

	/** Takes out the piece put in last (empty if there is none). */
	public ItemStack takeLast() {
		if (produce.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack taken = produce.removeLast();
		sync();
		return taken;
	}

	private void sync() {
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	/** Breaking the crate spills what is in it. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && !level.isClientSide()) {
			for (ItemStack stack : produce) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
			}
		}
		produce.clear();
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
		produce.clear();
		input.read("produce", ItemStack.CODEC.listOf()).ifPresent(list -> list.stream().filter(stack -> !stack.isEmpty()).limit(CAPACITY)
				.forEach(stack -> produce.add(stack.copyWithCount(1))));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("produce", ItemStack.CODEC.listOf(), List.copyOf(produce));
	}
}

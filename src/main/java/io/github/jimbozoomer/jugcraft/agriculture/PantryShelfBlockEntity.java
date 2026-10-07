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
 * The jars on a Pantry Shelf: up to {@value #SLOTS}, three to a shelf, filled from the top left and taken from the last.
 * Saved, and sent to clients to draw them; broken, the shelf spills them.
 */
public class PantryShelfBlockEntity extends BlockEntity {
	public static final int SLOTS = 6;
	private final List<ItemStack> jars = new ArrayList<>();

	public PantryShelfBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.PANTRY_SHELF_ENTITY, pos, state);
	}

	public List<ItemStack> jars() {
		return List.copyOf(jars);
	}

	public int count() {
		return jars.size();
	}

	/** What a shelf holds: jars of preserves, full or not, and empty Mason Jars. */
	public static boolean fits(ItemStack stack) {
		return stack.getItem() instanceof PreserveJarItem || stack.is(JugcraftAgriculture.item("mason_jar"));
	}

	public boolean store(ItemStack stack) {
		if (jars.size() >= SLOTS || !fits(stack)) {
			return false;
		}
		jars.add(stack.copyWithCount(1));
		changed();
		return true;
	}

	public ItemStack takeLast() {
		if (jars.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack last = jars.removeLast();
		changed();
		return last;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			for (ItemStack jar : jars) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), jar);
			}
			jars.clear();
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		jars.clear();
		input.read("jars", ItemStack.CODEC.listOf()).ifPresent(list -> list.stream().limit(SLOTS).forEach(jars::add));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("jars", ItemStack.CODEC.listOf(), List.copyOf(jars));
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

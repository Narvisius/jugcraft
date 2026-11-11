package io.github.jimbozoomer.jugcraft.warehouse;

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
import org.jspecify.annotations.Nullable;

/**
 * A pallet place's pallet (what is on it; null when empty) and its location code (A-03-2: the aisle's letter, the
 * place along the run, the level), given by the terminal when it finds its warehouse. The client draws the code on
 * the beam's label (client/warehouse/RackRenderer.java); the pallet itself is the block's LOAD.
 */
public class RackBlockEntity extends BlockEntity {
	private @Nullable PalletLoad load;
	private String label = "";
	private String groundLabel = "";

	public RackBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.RACK_ENTITY, pos, state);
	}

	public @Nullable PalletLoad load() {
		return load;
	}

	public String label() {
		return label;
	}

	/** The ground place's code under this one (drawn on this place's beam too when this is the first level). */
	public String groundLabel() {
		return groundLabel;
	}

	/** Puts a pallet here (or takes it: null), setting the block's look. */
	public void setLoad(@Nullable PalletLoad load) {
		this.load = load == null || load.isEmpty() ? null : load.copy();
		setChanged();
		if (level != null && !level.isClientSide()) {
			BlockState state = getBlockState();
			int look = this.load == null ? 0 : this.load.look();
			if (state.getBlock() instanceof RackBlock && state.getValue(RackBlock.LOAD) != look) {
				level.setBlock(worldPosition, state.setValue(RackBlock.LOAD, look), Block.UPDATE_ALL);
			} else {
				level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
			}
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		}
	}

	public void setLabels(String label, String groundLabel) {
		if (!label.equals(this.label) || !groundLabel.equals(this.groundLabel)) {
			this.label = label;
			this.groundLabel = groundLabel;
			setChanged();
			if (level != null) {
				level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}

	/** Broken with a pallet on it: the pallet and everything on it drop. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && load != null) {
			for (ItemStack stack : load.stacks()) {
				Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
			}
			Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(JugcraftWarehouse.PALLET));
			load = null;
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putString("label", label);
		tag.putString("ground", groundLabel);
		return tag;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		load = input.read("load", PalletLoad.CODEC).filter(l -> !l.isEmpty()).orElse(null);
		label = input.getStringOr("label", "");
		groundLabel = input.getStringOr("ground", "");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (load != null) {
			output.store("load", PalletLoad.CODEC, load);
		}
		output.putString("label", label);
		output.putString("ground", groundLabel);
	}
}

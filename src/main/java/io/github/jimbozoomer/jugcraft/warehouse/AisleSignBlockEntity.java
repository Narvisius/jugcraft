package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** What an aisle sign shows: the aisle ("2B"), its bays ("BAYS 01 - 08"), its levels ("LEVELS 0 - 4"); blank till found. */
public class AisleSignBlockEntity extends BlockEntity {
	private String aisle = "", bays = "", levels = "";

	public AisleSignBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.AISLE_SIGN_ENTITY, pos, state);
	}

	public String aisle() {
		return aisle;
	}

	public String bays() {
		return bays;
	}

	public String levels() {
		return levels;
	}

	public void set(String aisle, String bays, String levels) {
		if (!aisle.equals(this.aisle) || !bays.equals(this.bays) || !levels.equals(this.levels)) {
			this.aisle = aisle;
			this.bays = bays;
			this.levels = levels;
			setChanged();
			if (level != null) {
				level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putString("aisle", aisle);
		tag.putString("bays", bays);
		tag.putString("levels", levels);
		return tag;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		aisle = input.getStringOr("aisle", "");
		bays = input.getStringOr("bays", "");
		levels = input.getStringOr("levels", "");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("aisle", aisle);
		output.putString("bays", bays);
		output.putString("levels", levels);
	}
}

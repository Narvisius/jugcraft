package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The colours of a Silk Spool Stack's three spools, saved and sent to clients. */
public class SilkSpoolStackBlockEntity extends BlockEntity {
	/** The colours a new stack's spools start in. */
	public static final DyeColor[] STARTING = {DyeColor.WHITE, DyeColor.PURPLE, DyeColor.RED};
	private final DyeColor[] colours = STARTING.clone();

	public SilkSpoolStackBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.SILK_SPOOL_ENTITY, pos, state);
	}

	public DyeColor colour(int spool) {
		return colours[spool];
	}

	public void setColour(int spool, DyeColor colour) {
		colours[spool] = colour;
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	private static DyeColor named(String name, DyeColor fallback) {
		for (DyeColor colour : DyeColor.values()) {
			if (colour.getSerializedName().equals(name)) {
				return colour;
			}
		}
		return fallback;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		for (int i = 0; i < colours.length; i++) {
			colours[i] = named(input.getStringOr("spool_" + i, STARTING[i].getSerializedName()), STARTING[i]);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		for (int i = 0; i < colours.length; i++) {
			output.putString("spool_" + i, colours[i].getSerializedName());
		}
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

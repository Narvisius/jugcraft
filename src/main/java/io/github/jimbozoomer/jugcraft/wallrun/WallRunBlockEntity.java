package io.github.jimbozoomer.jugcraft.wallrun;

import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;
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
import org.jspecify.annotations.Nullable;

/**
 * What a pipe or cable in a wall looks like: the block the wall was. The client's model draws that block's own model
 * here (client/WallRunModel), so any block keeps its look with no textures of its own.
 */
public class WallRunBlockEntity extends BlockEntity implements RenderDataBlockEntity {
	private @Nullable BlockState disguise;

	public WallRunBlockEntity(BlockPos pos, BlockState state) {
		super(WallRuns.ENTITY, pos, state);
	}

	public boolean hasDisguise() {
		return disguise != null;
	}

	/** The wall's block (smooth stone until it is known). */
	public BlockState disguise() {
		return disguise == null ? WallRuns.FALLBACK : disguise;
	}

	public void setDisguise(BlockState state) {
		disguise = state;
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public @Nullable Object getRenderData() {
		return disguise();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		BlockState before = disguise;
		disguise = input.read("disguise", BlockState.CODEC).orElse(null);
		// A client told what the wall is redraws it.
		if (level != null && level.isClientSide() && before != disguise) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (disguise != null) {
			output.store("disguise", BlockState.CODEC, disguise);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		if (disguise != null) {
			tag.store("disguise", BlockState.CODEC, disguise);
		}
		return tag;
	}
}

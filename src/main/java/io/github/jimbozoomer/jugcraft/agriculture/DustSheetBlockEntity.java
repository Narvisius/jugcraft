package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * What is under a {@link DustSheetBlock}: the block it covers and, if that block had a block entity (a chest's
 * contents, say), that block entity's saved data, put back as it was when the sheet comes off. Clients are told only
 * the block, to draw the sheet over its shape; a covered chest's contents stay on the server.
 */
public class DustSheetBlockEntity extends BlockEntity {
	private BlockState covered = Blocks.AIR.defaultBlockState();
	private @Nullable CompoundTag coveredData;

	public DustSheetBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.DUST_SHEET_ENTITY, pos, state);
	}

	/** The block under the sheet (air if none is known). */
	public BlockState covered() {
		return covered;
	}

	/** The covered block entity's saved data, with its type and position, or null. */
	public @Nullable CompoundTag coveredData() {
		return coveredData;
	}

	/** Puts {@code state} (with its block entity's {@code data}, if any) under the sheet. */
	public void cover(BlockState state, @Nullable CompoundTag data) {
		covered = state;
		coveredData = data;
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Forgets what is under the sheet, once it has been put back (so removing the sheet spills nothing). */
	public void release() {
		covered = Blocks.AIR.defaultBlockState();
		coveredData = null;
		setChanged();
	}

	/** The covered block entity as it was saved, rebuilt (not placed in the world), or null. */
	public @Nullable BlockEntity rebuild(Level level) {
		return coveredData == null || covered.isAir() ? null : BlockEntity.loadStatic(worldPosition, covered, coveredData, level.registryAccess());
	}

	/** Breaking a sheet over a chest or barrel spills what was in it, as breaking the chest would. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && !level.isClientSide() && rebuild(level) instanceof Container container) {
			Containers.dropContents(level, pos, container);
		}
		coveredData = null;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		covered = input.read("covered", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
		coveredData = input.read("covered_data", CompoundTag.CODEC).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("covered", BlockState.CODEC, covered);
		if (coveredData != null) {
			output.store("covered_data", CompoundTag.CODEC, coveredData);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** Only the covered block goes to clients. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = saveWithoutMetadata(registries);
		tag.remove("covered_data");
		return tag;
	}
}

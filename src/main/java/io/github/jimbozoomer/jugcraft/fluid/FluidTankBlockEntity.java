package io.github.jimbozoomer.jugcraft.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The tinplate or glass tank's block entity. Holds up to 16 buckets of a single fluid. It does no ticking: buckets and pumps fill and empty it. Broken, it drops
 * with its fluid ({@link StoredFluid}), and placed again it holds the same.
 */
public class FluidTankBlockEntity extends BlockEntity {
	public static final long CAPACITY = 16 * FluidConstants.BUCKET;

	public final SingleFluidStorage storage = SingleFluidStorage.withFixedCapacity(CAPACITY, this::fluidChanged);

	public FluidTankBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftFluids.TANK_ENTITY, pos, state);
	}

	/** Saved; a glass tank also sends its fluid to clients, which draw it (client/GlassTankRenderer). */
	private void fluidChanged() {
		setChanged();
		if (level != null && !level.isClientSide() && getBlockState().is(JugcraftFluids.GLASS_TANK)) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
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
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		StoredFluid stored = StoredFluid.of(storage);
		if (stored != null) {
			components.set(JugcraftFluids.STORED_FLUID, stored);
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		StoredFluid stored = components.get(JugcraftFluids.STORED_FLUID);
		if (stored != null) {
			stored.restore(storage);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		storage.readValue(input);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		storage.writeValue(output);
	}
}

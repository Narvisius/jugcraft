package io.github.jimbozoomer.jugcraft.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.BlockPos;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.TransportAttachments;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.TransportPortStorage;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Holds the fluid a {@link FluidFilterBlock} lets out (blank: nothing). */
public class FluidFilterBlockEntity extends BlockEntity implements TransportAttachments.Host {
    private final TransportAttachments.Fittings fittings=new TransportAttachments.Fittings(this);
    @Override public TransportAttachments.Fittings attachments(){return fittings;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup){return fittings.updateTag(lookup);}
    @Override public void preRemoveSideEffects(BlockPos p,BlockState state){fittings.drop();super.preRemoveSideEffects(p,state);}

	private FluidVariant filter = FluidVariant.blank();

	public FluidFilterBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftFluids.FILTER_ENTITY, pos, state);
	}

	public FluidVariant filter() {
		return filter;
	}

	public void setFilter(FluidVariant filter) {
		this.filter = filter;
		setChanged();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		if(input.getBooleanOr("fittings_only",false)){fittings.load(input);return;}super.loadAdditional(input);fittings.load(input);
		filter = input.read("filter", FluidVariant.CODEC).orElseGet(FluidVariant::blank);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);fittings.save(output);
		if (!filter.isBlank()) {
			output.store("filter", FluidVariant.CODEC, filter);
		}
	}
}

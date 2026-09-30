package io.github.jimbozoomer.jugcraft.kinetic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Which pulley a belt links this one to, if any. Both pulleys of a belt point at each other. */
public class BeltPulleyBlockEntity extends BlockEntity {
	/** Longest belt, in blocks between the two pulleys. */
	public static final int MAX_LENGTH = 16;

	private @Nullable BlockPos link;

	public BeltPulleyBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftKinetics.BELT_PULLEY_ENTITY, pos, state);
	}

	public @Nullable BlockPos link() {
		return link;
	}

	void setLink(@Nullable BlockPos link) {
		this.link = link == null ? null : link.immutable();
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	/**
	 * Why two pulleys cannot share a belt, or null if they can: both must be free pulleys with the same
	 * axis, level with each other along it (the belt runs square to the axis), and at most
	 * {@link #MAX_LENGTH} blocks apart.
	 */
	public static @Nullable String cannotLink(Level level, BlockPos a, BlockPos b) {
		if (a.equals(b)) {
			return "same";
		}
		if (!(level.getBlockEntity(a) instanceof BeltPulleyBlockEntity first) || !(level.getBlockEntity(b) instanceof BeltPulleyBlockEntity second)) {
			return "not_pulley";
		}
		if (first.link != null || second.link != null) {
			return "taken";
		}
		var axis = level.getBlockState(a).getValue(ShaftBlock.AXIS);
		if (level.getBlockState(b).getValue(ShaftBlock.AXIS) != axis || a.get(axis) != b.get(axis)) {
			return "axis";
		}
		if (a.distSqr(b) > (double) MAX_LENGTH * MAX_LENGTH) {
			return "far";
		}
		return null;
	}

	/** Links two pulleys with a belt (check {@link #cannotLink} first). */
	public static void connect(Level level, BlockPos a, BlockPos b) {
		if (level.getBlockEntity(a) instanceof BeltPulleyBlockEntity first && level.getBlockEntity(b) instanceof BeltPulleyBlockEntity second) {
			first.setLink(b);
			second.setLink(a);
			KineticNetworks.invalidate(level);
		}
	}

	/** Breaking a pulley takes its belt off (it drops) and frees the other pulley. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (link != null && level != null) {
			if (level.getBlockEntity(link) instanceof BeltPulleyBlockEntity other) {
				other.setLink(null);
			}
			if (!level.isClientSide()) {
				Block.popResource(level, pos, new ItemStack(JugcraftKinetics.BELT));
			}
			link = null;
			KineticNetworks.invalidate(level);
		}
	}

	/** The belt is drawn by the client (client/BeltRenderer), so the link is sent with the block. */
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
		link = input.getLong("link").map(BlockPos::of).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (link != null) {
			output.putLong("link", link.asLong());
		}
	}
}

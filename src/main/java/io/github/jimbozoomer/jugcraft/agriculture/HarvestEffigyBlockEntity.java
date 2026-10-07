package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * The foot of a {@link HarvestEffigyBlock}: the pumpkin it wears as a head (drawn on its shoulders by the client, as the
 * Scarecrow's is), and when it was lit, so the blaze grows the same for everyone and the burn survives a reload.
 */
public class HarvestEffigyBlockEntity extends BlockEntity {
	private ItemStack head = ItemStack.EMPTY;
	private long burnStart;

	public HarvestEffigyBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.HARVEST_EFFIGY_ENTITY, pos, state);
	}

	public ItemStack head() {
		return head;
	}

	public void setHead(ItemStack stack) {
		head = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
		changed();
	}

	/** The game time it was lit at (meaningful while it burns). */
	public long burnStart() {
		return burnStart;
	}

	void ignite(long now) {
		burnStart = now;
		changed();
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		if (!state.getValue(HarvestEffigyBlock.LIT)) {
			return;
		}
		long now = level.getGameTime();
		if (now - burnStart >= HarvestEffigyBlock.BURN_TICKS) {
			HarvestEffigyBlock.collapse(level, pos);
			return;
		}
		if (Math.floorMod(now - burnStart, HarvestEffigyBlock.CHECK_TICKS) != 0) {
			return;
		}
		if (HarvestEffigyBlock.rainedOn(level, pos, state)) {
			HarvestEffigyBlock.extinguish(level, pos);
			return;
		}
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(HarvestEffigyBlock.CHEER_RADIUS))) {
			if (player.isAlive() && !player.isSpectator()) {
				HarvestCheer.give(level, player);
			}
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && !level.isClientSide() && !head.isEmpty()) {
			Containers.dropItemStack(level, pos.getX(), pos.getY() + 2, pos.getZ(), head);
		}
		head = ItemStack.EMPTY;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		head = input.read("head", ItemStack.CODEC).map(stack -> stack.copyWithCount(1)).orElse(ItemStack.EMPTY);
		burnStart = input.getLongOr("burn_start", 0L);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!head.isEmpty()) {
			output.store("head", ItemStack.CODEC, head);
		}
		output.putLong("burn_start", burnStart);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}

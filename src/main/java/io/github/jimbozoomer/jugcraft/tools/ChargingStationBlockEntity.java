package io.github.jimbozoomer.jugcraft.tools;

import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The charging station's buffer and the tool on its cradle (drawn by client/ChargingStationRenderer). */
public class ChargingStationBlockEntity extends BlockEntity {
	public static final long CAPACITY = 50_000;
	/** JE per tick it takes from cables. */
	public static final long INPUT = 1_024;
	/** JE per tick it puts into the tool. */
	public static final long CHARGE_RATE = 512;
	/** Ticks between client updates while charging (for the tool's charge bar when picked up; the look does not change). */
	private static final int SYNC_INTERVAL = 20;

	final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, INPUT, 0, this::setChanged);
	private ItemStack tool = ItemStack.EMPTY;

	public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftTools.CHARGING_STATION_ENTITY, pos, state);
	}

	public SimpleEnergyStorage energy() {
		return energy;
	}

	public ItemStack tool() {
		return tool;
	}

	public void setTool(ItemStack stack) {
		tool = stack;
		sync();
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		long given = 0;
		if (!tool.isEmpty() && energy.getAmount() > 0) {
			given = Chargeable.charge(tool, Math.min(CHARGE_RATE, energy.getAmount()));
			if (given > 0) {
				energy.setAmount(energy.getAmount() - given);
				setChanged();
				if (level.getGameTime() % SYNC_INTERVAL == 0) {
					sync();
				}
			}
		}
		boolean charging = given > 0;
		if (state.getValue(ChargingStationBlock.LIT) != charging) {
			level.setBlock(pos, state.setValue(ChargingStationBlock.LIT, charging), Block.UPDATE_CLIENTS);
			BlockPos above = pos.above();
			BlockState upper = level.getBlockState(above);
			if (upper.getBlock() instanceof ChargingStationBlock) {
				level.setBlock(above, upper.setValue(ChargingStationBlock.LIT, charging), Block.UPDATE_CLIENTS);
			}
		}
	}

	private void sync() {
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	/** Breaking the station drops the tool on it. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && !level.isClientSide() && !tool.isEmpty()) {
			Block.popResource(level, pos, tool);
		}
		tool = ItemStack.EMPTY;
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
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		energy.setAmount(input.getLong("energy").orElse(0L));
		NonNullList<ItemStack> slot = NonNullList.withSize(1, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, slot);
		tool = slot.get(0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("energy", energy.getAmount());
		ContainerHelper.saveAllItems(output, NonNullList.of(ItemStack.EMPTY, tool));
	}
}

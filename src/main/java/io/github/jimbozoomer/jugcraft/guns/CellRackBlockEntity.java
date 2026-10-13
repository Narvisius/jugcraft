package io.github.jimbozoomer.jugcraft.guns;

import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.tools.ChargingStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Cell Rack's buffer and the six Energy Cells in its cradles (slice 10E; drawn by client/guns/CellRackRenderer).
 * Each tick it shares {@value #CHARGE_RATE} JE among the cells that are not yet full, evenly, from the charge it takes
 * from cables; no cell takes more a tick than the Charging Station gives one. Every JE that leaves the buffer goes
 * into a cell. Hoppers and pipes put Energy Cells in from above or the sides, and take out only full cells, from below.
 */
public class CellRackBlockEntity extends BlockEntity implements WorldlyContainer {
	public static final int SLOTS = 6;
	public static final long CAPACITY = 50_000;
	/** JE per tick it takes from cables. */
	public static final long INPUT = 1_024;
	/** JE per tick it puts into its cells, shared evenly between those not yet full. */
	public static final long CHARGE_RATE = 1_024;
	/** The most JE a tick one cell takes: as fast as the Charging Station fills one. */
	public static final long PER_CELL = ChargingStationBlockEntity.CHARGE_RATE;
	/** Ticks between client updates while charging (a cell's look changes once it holds charge). */
	private static final int SYNC_INTERVAL = 20;
	private static final int[] ALL = {0, 1, 2, 3, 4, 5};

	final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, INPUT, 0, this::setChanged);
	private final NonNullList<ItemStack> cells = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

	public CellRackBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftGuns.CELL_RACK_ENTITY, pos, state);
	}

	public SimpleEnergyStorage energy() {
		return energy;
	}

	/** Whether this Energy Cell holds all it can. */
	public static boolean full(ItemStack cell) {
		return Chargeable.energy(cell) >= Chargeable.capacity(cell);
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		int needing = 0;
		for (ItemStack cell : cells) {
			if (!cell.isEmpty() && !full(cell)) {
				needing++;
			}
		}
		long given = 0;
		if (needing > 0 && energy.getAmount() > 0) {
			long share = Math.max(1, Math.min(PER_CELL, Math.min(CHARGE_RATE, energy.getAmount()) / needing));
			for (ItemStack cell : cells) {
				if (cell.isEmpty() || full(cell) || energy.getAmount() <= 0) {
					continue;
				}
				long put = Chargeable.charge(cell, Math.min(share, energy.getAmount()));
				energy.setAmount(energy.getAmount() - put);
				given += put;
			}
		}
		if (given > 0) {
			setChanged();
			if (level.getGameTime() % SYNC_INTERVAL == 0) {
				sync();
			}
		}
		boolean charging = given > 0;
		if (state.getValue(CellRackBlock.LIT) != charging) {
			level.setBlock(pos, state.setValue(CellRackBlock.LIT, charging), Block.UPDATE_CLIENTS);
		}
	}

	/**
	 * The slot nearest this cradle that is empty ({@code empty}) or holds a cell; -1 if there is none. Cradles are
	 * nearer along a shelf than across the shelves; of two as near, the lower slot.
	 */
	public int nearest(int cradle, boolean empty) {
		int best = -1;
		int bestDistance = Integer.MAX_VALUE;
		for (int slot = 0; slot < SLOTS; slot++) {
			if (cells.get(slot).isEmpty() != empty) {
				continue;
			}
			int distance = 2 * Math.abs(slot % 3 - cradle % 3) + 3 * Math.abs(slot / 3 - cradle / 3);
			if (distance < bestDistance) {
				best = slot;
				bestDistance = distance;
			}
		}
		return best;
	}

	/** Marks the rack changed and tells the clients that see it (a cell went in or came out). */
	public void changed() {
		sync();
	}

	private void sync() {
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	// ---------------------------------------------------------------- the cradles as a container (hoppers and pipes)

	@Override
	public int[] getSlotsForFace(Direction side) {
		return ALL;
	}

	/** Energy Cells go in from above or the sides, into an empty cradle. */
	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side != Direction.DOWN && canPlaceItem(slot, stack);
	}

	/** Only full cells come out, from below. */
	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN && full(stack);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot >= 0 && slot < SLOTS && stack.is(JugcraftGuns.ENERGY_CELL) && cells.get(slot).isEmpty();
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	public boolean isEmpty() {
		return cells.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return slot >= 0 && slot < SLOTS ? cells.get(slot) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack taken = ContainerHelper.removeItem(cells, slot, count);
		if (!taken.isEmpty()) {
			sync();
		}
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(cells, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot >= 0 && slot < SLOTS) {
			cells.set(slot, stack);
			sync();
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void clearContent() {
		cells.clear();
	}

	/** Breaking the rack drops the cells in it. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && !level.isClientSide()) {
			for (ItemStack cell : cells) {
				if (!cell.isEmpty()) {
					Block.popResource(level, pos, cell);
				}
			}
		}
		cells.clear();
	}

	// ---------------------------------------------------------------- saving and clients

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
		cells.clear();
		ContainerHelper.loadAllItems(input, cells);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("energy", energy.getAmount());
		ContainerHelper.saveAllItems(output, cells);
	}
}

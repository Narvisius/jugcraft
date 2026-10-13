package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.CargoPackagerBlockEntity;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import org.jspecify.annotations.Nullable;

/** Link only: the terminal and cargo packager remain the sole owners of power/items. No duplicate buffers. */
public final class ExchangeStationBlockEntity extends BlockEntity {
	private @Nullable UUID owner;
	private @Nullable UUID depotId;
	private @Nullable BlockPos terminalPos;
	private @Nullable BlockPos corePos;

	public ExchangeStationBlockEntity(BlockPos pos, BlockState state) {
		super(ExchangeStations.ENTITY, pos, state);
	}

	/** Display endpoints only. They grant no access and never bypass terminal()'s server validation. */
	public @Nullable BlockPos displayTerminalPos() { return terminalPos; }
	public @Nullable BlockPos displayCorePos() { return corePos; }
	public @Nullable UUID displayDepotId() { return depotId; }

	public boolean formed() {
		return !isRemoved() && level != null && getBlockState().getBlock() instanceof ExchangeStationBlock station
				&& station.complete(level, worldPosition, getBlockState());
	}

	/** Cached API handles remain guarded after a part breaks or the linked terminal changes. */
	public EnergyStorage energyStorage() {
		return new EnergyStorage() {
			private @Nullable EnergyStorage target() {
				DroneTerminalBlockEntity terminal = terminal();
				return terminal == null ? null : terminal.energy();
			}
			@Override public boolean supportsInsertion() { return target() != null; }
			@Override public boolean supportsExtraction() { return false; }
			@Override public long insert(long amount, TransactionContext tx) {
				EnergyStorage target = target();
				return target == null ? 0 : target.insert(amount, tx);
			}
			@Override public long extract(long amount, TransactionContext tx) { return 0; }
			@Override public long getAmount() { EnergyStorage target = target(); return target == null ? 0 : target.getAmount(); }
			@Override public long getCapacity() { EnergyStorage target = target(); return target == null ? 0 : target.getCapacity(); }
		};
	}

	public Storage<ItemVariant> cargoStorage() {
		return new Storage<>() {
			private @Nullable Storage<ItemVariant> target() {
				DroneTerminalBlockEntity terminal = terminal();
				var packager = terminal == null ? null : terminal.packagerEntity();
				return packager == null ? null : ContainerStorage.of(packager, null);
			}
			@Override public long insert(ItemVariant resource, long amount, TransactionContext tx) {
				Storage<ItemVariant> target = target();
				return target == null ? 0 : target.insert(resource, amount, tx);
			}
			@Override public long extract(ItemVariant resource, long amount, TransactionContext tx) {
				Storage<ItemVariant> target = target();
				return target == null ? 0 : target.extract(resource, amount, tx);
			}
			@Override public Iterator<StorageView<ItemVariant>> iterator() {
				DroneTerminalBlockEntity linked = terminal();
				var linkedPackager = linked == null ? null : linked.packagerEntity();
				Storage<ItemVariant> target = target();
				if (target == null) return Collections.emptyIterator();
				Iterator<StorageView<ItemVariant>> views = target.iterator();
				return new Iterator<>() {
					@Override public boolean hasNext() { return views.hasNext(); }
					@Override public StorageView<ItemVariant> next() {
						StorageView<ItemVariant> view = views.next();
						return new StorageView<>() {
							private boolean available() {
								return linked != null && linkedPackager != null && !linkedPackager.isRemoved()
										&& terminal() == linked && linked.packagerEntity() == linkedPackager;
							}
							@Override public boolean isResourceBlank() { return !available() || view.isResourceBlank(); }
							@Override public ItemVariant getResource() { return available() ? view.getResource() : ItemVariant.blank(); }
							@Override public long getAmount() { return available() ? view.getAmount() : 0; }
							@Override public long getCapacity() { return available() ? view.getCapacity() : 0; }
							@Override public long extract(ItemVariant resource, long amount, TransactionContext tx) {
								return available() ? view.extract(resource, amount, tx) : 0;
							}
						};
					}
				};
			}
		};
	}

	/** Opens the original store through this station, with validity measured at the clicked part. */
	public @Nullable ChestMenu cargoMenu(int id, Inventory inventory, Player player, BlockPos accessPos) {
		DroneTerminalBlockEntity linked = terminal();
		CargoPackagerBlockEntity packager = linked == null ? null : linked.packagerEntity();
		BlockPos access = accessPos.immutable();
		if (linked == null || packager == null || !cargoMenuAvailable(player, access, linked, packager)
				|| !packager.canOpen(player)) return null;
		return new ChestMenu(MenuType.GENERIC_9x3, id, inventory, packager, 3) {
			@Override public boolean stillValid(Player viewer) {
				return cargoMenuAvailable(viewer, access, linked, packager);
			}
		};
	}

	private boolean cargoMenuAvailable(Player player, BlockPos access, DroneTerminalBlockEntity linked, CargoPackagerBlockEntity packager) {
		if (level == null || player.level() != level || !level.isLoaded(access) || packager.isRemoved()
				|| player.distanceToSqr(access.getX() + .5, access.getY() + .5, access.getZ() + .5) > 64
				|| terminal() != linked || linked.packagerEntity() != packager || !mayUse(player)) return false;
		BlockState state = level.getBlockState(access);
		return state.is(ExchangeStations.CARGO) && ExchangeStations.CARGO.controller(level, access, state) == this;
	}

	/** A fixed identity, never whichever terminal happens to be closest this tick. */
	public @Nullable DroneTerminalBlockEntity terminal() {
		if (!formed() || owner == null || depotId == null || terminalPos == null || corePos == null || level == null
				|| !level.isLoaded(terminalPos) || !level.isLoaded(corePos)
				|| worldPosition.distSqr(terminalPos) > ExchangePortBlock.RANGE * ExchangePortBlock.RANGE) return null;
		if (!(level.getBlockEntity(corePos) instanceof TowerCoreBlockEntity core) || core.tier() < 1
				|| !owner.equals(core.owner()) || !terminalPos.equals(core.terminalPos())) return null;
		if (!(level.getBlockEntity(terminalPos) instanceof DroneTerminalBlockEntity terminal)
				|| !owner.equals(terminal.owner()) || !depotId.equals(terminal.depotId()) || !corePos.equals(terminal.towerPos())) return null;
		return terminal;
	}

	public boolean bind(TowerCoreBlockEntity core) {
		DroneTerminalBlockEntity terminal = core.terminal();
		if (!formed() || core.tier() < 1 || core.owner() == null || terminal == null
				|| !core.owner().equals(terminal.owner()) || !core.getBlockPos().equals(terminal.towerPos())
				|| worldPosition.distSqr(terminal.getBlockPos()) > ExchangePortBlock.RANGE * ExchangePortBlock.RANGE
				|| owner != null && !owner.equals(core.owner())) return false;
		owner = core.owner();
		depotId = terminal.depotId();
		terminalPos = terminal.getBlockPos().immutable();
		corePos = core.getBlockPos().immutable();
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		return true;
	}

	/** A placed station may connect only to a tower owned by its placer. Bounded by the saved tower index. */
	public boolean bindOwned(Player player) {
		if (!(level instanceof ServerLevel server) || !formed() || owner == null || !owner.equals(player.getUUID())) return false;
		TowerCoreBlockEntity best = null;
		double distance = (double) ExchangePortBlock.RANGE * ExchangePortBlock.RANGE;
		for (BlockPos pos : TowerRegistry.get(server).coresOf(server, owner)) {
			if (!server.isLoaded(pos) || !(server.getBlockEntity(pos) instanceof TowerCoreBlockEntity candidate)) continue;
			double d = worldPosition.distSqr(candidate.terminalPos());
			if (d <= distance && candidate.tier() >= 1 && owner.equals(candidate.owner())) {
				best = candidate;
				distance = d;
			}
		}
		return best != null && bind(best);
	}

	/** Native item placement records ownership before any linking attempt, even away from a tower. */
	public void placedBy(UUID placer) {
		if (owner == null) {
			owner = placer;
			setChanged();
		}
	}

	public boolean mayUse(Player player) {
		DroneTerminalBlockEntity terminal = terminal();
		return owner != null && owner.equals(player.getUUID()) || terminal != null
				&& terminal.tower() != null && terminal.tower().mayUse(player);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		var display = input.child("display_link");
		if (display.isPresent()) {
			// Client presentation uses the exact bound endpoints and the terminal's existing snapshot.
			terminalPos = display.get().getLong("terminal").map(BlockPos::of).orElse(null);
			corePos = display.get().getLong("core").map(BlockPos::of).orElse(null);
			depotId = display.get().read("depot", UUIDUtil.CODEC).orElse(null);
			return;
		}
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		depotId = input.read("depot", UUIDUtil.CODEC).orElse(null);
		terminalPos = input.getLong("terminal").map(BlockPos::of).orElse(null);
		corePos = input.getLong("core").map(BlockPos::of).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
		if (depotId != null) output.store("depot", UUIDUtil.CODEC, depotId);
		if (terminalPos != null) output.putLong("terminal", terminalPos.asLong());
		if (corePos != null) output.putLong("core", corePos.asLong());
	}

	@Override public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		ValueOutput display = output.child("display_link");
		if (terminalPos != null) display.putLong("terminal", terminalPos.asLong());
		if (corePos != null) display.putLong("core", corePos.asLong());
		if (depotId != null) display.store("depot", UUIDUtil.CODEC, depotId);
		return output.buildResult();
	}
}

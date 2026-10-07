package io.github.jimbozoomer.jugcraft.compat.jade;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** Sends only a bounded snapshot of the inspected machine; never scans or mutates the world. */
public enum MachineDataProvider implements IServerDataProvider<BlockAccessor> {
	INSTANCE;

	public static final Identifier ID = Jugcraft.id("machine_status");
	public static final String DATA_KEY = "jugcraft:machine_status";

	@Override
	public Identifier getUid() {
		return ID;
	}

	@Override
	public void appendServerData(CompoundTag data, BlockAccessor accessor) {
		if (!(accessor.getBlockEntity() instanceof MachineBlockEntity machine)) return;
		data.put(DATA_KEY, snapshot(machine));
	}

	public static CompoundTag snapshot(MachineBlockEntity machine) {
		CompoundTag snapshot = new CompoundTag();
		EnergyStorage energy = machine.energyFor(null);
		if (energy != null && energy.getCapacity() > 0) {
			snapshot.putLong("energy", energy.getAmount());
			snapshot.putLong("capacity", energy.getCapacity());
		}
		int duration = machine.processingDuration();
		if (duration > 0) {
			snapshot.putInt("progress", Math.clamp(machine.processingProgress(), 0, duration));
			snapshot.putInt("duration", duration);
		}
		return snapshot;
	}
}

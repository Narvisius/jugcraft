package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What a plant status screen shows, read once a second from the nearest reactor vessel within {@link #RANGE} blocks
 * (its chunk must be loaded) and sent to the players nearby. Nothing here is saved: the screen reads again on load.
 */
public class StatusScreenBlockEntity extends BlockEntity {
	public static final int RANGE = 512;
	public static final int SAMPLES = 48;
	/** 0 no reactor in range, 1 running, 2 shut down, 3 tripped (SCRAM). */
	public int status;
	public double power, rods, temperature;
	public long electric;
	public int fuel, fuelSize, alarms;
	public String reactor = "";
	public final int[] trend = new int[SAMPLES];
	private int timer;

	public StatusScreenBlockEntity(BlockPos pos, BlockState state) {
		super(PlantInterior.STATUS_ENTITY, pos, state);
	}

	void serverTick(ServerLevel level) {
		if (timer-- > 0) {
			return;
		}
		timer = 20;
		EquipmentBlockEntity vessel = nearest(level);
		if (vessel == null) {
			status = 0;
		} else {
			ReactorCore core = vessel.core;
			status = core.scrammed ? 3 : core.shutDown() ? 2 : 1;
			power = core.heat();
			rods = core.rods;
			temperature = core.temperature;
			electric = core.electric;
			fuel = core.fuel.size();
			fuelSize = core.design.size();
			alarms = core.alarms;
			reactor = vessel.kind().name();
			for (int i = 0; i < SAMPLES; i++) {
				int at = Math.floorMod(core.trendHead - (SAMPLES - i) * (ReactorCore.TREND / SAMPLES), ReactorCore.TREND);
				trend[i] = core.trend[at];
			}
		}
		setChanged();
		level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	private EquipmentBlockEntity nearest(ServerLevel level) {
		EquipmentBlockEntity best = null;
		double bestDistance = (double) RANGE * RANGE;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : EquipmentRegistry.of(level).anchors().entrySet()) {
			EquipmentKind kind = e.getValue().kind();
			if (kind != EquipmentKind.REACTOR_VESSEL && kind != EquipmentKind.EPR_VESSEL && kind != EquipmentKind.SMALL_MODULAR_REACTOR) {
				continue;
			}
			double d = e.getKey().distSqr(getBlockPos());
			if (d < bestDistance && level.isLoaded(e.getKey()) && level.getBlockEntity(e.getKey()) instanceof EquipmentBlockEntity v) {
				best = v;
				bestDistance = d;
			}
		}
		return best;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		out.putInt("status", status);
		out.putDouble("power", power);
		out.putDouble("rods", rods);
		out.putDouble("temperature", temperature);
		out.putLong("electric", electric);
		out.putInt("fuel", fuel);
		out.putInt("fuel_size", fuelSize);
		out.putInt("alarms", alarms);
		out.putString("reactor", reactor);
		out.putIntArray("trend", trend);
		return out.buildResult();
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		status = in.getIntOr("status", 0);
		power = in.getDoubleOr("power", 0);
		rods = in.getDoubleOr("rods", 0);
		temperature = in.getDoubleOr("temperature", 0);
		electric = in.getLongOr("electric", 0);
		fuel = in.getIntOr("fuel", 0);
		fuelSize = in.getIntOr("fuel_size", 0);
		alarms = in.getIntOr("alarms", 0);
		reactor = in.getStringOr("reactor", "");
		in.getIntArray("trend").ifPresent(t -> System.arraycopy(t, 0, trend, 0, Math.min(t.length, SAMPLES)));
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
	}
}

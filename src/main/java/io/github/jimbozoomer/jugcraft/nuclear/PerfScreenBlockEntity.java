package io.github.jimbozoomer.jugcraft.nuclear;

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
 * What a unit performance screen keeps, from the nearest reactor every two seconds: the unit's state and output now;
 * the energy it has sent out today and this month (MWh, a game hour 1,000 ticks, a day 24,000 from 06:00, a month 30
 * days), each hour of today's; the capacity factor (this month's energy against the rated output over the month so
 * far, since the screen went up); how long the generator has been on line and since the last trip (game days). Saved,
 * and sent to the players nearby.
 */
public class PerfScreenBlockEntity extends BlockEntity {
	public static final int HOURS = 24, MONTH_DAYS = 30, TICKS = 40;
	/** 0 no reactor in range, 1 at power, 2 shut down, 3 tripped. */
	public int status;
	public int mwe, power;
	public double dayMwh, monthMwh;
	public final double[] hours = new double[HOURS];
	public long day = Long.MIN_VALUE, month = Long.MIN_VALUE, placed = -1, monthFrom = -1, onlineSince = -1, lastTrip = -1;
	/** The game time now (for the client's day counts) and the day clock. */
	public long now, dayTime;
	private int timer;

	public PerfScreenBlockEntity(BlockPos pos, BlockState state) {
		super(PlantInterior.PERF_ENTITY, pos, state);
	}

	public void serverTick(ServerLevel level) {
		if (timer-- > 0) {
			return;
		}
		timer = TICKS - 1;
		now = level.getGameTime();
		dayTime = level.getOverworldClockTime();
		if (placed < 0) {
			placed = now;
		}
		EquipmentBlockEntity vessel = StatusScreenBlockEntity.nearestVessel(level, getBlockPos());
		int s = 0, m = 0, p = 0;
		if (vessel != null) {
			ReactorCore core = vessel.core;
			s = core.scrammed ? 3 : core.shutDown() ? 2 : 1;
			p = (int) Math.round(Math.max(0, core.heat()) * 100);
			m = core.electric > 0 ? (int) Math.round(DeskScreenBlockEntity.RATED_MWE * Math.min(1, Math.max(0, core.heat()))) : 0;
		}
		if (s == 3 && status != 3) {
			lastTrip = now;
		}
		status = s;
		mwe = m;
		power = p;
		if (m > 0) {
			if (onlineSince < 0) {
				onlineSince = now;
			}
		} else {
			onlineSince = -1;
		}
		long d = Math.floorDiv(dayTime, 24_000L), mo = Math.floorDiv(d, MONTH_DAYS);
		if (d != day) {
			day = d;
			dayMwh = 0;
			java.util.Arrays.fill(hours, 0);
		}
		if (mo != month) {
			month = mo;
			monthMwh = 0;
			monthFrom = now;
		}
		double mwh = m * TICKS / 1000.0;
		dayMwh += mwh;
		monthMwh += mwh;
		int hour = (int) (Math.floorMod(dayTime, 24_000L) / 1000);
		hours[Math.max(0, Math.min(HOURS - 1, hour))] += mwh;
		setChanged();
		level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	/** This month's capacity factor (percent), or -1 before an hour has gone by. */
	public double capacityFactor() {
		double hoursSoFar = (now - Math.max(monthFrom, placed)) / 1000.0;
		return hoursSoFar < 1 ? -1 : 100 * monthMwh / (DeskScreenBlockEntity.RATED_MWE * hoursSoFar);
	}

	/** Game days on line (-1 off line). */
	public double daysOnLine() {
		return onlineSince < 0 ? -1 : (now - onlineSince) / 24_000.0;
	}

	/** Game days since the last trip (since the screen went up if it has seen none). */
	public double daysSinceTrip() {
		return (now - (lastTrip >= 0 ? lastTrip : Math.max(0, placed))) / 24_000.0;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		save(out);
		return out.buildResult();
	}

	private void save(ValueOutput out) {
		out.putInt("status", status);
		out.putInt("mwe", mwe);
		out.putInt("power", power);
		out.putDouble("day_mwh", dayMwh);
		out.putDouble("month_mwh", monthMwh);
		out.store("hours", com.mojang.serialization.Codec.DOUBLE.listOf(), java.util.Arrays.stream(hours).boxed().toList());
		out.putLong("day", day);
		out.putLong("month", month);
		out.putLong("placed", placed);
		out.putLong("month_from", monthFrom);
		out.putLong("online_since", onlineSince);
		out.putLong("last_trip", lastTrip);
		out.putLong("now", now);
		out.putLong("day_time", dayTime);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		save(out);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		status = in.getIntOr("status", 0);
		mwe = in.getIntOr("mwe", 0);
		power = in.getIntOr("power", 0);
		dayMwh = in.getDoubleOr("day_mwh", 0);
		monthMwh = in.getDoubleOr("month_mwh", 0);
		java.util.List<Double> h = in.read("hours", com.mojang.serialization.Codec.DOUBLE.listOf()).orElse(java.util.List.of());
		for (int i = 0; i < HOURS; i++) {
			hours[i] = i < h.size() ? h.get(i) : 0;
		}
		day = in.getLongOr("day", Long.MIN_VALUE);
		month = in.getLongOr("month", Long.MIN_VALUE);
		placed = in.getLongOr("placed", -1L);
		monthFrom = in.getLongOr("month_from", -1L);
		onlineSince = in.getLongOr("online_since", -1L);
		lastTrip = in.getLongOr("last_trip", -1L);
		now = in.getLongOr("now", 0L);
		dayTime = in.getLongOr("day_time", 0L);
	}
}

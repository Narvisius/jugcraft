package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A diesel room panel's link to its generator (round 14, Justin: "link the equipment to the nearest diesel
 * generator... a generator connects to at most one"). Each panel links to the nearest emergency diesel generator within
 * {@link #REACH} blocks that has no other panel of its kind yet, so a generator has at most one engine panel, one
 * generator panel, one regulator and so on; a second set of panels put down by a second generator takes that one. The
 * link is kept while the generator stands (checked every two seconds), saved, and sent to the players, whose
 * client/LivePanelRenderer.java draws the panel from that generator's state.
 */
public class LivePanelBlockEntity extends BlockEntity {
	public static final int REACH = 24;
	/**
	 * A battery charger (round 25): what it holds of its AC supply (JE; a cable into any block of it fills it, from a
	 * load centre's CHARGER breaker), and what it charges its battery with at most (JE/t, its 150 A limit).
	 */
	public static final long CHARGER_BUFFER = 40_000, CHARGER_RATE = 1_024;
	/** A battery charger's or the DC switchboard's station battery (the placing block of the rack), or null (synced). */
	public @Nullable BlockPos battery;
	/** A battery charger: what came in from its supply in the last two seconds (whether its AC is on), and what it put
	 * into its battery a tick, on average (both synced for its meters). */
	public boolean ac;
	public int charging;
	public final io.github.jimbozoomer.jugcraft.energy.@Nullable SimpleEnergyStorage input;
	private long received, put;
	/** The linked generator's anchor, or null. */
	public @Nullable BlockPos generator;
	/**
	 * The generator's run time (game ticks) when this panel was linked to it: the engine panel's hour meter counts from
	 * there, so it reads zero when it is linked (Justin, round 14). -1: not linked.
	 */
	public long hoursBase = -1;
	private int timer;

	public LivePanelBlockEntity(BlockPos pos, BlockState state) {
		super(PlantInterior.LIVE_PANEL_ENTITY, pos, state);
		input = isCharger(state) ? new io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage(CHARGER_BUFFER, 4_096, 0, this::setChanged) {
			@Override
			public long insert(long maxAmount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
				long in = super.insert(maxAmount, transaction);
				if (in > 0) {
					transaction.addCloseCallback((t, result) -> {
						if (result.wasCommitted()) received += in;
					});
				}
				return in;
			}
		} : null;
	}

	static boolean isCharger(BlockState state) {
		return state.getBlock() == PlantInterior.BLOCKS.get("edg_battery_charger");
	}

	static boolean isSwitchboard(BlockState state) {
		return state.getBlock() == PlantInterior.BLOCKS.get("dc_switchboard");
	}

	public void serverTick(ServerLevel level) {
		if (input != null) {
			charge(level);
		}
		if (timer-- > 0) {
			return;
		}
		timer = 40;
		if (input != null || isSwitchboard(getBlockState())) {
			linkBattery(level);
		}
		if (generator != null && isGenerator(level, generator) && generator.distSqr(getBlockPos()) <= (double) REACH * REACH) {
			if (hoursBase < 0 && level.getBlockEntity(generator) instanceof EquipmentBlockEntity edg) {
				// Linked before the hour meter counted from the link: it starts from zero now.
				hoursBase = edg.runTicksAt(level.getGameTime());
				sync(level);
			}
			return;
		}
		BlockPos found = choose(level);
		if (found == null ? generator != null : !found.equals(generator)) {
			generator = found;
			hoursBase = found != null && level.getBlockEntity(found) instanceof EquipmentBlockEntity edg ? edg.runTicksAt(level.getGameTime()) : -1;
			sync(level);
		}
	}

	/** A charger: every tick it charges its battery from what its supply gave it, up to its rate. */
	private void charge(ServerLevel level) {
		if (input == null) {
			return;
		}
		StationBatteryBlockEntity bank = battery != null && level.isLoaded(battery)
				&& level.getBlockEntity(battery) instanceof StationBatteryBlockEntity b ? b : null;
		if (bank != null && input.getAmount() > 0) {
			// (Its input gives nothing back to a cable, so the charge is moved by hand.)
			long in;
			try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction t = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
				in = bank.energy.insert(Math.min(CHARGER_RATE, input.getAmount()), t);
				if (in > 0) {
					t.commit();
				}
			}
			if (in > 0) {
				input.setAmount(input.getAmount() - in);
				put += in;
			}
		}
		if (level.getGameTime() % 40 == 0) {
			boolean on = received > 0;
			int rate = (int) (put / 40);
			received = put = 0;
			if (on != ac || rate != charging) {
				ac = on;
				charging = rate;
				sync(level);
			}
		}
	}

	/**
	 * A charger or the DC switchboard keeps to its station battery: the nearest rack within {@link #REACH} blocks (a
	 * charger takes one no other charger has, if it can). The racks are looked for block by block round it, so racks
	 * put down before batteries held charge are found (and get their charge then).
	 */
	private void linkBattery(ServerLevel level) {
		if (battery != null && StationBatteryBlock.battery(level, battery) != null) {
			return;
		}
		BlockPos here = getBlockPos(), best = null, free = null;
		double bestD = Double.MAX_VALUE, freeD = Double.MAX_VALUE;
		java.util.Set<BlockPos> seen = new java.util.HashSet<>();
		for (BlockPos at : BlockPos.betweenClosed(here.offset(-REACH, -4, -REACH), here.offset(REACH, 4, REACH))) {
			if (!(level.getBlockState(at).getBlock() instanceof StationBatteryBlock)) {
				continue;
			}
			StationBatteryBlockEntity bank = StationBatteryBlock.battery(level, at);
			if (bank == null || !seen.add(bank.getBlockPos())) {
				continue;
			}
			double d = bank.getBlockPos().distSqr(here);
			if (d < bestD) {
				bestD = d;
				best = bank.getBlockPos();
			}
			if (d < freeD && (input == null || !batteryTaken(level, bank.getBlockPos()))) {
				freeD = d;
				free = bank.getBlockPos();
			}
		}
		BlockPos found = free != null ? free : best;
		if (found == null ? battery != null : !found.equals(battery)) {
			battery = found == null ? null : found.immutable();
			sync(level);
		}
	}

	/** Whether another charger already charges this battery. */
	private boolean batteryTaken(ServerLevel level, BlockPos bank) {
		int r = (REACH >> 4) + 1;
		for (int cx = (bank.getX() >> 4) - r; cx <= (bank.getX() >> 4) + r; cx++) {
			for (int cz = (bank.getZ() >> 4) - r; cz <= (bank.getZ() >> 4) + r; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be != this && be instanceof LivePanelBlockEntity other && other.input != null && bank.equals(other.battery)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** What a cable touching a charger finds: its AC input (it takes power, gives none back). */
	public static io.github.jimbozoomer.jugcraft.energy.@Nullable EnergyStorage chargerInput(net.minecraft.world.level.Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof LivePanelBlock block) || !isCharger(state)) {
			return null;
		}
		BlockPos origin = pos.subtract(block.offset(state.getValue(LivePanelBlock.FACING), state.getValue(block.partProperty())));
		return level.getBlockEntity(origin) instanceof LivePanelBlockEntity panel ? panel.input : null;
	}

	private void sync(ServerLevel level) {
		setChanged();
		level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	/** The hours the linked generator has run since this panel was linked to it, at game time {@code now}. */
	public double hours(EquipmentBlockEntity edg, double now) {
		long base = Math.max(0, hoursBase);
		double total = edg.runTicks + (edg.running ? Math.max(0, now - edg.runningSince) : 0);
		return Math.max(0, total - base) / DieselFuel.TICKS_PER_HOUR;
	}

	private static boolean isGenerator(ServerLevel level, BlockPos pos) {
		return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EquipmentBlockEntity e && e.kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR;
	}

	/** The nearest generator in reach with no other panel of this kind linked to it. */
	private @Nullable BlockPos choose(ServerLevel level) {
		List<BlockPos> candidates = new ArrayList<>();
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : EquipmentRegistry.of(level).anchors().entrySet()) {
			if (e.getValue().kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR && e.getKey().distSqr(getBlockPos()) <= (double) REACH * REACH
					&& isGenerator(level, e.getKey())) {
				candidates.add(e.getKey().immutable());
			}
		}
		candidates.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(getBlockPos())));
		for (BlockPos candidate : candidates) {
			if (!taken(level, candidate)) {
				return candidate;
			}
		}
		return null;
	}

	/** Whether another panel of this kind is already linked to the generator. */
	private boolean taken(ServerLevel level, BlockPos generator) {
		Block kind = getBlockState().getBlock();
		int r = (REACH >> 4) + 1;
		for (int cx = (generator.getX() >> 4) - r; cx <= (generator.getX() >> 4) + r; cx++) {
			for (int cz = (generator.getZ() >> 4) - r; cz <= (generator.getZ() >> 4) + r; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be != this && be instanceof LivePanelBlockEntity other && generator.equals(other.generator)
							&& other.getBlockState().getBlock() == kind) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		if (generator != null) {
			out.store("generator", BlockPos.CODEC, generator);
			out.putLong("hours_base", hoursBase);
		}
		saveBattery(out);
		if (input != null) {
			out.putLong("input", input.getAmount());
		}
	}

	private void saveBattery(ValueOutput out) {
		if (battery != null) {
			out.store("battery", BlockPos.CODEC, battery);
		}
		out.putBoolean("ac", ac);
		out.putInt("charging", charging);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		generator = in.read("generator", BlockPos.CODEC).orElse(null);
		hoursBase = in.getLongOr("hours_base", -1L);
		battery = in.read("battery", BlockPos.CODEC).orElse(null);
		ac = in.getBooleanOr("ac", false);
		charging = in.getIntOr("charging", 0);
		if (input != null) {
			input.setAmount(in.getLongOr("input", 0L));
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		if (generator != null) {
			out.store("generator", BlockPos.CODEC, generator);
			out.putLong("hours_base", hoursBase);
		}
		saveBattery(out);
		return out.buildResult();
	}
}

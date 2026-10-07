package io.github.jimbozoomer.jugcraft.control;

import java.util.Arrays;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A logic controller's rules and what they have decided (batch 36, docs/features/control-electronics.md).
 * <p>
 * Every {@link #INTERVAL} ticks it walks its {@link ControlNetwork}, reads each channel (the average of its sensors'
 * percentages), then goes through its {@link #RULES} rules in order: a rule that is on and whose condition holds
 * ("channel A below/above N%") sets its target channel on or off, a later rule overriding an earlier one. A target
 * channel no rule sets keeps its last state, so a pair of rules ("above 90% off", "below 50% on") gives a dead band.
 * Then every relay on a channel this controller targets is switched to match.
 * <p>
 * A rule is six numbers ({@link #FIELDS}): on, sensor channel, above (1) or below (0), threshold percent (0-100, in
 * steps of {@link #STEP}), target channel, and on (1) or off (0).
 */
public class LogicControllerBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
	public static final int RULES = 8;
	public static final int FIELDS = 6;
	public static final int INTERVAL = 20;
	public static final int STEP = 5;
	public static final int ENABLED = 0;
	public static final int SENSOR = 1;
	public static final int ABOVE = 2;
	public static final int THRESHOLD = 3;
	public static final int TARGET = 4;
	public static final int ACTION = 5;
	/** Menu data: the rules, then each channel's reading plus one (0: no sensor), then the channel states as bits. */
	public static final int DATA_COUNT = RULES * FIELDS + Channels.COUNT + 1;
	/** Batch 37: readings kept for the control monitor's graphs, one every {@link #HISTORY_EVERY} passes. */
	public static final int HISTORY = 24;
	public static final int HISTORY_EVERY = 5;

	private final int[] rules = new int[RULES * FIELDS];
	private final int[] readings = new int[Channels.COUNT];
	private int states;
	/** Per channel, the last {@link #HISTORY} readings, oldest first (-1: none); and how many passes since a sample. */
	private final byte[] history = new byte[Channels.COUNT * HISTORY];
	private int sinceSample;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			if (index < RULES * FIELDS) {
				return rules[index];
			}
			if (index < RULES * FIELDS + Channels.COUNT) {
				return readings[index - RULES * FIELDS] + 1;
			}
			return states;
		}

		@Override
		public void set(int index, int value) {
		}

		@Override
		public int getCount() {
			return DATA_COUNT;
		}
	};

	public LogicControllerBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftControl.CONTROLLER_ENTITY, pos, state);
		Arrays.fill(readings, -1);
		Arrays.fill(history, (byte) -1);
		for (int rule = 0; rule < RULES; rule++) {
			rules[rule * FIELDS + THRESHOLD] = 50;
			rules[rule * FIELDS + ACTION] = 1;
		}
	}

	public int rule(int rule, int field) {
		return rules[rule * FIELDS + field];
	}

	/** Sets a whole rule (tests and the menu). */
	public void setRule(int rule, boolean enabled, DyeColor sensor, boolean above, int threshold, DyeColor target, boolean on) {
		int base = rule * FIELDS;
		rules[base + ENABLED] = enabled ? 1 : 0;
		rules[base + SENSOR] = sensor.ordinal();
		rules[base + ABOVE] = above ? 1 : 0;
		rules[base + THRESHOLD] = Math.max(0, Math.min(100, threshold));
		rules[base + TARGET] = target.ordinal();
		rules[base + ACTION] = on ? 1 : 0;
		setChanged();
	}

	/** A menu button: {@code op} 0 on/off, 1 next sensor channel, 2 above/below, 3 threshold down, 4 up, 5 next target, 6 action. */
	public boolean click(int rule, int op) {
		if (rule < 0 || rule >= RULES) {
			return false;
		}
		int base = rule * FIELDS;
		switch (op) {
			case 0 -> rules[base + ENABLED] ^= 1;
			case 1 -> rules[base + SENSOR] = (rules[base + SENSOR] + 1) % Channels.COUNT;
			case 2 -> rules[base + ABOVE] ^= 1;
			case 3 -> rules[base + THRESHOLD] = Math.max(0, rules[base + THRESHOLD] - STEP);
			case 4 -> rules[base + THRESHOLD] = Math.min(100, rules[base + THRESHOLD] + STEP);
			case 5 -> rules[base + TARGET] = (rules[base + TARGET] + 1) % Channels.COUNT;
			case 6 -> rules[base + ACTION] ^= 1;
			default -> {
				return false;
			}
		}
		setChanged();
		return true;
	}

	/** A channel's last reading, 0 to 100, or -1 if no sensor on it reads anything. */
	public int reading(DyeColor channel) {
		return readings[channel.ordinal()];
	}

	/** Whether this controller last set {@code channel} on. */
	public boolean isOn(DyeColor channel) {
		return (states & (1 << channel.ordinal())) != 0;
	}

	public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		if (Math.floorMod(level.getGameTime() + pos.asLong(), INTERVAL) != 0) {
			return;
		}
		evaluate(level, pos);
	}

	/** A channel's history for the monitor, oldest first: 0 to 100, or -1 where there was no reading. */
	public int[] history(DyeColor channel) {
		int[] out = new int[HISTORY];
		for (int i = 0; i < HISTORY; i++) {
			out[i] = history[channel.ordinal() * HISTORY + i];
		}
		return out;
	}

	/** Flips a channel by hand (the remote) and switches its relays and alarms at once; rules may switch it back later. */
	public boolean toggle(ServerLevel level, DyeColor channel) {
		int bit = 1 << channel.ordinal();
		states ^= bit;
		boolean on = (states & bit) != 0;
		for (BlockPos device : ControlNetwork.find(level, worldPosition).switches()) {
			BlockState state = level.getBlockState(device);
			if (state.getValue(Channels.CHANNEL) == channel && state.getBlock() instanceof ChannelSwitch block) {
				block.switchTo(level, device, state, on);
			}
		}
		setChanged();
		sync();
		return on;
	}

	/** One pass: read the sensors, apply the rules, switch the relays and alarms. */
	public void evaluate(ServerLevel level, BlockPos pos) {
		ControlNetwork.Devices devices = ControlNetwork.find(level, pos);
		int[] sum = new int[Channels.COUNT];
		int[] count = new int[Channels.COUNT];
		for (BlockPos sensor : devices.sensors()) {
			BlockState sensorState = level.getBlockState(sensor);
			int percent = SensorBlock.percent(level, sensor, sensorState);
			if (percent >= 0) {
				int channel = sensorState.getValue(Channels.CHANNEL).ordinal();
				sum[channel] += percent;
				count[channel]++;
			}
		}
		boolean changed = false;
		for (int channel = 0; channel < Channels.COUNT; channel++) {
			int reading = count[channel] > 0 ? sum[channel] / count[channel] : -1;
			changed |= reading != readings[channel];
			readings[channel] = reading;
		}
		if (++sinceSample >= HISTORY_EVERY) {
			sinceSample = 0;
			for (int channel = 0; channel < Channels.COUNT; channel++) {
				System.arraycopy(history, channel * HISTORY + 1, history, channel * HISTORY, HISTORY - 1);
				history[channel * HISTORY + HISTORY - 1] = (byte) readings[channel];
			}
			changed = true;
		}
		int targeted = 0;
		int before = states;
		for (int rule = 0; rule < RULES; rule++) {
			int base = rule * FIELDS;
			if (rules[base + ENABLED] == 0) {
				continue;
			}
			int target = rules[base + TARGET];
			targeted |= 1 << target;
			int reading = readings[rules[base + SENSOR]];
			if (reading < 0) {
				continue;
			}
			boolean holds = rules[base + ABOVE] == 1 ? reading > rules[base + THRESHOLD] : reading < rules[base + THRESHOLD];
			if (holds) {
				states = rules[base + ACTION] == 1 ? states | (1 << target) : states & ~(1 << target);
			}
		}
		for (BlockPos device : devices.switches()) {
			BlockState deviceState = level.getBlockState(device);
			int channel = deviceState.getValue(Channels.CHANNEL).ordinal();
			if ((targeted & (1 << channel)) != 0 && deviceState.getBlock() instanceof ChannelSwitch block) {
				block.switchTo(level, device, deviceState, (states & (1 << channel)) != 0);
			}
		}
		if (states != before || changed) {
			setChanged();
			sync();
		}
	}

	/** Sends the readings, states and history to nearby clients, for control monitors. */
	private void sync() {
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		int[] savedReadings = input.getIntArray("readings").orElse(new int[0]);
		System.arraycopy(savedReadings, 0, readings, 0, Math.min(savedReadings.length, readings.length));
		int[] savedHistory = input.getIntArray("history").orElse(new int[0]);
		for (int i = 0; i < Math.min(savedHistory.length, history.length); i++) {
			history[i] = (byte) savedHistory[i];
		}
		int[] saved = input.getIntArray("rules").orElse(new int[0]);
		System.arraycopy(saved, 0, rules, 0, Math.min(saved.length, rules.length));
		states = input.getIntOr("states", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putIntArray("rules", rules.clone());
		output.putInt("states", states);
		output.putIntArray("readings", readings.clone());
		int[] savedHistory = new int[history.length];
		for (int i = 0; i < history.length; i++) {
			savedHistory[i] = history[i];
		}
		output.putIntArray("history", savedHistory);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return worldPosition;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.jugcraft.logic_controller");
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new LogicControllerMenu(containerId, inventory, worldPosition, data, this);
	}
}

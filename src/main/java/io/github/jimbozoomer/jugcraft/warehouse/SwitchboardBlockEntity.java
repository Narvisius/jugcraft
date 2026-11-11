package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Part W5: the Warehouse Switchboard (its back row in an outer wall). Outside, the service entrance: the incoming cable
 * connects to it (any of its faces: EnergyStorage.SIDED), its meter shows what the warehouse draws, its main switch
 * cuts the warehouse off (for maintenance). Inside, the main distribution board: the incomer's breaker and power
 * screen, the six circuits' breakers and lamps (the terminal's circuits, switched here or on the terminal's CONTROL
 * page), the UPS section. It stores power for the warehouse; the terminal takes what the warehouse draws from it each
 * second. What its live parts show comes from its terminal once a second.
 */
public class SwitchboardBlockEntity extends BlockEntity implements PieceEntity, AnalogSource, ExtendedMenuProvider<BlockPos> {
	/** Its store: filled by cable (nothing takes from it but its warehouse: it is no battery for the grid). */
	public final SimpleEnergyStorage energy = new SimpleEnergyStorage(WarehousePower.CAPACITY, WarehousePower.MAX_IN, 0, this::setChanged);
	private boolean mainOn = true;
	private @Nullable BlockPos terminal;
	/** Taken by the terminal since the last second; the store's amount then (for what came in). */
	private long taken, lastAmount;
	// What its live parts show (from its terminal, once a second; synced when it changes).
	private int circuits = 0x3F, shed;
	private long supply, draw;
	private int storedPct;
	private boolean linked;
	private int lastAnalog = -1;

	public SwitchboardBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.SWITCHBOARD_ENTITY, pos, state);
	}

	public boolean mainOn() {
		return mainOn;
	}

	public boolean linked() {
		return linked;
	}

	/** The circuits switched on (bit k: WarehousePower.Circuit k) and shed. */
	public int circuits() {
		return circuits;
	}

	public int shed() {
		return shed;
	}

	/** JE a tick that came in by cable over the last second, and what the warehouse drew. */
	public long supply() {
		return supply;
	}

	public long draw() {
		return draw;
	}

	public int storedPct() {
		return storedPct;
	}

	public @Nullable TerminalBlockEntity terminal() {
		return level == null ? null : Warehouses.terminalNear(level, worldPosition);
	}

	public @Nullable BlockPos terminalPos() {
		return terminal;
	}

	/** What the warehouse may take now (nothing with the main switch off). */
	long available() {
		return mainOn ? energy.getAmount() : 0;
	}

	/** The terminal takes what the warehouse drew. */
	long take(long amount) {
		long n = Math.max(0, Math.min(amount, available()));
		if (n > 0) {
			energy.setAmount(energy.getAmount() - n);
			taken += n;
			setChanged();
		}
		return n;
	}

	/** The terminal's view, once a second (what the lamps, toggles, screens and meter show). */
	void show(int circuits, int shed, long supply, long draw) {
		int pct = (int) Math.round(100.0 * energy.getAmount() / Math.max(1, energy.getCapacity()));
		if (circuits != this.circuits || shed != this.shed || draw != this.draw || pct != storedPct || !linked) {
			this.circuits = circuits;
			this.shed = shed;
			this.draw = draw;
			this.storedPct = pct;
			this.linked = true;
			sync();
		}
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		Warehouses.addSwitchboard(level, worldPosition);
		if (now % 20 == 0) {
			// What came in by cable over the last second: the store's rise plus what the terminal took.
			long amount = energy.getAmount();
			long in = Math.max(0, (amount - lastAmount + taken) / 20);
			lastAmount = amount;
			taken = 0;
			TerminalBlockEntity t = terminal();
			BlockPos tp = t == null ? null : t.getBlockPos();
			boolean changed = in != supply || !java.util.Objects.equals(tp, terminal);
			supply = in;
			terminal = tp;
			if (t == null && linked) {
				linked = false;
				circuits = 0;
				shed = 0;
				draw = 0;
				changed = true;
			}
			int pct = (int) Math.round(100.0 * amount / Math.max(1, energy.getCapacity()));
			if (pct != storedPct) {
				storedPct = pct;
				changed = true;
			}
			if (changed) {
				sync();
			}
			int a = analog();
			if (a != lastAnalog) {
				lastAnalog = a;
				PieceBlock.analogChanged(level, worldPosition, getBlockState());
			}
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null) {
			Warehouses.removeSwitchboard(level, worldPosition);
		}
	}

	@Override
	public int analog() {
		long amount = energy.getAmount();
		return amount <= 0 ? 0 : 1 + (int) (14 * amount / Math.max(1, energy.getCapacity()));
	}

	/** The main switch (the outside one, or MAIN OFF on the board's screen). */
	public void setMain(boolean on) {
		if (on != mainOn) {
			mainOn = on;
			if (level != null) {
				level.playSound(null, worldPosition, on ? SoundEvents.LEVER_CLICK : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, on ? 0.7F : 0.6F);
			}
			sync();
		}
	}

	void sync() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public InteractionResult use(Level level, BlockPos clicked, Player player, ItemStack stack, InteractionHand hand) {
		if (!level.isClientSide()) {
			player.openMenu(this);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.jugcraft.warehouse_switchboard");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new SwitchboardMenu(id, inventory, worldPosition);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return worldPosition;
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
		energy.setAmount(Math.max(0, Math.min(energy.getCapacity(), input.getLongOr("energy", 0))));
		mainOn = input.getBooleanOr("main", true);
		circuits = input.getIntOr("circuits", 0x3F);
		shed = input.getIntOr("shed", 0);
		supply = input.getLongOr("supply", 0);
		draw = input.getLongOr("draw", 0);
		storedPct = input.getIntOr("stored_pct", 0);
		linked = input.getBooleanOr("linked", false);
		lastAmount = energy.getAmount();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("energy", energy.getAmount());
		output.putBoolean("main", mainOn);
		output.putInt("circuits", circuits);
		output.putInt("shed", shed);
		output.putLong("supply", supply);
		output.putLong("draw", draw);
		output.putInt("stored_pct", storedPct);
		output.putBoolean("linked", linked);
	}
}

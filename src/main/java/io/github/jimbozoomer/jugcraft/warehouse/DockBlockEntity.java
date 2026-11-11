package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A charging dock's drone (its kind and fleet number; none: an empty dock) and what its charger shows (home or out,
 * the battery), kept by the terminal (TerminalBlockEntity.battery) for the charger's display and LED bar.
 */
public class DockBlockEntity extends BlockEntity {
	private @Nullable DroneKind kind;
	private int droneId;
	private boolean home = true;
	private float battery = 1;

	public DockBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.DOCK_ENTITY, pos, state);
	}

	public @Nullable DroneKind kind() {
		return kind;
	}

	public int droneId() {
		return droneId;
	}

	public boolean home() {
		return home;
	}

	public float battery() {
		return battery;
	}

	void setDrone(@Nullable DroneKind kind, int id) {
		this.kind = kind;
		this.droneId = id;
		sync();
	}

	/** What the charger shows: the drone home (charging) or out, its battery (only sent on a visible change). */
	void status(boolean home, float battery) {
		if (home != this.home || Math.abs(battery - this.battery) >= 0.01F) {
			this.home = home;
			this.battery = battery;
			sync();
		}
	}

	/** A player parks a drone here (DroneItem.useOn). */
	public InteractionResult park(Player player, ItemStack stack, DroneKind droneKind) {
		if (level == null || level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (kind != null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.dock_taken"));
			return InteractionResult.FAIL;
		}
		kind = droneKind;
		droneId = 0;
		home = true;
		battery = 1;
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
		level.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 1.3F);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.docked", Component.translatable("item.jugcraft."
				+ (droneKind == DroneKind.STACKER ? "stacker_drone" : "pallet_drone"))));
		sync();
		Warehouses.changed(level, worldPosition);
		return InteractionResult.SUCCESS;
	}

	/** Sneak-using the dock with an empty hand takes its drone back, when it is home and not busy. */
	public InteractionResult use(Player player) {
		if (level == null || level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (kind == null || !player.isSecondaryUseActive() || !player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		TerminalBlockEntity terminal = Warehouses.terminalOf(level, worldPosition);
		if (terminal != null) {
			for (Drone d : terminal.drones()) {
				if (d.dock.equals(worldPosition) && (!d.docked || d.busy(level.getGameTime()) || level.getGameTime() < d.leg.end())) {
					return InteractionResult.FAIL;
				}
			}
		}
		ItemStack drone = new ItemStack(kind == DroneKind.STACKER ? JugcraftWarehouse.STACKER_DRONE : JugcraftWarehouse.PALLET_DRONE);
		if (!player.getInventory().add(drone)) {
			Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), drone);
		}
		kind = null;
		droneId = 0;
		sync();
		Warehouses.changed(level, worldPosition);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && kind != null) {
			Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
					new ItemStack(kind == DroneKind.STACKER ? JugcraftWarehouse.STACKER_DRONE : JugcraftWarehouse.PALLET_DRONE));
			kind = null;
		}
	}

	private void sync() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
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
		String k = input.getStringOr("kind", "");
		kind = k.isEmpty() ? null : DroneKind.valueOf(k);
		droneId = input.getIntOr("drone", 0);
		home = input.getBooleanOr("home", true);
		battery = input.getFloatOr("battery", 1);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("kind", kind == null ? "" : kind.name());
		output.putInt("drone", droneId);
		output.putBoolean("home", home);
		output.putFloat("battery", battery);
	}
}

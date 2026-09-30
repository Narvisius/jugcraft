package io.github.jimbozoomer.jugcraft.kinetic;

import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import io.github.jimbozoomer.jugcraft.machine.GeneratorFuels;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The steam engine's fuel, water and fire (see {@link SteamEngineBlock}). It burns only while
 * something on its shaft line takes power, so fuel is never wasted into a stopped line.
 */
public class SteamEngineBlockEntity extends BlockEntity {
	/** KE per tick while running. */
	public static final long OUTPUT = 64;
	/** Water (mB) boiled per tick of running. */
	public static final int WATER_PER_TICK = 10;
	public static final int TANK_MB = 8_000;
	/** mB per tick from a water source directly below. */
	public static final int SOURCE_REFILL = 20;
	/** Fuel items it holds (one kind at a time). */
	public static final int FUEL_CAPACITY = 64;

	final SingleItemStorage fuel = new SingleItemStorage() {
		@Override
		protected long getCapacity(ItemVariant variant) {
			return FUEL_CAPACITY;
		}

		@Override
		protected boolean canInsert(ItemVariant variant) {
			return GeneratorFuels.steamBurnTicks(variant.toStack()) > 0;
		}

		@Override
		protected boolean canExtract(ItemVariant variant) {
			return false;
		}

		@Override
		protected void onFinalCommit() {
			setChanged();
		}
	};
	/** Lets pumps, pipes and buckets fill the water tank, in whole millibuckets. */
	final WaterInlet waterInlet = new WaterInlet();
	private int water;
	private int burn;

	public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftKinetics.STEAM_ENGINE_ENTITY, pos, state);
	}

	int water() {
		return water;
	}

	boolean addWater(int millibuckets) {
		if (water + millibuckets > TANK_MB) {
			return false;
		}
		water += millibuckets;
		setChanged();
		return true;
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		if (water < TANK_MB && level.getFluidState(pos.below()).isSourceOfType(Fluids.WATER)) {
			water = Math.min(TANK_MB, water + SOURCE_REFILL);
			setChanged();
		}
		if (burn <= 0 && water >= WATER_PER_TICK && !fuel.isResourceBlank()) {
			int ticks = GeneratorFuels.steamBurnTicks(fuel.variant.toStack());
			if (ticks > 0) {
				fuel.amount--;
				if (fuel.amount <= 0) {
					fuel.variant = ItemVariant.blank();
				}
				burn = ticks;
				setChanged();
			}
		}
		boolean running = false;
		if (burn > 0 && water >= WATER_PER_TICK) {
			long taken = KineticNetworks.push(level, pos, state.getValue(SteamEngineBlock.FACING).getOpposite(), OUTPUT);
			if (taken > 0) {
				burn--;
				water -= WATER_PER_TICK;
				running = true;
				setChanged();
			}
		}
		if (state.getValue(SteamEngineBlock.LIT) != running) {
			level.setBlock(pos, state.setValue(SteamEngineBlock.LIT, running), Block.UPDATE_ALL);
		}
	}

	final class WaterInlet extends SnapshotParticipant<Integer> implements InsertionOnlyStorage<FluidVariant> {
		@Override
		public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
			StoragePreconditions.notBlankNotNegative(resource, maxAmount);
			if (!resource.isOf(Fluids.WATER)) {
				return 0;
			}
			long millibuckets = Math.min(maxAmount / FluidNetworks.DROPLETS_PER_MB, TANK_MB - water);
			if (millibuckets <= 0) {
				return 0;
			}
			updateSnapshots(transaction);
			water += (int) millibuckets;
			return millibuckets * FluidNetworks.DROPLETS_PER_MB;
		}

		@Override
		protected Integer createSnapshot() {
			return water;
		}

		@Override
		protected void readSnapshot(Integer snapshot) {
			water = snapshot;
		}

		@Override
		protected void onFinalCommit() {
			setChanged();
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		fuel.readValue(input);
		water = input.getInt("water").orElse(0);
		burn = input.getInt("burn").orElse(0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		fuel.writeValue(output);
		output.putInt("water", water);
		output.putInt("burn", burn);
	}
}

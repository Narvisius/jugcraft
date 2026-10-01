package io.github.jimbozoomer.jugcraft.farming;

import io.github.jimbozoomer.jugcraft.chemistry.FertilizerItem;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The sprinkler's water tank and fertilizer hopper. Every {@link #PULSE_TICKS} ticks with at least
 * {@link #WATER_PER_PULSE} mB of water, it uses that water and gives every growing crop within {@link #RADIUS} blocks
 * (at its height and one below) one extra growth tick, as if the game had picked it. Every
 * {@link #FERTILIZE_PULSES} pulses with fertilizer loaded, it spreads one over the 5x5 crops around it (only if one
 * grows).
 */
public class SprinklerBlockEntity extends BlockEntity {
	public static final int PULSE_TICKS = 100;
	public static final int WATER_PER_PULSE = 50;
	public static final int RADIUS = 4;
	public static final int FERTILIZE_PULSES = 6;
	public static final int TANK_MB = 4_000;
	public static final int MAX_FERTILIZER = 16;

	final SingleFluidStorage water = new SingleFluidStorage() {
		@Override
		protected long getCapacity(FluidVariant variant) {
			return TANK_MB * FluidNetworks.DROPLETS_PER_MB;
		}

		@Override
		protected boolean canInsert(FluidVariant variant) {
			return variant.isOf(Fluids.WATER);
		}

		@Override
		protected void onFinalCommit() {
			setChanged();
		}
	};

	private ItemStack fertilizerStack = ItemStack.EMPTY;
	/** Hoppers and pipes load fertilizer here; nothing takes it back out. */
	final SingleStackStorage hopper = new SingleStackStorage() {
		@Override
		protected ItemStack getStack() {
			return fertilizerStack;
		}

		@Override
		protected void setStack(ItemStack stack) {
			fertilizerStack = stack;
		}

		@Override
		protected boolean canInsert(ItemVariant variant) {
			return variant.isOf(PetroItems.FERTILIZER);
		}

		@Override
		protected int getCapacity(ItemVariant variant) {
			return MAX_FERTILIZER;
		}

		@Override
		public long extract(ItemVariant variant, long maxAmount, TransactionContext transaction) {
			return 0;
		}

		@Override
		protected void onFinalCommit() {
			setChanged();
		}
	};
	private int pulses;

	public SprinklerBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftFarming.SPRINKLER_ENTITY, pos, state);
	}

	public SingleFluidStorage water() {
		return water;
	}

	public int fertilizer() {
		return fertilizerStack.getCount();
	}

	/** Loads up to {@code count} fertilizer from a player's hand; returns how many it took. */
	public int addFertilizer(int count) {
		int added = Math.min(count, MAX_FERTILIZER - fertilizer());
		if (added > 0) {
			fertilizerStack = new ItemStack(PetroItems.FERTILIZER, fertilizer() + added);
			setChanged();
		}
		return added;
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		long perPulse = WATER_PER_PULSE * FluidNetworks.DROPLETS_PER_MB;
		boolean wet = water.amount >= perPulse;
		if (state.getValue(SprinklerBlock.WET) != wet) {
			level.setBlock(pos, state.setValue(SprinklerBlock.WET, wet), Block.UPDATE_CLIENTS);
		}
		if (!wet || Math.floorMod(level.getGameTime() + pos.asLong(), PULSE_TICKS) != 0) {
			return;
		}
		water.amount -= perPulse;
		for (BlockPos at : BlockPos.betweenClosed(pos.offset(-RADIUS, -1, -RADIUS), pos.offset(RADIUS, 0, RADIUS))) {
			BlockState crop = level.getBlockState(at);
			if (crop.getBlock() instanceof CropBlock block && !block.isMaxAge(crop)) {
				crop.randomTick(level, at, level.getRandom());
			}
		}
		if (++pulses >= FERTILIZE_PULSES) {
			pulses = 0;
			if (fertilizer() > 0 && FertilizerItem.fertilize(level, pos) > 0) {
				fertilizerStack.shrink(1);
			}
		}
		setChanged();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water.readValue(input);
		int count = input.getInt("fertilizer").orElse(0);
		fertilizerStack = count > 0 ? new ItemStack(PetroItems.FERTILIZER, count) : ItemStack.EMPTY;
		pulses = input.getInt("pulses").orElse(0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		water.writeValue(output);
		output.putInt("fertilizer", fertilizer());
		output.putInt("pulses", pulses);
	}
}

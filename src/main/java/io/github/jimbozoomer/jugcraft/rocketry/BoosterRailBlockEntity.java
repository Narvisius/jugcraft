package io.github.jimbozoomer.jugcraft.rocketry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A booster rail's fuel (batch 42, docs/features/booster-rails.md): solid propellant loaded by hand or by a hopper
 * into its one slot, burnt {@value #CHARGES_PER_PROPELLANT} boosts at a time, at most {@value #MAX_CHARGES} boosts
 * held. While the rail is powered, each minecart that rolls on to it (or sits on it) without a boost already is
 * boosted ({@link BoosterRails}), using one charge.
 */
public class BoosterRailBlockEntity extends BlockEntity implements WorldlyContainer {
	public static final int BOOST_TICKS = 200;
	public static final int CHARGES_PER_PROPELLANT = 8;
	public static final int MAX_CHARGES = 64;
	private static final int[] SLOTS = {0};

	private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	private int charges;

	public BoosterRailBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftRocketry.BOOSTER_RAIL_ENTITY, pos, state);
	}

	public int charges() {
		return charges;
	}

	/** Loads propellant from a player's hand: as much as the rail has room for. */
	public void load(Player player, ItemStack stack) {
		int room = (MAX_CHARGES - charges) / CHARGES_PER_PROPELLANT;
		int used = Math.min(room, stack.getCount());
		if (used > 0) {
			charges += used * CHARGES_PER_PROPELLANT;
			if (!player.hasInfiniteMaterials()) {
				stack.shrink(used);
			}
			setChanged();
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.booster_rail.charges", charges));
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		ItemStack fuel = items.get(0);
		if (!fuel.isEmpty() && charges + CHARGES_PER_PROPELLANT <= MAX_CHARGES) {
			fuel.shrink(1);
			charges += CHARGES_PER_PROPELLANT;
			setChanged();
		}
		if (!state.getValue(PoweredRailBlock.POWERED) || charges <= 0) {
			return;
		}
		for (AbstractMinecart cart : level.getEntitiesOfClass(AbstractMinecart.class, new AABB(pos).inflate(0.2))) {
			if (BoosterRails.boosted(cart)) {
				continue;
			}
			BoosterRails.boost(level, cart, direction(level, pos, state, cart));
			charges--;
			setChanged();
			level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1.2F, 0.7F);
			if (charges <= 0) {
				break;
			}
		}
	}

	/**
	 * Which way along the rail to send {@code cart}: the way it is already going; from a standstill, uphill on a
	 * slope, else away from a solid block at one end (as a powered rail starts a cart), else south or east.
	 */
	static Vec3 direction(ServerLevel level, BlockPos pos, BlockState state, AbstractMinecart cart) {
		RailShape shape = state.getValue(PoweredRailBlock.SHAPE);
		Direction forward = switch (shape) {
			case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> Direction.EAST;
			default -> Direction.SOUTH;
		};
		Vec3 axis = new Vec3(forward.getStepX(), 0, forward.getStepZ());
		double along = cart.getDeltaMovement().dot(axis);
		if (Math.abs(along) > 0.01) {
			return along > 0 ? axis : axis.reverse();
		}
		switch (shape) {
			case ASCENDING_EAST, ASCENDING_SOUTH -> {
				return axis;
			}
			case ASCENDING_WEST, ASCENDING_NORTH -> {
				return axis.reverse();
			}
			default -> {
			}
		}
		if (level.getBlockState(pos.relative(forward)).isRedstoneConductor(level, pos.relative(forward))) {
			return axis.reverse();
		}
		return axis;
	}

	// ---------------------------------------------------------------- one fuel slot for hoppers

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return items.get(0).isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack taken = ContainerHelper.removeItem(items, slot, amount);
		setChanged();
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return stack.is(JugcraftRocketry.SOLID_PROPELLANT);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			net.minecraft.world.Containers.dropContents(level, pos, items);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(1, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		charges = input.getIntOr("charges", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("charges", charges);
	}
}

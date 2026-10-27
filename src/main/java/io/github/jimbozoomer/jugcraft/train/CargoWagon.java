package io.github.jimbozoomer.jugcraft.train;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The cargo wagon (docs/features/trains.md): a covered goods van holding {@value #SLOTS} stacks, opened by using it
 * like a chest, wherever the train stands. Its goods spill out if it is broken or comes off the line.
 */
public class CargoWagon extends TrainCar {
	public static final double LENGTH = 3.5;
	public static final float WIDTH = 1.4F;
	public static final float HEIGHT = 2.35F;
	public static final int SLOTS = 27;

	private final SimpleContainer cargo = new SimpleContainer(SLOTS) {
		@Override
		public boolean stillValid(Player player) {
			return isAlive() && player.distanceToSqr(CargoWagon.this) < 64.0;
		}
	};

	public CargoWagon(EntityType<? extends CargoWagon> type, Level level) {
		super(type, level);
	}

	@Override
	public double length() {
		return LENGTH;
	}

	@Override
	public ItemStack asItem() {
		return new ItemStack(JugcraftTrains.CARGO_WAGON_ITEM);
	}

	@Override
	public int seats() {
		return 0;
	}

	@Override
	protected Vec3 seat(int index) {
		return Vec3.ZERO;
	}

	/** The goods, for game tests and the menu. */
	public SimpleContainer cargo() {
		return cargo;
	}

	@Override
	public InteractionResult use(Player player, InteractionHand hand) {
		if (!level().isClientSide()) {
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, cargo),
					Component.translatable("entity.jugcraft.cargo_wagon")));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void dropContents(ServerLevel level) {
		Containers.dropContents(level, this, cargo);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		for (int i = 0; i < SLOTS; i++) {
			cargo.setItem(i, items.get(i));
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		for (int i = 0; i < SLOTS; i++) {
			items.set(i, cargo.getItem(i));
		}
		ContainerHelper.saveAllItems(output, items);
	}
}

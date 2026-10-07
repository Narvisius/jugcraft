package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * One length of a Harvest Feast Table: two dishes, side by side along the table, each a heap of up to
 * {@value #SERVINGS} servings of one food. It remembers who has eaten from it in the last {@value #WINDOW} ticks (in
 * memory only: a restart forgets the diners, never the dishes). A feast is worked out over the whole table, up to
 * {@value #MAX_LENGTH} lengths joined end to end ({@link #table}).
 */
public class FeastTableBlockEntity extends BlockEntity {
	public static final int DISHES = 2;
	public static final int SERVINGS = 8;
	public static final int WINDOW = 2400;
	public static final int MAX_LENGTH = 8;

	private final NonNullList<ItemStack> dishes = NonNullList.withSize(DISHES, ItemStack.EMPTY);
	private final Map<UUID, Long> diners = new HashMap<>();

	public FeastTableBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.FEAST_TABLE_ENTITY, pos, state);
	}

	public ItemStack dish(int index) {
		return dishes.get(index);
	}

	/** Whether {@code stack} can be served: anything that is eaten or drunk. */
	public static boolean food(ItemStack stack) {
		return !stack.isEmpty() && stack.has(DataComponents.FOOD);
	}

	/** Serves up to {@value #SERVINGS} of {@code stack} on dish {@code index} (empty, or holding the same food); returns how many. */
	public int serve(int index, ItemStack stack) {
		ItemStack dish = dishes.get(index);
		if (!food(stack) || !dish.isEmpty() && !ItemStack.isSameItemSameComponents(dish, stack)) {
			return 0;
		}
		int served = Math.min(stack.getCount(), SERVINGS - dish.getCount());
		if (served <= 0) {
			return 0;
		}
		if (dish.isEmpty()) {
			dishes.set(index, stack.copyWithCount(served));
		} else {
			dish.grow(served);
		}
		changed();
		return served;
	}

	/** Takes one serving from dish {@code index}, or nothing if it is empty. */
	public ItemStack takeServing(int index) {
		ItemStack serving = dishes.get(index).split(1);
		if (!serving.isEmpty()) {
			changed();
		}
		return serving;
	}

	/** Takes the whole dish back. */
	public ItemStack clear(int index) {
		ItemStack dish = dishes.get(index);
		dishes.set(index, ItemStack.EMPTY);
		if (!dish.isEmpty()) {
			changed();
		}
		return dish;
	}

	/** Remembers that {@code diner} ate here at {@code now}, forgetting anyone who last ate before the window. */
	public void ate(UUID diner, long now) {
		diners.values().removeIf(time -> now - time > WINDOW || time > now);
		diners.put(diner, now);
	}

	/** Who has eaten here within the window before {@code now}. */
	public Set<UUID> diners(long now) {
		Set<UUID> recent = new HashSet<>();
		diners.forEach((diner, time) -> {
			if (now - time <= WINDOW && time <= now) {
				recent.add(diner);
			}
		});
		return recent;
	}

	/** The whole table {@code pos} is part of: every length joined end to end along its axis, at most {@value #MAX_LENGTH}. */
	public static List<FeastTableBlockEntity> table(Level level, BlockPos pos) {
		List<FeastTableBlockEntity> lengths = new ArrayList<>();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof FeastTableBlock) || !(level.getBlockEntity(pos) instanceof FeastTableBlockEntity here)) {
			return lengths;
		}
		lengths.add(here);
		Direction.Axis axis = state.getValue(FeastTableBlock.AXIS);
		for (Direction way : new Direction[] {Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE),
				Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE)}) {
			BlockPos next = pos.relative(way);
			while (lengths.size() < MAX_LENGTH && FeastTableBlock.joins(state, level.getBlockState(next))
					&& level.getBlockEntity(next) instanceof FeastTableBlockEntity length) {
				lengths.add(length);
				next = next.relative(way);
			}
		}
		return lengths;
	}

	/** How many different foods are served across {@code table}. */
	public static int variety(List<FeastTableBlockEntity> table) {
		Set<Item> foods = new HashSet<>();
		for (FeastTableBlockEntity length : table) {
			for (ItemStack dish : length.dishes) {
				if (!dish.isEmpty()) {
					foods.add(dish.getItem());
				}
			}
		}
		return foods.size();
	}

	/** Who has eaten anywhere at {@code table} within the window before {@code now}. */
	public static Set<UUID> diners(List<FeastTableBlockEntity> table, long now) {
		Set<UUID> all = new HashSet<>();
		for (FeastTableBlockEntity length : table) {
			all.addAll(length.diners(now));
		}
		return all;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			Containers.dropContents(level, pos, dishes);
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		List<ItemStack> saved = input.read("dishes", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
		for (int i = 0; i < DISHES; i++) {
			dishes.set(i, i < saved.size() ? saved.get(i) : ItemStack.EMPTY);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("dishes", ItemStack.OPTIONAL_CODEC.listOf(), new ArrayList<>(dishes));
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}

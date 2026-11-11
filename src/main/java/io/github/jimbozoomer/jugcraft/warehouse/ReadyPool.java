package io.github.jimbozoomer.jugcraft.warehouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Ready Storage's contents (part W2), one pool for every Ready Storage of a warehouse (kept by its terminal): slots of
 * 108 a module, each holding a stack for the player whose order brought it (or nobody's: an empty pallet), tagged with
 * the order. Players take from it at the Goods Out window; nothing is put in by hand.
 */
public final class ReadyPool {
	public static final int MODULE = 108;
	private final List<ItemStack> stacks = new ArrayList<>();
	private final List<@Nullable UUID> owners = new ArrayList<>();
	private final List<Integer> orders = new ArrayList<>();
	private int capacity;

	public int capacity() {
		return capacity;
	}

	/** The slots the warehouse's Ready Storage has now (what is already in slots past it stays till taken). */
	public void setCapacity(int capacity) {
		this.capacity = capacity;
		while (stacks.size() < capacity) {
			stacks.add(ItemStack.EMPTY);
			owners.add(null);
			orders.add(0);
		}
		// Empty slots past the capacity go.
		for (int i = stacks.size() - 1; i >= capacity && stacks.get(i).isEmpty(); i--) {
			stacks.remove(i);
			owners.remove(i);
			orders.remove(i);
		}
	}

	public int size() {
		return stacks.size();
	}

	public ItemStack get(int i) {
		return stacks.get(i);
	}

	public @Nullable UUID owner(int i) {
		return owners.get(i);
	}

	public int order(int i) {
		return orders.get(i);
	}

	public void set(int i, ItemStack stack) {
		stacks.set(i, stack);
		if (stack.isEmpty()) {
			owners.set(i, null);
			orders.set(i, 0);
		}
	}

	public int used() {
		int n = 0;
		for (ItemStack s : stacks) {
			if (!s.isEmpty()) {
				n++;
			}
		}
		return n;
	}

	public boolean hasOrder(int id) {
		for (int i = 0; i < stacks.size(); i++) {
			if (orders.get(i) == id && !stacks.get(i).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public boolean mine(int i, Player player) {
		UUID o = owners.get(i);
		return o != null && o.equals(player.getUUID());
	}

	/** How many of an item would go in for an order (topping up its own stacks, then empty slots). */
	public long room(ItemStack item, int order) {
		long room = 0;
		int max = Math.max(1, item.getMaxStackSize());
		for (int i = 0; i < Math.min(capacity, stacks.size()); i++) {
			ItemStack s = stacks.get(i);
			if (s.isEmpty()) {
				room += max;
			} else if (orders.get(i) == order && ItemStack.isSameItemSameComponents(s, item)) {
				room += Math.max(0, max - s.getCount());
			}
		}
		return room;
	}

	/** Puts up to n of an item in for an order; returns how many went in. */
	public long add(ItemStack item, long n, @Nullable UUID owner, int order) {
		long left = n;
		int max = Math.max(1, item.getMaxStackSize());
		for (int pass = 0; pass < 2 && left > 0; pass++) {
			for (int i = 0; i < Math.min(capacity, stacks.size()) && left > 0; i++) {
				ItemStack s = stacks.get(i);
				if (pass == 0 && !s.isEmpty() && orders.get(i) == order && ItemStack.isSameItemSameComponents(s, item) && s.getCount() < max) {
					int k = (int) Math.min(left, max - s.getCount());
					s.grow(k);
					left -= k;
				} else if (pass == 1 && s.isEmpty()) {
					int k = (int) Math.min(left, max);
					stacks.set(i, item.copyWithCount(k));
					owners.set(i, owner);
					orders.set(i, order);
					left -= k;
				}
			}
		}
		return n - left;
	}

	/** Everything in it (to drop when its terminal goes). */
	public List<ItemStack> drain() {
		List<ItemStack> out = new ArrayList<>();
		for (int i = 0; i < stacks.size(); i++) {
			if (!stacks.get(i).isEmpty()) {
				out.add(stacks.get(i));
			}
			set(i, ItemStack.EMPTY);
		}
		return out;
	}

	private record Slot(int index, ItemStack stack, Optional<UUID> owner, int order) {
		static final Codec<Slot> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.INT.fieldOf("i").forGetter(Slot::index),
				ItemStack.CODEC.fieldOf("stack").forGetter(Slot::stack), UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Slot::owner),
				Codec.INT.fieldOf("order").forGetter(Slot::order)).apply(i, Slot::new));
	}

	public static final Codec<ReadyPool> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("capacity").forGetter(ReadyPool::capacity),
			Slot.CODEC.listOf().fieldOf("slots").forGetter(p -> {
				List<Slot> out = new ArrayList<>();
				for (int k = 0; k < p.stacks.size(); k++) {
					if (!p.stacks.get(k).isEmpty()) {
						out.add(new Slot(k, p.stacks.get(k), Optional.ofNullable(p.owners.get(k)), p.orders.get(k)));
					}
				}
				return out;
			})).apply(i, (capacity, slots) -> {
				ReadyPool p = new ReadyPool();
				int size = capacity;
				for (Slot s : slots) {
					size = Math.max(size, s.index() + 1);
				}
				p.setCapacity(size);
				for (Slot s : slots) {
					p.stacks.set(s.index(), s.stack());
					p.owners.set(s.index(), s.owner().orElse(null));
					p.orders.set(s.index(), s.order());
				}
				p.capacity = capacity;
				return p;
			}));

	/**
	 * A window onto the pool for a Goods Out screen: its slots are the pool's slots that pass the screen's filter
	 * (mine, a search), from an offset; taking from one takes from the pool. Nothing can be put in.
	 */
	public static final class View implements Container {
		private final int size;
		private @Nullable ReadyPool pool;
		private final int[] index;
		/** What the client shows (it has no pool: the server's slot updates land here). */
		private final ItemStack[] local;
		private Runnable dirty = () -> {
		};

		public View(int size) {
			this.size = size;
			this.index = new int[size];
			java.util.Arrays.fill(index, -1);
			this.local = new ItemStack[size];
			java.util.Arrays.fill(local, ItemStack.EMPTY);
		}

		/** Points the view at the pool's slots (index[k], -1 for none). */
		public void show(ReadyPool pool, int[] slots, Runnable dirty) {
			this.pool = pool;
			this.dirty = dirty;
			for (int k = 0; k < size; k++) {
				index[k] = k < slots.length ? slots[k] : -1;
			}
		}

		public int poolIndex(int k) {
			return index[k];
		}

		private boolean valid(int k) {
			return pool != null && k >= 0 && k < size && index[k] >= 0 && index[k] < pool.size();
		}

		@Override
		public int getContainerSize() {
			return size;
		}

		@Override
		public boolean isEmpty() {
			for (int k = 0; k < size; k++) {
				if (!getItem(k).isEmpty()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public ItemStack getItem(int k) {
			if (pool == null) {
				return k >= 0 && k < size ? local[k] : ItemStack.EMPTY;
			}
			return valid(k) ? pool.get(index[k]) : ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItem(int k, int count) {
			if (pool == null) {
				return k >= 0 && k < size ? local[k].split(count) : ItemStack.EMPTY;
			}
			if (!valid(k)) {
				return ItemStack.EMPTY;
			}
			ItemStack s = pool.get(index[k]);
			ItemStack out = s.split(count);
			if (s.isEmpty()) {
				pool.set(index[k], ItemStack.EMPTY);
			}
			setChanged();
			return out;
		}

		@Override
		public ItemStack removeItemNoUpdate(int k) {
			if (pool == null) {
				if (k < 0 || k >= size) {
					return ItemStack.EMPTY;
				}
				ItemStack s = local[k];
				local[k] = ItemStack.EMPTY;
				return s;
			}
			if (!valid(k)) {
				return ItemStack.EMPTY;
			}
			ItemStack s = pool.get(index[k]);
			pool.set(index[k], ItemStack.EMPTY);
			setChanged();
			return s;
		}

		@Override
		public void setItem(int k, ItemStack stack) {
			if (pool == null) {
				if (k >= 0 && k < size) {
					local[k] = stack;
				}
				return;
			}
			if (valid(k)) {
				// Only ever what was taken coming back (a cancelled drag): put it back as it was.
				if (stack.isEmpty()) {
					pool.set(index[k], ItemStack.EMPTY);
				} else {
					pool.stacks.set(index[k], stack);
				}
				setChanged();
			}
		}

		@Override
		public void setChanged() {
			dirty.run();
		}

		@Override
		public boolean stillValid(Player player) {
			return true;
		}

		@Override
		public void clearContent() {
		}
	}
}

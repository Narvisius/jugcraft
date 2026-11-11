package io.github.jimbozoomer.jugcraft.warehouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * The Output Port's buffer (part W2): what goes into the pipe next, nine stacks, each tagged with the order (or a
 * stream's refill) that brought it. The ports push from it into the pipes at their flanges; extractors may pull from
 * it too. Nothing goes in from outside.
 */
public final class PortBuffer {
	public static final int SLOTS = 9;
	private final SimpleContainer items = new SimpleContainer(SLOTS);
	private final int[] tags = new int[SLOTS];
	private final Storage<ItemVariant> storage = ContainerStorage.of(items, null);

	public ItemStack get(int i) {
		return items.getItem(i);
	}

	public int tag(int i) {
		return tags[i];
	}

	public boolean has(int tag) {
		for (int i = 0; i < SLOTS; i++) {
			if (tags[i] == tag && !items.getItem(i).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public long count(ItemStack item) {
		long n = 0;
		for (int i = 0; i < SLOTS; i++) {
			if (ItemStack.isSameItemSameComponents(items.getItem(i), item)) {
				n += items.getItem(i).getCount();
			}
		}
		return n;
	}

	public int used() {
		int n = 0;
		for (int i = 0; i < SLOTS; i++) {
			if (!items.getItem(i).isEmpty()) {
				n++;
			}
		}
		return n;
	}

	public long room(ItemStack item, int tag) {
		long room = 0;
		int max = Math.max(1, item.getMaxStackSize());
		for (int i = 0; i < SLOTS; i++) {
			ItemStack s = items.getItem(i);
			if (s.isEmpty()) {
				room += max;
			} else if (tags[i] == tag && ItemStack.isSameItemSameComponents(s, item)) {
				room += Math.max(0, max - s.getCount());
			}
		}
		return room;
	}

	public long add(ItemStack item, long n, int tag) {
		long left = n;
		int max = Math.max(1, item.getMaxStackSize());
		for (int pass = 0; pass < 2 && left > 0; pass++) {
			for (int i = 0; i < SLOTS && left > 0; i++) {
				ItemStack s = items.getItem(i);
				if (pass == 0 && !s.isEmpty() && tags[i] == tag && ItemStack.isSameItemSameComponents(s, item) && s.getCount() < max) {
					int k = (int) Math.min(left, max - s.getCount());
					s.grow(k);
					left -= k;
				} else if (pass == 1 && s.isEmpty()) {
					int k = (int) Math.min(left, max);
					items.setItem(i, item.copyWithCount(k));
					tags[i] = tag;
					left -= k;
				}
			}
		}
		return n - left;
	}

	/** After items left (pushed or pulled): emptied slots lose their tags. */
	public void tidy() {
		for (int i = 0; i < SLOTS; i++) {
			if (items.getItem(i).isEmpty()) {
				tags[i] = 0;
			}
		}
	}

	/** The buffer for pipes and extractors: items may only be taken out (those allowed, for the port's push). */
	public Storage<ItemVariant> output(Predicate<ItemVariant> allowed) {
		return new FilteringStorage<>(storage) {
			@Override
			protected boolean canInsert(ItemVariant resource) {
				return false;
			}

			@Override
			protected boolean canExtract(ItemVariant resource) {
				return allowed.test(resource);
			}
		};
	}

	public List<ItemStack> stacks() {
		List<ItemStack> out = new ArrayList<>();
		for (int i = 0; i < SLOTS; i++) {
			out.add(items.getItem(i).copy());
		}
		return out;
	}

	public List<ItemStack> drain() {
		List<ItemStack> out = new ArrayList<>();
		for (int i = 0; i < SLOTS; i++) {
			if (!items.getItem(i).isEmpty()) {
				out.add(items.getItem(i));
			}
			items.setItem(i, ItemStack.EMPTY);
			tags[i] = 0;
		}
		return out;
	}

	private record Slot(int index, ItemStack stack, int tag) {
		static final Codec<Slot> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.INT.fieldOf("i").forGetter(Slot::index),
				ItemStack.CODEC.fieldOf("stack").forGetter(Slot::stack), Codec.INT.fieldOf("tag").forGetter(Slot::tag)).apply(i, Slot::new));
	}

	public static final Codec<PortBuffer> CODEC = Slot.CODEC.listOf().xmap(slots -> {
		PortBuffer b = new PortBuffer();
		for (Slot s : slots) {
			if (s.index() >= 0 && s.index() < SLOTS) {
				b.items.setItem(s.index(), s.stack());
				b.tags[s.index()] = s.tag();
			}
		}
		return b;
	}, b -> {
		List<Slot> out = new ArrayList<>();
		for (int i = 0; i < SLOTS; i++) {
			if (!b.items.getItem(i).isEmpty()) {
				out.add(new Slot(i, b.items.getItem(i), b.tags[i]));
			}
		}
		return out;
	});
}

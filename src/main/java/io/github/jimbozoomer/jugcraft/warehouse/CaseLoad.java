package io.github.jimbozoomer.jugcraft.warehouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * One case: one kind of item (item, its count 1: the kind with its components), count of them, up to four stacks'
 * worth (the Intake Case Packer packs no more). Part W4: or a drum of a liquid (item a book drum, Drums.book; count
 * the millibuckets in it, 0 once it has been emptied).
 */
public record CaseLoad(ItemStack item, int count) {
	public static final Codec<CaseLoad> CODEC = RecordCodecBuilder.create(i -> i.group(
			ItemStack.CODEC.fieldOf("item").forGetter(CaseLoad::item),
			Codec.INT.fieldOf("count").forGetter(CaseLoad::count)).apply(i, CaseLoad::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, CaseLoad> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC, CaseLoad::item, ByteBufCodecs.VAR_INT, CaseLoad::count, CaseLoad::new);

	public CaseLoad {
		item = item.copyWithCount(1);
	}

	/** How many of an item one case holds: four stacks (a drum of a liquid, part W4: its millibuckets). */
	public static int capacity(ItemStack item) {
		return Drums.isLiquid(item) ? (int) Drums.CAPACITY : item.getMaxStackSize() * 4;
	}

	public CaseKind kind() {
		return CaseKind.of(item);
	}

	public boolean sameItem(ItemStack other) {
		return ItemStack.isSameItemSameComponents(item, other);
	}

	/** The case's contents as stacks (to drop or hand out); a drum of a liquid: the drum, its liquid in it. */
	public java.util.List<ItemStack> stacks() {
		java.util.List<ItemStack> out = new java.util.ArrayList<>();
		if (Drums.isLiquid(item)) {
			out.add(Drums.dropped(this));
			return out;
		}
		int left = count, max = Math.max(1, item.getMaxStackSize());
		while (left > 0) {
			int n = Math.min(left, max);
			out.add(item.copyWithCount(n));
			left -= n;
		}
		return out;
	}
}

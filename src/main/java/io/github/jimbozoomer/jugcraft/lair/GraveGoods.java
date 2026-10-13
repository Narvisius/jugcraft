package io.github.jimbozoomer.jugcraft.lair;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * What a player dropped dying in a lair (their items and experience), gathered up at once and kept with the player
 * (across their death, logouts and restarts) until they respawn, outside, and are handed it back.
 */
public record GraveGoods(List<ItemStack> items, int experience) {
	public static final Codec<GraveGoods> CODEC = RecordCodecBuilder.create(i -> i.group(
			ItemStack.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(GraveGoods::items),
			Codec.INT.optionalFieldOf("experience", 0).forGetter(GraveGoods::experience)).apply(i, GraveGoods::new));

	/** These goods and {@code more} together (a second death before the first goods were handed back). */
	public GraveGoods plus(List<ItemStack> more, int moreExperience) {
		List<ItemStack> all = new ArrayList<>(items);
		all.addAll(more);
		return new GraveGoods(List.copyOf(all), experience + moreExperience);
	}

	public int count() {
		return items.stream().mapToInt(ItemStack::getCount).sum();
	}
}

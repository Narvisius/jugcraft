package io.github.jimbozoomer.jugcraft.machine.form;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A protected place for one reusable tool: a catalyst bed, pattern, mold or die. A tool is paid for once, saved
 * once and never used up by the work it enables. A player or an assigned companion can replace an idle tool;
 * pipes and hoppers cannot access sockets, nor can a companion replace a tool used by a running batch.
 *
 * @param name a stable name, shown as the socket's label
 * @param accepts the items this socket holds
 */
public record ToolSocket(String name, TagKey<Item> accepts) {
	public boolean accepts(ItemStack stack) {
		return !stack.isEmpty() && stack.is(accepts);
	}
}

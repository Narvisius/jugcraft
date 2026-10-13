package io.github.jimbozoomer.jugcraft.machine.form;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A protected place for one reusable tool: a catalyst bed, pattern, mold or die. A tool is paid for once, saved
 * once and never used up by the work it enables. Only a player at the machine's screen can put one in or take it
 * out, never pipes or hoppers, and not while a batch that needs it is running.
 *
 * @param name a stable name, shown as the socket's label
 * @param accepts the items this socket holds
 */
public record ToolSocket(String name, TagKey<Item> accepts) {
	public boolean accepts(ItemStack stack) {
		return !stack.isEmpty() && stack.is(accepts);
	}
}

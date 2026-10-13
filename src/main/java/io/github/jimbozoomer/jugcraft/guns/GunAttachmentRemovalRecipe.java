package io.github.jimbozoomer.jugcraft.guns;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Taking an attachment off (recipe type {@code jugcraft:gun_attachment_removal}): a gun with attachments and shears,
 * alone in a crafting grid, give the gun without the attachment fitted last; that attachment stays in the grid where the
 * gun lay, and the shears are kept. Rounds beyond the gun's own magazine stay loaded until they are fired.
 */
public class GunAttachmentRemovalRecipe extends CustomRecipe {
	public static final GunAttachmentRemovalRecipe INSTANCE = new GunAttachmentRemovalRecipe();
	public static final MapCodec<GunAttachmentRemovalRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
	public static final StreamCodec<RegistryFriendlyByteBuf, GunAttachmentRemovalRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
	public static final RecipeSerializer<GunAttachmentRemovalRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	/** Where the gun lies in the grid, if it holds one gun with an attachment and one pair of shears; otherwise -1. */
	private static int gunSlot(CraftingInput input) {
		int gun = -1;
		boolean shears = false;
		for (int i = 0; i < input.size(); i++) {
			ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (stack.getItem() instanceof GunItem && gun < 0 && !GunItem.attachments(stack).isEmpty()) {
				gun = i;
			} else if (stack.is(Items.SHEARS) && !shears) {
				shears = true;
			} else {
				return -1;
			}
		}
		return shears ? gun : -1;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return gunSlot(input) >= 0;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		int slot = gunSlot(input);
		if (slot < 0) {
			return ItemStack.EMPTY;
		}
		ItemStack gun = input.getItem(slot).copyWithCount(1);
		List<String> fitted = GunItem.attachments(gun);
		GunItem.remove(gun, fitted.get(fitted.size() - 1));
		return gun;
	}

	/** The attachment taken off stays in the grid where the gun lay; the shears are kept. */
	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
		NonNullList<ItemStack> left = NonNullList.withSize(input.size(), ItemStack.EMPTY);
		int slot = gunSlot(input);
		if (slot >= 0) {
			List<String> fitted = GunItem.attachments(input.getItem(slot));
			left.set(slot, new ItemStack(JugcraftGuns.ATTACHMENT_ITEMS.get(fitted.get(fitted.size() - 1))));
			for (int i = 0; i < input.size(); i++) {
				if (input.getItem(i).is(Items.SHEARS)) {
					left.set(i, input.getItem(i).copyWithCount(1));
				}
			}
		}
		return left;
	}

	@Override
	public RecipeSerializer<GunAttachmentRemovalRecipe> getSerializer() {
		return SERIALIZER;
	}
}

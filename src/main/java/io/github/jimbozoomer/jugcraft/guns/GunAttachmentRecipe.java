package io.github.jimbozoomer.jugcraft.guns;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Fitting an attachment (recipe type {@code jugcraft:gun_attachment}): a gun and one attachment it takes, alone in a
 * crafting grid, give the gun with the attachment fitted, its loaded rounds kept. An attachment the gun already wore in
 * that slot comes off and stays in the grid where the new one lay. Nothing else may be in the grid.
 */
public class GunAttachmentRecipe extends CustomRecipe {
	public static final GunAttachmentRecipe INSTANCE = new GunAttachmentRecipe();
	public static final MapCodec<GunAttachmentRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
	public static final StreamCodec<RegistryFriendlyByteBuf, GunAttachmentRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
	public static final RecipeSerializer<GunAttachmentRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	/** The grid's gun, the attachment and where the attachment lies. */
	private record Fitting(ItemStack gun, String attachment, int slot) {
	}

	/** Null unless the grid holds just a gun and an attachment it takes and does not wear yet. */
	private static @Nullable Fitting fitting(CraftingInput input) {
		ItemStack gun = ItemStack.EMPTY;
		String attachment = null;
		int slot = -1;
		for (int i = 0; i < input.size(); i++) {
			ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (stack.getItem() instanceof GunItem && gun.isEmpty()) {
				gun = stack;
			} else if (stack.getItem() instanceof AttachmentItem item && attachment == null) {
				attachment = item.name();
				slot = i;
			} else {
				return null;
			}
		}
		if (gun.isEmpty() || attachment == null || !GunItem.takes(gun, attachment) || GunItem.attachments(gun).contains(attachment)) {
			return null;
		}
		return new Fitting(gun, attachment, slot);
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return fitting(input) != null;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		Fitting fitting = fitting(input);
		if (fitting == null) {
			return ItemStack.EMPTY;
		}
		ItemStack gun = fitting.gun().copyWithCount(1);
		GunItem.fit(gun, fitting.attachment());
		return gun;
	}

	/** The attachment that came off (if the slot held one) stays in the grid. */
	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
		NonNullList<ItemStack> left = NonNullList.withSize(input.size(), ItemStack.EMPTY);
		Fitting fitting = fitting(input);
		if (fitting != null) {
			String old = GunItem.inSlot(fitting.gun(), JugcraftGuns.ATTACHMENTS.get(fitting.attachment()).slot());
			if (old != null) {
				left.set(fitting.slot(), new ItemStack(JugcraftGuns.ATTACHMENT_ITEMS.get(old)));
			}
		}
		return left;
	}

	@Override
	public RecipeSerializer<GunAttachmentRecipe> getSerializer() {
		return SERIALIZER;
	}
}

package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Copying a Skeleton Key (recipe type {@code jugcraft:key_copying}): one cut key and one to {@value #MAX_COPIES} Key
 * Blanks in a crafting grid give that many copies of the key, wards, name and all, and the key itself stays in the grid,
 * as a written book does when it is copied. Nothing else may be in the grid.
 */
public class KeyCopyingRecipe extends CustomRecipe {
	public static final int MAX_COPIES = 8;
	public static final KeyCopyingRecipe INSTANCE = new KeyCopyingRecipe();
	public static final MapCodec<KeyCopyingRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
	public static final StreamCodec<RegistryFriendlyByteBuf, KeyCopyingRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
	public static final RecipeSerializer<KeyCopyingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	/** The cut key in {@code input}, if the grid holds exactly one and one to eight blanks; otherwise empty. */
	private static ItemStack original(CraftingInput input) {
		ItemStack key = ItemStack.EMPTY;
		int blanks = 0;
		for (int i = 0; i < input.size(); i++) {
			ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (SkeletonKeyItem.wards(stack) != 0) {
				if (!key.isEmpty()) {
					return ItemStack.EMPTY;
				}
				key = stack;
			} else if (stack.is(JugcraftAgriculture.item(JugcraftAgriculture.KEY_BLANK))) {
				blanks++;
			} else {
				return ItemStack.EMPTY;
			}
		}
		return blanks >= 1 && blanks <= MAX_COPIES ? key : ItemStack.EMPTY;
	}

	private static int blanks(CraftingInput input) {
		int blanks = 0;
		for (int i = 0; i < input.size(); i++) {
			if (input.getItem(i).is(JugcraftAgriculture.item(JugcraftAgriculture.KEY_BLANK))) {
				blanks++;
			}
		}
		return blanks;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return !original(input).isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		ItemStack key = original(input);
		return key.isEmpty() ? ItemStack.EMPTY : key.copyWithCount(blanks(input));
	}

	/** The key that was copied stays in the grid. */
	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
		NonNullList<ItemStack> left = NonNullList.withSize(input.size(), ItemStack.EMPTY);
		for (int i = 0; i < input.size(); i++) {
			if (SkeletonKeyItem.wards(input.getItem(i)) != 0) {
				left.set(i, input.getItem(i).copyWithCount(1));
			}
		}
		return left;
	}

	@Override
	public RecipeSerializer<KeyCopyingRecipe> getSerializer() {
		return SERIALIZER;
	}
}

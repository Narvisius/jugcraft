package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Cooking Pot's contents and cooking. Every tick (server only) it checks the one block under it
 * for heat; with heat and a {@link CookingPotRecipe} in its ingredient slots it cooks, and after the
 * recipe's time it takes one batch of ingredients and adds the meal to a result slot. Without
 * heat, progress falls back {@value #COOLING} per tick, like a furnace going out. The recipe is looked
 * up again only when the slots change.
 * <p>
 * Hoppers and pipes put ingredients in from the top and sides and take meals out from the bottom.
 */
public class CookingPotBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<BlockPos> {
	/** What heats the pot from directly below. Keep in sync with HEAT_TAG in tools/agriculture.py. */
	public static final TagKey<Block> HEAT_SOURCES = TagKey.create(Registries.BLOCK, Jugcraft.id("heat_sources"));
	/**
	 * Ingredient slots, then result slots: four, because soups do not stack and a pot should cook several
	 * bowls in a row. Keep in sync with POT_INPUTS and POT_OUTPUTS in tools/agriculture.py.
	 */
	public static final int INPUTS = 6;
	public static final int OUTPUTS = 4;
	/** The first result slot. */
	public static final int RESULT = INPUTS;
	public static final int SLOTS = INPUTS + OUTPUTS;
	/** Progress lost per tick without heat. Keep in sync with POT_COOLING in tools/agriculture.py. */
	public static final int COOLING = 2;

	public static final int DATA_PROGRESS = 0;
	public static final int DATA_TIME = 1;
	public static final int DATA_HEAT = 2;
	public static final int DATA_COUNT = 3;

	private static final int[] INPUT_SLOTS = {0, 1, 2, 3, 4, 5};
	private static final int[] RESULT_SLOTS = {6, 7, 8, 9};

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private int progress;
	private int time;
	private boolean heated;
	/** The slots changed since the recipe was last looked up. */
	private boolean recheck = true;
	/** Just loaded from disk: the first lookup keeps the saved progress. */
	private boolean loaded;
	private @Nullable CookingPotRecipe recipe;
	/**
	 * What the input slots held at the last tick, to notice changes made without setItem: hoppers and shift-clicks
	 * top up a stack in place.
	 */
	private final @Nullable Item[] seenItems = new Item[INPUTS];
	private final int[] seenCounts = new int[INPUTS];

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case DATA_PROGRESS -> progress;
				case DATA_TIME -> time;
				case DATA_HEAT -> heated ? 1 : 0;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
		}

		@Override
		public int getCount() {
			return DATA_COUNT;
		}
	};

	public CookingPotBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.COOKING_POT_ENTITY, pos, state);
	}

	/** Whether the block under {@code pos} heats a pot: in {@link #HEAT_SOURCES}, and lit if it can be unlit (campfires). */
	public static boolean isHeated(Level level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return below.is(HEAT_SOURCES) && (!below.hasProperty(BlockStateProperties.LIT) || below.getValue(BlockStateProperties.LIT));
	}

	public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		heated = isHeated(level, pos);
		if (inputsChanged()) {
			recheck = true;
		}
		if (recheck) {
			recheck = false;
			CookingPotRecipe found = CookingPotRecipe.find(level.getServer(), items.subList(0, INPUTS))
					.map(CookingPotRecipe.Match::recipe).orElse(null);
			if (found != recipe) {
				recipe = found;
				progress = loaded && found != null ? progress : 0;
			}
			loaded = false;
			time = recipe == null ? 0 : recipe.time();
		}
		boolean cooking = heated && recipe != null && resultSlotFor(recipe.output().create()) >= 0;
		if (cooking) {
			progress++;
			if (progress >= time) {
				progress = 0;
				cook(level, pos, recipe);
			}
			setChanged();
		} else if (progress > 0) {
			progress = Math.max(0, progress - COOLING);
			setChanged();
		}
		if (state.getValue(CookingPotBlock.COOKING) != cooking) {
			level.setBlock(pos, state.setValue(CookingPotBlock.COOKING, cooking), Block.UPDATE_ALL);
		}
	}

	/** The meal a recipe serves at {@code now}: a jar of preserves is stamped with the time it was cooked (it spoils from then). */
	private static ItemStack served(ItemStackTemplate output, long now) {
		ItemStack meal = output.create();
		if (meal.getItem() instanceof PreserveJarItem) {
			PreserveJarItem.cooked(meal, now);
		}
		return meal;
	}

	/** Whether the input slots changed since the last tick (and remembers them). */
	private boolean inputsChanged() {
		boolean changed = false;
		for (int slot = 0; slot < INPUTS; slot++) {
			ItemStack stack = items.get(slot);
			Item item = stack.isEmpty() ? null : stack.getItem();
			if (item != seenItems[slot] || stack.getCount() != seenCounts[slot]) {
				seenItems[slot] = item;
				seenCounts[slot] = stack.getCount();
				changed = true;
			}
		}
		return changed;
	}

	/** A result slot that can take the whole meal: one already holding it with room, else an empty one, else -1. */
	private int resultSlotFor(ItemStack meal) {
		int empty = -1;
		for (int slot = RESULT; slot < SLOTS; slot++) {
			ItemStack result = items.get(slot);
			if (result.isEmpty()) {
				empty = empty < 0 ? slot : empty;
			} else if (sameMeal(result, meal) && result.getCount() + meal.getCount() <= result.getMaxStackSize()) {
				return slot;
			}
		}
		return empty;
	}

	/**
	 * Whether two meals stack in a result slot: the same item and components, or jars of the same preserve cooked at
	 * different times. Jars added to a stack take on the stack's (earlier) time, so none comes out fresher.
	 */
	private static boolean sameMeal(ItemStack result, ItemStack meal) {
		if (result.getItem() instanceof PreserveJarItem && meal.is(result.getItem())) {
			return PreserveJarItem.sealed(result) == PreserveJarItem.sealed(meal)
					&& PreserveJarItem.servings(result) == PreserveJarItem.servings(meal);
		}
		return ItemStack.isSameItemSameComponents(result, meal);
	}

	/** Puts a container left from an ingredient (a bucket, a bottle) where automation can take it: the result slots. */
	private boolean storeLeftover(ItemStack leftover) {
		for (int slot = RESULT; slot < SLOTS; slot++) {
			ItemStack result = items.get(slot);
			if (!result.isEmpty() && ItemStack.isSameItemSameComponents(result, leftover)
					&& result.getCount() + leftover.getCount() <= result.getMaxStackSize()) {
				result.grow(leftover.getCount());
				return true;
			}
		}
		for (int slot = RESULT; slot < SLOTS; slot++) {
			if (items.get(slot).isEmpty()) {
				items.set(slot, leftover);
				return true;
			}
		}
		return false;
	}

	/** Takes one batch of ingredients (leaving containers such as buckets behind) and adds the meal. */
	private void cook(ServerLevel level, BlockPos pos, CookingPotRecipe cooked) {
		Optional<CookingPotRecipe.Match> match = CookingPotRecipe.find(level.getServer(), items.subList(0, INPUTS));
		if (match.isEmpty() || match.get().recipe() != cooked) {
			recheck = true;
			return;
		}
		ItemStack meal = served(cooked.output(), level.getGameTime());
		int slot = resultSlotFor(meal);
		if (slot < 0) {
			recheck = true;
			return;
		}
		int[] take = match.get().take();
		List<ItemStack> leftovers = new ArrayList<>();
		for (int input = 0; input < INPUTS; input++) {
			if (take[input] == 0) {
				continue;
			}
			ItemStack stack = items.get(input);
			ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
			stack.shrink(take[input]);
			for (int n = 0; remainder != null && n < take[input]; n++) {
				leftovers.add(remainder.create());
			}
		}
		if (items.get(slot).isEmpty()) {
			items.set(slot, meal);
		} else {
			items.get(slot).grow(meal.getCount());
		}
		// Containers go to the result slots, never back among the ingredients, where they would stop the next batch.
		for (ItemStack leftover : leftovers) {
			if (!storeLeftover(leftover)) {
				Block.popResource(level, pos.above(), leftover);
			}
		}
		level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
		recheck = true;
	}

	/** 0 when the result slots are empty, up to 15 when they are full, like a container's comparator reading. */
	public int comparatorSignal() {
		float fullness = 0.0F;
		boolean any = false;
		for (int slot = RESULT; slot < SLOTS; slot++) {
			ItemStack result = items.get(slot);
			if (!result.isEmpty()) {
				fullness += (float) result.getCount() / result.getMaxStackSize();
				any = true;
			}
		}
		return any ? 1 + (int) (fullness / OUTPUTS * 14.0F) : 0;
	}

	// ---------------------------------------------------------------- inventory

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jugcraft.cooking_pot");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
		recheck = true;
	}

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		super.setItem(slot, stack);
		recheck = true;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		recheck = true;
		return super.removeItem(slot, amount);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		recheck = true;
		return super.removeItemNoUpdate(slot);
	}

	/** Ingredient slots take only items some pot recipe uses; only the server knows the recipes. */
	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot < INPUTS && (!(level instanceof ServerLevel server) || CookingPotRecipe.isIngredient(server.getServer(), stack));
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? RESULT_SLOTS : INPUT_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side != Direction.DOWN && canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN && slot >= RESULT;
	}

	// ---------------------------------------------------------------- menu

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new CookingPotMenu(containerId, inventory, this, data);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return worldPosition;
	}

	// ---------------------------------------------------------------- saving

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		progress = input.getInt("progress").orElse(0);
		recheck = true;
		loaded = true;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("progress", progress);
	}
}

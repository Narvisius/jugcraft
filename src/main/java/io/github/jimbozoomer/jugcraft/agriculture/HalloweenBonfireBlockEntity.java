package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Halloween Bonfire's skewers: up to {@value #SLOTS} foods, each cooking by the campfire recipe that takes it (data
 * driven, vanilla's {@code campfire_cooking}) in its time divided by {@value #SPEED}; when one is done it pops off
 * above the fire. Saved, and sent to clients for the skewers to show; breaking the bonfire drops what is on them.
 */
public class HalloweenBonfireBlockEntity extends BlockEntity {
	public static final int SLOTS = 4;
	public static final int SPEED = 2;
	private static final RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> RECIPES =
			RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);

	private record Skewer(int slot, ItemStack item, int progress, int time) {
		static final Codec<Skewer> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("slot").forGetter(Skewer::slot),
				ItemStack.CODEC.fieldOf("item").forGetter(Skewer::item),
				Codec.INT.fieldOf("progress").forGetter(Skewer::progress),
				Codec.INT.fieldOf("time").forGetter(Skewer::time)).apply(i, Skewer::new));
	}

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private final int[] progress = new int[SLOTS];
	private final int[] time = new int[SLOTS];

	public HalloweenBonfireBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.BONFIRE_ENTITY, pos, state);
	}

	public List<ItemStack> items() {
		return List.copyOf(items);
	}

	/** Whether {@code stack} looks like something to cook (the client can't check recipes; the server decides). */
	public boolean cookable(ItemStack stack) {
		return !stack.isEmpty() && freeSlot() >= 0;
	}

	private int freeSlot() {
		for (int slot = 0; slot < SLOTS; slot++) {
			if (items.get(slot).isEmpty()) {
				return slot;
			}
		}
		return -1;
	}

	private static Optional<RecipeHolder<CampfireCookingRecipe>> recipe(ServerLevel level, ItemStack stack) {
		return RECIPES.getRecipeFor(new SingleRecipeInput(stack), level);
	}

	/** Puts one of {@code stack} on a free skewer if a campfire recipe takes it; returns whether it did. */
	public boolean cook(ServerLevel level, ItemStack stack, Player player) {
		if (stack.isEmpty()) {
			return false;
		}
		int slot = freeSlot();
		if (slot < 0) {
			return false;
		}
		Optional<RecipeHolder<CampfireCookingRecipe>> recipe = recipe(level, stack);
		if (recipe.isEmpty()) {
			return false;
		}
		items.set(slot, stack.copyWithCount(1));
		progress[slot] = 0;
		time[slot] = Math.max(1, recipe.get().value().cookingTime() / SPEED);
		stack.consume(1, player);
		changed();
		return true;
	}

	void serverTick(ServerLevel level) {
		boolean done = false;
		for (int slot = 0; slot < SLOTS; slot++) {
			ItemStack food = items.get(slot);
			if (food.isEmpty() || ++progress[slot] < time[slot]) {
				continue;
			}
			SingleRecipeInput input = new SingleRecipeInput(food);
			ItemStack cooked = recipe(level, food).map(holder -> holder.value().assemble(input)).orElse(food);
			Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, cooked);
			items.set(slot, ItemStack.EMPTY);
			done = true;
		}
		if (done) {
			changed();
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			for (ItemStack stack : items) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
			}
			items.clear();
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.clear();
		for (Skewer skewer : input.read("skewers", Skewer.CODEC.listOf()).orElse(List.of())) {
			if (skewer.slot() >= 0 && skewer.slot() < SLOTS) {
				items.set(skewer.slot(), skewer.item());
				progress[skewer.slot()] = skewer.progress();
				time[skewer.slot()] = skewer.time();
			}
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		List<Skewer> skewers = new ArrayList<>();
		for (int slot = 0; slot < SLOTS; slot++) {
			if (!items.get(slot).isEmpty()) {
				skewers.add(new Skewer(slot, items.get(slot), progress[slot], time[slot]));
			}
		}
		output.store("skewers", Skewer.CODEC.listOf(), skewers);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}

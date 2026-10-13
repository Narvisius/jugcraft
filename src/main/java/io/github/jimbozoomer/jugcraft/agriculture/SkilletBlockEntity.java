package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
 * The Skillet's pan: up to {@value #CAPACITY} of one food, fried one after another by the campfire recipe that takes it
 * (vanilla's {@code campfire_cooking}) in its time divided by {@value #SPEED} while a heat source is under the pan
 * ({@link CookingPotBlockEntity#isHeated}); the fried food gathers in the pan beside it, up to a stack. Without heat
 * nothing cooks and nothing is lost. Saved, and sent to clients for the pan to show; breaking the skillet drops what
 * is in it. It reads one block (the one below it) a tick and looks the recipe up when a food is put in or finished.
 */
public class SkilletBlockEntity extends BlockEntity {
	public final local.peepo.KitchenCompanionPort companionKitchen = new local.peepo.KitchenCompanionPort(this);
	public static final int CAPACITY = 16;
	public static final int SPEED = 3;
	private static final RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> RECIPES =
			RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);

	/** What became of food put in the pan. */
	public enum Added { ADDED, FULL, OTHER, UNCOOKABLE }

	private ItemStack raw = ItemStack.EMPTY;
	private ItemStack fried = ItemStack.EMPTY;
	private int progress;
	private int time;
	public int companionRoom(ItemStack stack,ItemStack result){
		if(!raw.isEmpty() && !ItemStack.isSameItemSameComponents(raw,stack) || !fried.isEmpty() && !ItemStack.isSameItemSameComponents(fried,result))return 0;
		return Math.max(0,Math.min(CAPACITY-raw.getCount(),result.getMaxStackSize()/Math.max(1,result.getCount())-raw.getCount()-fried.getCount()/Math.max(1,result.getCount())));
	}
	public int companionInsert(ItemStack stack,int maximum){
		if(!(level instanceof ServerLevel server))return 0;var r=recipe(server,stack).orElse(null);if(r==null)return 0;
		var result=r.value().assemble(new SingleRecipeInput(stack));int count=Math.min(maximum,companionRoom(stack,result));if(count<=0)return 0;
		if(raw.isEmpty()){raw=stack.copyWithCount(count);progress=0;}else raw.grow(count);
		time=Math.max(1,r.value().cookingTime()/SPEED);return count;
	}
	private record CompanionSnapshot(ItemStack raw,ItemStack fried,int progress,int time){}
	public Object companionSnapshot(){return new CompanionSnapshot(raw.copy(),fried.copy(),progress,time);}
	public void companionRestore(Object value){var s=(CompanionSnapshot)value;raw=s.raw.copy();fried=s.fried.copy();progress=s.progress;time=s.time;}


	public SkilletBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.SKILLET_ENTITY, pos, state);
	}

	/** The food waiting to fry. */
	public ItemStack raw() {
		return raw;
	}

	/** What has been fried so far. */
	public ItemStack fried() {
		return fried;
	}

	private static Optional<RecipeHolder<CampfireCookingRecipe>> recipe(ServerLevel level, ItemStack stack) {
		return RECIPES.getRecipeFor(new SingleRecipeInput(stack), level);
	}

	/** Puts as much of {@code stack} in the pan as fits, if a campfire recipe fries it and the pan holds no other food. */
	public Added add(ServerLevel level, ItemStack stack, Player player) {
		Optional<RecipeHolder<CampfireCookingRecipe>> recipe = recipe(level, stack);
		if (recipe.isEmpty()) {
			return Added.UNCOOKABLE;
		}
		if (!raw.isEmpty() && !ItemStack.isSameItemSameComponents(raw, stack)) {
			return Added.OTHER;
		}
		ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(stack.copyWithCount(1)));
		if (!fried.isEmpty() && !ItemStack.isSameItemSameComponents(fried, result)) {
			return Added.OTHER;
		}
		int room = CAPACITY - raw.getCount();
		if (room <= 0) {
			return Added.FULL;
		}
		int moved = Math.min(room, stack.getCount());
		if (raw.isEmpty()) {
			raw = stack.copyWithCount(moved);
			progress = 0;
		} else {
			raw.grow(moved);
		}
		time = Math.max(1, recipe.get().value().cookingTime() / SPEED);
		stack.consume(moved, player);
		changed();
		return Added.ADDED;
	}

	/** Everything in the pan, fried and not, handed back; the pan is left empty. */
	public ItemStack[] takeAll() {
		ItemStack[] out = {fried, raw};
		fried = ItemStack.EMPTY;
		raw = ItemStack.EMPTY;
		progress = 0;
		changed();
		return out;
	}

	public boolean isEmpty() {
		return raw.isEmpty() && fried.isEmpty();
	}

	void serverTick(ServerLevel level, BlockPos pos) {
		if (raw.isEmpty() || !CookingPotBlockEntity.isHeated(level, pos)) {
			return;
		}
		if (++progress < time) {
			return;
		}
		progress = 0;
		ItemStack one = raw.copyWithCount(1);
		Optional<RecipeHolder<CampfireCookingRecipe>> recipe = recipe(level, one);
		if (recipe.isEmpty()) {
			// The recipe went away (a data pack reload): hand the food back rather than lose it.
			Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, raw);
			raw = ItemStack.EMPTY;
			changed();
			return;
		}
		ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(one));
		if (fried.isEmpty()) {
			fried = result;
		} else if (ItemStack.isSameItemSameComponents(fried, result) && fried.getCount() + result.getCount() <= fried.getMaxStackSize()) {
			fried.grow(result.getCount());
		} else {
			// The pan is full of fried food: wait for it to be taken out.
			progress = time;
			return;
		}
		raw.shrink(1);
		changed();
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		companionKitchen.drop();
		if (level != null) {
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), fried);
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), raw);
			fried = ItemStack.EMPTY;
			raw = ItemStack.EMPTY;
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		companionKitchen.load(input);
		raw = input.read("raw", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
		fried = input.read("fried", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
		progress = input.getIntOr("progress", 0);
		time = input.getIntOr("time", 1);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		companionKitchen.save(output);
		output.store("raw", ItemStack.OPTIONAL_CODEC, raw);
		output.store("fried", ItemStack.OPTIONAL_CODEC, fried);
		output.putInt("progress", progress);
		output.putInt("time", time);
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

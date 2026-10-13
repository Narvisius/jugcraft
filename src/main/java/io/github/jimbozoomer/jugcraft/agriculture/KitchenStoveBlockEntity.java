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
 * The Kitchen Stove's hob: up to {@value #SLOTS} foods, each cooking by the campfire recipe that takes it (data driven,
 * vanilla's {@code campfire_cooking}) in its time divided by {@value #SPEED}, while the stove is lit and nothing stands on
 * top of it; when one is done it pops off above the stove. Saved, and sent to clients for the hob to show; breaking the
 * stove drops what is on it. It reads one block (the one above it) a tick, and looks a recipe up only when a food goes
 * on or comes off.
 */
public class KitchenStoveBlockEntity extends BlockEntity {
	public final local.peepo.KitchenCompanionPort companionKitchen = new local.peepo.KitchenCompanionPort(this);
	public static final int SLOTS = 6;
	public static final int SPEED = 2;
	private static final RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> RECIPES =
			RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);

	/** What became of a food put on the hob. */
	public enum Placed { PLACED, FULL, UNCOOKABLE }

	private record Pan(int slot, ItemStack item, int progress, int time) {
		static final Codec<Pan> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("slot").forGetter(Pan::slot),
				ItemStack.CODEC.fieldOf("item").forGetter(Pan::item),
				Codec.INT.fieldOf("progress").forGetter(Pan::progress),
				Codec.INT.fieldOf("time").forGetter(Pan::time)).apply(i, Pan::new));
	}

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private final int[] progress = new int[SLOTS];
	private final int[] time = new int[SLOTS];
	private final boolean[] companionLoaded = new boolean[SLOTS];
	public int companionRoom(){int free=0;for(var s:items)if(s.isEmpty())free++;return free;}
	public int companionInsert(ItemStack stack,int maximum){
		if(!(level instanceof ServerLevel server))return 0;var r=recipe(server,stack).orElse(null);if(r==null)return 0;
		int moved=0;for(int i=0;i<SLOTS && moved<maximum;i++)if(items.get(i).isEmpty()){
			items.set(i,stack.copyWithCount(1));progress[i]=0;time[i]=Math.max(1,r.value().cookingTime()/SPEED);companionLoaded[i]=true;moved++;
		}return moved;
	}
	private record CompanionSnapshot(List<ItemStack> items,int[] progress,int[] time,boolean[] managed){}
	public Object companionSnapshot(){return new CompanionSnapshot(items.stream().map(ItemStack::copy).toList(),progress.clone(),time.clone(),companionLoaded.clone());}
	public void companionRestore(Object value){var s=(CompanionSnapshot)value;for(int i=0;i<SLOTS;i++){items.set(i,s.items.get(i).copy());progress[i]=s.progress[i];time[i]=s.time[i];companionLoaded[i]=s.managed[i];}}


	public KitchenStoveBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.KITCHEN_STOVE_ENTITY, pos, state);
	}

	public List<ItemStack> items() {
		return List.copyOf(items);
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

	/** Puts one of {@code stack} on a free place on the hob if a campfire recipe takes it. */
	public Placed place(ServerLevel level, ItemStack stack, Player player) {
		Optional<RecipeHolder<CampfireCookingRecipe>> recipe = recipe(level, stack);
		if (recipe.isEmpty()) {
			return Placed.UNCOOKABLE;
		}
		int slot = freeSlot();
		if (slot < 0) {
			return Placed.FULL;
		}
		items.set(slot, stack.copyWithCount(1));
		companionLoaded[slot]=false;
		progress[slot] = 0;
		time[slot] = Math.max(1, recipe.get().value().cookingTime() / SPEED);
		stack.consume(1, player);
		changed();
		return Placed.PLACED;
	}

	/** Called while the stove is lit: the hob cooks unless something stands on top of it. */
	void serverTick(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos.above()).isAir()) {
			return;
		}
		boolean done = false;
		for (int slot = 0; slot < SLOTS; slot++) {
			ItemStack food = items.get(slot);
			if (food.isEmpty() || ++progress[slot] < time[slot]) {
				continue;
			}
			if(companionLoaded[slot] && progress[slot]>time[slot] && level.getGameTime()%20!=0)continue;
			SingleRecipeInput input = new SingleRecipeInput(food);
			ItemStack cooked = recipe(level, food).map(holder -> holder.value().assemble(input)).orElse(food);
			if(companionLoaded[slot]){
				if(!companionKitchen.finish(cooked)){progress[slot]=time[slot]+1;continue;}
			}else Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, cooked);
			companionLoaded[slot]=false;
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
		companionKitchen.drop();
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
		companionKitchen.load(input);
		for(int i=0;i<SLOTS;i++)companionLoaded[i]=input.getBooleanOr("companionHob"+i,false);
		items.clear();
		for (Pan pan : input.read("hob", Pan.CODEC.listOf()).orElse(List.of())) {
			if (pan.slot() >= 0 && pan.slot() < SLOTS) {
				items.set(pan.slot(), pan.item());
				progress[pan.slot()] = pan.progress();
				time[pan.slot()] = pan.time();
			}
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		companionKitchen.save(output);
		for(int i=0;i<SLOTS;i++)output.putBoolean("companionHob"+i,companionLoaded[i]);
		List<Pan> hob = new ArrayList<>();
		for (int slot = 0; slot < SLOTS; slot++) {
			if (!items.get(slot).isEmpty()) {
				hob.add(new Pan(slot, items.get(slot), progress[slot], time[slot]));
			}
		}
		output.store("hob", Pan.CODEC.listOf(), hob);
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

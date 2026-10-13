package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Canning Kettle's water bath: whether it holds water, how hot it is, and up to {@value #JARS} jars of preserves with how
 * long each has been processed. Over a heat source ({@link CookingPotBlockEntity#isHeated}) the water warms a step a tick
 * and boils at {@value #BOIL_TICKS}; without heat it cools again. A jar held in boiling water for {@value #PROCESS_TICKS}
 * ticks is sealed ({@link PreserveJarItem#seal}). Saved, and sent to clients to draw the water and the jars.
 */
public class CanningKettleBlockEntity extends BlockEntity {
	public static final int JARS = 4;
	public static final int BOIL_TICKS = 200;
	public static final int PROCESS_TICKS = 400;

	private boolean water;
	private int heat;
	private final List<ItemStack> jars = new ArrayList<>();
	private final List<Integer> processed = new ArrayList<>();
	/** One bucket left by companion filling; retained until collected, never spawned as a loose item. */
	private int returnedBucket;
	private final Transfers transfers = new Transfers();
	private final Storage<ItemVariant> companionInputs = new Storage<>() {
		@Override public long insert(ItemVariant resource, long maxAmount, TransactionContext tx) {
			if (resource.isBlank() || maxAmount <= 0) return 0;
			ItemStack stack = resource.toStack(1);
			int count = (int) Math.min(maxAmount, companionNeed(stack));
			if (count <= 0) return 0;
			transfers.updateSnapshots(tx);
			if (stack.is(Items.WATER_BUCKET)) { water = true; returnedBucket = 1; }
			else for (int i = 0; i < count; i++) { jars.add(stack.copy()); processed.add(0); }
			return count;
		}
		@Override public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) { return 0; }
		@Override public boolean supportsExtraction() { return false; }
		@Override public Iterator<StorageView<ItemVariant>> iterator() { return Collections.emptyIterator(); }
	};
	private final Storage<ItemVariant> companionOutputs = new Storage<>() {
		@Override public long insert(ItemVariant resource, long maxAmount, TransactionContext tx) { return 0; }
		@Override public boolean supportsInsertion() { return false; }
		@Override public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) {
			if (maxAmount <= 0) return 0;
			int count = (int) Math.min(maxAmount, outputCount(resource));
			if (count <= 0) return 0;
			transfers.updateSnapshots(tx);
			if (resource.equals(ItemVariant.of(Items.BUCKET))) returnedBucket -= count;
			else {
				int left = count;
				for (int i = jars.size() - 1; i >= 0 && left > 0; i--)
					if (readyOutput(jars.get(i)) && resource.equals(ItemVariant.of(jars.get(i)))) {
						jars.remove(i); processed.remove(i); left--;
					}
			}
			return count;
		}
		@Override public Iterator<StorageView<ItemVariant>> iterator() {
			// At most five variants. Equal sealed jars share a trip; freshness components remain distinct.
			var variants = new LinkedHashSet<ItemVariant>();
			if (returnedBucket > 0) variants.add(ItemVariant.of(Items.BUCKET));
			for (var jar : jars) if (readyOutput(jar)) variants.add(ItemVariant.of(jar));
			List<StorageView<ItemVariant>> views = new ArrayList<>();
			for (var variant : variants) views.add(outputView(variant));
			return views.iterator();
		}
	};

	public CanningKettleBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.CANNING_KETTLE_ENTITY, pos, state);
	}

	private boolean live() { return level != null && !level.isClientSide() && !isRemoved(); }
	private boolean heated() { return live() && level.hasChunkAt(worldPosition.below()) && CookingPotBlockEntity.isHeated(level, worldPosition); }
	private boolean readyOutput(ItemStack jar) {
		return live() && (PreserveJarItem.sealed(jar) || !PreserveJarItem.sealable(jar, level.getGameTime()));
	}
	public int companionNeed(ItemStack stack) {
		if (!live()) return 0;
		if (stack.is(Items.WATER_BUCKET)) return !water && returnedBucket == 0 ? 1 : 0;
		if (!water || returnedBucket > 0 || !heated()) return 0;
		// Clear completed/rejected jars before supplying more when Output is blocked.
		for (var jar : jars) if (readyOutput(jar)) return 0;
		return PreserveJarItem.sealable(stack, level.getGameTime()) ? JARS - jars.size() : 0;
	}
	public local.peepo.CompanionStatus companionStatus() {
		if (!live()) return local.peepo.CompanionStatus.UNLOADED;
		for (var jar : jars) if (!PreserveJarItem.sealed(jar) && readyOutput(jar)) return local.peepo.CompanionStatus.SPOILED_INPUT;
		if (returnedBucket > 0 || sealedCount() > 0) return local.peepo.CompanionStatus.READY;
		if (!heated()) return local.peepo.CompanionStatus.NO_HEAT;
		if (!water || jars.isEmpty()) return local.peepo.CompanionStatus.NO_INPUT;
		return local.peepo.CompanionStatus.WORKING;
	}
	public Storage<ItemVariant> companionInputs() { return companionInputs; }
	public Storage<ItemVariant> companionOutputs() { return companionOutputs; }
	private int outputCount(ItemVariant resource) {
		if (!live() || resource.isBlank()) return 0;
		if (resource.equals(ItemVariant.of(Items.BUCKET))) return returnedBucket;
		int count = 0;
		for (var jar : jars) if (readyOutput(jar) && resource.equals(ItemVariant.of(jar))) count++;
		return count;
	}
	private SingleSlotStorage<ItemVariant> outputView(ItemVariant variant) {
		return new SingleSlotStorage<>() {
			@Override public long insert(ItemVariant resource, long maxAmount, TransactionContext tx) { return 0; }
			@Override public boolean supportsInsertion() { return false; }
			@Override public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) {
				return variant.equals(resource) ? companionOutputs.extract(resource, maxAmount, tx) : 0;
			}
			@Override public boolean isResourceBlank() { return getAmount() == 0; }
			@Override public ItemVariant getResource() { return isResourceBlank() ? ItemVariant.blank() : variant; }
			@Override public long getAmount() { return outputCount(variant); }
			@Override public long getCapacity() { return variant.equals(ItemVariant.of(Items.BUCKET)) ? 1 : JARS; }
		};
	}
	private record TransferSnapshot(boolean water, int heat, int bucket, List<ItemStack> jars, List<Integer> times) {}
	private final class Transfers extends SnapshotParticipant<TransferSnapshot> {
		@Override protected TransferSnapshot createSnapshot() {
			return new TransferSnapshot(water, heat, returnedBucket, jars.stream().map(ItemStack::copy).toList(), List.copyOf(processed));
		}
		@Override protected void readSnapshot(TransferSnapshot s) {
			water=s.water(); heat=s.heat(); returnedBucket=s.bucket();
			jars.clear(); jars.addAll(s.jars()); processed.clear(); processed.addAll(s.times());
		}
		@Override protected void onFinalCommit() { changed(); }
	}
	/** Manual recovery of the automation remainder, with overflow left inside the kettle. */
	public void collectReturnedBucket(Player player) {
		if (!live() || returnedBucket == 0) return;
		var bucket = new ItemStack(Items.BUCKET, returnedBucket);
		player.getInventory().add(bucket); returnedBucket = bucket.getCount(); changed();
	}

	public boolean water() {
		return water;
	}

	public boolean boiling() {
		return water && heat >= BOIL_TICKS;
	}

	public List<ItemStack> jars() {
		return List.copyOf(jars);
	}

	public int sealedCount() {
		return (int) jars.stream().filter(PreserveJarItem::sealed).count();
	}

	public void fill() {
		water = true;
		changed();
	}

	/** Pours the water out (only when no jars are in it); returns whether it held any. */
	public boolean drain() {
		if (!water || !jars.isEmpty()) {
			return false;
		}
		water = false;
		heat = 0;
		changed();
		return true;
	}

	/** Brings the water to the boil at once (a kettle left long over the fire). */
	public void boil() {
		heat = BOIL_TICKS;
		changed();
	}

	/** Puts a full, unsealed jar in, if there is water and room. */
	public boolean add(ItemStack jar) {
		if (!water || jars.size() >= JARS || level == null || !PreserveJarItem.sealable(jar, level.getGameTime())) {
			return false;
		}
		jars.add(jar.copyWithCount(1));
		processed.add(0);
		changed();
		return true;
	}

	/** Takes out the jars that are sealed. */
	public List<ItemStack> takeSealed() {
		List<ItemStack> out = new ArrayList<>();
		for (int i = jars.size() - 1; i >= 0; i--) {
			if (PreserveJarItem.sealed(jars.get(i))) {
				out.add(jars.remove(i));
				processed.remove(i);
			}
		}
		if (!out.isEmpty()) {
			changed();
		}
		return out;
	}

	/** Takes out every jar, sealed or not. */
	public List<ItemStack> takeAll() {
		List<ItemStack> out = new ArrayList<>(jars);
		jars.clear();
		processed.clear();
		changed();
		return out;
	}

	/** Ticks left until the next unsealed jar is sealed, if the water stays at the boil; 0 if none is waiting. */
	public int nextSeal() {
		int best = 0;
		for (int i = 0; i < jars.size(); i++) {
			if (!PreserveJarItem.sealed(jars.get(i))) {
				int left = PROCESS_TICKS - processed.get(i);
				best = best == 0 ? left : Math.min(best, left);
			}
		}
		return best;
	}

	void serverTick(ServerLevel level) {
		if (!water) {
			return;
		}
		boolean wasBoiling = boiling();
		heat = Math.clamp(heat + (CookingPotBlockEntity.isHeated(level, worldPosition) ? 1 : -1), 0, BOIL_TICKS);
		boolean sealedOne = false;
		if (boiling()) {
			for (int i = 0; i < jars.size(); i++) {
				ItemStack jar = jars.get(i);
				if (PreserveJarItem.sealable(jar, level.getGameTime())) {
					processed.set(i, Math.min(PROCESS_TICKS, processed.get(i) + 1));
					// A jar that spoiled while it boiled stays unsealed.
					if (processed.get(i) >= PROCESS_TICKS && !PreserveJarItem.spoiled(jar, level.getGameTime())) {
						PreserveJarItem.seal(jar);
						sealedOne = true;
					}
				}
			}
		}
		if (sealedOne || wasBoiling != boiling()) {
			changed();
		} else if (level.getGameTime() % 20 == 0) {
			setChanged();
		}
	}

	/** Broken, the kettle spills its jars (its water is lost). */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !level.isClientSide()) {
			for (ItemStack jar : jars) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), jar);
			}
			jars.clear();
			processed.clear();
			if (returnedBucket > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.BUCKET, returnedBucket));
			returnedBucket = 0;
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water = input.getBooleanOr("water", false);
		heat = Math.clamp(input.getIntOr("heat", 0), 0, BOIL_TICKS);
		returnedBucket = Math.clamp(input.getIntOr("companionBucket", 0), 0, 1);
		jars.clear();
		processed.clear();
		input.read("jars", ItemStack.CODEC.listOf()).ifPresent(list -> list.stream().limit(JARS).forEach(jars::add));
		List<Integer> times = input.read("processed", com.mojang.serialization.Codec.INT.listOf()).orElse(List.of());
		for (int i = 0; i < jars.size(); i++) {
			processed.add(i < times.size() ? Math.clamp(times.get(i), 0, PROCESS_TICKS) : 0);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("water", water);
		output.putInt("heat", heat);
		output.putInt("companionBucket", returnedBucket);
		output.store("jars", ItemStack.CODEC.listOf(), List.copyOf(jars));
		output.store("processed", com.mojang.serialization.Codec.INT.listOf(), List.copyOf(processed));
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

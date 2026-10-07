package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
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

	public CanningKettleBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.CANNING_KETTLE_ENTITY, pos, state);
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
				if (!PreserveJarItem.sealed(jar)) {
					processed.set(i, processed.get(i) + 1);
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
		if (level != null) {
			for (ItemStack jar : jars) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), jar);
			}
			jars.clear();
			processed.clear();
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

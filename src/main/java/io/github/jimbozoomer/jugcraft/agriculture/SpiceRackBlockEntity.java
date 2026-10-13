package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The spices on a Spice Rack (tools/spices.py RACK): up to {@value #SLOTS}, four to a shelf, one of each put up, filled
 * from the top left and taken from the last. What fits is the item tag {@code jugcraft:spices} (data packs can add their
 * own). Saved, and sent to clients to draw them; broken, the rack spills them, as the Pantry Shelf does its jars.
 */
public class SpiceRackBlockEntity extends BlockEntity {
	public static final int SLOTS = 8;
	public static final TagKey<Item> SPICES = TagKey.create(Registries.ITEM, Jugcraft.id("spices"));
	private final List<ItemStack> spices = new ArrayList<>();

	public SpiceRackBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.SPICE_RACK_ENTITY, pos, state);
	}

	public List<ItemStack> spices() {
		return List.copyOf(spices);
	}

	public int count() {
		return spices.size();
	}

	public static boolean fits(ItemStack stack) {
		return stack.is(SPICES);
	}

	public boolean store(ItemStack stack) {
		if (spices.size() >= SLOTS || !fits(stack)) {
			return false;
		}
		spices.add(stack.copyWithCount(1));
		changed();
		return true;
	}

	public ItemStack takeLast() {
		if (spices.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack last = spices.removeLast();
		changed();
		return last;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			for (ItemStack spice : spices) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), spice);
			}
			spices.clear();
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
		spices.clear();
		input.read("spices", ItemStack.CODEC.listOf()).ifPresent(list -> list.stream().limit(SLOTS).forEach(spices::add));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("spices", ItemStack.CODEC.listOf(), List.copyOf(spices));
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

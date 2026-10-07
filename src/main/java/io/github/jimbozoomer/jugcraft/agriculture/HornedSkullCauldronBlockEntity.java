package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What a {@link HornedSkullCauldronBlock} holds: which potion (water is a potion too; nothing when it is empty) and up to
 * {@value HornedSkullCauldronBlock#FLOATERS} things floating in it, last in on top. Saved, and sent to clients, which draw
 * the brew in the potion's colour with the things bobbing in it.
 */
public class HornedSkullCauldronBlockEntity extends BlockEntity {
	/** Vanilla water's colour, for a cauldron of plain water. */
	public static final int WATER_COLOR = 0x3F76E4;
	private PotionContents contents = PotionContents.EMPTY;
	private final List<ItemStack> floating = new ArrayList<>();

	public HornedSkullCauldronBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.HORNED_SKULL_CAULDRON_ENTITY, pos, state);
	}

	public PotionContents contents() {
		return contents;
	}

	public boolean isWater() {
		return contents.is(Potions.WATER);
	}

	/** The brew's colour (RGB): the potion's, or water's. */
	public int color() {
		return isWater() || contents.equals(PotionContents.EMPTY) ? WATER_COLOR : contents.getColor() & 0xFFFFFF;
	}

	public void setContents(PotionContents contents) {
		this.contents = contents;
		changed();
	}

	public List<ItemStack> floating() {
		return List.copyOf(floating);
	}

	/** Drops one of {@code stack} in to float, if there is room. */
	public boolean addFloating(ItemStack stack) {
		if (floating.size() >= HornedSkullCauldronBlock.FLOATERS || stack.isEmpty()) {
			return false;
		}
		floating.add(stack.copyWithCount(1));
		changed();
		return true;
	}

	/** Takes out the thing that went in last, or nothing. */
	public ItemStack takeFloating() {
		if (floating.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack last = floating.removeLast();
		changed();
		return last;
	}

	/** Breaking the cauldron gives back what floats in it; the brew is lost, as a vanilla cauldron's is. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !level.isClientSide()) {
			for (ItemStack stack : floating) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
			}
		}
		floating.clear();
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		contents = input.read("contents", PotionContents.CODEC).orElse(PotionContents.EMPTY);
		floating.clear();
		input.read("floating", ItemStack.CODEC.listOf()).ifPresent(list -> list.stream().limit(HornedSkullCauldronBlock.FLOATERS).forEach(floating::add));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!contents.equals(PotionContents.EMPTY)) {
			output.store("contents", PotionContents.CODEC, contents);
		}
		output.store("floating", ItemStack.CODEC.listOf(), List.copyOf(floating));
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

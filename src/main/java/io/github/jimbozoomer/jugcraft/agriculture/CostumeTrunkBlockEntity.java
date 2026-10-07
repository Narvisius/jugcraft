package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The costumes in a Costume Trunk, one of each item, in order: the next to change into first. Only costumes (item tag
 * {@link TrickOrTreat#COSTUMES}) go in, at most {@value #SLOTS}. They spill out when the trunk is broken.
 */
public class CostumeTrunkBlockEntity extends BlockEntity {
	public static final int SLOTS = 9;
	private final List<ItemStack> outfits = new ArrayList<>();

	public CostumeTrunkBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.COSTUME_TRUNK_ENTITY, pos, state);
	}

	public List<ItemStack> outfits() {
		return Collections.unmodifiableList(outfits);
	}

	public int count() {
		return outfits.size();
	}

	/** Packs one of {@code costume} away at the back, if it is a costume and there is room. */
	public boolean store(ItemStack costume) {
		if (costume.isEmpty() || !costume.is(TrickOrTreat.COSTUMES) || outfits.size() >= SLOTS) {
			return false;
		}
		outfits.add(costume.copyWithCount(1));
		setChanged();
		return true;
	}

	/** Takes out the costume at the front (the next to change into), or nothing. */
	public ItemStack takeNext() {
		if (outfits.isEmpty()) {
			return ItemStack.EMPTY;
		}
		setChanged();
		return outfits.removeFirst();
	}

	/** Takes out the costume packed last, or nothing. */
	public ItemStack takeLast() {
		if (outfits.isEmpty()) {
			return ItemStack.EMPTY;
		}
		setChanged();
		return outfits.removeLast();
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			for (ItemStack stack : outfits) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
			}
			outfits.clear();
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		outfits.clear();
		input.read("outfits", ItemStack.CODEC.listOf()).ifPresent(list -> list.stream()
				.filter(stack -> stack.is(TrickOrTreat.COSTUMES)).limit(SLOTS).forEach(stack -> outfits.add(stack.copyWithCount(1))));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("outfits", ItemStack.CODEC.listOf(), List.copyOf(outfits));
	}
}

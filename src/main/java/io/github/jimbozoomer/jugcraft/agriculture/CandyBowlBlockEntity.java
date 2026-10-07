package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * What a Candy Bowl holds and who has taken from it. It holds up to {@value #CAPACITY} treats
 * ({@link CandyBagItem#TREATS}); anyone may add. Each visitor takes one treat a night (the trick-or-treat night,
 * {@link TrickOrTreat#night}); its owner, whoever placed it, takes one whenever they like. The bowl remembers the last
 * {@value #VISITORS} visitors and the night each last took a treat, saved with the world, so a restart gives no
 * second treat. A bowl (not a cache) also invites trick-or-treaters on Halloween nights ({@link TrickOrTreaters}),
 * looking every {@value TrickOrTreaters#CHECK_TICKS} ticks, and counts the groups that came tonight.
 */
public class CandyBowlBlockEntity extends BlockEntity {
	public static final int CAPACITY = 64;
	public static final int VISITORS = 256;
	/** The treat count at which the bowl looks a little, half and heaped full. */
	public static final int[] FILL = {1, 16, 48};

	public enum Taken {
		TAKEN, EMPTY, HAD_ONE
	}

	private record Visit(UUID visitor, long night) {
		static final Codec<Visit> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("visitor").forGetter(Visit::visitor),
				Codec.LONG.fieldOf("night").forGetter(Visit::night)).apply(i, Visit::new));
	}

	private final List<ItemStack> treats = new ArrayList<>();
	private final Map<UUID, Long> visits = new LinkedHashMap<>();
	private @Nullable UUID owner;
	private long groupsNight = Long.MIN_VALUE;
	private int groups;

	public CandyBowlBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.CANDY_BOWL_ENTITY, pos, state);
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public void setOwner(@Nullable UUID player) {
		owner = player;
		setChanged();
	}

	public int count() {
		return treats.stream().mapToInt(ItemStack::getCount).sum();
	}

	/** Puts as much of {@code treat} in as fits (treats only); {@code treat} keeps the rest. Returns how many went in. */
	public int add(ItemStack treat) {
		if (treat.isEmpty() || !treat.is(CandyBagItem.TREATS)) {
			return 0;
		}
		int room = CAPACITY - count();
		int moved = Math.min(room, treat.getCount());
		int left = moved;
		for (ItemStack stack : treats) {
			if (left == 0) {
				break;
			}
			if (ItemStack.isSameItemSameComponents(stack, treat)) {
				int add = Math.min(left, stack.getMaxStackSize() - stack.getCount());
				stack.grow(add);
				left -= add;
			}
		}
		while (left > 0) {
			int add = Math.min(left, treat.getMaxStackSize());
			treats.add(treat.copyWithCount(add));
			left -= add;
		}
		treat.shrink(moved);
		if (moved > 0) {
			changed();
		}
		return moved;
	}

	/** {@code player} tries to take one treat on {@code night}; on success it is in their inventory (or on top of the bowl). */
	public Taken take(Player player, long night) {
		if (treats.isEmpty()) {
			return Taken.EMPTY;
		}
		boolean ownBowl = player.getUUID().equals(owner);
		if (!ownBowl && visits.getOrDefault(player.getUUID(), Long.MIN_VALUE) == night) {
			return Taken.HAD_ONE;
		}
		ItemStack from = treats.get(level == null ? 0 : level.getRandom().nextInt(treats.size()));
		ItemStack treat = from.split(1);
		if (from.isEmpty()) {
			treats.remove(from);
		}
		if (!ownBowl) {
			visits.remove(player.getUUID());
			visits.put(player.getUUID(), night);
			while (visits.size() > VISITORS) {
				visits.remove(visits.keySet().iterator().next());
			}
		}
		if (!player.getInventory().add(treat) && level != null) {
			Block.popResource(level, worldPosition.above(), treat);
		}
		changed();
		return Taken.TAKEN;
	}

	/** Hands one treat out, whoever asks (a trick-or-treater), or an empty stack if the bowl is empty. */
	public ItemStack handOut() {
		if (treats.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack from = treats.get(level == null ? 0 : level.getRandom().nextInt(treats.size()));
		ItemStack treat = from.split(1);
		if (from.isEmpty()) {
			treats.remove(from);
		}
		changed();
		return treat;
	}

	/** How many groups of trick-or-treaters came to this bowl on {@code night}. */
	public int groups(long night) {
		return night == groupsNight ? groups : 0;
	}

	void recordGroup(long night) {
		groups = groups(night) + 1;
		groupsNight = night;
		setChanged();
	}

	void serverTick(ServerLevel level) {
		if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), TrickOrTreaters.CHECK_TICKS) == 0) {
			TrickOrTreaters.maybeVisit(level, worldPosition, this);
		}
	}

	/** 0 for empty, then 1-3 as it fills ({@link #FILL}). */
	public int fillLevel() {
		int count = count();
		int fill = 0;
		for (int i = 0; i < FILL.length; i++) {
			if (count >= FILL[i]) {
				fill = i + 1;
			}
		}
		return fill;
	}

	private void changed() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			BlockState state = getBlockState();
			int fill = fillLevel();
			if (state.getValue(CandyBowlBlock.FILL) != fill) {
				level.setBlock(worldPosition, state.setValue(CandyBowlBlock.FILL, fill), Block.UPDATE_ALL);
			}
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			for (ItemStack stack : treats) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
			}
			treats.clear();
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		treats.clear();
		input.read("treats", ItemStack.CODEC.listOf()).ifPresent(list -> {
			int room = CAPACITY;
			for (ItemStack stack : list) {
				if (room > 0 && stack.is(CandyBagItem.TREATS)) {
					treats.add(stack.copyWithCount(Math.min(room, stack.getCount())));
					room -= Math.min(room, stack.getCount());
				}
			}
		});
		visits.clear();
		input.read("visits", Visit.CODEC.listOf()).ifPresent(list -> list.stream().skip(Math.max(0, list.size() - VISITORS))
				.forEach(visit -> visits.put(visit.visitor(), visit.night())));
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		groupsNight = input.getLongOr("groups_night", Long.MIN_VALUE);
		groups = input.getIntOr("groups", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("treats", ItemStack.CODEC.listOf(), List.copyOf(treats));
		output.store("visits", Visit.CODEC.listOf(), visits.entrySet().stream().map(e -> new Visit(e.getKey(), e.getValue())).toList());
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.putLong("groups_night", groupsNight);
		output.putInt("groups", groups);
	}
}

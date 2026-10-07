package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The things on show in a display (Halloween decorations batch 17): the Curiosity Cabinet's nine places, the Bell Jar's
 * one, the Broom Rack's three pegs. One of a thing to a place; a place is chosen by where it is used ({@link Showcase}).
 * Saved, and sent to clients to draw them; broken, it spills them. A comparator reads how full it is.
 */
public class ShowcaseBlockEntity extends BlockEntity {
	private final NonNullList<ItemStack> items;
	/** When it was last opened (the cabinet's doors swing open), on clients. */
	private long opened = Long.MIN_VALUE;

	/** A block whose block entity shows things: how many places it has, what it takes, and its sounds. */
	public interface Showcase {
		int places();

		boolean accepts(ItemStack stack);

		SoundEvent putSound();
	}

	public ShowcaseBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.SHOWCASE_ENTITY, pos, state);
		items = NonNullList.withSize(state.getBlock() instanceof Showcase showcase ? showcase.places() : 1, ItemStack.EMPTY);
	}

	public int places() {
		return items.size();
	}

	public void mark(long time) {
		opened = time;
	}

	public long opened() {
		return opened;
	}

	/**
	 * Where a hit at {@code x}, {@code z} pixels in its block lies on a display facing {@code facing}, in the coordinates
	 * of its model (which faces north): {x, z}.
	 */
	public static double[] local(net.minecraft.core.Direction facing, double x, double z) {
		return switch (facing) {
			case EAST -> new double[] {z, 16 - x};
			case SOUTH -> new double[] {16 - x, 16 - z};
			case WEST -> new double[] {16 - z, x};
			default -> new double[] {x, z};
		};
	}

	public ItemStack get(int place) {
		return place >= 0 && place < items.size() ? items.get(place) : ItemStack.EMPTY;
	}

	public int count() {
		return (int) items.stream().filter(s -> !s.isEmpty()).count();
	}

	/**
	 * Using place {@code place} with {@code held}: an empty place takes one of what is held, if the display takes it; a full
	 * place gives its thing back (into an empty hand, else the inventory).
	 */
	public InteractionResult use(int place, ItemStack held, Player player, InteractionHand hand, Showcase showcase) {
		if (place < 0 || place >= items.size()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		ItemStack there = items.get(place);
		if (!there.isEmpty()) {
			if (level != null && !level.isClientSide()) {
				items.set(place, ItemStack.EMPTY);
				if (held.isEmpty()) {
					player.setItemInHand(hand, there);
				} else if (!player.getInventory().add(there)) {
					player.spawnAtLocation((ServerLevel) level, there);
				}
				level.playSound(null, worldPosition, showcase.putSound(), SoundSource.BLOCKS, 0.8F, 1.2F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, worldPosition);
				changed();
			}
			return InteractionResult.SUCCESS;
		}
		if (held.isEmpty() || !showcase.accepts(held)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level != null && !level.isClientSide()) {
			items.set(place, held.copyWithCount(1));
			held.consume(1, player);
			level.playSound(null, worldPosition, showcase.putSound(), SoundSource.BLOCKS, 0.8F, 0.9F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, worldPosition);
			changed();
		}
		return InteractionResult.SUCCESS;
	}

	/** Puts {@code stack} (one of it) in place {@code place} if it is empty; for tests and builders. */
	public boolean put(int place, ItemStack stack) {
		if (place < 0 || place >= items.size() || !items.get(place).isEmpty() || stack.isEmpty()) {
			return false;
		}
		items.set(place, stack.copyWithCount(1));
		changed();
		return true;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !level.isClientSide()) {
			for (ItemStack stack : items) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
			}
		}
		items.clear();
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
		for (int i = 0; i < items.size(); i++) {
			items.set(i, ItemStack.EMPTY);
		}
		input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> {
			for (int i = 0; i < Math.min(list.size(), items.size()); i++) {
				items.set(i, list.get(i));
			}
		});
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), java.util.List.copyOf(items));
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

package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The upper half of a {@link ScarecrowBlock}: the head it wears (one pumpkin of any kind, from the
 * {@code jugcraft:scarecrow_heads} item tag), drawn on its shoulders by the client. A lit head lights the scarecrow
 * ({@link ScarecrowBlock#LIGHT}) as brightly as it would light the block it came from.
 */
public class ScarecrowBlockEntity extends BlockEntity {
	private ItemStack head = ItemStack.EMPTY;

	public ScarecrowBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.SCARECROW_ENTITY, pos, state);
	}

	public ItemStack head() {
		return head;
	}

	/** Puts a head on (one of {@code stack}) or, with an empty stack, takes it off; returns the head it wore before. */
	public ItemStack setHead(ItemStack stack) {
		ItemStack old = head;
		head = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			int light = light(head);
			if (state.getValue(ScarecrowBlock.LIGHT) != light) {
				ScarecrowBlock.setLight(level, worldPosition, state, light);
			} else {
				level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
			}
		}
		return old;
	}

	/** Whether {@code head} is a carved pumpkin with a candle inside: a jack o'lantern, or a lit hand-carved pumpkin. */
	public static boolean lit(ItemStack head) {
		return light(head) > 0;
	}

	/**
	 * How much light a head gives: a jack o'lantern's, a lit hand-carved pumpkin's glow (capped at a soul torch's light
	 * when the flame is a soul torch's), or none.
	 */
	public static int light(ItemStack head) {
		if (head.is(Items.JACK_O_LANTERN)) {
			return 15;
		}
		if (head.getItem() instanceof BlockItem item && item.getBlock() instanceof CarvedPumpkinBlock
				&& Boolean.TRUE.equals(head.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(CarvedPumpkinBlock.LIT))) {
			return CarvedPumpkinBlock.flameLight(head.getOrDefault(JugcraftAgriculture.CARVING, PumpkinCarving.BLANK).glow(), soul(head));
		}
		return 0;
	}

	/** Whether a hand-carved head is lit by a soul torch. */
	public static boolean soul(ItemStack head) {
		return Boolean.TRUE.equals(head.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(CarvedPumpkinBlock.SOUL));
	}

	/** Breaking the scarecrow drops its head. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && !level.isClientSide() && !head.isEmpty()) {
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), head);
		}
		head = ItemStack.EMPTY;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		head = input.read("head", ItemStack.CODEC).map(stack -> stack.copyWithCount(1)).orElse(ItemStack.EMPTY);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!head.isEmpty()) {
			output.store("head", ItemStack.CODEC, head);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}

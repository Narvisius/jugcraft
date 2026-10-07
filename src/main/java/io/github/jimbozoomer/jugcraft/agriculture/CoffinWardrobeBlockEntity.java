package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Coffin Wardrobe's four armour pieces, kept in its lower half and sent to clients to draw on the mannequin. The
 * client keeps an invisible armour stand here to wear them ({@link #display}); it is never added to the world.
 */
public class CoffinWardrobeBlockEntity extends BlockEntity {
	public static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private final NonNullList<ItemStack> armor = NonNullList.withSize(SLOTS.length, ItemStack.EMPTY);
	/** The client's display stand (an ArmorStand), made by its renderer; never saved or added to the world. */
	public @Nullable Object display;

	public CoffinWardrobeBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.COFFIN_WARDROBE_ENTITY, pos, state);
	}

	private static int index(EquipmentSlot slot) {
		for (int i = 0; i < SLOTS.length; i++) {
			if (SLOTS[i] == slot) {
				return i;
			}
		}
		throw new IllegalArgumentException("Not an armour slot: " + slot);
	}

	public ItemStack get(EquipmentSlot slot) {
		return armor.get(index(slot));
	}

	public void set(EquipmentSlot slot, ItemStack stack) {
		armor.set(index(slot), stack);
		changed();
	}

	/** Whether {@code worn} must stay on {@code player}: cursed with Binding, unless they build in creative. */
	public static boolean bound(Player player, ItemStack worn) {
		return !player.getAbilities().instabuild && !worn.isEmpty() && EnchantmentHelper.has(worn, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
	}

	/** Swaps each piece {@code player} wears with the one hung here; bound pieces stay. Returns how many slots changed. */
	public int swap(Player player) {
		int swapped = 0;
		for (EquipmentSlot slot : SLOTS) {
			ItemStack worn = player.getItemBySlot(slot);
			ItemStack hung = get(slot);
			if (worn.isEmpty() && hung.isEmpty() || bound(player, worn)) {
				continue;
			}
			player.setItemSlot(slot, hung);
			armor.set(index(slot), worn);
			swapped++;
		}
		if (swapped > 0) {
			changed();
		}
		return swapped;
	}

	private void changed() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !level.isClientSide()) {
			Containers.dropContents(level, pos, armor);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		for (int i = 0; i < SLOTS.length; i++) {
			armor.set(i, input.read(SLOTS[i].getName(), ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		for (int i = 0; i < SLOTS.length; i++) {
			if (!armor.get(i).isEmpty()) {
				output.store(SLOTS[i].getName(), ItemStack.OPTIONAL_CODEC, armor.get(i));
			}
		}
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

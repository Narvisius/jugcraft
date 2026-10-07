package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Coffin Wardrobe (Halloween decorations batch 18): an upright coffin two blocks tall with a window in its lid and a
 * skeleton mannequin inside that wears the armour it holds (drawn by the client). Use it with an empty hand to swap the
 * four armour pieces you wear with the four it holds, all at once, as an armour stand does; pieces cursed with Binding
 * stay on you (in survival). Use it holding a piece of armour to hang that one piece on the mannequin, taking back what
 * hung there. Breaking it drops what it holds. Its lower half keeps the armour.
 */
public class CoffinWardrobeBlock extends TallDecorationBlock implements EntityBlock {
	public CoffinWardrobeBlock(Properties properties) {
		super(properties, Block.box(1.0, 0.0, 1.5, 15.0, 16.0, 14.5), Block.box(1.0, 0.0, 1.5, 15.0, 16.0, 14.5));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new CoffinWardrobeBlockEntity(pos, state) : null;
	}

	private static BlockPos lower(BlockState state, BlockPos pos) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
	}

	/** The armour slot {@code stack} is worn in, or null if it is not armour. */
	public static @Nullable EquipmentSlot armorSlot(ItemStack stack) {
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		return equippable != null && equippable.slot().isArmor() ? equippable.slot() : null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		EquipmentSlot slot = armorSlot(stack);
		if (slot == null) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(level.getBlockEntity(lower(state, pos)) instanceof CoffinWardrobeBlockEntity wardrobe) || !level.mayInteract(player, pos)) {
			return InteractionResult.FAIL;
		}
		ItemStack hung = wardrobe.get(slot);
		wardrobe.set(slot, stack.copyWithCount(1));
		stack.shrink(1);
		if (!hung.isEmpty()) {
			if (stack.isEmpty()) {
				player.setItemInHand(hand, hung);
			} else if (!player.getInventory().add(hung)) {
				player.spawnAtLocation((ServerLevel) level, hung);
			}
		}
		level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 1.0F, 0.9F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide() && level.getBlockEntity(lower(state, pos)) instanceof CoffinWardrobeBlockEntity wardrobe
				&& level.mayInteract(player, pos)) {
			int swapped = wardrobe.swap(player);
			level.playSound(null, pos, swapped > 0 ? SoundEvents.ARMOR_EQUIP_GENERIC.value() : SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS,
					1.0F, swapped > 0 ? 1.0F : 0.6F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}
}

package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneDepots;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A room kit piece drawn live over its model (client/RoomKitDisplayRenderer): a wall screen's anchor, and every cell
 * with a live face (a laptop's page, a clock's figures, the bottle filler's counter, the scrubber's battery gauge), holds
 * a block entity pinned to the tower terminal (the tower's build pins it; placed by hand, the nearest terminal within
 * reach), whose data it shows. The Ops Wall Screen shows its role's three pages in turn (the console desk's roles, and
 * 15, the training room's lesson); sneaking and using it with an empty hand turns it to the next role. The bottle filler
 * fills a glass bottle with water, and its counter goes up by one.
 */
public class RoomKitDisplayBlock extends RoomKitBlock implements EntityBlock, DepotDisplayBlockEntity.Display {
	/** The wall screen's roles: tower/ConsoleDeskBlock ROLES, then the lesson. */
	public static final int LESSON = ConsoleDeskBlock.ROLES.length;
	/** A live page's role for the Lounge TV's channel, TOWER TV (client/TowerTv): after the lesson. */
	public static final int TV = LESSON + 1;

	static RoomKitDisplayBlock create(RoomKit.Spec spec, Properties properties) {
		return (RoomKitDisplayBlock) RoomKitBlock.create(spec, properties, RoomKitDisplayBlock::new);
	}

	protected RoomKitDisplayBlock(RoomKit.Spec spec, Properties properties) {
		super(spec, properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return hasEntity(state) ? new RoomKitBlockEntity(RoomKit.DISPLAY_ENTITY, pos, state) : null;
	}

	/** The cells that draw something live: a wall screen's anchor, and any state with a live face. */
	public boolean hasEntity(BlockState state) {
		return isAnchor(state) && spec.display() != null || !live(state).isEmpty();
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != RoomKit.DISPLAY_ENTITY) {
			return null;
		}
		return (l, p, s, e) -> RoomKitBlockEntity.serverTick(l, p, s, (RoomKitBlockEntity) e);
	}

	@Override
	public int linkRange() {
		return 64;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		var nearest = DroneDepots.nearest(level, pos, linkRange());
		if (nearest != null) {
			for (BlockPos at : cells(anchor(pos, state), state)) {
				if (level.getBlockEntity(at) instanceof DepotDisplayBlockEntity display) {
					display.pin(nearest.getBlockPos());
				}
			}
		}
	}

	/** The bottle filler: a glass bottle held under it comes away full of water, and its counter counts one more. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!"bottle_filler".equals(spec.id()) || !stack.is(Items.GLASS_BOTTLE)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (!level.isClientSide()) {
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, PotionContents.createItemStack(Items.POTION, Potions.WATER)));
			level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			for (BlockPos at : cells(anchor(pos, state), state)) {
				if (level.getBlockEntity(at) instanceof RoomKitBlockEntity counter
						&& live(level.getBlockState(at)).stream().anyMatch(f -> "counter".equals(f.kind()))) {
					counter.addCount(1);
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** The role property of a wall screen (null for other displays). */
	public @Nullable IntegerProperty role() {
		return property("role") instanceof IntegerProperty p ? p : null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		IntegerProperty role = role();
		if (role == null || !player.isSecondaryUseActive()) {
			return super.useWithoutItem(state, level, pos, player, hit);
		}
		if (!level.isClientSide()) {
			int next = (state.getValue(role) + 1) % (role.getPossibleValues().size());
			BlockPos anchor = anchor(pos, state);
			for (BlockPos at : cells(anchor, state)) {
				BlockState there = level.getBlockState(at);
				if (there.is(this)) {
					level.setBlock(at, there.setValue(role, next), Block.UPDATE_CLIENTS);
				}
			}
		}
		return InteractionResult.SUCCESS;
	}
}

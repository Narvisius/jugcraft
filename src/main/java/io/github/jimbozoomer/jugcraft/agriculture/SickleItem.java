package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A hand tool for harvest time. Right-clicking a crop harvests every ripe crop in a square around
 * it (3×3 for radius 1, 5×5 for radius 2) and one block up or down:
 * <ul>
 * <li>one-block crops, including vanilla wheat, carrots, potatoes and beetroots, drop their loot and
 * are replanted with one seed from that loot;</li>
 * <li>tall crops and ripe cranberry bushes are picked and stay standing;</li>
 * <li>squash and gourds that a stem is still attached to are cut off, so the stem grows another.</li>
 * </ul>
 * Unripe crops are left alone. Costs 1 durability per use that harvests anything. All work happens on
 * the server, and each block is checked against spawn protection and claims ({@code mayInteract}).
 */
public class SickleItem extends Item {
	private final int radius;

	public SickleItem(Properties properties, int radius) {
		super(properties);
		this.radius = radius;
	}

	private static boolean isCrop(BlockState state) {
		return state.getBlock() instanceof TallCropBlock || state.getBlock() instanceof CropBlock
				|| state.getBlock() instanceof CranberryBushBlock || state.getBlock() instanceof GourdBlock;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		BlockState state = level.getBlockState(clicked);
		Player player = context.getPlayer();
		if (!isCrop(state) || (player != null && !player.mayBuild())) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack sickle = context.getItemInHand();
		BlockPos center = state.getBlock() instanceof TallCropBlock tall ? tall.bottom(clicked, state) : clicked;
		int harvested = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
			BlockPos target = pos.immutable();
			if ((player == null || serverLevel.mayInteract(player, target)) && harvest(serverLevel, target, player, sickle)) {
				harvested++;
			}
		}
		if (harvested == 0) {
			return InteractionResult.PASS;
		}
		serverLevel.playSound(null, center, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
		if (player != null) {
			sickle.hurtAndBreak(1, player, context.getHand());
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Whether an attached stem next to the gourd at {@code pos} holds it; gourds a player set down are left alone. */
	private static boolean isOnStem(ServerLevel level, BlockPos pos) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos stem = pos.relative(side);
			BlockState state = level.getBlockState(stem);
			if (state.getBlock() instanceof AttachedGourdStemBlock attached && attached.holds(state, stem, pos)) {
				return true;
			}
		}
		return false;
	}

	/** Harvests one ripe crop at {@code pos}. Returns false if there is none. */
	static boolean harvest(ServerLevel level, BlockPos pos, @Nullable Player player, ItemStack sickle) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof TallCropBlock tall) {
			return state.getValue(TallCropBlock.SECTION) == 0 && tall.pick(level, pos, pos.above());
		}
		if (state.getBlock() instanceof CranberryBushBlock bush) {
			return bush.pick(level, pos, pos.above());
		}
		if (state.getBlock() instanceof GourdBlock) {
			return isOnStem(level, pos) && level.destroyBlock(pos, true, player);
		}
		if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
			return false;
		}
		Item seed = crop instanceof JugcraftCropBlock jugcraftCrop ? jugcraftCrop.seed() : crop.asItem();
		List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, sickle);
		boolean replanted = false;
		for (ItemStack drop : drops) {
			if (!replanted && drop.is(seed)) {
				drop.shrink(1);
				replanted = true;
			}
		}
		for (ItemStack drop : drops) {
			if (!drop.isEmpty()) {
				Block.popResource(level, pos, drop);
			}
		}
		level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(state));
		level.setBlock(pos, replanted ? crop.getStateForAge(0) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		return true;
	}
}

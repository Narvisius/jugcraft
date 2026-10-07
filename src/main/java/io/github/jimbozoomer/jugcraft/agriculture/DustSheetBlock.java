package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Dust Sheet over a piece of furniture: an old white sheet draped over a chair, a table (slabs, stairs), a chest or
 * another block of the {@code jugcraft:dust_sheet_coverable} block tag, for the look of a house shut up long ago. The
 * sheet takes the furniture's place and keeps it ({@link DustSheetBlockEntity}): its shape (so the sheet drapes over
 * it and it still blocks the way), how long it takes to break, what it drops, and a chest's contents. Use it with an
 * empty hand to pull the sheet off: the furniture is back as it was, and the sheet goes back to the player. Breaking a
 * covered block drops the sheet and whatever the block would drop (spilling a chest's contents). The client draws the
 * sheet (client/DustSheetRenderer.java); at night one sheet in {@value #BREATHE_CHANCE} seems to breathe.
 */
public class DustSheetBlock extends BaseEntityBlock {
	public static final int BREATHE_CHANCE = 3;
	/** Under a sheet that has lost its block. */
	private static final VoxelShape FALLBACK = Block.box(1.0, 0.0, 1.0, 15.0, 8.0, 15.0);

	public DustSheetBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DustSheetBlockEntity(pos, state);
	}

	/** The block under the sheet at {@code pos}, or null. */
	public static @Nullable BlockState covered(BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof DustSheetBlockEntity sheet && !sheet.covered().isAir() ? sheet.covered() : null;
	}

	/** Whether the sheet at {@code pos} seems to breathe at night (one in {@value #BREATHE_CHANCE}, by where it is). */
	public static boolean breathes(BlockPos pos) {
		return Math.floorMod(pos.hashCode() * 31 + 7, BREATHE_CHANCE) == 0;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		BlockState covered = covered(level, pos);
		return covered != null ? covered.getShape(level, pos, context) : FALLBACK;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		BlockState covered = covered(level, pos);
		return covered != null ? covered.getCollisionShape(level, pos, context) : FALLBACK;
	}

	/** It takes as long to break as the block under it. */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		BlockState covered = covered(level, pos);
		return covered != null ? covered.getDestroyProgress(player, level, pos) : super.getDestroyProgress(state, player, level, pos);
	}

	/**
	 * The sheet, and what the covered block would drop (as its own block entity would have it), if the player breaking
	 * it has the tool it needs. A covered chest's contents spill from the block entity.
	 */
	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
		if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof DustSheetBlockEntity sheet) || sheet.covered().isAir()) {
			return drops;
		}
		BlockState covered = sheet.covered();
		Entity breaker = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
		if (breaker instanceof Player player && covered.requiresCorrectToolForDrops() && !player.hasCorrectToolForDrops(covered)) {
			return drops;
		}
		BlockEntity rebuilt = sheet.rebuild(params.getLevel());
		if (rebuilt != null) {
			params.withParameter(LootContextParams.BLOCK_ENTITY, rebuilt);
		}
		drops.addAll(covered.getDrops(params));
		return drops;
	}

	/** A Dust Sheet, whatever is under it. */
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(JugcraftAgriculture.item(JugcraftAgriculture.DUST_SHEET));
	}

	/** Whether a Dust Sheet may go over the block at {@code pos}: one of the tag's, holding nothing a sheet could lose. */
	public static boolean coverable(Level level, BlockPos pos, BlockState state) {
		if (!state.is(JugcraftAgriculture.DUST_SHEET_COVERABLE)) {
			return false;
		}
		BlockEntity entity = level.getBlockEntity(pos);
		return entity == null || entity instanceof Container || entity instanceof DecorationBlockEntity;
	}

	/**
	 * Puts a Dust Sheet over the block at {@code pos}, keeping the block and its block entity's data under it. Anyone
	 * looking into it (a chest) has it closed first; its contents move under the sheet, not onto the floor.
	 */
	public static void cover(ServerLevel level, BlockPos pos, @Nullable Player player) {
		BlockState state = level.getBlockState(pos);
		BlockEntity entity = level.getBlockEntity(pos);
		CompoundTag data = null;
		if (entity != null) {
			if (entity instanceof Container container) {
				for (ServerPlayer viewer : level.players()) {
					if (viewer.containerMenu != viewer.inventoryMenu && looksInto(viewer, container)) {
						viewer.closeContainer();
					}
				}
			}
			data = entity.saveWithFullMetadata(level.registryAccess());
			if (entity instanceof Container container) {
				container.clearContent(); // Now kept under the sheet, so replacing the block must not spill it.
			}
		}
		level.setBlock(pos, JugcraftAgriculture.block(JugcraftAgriculture.DUST_SHEET).defaultBlockState(), Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof DustSheetBlockEntity sheet) {
			sheet.cover(state, data);
		}
		level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
	}

	private static boolean looksInto(ServerPlayer viewer, Container container) {
		for (Slot slot : viewer.containerMenu.slots) {
			if (slot.container == container || slot.container instanceof CompoundContainer both && both.contains(container)) {
				return true;
			}
		}
		return false;
	}

	/** Pulls the sheet off: the block comes back as it was, and the sheet goes to the player (unless in creative). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		uncover(server, pos);
		if (!player.getAbilities().instabuild) {
			ItemStack sheet = new ItemStack(JugcraftAgriculture.item(JugcraftAgriculture.DUST_SHEET));
			if (!player.getInventory().add(sheet)) {
				Block.popResource(level, pos, sheet);
			}
		}
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}

	/** Takes the sheet off the block at {@code pos}, putting back what was under it (and its block entity's data). */
	public static void uncover(ServerLevel level, BlockPos pos) {
		BlockState covered = Blocks.AIR.defaultBlockState();
		CompoundTag data = null;
		if (level.getBlockEntity(pos) instanceof DustSheetBlockEntity sheet) {
			covered = sheet.covered();
			data = sheet.coveredData();
			sheet.release();
		}
		BlockState restored = covered.isAir() ? covered : Block.updateFromNeighbourShapes(covered, level, pos);
		level.setBlock(pos, restored, Block.UPDATE_ALL);
		if (data != null && !restored.isAir()) {
			BlockEntity entity = BlockEntity.loadStatic(pos, restored, data, level.registryAccess());
			if (entity != null) {
				level.setBlockEntity(entity);
				entity.setChanged();
			}
		}
		level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 1.2F);
		level.sendParticles(ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 20, 0.4, 0.3, 0.4, 0.02);
	}
}

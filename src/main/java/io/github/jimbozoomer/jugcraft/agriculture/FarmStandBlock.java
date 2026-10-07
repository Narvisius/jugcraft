package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.town.TownProtection;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Farm Stand (Halloween decorations batch 20, Pumpkin Night): a two-block roadside stall of weathered planks under a
 * striped orange-and-white awning, a slate price board on a post and {@value FarmStandBlockEntity#CRATES} slatted crates
 * tilted towards the road. Whoever places it owns it: they stock the crates and chalk a price in Jugs on each through its
 * screen ({@link FarmStandMenu}); anyone else in reach buys from it one item at a time, the Jugs going straight to the
 * owner. Only the owner (or an operator in creative) can take it down; it shrugs off explosions and pistons. Using it
 * opens the screen; it has no lanterns to light ({@link #LIT} stays on).
 */
public class FarmStandBlock extends MultiDecorationBlock implements EntityBlock {
	private static final int[][] CELLS = {{0, 0}, {1, 0}};
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, CELLS.length - 1);
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

	public FarmStandBlock(Properties properties) {
		super(properties);
	}

	@Override
	public int[][] cells() {
		return CELLS;
	}

	@Override
	public IntegerProperty partProperty() {
		return PART;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** The stand's records, on its first block, from any of its blocks. */
	public @Nullable FarmStandBlockEntity stand(Level level, BlockPos pos, BlockState state) {
		return level.getBlockEntity(masterPos(pos, state)) instanceof FarmStandBlockEntity stand ? stand : null;
	}

	/** Whoever places it owns it. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide() && placer instanceof Player player && level.getBlockEntity(pos) instanceof FarmStandBlockEntity stand) {
			stand.setOwner(player);
		}
	}

	/** Opens the stand's screen (a stand with no owner, set by a command, goes to the first player to use it). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server) {
			FarmStandBlockEntity stand = stand(level, pos, state);
			if (stand == null) {
				return InteractionResult.PASS;
			}
			if (stand.owner() == null) {
				stand.setOwner(player);
			}
			FarmStandMenu.open(server, stand);
		}
		return InteractionResult.SUCCESS;
	}

	/** Whether {@code player} may take down the stand: its owner, an operator in creative, or anyone if it has no owner. */
	public static boolean mayBreak(Player player, FarmStandBlockEntity stand) {
		return stand.owner() == null || stand.isOwner(player) || TownProtection.exempt(player);
	}

	/** Only the owner breaks a stand: everyone else is told whose it is. */
	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (level.isClientSide() || !(state.getBlock() instanceof FarmStandBlock block)) {
				return true;
			}
			FarmStandBlockEntity stand = block.stand(level, pos, state);
			if (stand == null || mayBreak(player, stand)) {
				return true;
			}
			player.sendOverlayMessage(Component.translatable("message.jugcraft.farm_stand.locked", stand.ownerName()));
			return false;
		});
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return part(state) == MASTER ? new FarmStandBlockEntity(pos, state) : null;
	}
}

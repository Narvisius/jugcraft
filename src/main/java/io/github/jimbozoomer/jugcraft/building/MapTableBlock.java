package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.artillery.Spotting;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Map Table (batch 59): a campaign map on a table, where a dugout's officers plot what the spotters report.
 *
 * <p>Use it to read the plot: the target marks ({@link Spotting}, made with a Range Finder or a Trench Periscope) in this
 * dimension pointing within {@value Bunkerworks#MAP_RANGE} blocks of the table, nearest first, at most
 * {@value Bunkerworks#MAP_LINES}: who marked it, how far and which way from the table, and how long ago. Sneak and use it
 * to hand the next mark on the plot to a Fire Control Table within {@value Bunkerworks#MAP_LINK} blocks: the mark after
 * that table's present target, round to the first again after the last.
 */
public class MapTableBlock extends HorizontalDirectionalBlock {
	private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 12, 0, 16, 14.5, 16), Block.box(1, 0, 1, 15, 12, 15));

	public MapTableBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		List<Spotting.Plot> plots = Spotting.near(server, pos, Bunkerworks.MAP_RANGE);
		level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.0F);
		if (player.isShiftKeyDown()) {
			plotForFireControl(server, pos, player, plots);
			return InteractionResult.SUCCESS;
		}
		if (plots.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.map_table.none", Bunkerworks.MAP_RANGE));
			return InteractionResult.SUCCESS;
		}
		long now = server.getGameTime();
		for (Spotting.Plot plot : plots.subList(0, Math.min(Bunkerworks.MAP_LINES, plots.size()))) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.map_table.line", spotter(server, plot),
					distance(pos, plot.target()), bearing(pos, plot.target()), Math.max(0L, (now - plot.time()) / 20L)));
		}
		return InteractionResult.SUCCESS;
	}

	/** Sneak-use: the next mark on the plot becomes the target of the nearest fire control table within reach. */
	private static void plotForFireControl(ServerLevel level, BlockPos pos, Player player, List<Spotting.Plot> plots) {
		FireControlTableBlock.Entity table = nearestFireControl(level, pos);
		if (table == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.map_table.no_fire_control", Bunkerworks.MAP_LINK));
			return;
		}
		if (plots.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.map_table.none", Bunkerworks.MAP_RANGE));
			return;
		}
		int current = -1;
		BlockPos target = table.target();
		for (int i = 0; i < plots.size(); i++) {
			if (plots.get(i).target().equals(target)) {
				current = i;
				break;
			}
		}
		Spotting.Plot next = plots.get((current + 1) % plots.size());
		table.setTarget(next.target());
		player.sendOverlayMessage(Component.translatable("message.jugcraft.map_table.plotted", spotter(level, next),
				distance(table.getBlockPos(), next.target()), bearing(table.getBlockPos(), next.target())));
	}

	/** The fire control table nearest {@code pos}, within {@value Bunkerworks#MAP_LINK} blocks every way, if any. */
	public static FireControlTableBlock.@Nullable Entity nearestFireControl(Level level, BlockPos pos) {
		FireControlTableBlock.Entity best = null;
		double bestDistance = Double.MAX_VALUE;
		int r = Bunkerworks.MAP_LINK;
		for (BlockPos at : BlockPos.betweenClosed(pos.offset(-r, -r, -r), pos.offset(r, r, r))) {
			if (level.getBlockEntity(at) instanceof FireControlTableBlock.Entity table && at.distSqr(pos) < bestDistance) {
				best = table;
				bestDistance = at.distSqr(pos);
			}
		}
		return best;
	}

	private static Component spotter(ServerLevel level, Spotting.Plot plot) {
		ServerPlayer who = level.getServer().getPlayerList().getPlayer(plot.spotter());
		return who != null ? who.getDisplayName() : Component.translatable("message.jugcraft.map_table.someone");
	}

	private static int distance(BlockPos from, BlockPos to) {
		return (int) Math.round(Math.sqrt(from.distSqr(to)));
	}

	private static String bearing(BlockPos from, BlockPos to) {
		return Bunkerworks.bearing(to.getX() - from.getX(), to.getZ() - from.getZ());
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}

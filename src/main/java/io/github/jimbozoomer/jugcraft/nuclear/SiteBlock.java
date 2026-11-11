package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Plant Structure (towers and pump houses build, 19 October 2026): a block of a compiled site, the concrete, steel and
 * equipment of a cooling tower or a pump house in the part of it this block holds. It has no look or shape of its own:
 * its site (found through its anchor, SiteRegistry) gives both, so a whole structure of smooth, detailed geometry is
 * built and mined block by block, and drawn with the world's own chunks (client/SiteModel.java). A block with no
 * site loaded stands as a full block. Clicking a control in it (a door, a switch, a handwheel) works that control.
 * The ladder variant is climbable (in the climbable tag).
 */
public class SiteBlock extends Block implements SimpleWaterloggedBlock {
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	public SiteBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return !state.getValue(WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape s = shape(level, pos);
		// A doorway can be clicked (to shut its door) with the door standing open: its zone is part of the outline.
		SiteRegistry.Hit hit = SiteRegistry.find(level, pos);
		if (hit != null) {
			VoxelShape z = Shapes.empty();
			for (SiteData.Zone zone : hit.placement().data().zones) {
				if (!zone.name().startsWith("door:")) continue;
				AABB b = zone.box();
				double x0 = Math.max(b.minX - hit.x(), 0), y0 = Math.max(b.minY - hit.y(), 0), z0 = Math.max(b.minZ - hit.z(), 0);
				double x1 = Math.min(b.maxX - hit.x(), 1), y1 = Math.min(b.maxY - hit.y(), 1), z1 = Math.min(b.maxZ - hit.z(), 1);
				if (x1 > x0 && y1 > y0 && z1 > z0) {
					z = Shapes.joinUnoptimized(z, Shapes.box(x0, y0, z0, x1, y1, z1), BooleanOp.OR);
				}
			}
			if (!z.isEmpty()) {
				s = Shapes.joinUnoptimized(s, turn(z.optimize(), hit.placement().facing()), BooleanOp.OR).optimize();
			}
		}
		return s;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(level, pos);
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState state) {
		return Shapes.empty();
	}

	/** The block's shape: its site's collision in this cell (turned with the site), its live parts' (a shut door's leaf). */
	public static VoxelShape shape(BlockGetter level, BlockPos pos) {
		SiteRegistry.Hit hit = SiteRegistry.find(level, pos);
		if (hit == null) {
			return Shapes.block();
		}
		SiteRegistry.Placement p = hit.placement();
		SiteData d = p.data();
		VoxelShape s = d.shape(d.cell(hit.x(), hit.y(), hit.z()), hit.x(), hit.y(), hit.z());
		// Live groups' collision in this cell, for the values the anchor shows now.
		if (level instanceof Level lv && SiteRegistry.anchorOf(lv, p) instanceof SiteAnchor anchor) {
			SiteData.Group[][] table = anchor.groups();
			for (int v : d.solidVariables()) {
				if (v >= table.length) continue;
				int value = anchor.value(v);
				SiteData.Group g = value >= 0 && value < table[v].length ? table[v][value] : null;
				if (g == null || g.boxes().length == 0) continue;
				float[] b = g.boxes();
				for (int i = 0; i < b.length; i += 6) {
					double x0 = Math.max(b[i] - hit.x(), 0), y0 = Math.max(b[i + 1] - hit.y(), 0), z0 = Math.max(b[i + 2] - hit.z(), 0);
					double x1 = Math.min(b[i + 3] - hit.x(), 1), y1 = Math.min(b[i + 4] - hit.y(), 1), z1 = Math.min(b[i + 5] - hit.z(), 1);
					if (x1 > x0 && y1 > y0 && z1 > z0) {
						s = Shapes.joinUnoptimized(s, Shapes.box(x0, y0, z0, x1, y1, z1), BooleanOp.OR);
					}
				}
			}
		}
		return turn(s, p.facing());
	}

	/** A shape in the site's frame turned to the site's facing (about the block's middle). */
	static VoxelShape turn(VoxelShape s, Direction facing) {
		if (facing == Direction.SOUTH || s.isEmpty()) {
			return s;
		}
		List<AABB> boxes = new ArrayList<>(s.toAabbs());
		VoxelShape out = Shapes.empty();
		for (AABB b : boxes) {
			double x0 = b.minX - 0.5, x1 = b.maxX - 0.5, z0 = b.minZ - 0.5, z1 = b.maxZ - 0.5;
			double nx0, nx1, nz0, nz1;
			switch (facing) {
				case WEST -> {      // (x, z) -> (-z, x)
					nx0 = -z1;
					nx1 = -z0;
					nz0 = x0;
					nz1 = x1;
				}
				case NORTH -> {     // (x, z) -> (-x, -z)
					nx0 = -x1;
					nx1 = -x0;
					nz0 = -z1;
					nz1 = -z0;
				}
				default -> {        // east: (x, z) -> (z, -x)
					nx0 = z0;
					nx1 = z1;
					nz0 = -x1;
					nz1 = -x0;
				}
			}
			out = Shapes.joinUnoptimized(out, Shapes.box(nx0 + 0.5, b.minY, nz0 + 0.5, nx1 + 0.5, b.maxY, nz1 + 0.5), BooleanOp.OR);
		}
		return out.optimize();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		SiteRegistry.Hit h = SiteRegistry.find(level, pos);
		if (h == null || !(SiteRegistry.anchorOf(level, h.placement()) instanceof SiteAnchor anchor)) {
			return InteractionResult.PASS;
		}
		Vec3 at = h.placement().localPoint(hit.getLocation());
		SiteData.Zone best = null;
		double bestD = Double.MAX_VALUE;
		for (SiteData.Zone z : h.placement().data().zones) {
			if (!z.box().inflate(0.06).contains(at)) continue;
			double d = z.box().getCenter().distanceToSqr(at);
			if (d < bestD) {
				best = z;
				bestD = d;
			}
		}
		if (best == null) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server) {
			anchor.onZone(server, player, best.name());
			return InteractionResult.SUCCESS_SERVER;
		}
		return InteractionResult.SUCCESS;
	}
}

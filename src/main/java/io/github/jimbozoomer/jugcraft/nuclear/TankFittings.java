package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The roof and shell fittings that go with a tank's rating (TankRating), swapped when a placed tank is re-rated
 * (TankUpgrades): the roof vent (a gooseneck, a pressure/vacuum vent on a flame arrester, a scrubber vent) and, on a
 * flammable tank, two foam chambers on the top course with their foam lines down the shell to the foot, and an earthing
 * boss at the foot. A tank kit lays the same fittings (tools/tank_kits.py), and the corrosive kit's spill bund too.
 */
public final class TankFittings {
	private TankFittings() {
	}

	/** The roof vent for a rating. */
	public static Block vent(TankRating rating) {
		return switch (rating) {
			case FLAMMABLE -> StorageTanks.BLOCKS.get("storage_tank_pv_vent");
			case CORROSIVE -> StorageTanks.BLOCKS.get("storage_tank_scrubber_vent");
			case CRYOGENIC -> StorageTanks.BLOCKS.get("tank_relief_valves");
			default -> StorageTanks.BLOCKS.get("storage_tank_vent");
		};
	}

	private static boolean isVent(BlockState state) {
		for (TankRating rating : TankRating.values()) {
			if (state.is(vent(rating))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isFoam(BlockState state) {
		return state.is(StorageTanks.BLOCKS.get("tank_foam_chamber")) || state.is(StorageTanks.BLOCKS.get("tank_foam_line"))
				|| state.is(StorageTanks.BLOCKS.get("tank_earthing_boss"));
	}

	/** Swaps a formed tank's fittings for its rating's. */
	public static void refit(ServerLevel level, StorageTankPortBlockEntity tank, TankRating rating) {
		List<BlockPos> plates = tank.platePositions();
		int flags = Block.UPDATE_ALL;
		// The roof vent(s): whatever stands on the roof.
		Block vent = vent(rating);
		for (BlockPos p : plates) {
			BlockPos above = p.above();
			BlockState there = level.getBlockState(above);
			if (isVent(there) && !there.is(vent)) {
				BlockState fresh = vent.defaultBlockState();
				if (there.hasProperty(HorizontalDirectionalBlock.FACING) && fresh.hasProperty(HorizontalDirectionalBlock.FACING)) {
					fresh = fresh.setValue(HorizontalDirectionalBlock.FACING, there.getValue(HorizontalDirectionalBlock.FACING));
				}
				level.setBlock(above, fresh, flags);
			}
		}
		// Foam chambers, foam lines and the earthing boss come off; a flammable tank gets them back.
		for (BlockPos p : plates) {
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos q = p.relative(d);
				if (isFoam(level.getBlockState(q))) {
					level.setBlock(q, Blocks.AIR.defaultBlockState(), flags);
				}
			}
		}
		if (rating == TankRating.FLAMMABLE) {
			foam(level, plates);
		}
	}

	/** Two foam chambers on the top course, as far apart as they can be, with their lines down to the foot; an earthing boss. */
	private static void foam(ServerLevel level, List<BlockPos> plates) {
		int top = Integer.MIN_VALUE, bottom = Integer.MAX_VALUE;
		double cx = 0, cz = 0;
		int walls = 0;
		for (BlockPos p : plates) {
			BlockState state = level.getBlockState(p);
			if (!(state.getBlock() instanceof StorageTankPlateBlock)) {
				continue;
			}
			StorageTankPlateBlock.Band band = state.getValue(StorageTankPlateBlock.BAND);
			if (band == StorageTankPlateBlock.Band.BAND) {
				top = Math.max(top, p.getY());
			}
			if (band != StorageTankPlateBlock.Band.ROOF && band != StorageTankPlateBlock.Band.RIM) {
				bottom = Math.min(bottom, p.getY());
				cx += p.getX() + 0.5;
				cz += p.getZ() + 0.5;
				walls++;
			}
		}
		if (walls == 0 || top == Integer.MIN_VALUE) {
			return;
		}
		cx /= walls;
		cz /= walls;
		// Outside cells beside the top course whose column is clear down to the foot.
		List<Spot> spots = new ArrayList<>();
		for (BlockPos p : plates) {
			if (p.getY() != top) {
				continue;
			}
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos out = p.relative(d);
				double ox = out.getX() + 0.5 - cx, oz = out.getZ() + 0.5 - cz;
				if (ox * d.getStepX() + oz * d.getStepZ() <= 0 || !clearDown(level, out, bottom)) {
					continue;
				}
				spots.add(new Spot(out, d, Math.atan2(oz, ox)));
			}
		}
		Spot first = best(spots, Math.PI / 4, null);
		if (first == null) {
			return;
		}
		Spot second = best(spots, first.angle + Math.PI, first);
		BlockState chamber = StorageTanks.BLOCKS.get("tank_foam_chamber").defaultBlockState();
		BlockState line = StorageTanks.BLOCKS.get("tank_foam_line").defaultBlockState();
		for (Spot spot : second == null ? List.of(first) : List.of(first, second)) {
			level.setBlock(spot.pos, chamber.setValue(HorizontalDirectionalBlock.FACING, spot.facing), Block.UPDATE_ALL);
			for (int y = spot.pos.getY() - 1; y >= bottom; y--) {
				level.setBlock(spot.pos.atY(y), line.setValue(HorizontalDirectionalBlock.FACING, spot.facing), Block.UPDATE_ALL);
			}
		}
		// The earthing boss at the foot, a quarter turn round from the first chamber.
		List<Spot> foot = new ArrayList<>();
		for (BlockPos p : plates) {
			if (p.getY() != bottom) {
				continue;
			}
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos out = p.relative(d);
				double ox = out.getX() + 0.5 - cx, oz = out.getZ() + 0.5 - cz;
				if (ox * d.getStepX() + oz * d.getStepZ() > 0 && level.getBlockState(out).isAir()) {
					foot.add(new Spot(out, d, Math.atan2(oz, ox)));
				}
			}
		}
		Spot boss = best(foot, first.angle + Math.PI / 2, null);
		if (boss != null) {
			level.setBlock(boss.pos, StorageTanks.BLOCKS.get("tank_earthing_boss").defaultBlockState()
					.setValue(HorizontalDirectionalBlock.FACING, boss.facing), Block.UPDATE_ALL);
		}
	}

	private record Spot(BlockPos pos, Direction facing, double angle) {
	}

	private static @Nullable Spot best(List<Spot> spots, double angle, @Nullable Spot not) {
		Spot best = null;
		double bestOff = Double.MAX_VALUE;
		for (Spot s : spots) {
			if (not != null && s.pos.distManhattan(not.pos) < 3) {
				continue;
			}
			double off = Math.abs(Math.atan2(Math.sin(s.angle - angle), Math.cos(s.angle - angle)));
			if (off < bestOff) {
				bestOff = off;
				best = s;
			}
		}
		return best;
	}

	private static boolean clearDown(ServerLevel level, BlockPos from, int bottom) {
		for (int y = from.getY(); y >= bottom; y--) {
			if (!level.getBlockState(from.atY(y)).isAir()) {
				return false;
			}
		}
		return true;
	}
}

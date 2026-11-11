package io.github.jimbozoomer.jugcraft.sound;

import io.github.jimbozoomer.jugcraft.nuclear.EquipmentFrameBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * How much wall stands between a sound and an ear, in blocks of solid wall: what client/SoundOcclusion (every sound in
 * the world) and the TVs (client/tv/TvPlayer) muffle by. Only reads the level, so it is the same on both sides and the
 * server's tests check it. A closed door counts nearly as a wall, glass a little less, a pane, slab or piece of
 * furniture a third, wool more than stone; open doors, bars, fences, plants and the big machines' frames let sound
 * through. A block that doesn't fill its cell (a door, a sofa, a TV's panel, a slab) counts only where the line goes
 * through what's there, not past it over a seat or in front of a panel (Justin, 10 November 2026: the TV's sound cut by
 * the upstairs sofa in front of it).
 */
public final class SoundWalls {
	private static final Vec3[] AROUND = {Vec3.ZERO, new Vec3(0.35, 0, 0), new Vec3(-0.35, 0, 0), new Vec3(0, 0, 0.35), new Vec3(0, 0, -0.35),
			new Vec3(0, 0.35, 0)};

	private SoundWalls() {
	}

	/** How much wall stands between the ear and a sound (in blocks of solid wall): the clearest of a few lines to it. */
	public static double walls(Level level, Vec3 ear, Vec3 at) {
		BlockPos source = BlockPos.containing(at);
		double best = Double.MAX_VALUE;
		for (Vec3 off : AROUND) {
			best = Math.min(best, along(level, ear, at.add(off), source));
			if (best < 0.02) {
				break;
			}
		}
		return best;
	}

	/**
	 * The wall along one line, in blocks of wall: walked cell by cell, each run of solid cells counted by how thick it is
	 * across (measured along the face the line came in through), not by how long the line runs inside it, so a wall one
	 * block thick counts one however slanting the line through it.
	 */
	private static double along(Level level, Vec3 from, Vec3 to, BlockPos source) {
		double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length < 1e-6) {
			return 0;
		}
		double[] u = {dx / length, dy / length, dz / length};
		int[] cell = {Mth.floor(from.x), Mth.floor(from.y), Mth.floor(from.z)};
		double[] start = {from.x, from.y, from.z};
		int[] step = new int[3];
		double[] next = new double[3], delta = new double[3];
		for (int a = 0; a < 3; a++) {
			step[a] = u[a] > 0 ? 1 : u[a] < 0 ? -1 : 0;
			delta[a] = step[a] == 0 ? Double.MAX_VALUE : 1 / Math.abs(u[a]);
			next[a] = step[a] == 0 ? Double.MAX_VALUE
					: (step[a] > 0 ? cell[a] + 1 - start[a] : start[a] - cell[a]) / Math.abs(u[a]);
		}
		int dominant = Math.abs(u[0]) >= Math.abs(u[1]) && Math.abs(u[0]) >= Math.abs(u[2]) ? 0 : Math.abs(u[1]) >= Math.abs(u[2]) ? 1 : 2;
		BlockPos ear = BlockPos.containing(from);
		double t = 0, total = 0;
		int entered = -1, run = -1;
		for (int guard = 0; guard < 1024; guard++) {
			double leave = Math.min(Math.min(next[0], next[1]), Math.min(next[2], length));
			boolean skipped = cell[0] == ear.getX() && cell[1] == ear.getY() && cell[2] == ear.getZ()
					|| cell[0] == source.getX() && cell[1] == source.getY() && cell[2] == source.getZ();
			double wall = skipped ? 0 : block(level, new BlockPos(cell[0], cell[1], cell[2]),
					new Vec3(from.x + u[0] * t, from.y + u[1] * t, from.z + u[2] * t),
					new Vec3(from.x + u[0] * leave, from.y + u[1] * leave, from.z + u[2] * leave));
			if (wall > 0) {
				if (run < 0) {
					run = entered < 0 ? dominant : entered;
				}
				total += wall * (leave - t) * Math.abs(u[run]);
				if (total >= 2) {
					return total;
				}
			} else {
				run = -1;
			}
			if (leave >= length) {
				break;
			}
			t = leave;
			int a = next[0] <= next[1] && next[0] <= next[2] ? 0 : next[1] <= next[2] ? 1 : 2;
			cell[a] += step[a];
			next[a] += delta[a];
			entered = a;
		}
		return total;
	}

	/**
	 * How much of a wall one block is, for the line from {@code in} to {@code out} through its cell: a block that doesn't
	 * fill the cell counts only if the line goes through it.
	 */
	static double block(Level level, BlockPos pos, Vec3 in, Vec3 out) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir()) {
			return 0;
		}
		double wall = block(level, pos, state);
		if (wall > 0 && !state.is(BlockTags.WOOL)) {
			VoxelShape shape = state.getCollisionShape(level, pos);
			if (!shape.isEmpty() && !Block.isShapeFullBlock(shape) && shape.clip(in, out, pos) == null) {
				return 0;                                            // (past it: over the seat, in front of the panel)
			}
		}
		return wall;
	}

	/** How much of a wall one block is, where the line goes through it. */
	static double block(Level level, BlockPos pos, BlockState state) {
		Block block = state.getBlock();
		if (block instanceof DoorBlock) {
			return state.getValue(DoorBlock.OPEN) ? 0.05 : 0.9;
		}
		if (block instanceof TrapDoorBlock) {
			return state.getValue(TrapDoorBlock.OPEN) ? 0.05 : 0.5;
		}
		if (block instanceof FenceGateBlock || block instanceof FenceBlock) {
			return 0.05;
		}
		if (state.is(BlockTags.WOOL)) {
			return 1.5;
		}
		if (state.is(BlockTags.WOOL_CARPETS) || state.is(BlockTags.LEAVES)) {
			return 0.15;
		}
		// The big machines' invisible frames: machinery, not wall (and a machine's own sound comes from inside them).
		if (block instanceof EquipmentFrameBlock) {
			return 0;
		}
		VoxelShape shape = state.getCollisionShape(level, pos);
		if (shape.isEmpty()) {
			return state.getFluidState().isEmpty() ? 0 : 0.4;
		}
		boolean glass = state.getSoundType() == SoundType.GLASS;
		if (block instanceof IronBarsBlock) {
			return glass ? 0.4 : 0.05;
		}
		if (Block.isShapeFullBlock(shape)) {
			return glass ? 0.7 : 1.0;
		}
		return 0.35;
	}
}

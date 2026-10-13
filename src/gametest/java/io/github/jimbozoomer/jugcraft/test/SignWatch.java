package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Roadmap step 27: the signs the server decides to show inside one test's area, for checking that what is shown is
 * what really happened. Tests share a level, so only signs in this test's own blocks count. Close it when done (a
 * failed test that leaves one open only keeps a short list).
 */
final class SignWatch implements AutoCloseable {
	/** How far from the test's corner its blocks reach: the empty test structure's 8 blocks, which these tests build in. */
	private static final int SIZE = 8;

	private final GameTestHelper helper;
	private final AABB area;
	private final List<Signs.Shown> seen = new ArrayList<>();
	private final AutoCloseable handle;

	SignWatch(GameTestHelper helper) {
		this.helper = helper;
		BlockPos corner = helper.absolutePos(BlockPos.ZERO);
		this.area = new AABB(corner.getX(), corner.getY(), corner.getZ(), corner.getX() + SIZE, corner.getY() + SIZE, corner.getZ() + SIZE);
		this.handle = Signs.observe(shown -> {
			if (shown.level() == helper.getLevel() && area.contains(shown.at())) {
				seen.add(shown);
			}
		});
	}

	/** Every sign of {@code sign}'s kind seen so far. */
	List<Signs.Shown> of(Sign sign) {
		return seen.stream().filter(shown -> shown.sign() == sign).toList();
	}

	/** How many of {@code sign} were shown at the block {@code relative} (a sign stands a little above its block). */
	long at(Sign sign, BlockPos relative) {
		Vec3 centre = Vec3.atCenterOf(helper.absolutePos(relative));
		return of(sign).stream().filter(shown -> shown.at().distanceTo(centre) <= 1.0).count();
	}

	/** How many of {@code sign} travelled from the block {@code from} to the block {@code to}. */
	long flowed(Sign sign, BlockPos from, BlockPos to) {
		Vec3 source = Vec3.atCenterOf(helper.absolutePos(from));
		Vec3 target = Vec3.atCenterOf(helper.absolutePos(to));
		return of(sign).stream().filter(shown -> shown.from() != null && shown.from().distanceTo(source) <= 1.0
				&& shown.at().distanceTo(target) <= 1.0).count();
	}

	List<Signs.Shown> all() {
		return List.copyOf(seen);
	}

	@Override
	public void close() {
		try {
			handle.close();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}

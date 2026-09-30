package io.github.jimbozoomer.jugcraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Where the ore drill digs and what it leaves behind. It works through a (2R+1)x(2R+1) column
 * below itself (R = {@link MachineKind#DRILL_RADIUS}), one layer at a time from the top, down to the
 * bottom of the world. Only blocks in {@code c:ores} are taken, as whole ore blocks (like silk touch),
 * so ore processing still decides the yield; the hole is refilled with the ore's host rock.
 */
public final class OreDrilling {
	public static final TagKey<Block> ORES = tag("ores");
	private static final TagKey<Block> IN_DEEPSLATE = tag("ores_in_ground/deepslate");
	private static final TagKey<Block> IN_NETHERRACK = tag("ores_in_ground/netherrack");

	private OreDrilling() {
	}

	private static TagKey<Block> tag(String path) {
		return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", path));
	}

	/** Blocks in one layer of the drilled column. */
	public static int layerSize() {
		int side = 2 * MachineKind.DRILL_RADIUS + 1;
		return side * side;
	}

	/** The block at position {@code cursor} of the drill's search order, or null once it is below the world. */
	public static @Nullable BlockPos target(BlockPos drill, int cursor, int minY) {
		int side = 2 * MachineKind.DRILL_RADIUS + 1;
		int layer = cursor / layerSize();
		int index = cursor % layerSize();
		int y = drill.getY() - 1 - layer;
		if (y < minY) {
			return null;
		}
		return new BlockPos(drill.getX() - MachineKind.DRILL_RADIUS + index % side, y,
				drill.getZ() - MachineKind.DRILL_RADIUS + index / side);
	}

	public static boolean isOre(BlockState state) {
		return state.is(ORES);
	}

	/** The rock that replaces a mined ore: deepslate, netherrack or stone. */
	public static BlockState filler(BlockState ore) {
		if (ore.is(IN_DEEPSLATE)) {
			return Blocks.DEEPSLATE.defaultBlockState();
		}
		if (ore.is(IN_NETHERRACK)) {
			return Blocks.NETHERRACK.defaultBlockState();
		}
		return Blocks.STONE.defaultBlockState();
	}
}

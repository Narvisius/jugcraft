package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.*;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class DoorWreathGameTests {
	private static Block wreath(String name) { return JugcraftAgriculture.block(name); }
	private static void door(GameTestHelper h, BlockPos base, BlockState state) {
		var level = h.getLevel();
		level.setBlock(base.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(base, state, Block.UPDATE_ALL);
		level.setBlock(base.above(), state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
	}
	private static void open(GameTestHelper h, BlockPos base, boolean powered, boolean value) {
		var level = h.getLevel();
		if (powered) level.setBlock(base.below(), (value ? Blocks.REDSTONE_BLOCK : Blocks.STONE).defaultBlockState(), Block.UPDATE_ALL);
		else ((DoorBlock) level.getBlockState(base).getBlock()).setOpen(null, level, level.getBlockState(base), base, value);
	}
	private static Direction expectedFace(BlockState state, boolean front) {
		Direction facing = state.getValue(DoorBlock.FACING);
		Direction outward = !state.getValue(DoorBlock.OPEN) ? facing.getOpposite()
				: state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
		return front ? outward : outward.getOpposite();
	}
	public static DoorWreath at(GameTestHelper h, BlockPos support, boolean front) {
		var found = h.getLevel().getEntitiesOfClass(DoorWreath.class, new AABB(support).inflate(1),
				e -> e.getPos().equals(support) && e.isFront() == front);
		h.assertTrue(found.size() == 1, "Exactly one wreath per door half/face, got " + found.size());
		return found.getFirst();
	}
	private static void assertFace(GameTestHelper h, DoorWreath entity, Block block) {
		var level = h.getLevel(); var support = entity.getPos(); var state = level.getBlockState(support);
		Direction normal = expectedFace(state, entity.isFront());
		h.assertTrue(entity.getDirection() == normal && entity.wreathState().is(block)
				&& entity.wreathState().getValue(AutumnWreathBlock.FLOWERS) == AutumnWreathBlock.Mums.RED, "Swing changed material face, wreath or colour");
		var panel = state.getShape(level, support).bounds().move(support); var ring = entity.getBoundingBox();
		double gap = switch (normal) {
			case NORTH -> panel.minZ - ring.maxZ;
			case SOUTH -> ring.minZ - panel.maxZ;
			case WEST -> panel.minX - ring.maxX;
			case EAST -> ring.minX - panel.maxX;
			default -> throw new AssertionError();
		};
		h.assertTrue(Math.abs(gap) < 1.0E-6 && !panel.intersects(ring.deflate(1.0E-6)), "Wreath is inside/on the wrong side of the panel: " + gap);
		// The physical front is inside the cell when open, outside when closed. The old adjacent-block fix fails this.
		if (entity.isFront()) h.assertTrue(new AABB(support).contains(ring.getCenter()) == state.getValue(DoorBlock.OPEN), "Wreath jumped to the opposite face");
		var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		h.assertTrue(entity.save(saved), "Wreath cannot be saved");
		var loaded = new DoorWreath(JugcraftAgriculture.DOOR_WREATH, level);
		loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
		h.assertTrue(loaded.getPos().equals(support) && loaded.isFront() == entity.isFront() && loaded.wreathState().equals(entity.wreathState())
				&& loaded.getBoundingBox().equals(ring), "Save/load lost the face, anchor or appearance");
	}

	@GameTest(maxTicks = 500)
	public void wreathsStayOnBothPhysicalFacesThroughEveryHingeDirectionAndHalf(GameTestHelper h) {
		var level = h.getLevel(); var base = h.absolutePos(new BlockPos(4,2,4)); var area = new AABB(base).inflate(3); int sample = 0;
		var player = h.makeMockPlayer(GameType.SURVIVAL);
		for (String name : List.of("holly_wreath", "autumn_wreath")) for (var facing : Direction.Plane.HORIZONTAL)
			for (var hinge : DoorHingeSide.values()) for (boolean initialOpen : List.of(false, true)) {
				var block = wreath(name); boolean powered = name.equals("autumn_wreath");
				var closed = (powered ? Blocks.IRON_DOOR : Blocks.OAK_DOOR).defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, hinge);
				int start = 1 + sample++ * 14;
				h.runAtTickTime(start, () -> {
					for (var p : BlockPos.betweenClosed(base.offset(-2,0,-2), base.offset(2,2,2))) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					door(h, base, closed); open(h, base, powered, initialOpen);
					for (int half = 0; half < 2; half++) for (boolean front : List.of(false, true)) {
						var support = base.above(half); var normal = expectedFace(level.getBlockState(support), front);
						var stack = new ItemStack(block, 2); stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(AutumnWreathBlock.FLOWERS, AutumnWreathBlock.Mums.RED));
						player.setItemInHand(InteractionHand.MAIN_HAND, stack);
						var use = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(support), normal, support, false));
						stack.useOn(use); h.assertTrue(stack.getCount() == 1, "Door placement did not consume exactly one wreath");
						stack.useOn(use); h.assertTrue(stack.getCount() == 1, "Duplicate placement consumed an item");
						assertFace(h, at(h, support, front), block);
					}
					open(h, base, powered, !initialOpen);
				});
				h.runAtTickTime(start + 2, () -> {
					for (int half = 0; half < 2; half++) for (boolean front : List.of(false,true)) assertFace(h, at(h, base.above(half), front), block);
					open(h, base, powered, initialOpen);
				});
				h.runAtTickTime(start + 4, () -> {
					for (int half = 0; half < 2; half++) for (boolean front : List.of(false,true)) assertFace(h, at(h, base.above(half), front), block);
					open(h, base, powered, true);
				});
				h.runAtTickTime(start + 6, () -> {
					var outside = BlockPos.containing(at(h, base.above(), false).getBoundingBox().getCenter());
					h.assertTrue(!outside.equals(base.above()), "Back of open door should lie outside cell");
					level.setBlock(outside, Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
				});
				h.runAtTickTime(start + 8, () -> {
					h.assertTrue(level.getEntitiesOfClass(DoorWreath.class, area).size() == 3, "Blocked wreath did not drop exactly once");
					level.destroyBlock(base, false);
				});
				h.runAtTickTime(start + 10, () -> {
					h.assertTrue(level.getEntitiesOfClass(DoorWreath.class, area).isEmpty(), "Door loss left a floating wreath");
					var drops = level.getEntitiesOfClass(ItemEntity.class, area, e -> e.getItem().is(block.asItem()));
					h.assertTrue(drops.stream().mapToInt(e -> e.getItem().getCount()).sum() == 4, "Obstruction/support loss duplicated or lost wreaths");
					if (powered) for (var drop : drops) h.assertTrue(drop.getItem().get(DataComponents.BLOCK_STATE).get(AutumnWreathBlock.FLOWERS) == AutumnWreathBlock.Mums.RED, "Dropped wreath lost red mums");
					drops.forEach(ItemEntity::discard);
				});
			}
		h.runAtTickTime(sample * 14 + 1, h::succeed);
	}

	@GameTest
	public void wreathInteractionKeepsDoorUseMumColoursAndBreakDrops(GameTestHelper h) {
		var level = h.getLevel(); var base = h.absolutePos(new BlockPos(4,2,4)); door(h, base, Blocks.OAK_DOOR.defaultBlockState());
		var autumn = DoorWreath.attach(level, base.above(), true, wreath("autumn_wreath").defaultBlockState());
		var holly = DoorWreath.attach(level, base.above(), false, wreath("holly_wreath").defaultBlockState());
		var player = h.makeMockPlayer(GameType.SURVIVAL); var mums = new ItemStack(JugcraftAgriculture.item("red_mum"), 3); player.setItemInHand(InteractionHand.MAIN_HAND, mums);
		autumn.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO); autumn.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
		h.assertTrue(mums.getCount() == 2 && autumn.wreathState().getValue(AutumnWreathBlock.FLOWERS) == AutumnWreathBlock.Mums.RED, "Mum recolour consumed incorrectly");
		holly.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
		h.assertTrue(mums.getCount() == 2 && level.getBlockState(base).getValue(DoorBlock.OPEN), "Holly consumed mums or blocked opening door");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); autumn.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
		h.assertTrue(!level.getBlockState(base).getValue(DoorBlock.OPEN), "Clicking wreath did not close door");
		autumn.hurtServer(level, level.damageSources().playerAttack(player), 1); autumn.hurtServer(level, level.damageSources().playerAttack(player), 1);
		var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(3), e -> e.getItem().is(wreath("autumn_wreath").asItem()));
		h.assertTrue(drops.stream().mapToInt(e -> e.getItem().getCount()).sum() == 1, "Repeated attack duplicated drop");
		var creative = h.makeMockPlayer(GameType.CREATIVE);
		// The helper overrides gameMode(), but does not populate the abilities a real creative player receives.
		GameType.CREATIVE.updatePlayerAbilities(creative.getAbilities());
		holly.hurtServer(level, level.damageSources().playerAttack(creative), 1);
		h.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(3), e -> e.getItem().is(wreath("holly_wreath").asItem())).isEmpty(), "Creative breaking duplicated an item");
		h.succeed();
	}

	@GameTest
	public void legacyWreathBlocksMigrateWhenTheDoorIsOperated(GameTestHelper h) {
		var level = h.getLevel(); var base = h.absolutePos(new BlockPos(4,2,4)); door(h, base, Blocks.OAK_DOOR.defaultBlockState());
		var state = wreath("autumn_wreath").defaultBlockState().setValue(AutumnWreathBlock.FACING, Direction.SOUTH).setValue(AutumnWreathBlock.FLOWERS, AutumnWreathBlock.Mums.RED);
		level.setBlock(base.above().south(), state, Block.UPDATE_ALL);
		open(h, base, false, true);
		h.runAtTickTime(3, () -> {
			h.assertTrue(level.isEmptyBlock(base.above().south()), "Legacy block did not migrate");
			assertFace(h, at(h, base.above(), true), wreath("autumn_wreath")); h.succeed();
		});
	}
}

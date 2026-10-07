package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BoneWindChimesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DeadHollowTreeBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.GraspingHandsBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedArchwayBlock;
import io.github.jimbozoomer.jugcraft.agriculture.InflatableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MultiDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PorchWitchBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PorchWitchBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.PoseableSkeletonBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpookySignBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpookySignBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.WeathervaneBlock;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the yard and porch: the Yard Inflatables (blown up by hand and by redstone), the Porch Witch
 * (cackling at a visitor, once a visit), Grasping Hands (grabbing an ankle, resting), the Poseable Skeleton (its poses),
 * Bone Wind Chimes (hanging under a block, the weather), the Weathervanes (on a post, the wind), the Spooky Sign
 * (painted and written words, kept when broken), the Haunted Archway and the Dead Hollow Tree (placed and broken as
 * one, their lanterns), and that their data loads.
 */
public class Decor9GameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static void moveTo(GameTestHelper helper, ServerPlayer player, BlockPos standAt) {
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
	}

	private static BlockHitResult hit(GameTestHelper helper, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		return new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(helper, pos, side));
	}

	private static InteractionResult place(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, pos, side)));
	}

	private static List<ItemEntity> drops(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item));
	}

	private static int dropped(GameTestHelper helper, Item item) {
		return drops(helper, item).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static int light(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getLightEmission();
	}

	// ---------------------------------------------------------------- the yard inflatables

	/**
	 * Each design is its own block; an inflatable stands two blocks tall, flat until its blower runs; using it switches
	 * the blower on (both halves, light 7) and off; a redstone signal runs it too; it fills in 40 ticks and empties in
	 * 60; breaking the upper half drops it once.
	 */
	@GameTest(maxTicks = 40)
	public void inflatablesBlowUp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		for (String design : JugcraftAgriculture.INFLATABLE_DESIGNS) {
			helper.assertTrue(block("inflatable_" + design) instanceof InflatableBlock inflatable && inflatable.design().equals(design),
					"The " + design + " inflatable is a block of its own");
		}
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("inflatable_ghost")));
		player.setYRot(180.0F);
		BlockPos lower = new BlockPos(3, 2, 3);
		BlockPos upper = lower.above();
		place(helper, player, lower.below(), Direction.UP);
		helper.assertTrue(helper.getBlockState(lower).is(block("inflatable_ghost")) && helper.getBlockState(upper).getValue(InflatableBlock.HALF)
				== DoubleBlockHalf.UPPER, "It stands two blocks tall");
		helper.assertTrue(helper.getBlockEntity(lower, DecorationBlockEntity.class) != null, "The figure is drawn from the lower half");
		helper.assertFalse(InflatableBlock.inflated(helper.getBlockState(lower)) || light(helper, lower) > 0, "Placed, it is flat and dark");
		helper.assertTrue(helper.getBlockState(upper).getShape(level, helper.absolutePos(upper)).isEmpty(), "Flat, nothing of it is up high");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, upper, Direction.SOUTH);
		helper.assertTrue(helper.getBlockState(lower).getValue(InflatableBlock.ON) && helper.getBlockState(upper).getValue(InflatableBlock.ON)
				&& light(helper, lower) == InflatableBlock.LIGHT, "Used, its blower runs and it glows");
		use(helper, player, lower, Direction.SOUTH);
		helper.assertFalse(InflatableBlock.inflated(helper.getBlockState(lower)) || InflatableBlock.inflated(helper.getBlockState(upper)),
				"Used again, it goes flat");
		helper.setBlock(lower.east(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(lower).getValue(InflatableBlock.POWERED) && InflatableBlock.inflated(helper.getBlockState(upper)),
				"A redstone signal runs the blower");
		helper.setBlock(lower.east(), Blocks.AIR);
		helper.assertFalse(InflatableBlock.inflated(helper.getBlockState(lower)), "and without it, it goes flat");
		helper.assertTrue(InflatableBlock.fill(0.0F, true) == 1.0F / InflatableBlock.INFLATE_TICKS
				&& InflatableBlock.fill(1.0F, false) == 1.0F - 1.0F / InflatableBlock.DEFLATE_TICKS
				&& InflatableBlock.fill(1.0F, true) == 1.0F && InflatableBlock.fill(0.0F, false) == 0.0F, "It fills in 40 ticks and empties in 60");
		float[] wobble = InflatableBlock.wobble(helper.absolutePos(lower), 17.0F, 0.0F);
		helper.assertTrue(Math.abs(wobble[0]) <= InflatableBlock.WOBBLE_DEGREES && Math.abs(wobble[1]) <= InflatableBlock.WOBBLE_DEGREES,
				"It wobbles no more than its wobble");
		level.destroyBlock(helper.absolutePos(upper), true);
		helper.runAfterDelay(3, () -> {
			helper.assertBlockNotPresent(block("inflatable_ghost"), lower);
			helper.assertTrue(dropped(helper, item("inflatable_ghost")) == 1, "Broken, it drops once");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the porch witch

	/**
	 * Nobody near, she only stirs; a visitor walking up sets her cackling (both halves) and she settles after her time;
	 * a visitor who stays doesn't set her off again; one who goes and comes back after her rest does. Her spoon keeps to
	 * its round.
	 */
	@GameTest(maxTicks = 400)
	public void thePorchWitchCackles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos lower = new BlockPos(3, 2, 3);
		BlockState witch = block("porch_witch").defaultBlockState().setValue(PorchWitchBlock.FACING, Direction.SOUTH);
		helper.setBlock(lower, witch.setValue(PorchWitchBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(lower.above(), witch.setValue(PorchWitchBlock.HALF, DoubleBlockHalf.UPPER));
		PorchWitchBlockEntity entity = helper.getBlockEntity(lower, PorchWitchBlockEntity.class);
		for (int t = 0; t < PorchWitchBlock.STIR_PERIOD; t += 3) {
			float[] slow = PorchWitchBlock.stir(false, t);
			float[] fast = PorchWitchBlock.stir(true, t);
			helper.assertTrue(Math.abs(slow[0]) <= PorchWitchBlock.STIR_YAW && Math.abs(slow[1]) <= PorchWitchBlock.STIR_PITCH
					&& Math.abs(fast[0]) <= PorchWitchBlock.STIR_YAW, "Her spoon keeps to its round");
		}
		BlockPos far = new BlockPos(7, 2, 7);
		BlockPos near = new BlockPos(3, 2, 5);
		ServerPlayer visitor = player(helper, far, ItemStack.EMPTY);
		helper.runAfterDelay(PorchWitchBlockEntity.PERIOD * 2, () -> {
			helper.assertFalse(helper.getBlockState(lower).getValue(PorchWitchBlock.CACKLING), "With nobody near she only stirs");
			moveTo(helper, visitor, near);
			helper.runAfterDelay(PorchWitchBlockEntity.PERIOD * 2, () -> {
				helper.assertTrue(helper.getBlockState(lower).getValue(PorchWitchBlock.CACKLING)
						&& helper.getBlockState(lower.above()).getValue(PorchWitchBlock.CACKLING), "A visitor walking up sets her cackling");
				helper.runAfterDelay(PorchWitchBlock.CACKLE_TICKS + PorchWitchBlockEntity.PERIOD, () -> {
					helper.assertFalse(helper.getBlockState(lower).getValue(PorchWitchBlock.CACKLING), "She settles after her time");
					helper.runAfterDelay(PorchWitchBlock.COOLDOWN_TICKS, () -> {
						helper.assertFalse(entity.update(level), "A visitor who stays doesn't set her off again");
						moveTo(helper, visitor, far);
						helper.runAfterDelay(PorchWitchBlockEntity.PERIOD * 2, () -> {
							moveTo(helper, visitor, near);
							helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(lower).getValue(PorchWitchBlock.CACKLING),
									"One who goes and comes back sets her off again"));
						});
					});
				});
			});
		});
	}

	// ---------------------------------------------------------------- grasping hands

	/**
	 * Grasping hands need a solid floor; something sneaking over them isn't grabbed; something walking over them is
	 * (Slowness II for 30 ticks); while up and sinking back they grab nothing more, then rest; without their floor they
	 * drop.
	 */
	@GameTest(maxTicks = 120)
	public void graspingHandsGrabAnkles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.setBlock(new BlockPos(6, 1, 6), Blocks.AIR);
		helper.assertFalse(block("grasping_hands").defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(6, 2, 6))),
				"They need a solid floor");
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("grasping_hands"));
		ServerPlayer player = player(helper, pos, ItemStack.EMPTY);
		BlockPos at = helper.absolutePos(pos);
		player.setShiftKeyDown(true);
		helper.getBlockState(pos).getBlock().stepOn(level, at, helper.getBlockState(pos), player);
		helper.assertTrue(helper.getBlockState(pos).getValue(GraspingHandsBlock.PHASE) == GraspingHandsBlock.Phase.REST
				&& !player.hasEffect(MobEffects.SLOWNESS), "Sneaking over them, nothing is grabbed");
		player.setShiftKeyDown(false);
		helper.getBlockState(pos).getBlock().stepOn(level, at, helper.getBlockState(pos), player);
		MobEffectInstance slow = player.getEffect(MobEffects.SLOWNESS);
		helper.assertTrue(helper.getBlockState(pos).getValue(GraspingHandsBlock.PHASE) == GraspingHandsBlock.Phase.GRAB && slow != null
				&& slow.getAmplifier() == GraspingHandsBlock.SLOWNESS_LEVEL - 1 && slow.getDuration() <= GraspingHandsBlock.SLOW_TICKS,
				"Walking over them, they grab an ankle");
		helper.runAfterDelay(GraspingHandsBlock.GRAB_TICKS + 2, () -> {
			helper.assertTrue(helper.getBlockState(pos).getValue(GraspingHandsBlock.PHASE) == GraspingHandsBlock.Phase.RECOVER,
					"They sink back after their time");
			player.removeEffect(MobEffects.SLOWNESS);
			helper.assertFalse(GraspingHandsBlock.grab(level, at, helper.getBlockState(pos), player) || player.hasEffect(MobEffects.SLOWNESS),
					"Sinking back, they grab nothing");
			helper.runAfterDelay(GraspingHandsBlock.REST_TICKS + 2, () -> {
				helper.assertTrue(helper.getBlockState(pos).getValue(GraspingHandsBlock.PHASE) == GraspingHandsBlock.Phase.REST, "then rest");
				helper.setBlock(pos.below(), Blocks.AIR);
				helper.runAfterDelay(3, () -> {
					helper.assertBlockNotPresent(block("grasping_hands"), pos);
					helper.assertTrue(dropped(helper, item("grasping_hands")) == 1, "Without their floor they drop, once");
					helper.succeed();
				});
			});
		});
	}

	// ---------------------------------------------------------------- the poseable skeleton

	/** Placed sitting, use poses it waving, lounging, hanging and sitting again, on both halves; broken, it drops once. */
	@GameTest(maxTicks = 40)
	public void theSkeletonStrikesAPose(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("poseable_skeleton")));
		player.setYRot(180.0F);
		BlockPos lower = new BlockPos(3, 2, 3);
		BlockPos upper = lower.above();
		place(helper, player, lower.below(), Direction.UP);
		helper.assertTrue(helper.getBlockState(lower).getValue(PoseableSkeletonBlock.POSE) == PoseableSkeletonBlock.Pose.SITTING
				&& helper.getBlockState(upper).getShape(level, helper.absolutePos(upper)).isEmpty(), "Placed, it sits, nothing of it up high");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		for (PoseableSkeletonBlock.Pose next : new PoseableSkeletonBlock.Pose[] {PoseableSkeletonBlock.Pose.WAVING, PoseableSkeletonBlock.Pose.LOUNGING,
				PoseableSkeletonBlock.Pose.HANGING, PoseableSkeletonBlock.Pose.SITTING}) {
			use(helper, player, lower, Direction.SOUTH);
			helper.assertTrue(helper.getBlockState(lower).getValue(PoseableSkeletonBlock.POSE) == next
					&& helper.getBlockState(upper).getValue(PoseableSkeletonBlock.POSE) == next, "Used, it poses " + next);
			helper.assertTrue(helper.getBlockState(upper).getShape(level, helper.absolutePos(upper)).isEmpty() != next.tall(),
					"Standing or hanging, it fills the upper half");
		}
		level.destroyBlock(helper.absolutePos(lower), true);
		helper.runAfterDelay(3, () -> {
			helper.assertBlockNotPresent(block("poseable_skeleton"), upper);
			helper.assertTrue(dropped(helper, item("poseable_skeleton")) == 1, "Broken, it drops once");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- bone wind chimes

	/**
	 * Wind chimes hang under a block, not on the floor; they swing and clack more, and louder, in rain and more again
	 * in a storm; without the block above them they drop.
	 */
	@GameTest(maxTicks = 40)
	public void windChimesHangUnderABlock(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("bone_wind_chimes"), 2));
		place(helper, player, new BlockPos(5, 1, 5), Direction.UP);
		helper.assertBlockNotPresent(block("bone_wind_chimes"), new BlockPos(5, 2, 5));
		BlockPos beam = new BlockPos(3, 4, 3);
		BlockPos pos = beam.below();
		helper.setBlock(beam, Blocks.OAK_PLANKS);
		place(helper, player, beam, Direction.DOWN);
		helper.assertBlockPresent(block("bone_wind_chimes"), pos);
		helper.assertTrue(helper.getBlockEntity(pos, DecorationBlockEntity.class) != null, "The bones are drawn from its block entity");
		helper.assertTrue(BoneWindChimesBlock.swing(0.0F, 0.0F) == BoneWindChimesBlock.CALM_SWING
				&& BoneWindChimesBlock.swing(1.0F, 0.0F) == BoneWindChimesBlock.RAIN_SWING
				&& BoneWindChimesBlock.swing(1.0F, 1.0F) == BoneWindChimesBlock.STORM_SWING, "They swing more in rain and a storm");
		helper.assertTrue(BoneWindChimesBlock.chance(false, false) > BoneWindChimesBlock.chance(true, false)
				&& BoneWindChimesBlock.chance(true, false) > BoneWindChimesBlock.chance(true, true)
				&& BoneWindChimesBlock.volume(false, false) < BoneWindChimesBlock.volume(true, false)
				&& BoneWindChimesBlock.volume(true, false) < BoneWindChimesBlock.volume(true, true), "and clack more often and louder");
		helper.setBlock(beam, Blocks.AIR);
		helper.runAfterDelay(3, () -> {
			helper.assertBlockNotPresent(block("bone_wind_chimes"), pos);
			helper.assertTrue(dropped(helper, item("bone_wind_chimes")) == 1, "Without the block above, they drop once");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- weathervanes

	/**
	 * Both weathervanes are blocks of their own; one stands on a fence post, not on air; the wind it points into is
	 * always 0 to 360 degrees, changes by less than a fifth of a degree a tick, and comes round over the days; still
	 * weather adds no gusts, and a storm's are bounded; without its post the vane drops.
	 */
	@GameTest(maxTicks = 40)
	public void weathervanesPointIntoTheWind(GameTestHelper helper) {
		floor(helper);
		for (String design : JugcraftAgriculture.WEATHERVANE_DESIGNS) {
			helper.assertTrue(block(design + "_weathervane") instanceof WeathervaneBlock vane && vane.design().equals(design),
					"The " + design + " weathervane is a block of its own");
		}
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("bat_weathervane"), 2));
		BlockPos post = new BlockPos(3, 2, 3);
		BlockPos pos = post.above();
		helper.setBlock(post, Blocks.OAK_FENCE);
		place(helper, player, post, Direction.UP);
		helper.assertBlockPresent(block("bat_weathervane"), pos);
		helper.assertTrue(helper.getBlockEntity(pos, DecorationBlockEntity.class) != null, "The vane is drawn from its block entity");
		helper.assertFalse(block("bat_weathervane").defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(new BlockPos(5, 4, 5))),
				"It doesn't stand on air");
		for (long t = 0; t < 72000; t += 997) {
			float wind = WeathervaneBlock.wind(t);
			float change = Math.abs(((WeathervaneBlock.wind(t + 1) - wind) % 360.0F + 540.0F) % 360.0F - 180.0F);
			helper.assertTrue(wind >= 0.0F && wind < 360.0F && change < 0.2F, "The wind turns slowly: " + wind + ", " + change);
			helper.assertTrue(WeathervaneBlock.gust(t, 0.0F, 0.0F) == 0.0F
					&& Math.abs(WeathervaneBlock.gust(t, 1.0F, 1.0F)) <= WeathervaneBlock.RAIN_SWING + WeathervaneBlock.STORM_SWING, "Gusts are bounded");
		}
		float apart = Math.abs(((WeathervaneBlock.wind(36000) - WeathervaneBlock.wind(0)) % 360.0F + 540.0F) % 360.0F - 180.0F);
		helper.assertTrue(apart > 30.0F, "The wind comes round over the days: " + apart);
		helper.setBlock(post, Blocks.AIR);
		helper.runAfterDelay(3, () -> {
			helper.assertBlockNotPresent(block("bat_weathervane"), pos);
			helper.assertTrue(dropped(helper, item("bat_weathervane")) == 1, "Without its post, it drops once");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the spooky sign

	/**
	 * A spooky sign faces the player, saying BEWARE; use paints the next warning; a Name Tag named in an anvil paints
	 * its name, and isn't used up; an unnamed one paints nothing; with its own words, use changes nothing and sneak-use
	 * wipes them; broken, it keeps its words, its own as its name.
	 */
	@GameTest(maxTicks = 40)
	public void spookySignsTakeWords(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("spooky_sign")));
		player.setYRot(180.0F);
		BlockPos pos = new BlockPos(3, 2, 3);
		place(helper, player, pos.below(), Direction.UP);
		helper.assertTrue(helper.getBlockState(pos).getValue(SpookySignBlock.FACING) == Direction.SOUTH
				&& helper.getBlockState(pos).getValue(SpookySignBlock.WORDS) == SpookySignBlock.Words.BEWARE, "It faces the player, saying BEWARE");
		SpookySignBlockEntity sign = helper.getBlockEntity(pos, SpookySignBlockEntity.class);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos, Direction.SOUTH);
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(helper.getBlockState(pos).getValue(SpookySignBlock.WORDS) == SpookySignBlock.Words.TURN_BACK,
				"Use paints the next warning");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NAME_TAG));
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(sign.text().isEmpty(), "An unnamed tag paints nothing");
		ItemStack tag = new ItemStack(Items.NAME_TAG);
		tag.set(DataComponents.CUSTOM_NAME, Component.literal("Here Be Ghouls"));
		player.setItemInHand(InteractionHand.MAIN_HAND, tag);
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(sign.text().equals("Here Be Ghouls") && player.getMainHandItem().getCount() == 1, "A named tag paints its name, kept");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(helper.getBlockState(pos).getValue(SpookySignBlock.WORDS) == SpookySignBlock.Words.TURN_BACK
				&& sign.text().equals("Here Be Ghouls"), "With its own words, use changes nothing");
		player.setShiftKeyDown(true);
		use(helper, player, pos, Direction.SOUTH);
		player.setShiftKeyDown(false);
		helper.assertTrue(sign.text().isEmpty(), "Sneak-use wipes them off");
		sign.paint("Go Home");
		level.destroyBlock(helper.absolutePos(pos), true);
		helper.runAfterDelay(3, () -> {
			List<ItemEntity> signs = drops(helper, item("spooky_sign"));
			helper.assertTrue(signs.size() == 1, "Broken, it drops once");
			ItemStack stack = signs.get(0).getItem();
			Component name = stack.get(DataComponents.CUSTOM_NAME);
			BlockItemStateProperties state = stack.get(DataComponents.BLOCK_STATE);
			helper.assertTrue(name != null && name.getString().equals("Go Home"), "with its own words as its name: " + name);
			helper.assertTrue(state != null && state.apply(block("spooky_sign").defaultBlockState()).getValue(SpookySignBlock.WORDS)
					== SpookySignBlock.Words.TURN_BACK, "and its painted warning: " + state);
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the haunted archway and the dead tree

	/**
	 * An archway goes up from the block aimed at to the player's right, three by three with an opening in the middle,
	 * facing the player; it won't go where a block is in its way; its pillar lanterns light 14; using any block puts
	 * them all out, and again lights them; breaking a corner breaks it all and drops one archway.
	 */
	@GameTest(maxTicks = 40)
	public void theArchwayStandsAsOne(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(6, 2, 4), new ItemStack(item("haunted_archway"), 2));
		player.setYRot(180.0F); // looking north: their right is east
		helper.setBlock(new BlockPos(7, 4, 1), Blocks.STONE);
		place(helper, player, new BlockPos(5, 1, 1), Direction.UP);
		helper.assertBlockNotPresent(block("haunted_archway"), new BlockPos(5, 2, 1));
		moveTo(helper, player, new BlockPos(3, 2, 7));
		place(helper, player, new BlockPos(2, 1, 4), Direction.UP);
		int[][] cells = {{2, 2}, {2, 3}, {2, 4}, {3, 4}, {4, 4}, {4, 3}, {4, 2}};
		for (int part = 0; part < cells.length; part++) {
			BlockState state = helper.getBlockState(new BlockPos(cells[part][0], cells[part][1], 4));
			helper.assertTrue(state.is(block("haunted_archway")) && state.getValue(HauntedArchwayBlock.PART) == part
					&& state.getValue(MultiDecorationBlock.FACING) == Direction.SOUTH, "Part " + part + " is where it should be: " + state);
		}
		helper.assertTrue(helper.getBlockState(new BlockPos(3, 2, 4)).isAir() && helper.getBlockState(new BlockPos(3, 3, 4)).isAir(),
				"You walk through the middle");
		BlockPos lantern = new BlockPos(2, 3, 4);
		helper.assertTrue(light(helper, lantern) == HauntedArchwayBlock.LIGHT && light(helper, new BlockPos(2, 2, 4)) == 0, "Its lanterns are lit");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, new BlockPos(3, 4, 4), Direction.SOUTH);
		helper.assertTrue(light(helper, lantern) == 0 && !helper.getBlockState(new BlockPos(4, 2, 4)).getValue(MultiDecorationBlock.LIT),
				"Used, they all go out");
		use(helper, player, new BlockPos(4, 2, 4), Direction.SOUTH);
		helper.assertTrue(light(helper, new BlockPos(4, 3, 4)) == HauntedArchwayBlock.LIGHT, "and are lit again");
		level.destroyBlock(helper.absolutePos(new BlockPos(4, 4, 4)), true);
		helper.runAfterDelay(3, () -> {
			for (int[] cell : cells) {
				helper.assertBlockNotPresent(block("haunted_archway"), new BlockPos(cell[0], cell[1], 4));
			}
			helper.assertTrue(dropped(helper, item("haunted_archway")) == 1, "Breaking a corner breaks it all, dropping it once");
			helper.succeed();
		});
	}

	/** The dead tree goes up four blocks; its lanterns light 13 at its third; use puts them out; broken, it drops once. */
	@GameTest(maxTicks = 40)
	public void theDeadTreeStandsAsOne(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("dead_hollow_tree")));
		player.setYRot(180.0F);
		BlockPos foot = new BlockPos(3, 2, 3);
		place(helper, player, foot.below(), Direction.UP);
		for (int part = 0; part < DeadHollowTreeBlock.HEIGHT; part++) {
			BlockState state = helper.getBlockState(foot.above(part));
			helper.assertTrue(state.is(block("dead_hollow_tree")) && state.getValue(DeadHollowTreeBlock.PART) == part, "Part " + part + " is in place");
		}
		BlockPos lanterns = foot.above(DeadHollowTreeBlock.LANTERNS);
		helper.assertTrue(light(helper, lanterns) == DeadHollowTreeBlock.LIGHT && light(helper, foot) == 0, "Its lanterns are lit");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, foot, Direction.SOUTH);
		helper.assertTrue(light(helper, lanterns) == 0, "Used, they go out");
		level.destroyBlock(helper.absolutePos(foot.above(3)), true);
		helper.runAfterDelay(3, () -> {
			helper.assertBlockNotPresent(block("dead_hollow_tree"), foot);
			helper.assertTrue(dropped(helper, item("dead_hollow_tree")) == 1, "Breaking its top breaks it all, dropping it once");
			helper.succeed();
		});
	}

	/** The recipes and loot tables load. */
	@GameTest
	public void yardDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("inflatable_ghost", "inflatable_cat", "inflatable_pumpkin", "inflatable_spider", "porch_witch", "grasping_hands",
				"poseable_skeleton", "bone_wind_chimes", "bat_weathervane", "witch_weathervane", "spooky_sign", "haunted_archway", "dead_hollow_tree")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/" + id)));
			helper.assertTrue(table != LootTable.EMPTY, "The " + id + " drops itself");
		}
		helper.succeed();
	}
}

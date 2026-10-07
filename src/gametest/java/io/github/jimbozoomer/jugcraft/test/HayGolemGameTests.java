package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Crow;
import io.github.jimbozoomer.jugcraft.agriculture.HayGolem;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Scarecrows;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * In-game tests for the Hay Golem: building one from a T of four hay bales and a carved head (not without its arms, with
 * something under an arm, or with a pumpkin that has no face), guarding crops from crows as a scarecrow wearing its head
 * would, harvesting and replanting ripe crops into its pouch and emptying it into the chest under its post (or onto the
 * ground), walking to a ripe crop and harvesting it with its own AI, wheat healing it, fire hurting it double, shears
 * taking it apart, and what it drops when killed.
 *
 * <p>Every golem a test makes is gone before the test ends, so none guards or harvests another test's crops; the one with
 * its own AI only looks at crops inside its own test.
 */
public class HayGolemGameTests {
	private static final int RIPE = 7;

	/** A golem that only tends the crops inside its own test's area. */
	private static final class PennedGolem extends HayGolem {
		private final AABB pen;

		PennedGolem(ServerLevel level, AABB pen) {
			super(JugcraftAgriculture.HAY_GOLEM, level);
			this.pen = pen;
		}

		@Override
		public @Nullable BlockPos findRipeCrop(ServerLevel level) {
			BlockPos crop = super.findRipeCrop(level);
			return crop != null && pen.contains(Vec3.atCenterOf(crop)) ? crop : null;
		}
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	/** A golem with no AI of its own standing at {@code pos}, wearing {@code head}, its post where it stands. */
	private static HayGolem golem(GameTestHelper helper, BlockPos pos, ItemStack head) {
		HayGolem golem = helper.spawnWithNoFreeWill(JugcraftAgriculture.HAY_GOLEM, pos);
		golem.setNoAi(true);
		golem.setHead(head);
		golem.setPost(helper.absolutePos(pos));
		return golem;
	}

	/** Ripe {@code crop} on farmland at {@code pos}. */
	private static void ripe(GameTestHelper helper, BlockPos pos, CropBlock crop) {
		helper.setBlock(pos.below(), Blocks.FARMLAND);
		helper.setBlock(pos, crop.getStateForAge(RIPE));
	}

	private static int age(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getBlock() instanceof CropBlock crop ? crop.getAge(helper.getBlockState(pos)) : -1;
	}

	/** How many of {@code item} lie on the ground within two blocks of {@code pos}. */
	private static int lying(GameTestHelper helper, BlockPos pos, Item item) {
		int count = 0;
		for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(2.0))) {
			if (entity.getItem().is(item)) {
				count += entity.getItem().getCount();
			}
		}
		return count;
	}

	/** {@code player} uses what they hold on the top of the block at {@code pos}. */
	private static InteractionResult useOnTop(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		return HayGolem.onUseBlock(player, helper.getLevel(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(absolute).add(0.0, 0.5, 0.0), Direction.UP, absolute, false));
	}

	/**
	 * Only a pumpkin with a face makes a head. A carved head put on a T of hay bales (two stacked, an arm either side of
	 * the upper one) makes a golem facing its builder, the hay gone and the head used, and earns Man of Straw; one arm
	 * short, or with stone under an arm, it is only hay.
	 */
	@GameTest(maxTicks = 20)
	public void aTOfHayAndACarvedHeadMakesAGolem(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.assertTrue(HayGolem.faced(new ItemStack(Items.CARVED_PUMPKIN)) && HayGolem.faced(new ItemStack(Items.JACK_O_LANTERN))
				&& !HayGolem.faced(new ItemStack(Items.PUMPKIN)) && !HayGolem.faced(new ItemStack(JugcraftAgriculture.item("hand_carved_pumpkin"))),
				"Carved pumpkins and jack o'lanterns are heads; a plain pumpkin and an uncut hand-carved one are not");
		BlockPos legs = new BlockPos(3, 2, 3);
		BlockPos chest = legs.above();
		for (BlockPos pos : List.of(legs, chest, chest.east())) {
			helper.setBlock(pos, Blocks.HAY_BLOCK);
		}
		ServerPlayer builder = player(helper, new BlockPos(3, 2, 0));
		builder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARVED_PUMPKIN, 2));
		helper.assertTrue(useOnTop(helper, builder, chest) == InteractionResult.PASS, "One arm short, it is only hay");
		helper.setBlock(chest.west(), Blocks.HAY_BLOCK);
		helper.setBlock(chest.west().below(), Blocks.STONE);
		helper.assertTrue(useOnTop(helper, builder, chest) == InteractionResult.PASS, "With stone under an arm, it is only hay");
		helper.setBlock(chest.west().below(), Blocks.AIR);
		helper.assertTrue(useOnTop(helper, builder, chest).consumesAction(), "A T of hay and a carved head make a golem");
		List<HayGolem> golems = level.getEntitiesOfClass(HayGolem.class, new AABB(helper.absolutePos(legs)).inflate(2.0), Entity::isAlive);
		helper.assertTrue(golems.size() == 1, "One golem: " + golems.size());
		HayGolem golem = golems.get(0);
		boolean hayGone = true;
		for (BlockPos pos : List.of(legs, chest, chest.east(), chest.west())) {
			hayGone &= helper.getBlockState(pos).isAir();
		}
		helper.assertTrue(hayGone, "The hay bales are gone");
		helper.assertTrue(golem.head().is(Items.CARVED_PUMPKIN) && golem.post().equals(helper.absolutePos(legs))
				&& golem.blockPosition().equals(helper.absolutePos(legs)), "It wears the pumpkin and keeps its post where it stands");
		helper.assertTrue(Math.abs(Mth.wrapDegrees(golem.getYRot() - 180.0F)) < 1.0F, "It faces its builder (north): " + golem.getYRot());
		helper.assertTrue(builder.getMainHandItem().getCount() == 1 && earned(builder, "man_of_straw"), "The head is used; Man of Straw");
		golem.discard();
		helper.succeed();
	}

	/**
	 * A golem guards crops as a scarecrow wearing its head would: eight blocks with a carved pumpkin, twelve with a jack
	 * o'lantern, four with none; a crow won't go after a ripe crop it guards.
	 */
	@GameTest(maxTicks = 20)
	public void itGuardsCropsFromCrows(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		HayGolem golem = golem(helper, new BlockPos(0, 2, 0), new ItemStack(Items.CARVED_PUMPKIN));
		helper.assertTrue(golem.guardRadius() == Scarecrows.HEADED, "A carved pumpkin head guards " + Scarecrows.HEADED + ": " + golem.guardRadius());
		helper.assertTrue(Scarecrows.guarded(level, helper.absolutePos(new BlockPos(0, 2, 7))), "Seven blocks off is guarded");
		golem.setHead(new ItemStack(Items.JACK_O_LANTERN));
		helper.assertTrue(golem.guardRadius() == Scarecrows.LIT, "A jack o'lantern guards " + Scarecrows.LIT);
		golem.setHead(ItemStack.EMPTY);
		helper.assertTrue(golem.guardRadius() == Scarecrows.BARE, "Headless, it guards " + Scarecrows.BARE);
		helper.assertTrue(Scarecrows.guarded(level, helper.absolutePos(new BlockPos(0, 2, 4)))
				&& !Scarecrows.guarded(level, helper.absolutePos(new BlockPos(0, 2, 6))), "Headless: four blocks off is guarded, six is not");
		golem.setHead(new ItemStack(Items.CARVED_PUMPKIN));
		BlockPos crop = new BlockPos(4, 2, 4);
		ripe(helper, crop, (CropBlock) Blocks.WHEAT);
		Crow crow = helper.spawnWithNoFreeWill(JugcraftAgriculture.CROW, new BlockPos(4, 4, 4));
		crow.setNoAi(true);
		helper.assertTrue(!crow.raid(level, helper.absolutePos(crop)), "A crow won't raid the wheat it guards");
		crow.discard();
		golem.discard();
		helper.succeed();
	}

	/**
	 * Harvesting a ripe crop replants it from its drops and pockets the rest; an unripe one is left. Unloading puts the
	 * pouch into the chest under its post, or, with none, on the ground. The pouch holds nine stacks, no more.
	 */
	@GameTest(maxTicks = 20)
	public void itHarvestsReplantsAndUnloads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos post = new BlockPos(1, 2, 1);
		helper.setBlock(post.below(), Blocks.CHEST);
		HayGolem golem = golem(helper, post, new ItemStack(Items.CARVED_PUMPKIN));
		List<BlockPos> crops = List.of(new BlockPos(3, 2, 4), new BlockPos(4, 2, 4), new BlockPos(5, 2, 4), new BlockPos(6, 2, 4));
		for (BlockPos crop : crops) {
			ripe(helper, crop, (CropBlock) Blocks.CARROTS);
		}
		for (BlockPos crop : crops) {
			helper.assertTrue(golem.harvest(level, helper.absolutePos(crop)), "It harvests the carrots at " + crop);
			helper.assertTrue(helper.getBlockState(crop).is(Blocks.CARROTS) && age(helper, crop) == 0, "and replants them from a carrot");
		}
		helper.assertTrue(!golem.harvest(level, helper.absolutePos(crops.get(0))), "It leaves unripe carrots");
		int carried = golem.carried();
		helper.assertTrue(carried > 0 && golem.pouch().stream().allMatch(stack -> stack.is(Items.CARROT)), "It carries the other carrots: " + carried);
		int unloaded = golem.unload(level);
		ChestBlockEntity chest = helper.getBlockEntity(post.below(), ChestBlockEntity.class);
		helper.assertTrue(unloaded == carried && chest.countItem(Items.CARROT) == carried && golem.carried() == 0,
				"It empties them into the chest under its post: " + chest.countItem(Items.CARROT) + " of " + carried);

		BlockPos bare = new BlockPos(6, 2, 1);
		golem.setPost(helper.absolutePos(bare));
		golem.pocket(new ItemStack(Items.WHEAT, 5));
		golem.unload(level);
		helper.assertTrue(lying(helper, bare, Items.WHEAT) == 5, "With no chest it sets them down at its post");

		ItemStack left = ItemStack.EMPTY;
		for (int i = 0; i <= HayGolem.POUCH_SLOTS; i++) {
			left = new ItemStack(Items.WHEAT_SEEDS, 64);
			golem.pocket(left);
		}
		helper.assertTrue(left.getCount() == 64 && golem.carried() == HayGolem.POUCH_SLOTS * 64, "Nine stacks, no more: " + golem.carried());
		golem.discard();
		helper.succeed();
	}

	/** With its own AI a golem finds the ripe carrots near its post, walks to them, and harvests and replants them. */
	@GameTest(maxTicks = 300)
	public void itWalksToARipeCropAndHarvestsIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos crop = new BlockPos(6, 2, 4);
		ripe(helper, crop, (CropBlock) Blocks.CARROTS);
		PennedGolem golem = new PennedGolem(level, new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(7.0, 5.0, 7.0));
		BlockPos post = helper.absolutePos(new BlockPos(1, 2, 4));
		golem.snapTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 0.0F, 0.0F);
		golem.setHead(new ItemStack(Items.CARVED_PUMPKIN));
		golem.setPost(post);
		level.addFreshEntity(golem);
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getBlockState(crop).is(Blocks.CARROTS) && age(helper, crop) == 0,
					"Waiting for it to walk over and harvest the carrots: at " + golem.position());
			golem.discard();
		});
	}

	/** Fire hurts it double; wheat heals it four; shears take it back to four hay bales, its head and its pouch. */
	@GameTest(maxTicks = 20)
	public void wheatHealsFireBurnsShearsUndo(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		HayGolem golem = golem(helper, new BlockPos(4, 2, 4), new ItemStack(Items.JACK_O_LANTERN));
		golem.pocket(new ItemStack(Items.CARROT, 3));
		golem.hurtServer(level, level.damageSources().onFire(), 2.0F);
		helper.assertTrue(golem.getHealth() == HayGolem.MAX_HEALTH - 2.0F * HayGolem.FIRE_FACTOR, "Fire hurts it double: " + golem.getHealth());
		ServerPlayer player = player(helper, new BlockPos(4, 2, 2));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 2));
		player.interactOn(golem, InteractionHand.MAIN_HAND, golem.position());
		helper.assertTrue(golem.getHealth() == HayGolem.MAX_HEALTH - 2.0F * HayGolem.FIRE_FACTOR + HayGolem.WHEAT_HEAL
				&& player.getMainHandItem().getCount() == 1, "A wheat heals it four: " + golem.getHealth());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
		player.interactOn(golem, InteractionHand.MAIN_HAND, golem.position());
		BlockPos at = new BlockPos(4, 2, 4);
		helper.assertTrue(golem.isRemoved() && lying(helper, at, Items.HAY_BLOCK) == HayGolem.HAY_BALES
				&& lying(helper, at, Items.JACK_O_LANTERN) == 1 && lying(helper, at, Items.CARROT) == 3,
				"Shears take it apart: hay bales, its head and its carrots");
		helper.succeed();
	}

	/** Killed by a player it drops wheat and its head; its loot table, head tag and advancement load. */
	@GameTest(maxTicks = 20)
	public void killedItDropsWheatAndItsHead(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos at = new BlockPos(4, 2, 4);
		HayGolem golem = golem(helper, at, new ItemStack(Items.CARVED_PUMPKIN));
		ServerPlayer player = player(helper, new BlockPos(4, 2, 2));
		golem.hurtServer(level, level.damageSources().playerAttack(player), 100.0F);
		int wheat = lying(helper, at, Items.WHEAT);
		helper.assertTrue(golem.isDeadOrDying() && wheat >= 2 && wheat <= 5 && lying(helper, at, Items.CARVED_PUMPKIN) == 1,
				"It drops two to five wheat and its head: " + wheat);
		helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("entities/hay_golem")))
				!= LootTable.EMPTY, "The golem's loot table loads");
		helper.assertTrue(new ItemStack(JugcraftAgriculture.item("hand_carved_white_pumpkin")).is(HayGolem.HEADS) && !new ItemStack(Items.PUMPKIN).is(HayGolem.HEADS),
				"The head tag holds the carved pumpkins");
		helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id("man_of_straw")) != null, "Man of Straw loads");
		helper.succeed();
	}
}

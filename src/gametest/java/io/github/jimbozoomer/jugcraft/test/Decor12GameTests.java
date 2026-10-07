package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBowlBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBowlBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenBonfireBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenBonfireBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedHayride;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MarshmallowStickItem;
import io.github.jimbozoomer.jugcraft.agriculture.ToiletPaperRoll;
import io.github.jimbozoomer.jugcraft.agriculture.ToiletPaperStreamerBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreaters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for night events: Toilet Paper (streamers hanging from leaves and draped over fences, a thrown roll),
 * the trick-or-treaters (treats and thank-you gifts from a full bowl, toilet paper for an empty one), the Haunted
 * Hayride (four seats, placed on rails), the Halloween Bonfire (cooking, burning, putting out and lighting) and
 * marshmallows (toasting and burning over a fire), and that their data loads.
 */
public class Decor12GameTests {
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

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, float yRot, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setYRot(yRot);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static BlockHitResult hit(GameTestHelper helper, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		return new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(helper, pos, side));
	}

	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static int droppedAnything(GameTestHelper helper) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static boolean streamer(GameTestHelper helper, BlockPos pos, boolean draped) {
		return helper.getBlockState(pos).is(block("toilet_paper_streamer"))
				&& helper.getBlockState(pos).getValue(ToiletPaperStreamerBlock.DRAPED) == draped;
	}

	private static int count(ServerPlayer player, Item item) {
		int count = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			count += stack.is(item) ? stack.getCount() : 0;
		}
		return count;
	}

	// ---------------------------------------------------------------- toilet paper

	/**
	 * Draping round a tree and a fence hangs a streamer under the leaves and drapes one over the fence; they need what
	 * holds them (the streamer goes when its leaves do) and drop nothing; a thrown roll that lands on a fence drapes it
	 * and is used up.
	 */
	@GameTest(maxTicks = 60)
	public void toiletPaperDrapesTreesAndFences(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos leaves = new BlockPos(2, 5, 3);
		BlockPos fence = new BlockPos(4, 2, 3);
		helper.setBlock(leaves, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
		helper.setBlock(fence, Blocks.OAK_FENCE);
		int papered = ToiletPaperStreamerBlock.drape(level, helper.absolutePos(new BlockPos(3, 3, 3)), 2, 4, level.getRandom());
		helper.assertTrue(papered == 2, "Two spots to paper, under the leaves and over the fence: " + papered);
		helper.assertTrue(streamer(helper, leaves.below(), false), "A streamer hangs under the leaves");
		helper.assertTrue(streamer(helper, fence.above(), true), "and one is draped over the fence");
		helper.assertTrue(!ToiletPaperStreamerBlock.canHang(level, helper.absolutePos(new BlockPos(6, 4, 6)))
				&& !ToiletPaperStreamerBlock.canDrape(level, helper.absolutePos(new BlockPos(6, 2, 6))), "Nothing holds paper in the open air");
		helper.setBlock(leaves, Blocks.AIR);
		helper.assertTrue(!helper.getBlockState(leaves.below()).is(block("toilet_paper_streamer")), "Without its leaves the streamer goes");
		helper.setBlock(fence.above(), Blocks.AIR);

		BlockPos post = new BlockPos(6, 2, 1);
		helper.setBlock(post, Blocks.OAK_FENCE);
		ServerPlayer thrower = player(helper, new BlockPos(6, 2, 6), 180.0F, ItemStack.EMPTY);
		ToiletPaperRoll roll = new ToiletPaperRoll(level, thrower, new ItemStack(item("toilet_paper_roll")));
		Vec3 above = Vec3.atBottomCenterOf(helper.absolutePos(post)).add(0.0, 3.0, 0.0);
		roll.setPos(above.x, above.y, above.z);
		roll.shoot(0.0, -1.0, 0.0, 0.6F, 0.0F);
		level.addFreshEntity(roll);
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(streamer(helper, post.above(), true), "A roll thrown down onto a fence drapes it");
			helper.assertTrue(level.getEntitiesOfClass(ToiletPaperRoll.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(10.0)).isEmpty(),
					"and is used up");
			helper.assertTrue(droppedAnything(helper) == 0, "Streamers drop nothing");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- trick-or-treaters

	/** A wooden door at {@code door} with a jack o'lantern by it and leaves overhead, and a candy bowl in front. */
	private static void porch(GameTestHelper helper, BlockPos door, BlockPos bowl) {
		helper.setBlock(door, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(door.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		helper.setBlock(door.east(), Blocks.JACK_O_LANTERN);
		helper.setBlock(bowl, block("candy_bowl").defaultBlockState().setValue(CandyBowlBlock.FACING, Direction.SOUTH));
		for (int x = 0; x <= 3; x++) {
			helper.setBlock(new BlockPos(x, 5, 1), Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
		}
	}

	/** Brings the visit's children up to the bowl (rather than waiting for them to walk). */
	private static void arrive(ServerLevel level, TrickOrTreaters.Visit visit) {
		int i = 0;
		for (UUID id : visit.kids()) {
			Entity kid = level.getEntity(id);
			if (kid != null) {
				kid.snapTo(visit.bowl.getX() + 0.5 + (i++ - 1) * 0.5, visit.bowl.getY(), visit.bowl.getZ() + 1.5, 180.0F, 0.0F);
			}
		}
	}

	/**
	 * The door's finding rules; a group of two at a bowl with two treats: they knock, each takes a treat and leaves a
	 * gift, nothing is papered, and they go; a group at an empty bowl papers the trees instead. The children wear
	 * costumes and can't be hurt. Trick-or-treating time is the Halloween event, dusk to midnight.
	 */
	@GameTest(maxTicks = 300)
	public void trickOrTreatersVisitCandyBowls(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos door = new BlockPos(1, 2, 1);
		BlockPos bowl = new BlockPos(1, 2, 3);
		porch(helper, door, bowl);
		helper.assertTrue(helper.absolutePos(door).equals(TrickOrTreaters.doorNear(level, helper.absolutePos(bowl)))
				&& TrickOrTreat.porchLight(level, helper.absolutePos(door)), "The bowl finds its door, and the door its porch light");
		HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
		boolean dusk = TrickOrTreaters.tonight(TrickOrTreat.DUSK + 100) && !TrickOrTreaters.tonight(TrickOrTreat.MIDNIGHT + 100)
				&& !TrickOrTreaters.tonight(6000);
		HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
		boolean off = !TrickOrTreaters.tonight(TrickOrTreat.DUSK + 100);
		HalloweenSeason.reset();
		helper.assertTrue(dusk && off, "They come in the event, between dusk and midnight");

		CandyBowlBlockEntity full = (CandyBowlBlockEntity) helper.getBlockEntity(bowl, CandyBowlBlockEntity.class);
		full.add(new ItemStack(item("candy_corn"), 2));
		TrickOrTreaters.Visit visit = TrickOrTreaters.send(level, helper.absolutePos(bowl), helper.absolutePos(door), helper.absolutePos(new BlockPos(6, 2, 6)), 2);
		helper.assertTrue(visit.kids().size() == 2, "Two children come");
		for (UUID id : visit.kids()) {
			helper.assertTrue(level.getEntity(id) instanceof Villager kid && kid.isBaby()
					&& !kid.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty(), "Each is a village child in costume");
			Villager kid = (Villager) level.getEntity(id);
			float health = kid.getHealth();
			helper.assertTrue(!kid.hurtServer(level, level.damageSources().generic(), 4.0F) && kid.getHealth() == health, "and can't be hurt");
		}
		arrive(level, visit);
		helper.runAfterDelay(TrickOrTreaters.WAIT_TICKS + 40, () -> {
			helper.assertTrue(visit.phase() == TrickOrTreaters.Phase.LEAVING && visit.treated() == 2 && !visit.pranked(),
					"They knocked, each took a treat and nothing was papered: " + visit.phase() + " " + visit.treated());
			helper.assertTrue(full.count() == 0, "The bowl gave out its two treats");
			int gifts = droppedAnything(helper);
			helper.assertTrue(gifts >= 2, "and they left thank-you gifts: " + gifts);

			TrickOrTreaters.Visit empty = TrickOrTreaters.send(level, helper.absolutePos(bowl), helper.absolutePos(door),
					helper.absolutePos(new BlockPos(6, 2, 6)), 1);
			arrive(level, empty);
			helper.runAfterDelay(TrickOrTreaters.WAIT_TICKS + 40, () -> {
				helper.assertTrue(empty.pranked() && empty.treated() == 0, "At an empty bowl they play a prank");
				int streamers = 0;
				for (int x = 0; x <= 3; x++) {
					streamers += helper.getBlockState(new BlockPos(x, 4, 1)).is(block("toilet_paper_streamer")) ? 1 : 0;
				}
				helper.assertTrue(streamers > 0, "and the leaves over the door are hung with toilet paper");
				for (TrickOrTreaters.Visit any : List.of(visit, empty)) {
					for (UUID id : any.kids()) {
						Entity kid = level.getEntity(id);
						if (kid != null) {
							kid.discard();
						}
					}
				}
				helper.succeed();
			});
		});
	}

	// ---------------------------------------------------------------- the haunted hayride

	/**
	 * A hayride placed on a rail from its item; four players climb aboard and the fifth finds it full; a sneaking player
	 * doesn't climb on; it drops itself; it is night from dusk to dawn for its spooks.
	 */
	@GameTest(maxTicks = 40)
	public void hauntedHayridesSeatFour(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos rail = new BlockPos(3, 2, 3);
		helper.setBlock(rail, Blocks.RAIL);
		ServerPlayer driver = player(helper, new BlockPos(3, 2, 1), 0.0F, new ItemStack(item("haunted_hayride")));
		driver.getMainHandItem().useOn(new UseOnContext(driver, InteractionHand.MAIN_HAND, hit(helper, rail, Direction.UP)));
		List<HauntedHayride> rides = level.getEntitiesOfClass(HauntedHayride.class, new AABB(helper.absolutePos(rail)).inflate(2.0));
		helper.assertTrue(rides.size() == 1, "The hayride is placed on the rail");
		HauntedHayride ride = rides.get(0);
		List<ServerPlayer> riders = new ArrayList<>();
		for (int i = 0; i < HauntedHayride.SEATS + 1; i++) {
			riders.add(player(helper, new BlockPos(1 + i, 2, 5), 0.0F, ItemStack.EMPTY));
		}
		ServerPlayer sneak = player(helper, new BlockPos(5, 2, 1), 0.0F, ItemStack.EMPTY);
		sneak.setShiftKeyDown(true);
		helper.assertTrue(HauntedHayride.board(sneak, level, InteractionHand.MAIN_HAND, ride) == InteractionResult.PASS && !sneak.isPassenger(),
				"Sneaking, nobody climbs on");
		for (int i = 0; i < HauntedHayride.SEATS; i++) {
			helper.assertTrue(HauntedHayride.board(riders.get(i), level, InteractionHand.MAIN_HAND, ride) == InteractionResult.SUCCESS
					&& riders.get(i).getVehicle() == ride, "Rider " + (i + 1) + " climbs aboard");
		}
		helper.assertTrue(HauntedHayride.board(riders.get(HauntedHayride.SEATS), level, InteractionHand.MAIN_HAND, ride) == InteractionResult.PASS
				&& ride.getPassengers().size() == HauntedHayride.SEATS, "The fifth finds it full");
		helper.assertTrue(ride.getPickResult().is(item("haunted_hayride")), "It is the hayride's own item");
		helper.assertTrue(HauntedHayride.night(13000) && HauntedHayride.night(22999) && !HauntedHayride.night(6000) && !HauntedHayride.night(23000),
				"Its spooks come by night");
		ride.spook(level);
		ride.ejectPassengers();
		ride.discard();
		helper.succeed();
	}

	// ---------------------------------------------------------------- the bonfire and marshmallows

	/**
	 * A bonfire is placed lit (light 15); chestnuts used on it go on its skewers, one each, four at most, and roast in
	 * half a campfire's time, popping off as roasted chestnuts; it burns what stands on it; a shovel puts it out and
	 * flint and steel lights it; broken, it drops itself and what is on its skewers.
	 */
	@GameTest(maxTicks = 400)
	public void bonfiresRoastAndBurn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("halloween_bonfire"));
		helper.assertTrue(helper.getBlockState(pos).getLightEmission() == HalloweenBonfireBlock.LIGHT, "It burns bright");
		HalloweenBonfireBlockEntity bonfire = (HalloweenBonfireBlockEntity) helper.getBlockEntity(pos, HalloweenBonfireBlockEntity.class);
		ServerPlayer cook = player(helper, pos.north(), 0.0F, new ItemStack(item("chestnut"), 6));
		for (int i = 0; i < 5; i++) {
			use(helper, cook, pos, Direction.NORTH);
		}
		helper.assertTrue(cook.getMainHandItem().getCount() == 2 && bonfire.items().stream().filter(stack -> stack.is(item("chestnut"))).count() == 4,
				"Four chestnuts go on its four skewers, and no more");
		cook.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		helper.assertTrue(!bonfire.cook(level, cook.getMainHandItem(), cook), "Nothing that a campfire doesn't cook goes on");

		// A pig, as a player who has just joined can't be hurt for a few seconds.
		LivingEntity victim = (LivingEntity) helper.spawn(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("pig")), pos.east());
		float health = victim.getHealth();
		helper.getBlockState(pos).getBlock().stepOn(level, helper.absolutePos(pos), helper.getBlockState(pos), victim);
		helper.assertTrue(victim.getHealth() < health, "It burns whoever stands on it");
		victim.discard();

		helper.runAfterDelay(600 / HalloweenBonfireBlockEntity.SPEED + 10, () -> {
			helper.assertTrue(dropped(helper, item("roasted_chestnuts")) == 4 && bonfire.items().stream().allMatch(ItemStack::isEmpty),
					"In half a campfire's time they roast and pop off");
			cook.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
			use(helper, cook, pos, Direction.NORTH);
			helper.assertTrue(!helper.getBlockState(pos).getValue(HalloweenBonfireBlock.LIT) && helper.getBlockState(pos).getLightEmission() == 0,
					"A shovel puts it out");
			cook.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
			use(helper, cook, pos, Direction.NORTH);
			helper.assertTrue(helper.getBlockState(pos).getValue(HalloweenBonfireBlock.LIT), "Flint and steel lights it");
			cook.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("chestnut")));
			use(helper, cook, pos, Direction.NORTH);
			level.destroyBlock(helper.absolutePos(pos), true);
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(dropped(helper, item("halloween_bonfire")) == 1 && dropped(helper, item("chestnut")) == 1,
						"Broken, it drops itself and the chestnut on its skewer");
				helper.succeed();
			});
		});
	}

	/**
	 * A marshmallow held over a fire: raw before three seconds, toasted after, burnt after seven; over a lit bonfire
	 * three blocks off or a campfire beside it, not an unlit one; let go after four seconds it is a toasted marshmallow
	 * in the toaster's pocket, the stick on it.
	 */
	@GameTest(maxTicks = 40)
	public void marshmallowsToastOverFires(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.assertTrue(MarshmallowStickItem.toasted(MarshmallowStickItem.TOAST_TICKS - 1) == item("marshmallow_on_a_stick")
				&& MarshmallowStickItem.toasted(MarshmallowStickItem.TOAST_TICKS) == item("toasted_marshmallow")
				&& MarshmallowStickItem.toasted(MarshmallowStickItem.BURN_TICKS) == item("burnt_marshmallow"), "Raw, toasted, then burnt");
		BlockPos fire = new BlockPos(1, 2, 1);
		helper.setBlock(fire, block("halloween_bonfire"));
		ServerPlayer toaster = player(helper, new BlockPos(4, 2, 1), 90.0F, new ItemStack(item("marshmallow_on_a_stick"), 2));
		helper.assertTrue(MarshmallowStickItem.fireNear(level, toaster), "A lit bonfire three blocks off is near enough");
		helper.setBlock(fire, block("halloween_bonfire").defaultBlockState().setValue(HalloweenBonfireBlock.LIT, false));
		helper.assertTrue(!MarshmallowStickItem.fireNear(level, toaster), "An unlit one isn't");
		helper.setBlock(fire, Blocks.AIR);
		helper.setBlock(new BlockPos(5, 2, 1), Blocks.CAMPFIRE);
		helper.assertTrue(MarshmallowStickItem.fireNear(level, toaster), "A lit campfire beside you is");
		ItemStack stick = toaster.getMainHandItem();
		stick.releaseUsing(level, toaster, 72000 - 80);
		helper.assertTrue(stick.getCount() == 1 && count(toaster, item("toasted_marshmallow")) == 1, "Let go after four seconds: toasted");
		stick.releaseUsing(level, toaster, 72000 - 20);
		helper.assertTrue(stick.getCount() == 1, "Let go after one second: still raw on its stick");
		helper.succeed();
	}

	// ---------------------------------------------------------------- data

	/** The recipes, the bonfire's and streamer's loot tables and the trick-or-treaters' gifts load. */
	@GameTest
	public void nightEventsDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("toilet_paper_roll", "marshmallow", "marshmallow_on_a_stick", "haunted_hayride", "halloween_bonfire")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
		}
		for (String table : List.of("blocks/halloween_bonfire", "blocks/toilet_paper_streamer", TrickOrTreaters.GIFT_TABLE)) {
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id(table)))
					!= LootTable.EMPTY, "Loot table " + table + " loads");
		}
		helper.succeed();
	}
}

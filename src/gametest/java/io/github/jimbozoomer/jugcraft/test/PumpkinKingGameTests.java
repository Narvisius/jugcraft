package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Scarecrows;
import io.github.jimbozoomer.jugcraft.lair.Crowfield;
import io.github.jimbozoomer.jugcraft.lair.Crowfield.Hill;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.KingsGourdBlock;
import io.github.jimbozoomer.jugcraft.lair.king.JackOStrawEntity;
import io.github.jimbozoomer.jugcraft.lair.king.JugcraftPumpkinKing;
import io.github.jimbozoomer.jugcraft.lair.king.KingsHeadEntity;
import io.github.jimbozoomer.jugcraft.lair.king.KingsPumpkinEntity;
import io.github.jimbozoomer.jugcraft.lair.king.PumpkinKingEntity;
import io.github.jimbozoomer.jugcraft.lair.king.PumpkinKingEntity.Action;
import io.github.jimbozoomer.jugcraft.lair.king.PumpkinKingEntity.Attack;
import io.github.jimbozoomer.jugcraft.lair.king.PumpkinKingEntity.Fire;
import io.github.jimbozoomer.jugcraft.lair.king.PumpkinKingEntity.Phase;
import io.github.jimbozoomer.jugcraft.lair.king.PumpkinKingLoot;
import io.github.jimbozoomer.jugcraft.weapons.ArmItem;
import io.github.jimbozoomer.jugcraft.weapons.FrightBoon;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Pumpkin King (docs/features/pumpkin-king.md), as far as a game-test server can show him: one put down on his own
 * (with no lair instance, over a field laid out from the corner of an arena the Crowfield's size, so his post's place, the
 * field and its eight hills are all loaded; each test lays a floor of dirt and grows a King's Gourd on every hill) hangs
 * on his post until a player who may fight him steps into the field, then tears free and steps down; his health grows
 * with the party, fire bites deeper and bereft deeper still; his Harvest ripens the gourds nearest his foe, a gourd that
 * ripens lights and splits and a Jack-o'-Straw climbs out (or, with four about, it feeds him), a harvest picked whole
 * leaves him bereft and one that lights does not; at half health the Crowning, below a quarter the Last Harvest; his
 * Scythe Sweep, Crow Storm, Gourd Toss, Vine Snare and Head Roll; his Jacks burn, take part for him, stand dazed and
 * crumble; left alone he hangs himself back on his post; the Crowward Charm, the King's Ember, Lantern and Crown; the
 * Fright boon; and his loot is each player's own. His fight in the Crowfield itself runs in a real world, in
 * {@link PumpkinKingClientGameTests}.
 *
 * <p>He scales for players in his field and its farmyard and fights those in his field; the arena keeps every other test's
 * players out of both. Test players are creative (no one he may fight) until the tick a test needs them, so nothing he
 * does before then touches them; those he must not hurt are shielded (Resistance V). Each test hangs him back on his post,
 * with his Jacks gone, and takes him away.
 */
public class PumpkinKingGameTests {
	/** An arena of air the size of the Crowfield (69 x 76): the field, its farmyard and its lane. */
	private static final String FIELD = "jugcraft-test:crowfield_plain";
	/**
	 * A new player cannot be hurt until its client has loaded the world, or for 60 ticks; a mock player has no client, so
	 * a test that hurts one waits this long first.
	 */
	private static final int LOADING_TICKS = 80;
	/** A creature hurt shrugs off a blow no harder than the last for 10 ticks after (vanilla's); a test waits this long between blows. */
	private static final int HURT_COOLDOWN = 12;
	/** Some ticks past his waking, when he has stepped down off his mound. */
	private static final int AWAKE = PumpkinKingEntity.WAKE_TICKS + 16;

	/** The origin of a field laid out from the arena's corner: its floor a block over the arena's. */
	private static BlockPos origin(GameTestHelper helper) {
		return helper.absolutePos(new BlockPos(0, 1 - Crowfield.STAND, 0));
	}

	/** He hangs on the post of the field laid out from {@code origin}. */
	private static PumpkinKingEntity waiting(GameTestHelper helper, BlockPos origin) {
		return PumpkinKingEntity.summon(helper.getLevel(), origin, null);
	}

	/** The field's floor, dirt, under the whole arena, and a green King's Gourd on every hill, facing the post. */
	private static void field(ServerLevel level, BlockPos origin) {
		for (int x = 0; x < 69; x++) {
			for (int z = 0; z < 76; z++) {
				level.setBlock(origin.offset(x, Crowfield.SOIL, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		for (Hill hill : Hill.values()) {
			level.setBlock(hill.at(origin), JugcraftLairs.KINGS_GOURD.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing(hill)),
					Block.UPDATE_CLIENTS);
		}
	}

	/** The way the gourd on {@code hill} faces: toward the post. */
	private static Direction facing(Hill hill) {
		BlockPos cell = Crowfield.HILLS[hill.ordinal()];
		double dx = Crowfield.CENTRE_X - (cell.getX() + 0.5);
		double dz = Crowfield.CENTRE_Z - (cell.getZ() + 0.5);
		return Math.abs(dx) > Math.abs(dz) ? dx > 0 ? Direction.EAST : Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
	}

	private static int stage(ServerLevel level, BlockPos origin, Hill hill) {
		return level.getBlockState(hill.at(origin)).getValue(KingsGourdBlock.STAGE);
	}

	/** A creative player (no one he may fight) standing at the template's ({@code x}, {@code z}) on the floor. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos origin, double x, double z) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.CREATIVE);
		player.snapTo(origin.getX() + x, origin.getY() + Crowfield.STAND, origin.getZ() + z, 0.0F, 0.0F);
		return player;
	}

	/** Kept from harm (Resistance V) for the rest of a test. */
	private static void shield(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 4, false, false));
	}

	/** Puts him on the floor at the template's ({@code x}, {@code z}), facing south. */
	private static void stand(PumpkinKingEntity king, double x, double z) {
		BlockPos origin = king.origin();
		king.teleportTo(origin.getX() + x, king.floorY(), origin.getZ() + z);
		king.setYRot(0.0F);
		king.setYBodyRot(0.0F);
		king.setYHeadRot(0.0F);
	}

	/** {@code player} uses the block at {@code pos} with an empty hand, as a click on its side would. */
	private static InteractionResult use(ServerLevel level, ServerPlayer player, BlockPos pos) {
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		return player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(id));
	}

	private static float lost(ServerPlayer player) {
		return player.getMaxHealth() - player.getHealth();
	}

	/** Whether his bar's words are {@code key}'s (read from the words, not their text: the server has no mod's words). */
	private static boolean says(Component bar, String key) {
		return bar.getContents() instanceof TranslatableContents words && words.getKey().equals(key);
	}

	/** Checks {@code body}, then hangs him back on his post and takes him away whether it passed or failed. */
	private static void ending(ServerLevel level, PumpkinKingEntity king, Runnable body) {
		try {
			body.run();
		} finally {
			if (king.isAlive()) {
				king.reset(level);
			}
			king.discard();
		}
	}

	/**
	 * He hangs on his post, his fire dark, unhurt by anything; a player in the farmyard does not wake him, nor does a
	 * creative player in the field or their blow; a player who may fight him in the field does: his head bursts into flame,
	 * he is unhurt while he tears free, and he steps down onto the arm of the cross toward the player at full health,
	 * scaled for one.
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 200)
	public void heHangsOnHisPostAndAPlayerInTheFieldWakesHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		PumpkinKingEntity king = waiting(helper, origin);
		ServerPlayer player = player(helper, origin, 34.5, 60.5);
		player.setGameMode(GameType.SURVIVAL);
		shield(player);
		helper.runAfterDelay(25, () -> {
			boolean yard = king.phase() == Phase.WAITING;
			boolean hanging = king.position().distanceTo(king.waiting()) < 0.01 && king.action() == Action.HANG && king.fire() == Fire.DARK;
			player.setGameMode(GameType.CREATIVE);
			player.snapTo(origin.getX() + 34.5, king.floorY(), origin.getZ() + 45.5, 0.0F, 0.0F);
			boolean harmless = !king.hurtServer(level, level.damageSources().playerAttack(player), 10.0F) && king.getHealth() == king.getMaxHealth();
			helper.runAfterDelay(25, () -> {
				boolean slept = king.phase() == Phase.WAITING;
				player.setGameMode(GameType.SURVIVAL);
				helper.runAfterDelay(12, () -> {
					boolean woken = king.phase() == Phase.WAKING && king.fire() == Fire.LIT && king.getHealth() == king.getMaxHealth();
					boolean unhurt = !king.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
					helper.runAfterDelay(AWAKE, () -> {
						ending(level, king, () -> {
							Vec3 down = king.downSpot(player);
							helper.assertTrue(yard, "A player in the farmyard woke him");
							helper.assertTrue(hanging, "He does not hang on his post, his fire dark");
							helper.assertTrue(harmless, "Hanging, he was harmed");
							helper.assertTrue(slept, "A creative player in the field, or their blow, woke him");
							helper.assertTrue(woken, "A player in the field did not wake him at full health, his fire lit");
							helper.assertTrue(unhurt, "Tearing free, he was harmed");
							helper.assertTrue(king.getMaxHealth() == (float) (PumpkinKingEntity.HEALTH * PumpkinKingEntity.partyScale(1)),
									"Woken by one player, his health is " + king.getMaxHealth());
							helper.assertTrue(king.phase() == Phase.REAPING, "He did not step down to fight: " + king.phase());
							helper.assertTrue(down.z > king.centre().z && Math.abs(king.getY() - king.floorY()) < 0.6
									&& Math.hypot(king.getX() - down.x, king.getZ() - down.z) < 3.0,
									"He did not step down onto the farmyard's arm, toward the player, but to " + king.position());
							player.discard();
						});
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * His health grows by half for each player after the first, at most two and a half times; Wither and poison never
	 * take hold of him. Fire is burning, a fire weapon (one that sets alight, or Fire Aspect) or a burning arrow; a plain
	 * blow does its damage, fire half as much again; bereft, a plain blow does half as much again, and fire twice. (His
	 * armour is taken off for the measure.)
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 200)
	public void fireBitesDeeperAndBereftDeeperStill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		PumpkinKingEntity king = waiting(helper, origin);
		ServerPlayer striker = player(helper, origin, 34.5, 45.5);
		striker.setGameMode(GameType.SURVIVAL);
		shield(striker);
		helper.assertTrue(PumpkinKingEntity.partyScale(1) == 1.0 && PumpkinKingEntity.partyScale(2) == 1.5
				&& PumpkinKingEntity.partyScale(4) == 2.5 && PumpkinKingEntity.partyScale(9) == 2.5, "His health scales with the party, at most 2.5 times");
		helper.assertTrue(!king.canBeAffected(new MobEffectInstance(MobEffects.WITHER, 100))
				&& !king.canBeAffected(new MobEffectInstance(MobEffects.POISON, 100)), "Wither or poison can take hold of him");
		helper.assertTrue(PumpkinKingEntity.fiery(level.damageSources().onFire()) && !PumpkinKingEntity.fiery(level.damageSources().generic())
				&& !PumpkinKingEntity.fiery(level.damageSources().playerAttack(striker)), "Burning is not fire, or a bare hand is");
		striker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("cinderbrand")));
		boolean ember = PumpkinKingEntity.fiery(level.damageSources().playerAttack(striker));
		ItemStack sword = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
		level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.FIRE_ASPECT).ifPresent(aspect -> sword.enchant(aspect, 1));
		striker.setItemInHand(InteractionHand.MAIN_HAND, sword);
		boolean aspect = PumpkinKingEntity.fiery(level.damageSources().playerAttack(striker));
		striker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.assertTrue(ember && aspect, "A blow with an arm that sets alight (" + ember + ") or Fire Aspect (" + aspect + ") is not fire");
		king.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			king.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
			float before = king.getHealth();
			king.hurtServer(level, level.damageSources().generic(), 4.0F);
			float plain = before - king.getHealth();
			helper.runAfterDelay(HURT_COOLDOWN, () -> {
				float before2 = king.getHealth();
				king.hurtServer(level, level.damageSources().onFire(), 4.0F);
				float fire = before2 - king.getHealth();
				king.bereave(level);
				helper.runAfterDelay(HURT_COOLDOWN, () -> {
					float before3 = king.getHealth();
					king.hurtServer(level, level.damageSources().generic(), 4.0F);
					float bereftPlain = before3 - king.getHealth();
					helper.runAfterDelay(HURT_COOLDOWN, () -> {
						float before4 = king.getHealth();
						king.hurtServer(level, level.damageSources().onFire(), 4.0F);
						float bereftFire = before4 - king.getHealth();
						ending(level, king, () -> {
							helper.assertTrue(Math.abs(plain - 4.0F) < 0.01F, "A plain blow of 4 did " + plain);
							helper.assertTrue(Math.abs(fire - 4.0F * PumpkinKingEntity.FIRE) < 0.01F, "Fire of 4 did " + fire);
							helper.assertTrue(Math.abs(bereftPlain - 4.0F * PumpkinKingEntity.BEREFT) < 0.01F, "Bereft, a plain blow of 4 did " + bereftPlain);
							helper.assertTrue(Math.abs(bereftFire - 4.0F * PumpkinKingEntity.BEREFT_FIRE) < 0.01F, "Bereft, fire of 4 did " + bereftFire);
							striker.discard();
						});
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * He calls the harvest on the gourd nearest his foe and the next round the ring toward them (two in the Reaping): he
	 * raises his arms for {@value PumpkinKingEntity#CALL_TICKS} ticks, then those two swell, the rest stay green, and his
	 * bar says the harvest ripens; halfway they ripen.
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 300)
	public void heCallsTheHarvestOnTheGourdsNearestHisFoe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		PumpkinKingEntity king = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 24.5, 21.5);
		foe.setGameMode(GameType.SURVIVAL);
		shield(foe);
		king.wake(level);
		List<Hill> expected = List.of(Hill.NORTH_WEST, Hill.NORTH);
		helper.runAfterDelay(AWAKE, () -> {
			List<Hill> run = king.run(foe, PumpkinKingEntity.REAPING_GOURDS);
			List<Hill> longer = king.run(foe, PumpkinKingEntity.HALLOWED_GOURDS);
			king.harvestIn(1);
			helper.runAfterDelay(3, () -> {
				List<Hill> calling = king.calling();
				Action raised = king.action();
				helper.runAfterDelay(PumpkinKingEntity.CALL_TICKS, () -> {
					List<Hill> harvest = king.harvest();
					boolean swelling = expected.stream().allMatch(hill -> stage(level, origin, hill) == KingsGourdBlock.SWELLING);
					boolean rest = List.of(Hill.values()).stream().filter(hill -> !expected.contains(hill))
							.allMatch(hill -> stage(level, origin, hill) == KingsGourdBlock.GREEN);
					boolean bar = says(king.bar(), "message.jugcraft.pumpkin_king.harvest_bar");
					helper.runAfterDelay(PumpkinKingEntity.SWELL_TICKS, () -> {
						ending(level, king, () -> {
							helper.assertTrue(run.equals(expected), "He would call " + run + " round his foe, not " + expected);
							helper.assertTrue(longer.equals(List.of(Hill.NORTH_WEST, Hill.NORTH, Hill.NORTH_EAST)),
									"Three gourds round the ring from his foe are " + longer);
							helper.assertTrue(calling.equals(expected) && raised == Action.CALL, "He did not call " + expected + ", arms raised: "
									+ calling + " " + raised);
							helper.assertTrue(harvest.equals(expected) && swelling && rest, "After his call the harvest is " + harvest
									+ ", swelling " + swelling + ", the rest green " + rest);
							helper.assertTrue(bar, "His bar does not say the harvest ripens: " + king.bar());
							helper.assertTrue(expected.stream().allMatch(hill -> stage(level, origin, hill) == KingsGourdBlock.RIPENING),
									"Halfway the gourds did not ripen");
							foe.discard();
						});
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * A gourd that ripens lights, and {@value PumpkinKingEntity#SPLIT_TICKS} ticks later splits, leaving its hill bare: a
	 * Jack-o'-Straw climbs out before it. With {@value PumpkinKingEntity#JACKS_MAX} Jacks about no more climb out, and the
	 * next gourd's fire feeds him instead.
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 300)
	public void aGourdThatRipensLightsAndAJackClimbsOut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		PumpkinKingEntity king = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 50.5, 50.5);
		foe.setGameMode(GameType.SURVIVAL);
		shield(foe);
		king.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			king.ripen(level, List.of(Hill.SOUTH));
			king.ripenTicks(PumpkinKingEntity.RIPEN_TICKS - 2);
			helper.runAfterDelay(4, () -> {
				boolean lit = stage(level, origin, Hill.SOUTH) == KingsGourdBlock.LIT && king.harvest().isEmpty();
				helper.runAfterDelay(PumpkinKingEntity.SPLIT_TICKS + 2, () -> {
					boolean split = stage(level, origin, Hill.SOUTH) == KingsGourdBlock.PICKED;
					int climbed = king.jacks().size();
					BlockPos front = Hill.SOUTH.at(origin).relative(facing(Hill.SOUTH));
					boolean before = level.getEntitiesOfClass(JackOStrawEntity.class, new AABB(front).inflate(1.5)).size() == 1;
					for (int i = 1; i < PumpkinKingEntity.JACKS_MAX; i++) {
						king.callJack(level, Vec3.atBottomCenterOf(origin.offset(20 + i * 3, Crowfield.STAND, 40)));
					}
					boolean full = king.jacks().size() == PumpkinKingEntity.JACKS_MAX
							&& king.callJack(level, Vec3.atBottomCenterOf(origin.offset(40, Crowfield.STAND, 40))) == null;
					king.setHealth(king.getMaxHealth() * 0.6F);
					float hurt = king.getHealth();
					king.ripen(level, List.of(Hill.WEST));
					king.ripenTicks(PumpkinKingEntity.RIPEN_TICKS - 2);
					helper.runAfterDelay(PumpkinKingEntity.SPLIT_TICKS + 6, () -> {
						ending(level, king, () -> {
							helper.assertTrue(lit, "A gourd that ripened through did not light, ending the harvest");
							helper.assertTrue(split && climbed == 1 && before, "A lit gourd split " + split + " and " + climbed
									+ " Jacks climbed out before it (" + before + ")");
							helper.assertTrue(full, "He has more than " + PumpkinKingEntity.JACKS_MAX + " Jacks, or fewer");
							helper.assertTrue(stage(level, origin, Hill.WEST) == KingsGourdBlock.PICKED && king.jacks().size() == PumpkinKingEntity.JACKS_MAX,
									"With his Jacks all out, a gourd that split brought another");
							helper.assertTrue(king.getHealth() > hurt + king.getMaxHealth() * PumpkinKingEntity.FEED * 0.9F,
									"With his Jacks all out, a gourd's fire did not feed him: " + hurt + " to " + king.getHealth());
							foe.discard();
						});
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * Every gourd of a harvest picked before any lights leaves him bereft: he sags, his fire gutters, his bar says so, his
	 * Jacks stand dazed, and whoever picked it takes part in the fight. A gourd picked alone, while the other still ripens,
	 * does not; and when the spell ends his fire burns again.
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 340)
	public void pickingAWholeHarvestLeavesHimBereft(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		PumpkinKingEntity king = waiting(helper, origin);
		ServerPlayer picker = player(helper, origin, 24.5, 21.5);
		picker.setGameMode(GameType.SURVIVAL);
		shield(picker);
		king.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			JackOStrawEntity jack = king.callJack(level, Vec3.atBottomCenterOf(origin.offset(30, Crowfield.STAND, 30)));
			king.ripen(level, List.of(Hill.NORTH_WEST, Hill.NORTH));
			helper.runAfterDelay(JackOStrawEntity.CLIMB_TICKS + 2, () -> {
				InteractionResult first = use(level, picker, Hill.NORTH_WEST.at(origin));
				boolean one = stage(level, origin, Hill.NORTH_WEST) == KingsGourdBlock.PICKED && !king.bereft() && king.harvest().size() == 2;
				InteractionResult second = use(level, picker, Hill.NORTH.at(origin));
				boolean bereft = king.bereft() && king.staggerTicks() > 0 && king.fire() == Fire.GUTTER && king.harvest().isEmpty();
				boolean bar = says(king.bar(), "message.jugcraft.pumpkin_king.bereft_bar");
				boolean took = king.encounter().qualified().contains(picker.getUUID());
				helper.runAfterDelay(4, () -> {
					boolean dazed = jack != null && jack.isAlive() && jack.action() == JackOStrawEntity.Action.DAZED;
					Action sagging = king.action();
					helper.runAfterDelay(PumpkinKingEntity.BEREFT_TICKS, () -> {
						ending(level, king, () -> {
							helper.assertTrue(first.consumesAction() && second.consumesAction(), "Using a ripening gourd was not answered");
							helper.assertTrue(one, "One gourd picked of two left him bereft, or ended the harvest");
							helper.assertTrue(bereft, "Picking the whole harvest did not leave him bereft, sagging, his fire guttering");
							helper.assertTrue(bar, "His bar does not say he is bereft: " + king.bar());
							helper.assertTrue(took, "Picking a harvest that left him bereft does not take part");
							helper.assertTrue(dazed, "His Jack does not stand dazed while he is bereft");
							helper.assertTrue(sagging == Action.STAGGER, "Bereft, he does not sag: " + sagging);
							helper.assertTrue(!king.bereft() && king.fire() == Fire.LIT, "The bereft spell did not end, his fire lit again: " + king.fire());
							picker.discard();
						});
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * A gourd that lights spoils its harvest: one of two picked, the other ripening through, he is not bereft, and the lit
	 * one cannot be picked.
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 200)
	public void aGourdThatLightsSpoilsTheHarvest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		PumpkinKingEntity king = waiting(helper, origin);
		ServerPlayer picker = player(helper, origin, 24.5, 34.5);
		picker.setGameMode(GameType.SURVIVAL);
		shield(picker);
		king.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			king.ripen(level, List.of(Hill.WEST, Hill.SOUTH_WEST));
			use(level, picker, Hill.WEST.at(origin));
			king.ripenTicks(PumpkinKingEntity.RIPEN_TICKS - 2);
			helper.runAfterDelay(4, () -> {
				boolean lit = stage(level, origin, Hill.SOUTH_WEST) == KingsGourdBlock.LIT;
				use(level, picker, Hill.SOUTH_WEST.at(origin));
				ending(level, king, () -> {
					helper.assertTrue(lit && stage(level, origin, Hill.SOUTH_WEST) == KingsGourdBlock.LIT, "A lit gourd was picked");
					helper.assertTrue(!king.bereft() && king.harvest().isEmpty(), "A harvest a gourd of which lit left him bereft");
					picker.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * At half health, the Crowning: back on his post, his head white-hot, unhurt, two Jacks climbing out of the rows; then
	 * the Hallowed Harvest, three gourds a harvest called more often, and he steps down toward his foe. Below a quarter,
	 * the Last Harvest: more often still, his fire white.
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 300)
	public void atHalfHealthTheCrowningAndBelowAQuarterTheLastHarvest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		PumpkinKingEntity king = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 34.5, 45.5);
		foe.setGameMode(GameType.SURVIVAL);
		shield(foe);
		king.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			king.setHealth(king.getMaxHealth() * PumpkinKingEntity.CROWN_AT);
			helper.runAfterDelay(3, () -> {
				boolean crowned = king.phase() == Phase.CROWNING && king.fire() == Fire.WHITE && king.action() == Action.CROWN;
				int jacks = king.jacks().size();
				boolean unhurt = !king.hurtServer(level, level.damageSources().playerAttack(foe), 5.0F);
				int gourds = king.gourds();
				helper.runAfterDelay(PumpkinKingEntity.CROWNING_TICKS + 14, () -> {
					boolean hallowed = king.phase() == Phase.HALLOWED && king.fire() == Fire.LIT
							&& king.harvestEvery() == PumpkinKingEntity.HALLOWED_EVERY;
					Vec3 down = king.downSpot(foe);
					boolean stepped = Math.hypot(king.getX() - down.x, king.getZ() - down.z) < 3.0 && Math.abs(king.getY() - king.floorY()) < 0.6;
					king.setHealth(king.getMaxHealth() * PumpkinKingEntity.LAST_AT - 1.0F);
					helper.runAfterDelay(3, () -> {
						ending(level, king, () -> {
							helper.assertTrue(crowned, "At half health he was not crowned on his post, his fire white");
							helper.assertTrue(jacks == PumpkinKingEntity.CROWN_JACKS, jacks + " Jacks climbed out at the Crowning");
							helper.assertTrue(unhurt, "Crowned, he was harmed");
							helper.assertTrue(gourds == PumpkinKingEntity.HALLOWED_GOURDS, "From the Crowning he calls " + gourds + " gourds");
							helper.assertTrue(hallowed, "After the Crowning he is not in the Hallowed Harvest, burning orange");
							helper.assertTrue(stepped, "After the Crowning he did not step down toward his foe, but to " + king.position());
							helper.assertTrue(king.phase() == Phase.LAST && king.fire() == Fire.WHITE && king.harvestEvery() == PumpkinKingEntity.LAST_EVERY,
									"Below a quarter he is not in the Last Harvest, his fire white: " + king.phase() + " " + king.fire());
							foe.discard();
						});
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * Scythe Sweep: a player before him within reach is struck; one behind him is not. Crow Storm: a player in the line the
	 * crows stream along is struck and blinded; one beside it is not.
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 300)
	public void hisScytheAndHisCrows(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		PumpkinKingEntity king = waiting(helper, origin);
		ServerPlayer front = player(helper, origin, 34.5, 43.5);
		ServerPlayer behind = player(helper, origin, 34.5, 37.5);
		ServerPlayer beside = player(helper, origin, 39.5, 46.5);
		List<ServerPlayer> all = List.of(front, behind, beside);
		king.wake(level);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			all.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			stand(king, 34.5, 40.5);
			king.begin(level, Attack.SCYTHE_SWEEP, front);
			helper.runAfterDelay(Attack.SCYTHE_SWEEP.windup + 2, () -> {
				float swept = lost(front);
				float spared = lost(behind);
				helper.runAfterDelay(Attack.SCYTHE_SWEEP.length(), () -> {
					stand(king, 34.5, 40.5);
					king.begin(level, Attack.CROW_STORM, front);
					helper.runAfterDelay(Attack.CROW_STORM.windup + 2, () -> {
						ending(level, king, () -> {
							helper.assertTrue(swept > 0.0F, "His Scythe Sweep did not strike the player before him");
							helper.assertTrue(spared == 0.0F, "His Scythe Sweep struck the player behind him");
							helper.assertTrue(lost(front) > swept && front.hasEffect(MobEffects.BLINDNESS), "His crows did not strike and blind the player in their line");
							helper.assertTrue(lost(beside) == 0.0F && !beside.hasEffect(MobEffects.BLINDNESS), "His crows struck the player beside their line");
							all.forEach(ServerPlayer::discard);
						});
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * Gourd Toss: he lifts a pumpkin overhead, a ring of embers on his foe's place; it flies there and bursts: the player on
	 * the ring is struck and set alight, one six blocks off is not.
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 200)
	public void hisGourdTossLandsOnItsRingOfEmbers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		PumpkinKingEntity king = waiting(helper, origin);
		ServerPlayer target = player(helper, origin, 34.5, 50.5);
		ServerPlayer off = player(helper, origin, 40.5, 50.5);
		List<ServerPlayer> all = List.of(target, off);
		king.wake(level);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			all.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			stand(king, 34.5, 40.5);
			king.begin(level, Attack.GOURD_TOSS, target);
			boolean lifted = king.holding() && Math.hypot(king.mark().x - target.getX(), king.mark().z - target.getZ()) < 0.01;
			helper.runAfterDelay(Attack.GOURD_TOSS.windup + 1, () -> {
				boolean thrown = !king.holding() && !level.getEntitiesOfClass(KingsPumpkinEntity.class, king.getBoundingBox().inflate(24.0)).isEmpty();
				helper.runAfterDelay(PumpkinKingEntity.TOSS_FLIGHT + 2, () -> {
					ending(level, king, () -> {
						helper.assertTrue(lifted, "He did not lift a pumpkin, marking his foe's place");
						helper.assertTrue(thrown, "He did not toss it");
						helper.assertTrue(lost(target) > 0.0F && target.getRemainingFireTicks() > 0, "The pumpkin did not strike and set alight the player on its ring");
						helper.assertTrue(lost(off) == 0.0F, "The pumpkin struck a player six blocks off");
						helper.assertTrue(level.getEntitiesOfClass(KingsPumpkinEntity.class, king.getBoundingBox().inflate(24.0)).isEmpty(),
								"The pumpkin did not burst");
						all.forEach(ServerPlayer::discard);
					});
					helper.succeed();
				});
			});
		});
	}

	/**
	 * Vine Snare: the vines lash up along the furrow where his foe stood: they are struck and rooted; one two blocks across
	 * the furrow is not. Head Roll: his head leaves his shoulders and rolls along the line toward his foe, striking them;
	 * one beside the line is not struck; then his head is back.
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 300)
	public void hisVineSnareAndHisHeadRoll(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		PumpkinKingEntity king = waiting(helper, origin);
		ServerPlayer snared = player(helper, origin, 40.5, 40.5);
		ServerPlayer across = player(helper, origin, 42.5, 40.5);
		ServerPlayer rolled = player(helper, origin, 34.5, 52.5);
		ServerPlayer aside = player(helper, origin, 37.5, 50.5);
		List<ServerPlayer> all = List.of(snared, across, rolled, aside);
		king.wake(level);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			all.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			stand(king, 34.5, 40.5);
			king.begin(level, Attack.VINE_SNARE, snared);
			helper.runAfterDelay(Attack.VINE_SNARE.windup + 2, () -> {
				MobEffectInstance rooted = snared.getEffect(MobEffects.SLOWNESS);
				boolean caught = lost(snared) > 0.0F && rooted != null && rooted.getAmplifier() == 3;
				boolean free = lost(across) == 0.0F;
				helper.runAfterDelay(Attack.VINE_SNARE.length(), () -> {
					stand(king, 34.5, 40.5);
					king.begin(level, Attack.HEAD_ROLL, rolled);
					helper.runAfterDelay(Attack.HEAD_ROLL.windup + 2, () -> {
						boolean headless = king.headless()
								&& !level.getEntitiesOfClass(KingsHeadEntity.class, king.getBoundingBox().inflate(24.0)).isEmpty();
						helper.runAfterDelay(Attack.HEAD_ROLL.active + 2, () -> {
							ending(level, king, () -> {
								helper.assertTrue(caught, "His vines did not strike and root the player where they lashed");
								helper.assertTrue(free, "His vines struck the player across the furrow");
								helper.assertTrue(headless, "His head did not leave his shoulders and roll");
								helper.assertTrue(lost(rolled) > 0.0F, "His rolling head did not strike the player in its line");
								helper.assertTrue(lost(aside) == 0.0F, "His rolling head struck the player beside its line");
								helper.assertTrue(!king.headless() && level.getEntitiesOfClass(KingsHeadEntity.class, king.getBoundingBox().inflate(24.0)).isEmpty(),
										"His head is not back on his shoulders");
								all.forEach(ServerPlayer::discard);
							});
							helper.succeed();
						});
					});
				});
			});
		});
	}

	/**
	 * His Jacks: unhurt while they climb out; a plain blow does its damage, fire twice; whoever strikes one takes part in
	 * his fight; they stand dazed while he is bereft, and crumble when he hangs himself back on his post.
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 200)
	public void hisJacksBurnTakePartStandDazedAndCrumble(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		PumpkinKingEntity king = waiting(helper, origin);
		ServerPlayer striker = player(helper, origin, 28.5, 46.5);
		striker.setGameMode(GameType.SURVIVAL);
		shield(striker);
		king.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			JackOStrawEntity jack = king.callJack(level, Vec3.atBottomCenterOf(origin.offset(30, Crowfield.STAND, 40)));
			boolean climbing = jack != null && !jack.hurtServer(level, level.damageSources().playerAttack(striker), 2.0F);
			helper.runAfterDelay(JackOStrawEntity.CLIMB_TICKS + 2, () -> {
				float before = jack.getHealth();
				jack.hurtServer(level, level.damageSources().generic(), 2.0F);
				float plain = before - jack.getHealth();
				helper.runAfterDelay(HURT_COOLDOWN, () -> {
					float before2 = jack.getHealth();
					jack.hurtServer(level, level.damageSources().onFire(), 2.0F);
					float fire = before2 - jack.getHealth();
					helper.runAfterDelay(HURT_COOLDOWN, () -> {
						jack.hurtServer(level, level.damageSources().playerAttack(striker), 1.0F);
						boolean took = king.encounter().qualified().contains(striker.getUUID());
						king.bereave(level);
						helper.runAfterDelay(3, () -> {
							boolean dazed = jack.action() == JackOStrawEntity.Action.DAZED;
							king.reset(level);
							helper.runAfterDelay(3, () -> {
								ending(level, king, () -> {
									helper.assertTrue(climbing, "A Jack was hurt while it climbed out");
									helper.assertTrue(Math.abs(plain - 2.0F) < 0.01F, "A plain blow of 2 did " + plain + " to a Jack");
									helper.assertTrue(Math.abs(fire - 2.0F * JackOStrawEntity.FIRE) < 0.01F, "Fire of 2 did " + fire + " to a Jack");
									helper.assertTrue(took, "Striking his Jack does not take part in his fight");
									helper.assertTrue(dazed, "His Jack does not stand dazed while he is bereft");
									helper.assertTrue(!jack.isAlive(), "His Jack did not crumble when he went back to his post");
									striker.discard();
								});
								helper.succeed();
							});
						});
					});
				});
			});
		});
	}

	/**
	 * Left alone (no one he may fight in his field) for {@value PumpkinKingEntity#ABANDON_TICKS} ticks, he hangs himself back
	 * on his post, healed, his fire dark; his gourds go green, his Jacks crumble, and who took part is forgotten.
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 340)
	public void leftAloneHeHangsHimselfBackOnHisPost(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		PumpkinKingEntity king = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 34.5, 45.5);
		foe.setGameMode(GameType.SURVIVAL);
		shield(foe);
		king.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			king.ripen(level, List.of(Hill.NORTH));
			JackOStrawEntity jack = king.callJack(level, Vec3.atBottomCenterOf(origin.offset(30, Crowfield.STAND, 40)));
			king.took(foe, 5.0F);
			king.setHealth(king.getMaxHealth() * 0.8F);
			foe.setGameMode(GameType.CREATIVE);
			helper.runAfterDelay(PumpkinKingEntity.ABANDON_TICKS + 10, () -> {
				ending(level, king, () -> {
					helper.assertTrue(king.phase() == Phase.WAITING && king.position().distanceTo(king.waiting()) < 0.01 && king.action() == Action.HANG,
							"Left alone he did not hang himself back on his post: " + king.phase() + " at " + king.position());
					helper.assertTrue(king.getHealth() == king.getMaxHealth() && king.fire() == Fire.DARK, "Back on his post he is not healed, his fire dark");
					helper.assertTrue(stage(level, origin, Hill.NORTH) == KingsGourdBlock.GREEN && king.harvest().isEmpty(), "His gourds did not go green");
					helper.assertTrue(jack != null && !jack.isAlive() && king.jacks().isEmpty(), "His Jacks did not crumble");
					helper.assertTrue(king.encounter().qualified().isEmpty(), "Who took part was not forgotten");
					foe.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * The Crowward Charm in the offhand keeps crows off crops within {@value JugcraftPumpkinKing#CHARM_REACH} blocks, as a
	 * lit scarecrow does (and only in the offhand); the King's Ember burns in a furnace; an ember and an heirloom pumpkin
	 * make a King's Lantern, which gives light {@value JugcraftPumpkinKing#LANTERN_LIGHT}; and the Pumpkin King's Crown is a
	 * costume worn on the head.
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 60)
	public void theCharmTheEmberTheLanternAndTheCrown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		ServerPlayer holder = player(helper, origin, 34.5, 38.5);
		BlockPos near = origin.offset(34 + JugcraftPumpkinKing.CHARM_REACH - 1, Crowfield.STAND, 38);
		BlockPos far = origin.offset(34 + JugcraftPumpkinKing.CHARM_REACH + 3, Crowfield.STAND, 38);
		boolean bare = Scarecrows.guarded(level, near);
		holder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftPumpkinKing.CROWWARD_CHARM));
		boolean inHand = Scarecrows.guarded(level, near);
		holder.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		holder.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftPumpkinKing.CROWWARD_CHARM));
		helper.assertTrue(!bare && !inHand, "Crops were guarded with no charm, or with the charm in the main hand");
		helper.assertTrue(Scarecrows.guarded(level, near) && !Scarecrows.guarded(level, far),
				"The charm in the offhand does not keep crows off within " + JugcraftPumpkinKing.CHARM_REACH + " blocks, and only there");
		ItemStack ember = new ItemStack(JugcraftPumpkinKing.KINGS_EMBER);
		helper.assertTrue(ember.has(DataComponents.COOKING_FUEL), "The King's Ember does not burn");
		CraftingInput grid = CraftingInput.of(2, 1, List.of(ember, new ItemStack(item("white_pumpkin"))));
		ItemStack lantern = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, grid, level)
				.orElseThrow(() -> helper.assertionException("An ember and an heirloom pumpkin make nothing")).value().assemble(grid);
		helper.assertTrue(lantern.is(JugcraftPumpkinKing.KINGS_LANTERN_ITEM), "An ember and an heirloom pumpkin made " + lantern);
		helper.assertTrue(JugcraftPumpkinKing.KINGS_LANTERN.defaultBlockState().getLightEmission() == JugcraftPumpkinKing.LANTERN_LIGHT,
				"The King's Lantern does not give light " + JugcraftPumpkinKing.LANTERN_LIGHT);
		ItemStack crown = new ItemStack(JugcraftPumpkinKing.PUMPKIN_KINGS_CROWN);
		TagKey<Item> costumes = TagKey.create(Registries.ITEM, Jugcraft.id("trick_or_treat_costumes"));
		var equippable = crown.get(DataComponents.EQUIPPABLE);
		helper.assertTrue(crown.is(costumes) && equippable != null && equippable.slot() == EquipmentSlot.HEAD,
				"The Pumpkin King's Crown is not a costume worn on the head");
		holder.discard();
		helper.succeed();
	}

	/**
	 * The Fright boon (the Hallowreap's and the Crowbill's): a monster struck panics, dropping its target, for
	 * {@value FrightBoon#PANIC_TICKS} ticks, and a strike meanwhile or soon after does not frighten it again; a boss (he
	 * himself) and a creature that is no monster are never frightened.
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 120)
	public void theFrightBoonPanicsAMonsterButNeverABoss(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		PumpkinKingEntity king = waiting(helper, origin);
		ServerPlayer wielder = player(helper, origin, 34.5, 50.5);
		wielder.setGameMode(GameType.SURVIVAL);
		shield(wielder);
		ItemStack reap = new ItemStack(JugcraftPumpkinKing.hallowreap());
		wielder.setItemInHand(InteractionHand.MAIN_HAND, reap);
		var husk = helper.spawn(EntityTypes.HUSK, new BlockPos(34, 1, 53));
		var cow = helper.spawn(EntityTypes.COW, new BlockPos(30, 1, 53));
		helper.assertTrue(FrightBoon.frightens(husk) && !FrightBoon.frightens(king) && !FrightBoon.frightens(cow),
				"The Fright boon would frighten a boss or a creature that is no monster, or not a monster");
		husk.setTarget(wielder);
		((ArmItem) reap.getItem()).hurtEnemy(reap, husk, wielder);
		boolean panics = FrightBoon.panicking(husk) && husk.getTarget() == null;
		helper.runAfterDelay(FrightBoon.PANIC_TICKS + 4, () -> {
			boolean over = !FrightBoon.panicking(husk);
			((ArmItem) reap.getItem()).hurtEnemy(reap, husk, wielder);
			boolean again = FrightBoon.panicking(husk);
			ending(level, king, () -> {
				helper.assertTrue(panics, "A husk struck did not panic, dropping its target");
				helper.assertTrue(over, "Its panic did not end after " + FrightBoon.PANIC_TICKS + " ticks");
				helper.assertTrue(!again, "A husk struck again within " + FrightBoon.COOLDOWN + " ticks panicked again");
				husk.discard();
				cow.discard();
				wielder.discard();
			});
			helper.succeed();
		});
	}

	/**
	 * Each player who took part (struck him, or picked a harvest that left him bereft) rolls his loot for themselves: the
	 * King's Ember always, and on a first win one of his trophies; the experience is shared; Uncrowned is theirs. A
	 * bystander gets nothing. He falls with his fire going out, and with no lair instance no Grey Mist opens.
	 */
	@GameTest(structure = FIELD, padding = 8, maxTicks = 200)
	public void hisLootIsEachPlayersOwn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		field(level, origin);
		PumpkinKingEntity king = waiting(helper, origin);
		ServerPlayer first = player(helper, origin, 30.5, 58.5);
		ServerPlayer second = player(helper, origin, 34.5, 58.5);
		ServerPlayer picker = player(helper, origin, 24.5, 21.5);
		ServerPlayer bystander = player(helper, origin, 38.5, 58.5);
		List<ServerPlayer> all = List.of(first, second, picker, bystander);
		all.forEach(player -> {
			player.setGameMode(GameType.SURVIVAL);
			shield(player);
		});
		king.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			king.took(first, 10.0F);
			king.took(second, 4.0F);
			king.ripen(level, List.of(Hill.NORTH_WEST, Hill.NORTH));
			use(level, picker, Hill.NORTH_WEST.at(origin));
			use(level, picker, Hill.NORTH.at(origin));
			ending(level, king, () -> {
				List<ServerPlayer> participants = king.participants(level);
				helper.assertTrue(participants.contains(first) && participants.contains(second) && participants.contains(picker),
						"Both who struck him and the one who left him bereft take part");
				helper.assertFalse(participants.contains(bystander), "A bystander who took no part does");
				helper.assertTrue(PumpkinKingLoot.firstKill(first), "A newcomer has already won Uncrowned");
				List<ItemStack> loot = PumpkinKingLoot.roll(level, king.position(), first);
				helper.assertTrue(loot.stream().anyMatch(stack -> stack.is(JugcraftPumpkinKing.KINGS_EMBER)), "His loot has no King's Ember");
				helper.assertTrue(loot.stream().filter(PumpkinKingLoot::trophy).count() == 1, "A first win always brings one of his trophies");
				king.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
				helper.assertTrue(king.isDeadOrDying() && king.fire() == Fire.DARK, "He did not fall with his fire going out");
				for (BlockPos cell : Crowfield.postExitCells()) {
					helper.assertFalse(level.getBlockState(cell.offset(origin)).is(JugcraftLairs.LAIR_EXIT), "With no lair instance Grey Mist opened by his post");
				}
				int share = PumpkinKingEntity.EXPERIENCE / 3;
				for (ServerPlayer player : List.of(first, second, picker)) {
					helper.assertTrue(player.getInventory().countItem(JugcraftPumpkinKing.KINGS_EMBER) >= 1, "A participant's loot did not go into their inventory");
					int trophies = player.getInventory().countItem(JugcraftPumpkinKing.hallowreap()) + player.getInventory().countItem(JugcraftPumpkinKing.crowbill());
					helper.assertTrue(trophies == 1, "A first win brought " + trophies + " of his trophies, not 1");
					helper.assertTrue(player.totalExperience >= share, "A participant got " + player.totalExperience + " experience, not " + share);
					helper.assertFalse(PumpkinKingLoot.firstKill(player), "Uncrowned is not theirs");
				}
				helper.assertTrue(bystander.getInventory().countItem(JugcraftPumpkinKing.KINGS_EMBER) == 0 && bystander.totalExperience == 0, "The bystander got loot");
				helper.assertTrue(PumpkinKingLoot.firstKill(bystander), "The bystander won Uncrowned");
				all.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}
}

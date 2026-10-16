package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Werewolf;
import io.github.jimbozoomer.jugcraft.agriculture.Werewolves;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.MoonlitRing;
import io.github.jimbozoomer.jugcraft.lair.MoonlitRing.Bell;
import io.github.jimbozoomer.jugcraft.lair.RakedTurfBlock;
import io.github.jimbozoomer.jugcraft.lair.alpha.AlphaWerewolfEntity;
import io.github.jimbozoomer.jugcraft.lair.alpha.AlphaWerewolfEntity.Action;
import io.github.jimbozoomer.jugcraft.lair.alpha.AlphaWerewolfEntity.Attack;
import io.github.jimbozoomer.jugcraft.lair.alpha.AlphaWerewolfEntity.Hunt;
import io.github.jimbozoomer.jugcraft.lair.alpha.AlphaWerewolfEntity.Phase;
import io.github.jimbozoomer.jugcraft.lair.alpha.AlphaWerewolfLoot;
import io.github.jimbozoomer.jugcraft.lair.alpha.JugcraftAlpha;
import io.github.jimbozoomer.jugcraft.lair.alpha.PackmateEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Alpha Werewolf (docs/features/alpha-werewolf.md), as far as a game-test server can show him: one put down on his
 * own (with no lair instance, over a ring laid out from the corner of an arena the clearing's size, so his stone, the
 * clearing and the three bell clearings are all loaded; each test lays a floor of moss, and the Warden Bells where it
 * rings them) crouches on his stone until a player who may fight him steps onto the turf, then howls, his pack lopes out,
 * and he leaps down; his health grows with the party, and his hide turns half of a blow but silver's until a bell flushes
 * him; his Hunt marks a quarry, runs to a clearing not theirs over a trail of raked turf, hides out of reach, is flushed
 * by its own clearing's bell and not another's, and unflushed springs on the quarry; in the Last Hunt he slips to another
 * clearing; at half health the Pack Gathers; his Claw Rake, Pounce, Rending Howl, Tree Leap and Hamstring Bite; his
 * Packmates bite, cower, are kept off by wolfsbane, and take silver harder; left alone he goes back to his stone and his
 * trail knits; the Packfinder Charm finds werewolves; and his loot is each participant's own. His fight in the Moonlit Ring
 * itself runs in a real world, in {@link AlphaWerewolfClientGameTests}.
 *
 * <p>He scales for players within 38 blocks of the clearing's middle and hunts those in his hunting ground; the arena
 * keeps every other test's players farther off than that. Test players are creative (no one he may fight) until the tick
 * a test needs them, so nothing he does before then touches them; those his pack must not bite hold wolfsbane. Each test
 * sends him back to his stone, with his pack, and takes him away.
 */
public class AlphaWerewolfGameTests {
	/** An arena of air the size of the ring's clearing and its bell clearings (76 x 76), the clearing's middle in its middle. */
	private static final String GLADE = "jugcraft-test:moonlit_glade";
	/**
	 * A new player cannot be hurt until its client has loaded the world, or for 60 ticks; a mock player has no client, so
	 * a test that hurts one waits this long first.
	 */
	private static final int LOADING_TICKS = 80;
	/** A creature hurt shrugs off a blow no harder than the last for 10 ticks after (vanilla's); a test waits this long between blows. */
	private static final int HURT_COOLDOWN = 12;
	/** Some ticks past his waking, when he has leapt down off the stone. */
	private static final int AWAKE = AlphaWerewolfEntity.WAKE_TICKS + 16;
	/** From the start of a hunt to his lying in wait, and a little more. */
	private static final int TO_GROUND = AlphaWerewolfEntity.MARK_TICKS + AlphaWerewolfEntity.RUN_TICKS + 4;
	/** The way each bell faces (into the ring), in the order of {@link Bell}. */
	private static final Direction[] BELL_FACES = {Direction.EAST, Direction.SOUTH, Direction.WEST};

	/** The origin of a ring laid out from the arena's corner: its floor a block over the arena's. */
	private static BlockPos origin(GameTestHelper helper) {
		return helper.absolutePos(new BlockPos(0, 1 - MoonlitRing.STAND, 0));
	}

	/** He crouches on the stone of the ring laid out from {@code origin}. */
	private static AlphaWerewolfEntity waiting(GameTestHelper helper, BlockPos origin) {
		return AlphaWerewolfEntity.summon(helper.getLevel(), origin, null);
	}

	/** The ring's turf, moss, under the whole arena. */
	private static void turf(ServerLevel level, BlockPos origin) {
		BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
		for (int x = 0; x < 76; x++) {
			for (int z = 0; z < 76; z++) {
				level.setBlock(origin.offset(x, MoonlitRing.SOIL, z), moss, Block.UPDATE_CLIENTS);
			}
		}
	}

	/** The three Warden Bells, each hung where it is in the ring. */
	private static void bells(ServerLevel level, BlockPos origin) {
		for (Bell bell : Bell.values()) {
			level.setBlock(bell.at(origin), JugcraftLairs.WARDEN_BELL.defaultBlockState()
					.setValue(HorizontalDirectionalBlock.FACING, BELL_FACES[bell.ordinal()]), Block.UPDATE_CLIENTS);
		}
	}

	/** A creative player (no one he may fight) standing at the template's ({@code x}, {@code z}) on the floor. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos origin, double x, double z) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.CREATIVE);
		player.snapTo(origin.getX() + x, origin.getY() + MoonlitRing.STAND, origin.getZ() + z, 0.0F, 0.0F);
		return player;
	}

	/** {@code player} holds a sprig of wolfsbane in their offhand: the pack will not bite them. */
	private static void sprig(ServerPlayer player) {
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Werewolves.wolfsbane()));
	}

	/** Kept from harm (Resistance V) for the rest of a test. */
	private static void shield(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 4, false, false));
	}

	/** Puts him on the floor at the template's ({@code x}, {@code z}), facing south. */
	private static void stand(AlphaWerewolfEntity alpha, double x, double z) {
		BlockPos origin = alpha.origin();
		alpha.teleportTo(origin.getX() + x, alpha.floorY(), origin.getZ() + z);
		alpha.setYRot(0.0F);
		alpha.setYBodyRot(0.0F);
		alpha.setYHeadRot(0.0F);
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

	/** Whether his bar says the pack hunts {@code player} (read from its words' arguments: the server has no mod's words). */
	private static boolean names(Component bar, ServerPlayer player) {
		return bar.getContents() instanceof TranslatableContents words && words.getKey().equals("message.jugcraft.alpha_werewolf.hunt_bar")
				&& words.getArgs().length == 1 && words.getArgs()[0] instanceof Component name
				&& name.getString().contains(player.getName().getString());
	}

	/** His Packmates, alive. */
	private static List<PackmateEntity> pack(ServerLevel level, AlphaWerewolfEntity alpha) {
		List<PackmateEntity> out = new ArrayList<>();
		for (UUID id : alpha.pack()) {
			if (level.getEntity(id) instanceof PackmateEntity packmate && packmate.isAlive()) {
				out.add(packmate);
			}
		}
		return out;
	}

	/** Checks {@code body}, then sends him back to his stone and takes him away whether it passed or failed. */
	private static void ending(ServerLevel level, AlphaWerewolfEntity alpha, Runnable body) {
		try {
			body.run();
		} finally {
			if (alpha.isAlive()) {
				alpha.reset(level);
			}
			alpha.discard();
		}
	}

	/**
	 * He crouches on the Howling Stone, his eyes dark, unhurt by anything; a player in the camp does not wake him, nor does
	 * a creative player on the turf or their blow; a player who may fight him on the turf does: his eyes light, he is
	 * unhurt while he wakes, his pack of {@value AlphaWerewolfEntity#PACK_SIZE} lopes out of the trees, and he leaps down
	 * onto the spoke toward the player at full health, scaled for one.
	 */
	@GameTest(structure = GLADE, padding = 8, maxTicks = 200)
	public void heCrouchesOnHisStoneAndAPlayerOnTheTurfWakesHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		turf(level, origin);
		AlphaWerewolfEntity alpha = waiting(helper, origin);
		ServerPlayer player = player(helper, origin, 37.5, 65.5);
		player.setGameMode(GameType.SURVIVAL);
		sprig(player);
		shield(player);
		helper.runAfterDelay(25, () -> {
			boolean camped = alpha.phase() == Phase.WAITING;
			boolean crouched = alpha.position().distanceTo(alpha.waiting()) < 0.01 && alpha.action() == Action.CROUCH && !alpha.eyesLit();
			player.setGameMode(GameType.CREATIVE);
			player.snapTo(origin.getX() + 37.5, alpha.floorY(), origin.getZ() + 50.5, 0.0F, 0.0F);
			boolean harmless = !alpha.hurtServer(level, level.damageSources().playerAttack(player), 10.0F) && alpha.getHealth() == alpha.getMaxHealth();
			helper.runAfterDelay(25, () -> {
				boolean slept = alpha.phase() == Phase.WAITING;
				player.setGameMode(GameType.SURVIVAL);
				helper.runAfterDelay(12, () -> {
					boolean woken = alpha.phase() == Phase.WAKING && alpha.eyesLit() && alpha.getHealth() == alpha.getMaxHealth();
					boolean unhurt = !alpha.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
					helper.runAfterDelay(AWAKE, () -> {
						ending(level, alpha, () -> {
							Vec3 down = alpha.downSpot(player);
							helper.assertTrue(camped, "A player in the camp woke him");
							helper.assertTrue(crouched, "He does not crouch on his stone, his eyes dark");
							helper.assertTrue(harmless, "Waiting, he was harmed");
							helper.assertTrue(slept, "A creative player on the turf, or their blow, woke him");
							helper.assertTrue(woken, "A player on the turf did not wake him at full health, his eyes lit");
							helper.assertTrue(unhurt, "Waking, he was harmed");
							helper.assertTrue(alpha.getMaxHealth() == (float) (AlphaWerewolfEntity.HEALTH * AlphaWerewolfEntity.partyScale(1)),
									"Woken by one player, his health is " + alpha.getMaxHealth());
							helper.assertTrue(alpha.phase() == Phase.CHASE, "He did not leap down to fight: " + alpha.phase());
							helper.assertTrue(pack(level, alpha).size() == AlphaWerewolfEntity.PACK_SIZE, "His pack is " + pack(level, alpha).size() + " strong");
							helper.assertTrue(down.z > alpha.centre().z && Math.abs(alpha.getY() - alpha.floorY()) < 0.6
									&& Math.hypot(alpha.getX() - down.x, alpha.getZ() - down.z) < 5.0,
									"He did not leap down onto the south spoke, toward the player, but to " + alpha.position());
							player.discard();
						});
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * His health grows by half for each player after the first, at most two and a half times; a player's blow from the
	 * camp wakes him, unhurt; Wither and poison never take hold of him. His hide lets half of a plain blow through, silver
	 * all of it; flushed, a plain blow lands in full and silver half as much again; every blow counts as taking part.
	 */
	@GameTest(structure = GLADE, padding = 8, maxTicks = 200)
	public void hisHideTurnsHalfABlowButSilversAndAFlushOpensIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		turf(level, origin);
		AlphaWerewolfEntity alpha = waiting(helper, origin);
		ServerPlayer camper = player(helper, origin, 35.5, 65.5);
		ServerPlayer other = player(helper, origin, 39.5, 65.5);
		camper.setGameMode(GameType.SURVIVAL);
		other.setGameMode(GameType.SURVIVAL);
		ServerPlayer striker = player(helper, origin, 37.5, 52.5);
		helper.assertTrue(AlphaWerewolfEntity.partyScale(1) == 1.0 && AlphaWerewolfEntity.partyScale(2) == 1.5
				&& AlphaWerewolfEntity.partyScale(4) == 2.5 && AlphaWerewolfEntity.partyScale(9) == 2.5, "His health scales with the party, at most 2.5 times");
		helper.assertTrue(!alpha.canBeAffected(new MobEffectInstance(MobEffects.WITHER, 100))
				&& !alpha.canBeAffected(new MobEffectInstance(MobEffects.POISON, 100)), "Wither or poison can take hold of him");
		boolean blow = alpha.hurtServer(level, level.damageSources().playerAttack(camper), 10.0F);
		helper.assertTrue(!blow && alpha.phase() == Phase.WAKING, "A player's blow from the camp did not wake him, unhurt");
		helper.assertTrue(alpha.getMaxHealth() == (float) (AlphaWerewolfEntity.HEALTH * AlphaWerewolfEntity.partyScale(2)),
				"Woken with two players near his ring, his health is " + alpha.getMaxHealth());
		float[] dealt = new float[4];
		ItemStack dagger = new ItemStack(item("silver_dagger"));
		helper.runAfterDelay(AWAKE, () -> {
			float before = alpha.getHealth();
			alpha.hurtServer(level, level.damageSources().playerAttack(striker), 20.0F);
			dealt[0] = before - alpha.getHealth();
		});
		helper.runAfterDelay(AWAKE + HURT_COOLDOWN, () -> {
			striker.setItemInHand(InteractionHand.MAIN_HAND, dagger.copy());
			float before = alpha.getHealth();
			alpha.hurtServer(level, level.damageSources().playerAttack(striker), 20.0F);
			dealt[1] = before - alpha.getHealth();
			alpha.flush(level, Bell.WEST, null);
		});
		helper.runAfterDelay(AWAKE + 2 * HURT_COOLDOWN, () -> {
			striker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			float before = alpha.getHealth();
			alpha.hurtServer(level, level.damageSources().playerAttack(striker), 20.0F);
			dealt[2] = before - alpha.getHealth();
		});
		helper.runAfterDelay(AWAKE + 3 * HURT_COOLDOWN, () -> {
			striker.setItemInHand(InteractionHand.MAIN_HAND, dagger.copy());
			float before = alpha.getHealth();
			alpha.hurtServer(level, level.damageSources().playerAttack(striker), 20.0F);
			dealt[3] = before - alpha.getHealth();
			ending(level, alpha, () -> {
				// His armour takes a little more off each: never more than his hide's half of a plain blow gets through.
				helper.assertTrue(dealt[0] > 0.0F && dealt[0] <= 20.0F * AlphaWerewolfEntity.HIDE + 1.0E-3F,
						"His hide let " + dealt[0] + " of a plain blow of 20 through, not at most " + 20.0F * AlphaWerewolfEntity.HIDE);
				helper.assertTrue(dealt[1] > 15.0F, "Silver did only " + dealt[1] + " of 20 through his hide");
				helper.assertTrue(dealt[2] > 15.0F, "Flushed, a plain blow of 20 did only " + dealt[2]);
				helper.assertTrue(dealt[3] > 25.0F, "Flushed, silver did only " + dealt[3] + " of 20, not half as much again");
				helper.assertTrue(alpha.participants(level).contains(striker) && !alpha.participants(level).contains(other),
						"Hurting him did not count as taking part, or standing by did");
				for (ServerPlayer each : List.of(camper, other, striker)) {
					each.discard();
				}
			});
			helper.succeed();
		});
	}

	/**
	 * The Hunt: he marks his quarry with a short howl and the bar names them; he runs to a clearing that is not the one
	 * nearest them, tearing a trail of raked turf through the moss as he goes, and lies in wait there, out of reach of any
	 * blow; the pack turns on the quarry.
	 */
	@GameTest(structure = GLADE, padding = 8, maxTicks = 240)
	public void theHuntMarksAQuarryAndHeRunsToGroundOverATrailOfRakedTurf(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		turf(level, origin);
		AlphaWerewolfEntity alpha = waiting(helper, origin);
		// South and a little east: the east clearing is the quarry's nearest.
		ServerPlayer quarry = player(helper, origin, 41.5, 50.5);
		sprig(quarry);
		ServerPlayer striker = player(helper, origin, 30.5, 50.5);
		alpha.wake(level);
		Vec3[] from = new Vec3[1];
		helper.runAfterDelay(AWAKE, () -> {
			quarry.setGameMode(GameType.SURVIVAL);
			shield(quarry);
			alpha.huntIn(1);
		});
		helper.runAfterDelay(AWAKE + 3, () -> {
			from[0] = alpha.position();
			boolean marked = alpha.hunt() == Hunt.MARK && alpha.action() == Action.MARK && names(alpha.bar(), quarry);
			boolean driven = alpha.packTarget(level) == quarry;
			helper.runAfterDelay(TO_GROUND, () -> {
				ending(level, alpha, () -> {
					Bell glade = alpha.glade();
					helper.assertTrue(marked, "He did not mark his quarry, the bar naming them: " + alpha.hunt() + ", " + alpha.bar());
					helper.assertTrue(driven, "His pack is not after his quarry");
					helper.assertTrue(alpha.hunt() == Hunt.HIDE && glade != null && alpha.action() == Action.HIDE, "He is not lying in wait: "
							+ alpha.hunt() + " in " + glade);
					helper.assertTrue(glade != Bell.EAST, "He hid in the clearing nearest his quarry");
					helper.assertTrue(alpha.position().distanceTo(alpha.hideSpot(glade)) < 0.5, "He is not in his clearing but at " + alpha.position());
					helper.assertTrue(alpha.hidden() && !alpha.hurtServer(level, level.damageSources().playerAttack(striker), 20.0F),
							"Hiding, he can be harmed");
					Set<BlockPos> raked = alpha.raked();
					helper.assertTrue(raked.size() >= 20, "His run left only " + raked.size() + " cells of raked turf");
					Vec3 to = alpha.hideSpot(glade);
					for (BlockPos cell : raked) {
						helper.assertTrue(level.getBlockState(cell).is(JugcraftLairs.RAKED_TURF) && cell.getY() == origin.getY() + MoonlitRing.SOIL,
								"His trail is not raked turf in the moss at " + cell);
						// Within a block and a half of the line he ran.
						Vec3 way = to.subtract(from[0]);
						Vec3 at = Vec3.atCenterOf(cell).subtract(from[0]);
						double along = Math.max(0.0, Math.min(1.0, (at.x * way.x + at.z * way.z) / (way.x * way.x + way.z * way.z)));
						double off = Math.hypot(at.x - way.x * along, at.z - way.z * along);
						helper.assertTrue(off < 1.6, "Raked turf at " + cell + " is " + off + " off the line he ran");
					}
					quarry.discard();
					striker.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * Lying in wait, he is flushed by the bell of his own clearing and not another's (which tolls over an empty clearing):
	 * he staggers out along its spoke for {@value AlphaWerewolfEntity#STAGGER_TICKS} ticks, his hide fails for
	 * {@value AlphaWerewolfEntity#FLUSHED_TICKS}, his pack cowers for {@value AlphaWerewolfEntity#COWER_TICKS}, and the
	 * ringer takes part.
	 */
	@GameTest(structure = GLADE, padding = 8, maxTicks = 260)
	public void theBellOfHisClearingFlushesHimAndAnotherDoesNot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		turf(level, origin);
		bells(level, origin);
		AlphaWerewolfEntity alpha = waiting(helper, origin);
		ServerPlayer quarry = player(helper, origin, 41.5, 50.5);
		sprig(quarry);
		ServerPlayer ringer = player(helper, origin, 37.5, 30.5);
		alpha.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			quarry.setGameMode(GameType.SURVIVAL);
			shield(quarry);
			alpha.huntIn(1);
		});
		helper.runAfterDelay(AWAKE + 3 + TO_GROUND, () -> {
			Bell glade = alpha.glade();
			boolean hiding = alpha.hunt() == Hunt.HIDE && glade != null;
			Bell other = glade == Bell.NORTH ? Bell.WEST : Bell.NORTH;
			use(level, ringer, other.at(origin));
			boolean stillHiding = alpha.hunt() == Hunt.HIDE && alpha.glade() == glade && !alpha.flushed();
			InteractionResult used = glade == null ? InteractionResult.PASS : use(level, ringer, glade.at(origin));
			boolean flushed = used.consumesAction() && alpha.hunt() == Hunt.NONE && alpha.flushed()
					&& alpha.flushedTicks() == AlphaWerewolfEntity.FLUSHED_TICKS && alpha.cowering()
					&& alpha.staggerTicks() == AlphaWerewolfEntity.STAGGER_TICKS && alpha.action() == Action.STAGGER;
			boolean part = alpha.participants(level).contains(ringer);
			boolean cower = alpha.cowerTicks() == AlphaWerewolfEntity.COWER_TICKS;
			helper.runAfterDelay(AlphaWerewolfEntity.STAGGER_TICKS + 2, () -> {
				ending(level, alpha, () -> {
					helper.assertTrue(hiding, "He did not lie in wait in a clearing");
					helper.assertTrue(stillHiding, "Another clearing's bell flushed him");
					helper.assertTrue(flushed, "His own clearing's bell did not flush him: " + used + ", " + alpha.hunt());
					helper.assertTrue(part && cower, "The ringer does not take part, or his pack does not cower");
					Vec3 out = alpha.flushSpot(glade);
					helper.assertTrue(Math.hypot(alpha.getX() - out.x, alpha.getZ() - out.z) < 2.5 && Math.abs(alpha.getY() - alpha.floorY()) < 0.6,
							"Flushed, he did not stagger out along the spoke but to " + alpha.position());
					helper.assertTrue(alpha.flushed() && alpha.huntIn() > AlphaWerewolfEntity.FLUSHED_TICKS, "His hide mended too soon, or he hunts again too soon");
					quarry.discard();
					ringer.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * Left in wait, a shadow spreads under his quarry and he springs onto it: {@value AlphaWerewolfEntity#SPRING_DAMAGE}
	 * to the quarry, knocked flat (Slowness IV), and nothing to a player off the shadow; the hunt is over, the next waits
	 * its full gap.
	 */
	@GameTest(structure = GLADE, padding = 8, maxTicks = 260)
	public void unflushedHeSpringsOntoHisQuarry(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		turf(level, origin);
		AlphaWerewolfEntity alpha = waiting(helper, origin);
		ServerPlayer quarry = player(helper, origin, 41.5, 50.5);
		ServerPlayer clear = player(helper, origin, 41.5, 56.5);
		sprig(quarry);
		sprig(clear);
		alpha.wake(level);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			quarry.setGameMode(GameType.SURVIVAL);
			clear.setGameMode(GameType.SURVIVAL);
			alpha.huntIn(1);
		});
		helper.runAfterDelay(LOADING_TICKS + 3 + TO_GROUND, () -> {
			boolean hiding = alpha.hunt() == Hunt.HIDE;
			alpha.ambushTicks(AlphaWerewolfEntity.AMBUSH_TICKS - AlphaWerewolfEntity.SHADOW_TICKS - 1);
			helper.runAfterDelay(3, () -> {
				boolean shadowed = alpha.shadow().distanceTo(quarry.position()) < 0.5;
				helper.runAfterDelay(AlphaWerewolfEntity.SHADOW_TICKS + 2, () -> {
					ending(level, alpha, () -> {
						helper.assertTrue(hiding, "He did not lie in wait");
						helper.assertTrue(shadowed, "No shadow spread under his quarry: " + alpha.shadow());
						helper.assertTrue(lost(quarry) >= AlphaWerewolfEntity.SPRING_DAMAGE - 0.5F, "His spring did " + lost(quarry) + " to his quarry");
						MobEffectInstance slow = quarry.getEffect(MobEffects.SLOWNESS);
						helper.assertTrue(slow != null && slow.getAmplifier() == 3, "His quarry was not knocked flat (Slowness IV)");
						helper.assertTrue(lost(clear) == 0.0F, "His spring struck a player off the shadow");
						helper.assertTrue(alpha.hunt() == Hunt.NONE && alpha.huntIn() > AlphaWerewolfEntity.HUNT_EVERY - 10,
								"The hunt did not end, the next waiting its gap: " + alpha.hunt() + ", " + alpha.huntIn());
						helper.assertTrue(Math.hypot(alpha.getX() - alpha.shadow().x, alpha.getZ() - alpha.shadow().z) < 2.0,
								"He did not land on the shadow but at " + alpha.position());
						quarry.discard();
						clear.discard();
					});
					helper.succeed();
				});
			});
		});
	}

	/**
	 * In the Last Hunt, {@value AlphaWerewolfEntity#SWITCH_AT} ticks into the ambush, he slips to another clearing; the bell
	 * of the one he left only tolls, and the one he is in flushes him.
	 */
	@GameTest(structure = GLADE, padding = 8, maxTicks = 260)
	public void inTheLastHuntHeSlipsToAnotherClearing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		turf(level, origin);
		bells(level, origin);
		AlphaWerewolfEntity alpha = waiting(helper, origin);
		ServerPlayer quarry = player(helper, origin, 41.5, 50.5);
		sprig(quarry);
		ServerPlayer ringer = player(helper, origin, 37.5, 30.5);
		alpha.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			quarry.setGameMode(GameType.SURVIVAL);
			shield(quarry);
			alpha.lastHunt(level);
			alpha.huntIn(1);
		});
		Bell[] first = new Bell[1];
		helper.runAfterDelay(AWAKE + 3 + TO_GROUND, () -> {
			first[0] = alpha.glade();
			alpha.ambushTicks(AlphaWerewolfEntity.SWITCH_AT - 1);
		});
		helper.runAfterDelay(AWAKE + 5 + TO_GROUND, () -> {
			boolean slipping = alpha.hunt() == Hunt.SLIP && alpha.glade() == null && alpha.hidden();
			helper.runAfterDelay(AlphaWerewolfEntity.RUN_TICKS + 2, () -> {
				Bell second = alpha.glade();
				boolean moved = alpha.hunt() == Hunt.HIDE && second != null && second != first[0];
				int trail = alpha.raked().size();
				if (first[0] != null) {
					use(level, ringer, first[0].at(origin));
				}
				boolean left = alpha.hunt() == Hunt.HIDE && !alpha.flushed();
				if (second != null) {
					use(level, ringer, second.at(origin));
				}
				ending(level, alpha, () -> {
					helper.assertTrue(alpha.phase() == Phase.LAST_HUNT || alpha.phase() == Phase.WAITING, "Not the Last Hunt");
					helper.assertTrue(first[0] != null, "He did not hide in a clearing first");
					helper.assertTrue(slipping, "In the Last Hunt he did not slip away halfway through the ambush");
					helper.assertTrue(moved, "He did not come to another clearing: " + first[0] + " then " + second);
					helper.assertTrue(trail > 0, "He left no trail");
					helper.assertTrue(left, "The bell of the clearing he had left flushed him");
					helper.assertTrue(alpha.participants(level).contains(ringer), "The bell of the clearing he was in did not flush him");
					quarry.discard();
					ringer.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * At half his health the Pack Gathers: he leaps onto the stone, unhurt, and howls his pack back up to
	 * {@value AlphaWerewolfEntity#PACK_SIZE}; then the Moon's Fury, which adds the Tree Leap and the Hamstring Bite and
	 * hunts sooner; the Chase has neither.
	 */
	@GameTest(structure = GLADE, padding = 8, maxTicks = 220)
	public void atHalfHealthThePackGathers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		turf(level, origin);
		AlphaWerewolfEntity alpha = waiting(helper, origin);
		ServerPlayer striker = player(helper, origin, 37.5, 52.5);
		alpha.wake(level);
		helper.assertTrue(Attack.CLAW_RAKE.chase && Attack.POUNCE.chase && Attack.RENDING_HOWL.chase && !Attack.TREE_LEAP.chase
				&& !Attack.HAMSTRING_BITE.chase, "The Chase's attacks are not the Claw Rake, the Pounce and the Rending Howl");
		helper.assertTrue(Attack.TREE_LEAP.fury && Attack.HAMSTRING_BITE.fury, "The Moon's Fury lacks the Tree Leap or the Hamstring Bite");
		helper.runAfterDelay(AWAKE, () -> {
			List<PackmateEntity> pack = pack(level, alpha);
			if (!pack.isEmpty()) {
				pack.get(0).discard();
			}
			alpha.setHealth(alpha.getMaxHealth() * AlphaWerewolfEntity.GATHER_AT - 1.0F);
		});
		helper.runAfterDelay(AWAKE + 3, () -> {
			boolean gathering = alpha.phase() == Phase.GATHERING && alpha.action() == Action.GATHER;
			boolean unhurt = !alpha.hurtServer(level, level.damageSources().playerAttack(striker), 10.0F);
			int before = pack(level, alpha).size();
			helper.runAfterDelay(AlphaWerewolfEntity.GATHER_TICKS - 8, () -> {
				boolean onStone = alpha.position().distanceTo(alpha.waiting()) < 0.6;
				int after = pack(level, alpha).size();
				helper.runAfterDelay(20, () -> {
					ending(level, alpha, () -> {
						helper.assertTrue(gathering, "At half health the Pack did not Gather: " + alpha.phase());
						helper.assertTrue(unhurt, "Gathering his pack, he was harmed");
						helper.assertTrue(onStone, "He did not leap onto the stone");
						helper.assertTrue(before == AlphaWerewolfEntity.PACK_SIZE - 1 && after == AlphaWerewolfEntity.PACK_SIZE,
								"His howl did not call the pack back up to full: " + before + " then " + after);
						helper.assertTrue(alpha.phase() == Phase.FURY && alpha.huntEvery() == AlphaWerewolfEntity.FURY_HUNT_EVERY
								&& alpha.huntIn() <= AlphaWerewolfEntity.FURY_HUNT_EVERY, "The Moon's Fury did not follow, hunting sooner");
						helper.assertTrue(Math.abs(alpha.getY() - alpha.floorY()) < 0.6, "He did not leap down off the stone");
						striker.discard();
					});
					helper.succeed();
				});
			});
		});
	}

	/**
	 * Claw Rake strikes before him and not behind; the Hamstring Bite bites and slows (Slowness II); the Rending Howl
	 * weakens those within {@value AlphaWerewolfEntity#HOWL_REACH} blocks and not beyond, and quickens his pack.
	 */
	@GameTest(structure = GLADE, padding = 8, maxTicks = 160)
	public void hisClawsHisBiteAndHisHowl(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		turf(level, origin);
		AlphaWerewolfEntity alpha = waiting(helper, origin);
		ServerPlayer front = player(helper, origin, 37.5, 47.5);
		ServerPlayer behind = player(helper, origin, 37.5, 43.0);
		ServerPlayer far = player(helper, origin, 37.5, 58.5);
		List<ServerPlayer> players = List.of(front, behind, far);
		players.forEach(AlphaWerewolfGameTests::sprig);
		alpha.wake(level);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, alpha, () -> {
				stand(alpha, 37.5, 45.0);
				int raked = alpha.clawRake(level);
				helper.assertTrue(raked == 1 && lost(front) > 0.0F && lost(behind) == 0.0F, "His Claw Rake struck " + raked + ", not the one before him alone");
				float clawed = lost(front);
				helper.assertTrue(Math.abs(clawed - AlphaWerewolfEntity.RAKE_DAMAGE) < 0.5F, "His Claw Rake did " + clawed);
				boolean bit = alpha.hamstring(level, behind);
				helper.assertTrue(bit && lost(behind) > 0.0F, "His Hamstring Bite did not bite");
				MobEffectInstance slow = behind.getEffect(MobEffects.SLOWNESS);
				helper.assertTrue(slow != null && slow.getAmplifier() == 1, "His Hamstring Bite did not slow (Slowness II)");
				int weakened = alpha.rendingHowl(level);
				helper.assertTrue(weakened == 2 && front.hasEffect(MobEffects.WEAKNESS) && !far.hasEffect(MobEffects.WEAKNESS),
						"His Rending Howl weakened " + weakened + ", not the two within earshot");
				for (PackmateEntity packmate : pack(level, alpha)) {
					helper.assertTrue(packmate.hasEffect(MobEffects.SPEED), "His howl did not quicken a Packmate");
				}
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * The Pounce: a shadow spreads where his foe stands, and he leaps onto it: {@value AlphaWerewolfEntity#POUNCE_DAMAGE}
	 * to whoever is on it, nothing to one off it. The Tree Leap: he leaps into the trees and is gone (no blow reaches him),
	 * a shadow spreads {@value AlphaWerewolfEntity#TREE_BEHIND} blocks behind his foe, and he drops onto it.
	 */
	@GameTest(structure = GLADE, padding = 8, maxTicks = 260)
	public void hisPounceAndHisTreeLeapLandOnTheirShadows(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		turf(level, origin);
		AlphaWerewolfEntity alpha = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 37.5, 52.5);
		ServerPlayer aside = player(helper, origin, 42.0, 52.5);
		ServerPlayer striker = player(helper, origin, 30.5, 40.5);
		sprig(foe);
		sprig(aside);
		alpha.wake(level);
		Attack pounce = Attack.POUNCE;
		Attack leap = Attack.TREE_LEAP;
		helper.runAfterDelay(LOADING_TICKS, () -> {
			foe.setGameMode(GameType.SURVIVAL);
			aside.setGameMode(GameType.SURVIVAL);
			stand(alpha, 37.5, 44.5);
			alpha.begin(level, pounce, foe);
		});
		boolean[] shadowed = new boolean[1];
		helper.runAfterDelay(LOADING_TICKS + 2, () -> shadowed[0] = alpha.shadow().distanceTo(foe.position()) < 0.5);
		float[] pounced = new float[2];
		helper.runAfterDelay(LOADING_TICKS + pounce.length() + 2, () -> {
			pounced[0] = lost(foe);
			pounced[1] = lost(aside);
			foe.heal(foe.getMaxHealth());
			foe.setYRot(0.0F);
			foe.setYHeadRot(0.0F);
			alpha.begin(level, leap, foe);
		});
		boolean[] gone = new boolean[1];
		helper.runAfterDelay(LOADING_TICKS + pounce.length() + 14, () -> gone[0] = alpha.hidden() && alpha.isInvisible()
				&& !alpha.hurtServer(level, level.damageSources().playerAttack(striker), 20.0F));
		Vec3[] behind = new Vec3[1];
		helper.runAfterDelay(LOADING_TICKS + pounce.length() + AlphaWerewolfEntity.TREE_TICKS + 3, () -> behind[0] = alpha.shadow());
		helper.runAfterDelay(LOADING_TICKS + pounce.length() + leap.windup + 6, () -> {
			ending(level, alpha, () -> {
				helper.assertTrue(shadowed[0], "No shadow spread where his foe stood for the Pounce");
				helper.assertTrue(pounced[0] >= AlphaWerewolfEntity.POUNCE_DAMAGE - 0.5F, "His Pounce did " + pounced[0] + " to the one on the shadow");
				helper.assertTrue(pounced[1] == 0.0F, "His Pounce struck one off the shadow");
				helper.assertTrue(gone[0], "In the trees he could be seen or harmed");
				Vec3 expected = foe.position().add(0.0, 0.0, -AlphaWerewolfEntity.TREE_BEHIND);
				helper.assertTrue(behind[0] != null && Math.hypot(behind[0].x - expected.x, behind[0].z - expected.z) < 0.6,
						"The Tree Leap's shadow is not behind his foe but at " + behind[0]);
				helper.assertTrue(!alpha.isInvisible() && !alpha.hidden(), "He did not come out of the trees");
				helper.assertTrue(lost(foe) >= AlphaWerewolfEntity.LEAP_DAMAGE - 0.5F, "His drop did " + lost(foe) + " to his foe");
				for (ServerPlayer each : List.of(foe, aside, striker)) {
					each.discard();
				}
			});
			helper.succeed();
		});
	}

	/**
	 * His Packmates: a bite does {@value PackmateEntity#DAMAGE}; a sprig of wolfsbane in hand wards a player off; silver
	 * does half as much again to one, a plain blow all of it, and hurting one counts as taking part in his fight; flushed,
	 * he sets them cowering; and when he goes back to his stone they melt into the trees.
	 */
	@GameTest(structure = GLADE, padding = 8, maxTicks = 200)
	public void hisPackBitesCowersAndMeltsAway(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		turf(level, origin);
		AlphaWerewolfEntity alpha = waiting(helper, origin);
		ServerPlayer bitten = player(helper, origin, 37.5, 60.5);
		ServerPlayer warded = player(helper, origin, 39.5, 60.5);
		ServerPlayer hunter = player(helper, origin, 35.5, 60.5);
		sprig(warded);
		alpha.wake(level);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			bitten.setGameMode(GameType.SURVIVAL);
			List<PackmateEntity> pack = pack(level, alpha);
			ending(level, alpha, () -> {
				helper.assertTrue(pack.size() == AlphaWerewolfEntity.PACK_SIZE, "His pack is " + pack.size() + " strong");
				PackmateEntity first = pack.get(0);
				helper.assertTrue(first.bite(level, bitten) && Math.abs(lost(bitten) - PackmateEntity.DAMAGE) < 0.01F, "A Packmate's bite did " + lost(bitten));
				helper.assertTrue(PackmateEntity.warded(warded) && !PackmateEntity.warded(bitten), "A sprig of wolfsbane does not ward a player off");
				first.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0.0);
				float before = first.getHealth();
				first.hurtServer(level, level.damageSources().playerAttack(hunter), 4.0F);
				float plain = before - first.getHealth();
				PackmateEntity second = pack.get(1);
				hunter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("silver_dagger")));
				before = second.getHealth();
				second.hurtServer(level, level.damageSources().playerAttack(hunter), 4.0F);
				float silver = before - second.getHealth();
				helper.assertTrue(Math.abs(plain - 4.0F) < 0.6F && silver > plain + 1.0F, "A plain blow did " + plain + " and silver " + silver + " to a Packmate");
				helper.assertTrue(alpha.participants(level).contains(hunter), "Hurting a Packmate did not count as taking part");
				helper.assertTrue(first.getScale() < 1.0F, "A Packmate is no smaller than a werewolf");
				alpha.flush(level, Bell.NORTH, null);
				helper.assertTrue(alpha.cowering(), "Flushed, he did not set his pack cowering");
				alpha.reset(level);
				helper.assertTrue(alpha.phase() == Phase.WAITING && alpha.pack().isEmpty() && pack.stream().noneMatch(PackmateEntity::isAlive),
						"Gone back to his stone, his pack did not melt away");
				for (ServerPlayer each : List.of(bitten, warded, hunter)) {
					each.discard();
				}
			});
			helper.succeed();
		});
	}

	/**
	 * His trail is raked turf, torn from the moss and from a trodden path alike, which knits back to what it was by itself
	 * {@value RakedTurfBlock#KNIT_TICKS} ticks after; it cannot be broken and has no item. Left alone for
	 * {@value AlphaWerewolfEntity#ABANDON_TICKS} ticks he goes back to his stone, healed, his eyes dark, his pack gone and
	 * his trail knit at once, the path a path again.
	 */
	@GameTest(structure = GLADE, padding = 8, maxTicks = 340)
	public void leftAloneHeGoesBackToHisStoneAndHisTrailKnits(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		turf(level, origin);
		AlphaWerewolfEntity alpha = waiting(helper, origin);
		alpha.wake(level);
		Vec3 centre = alpha.centre();
		BlockPos path = BlockPos.containing(centre.x, origin.getY() + MoonlitRing.SOIL, centre.z + 6.0);
		level.setBlock(path, Blocks.DIRT_PATH.defaultBlockState(), Block.UPDATE_CLIENTS);
		int torn = alpha.tearTurf(level, centre.add(-10.0, 0.0, 6.0), centre.add(10.0, 0.0, 6.0));
		helper.assertTrue(level.getBlockState(path).is(JugcraftLairs.RAKED_TURF) && RakedTurfBlock.knitsTo(level.getBlockState(path)).is(Blocks.DIRT_PATH),
				"A trodden path he ran over is not raked turf that knits back to a path");
		Set<BlockPos> trail = alpha.raked();
		BlockPos one = trail.iterator().next();
		BlockState turf = level.getBlockState(one);
		helper.assertTrue(torn >= 20 && turf.is(JugcraftLairs.RAKED_TURF), "His trail tore " + torn + " cells");
		helper.assertTrue(level.getBlockTicks().hasScheduledTick(one, JugcraftLairs.RAKED_TURF), "Raked turf does not knit by itself");
		helper.assertTrue(turf.getDestroySpeed(level, one) < 0.0F && JugcraftLairs.RAKED_TURF.asItem() == Items.AIR,
				"Raked turf can be broken, or has an item");
		turf.tick(level, one, level.getRandom());
		helper.assertTrue(level.getBlockState(one).is(Blocks.MOSS_BLOCK), "Raked turf did not knit back to moss");
		helper.runAfterDelay(AWAKE, () -> alpha.setHealth(alpha.getMaxHealth() / 2.0F + 10.0F));
		helper.runAfterDelay(AlphaWerewolfEntity.WAKE_TICKS + AlphaWerewolfEntity.ABANDON_TICKS + 30, () -> {
			ending(level, alpha, () -> {
				helper.assertTrue(alpha.phase() == Phase.WAITING && !alpha.eyesLit() && alpha.getHealth() == alpha.getMaxHealth(),
						"Left alone he did not go back to his stone, healed, his eyes dark: " + alpha.phase());
				helper.assertTrue(alpha.position().distanceTo(alpha.waiting()) < 0.01, "He is not on his stone");
				helper.assertTrue(alpha.pack().isEmpty(), "His pack did not melt away");
				for (BlockPos cell : trail) {
					Block was = cell.equals(path) ? Blocks.DIRT_PATH : Blocks.MOSS_BLOCK;
					helper.assertTrue(level.getBlockState(cell).is(was), "His trail did not knit back at " + cell + ": " + level.getBlockState(cell));
				}
				helper.assertTrue(alpha.raked().isEmpty(), "He still remembers a trail");
			});
			helper.succeed();
		});
	}

	/**
	 * The Packfinder Charm in the offhand makes every werewolf within {@value JugcraftAlpha#CHARM_REACH} blocks glow, and
	 * none beyond; the Alpha Pelt tans into four leather and binds a Howling Totem as a werewolf's pelt does; the Alpha's
	 * Hood is a costume worn on the head.
	 */
	@GameTest(structure = GLADE, padding = 8, maxTicks = 60)
	public void thePackfinderCharmThePeltAndTheHood(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		turf(level, origin);
		ServerPlayer holder = player(helper, origin, 10.5, 10.5);
		holder.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftAlpha.PACKFINDER_CHARM));
		Werewolf near = helper.spawn(JugcraftAgriculture.WEREWOLF, new BlockPos(30, 1, 10));
		Werewolf far = helper.spawn(JugcraftAgriculture.WEREWOLF, new BlockPos(50, 1, 10));
		near.setNoAi(true);
		far.setNoAi(true);
		helper.assertTrue(JugcraftAlpha.holdsCharm(holder), "The charm in the offhand is not held");
		int found = JugcraftAlpha.glow(level, holder);
		helper.assertTrue(found == 1 && near.hasEffect(MobEffects.GLOWING) && !far.hasEffect(MobEffects.GLOWING),
				"The charm found " + found + " werewolves, not the one within reach");
		ItemStack pelt = new ItemStack(JugcraftAlpha.ALPHA_PELT);
		CraftingInput tan = CraftingInput.of(1, 1, List.of(pelt));
		ItemStack leather = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, tan, level)
				.orElseThrow(() -> helper.assertionException("The Alpha Pelt tans into nothing")).value().assemble(tan);
		helper.assertTrue(leather.is(Items.LEATHER) && leather.getCount() == 4, "The Alpha Pelt tanned into " + leather);
		ItemStack silver = new ItemStack(item("silver_ingot"));
		ItemStack bone = new ItemStack(Items.BONE);
		CraftingInput grid = CraftingInput.of(3, 3, List.of(silver, pelt.copy(), silver.copy(), ItemStack.EMPTY, bone, ItemStack.EMPTY, bone.copy(),
				bone.copy(), ItemStack.EMPTY));
		ItemStack totem = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level)
				.orElseThrow(() -> helper.assertionException("The Alpha Pelt binds no Howling Totem")).value().assemble(grid);
		helper.assertTrue(totem.is(JugcraftLairs.HOWLING_TOTEM), "The Alpha Pelt, bones and silver made " + totem);
		ItemStack hood = new ItemStack(JugcraftAlpha.ALPHAS_HOOD);
		TagKey<Item> costumes = TagKey.create(Registries.ITEM, Jugcraft.id("trick_or_treat_costumes"));
		var equippable = hood.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
		helper.assertTrue(hood.is(costumes) && equippable != null && equippable.slot() == EquipmentSlot.HEAD,
				"The Alpha's Hood is not a costume worn on the head");
		near.discard();
		far.discard();
		holder.discard();
		helper.succeed();
	}

	/**
	 * Each participant (who hurt him or his pack, or rang the bell that flushed him) rolls his loot for themselves: the
	 * Alpha Pelt always, and on a first kill one of his trophies; the experience is shared; Hunter's Moon is theirs. A
	 * bystander gets nothing. He falls with his eyes going dark, and with no lair instance no Grey Mist opens.
	 */
	@GameTest(structure = GLADE, padding = 8, maxTicks = 60)
	public void hisLootIsEachParticipantsOwn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		turf(level, origin);
		AlphaWerewolfEntity alpha = waiting(helper, origin);
		ServerPlayer first = player(helper, origin, 33.5, 65.5);
		ServerPlayer second = player(helper, origin, 37.5, 65.5);
		ServerPlayer ringer = player(helper, origin, 41.5, 65.5);
		ServerPlayer bystander = player(helper, origin, 35.5, 67.5);
		List<ServerPlayer> all = List.of(first, second, ringer, bystander);
		all.forEach(player -> player.setGameMode(GameType.SURVIVAL));
		alpha.wake(level);
		alpha.took(first, 10.0F);
		alpha.took(second, 4.0F);
		alpha.flush(level, Bell.EAST, ringer);
		ending(level, alpha, () -> {
			List<ServerPlayer> participants = alpha.participants(level);
			helper.assertTrue(participants.contains(first) && participants.contains(second) && participants.contains(ringer),
					"Both who struck him and the one who flushed him take part");
			helper.assertFalse(participants.contains(bystander), "A bystander who took no part does");
			helper.assertTrue(AlphaWerewolfLoot.firstKill(first), "A newcomer has already won Hunter's Moon");
			List<ItemStack> loot = AlphaWerewolfLoot.roll(level, alpha, first);
			helper.assertTrue(loot.stream().anyMatch(stack -> stack.is(JugcraftAlpha.ALPHA_PELT)), "His loot has no Alpha Pelt");
			helper.assertTrue(loot.stream().filter(AlphaWerewolfLoot::trophy).count() == 1, "A first kill always brings one of his trophies");
			alpha.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			helper.assertTrue(alpha.isDeadOrDying() && !alpha.eyesLit(), "He did not fall with his eyes going dark");
			for (BlockPos cell : MoonlitRing.stoneExitCells()) {
				helper.assertFalse(level.getBlockState(cell.offset(origin)).is(JugcraftLairs.LAIR_EXIT), "With no lair instance Grey Mist opened on the stone");
			}
			int share = AlphaWerewolfEntity.EXPERIENCE / 3;
			for (ServerPlayer player : List.of(first, second, ringer)) {
				helper.assertTrue(player.getInventory().countItem(JugcraftAlpha.ALPHA_PELT) >= 1, "A participant's loot did not go into their inventory");
				int trophies = player.getInventory().countItem(JugcraftAlpha.moonfang()) + player.getInventory().countItem(JugcraftAlpha.howler());
				helper.assertTrue(trophies == 1, "A first kill brought " + trophies + " of his trophies, not 1");
				helper.assertTrue(player.totalExperience >= share, "A participant got " + player.totalExperience + " experience, not " + share);
				helper.assertFalse(AlphaWerewolfLoot.firstKill(player), "Hunter's Moon is not theirs");
			}
			helper.assertTrue(bystander.getInventory().countItem(JugcraftAlpha.ALPHA_PELT) == 0 && bystander.totalExperience == 0, "The bystander got loot");
			helper.assertTrue(AlphaWerewolfLoot.firstKill(bystander), "The bystander won Hunter's Moon");
			all.forEach(ServerPlayer::discard);
		});
		helper.succeed();
	}
}

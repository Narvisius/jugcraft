package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.menagerie.Menagerie;
import io.github.jimbozoomer.jugcraft.menagerie.MenagerieAnimal;
import io.github.jimbozoomer.jugcraft.pets.PetBehaviors;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The pet roster's behaviours ({@link PetBehaviors}): level bonuses, STAY through native sitting and the hold goal,
 * PASSIVE and DEFEND targeting, catching up, and the fluid rule for water-bound pets.
 */
public class PetBehaviorsGameTests {
	@GameTest(maxTicks = 20)
	public void levelBonusesAreModestBoundedAndNeverStack(GameTestHelper h) {
		h.assertTrue(PetBehaviors.level(0) == 1 && PetBehaviors.level(99) == 20, "Levels are 1 to 20");
		h.assertTrue(PetBehaviors.vitality(1) == 0 && PetBehaviors.strength(1) == 0 && PetBehaviors.pace(1) == 1.0,
				"A level-1 pet is its species as it is");
		for (int level = 2; level <= 20; level++) {
			h.assertTrue(PetBehaviors.vitality(level) > PetBehaviors.vitality(level - 1) && PetBehaviors.strength(level) > PetBehaviors.strength(level - 1)
					&& PetBehaviors.recovery(level) > PetBehaviors.recovery(level - 1), "Every level helps a little: " + level);
		}
		h.assertTrue(PetBehaviors.vitality(20) <= 0.4 && PetBehaviors.strength(20) <= 0.5 && PetBehaviors.recovery(20) < 0.3F
				&& PetBehaviors.guardRadius(20) == 12, "Level 20 is modest: +38% health, +47.5% damage, under 0.3 health a second");
		ServerPlayer owner = owner(h);
		Wolf wolf = wolf(h, owner, new BlockPos(2, 2, 2));
		double base = wolf.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
		for (int i = 0; i < 3; i++) {
			tick(owner, wolf, "FOLLOW", 20);
		}
		h.assertTrue(wolf.getAttribute(Attributes.MAX_HEALTH).getModifier(PetBehaviors.VITALITY).amount() == PetBehaviors.vitality(20)
				&& Math.abs(wolf.getMaxHealth() - base * 1.38) < 0.01, "Ticking three times applies one vitality bonus: " + wolf.getMaxHealth());
		h.assertTrue(wolf.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(PetBehaviors.STRENGTH).amount() == PetBehaviors.strength(20),
				"and one strength bonus");
		tick(owner, wolf, "FOLLOW", 5);
		h.assertTrue(wolf.getAttribute(Attributes.MAX_HEALTH).getModifier(PetBehaviors.VITALITY).amount() == PetBehaviors.vitality(5),
				"A level change replaces the bonus");
		wolf.setHealth(wolf.getMaxHealth());
		PetBehaviors.clear(wolf);
		h.assertTrue(wolf.getAttribute(Attributes.MAX_HEALTH).getModifier(PetBehaviors.VITALITY) == null
				&& wolf.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(PetBehaviors.STRENGTH) == null
				&& wolf.getHealth() <= wolf.getMaxHealth() && !PetBehaviors.tracking(wolf),
				"Clearing removes the bonuses, keeps health within the species' own maximum, and forgets the pet");
		wolf.discard();
		h.succeed();
	}

	@GameTest(maxTicks = 20)
	public void stayUsesTheNativeSitAndOtherModesOnlyStandUpWhatStaySat(GameTestHelper h) {
		ServerPlayer owner = owner(h);
		Wolf wolf = wolf(h, owner, new BlockPos(2, 2, 2));
		tick(owner, wolf, "STAY", 1);
		h.assertTrue(wolf.isOrderedToSit() && wolf.entityTags().contains(PetBehaviors.SAT_FOR_STAY), "STAY sits a wolf by its own rules");
		tick(owner, wolf, "follow", 1);
		h.assertTrue(!wolf.isOrderedToSit() && !wolf.entityTags().contains(PetBehaviors.SAT_FOR_STAY), "FOLLOW (any case) stands it up again");
		wolf.setOrderedToSit(true);
		tick(owner, wolf, "FOLLOW", 1);
		h.assertTrue(wolf.isOrderedToSit(), "A wolf its owner sat down by hand stays sitting");
		wolf.setOrderedToSit(false);
		tick(owner, wolf, "STAY", 1);
		PetBehaviors.clear(wolf);
		h.assertTrue(!wolf.isOrderedToSit() && !wolf.entityTags().contains(PetBehaviors.SAT_FOR_STAY), "Clearing (before a recall) stands it up");
		// Stored while staying and summoned in another mode: the saved tag still stands it up.
		wolf.setOrderedToSit(true);
		wolf.addTag(PetBehaviors.SAT_FOR_STAY);
		tick(owner, wolf, "DEFEND", 1);
		h.assertTrue(!wolf.isOrderedToSit(), "A pet summoned out of STAY is not left sitting");
		PetBehaviors.clear(wolf);
		wolf.discard();
		h.succeed();
	}

	@GameTest(maxTicks = 200, padding = 16)
	public void stayHoldsAPetThatCannotSitBetweenTicksAndClearRemovesTheGoals(GameTestHelper h) {
		floor(h, 10, 6);
		ServerPlayer owner = owner(h);
		AbstractHorse horse = EntityTypes.HORSE.create(h.getLevel(), EntitySpawnReason.COMMAND);
		BlockPos anchor = h.absolutePos(new BlockPos(2, 2, 2));
		horse.snapTo(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5, 0, 0);
		horse.tameWithName(owner);
		h.getLevel().addFreshEntity(horse);
		for (int i = 0; i < 3; i++) {
			tick(owner, horse, "STAY", 1);
		}
		h.assertTrue(commands(horse) == 2, "Two command goals, however often it is ticked: " + commands(horse));
		horse.snapTo(anchor.getX() + 4.5, anchor.getY(), anchor.getZ() + 0.5, 0, 0);
		for (int t = 20; t <= 160; t += 20) {
			h.runAfterDelay(t, () -> tick(owner, horse, "STAY", 1));
		}
		h.runAfterDelay(170, () -> {
			h.assertTrue(horse.distanceToSqr(Vec3.atBottomCenterOf(anchor)) <= 2.5 * 2.5,
					"A pushed horse is walked back to where it was told to stay: " + horse.position());
			PetBehaviors.clear(horse);
			h.assertTrue(commands(horse) == 0, "Clearing removes the command goals");
			horse.discard();
			h.succeed();
		});
	}

	@GameTest(maxTicks = 40, padding = 16)
	public void passivePetsNeverStrike(GameTestHelper h) {
		floor(h, 8, 8);
		ServerPlayer owner = owner(h);
		Wolf wolf = wolf(h, owner, new BlockPos(2, 2, 2));
		Zombie zombie = zombie(h, new BlockPos(3, 2, 2));
		float health = zombie.getHealth();
		tick(owner, wolf, "PASSIVE", 20);
		wolf.setTarget(zombie);   // as a native goal would, between the loop's ticks
		h.runAfterDelay(15, () -> {
			h.assertTrue(wolf.getTarget() == null && zombie.getHealth() == health,
					"A PASSIVE pet drops a target before it can strike: target " + wolf.getTarget() + ", zombie at " + zombie.getHealth());
			PetBehaviors.clear(wolf);
			wolf.discard();
			zombie.discard();
			h.succeed();
		});
	}

	@GameTest(maxTicks = 20, padding = 16)
	public void defendersTurnOnMonstersButNeverOnPlayersOrPets(GameTestHelper h) {
		floor(h, 8, 8);
		ServerPlayer owner = owner(h);
		ServerPlayer stranger = h.makeMockServerPlayerInLevel();
		Wolf wolf = wolf(h, owner, new BlockPos(2, 2, 2));
		Wolf theirs = wolf(h, stranger, new BlockPos(5, 2, 5));
		Zombie zombie = zombie(h, new BlockPos(4, 2, 2));
		h.assertTrue(!PetBehaviors.mayFight(owner, wolf, stranger, 8) && !PetBehaviors.mayFight(owner, wolf, theirs, 8)
				&& !PetBehaviors.mayFight(owner, wolf, owner, 8) && PetBehaviors.mayFight(owner, wolf, zombie, 8),
				"Never a player, anyone's pet or the owner; a monster near them, yes");
		owner.setLastHurtByMob(stranger);
		tick(owner, wolf, "DEFEND", 1);
		h.assertTrue(wolf.getTarget() == null, "A player who hits the owner is never a pet's target");
		zombie.setTarget(owner);
		tick(owner, wolf, "DEFEND", 1);
		h.assertTrue(wolf.getTarget() == zombie, "A monster stalking the owner is; target="+zombie.getTarget()+", wolfDamage="+wolf.getAttributeValue(Attributes.ATTACK_DAMAGE)+", nearby="+owner.level().getEntitiesOfClass(Mob.class,owner.getBoundingBox().inflate(8),m->m==zombie).size());
		tick(owner, wolf, "FOLLOW", 1);
		h.assertTrue(wolf.getTarget() == null, "Leaving DEFEND drops the target DEFEND gave");
		PetBehaviors.clear(wolf);
		for (Mob mob : new Mob[] {wolf, theirs, zombie}) {
			mob.discard();
		}
		h.succeed();
	}

	@GameTest(maxTicks = 20, padding = 40)
	public void landPetsCatchUpButWaterPetsNeverLandOnLand(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		floor(h, 34, 7);
		// A walled pool at the far end, the owner on dry stone at the other.
		for (int x = -1; x <= 3; x++) {
			for (int z = -1; z <= 3; z++) {
				boolean inside = x >= 0 && x <= 2 && z >= 0 && z <= 2;
				h.setBlock(new BlockPos(x + 1, 2, z + 1), inside ? Blocks.WATER : Blocks.STONE);
				h.setBlock(new BlockPos(x + 1, 3, z + 1), inside ? Blocks.WATER : Blocks.STONE);
			}
		}
		ServerPlayer owner = owner(h);
		BlockPos stand = h.absolutePos(new BlockPos(32, 2, 3));
		owner.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
		Wolf wolf = wolf(h, owner, new BlockPos(2, 2, 6));
		Menagerie.Species species = Menagerie.SPECIES.values().stream()
				.filter(s -> s.aquatic() && !s.habitat().equals("nether") && Menagerie.TYPES.containsKey(s.id())).findFirst().orElseThrow();
		MenagerieAnimal fish = Menagerie.TYPES.get(species.id()).create(level, EntitySpawnReason.COMMAND);
		BlockPos pool = h.absolutePos(new BlockPos(2, 2, 2));
		fish.snapTo(pool.getX() + 0.5, pool.getY(), pool.getZ() + 0.5, 0, 0);
		fish.tame(owner.getUUID());
		level.addFreshEntity(fish);
		h.assertTrue(PetBehaviors.Movement.of(fish) == PetBehaviors.Movement.FLUID && PetBehaviors.Movement.of(wolf) == PetBehaviors.Movement.GROUND,
				"Movement is read from each pet's own navigation");
		for (int i = 0; i < PetBehaviors.CATCH_UP_AFTER + 1; i++) {
			tick(owner, wolf, "FOLLOW", 1);
			tick(owner, fish, "FOLLOW", 1);
		}
		h.assertTrue(wolf.distanceTo(owner) <= 6, "A land pet left far behind catches up beside its owner: " + wolf.distanceTo(owner));
		h.assertTrue(!level.getFluidState(fish.blockPosition()).isEmpty() && fish.distanceTo(owner) > PetBehaviors.CATCH_UP,
				"A water pet is never moved onto land: " + fish.position() + ", in water " + fish.isInWater());
		PetBehaviors.clear(wolf);
		PetBehaviors.clear(fish);
		wolf.discard();
		fish.discard();
		h.succeed();
	}

	@GameTest(maxTicks = 160, padding = 16)
	public void petsRecoverOnlyOnceCalm(GameTestHelper h) {
		floor(h, 6, 6);
		ServerPlayer owner = owner(h);
		Wolf wolf = wolf(h, owner, new BlockPos(2, 2, 2));
		wolf.setNoAi(true);
		tick(owner, wolf, "STAY", 20);
		wolf.setHealth(wolf.getMaxHealth() / 2);
		float hurt = wolf.getHealth();
		tick(owner, wolf, "STAY", 20);
		h.assertTrue(wolf.getHealth() == hurt, "Just hurt, it does not recover yet");
		h.runAfterDelay(PetBehaviors.CALM_TICKS + 5, () -> {
			tick(owner, wolf, "STAY", 20);
			h.assertTrue(wolf.getHealth() > hurt && wolf.getHealth() <= hurt + PetBehaviors.recovery(20)*2 + .001,
					"Calm, it recovers a little each second: " + hurt + " -> " + wolf.getHealth());
			PetBehaviors.clear(wolf);
			wolf.discard();
			h.succeed();
		});
	}

	// ---------------------------------------------------------------- helpers
    private static void tick(ServerPlayer owner,Mob pet,String mode,int level){
        var entry=io.github.jimbozoomer.jugcraft.pets.Pets.enroll(pet);
        if(entry!=null){entry.putString("mode",mode.toUpperCase(java.util.Locale.ROOT));entry.putInt("xp",(PetBehaviors.level(level)-1)*120);}
        PetBehaviors.tick(owner,pet,mode,level);
    }


	/** An owner standing in the arena (the mock player's own spawn point is not guaranteed to be near it). */
	private static ServerPlayer owner(GameTestHelper h) {
		ServerPlayer owner = h.makeMockServerPlayerInLevel();
		owner.setGameMode(GameType.SURVIVAL);
		BlockPos at = h.absolutePos(new BlockPos(1, 2, 1));
		owner.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		return owner;
	}

	private static Wolf wolf(GameTestHelper h, ServerPlayer owner, BlockPos at) {
		Wolf wolf = EntityTypes.WOLF.create(h.getLevel(), EntitySpawnReason.COMMAND);
		BlockPos pos = h.absolutePos(at);
		wolf.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		wolf.tame(owner);
		h.getLevel().addFreshEntity(wolf);
		return wolf;
	}

	private static Zombie zombie(GameTestHelper h, BlockPos at) {
		// Vanilla GameTestHelper's mock player hardcodes Creative even after setGameMode.
        // This test-only monster accepts that mock as a threat; production targeting is untouched.
        Zombie zombie = new Zombie(EntityTypes.ZOMBIE,h.getLevel()) {
            @Override protected net.minecraft.world.entity.LivingEntity asValidTarget(net.minecraft.world.entity.LivingEntity target){return target;}
        };
		BlockPos pos = h.absolutePos(at);
		zombie.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		zombie.setNoAi(true);
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
		zombie.setPersistenceRequired();
		h.getLevel().addFreshEntity(zombie);
		return zombie;
	}

	private static void floor(GameTestHelper h, int width, int depth) {
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < depth; z++) {
				h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** How many of PetBehaviors' command goals the pet carries. */
	private static long commands(Mob pet) {
		String prefix = PetBehaviors.class.getName() + "$";
		return pet.getGoalSelector().getAvailableGoals().stream().filter(w -> w.getGoal().getClass().getName().startsWith(prefix)).count();
	}
}

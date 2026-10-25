package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import io.github.jimbozoomer.jugcraft.menagerie.Menagerie;
import io.github.jimbozoomer.jugcraft.menagerie.MenagerieAnimal;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauEncounters;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauSummons;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real damage paths with simulated survival players, not a two-account network playtest. */
public final class ClaimCombatGameTests {
    private static final String ARENA = "jugcraft-test:claim_combat_arena";

    @GameTest(structure = ARENA)
    public void visitorCanMeleeVanillaAndJugcraftHostiles(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            f.visitor.snapTo(Vec3.atCenterOf(f.target.west()));
            h.assertFalse(LandRegistry.mayBuild(f.visitor, h.getLevel(), f.visitor.blockPosition()), "Visitor stands inside another player's claim");
            for (var type : List.of(EntityTypes.ZOMBIE, EntityTypes.CREEPER, EntityTypes.SLIME,
                    NouveauEncounters.TYPES.get("wilden_stalker"), Menagerie.TYPES.get("beast_centipede_head"))) {
                var mob = f.mob(type);
                h.assertTrue(mob instanceof net.minecraft.world.entity.monster.Enemy, "Fixture is a hostile mob");
                mob.setCustomName(net.minecraft.network.chat.Component.literal("Named wild hostile"));
                h.assertTrue(attack(f.visitor, mob) == InteractionResult.PASS, "Claim permits attacking " + type);
                float before = mob.getHealth();
                f.visitor.attack(mob);
                h.assertTrue(mob.getHealth() < before, "Actual melee damages " + type);
                mob.discard();
            }
            var evoker = f.mob(EntityTypes.EVOKER);
            var vex = f.mob(EntityTypes.VEX);
            vex.setOwner(evoker);
            h.assertTrue(attack(f.visitor, vex) == InteractionResult.PASS, "An evoker's vex is a hostile, not a player pet");
            evoker.discard();
            h.assertTrue(vex.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(f.visitor), 1),
                    "Vex stays fightable after its evoker is removed");
        }
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void multipartHostilesUseTheirParentsOwnership(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var parent = (MenagerieAnimal)f.mob(Menagerie.TYPES.get("beast_centipede_head"));
            parent.tick();
            var parts = h.getLevel().getEntitiesOfClass(io.github.jimbozoomer.jugcraft.menagerie.RecoveredCreaturePart.class,
                    parent.getBoundingBox().inflate(12), part -> part.parent() == parent);
            h.assertTrue(!parts.isEmpty(), "Hostile creates real multipart hitboxes");
            var part = parts.getFirst();
            h.assertTrue(attack(f.visitor, part) == InteractionResult.PASS, "Visitor can strike hostile body segment");
            float before = parent.getHealth();
            h.assertTrue(part.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(f.visitor), 1)
                    && parent.getHealth() < before, "Segment hit damages its hostile parent");
            parent.tame(UUID.randomUUID());
            parent.damageCooldownTime = 0;
            before = parent.getHealth();
            part.snapTo(Vec3.atCenterOf(f.outside));
            h.assertTrue(LandRegistry.mayBuild(f.visitor, h.getLevel(), part.blockPosition())
                    && !LandRegistry.mayBuild(f.visitor, h.getLevel(), parent.blockPosition()),
                    "Body segment crosses the border while its owned parent remains in the claim");
            h.assertTrue(attack(f.visitor, part) == InteractionResult.FAIL, "Owned parent also protects its body segments");
            h.assertFalse(part.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(f.visitor), 1),
                    "Direct segment damage cannot bypass pet protection");
            h.assertTrue(parent.getHealth() == before, "Owned multipart creature loses no health");
        }
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void visitorProjectilesBulletsAndMagicDamageHostilesAcrossClaimBorder(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var mob = f.mob(EntityTypes.HUSK);
            var level = h.getLevel();
            var arrow = new Arrow(level, f.visitor, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            f.entities.add(arrow);
            arrow.setNoGravity(true);
            arrow.snapTo(f.visitor.getX(), mob.getY() + .8, f.visitor.getZ(), 0, 0);
            var direction = new Vec3(mob.getX(), mob.getY() + .8, mob.getZ()).subtract(arrow.position()).normalize();
            arrow.setDeltaMovement(direction.scale(1.5));
            level.addFreshEntity(arrow);
            float before = mob.getHealth();
            for (int i = 0; i < 8 && !arrow.isRemoved(); i++) arrow.tick();
            h.assertTrue(mob.getHealth() < before, "Actual player-fired arrow crosses border and damages hostile");
            arrow.discard();

            mob.damageCooldownTime = 0;
            before = mob.getHealth();
            h.assertTrue(mob.hurtServer(level, level.damageSources().indirectMagic(f.visitor, f.visitor), 1)
                    && mob.getHealth() < before, "Player-attributed magic damages hostile");
            mob.damageCooldownTime = 0;
            before = mob.getHealth();
            var aim = mob.getBoundingBox().getCenter().subtract(f.visitor.getEyePosition());
            f.visitor.setYRot((float)Math.toDegrees(Math.atan2(-aim.x, aim.z)));
            f.visitor.setYHeadRot(f.visitor.getYRot());
            f.visitor.setXRot((float)Math.toDegrees(Math.atan2(-aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
            var gun = new ItemStack(JugcraftGuns.GUNS.get("patchwork_carbine"));
            GunItem.setLoaded(gun, 10);
            f.visitor.setItemInHand(InteractionHand.MAIN_HAND, gun);
            h.assertTrue(GunShots.fire(f.visitor) && mob.getHealth() < before, "Actual bullet damages hostile");
        }
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void livestockPetsAndOwnedMonstersStayProtected(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var wolf = f.mob(EntityTypes.WOLF);
            wolf.tame(f.owner);
            var ownedMonster = (MenagerieAnimal)f.mob(Menagerie.TYPES.get("beast_centipede_head"));
            ownedMonster.tame(f.owner.getUUID());
            for (var mob : List.of(f.mob(EntityTypes.COW), f.mob(EntityTypes.VILLAGER), wolf, ownedMonster)) {
                assertProtected(h, f, mob);
            }
            // Ownership must still protect a monster when its keeper is offline.
            ownedMonster.tame(UUID.randomUUID());
            assertProtected(h, f, ownedMonster);
            var stand = f.mob(EntityTypes.ARMOR_STAND);
            assertProtected(h, f, stand);

            var cow = f.mob(EntityTypes.COW);
            h.assertTrue(attack(f.owner, cow) == InteractionResult.PASS, "Owner retains livestock access");
            h.assertTrue(cow.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(f.owner), 1), "Owner damage works");
            cow.snapTo(Vec3.atCenterOf(f.outside));
            cow.damageCooldownTime = 0;
            h.assertTrue(attack(f.visitor, cow) == InteractionResult.PASS
                    && cow.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(f.visitor), 1),
                    "Unclaimed livestock keeps ordinary combat rules");
        }
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void summonsAndNonCombatClaimPermissionsStayProtected(GameTestHelper h) {
        for (String kind : List.of("summon_undead", "summon_vex")) {
          try (var f = new Fixture(h)) {
            h.assertTrue(NouveauSummons.summon(h.getLevel(), f.owner, f.target, kind, 1, 200, 0, 0),
                    "Owner creates an actual player summon in their claim: " + kind);
            var summons = h.getLevel().getEntitiesOfClass(Mob.class,
                    new net.minecraft.world.phys.AABB(f.target).inflate(4), NouveauSummons::owned);
            f.entities.addAll(summons);
            h.assertTrue(!summons.isEmpty(), "Summon fixture is present");
            for (var summon : summons) assertProtected(h, f, summon);

            var hostile = f.mob(EntityTypes.ZOMBIE);
            h.assertTrue(attack(f.visitor, hostile) == InteractionResult.PASS, "Visitor can fight a wild hostile");
            h.assertTrue(UseEntityCallback.EVENT.invoker().interact(f.visitor, h.getLevel(), InteractionHand.MAIN_HAND, hostile, null)
                    == InteractionResult.FAIL, "Combat permission does not grant entity interaction");
            h.assertFalse(LandRegistry.mayBuild(f.visitor, h.getLevel(), f.target), "Combat does not grant building permission");
            h.assertTrue(attack(f.visitor, f.owner) == InteractionResult.PASS, "Claim does not replace existing PvP policy");
          }
        }
        h.succeed();
    }

    private static void assertProtected(GameTestHelper h, Fixture f, LivingEntity target) {
        h.assertTrue(attack(f.visitor, target) == InteractionResult.FAIL, "Visitor melee remains blocked for " + target.getType());
        float before = target.getHealth();
        h.assertFalse(target.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(f.visitor), 1), "Direct damage remains blocked");
        var arrow = new Arrow(h.getLevel(), f.visitor, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        h.assertFalse(target.hurtServer(h.getLevel(), h.getLevel().damageSources().arrow(arrow, f.visitor), 1), "Projectile damage remains blocked");
        h.assertTrue(target.getHealth() == before, "Protected target loses no health");
    }

    private static InteractionResult attack(ServerPlayer player, Entity target) {
        return AttackEntityCallback.EVENT.invoker().interact(player, player.level(), InteractionHand.MAIN_HAND, target, null);
    }

    private static ServerPlayer player(GameTestHelper h) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "claim-combat");
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, net.minecraft.server.level.ClientInformation.createDefault());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false));
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        h.assertFalse(p.isCreative(), "Survival player fixture");
        return p;
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final ServerPlayer owner, visitor;
        final BlockPos outside, target;
        final LandRegistry land;
        final FinanceBook book;
        final long key;
        final List<Entity> entities = new ArrayList<>();

        Fixture(GameTestHelper h) {
            this.h = h;
            owner = player(h);
            visitor = player(h);
            // The 33-wide test arena guarantees both sides of this chunk border are active.
            var base = h.absolutePos(new BlockPos(0, 3, 8));
            outside = new BlockPos(Math.floorDiv(base.getX(), 16) * 16 + 15, base.getY(), base.getZ());
            target = outside.east(3);
            h.assertTrue(h.getLevel().areEntitiesActuallyLoadedAndTicking(new net.minecraft.world.level.ChunkPos(target.getX() >> 4, target.getZ() >> 4)),
                    "Claimed combat area has active entities");
            land = LandRegistry.get(h.getLevel().getServer());
            book = Ledger.get(h.getLevel().getServer()).book();
            key = LandRegistry.keyOf(target);
            land.assign(book, key, book.account(owner.getUUID(), "CombatOwner"), 0);
            for (int x = -1; x <= 6; x++) for (int y = 0; y <= 4; y++) for (int z = -2; z <= 2; z++)
                h.getLevel().setBlockAndUpdate(outside.offset(x, y, z), Blocks.AIR.defaultBlockState());
            owner.snapTo(Vec3.atCenterOf(target.south(2)));
            visitor.snapTo(Vec3.atCenterOf(outside));
            h.assertFalse(LandRegistry.mayBuild(visitor, h.getLevel(), target), "Visitor has no claim permission");
            h.assertTrue(LandRegistry.mayBuild(visitor, h.getLevel(), outside), "Shooter starts outside the claim");
        }

        <T extends LivingEntity> T mob(EntityType<T> type) {
            T mob = type.create(h.getLevel(), EntitySpawnReason.TRIGGERED);
            h.assertTrue(mob != null, "Create mob fixture");
            if (mob instanceof Mob creature) creature.setNoAi(true);
            mob.snapTo(Vec3.atCenterOf(target));
            h.getLevel().addFreshEntity(mob);
            h.assertTrue(h.getLevel().getEntity(mob.getId()) == mob, "Mob is visible to actual projectile and gun queries");
            entities.add(mob);
            return mob;
        }

        @Override public void close() {
            entities.forEach(Entity::discard);
            land.abandon(owner, book, key, true, 0);
            for (var p : List.of(owner, visitor)) {
                h.getLevel().getServer().getPlayerList().remove(p);
                p.discard();
            }
        }
    }
}

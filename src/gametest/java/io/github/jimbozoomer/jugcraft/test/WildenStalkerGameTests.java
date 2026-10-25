package io.github.jimbozoomer.jugcraft.test;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauBeast;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauEncounters;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauSpine;
import java.lang.reflect.Field;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Deterministic server AI steps, real world visibility, projectile ticks and leap movement. */
public class WildenStalkerGameTests {
    private static final String ARENA = "jugcraft-test:arms_arena";

    private static final class Stalker extends NouveauBeast {
        Stalker(ServerLevel level) { super(NouveauEncounters.TYPES.get("wilden_stalker"), level, "wilden_stalker"); }
        void think(int ticks) { for (int i = 0; i < ticks; i++) super.customServerAiStep((ServerLevel) level()); }
        void moveWithoutGoals() {
            goalSelector.removeAllGoals(goal -> true);
            targetSelector.removeAllGoals(goal -> true);
            setNoAi(false); // Vanilla also disables travel with no-AI; retain movement while freezing ordinary goals.
        }
    }

    @GameTest(structure = ARENA)
    public void solidCoverPreventsWindup(GameTestHelper h) {
        var player = player(h, 12);
        var stalker = stalker(h, player);
        wall(h, true);
        h.assertTrue(!stalker.hasLineOfSight(player), "Wall actually blocks eye-to-eye visibility");
        stalker.think(65);
        h.assertTrue(clock(stalker) == 0, "Hidden player must not start the attack windup");
        h.assertTrue(player.getHealth() == 20, "Hidden player is unharmed");
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void distantPlayerCannotStartWindup(GameTestHelper h) {
        var player = player(h, 16);
        var stalker = stalker(h, player);
        h.assertTrue(stalker.distanceToSqr(player) >= 196, "Player is outside the existing fourteen-block range");
        stalker.think(65);
        h.assertTrue(clock(stalker) == 0, "Out-of-range player must not start the attack windup");
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void coverDuringWindupCancelsAndRequiresFreshWindup(GameTestHelper h) {
        var player = player(h, 12);
        var stalker = stalker(h, player);
        stalker.think(65);
        h.assertTrue(clock(stalker) == 65, "Visible player starts a windup");
        wall(h, true);
        stalker.think(1);
        h.assertTrue(clock(stalker) == 0, "Losing sight cancels the pending attack");
        wall(h, false);
        stalker.think(15);
        h.assertTrue(spikes(h, stalker).isEmpty() && player.getHealth() == 20, "Reappearing cannot release the cancelled attack");
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void launchDoesNotDamageAndDodgedProjectileMisses(GameTestHelper h) {
        var player = player(h, 12);
        var stalker = stalker(h, player);
        stalker.think(80);
        h.assertTrue(player.getHealth() == 20, "Launching the leap and spike deals no automatic damage");
        var spike = onlySpike(h, stalker);
        player.setPos(player.position().add(0, 0, 5));
        fly(spike, 41);
        h.assertTrue(player.getHealth() == 20 && spike.isRemoved(), "Dodged spike expires without harming the player");
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void wallStopsReleasedSpike(GameTestHelper h) {
        var player = player(h, 12);
        var stalker = stalker(h, player);
        stalker.think(80);
        var spike = onlySpike(h, stalker);
        wall(h, true);
        fly(spike, 30);
        h.assertTrue(spike.isRemoved() && player.getHealth() == 20, "Solid cover blocks the released projectile and all damage");
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void spikeHitStillDamagesPlayer(GameTestHelper h) {
        var player = player(h, 12);
        var stalker = stalker(h, player);
        stalker.think(80);
        var spike = onlySpike(h, stalker);
        fly(spike, 30);
        h.assertTrue(spike.isRemoved() && player.getHealth() == 16, "Actual spike impact deals its existing four damage; health=" + player.getHealth());
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void leapContactHitsOnlyOnce(GameTestHelper h) {
        var player = player(h, 4);
        var stalker = stalker(h, player);
        stalker.think(80);
        onlySpike(h, stalker).discard(); // Isolate the leap from projectile damage.
        stalker.moveWithoutGoals();
        h.assertTrue(player.getHealth() == 20, "Leap launch cannot hurt a player before contact");
        boolean contact = false;
        for (int tick = 0; tick < 8 && player.getHealth() == 20; tick++) {
            stalker.tick();
            if (player.getHealth() < 20) contact = stalker.getBoundingBox().intersects(player.getBoundingBox());
        }
        h.assertTrue(contact && player.getHealth() == 17, "Real leap movement must contact the player before dealing three damage; health=" + player.getHealth());
        stalker.setDeltaMovement(Vec3.ZERO);
        player.setInvulnerableTime(0);
        stalker.tick();
        h.assertTrue(player.getHealth() == 17, "One leap cannot repeatedly damage its target");
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void changingTargetCancelsWindup(GameTestHelper h) {
        var player = player(h, 12);
        var stalker = stalker(h, player);
        stalker.think(65);
        stalker.setTarget(player(h, 10));
        stalker.think(1);
        h.assertTrue(clock(stalker) == 0 && spikes(h, stalker).isEmpty(), "New target must receive a fresh windup");
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void solidCoverStopsLeapContact(GameTestHelper h) {
        var player = player(h, 4);
        var stalker = stalker(h, player);
        stalker.think(80);
        onlySpike(h, stalker).discard();
        stalker.moveWithoutGoals();
        for (int y = 1; y <= 6; y++) for (int z = 1; z <= 5; z++) h.setBlock(new BlockPos(3, y, z), Blocks.STONE);
        for (int tick = 0; tick < 10; tick++) stalker.tick();
        h.assertTrue(player.getHealth() == 20, "Leap colliding with a wall cannot hurt the player behind it");
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void missedLeapExpiresWithoutAStoredHit(GameTestHelper h) {
        var player = player(h, 12);
        var stalker = stalker(h, player);
        stalker.think(80);
        onlySpike(h, stalker).discard();
        stalker.moveWithoutGoals();
        player.setPos(player.position().add(0, 0, 5));
        for (int tick = 0; tick < 45; tick++) stalker.tick();
        h.assertTrue(player.getHealth() == 20, "A missed leap causes no damage");
        stalker.setPos(player.position());
        stalker.setDeltaMovement(Vec3.ZERO);
        stalker.tick();
        h.assertTrue(player.getHealth() == 20, "Expired leap cannot deal a delayed contact hit");
        h.succeed();
    }

    private static ServerPlayer player(GameTestHelper h, int x) {
        // The deprecated vanilla helper overrides gameMode() to always return CREATIVE.
        var cookie = CommonListenerCookie.createInitial(new GameProfile(java.util.UUID.randomUUID(), "stalker-test"), false);
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        h.assertTrue(p.connection.hasClientLoaded(), "Fixture completed the loading handshake and is damageable");
        var pos = h.absolutePos(new BlockPos(x, 2, 3));
        p.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        return p;
    }

    private static Stalker stalker(GameTestHelper h, ServerPlayer player) {
        var stalker = new Stalker(h.getLevel());
        var pos = h.absolutePos(new BlockPos(2, 2, 3));
        stalker.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        stalker.setNoAi(true); // Freeze vanilla goals, manually step the production special-attack AI.
        stalker.setNoGravity(true);
        stalker.setPersistenceRequired();
        h.getLevel().addFreshEntity(stalker);
        stalker.setTarget(player);
        h.assertTrue(stalker.getTarget() == player, "Fixture has a targetable survival player");
        return stalker;
    }

    private static void wall(GameTestHelper h, boolean solid) {
        for (int y = 1; y <= 6; y++) for (int z = 1; z <= 5; z++)
            h.setBlock(new BlockPos(7, y, z), solid ? Blocks.STONE : Blocks.AIR);
    }

    private static java.util.List<NouveauSpine> spikes(GameTestHelper h, Stalker stalker) {
        return h.getLevel().getEntitiesOfClass(NouveauSpine.class, stalker.getBoundingBox().inflate(2), e -> !e.isRemoved());
    }

    private static NouveauSpine onlySpike(GameTestHelper h, Stalker stalker) {
        var spikes = spikes(h, stalker);
        h.assertTrue(spikes.size() == 1, "Exactly one spike launches");
        return spikes.getFirst();
    }

    private static void fly(NouveauSpine spike, int ticks) {
        for (int tick = 0; tick < ticks && !spike.isRemoved(); tick++) { spike.tickCount++; spike.tick(); }
    }

    private static int clock(Stalker stalker) {
        try {
            Field field = NouveauBeast.class.getDeclaredField("attackClock");
            field.setAccessible(true);
            return field.getInt(stalker);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
}

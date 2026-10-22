package io.github.jimbozoomer.jugcraft.server;

import io.github.jimbozoomer.jugcraft.town.Townsfolk;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.npc.villager.AbstractVillager;

/** Changes the sun's clock, never the simulation tick rate. */
public final class ViewerWorldRules {
    private ViewerWorldRules() {}

    public static float daylightRate(long time) {
        return Math.floorMod(time, 24000L) < 12000 ? 0.5F : 1.5F;
    }

    public static void updateClock(MinecraftServer server) {
        var clock = server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
        var manager = server.clockManager();
        float desired = daylightRate(manager.getInstance(clock).totalTicks());
        // Native clock packets interpolate the sun smoothly and preserve pause, sleep and /time.
        if (manager.getInstance(clock).rate() != desired) manager.setRate(clock, desired);
    }

    public static boolean protectedResident(LivingEntity target) {
        return target instanceof AbstractVillager || target instanceof Townsfolk;
    }

    public static boolean blocksAttack(LivingEntity attacker, LivingEntity target) {
        if (attacker instanceof io.github.jimbozoomer.jugcraft.raiders.Raider)
            return !io.github.jimbozoomer.jugcraft.raiders.RaiderTargets.allowed(target);
        return attacker instanceof Mob && !(attacker instanceof Pillager) && protectedResident(target);
    }

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(ViewerWorldRules::updateClock);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((target, source, amount) ->
                !(source.getEntity() instanceof LivingEntity attacker && blocksAttack(attacker, target)));
    }
}

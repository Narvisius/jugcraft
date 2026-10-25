package io.github.jimbozoomer.jugcraft.grave;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.server.PlayerRules;
import java.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.PushReaction;

/** Captures only native death drops, after vanishing/totem/keep rules have made their decisions. */
public final class DeathGraves {
    public static GraveMarkerBlock MARKER;
    private static final ThreadLocal<Capture> ACTIVE = new ThreadLocal<>();
    private record Capture(ServerPlayer player, UUID id, GlobalPos location, boolean bossLair) {}
    private DeathGraves() {}

    public static boolean eligible(ServerPlayer player) {
        return !player.isSpectator() && !player.level().getGameRules().get(GameRules.KEEP_INVENTORY)
            && !PlayerRules.keeps(player) && !Lairs.isLair(player.level());
    }

    public static void duringDeath(ServerPlayer player, Runnable nativeDrops) {
        boolean bossLair = Lairs.isLair(player.level());
        if (player.isSpectator() || player.level().getGameRules().get(GameRules.KEEP_INVENTORY)
            || PlayerRules.keeps(player)) { nativeDrops.run(); return; }
        Capture previous = ACTIVE.get();
        var capture = new Capture(player, UUID.randomUUID(), GlobalPos.of(player.level().dimension(),
            bossLair ? player.blockPosition() : findPosition(player)), bossLair);
        ACTIVE.set(capture);
        try { nativeDrops.run(); }
        finally {
            ACTIVE.set(previous);
            var store = GraveStore.get(player.level().getServer());
            var grave = store.get(capture.id());
            if (grave != null) {
                BlockPos pos = grave.location().pos();
                var level = player.level();
                // Never replace a build or force-load a chunk. The recovery command also works if no marker fits.
                if (canPlaceTerrain(player, pos) && store.at(grave.location()).id().equals(grave.id())
                    && level.setBlockAndUpdate(pos, MARKER.defaultBlockState()))
                    store.put(new GraveStore.Grave(grave.id(), grave.owner(), grave.location(), grave.items(), true));
                player.sendSystemMessage(Component.literal("Your belongings are safe in a grave at " + pos.toShortString()
                    + ". Use /jugcraft graves to find or recover them."));
            }
        }
    }

    /** True only while this exact player's native death-loot method is running. */
    public static boolean capture(net.minecraft.world.entity.LivingEntity entity, ItemStack stack) {
        Capture c = ACTIVE.get();
        if (c == null || c.player() != entity || stack.isEmpty()) return false;
        if (c.bossLair()) { Lairs.retainDeathItem(c.player(), stack); return true; }
        var store = GraveStore.get(c.player().level().getServer());
        var old = store.get(c.id());
        var items = new ArrayList<ItemStack>();
        if (old != null) items.addAll(old.items());
        items.add(stack.copy());
        store.put(new GraveStore.Grave(c.id(), c.player().getUUID(), c.location(), List.copyOf(items), false));
        return true;
    }

    private static boolean canPlace(ServerPlayer player, BlockPos pos) {
        var level = player.level();
        return canPlaceTerrain(player, pos) && GraveStore.get(level.getServer()).at(GlobalPos.of(level.dimension(), pos)) == null;
    }

    private static boolean canPlaceTerrain(ServerPlayer player, BlockPos pos) {
        var level = player.level();
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
            && level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
            && level.getBlockState(pos).isAir() && io.github.jimbozoomer.jugcraft.concordance.Authority.mayChange(level, player, pos);
    }

    private static BlockPos findPosition(ServerPlayer player) {
        var level = player.level();
        BlockPos origin = new BlockPos(player.getBlockX(), Math.clamp(player.getBlockY(), level.getMinY() + 1, level.getMaxY() - 1), player.getBlockZ());
        for (int y = 0; y < 8; y++) for (int r = 0; r <= 3; r++)
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                BlockPos pos = origin.offset(x, y, z);
                if (canPlace(player, pos)) return pos;
            }
        // Stored safely without overwriting terrain; recovery command is the fallback.
        return origin;
    }

    public static int recover(ServerPlayer player, UUID id) {
        if (!player.isAlive() || player.isSpectator()) return 0;
        var store = GraveStore.get(player.level().getServer());
        var grave = store.get(id);
        if (grave == null || !grave.owner().equals(player.getUUID())) return 0;
        ServerLevel level = player.level().getServer().getLevel(grave.location().dimension());
        BlockPos pos = grave.location().pos();
        // An unloaded marker must not be mistaken for a missing marker.
        if (grave.marked() && (level == null || level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null)) {
            player.sendSystemMessage(Component.literal("Travel to the grave's area before recovering it.")); return 0;
        }
        boolean marker = grave.marked() && level != null && level.getBlockState(pos).is(MARKER);
        if (marker && (level != player.level() || player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > 64)) {
            player.sendSystemMessage(Component.literal("Move within 8 blocks of your grave.")); return 0;
        }
        var remaining = new ArrayList<ItemStack>();
        int before = grave.count();
        for (var saved : grave.items()) {
            ItemStack item = saved.copy();
            player.getInventory().add(item);
            if (!item.isEmpty()) remaining.add(item);
        }
        if (remaining.isEmpty()) {
            store.remove(id);
            if (marker) level.removeBlock(pos, false);
        } else store.put(new GraveStore.Grave(id, grave.owner(), grave.location(), List.copyOf(remaining), grave.marked()));
        int restored = before - remaining.stream().mapToInt(ItemStack::getCount).sum();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("Recovered " + restored + " items. "
            + (remaining.isEmpty() ? "Grave emptied." : "Make room and return for the rest.")));
        return restored;
    }

    public static void register() {
        var id = Jugcraft.id("death_gravestone");
        MARKER = Registry.register(BuiltInRegistries.BLOCK, id, new GraveMarkerBlock(BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, id)).strength(-1, 3600000).noOcclusion().pushReaction(PushReaction.IMMOVEABLE)));
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(Commands.literal("jugcraft")
            .then(Commands.literal("graves").executes(c -> {
                var player = c.getSource().getPlayerOrException();
                var graves = GraveStore.get(c.getSource().getServer()).owned(player.getUUID());
                if (graves.isEmpty()) player.sendSystemMessage(Component.literal("You have no stored graves."));
                for (var g : graves) player.sendSystemMessage(Component.literal(g.location().dimension().identifier() + " "
                    + g.location().pos().toShortString() + " (" + g.count() + " items): /jugcraft graves recover " + g.id()));
                return graves.size();
            }).then(Commands.literal("recover").then(Commands.argument("grave", UuidArgument.uuid())
                .executes(c -> recover(c.getSource().getPlayerOrException(), UuidArgument.getUuid(c, "grave"))))))));
    }
}

package io.github.jimbozoomer.jugcraft.server;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import java.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.*;

/** UUID-based, world-persistent opt-ins. Never changes the world's keepInventory gamerule. */
public final class PlayerRules extends SavedData {
    public static final Codec<PlayerRules> CODEC = UUIDUtil.CODEC.listOf().optionalFieldOf("keep_inventory", List.of()).xmap(PlayerRules::new, r -> new ArrayList<>(r.keepInventory)).codec();
    private static final SavedDataType<PlayerRules> TYPE = new SavedDataType<>(Jugcraft.id("player_rules"), PlayerRules::new, CODEC, null);
    private final Set<UUID> keepInventory = new HashSet<>();
    public PlayerRules() {}
    private PlayerRules(List<UUID> players) { keepInventory.addAll(players); }
    public static PlayerRules get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(TYPE); }
    public boolean keeps(UUID id) { return keepInventory.contains(id); }
    public void set(UUID id, boolean enabled) { if (enabled ? keepInventory.add(id) : keepInventory.remove(id)) setDirty(); }
    /** Whether a death keeps everything, as keepInventory does: the player's own rule, or dying in a lair (a boss fight). */
    public static boolean keeps(Player player) {
        if (Lairs.keepsBelongings(player)) return true;
        var server = player.level().getServer();
        return server != null && get(server).keeps(player.getUUID());
    }
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(Commands.literal("jugcraft")
            .then(Commands.literal("keepinventory").requires(s -> Commands.LEVEL_GAMEMASTERS.check(s.permissions()))
                .then(Commands.argument("players", EntityArgument.players())
                    .executes(c -> {
                        var rules = get(c.getSource().getServer());
                        for (var p : EntityArgument.getPlayers(c, "players")) c.getSource().sendSuccess(() -> Component.literal(p.getName().getString() + ": personal keep-inventory " + (rules.keeps(p.getUUID()) ? "ON" : "OFF (uses server default)")), false);
                        return 1;
                    })
                    .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(c -> {
                        boolean enabled = BoolArgumentType.getBool(c, "enabled");
                        var players = EntityArgument.getPlayers(c, "players");
                        for (var p : players) get(c.getSource().getServer()).set(p.getUUID(), enabled);
                        c.getSource().sendSuccess(() -> Component.literal("Personal keep-inventory " + (enabled ? "enabled" : "disabled") + " for " + players.size() + " player(s). World gamerule unchanged."), true);
                        return players.size();
                    }))))));
        TreeFelling.register();
    }
}

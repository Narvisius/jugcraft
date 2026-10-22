package io.github.jimbozoomer.jugcraft.finance;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Small player-facing entry points into the existing land office; no parallel claim registry. */
final class ClaimCommands {
    private interface Action { FinanceBook.Result run(ServerPlayer player, LandRegistry land, FinanceBook book); }
    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("claim")
            .executes(c -> {
                var p = c.getSource().getPlayerOrException(); var land = LandRegistry.get(c.getSource().getServer());
                var claim = land.claim(LandRegistry.keyOf(p.blockPosition()));
                c.getSource().sendSuccess(() -> Component.literal(p.level().dimension() != net.minecraft.world.level.Level.OVERWORLD ? "Claims are in the Overworld." : claim == null
                    ? "Unclaimed land. Free claims remaining: " + land.freeRemaining(p.getUUID()) + "/" + LandRegistry.FREE_STARTER_CHUNKS + ". /claim buy claims this chunk; /jugcraft exchange opens the land map."
                    : "Owner: " + claim.ownerName + (claim.starter ? " (free starter claim)" : "") + ". Your free claims remaining: " + land.freeRemaining(p.getUUID()) + ". Party sharing: " + (claim.sharedParty == null ? "off" : "on") + ". /claim party true|false; /claim trust <name>; /claim untrust <name>."), false);
                return 1;
            })
            .then(Commands.literal("buy").executes(c -> run(c.getSource(), (p,l,b) -> l.buy(p,b,LandRegistry.keyOf(p.blockPosition()),(l.claim(LandRegistry.keyOf(p.blockPosition())) == null ? l.nextPrice(p.getUUID(),b) : l.claim(LandRegistry.keyOf(p.blockPosition())).price),FinanceService.day(p.level().getServer())))))
            .then(Commands.literal("abandon").executes(c -> run(c.getSource(),(p,l,b)->l.abandon(p,b,LandRegistry.keyOf(p.blockPosition()),false,FinanceService.day(p.level().getServer())))))
            .then(Commands.literal("party").then(Commands.argument("enabled", BoolArgumentType.bool()).executes(c -> run(c.getSource(),(p,l,b)->l.shareParty(p,BoolArgumentType.getBool(c,"enabled"))))))
            .then(Commands.literal("trust").then(Commands.argument("player",StringArgumentType.word()).executes(c -> run(c.getSource(),(p,l,b)->l.trust(p,b,StringArgumentType.getString(c,"player"),true)))))
            .then(Commands.literal("untrust").then(Commands.argument("player",StringArgumentType.word()).executes(c -> run(c.getSource(),(p,l,b)->l.trust(p,b,StringArgumentType.getString(c,"player"),false))))));
    }
    private static int run(CommandSourceStack source, Action action) throws CommandSyntaxException {
        var player=source.getPlayerOrException();
        if (player.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            source.sendFailure(Component.literal("Claims are in the Overworld."));
            return 0;
        }
        var book=Ledger.get(source.getServer()).book();book.account(player.getUUID(),player.getName().getString());
        var result=action.run(player,LandRegistry.get(source.getServer()),book);
        Ledger.get(source.getServer()).setDirty();
        FinanceService.announce(source.getServer(),Ledger.get(source.getServer()));
        if(result.ok()) source.sendSuccess(()->Component.literal(result.message()),false);
        else source.sendFailure(Component.literal(result.message()));
        return result.ok()?1:0;
    }
}

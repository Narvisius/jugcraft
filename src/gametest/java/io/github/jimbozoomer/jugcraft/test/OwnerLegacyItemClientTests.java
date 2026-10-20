package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.menagerie.Menagerie;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Focused geometry and special-renderer bases, using an isolated disposable world. */
public final class OwnerLegacyItemClientTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();
            var at=c.computeOnClient(mc->mc.player.blockPosition());int x=at.getX(),y=at.getY(),z=at.getZ();
            server.runCommand("gamemode creative @a");server.runCommand("time set noon");server.runCommand("gamerule minecraft:spawn_mobs false");
            server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x-4,y-1,z-4,x+4,y-1,z+4));
            server.runOnServer(s->{s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),Menagerie.BLOCKS.get("beast_end_pirate_ship_wheel").defaultBlockState());});
            server.runCommand("tp @p %f %f %f 155 12".formatted(x+1.5,y+.1,z+3.5));c.waitTicks(15);c.takeScreenshot("legacy_ship_wheel");
            for(String id:java.util.List.of("cavern_shot_gum","cavern_raygun","cavern_galena_gauntlet","cavern_resistor_shield","beast_end_pirate_ship_wheel","beast_end_pirate_anchor","beast_end_pirate_anchor_winch","beast_shield_of_the_deep","beast_transmutation_table","nouveau_worn_notebook","nouveau_novice_spell_book","nouveau_spell_crossbow","nouveau_arcane_core")){
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("jugcraft",id))));});
                c.waitTicks(20);c.takeScreenshot("legacy_held_"+id);
            }
        }
    }
}

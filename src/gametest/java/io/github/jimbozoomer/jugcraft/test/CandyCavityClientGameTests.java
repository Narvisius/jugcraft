package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Generates real new chunks through normal feature/structure ordering in a disposable Candy biome world. */
public final class CandyCavityClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        try(var world=c.worldBuilder().adjustSettings(creator->{
            creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
            creator.setSeed("42");
            creator.updateDimensions((registries,dimensions)->dimensions.replaceOverworldGenerator(registries,
                    new NoiseBasedChunkGenerator(new FixedBiomeSource(registries.lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Jugcraft.id("cavern_candy_cavity")))),
                            registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD))));
        }).create()) {
            var server=world.getServer();
            server.runCommand("gamemode spectator @p");server.runCommand("gamerule minecraft:spawn_mobs false");
            server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
            server.runCommand("tp @p 48 15 94 180 22");c.waitTicks(80);world.getConnection().waitForChunksRender();
            server.runOnServer(s->{
                var l=s.overworld();
                check(s.getPlayerList().getPlayers().stream().allMatch(p -> p.isSpectator()),"Screenshot camera must be spectator, not falling through the scene");
                var counts=new HashMap<String,Integer>();
                for(int x=-16;x<112;x++)for(int z=-16;z<112;z++) {
                    l.getChunk(x>>4,z>>4);
                    for(int y=-50;y<24;y++) {
                        var id=BuiltInRegistries.BLOCK.getKey(l.getBlockState(new BlockPos(x,y,z)).getBlock()).toString();
                        if(id.startsWith("jugcraft:cavern_"))counts.merge(id.substring("jugcraft:cavern_".length()),1,Integer::sum);
                    }
                }
                for(String name:List.of("cake_layer","block_of_chocolate","block_of_frosted_chocolate","purple_soda","sugar_glass","stripped_candy_cane_block","licoroot","dough_block"))
                    check(counts.getOrDefault(name,0)>0,"Natural Candy generation missing "+name+": "+counts);
                check(counts.entrySet().stream().filter(e->e.getKey().startsWith("rock_candy_")).count()>=4,"Multiple rock candy colors in natural generation");
                Jugcraft.LOGGER.info("[candy-check] Natural terrain/decor counts: {}",counts);
            });
            c.runOnClient(mc->{if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();});
            c.takeScreenshot("candy_cavity_plateaus_and_arch");
            int mouth=(int)Math.round(CandyCavity.riverZ(42,80,0));
            server.runCommand("tp @p 95 -30 "+(mouth+14)+" 135 20");c.waitTicks(30);c.takeScreenshot("candy_cavity_soda_bottle_and_river");
            server.runCommand("tp @p -48 15 -25 -45 22");c.waitTicks(40);world.getConnection().waitForChunksRender();c.takeScreenshot("candy_cavity_licoroot_lollipops");
            // Exercise structure placement at valid dry locations through Minecraft's native command.
            for(String name:List.of("licowitch_tower","gingerbread_town")) {
                final int[] chosen={0,0};
                server.runOnServer(s->{
                    var l=s.overworld(); var generator=l.getChunkSource().getGenerator();
                    var structure=l.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(Jugcraft.id("cavern_"+name));
                    boolean found=false;
                    int start=name.equals("licowitch_tower")?10:30;
                    for(int cx=start;cx<start+16&&!found;cx++)for(int cz=10;cz<26&&!found;cz++) {
                        var context=new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(l.registryAccess(),generator,generator.getBiomeSource(),l.getChunkSource().randomState().createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.EMPTY_UNCACHED),l.getChunkSource().randomState(),l.getStructureTemplateManager(),l.getSeed(),new net.minecraft.world.level.ChunkPos(cx,cz),l,b->true);
                        if(structure.findValidGenerationPoint(context).isPresent()){chosen[0]=cx*16+8;chosen[1]=cz*16+8;found=true;}
                    }
                    check(found,"A dry valid location exists for "+name);
                });
                int x=chosen[0],z=chosen[1];
                server.runCommand("tp @p "+x+" 10 "+(z+48)+" 180 8");c.waitTicks(40);world.getConnection().waitForChunksRender();
                server.runCommand("place structure jugcraft:cavern_"+name+" "+x+" -30 "+z);
                server.runOnServer(s->{
                    var l=s.overworld();int contents=0;
                    for(int dx=-28;dx<=28;dx++)for(int dz=-28;dz<=28;dz++)for(int y=-50;y<=30;y++) {
                        var state=l.getBlockState(new BlockPos(x+dx,y,z+dz));
                        if(state.is(Menagerie.BLOCKS.get(name.equals("licowitch_tower")?"cavern_licoroot":"cavern_gingerbarrel")))contents++;
                        check(!state.is(net.minecraft.world.level.block.Blocks.STRUCTURE_BLOCK),"Template markers must not become player-facing structure blocks");
                    }
                    check(contents>=(name.equals("licowitch_tower")?100:4),"Native /place actually built the supplied "+name);
                    var col=CandyCavity.column(l.getSeed(),x,z);
                    int height=name.equals("licowitch_tower")?31:24,base=Math.min(col.floor()+1,col.roof()-height-2);
                    var interior=new BlockPos(x-(name.equals("licowitch_tower")?0:1),base+(name.equals("licowitch_tower")?14:5),z);
                    check(l.getBlockState(interior).isAir()&&l.getBlockState(interior.above()).isAir(),"Supplied interior remains open and two blocks tall");
                });
                c.waitTicks(40);world.getConnection().waitForChunksRender();c.takeScreenshot("candy_cavity_"+name);
            }
            c.runOnClient(mc->{if(mc.gui.hud.isHidden())mc.gui.hud.toggle();});
        }
        // Also exercise Jugcraft's actual regional biome selector, not only the all-Candy fixture.
        try(var world=c.worldBuilder().adjustSettings(creator->{
            creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            creator.setSeed("42");
            creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
        }).create()) {
            final int[] location={0,0};
            world.getServer().runOnServer(s->{
                var l=s.overworld();boolean found=false;
                for(int cx=-8;cx<=8&&!found;cx++)for(int cz=-8;cz<=8&&!found;cz++) {
                    int x=cx*768+384,z=cz*768+384;
                    if(l.getUncachedNoiseBiome(x>>2,-8,z>>2).is(Jugcraft.id("cavern_candy_cavity"))) {
                        location[0]=x;location[1]=z;found=true;
                        l.getChunk(x>>4,z>>4);var col=CandyCavity.column(l.getSeed(),x,z);
                        check(l.getBlockState(new BlockPos(x,col.floor()-3,z)).is(Menagerie.BLOCKS.get("cavern_cake_layer"))
                                || l.getBlockState(new BlockPos(x,col.floor()-3,z)).is(Menagerie.BLOCKS.get("cavern_block_of_chocolate"))
                                || BuiltInRegistries.BLOCK.getKey(l.getBlockState(new BlockPos(x,col.floor()-3,z)).getBlock()).getPath().startsWith("cavern_rock_candy_"),"Actual regional cave has layered terrain");
                    }
                }
                check(found,"Candy biome remains reachable in a normal Jugcraft Overworld");
                Jugcraft.LOGGER.info("[candy-check] Normal Overworld Candy region at {}, {}",location[0],location[1]);
            });
            world.getServer().runCommand("gamemode spectator @p");
            world.getServer().runCommand("effect give @p minecraft:night_vision infinite 0 true");
            world.getServer().runCommand("tp @p "+location[0]+" 15 "+location[1]+" 145 22");
            c.waitTicks(60);world.getConnection().waitForChunksRender();
            c.runOnClient(mc->{if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();});
            c.takeScreenshot("candy_cavity_normal_overworld");
        }
    }
    private static void check(boolean pass,String message){if(!pass)throw new AssertionError(message);}
}

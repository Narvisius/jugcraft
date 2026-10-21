package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Real three-dimension world, authored comparison plots, natural stops, and full save/reopen checks. */
public class FungalEndTreeClientGameTests extends TreeBatchTwoClientGameTests {
    private final Map<BlockPos,Map<BlockPos,BlockState>> saved=new LinkedHashMap<>();
    @Override protected List<String> shapes(){return List.of();}
    @Override protected List<String> biomes(){return List.of();}
    @Override protected String worldName(){return "Fungi and Dragonblood Inspection";}
    @Override protected String exportName(){return "tree-batch-9-9b-preview";}
    @Override protected String screenshotPrefix(){return "jugcraft_fungal_end_";}
    @Override protected void validateReopened(ServerLevel level){saved.forEach((at,before)->check(FungalEndTreeGameTests.snapshot(level,at).equals(before),"Changed across save/reopen: "+at));}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static String xyz(BlockPos p){return p.getX()+" "+p.getY()+" "+p.getZ();}
    @Override protected void extraDisplays(ClientGameTestContext context,TestSingleplayerContext world,List<String> tour){
        var server=world.getServer();
        for(int i=0;i<FungalEndTreeGameTests.SHAPES.size();i++){
            String shape=FungalEndTreeGameTests.SHAPES.get(i);var row=GARDEN.offset(i*40,0,0);
            for(int sample=0;sample<3;sample++){
                var at=row.offset(0,0,-40*sample);long seed=sample+1;
                server.runOnServer(mc->{var level=mc.overworld();TreeBatchTwoGameTests.reset(level,at,FungalEndTreeGameTests.soil(shape));
                    for(var p:BlockPos.betweenClosed(at.offset(-20,-2,-20),at.offset(20,-2,20)))level.setBlock(p,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
                    check(FungalEndTreeGameTests.grow(level,shape,seed,at),"Gallery placement failed "+shape);
                    saved.put(at,FungalEndTreeGameTests.snapshot(level,at));});
            }
            server.runOnServer(mc->PineWoodClientGameTests.label(mc.overworld(),row.offset(0,0,10),shape.replace('_',' '),"Seeds 1 / 2 / 3","Walk north"));
            tour.add(shape+": /execute in minecraft:overworld run tp @s "+xyz(row.offset(0,0,12))+" 180 0");
            shoot(context,world,row.offset(13,7,15),row.above(4),screenshotPrefix()+shape);
        }
        // Safe arrival pad joins the first row. This is intentionally outside the measured plots.
        server.runOnServer(mc->{for(var p:BlockPos.betweenClosed(GARDEN.offset(-45,-1,10),GARDEN.offset(280,-1,18)))mc.overworld().setBlock(p,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_CLIENTS);});
        BlockPos wood=GARDEN.offset(0,0,45);
        server.runOnServer(mc->{var level=mc.overworld();TreeBatchTwoGameTests.reset(level,wood,Blocks.STONE_BRICKS);
            String[] ids={"dragonblood_log","dragonblood_wood","stripped_dragonblood_log","stripped_dragonblood_wood","dragonblood_planks","dragonblood_stairs","dragonblood_slab","dragonblood_fence","dragonblood_fence_gate"};
            for(int i=0;i<ids.length;i++){var p=wood.east(i*3);level.setBlock(p,JugcraftAgriculture.block(ids[i]).defaultBlockState(),Block.UPDATE_CLIENTS);PineWoodClientGameTests.label(level,p.south(2),ids[i].replace('_',' '),"","");}
            for(var p:BlockPos.betweenClosed(wood.offset(-3,-1,-3),wood.offset(29,-1,6)))level.setBlock(p,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_CLIENTS);
        });
        shoot(context,world,wood.offset(12,8,18),wood.east(12),screenshotPrefix()+"wood_set");
        for(boolean cutout:List.of(true,false)){
            context.runOnClient(c->c.options.cutoutLeaves().set(cutout));
            shoot(context,world,GARDEN.offset(5*40+10,7,12),GARDEN.offset(5*40,4,0),screenshotPrefix()+(cutout?"fronds_cutout":"fronds_solid"));
        }
        context.runOnClient(c->c.options.cutoutLeaves().set(true));
        server.runCommand("time set midnight");shoot(context,world,GARDEN.offset(12,7,15),GARDEN.above(5),screenshotPrefix()+"glowcaps_night");server.runCommand("time set noon");
        tour.add("Dragonblood timber: /execute in minecraft:overworld run tp @s "+xyz(wood.offset(12,1,12))+" 180 0");
        server.runOnServer(mc->{var level=mc.overworld();
            String[] seeds={"glowcap","blight_fungus","bramble","dragonblood_sapling"};
            Block[] floors={Blocks.MYCELIUM,Blocks.SOUL_SOIL,Blocks.CRIMSON_NYLIUM,Blocks.END_STONE};
            for(int i=0;i<seeds.length;i++){
                var at=GARDEN.offset(i*24,0,85);TreeBatchTwoGameTests.reset(level,at,floors[i]);
                for(var p:BlockPos.betweenClosed(at.offset(-10,-2,-10),at.offset(10,-2,10)))level.setBlock(p,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
                if(i==0)for(var p:BlockPos.betweenClosed(at.offset(-10,14,-10),at.offset(10,14,10)))level.setBlock(p,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_CLIENTS);
                level.setBlock(at,JugcraftAgriculture.block(seeds[i]).defaultBlockState(),Block.UPDATE_CLIENTS);
                PineWoodClientGameTests.label(level,at.south(10),seeds[i].replace('_',' '),"Try bone meal",i==0?"Dim light needed":"Space above needed");
            }
        });
        for(String item:List.of("minecraft:bone_meal 64","minecraft:iron_axe","minecraft:shears","jugcraft:glowcap 16","jugcraft:blight_fungus 16","jugcraft:bramble 16","jugcraft:dragonblood_sapling 16"))server.runCommand("give @a "+item);
        tour.add("Growth demonstration plots: /execute in minecraft:overworld run tp @s 1000 180 -905 180 0. Walk east for blight, bramble and dragonblood. The glowcap plot has a roof for low light.");
        // Native leaves schedule distance updates after TreeFeature returns. Compare the stable
        // saved state, but first prove settling changed only that derived support-distance property.
        context.waitTicks(20);
        server.runOnServer(mc->saved.replaceAll((at,before)->{
            var after=FungalEndTreeGameTests.snapshot(mc.overworld(),at);
            check(before.keySet().equals(after.keySet()),"Gallery blocks changed while settling: "+at);
            int changed=0;
            for(var entry:before.entrySet()){
                var old=entry.getValue();var now=after.get(entry.getKey());
                if(!old.equals(now))changed++;
                if(old.hasProperty(LeavesBlock.DISTANCE)&&now.hasProperty(LeavesBlock.DISTANCE)){
                    check(now.getValue(LeavesBlock.DISTANCE)<7,"Unsupported settled leaves: "+at);
                    old=old.setValue(LeavesBlock.DISTANCE,1);now=now.setValue(LeavesBlock.DISTANCE,1);
                }
                check(old.equals(now),"Unexpected settling change at "+at+" / "+entry.getKey()+": "+old+" -> "+now);
            }
            if(changed>0)Jugcraft.LOGGER.info("[fungal End settling] {}: {} native leaf-distance updates; every block and other property preserved",at,changed);
            return after;
        }));
        for(String name:List.of("toadstool_field","mycelial_jungle","glowcap_grotto","fungal_thicket","blighted_sands","withered_hollow","marrow_heap","netherbrush","ender_wilds","outer_flats","rotted_expanse")){
            ResourceKey<Level> dimension=List.of("fungal_thicket","blighted_sands","withered_hollow","marrow_heap","netherbrush").contains(name)?Level.NETHER:
                List.of("ender_wilds","outer_flats","rotted_expanse").contains(name)?Level.END:Level.OVERWORLD;
            var found=server.computeOnServer(mc->natural(mc.getLevel(dimension),name));
            check(found!=null,"No natural new growth found in "+name);
            var target=found;var camera=server.computeOnServer(mc->camera(mc.getLevel(dimension), target));
            shootNatural(context,world,dimension,camera,target.above(2),screenshotPrefix()+"natural_"+name);
            tour.add(name+" (natural): /execute in "+dimension.identifier()+" run tp @s "+xyz(camera)+" 140 20");
        }
        server.runCommand("execute in minecraft:overworld run tp @a 960 180 -988");
        tour.add("Glowcaps: bone meal a small Glowcap on moss, mycelium, mud or nylium in dim light (below 13). Blight: soul soil or blackstone. Bramble: nylium/soul soil/soul sand. Each has 25% growth chance per bone meal; leave room for the full crown.");
        tour.add("Marrow fungi are world-generation-only. Silk Touch keeps huge cap blocks; normal breaking yields occasional planting fungi. Shears collect bramble leaves.");
        tour.add("Dragonblood saplings grow on end stone or moss with no light requirement. Leaves yield saplings and rare red-dye resin. Random ticks are paused: /gamerule minecraft:random_tick_speed 3 enables growth and leaf decay.");
    }
    private static void shootNatural(ClientGameTestContext context, TestSingleplayerContext world,
            ResourceKey<Level> dimension, BlockPos camera, BlockPos target, String name) {
        // The inherited gallery helper runs a server-origin tp, whose dimension is always the Overworld.
        world.getServer().runCommand("execute in "+dimension.identifier()+" run tp @a "+xyz(camera));
        world.getServer().runOnServer(server->{
            var player=server.getPlayerList().getPlayers().getFirst();
            check(player.level().dimension().equals(dimension),"Natural camera is in the wrong dimension");
            player.getAbilities().flying=true;player.onUpdateAbilities();
        });
        context.waitTicks(30);
        world.getConnection().waitForChunksRender();
        context.runOnClient(client->check(client.level.dimension().equals(dimension),"Rendered dimension did not follow teleport"));
        context.getInput().lookAt(target);
        context.runOnClient(client->{if(!client.gui.hud.isHidden())client.gui.hud.toggle();});
        context.waitTicks(10);context.takeScreenshot(name);
        context.runOnClient(client->{if(client.gui.hud.isHidden())client.gui.hud.toggle();});
    }
    private static BlockPos camera(ServerLevel level, BlockPos target){
        for(int height:new int[]{3,1,-1,5})for(int radius:new int[]{8,6,4})for(int[] dir:new int[][]{{1,1},{-1,1},{1,-1},{-1,-1}}){
            var at=target.offset(dir[0]*radius,height,dir[1]*radius);
            if(!level.getBlockState(at).isAir()||!level.getBlockState(at.above()).isAir())continue;
            boolean clear=true;
            for(int step=1;step<12;step++){
                double t=step/16.;var p=BlockPos.containing(at.getX()+(target.getX()-at.getX())*t,
                    at.getY()+1+(target.getY()+2-at.getY()-1)*t,at.getZ()+(target.getZ()-at.getZ())*t);
                if(!level.getBlockState(p).isAir()){clear=false;break;}
            }
            if(clear)return at;
        }
        // A last bounded search still guarantees the player never arrives embedded in rock.
        for(var p:BlockPos.betweenClosed(target.offset(-8,-3,-8),target.offset(8,8,8)))
            if(level.getBlockState(p).isAir()&&level.getBlockState(p.above()).isAir())return p.immutable();
        throw new AssertionError("No safe inspection camera near "+target);
    }
    private static Block expected(String biome){return switch(biome){case "blighted_sands","withered_hollow","rotted_expanse"->JugcraftAgriculture.block("blight_wart_block");case "marrow_heap"->Blocks.SHROOMLIGHT;case "netherbrush"->JugcraftAgriculture.block("bramble_bush_leaves");case "ender_wilds","outer_flats"->JugcraftAgriculture.block("dragonblood_log");default->JugcraftAgriculture.block("glowcap_block");};}
    private static boolean rareEndBiome(String name){return name.equals("outer_flats")||name.equals("rotted_expanse");}
    private static BlockPos natural(ServerLevel level,String name){
        var key=ResourceKey.create(Registries.BIOME,Jugcraft.id(name));
        // The nearest biome fragment need not have a successful placement: Netherbrush may
        // expose too few clear floors, just as a tiny End fringe may miss a rare tree.
        // Keep the natural-growth assertion, but sample at most five regions for every biome.
        // This only observes existing generation; it never plants or changes placement rates.
        int[][] anchors={{0,0},{4096,0},{0,4096},{-4096,0},{0,-4096}};
        for(int[] anchor:anchors){
            var result=naturalNear(level,name,key,new BlockPos(anchor[0],64,anchor[1]));
            if(result!=null)return result;
        }
        return null;
    }
    private static BlockPos naturalNear(ServerLevel level,String name,ResourceKey<Biome> key,BlockPos anchor){
        var nearest=level.findClosestBiome3d(b->b.is(key),anchor,12800,32,32);
        if(nearest==null)return null;
        var first=nearest.getFirst();var wanted=expected(name);int chunks=0;
        // A bounded spiral covers sparse placements without planting anything or changing terrain.
        for(int r=0;r<=(rareEndBiome(name)?24:10);r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
            int x0=(first.getX()>>4)*16+dx*16,z0=(first.getZ()>>4)*16+dz*16;
            // Rare End extras may occupy a biome corner even when the chunk centre belongs to another biome.
            if(!rareEndBiome(name)&&!level.getBiome(new BlockPos(x0+8,first.getY(),z0+8)).is(key))continue;
            level.getChunk(x0>>4,z0>>4);chunks++;
            for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++){
                int lo=level.dimension()==Level.NETHER?1:name.equals("glowcap_grotto")?level.getMinY()+1:Math.max(level.getMinY()+1,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-20);
                int hi=level.dimension()==Level.NETHER?124:level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
                for(int y=lo;y<hi;y++){
                    var p=new BlockPos(x,y,z);if(level.getBlockState(p).is(wanted)&&level.getBiome(p).is(key)){
                        Jugcraft.LOGGER.info("[fungal End natural] {} {} after {} surveyed chunks",name,p,chunks);return p;
                    }
                }
            }
        }
        Jugcraft.LOGGER.warn("[fungal End natural] no {} in {} surveyed chunks near {}",name,chunks,first);return null;
    }
}

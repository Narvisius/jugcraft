package io.github.jimbozoomer.jugcraft.test;

import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/** Real rendered import smoke tests in a disposable world, never the owner's profile. */
public final class NewestCollectionClientTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        c.runOnClient(mc->{
            var families=java.util.Set.of("jugtania","forbidden","wilds","aquatic","supps","amends","packworks","ars","skyweave","homeworks","stoneworks","windowworks","doorworks","bridgeworks","roofworks","furnitureworks","fenceworks","twigs","holiday","ribbit");
            var building=io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks.source("newest_building.json").getAsJsonObject("blocks").keySet();
            var models=mc.getModelManager().getBlockStateModelSet();
            var atlas=mc.getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS);
            var checked=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<net.minecraft.client.renderer.block.dispatch.BlockStateModel,Boolean>());
            int states=0;
            var broken=new java.util.LinkedHashSet<String>();
            for(var block:BuiltInRegistries.BLOCK){
                var id=BuiltInRegistries.BLOCK.getKey(block);String name=id.getPath().replaceFirst("^diagonal_","");
                if(!id.getNamespace().equals("jugcraft")||(!building.contains(name)&&!families.contains(name.split("_",2)[0])))continue;
                for(var state:block.getStateDefinition().getPossibleStates()){
                    if(state.getRenderShape()!=net.minecraft.world.level.block.RenderShape.MODEL)continue;
                    var model=models.get(state);
                    if(model==models.missingModel()){broken.add("Rejected new block model: "+state);continue;}
                    if(checked.add(model)){
                        var parts=new java.util.ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
                        model.collectParts(net.minecraft.util.RandomSource.create(0),parts);
                        for(var part:parts)for(int face=-1;face<6;face++)for(var quad:part.getQuads(face<0?null:net.minecraft.core.Direction.values()[face]))
                            if(quad.materialInfo().sprite()==atlas.missingSprite())broken.add("Missing new block face: "+state);
                    }
                    states++;
                }
            }
            System.out.println("Newest collection baked block audit: "+states+" states, "+checked.size()+" distinct models.");
            if(!broken.isEmpty())throw new AssertionError(broken.size()+" broken new block presentations; first 20: "+broken.stream().limit(20).toList());
        });
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();
            server.runCommand("gamemode creative @a");server.runCommand("time set noon");server.runCommand("gamerule minecraft:spawn_mobs false");
            // Resolve every registered item in these new families, including native
            // special renderers. This catches exceptions, not visual resemblance.
            c.runOnClient(mc->{int count=0;
                for(var item:BuiltInRegistries.ITEM){var id=BuiltInRegistries.ITEM.getKey(item);
                    if(!id.getNamespace().equals("jugcraft")||List.of("jugtania_","forbidden_","wilds_","aquatic_","supps_","amends_","packworks_","ars_additions_","skyweave_","homeworks_").stream().noneMatch(id.getPath()::startsWith))continue;
                    var state=new ItemStackRenderState();mc.getItemModelResolver().updateForTopItem(state,new ItemStack(item),ItemDisplayContext.GUI,mc.level,mc.player,0);count++;
                }
                if(count<500)throw new AssertionError("New collection registration unexpectedly incomplete: "+count);
            });
            for(String name:List.of("jugtania_mana_ring","jugtania_mana_tablet","forbidden_quantum_injector","forbidden_soul_extractor","wilds_earthrend_gauntlet","aquatic_tackle_box","aquatic_arapaima_spawn_egg","supps_statue","supps_prismarine_rod","supps_lumisene_bottle","packworks_backpack","homeworks_dye_vat","homeworks_iron_bathtub","homeworks_acacia_wall_clock")){
                var id=Identifier.fromNamespaceAndPath("jugcraft",name);
                server.runOnServer(s->{var item=BuiltInRegistries.ITEM.getValue(id);if(item==null||item==net.minecraft.world.item.Items.AIR)throw new AssertionError("Missing sample item "+id);s.getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));});
                c.waitTicks(12);c.takeScreenshot("newest_held_"+name);
            }
            server.runCommand("fill -12 79 -12 12 79 24 minecraft:stone");
            server.runCommand("tp @a 0 80 0 0 0");
            for(String name:List.of("wilds_frostmaw","wilds_grottol","wilds_foliaath","ribbit_gardener","supps_hat_stand")){
                server.runOnServer(s->{var id=Identifier.fromNamespaceAndPath("jugcraft",name);if(!BuiltInRegistries.ENTITY_TYPE.containsKey(id))throw new AssertionError("Missing creature "+id);});
                server.runCommand("summon jugcraft:"+name+" 0 80 9 {NoAI:1b,PersistenceRequired:1b,Tags:[\"newest_preview\"]}");
                c.waitTicks(30);c.takeScreenshot("newest_creature_"+name);
                server.runCommand("kill @e[tag=newest_preview]");
            }
            for(String name:List.of("supps_statue","supps_barnacles","supps_lumisene","jugtania_mana_pool","jugtania_runic_altar","homeworks_dye_vat","windowworks_blackstone_brick_gothic",
                    "homeworks_bamboo_lamp","homeworks_jungle_lamp","homeworks_oak_large_stump","supps_lunch_basket",
                    "twigworks_paper_lantern","jugtania_corporea_crystal_cube","jugtania_mana_spreader","packworks_backpack","supps_awning")){
                server.runOnServer(s->{var id=Identifier.fromNamespaceAndPath("jugcraft",name);if(!BuiltInRegistries.BLOCK.containsKey(id))throw new AssertionError("Missing block "+id);});
                server.runCommand("setblock 0 80 4 jugcraft:"+name);
                c.waitTicks(20);c.takeScreenshot("newest_block_"+name);
                if(List.of("homeworks_bamboo_lamp","homeworks_jungle_lamp","homeworks_oak_large_stump","supps_lunch_basket",
                        "twigworks_paper_lantern","jugtania_corporea_crystal_cube","jugtania_mana_spreader","packworks_backpack","supps_awning").contains(name)){
                    server.runCommand("tp @a 3 81 1 45 20");c.waitTicks(12);c.takeScreenshot("nov1_geometry_side_"+name);
                    server.runCommand("tp @a 0 81 8 180 20");c.waitTicks(12);c.takeScreenshot("nov1_geometry_back_"+name);
                    server.runCommand("tp @a 0 80 0 0 0");
                }
                server.runCommand("fill -4 80 1 4 83 7 minecraft:air");
            }
            // Joined stump components deliberately omit internal faces. Inspect
            // the assembled L, not an isolated corner template treated as a cube.
            server.runCommand("setblock 0 80 4 jugcraft:homeworks_oak_large_stump");
            server.runCommand("setblock 1 80 4 jugcraft:homeworks_oak_large_stump");
            server.runCommand("setblock 0 80 5 jugcraft:homeworks_oak_large_stump");
            server.runCommand("tp @a 4 83 1 45 35");c.waitTicks(20);c.takeScreenshot("nov1_joined_stump_front");
            server.runCommand("tp @a -3 83 8 -135 35");c.waitTicks(20);c.takeScreenshot("nov1_joined_stump_back");
        }
    }
}

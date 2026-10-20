package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.FormMachineScreen;
import io.github.jimbozoomer.jugcraft.client.MachineScreen;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineUpgrades;
import io.github.jimbozoomer.jugcraft.machine.form.*;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Disposable automated UI fixture; its saved copy is handed to the owner for manual play. */
public class TechSevenClientGameTests implements FabricClientGameTest {
    private static final List<String> STATIONS=List.of("precision_finisher","wet_processing_bench","mineral_conditioner",
        "alumina_calciner","chemical_dissolution_chamber","aluminate_filter_press","chemical_infuser",
        "chemical_distillation_station","bulk_chemical_reactor","electrolytic_separator","lead_acid_battery_bank",
        "lithium_battery_bank","flow_battery","cryogenic_liquefier","lithography_station","circuit_assembler",
        "rocket_workshop","insulated_fluid_tank","vacuum_insulated_tank");
    @Override public void runTest(ClientGameTestContext c) {
        try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            world.getConnection().waitForChunksRender();var server=world.getServer();
            var origin=c.computeOnClient(mc->BlockPos.containing(mc.player.position()));
            var base=new BlockPos(origin.getX(),80,origin.getZ());int x=base.getX(),y=base.getY(),z=base.getZ();
            server.runCommand("time set noon");server.runCommand("weather clear");
            server.runCommand("gamerule minecraft:advance_time false");server.runCommand("gamerule minecraft:spawn_mobs false");
            server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-8,y,z-8,x+55,y+7,z+52));
            server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x-8,y-1,z-8,x+55,y-1,z+52));
            for(int i=0;i<STATIONS.size();i++) {
                final int index=i;var at=base.offset((i%5)*11,0,(i/5)*12);
                server.runOnServer(s->{
                    var level=s.overworld();var block=BuiltInRegistries.BLOCK.getValue(Jugcraft.id(STATIONS.get(index)));
                    if(block==Blocks.AIR)throw new AssertionError("Missing station "+STATIONS.get(index));
                    var state=block.defaultBlockState();if(state.hasProperty(MachineBlock.FACING))state=state.setValue(MachineBlock.FACING,Direction.SOUTH);
                    level.setBlock(at,state,Block.UPDATE_ALL);block.setPlacedBy(level,at,state,null,ItemStack.EMPTY);
                    var entity=level.getBlockEntity(at);
                    if(entity instanceof FormMachineBlockEntity m) {
                        ((SimpleEnergyStorage)m.energy()).setAmount(m.energy().getCapacity());
                        var entries=m.recipes(s);if(!entries.isEmpty()) {
                            var entry=entries.getFirst();var r=entry.recipe();m.selectRecipe(entry.id());
                            for(int n=0;n<r.items().size();n++)m.container().setItem(n,new ItemStack(r.items().get(n).ingredient().items().findFirst().orElseThrow(),r.items().get(n).count()));
                            for(int n=0;n<r.fluids().size();n++)m.tanks().input(n).fill(r.fluids().get(n).fluid(),r.fluids().get(n).amount());
                            r.tool().ifPresent(t->m.container().setItem(m.form().firstSocketSlot(),new ItemStack(t.items().findFirst().orElseThrow())));
                        }
                        m.container().setItem(m.form().firstUpgradeSlot(),new ItemStack(MachineUpgrades.AUTOMATION));
                        m.togglePause();
                    } else if(entity instanceof MachineBlockEntity m) {
                        if(STATIONS.get(index).equals("flow_battery")) {
                            for(int n=0;n<3;n++) {
                                var pos=at.offset(1,n==2?3:0,n==2?0:-2-n);
                                var part=BuiltInRegistries.BLOCK.getValue(Jugcraft.id(n==2?"flow_stack_module":"flow_tank_module"));
                                var partState=part.defaultBlockState().setValue(MachineBlock.FACING,Direction.SOUTH);
                                level.setBlock(pos,partState,Block.UPDATE_ALL);part.setPlacedBy(level,pos,partState,null,ItemStack.EMPTY);
                            }
                            m.reservoir().variant=net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(io.github.jimbozoomer.jugcraft.chemistry.PetroFluids.VANADIUM_ELECTROLYTE.source());
                            m.reservoir().amount=96000L*io.github.jimbozoomer.jugcraft.fluid.FluidNetworks.DROPLETS_PER_MB;
                        }
                        if(STATIONS.get(index).equals("cryogenic_liquefier"))m.setItem(0,new ItemStack(io.github.jimbozoomer.jugcraft.chemistry.PetroItems.HYDROGEN_COOLING_MODULE));
                        if(STATIONS.get(index).equals("lithography_station"))m.setItem(3,new ItemStack(io.github.jimbozoomer.jugcraft.chemistry.PetroItems.GENERAL_CHIP_PATTERN));
                        var energy=io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.find(level,at,Direction.NORTH);
                        if(energy!=null) {
                            var saved=m.saveWithFullMetadata(level.registryAccess());saved.putLong("energy",energy.getCapacity());
                            m.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),saved));
                        }
                    }
                    var chest=at.offset(4,0,4);level.setBlock(chest,Blocks.CHEST.defaultBlockState(),Block.UPDATE_ALL);
                    var inventory=(ChestBlockEntity)level.getBlockEntity(chest);inventory.setItem(0,new ItemStack(block));
                    if(entity instanceof FormMachineBlockEntity m) {
                        var supplies=new java.util.LinkedHashMap<net.minecraft.world.item.Item,ItemStack>();
                        for(var e:m.recipes(s)) {
                            var r=e.recipe();r.items().forEach(p->{var item=p.ingredient().items().findFirst().orElseThrow();supplies.put(item.value(),new ItemStack(item,64));});
                            r.tool().ifPresent(t->{var item=t.items().findFirst().orElseThrow();supplies.put(item.value(),new ItemStack(item));});
                            r.fluids().forEach(f->{var item=f.fluid().getBucket();if(item!=net.minecraft.world.item.Items.AIR)supplies.put(item,new ItemStack(item));});
                        }
                        int slot=1;for(var stack:supplies.values()){if(slot>=27)break;inventory.setItem(slot++,stack);}
                    }
                });
            }
            for(int i:List.of(0,1,10,12,13,14)) {
                var at=base.offset((i%5)*11,0,(i/5)*12);
                server.runCommand("tp @p %d %d %d 180 12".formatted(at.getX()+1,y,at.getZ()+5));
                c.waitTicks(25);world.getConnection().waitForChunksRender();c.takeScreenshot("tech_seven_model_"+STATIONS.get(i));
                server.runOnServer(s->{var entity=s.overworld().getBlockEntity(at);s.getPlayerList().getPlayers().getFirst().openMenu((net.minecraft.world.MenuProvider)entity);});
                if(i<2){
                    c.waitForScreen(FormMachineScreen.class);c.takeScreenshot("tech_seven_menu_"+i);
                    c.runOnClient(mc->((FormMachineScreen)mc.gui.screen()).openRecipes());
                    c.waitForScreen(io.github.jimbozoomer.jugcraft.client.MachineRecipeScreen.class);c.waitTicks(5);
                    c.takeScreenshot("tech_seven_recipes_"+i);
                    c.runOnClient(mc->{var buttons=mc.gui.screen().children().stream().filter(v->v instanceof net.minecraft.client.gui.components.Button).map(v->(net.minecraft.client.gui.components.Button)v).filter(v->v.visible).toList();
                        for(int a=0;a<buttons.size();a++)for(int b=a+1;b<buttons.size();b++){var u=buttons.get(a);var v=buttons.get(b);if(u.getX()<v.getX()+v.getWidth()&&u.getX()+u.getWidth()>v.getX()&&u.getY()<v.getY()+v.getHeight()&&u.getY()+u.getHeight()>v.getY())throw new AssertionError("Overlapping recipe controls");}
                    });
                }else {c.waitForScreen(MachineScreen.class);c.takeScreenshot("tech_seven_menu_"+i);}
                c.setScreen(()->null);
            }
            server.runCommand("setworldspawn %d %d %d".formatted(x+20,y,z+49));
            server.runCommand("tp @p %d %d %d 180 0".formatted(x+20,y,z+49));
            c.waitTicks(15);world.getConnection().waitForChunksRender();c.takeScreenshot("tech_seven_overview");
        }
    }
}

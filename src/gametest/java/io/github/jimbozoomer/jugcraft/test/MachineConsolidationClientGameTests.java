package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.client.FormMachineScreen;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.form.IndustrialForms;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;

/** Actual native models and saved local showroom. Test closure is separate from the owner's later manual session. */
public class MachineConsolidationClientGameTests implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            world.getConnection().waitForChunksRender();
            var server = world.getServer();
            BlockPos initialSpawn = context.computeOnClient(c -> BlockPos.containing(c.player.position()));
            BlockPos spawn = new BlockPos(initialSpawn.getX(), 62, initialSpawn.getZ());
            int y = spawn.getY();
            int x = spawn.getX();
            int z = spawn.getZ();
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("gamerule minecraft:advance_time false");
            server.runCommand("gamerule minecraft:spawn_mobs false");
            server.runCommand("gamerule minecraft:send_command_feedback false");
            server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-12,y,z-14,x+56,y+7,z+36));
            server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x-12,y-2,z-14,x+56,y-1,z+36));
            List<FormMachineBlock> blocks = List.of(IndustrialForms.BRINE_CONCENTRATOR,IndustrialForms.BRINE_CONCENTRATOR,IndustrialForms.GAS_CLEANING_RACK,IndustrialForms.GAS_CLEANING_RACK,IndustrialForms.GAS_CLEANING_RACK,IndustrialForms.ALUMINATE_FILTER_PRESS,IndustrialForms.ALUMINATE_FILTER_PRESS,IndustrialForms.ALUMINA_CALCINER,IndustrialForms.ALUMINA_CALCINER,IndustrialForms.ALUMINA_CALCINER,IndustrialForms.ALUMINA_CALCINER,IndustrialForms.ALUMINA_CALCINER,IndustrialForms.ACID_CONCENTRATOR,IndustrialForms.BIOGAS_CLEANER,IndustrialForms.DIGESTATE_SEPARATOR,IndustrialForms.CHLORIDE_DRYER,IndustrialForms.PTFE_SINTERING_OVEN);
            for (int i = 0; i < blocks.size(); i++) {
                final int which = i;
                BlockPos pos = spawn.offset((i%6)*10,0,(i/6)*14-4);
                FormMachineBlock block = blocks.get(i);
                server.runOnServer(s -> {
                    var level = s.overworld();
                    var state = block.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
                    level.setBlock(pos,state,Block.UPDATE_ALL);
                    block.setPlacedBy(level,pos,state,null,ItemStack.EMPTY);
                    stock(level,pos,which);
                });
            }
            context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); });
            for (int i=0;i<blocks.size();i++) {
                final int which=i;
                BlockPos pos=spawn.offset((i%6)*10,0,(i/6)*14-4);
                double cx=pos.getX()+blocks.get(i).form().width()/2.0;
                double cz=pos.getZ()+1-blocks.get(i).form().depth()/2.0;
                double cy=y+blocks.get(i).form().height()/2.0;
                server.runOnServer(s -> { stock(s.overworld(),pos,which); machine(s.overworld(),pos).togglePause(); });
                double dx=blocks.get(i).form().height()>=4?6.5:3.2;
                double dz=blocks.get(i).form().height()>=4?9:4.8;
                int cameraY=y+(blocks.get(i).form().height()>=4?2:1);
                context.waitTicks(80); // Includes the light hold and the longest rotor spin-down.
                shoot(context,world,cx-dx,cameraY,cz+dz,cx,cy,cz,"consolidation_"+blocks.get(i).form().id().getPath()+"_idle");
                float idle=angle(context,pos);
                context.waitTicks(7);
                if(Math.abs(angle(context,pos)-idle)>.01f) throw new AssertionError("Paused machine still rotates: "+blocks.get(i).form().id());
                server.runOnServer(s -> machine(s.overworld(),pos).togglePause());
                for (int view=0;view<4;view++) {
                    server.runOnServer(s -> stock(s.overworld(),pos,which));
                    double[][] at={{cx-dx,cz+dz},{cx+dx,cz+dz},{cx+dx,cz-dz},{cx-dx,cz-dz}};
                    shoot(context,world,at[view][0],cameraY,at[view][1],cx,cy,cz,"consolidation_"+blocks.get(i).form().id().getPath()+"_working_"+view);
                    if(view==0) {
                        float moving=angle(context,pos);
                        context.waitTicks(7);
                        if(!io.github.jimbozoomer.jugcraft.machine.form.CoastalSeawaterIntake.matches(blocks.get(i).form()) && Math.abs(angle(context,pos)-moving)<.1f) throw new AssertionError("Working machine has no movement: "+blocks.get(i).form().id());
                    }
                }
            }
            for(int i=0;i<blocks.size();i++) {
                BlockPos pos=spawn.offset((i%6)*10,0,(i/6)*14-4);
                server.runCommand("tp @p %d %d %d 180 0".formatted(pos.getX(),y,pos.getZ()+3));context.waitTicks(3);
                server.runOnServer(t -> t.getPlayerList().getPlayers().getFirst().openMenu(machine(t.overworld(),pos)));
                context.waitForScreen(FormMachineScreen.class);context.takeScreenshot("consolidation_menu_"+i);
                context.clickScreenButton("Recipes");
                context.waitForScreen(io.github.jimbozoomer.jugcraft.client.MachineRecipeScreen.class);
                context.waitTicks(3);context.takeScreenshot("recipe_picker_"+i);
                context.clickScreenButton("Lock recipe");context.waitTicks(3);
                context.runOnClient(c -> {
                    var menu=(io.github.jimbozoomer.jugcraft.machine.form.FormMachineMenu)c.player.containerMenu;
                    if(menu.selectedRecipe()==0 || menu.catalog().isEmpty()) throw new AssertionError("Recipe selection did not synchronize");
                });
                context.takeScreenshot("recipe_locked_"+i);
                context.runOnClient(c -> c.gui.screen().mouseScrolled(c.getWindow().getGuiScaledWidth()-20,100,0,-12));
                context.takeScreenshot("recipe_outputs_"+i);
                if(i==7) {
                    context.runOnClient(c -> c.gui.screen().children().stream()
                        .filter(child -> child instanceof net.minecraft.client.gui.components.EditBox).findFirst()
                        .ifPresent(child -> ((net.minecraft.client.gui.components.EditBox)child).setValue("ptfe")));
                    context.runOnClient(c -> {
                        long count=c.gui.screen().children().stream().filter(child -> child instanceof net.minecraft.client.gui.components.Button button
                            && button.visible && button.getMessage().getString().toLowerCase(java.util.Locale.ROOT).contains("ptfe")).count();
                        if(count!=3)throw new AssertionError("Search must show all three PTFE processes, found "+count);
                    });
                    context.clickScreenButton("Sinter ptfe seal");context.waitTicks(2);
                    context.clickScreenButton("Lock recipe");context.waitTicks(3);
                    context.runOnClient(c -> {
                        var menu=(io.github.jimbozoomer.jugcraft.machine.form.FormMachineMenu)c.player.containerMenu;
                        if(menu.selectedRecipe()==0 || !menu.catalog().get(menu.selectedRecipe()-1).id().getPath().endsWith("sinter_ptfe_seal"))
                            throw new AssertionError("Filtered recipe row did not select and lock PTFE seal");
                    });
                    context.takeScreenshot("recipe_search_ptfe");
                    int previousScale=context.computeOnClient(c->c.options.guiScale().get());
                    for(int scale:List.of(2,3)) {
                        context.runOnClient(c->{c.options.guiScale().set(scale);c.resizeGui();});
                        context.waitTicks(2);context.takeScreenshot("recipe_scale_"+scale);
                    }
                    context.runOnClient(c->{c.options.guiScale().set(previousScale);c.resizeGui();});
                }
                context.clickScreenButton("Automatic");context.waitTicks(3);
                context.runOnClient(c -> {
                    if(((io.github.jimbozoomer.jugcraft.machine.form.FormMachineMenu)c.player.containerMenu).selectedRecipe()!=0)
                        throw new AssertionError("Automatic mode did not synchronize");
                });
                context.clickScreenButton("Back to machine");context.waitForScreen(FormMachineScreen.class);
                context.setScreen(()->null);
            }
            // End with a stocked, paused factory so the owner can start each machine when ready.
            server.runOnServer(s -> {
                for(int i=0;i<blocks.size();i++) {
                    BlockPos pos=spawn.offset((i%6)*10,0,(i/6)*14-4);
                    var m=machine(s.overworld(),pos);
                    for(int tank=0;tank<m.form().tanks().inputTanks().size();tank++) {
                        var input=m.tanks().input(tank);
                        input.drain(input.millibuckets());
                    }
                    m.cancel();
                    stock(s.overworld(),pos,i);
                    if(!m.paused()) m.togglePause();
                    // Ordinary charged battery and copper cable, so the owner has sustained test power.
                    var port=m.form().ports().stream().filter(p -> p.kind().energy()).findFirst().orElseThrow();
                    BlockPos at=m.form().cellPos(pos,Direction.SOUTH,m.form().cellIndex(port.column(),port.row(),port.layer()));
                    Direction side=port.side().world(Direction.SOUTH);
                    var battery=io.github.jimbozoomer.jugcraft.machine.JugcraftMachines.MACHINES.get(io.github.jimbozoomer.jugcraft.machine.MachineKind.BATTERY_BOX);
                    var batteryState=battery.defaultBlockState().setValue(MachineBlock.FACING, m.form().generator()?side:side.getOpposite());
                    BlockPos batteryPos=at.relative(side,2);
                    s.overworld().setBlock(batteryPos,batteryState,Block.UPDATE_ALL);
                    battery.setPlacedBy(s.overworld(),batteryPos,batteryState,null,ItemStack.EMPTY);
                    s.overworld().setBlock(at.relative(side),io.github.jimbozoomer.jugcraft.machine.JugcraftMachines.COPPER_CABLE.defaultBlockState(),Block.UPDATE_ALL);
                    if(s.overworld().getBlockEntity(batteryPos) instanceof io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity box) {
                        var energy=io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.find(s.overworld(),batteryPos,null);
                        if(energy instanceof SimpleEnergyStorage store) store.setAmount(m.form().generator()?0:store.getCapacity());
                    }
                }

                var supplies=new java.util.ArrayList<ItemStack>();
                for(var block:blocks.stream().distinct().toList()) {
                    supplies.add(new ItemStack(block));
                    int slot=blocks.indexOf(block);
                    var formMachine=machine(s.overworld(),spawn.offset((slot%6)*10,0,(slot/6)*14-4));
                    for(var entry:formMachine.recipes(s)) {
                        for(var item:entry.recipe().items()) supplies.add(new ItemStack(item.ingredient().items().findFirst().orElseThrow(),64));
                        for(var fluid:entry.recipe().fluids()) {
                            var variant=net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(fluid.fluid());
                            if(io.github.jimbozoomer.jugcraft.fluid.GasCylinderItem.isGas(variant)) {
                                var cylinder=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id("gas_cylinder")));
                                supplies.add(io.github.jimbozoomer.jugcraft.fluid.GasCylinderItem.filled(cylinder,variant,8000L*io.github.jimbozoomer.jugcraft.fluid.FluidNetworks.DROPLETS_PER_MB));
                            } else supplies.add(new ItemStack(fluid.fluid().getBucket()));
                        }
                    }
                }
                for(int i=0;i<supplies.size();i++) {
                    BlockPos chest=spawn.offset(6+(i/27)*3,0,36);
                    if(i%27==0)s.overworld().setBlock(chest,Blocks.CHEST.defaultBlockState(),Block.UPDATE_ALL);
                    ((ChestBlockEntity)s.overworld().getBlockEntity(chest)).setItem(i%27,supplies.get(i));
                }

                io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.info("Consolidated factory manual-test world: {}",s.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT));
            });
            context.waitTicks(8);
            for(int i=0;i<blocks.size();i++){
                BlockPos pos=spawn.offset((i%6)*10,0,(i/6)*14-4);var f=blocks.get(i).form();
                shoot(context,world,pos.getX()-4,y+2,pos.getZ()+5,pos.getX()+f.width()/2.,y+f.height()/2.,pos.getZ()+1-f.depth()/2.,"consolidation_connections_"+i);
            }
            server.runCommand("fill %d %d %d %d %d %d minecraft:air replace minecraft:barrier".formatted(x-12,y,z-14,x+56,y+7,z+36));
            context.runOnClient(c -> {if(c.gui.hud.isHidden())c.gui.hud.toggle();});
            server.runCommand("setworldspawn %d %d %d".formatted(x+13,y,z+34));
            server.runCommand("tp @p %d %d %d 180 0".formatted(x+13,y,z+34));context.waitTicks(10);
        }
    }
    private static FormMachineBlockEntity machine(ServerLevel level,BlockPos p) { return (FormMachineBlockEntity)level.getBlockEntity(p); }
    private static float angle(ClientGameTestContext context,BlockPos pos) {
        return context.computeOnClient(c -> io.github.jimbozoomer.jugcraft.client.MachineRotors.extract(c.level.getBlockEntity(pos),0).getFirst().angle());
    }
    private static void stock(ServerLevel level,BlockPos p,int index) {
        var m=machine(level,p);
        ((SimpleEnergyStorage)m.energy()).setAmount(m.energy().getCapacity());
        for(int i=0;i<m.form().tanks().outputTanks().size();i++) m.tanks().output(i).drain(m.tanks().output(i).millibuckets());
        for(int i=0;i<m.form().itemOutputs();i++) m.container().setItem(m.form().firstOutputSlot()+i,ItemStack.EMPTY);
        if(io.github.jimbozoomer.jugcraft.machine.form.CoastalSeawaterIntake.matches(m.form())) return;
        var recipes=m.recipes(level.getServer());
        String preferred=List.of("concentrate_coastal_seawater","concentrate_sulfuric_acid","rack_clean_synthesis_gas","clean_biogas","clean_fermentation_co2","recover_aluminate","separate_digestate","calcine_hydroxide","dry_magnesium_chloride","sinter_ptfe_seal","sinter_ptfe_fitting","sinter_ptfe_liner","concentrate_sulfuric_acid","clean_biogas","separate_digestate","dry_magnesium_chloride","sinter_ptfe_seal").get(index);
        var recipe=preferred.isEmpty()?recipes.getFirst().recipe():recipes.stream().filter(e->e.id().getPath().endsWith(preferred)).findFirst().orElseThrow().recipe();
        recipe.tool().ifPresent(tool -> m.container().setItem(m.form().firstSocketSlot(),new ItemStack(tool.items().findFirst().orElseThrow())));
        for(int i=0;i<recipe.items().size();i++) m.container().setItem(i,new ItemStack(recipe.items().get(i).ingredient().items().findFirst().orElseThrow(),64));
        for(int i=0;i<recipe.fluids().size();i++) topUp(m.tanks().input(i),recipe.fluids().get(i).fluid());
    }
    private static void topUp(io.github.jimbozoomer.jugcraft.chemistry.FluidTank tank,net.minecraft.world.level.material.Fluid fluid) {
        int needed=Math.min(2000,tank.capacityMb())-tank.millibuckets();
        if(needed<0 || !tank.fits(fluid,Math.max(0,needed))) throw new AssertionError("Invalid showroom tank contents");
        if(needed>0) tank.fill(fluid,needed);
    }
    private static void shoot(ClientGameTestContext c,TestSingleplayerContext w,double x,int y,double z,double tx,double ty,double tz,String name) {
        w.getServer().runCommand("setblock %d %d %d minecraft:barrier".formatted((int)Math.floor(x),y-1,(int)Math.floor(z)));
        w.getServer().runCommand(String.format(Locale.ROOT,"tp @p %.2f %d %.2f facing %.2f %.2f %.2f",x,y,z,tx,ty-1.62,tz));
        c.waitTicks(15);
        w.getConnection().waitForChunksRender();
        c.takeScreenshot(name);
    }
}

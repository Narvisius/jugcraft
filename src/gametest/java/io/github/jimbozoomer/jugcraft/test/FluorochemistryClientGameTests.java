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
public class FluorochemistryClientGameTests implements FabricClientGameTest {
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
            List<FormMachineBlock> blocks = List.of(IndustrialForms.ACID_CONCENTRATOR, IndustrialForms.CHEMICAL_DISSOLUTION_CHAMBER, IndustrialForms.FLUOROCARBON_REACTOR, IndustrialForms.FLUOROMONOMER_CRACKER, IndustrialForms.PTFE_SINTERING_OVEN, IndustrialForms.LINED_FLUOROCHEMICAL_REACTOR, IndustrialForms.MINERAL_CONDITIONER, IndustrialForms.MONOMER_PREPARATION_UNIT, IndustrialForms.POLYMERIZATION_KETTLE, IndustrialForms.POLYMER_MOLDING_PRESS, IndustrialForms.POLYMER_EXTRUDER, IndustrialForms.POLYMER_RECLAIMER, IndustrialForms.COASTAL_SEAWATER_INTAKE, IndustrialForms.BRINE_CONCENTRATOR, IndustrialForms.CHLORIDE_DRYER, IndustrialForms.MAGNESIUM_ELECTROLYSIS_CELL, IndustrialForms.TITANIUM_CHLORINATOR, IndustrialForms.TITANIUM_REDUCTION_RETORT);
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
                shoot(context,world,cx-dx,cameraY,cz+dz,cx,cy,cz,"fluorochemistry_"+blocks.get(i).form().id().getPath()+"_idle");
                float idle=angle(context,pos);
                context.waitTicks(7);
                if(Math.abs(angle(context,pos)-idle)>.01f) throw new AssertionError("Paused machine still rotates: "+blocks.get(i).form().id());
                server.runOnServer(s -> machine(s.overworld(),pos).togglePause());
                for (int view=0;view<4;view++) {
                    server.runOnServer(s -> stock(s.overworld(),pos,which));
                    double[][] at={{cx-dx,cz+dz},{cx+dx,cz+dz},{cx+dx,cz-dz},{cx-dx,cz-dz}};
                    shoot(context,world,at[view][0],cameraY,at[view][1],cx,cy,cz,"fluorochemistry_"+blocks.get(i).form().id().getPath()+"_working_"+view);
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
                context.waitForScreen(FormMachineScreen.class);context.takeScreenshot("fluorochemistry_menu_"+i);context.setScreen(()->null);
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

                java.util.ArrayList<ItemStack> supplies=new java.util.ArrayList<>();
                for(var block:blocks)supplies.add(new ItemStack(block));
                for(String id:List.of("fluorite_concentrate", "calcium_sulfate", "ptfe_powder", "ptfe_process_insert", "ptfe_paste_charge", "ptfe_seal_blank", "ptfe_fitting_blank", "ptfe_liner_blank", "ptfe_seal", "ptfe_fitting", "ptfe_liner", "ptfe_offcut", "fluorite", "hdpe_pellets", "ldpe_pellets", "pvc_resin", "rubber", "hdpe_offcut", "ldpe_offcut", "pvc_offcut", "rubber_offcut", "hdpe_process_insert", "ldpe_process_insert", "gasket_mold", "fitting_mold", "panel_mold", "housing_mold", "sheet_extrusion_die", "film_extrusion_die", "profile_extrusion_die", "hose_extrusion_die", "insulation_extrusion_die", "gas_cylinder", "steel_fluid_pipe", "copper_cable", "fluorite_ore", "deepslate_fluorite_ore", "fluorite_block")) supplies.add(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id(id)),64));
                for(var fluid:List.of(PetroFluids.CONCENTRATED_SULFURIC_ACID,PetroFluids.CHLOROFORM,PetroFluids.BIOETHANOL,PetroFluids.SULFURIC_ACID,PetroFluids.LUBRICANT))supplies.add(new ItemStack(fluid.bucket()));
                for(int i=0;i<supplies.size();i++) {
                    BlockPos chest=spawn.offset(6+(i/27)*3,0,36);
                    if(i%27==0)s.overworld().setBlock(chest,Blocks.CHEST.defaultBlockState(),Block.UPDATE_ALL);
                    ((ChestBlockEntity)s.overworld().getBlockEntity(chest)).setItem(i%27,supplies.get(i));
                }
                for(int i=0;i<3;i++)s.overworld().setBlock(spawn.offset(20+i*2,0,36),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id(List.of("fluorite_ore","deepslate_fluorite_ore","fluorite_block").get(i))).defaultBlockState(),Block.UPDATE_ALL);
                io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.info("Fluorochemistry factory manual-test world: {}",s.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT));
            });
            context.waitTicks(8);
            for(int i=0;i<blocks.size();i++){
                BlockPos pos=spawn.offset((i%6)*10,0,(i/6)*14-4);var f=blocks.get(i).form();
                shoot(context,world,pos.getX()-4,y+2,pos.getZ()+5,pos.getX()+f.width()/2.,y+f.height()/2.,pos.getZ()+1-f.depth()/2.,"fluorochemistry_connections_"+i);
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
        String preferred=List.of("concentrate_sulfuric_acid","dissolve_fluorite","fluorinate_chloroform","crack_fluoromonomer","sinter_ptfe_seal","prepare_chloroform","prepare_ptfe_paste","prepare_ethylene_dichloride","polymerize_ptfe","press_ptfe_fitting","extrude_ptfe_liner","reclaim_unsintered_ptfe","","concentrate_coastal_seawater","dry_magnesium_chloride","electrolyze_dry_magnesium_chloride","chlorinate_titanium_mineral","reduce_and_clean_titanium_sponge").get(index);
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

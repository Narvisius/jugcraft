package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;
import org.slf4j.LoggerFactory;

/** Only companion shared storage, routes, tool pickup and the companion menu. */
public final class PeepoSuppliesClientTests implements FabricClientGameTest {
    private BlockPos base;
    private PeepoEntity npc;
    private final Container[] supply=new Container[4],output=new Container[4];
    private int assertions;
    private void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    private int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private BlockPos pos(boolean out,int i){return base.offset(out?4:-4,0,-3+i*2);}
    private void reset(ServerLevel level,boolean jughead){
        if(npc!=null){npc.resetCompanionRoutine();npc.discard();}
        for(var p:BlockPos.betweenClosed(base.offset(-7,-1,-7),base.offset(7,5,7)))level.setBlock(p,p.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        npc=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);
        npc.setNoAi(true);npc.snapTo(base.getX()+.5,base.getY(),base.getZ()+.5,0,0);npc.setOnGround(true);level.addFreshEntity(npc);
        npc.orders.tame(level.getServer().getPlayerList().getPlayers().getFirst());npc.preferences.social=false;npc.preferences.carryMeals=0;
        npc.orders.command(level.getServer().getPlayerList().getPlayers().getFirst(),3);
        for(int i=0;i<4;i++)for(boolean out:new boolean[]{false,true}){
            var p=pos(out,i);level.setBlockAndUpdate(p,Blocks.BARREL.defaultBlockState());
            (out?output:supply)[i]=(Container)level.getBlockEntity(p);npc.assignments.assignContainer(level,p,Direction.UP,out);
        }
    }
    private void ticks(TestServerContext server,int count){long end=server.computeOnServer(s->s.overworld().getGameTime()+count);server.waitFor(s->s.overworld().getGameTime()>=end,count+50);}
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{
            c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            c.setGenerateStructures(false);
        }).create()){
            world.getConnection().waitForChunksRender();context.runOnClient(c->{c.options.guiScale().set(1);c.resizeGui();});var server=world.getServer();base=context.computeOnClient(c->c.player.blockPosition().above(4));
            server.runCommand("time set noon");server.runCommand("weather clear");
            server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f",base.getX()+.5,base.getY(),base.getZ()-5.5));
            server.runOnServer(s->configuration(s.overworld()));
            for(boolean jughead:new boolean[]{false,true})porter(server,jughead);
            tools(server);
            menu(context,server);
            server.runOnServer(s->{npc.resetCompanionRoutine();npc.discard();});
        }
        LoggerFactory.getLogger("peepo-supplies-test").info("[peepo-supplies-test] PASS {} assertions",assertions);
    }
    private void configuration(ServerLevel level){
        reset(level,false);
        for(int i=0;i<4;i++)for(boolean out:new boolean[]{false,true})check(npc.assignments.get(CompanionAssignments.containerSlot(out,i)).at().pos().equals(pos(out,i)),"four stable containers for each role");
        var extra=base.north(6);level.setBlockAndUpdate(extra,Blocks.BARREL.defaultBlockState());
        check(npc.assignments.assignContainer(level,extra,Direction.UP,false).contains("full"),"fifth source must not replace an assignment");
        var original=npc.assignments.get(6);check(npc.assignments.cycleContainer(level,pos(false,0),Direction.NORTH).contains("full") && npc.assignments.get(6).equals(original),"full role switch is atomic");
        npc.assignments.clear(13);npc.assignments.cycleContainer(level,extra,Direction.UP);
        check(npc.assignments.get(13).at().pos().equals(extra),"new containers can fill Output after all Supplies are assigned");
        npc.assignments.clear(9);npc.assignments.cycleContainer(level,pos(false,0),Direction.NORTH);
        check(npc.assignments.get(6)==null && npc.assignments.get(9).at().pos().equals(pos(false,0)) && npc.assignments.get(9).face()==Direction.NORTH,"switch only clicked container and clicked face");
        reset(level,false);
        for(int i=0;i<2;i++){var p=base.offset(i*2,0,4);level.setBlockAndUpdate(p,JugcraftAgriculture.block("cooking_pot").defaultBlockState());npc.assignments.assign(level,p);}
        var work=npc.assignments.get(1);check(npc.assignments.supplies.containers(work,false).size()==4,"all supplies default");
        npc.assignments.supplies.link(1,false,1);
        check(npc.assignments.supplies.containers(work,false).size()==1 && npc.assignments.supplies.containers(work,false).getFirst().equals(npc.assignments.get(6)),"explicit subset");
        npc.assignments.moveWork(1,1);check(npc.assignments.supplies.mask(2,false)==17,"job links follow priority movement");
        npc.assignments.clear(1);check(npc.assignments.supplies.mask(1,false)==17,"job links follow compaction");
        npc.assignments.supplies.toggleTools(2);npc.assignments.supplies.toggleHandLock();
        var r=npc.assignments.supplies.route(2);r.source=2;r.destination=3;r.enabled=true;r.leave=16;r.keep=64;
        var ghost=new ItemStack(Items.DIAMOND,7);ghost.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Real item"));r.filter(0,ghost);
        check(r.icon(0).getCount()==1 && !r.icon(0).has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) && ghost.getCount()==7,"ghost keeps only type; original stack untouched");
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());npc.saveWithoutId(saved);
        var copy=PeepoMod.PEEPO.create(level,EntitySpawnReason.COMMAND);copy.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),saved.buildResult()));
        check(copy.assignments.supplies.tools(2) && copy.assignments.supplies.handLocked() && copy.assignments.supplies.mask(1,false)==17,"settings survive reload");
        var rr=copy.assignments.supplies.route(2);check(rr.enabled && rr.source==2 && rr.destination==3 && rr.leave==16 && rr.keep==64 && rr.icon(0).getCount()==1,"route and ghost filter survive reload");
        npc.assignments.clear(10);check(!r.enabled && !npc.assignments.supplies.tools(2),"removal disables referencing route and tools flag");
        var old=saved.buildResult();old.remove("SharedSupplies");old.putInt("CompanionCommand",4);copy.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),old));
        check(copy.orders.mode()==3 && copy.assignments.supplies.route(0).enabled,"legacy dedicated porter migrates to Work with an explicit route");
        npc.belongings.setItem(0,new ItemStack(Items.IRON_HOE));npc.belongings.setItem(9,new ItemStack(Items.TORCH));
        try(var tx=Transaction.openOuter()){check(npc.food.equipCargo(0,new ItemStack(Items.IRON_HOE),tx),"equip actual cargo");}
        check(npc.belongings.getItem(9).is(Items.TORCH) && npc.belongings.getItem(0).is(Items.IRON_HOE),"tool transaction rollback restores hand and cargo");
        // Combined planning sees materials spread across the configured sources.
        npc.assignments.supplies.link(1,false,0);supply[0].setItem(0,new ItemStack(Items.CARROT,3));supply[1].setItem(0,new ItemStack(Items.POTATO,4));
        var joined=npc.assignments.supplies.combined(work,false);
        try(var tx=Transaction.openOuter()){check(joined.extract(ItemVariant.of(Items.CARROT),3,tx)==3 && joined.extract(ItemVariant.of(Items.POTATO),4,tx)==4,"combined inventory exposes split ingredients");}
        check(count(supply[0],Items.CARROT)==3 && count(supply[1],Items.POTATO)==4,"planning probe does not consume items");
        for(var chest:supply)chest.clearContent();
        var pot=(CookingPotBlockEntity)level.getBlockEntity(work.at().pos());
        var recipe=CookingPotRecipe.catalog(level.getServer()).entrySet().stream().filter(e->e.getValue().parts().size()>=2 && e.getValue().parts().size()<=6).findFirst().orElseThrow();
        pot.filter().set(0,recipe.getValue().output().create());pot.companionFilterChanged();
        int part=0;for(var ingredient:recipe.getValue().parts())supply[part++%2].setItem(part/2,new ItemStack(ingredient.ingredient().items().findFirst().orElseThrow(),ingredient.count()));
        var port=CompanionLogistics.resolve(npc,work);port.prepare(npc.assignments.supplies.combined(work,false));
        check(port.plan()!=null,"real pot finds an allowed recipe with ingredients split across sources");
        var largeSources=new ArrayList<net.fabricmc.fabric.api.transfer.v1.storage.Storage<ItemVariant>>();
        for(int i=0;i<4;i++){var chest=new SimpleContainer(54);chest.setItem(53,new ItemStack(i==3?Items.EMERALD:Items.STONE));largeSources.add(net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(chest,Direction.UP));}
        check(RecipeSupplies.available(new CompanionSupplies.SharedStorage(largeSources)).stream().anyMatch(stack->stack.is(Items.EMERALD)),"four double-chest-sized inventories include the last slot of the fourth source");
    }
    private void porter(TestServerContext server,boolean jughead){
        server.runOnServer(s->{reset(s.overworld(),jughead);var job=base.south(4);s.overworld().setBlockAndUpdate(job,JugcraftAgriculture.block("cooking_pot").defaultBlockState());npc.assignments.assign(s.overworld(),job);supply[2].setItem(0,new ItemStack(Items.DIAMOND,64));supply[2].setItem(1,new ItemStack(Items.DIRT,64));
            var r=npc.assignments.supplies.route(0);r.source=2;r.destination=3;r.enabled=true;r.leave=16;r.keep=32;r.filter(0,new ItemStack(Items.DIAMOND));npc.setNoAi(false);});
        server.waitFor(s->count(output[3],Items.DIAMOND)==32,900);
        ticks(server,130);
        server.runOnServer(s->{check(count(supply[2],Items.DIAMOND)==32 && count(supply[2],Items.DIRT)==64 && count(output[3],Items.DIRT)==0,"filter and destination cap "+jughead);npc.assignments.supplies.route(0).keep=0;});
        server.waitFor(s->count(output[3],Items.DIAMOND)==48,800);ticks(server,110);
        server.runOnServer(s->{check(count(supply[2],Items.DIAMOND)==16 && count(npc.belongings,Items.DIAMOND)==0,"source reserve and physical item conservation "+jughead);});
    }
    private void tools(TestServerContext server){
        final CuttingBoardBlockEntity[] board={null};
        server.runOnServer(s->{reset(s.overworld(),true);var p=base.south(3);s.overworld().setBlockAndUpdate(p,JugcraftAgriculture.block("cutting_board").defaultBlockState());board[0]=(CuttingBoardBlockEntity)s.overworld().getBlockEntity(p);board[0].put(new ItemStack(Items.PORKCHOP));npc.assignments.assign(s.overworld(),p);
            for(int i=0;i<output[0].getContainerSize();i++)output[0].setItem(i,new ItemStack(Items.STONE,64));
            supply[1].setItem(0,new ItemStack(JugcraftAgriculture.item("iron_knife")));npc.assignments.supplies.toggleTools(1);npc.belongings.setItem(9,new ItemStack(Items.TORCH));npc.assignments.supplies.toggleHandLock();npc.setNoAi(false);});
        ticks(server,130);
        server.runOnServer(s->{check(npc.belongings.getItem(9).is(Items.TORCH) && !supply[1].getItem(0).isEmpty(),"Hand lock protects torch");npc.assignments.supplies.toggleHandLock();});
        server.waitFor(s->board[0].item().isEmpty(),1100);
        server.waitFor(s->count(output[1],JugcraftAgriculture.item("bacon"))==2,600);
        server.runOnServer(s->{check(npc.belongings.getItem(9).is(JugcraftAgriculture.KNIVES) && count(npc.belongings,Items.TORCH)==1 && supply[1].getItem(0).isEmpty(),"walks to tool chest, preserves torch and uses equipped knife");});
    }
    private void menu(ClientGameTestContext context,TestServerContext server){
        server.runOnServer(s->{npc.setNoAi(true);npc.resetCompanionRoutine();npc.snapTo(base.getX()+.5,base.getY(),base.getZ()-3.5,0,0);var owner=s.getPlayerList().getPlayers().getFirst();owner.openMenu(new SimpleMenuProvider((id,inv,p)->new CompanionMenu(id,inv,npc),npc.getDisplayName()));});
        context.waitForScreen(local.peepo.client.CompanionScreen.class);
        context.runOnClient(c->{for(var child:c.gui.screen().children())if(child instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("Supplies"))b.onPress(null);});
        context.waitTicks(5);context.takeScreenshot("peepo_shared_supplies");
        context.runOnClient(c->{for(var child:c.gui.screen().children())if(child instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("Job links"))b.onPress(null);});
        context.waitTicks(5);context.takeScreenshot("peepo_job_storage_links");
        context.runOnClient(c->c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,201));
        server.waitFor(s->npc.assignments.supplies.mask(1,false)==17,100);ticks(server,3);
        context.runOnClient(c->c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,200));
        server.waitFor(s->npc.assignments.supplies.mask(1,false)==0,100);ticks(server,3);
        context.runOnClient(c->{for(var child:c.gui.screen().children())if(child instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("Porter routes"))b.onPress(null);});
        context.waitTicks(5);context.takeScreenshot("peepo_shared_porter_routes");
        context.runOnClient(c->c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,303));
        context.waitFor(c->c.player.containerMenu instanceof CompanionMenu m && m.filterRow()==4,100);
        server.runOnServer(s->{var menu=(CompanionMenu)s.getPlayerList().getPlayers().getFirst().containerMenu;menu.setCarried(new ItemStack(Items.DIAMOND,5));menu.broadcastChanges();});
        context.waitFor(c->c.player.containerMenu.getCarried().getCount()==5,100);
        context.runOnClient(c->c.gameMode.handleContainerInput(c.player.containerMenu.containerId,50,0,net.minecraft.world.inventory.ContainerInput.PICKUP,c.player));
        server.waitFor(s->npc.assignments.supplies.route(0).icon(0).is(Items.DIAMOND),100);
        check(context.computeOnClient(c->c.player.containerMenu.getCarried().getCount()==5),"route ghost leaves cursor untouched");
        context.waitTicks(5);context.takeScreenshot("peepo_porter_filter_stock_limits");
        context.runOnClient(c->c.player.closeContainer());context.waitForScreen(null);
    }
}

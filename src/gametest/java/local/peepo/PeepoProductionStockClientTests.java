package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.*;
import org.slf4j.LoggerFactory;

/** Stock targets only: real menus, bounded planning, persisted cargo and physical kitchen work. */
public final class PeepoProductionStockClientTests implements FabricClientGameTest {
    private BlockPos base;
    private PeepoEntity npc,other;
    private BlockEntity station;
    private Container source,output;
    private int assertions;
    private final Item soup=JugcraftAgriculture.item("tomato_soup"),tomato=JugcraftAgriculture.item("tomato"),onion=JugcraftAgriculture.item("onion"),bacon=JugcraftAgriculture.item("bacon");
    private void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    private int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private int count(List<ItemStack> list,Item item){return list.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    private CompanionProductionStock stock(){return CompanionProductionStock.of(station);}
    private CompanionRecipeFilter filter(){return CompanionFilters.get(station);}
    private CompanionAssignments.Target work(){return npc.assignments.get(1);}
    private CompanionLogistics.Port port(){return CompanionLogistics.resolve(npc,work());}
    private void reset(ServerLevel level,String kind,boolean jughead){
        LoggerFactory.getLogger("peepo-production-stock-test").info("Stock scenario: {} / {}",kind,jughead?"Jughead":"Peepo");
        for(var p:new PeepoEntity[]{npc,other})if(p!=null){p.transport.releaseStock();p.transport.stop();p.resetCompanionRoutine();p.discard();}
        npc=other=null;
        for(var p:BlockPos.betweenClosed(base.offset(-8,-1,-7),base.offset(8,4,7)))level.setBlock(p,p.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        level.setBlockAndUpdate(base,JugcraftAgriculture.block(kind).defaultBlockState());station=level.getBlockEntity(base);
        level.setBlockAndUpdate(base.west(5),Blocks.BARREL.defaultBlockState());source=(Container)level.getBlockEntity(base.west(5));
        level.setBlockAndUpdate(base.east(5),Blocks.BARREL.defaultBlockState());output=(Container)level.getBlockEntity(base.east(5));
        npc=helper(level,jughead,-2);
        if(station instanceof CookingPotBlockEntity pot)pot.selectRecipe(Jugcraft.id("pot_cooking/tomato_soup"));
    }
    private PeepoEntity helper(ServerLevel level,boolean jughead,int z){
        var p=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);
        p.setNoAi(true);p.snapTo(base.getX()-2.5,base.getY(),base.getZ()+z+.5,0,0);p.setOnGround(true);level.addFreshEntity(p);
        var owner=level.getServer().getPlayerList().getPlayers().getFirst();p.orders.tame(owner);p.orders.command(owner,3);p.preferences.social=false;p.preferences.carryMeals=0;
        p.assignments.assign(level,base);p.assignments.assignContainer(level,base.west(5),Direction.UP,false);p.assignments.assignContainer(level,base.east(5),Direction.UP,true);
        p.belongings.setItem(9,new ItemStack(JugcraftAgriculture.item("iron_knife")));return p;
    }
    private void goal(Item item,int target,int restock){filter().set(0,new ItemStack(item));stock().useOutputs(npc,work());stock().set(new ItemStack(item),target,restock);station.setChanged();}
    private void ingredients(int batches){source.setItem(0,new ItemStack(Items.BOWL,batches));source.setItem(1,new ItemStack(tomato,batches*2));source.setItem(2,new ItemStack(onion,batches));}
    private void carry(PeepoEntity worker,boolean input,List<ItemStack> stacks){
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,worker.registryAccess());
        for(int i=0;i<8;i++)worker.belongings.setItem(i,i<stacks.size()?stacks.get(i).copy():ItemStack.EMPTY);
        if(!stacks.isEmpty()){
            var c=out.child("Transport");c.store("Manifest",ItemStack.CODEC,stacks.getFirst());c.putInt("Slot",0);c.putBoolean("Supply",input);
            CompanionTransport.saveTarget(c,"Work",worker.assignments.get(1));CompanionTransport.saveTarget(c,"Store",worker.assignments.get(input?CompanionAssignments.SUPPLY:CompanionAssignments.OUTPUT));
            if(station instanceof CookingPotBlockEntity pot)c.store("Recipe",net.minecraft.resources.Identifier.CODEC,pot.selectedRecipe());
            for(int i=1;i<stacks.size();i++){var p=c.child("Pending"+(i-1));p.putInt("Slot",i);p.store("Stack",ItemStack.CODEC,stacks.get(i));}
        }
        worker.transport.load(TagValueInput.create(ProblemReporter.DISCARDING,worker.registryAccess(),out.buildResult()));
    }
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{
            c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));c.setGenerateStructures(false);
        }).create()){
            world.getConnection().waitForChunksRender();context.runOnClient(c->{c.options.guiScale().set(1);c.resizeGui();});var server=world.getServer();base=context.computeOnClient(c->c.player.blockPosition().above(4));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f",base.getX()+.5,base.getY(),base.getZ()-4.5));
            server.runOnServer(s->{planning(s.overworld());boardAndOven(s.overworld());doubleChest(s.overworld());savedCarrier(s.overworld());recipePriority(s.overworld());});
            menu(context,server);
            for(boolean jughead:new boolean[]{false,true}){potCycle(server,jughead);boardCycle(server,jughead);ovenCycle(server,jughead);}
            server.runOnServer(s->{npc.transport.releaseStock();npc.discard();if(other!=null)other.discard();});
        }
        LoggerFactory.getLogger("peepo-production-stock-test").info("[peepo-production-stock-test] PASS {} assertions",assertions);
    }
    private void planning(ServerLevel level){
        reset(level,"cooking_pot",false);ingredients(16);var pot=(CookingPotBlockEntity)station;
        var storage=ContainerStorage.of(source,Direction.UP);
        check(count(port().supplyBatch(storage,8),Items.BOWL)==16,"Unlimited keeps the existing bulk plan");
        goal(soup,4,1);output.setItem(0,new ItemStack(soup));stock().invalidate();
        var batch=port().supplyBatch(storage,8);
        check(count(batch,Items.BOWL)==3 && count(batch,tomato)==6 && count(batch,onion)==3,"stock goal bounds a complete bulk recipe");
        check(count(source,tomato)==32 && pot.isEmpty(),"planning never consumes source or installs ghost ingredients");
        other=helper(level,true,2);carry(npc,true,batch);
        var otherPort=CompanionLogistics.resolve(other,other.assignments.get(1));
        check(otherPort.supplyBatch(storage,8).isEmpty(),"second helper counts inbound ingredients instead of duplicating a batch");
        check(stock().count(other,pot,new ItemStack(soup)).underway()==3,"inbound inputs appear as expected finished products");
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());filter().save(saved);
        filter().load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),saved.buildResult()));
        check(stock().target(new ItemStack(soup))==4 && stock().restock(new ItemStack(soup))==1 && stock().outputs().size()==1,"targets, thresholds and counted containers survive reload");
        check(otherPort.supplyBatch(storage,8).isEmpty(),"reload retains registered incoming cargo");
        carry(npc,false,List.of(new ItemStack(soup),new ItemStack(soup),new ItemStack(soup)));
        check(!stock().wants(other,pot,new ItemStack(soup)),"outbound finished goods also count before delivery");
        carry(npc,false,List.of());
        output.setItem(1,new ItemStack(soup));output.setItem(2,new ItemStack(soup));output.setItem(3,new ItemStack(soup));stock().invalidate();
        check(!stock().wants(npc,pot,new ItemStack(soup)),"stocked target stops new production");
        pot.selectRecipe(null);check(stock().idleStatus(npc,pot)==CompanionStatus.STOCKED,"stocked pot remains safe after clearing its selected recipe");pot.selectRecipe(Jugcraft.id("pot_cooking/tomato_soup"));
        output.setItem(3,ItemStack.EMPTY);stock().invalidate();check(!stock().wants(npc,pot,new ItemStack(soup)),"taking one item does not restart a stocked batch");
        var stopped=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());filter().save(stopped);filter().load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),stopped.buildResult()));
        check(!stock().wants(npc,pot,new ItemStack(soup)),"restock latch survives reload above restart threshold");
        output.setItem(1,ItemStack.EMPTY);output.setItem(2,ItemStack.EMPTY);stock().invalidate();check(stock().wants(npc,pot,new ItemStack(soup)),"restarts exactly at the configured threshold");
        var outPos=base.east(5);level.setBlockAndUpdate(outPos,Blocks.AIR.defaultBlockState());
        check(port().supplyBatch(storage,8).isEmpty() && stock().idleStatus(npc,pot)==CompanionStatus.STOCK_UNAVAILABLE,"missing counted Output never masquerades as empty stock");
        level.setBlockAndUpdate(outPos,Blocks.BARREL.defaultBlockState());output=(Container)level.getBlockEntity(outPos);stock().invalidate();
        pot.setItem(0,new ItemStack(Items.BOWL,4));pot.setItem(1,new ItemStack(tomato,8));pot.setItem(2,new ItemStack(onion,4));
        check(port().supplyBatch(storage,8).isEmpty(),"loaded pot batches count against replenishment");
        pot.clearContent();stock().set(new ItemStack(soup),0,0);check(count(port().supplyBatch(storage,8),Items.BOWL)==16,"Unlimited restores full bulk pickup");
        stock().set(new ItemStack(soup),4,1);filter().set(0,ItemStack.EMPTY);check(!stock().enabled(),"clearing an item removes its target");
    }
    private void boardAndOven(ServerLevel level){
        reset(level,"cutting_board",false);goal(bacon,3,1);source.setItem(0,new ItemStack(Items.PORKCHOP,8));
        output.setItem(0,new ItemStack(bacon,2));stock().invalidate();
        var batch=port().supplyBatch(ContainerStorage.of(source,Direction.UP),8);check(count(batch,Items.PORKCHOP)==1,"board allows whole recipe batch for one missing bacon");
        check(((CuttingBoardBlockEntity)station).item().isEmpty(),"board input simulation rolls back");
        other=helper(level,true,2);carry(npc,true,batch);
        check(CompanionLogistics.resolve(other,other.assignments.get(1)).pickupNeeded(new ItemStack(Items.PORKCHOP))==0,"board pickup is reserved across helpers");
        carry(npc,false,List.of());output.setItem(0,new ItemStack(bacon,4));stock().invalidate();
        check(port().supplyBatch(ContainerStorage.of(source,Direction.UP),8).isEmpty(),"batch surplus stops another cut");
        reset(level,"hearth_oven",true);var oven=(HearthOvenBlockEntity)station;var filling=PieFilling.values()[0];
        var raw=JugcraftAgriculture.item(filling.rawPie());var baked=JugcraftAgriculture.item(filling.pie());goal(baked,1,0);
        source.setItem(0,new ItemStack(raw,3));source.setItem(1,new ItemStack(Items.COAL,8));
        batch=port().supplyBatch(ContainerStorage.of(source,Direction.UP),8);
        check(count(batch,raw)==1 && count(batch,Items.COAL)>0,"oven takes one raw pie and its fuel together");
        check(oven.pie()==null && oven.burn()==0,"oven pickup probe restores pie and fuel");
        other=helper(level,false,2);carry(npc,true,batch);
        check(CompanionLogistics.resolve(other,other.assignments.get(1)).pickupNeeded(new ItemStack(raw))==0,"second helper does not fetch a second reserved pie");
        carry(npc,false,List.of());oven.set(0,0,filling,0);output.setItem(0,new ItemStack(baked));stock().invalidate();
        check(port().pickupNeeded(new ItemStack(Items.COAL))>0,"already loaded pie can still get fuel when stock is full");
        oven.set(500,100,filling,HearthOvenBlockEntity.BAKED);
        check(stock().count(npc,oven,new ItemStack(baked)).underway()==1,"finished oven pie counts once, not as both raw and cooked");
        try(var tx=Transaction.openOuter()){check(port().outputs().extract(ItemVariant.of(baked),1,tx)==1,"full target does not block collecting completed food");}
    }
    private void doubleChest(ServerLevel level){
        reset(level,"cooking_pot",false);goal(soup,4,1);var p=base.east(5);
        level.setBlockAndUpdate(p,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.LEFT));level.setBlockAndUpdate(p.east(),Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.RIGHT));
        npc.assignments.clear(CompanionAssignments.OUTPUT);npc.assignments.assignContainer(level,p,Direction.UP,true);stock().useOutputs(npc,work());
        ((Container)level.getBlockEntity(p)).setItem(0,new ItemStack(soup));
        ((Container)level.getBlockEntity(p.east())).setItem(0,new ItemStack(soup));
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());filter().save(saved);
        var data=saved.buildResult();var alias=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
        CompanionTransport.saveTarget(alias,"Output1",new CompanionAssignments.Target(GlobalPos.of(level.dimension(),p.east()),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(Blocks.CHEST),Direction.UP));
        data.getCompound("ProductionStock").orElseThrow().merge(alias.buildResult());
        filter().load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data));
        check(stock().count(npc,station,new ItemStack(soup)).stored()==2,"double chest halves are counted exactly once");
    }
    private void savedCarrier(ServerLevel level){
        reset(level,"cooking_pot",false);goal(soup,2,0);ingredients(8);
        carry(npc,true,List.of(new ItemStack(Items.BOWL,2),new ItemStack(tomato,4),new ItemStack(onion,2)));
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());filter().save(saved);
        var data=saved.buildResult();var absent=UUID.randomUUID();
        data.getCompound("ProductionStock").orElseThrow().getCompound("Delivery0").orElseThrow().putString("Worker",absent.toString());
        carry(npc,false,List.of());filter().load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data));
        check(!stock().count(npc,station,new ItemStack(soup)).available(),"saved unloaded carrier pauses counting rather than duplicating cargo");
        var cancellations=ProductionDeliveryCancellations.get(level.getServer());var at=GlobalPos.of(level.dimension(),base);
        cancellations.cancel(at,absent);cancellations.cancel(at,absent);
        check(stock().count(npc,station,new ItemStack(soup)).available() && stock().incoming().isEmpty(),"deferred removal releases an unavailable carrier without loading its chunk");
        check(cancellations.take(at).isEmpty(),"deferred cancellation is consumed once");
        check(count(port().supplyBatch(ContainerStorage.of(source,Direction.UP),8),Items.BOWL)==2,"production resumes after cancelled delivery is reconciled");
    }
    private void recipePriority(ServerLevel level){
        reset(level,"cooking_pot",false);ingredients(8);goal(soup,4,1);
        var vegetable=JugcraftAgriculture.item("vegetable_soup");var pot=(CookingPotBlockEntity)station;
        filter().set(1,new ItemStack(vegetable));stock().set(new ItemStack(vegetable),4,1);
        source.setItem(3,new ItemStack(JugcraftAgriculture.item("cabbage"),4));source.setItem(4,new ItemStack(Items.CARROT,4));source.setItem(5,new ItemStack(Items.POTATO,4));
        for(int i=0;i<3;i++)output.setItem(i,new ItemStack(soup));stock().invalidate();var storage=ContainerStorage.of(source,Direction.UP);
        port().prepare(storage);check(Jugcraft.id("pot_cooking/vegetable_soup").equals(pot.selectedRecipe()),"available recipe with greatest proportional shortage wins");
        pot.setItem(0,new ItemStack(Items.BOWL));for(int i=4;i<8;i++)output.setItem(i,new ItemStack(vegetable));stock().invalidate();
        port().prepare(storage);check(Jugcraft.id("pot_cooking/vegetable_soup").equals(pot.selectedRecipe()),"target edits and other demand do not switch a loaded recipe");
        pot.clearContent();port().prepare(storage);check(Jugcraft.id("pot_cooking/tomato_soup").equals(pot.selectedRecipe()),"stocked recipe yields to another product below target");
        output.setItem(3,new ItemStack(soup));stock().invalidate();stock().set(new ItemStack(vegetable),0,0);
        port().prepare(storage);check(Jugcraft.id("pot_cooking/vegetable_soup").equals(pot.selectedRecipe()),"Unlimited recipe stays available while limited products are stocked");
        other=helper(level,true,2);other.assignments.clear(CompanionAssignments.OUTPUT);
        level.setBlockAndUpdate(base.east(6),Blocks.BARREL.defaultBlockState());other.assignments.assignContainer(level,base.east(6),Direction.UP,true);
        check(!stock().count(other,pot,new ItemStack(soup)).available(),"helper without a route to counted Outputs cannot silently use a different stock pool");
        check(stock().outputs().getFirst().at().pos().equals(base.east(5)),"another helper does not replace shared counted Outputs");
    }
    private void menu(ClientGameTestContext context,TestServerContext server){
        server.runOnServer(s->{reset(s.overworld(),"cooking_pot",false);goal(soup,64,32);var p=s.getPlayerList().getPlayers().getFirst();
            p.openMenu(new SimpleMenuProvider((id,inv,player)->new CompanionMenu(id,inv,npc),npc.getDisplayName()));
        });
        context.waitForScreen(local.peepo.client.CompanionScreen.class);
        context.runOnClient(c->c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,60));
        context.waitFor(c->c.player.containerMenu instanceof CompanionMenu m && m.stockView!=null && m.stockView.entries().getFirst().target()==64,100);
        context.runOnClient(c->{var m=(CompanionMenu)c.player.containerMenu;var v=m.stockView;
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new CompanionStockEditPayload(m.containerId,v.row(),v.anchor(),v.revision(),0,1,8,2));
        });
        server.waitFor(s->stock().target(new ItemStack(soup))==8,100);
        context.waitFor(c->((CompanionMenu)c.player.containerMenu).stockView.entries().getFirst().target()==8,100);
        context.takeScreenshot("peepo_production_stock_filter");
        server.runOnServer(s->{var player=s.getPlayerList().getPlayers().getFirst();var menu=(CompanionMenu)player.containerMenu;int revision=stock().revision();
            check(!menu.editStock(player,new CompanionStockEditPayload(menu.containerId,0,base,revision,0,1,3,3)),"server rejects invalid restart threshold");
            check(!menu.editStock(player,new CompanionStockEditPayload(menu.containerId,0,base,revision-1,0,1,9,2)),"server rejects stale target edit");
            check(!menu.editStock(player,new CompanionStockEditPayload(menu.containerId,0,base.above(),revision,0,1,9,2)),"server rejects wrong station identity");
            check(!menu.editStock(player,new CompanionStockEditPayload(menu.containerId,0,base,revision,10,1,9,2)),"server rejects oversized slot index");
            check(!menu.editStock(player,new CompanionStockEditPayload(menu.containerId,0,base,revision,0,1,5000,2)),"server rejects excessive target");
            menu.setCarried(ItemStack.EMPTY);menu.clicked(CompanionMenu.FILTER_START,0,ContainerInput.PICKUP,player);
            check(!filter().get(0).isEmpty() && menu.getCarried().isEmpty(),"empty-hand left click selects a ghost without clearing or creating an item");
            check(menu.slots.size()==59,"stock controls add no real inventory slots");player.closeContainer();
        });
        context.waitFor(c->!(c.player.containerMenu instanceof CompanionMenu),100);
    }
    private void ticks(TestServerContext server,int amount){long until=server.computeOnServer(s->s.overworld().getGameTime()+amount);server.waitFor(s->s.overworld().getGameTime()>=until,amount+100);}
    private void potCycle(TestServerContext server,boolean jughead){
        server.runOnServer(s->{reset(s.overworld(),"cooking_pot",jughead);goal(soup,2,0);ingredients(8);s.overworld().setBlockAndUpdate(base.below(),Blocks.MAGMA_BLOCK.defaultBlockState());npc.setNoAi(false);});
        server.waitFor(s->count(output,soup)==2,2200);ticks(server,120);
        server.runOnServer(s->{check(count(source,Items.BOWL)==6 && count(source,tomato)==12,"real pot work stops after target for "+jughead);check(count(npc.belongings,soup)==0,"real pot outputs delivered");output.setItem(0,ItemStack.EMPTY);stock().invalidate();});
        ticks(server,120);server.runOnServer(s->check(count(source,Items.BOWL)==6,"pot stays stopped above restart threshold"));
        server.runOnServer(s->{output.clearContent();stock().invalidate();});server.waitFor(s->count(output,soup)==2,2200);
        server.runOnServer(s->check(count(source,Items.BOWL)==4,"pot resumes after stock reaches threshold"));
    }
    private void boardCycle(TestServerContext server,boolean jughead){
        server.runOnServer(s->{reset(s.overworld(),"cutting_board",jughead);goal(bacon,3,1);source.setItem(0,new ItemStack(Items.PORKCHOP,8));npc.setNoAi(false);});
        server.waitFor(s->count(output,bacon)==4,1700);ticks(server,100);
        server.runOnServer(s->check(count(source,Items.PORKCHOP)==6,"board makes complete two-item batches and stops for "+jughead));
    }
    private void ovenCycle(TestServerContext server,boolean jughead){
        var filling=PieFilling.values()[jughead?1:0];var raw=JugcraftAgriculture.item(filling.rawPie());var baked=JugcraftAgriculture.item(filling.pie());
        server.runOnServer(s->{reset(s.overworld(),"hearth_oven",jughead);goal(baked,1,0);source.setItem(0,new ItemStack(raw,3));source.setItem(1,new ItemStack(Items.COAL,8));npc.setNoAi(false);});
        server.waitFor(s->count(output,baked)==1,1800);ticks(server,100);
        server.runOnServer(s->{check(count(source,raw)==2 && ((HearthOvenBlockEntity)station).pie()==null,"oven target stops further pies for "+jughead);output.clearContent();stock().invalidate();});
        server.waitFor(s->count(output,baked)==1,1800);
        server.runOnServer(s->check(count(source,raw)==1,"oven restarts when its pie is taken"));
    }
}

package local.peepo;

import java.util.*;
import com.mojang.authlib.GameProfile;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.farming.*;
import io.github.jimbozoomer.jugcraft.finance.*;
import io.github.jimbozoomer.jugcraft.workshop.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;

/** Only the new companion claims, fertilizer blends and starter workshop integration. */
public final class PeepoWorkshopClientTests implements FabricClientGameTest {
    private BlockPos base;
    private PeepoEntity npc;
    private WorkshopBlockEntity station;
    private Container source,output;
    private WorkshopRecipe recipe;
    private int assertions;
    private void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    private void phase(String text){LoggerFactory.getLogger("peepo-workshop-test").info("[peepo-workshop-test] {}",text);}
    private int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private void setup(ServerLevel level,int kind,Direction facing,boolean jughead){
        if(npc!=null){npc.transport.stop();npc.resetCompanionRoutine();npc.discard();}
        for(var p:BlockPos.betweenClosed(base.offset(-9,-1,-9),base.offset(9,5,9)))
            level.setBlock(p,p.getY()<base.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        var block=JugcraftWorkshop.BLOCKS[kind];var state=block.defaultBlockState().setValue(WorkshopBlock.FACING,facing);
        level.setBlockAndUpdate(base,state);block.setPlacedBy(level,base,state,null,ItemStack.EMPTY);
        station=(WorkshopBlockEntity)level.getBlockEntity(base);
        level.setBlockAndUpdate(base.west(6),Blocks.BARREL.defaultBlockState());source=(Container)level.getBlockEntity(base.west(6));
        level.setBlockAndUpdate(base.east(6),Blocks.BARREL.defaultBlockState());output=(Container)level.getBlockEntity(base.east(6));
        npc=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);
        var start=Vec3.atBottomCenterOf(base.relative(facing.getOpposite(),4));npc.snapTo(start.x,start.y,start.z,0,0);
        npc.setNoAi(true);npc.setOnGround(true);level.addFreshEntity(npc);
        npc.orders.tame(level.getServer().getPlayerList().getPlayers().getFirst());npc.preferences.social=false;npc.preferences.carryMeals=0;
        npc.assignments.assign(level,base);
        var entry=station.catalog().entries().getFirst();recipe=entry.recipe();station.select(entry.id());station.companion.filter.set(0,recipe.result().create());
    }
    private void stockBench(){
        for(int i=0;i<recipe.parts().size();i++){var p=recipe.parts().get(i);station.setItem(i,new ItemStack(p.ingredient().items().findFirst().orElseThrow(),p.count()));}
        station.setItem(WorkshopBlockEntity.TOOL,recipe.toolStack());
        if(recipe.heated())station.setItem(WorkshopBlockEntity.FUEL,new ItemStack(Items.COAL));
    }
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{
            c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            c.setGenerateStructures(false);
        }).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();
            var spawn=context.computeOnClient(c->c.player.blockPosition());base=new BlockPos((spawn.getX()>>4)<<4,spawn.getY()+4,(spawn.getZ()>>4)*16+8);
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamerule minecraft:random_tick_speed 0");
            server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f",base.getX()+.5,base.getY(),base.getZ()-8.5));
            phase("claim permissions");server.runOnServer(s->claims(s.overworld()));
            phase("fertilizer blends and limits");server.runOnServer(s->fertilizers(s.overworld()));
            phase("workshop ports, filters and transaction rollback");
            for(int kind=0;kind<3;kind++){final int k=kind;server.runOnServer(s->ports(s.overworld(),k));}
            for(boolean jughead:new boolean[]{false,true})for(var facing:Direction.Plane.HORIZONTAL){
                phase("bench approach "+facing+" jughead="+jughead);
                server.runOnServer(s->{setup(s.overworld(),0,facing,jughead);stockBench();navigation();npc.setNoAi(false);});
                awaitOutput(server,false,1600);
                server.runOnServer(s->{check(station.getItem(WorkshopBlockEntity.OUTPUT).getCount()==recipe.result().create().getCount(),"native bench yield exactly once");
                    check(npc.getEnergy()<CompanionEnergy.CAPACITY,"bench work consumes companion energy");
                    check(CompanionHazards.safeAt(npc,npc.position()),"working companion stays outside the bench");});
            }
            for(int kind=0;kind<3;kind++){
                final int k=kind;phase("physical Supply/Output station="+kind);
                server.runOnServer(s->{setup(s.overworld(),k,Direction.NORTH,false);
                    npc.assignments.assignContainer(s.overworld(),base.west(6),Direction.UP,false);npc.assignments.assignContainer(s.overworld(),base.east(6),Direction.UP,true);
                    int slot=0;for(var p:recipe.parts())source.setItem(slot++,new ItemStack(p.ingredient().items().findFirst().orElseThrow(),p.count()));
                    if(!recipe.toolStack().isEmpty())source.setItem(slot++,recipe.toolStack());
                    if(recipe.heated())source.setItem(slot++,new ItemStack(Items.COAL));
                    if(k==2){source.setItem(slot++,new ItemStack(JugcraftWorkshop.ITEMS.get("basic_ceramic_lining")));source.setItem(slot,new ItemStack(JugcraftWorkshop.ITEMS.get("ceramic_crucible")));}
                    npc.setNoAi(false);
                });
                awaitOutput(server,true,3000);
                server.runOnServer(s->{check(count(output,recipe.result().create().getItem())==recipe.result().create().getCount(),"Supply to production to Output conserves exact yield");
                    check(ItemStack.matches(station.getItem(WorkshopBlockEntity.TOOL),recipe.toolStack()),"reusable tool retained");});
            }
            server.runOnServer(s->{npc.transport.stop();npc.resetCompanionRoutine();npc.discard();});
        }
        phase("PASS "+assertions+" assertions");
    }
    private void awaitOutput(TestServerContext server,boolean delivered,int ticks){
        try{server.waitFor(s->count(delivered?output:station,recipe.result().create().getItem())>=recipe.result().create().getCount(),ticks);}
        catch(AssertionError e){throw new AssertionError(server.computeOnServer(s->"Workshop stalled: kind="+station.station()+", position="+npc.position()+", routine="+npc.routineStatus()+", transport="+npc.transport.activity()+", native="+station.status()+", progress="+station.progress()),e);}
    }
    private void navigation(){
        var entrances=StarterWorkshopJob.frontEntrances(npc,station);check(entrances.size()==2,"two safe front entrances in every orientation");
        var path=npc.getNavigation().createPath(entrances.getFirst(),0,64);check(path!=null && path.canReach(),"route from behind reaches front");
        for(int i=1;i<path.getNodeCount();i++)check(CompanionHazards.safeAt(npc,path.getEntityPosAtNode(npc,i)),"route avoids both bench halves and top");
        check(!CompanionHazards.safeAt(npc,Vec3.atBottomCenterOf(base)),"under-bench destination refused");
        npc.level().setBlockAndUpdate(entrances.getFirst(),Blocks.STONE.defaultBlockState());
        check(StarterWorkshopJob.frontEntrances(npc,station).size()==1,"blocked front uses other half");
        npc.level().setBlockAndUpdate(entrances.getLast(),Blocks.STONE.defaultBlockState());
        check(StarterWorkshopJob.frontEntrances(npc,station).isEmpty(),"no entrance through blocked front");
        for(var p:entrances)npc.level().setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
    }
    private void claims(ServerLevel level){
        setup(level,0,Direction.NORTH,false);
        var server=level.getServer();var owner=server.getPlayerList().getPlayers().getFirst();var book=Ledger.get(server).book();var land=LandRegistry.get(server);
        var offline=FakePlayer.get(level,new GameProfile(UUID.randomUUID(),"OfflineOwner"));
        var offlineAccount=book.account(offline.getUUID(),"OfflineOwner");var ownerAccount=book.account(owner.getUUID(),owner.getName().getString());
        var keys=Set.of(LandRegistry.keyOf(base),LandRegistry.keyOf(base.west()));
        try{
            for(long key:keys)check(land.assign(book,key,ownerAccount,0).ok(),"claim fixture");
            check(station.companion.accessible(npc),"own claim permits workshop");
            land.assign(book,LandRegistry.keyOf(base.west()),offlineAccount,0);
            check(!station.companion.accessible(npc),"foreign second bench half denies master access");
            check(land.trust(offline,book,ownerAccount.name,true).ok(),"offline owner trusts companion owner");
            check(station.companion.accessible(npc),"trust permits full footprint");
            land.trust(offline,book,ownerAccount.name,false);
            check(!station.companion.accessible(npc),"trust revocation takes effect immediately");
            npc.discard();npc=PeepoMod.PEEPO.create(level,EntitySpawnReason.COMMAND);npc.setNoAi(true);level.addFreshEntity(npc);npc.orders.tame(offline);
            check(npc.orders.ownerPlayer()==null,"offline fixture absent from player list");
            check(CompanionJobs.permitted(npc,base.west()),"offline UUID still permits owned land");
            check(!CompanionJobs.permitted(npc,base),"offline UUID cannot use foreign land");
            var wild=PeepoMod.PEEPO.create(level,EntitySpawnReason.COMMAND);check(!CompanionJobs.permitted(wild,base),"untamed cannot use claims");wild.discard();
        }finally{for(long key:keys)land.abandon(owner,book,key,true,0);}
    }
    private void fertilizers(ServerLevel level){
        int index=0;
        for(var blend:List.of(PetroItems.NITROGEN_FERTILIZER,PetroItems.TRIPLE_SUPERPHOSPHATE,PetroItems.MIXED_FARM_BLEND)){
            setup(level,0,Direction.NORTH,false);var stack=new ItemStack(blend,4);
            check(GardenMaintenance.fertilizer(stack) && SprinklerBlockEntity.acceptsFertilizer(stack),"blend accepted by garden and sprinkler");
            var soil=base.south(3+index++).below();var outside=soil.east();
            for(var p:List.of(soil,outside)){level.setBlockAndUpdate(p,Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7));level.setBlockAndUpdate(p.above(),Blocks.WHEAT.defaultBlockState());}
            npc.assignments.clear(1);npc.assignments.assignGardenZone(soil,soil,false);npc.assignments.toggleGardenMaintenance(1,CompanionAssignments.FERTILIZE);
            var target=npc.assignments.get(1);var maintenance=new GardenMaintenance(npc);npc.belongings.setItem(0,stack.copy());
            check(maintenance.fertilize(target,soil.above()),"blend grows crop");
            check(npc.belongings.getItem(0).getCount()==3,"one dose consumed");
            check(level.getBlockState(soil.above()).getValue(CropBlock.AGE)>0,"native growth applied");
            check(level.getBlockState(outside.above()).getValue(CropBlock.AGE)==0,"growth stays inside garden zone");
            check(!maintenance.fertilize(target,soil.above()),"cooldown blocks repeated fertilizing");
            level.setBlockAndUpdate(base.east(3),JugcraftFarming.SPRINKLER.defaultBlockState());
            var sprinkler=(SprinklerBlockEntity)level.getBlockEntity(base.east(3));var port=sprinkler.companion.forCompanion(npc);
            try(var tx=Transaction.openOuter()){check(port.inputs().insert(ItemVariant.of(blend),64,tx)==4,"sprinkler companion stock limited to four");tx.commit();}
            check(sprinkler.fertilizer()==4 && port.needed(stack)==0,"full stock stops supply");
            var other=blend==PetroItems.MIXED_FARM_BLEND?PetroItems.NITROGEN_FERTILIZER:PetroItems.MIXED_FARM_BLEND;
            try(var tx=Transaction.openOuter()){check(port.inputs().insert(ItemVariant.of(other),1,tx)==0,"different blends cannot mix");}
        }
    }
    private void ports(ServerLevel level,int kind){
        setup(level,kind,Direction.NORTH,false);var port=station.companion.forCompanion(npc);
        var supplies=new SimpleContainer(12);int slot=0;
        for(var part:recipe.parts())supplies.setItem(slot++,new ItemStack(part.ingredient().items().findFirst().orElseThrow(),part.count()));
        if(!recipe.toolStack().isEmpty())supplies.setItem(slot++,recipe.toolStack());
        supplies.setItem(slot++,new ItemStack(Items.COAL));
        if(kind==2){supplies.setItem(slot++,new ItemStack(JugcraftWorkshop.ITEMS.get("basic_ceramic_lining")));supplies.setItem(slot,new ItemStack(JugcraftWorkshop.ITEMS.get("ceramic_crucible")));}
        port.prepare(ContainerStorage.of(supplies,Direction.UP));check(port.plan()!=null,"recipe selected from real supply");
        check(!station.companion.acceptsFilter(new ItemStack(Items.DIRT)),"unrelated filter refused");
        for(int i=0;i<supplies.getContainerSize();i++){
            var item=supplies.getItem(i);if(item.isEmpty() || port.needed(item)==0)continue;
            try(var tx=Transaction.openOuter()){check(port.inputs().insert(ItemVariant.of(item),item.getCount(),tx)>0,"needed supply inserts in simulation");}
            check(station.isEmpty(),"delivery simulation rolls back inventory");
        }
        for(int i=0;i<supplies.getContainerSize();i++){
            var item=supplies.getItem(i);if(item.isEmpty() || port.needed(item)==0)continue;
            try(var tx=Transaction.openOuter()){check(port.inputs().insert(ItemVariant.of(item),item.getCount(),tx)>0,"real delivery inserts");tx.commit();}
        }
        check(station.status()==(kind==0?6:0),"stocked native station ready");
        station.companion.filter.set(0,new ItemStack(Items.DIRT));
        check(port.needed(new ItemStack(recipe.parts().getFirst().ingredient().items().findFirst().orElseThrow()))==0,"filter edit stops old input");
        station.companion.filter.set(0,ItemStack.EMPTY);check(port.plan()!=null,"blank filter accepts available recipe");
        station.setItem(WorkshopBlockEntity.OUTPUT,recipe.result().create());
        try(var tx=Transaction.openOuter()){check(port.outputs().extract(ItemVariant.of(recipe.result().create()),64,tx)==recipe.result().create().getCount(),"only finished output extracted");tx.commit();}
        check(station.getItem(WorkshopBlockEntity.OUTPUT).isEmpty(),"output removed once");
        check(ItemStack.matches(station.getItem(WorkshopBlockEntity.TOOL),recipe.toolStack()),"output leaves current tool installed");
    }
}

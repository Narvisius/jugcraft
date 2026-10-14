package local.peepo;

import java.util.*;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Runs in the existing produce/harvest suite, without an extra client/world startup. */
final class CompanionEggCollectionChecks {
    private final BiConsumer<Boolean,String> check;
    private final BlockPos base;
    private final List<Entity> spawned=new ArrayList<>();
    private PeepoEntity npc;
    private Chicken chicken;
    CompanionEggCollectionChecks(BiConsumer<Boolean,String> check,BlockPos base){this.check=check;this.base=base;}
    private void cleanup(){for(var entity:spawned){if(entity instanceof PeepoEntity p){p.transport.stop();p.resetCompanionRoutine();}entity.discard();}spawned.clear();}
    private PeepoEntity worker(ServerLevel level,boolean jughead){
        var p=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);
        p.setNoAi(true);p.snapTo(base.getX()-2.5,base.getY(),base.getZ()+2.5,0,0);p.setOnGround(true);level.addFreshEntity(p);spawned.add(p);
        var owner=level.getServer().getPlayerList().getPlayers().getFirst();p.orders.tame(owner);p.orders.command(owner,3);p.preferences.social=false;p.preferences.carryMeals=0;
        p.assignments.assignAnimal(chicken);return p;
    }
    private void setup(ServerLevel level,boolean jughead){
        cleanup();for(var p:BlockPos.betweenClosed(base.offset(-10,-1,-4),base.offset(10,3,4)))level.setBlock(p,p.getY()<base.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),18);
        chicken=EntityTypes.CHICKEN.create(level,EntitySpawnReason.COMMAND);chicken.setNoAi(true);chicken.snapTo(base.getX()+.5,base.getY(),base.getZ()+.5,0,0);chicken.setOnGround(true);level.addFreshEntity(chicken);spawned.add(chicken);
        npc=worker(level,jughead);
    }
    private ItemEntity drop(ServerLevel level,Item item,int count,double dx){
        var egg=new ItemEntity(level,base.getX()+.5+dx,base.getY(),base.getZ()+.5,new ItemStack(item,count));
        egg.setDeltaMovement(Vec3.ZERO);egg.setNoPickUpDelay();egg.setOnGround(true);level.addFreshEntity(egg);spawned.add(egg);return egg;
    }
    private int count(Container cargo,Item item){int count=0;for(int i=0;i<cargo.getContainerSize();i++)if(cargo.getItem(i).is(item))count+=cargo.getItem(i).getCount();return count;}
    private CompanionJob ready(){var job=CompanionJobs.resolve(npc,npc.assignments.get(1).at().pos());check.accept(job!=null && job.workStatus(npc)==CompanionStatus.READY,"egg collection job is ready");var at=job.approachPosition();npc.snapTo(at.x,at.y,at.z,0,0);check.accept(job.claim(npc),"egg stack is claimed");return job;}
    private CompanionStatus complete(TestServerContext server,CompanionJob job){
        long[] last={Long.MIN_VALUE};CompanionStatus[] result={null};
        server.waitFor(s->{long now=s.overworld().getGameTime();if(last[0]==now)return false;last[0]=now;result[0]=job.work(npc);return result[0]!=CompanionStatus.WORKING;},50);
        return result[0];
    }
    void run(TestServerContext server){
        try{
            server.runOnServer(s->{var level=s.overworld();setup(level,false);var target=npc.assignments.get(1);
                check.accept(target.eggs() && target.livestock() && target.carriesProduce(),"chicken creates a normal animal produce assignment");
                check.accept(target.at().pos().equals(base.below()),"egg area is anchored beneath the selected chicken");
                check.accept(CompanionLogistics.resolve(npc,target).needed(new ItemStack(Items.BUCKET))==0,"egg job never requests supplies");
                var save=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());npc.assignments.save(save);npc.assignments.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),save.buildResult()));
                check.accept(target.equals(npc.assignments.get(1)),"egg assignment survives save/reload");
                chicken.discard();check.accept(target.present(level),"egg area persists when selected chicken leaves");
                drop(level,Items.EGG,1,8.1);drop(level,Items.FEATHER,1,1);drop(level,Items.EGG,1,2).setNeverPickUp();
                check.accept(npc.livestock.job(target.at().pos()).workStatus(npc)==CompanionStatus.EGGS_WAIT,"outside-radius eggs, delayed eggs and unrelated drops are ignored");
            });
            // Full cargo cannot destroy ground items or combine them into player-given egg stacks.
            CompanionJob[] active={null};ItemEntity[] eggs={null};
            server.runOnServer(s->{setup(s.overworld(),false);eggs[0]=drop(s.overworld(),Items.EGG,5,1);
                for(int i=0;i<7;i++)npc.belongings.setItem(i,new ItemStack(Items.COBBLESTONE,64));npc.belongings.setItem(7,new ItemStack(Items.EGG,14));
                active[0]=ready();var rival=worker(s.overworld(),true);var rivalJob=rival.livestock.job(base.below());
                check.accept(rivalJob.workStatus(rival)!=CompanionStatus.READY && !rivalJob.claim(rival),"overlapping workers cannot claim the same eggs");
            });
            var full=complete(server,active[0]);
            server.runOnServer(s->{check.accept(full==CompanionStatus.FULL && eggs[0].isAlive() && eggs[0].getItem().getCount()==5,"full cargo leaves every egg on the ground");check.accept(count(npc.belongings,Items.EGG)==14 && count(npc.belongings,Items.COBBLESTONE)==448,"failed pickup rolls back cargo");});
            // Partial capacity keeps uncollected eggs in the world and preserves the output ledger.
            server.runOnServer(s->{setup(s.overworld(),true);eggs[0]=drop(s.overworld(),Items.BLUE_EGG,5,1);
                for(int i=0;i<7;i++)npc.belongings.setItem(i,new ItemStack(Items.COBBLESTONE,64));
                try(var tx=Transaction.openOuter()){check.accept(npc.garden.storeHarvest(new ItemStack(Items.BLUE_EGG,14),npc.assignments.get(1).at(),tx),"seed marked cargo for partial-capacity test");tx.commit();}
                active[0]=ready();
            });
            var partial=complete(server,active[0]);
            server.runOnServer(s->{check.accept(partial==CompanionStatus.IDLE && count(npc.belongings,Items.BLUE_EGG)==16,"Jughead collects exactly the two eggs that fit");check.accept(eggs[0].isAlive() && eggs[0].getItem().getCount()==3,"partial pickup leaves the remaining three eggs");check.accept(npc.garden.output(7,npc.assignments.get(1)).getCount()==16,"all collected eggs retain their job output provenance");});
            // Real scheduler/navigation/output trip, all three vanilla egg variants, with no chicken remaining.
            Container[] output={null};
            server.runOnServer(s->{var level=s.overworld();setup(level,false);
                level.setBlockAndUpdate(base.east(5),Blocks.BARREL.defaultBlockState());output[0]=(Container)level.getBlockEntity(base.east(5));npc.assignments.assignContainer(level,base.east(5),Direction.UP,true);
                npc.belongings.setItem(0,new ItemStack(Items.EGG,2));
                drop(level,Items.EGG,3,1);drop(level,Items.BLUE_EGG,2,2);drop(level,Items.BROWN_EGG,4,3);chicken.discard();npc.setNoAi(false);
            });
            try{server.waitFor(s->count(output[0],Items.EGG)==3 && count(output[0],Items.BLUE_EGG)==2 && count(output[0],Items.BROWN_EGG)==4,1000);}
            catch(AssertionError failure){server.runOnServer(s->org.slf4j.LoggerFactory.getLogger("peepo-egg-test").error("Egg delivery timeout: position={}, routine={}, hauling={}, row={}, cargo={}/{}/{}, output={}/{}/{}, ground={}",npc.position(),npc.routineStatus(),npc.transport.activity(),CompanionStatus.from(npc.report.row(1)),count(npc.belongings,Items.EGG),count(npc.belongings,Items.BLUE_EGG),count(npc.belongings,Items.BROWN_EGG),count(output[0],Items.EGG),count(output[0],Items.BLUE_EGG),count(output[0],Items.BROWN_EGG),spawned.stream().filter(e->e instanceof ItemEntity && e.isAlive()).map(e->e.position()+":"+((ItemEntity)e).getItem()).toList()));throw failure;}
            server.runOnServer(s->{
                check.accept(count(npc.belongings,Items.EGG)==2,"Output keeps player-given eggs in cargo");
                check.accept(spawned.stream().noneMatch(e->e instanceof ItemEntity && e.isAlive()),"all area eggs were collected without duplicates");
                check.accept(npc.belongings.getItem(9).isEmpty(),"egg collection needs no tool");
                npc.assignments.remove(s.overworld(),base.below());check.accept(npc.assignments.get(1)==null,"planner anchor removal clears egg job");
            });
        }finally{server.runOnServer(s->cleanup());}
    }
}

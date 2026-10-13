package local.peepo;

import java.util.*;
import java.util.function.BiConsumer;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Runtime regressions for planning isolation, snapshot cadence, resting and reused navigation. */
final class CompanionPerformanceChecks {
    private final BiConsumer<Boolean,String> check;
    private final BiConsumer<String,Runnable> group;
    private final BlockPos base;
    private final List<PeepoEntity> entities=new ArrayList<>();
    CompanionPerformanceChecks(BiConsumer<Boolean,String> check,BiConsumer<String,Runnable> group,BlockPos base){this.check=check;this.group=group;this.base=base;}
    private void verify(boolean value,String message){check.accept(value,message);}
    private void reset(ServerLevel level){
        for(var p:entities){p.resetCompanionRoutine();p.discard();}entities.clear();
        for(var pos:BlockPos.betweenClosed(base.offset(-9,-1,-9),base.offset(12,10,10)))
            level.setBlock(pos,pos.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
    }
    private PeepoEntity npc(ServerLevel level){
        var p=PeepoMod.PEEPO.create(level,EntitySpawnReason.COMMAND);p.setNoAi(true);p.setNoGravity(true);
        p.snapTo(base.getX()+.5,base.getY(),base.getZ()+.5,0,0);p.setOnGround(true);level.addFreshEntity(p);entities.add(p);return p;
    }
    private Object data(PeepoEntity p,String name){
        try{var field=PeepoEntity.class.getDeclaredField(name);field.setAccessible(true);return p.getEntityData().get((EntityDataAccessor<?>)field.get(null));}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    void run(ClientGameTestContext context,TestServerContext server){
        group.accept("readiness sharing and companion isolation",()->server.runOnServer(s->{reset(s.overworld());cache(s.overworld());}));
        group.accept("exact energy with paced snapshots and stable meal timestamps",()->snapshots(context,server));
        group.accept("cached bunk clearance reacts to a blocked entrance",()->beds(server));
        group.accept("navigation reuses progress and rejects failed entrance",()->paths(server));
        server.runOnServer(s->reset(s.overworld()));
    }
    private void cache(ServerLevel level){
        var first=npc(level);var second=npc(level);var pos=base.east(3);
        level.setBlockAndUpdate(pos,JugcraftAgriculture.block("cooking_pot").defaultBlockState());
        var pot=(CookingPotBlockEntity)level.getBlockEntity(pos);int[] reads={0};
        for(int i=0;i<20;i++)verify(CompanionReadiness.shared(pot,()->{reads[0]++;return CompanionStatus.NO_INPUT;})==CompanionStatus.NO_INPUT,"shared facts");
        verify(reads[0]==1,"same workstation facts computed more than once in a tick");
        CompanionReadiness.invalidate(pot);
        verify(CompanionReadiness.shared(pot,()->{reads[0]++;return CompanionStatus.READY;})==CompanionStatus.READY && reads[0]==2,"explicit invalidation ignored");
        int[] personal={0};boolean[] ready={true};
        CompanionJob job=new CompanionJob(){
            public Kind kind(){return Kind.WORK;}
            public BlockPos stationPosition(){return pos;}
            public Vec3 approachPosition(){return Vec3.atBottomCenterOf(pos.west());}
            public boolean availableTo(PeepoEntity p){return true;}
            public CompanionStatus workStatus(PeepoEntity p){personal[0]++;return p==first && ready[0]?CompanionStatus.READY:CompanionStatus.FORBIDDEN;}
            public boolean claim(PeepoEntity p){return workStatus(p)==CompanionStatus.READY;}
            public boolean occupy(PeepoEntity p){return claim(p);}
            public void release(PeepoEntity p){}
            public CompanionStatus work(PeepoEntity p){return workStatus(p);}
        };
        for(int i=0;i<20;i++){
            verify(job.planningStatus(first)==CompanionStatus.READY,"first companion readiness");
            verify(job.planningStatus(second)==CompanionStatus.FORBIDDEN,"readiness leaked between companions");
        }
        verify(personal[0]==2,"personal status not reused");ready[0]=false;
        verify(!job.claim(first),"cached readiness authorized a stale claim");
        var old=CompanionJobs.resolve(first,pos);level.removeBlock(pos,false);
        level.setBlockAndUpdate(pos,JugcraftAgriculture.block("cooking_pot").defaultBlockState());
        verify(CompanionJobs.resolve(first,pos)!=old,"same-tick replacement reused old block entity");
    }
    private void snapshots(ClientGameTestContext context,TestServerContext server){
        final PeepoEntity[] p={null};final long[] start={0},eatEnd={0},lastTick={-1};final int[] changes={0},lastEnergy={-1};
        server.runOnServer(s->{reset(s.overworld());p[0]=npc(s.overworld());p[0].setStoredEnergy(20000);
            try(var tx=Transaction.openOuter()){p[0].extractEnergy(32,tx);}
            verify(p[0].getEnergy()==20000,"snapshot throttling broke transactional rollback");
            p[0].beginLunchMeal(new ItemStack(Items.BREAD),null);eatEnd[0]=(Long)data(p[0],"EATING_END");start[0]=s.overworld().getGameTime();
        });
        server.waitFor(s->{long now=s.overworld().getGameTime();if(now==lastTick[0])return false;lastTick[0]=now;
            int published=(Integer)data(p[0],"ENERGY");if(published!=lastEnergy[0]){changes[0]++;lastEnergy[0]=published;}
            if(p[0].isEating())verify((Long)data(p[0],"EATING_END")==eatEnd[0],"eating timestamp changed every tick");
            if(now-start[0]>12)verify(Math.abs(published-p[0].getEnergy())<400,"energy snapshot too stale");
            return now-start[0]>=80;
        },150);
        server.runOnServer(s->{
            verify(changes[0]>=4 && changes[0]<=11,"energy snapshots should update about twice/second, observed "+changes[0]);
            verify(!p[0].isEating() && p[0].getFoodRegenTicks()>0 && p[0].getEnergy()>20000,"meal/regen stopped after timer change");
            long end=(Long)data(p[0],"FOOD_END");verify(Math.abs(end-s.overworld().getGameTime()-p[0].getFoodRegenTicks())<=1,"food countdown disagrees with server time");
        });
        int id=p[0].getId();context.waitFor(c->c.level.getEntity(id) instanceof PeepoEntity companion && !companion.isEating() && companion.getFoodRegenTicks()>0,100);
    }
    private void beds(TestServerContext server){
        server.runCommand("time set midnight");final CompanionBedEntity[] bed={null};final PeepoEntity[] p={null};final BlockPos[] entry={null};
        server.runOnServer(s->{var level=s.overworld();reset(level);p[0]=npc(level);
            var pos=base.east(4);for(int i=0;i<6;i++)level.setBlockAndUpdate(pos.above(i),CompanionBeds.BLOCKS.getFirst().defaultBlockState());
            bed[0]=(CompanionBedEntity)level.getBlockEntity(pos.above(5));entry[0]=BlockPos.containing(bed[0].entrance());
            verify(bed[0].claim(p[0]) && bed[0].occupy(p[0]),"cached upper bunk cannot be occupied");
            var resting=p[0].position();for(int i=0;i<20;i++)verify(bed[0].occupy(p[0]) && p[0].position().equals(resting),"stationary bed mount moved");
            level.setBlockAndUpdate(entry[0].above(2),Blocks.STONE.defaultBlockState());
        });
        server.waitFor(s->{if(!bed[0].hasOccupant())return true;bed[0].occupy(p[0]);return false;},60);
        server.runOnServer(s->{verify(p[0].getRestMode()==CompanionEnergy.Rest.NONE,"blocked bunk retained sleeping state");verify(p[0].getY()<=base.getY()+1,"bunk did not release to bottom entrance");});
        server.runCommand("time set noon");
    }
    private void paths(TestServerContext server){
        final PeepoEntity[] p={null};final CompanionNavigation.Progress tracker=new CompanionNavigation.Progress();final Vec3[] goal={null};final long[] start={0};
        server.runOnServer(s->{var level=s.overworld();reset(level);p[0]=npc(level);goal[0]=Vec3.atBottomCenterOf(base.east(10));
            var path=p[0].getNavigation().createPath(BlockPos.containing(goal[0]),0,64);verify(path!=null && path.canReach(),"path fixture not reachable");
            p[0].getNavigation().moveTo(path,1);tracker.started(p[0],goal[0]);start[0]=level.getGameTime();
        });
        server.waitFor(s->s.overworld().getGameTime()-start[0]>=12,40);
        server.runOnServer(s->{p[0].snapTo(base.getX()+1.5,base.getY(),base.getZ()+.5,0,0);verify(!tracker.needsPath(p[0],goal[0]),"progressing path unnecessarily rebuilt");});
        server.waitFor(s->s.overworld().getGameTime()-start[0]>=55,90);
        server.runOnServer(s->{verify(tracker.needsPath(p[0],goal[0]),"stuck path never retried");
            var pos=base.east(4);s.overworld().setBlockAndUpdate(pos,JugcraftAgriculture.block("cooking_pot").defaultBlockState());
            var pot=(CookingPotBlockEntity)s.overworld().getBlockEntity(pos);var job=pot.companionJob.prepare(p[0]);var first=job.approachPosition();
            p[0].navigationMemory.reject(p[0],pos,first);job.approachFailed(p[0]);job.prepare(p[0]);
            verify(!job.approachPosition().equals(first),"failed entrance selected again instead of another side");
        });
    }
}

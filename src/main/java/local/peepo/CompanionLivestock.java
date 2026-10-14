package local.peepo;

import java.util.*;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.*;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.*;

/** Four explicit anchors at most. Entity searches are budgeted, loaded-only and capped. */
public final class CompanionLivestock {
    public static final Identifier SHEARING=PeepoMod.id("shearing_job"), MILKING=PeepoMod.id("milking_job"), EGGS=PeepoMod.id("egg_collection_job"), CARE=PeepoMod.id("animal_care_job");
    public static final int RADIUS=8, WORK_TICKS=60, ENERGY_PER_TICK=4, MILK_COOLDOWN=1200;
    private record Claim(UUID worker,long until,int task){Claim(UUID worker,long until){this(worker,until,0);}}
    private static final AttachmentType<Claim> CLAIM=AttachmentRegistry.<Claim>builder().buildAndRegister(PeepoMod.id("animal_work_claim"));
    private static final AttachmentType<Long> MILK_READY=AttachmentRegistry.<Long>builder().persistent(Codec.LONG).buildAndRegister(PeepoMod.id("milk_ready"));
    private final PeepoEntity npc;
    private final Map<BlockPos,Job> jobs=new HashMap<>();
    private final Map<BlockPos,CareJob> careJobs=new HashMap<>();
    private final CompanionEggCollection eggs;
    public static void initialize(){CompanionEggCollection.initialize();CompanionSlaughterLoot.initialize();} // Register attachment types before worlds/entities are loaded.
    CompanionLivestock(PeepoEntity npc){this.npc=npc;eggs=new CompanionEggCollection(npc);}
    public static Identifier assignmentType(Animal animal){return animal instanceof Sheep?SHEARING:animal instanceof Cow?MILKING:animal instanceof Chicken?EGGS:null;}
    private long now(){return npc.level().getGameTime();}
    private long milkClock(){return ((ServerLevel)npc.level()).getServer().overworld().getGameTime();}
    private boolean enabled(){return npc.level() instanceof ServerLevel level && level.getGameRules().get(GameRules.MOB_GRIEFING);}
    private boolean shears(){var held=npc.belongings.getItem(9);return held.is(Items.SHEARS) && (!held.isDamageableItem() || held.getDamageValue()<held.getMaxDamage());}
    private int bucketSlot(){for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && npc.belongings.getItem(i).is(Items.BUCKET))return i;return -1;}
    public CompanionJob job(BlockPos anchor){
        int row=npc.assignments.workPriority(anchor);var target=row<5?npc.assignments.get(row):null;
        if(target==null || !target.livestock() || !target.present(npc.level()))return null;
        if(target.care())return care(target);
        if(target.eggs())return eggs.job(target);
        jobs.entrySet().removeIf(e->!npc.assignments.assignedWork(e.getKey()));
        var old=jobs.get(anchor);if(old==null || !old.target.equals(target)){if(old!=null)old.release(npc);old=new Job(target,target.shearing()?AnimalCareZone.SHEAR:AnimalCareZone.MILK);jobs.put(anchor.immutable(),old);}return old;
    }
    void inputsChanged(BlockPos anchor){var job=jobs.get(anchor);if(job!=null)job.nextScan=0;var care=careJobs.get(anchor);if(care!=null){for(var child:care.children)if(child instanceof Job animal)animal.nextScan=0;care.husbandry.inputsChanged();}npc.readiness.clear();}
    boolean hasReadyAnimal(CompanionAssignments.Target target){return readyAnimal(target,target.shearing()?AnimalCareZone.SHEAR:AnimalCareZone.MILK);}
    boolean hasReadySheep(CompanionAssignments.Target target){return readyAnimal(target,AnimalCareZone.SHEAR);}
    private boolean readyAnimal(CompanionAssignments.Target target,int task){
        if(!target.task(task))return false;
        if(target.care())return ((Job)care(target).children[task==AnimalCareZone.SHEAR?0:1]).findAnimal();
        return job(target.at().pos()) instanceof Job job && job.findAnimal();
    }
    private CareJob care(CompanionAssignments.Target target){
        careJobs.entrySet().removeIf(e->!npc.assignments.assignedWork(e.getKey()));
        var old=careJobs.get(target.at().pos());if(old==null || !old.target.equals(target)){if(old!=null)old.release(npc);old=new CareJob(target);careJobs.put(target.at().pos(),old);}return old;
    }
    private final class CareJob implements CompanionJob {
        final CompanionAssignments.Target target;
        final CompanionJob[] children;
        final CompanionHusbandry husbandry;
        int active=-1,cursor=2;
        CareJob(CompanionAssignments.Target target){this.target=target;husbandry=new CompanionHusbandry(npc,target);children=new CompanionJob[]{new Job(target,AnimalCareZone.SHEAR),new Job(target,AnimalCareZone.MILK),eggs.job(target),husbandry.breed,husbandry.slaughter,eggs.lootJob(target)};}
        public Kind kind(){return Kind.WORK;}
        public BlockPos stationPosition(){return target.at().pos();}
        public Vec3 approachPosition(){return active<0?Vec3.atBottomCenterOf(stationPosition().above()):children[active].approachPosition();}
        public BlockPos animationTarget(){return active<0?stationPosition():children[active].animationTarget();}
        public WorkAnimation animation(){return active<0?WorkAnimation.NONE:children[active].animation();}
        private boolean assigned(){int row=npc.assignments.workPriority(stationPosition());return row<5 && target.equals(npc.assignments.get(row)) && target.present(npc.level());}
        public boolean availableTo(PeepoEntity other){return other==npc && assigned() && (active<0 || children[active].availableTo(npc));}
        public CompanionStatus workStatus(PeepoEntity other){
            if(other!=npc || !assigned())return CompanionStatus.MISSING;
            if(!enabled())return CompanionStatus.GARDEN_DISABLED;
            if(active>=0){var state=children[active].workStatus(npc);if(state==CompanionStatus.READY)return state;children[active].release(npc);active=-1;}
            CompanionStatus result=CompanionStatus.IDLE;
            for(int n=0;n<children.length;n++){int i=(cursor+n)%children.length;if(i<5 && !target.task(1<<i) || i==5 && !CompanionSlaughterLoot.hasOverflow(npc))continue;var state=children[i].workStatus(npc);
                if(i==5 && state==CompanionStatus.EGGS_WAIT)continue;
                if(state==CompanionStatus.READY){active=i;return state;}
                if(result==CompanionStatus.IDLE || state.problem())result=state;
            }
            return result;
        }
        public boolean claim(PeepoEntity other){return other==npc && assigned() && active>=0 && children[active].claim(other);}
        public boolean occupy(PeepoEntity other){return other==npc && assigned() && active>=0 && children[active].occupy(other);}
        public void approachFailed(PeepoEntity other){if(active>=0)children[active].approachFailed(other);release(other);}
        public void release(PeepoEntity other){if(other!=npc)return;for(var child:children)child.release(other);active=-1;}
        public CompanionStatus work(PeepoEntity other){
            if(other!=npc || !assigned() || active<0)return CompanionStatus.IDLE;
            int index=active;var state=children[index].work(other);
            if(state!=CompanionStatus.WORKING){children[index].release(other);active=-1;cursor=(index+1)%children.length;}
            return state;
        }
    }
    int requiredTool(CompanionAssignments.Target target){
        boolean shear=target.task(AnimalCareZone.SHEAR) && hasReadySheep(target);
        boolean kill=target.task(AnimalCareZone.SLAUGHTER) && care(target).husbandry.slaughterReady();
        if(shear && (shears() || !kill || !CompanionHusbandry.weapon(npc.assignedHand())))return AnimalCareZone.SHEAR;
        return kill?AnimalCareZone.SLAUGHTER:shear?AnimalCareZone.SHEAR:0;
    }
    CompanionLogistics.Port port(CompanionAssignments.Target target){return new CompanionLogistics.Port(){
        public Identifier plan(){return target.block();}
        public int needed(ItemStack item){
            if(!enabled())return 0;
            if(target.care() && !item.is(Items.BUCKET))return care(target).husbandry.needed(item);
            if(!target.task(AnimalCareZone.MILK) || !item.is(Items.BUCKET) || !hasReadyAnimal(target))return 0;
            int buckets=0;for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && npc.belongings.getItem(i).is(Items.BUCKET))buckets+=npc.belongings.getItem(i).getCount();
            return Math.max(0,4-buckets);
        }
        public Storage<ItemVariant> inputs(){return new Storage<>(){
            public long insert(ItemVariant item,long count,TransactionContext tx){int n=(int)Math.min(count,needed(item.toStack(1)));return n<=0?0:npc.food.store(item.toStack(n),tx);}
            public long extract(ItemVariant item,long count,TransactionContext tx){return 0;}
            public boolean supportsExtraction(){return false;}
            public Iterator<StorageView<ItemVariant>> iterator(){return Collections.emptyIterator();}
        };}
        public Storage<ItemVariant> outputs(){return Storage.empty();}
    };}

    public final class Job implements CompanionJob {
        final CompanionAssignments.Target target;
        final int task;
        private UUID animalId;
        private Vec3 approach;
        private Hold hold;
        private long nextScan,nextApproach;
        private int progress;
        private CompanionStatus status=CompanionStatus.ANIMALS_WAIT;
        Job(CompanionAssignments.Target target,int task){this.target=target;this.task=task;}
        public Kind kind(){return Kind.WORK;}
        public BlockPos stationPosition(){return target.at().pos();}
        public WorkAnimation animation(){return (task==AnimalCareZone.SHEAR)?WorkAnimation.SHEAR:WorkAnimation.MILK;}
        public Vec3 approachPosition(){return approach==null?Vec3.atBottomCenterOf(stationPosition().above()):approach;}
        private Animal animal(){return animalId!=null && ((ServerLevel)npc.level()).getEntity(animalId) instanceof Animal a?a:null;}
        private boolean assigned(){int row=npc.assignments.workPriority(stationPosition());return row<5 && target.equals(npc.assignments.get(row)) && target.present(npc.level()) && target.task(task);}
        private boolean eligible(Animal a){
            if((task==AnimalCareZone.SHEAR)?!(a instanceof Sheep):!(a instanceof Cow))return false;
            if(!a.isAlive() || a.isBaby() || a.isPassenger() || a.isVehicle() || a.isLeashed() || a.isInWater() || a.isOnFire() || a.hurtTime>0
                || !target.animalContains(a.position())
                || !CompanionJobs.permitted(npc,a.blockPosition()))return false;
            return (task==AnimalCareZone.SHEAR)?a instanceof Sheep sheep && sheep.readyForShearing():a instanceof Cow && a.getAttachedOrElse(MILK_READY,0L)<=milkClock();
        }
        private boolean free(Animal a){var c=a.getAttached(CLAIM);return c==null || c.until<=now() || c.worker.equals(npc.getUUID());}
        private Vec3 standing(Animal a){return CompanionLivestock.standing(npc,target,a);}
        private boolean findAnimal(){
            var a=animal();
            if(a!=null && eligible(a) && free(a)){
                if(progress==0 && now()>=nextApproach){nextApproach=now()+10;approach=standing(a);}
                return approach!=null;
            }
            if(animalId!=null)release(npc);
            if(now()<nextScan)return false;
            if(!CompanionBudget.search(npc)){status=CompanionStatus.WAITING;return false;}
            nextScan=now()+80+Math.floorMod(npc.getId(),20);status=CompanionStatus.ANIMALS_WAIT;
            var candidates=new ArrayList<Animal>(24);
            ((ServerLevel)npc.level()).getEntities(EntityTypeTest.forClass(Animal.class),target.animalBox(),a0->eligible(a0) && free(a0),candidates,24);
            candidates.sort(Comparator.comparingDouble(npc::distanceToSqr));
            for(var candidate:candidates){var p=standing(candidate);if(p==null){status=CompanionStatus.BLOCKED;continue;}animalId=candidate.getUUID();approach=p;nextApproach=now()+10;return true;}
            return false;
        }
        public boolean availableTo(PeepoEntity other){var a=animal();return other==npc && assigned() && (a==null || free(a));}
        public CompanionStatus workStatus(PeepoEntity other){
            if(!assigned())return CompanionStatus.MISSING;
            if(!enabled())return CompanionStatus.GARDEN_DISABLED;
            if(!findAnimal())return status;
            if((task==AnimalCareZone.SHEAR) && !shears())return CompanionStatus.NEEDS_SHEARS;
            if((task==AnimalCareZone.MILK) && bucketSlot()<0)return CompanionStatus.NO_BUCKETS;
            return CompanionStatus.READY;
        }
        public boolean claim(PeepoEntity other){var a=animal();if(other!=npc || a==null || !assigned() || !eligible(a) || !free(a))return false;a.setAttached(CLAIM,new Claim(npc.getUUID(),now()+100));return true;}
        public boolean occupy(PeepoEntity other){return npc.position().distanceToSqr(approachPosition())<=.64 && claim(other);}
        public void approachFailed(PeepoEntity other){release(other);nextScan=now()+20;}
        public void release(PeepoEntity other){
            if(other!=npc)return;var a=animal();
            if(hold!=null){hold.animal.getGoalSelector().removeGoal(hold);hold=null;}
            if(a!=null){var c=a.getAttached(CLAIM);if(c!=null && c.worker.equals(npc.getUUID()))a.removeAttached(CLAIM);}
            npc.setWorkAnimal(-1);animalId=null;approach=null;progress=0;
        }
        public CompanionStatus work(PeepoEntity other){
            var a=animal();if(a==null || !occupy(other) || !enabled() || !eligible(a))return CompanionStatus.IDLE;
            if((task==AnimalCareZone.SHEAR) && !shears())return CompanionStatus.NEEDS_SHEARS;
            if((task==AnimalCareZone.MILK) && bucketSlot()<0)return CompanionStatus.NO_BUCKETS;
            if(!npc.hasLineOfSight(a) || npc.distanceToSqr(a)>3)return CompanionStatus.BLOCKED;
            if(progress==0 && npc.position().distanceToSqr(approach)>.025){npc.getMoveControl().setWantedPosition(approach.x,approach.y,approach.z,.65);return CompanionStatus.WORKING;}
            if(hold==null){hold=new Hold(a,npc.getUUID());a.getNavigation().stop();a.getGoalSelector().addGoal(-2,hold);}
            if(!hold.canUse())return CompanionStatus.IDLE;
            try(var tx=Transaction.openOuter()){if(npc.extractEnergy(ENERGY_PER_TICK,tx)!=ENERGY_PER_TICK)return CompanionStatus.RECOVERING;tx.commit();}
            npc.setWorkAnimal(a.getId());float yaw=(float)Math.toDegrees(Math.atan2(-(a.getX()-npc.getX()),a.getZ()-npc.getZ()));
            npc.setYRot(yaw);npc.yBodyRot=yaw;npc.setYHeadRot(yaw);
            if((task==AnimalCareZone.SHEAR) && progress%15==0)npc.level().playSound(null,a.blockPosition(),SoundEvents.SHEEP_SHEAR,SoundSource.NEUTRAL,.45F,1.1F);
            if(++progress<WORK_TICKS)return CompanionStatus.WORKING;
            var result=finish(a);release(npc);nextScan=now()+(result==CompanionStatus.FULL?100:1);status=result;return result;
        }
        private CompanionStatus finish(Animal a){
            if(!eligible(a) || !assigned())return CompanionStatus.IDLE;
            var level=(ServerLevel)npc.level();var products=new ArrayList<ItemStack>();
            if(a instanceof Sheep sheep){
                ((io.github.jimbozoomer.jugcraft.mixin.CompanionShearingInvoker)sheep).jugcraft$collectShearing(level,BuiltInLootTables.SHEAR_SHEEP,npc.belongings.getItem(9),(l,stack)->{if(products.size()<33)products.add(stack.copy());});
            }else products.add(new ItemStack(Items.MILK_BUCKET));
            if(products.size()>32)return CompanionStatus.FULL;
            try(var tx=Transaction.openOuter()){
                if((task==AnimalCareZone.MILK)){int slot=bucketSlot();if(slot<0 || npc.food.takeCargo(slot,npc.belongings.getItem(slot),1,tx)!=1)return CompanionStatus.NO_BUCKETS;}
                for(var stack:products)if(!npc.garden.storeHarvest(stack,target.at(),tx))return CompanionStatus.FULL;
                tx.commit();
            }
            if(a instanceof Sheep sheep){sheep.setSheared(true);npc.belongings.getItem(9).hurtAndBreak(1,npc,EquipmentSlot.MAINHAND);npc.belongings.setChanged();a.gameEvent(GameEvent.SHEAR,npc);}
            else {a.setAttached(MILK_READY,milkClock()+MILK_COOLDOWN);level.playSound(null,a.blockPosition(),SoundEvents.COW_MILK,SoundSource.NEUTRAL,.6F,1);}
            npc.transport.workChanged();return CompanionStatus.IDLE;
        }
    }
    static boolean free(Animal animal,PeepoEntity npc){var c=animal.getAttached(CLAIM);return c==null || c.until<=npc.level().getGameTime() || c.worker.equals(npc.getUUID());}
    static boolean ownedClaim(Animal animal,PeepoEntity npc,int task){var c=animal.getAttached(CLAIM);return c!=null && c.until>npc.level().getGameTime() && c.worker.equals(npc.getUUID()) && c.task==task;}
    static int claimTask(Animal animal){var c=animal.getAttached(CLAIM);return c!=null && c.until>animal.level().getGameTime()?c.task:0;}
    static boolean reserve(Animal animal,PeepoEntity npc,int task){if(!free(animal,npc))return false;animal.setAttached(CLAIM,new Claim(npc.getUUID(),npc.level().getGameTime()+100,task));return true;}
    static void releaseClaim(Animal animal,PeepoEntity npc){if(animal==null)return;var c=animal.getAttached(CLAIM);if(c!=null && c.worker.equals(npc.getUUID()))animal.removeAttached(CLAIM);}
    static Goal hold(Animal animal,PeepoEntity npc){var goal=new Hold(animal,npc.getUUID());animal.getNavigation().stop();animal.getGoalSelector().addGoal(-2,goal);return goal;}
    static Vec3 standing(PeepoEntity npc,CompanionAssignments.Target target,Animal a){
            Vec3 best=null;double radius=a.getBbWidth()/2+npc.getBbWidth()/2+.18;
            // Try both flanks before front/back. All candidates have actual solid footing.
            double yaw=Math.toRadians(a.yBodyRot);
            for(int i=0;i<4;i++){
                double angle=yaw+Math.PI/2*(i==0?1:i==1?-1:i==2?0:2);
                var p=a.position().add(-Math.sin(angle)*radius,0,Math.cos(angle)*radius);
                var floor=BlockPos.containing(p).below();double r=npc.getBbWidth()/2+.015;
                if(!npc.level().hasChunkAt(floor) || !CompanionJobs.permitted(npc,BlockPos.containing(p))
                    || !npc.level().getBlockState(floor).isFaceSturdy(npc.level(),floor,Direction.UP)
                    || !CompanionHazards.safeAt(npc,p) || npc.navigationMemory.failed(npc,target.at().pos(),p)
                    || !npc.level().noCollision(npc,new AABB(p.x-r,p.y+.01,p.z-r,p.x+r,p.y+npc.getBbHeight(),p.z+r)))continue;
                if(best==null || npc.position().distanceToSqr(p)<npc.position().distanceToSqr(best))best=p;
            }
            return best;
        }
    /** Temporary goal, not saved NoAI: damage, water, fire, abandonment or lease expiry releases movement. */
    private static final class Hold extends Goal {
        final Animal animal;final UUID owner;
        Hold(Animal animal,UUID owner){this.animal=animal;this.owner=owner;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
        public boolean canUse(){var c=animal.getAttached(CLAIM);return animal.isAlive() && animal.hurtTime==0 && !animal.isInWater() && !animal.isOnFire() && c!=null && c.worker.equals(owner) && c.until>animal.level().getGameTime();}
        public boolean canContinueToUse(){return canUse();}
        public boolean requiresUpdateEveryTick(){return true;}
        public void tick(){animal.getNavigation().stop();animal.setSpeed(0);var v=animal.getDeltaMovement();animal.setDeltaMovement(0,v.y,0);}
        public void start(){tick();}
    }
}

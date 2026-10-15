package local.peepo;

import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** Explicit species rules; planning is cached, population-changing actions re-count under the shared budget. */
final class CompanionHusbandry {
    static final int CENSUS_LIMIT=256;
    /** Vanilla's breed goal looks for a partner within 8 blocks. Both animals are held still until both are fed, so they look
     *  from where they were fed: a pair farther apart would waste its food. */
    static final double PARTNER_RANGE=8;
    final PeepoEntity npc;
    final CompanionAssignments.Target target;
    final Job breed=new Job(false),slaughter=new Job(true);
    private List<Animal> cached=List.of();
    private long nextCensus,supplyTick=Long.MIN_VALUE,toolTick=Long.MIN_VALUE;
    private boolean toolReady;
    private List<List<Animal>> supplyGroups=List.of();
    private CompanionStatus censusStatus=CompanionStatus.IDLE;
    CompanionHusbandry(PeepoEntity npc,CompanionAssignments.Target target){this.npc=npc;this.target=target;}
    static Identifier type(Animal animal){return BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType());}
    static boolean partners(Animal a,Animal b){return a.distanceToSqr(b)<=PARTNER_RANGE*PARTNER_RANGE;}
    static boolean weapon(ItemStack item){return !item.isEmpty() && (item.is(ItemTags.SWORDS) || item.is(ItemTags.AXES)) && (!item.isDamageableItem() || item.getDamageValue()<item.getMaxDamage());}
    static boolean loaded(ServerLevel level,AnimalCareZone zone){
        var box=zone.box();for(int x=((int)box.minX)>>4;x<=(((int)box.maxX-1)>>4);x++)for(int z=((int)box.minZ)>>4;z<=(((int)box.maxZ-1)>>4);z++)if(!level.getChunkSource().hasChunk(x,z))return false;return true;
    }
    static List<Animal> scan(PeepoEntity npc,CompanionAssignments.Target target){
        if(!(npc.level() instanceof ServerLevel level) || target.zone()==null || !loaded(level,target.zone()) || !CompanionBudget.search(npc))return null;
        var animals=new ArrayList<Animal>();level.getEntities(EntityTypeTest.forClass(Animal.class),target.animalBox(),a->a.isAlive() && target.animalContains(a.position()),animals,CENSUS_LIMIT+1);return animals;
    }
    static List<Identifier> discover(PeepoEntity npc,CompanionAssignments.Target target){var animals=scan(npc,target);return animals==null?null:animals.stream().filter(a->!(a instanceof Enemy) && CompanionJobs.permitted(npc,a.blockPosition())).map(CompanionHusbandry::type).distinct().sorted(Comparator.comparing(Identifier::toString)).limit(32).toList();}
    private List<Animal> census(boolean fresh){
        long now=npc.level().getGameTime();
        if(!loaded((ServerLevel)npc.level(),target.zone())){censusStatus=CompanionStatus.UNLOADED;return null;}
        if(!fresh && now<nextCensus)return censusStatus==CompanionStatus.POPULATION_BUSY?null:cached;
        var list=scan(npc,target);if(list==null){censusStatus=CompanionStatus.WAITING;return null;}
        cached=list;nextCensus=now+80+Math.floorMod(npc.getId(),20);
        censusStatus=list.size()>CENSUS_LIMIT?CompanionStatus.POPULATION_BUSY:CompanionStatus.IDLE;return list.size()>CENSUS_LIMIT?null:list;
    }
    void inputsChanged(){nextCensus=0;supplyTick=toolTick=Long.MIN_VALUE;breed.nextScan=slaughter.nextScan=0;}
    private boolean inside(Animal a){return a!=null && a.isAlive() && target.animalContains(a.position());}
    private boolean eligible(Animal a,boolean kill){
        if(!inside(a) || a instanceof Enemy || a.isBaby() || a.isPassenger() || a.isVehicle() || a.isLeashed() || a.isInWater() || a.isOnFire() || a.isInvulnerable()
            || a instanceof OwnableEntity own && own.getOwnerReference()!=null || a instanceof TamableAnimal pet && pet.isTame() || a instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse && horse.isTamed()
            || !CompanionJobs.permitted(npc,a.blockPosition()) || target.zone().population(type(a))==null)return false;
        if(kill)return !a.hasCustomName() && !a.isInLove() && a.getAge()==0;
        return a.hurtTime==0 && a.getAge()==0 && !a.isInLove() && a.canFallInLove();
    }
    private boolean allowed(List<Animal> animals,AnimalCareZone.Population rule,boolean kill,Set<UUID> own){
        int total=0,adults=0,pending=0,removals=0;
        for(var a:animals)if(inside(a) && type(a).equals(rule.type())){
            total++;if(!a.isBaby())adults++;
            if(!own.contains(a.getUUID())){int task=CompanionLivestock.claimTask(a);if(a.isInLove() || task==AnimalCareZone.BREED)pending++;if(task==AnimalCareZone.SLAUGHTER)removals++;}
        }
        return kill?adults-removals>rule.keep():total+(pending+1)/2+1<=rule.target();
    }
    private boolean outputBlocked(){
        // Never breed through a known delivery backlog. This probes only the eight real cargo slots.
        var output=npc.assignments.supplies.combined(target,true);
        for(int i=0;i<8;i++){var item=npc.garden.output(i,target);if(item.isEmpty())continue;
            if(!npc.assignments.transportAllowed(target,false))return true;
            try(var tx=Transaction.openOuter()){if(output.insert(ItemVariant.of(item),item.getCount(),tx)<item.getCount())return true;}
        }
        return false;
    }
    private boolean slaughterOutput(){return npc.assignments.transportAllowed(target,false) && npc.assignments.supplies.containers(target,true).stream().anyMatch(t->CompanionStorage.find(npc,t)!=null);}
    private boolean slaughterRoom(){
        if(!slaughterOutput())return false;
        int empty=0;for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && npc.belongings.getItem(i).isEmpty())empty++;return empty>=3 && !outputBlocked();
    }
    boolean slaughterReady(){
        long now=npc.level().getGameTime();if(toolTick==now)return toolReady;toolTick=now;toolReady=false;
        // Tool selection/delivery must outlive releasing the work goal to walk to Supply.
        var animals=census(false);if(animals==null || !slaughterOutput() || outputBlocked())return false;
        // A fetched tool temporarily occupies cargo until equipped; enforce loot room at the job, not during that swap.
        for(var rule:target.zone().populations())if(allowed(animals,rule,true,Set.of()) && animals.stream().anyMatch(a->type(a).equals(rule.type()) && eligible(a,true) && CompanionLivestock.free(a,npc)))return toolReady=true;
        return false;
    }
    private int foodCount(Animal a,Animal b){int n=0;for(int i=0;i<8;i++)if(!npc.transport.reserved(i)){var stack=npc.belongings.getItem(i);if(a.isFood(stack) && b.isFood(stack))n+=stack.getCount();}return n;}
    int needed(ItemStack item){
        if(!target.task(AnimalCareZone.BREED))return 0;
        // Supply demand must survive releasing the work goal for a delivery. It must not depend on its scan cooldown.
        long now=npc.level().getGameTime();
        if(supplyTick!=now){
            supplyTick=now;var groups=new ArrayList<List<Animal>>();var animals=census(false);
            if(animals!=null && !outputBlocked())for(var rule:target.zone().populations()){
                if(!allowed(animals,rule,false,Set.of()))continue;
                var group=animals.stream().filter(a->type(a).equals(rule.type()) && eligible(a,false) && CompanionLivestock.free(a,npc)).limit(24).toList();
                if(group.size()>=2)groups.add(group);
            }
            supplyGroups=List.copyOf(groups);
        }
        // Inventory probes can call needed for many stacks. Classify animals only once per tick,
        // but read actual cargo each time so transactional capacity probes see their own inserts.
        for(var group:supplyGroups){var pair=group.stream().filter(a->a.isFood(item)).limit(2).toList();if(pair.size()==2)return Math.max(0,4-foodCount(pair.get(0),pair.get(1)));}
        return 0;
    }
    final class Job implements CompanionJob {
        final boolean kill;
        UUID firstId,secondId;
        Vec3 approach;
        Goal firstHold,secondHold;
        Animal heldFirst,heldSecond;
        int phase,progress;
        long nextScan,nextApproach;
        CompanionStatus status=CompanionStatus.POPULATION_LIMIT;
        Job(boolean kill){this.kill=kill;}
        int task(){return kill?AnimalCareZone.SLAUGHTER:AnimalCareZone.BREED;}
        Animal animal(UUID id){return id!=null && ((ServerLevel)npc.level()).getEntity(id) instanceof Animal a?a:null;}
        Animal first(){return animal(firstId);} Animal second(){return animal(secondId);}
        Animal current(){return phase==0?first():second();}
        Set<UUID> own(){return firstId==null?Set.of():secondId==null?Set.of(firstId):Set.of(firstId,secondId);}
        boolean assigned(){int row=npc.assignments.workPriority(stationPosition());return row<5 && target.equals(npc.assignments.get(row)) && target.task(task()) && target.present(npc.level());}
        boolean usable(){var a=first();var b=second();return eligible(a,kill) && CompanionLivestock.free(a,npc) && (kill || eligible(b,false) && CompanionLivestock.free(b,npc) && partners(a,b));}
        boolean find(){
            if(!assigned())return false;
            if(firstId!=null && usable()){
                if(progress==0 && npc.level().getGameTime()>=nextApproach){approach=CompanionLivestock.standing(npc,target,current());nextApproach=npc.level().getGameTime()+10;}return approach!=null;
            }
            if(firstId!=null)release(npc);
            if(npc.level().getGameTime()<nextScan)return false;nextScan=npc.level().getGameTime()+80;
            if(target.zone().populations().isEmpty()){status=CompanionStatus.POPULATION_SETUP;return false;}
            var animals=census(false);if(animals==null){status=censusStatus;nextScan=npc.level().getGameTime()+5;return false;}
            if(outputBlocked() || kill && !slaughterRoom()){status=CompanionStatus.FULL;return false;}
            var candidates=animals.stream().filter(a->eligible(a,kill) && CompanionLivestock.free(a,npc)).sorted(Comparator.comparingDouble(npc::distanceToSqr)).limit(24).toList();
            status=CompanionStatus.POPULATION_LIMIT;
            for(var a:candidates){var rule=target.zone().population(type(a));if(!allowed(animals,rule,kill,Set.of()))continue;
                Animal b=null;if(!kill){b=candidates.stream().filter(other->other!=a && other.getType()==a.getType() && partners(a,other)).findFirst().orElse(null);if(b==null)continue;}
                var point=CompanionLivestock.standing(npc,target,a);if(point==null){status=CompanionStatus.BLOCKED;continue;}
                firstId=a.getUUID();secondId=b==null?null:b.getUUID();approach=point;nextApproach=npc.level().getGameTime()+10;return true;
            }
            return false;
        }
        public Kind kind(){return Kind.WORK;}
        public BlockPos stationPosition(){return target.at().pos();}
        public Vec3 approachPosition(){return approach==null?Vec3.atBottomCenterOf(stationPosition().above()):approach;}
        public BlockPos animationTarget(){var a=current();return a==null?stationPosition():a.blockPosition();}
        public WorkAnimation animation(){return kill?WorkAnimation.SLAUGHTER:WorkAnimation.INTERACT;}
        public boolean availableTo(PeepoEntity other){return other==npc && assigned() && (firstId==null || usable());}
        public CompanionStatus workStatus(PeepoEntity other){
            if(other!=npc || !assigned())return CompanionStatus.MISSING;
            if(!((ServerLevel)npc.level()).getGameRules().get(GameRules.MOB_GRIEFING))return CompanionStatus.GARDEN_DISABLED;
            if(!find())return status;
            if(kill && !weapon(npc.assignedHand()))return CompanionStatus.NEEDS_WEAPON;
            if(!kill && foodCount(first(),second())<2)return CompanionStatus.NEEDS_BREEDING_FOOD;
            return CompanionStatus.READY;
        }
        public boolean claim(PeepoEntity other){
            if(other!=npc || !assigned() || !usable() || !loaded((ServerLevel)npc.level(),target.zone()))return false;
            var a=first();var b=second();
            if(!CompanionLivestock.ownedClaim(a,npc,task()) || b!=null && !CompanionLivestock.ownedClaim(b,npc,task())){
                var animals=census(true);if(animals==null || !allowed(animals,target.zone().population(type(a)),kill,own()))return false;
            }
            return CompanionLivestock.reserve(a,npc,task()) && (b==null || CompanionLivestock.reserve(b,npc,task()));
        }
        public boolean occupy(PeepoEntity other){return npc.position().distanceToSqr(approachPosition())<=.64 && claim(other);}
        public void approachFailed(PeepoEntity other){release(other);nextScan=npc.level().getGameTime()+40;}
        public void release(PeepoEntity other){
            if(other!=npc)return;
            if(heldFirst!=null && firstHold!=null)heldFirst.getGoalSelector().removeGoal(firstHold);
            if(heldSecond!=null && secondHold!=null)heldSecond.getGoalSelector().removeGoal(secondHold);
            CompanionLivestock.releaseClaim(first(),npc);CompanionLivestock.releaseClaim(second(),npc);
            heldFirst=heldSecond=null;firstHold=secondHold=null;firstId=secondId=null;approach=null;phase=progress=0;npc.setWorkAnimal(-1);
        }
        public CompanionStatus work(PeepoEntity other){
            if(!occupy(other) || !((ServerLevel)npc.level()).getGameRules().get(GameRules.MOB_GRIEFING))return CompanionStatus.IDLE;
            var animal=current();
            if(kill && !weapon(npc.assignedHand()))return CompanionStatus.NEEDS_WEAPON;
            if(!kill && foodCount(first(),second())<2)return CompanionStatus.NEEDS_BREEDING_FOOD;
            if(progress==0 && npc.position().distanceToSqr(approach)>.025){npc.getMoveControl().setWantedPosition(approach.x,approach.y,approach.z,.65);return CompanionStatus.WORKING;}
            if(!npc.hasLineOfSight(animal) || npc.distanceToSqr(animal)>3)return CompanionStatus.BLOCKED;
            if(phase==0 && firstHold==null){heldFirst=animal;firstHold=CompanionLivestock.hold(animal,npc);}
            if(phase==1 && secondHold==null){heldSecond=animal;secondHold=CompanionLivestock.hold(animal,npc);}
            int effort=kill?6:4;try(var tx=Transaction.openOuter()){if(npc.extractEnergy(effort,tx)!=effort)return CompanionStatus.RECOVERING;tx.commit();}
            npc.setWorkAnimal(animal.getId());float yaw=(float)Math.toDegrees(Math.atan2(-(animal.getX()-npc.getX()),animal.getZ()-npc.getZ()));npc.setYRot(yaw);npc.yBodyRot=yaw;npc.setYHeadRot(yaw);
            if(++progress<(kill?40:60))return CompanionStatus.WORKING;
            if(!kill && phase==0){phase=1;progress=0;approach=CompanionLivestock.standing(npc,target,second());if(approach==null)return CompanionStatus.BLOCKED;return CompanionStatus.WORKING;}
            var animals=census(true);if(animals==null){progress--;return CompanionStatus.WORKING;}
            if(!allowed(animals,target.zone().population(type(animal)),kill,own()))return CompanionStatus.POPULATION_LIMIT;
            if(outputBlocked() || kill && !slaughterRoom())return CompanionStatus.FULL;
            if(kill){
                npc.setWorkAnimation(WorkAnimation.SLAUGHTER,animal.blockPosition());npc.setItemSlot(EquipmentSlot.MAINHAND,npc.assignedHand().copy());
                boolean hit;try(var drops=CompanionSlaughterLoot.capture(npc,target,animal)){hit=npc.doHurtTarget((ServerLevel)npc.level(),animal);}
                if(hit){npc.assignedHand().hurtAndBreak(1,npc,EquipmentSlot.MAINHAND);npc.belongings.setChanged();}
                progress=0;if(!animal.isAlive()){npc.transport.workChanged();nextCensus=0;return CompanionStatus.IDLE;}return hit?CompanionStatus.WORKING:CompanionStatus.FORBIDDEN;
            }
            try(var tx=Transaction.openOuter()){
                int left=2;var returns=new ArrayList<ItemStack>(2);
                for(int i=0;i<8 && left>0;i++){
                    var item=npc.belongings.getItem(i);if(npc.transport.reserved(i) || !first().isFood(item) || !second().isFood(item))continue;
                    int taken=npc.food.takeCargo(i,item,Math.min(left,item.getCount()),tx);left-=taken;
                    var remainder=item.get(DataComponents.USE_REMAINDER);if(remainder!=null)for(int n=0;n<taken;n++)returns.add(remainder.convertInto().create());
                }
                if(left!=0)return CompanionStatus.NEEDS_BREEDING_FOOD;
                for(var stack:returns)if(!npc.garden.storeHarvest(stack,target.at(),tx))return CompanionStatus.FULL;
                tx.commit();
            }
            // Normal animal AI performs mating/offspring/cooldowns. In-love parents reserve future births even after reload.
            first().setInLove(null);second().setInLove(null);nextCensus=0;npc.transport.workChanged();return CompanionStatus.IDLE;
        }
    }
}

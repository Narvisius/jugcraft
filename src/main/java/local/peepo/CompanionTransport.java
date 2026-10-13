package local.peepo;

import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity;

/** Bounded deliveries in the eight real cargo slots. No remote inventory-to-inventory transfers. */
public final class CompanionTransport extends Goal {
    private final PeepoEntity npc;
    private CompanionAssignments.Target workstation,store;
    private Identifier recipe;
    private ItemStack manifest=ItemStack.EMPTY;
    private int cargoSlot=-1;
    private record Cargo(int slot,ItemStack stack){}
    private final List<Cargo> pending=new ArrayList<>(),unused=new ArrayList<>();
    private boolean supply,returning,active,porter,tooling;
    private int route=-1,routeCursor;
    private Vec3 approach,lastPosition;
    private final CompanionNavigation.Progress travel=new CompanionNavigation.Progress();
    private long nextSearch,deadline,nextPath;
    private long handoffUntil;
    private long nextValidity;
    private boolean validRoute;
    private io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity tending;
    private long tendingStarted;
    private WorkAnimation pieAction=WorkAnimation.NONE;
    private long pieActionStarted;
    private static final int PIE_ACTION_TICKS=20;
    private CompanionStatus status=CompanionStatus.IDLE;
    private CompanionStatus porterState=CompanionStatus.IDLE;
    private final Map<BlockPos,Long> blocked=new HashMap<>();
    public CompanionTransport(PeepoEntity npc){this.npc=npc;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    /** A known job transition may need supplies or unloading; retain the shared admission budget. */
    void workChanged(){if(!active)nextSearch=0;}
    boolean hasPendingDelivery(){return !manifest.isEmpty() || tending!=null || handoffUntil!=0;}
    public boolean reserved(int slot){
        if(cargoSlot==slot && (tending!=null || !manifest.isEmpty()))return true;
        for(var cargo:pending)if(cargo.slot==slot)return true;
        for(var cargo:unused)if(cargo.slot==slot)return true;
        return false;
    }
    /** Two JE/tick for a light single stack; each additional stack or 32-item load unit adds one. */
    int movementCost(){
        int stacks=manifest.isEmpty()?0:1,items=manifest.getCount();
        for(var c:pending){stacks++;items+=c.stack.getCount();}
        for(var c:unused){stacks++;items+=c.stack.getCount();}
        return 2+Math.max(0,stacks-1)+Math.max(0,(items+31)/32-1);
    }
    public CompanionStatus activity(){return active?status:CompanionStatus.IDLE;}
    public CompanionStatus porterStatus(){return !npc.assignments.supplies.anyRoutes()?CompanionStatus.PORTER_SETUP:porterState;}
    public CompanionStatus containerStatus(int row){
        if(npc.orders.porter() && row>=CompanionAssignments.SUPPLY)return porterStatus();
        return store!=null && store.equals(npc.assignments.get(row)) && (active || !manifest.isEmpty())?status:CompanionStatus.READY;
    }
    private ItemStack carried(){
        if(cargoSlot<0 || cargoSlot>=8 || manifest.isEmpty())return ItemStack.EMPTY;
        var held=npc.belongings.getItem(cargoSlot);
        return ItemStack.isSameItemSameComponents(held,manifest)?held.copyWithCount(Math.min(held.getCount(),manifest.getCount())):ItemStack.EMPTY;
    }
    private boolean linked(){
        if(store==null || workstation==null)return false;
        if(porter)return npc.assignments.supplies.routeMatches(route,workstation,store) && !workstation.at().equals(store.at());
        if(tooling){
            boolean found=false;
            for(int i=0;i<4;i++)if(npc.assignments.supplies.tools(i) && store.equals(npc.assignments.get(CompanionAssignments.containerSlot(false,i))))found=true;
            if(!found)return false;
        }else if(!npc.assignments.supplies.contains(workstation,store,!supply))return false;
        for(int i=1;i<5;i++)if(workstation.equals(npc.assignments.get(i)))return true;
        return false;
    }
    private boolean allowed(){return npc.orders.tamed() && (npc.orders.porter() || npc.orders.mode()==CompanionOrders.Mode.WORK.ordinal()) && npc.preferences.canWork() && !npc.isEating();}
    private boolean loaded(){
        if(!linked() || !workstation.present(npc.level()) || !store.present(npc.level()) || CompanionStorage.find(npc,store)==null)return false;
        if(!porter)return tooling?CompanionJobs.permitted(npc,workstation.at().pos()):CompanionLogistics.resolve(npc,workstation)!=null;
        return CompanionStorage.find(npc,workstation)!=null
            && !CompanionAssignments.canonical(npc.level(),workstation.at().pos()).equals(CompanionAssignments.canonical(npc.level(),store.at().pos()));
    }
    private boolean travelValid(){
        long now=npc.level().getGameTime();
        if(now>=nextValidity){nextValidity=now+10;validRoute=loaded();}
        return validRoute && linked();
    }
    private void releaseTending(){if(tending!=null){tending.releaseTender(npc.getUUID());tending=null;npc.setWorkAnimation(WorkAnimation.NONE,npc.blockPosition());}}
    private boolean hearthRoute(){return !porter && !tooling && workstation!=null && workstation.block().equals(io.github.jimbozoomer.jugcraft.Jugcraft.id("hearth_oven"));}
    private void clearPiePose(){
        pieAction=WorkAnimation.NONE;npc.setWorkPie(ItemStack.EMPTY);
        if(npc.workAnimation().isPie())npc.setWorkAnimation(WorkAnimation.NONE,npc.blockPosition());
    }
    private void faceOven(){
        var pos=workstation.at().pos();float yaw=(float)Math.toDegrees(Math.atan2(-(pos.getX()+.5-npc.getX()),pos.getZ()+.5-npc.getZ()));
        npc.setYRot(yaw);npc.yBodyRot=yaw;npc.setYHeadRot(yaw);
    }
    private void piePose(WorkAnimation action,ItemStack stack){
        npc.setWorkAnimation(action,workstation.at().pos());npc.setWorkPie(stack);
    }
    /** The load commits after this reach; collection commits before the pull so a ready pie cannot burn during it. */
    private boolean animatePie(WorkAnimation action,ItemStack stack){
        long now=npc.level().getGameTime();
        if(pieAction!=action){pieAction=action;pieActionStarted=now;}
        faceOven();piePose(action,stack);npc.getNavigation().stop();
        return now-pieActionStarted>=PIE_ACTION_TICKS;
    }
    private void forget(){handoffUntil=0;releaseTending();clearPiePose();manifest=ItemStack.EMPTY;cargoSlot=-1;pending.clear();unused.clear();workstation=store=null;recipe=null;returning=porter=tooling=false;route=-1;approach=null;}
    private boolean advanceCargo(){
        clearPiePose();manifest=ItemStack.EMPTY;cargoSlot=-1;
        while(!pending.isEmpty()){
            var next=pending.removeFirst();cargoSlot=next.slot;manifest=next.stack;
            if(!carried().isEmpty())return true;
        }
        if(!unused.isEmpty()){
            pending.addAll(unused);unused.clear();returning=true;approach=null;nextPath=0;
            return advanceCargo();
        }
        manifest=ItemStack.EMPTY;cargoSlot=-1;return false;
    }
    private void finishCargo(){
        if(advanceCargo())return;
        if(porter)porterState=CompanionStatus.READY;
        if(supply && !returning && readyTending(workstation,true))return;
        beginHandoff();
    }
    private void beginHandoff(){
        // Stay here while choosing the next bounded step: return to work, restock,
        // collect another output, or continue a porter route. Never wander between legs.
        forget();handoffUntil=npc.level().getGameTime()+40;active=true;
        status=CompanionStatus.WAITING;npc.getNavigation().stop();
    }
    private void deferUnused(){
        if(!carried().isEmpty())unused.add(new Cargo(cargoSlot,carried()));
        finishCargo();
    }
    private int emptySlot(){for(int i=0;i<8;i++)if(npc.belongings.getItem(i).isEmpty())return i;return -1;}
    /** A loaded hot pie has a deadline. One helper stays until it can take the result into real cargo. */
    private boolean readyTending(CompanionAssignments.Target work,boolean here){
        if(!npc.assignments.transportAllowed(work,false))return false;
        var port=CompanionLogistics.resolve(npc,work);
        var output=npc.assignments.supplies.containers(work,true).stream().filter(t->CompanionStorage.find(npc,t)!=null).findFirst().orElse(null);
        int slot=emptySlot();
        if(port==null || slot<0 || CompanionStorage.find(npc,output)==null
            || !(npc.level().getBlockEntity(work.at().pos()) instanceof io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity oven)
            || !oven.needsTending() || !oven.claimTender(npc.getUUID()))return false;
        workstation=work;store=output;supply=returning=false;recipe=port.plan();cargoSlot=slot;
        tending=oven;tendingStarted=0;nextValidity=0;deadline=npc.level().getGameTime()+600;
        if(!here){approach=null;nextPath=0;}
        return true;
    }
    private boolean readyRoute(CompanionAssignments.Target work,boolean supplying){
        if(!npc.assignments.transportAllowed(work,supplying))return false;
        if(supplying){var port=CompanionLogistics.resolve(npc,work);if(port!=null)port.prepare(npc.assignments.supplies.combined(work,false));}
        for(var bound:npc.assignments.supplies.containers(work,!supplying))if(readyRoute(work,supplying,bound))return true;
        return false;
    }
    private boolean readyRoute(CompanionAssignments.Target work,boolean supplying,CompanionAssignments.Target bound){
        if(!npc.assignments.transportAllowed(work,supplying))return false;
        var port=CompanionLogistics.resolve(npc,work);var storage=CompanionStorage.find(npc,bound);
        if(port==null || storage==null)return false;
        if(supplying && port.plan()==null)return false;
        tooling=porter=false;route=-1;workstation=work;store=bound;supply=supplying;recipe=port.plan();returning=false;
        if(work.carriesProduce() && !supplying){
            // Harvests already occupy real slots. Probe their combined destination capacity once.
            try(var tx=Transaction.openOuter()){
                for(int i=0;i<8;i++){
                    var stack=npc.garden.output(i,work);if(stack.isEmpty())continue;
                    int room=(int)storage.insert(ItemVariant.of(stack),stack.getCount(),tx);
                    if(room>0)pending.add(new Cargo(i,stack.copyWithCount(room)));
                }
            }
            for(var cargo:pending)npc.garden.collected(cargo.slot,cargo.stack.getCount());
            return advanceCargo();
        }
        if(emptySlot()<0)return false;
        return !candidate().isEmpty();
    }
    private boolean readyPorter(int index){
        if(!npc.assignments.supplies.route(index).enabled)return false;
        route=index;tooling=false;workstation=npc.assignments.supplies.endpoint(index,false);store=npc.assignments.supplies.endpoint(index,true);
        porter=true;supply=returning=false;recipe=null;
        if(workstation==null || store==null){porterState=CompanionStatus.PORTER_SETUP;return false;}
        if(!workstation.local(npc.level()) || !store.local(npc.level())){porterState=CompanionStatus.OTHER_DIMENSION;return false;}
        if(!npc.level().hasChunkAt(workstation.at().pos()) || !npc.level().hasChunkAt(store.at().pos())){porterState=CompanionStatus.UNLOADED;return false;}
        if(!workstation.present(npc.level()) || !store.present(npc.level())){porterState=CompanionStatus.MISSING;return false;}
        if(!loaded()){porterState=CompanionStatus.FORBIDDEN;return false;}
        if(workstation.at().pos().distSqr(store.at().pos())>64*64){porterState=CompanionStatus.BLOCKED;return false;}
        if(blocked.containsKey(workstation.at().pos()))return false;
        return !candidate().isEmpty();
    }
    /** A bounded, rolled-back capacity probe. Repeated with fresh endpoints at pickup. */
    private ItemStack candidate(){
        if(tooling)return toolCandidate();
        if(!porter && !npc.assignments.transportAllowed(workstation,supply))return ItemStack.EMPTY;
        var port=porter?null:CompanionLogistics.resolve(npc,workstation);var external=CompanionStorage.find(npc,store);
        if((!porter && port==null) || external==null || supply && !Objects.equals(recipe,port.plan()))return ItemStack.EMPTY;
        var from=porter?CompanionStorage.find(npc,workstation):supply?external:port.outputs();var to=supply?port.inputs():external;
        if(from==null || to==null)return ItemStack.EMPTY;
        var limits=porter?npc.assignments.supplies.route(route):null;
        var sourceCounts=limits!=null && limits.leave>0?counts(from):null;
        var destinationCounts=limits!=null && limits.keep>0?counts(to):null;
        if(limits!=null && (limits.leave>0 && sourceCounts==null || limits.keep>0 && destinationCounts==null))return ItemStack.EMPTY;
        int views=0;boolean extractable=false;
        for(var view:from){
            if(++views>128)break;
            if(view.isResourceBlank() || view.getAmount()<=0)continue;
            var variant=view.getResource();var stack=variant.toStack(1);
            int max=stack.getMaxStackSize();
            if(limits!=null){
                if(!limits.accepts(stack))continue;
                if(sourceCounts!=null)max=Math.min(max,Math.max(0,sourceCounts.getOrDefault(stack.getItem(),0)-limits.leave));
                if(destinationCounts!=null)max=Math.min(max,Math.max(0,limits.keep-destinationCounts.getOrDefault(stack.getItem(),0)));
            }
            if(supply)max=Math.min(max,port.needed(stack));
            if(max<=0)continue;
            try(var scope=io.github.jimbozoomer.jugcraft.machine.MachineItemAutomation.companionTransfer();var tx=Transaction.openOuter()){
                int take=(int)view.extract(variant,max,tx);
                if(take<=0)continue;extractable=true;
                int room=(int)to.insert(variant,take,tx);
                if(room>0){if(porter)porterState=CompanionStatus.READY;return variant.toStack(room);}
            }
        }
        if(porter)porterState=extractable?CompanionStatus.FULL:CompanionStatus.SUPPLY_EMPTY;
        return ItemStack.EMPTY;
    }
    @Override public boolean canUse(){
        long now=npc.level().getGameTime();if(!allowed() || now<nextSearch)return false;
        if(!CompanionBudget.search(npc))return false;
        nextSearch=now+80+Math.floorMod(npc.getId(),20);
        if(!manifest.isEmpty()){
            if(!linked() || carried().isEmpty() && !advanceCargo())forget();
            else {
                if(!supply && !porter && loaded() && !outputRoom(store,carried())){
                    for(var alternative:npc.assignments.supplies.containers(workstation,true))if(outputRoom(alternative,carried())){store=alternative;break;}
                }
                return loaded();
            }
        }
        return chooseRoute(false);
    }
    /** At most four jobs and the existing bounded container/route lists; caller owns a search grant. */
    private boolean chooseRoute(boolean continuing){
        long now=npc.level().getGameTime();
        forget();if(emptySlot()<0 && npc.orders.porter()){porterState=CompanionStatus.FULL;return false;}
        blocked.entrySet().removeIf(e->e.getValue()<=now);

        // Rescue/tend an existing hot pie before ordinary job priority. At most four assigned blocks.
        for(int i=1;i<5;i++){
            var work=npc.assignments.get(i);if(work!=null && !blocked.containsKey(work.at().pos()) && readyTending(work,false))return true;
        }
        // Required tools use the same physical delivery lifecycle as ingredients.
        for(int i=1;i<5;i++){
            var work=npc.assignments.get(i);if(work!=null && !blocked.containsKey(work.at().pos()) && readyTool(work))return true;
        }
        // Respect workstation priority; clear outputs before stocking the next recipe batch.
        for(int i=1;i<5;i++){
            var work=npc.assignments.get(i);if(work==null || blocked.containsKey(work.at().pos()))continue;
            if(npc.deferJobTransport(work.at().pos()))continue;
            // A supplied processor/plot should start working before we plan another haul.
            // This also lets a higher-priority productive job interrupt a delivery chain.
            if(continuing && work.present(npc.level())){
                var job=CompanionJobs.resolve(npc,work.at().pos());
                if(job!=null && npc.orders.station(job) && job.worthStarting(npc))return false;
            }
            if(readyRoute(work,false) || readyRoute(work,true))return true;
        }
        // Generic routes yield to productive work, but do not consume a workstation assignment.
        if(npc.assignments.workApproach()==null && emptySlot()>=0)for(int i=0;i<CompanionSupplies.ROUTES;i++){
            int index=(routeCursor+i)%CompanionSupplies.ROUTES;
            if(readyPorter(index)){routeCursor=(index+1)%CompanionSupplies.ROUTES;return true;}
        }
        forget();return false;
    }
    @Override public boolean canContinueToUse(){return active && allowed() && (handoffUntil!=0 || travelValid() && npc.level().getGameTime()<deadline);}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void start(){
        handoffUntil=0;active=true;npc.resetCompanionRoutine();npc.leaveCompanionBed();npc.setRestMode(CompanionEnergy.Rest.NONE);
        deadline=npc.level().getGameTime()+600;nextPath=nextValidity=0;approach=null;travel.reset();lastPosition=npc.position();
    }
    @Override public void stop(){handoffUntil=0;active=false;clearPiePose();npc.getNavigation().stop();approach=null;if(npc.level().getGameTime()>=deadline)nextSearch=npc.level().getGameTime()+200;if(manifest.isEmpty())forget();}
    private CompanionAssignments.Target destination(){return manifest.isEmpty()?(supply?store:workstation):supply && !returning?workstation:store;}
    private void fail(CompanionStatus why){
        clearPiePose();
        if(porter)porterState=why;
        status=why;active=false;nextSearch=npc.level().getGameTime()+200;
        if(workstation!=null){if(blocked.size()>=8)blocked.clear();blocked.put(workstation.at().pos(),nextSearch);}
    }
    private void pathTo(CompanionAssignments.Target target){
        if(!target.present(npc.level()) || target.at().pos().distToCenterSqr(npc.position())>64*64){fail(CompanionStatus.UNLOADED);return;}
        var points=new ArrayList<BlockPos>(12);
        var targetState=npc.level().getBlockState(target.at().pos());
        boolean oven=targetState.getBlock() instanceof HearthOvenBlock;
        if(target.carriesProduce()){
            var floors=new LinkedHashSet<BlockPos>();
            for(var soil:target.livestock()?List.of(target.at().pos()):target.plot())if((target.livestock() || CompanionGarden.farmland(npc.level(),soil)) && CompanionJobs.permitted(npc,soil)){
                floors.add(soil);for(var side:Direction.Plane.HORIZONTAL)floors.add(soil.relative(side));
            }
            for(var floor:floors){
                if(!npc.level().hasChunkAt(floor))continue;
                var state=npc.level().getBlockState(floor);
                double top=state.getBlock() instanceof net.minecraft.world.level.block.FarmlandBlock?.9375:state.isFaceSturdy(npc.level(),floor,Direction.UP)?1:0;
                if(top==0)continue;
                var p=new Vec3(floor.getX()+.5,floor.getY()+top,floor.getZ()+.5);double r=npc.getBbWidth()/2+.01;
                if(CompanionHazards.safeAt(npc,p) && npc.level().noCollision(new AABB(p.x-r,p.y+.001,p.z-r,p.x+r,p.y+npc.getBbHeight(),p.z+r)))points.add(floor.above());
            }
        }
        for(var side:Direction.Plane.HORIZONTAL)for(int dy=0;dy>=-1;dy--){
            // The pie must enter the mouth, with a standing surface at the oven's level.
            if(oven && (side!=targetState.getValue(HearthOvenBlock.FACING) || dy!=0))continue;
            var pos=target.at().pos().relative(side).offset(0,dy,0);var p=Vec3.atBottomCenterOf(pos);var floor=pos.below();
            if(!npc.level().hasChunkAt(pos) || !npc.level().getBlockState(floor).isFaceSturdy(npc.level(),floor,Direction.UP))continue;
            double r=npc.getBbWidth()/2+.01;
            if(CompanionHazards.safeAt(npc,p) && npc.level().noCollision(new AABB(p.x-r,p.y,p.z-r,p.x+r,p.y+npc.getBbHeight(),p.z+r)))points.add(pos);
        }
        points.removeIf(p->npc.navigationMemory.failed(npc,target.at().pos(),Vec3.atBottomCenterOf(p)));
        points.sort(Comparator.comparingDouble(p->p.distToCenterSqr(npc.position())));
        // Explicit 64-block range; the multi-target overload uses the mob's shorter follow range.
        net.minecraft.world.level.pathfinder.Path path=null;
        for(int i=0;i<Math.min(2,points.size());i++){
            if(!CompanionBudget.path(npc)){status=CompanionStatus.WAITING;return;}
            npc.getNavigation().stop();
            var point=points.get(i);
            path=npc.getNavigation().createPath(point,0,64);
            if(path!=null && path.canReach())break;
            npc.navigationMemory.reject(npc,target.at().pos(),Vec3.atBottomCenterOf(point));
        }
        nextPath=npc.level().getGameTime()+40;
        if(path==null || !path.canReach()){
            if(points.size()>2){nextPath=npc.level().getGameTime()+20;return;}
            fail(CompanionStatus.BLOCKED);return;
        }
        approach=Vec3.atBottomCenterOf(path.getTarget());
        if(oven){var facing=targetState.getValue(HearthOvenBlock.FACING);approach=approach.add(-facing.getStepX()*.24,0,-facing.getStepZ()*.24);}
        npc.getNavigation().moveTo(path,1);travel.started(npc,approach);
    }
    @Override public void tick(){
        if(handoffUntil!=0){
            long now=npc.level().getGameTime();
            npc.getNavigation().stop();
            // Food-search goals share transport's priority. Yield at a safe empty-cargo
            // boundary so an endless porter/oven chain cannot prevent a needed meal.
            if(allowed() && now<handoffUntil && !(npc.needsAutomaticFood() && npc.food.meals()==0)){
                if(!CompanionBudget.search(npc))return;
                handoffUntil=0;
                if(chooseRoute(true)){start();return;}
            }
            handoffUntil=0;forget();active=false;
            nextSearch=now+80+Math.floorMod(npc.getId(),20);npc.resetCompanionRoutine();return;
        }
        if(!allowed() || !travelValid()){fail(CompanionStatus.FORBIDDEN);return;}
        long now=npc.level().getGameTime();
        if(tending!=null && (tending.isRemoved() || !tending.claimTender(npc.getUUID()))){fail(CompanionStatus.OCCUPIED);return;}
        if(!manifest.isEmpty() && carried().isEmpty()){finishCargo();if(!active || handoffUntil!=0)return;}
        if(!porter && !tooling && !npc.assignments.transportAllowed(workstation,supply)){
            if(manifest.isEmpty()){forget();active=false;return;}
            if(supply && !returning){clearPiePose();returning=true;approach=null;nextPath=0;}
            // Already collected outputs finish their delivery; unused supplies return to their source.
        }
        if(pieAction==WorkAnimation.PIE_TAKE){
            if(!animatePie(WorkAnimation.PIE_TAKE,carried()))return;
            clearPiePose();approach=null;nextPath=0;
        }
        var visibleCargo=carried();
        if(pieAction!=WorkAnimation.PIE_LOAD && hearthRoute() && !visibleCargo.isEmpty()
            && (HearthOvenBlockEntity.selectedFilling(visibleCargo)!=null || (visibleCargo.is(io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture.item("burnt_pie")) || visibleCargo.is(io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture.item("burnt_cake")))))
            piePose(WorkAnimation.PIE_CARRY,visibleCargo);
        status=manifest.isEmpty()?(supply || porter?CompanionStatus.FETCHING_SUPPLIES:CompanionStatus.COLLECTING_OUTPUT):returning?CompanionStatus.RETURNING_SUPPLIES:CompanionStatus.DELIVERING;
        if(porter)porterState=status;
        var target=destination();
        if(approach!=null && !CompanionHazards.safeAt(npc,approach)){
            npc.getNavigation().stop();fail(CompanionStatus.BLOCKED);return;
        }
        if(approach==null || npc.position().distanceToSqr(approach)>.64){
            if(pieAction==WorkAnimation.PIE_LOAD)clearPiePose();
            if(now>=nextPath && (approach==null || travel.needsPath(npc,approach)))pathTo(target);
            if(npc.position().distanceToSqr(lastPosition)>.0001)try(var tx=Transaction.openOuter()){npc.extractEnergy(movementCost(),tx);tx.commit();}
            lastPosition=npc.position();return;
        }
        npc.getNavigation().stop();
        if(hearthRoute() && target.equals(workstation) && npc.position().distanceToSqr(approach)>.0225){
            if(pieAction==WorkAnimation.PIE_LOAD)clearPiePose();
            npc.getMoveControl().setWantedPosition(approach.x,approach.y,approach.z,.7);return;
        }
        if(tending!=null){
            if(target.at().pos().distToCenterSqr(npc.position())>6.25){fail(CompanionStatus.BLOCKED);return;}
            tendAtWork();return;
        }
        // Never rely on the travel cache for an inventory mutation.
        if(!loaded()){fail(CompanionStatus.FORBIDDEN);return;}
        if(supply && !tooling && !manifest.isEmpty() && !returning){
            var port=CompanionLogistics.resolve(npc,workstation);
            if(!Objects.equals(recipe,port.plan())){clearPiePose();returning=true;approach=null;nextPath=0;return;}
            if(port.needed(carried())<=0){deferUnused();return;}
        }
        boolean near=target.garden()?target.plot().stream().anyMatch(p->CompanionGarden.farmland(npc.level(),p) && CompanionJobs.permitted(npc,p) && p.distToCenterSqr(npc.position())<=6.25):target.at().pos().distToCenterSqr(npc.position())<=6.25;
        if(!near){fail(CompanionStatus.BLOCKED);return;}
        if(hearthRoute() && supply && !returning && HearthOvenBlockEntity.rawFilling(carried())!=null){
            if(!animatePie(WorkAnimation.PIE_LOAD,carried()))return;
        }
        if(manifest.isEmpty())pickup();else deliver();
    }
    private void tendAtWork(){
        long now=npc.level().getGameTime();
        if(!tending.needsTending()){beginHandoff();return;}
        if(tendingStarted==0){tendingStarted=now;deadline=now+800;}
        status=CompanionStatus.WORKING;
        faceOven();piePose(WorkAnimation.PIE_WAIT,ItemStack.EMPTY);
        var outputs=tending.companionOutputs();
        for(var view:outputs){
            if(view.isResourceBlank() || view.getAmount()==0)continue;
            // Waiting uses the ten-tick route cache; collection rechecks access immediately.
            if(!loaded()){fail(CompanionStatus.FORBIDDEN);return;}
            int slot=cargoSlot>=0 && npc.belongings.getItem(cargoSlot).isEmpty()?cargoSlot:emptySlot();
            if(slot<0){fail(CompanionStatus.FULL);return;}
            var variant=view.getResource();var stack=variant.toStack(1);
            try(var tx=Transaction.openOuter()){
                if(view.extract(variant,1,tx)!=1 || !npc.food.putCargo(slot,stack,tx))return;
                if(npc.extractEnergy(16,tx)<=0){fail(CompanionStatus.RECOVERING);return;}
                tx.commit();cargoSlot=slot;manifest=stack.copy();
            }
            releaseTending();animatePie(WorkAnimation.PIE_TAKE,carried());approach=null;nextPath=0;deadline=now+600;return;
        }
    }
    private void pickup(){
        if(!tooling && !workstation.carriesProduce()){pickupBatch();return;}
        var selected=candidate();int slot=emptySlot();
        if(selected.isEmpty() || slot<0){fail(slot<0?CompanionStatus.FULL:porter?porterState:CompanionStatus.NO_INPUT);return;}
        var port=porter || tooling?null:CompanionLogistics.resolve(npc,workstation);
        var source=porter?CompanionStorage.find(npc,workstation):supply?CompanionStorage.find(npc,store):port.outputs();
        try(var scope=io.github.jimbozoomer.jugcraft.machine.MachineItemAutomation.companionTransfer();var tx=Transaction.openOuter()){
            int taken=(int)source.extract(ItemVariant.of(selected),selected.getCount(),tx);
            if(taken<=0){fail(CompanionStatus.NO_INPUT);return;}
            var stack=selected.copyWithCount(taken);
            if(!npc.food.putCargo(slot,stack,tx)){fail(CompanionStatus.FULL);return;}
            if(!porter && !supply && npc.level().getBlockEntity(workstation.at().pos()) instanceof io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity
                && npc.extractEnergy(16,tx)<=0){fail(CompanionStatus.RECOVERING);return;}
            tx.commit();cargoSlot=slot;manifest=stack.copy();
        }
        approach=null;nextPath=0;deadline=npc.level().getGameTime()+600;
        if(hearthRoute() && !supply)animatePie(WorkAnimation.PIE_TAKE,carried());
    }
    private void pickupBatch(){
        var port=porter?null:CompanionLogistics.resolve(npc,workstation);
        if(!porter && (port==null || !npc.assignments.transportAllowed(workstation,supply)
                || supply && !Objects.equals(recipe,port.plan()))){fail(CompanionStatus.NO_INPUT);return;}
        var source=porter?CompanionStorage.find(npc,workstation):supply?CompanionStorage.find(npc,store):port.outputs();
        var destination=supply?port.inputs():CompanionStorage.find(npc,store);
        var slots=new ArrayList<Integer>();for(int i=0;i<8;i++)if(npc.belongings.getItem(i).isEmpty())slots.add(i);
        var batch=CompanionDeliveryPlan.create(source,destination,slots.size(),supply?port::needed:null,porter?npc.assignments.supplies.route(route):null);
        if(batch.isEmpty()){fail(slots.isEmpty()?CompanionStatus.FULL:porter?CompanionStatus.FULL:CompanionStatus.NO_INPUT);return;}
        try(var scope=io.github.jimbozoomer.jugcraft.machine.MachineItemAutomation.companionTransfer();var tx=Transaction.openOuter()){
            for(int i=0;i<batch.size();i++){
                var stack=batch.get(i);
                if(source.extract(ItemVariant.of(stack),stack.getCount(),tx)!=stack.getCount()
                        || !npc.food.putCargo(slots.get(i),stack,tx)){fail(CompanionStatus.NO_INPUT);return;}
            }
            if(hearthRoute() && !supply && npc.extractEnergy(16,tx)<=0){fail(CompanionStatus.RECOVERING);return;}
            tx.commit();
        }
        for(int i=0;i<batch.size();i++)pending.add(new Cargo(slots.get(i),batch.get(i).copy()));
        advanceCargo();approach=null;nextPath=0;deadline=npc.level().getGameTime()+600;
        if(hearthRoute() && !supply)animatePie(WorkAnimation.PIE_TAKE,carried());
    }
    private void deliver(){
        if(tooling && !returning){deliverTool();return;}
        var stack=carried();var port=porter || tooling?null:CompanionLogistics.resolve(npc,workstation);
        Storage<ItemVariant> destination=supply && !returning?port.inputs():CompanionStorage.find(npc,store);
        if(destination==null){fail(CompanionStatus.FORBIDDEN);return;}
        int amount=stack.getCount();
        if(porter){var r=npc.assignments.supplies.route(route);if(r.keep>0){var counts=counts(destination);amount=counts==null?0:Math.min(amount,Math.max(0,r.keep-counts.getOrDefault(stack.getItem(),0)));}}
        if(porter && amount<=0){fail(CompanionStatus.FULL);return;}
        if(supply && !returning)amount=Math.min(amount,port.needed(stack));
        if(amount<=0){deferUnused();return;}
        if(supply && !returning && workstation.carriesProduce()){
            // Pickup already put these seeds into this companion's inventory. Do not insert a second copy.
            npc.garden.inputsChanged(workstation.at().pos());npc.livestock.inputsChanged(workstation.at().pos());beginHandoff();return;
        }
        int inserted;
        try(var scope=io.github.jimbozoomer.jugcraft.machine.MachineItemAutomation.companionTransfer();var tx=Transaction.openOuter()){
            inserted=(int)destination.insert(ItemVariant.of(stack),amount,tx);
            if(inserted<=0){
                if(supply && !returning)deferUnused();else fail(CompanionStatus.FULL);
                return;
            }
            if(npc.food.takeCargo(cargoSlot,stack,inserted,tx)!=inserted)return;
            tx.commit();
        }
        manifest.shrink(inserted);
        clearPiePose();
        if(manifest.isEmpty())finishCargo();
        else if(supply && !returning)deferUnused();
        else fail(CompanionStatus.FULL);
    }
    /** Bounded counts by item type, matching the user-facing ghost filter. */
    private Map<net.minecraft.world.item.Item,Integer> counts(Storage<ItemVariant> storage){
        var result=new HashMap<net.minecraft.world.item.Item,Integer>();int views=0;
        for(var view:storage){
            if(++views>128)return null; // Unknown totals must not bypass a stock cap.
            if(!view.isResourceBlank()){var item=view.getResource().getItem();result.put(item,(int)Math.min(1_000_000L,result.getOrDefault(item,0)+view.getAmount()));}
        }
        return result;
    }
    private boolean outputRoom(CompanionAssignments.Target target,ItemStack stack){
        var storage=CompanionStorage.find(npc,target);if(storage==null || stack.isEmpty())return false;
        try(var tx=Transaction.openOuter()){return storage.insert(ItemVariant.of(stack),stack.getCount(),tx)>0;}
    }
    private boolean toolFits(CompanionAssignments.Target work,ItemStack stack){
        if(stack.isEmpty() || stack.isDamageableItem() && stack.getDamageValue()>=stack.getMaxDamage())return false;
        return work.shearing()?stack.is(net.minecraft.world.item.Items.SHEARS):work.garden()?stack.is(net.minecraft.tags.ItemTags.HOES):work.block().equals(io.github.jimbozoomer.jugcraft.Jugcraft.id("cutting_board")) && stack.is(io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture.KNIVES);
    }
    private boolean needsTool(CompanionAssignments.Target work){
        return !npc.assignments.supplies.handLocked() && work.present(npc.level()) && CompanionJobs.permitted(npc,work.at().pos())
            && (work.garden() || work.shearing() || work.block().equals(io.github.jimbozoomer.jugcraft.Jugcraft.id("cutting_board"))) && !toolFits(work,npc.belongings.getItem(9));
    }
    private boolean readyTool(CompanionAssignments.Target work){
        if(!npc.assignments.supplies.anyTools() || !needsTool(work))return false;
        if(work.shearing() && !npc.livestock.hasReadyAnimal(work))return false;
        // Do not swap back and forth between tools for idle/lower-priority jobs.
        if(work.garden()){
            var job=CompanionJobs.resolve(npc,work.at().pos());if(job==null || job.planningStatus(npc)!=CompanionStatus.READY)return false;
        }else if(npc.level().getBlockEntity(work.at().pos()) instanceof io.github.jimbozoomer.jugcraft.agriculture.CuttingBoardBlockEntity board && board.item().isEmpty()){
            var port=CompanionLogistics.resolve(npc,work);if(port==null)return false;
            port.prepare(npc.assignments.supplies.combined(work,false));if(port.plan()==null)return false;
        }
        for(int row=1;row<npc.assignments.workPriority(work.at().pos());row++){
            var higher=npc.assignments.get(row);if(higher==null || !higher.present(npc.level()))continue;
            var job=CompanionJobs.resolve(npc,higher.at().pos());if(job!=null && job.planningStatus(npc)==CompanionStatus.READY)return false;
        }
        var useful=toolUsefulness(work);
        for(int i=0;i<4;i++)if(npc.assignments.supplies.tools(i)){
            var source=npc.assignments.get(CompanionAssignments.containerSlot(false,i));if(CompanionStorage.find(npc,source)==null)continue;
            workstation=work;store=source;tooling=supply=true;porter=returning=false;recipe=null;route=-1;
            for(int slot=0;slot<8;slot++)if(npc.belongings.getItem(slot).getCount()==1 && toolFits(work,npc.belongings.getItem(slot)) && useful.test(npc.belongings.getItem(slot))){cargoSlot=slot;manifest=npc.belongings.getItem(slot).copy();return true;}
            if(emptySlot()>=0 && !toolCandidate(useful).isEmpty())return true;
        }
        tooling=false;return false;
    }
    private ItemStack toolCandidate(){
        return toolCandidate(toolUsefulness(workstation));
    }
    private ItemStack toolCandidate(java.util.function.Predicate<ItemStack> useful){
        if(!needsTool(workstation))return ItemStack.EMPTY;
        var source=CompanionStorage.find(npc,store);if(source==null)return ItemStack.EMPTY;
        int views=0;
        for(var view:source){
            if(++views>32)break;if(view.isResourceBlank() || view.getAmount()==0)continue;
            var item=view.getResource().toStack(1);if(!toolFits(workstation,item) || !useful.test(item))continue;
            try(var tx=Transaction.openOuter()){if(view.extract(view.getResource(),1,tx)==1)return item;}
        }
        return ItemStack.EMPTY;
    }
    private java.util.function.Predicate<ItemStack> toolUsefulness(CompanionAssignments.Target work){
        if(work.garden() || work.shearing())return tool->true;
        if(!(npc.level().getBlockEntity(work.at().pos()) instanceof io.github.jimbozoomer.jugcraft.agriculture.CuttingBoardBlockEntity board)
            || !(npc.level() instanceof net.minecraft.server.level.ServerLevel level))return tool->false;
        // One rolled-back input snapshot per search, not one full scan for every knife in storage.
        var inputs=board.item().isEmpty()?RecipeSupplies.available(npc.assignments.supplies.combined(work,false)):List.of(board.item().copy());
        var distinct=new HashMap<net.minecraft.world.item.Item,ItemStack>();for(var raw:inputs)distinct.putIfAbsent(raw.getItem(),raw);
        var memo=new HashMap<net.minecraft.world.item.Item,Boolean>();
        return tool->memo.computeIfAbsent(tool.getItem(),item->distinct.values().stream().anyMatch(raw->
            io.github.jimbozoomer.jugcraft.agriculture.CuttingRecipe.find(level.getServer(),raw,tool)
                .map(r->r.results().stream().anyMatch(result->board.companionKitchen.filter.allows(result.create()))).orElse(false)));
    }
    private void deliverTool(){
        var stack=carried();
        if(!needsTool(workstation) || !toolFits(workstation,stack)){
            returning=true;approach=null;nextPath=0;return;
        }
        // At the job: move the previous hand item into the vacated cargo slot atomically.
        try(var tx=Transaction.openOuter()){
            if(!npc.food.equipCargo(cargoSlot,stack,tx))return;
            tx.commit();
        }
        if(workstation.garden())npc.garden.inputsChanged(workstation.at().pos());
        if(workstation.livestock())npc.livestock.inputsChanged(workstation.at().pos());
        beginHandoff();
    }
    private static void saveTarget(ValueOutput out,String name,CompanionAssignments.Target t){
        var child=out.child(name);child.store("At",GlobalPos.CODEC,t.at());child.store("Block",Identifier.CODEC,t.block());child.putInt("Face",t.face().ordinal());
        if(t.garden())child.store("Plot",BlockPos.CODEC.listOf(),t.plot());
    }
    private static CompanionAssignments.Target loadTarget(ValueInput in,String name){
        var c=in.child(name);if(c.isEmpty())return null;var at=c.get().read("At",GlobalPos.CODEC).orElse(null);var id=c.get().read("Block",Identifier.CODEC).orElse(null);
        return at==null || id==null?null:new CompanionAssignments.Target(at,id,Direction.values()[Math.clamp(c.get().getIntOr("Face",1),0,5)],c.get().read("Plot",BlockPos.CODEC.listOf()).orElse(List.of()));
    }
    public void save(ValueOutput out){
        if(manifest.isEmpty() || workstation==null || store==null)return;
        var c=out.child("Transport");c.store("Manifest",ItemStack.CODEC,manifest);c.putInt("Slot",cargoSlot);c.putBoolean("Supply",supply);c.putBoolean("Returning",returning);
        c.putBoolean("Porter",porter);c.putBoolean("Tool",tooling);c.putInt("Route",route);
        saveCargo(c,"Pending",pending);saveCargo(c,"Unused",unused);
        if(recipe!=null)c.store("Recipe",Identifier.CODEC,recipe);saveTarget(c,"Work",workstation);saveTarget(c,"Store",store);
    }
    public void load(ValueInput in){
        forget();var c=in.child("Transport");if(c.isEmpty())return;
        workstation=loadTarget(c.get(),"Work");store=loadTarget(c.get(),"Store");cargoSlot=c.get().getIntOr("Slot",-1);
        manifest=c.get().read("Manifest",ItemStack.CODEC).orElse(ItemStack.EMPTY);supply=c.get().getBooleanOr("Supply",false);returning=c.get().getBooleanOr("Returning",false);recipe=c.get().read("Recipe",Identifier.CODEC).orElse(null);
        tooling=c.get().getBooleanOr("Tool",false);route=Math.clamp(c.get().getIntOr("Route",0),0,3);porter=c.get().getBooleanOr("Porter",false);if(porter){supply=returning=false;recipe=null;}
        var seen=new HashSet<Integer>();seen.add(cargoSlot);
        loadCargo(c.get(),"Pending",pending,seen);loadCargo(c.get(),"Unused",unused,seen);
        if(workstation==null || store==null || carried().isEmpty() && !advanceCargo())forget();
    }
    private static void saveCargo(ValueOutput out,String name,List<Cargo> entries){
        for(int i=0;i<entries.size();i++){var c=out.child(name+i);c.putInt("Slot",entries.get(i).slot);c.store("Stack",ItemStack.CODEC,entries.get(i).stack);}
    }
    private void loadCargo(ValueInput in,String name,List<Cargo> entries,Set<Integer> seen){
        for(int i=0;i<7;i++){
            var child=in.child(name+i);if(child.isEmpty())continue;
            int slot=child.get().getIntOr("Slot",-1);var stack=child.get().read("Stack",ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if(slot<0 || slot>=8 || stack.isEmpty() || !seen.add(slot))continue;
            var held=npc.belongings.getItem(slot);
            if(ItemStack.isSameItemSameComponents(held,stack))entries.add(new Cargo(slot,stack.copyWithCount(Math.min(stack.getCount(),held.getCount()))));
        }
    }
}

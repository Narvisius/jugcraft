package local.peepo;

import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.*;

/** Bounded per-companion storage preferences. Endpoints remain stable assignment slots. */
public final class CompanionSupplies {
    public static final int ROUTES=4;
    public static final int[] LIMITS={0,16,32,64,128,256,512};
    private final PeepoEntity npc;
    static final class SharedStorage extends CombinedStorage<ItemVariant,Storage<ItemVariant>> {
        final int viewLimit;
        SharedStorage(List<Storage<ItemVariant>> parts){super(parts);viewLimit=Math.min(4,parts.size())*128;}
    }
    // Zero means all; bit four distinguishes an explicit empty selection from all.
    private final int[][] links=new int[4][2];
    private int tools;
    private boolean handLocked;
    public static final class Route {
        public int source,destination,leave,keep;
        public boolean enabled;
        private final ItemStack[] filter=new ItemStack[9];
        Route(){Arrays.fill(filter,ItemStack.EMPTY);}
        public ItemStack icon(int i){return filter[i].copy();}
        public void filter(int i,ItemStack item){if(i>=0 && i<9)filter[i]=item.isEmpty()?ItemStack.EMPTY:new ItemStack(item.getItem());}
        public boolean accepts(ItemStack item){boolean any=false;for(var icon:filter)if(!icon.isEmpty()){any=true;if(item.is(icon.getItem()))return true;}return !any;}
    }
    private final Route[] routes=new Route[ROUTES];
    CompanionSupplies(PeepoEntity npc){this.npc=npc;Arrays.setAll(routes,i->new Route());}
    public Route route(int i){return routes[i];}
    public boolean tools(int i){return (tools&(1<<i))!=0;}
    public boolean anyTools(){return tools!=0;}
    public boolean handLocked(){return handLocked;}
    public void toggleTools(int i){if(i>=0 && i<4)tools^=1<<i;}
    public void toggleHandLock(){handLocked=!handLocked;}
    public int mask(int row,boolean output){return row>=1 && row<=4?links[row-1][output?1:0]:0;}
    public void link(int row,boolean output,int choice){
        if(row<1 || row>4 || choice<0 || choice>4)return;
        int side=output?1:0,value=links[row-1][side];
        links[row-1][side]=choice==0?0:16|(value==0?1<<(choice-1):(value&15)^(1<<(choice-1)));
        npc.readiness.clear();
    }
    public List<CompanionAssignments.Target> containers(CompanionAssignments.Target work,boolean output){
        int row=work==null?0:npc.assignments.workPriority(work.at().pos()),mask=mask(row,output);
        var result=new ArrayList<CompanionAssignments.Target>(4);
        for(int i=0;i<4;i++){
            var target=npc.assignments.get(CompanionAssignments.containerSlot(output,i));
            if(target!=null && (mask==0 || (mask&(1<<i))!=0))result.add(target);
        }
        return result;
    }
    public boolean contains(CompanionAssignments.Target work,CompanionAssignments.Target target,boolean output){
        if(target==null)return false;
        int row=work==null?0:npc.assignments.workPriority(work.at().pos()),mask=mask(row,output);
        for(int i=0;i<4;i++)if((mask==0 || (mask&(1<<i))!=0) && target.equals(npc.assignments.get(CompanionAssignments.containerSlot(output,i))))return true;
        return false;
    }
    /** Recipe planning may see ingredients split across four chests; mutations still walk to one chest. */
    public Storage<ItemVariant> combined(CompanionAssignments.Target work,boolean output){
        var parts=new ArrayList<Storage<ItemVariant>>(4);
        var physical=new HashSet<net.minecraft.core.BlockPos>();
        for(var target:containers(work,output)){
            var storage=CompanionStorage.find(npc,target);
            if(storage!=null && physical.add(CompanionAssignments.canonical(npc.level(),target.at().pos())))parts.add(storage);
        }
        return new SharedStorage(parts);
    }
    public CompanionAssignments.Target endpoint(int route,boolean output){
        var r=routes[route];return npc.assignments.get(CompanionAssignments.containerSlot(output,output?r.destination:r.source));
    }
    public boolean routeMatches(int route,CompanionAssignments.Target from,CompanionAssignments.Target to){
        return route>=0 && route<ROUTES && routes[route].enabled && from!=null && to!=null
            && from.equals(endpoint(route,false)) && to.equals(endpoint(route,true));
    }
    public boolean anyRoutes(){for(var route:routes)if(route.enabled)return true;return false;}
    void cleared(int slot){
        if(slot>=1 && slot<=4){Arrays.fill(links[slot-1],0);return;}
        if(slot<CompanionAssignments.SUPPLY)return;
        int index=(slot-CompanionAssignments.SUPPLY)/2;boolean output=!CompanionAssignments.supplySlot(slot);
        if(!output)tools&=~(1<<index);
        for(var row:links)if(row[output?1:0]!=0)row[output?1:0]&=~(1<<index);
        for(var route:routes)if((output?route.destination:route.source)==index)route.enabled=false;
    }
    void moveRow(int from,int to){if(from!=to){links[to-1]=links[from-1].clone();Arrays.fill(links[from-1],0);}}
    void swapRows(int a,int b){var tmp=links[a-1];links[a-1]=links[b-1];links[b-1]=tmp;}
    public void save(ValueOutput out){
        var c=out.child("SharedSupplies");c.putInt("Tools",tools);c.putBoolean("HandLocked",handLocked);
        for(int i=0;i<4;i++){
            c.putInt("SupplyLinks"+i,links[i][0]);c.putInt("OutputLinks"+i,links[i][1]);
            var r=routes[i];var v=c.child("Route"+i);v.putInt("Source",r.source);v.putInt("Destination",r.destination);
            v.putBoolean("Enabled",r.enabled);v.putInt("Leave",r.leave);v.putInt("Keep",r.keep);
            for(int j=0;j<9;j++)if(!r.filter[j].isEmpty())v.store("Filter"+j,ItemStack.CODEC,r.filter[j]);
        }
    }
    public void load(ValueInput in){
        for(var row:links)Arrays.fill(row,0);tools=0;handLocked=false;Arrays.setAll(routes,i->new Route());
        var child=in.child("SharedSupplies");
        if(child.isEmpty()){
            // Old dedicated porters become Work companions with the original route enabled.
            if(in.getIntOr("CompanionCommand",3)==4)routes[0].enabled=true;
            return;
        }
        var c=child.get();tools=c.getIntOr("Tools",0)&15;handLocked=c.getBooleanOr("HandLocked",false);
        for(int i=0;i<4;i++){
            links[i][0]=c.getIntOr("SupplyLinks"+i,0)&31;links[i][1]=c.getIntOr("OutputLinks"+i,0)&31;
            var entry=c.child("Route"+i);if(entry.isEmpty())continue;var v=entry.get();var r=routes[i];
            r.source=Math.clamp(v.getIntOr("Source",0),0,3);r.destination=Math.clamp(v.getIntOr("Destination",0),0,3);
            r.enabled=v.getBooleanOr("Enabled",false);r.leave=Math.clamp(v.getIntOr("Leave",0),0,512);r.keep=Math.clamp(v.getIntOr("Keep",0),0,512);
            for(int j=0;j<9;j++)r.filter(j,v.read("Filter"+j,ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
    }
}

package local.peepo;

import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CiderPressBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CanningKettleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity;

/** Explicit workstation ports. Future ovens/processors opt in without changing porter navigation. */
public final class CompanionLogistics {
    public interface Port {
        Identifier plan();
        default void prepare(Storage<ItemVariant> source){}
        int needed(ItemStack stack);
        default int needed(ItemStack stack,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){return needed(stack);}
        Storage<ItemVariant> inputs();
        Storage<ItemVariant> outputs();
        /** Status for logistics-only stations that process without a resident helper. */
        default CompanionStatus status(){return CompanionStatus.READY;}
    }
    public interface Adapter { Port resolve(PeepoEntity npc,BlockEntity block); }
    private static final List<Adapter> ADAPTERS=new ArrayList<>();
    public static void register(Adapter adapter){ADAPTERS.add(Objects.requireNonNull(adapter));}
    public static Port resolve(PeepoEntity npc,CompanionAssignments.Target target){
        if(target==null || !target.present(npc.level()) || !npc.assignments.assignedWork(target.at().pos()) || !CompanionJobs.permitted(npc,target.at().pos()))return null;
        if(target.garden())return npc.garden.port(target);
        if(target.livestock())return npc.livestock.port(target);
        var be=npc.level().getBlockEntity(target.at().pos());
        if(be instanceof io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity machine
            && machine.kind().supportsCompanionAssistance() && !machine.isLocked())return machine.companionPort;
        var kitchen=KitchenCompanionPort.of(be);if(kitchen!=null)return kitchen.forCompanion(npc);
        if(be instanceof HearthOvenBlockEntity oven)return new Port(){
            public Identifier plan(){return Identifier.fromNamespaceAndPath("jugcraft","hearth_oven");}
            public int needed(ItemStack candidate){
                return needed(candidate,null);
            }
            public int needed(ItemStack candidate,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext parent){
                int need=oven.companionNeed(candidate);if(need<=0)return 0;
                var output=npc.assignments.supplies.combined(target,true);
                if(output==null)return 0;
                var filling=HearthOvenBlockEntity.rawFilling(candidate);
                if(filling!=null)try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openNested(parent)){
                    var pie=ItemVariant.of(io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture.item(filling.pie()));
                    if(output.insert(pie,1,tx)!=1)return 0;
                }
                return need;
            }
            public Storage<ItemVariant> inputs(){return oven.companionInputs();}
            public Storage<ItemVariant> outputs(){return oven.companionOutputs();}
            public CompanionStatus status(){return oven.companionStatus();}
        };
        if(be instanceof CanningKettleBlockEntity kettle)return new Port(){
            public Identifier plan(){return Identifier.fromNamespaceAndPath("jugcraft","canning_kettle");}
            public int needed(ItemStack candidate){return kettle.companionNeed(candidate);}
            public Storage<ItemVariant> inputs(){return kettle.companionInputs();}
            public Storage<ItemVariant> outputs(){return kettle.companionOutputs();}
            public CompanionStatus status(){return kettle.companionStatus();}
        };
        if(be instanceof CiderPressBlockEntity press)return new Port(){
            public Identifier plan(){return Identifier.fromNamespaceAndPath("jugcraft","sweet_cider");}
            public int needed(ItemStack candidate){return press.companionNeed(candidate);}
            public Storage<ItemVariant> inputs(){return press.companionInputs();}
            public Storage<ItemVariant> outputs(){return press.companionOutputs();}
        };
        if(be instanceof CookingPotBlockEntity pot && !pot.isLocked())return new Port(){
            public void prepare(Storage<ItemVariant> source){pot.prepareCompanionRecipe(source);}
            public Identifier plan(){return pot.supplyPlan().map(p->p.id()).orElse(null);}
            public int needed(ItemStack candidate){
                var plan=pot.supplyPlan().orElse(null);if(plan==null)return 0;
                int[] available=new int[CookingPotBlockEntity.INPUTS];
                for(int i=0;i<available.length;i++)available[i]=pot.getItem(i).getCount();
                int missing=0;
                for(var part:plan.ingredients()){
                    int left=part.count();
                    for(int i=0;i<available.length && left>0;i++)if(part.ingredient().test(pot.getItem(i))){int take=Math.min(left,available[i]);left-=take;available[i]-=take;}
                    if(part.ingredient().test(candidate))missing=Math.min(32,missing+Math.min(32,left));
                }
                return Math.min(32,missing);
            }
            public Storage<ItemVariant> inputs(){return ContainerStorage.of(pot,Direction.UP);}
            public Storage<ItemVariant> outputs(){return ContainerStorage.of(pot,Direction.DOWN);}
        };
        for(var adapter:ADAPTERS){var port=adapter.resolve(npc,be);if(port!=null)return port;}
        return null;
    }
}

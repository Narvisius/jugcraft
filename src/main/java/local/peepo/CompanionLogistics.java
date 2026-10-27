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
        /** Cheap discovery demand; a pickup may expand this into a larger balanced batch. */
        default int pickupNeeded(ItemStack stack){return needed(stack);}
        /** Fuel, retained tools and ceramics are not production ingredients. */
        default int stockAuxiliaryNeed(ItemStack stack){return 0;}
        default double supplyPriority(ItemStack stack){return 0;}
        default int needed(ItemStack stack,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){return needed(stack);}
        Storage<ItemVariant> inputs();
        /** Keeps dual-purpose ingredients out of a fuel slot (and fuel out of recipe slots) in limited batches. */
        default Storage<ItemVariant> stockInputs(java.util.function.ToIntFunction<ItemStack> ingredientRoom){return inputs();}
        Storage<ItemVariant> outputs();
        /** Pickup-only planning; the real transfer still commits in CompanionTransport. */
        default List<ItemStack> supplyBatch(Storage<ItemVariant> source,int slots){
            return CompanionDeliveryPlan.create(source,inputs(),slots,this::needed,null);
        }
        /** Status for logistics-only stations that process without a resident helper. */
        default CompanionStatus status(){return CompanionStatus.READY;}
    }
    public interface Adapter { Port resolve(PeepoEntity npc,BlockEntity block); }
    private static final List<Adapter> ADAPTERS=new ArrayList<>();
    public static void register(Adapter adapter){ADAPTERS.add(Objects.requireNonNull(adapter));}
    public static Port resolve(PeepoEntity npc,CompanionAssignments.Target target){
        var base=resolveBase(npc,target);if(base==null || !target.present(npc.level()))return base;
        var block=npc.level().getBlockEntity(target.at().pos());var stock=CompanionProductionStock.of(block);
        return stock==null?base:stock.wrap(npc,block,target,base);
    }
    private static Port resolveBase(PeepoEntity npc,CompanionAssignments.Target target){
        if(target==null || !target.present(npc.level()) || !npc.assignments.assignedWork(target.at().pos()) || !CompanionJobs.permitted(npc,target.at().pos()))return null;
        if(target.fishing())return npc.fishing.port(target);
        if(target.water())return npc.water.port(target);
        if(target.treeTap())return npc.treeTaps.port(target);
        if(target.garden())return npc.garden.port(target);
        if(target.livestock())return npc.livestock.port(target);
        if(npc.level() instanceof net.minecraft.server.level.ServerLevel server && npc.level().getBlockState(target.at().pos()).getBlock() instanceof net.minecraft.world.level.block.ComposterBlock)return new ComposterCompanionPort(server,target.at().pos());
        var be=npc.level().getBlockEntity(target.at().pos());
        if(be instanceof CraftingTableCompanionBlockEntity table)return table.accessible(npc)?table.forCompanion(npc):null;
        if(be instanceof io.github.jimbozoomer.jugcraft.agriculture.BatHouseBlockEntity)return new Port(){
            public Identifier plan(){return null;}
            public int needed(ItemStack stack){return 0;}
            public Storage<ItemVariant> inputs(){return Storage.empty();}
            public Storage<ItemVariant> outputs(){return Storage.empty();} // Collected guano is already marked cargo.
        };
        if(be instanceof io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity form)return form.companionPort.accessible(npc)?form.companionPort.forCompanion(npc):null;
        if(be instanceof io.github.jimbozoomer.jugcraft.workshop.WorkshopBlockEntity workshop)return workshop.companion.accessible(npc)?workshop.companion.forCompanion(npc):null;
        if(be instanceof io.github.jimbozoomer.jugcraft.farming.SprinklerBlockEntity sprinkler)return sprinkler.companion.forCompanion(npc);
        var serving=ServingCompanion.of(be);if(serving!=null)return serving.forCompanion(npc);
        var magic=MagicGardenCompanion.of(npc,be);if(magic!=null)return magic;
        if(be instanceof io.github.jimbozoomer.jugcraft.agriculture.FarmStandBlockEntity stand)return stand.companion.forCompanion(npc);
        if(be instanceof io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity crucible)return crucible.companion;
        var vanilla=VanillaCompanionPort.of(be);if(vanilla!=null)return vanilla;
        if(be instanceof io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity machine
            && machine.kind().supportsCompanionLogistics() && !machine.isLocked())return machine.companionPort;
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
        if(be instanceof io.github.jimbozoomer.jugcraft.agriculture.CandyKettleBlockEntity kettle)return kettle.companion;
        if(be instanceof CanningKettleBlockEntity kettle)return new Port(){
            public Identifier plan(){return Identifier.fromNamespaceAndPath("jugcraft","canning_kettle");}
            public int needed(ItemStack candidate){return kettle.companionNeed(candidate);}
            public Storage<ItemVariant> inputs(){return kettle.companionInputs();}
            public Storage<ItemVariant> outputs(){return kettle.companionOutputs();}
            public CompanionStatus status(){return kettle.companionStatus();}
        };
        if(be instanceof io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlockEntity wheel)return new Port(){
            public Identifier plan(){return Identifier.fromNamespaceAndPath("jugcraft","spinning_wheel");}
            public int needed(ItemStack candidate){return wheel.companionNeed(candidate);}
            public Storage<ItemVariant> inputs(){return wheel.companionInputs();}
            public Storage<ItemVariant> outputs(){return wheel.companionOutputs();}
            public CompanionStatus status(){return wheel.companionStatus();}
        };
        if(be instanceof io.github.jimbozoomer.jugcraft.agriculture.CiderBarrelBlockEntity barrel)return new Port(){
            public Identifier plan(){return Identifier.fromNamespaceAndPath("jugcraft","cider_barrel");}
            public int needed(ItemStack candidate){return barrel.companionNeed(candidate);}
            public Storage<ItemVariant> inputs(){return barrel.companionInputs();}
            public Storage<ItemVariant> outputs(){return barrel.companionOutputs();}
            public CompanionStatus status(){return barrel.companionStatus();}
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
            public int pickupNeeded(ItemStack candidate){return CookingPotSupplies.missing(pot,candidate);}
            public int needed(ItemStack candidate){
                var plan=pot.supplyPlan().orElse(null);
                return plan!=null && !candidate.isEmpty() && plan.ingredients().stream().anyMatch(p->p.ingredient().test(candidate))?candidate.getMaxStackSize():0;
            }
            public List<ItemStack> supplyBatch(Storage<ItemVariant> source,int slots){
                return CookingPotSupplies.create(pot,source,npc.assignments.supplies.combined(target,false),slots);
            }
            public Storage<ItemVariant> inputs(){return ContainerStorage.of(pot,Direction.UP);}
            public Storage<ItemVariant> outputs(){return ContainerStorage.of(pot,Direction.DOWN);}
        };
        for(var adapter:ADAPTERS){var port=adapter.resolve(npc,be);if(port!=null)return port;}
        return null;
    }
}

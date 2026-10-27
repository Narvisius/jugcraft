package local.peepo;

import java.nio.charset.StandardCharsets;
import java.util.*;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.*;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;

/** Supplies the native written formula. Heat, water, brewing and research remain native. What it lacks is the crucible's own demand (CrucibleBlockEntity.shortage, Concordance Chunk C, C-06), shared with the crucible's courier request: this port asks for it less what a courier holding that request still brings. */
public final class CrucibleCompanion implements CompanionLogistics.Port {
    private final CrucibleBlockEntity crucible;
    public final CompanionRecipeFilter filter=new CompanionRecipeFilter();
    private Formula cachedFormula;
    private Identifier cachedPlan;
    private int stockForm;
    public CrucibleCompanion(CrucibleBlockEntity crucible){this.crucible=crucible;}
    private boolean live(){return !crucible.isRemoved() && crucible.getLevel() instanceof ServerLevel;}
    private boolean enabled(){return live() && JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);}
    public static boolean acceptsFilter(ItemStack stack){return stack.isEmpty() || stack.is(JugcraftConcordance.DRAUGHT) || stack.is(JugcraftConcordance.SALVE);}
    public boolean salve(){return stockForm==2 && filter.allows(new ItemStack(JugcraftConcordance.SALVE)) || !filter.allows(new ItemStack(JugcraftConcordance.DRAUGHT)) && filter.allows(new ItemStack(JugcraftConcordance.SALVE));}
    public void stockForm(boolean salve){if(!stockBusy() && stockForm!=(salve?2:1)){stockForm=salve?2:1;crucible.setChanged();}}
    public void saveStockForm(net.minecraft.world.level.storage.ValueOutput out){out.putInt("CompanionStockForm",stockForm);}
    public void loadStockForm(net.minecraft.world.level.storage.ValueInput in){stockForm=Math.clamp(in.getIntOr("CompanionStockForm",0),0,2);}
    public void selectSalve(boolean salve){for(int i=0;i<9;i++)filter.set(i,ItemStack.EMPTY);filter.set(0,new ItemStack(salve?JugcraftConcordance.SALVE:JugcraftConcordance.DRAUGHT));crucible.setChanged();CompanionReadiness.invalidate(crucible);}
    private Item container(){return salve()?Items.BOWL:Items.GLASS_BOTTLE;}
    public Identifier plan(){
        if(!enabled() || crucible.program()==null)return null;
        if(cachedFormula!=crucible.program()){
            cachedFormula=crucible.program();cachedPlan=Identifier.fromNamespaceAndPath("jugcraft","crucible/"+UUID.nameUUIDFromBytes(cachedFormula.text().getBytes(StandardCharsets.UTF_8)));
        }
        return cachedPlan;
    }
    private List<Operation> remaining(){var formula=crucible.program();return formula==null?List.of():formula.operations().subList(crucible.step(),formula.operations().size());}
    private int containers(){int parts=crucible.mixture().parts();for(var op:remaining())if(op instanceof Operation.Water water)parts+=water.parts();return Math.min(Mixture.MAX_PARTS,parts);}
    public ItemStack stockProduct(){return new ItemStack(salve()?JugcraftConcordance.SALVE:JugcraftConcordance.DRAUGHT);}
    public boolean stockBusy(){
        if(crucible.mixture().history().stream().anyMatch(op->op instanceof Operation.Add))return true;
        for(int i=0;i<=CrucibleBlockEntity.BOTTLE_SLOT;i++)if(!crucible.getItem(i).isEmpty())return true;
        return false;
    }
    public int stockPending(List<ItemStack> incoming){return stockBusy() || !incoming.isEmpty()?containers():0;}
    public int needed(ItemStack stack){
        if(!enabled() || crucible.program()==null || crucible.incompatibility()!=null || stack.isEmpty())return 0; // C-01: an incompatible formula asks for nothing
        if(stack.is(container())){
            var held=crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT);
            return held.isEmpty() || ItemStack.isSameItemSameComponents(held,stack)?Math.max(0,containers()-held.getCount()):0;
        }
        // C-06: the crucible's one demand, less what a courier holding its request still brings.
        var missing=crucible.shortage();if(missing==null || !CrucibleBlockEntity.matchesIngredient(missing.add(),stack))return 0;
        return Math.max(0,missing.count()-crucible.courierRemainder(missing.add()));
    }
    private final Storage<ItemVariant> input=new InsertionOnlyStorage<>(){
        public long insert(ItemVariant item,long amount,TransactionContext tx){
            StoragePreconditions.notBlankNotNegative(item,amount);var stack=item.toStack(1);
            long left=Math.min(amount,needed(stack));if(left<=0)return 0;
            var storage=ContainerStorage.of(crucible,null);
            if(stack.is(container()))return storage.getSlot(CrucibleBlockEntity.BOTTLE_SLOT).insert(item,left,tx);
            long moved=0;
            // Combine first, preserving empty slots for other preparations.
            for(int pass=0;pass<2;pass++)for(int i=0;i<CrucibleBlockEntity.BUFFER_SLOTS && moved<left;i++)
                if(crucible.getItem(i).isEmpty()==(pass==1))moved+=storage.getSlot(i).insert(item,left-moved,tx);
            return moved;
        }
    };
    private boolean returnSlot(int slot){
        if(!live())return false;
        if(slot==CrucibleBlockEntity.OUTPUT_SLOT)return true;
        if(!enabled() || crucible.program()==null)return false;
        var stack=crucible.getItem(slot);if(stack.isEmpty())return false;
        if(slot==CrucibleBlockEntity.BOTTLE_SLOT)return !stack.is(container());
        // Return obsolete ingredients after a formula change, keeping anything used by any step of this formula.
        return crucible.program().operations().stream().noneMatch(op->op instanceof Operation.Add add && CrucibleBlockEntity.matchesIngredient(add,stack));
    }
    private final class OutputSlot implements SingleSlotStorage<ItemVariant>{
        private final int slot;OutputSlot(int slot){this.slot=slot;}
        public boolean supportsInsertion(){return false;}
        public long insert(ItemVariant item,long amount,TransactionContext tx){return 0;}
        public long extract(ItemVariant item,long amount,TransactionContext tx){StoragePreconditions.notBlankNotNegative(item,amount);return returnSlot(slot)?ContainerStorage.of(crucible,null).getSlot(slot).extract(item,amount,tx):0;}
        public ItemVariant getResource(){return returnSlot(slot)?ItemVariant.of(crucible.getItem(slot)):ItemVariant.blank();}
        public boolean isResourceBlank(){return getResource().isBlank();}
        public long getAmount(){return returnSlot(slot)?crucible.getItem(slot).getCount():0;}
        public long getCapacity(){return 64;}
    }
    private final Storage<ItemVariant> output=new CombinedStorage<>(java.util.stream.IntStream.range(0,CrucibleBlockEntity.SLOTS).mapToObj(OutputSlot::new).toList());
    public Storage<ItemVariant> inputs(){return input;}
    public Storage<ItemVariant> outputs(){return output;}
    /** One source (C-01): what blocks the crucible is its own reason (CrucibleBlockEntity.waiting()); after it, this port's lookahead. */
    public CompanionStatus status(){
        if(!enabled())return CompanionStatus.FORBIDDEN;
        if(crucible.program()==null || crucible.incompatibility()!=null)return CompanionStatus.RECIPE_MISSING;
        var reason=crucible.waiting();
        if(reason!=null)return switch(reason.code()){
            case NEEDS_WATER->CompanionStatus.NEEDS_WATER;
            case MISSING_INGREDIENT,WRONG_PREPARATION,COURIER_QUOTA->CompanionStatus.NO_INPUT;
            case NEEDS_CONTAINER->CompanionStatus.NEEDS_BOTTLES;
            case INSUFFICIENT_CAPACITY,OUTPUT_DIFFERS->CompanionStatus.FULL;
            case WRONG_HEAT,HELD->CompanionStatus.WRONG_TEMPERATURE;
            case NEEDS_EMPTYING->CompanionStatus.OCCUPIED;
            case MISSING_PERMISSION->CompanionStatus.FORBIDDEN;
            case INCOMPATIBLE,UNREADABLE->CompanionStatus.RECIPE_MISSING;
            default->CompanionStatus.IDLE;
        };
        if(crucible.shortage()!=null)return CompanionStatus.NO_INPUT;
        if(containers()>0 && crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty())return CompanionStatus.NEEDS_BOTTLES;
        return CompanionStatus.IDLE;
    }
}

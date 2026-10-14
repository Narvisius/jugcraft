package local.peepo;

import java.util.Arrays;
import io.github.jimbozoomer.jugcraft.agriculture.FarmStandBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.*;

/** Restocks remembered sale slots; never exposes sale stock or touches prices/currency. */
public final class FarmStandCompanion {
    private final FarmStandBlockEntity stand;
    private final ItemStack[] templates=new ItemStack[FarmStandBlockEntity.CRATES];
    public final CompanionRecipeFilter filter=new CompanionRecipeFilter();
    public FarmStandCompanion(FarmStandBlockEntity stand){this.stand=stand;Arrays.fill(templates,ItemStack.EMPTY);}
    public void observe(){
        boolean changed=false;
        for(int i=0;i<templates.length;i++){
            var stack=stand.crate(i);
            if(!stack.isEmpty() && !ItemStack.isSameItemSameComponents(stack,templates[i])){
                templates[i]=stack.copyWithCount(1);changed=true;
            }
        }
        if(changed){stand.setChanged();CompanionReadiness.invalidate(stand);}
    }
    public void save(ValueOutput out){filter.save(out);out.store("CompanionStock",ItemStack.OPTIONAL_CODEC.listOf(),Arrays.asList(templates));}
    public void load(ValueInput in){
        filter.load(in);Arrays.fill(templates,ItemStack.EMPTY);
        in.read("CompanionStock",ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list->{
            for(int i=0;i<Math.min(list.size(),templates.length);i++)templates[i]=list.get(i).copyWithCount(1);
        });
        observe();
    }
    public CompanionLogistics.Port forCompanion(PeepoEntity npc){return new CompanionLogistics.Port(){
        private boolean allowed(){return !stand.isRemoved() && stand.getLevel() instanceof ServerLevel && npc.orders.foodAccess(stand.owner(),false);}
        public void prepare(Storage<ItemVariant> source){if(allowed())observe();}
        public Identifier plan(){return allowed()?Identifier.fromNamespaceAndPath("jugcraft","farm_stand_restock"):null;}
        private int room(int slot,ItemStack candidate){
            var template=templates[slot];var held=stand.crate(slot);
            if(template.isEmpty() || !filter.allows(template) || !ItemStack.isSameItemSameComponents(template,candidate)
                || !held.isEmpty() && !ItemStack.isSameItemSameComponents(held,candidate))return 0;
            return Math.max(0,Math.min(candidate.getMaxStackSize(),stand.crates().getMaxStackSize())-held.getCount());
        }
        public int needed(ItemStack stack){if(!allowed())return 0;int n=0;for(int i=0;i<templates.length;i++)n+=room(i,stack);return n;}
        private final Storage<ItemVariant> input=new InsertionOnlyStorage<>(){
            public long insert(ItemVariant item,long amount,TransactionContext tx){
                StoragePreconditions.notBlankNotNegative(item,amount);if(!allowed())return 0;
                long moved=0;var stack=item.toStack(1);var storage=ContainerStorage.of(stand.crates(),null);
                for(int i=0;i<templates.length && moved<amount;i++){
                    int space=room(i,stack);if(space>0)moved+=storage.getSlot(i).insert(item,Math.min(amount-moved,space),tx);
                }
                return moved;
            }
        };
        public Storage<ItemVariant> inputs(){return input;}
        public Storage<ItemVariant> outputs(){return Storage.empty();}
        public CompanionStatus status(){
            if(!allowed())return CompanionStatus.FORBIDDEN;
            boolean configured=false;
            for(int i=0;i<templates.length;i++)if(!templates[i].isEmpty() && filter.allows(templates[i])){
                configured=true;if(room(i,templates[i])>0)return CompanionStatus.NO_INPUT;
            }
            return configured?CompanionStatus.FULL:CompanionStatus.STOCK_SETUP;
        }
    };}
}

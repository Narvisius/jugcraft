package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.*;

/** Transactional kitchen ports. Finished managed stove/board batches stay in saved storage, never loose drops. */
public final class KitchenCompanionPort extends SnapshotParticipant<KitchenCompanionPort.Snapshot> {
    private final BlockEntity block;
    public final CompanionRecipeFilter filter=new CompanionRecipeFilter();
    private final ItemStack[] finished=new ItemStack[9];
    public final CuttingBoardJob job;
    private long nextStatus;
    private int filterRevision;
    private ItemStack statusInput=ItemStack.EMPTY,statusKnife=ItemStack.EMPTY;
    private CompanionStatus boardStatus=CompanionStatus.NO_INPUT;
    public KitchenCompanionPort(BlockEntity block){this.block=block;Arrays.fill(finished,ItemStack.EMPTY);job=new CuttingBoardJob(this,block);filter.initialize(ItemStack.EMPTY);}
    public static KitchenCompanionPort of(BlockEntity block){return block instanceof CuttingBoardBlockEntity b?b.companionKitchen:block instanceof SkilletBlockEntity s?s.companionKitchen:block instanceof KitchenStoveBlockEntity s?s.companionKitchen:null;}
    private boolean live(){return block.getLevel() instanceof ServerLevel && !block.isRemoved();}
    private ItemStack cooked(ItemStack raw){
        if(!(block.getLevel() instanceof ServerLevel level))return ItemStack.EMPTY;
        return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING,new SingleRecipeInput(raw),level)
            .map(r->r.value().assemble(new SingleRecipeInput(raw.copyWithCount(1)))).orElse(ItemStack.EMPTY);
    }
    public boolean acceptsFilter(ItemStack stack){
        if(stack.isEmpty())return true;if(!(block.getLevel() instanceof ServerLevel level))return false;
        for(var holder:level.getServer().getRecipeManager().getRecipes()){
            if(block instanceof CuttingBoardBlockEntity && holder.value() instanceof CuttingRecipe r && r.results().stream().anyMatch(t->t.create().is(stack.getItem())))return true;
            if(!(block instanceof CuttingBoardBlockEntity) && holder.value() instanceof CampfireCookingRecipe r && r.assemble(new SingleRecipeInput(ItemStack.EMPTY)).is(stack.getItem()))return true;
        }return false;
    }
    private CuttingRecipe cutting(PeepoEntity npc,ItemStack item){
        if(!(block.getLevel() instanceof ServerLevel level) || !npc.assignedHand().is(JugcraftAgriculture.KNIVES))return null;
        var r=CuttingRecipe.find(level.getServer(),item,npc.assignedHand()).orElse(null);
        return r!=null && !r.results().isEmpty() && r.results().stream().anyMatch(t->filter.allows(t.create()))?r:null;
    }
    public CompanionLogistics.Port forCompanion(PeepoEntity npc){return new CompanionLogistics.Port(){
        public Identifier plan(){return Identifier.fromNamespaceAndPath("peepo","kitchen");}
        public int needed(ItemStack item){return need(npc,item);}
        public Storage<ItemVariant> inputs(){return new InsertionOnlyStorage<>(){
            public long insert(ItemVariant variant,long amount,TransactionContext tx){
                if(variant.isBlank() || amount<=0)return 0;var stack=variant.toStack(1);int n=(int)Math.min(amount,need(npc,stack));if(n<=0)return 0;
                updateSnapshots(tx);
                if(block instanceof CuttingBoardBlockEntity b)b.companionPut(stack);
                else if(block instanceof SkilletBlockEntity s)n=s.companionInsert(stack,n);
                else if(block instanceof KitchenStoveBlockEntity s)n=s.companionInsert(stack,n);
                return n;
            }
        };}
        public Storage<ItemVariant> outputs(){return outputStorage();}
        public CompanionStatus status(){return kitchenStatus(npc);}
    };}
    private int need(PeepoEntity npc,ItemStack raw){
        if(!live() || !CompanionJobs.permitted(npc,block.getBlockPos()))return 0;
        if(block instanceof CuttingBoardBlockEntity b){
            var recipe=cutting(npc,raw);return b.item().isEmpty() && recipe!=null && fits(recipe.cut())?1:0;
        }
        var result=cooked(raw);if(result.isEmpty() || !filter.allows(result))return 0;
        if(block instanceof SkilletBlockEntity s)return CookingPotBlockEntity.isHeated(block.getLevel(),block.getBlockPos())?s.companionRoom(raw,result):0;
        var s=(KitchenStoveBlockEntity)block;
        return s.getBlockState().getValue(KitchenStoveBlock.LIT) && block.getLevel().getBlockState(block.getBlockPos().above()).isAir() && fits(List.of(result))?s.companionRoom():0;
    }
    public CompanionStatus kitchenStatus(PeepoEntity npc){
        if(!live())return CompanionStatus.MISSING;
        if(block instanceof CuttingBoardBlockEntity b){
            if(!npc.assignedHand().is(JugcraftAgriculture.KNIVES))return CompanionStatus.NO_TOOL;
            long now=block.getLevel().getGameTime();
            if(now<nextStatus && filterRevision==filter.revision() && ItemStack.matches(statusInput,b.item()) && ItemStack.matches(statusKnife,npc.assignedHand()))return boardStatus;
            nextStatus=now+20;filterRevision=filter.revision();statusInput=b.item().copy();statusKnife=npc.assignedHand().copy();
            var r=cutting(npc,b.item());boardStatus=r==null?CompanionStatus.NO_INPUT:fits(r.cut())?CompanionStatus.READY:CompanionStatus.FULL;return boardStatus;
        }
        if(block instanceof SkilletBlockEntity s)return !s.fried().isEmpty()?CompanionStatus.READY:!CookingPotBlockEntity.isHeated(block.getLevel(),block.getBlockPos())?CompanionStatus.NO_HEAT:s.raw().isEmpty()?CompanionStatus.NO_INPUT:CompanionStatus.WORKING;
        for(var s:finished)if(!s.isEmpty())return CompanionStatus.READY;
        return !block.getBlockState().getValue(KitchenStoveBlock.LIT)?CompanionStatus.NO_HEAT:!block.getLevel().getBlockState(block.getBlockPos().above()).isAir()?CompanionStatus.BLOCKED:CompanionStatus.NO_INPUT;
    }
    public boolean cut(PeepoEntity npc){
        if(!(block instanceof CuttingBoardBlockEntity board) || kitchenStatus(npc)!=CompanionStatus.READY)return false;
        var recipe=cutting(npc,board.item());if(recipe==null)return false;
        try(var tx=Transaction.openOuter()){
            updateSnapshots(tx);if(!store(recipe.cut()) || npc.extractEnergy(16,tx)<=0)return false;
            board.companionPut(ItemStack.EMPTY);tx.commit();
        }
        npc.belongings.getItem(9).hurtAndBreak(1,npc,net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        npc.belongings.setChanged();
        block.getLevel().playSound(null,block.getBlockPos(),net.minecraft.sounds.SoundEvents.HONEY_BLOCK_SLIDE,net.minecraft.sounds.SoundSource.BLOCKS,.7F,1.6F);return true;
    }
    private boolean store(List<ItemStack> results){
        for(var result:results){int left=result.getCount();
            for(int i=0;i<finished.length && left>0;i++){var held=finished[i];if(!held.isEmpty() && !ItemStack.isSameItemSameComponents(held,result))continue;
                int n=Math.min(left,result.getMaxStackSize()-held.getCount());if(n>0){finished[i]=result.copyWithCount(held.getCount()+n);left-=n;}}
            if(left>0)return false;
        }return true;
    }
    public boolean fits(List<ItemStack> results){var saved=Arrays.stream(finished).map(ItemStack::copy).toArray(ItemStack[]::new);boolean fits=store(results);System.arraycopy(saved,0,finished,0,9);return fits;}
    public boolean finish(ItemStack item){if(!fits(List.of(item)))return false;store(List.of(item));changed();return true;}
    private ItemStack result(int slot){return block instanceof SkilletBlockEntity s?(slot==0?s.fried():ItemStack.EMPTY):finished[slot];}
    public Storage<ItemVariant> outputStorage(){
        var slots=new ArrayList<Storage<ItemVariant>>();for(int i=0;i<(block instanceof SkilletBlockEntity?1:9);i++){final int slot=i;
            slots.add(new SingleSlotStorage<>(){
                public boolean isResourceBlank(){return !live() || result(slot).isEmpty();}
                public ItemVariant getResource(){return isResourceBlank()?ItemVariant.blank():ItemVariant.of(result(slot));}
                public long getAmount(){return isResourceBlank()?0:result(slot).getCount();}
                public long getCapacity(){return 64;}
                public boolean supportsInsertion(){return false;}
                public long insert(ItemVariant v,long n,TransactionContext tx){return 0;}
                public long extract(ItemVariant v,long n,TransactionContext tx){if(n<=0 || isResourceBlank() || !getResource().equals(v))return 0;
                    int take=(int)Math.min(n,getAmount());updateSnapshots(tx);result(slot).shrink(take);return take;}
            });
        }return new CombinedStorage<>(slots);
    }
    record Snapshot(Object input,List<ItemStack> output){}
    protected Snapshot createSnapshot(){Object input=block instanceof CuttingBoardBlockEntity b?b.item().copy():block instanceof SkilletBlockEntity s?s.companionSnapshot():((KitchenStoveBlockEntity)block).companionSnapshot();return new Snapshot(input,Arrays.stream(finished).map(ItemStack::copy).toList());}
    protected void readSnapshot(Snapshot s){if(block instanceof CuttingBoardBlockEntity b)b.companionPut((ItemStack)s.input);else if(block instanceof SkilletBlockEntity pan)pan.companionRestore(s.input);else ((KitchenStoveBlockEntity)block).companionRestore(s.input);for(int i=0;i<9;i++)finished[i]=s.output.get(i).copy();}
    protected void onFinalCommit(){changed();}
    public void changed(){nextStatus=0;block.setChanged();if(block.getLevel()!=null)block.getLevel().sendBlockUpdated(block.getBlockPos(),block.getBlockState(),block.getBlockState(),net.minecraft.world.level.block.Block.UPDATE_CLIENTS);}
    public void save(ValueOutput out){filter.save(out);if(!(block instanceof SkilletBlockEntity))out.store("CompanionFinished",ItemStack.OPTIONAL_CODEC.listOf(),Arrays.asList(finished));}
    public void load(ValueInput in){filter.load(in);filter.initialize(ItemStack.EMPTY);Arrays.fill(finished,ItemStack.EMPTY);var list=in.read("CompanionFinished",ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());for(int i=0;i<Math.min(9,list.size());i++)finished[i]=list.get(i);}
    public boolean handOver(net.minecraft.world.entity.player.Player player){boolean any=false;for(int i=0;i<9;i++){var item=finished[i];if(item.isEmpty())continue;any=true;finished[i]=ItemStack.EMPTY;if(!player.getInventory().add(item))net.minecraft.world.level.block.Block.popResource(block.getLevel(),block.getBlockPos(),item);}if(any)changed();return any;}
    public void drop(){if(block.getLevel()==null)return;for(var item:finished)net.minecraft.world.Containers.dropItemStack(block.getLevel(),block.getBlockPos().getX()+.5,block.getBlockPos().getY()+.5,block.getBlockPos().getZ()+.5,item);Arrays.fill(finished,ItemStack.EMPTY);job.removed();}
}

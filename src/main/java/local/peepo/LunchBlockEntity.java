package local.peepo;
import java.util.UUID;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
public final class LunchBlockEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items=NonNullList.withSize(9,ItemStack.EMPTY);
    public final ContainerStorage inventory=ContainerStorage.of(this,null);
    private UUID owner;private boolean party;
    public LunchBlockEntity(BlockPos pos,BlockState state){super(CompanionLunch.ENTITY,pos,state);}
    public boolean cover(){return getBlockState().getBlock()==CompanionLunch.COVER;}
    public BlockPos sourcePos(){return cover()?worldPosition.relative(getBlockState().getValue(LunchBlock.FACE).getOpposite()):worldPosition;}
    public void setOwner(UUID owner){this.owner=owner;setChanged();}
    public boolean isOwner(Player p){return p.getUUID().equals(owner);}
    public boolean party(){return party;}
    public void toggleParty(){party=!party;setChanged();}
    public boolean allowed(Player p){return !p.isSpectator() && (isOwner(p) || owner!=null && party && JugcraftParties.sameParty(owner,p.getUUID()));}
    public boolean feeds(PeepoEntity npc){return !isRemoved() && owner!=null && npc.orders.foodAccess(owner,party) && !isLocked()
        && CompanionJobs.permitted(npc,worldPosition) && CompanionJobs.permitted(npc,sourcePos());}
    public Storage<ItemVariant> source(){
        BlockPos pos=sourcePos();if(level==null || !level.hasChunkAt(pos))return null;
        if(!cover())return inventory;
        if(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity container && container.isLocked())return null;
        // Double chests can expose both halves through one storage; do not bypass a locked half.
        if(level.getBlockEntity(pos) instanceof ChestBlockEntity)for(Direction d:Direction.Plane.HORIZONTAL){
            BlockPos other=pos.relative(d);if(!level.hasChunkAt(other))continue;
            if(level.getBlockEntity(other) instanceof ChestBlockEntity chest && chest.isLocked())return null;
        }
        return ItemStorage.SIDED.find(level,pos,getBlockState().getValue(LunchBlock.FACE));
    }
    /** Inspect a bounded number of accessible views and preserve all food components. */
    public ItemStack takeMeal(PeepoEntity npc,boolean take){
        return meal(npc,take,false);
    }
    public boolean stockMeal(PeepoEntity npc){return !meal(npc,true,true).isEmpty();}
    private ItemStack meal(PeepoEntity npc,boolean take,boolean stock){
        if(!feeds(npc) || npc.isEating() || !(npc.needsAutomaticFood() || npc.food.needsSupplies()))return ItemStack.EMPTY;
        var storage=source();if(storage==null || !storage.supportsExtraction())return ItemStack.EMPTY;
        ItemVariant best=null;int score=-1,views=0;
        for(var view:storage){
            if(++views>128)break;
            if(view.isResourceBlank() || view.getAmount()<1)continue;
            var variant=view.getResource();var stack=variant.toStack(1);
            if(!PeepoEntity.isEdible(stack))continue;
            int quality=npc.preferences.foodScore(stack);if(quality<=score)continue;
            if(stock)try(var tx=Transaction.openOuter()){if(npc.food.store(stack,tx)!=1)continue;}
            try(var tx=Transaction.openOuter()){if(view.extract(variant,1,tx)!=1)continue;}
            best=variant;score=quality;
        }
        if(best==null)return ItemStack.EMPTY;
        if(!take)return best.toStack(1);
        try(var tx=Transaction.openOuter()){
            if(storage.extract(best,1,tx)!=1)return ItemStack.EMPTY;
            if(stock && npc.food.store(best.toStack(1),tx)!=1)return ItemStack.EMPTY;
            tx.commit();return best.toStack(1);
        }
    }
    public ItemStack returnRemainder(PeepoEntity npc,ItemStack stack){
        if(!feeds(npc))return stack;
        var storage=source();if(storage==null)return stack;
        try(var tx=Transaction.openOuter()){long inserted=storage.insert(ItemVariant.of(stack),stack.getCount(),tx);tx.commit();stack.shrink((int)inserted);}return stack;
    }
    public int getContainerSize(){return 9;}
    protected NonNullList<ItemStack> getItems(){return items;}
    protected void setItems(NonNullList<ItemStack> items){this.items=items;}
    protected Component getDefaultName(){return Component.translatable("block.peepo_companion.lunch_crate");}
    public boolean canOpen(Player player){return allowed(player) && super.canOpen(player);}
    public boolean stillValid(Player player){return allowed(player) && super.stillValid(player);}
    protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new ChestMenu(MenuType.GENERIC_9x1,id,inventory,this,1);}
    protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);if(owner!=null)out.putString("LunchOwner",owner.toString());out.putBoolean("LunchParty",party);}
    protected void loadAdditional(ValueInput in){super.loadAdditional(in);items=NonNullList.withSize(9,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);try{owner=UUID.fromString(in.getStringOr("LunchOwner",""));}catch(IllegalArgumentException e){owner=null;}party=in.getBooleanOr("LunchParty",false);}
}

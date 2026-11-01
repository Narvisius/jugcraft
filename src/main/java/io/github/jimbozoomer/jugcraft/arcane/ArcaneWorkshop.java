package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Physical source-layout ritual: ingredients are escrowed before timed completion, never duplicated. */
public final class ArcaneWorkshop extends BlockEntity implements Container,MenuProvider {
    private static final String[] FLOOR={"***PPP***","*PPPAPPP*","*PAPPPAP*","PPPPCPPPP","PAPCACPAP","PPPPCPPPP","*PAPPPAP*","*PPPAPPP*","***PPP***"};
    private static final int[][] PEDESTALS={{0,-3},{-2,-2},{2,-2},{-3,0},{3,0},{-2,2},{2,2},{0,3}};
    private static final int[][] CAPACITY={{1000,10,10000,900},{3000,50,15000,1350},{5000,100,30000,2500},{10000,500,50000,5000},{20000,1000,100000,7500}};
    private ItemStack main=ItemStack.EMPTY,pending=ItemStack.EMPTY;
    private final NonNullList<ItemStack> escrow=NonNullList.withSize(9,ItemStack.EMPTY);
    private final NonNullList<ItemStack> inventory=NonNullList.withSize(9,ItemStack.EMPTY);
    private final int[] essence=new int[4];
    private UUID owner;private int ticks,duration,upgrade;
    public ArcaneWorkshop(BlockPos p,BlockState s){super(ForbiddenArcane.WORKSHOP,p,s);}
    public String kind(){return getBlockState().getBlock() instanceof ArcaneWorkshopBlock b?b.kind:"";}
    public boolean forge(){return kind().startsWith("hephaestus_forge_tier_");}
    public int tier(){return forge()?Character.digit(kind().charAt(kind().length()-1),10):1;}
    public ItemStack displayed(){return ticks>0?escrow.get(0):main;}
    public float progress(){return duration>0?1-ticks/(float)duration:0;}
    public boolean running(){return ticks>0;}
    public int essence(int type){return essence[type];}
    public static int capacity(int tier,int type){return CAPACITY[Math.clamp(tier,1,5)-1][type];}
    public void owner(UUID id){owner=id;setChanged();}
    boolean acceptsSource(BlockPos source){if(!(level instanceof ServerLevel l))return false;var actor=Authority.present(l,owner);return actor!=null&&Authority.mayChange(l,actor,worldPosition)&&Authority.mayChange(l,actor,source)&&ClaimTransfers.allowed(l,source,worldPosition);}
    void addAureal(int value){essence[0]=Math.min(capacity(tier(),0),essence[0]+Math.max(0,value));changed();}
    void addEssence(int type,int value){if(type>=0&&type<4){essence[type]=Math.min(capacity(tier(),type),essence[type]+Math.max(0,value));changed();}}
    boolean magnetize(ServerPlayer p){
        if(!kind().equals("darkstone_pedestal")||running())return false;var old=main;main=ItemStack.EMPTY;var state=getBlockState();
        if(!p.level().setBlockAndUpdate(worldPosition,ForbiddenArcane.block("magnetized_darkstone_pedestal").defaultBlockState().setValue(ArcaneWorkshopBlock.WATERLOGGED,state.getValue(ArcaneWorkshopBlock.WATERLOGGED)))){main=old;return false;}
        if(p.level().getBlockEntity(worldPosition) instanceof ArcaneWorkshop next){next.main=old;next.owner=p.getUUID();next.changed();}else Block.popResource(p.level(),worldPosition,old);return true;
    }
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public static boolean floor(ServerLevel l,BlockPos center,ServerPlayer actor,boolean forming){
        for(int z=-4;z<=4;z++)for(int x=-4;x<=4;x++){
            char symbol=FLOOR[z+4].charAt(x+4);if(symbol=='*')continue;var at=center.offset(x,-1,z);
            if(!l.hasChunkAt(at)||!Authority.mayChange(l,actor,at))return false;
            String name=symbol=='A'?"gilded_chiseled_polished_darkstone":symbol=='C'?"chiseled_arcane_polished_darkstone":"polished_darkstone";
            if(!l.getBlockState(at).is(ForbiddenArcane.block(name)))return false;
            if(forming&&(x!=0||z!=0)){
                var above=at.above();if(!l.hasChunkAt(above)||!Authority.mayChange(l,actor,above))return false;
                boolean spot=false;for(var offset:PEDESTALS)if(offset[0]==x&&offset[1]==z){spot=true;break;}
                if(!l.isEmptyBlock(above)&&!(spot&&l.getBlockEntity(above) instanceof ArcaneWorkshop w&&!w.forge()))return false;
            }
        }
        return true;
    }
    public InteractionResult use(ServerPlayer p,InteractionHand hand){
        if(!Authority.mayChange(p.level(),p,worldPosition))return InteractionResult.FAIL;
        var held=p.getItemInHand(hand);
        if(ticks>0){p.sendOverlayMessage(Component.literal("Ritual in progress: "+Math.round(progress()*100)+"%"));return InteractionResult.SUCCESS;}
        if(forge()){
            int type=ArcaneEssences.kind(held),value=ArcaneEssences.amount(held);
            if(type>=0&&value>0){
                int room=CAPACITY[tier()-1][type]-essence[type];if(room<=0||!ArcaneEssences.divisible(held)&&room<value){status(p);return InteractionResult.SUCCESS;}
                int added=Math.min(room,value);essence[type]+=added;ArcaneEssences.consume(p,hand,added);changed();status(p);return InteractionResult.SUCCESS;
            }
            String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).getPath();
            if(id.startsWith("forbidden_")&&id.endsWith("_blacksmith_gavel")){
                if(start(p))held.hurtAndBreak(50,p,hand.asEquipmentSlot());else p.sendOverlayMessage(Component.literal("No matching ritual. Check the darkstone floor, eight pedestal offerings, main ingredient and essences."));
                return InteractionResult.SUCCESS;
            }
            if(!p.isShiftKeyDown()&&held.isEmpty()){p.openMenu(this);return InteractionResult.SUCCESS;}
        }
        if(main.isEmpty()&&!held.isEmpty()){main=held.copyWithCount(1);held.consume(1,p);owner=p.getUUID();changed();}
        else if(!main.isEmpty()){var taken=main;main=ItemStack.EMPTY;changed();if(!p.addItem(taken))p.drop(taken,false,net.minecraft.util.Prediction.SERVER_ONLY);}
        else if(forge())status(p);
        return InteractionResult.SUCCESS;
    }
    private void status(ServerPlayer p){p.sendSystemMessage(Component.literal("Forge "+tier()+" | Aureal "+essence[0]+" | Souls "+essence[1]+" | Blood "+essence[2]+" | XP "+essence[3]+". Place one main item; strike with a blacksmith gavel."));}
    private boolean start(ServerPlayer p){
        var l=p.level();if(main.isEmpty()||!floor(l,worldPosition,p,false))return false;
        var stands=new ArrayList<ArcaneWorkshop>();var slots=new ArrayList<ItemStack>();
        for(var offset:PEDESTALS){var at=worldPosition.offset(offset[0],0,offset[1]);if(!l.hasChunkAt(at)||!Authority.mayChange(l,p,at)||!ClaimTransfers.allowed(l,at,worldPosition))return false;
            ArcaneWorkshop w=l.getBlockEntity(at) instanceof ArcaneWorkshop b&&!b.forge()?b:null;stands.add(w);slots.add(w==null?ItemStack.EMPTY:w.main);}
        for(var recipe:ArcaneRituals.all()){
            var enhancers=inventory.subList(0,Math.min(4,tier()));var costs=ArcaneEnhancers.costs(recipe,enhancers,l);
            if(!recipe.matches(main,slots,tier(),essence,costs,enhancers))continue;
            int next=recipe.upgrade();ItemStack result=next==0?recipe.output(l.registryAccess(),main):ItemStack.EMPTY;
            if(next==0&&result.isEmpty()||next!=0&&(next!=tier()+1||next>5))continue;
            // This server-thread transaction has no external callbacks after validation.
            escrow.set(0,main);main=ItemStack.EMPTY;for(int i=0;i<8;i++)if(stands.get(i)!=null){escrow.set(i+1,stands.get(i).main);stands.get(i).main=ItemStack.EMPTY;stands.get(i).changed();}
            for(int i=0;i<4;i++)essence[i]-=costs[i];
            pending=result;upgrade=next;ticks=duration=recipe.duration();owner=p.getUUID();changed();l.setBlock(worldPosition,getBlockState().setValue(ArcaneWorkshopBlock.ACTIVE,true),3);l.playSound(null,worldPosition,SoundEvents.ANVIL_USE,SoundSource.BLOCKS,.8F,.85F);return true;
        }
        return false;
    }
    public static void tick(Level raw,BlockPos at,BlockState state,ArcaneWorkshop w){
        if(!(raw instanceof ServerLevel l))return;var actor=Authority.present(l,w.owner);
        if(actor==null||!Authority.mayChange(l,actor,at))return;
        if(!w.forge()){
            if(w.kind().equals("magnetized_darkstone_pedestal")&&w.main.isEmpty()&&l.getGameTime()%5==0){
                var box=new AABB(at.getX()+1/16D,at.getY()+14/16D,at.getZ()+1/16D,at.getX()+15/16D,at.getY()+18/16D,at.getZ()+15/16D);
                int count=0;for(var item:l.getEntitiesOfClass(ItemEntity.class,box)){if(++count>16)break;var target=((io.github.jimbozoomer.jugcraft.mixin.OwnerItemTargetAccess)item).jugcraft$pickupTarget();if(item.isRemoved()||item.getItem().isEmpty()||target!=null&&!target.equals(actor.getUUID())||!Authority.mayChange(l,actor,item.blockPosition())||!ClaimTransfers.allowed(l,item.blockPosition(),at))continue;w.main=item.getItem().split(1);if(item.getItem().isEmpty())item.discard();w.changed();break;}
            }return;
        }
        if(l.getGameTime()%10==0)for(int i=0;i<4;i++){
            var input=w.inventory.get(i+5);int amount=ArcaneEssences.amount(input);if(ArcaneEssences.kind(input)!=i||amount<=0)continue;
            int room=capacity(w.tier(),i)-w.essence[i];if(room<=0||!ArcaneEssences.divisible(input)&&room<amount)continue;
            int added=Math.min(amount,room);w.essence[i]+=added;
            w.inventory.set(i+5,ArcaneEssences.finishInput(input,added));w.changed();
        }
        if(w.ticks<=0)return;
        // Check the base every tick while active; at most 69 loaded blocks for one running forge.
        if(!floor(l,at,actor,false))return;
        if(w.ticks%5==0)l.sendParticles(ParticleTypes.ENCHANT,at.getX()+.5,at.getY()+1.2,at.getZ()+.5,8,.7,.3,.7,.04);
        if(--w.ticks>0){w.setChanged();return;}
        int next=w.upgrade;ItemStack result=w.pending;int[] reserves=w.essence.clone();UUID owner=w.owner;
        var retained=new ArrayList<ItemStack>();for(var stack:w.inventory)retained.add(stack.copy());
        w.escrow.clear();w.pending=ItemStack.EMPTY;w.upgrade=0;w.duration=0;
        if(next>0){
            // Clear escrow before replacing the block so the removal hook cannot duplicate it.
            w.inventory.clear();w.changed();l.setBlockAndUpdate(at,ForbiddenArcane.block("hephaestus_forge_tier_"+next).defaultBlockState().setValue(ArcaneWorkshopBlock.WATERLOGGED,state.getValue(ArcaneWorkshopBlock.WATERLOGGED)));
            if(l.getBlockEntity(at) instanceof ArcaneWorkshop replacement){System.arraycopy(reserves,0,replacement.essence,0,4);for(int i=0;i<9;i++)replacement.inventory.set(i,retained.get(i));replacement.owner=owner;replacement.changed();}
        }else{w.main=result;w.changed();l.setBlock(at,w.getBlockState().setValue(ArcaneWorkshopBlock.ACTIVE,false),3);}
        l.playSound(null,at,SoundEvents.ENCHANTMENT_TABLE_USE,SoundSource.BLOCKS,1,.7F);
    }
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){if(level instanceof ServerLevel s){Block.popResource(s,pos,main);main=ItemStack.EMPTY;for(var stack:escrow)Block.popResource(s,pos,stack);for(var stack:inventory)Block.popResource(s,pos,stack);inventory.clear();escrow.clear();pending=ItemStack.EMPTY;}super.preRemoveSideEffects(pos,state);}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.store("main",ItemStack.OPTIONAL_CODEC,main);out.store("pending",ItemStack.OPTIONAL_CODEC,pending);ContainerHelper.saveAllItems(out,escrow);for(int i=0;i<9;i++)out.store("input_"+i,ItemStack.OPTIONAL_CODEC,inventory.get(i));for(int i=0;i<4;i++)out.putInt("essence_"+i,essence[i]);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.putInt("ticks",ticks);out.putInt("duration",duration);out.putInt("upgrade",upgrade);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);main=in.read("main",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);pending=in.read("pending",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);escrow.clear();ContainerHelper.loadAllItems(in,escrow);for(int i=0;i<9;i++)inventory.set(i,in.read("input_"+i,ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));for(int i=0;i<4;i++)essence[i]=Math.clamp(in.getIntOr("essence_"+i,0),0,CAPACITY[4][i]);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);ticks=Math.clamp(in.getIntOr("ticks",0),0,72000);duration=Math.clamp(in.getIntOr("duration",0),0,72000);upgrade=Math.clamp(in.getIntOr("upgrade",0),0,5);}
    @Override public int getContainerSize(){return 9;}
    @Override public boolean isEmpty(){return main.isEmpty()&&inventory.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return slot==4?main:inventory.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){if(running())return ItemStack.EMPTY;var result=getItem(slot).split(count);changed();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){if(running())return ItemStack.EMPTY;var result=getItem(slot);if(slot==4)main=ItemStack.EMPTY;else inventory.set(slot,ItemStack.EMPTY);return result;}
    @Override public void setItem(int slot,ItemStack stack){if(slot==4)main=stack;else inventory.set(slot,stack);changed();}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return !running()&&(slot<Math.min(4,tier())&&ArcaneEnhancers.accepts(stack)||slot==4||slot>=5&&ArcaneEssences.kind(stack)==slot-5);}
    @Override public boolean stillValid(net.minecraft.world.entity.player.Player p){return p instanceof ServerPlayer sp&&level!=null&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(Vec3.atCenterOf(worldPosition))<=64&&Authority.mayChange(sp.level(),sp,worldPosition);}
    @Override public void clearContent(){main=ItemStack.EMPTY;inventory.clear();changed();}
    @Override public Component getDisplayName(){return Component.literal("Hephaestus Forge");}
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inv,net.minecraft.world.entity.player.Player p){return new ArcaneForgeMenu(id,inv,this);}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}

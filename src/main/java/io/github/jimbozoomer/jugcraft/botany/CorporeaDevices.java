package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;

/** Index, funnels, crystal inventory monitors and retained shortage requests. */
public final class CorporeaDevices {
    static final Set<String> TYPES=Set.of("corporea_index","corporea_funnel","corporea_crystal_cube","corporea_interceptor","corporea_retainer");
    private final BotanicalWorkshop host;
    private ItemStack sample=ItemStack.EMPTY,pending=ItemStack.EMPTY;
    private BlockPos requestPos;private int requested,pulse;private long count;private boolean redstone,locked,retainMissing=true,hideCount;
    CorporeaDevices(BotanicalWorkshop host){this.host=host;}
    public ItemStack sample(){return sample;}
    public long count(){return count;}
    public boolean hideCount(){return hideCount;}
    public int signal(){return pulse>0?15:host.kind().equals("corporea_retainer")?Math.min(15,requested):count<=0?0:Math.min(15,1+(int)(Math.log(count)/Math.log(2)));}
    InteractionResult use(ServerPlayer p,InteractionHand hand){
        var held=p.getItemInHand(hand);if(held.is(Jugtania.item("corporea_spark"))||held.is(Jugtania.item("corporea_spark_master"))||held.is(Jugtania.item("corporea_spark_creative")))return InteractionResult.PASS;
        if(host.kind().equals("corporea_index")){CorporeaMenu.open(p,host);return InteractionResult.SUCCESS;}
        if(host.kind().equals("corporea_crystal_cube")){
            if(held.is(Jugtania.item("phantom_ink"))){hideCount=!hideCount;if(!p.hasInfiniteMaterials())held.shrink(1);host.changed();return InteractionResult.SUCCESS;}
            if(!held.isEmpty()&&!locked){sample=held.copyWithCount(1);host.changed();refresh(p);return InteractionResult.SUCCESS;}
            if(held.isEmpty()&&!sample.isEmpty()){int moved=request(p,sample,p.isShiftKeyDown()?64:1,true);p.sendOverlayMessage(Component.literal("Requested "+moved+" · network has "+count));return InteractionResult.SUCCESS;}
        }
        p.sendOverlayMessage(Component.literal(status()));return InteractionResult.SUCCESS;
    }
    void wand(ServerPlayer p){if(host.kind().equals("corporea_crystal_cube"))locked=!locked;else if(host.kind().equals("corporea_retainer"))retainMissing=!retainMissing;host.changed();p.sendOverlayMessage(Component.literal(status()));}
    private String status(){return switch(host.kind()){case "corporea_crystal_cube"->"Crystal Cube: "+(sample.isEmpty()?"use an item to select":sample.getHoverName().getString()+" × "+count)+(locked?" · locked":" · unlocked");case "corporea_retainer"->"Retainer: "+requested+" pending · "+(retainMissing?"missing items only":"whole request")+" · redstone retries";case "corporea_funnel"->"Funnel: redstone requests the item in an attached frame; rotate the frame for 1/2/4/8/16/32/48/64 items. Output below or stored safely inside.";default->"Interceptor: emits a two-tick pulse for matching shortages and records the request in adjacent retainers.";};}
    private void refresh(ServerPlayer p){long next=CorporeaInventory.count(p,CorporeaSpark.at(p.level(),host.getBlockPos()),sample);if(next!=count){count=next;host.changed();p.level().updateNeighbourForOutputSignal(host.getBlockPos(),host.getBlockState().getBlock());}}
    private Container output(){return new Container(){
        public int getContainerSize(){return 17;}public boolean isEmpty(){return host.isEmpty();}public ItemStack getItem(int slot){return host.getItem(slot);}public ItemStack removeItem(int slot,int n){return host.removeItem(slot,n);}public ItemStack removeItemNoUpdate(int slot){return host.removeItemNoUpdate(slot);}public void setItem(int slot,ItemStack s){host.setItem(slot,s);}public void setChanged(){host.changed();}public boolean stillValid(Player p){return host.stillValid(p);}public void clearContent(){host.clearContent();}
    };}
    int request(ServerPlayer actor,ItemStack requestedItem,int number,boolean playerOutput){
        if(!host.permitted(actor)||requestedItem.isEmpty())return 0;var start=CorporeaSpark.at(actor.level(),host.getBlockPos());
        var destination=ContainerStorage.of(playerOutput?actor.getInventory():output(),null);
        long available=CorporeaInventory.count(actor,start,requestedItem);
        int moved=CorporeaInventory.transfer(actor,start,ItemVariant.of(requestedItem),number,destination);
        if(number>available)shortage(actor,start,requestedItem,number,(int)(number-available));refresh(actor);return moved;
    }
    private void shortage(ServerPlayer actor,CorporeaSpark start,ItemStack item,int number,int missing){
        for(var node:CorporeaSpark.network(actor,start)){
            if(!(actor.level().getBlockEntity(node.anchor()) instanceof BotanicalWorkshop b)||!b.kind().equals("corporea_interceptor")||!b.permitted(actor)||b.corporea.pulse>0)continue;
            var filters=BotanicalCollection.frames(actor.level(),node.anchor());if(!filters.isEmpty()&&filters.stream().noneMatch(s->ItemStack.isSameItemSameComponents(s,item)))continue;
            b.corporea.pulse=2;b.changed();actor.level().updateNeighborsAt(b.getBlockPos(),b.getBlockState().getBlock());actor.level().scheduleTick(b.getBlockPos(),b.getBlockState().getBlock(),2);
            for(var d:Direction.values()){var pos=b.getBlockPos().relative(d);if(actor.level().hasChunkAt(pos)&&ClaimTransfers.allowed(actor.level(),b.getBlockPos(),pos)&&actor.level().getBlockEntity(pos) instanceof BotanicalWorkshop retainer&&retainer.kind().equals("corporea_retainer")&&retainer.permitted(actor)){
                var r=retainer.corporea;r.pending=item.copyWithCount(1);r.requestPos=host.getBlockPos().immutable();r.requested=Math.min(4096,r.retainMissing?missing:number);retainer.changed();
            }}
        }
    }
    void resetPulse(ServerLevel level){pulse=0;host.changed();level.updateNeighborsAt(host.getBlockPos(),host.getBlockState().getBlock());}
    void tick(ServerLevel level,ServerPlayer p){
        if(host.kind().equals("corporea_crystal_cube")&&level.getGameTime()%20==0)refresh(p);
        boolean power=level.hasNeighborSignal(host.getBlockPos()),rise=power&&!redstone;if(power!=redstone){redstone=power;host.setChanged();}
        if(rise&&host.kind().equals("corporea_funnel")){
            var frames=level.getEntitiesOfClass(ItemFrame.class,new AABB(host.getBlockPos()).inflate(1),f->!f.getItem().isEmpty()&&f.getPos().relative(f.getDirection().getOpposite()).equals(host.getBlockPos()));
            if(!frames.isEmpty()){var frame=frames.get(level.getRandom().nextInt(frames.size()));request(p,frame.getItem(),new int[]{1,2,4,8,16,32,48,64}[Math.floorMod(frame.getRotation(),8)],false);}
        }
        if(rise&&host.kind().equals("corporea_retainer")&&requestPos!=null&&!pending.isEmpty()&&requested>0&&level.hasChunkAt(requestPos)&&ClaimTransfers.allowed(level,host.getBlockPos(),requestPos)&&level.getBlockEntity(requestPos) instanceof BotanicalWorkshop target&&TYPES.contains(target.kind())&&target.permitted(p)){
            var item=pending.copy();int amount=requested;pending=ItemStack.EMPTY;requested=0;host.changed();target.corporea.request(p,item,amount,false);
        }
        if(level.getGameTime()%5==0&&!host.isEmpty()){
            var below=host.getBlockPos().below();if(!level.hasChunkAt(below)||!Authority.mayChange(level,p,below)||!ClaimTransfers.allowed(level,host.getBlockPos(),below))return;
            var dest=ItemStorage.SIDED.find(level,below,Direction.UP);if(dest==null)return;var source=ContainerStorage.of(output(),null);
            try(var transaction=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
                for(var view:source){if(view.isResourceBlank())continue;try(var part=transaction.openNested()){long accepted=dest.insert(view.getResource(),Math.min(64,view.getAmount()),part);if(accepted>0&&view.extract(view.getResource(),accepted,part)==accepted){part.commit();break;}}}transaction.commit();
            }
        }
    }
    void save(ValueOutput out){out.store("corporea_sample",ItemStack.OPTIONAL_CODEC,sample);out.store("corporea_pending",ItemStack.OPTIONAL_CODEC,pending);if(requestPos!=null)out.store("corporea_request_pos",BlockPos.CODEC,requestPos);out.putInt("corporea_requested",requested);out.putLong("corporea_count",count);out.putInt("corporea_pulse",pulse);out.putBoolean("corporea_redstone",redstone);out.putBoolean("corporea_locked",locked);out.putBoolean("corporea_missing",retainMissing);out.putBoolean("corporea_hide",hideCount);}
    void load(ValueInput in){sample=in.read("corporea_sample",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);pending=in.read("corporea_pending",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);requestPos=in.read("corporea_request_pos",BlockPos.CODEC).orElse(null);requested=Math.clamp(in.getIntOr("corporea_requested",0),0,4096);count=Math.max(0,in.getLongOr("corporea_count",0));pulse=Math.clamp(in.getIntOr("corporea_pulse",0),0,2);redstone=in.getBooleanOr("corporea_redstone",false);locked=in.getBooleanOr("corporea_locked",false);retainMissing=in.getBooleanOr("corporea_missing",true);hideCount=in.getBooleanOr("corporea_hide",false);}
    static final class Device extends BotanicalBlock {
        Device(String n,Properties p){super(n,p);}
        @Override protected boolean isSignalSource(BlockState s){return functionKind().equals("corporea_interceptor");}
        @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos pos,Direction d){return functionKind().equals("corporea_interceptor")&&l.getBlockEntity(pos) instanceof BotanicalWorkshop b?b.corporea.signal():0;}
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos pos,Direction d){return l.getBlockEntity(pos) instanceof BotanicalWorkshop b?b.corporea.signal():0;}
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos pos,net.minecraft.util.RandomSource r){if(l.getBlockEntity(pos) instanceof BotanicalWorkshop b)b.corporea.resetPulse(l);}
    }
}

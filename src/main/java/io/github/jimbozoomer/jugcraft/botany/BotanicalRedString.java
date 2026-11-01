package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.*;

/** Remote adapters use live storage transactions, never shadow copies of a remote inventory. */
public final class BotanicalRedString {
    public static final Set<String> TYPES=Set.of("red_string_container","red_string_dispenser","red_string_comparator","red_string_fertilizer","red_string_interceptor","red_string_relay");
    private static final ThreadLocal<Integer> LOOKUP=ThreadLocal.withInitial(()->0);
    private final BotanicalWorkshop host;private BlockPos target;private boolean powered;private int signal;
    BotanicalRedString(BotanicalWorkshop host){this.host=host;}
    static void register(){
        ItemStorage.SIDED.registerForBlocks((level,pos,state,be,side)->be instanceof BotanicalWorkshop w?w.redString.port(side):null,Jugtania.block("red_string_container"),Jugtania.block("red_string_dispenser"));
        UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            if(!(player instanceof ServerPlayer p)||!Authority.mayChange(p.level(),p,hit.getBlockPos()))return InteractionResult.PASS;
            for(var direction:Direction.values())for(int n=1;n<=8;n++){var at=hit.getBlockPos().relative(direction,n);if(!level.hasChunkAt(at))break;if(level.getBlockEntity(at) instanceof BotanicalWorkshop w&&w.kind().equals("red_string_interceptor")&&w.permitted(p)&&hit.getBlockPos().equals(w.redString.resolve())){
                level.setBlockAndUpdate(at,w.getBlockState().setValue(BlockStateProperties.POWERED,true));p.level().scheduleTick(at,w.getBlockState().getBlock(),2);
            }}return InteractionResult.PASS;
        });
    }
    private boolean allowed(ServerPlayer p,BlockPos at){return p!=null&&!host.isRemoved()&&p.level()==host.getLevel()&&p.level().hasChunkAt(host.getBlockPos())&&p.level().getBlockEntity(host.getBlockPos())==host&&host.permitted(p)&&p.level().hasChunkAt(at)&&Authority.mayChange(p.level(),p,at)&&ClaimTransfers.allowed(p.level(),host.getBlockPos(),at);}
    private boolean locked(BlockEntity be){return be instanceof BaseContainerBlockEntity c&&c.isLocked()||be instanceof RandomizableContainerBlockEntity l&&l.getLootTable()!=null;}
    BlockPos resolve(){
        var actor=host.actor();if(actor==null||!host.permitted(actor)||LOOKUP.get()>=8)return null;LOOKUP.set(LOOKUP.get()+1);
        try{var level=actor.level();var face=host.getBlockState().getValue(BlockStateProperties.FACING);
            for(int i=1;i<=8;i++){var at=host.getBlockPos().relative(face,i);if(!level.hasChunkAt(at))break;var be=level.getBlockEntity(at);if(be instanceof BotanicalWorkshop b&&TYPES.contains(b.kind())||locked(be)||!allowed(actor,at))continue;
                var state=level.getBlockState(at);boolean match=switch(host.kind()){
                    case "red_string_container"->be!=null&&ItemStorage.SIDED.find(level,at,face.getOpposite())!=null;
                    case "red_string_dispenser"->be instanceof DispenserBlockEntity;
                    case "red_string_comparator"->state.hasAnalogOutputSignal();
                    case "red_string_interceptor"->be!=null;
                    case "red_string_fertilizer"->state.getBlock() instanceof BonemealableBlock;
                    case "red_string_relay"->!at.equals(host.getBlockPos().above())&&plant(state.getBlock())&&!(be instanceof BotanicalWorkshop);
                    default->false;};if(match)return at.immutable();
            }return null;
        }finally{LOOKUP.set(LOOKUP.get()-1);}
    }
    private static boolean plant(net.minecraft.world.level.block.Block block){if(block instanceof FlowerPotBlock pot)block=pot.getPotted();return block instanceof FlowerBlock||block instanceof MushroomBlock||block instanceof NetherFungusBlock||block instanceof DoublePlantBlock;}
    public BlockPos target(){return target;}
    int signal(){return signal;}
    void tick(ServerLevel level,ServerPlayer actor){
        if(level.getGameTime()%4==0){var old=target;target=resolve();if(!Objects.equals(old,target))host.changed();}
        if(host.kind().equals("red_string_comparator")){int now=target!=null&&allowed(actor,target)?level.getBlockState(target).getAnalogOutputSignal(level,target,host.getBlockState().getValue(BlockStateProperties.FACING).getOpposite()):0;if(now!=signal){signal=now;host.changed();}}
        if(host.kind().equals("red_string_dispenser")){boolean now=level.hasNeighborSignal(host.getBlockPos());if(now&&!powered){var at=resolve();if(at!=null&&safeArea(actor,at,2,2)){var state=level.getBlockState(at);if(state.getBlock() instanceof DispenserBlock)level.scheduleTick(at,state.getBlock(),4);}}if(now!=powered){powered=now;host.setChanged();}}
    }
    private boolean safeArea(ServerPlayer p,BlockPos center,int horizontal,int vertical){for(var at:BlockPos.betweenClosed(center.offset(-horizontal,-vertical,-horizontal),center.offset(horizontal,vertical,horizontal)))if(!allowed(p,at))return false;return true;}
    InteractionResult use(ServerPlayer p,InteractionHand hand){var stack=p.getItemInHand(hand);var at=resolve();
        if(host.kind().equals("red_string_fertilizer")&&stack.is(Items.BONE_MEAL)){
            // Bonemeal may grow trees rather than just the selected block. Reserve the entire vanilla growth envelope.
            if(at==null||!safeArea(p,at,16,32))return InteractionResult.FAIL;
            var charge=stack.copyWithCount(1);if(BoneMealItem.growCrop(charge,p.level(),at)){if(!p.hasInfiniteMaterials())stack.shrink(1);p.level().levelEvent(1505,at,0);return InteractionResult.SUCCESS;}return InteractionResult.PASS;
        }
        p.sendOverlayMessage(net.minecraft.network.chat.Component.literal(at==null?"No permitted target within eight blocks in the arrow direction":"Connected: "+at.toShortString()));return InteractionResult.SUCCESS;
    }
    Storage<ItemVariant> port(Direction side){
        var at=resolve();var actor=host.actor();if(at==null||actor==null||side!=null&&!allowed(actor,host.getBlockPos().relative(side))||LOOKUP.get()>=8)return Storage.empty();
        LOOKUP.set(LOOKUP.get()+1);Storage<ItemVariant> storage;
        try{var be=actor.level().getBlockEntity(at);storage=be instanceof ChestBlockEntity chest?ContainerStorage.of(chest,side):ItemStorage.SIDED.find(actor.level(),at,side);}finally{LOOKUP.set(LOOKUP.get()-1);}
        if(storage==null)return Storage.empty();var captured=storage;var capturedEntity=actor.level().getBlockEntity(at);
        return new Storage<>(){
            private boolean live(){var p=host.actor();return p!=null&&at.equals(resolve())&&allowed(p,at)&&p.level().getBlockEntity(at)==capturedEntity&&!locked(capturedEntity)&&(side==null||allowed(p,host.getBlockPos().relative(side)));}
            public long insert(ItemVariant r,long amount,TransactionContext tx){StoragePreconditions.notBlankNotNegative(r,amount);return live()?captured.insert(r,amount,tx):0;}
            public long extract(ItemVariant r,long amount,TransactionContext tx){StoragePreconditions.notBlankNotNegative(r,amount);return live()?captured.extract(r,amount,tx):0;}
            public boolean supportsInsertion(){return live()&&captured.supportsInsertion();}
            public boolean supportsExtraction(){return live()&&captured.supportsExtraction();}
            public Iterator<StorageView<ItemVariant>> iterator(){
                if(!live())return Collections.emptyIterator();var it=captured.iterator();return new Iterator<>(){
                    public boolean hasNext(){return live()&&it.hasNext();}
                    public StorageView<ItemVariant> next(){if(!live())throw new NoSuchElementException();var view=it.next();return new StorageView<>(){
                        public boolean isResourceBlank(){return !live()||view.isResourceBlank();}
                        public ItemVariant getResource(){return live()?view.getResource():ItemVariant.blank();}
                        public long getAmount(){return live()?view.getAmount():0;}
                        public long getCapacity(){return live()?view.getCapacity():0;}
                        public long extract(ItemVariant r,long amount,TransactionContext tx){StoragePreconditions.notBlankNotNegative(r,amount);return live()?view.extract(r,amount,tx):0;}
                    };}
                };
            }
        };
    }
    void save(ValueOutput out){if(target!=null)out.store("red_string_target",BlockPos.CODEC,target);out.putBoolean("red_string_powered",powered);out.putInt("red_string_signal",signal);}
    void load(ValueInput in){target=in.read("red_string_target",BlockPos.CODEC).filter(p->p.distSqr(host.getBlockPos())<=64).orElse(null);powered=in.getBooleanOr("red_string_powered",false);signal=Math.clamp(in.getIntOr("red_string_signal",0),0,15);}
    public static final class Block extends BotanicalBlock {
        Block(String kind,Properties p){super(kind,p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING,Direction.NORTH).setValue(BlockStateProperties.POWERED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.FACING,BlockStateProperties.POWERED);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.FACING,c.getNearestLookingDirection().getOpposite());}
        @Override protected boolean isSignalSource(BlockState s){return kind.equals("red_string_interceptor");}
        @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return kind.equals("red_string_interceptor")&&s.getValue(BlockStateProperties.POWERED)?15:0;}
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction d){return l.getBlockEntity(p) instanceof BotanicalWorkshop b&&kind.equals("red_string_comparator")?b.redString.signal():0;}
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,net.minecraft.util.RandomSource r){if(s.getValue(BlockStateProperties.POWERED))l.setBlockAndUpdate(p,s.setValue(BlockStateProperties.POWERED,false));}
    }
}

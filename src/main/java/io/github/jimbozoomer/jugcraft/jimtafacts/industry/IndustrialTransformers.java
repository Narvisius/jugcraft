package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.menu.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;

/** Original transformer housings as directional JE rate adapters, not an invented second voltage currency. */
public final class IndustrialTransformers {
    public static final List<String> TIERS=List.of("lv","mv","hv","ev");
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static final EnumProperty<Direction> FACING=BlockStateProperties.FACING;
    public static BlockEntityType<Node> TYPE;public static ExtendedMenuType<Menu,BlockPos> MENU;
    private IndustrialTransformers(){}
    public static void register(){
        for(int tier=0;tier<TIERS.size();tier++){var name=TIERS.get(tier);var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_"+name+"_transformer"));
            var block=Registry.register(BuiltInRegistries.BLOCK,key,new Device(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key),tier));BLOCKS.put(name,block);
            var ik=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(block,new Item.Properties().setId(ik).useBlockDescriptionPrefix()));}
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_transformer"),FabricBlockEntityTypeBuilder.create(Node::new,BLOCKS.values().toArray(Block[]::new)).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("jimta_transformer"),new ExtendedMenuType<>(Menu::new,BlockPos.STREAM_CODEC));
        EnergyStorage.SIDED.registerForBlockEntity(Node::port,TYPE);
        PlayerBlockBreakEvents.BEFORE.register((l,p,pos,s,be)->!(be instanceof Node n)||n.identity(p));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->BLOCKS.values().forEach(out::accept));
    }
    static final class Device extends Block implements EntityBlock,EnergyConnectable {
        final int tier;Device(Properties p,int tier){super(p);this.tier=tier;registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getNearestLookingDirection().getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T>t){return !l.isClientSide()&&t==TYPE?(world,p,state,e)->((Node)e).step():null;}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity actor,ItemStack stack){if(actor!=null&&l.getBlockEntity(p) instanceof Node n){n.owner=actor.getUUID();n.setChanged();}}
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){EnergyNetworks.invalidate(l);}
        @Override protected void affectNeighborsAfterRemoval(BlockState s,ServerLevel l,BlockPos p,boolean moved){EnergyNetworks.invalidate(l);}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player actor,BlockHitResult hit){if(!(l.getBlockEntity(p) instanceof Node n)||!n.permitted(actor))return InteractionResult.FAIL;
            if(actor instanceof ServerPlayer sp){if(n.owner==null){n.owner=sp.getUUID();n.setChanged();}sp.openMenu(n);}return InteractionResult.SUCCESS;}
    }
    public static final class Node extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
        public UUID owner;public int mode;public final SimpleEnergyStorage energy;private final Quota quota=new Quota();private int cursor;
        public Node(BlockPos p,BlockState s){super(TYPE,p,s);energy=new SimpleEnergyStorage(high()*8L,high(),high(),this::setChanged);}
        public int low(){return 32<<(2*((Device)getBlockState().getBlock()).tier);}public int high(){return low()*4;}
        public Direction facing(){return getBlockState().getValue(FACING);}
        public boolean reversed(){return mode==2||mode==0&&level!=null&&level.hasNeighborSignal(worldPosition);}
        boolean identity(Player p){return owner==null||owner.equals(p.getUUID())||JugcraftParties.sameParty(owner,p.getUUID());}
        public boolean permitted(Player p){return level!=null&&!isRemoved()&&level.getBlockEntity(worldPosition)==this&&p.isAlive()&&!p.isSpectator()&&identity(p)&&Authority.mayChange(level,p,worldPosition);}
        private boolean automation(){var p=level instanceof ServerLevel s?Authority.answering(s,owner):null;return p!=null&&permitted(p);}
        public EnergyStorage port(Direction side){return new EnergyStorage(){
            private boolean input(){return side!=null&&((side==facing())!=reversed());}
            public Object transferIdentity(){return energy;}
            public boolean supportsInsertion(){return input();}public boolean supportsExtraction(){return side!=null&&!input();}
            public long insert(long max,TransactionContext tx){if(!input()||!automation()||max<=0)return 0;long moved=energy.insert(Math.min(max,quota.remaining(true,side)),tx);if(moved>0)quota.use(true,side,moved,tx);return moved;}
            public long extract(long max,TransactionContext tx){if(!supportsExtraction()||!automation()||max<=0)return 0;long moved=energy.extract(Math.min(max,quota.remaining(false,side)),tx);if(moved>0)quota.use(false,side,moved,tx);return moved;}
            public long getAmount(){return energy.getAmount();}public long getCapacity(){return energy.getCapacity();}
        };}
        /** Shared per-tick quota is transactional too: simulations/aborts cannot spend throughput. */
        private final class Quota extends SnapshotParticipant<Usage> {
            private long tick=Long.MIN_VALUE,in,out;private long[] incoming=new long[6],outgoing=new long[6];
            private long now(){return level==null?Long.MIN_VALUE:level.getGameTime();}
            long remaining(boolean input,Direction side){boolean current=tick==now();long total=current?(input?in:out):0,face=current?(input?incoming:outgoing)[side.ordinal()]:0;return Math.max(0,Math.min(high()-total,sideLimit(side)-face));}
            void use(boolean input,Direction side,long amount,TransactionContext tx){updateSnapshots(tx);if(tick!=now()){tick=now();in=out=0;Arrays.fill(incoming,0);Arrays.fill(outgoing,0);}if(input){in+=amount;incoming[side.ordinal()]+=amount;}else{out+=amount;outgoing[side.ordinal()]+=amount;}}
            protected Usage createSnapshot(){return new Usage(tick,in,out,incoming.clone(),outgoing.clone());}protected void readSnapshot(Usage s){tick=s.tick;in=s.in;out=s.out;incoming=s.incoming.clone();outgoing=s.outgoing.clone();}
        }
        private record Usage(long tick,long in,long out,long[] incoming,long[] outgoing){}
        public void step(){if(!automation())return;for(int i=0;i<6;i++){Direction d=Direction.values()[(cursor+i)%6];var source=port(d);if(source.supportsExtraction())EnergyNetworks.pushToNeighbors(level,worldPosition,source,sideLimit(d),List.of(d));}cursor=(cursor+1)%6;}
        private int sideLimit(Direction d){return d==facing()?high():low();}
        public boolean configure(Player p,int selected){if(selected<0||selected>2||!permitted(p)||p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)>64)return false;mode=selected;setChanged();EnergyNetworks.invalidate(level);return true;}
        private final ContainerData data=new ContainerData(){public int get(int i){return switch(i){case 0->low();case 1->(int)(energy.getAmount()/4);case 2->mode;case 3->reversed()?1:0;default->0;};}public void set(int i,int v){}public int getCount(){return 4;}};
        public Component getDisplayName(){return Component.literal(TIERS.get(((Device)getBlockState().getBlock()).tier).toUpperCase()+" Transformer");}
        public BlockPos getScreenOpeningData(ServerPlayer p){return worldPosition;}public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new Menu(id,inv,worldPosition,this,data);}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.putInt("mode",mode);out.putLong("energy",energy.getAmount());}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);mode=Math.clamp(in.getIntOr("mode",0),0,2);energy.setAmount(in.getLongOr("energy",0));}
    }
    public static final class Menu extends AbstractContainerMenu {
        public final ContainerData data;private final Node node;private final BlockPos pos;
        public Menu(int id,Inventory inv,BlockPos p){this(id,inv,p,null,new SimpleContainerData(4));}
        Menu(int id,Inventory inv,BlockPos p,Node node,ContainerData data){super(MENU,id);this.pos=p;this.node=node;this.data=data;addDataSlots(data);
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+18*col,137+18*row));for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+18*col,195));}
        @Override public boolean stillValid(Player p){return p.level().isClientSide()||node!=null&&p.level().getBlockEntity(pos)==node&&node.permitted(p)&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
        @Override public boolean clickMenuButton(Player p,int id){return stillValid(p)&&node!=null&&node.configure(p,id);}
        @Override public void clicked(int i,int b,ContainerInput type,Player p){if(stillValid(p))super.clicked(i,b,type,p);}
        @Override public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}
    }
}

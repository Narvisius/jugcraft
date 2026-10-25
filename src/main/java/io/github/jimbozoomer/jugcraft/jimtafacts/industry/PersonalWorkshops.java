package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.menu.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
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
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Private storage and explicitly published barter/charging, backed only by real owner stock and player payment. */
public final class PersonalWorkshops {
    public static final List<String> KINDS=List.of("personal_safe","barter_station","energy_vendor");
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static final int[] CHARGES={1000,10000,100000};
    public static final EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN=BlockStateProperties.OPEN;
    public static BlockEntityType<Node> TYPE;public static ExtendedMenuType<Menu,BlockPos> MENU;
    private PersonalWorkshops(){}
    public static void register(){
        for(String name:KINDS){var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_"+name));var block=Registry.register(BuiltInRegistries.BLOCK,key,new Device(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).strength(5,3600000).pushReaction(PushReaction.IMMOVEABLE).noOcclusion(),name));BLOCKS.put(name,block);
            var ik=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(block,new Item.Properties().setId(ik).useBlockDescriptionPrefix()));}
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_personal_workshop"),FabricBlockEntityTypeBuilder.create(Node::new,BLOCKS.values().toArray(Block[]::new)).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("jimta_personal_workshop"),new ExtendedMenuType<>(Menu::new,BlockPos.STREAM_CODEC));
        EnergyStorage.SIDED.registerForBlockEntity((n,side)->n.kind().equals("energy_vendor")?n.energy:null,TYPE);
        PlayerBlockBreakEvents.BEFORE.register((l,p,pos,s,be)->!(be instanceof Node n)||n.identity(p));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->BLOCKS.values().forEach(out::accept));
    }
    /** Only a published shop's own action may cross a land-use restriction; never fall through to held-item placement. */
    public static InteractionResult publicUse(Player p,Level level,BlockPos pos){
        if(!(level.getBlockEntity(pos) instanceof Node n)||n.safe()||!(level.isClientSide()?n.getBlockState().getValue(OPEN):n.published)||!n.canOpen(p))return InteractionResult.PASS;
        if(p instanceof ServerPlayer sp)sp.openMenu(n);return InteractionResult.SUCCESS;
    }
    static final class Device extends Block implements EntityBlock,EnergyConnectable {
        final String kind;Device(Properties p,String kind){super(p);this.kind=kind;registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(OPEN,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING,OPEN);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T>t){return t==TYPE?(world,p,state,e)->((Node)e).tick():null;}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity actor,ItemStack stack){if(actor!=null&&l.getBlockEntity(p) instanceof Node n){n.owner=actor.getUUID();n.setChanged();}}
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){EnergyNetworks.invalidate(l);}
        @Override protected void affectNeighborsAfterRemoval(BlockState s,ServerLevel l,BlockPos p,boolean moved){EnergyNetworks.invalidate(l);}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){return open(l,p,player);}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return player.isShiftKeyDown()?InteractionResult.PASS:open(l,p,player);}
        private InteractionResult open(Level l,BlockPos p,Player player){if(!(l.getBlockEntity(p) instanceof Node n)||!n.canOpen(player))return InteractionResult.FAIL;
            if(player instanceof ServerPlayer sp){if(n.owner==null){n.owner=sp.getUUID();n.setChanged();}sp.openMenu(n);}return InteractionResult.SUCCESS;}
    }
    public static final class Node extends BaseContainerBlockEntity implements WorldlyContainer,ExtendedMenuProvider<BlockPos> {
        public UUID owner;public boolean published;public int chargeIndex,status;public long trades;
        public ItemStack price=ItemStack.EMPTY,offer=ItemStack.EMPTY;
        private NonNullList<ItemStack> items;
        public final SimpleEnergyStorage energy=new SimpleEnergyStorage(1000000,512,0,this::setChanged);
        public float previousDoor,door;private long lastTrade=Long.MIN_VALUE;
        public Node(BlockPos p,BlockState s){super(TYPE,p,s);items=NonNullList.withSize(safe()?54:18,ItemStack.EMPTY);}
        public String kind(){return ((Device)getBlockState().getBlock()).kind;}public boolean safe(){return kind().equals("personal_safe");}
        boolean identity(Player p){return owner==null||owner.equals(p.getUUID())||JugcraftParties.sameParty(owner,p.getUUID());}
        public boolean manager(Player p){return identity(p)&&Authority.mayChange(level,p,worldPosition);}
        private boolean live(Player p){return level!=null&&!isRemoved()&&level.getBlockEntity(worldPosition)==this&&p.level()==level&&p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
        private boolean shopAllowed(){var actor=level instanceof ServerLevel s?Authority.answering(s,owner):null;return actor!=null&&Authority.mayChange(level,actor,worldPosition);}
        public boolean canOpen(Player p){if(!live(p))return false;if(manager(p))return true;if(level.isClientSide())return !safe()&&getBlockState().getValue(OPEN);return !safe()&&published&&level.mayInteract(p,worldPosition)&&shopAllowed();}
        public void tick(){if(!safe()||level==null)return;if(level.isClientSide()){previousDoor=door;door=Math.clamp(door+(getBlockState().getValue(OPEN)?.1F:-.1F),0,1);return;}
            if(level.getGameTime()%5!=0)return;var viewers=new ArrayList<ServerPlayer>();((ServerLevel)level).getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ServerPlayer.class),new AABB(worldPosition).inflate(8),p->p.containerMenu instanceof Menu m&&m.node==this&&canOpen(p),viewers,1);
            boolean open=!viewers.isEmpty();if(getBlockState().getValue(OPEN)!=open)level.setBlockAndUpdate(worldPosition,getBlockState().setValue(OPEN,open));
        }
        public boolean template(Player p,int slot,ItemStack stack){if(!live(p)||!manager(p)||safe()||slot<0||slot>1||slot==1&&kind().equals("energy_vendor"))return false;
            ItemStack copy=stack.isEmpty()?ItemStack.EMPTY:stack.copyWithCount(Math.min(stack.getCount(),stack.getMaxStackSize()));if(slot==0)price=copy;else offer=copy;setChanged();return true;}
        public boolean action(Player p,int id){if(!canOpen(p)||safe())return false;if(id==0)return trade(p);if(!manager(p))return false;
            if(id==1){published=!published;level.setBlockAndUpdate(worldPosition,getBlockState().setValue(OPEN,published));}else if(id==2&&kind().equals("energy_vendor"))chargeIndex=(chargeIndex+1)%CHARGES.length;else return false;setChanged();return true;}
        public boolean trade(Player p){
            if(!canOpen(p)||safe()||!published||!shopAllowed()||price.isEmpty()||level.hasNeighborSignal(worldPosition)||level.getGameTime()==lastTrade){status=1;return false;}
            var wallet=new ArrayList<ItemStack>(36);for(int i=0;i<36;i++)wallet.add(p.getInventory().getItem(i).copy());
            var stock=new ArrayList<ItemStack>(18);for(var stack:items)stock.add(stack.copy());
            if(!remove(wallet,0,36,price)){status=2;return false;}if(!add(stock,9,18,price)){status=3;return false;}
            if(kind().equals("barter_station")){
                if(offer.isEmpty()||!remove(stock,0,9,offer)){status=4;return false;}if(!add(wallet,0,36,offer)){status=5;return false;}
            }else{
                int selected=p.getInventory().getSelectedSlot();ItemStack held=wallet.get(selected);int charge=CHARGES[chargeIndex];
                if(energy.getAmount()<charge||held.getCount()!=1||Chargeable.capacity(held)-Chargeable.energy(held)<charge){status=6;return false;}
                if(Chargeable.charge(held,charge)!=charge){status=6;return false;}
            }
            // Every capacity/payment check above ran on copies. This one server-thread commit cannot drop, duplicate or partially pay.
            for(int i=0;i<36;i++)p.getInventory().setItem(i,wallet.get(i));for(int i=0;i<18;i++)items.set(i,stock.get(i));
            if(kind().equals("energy_vendor"))energy.setAmount(energy.getAmount()-CHARGES[chargeIndex]);
            lastTrade=level.getGameTime();trades=Math.min(Long.MAX_VALUE-1,trades)+1;status=0;p.getInventory().setChanged();setChanged();return true;
        }
        private static boolean remove(List<ItemStack> list,int from,int to,ItemStack sample){int left=sample.getCount();for(int i=from;i<to&&left>0;i++){var stack=list.get(i);if(ItemStack.isSameItemSameComponents(stack,sample)){int n=Math.min(left,stack.getCount());stack.shrink(n);left-=n;}}return left==0;}
        private static boolean add(List<ItemStack> list,int from,int to,ItemStack sample){int left=sample.getCount();for(int pass=0;pass<2;pass++)for(int i=from;i<to&&left>0;i++){var stack=list.get(i);if(pass==0&&!stack.isEmpty()&&ItemStack.isSameItemSameComponents(stack,sample)){int n=Math.min(left,Math.max(0,stack.getMaxStackSize()-stack.getCount()));stack.grow(n);left-=n;}else if(pass==1&&stack.isEmpty()){int n=Math.min(left,sample.getMaxStackSize());list.set(i,sample.copyWithCount(n));left-=n;}}return left==0;}
        @Override public boolean stillValid(Player p){return canOpen(p);}
        @Override public int getContainerSize(){return items.size();}@Override protected NonNullList<ItemStack> getItems(){return items;}@Override protected void setItems(NonNullList<ItemStack> list){items=list;}
        @Override protected Component getDefaultName(){return Component.translatable("block.jugcraft.jimta_"+kind());}
        @Override public BlockPos getScreenOpeningData(ServerPlayer p){return worldPosition;}
        @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new Menu(id,inv,worldPosition,this,new ContainerData(){public int get(int i){return switch(i){case 0->manager(inv.player)?1:0;case 1->published?1:0;case 2->chargeIndex;case 3->(int)(energy.getAmount()/100);case 4->status;case 5->(int)Math.min(32767,trades);default->0;};}public void set(int i,int v){}public int getCount(){return 6;}});}
        // A hopper has no customer identity. Publishing a shop never exposes owner stock through generic automation.
        @Override public int[] getSlotsForFace(Direction d){return new int[0];}@Override public boolean canPlaceItemThroughFace(int i,ItemStack s,Direction d){return false;}@Override public boolean canTakeItemThroughFace(int i,ItemStack s,Direction d){return false;}
        @Override public void preRemoveSideEffects(BlockPos p,BlockState s){if(level!=null&&!level.isClientSide()){Containers.dropContents(level,p,this);clearContent();}}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.store("price",ItemStack.OPTIONAL_CODEC,price);out.store("offer",ItemStack.OPTIONAL_CODEC,offer);out.putBoolean("published",published);out.putInt("charge",chargeIndex);out.putLong("energy",energy.getAmount());out.putLong("trades",trades);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);items=NonNullList.withSize(safe()?54:18,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);price=in.read("price",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);offer=in.read("offer",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);published=in.getBooleanOr("published",false);chargeIndex=Math.clamp(in.getIntOr("charge",0),0,2);energy.setAmount(in.getLongOr("energy",0));trades=Math.max(0,in.getLongOr("trades",0));}
    }
    public static final class Menu extends AbstractContainerMenu {
        public final Container contents;public final SimpleContainer templates=new SimpleContainer(2);public final ContainerData data;public final String kind;
        private final BlockPos pos;private final Node node;private final int own,offset;
        public Menu(int id,Inventory inv,BlockPos pos){this(id,inv,pos,null,new SimpleContainerData(6));}
        Menu(int id,Inventory inv,BlockPos pos,Node node,ContainerData data){super(MENU,id);this.pos=pos;this.node=node;this.data=data;
            kind=inv.player.level().getBlockState(pos).getBlock() instanceof Device device?device.kind:"personal_safe";boolean safe=kind.equals("personal_safe");own=safe?54:18;offset=safe?0:2;contents=node==null?new SimpleContainer(own):node;addDataSlots(data);
            if(!safe){addSlot(new Slot(templates,0,50,19){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}});addSlot(new Slot(templates,1,50,53){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}});}
            for(int row=0;row<own/9;row++)for(int col=0;col<9;col++)addSlot(new Slot(contents,row*9+col,8+col*18,(safe?18:74)+row*18){public boolean mayPlace(ItemStack s){return data.get(0)==1;}public boolean mayPickup(Player p){return data.get(0)==1;}});
            int y=safe?140:128;for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,y+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,y+58));
        }
        @Override public boolean stillValid(Player p){return p.level().isClientSide()||node!=null&&p.level().getBlockEntity(pos)==node&&node.canOpen(p);}
        @Override public void broadcastChanges(){if(node!=null&&!node.safe()){templates.setItem(0,node.price.copy());templates.setItem(1,node.offer.copy());}super.broadcastChanges();}
        @Override public void clicked(int slot,int button,ContainerInput type,Player p){if(!stillValid(p))return;if(slot>=0&&slot<offset){if(node!=null&&type==ContainerInput.PICKUP)node.template(p,slot,getCarried());broadcastChanges();return;}
            if(slot>=offset&&slot<offset+own&&data.get(0)!=1)return;super.clicked(slot,button,type,p);}
        @Override public boolean clickMenuButton(Player p,int id){return stillValid(p)&&node!=null&&node.action(p,id);}
        @Override public ItemStack quickMoveStack(Player p,int index){if(!stillValid(p)||data.get(0)!=1||index<offset||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);var stack=slot.getItem();var before=stack.copy();if(stack.isEmpty())return ItemStack.EMPTY;
            boolean moved=index<offset+own?moveItemStackTo(stack,offset+own,slots.size(),true):moveItemStackTo(stack,offset,offset+own,false);if(!moved)return ItemStack.EMPTY;slot.setByPlayer(stack.isEmpty()?ItemStack.EMPTY:stack);slot.setChanged();return before;}
    }
}

package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.logistics.*;
import io.github.jimbozoomer.jugcraft.mixin.AlchemicalItemAccess;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.menu.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.phys.*;

/** Supplied chute mesh with directional collection, selectable outputs and a finite engine-powered boost. */
public final class WorkshopChute {
    public static Block BLOCK,ITEM_BUFFER;public static BlockEntityType<Node> TYPE;public static ExtendedMenuType<Menu,BlockPos> MENU;
    public static final EnumProperty<Direction> FACING=BlockStateProperties.FACING;
    public static final Map<Direction,BooleanProperty> CONNECTIONS=new EnumMap<>(Direction.class);
    static{for(Direction side:Direction.values())CONNECTIONS.put(side,BooleanProperty.create("connected_"+side.getName()));}
    private WorkshopChute(){}
    public static void register(){var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_chute"));BLOCK=Registry.register(BuiltInRegistries.BLOCK,key,new Chute(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER).setId(key).noOcclusion()));
        var ik=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(BLOCK,new Item.Properties().setId(ik).useBlockDescriptionPrefix()));
        var bufferKey=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_item_buffer"));
        ITEM_BUFFER=Registry.register(BuiltInRegistries.BLOCK,bufferKey,new Chute(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(bufferKey)));
        var bufferItemKey=ResourceKey.create(Registries.ITEM,bufferKey.identifier());Registry.register(BuiltInRegistries.ITEM,bufferItemKey,new BlockItem(ITEM_BUFFER,new Item.Properties().setId(bufferItemKey).useBlockDescriptionPrefix()));
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_chute"),FabricBlockEntityTypeBuilder.create(Node::new,BLOCK,ITEM_BUFFER).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("jimta_chute"),new ExtendedMenuType<>(Menu::new,BlockPos.STREAM_CODEC));
        ItemStorage.SIDED.registerForBlockEntity(Node::port,TYPE);EnergyStorage.SIDED.registerForBlockEntity((n,side)->n.powerPort(),TYPE);
        PlayerBlockBreakEvents.BEFORE.register((l,p,pos,s,be)->!(be instanceof Node n)||n.identity(p));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->{out.accept(BLOCK);out.accept(ITEM_BUFFER);});
    }
    static final class Chute extends Block implements EntityBlock,ItemConnectable,EnergyConnectable {
        Chute(Properties p){super(p);var state=defaultBlockState().setValue(FACING,Direction.DOWN);for(var property:CONNECTIONS.values())state=state.setValue(property,false);registerDefaultState(state);}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING);for(var property:CONNECTIONS.values())b.add(property);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getClickedFace().getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T>t){return !l.isClientSide()&&t==TYPE?(world,p,state,e)->((Node)e).step():null;}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity actor,ItemStack stack){if(actor!=null&&l.getBlockEntity(p) instanceof Node n){n.owner=actor.getUUID();n.outputs=1<<s.getValue(FACING).ordinal();n.setChanged();}}
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){ItemNetworks.invalidate(l);EnergyNetworks.invalidate(l);}
        @Override protected void affectNeighborsAfterRemoval(BlockState s,ServerLevel l,BlockPos p,boolean moved){ItemNetworks.invalidate(l);EnergyNetworks.invalidate(l);}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!(l.getBlockEntity(p) instanceof Node n)||!n.permitted(player))return InteractionResult.FAIL;if(player instanceof ServerPlayer sp){if(n.owner==null){n.owner=sp.getUUID();n.setChanged();}sp.openMenu(n);}return InteractionResult.SUCCESS;}
    }
    public static final class Node extends BaseContainerBlockEntity implements ExtendedMenuProvider<BlockPos>,WorldlyContainer {
        public UUID owner;public int outputs=1;public boolean collect=true;private int cursor;
        private NonNullList<ItemStack> items=NonNullList.withSize(9,ItemStack.EMPTY);private long lastStep=Long.MIN_VALUE;
        public final SimpleEnergyStorage energy=new SimpleEnergyStorage(1000,32,0,this::setChanged);
        public Node(BlockPos p,BlockState s){super(TYPE,p,s);outputs=1<<s.getValue(FACING).ordinal();}
        public Direction intake(){return getBlockState().getValue(FACING).getOpposite();}
        boolean identity(Player p){return owner==null||owner.equals(p.getUUID())||JugcraftParties.sameParty(owner,p.getUUID());}
        public boolean permitted(Player p){return level!=null&&!isRemoved()&&level.getBlockEntity(worldPosition)==this&&p.isAlive()&&!p.isSpectator()&&identity(p)&&Authority.mayChange(level,p,worldPosition);}
        private boolean automation(){var p=level instanceof ServerLevel s?Authority.answering(s,owner):null;return p!=null&&permitted(p);}
        private boolean allowed(BlockPos p){return level instanceof ServerLevel s&&ClaimTransfers.allowed(s,worldPosition,p)&&Authority.mayChangeFor(s,owner,p);}
        public Storage<ItemVariant> port(Direction side){var inventory=ContainerStorage.of(this,null);return new FilteringStorage<ItemVariant>(inventory){
            public boolean canInsert(ItemVariant v){return side==intake()&&automation();}
            public boolean canExtract(ItemVariant v){return side!=null&&side!=intake()&&(outputs&(1<<side.ordinal()))!=0&&automation();}
        };}
        public EnergyStorage powerPort(){return new EnergyStorage(){public boolean supportsInsertion(){return true;}public long insert(long max,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){return automation()?energy.insert(max,tx):0;}public long extract(long max,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){return 0;}public boolean supportsExtraction(){return false;}public long getAmount(){return energy.getAmount();}public long getCapacity(){return energy.getCapacity();}public Object transferIdentity(){return energy;}};}
        public boolean configure(Player p,int button){if(!stillValid(p))return false;if(button==6)collect=!collect;else if(button>=0&&button<6&&button!=intake().ordinal())outputs^=1<<button;else return false;setChanged();ItemNetworks.invalidate(level);return true;}
        public void step(){if(!(level instanceof ServerLevel s)||lastStep==s.getGameTime()||!automation()||s.hasNeighborSignal(worldPosition))return;lastStep=s.getGameTime();boolean boosted=energy.getAmount()>=4;if(s.getGameTime()%(boosted?4:20)!=0)return;
            var shape=getBlockState();for(Direction side:Direction.values()){var neighbor=worldPosition.relative(side);boolean connected=side!=intake()&&(outputs&(1<<side.ordinal()))!=0&&allowed(neighbor)&&(s.getBlockState(neighbor).getBlock() instanceof ItemConnectable||ItemStorage.SIDED.find(s,neighbor,side.getOpposite())!=null);shape=shape.setValue(CONNECTIONS.get(side),connected);}if(shape!=getBlockState())s.setBlockAndUpdate(worldPosition,shape);
            var inventory=ContainerStorage.of(this,null);BlockPos input=worldPosition.relative(intake());long moved=0;int limit=boosted?16:1;
            if(collect&&allowed(input)){
                var source=ItemStorage.SIDED.find(s,input,intake().getOpposite());
                if(source!=null)moved=moveBounded(source,inventory,limit);
                // Collect only through an open intake, never through a solid wall or closed container.
                else if(s.getBlockState(input).getCollisionShape(s,input).isEmpty()){
                    List<ItemEntity> entities=new ArrayList<>();s.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),new AABB(input),e->e.isAlive()&&!e.hasPickUpDelay(),entities,8);
                    for(var e:entities){UUID target=((AlchemicalItemAccess)e).jugcraft$pickupTarget();if(target!=null&&!target.equals(owner)||!allowed(e.blockPosition()))continue;var stack=e.getItem();try(var tx=Transaction.openOuter()){
                        long count=inventory.insert(ItemVariant.of(stack),Math.min(limit,stack.getCount()),tx);if(count<=0)continue;tx.commit();int left=stack.getCount()-(int)count;if(left==0)e.discard();else e.setItem(stack.copyWithCount(left));moved+=count;break;}}
                }
            }
            long left=limit;for(int i=0;i<6&&left>0;i++){Direction side=Direction.values()[(cursor+i)%6];if(side==intake()||(outputs&(1<<side.ordinal()))==0||!allowed(worldPosition.relative(side)))continue;long sent=ItemNetworks.push(s,worldPosition,side,inventory,left,Set.of(worldPosition,input));left-=sent;moved+=sent;}
            cursor=(cursor+1)%6;if(moved>0){if(boosted)energy.setAmount(energy.getAmount()-4);setChanged();}
        }
        private static long moveBounded(Storage<ItemVariant> source,Storage<ItemVariant> target,int max){Map<ItemVariant,Long> offered=new LinkedHashMap<>();int count=0;for(var view:source){if(++count>128)return 0;if(!view.isResourceBlank()&&view.getAmount()>0)offered.merge(view.getResource(),Math.min(max,view.getAmount()),(a,b)->Math.min(max,a+b));}
            long moved=0;for(var entry:offered.entrySet()){if(moved>=max)break;try(var tx=Transaction.openOuter()){long accepted=target.insert(entry.getKey(),Math.min(max-moved,entry.getValue()),tx);if(accepted>0&&source.extract(entry.getKey(),accepted,tx)==accepted){tx.commit();moved+=accepted;}}}return moved;}
        @Override public boolean stillValid(Player p){return super.stillValid(p)&&permitted(p);}
        @Override public int getContainerSize(){return 9;}@Override protected NonNullList<ItemStack> getItems(){return items;}@Override protected void setItems(NonNullList<ItemStack> v){items=v;}
        @Override public int[] getSlotsForFace(Direction side){return automation()&&(side==intake()||(outputs&(1<<side.ordinal()))!=0)?new int[]{0,1,2,3,4,5,6,7,8}:new int[0];}
        @Override public boolean canPlaceItemThroughFace(int i,ItemStack stack,Direction side){return automation()&&side==intake();}
        @Override public boolean canTakeItemThroughFace(int i,ItemStack stack,Direction side){return automation()&&side!=intake()&&(outputs&(1<<side.ordinal()))!=0;}
        @Override protected Component getDefaultName(){return getBlockState().getBlock().getName();}public BlockPos getScreenOpeningData(ServerPlayer p){return worldPosition;}
        private final ContainerData data=new ContainerData(){public int get(int i){return switch(i){case 0->outputs;case 1->collect?1:0;case 2->(int)energy.getAmount();case 3->intake().ordinal();default->0;};}public void set(int i,int v){}public int getCount(){return 4;}};
        @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new Menu(id,inv,worldPosition,this,data);}
        @Override public void preRemoveSideEffects(BlockPos p,BlockState s){if(level!=null&&!level.isClientSide()){Containers.dropContents(level,p,this);clearContent();}}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.putInt("outputs",outputs);out.putBoolean("collect",collect);out.putLong("energy",energy.getAmount());}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);items=NonNullList.withSize(9,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);outputs=in.getIntOr("outputs",1)&63&~(1<<intake().ordinal());collect=in.getBooleanOr("collect",true);energy.setAmount(in.getLongOr("energy",0));}
    }
    public static final class Menu extends AbstractContainerMenu {
        public final ContainerData data;private final Container contents;private final BlockPos pos;
        public Menu(int id,Inventory inv,BlockPos p){this(id,inv,p,new SimpleContainer(9),new SimpleContainerData(4));}
        Menu(int id,Inventory inv,BlockPos p,Container contents,ContainerData data){super(MENU,id);this.pos=p;this.contents=contents;this.data=data;addDataSlots(data);for(int i=0;i<9;i++)addSlot(new Slot(contents,i,8+18*i,20));
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+18*col,112+18*row));for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+18*col,170));}
        public boolean stillValid(Player p){return p.level().isClientSide()||p.level().getBlockEntity(pos)==contents&&contents.stillValid(p);}
        public boolean clickMenuButton(Player p,int id){return stillValid(p)&&contents instanceof Node n&&n.configure(p,id);}
        @Override public void clicked(int i,int b,ContainerInput type,Player p){if(stillValid(p))super.clicked(i,b,type,p);}
        @Override public ItemStack quickMoveStack(Player p,int i){if(!stillValid(p)||i<0||i>=slots.size())return ItemStack.EMPTY;var slot=slots.get(i);var stack=slot.getItem();var before=stack.copy();if(stack.isEmpty()||!(i<9?moveItemStackTo(stack,9,slots.size(),true):moveItemStackTo(stack,0,9,false)))return ItemStack.EMPTY;slot.setByPlayer(stack.isEmpty()?ItemStack.EMPTY:stack);slot.setChanged();return before;}
    }
}

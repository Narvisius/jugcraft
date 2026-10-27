package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.kinetic.KineticConsumer;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.menu.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Five source profile segments on a paid KE lathe, with explicit shared-gameplay finishing recipes. */
public final class IndustrialLathe {
    public static Block BLOCK;
    public static BlockEntityType<Node> TYPE;
    public static ExtendedMenuType<Menu,BlockPos> MENU;
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    private IndustrialLathe() {}
    public static void register(){
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_lathe"));
        BLOCK=Registry.register(BuiltInRegistries.BLOCK,key,new Lathe(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE).setId(key)));
        var ik=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(BLOCK,new Item.Properties().setId(ik).useBlockDescriptionPrefix()));
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_lathe"),FabricBlockEntityTypeBuilder.create(Node::new,BLOCK).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("jimta_lathe"),new ExtendedMenuType<>(Menu::new,BlockPos.STREAM_CODEC));
        ItemStorage.SIDED.registerForBlockEntity((node,side)->ContainerStorage.of(node,side),TYPE);
        PlayerBlockBreakEvents.BEFORE.register((l,p,at,state,be)->!(be instanceof Node node)||node.identity(p));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->out.accept(BLOCK));
    }
    public static int width(ItemStack stack){
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());return id.equals(Jugcraft.id("jimta_wood_turning_blank"))?3:id.equals(Jugcraft.id("jimta_iron_turning_blank"))?5:0;
    }
    public static int[] profile(ItemStack stack){
        int max=width(stack);int[] values=new int[5];var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        for(int i=0;i<5;i++)values[i]=max==0?0:Math.clamp(tag.getIntOr("JimtaLathe"+i,max),1,max);return values;
    }
    public static ItemStack finished(ItemStack stack){
        int[] p=profile(stack);String id=null;
        if(width(stack)==3&&Arrays.equals(p,new int[]{2,3,2,3,2}))return new ItemStack(Items.STICK,4);
        if(width(stack)==5&&Arrays.equals(p,new int[]{4,3,4,3,4}))id="iron_shaft";
        if(width(stack)==5&&Arrays.equals(p,new int[]{3,2,3,2,3}))id="jimta_thin_fluted_axle";
        if(width(stack)==5&&Arrays.equals(p,new int[]{4,3,2,2,2}))id="jimta_turned_handle";
        if(width(stack)==5&&Arrays.equals(p,new int[]{5,4,3,2,1}))id="jimta_turned_drill";
        return id==null?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id(id)));
    }
    public static final class Blank extends Item {
        public Blank(Properties p){super(p.stacksTo(1));}
        @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){out.accept(Component.literal("Lathe profile: "+Arrays.toString(profile(s))));out.accept(Component.literal(width(s)==3?"Cut to 2·3·2·3·2 for wooden rods.":"Choose a target profile in the kinetic lathe's help menu."));}
    }
    static final class Lathe extends Block implements EntityBlock {
        public static final EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;
        Lathe(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(LIT,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){io.github.jimbozoomer.jugcraft.kinetic.KineticNetworks.invalidate(l);}
        @Override protected void affectNeighborsAfterRemoval(BlockState s,ServerLevel l,BlockPos p,boolean moved){io.github.jimbozoomer.jugcraft.kinetic.KineticNetworks.invalidate(l);}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity who,ItemStack stack){if(who!=null&&l.getBlockEntity(p) instanceof Node n){n.owner=who.getUUID();n.setChanged();}}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide()&&type==TYPE?(world,p,state,e)->((Node)e).tick():null;}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide()&&l.getBlockEntity(p) instanceof Node n){if(!n.permitted(player))return InteractionResult.FAIL;player.openMenu(n);}return InteractionResult.SUCCESS;}
    }
    public static final class Node extends BaseContainerBlockEntity implements WorldlyContainer,ExtendedMenuProvider<BlockPos>,KineticConsumer {
        private NonNullList<ItemStack> items=NonNullList.withSize(3,ItemStack.EMPTY);
        public UUID owner;public int kinetic,status;private long lastCut=-1;
        final ContainerData data=new ContainerData(){public int get(int i){return i==0?kinetic:status;}public void set(int i,int v){}public int getCount(){return 2;}};
        public Node(BlockPos p,BlockState s){super(TYPE,p,s);}
        boolean identity(Player p){return owner==null||owner.equals(p.getUUID())||JugcraftParties.sameParty(owner,p.getUUID());}
        public boolean permitted(Player p){return !isRemoved()&&level!=null&&level.getBlockEntity(worldPosition)==this&&p.isAlive()&&!p.isSpectator()&&identity(p)&&Authority.mayChange(level,p,worldPosition);}
        private boolean automation(){var actor=level instanceof ServerLevel s?Authority.answering(s,owner):null;return actor!=null&&permitted(actor);}
        @Override public long acceptKinetic(Direction side,long maximum){if(level==null||level.isClientSide()||isRemoved()||level.getBlockEntity(worldPosition)!=this||maximum<=0||level.hasNeighborSignal(worldPosition)||io.github.jimbozoomer.jugcraft.jimtafacts.network.SpatialReservation.reserved(level,worldPosition))return 0;int moved=(int)Math.min(maximum,10_000-kinetic);if(moved>0){kinetic+=moved;setChanged();}return moved;}
        public void tick(){if(level==null||level.isClientSide())return;boolean lit=kinetic>500&&!level.hasNeighborSignal(worldPosition);if(getBlockState().getValue(LIT)!=lit)level.setBlockAndUpdate(worldPosition,getBlockState().setValue(LIT,lit));
            // Native flywheel drag once per second; bounded and saved, not an offline wall-clock charge.
            if(level.getGameTime()%20==0&&kinetic>0){kinetic=Math.max(0,kinetic-Math.max(1,kinetic/100));setChanged();}}
        public boolean cut(int segment,Player player){
            if(segment<0||segment>=5||!permitted(player))return false;
            if(level.hasNeighborSignal(worldPosition)){status=1;return false;}
            if(level.getGameTime()==lastCut)return false;
            var blank=items.get(1);var tool=items.get(0);int[] p=profile(blank);
            if(width(blank)==0||blank.getCount()!=1||!tool.is(IndustrialUtilityItems.ITEMS.get("lathing_tool"))||tool.getDamageValue()>=tool.getMaxDamage()){status=2;return false;}
            if(p[segment]<=1){status=3;return false;}if(kinetic<1000){status=4;return false;}
            var tag=blank.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();int previous=Math.clamp(tag.getIntOr("JimtaLatheCuts",0),0,20);
            int cuts=previous+1;boolean byproduct=cuts%(width(blank)==3?10:2)==0;
            var scrap=new ItemStack(width(blank)==3?Items.STICK:Items.IRON_NUGGET);var output=items.get(2);
            if(byproduct&&!output.isEmpty()&&(!ItemStack.isSameItemSameComponents(output,scrap)||output.getCount()>=output.getMaxStackSize())){status=5;return false;}
            tag.putInt("JimtaLathe"+segment,p[segment]-1);tag.putInt("JimtaLatheCuts",cuts);blank.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            blank.set(DataComponents.CUSTOM_MODEL_DATA,new net.minecraft.world.item.component.CustomModelData(Arrays.stream(profile(blank)).mapToObj(i->(float)i).toList(),List.of(),List.of(),List.of()));
            if(byproduct){if(output.isEmpty())items.set(2,scrap);else output.grow(1);}
            tool.setDamageValue(tool.getDamageValue()+1);if(tool.getDamageValue()>=tool.getMaxDamage())items.set(0,ItemStack.EMPTY);
            kinetic-=1000;lastCut=level.getGameTime();status=6;setChanged();return true;
        }
        public boolean finish(Player player){if(!permitted(player)||level.hasNeighborSignal(worldPosition)||items.get(1).getCount()!=1)return false;var product=finished(items.get(1));if(product.isEmpty()){status=7;return false;}items.set(1,product);status=8;setChanged();return true;}
        @Override public boolean stillValid(Player p){return super.stillValid(p)&&permitted(p);}
        @Override public int getContainerSize(){return 3;}
        @Override protected NonNullList<ItemStack> getItems(){return items;}
        @Override protected void setItems(NonNullList<ItemStack> value){items=value;}
        @Override protected Component getDefaultName(){return Component.literal("Kinetic Lathe");}
        @Override public BlockPos getScreenOpeningData(ServerPlayer p){return worldPosition;}
        @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new Menu(id,inv,worldPosition,this,data);}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0?stack.is(IndustrialUtilityItems.ITEMS.get("lathing_tool")):slot==1&&width(stack)>0&&stack.getCount()==1;}
        @Override public int[] getSlotsForFace(Direction d){return d==Direction.DOWN?new int[]{1,2}:d==Direction.UP?new int[]{1}:new int[]{0};}
        @Override public boolean canPlaceItemThroughFace(int slot,ItemStack s,@Nullable Direction d){return d!=Direction.DOWN&&automation()&&canPlaceItem(slot,s);}
        @Override public boolean canTakeItemThroughFace(int slot,ItemStack s,Direction d){return d==Direction.DOWN&&automation()&&(slot==2||slot==1&&width(s)==0);}
        @Override public void preRemoveSideEffects(BlockPos p,BlockState s){if(level!=null&&!level.isClientSide()){Containers.dropContents(level,p,this);clearContent();}}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);out.putInt("kinetic",kinetic);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);items=NonNullList.withSize(3,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);kinetic=Math.clamp(in.getIntOr("kinetic",0),0,10000);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);}
    }
    public static final class Menu extends AbstractContainerMenu {
        public final Container contents;public final ContainerData data;private final BlockPos pos;
        public Menu(int id,Inventory inv,BlockPos pos){this(id,inv,pos,new SimpleContainer(3),new SimpleContainerData(2));}
        Menu(int id,Inventory inv,BlockPos pos,Container contents,ContainerData data){super(MENU,id);this.contents=contents;this.data=data;this.pos=pos;addDataSlots(data);
            addSlot(new Slot(contents,0,10,30){@Override public boolean mayPlace(ItemStack s){return s.is(IndustrialUtilityItems.ITEMS.get("lathing_tool"));}@Override public int getMaxStackSize(){return 1;}});
            addSlot(new Slot(contents,1,10,12){@Override public boolean mayPlace(ItemStack s){return width(s)>0;}@Override public int getMaxStackSize(){return 1;}});
            addSlot(new Slot(contents,2,10,57){@Override public boolean mayPlace(ItemStack s){return false;}});
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,84+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,142));
        }
        @Override public boolean stillValid(Player p){return p.level().isClientSide()||p.level().getBlockEntity(pos)==contents&&contents.stillValid(p);}
        @Override public boolean clickMenuButton(Player p,int id){if(!stillValid(p)||!(contents instanceof Node n))return false;return id==5?n.finish(p):n.cut(id,p);}
        @Override public void clicked(int i,int b,ContainerInput type,Player p){if(stillValid(p))super.clicked(i,b,type,p);}
        @Override public ItemStack quickMoveStack(Player p,int i){if(!stillValid(p)||i<0||i>=slots.size())return ItemStack.EMPTY;var slot=slots.get(i);var stack=slot.getItem();var before=stack.copy();if(stack.isEmpty())return ItemStack.EMPTY;
            boolean moved=i<3?moveItemStackTo(stack,3,slots.size(),true):width(stack)>0?moveItemStackTo(stack,1,2,false):stack.is(IndustrialUtilityItems.ITEMS.get("lathing_tool"))&&moveItemStackTo(stack,0,1,false);
            if(!moved)return ItemStack.EMPTY;slot.setByPlayer(stack.isEmpty()?ItemStack.EMPTY:stack);slot.setChanged();return before;}
    }
}

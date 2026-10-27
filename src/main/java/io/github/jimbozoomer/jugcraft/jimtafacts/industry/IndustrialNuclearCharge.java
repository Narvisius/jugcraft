package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
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

/** Original charge housing and ring inventory, bounded by the shared owner/claim blast policy. */
public final class IndustrialNuclearCharge {
    private IndustrialNuclearCharge(){}
    public static Block BLOCK;public static BlockEntityType<Node> TYPE;public static MenuType<Menu> MENU;
    public static void register(){
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_nuclear_charge"));
        BLOCK=Registry.register(BuiltInRegistries.BLOCK,key,new Housing(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4,6).setId(key)));
        var item=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,item,new BlockItem(BLOCK,new Item.Properties().setId(item).useBlockDescriptionPrefix()));
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,key.identifier(),FabricBlockEntityTypeBuilder.create(Node::new,BLOCK).build());
        MENU=Registry.register(BuiltInRegistries.MENU,key.identifier(),new MenuType<>(Menu::new,FeatureFlags.DEFAULT_FLAGS));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(o->o.accept(BLOCK));
    }
    public static boolean core(ItemStack stack){return Set.of("jimta_uranium_235","jimta_plutonium_pellet").stream().anyMatch(id->stack.is(BuiltInRegistries.ITEM.getValue(Jugcraft.id(id))));}
    private static final class Housing extends Block implements EntityBlock {
        static final EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;
        Housing(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity entity,ItemStack stack){if(entity instanceof ServerPlayer player&&l.getBlockEntity(p) instanceof Node n){n.owner=player.getUUID();n.setChanged();}}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player player,BlockHitResult hit){if(player instanceof ServerPlayer p&&l.getBlockEntity(pos) instanceof Node n&&n.permitted(p))p.openMenu(n);return InteractionResult.SUCCESS;}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide()&&type==TYPE?(world,pos,state,node)->((Node)node).tick():null;}
    }
    public static final class Node extends BaseContainerBlockEntity implements WorldlyContainer {
        private NonNullList<ItemStack> items=NonNullList.withSize(9,ItemStack.EMPTY);
        public UUID owner;public int fuse=-1;
        private final ContainerData data=new ContainerData(){public int get(int i){return fuse;}public void set(int i,int n){}public int getCount(){return 1;}};
        public Node(BlockPos p,BlockState s){super(TYPE,p,s);}
        public boolean permitted(Player player){return player instanceof ServerPlayer p&&p.isAlive()&&!p.isSpectator()&&p.level()==level&&owner!=null&&(owner.equals(p.getUUID())||io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(owner,p.getUUID()))&&p.distanceToSqr(Vec3.atCenterOf(worldPosition))<=64&&Authority.mayChange(level,p,worldPosition);}
        public boolean loaded(){if(!core(items.get(0)))return false;for(int i=1;i<9;i++)if(!items.get(i).is(IndustrialDynamite.INDUSTRIAL_TNT.asItem()))return false;return true;}
        public boolean arm(ServerPlayer player){if(!permitted(player)||fuse>=0||!loaded())return false;fuse=200;setChanged();return true;}
        public boolean disarm(ServerPlayer player){if(!permitted(player)||fuse<0)return false;fuse=-1;setChanged();return true;}
        public void tick(){
            if(!(level instanceof ServerLevel server)||fuse<0)return;
            var actor=Authority.present(server,owner);
            if(actor==null||actor.isSpectator()||!Authority.mayChange(server,actor,worldPosition)){fuse=-1;setChanged();return;}
            if(!loaded()){fuse=-1;setChanged();return;}
            if(--fuse>0){if(fuse%20==0)server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,3,.1,.1,.1,0);setChanged();return;}
            // All cargo is consumed before the block-removal callback; it cannot drop a second copy.
            for(var player:server.players())if(player.containerMenu instanceof Menu menu&&menu.node==this)player.closeContainer();
            clearContent();fuse=-1;server.removeBlock(worldPosition,false);
            IndustrialDynamite.nuclearBurst(server,Vec3.atCenterOf(worldPosition),owner);
        }
        @Override protected Component getDefaultName(){return Component.literal("Industrial Nuclear Charge");}
        @Override protected NonNullList<ItemStack> getItems(){return items;}
        @Override protected void setItems(NonNullList<ItemStack> values){items=values;}
        // No hopper or sided automation may change a charge, especially during its fuse.
        @Override public int[] getSlotsForFace(Direction side){return new int[0];}
        @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return false;}
        @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return false;}
        @Override public int getContainerSize(){return 9;}
        @Override public int getMaxStackSize(){return 1;}
        @Override public boolean canPlaceItem(int i,ItemStack stack){return fuse<0&&(i==0?core(stack):stack.is(IndustrialDynamite.INDUSTRIAL_TNT.asItem()));}
        @Override public boolean stillValid(Player p){return super.stillValid(p)&&permitted(p);}
        @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new Menu(id,inventory,this,data);}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);if(owner!=null)out.store("Owner",UUIDUtil.CODEC,owner);out.putInt("Fuse",fuse);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);items=NonNullList.withSize(9,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);owner=in.read("Owner",UUIDUtil.CODEC).orElse(null);fuse=Math.clamp(in.getIntOr("Fuse",-1),-1,200);}
        @Override public void preRemoveSideEffects(BlockPos p,BlockState s){fuse=-1;if(level!=null&&!level.isClientSide()){Containers.dropContents(level,p,this);clearContent();}super.preRemoveSideEffects(p,s);}
    }
    public static final class Menu extends AbstractContainerMenu {
        final Node node;final Container storage;public final ContainerData data;
        public Menu(int id,Inventory inv){this(id,inv,null,new SimpleContainerData(1));}
        Menu(int id,Inventory inv,Node node,ContainerData data){super(MENU,id);this.node=node;this.storage=node==null?new SimpleContainer(9):node;this.data=data;addDataSlots(data);
            int[][] positions={{79,62},{52,8},{106,8},{26,35},{133,35},{26,89},{133,89},{52,116},{106,116}};
            for(int i=0;i<9;i++){int slot=i;addSlot(new Slot(storage,i,positions[i][0],positions[i][1]){@Override public int getMaxStackSize(){return 1;}@Override public boolean mayPlace(ItemStack s){return data.get(0)<0&&(slot==0?core(s):s.is(IndustrialDynamite.INDUSTRIAL_TNT.asItem()));}@Override public boolean mayPickup(Player p){return data.get(0)<0;}});}
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,137+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,195));
        }
        @Override public boolean stillValid(Player p){return node==null||node.stillValid(p);}
        @Override public boolean clickMenuButton(Player p,int button){if(node==null||!stillValid(p)||!(p instanceof ServerPlayer player))return false;return button==0?node.arm(player):button==1&&node.disarm(player);}
        @Override public void clicked(int slot,int button,ContainerInput input,Player p){if(stillValid(p))super.clicked(slot,button,input,p);}
        @Override public ItemStack quickMoveStack(Player p,int i){if(!stillValid(p)||data.get(0)>=0||i<0||i>=slots.size())return ItemStack.EMPTY;var slot=slots.get(i);var stack=slot.getItem();if(stack.isEmpty())return ItemStack.EMPTY;var copy=stack.copy();if(i<9?!moveItemStackTo(stack,9,slots.size(),true):!moveItemStackTo(stack,core(stack)?0:1,core(stack)?1:9,false))return ItemStack.EMPTY;slot.setByPlayer(stack.isEmpty()?ItemStack.EMPTY:stack);slot.setChanged();return copy;}
    }
}

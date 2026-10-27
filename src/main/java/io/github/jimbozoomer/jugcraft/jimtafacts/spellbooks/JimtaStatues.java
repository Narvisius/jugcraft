package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.shapes.*;

/** Two-block original player statues. Poses are available to everyone; no external supporter service. */
public final class JimtaStatues {
    public static final String ID="jimta_spellbooks_player_statue";
    public static final List<String> POSES=List.of("neutral","attention","squat","reaching","pointing_forward","fallen","statue","mourn","dramatic","waving","marionette","david","sitting","applause","archery","heart","ballet","zombie","walking","kneeling","ready_to_strike","exploring","at_guard");
    public static final IntegerProperty ROTATION=BlockStateProperties.ROTATION_16;
    public static final IntegerProperty Y=IntegerProperty.create("y_offset",0,1);
    public static Block BLOCK;
    public static final Map<String,Block> DECORATIVE=new LinkedHashMap<>();
    public static java.util.function.Function<String,Object> decorativeRenderer=kind->null;
    public static BlockEntityType<Node> TYPE;
    public static MenuType<Menu> MENU;
    public static java.util.function.Supplier<Object> renderer=()->null;
    private JimtaStatues(){}
    public static void register(){
        var id=Jugcraft.id(ID);var key=ResourceKey.create(Registries.BLOCK,id);
        BLOCK=Registry.register(BuiltInRegistries.BLOCK,key,new StatueBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).setId(key).noOcclusion()));
        var itemKey=ResourceKey.create(Registries.ITEM,id);
        JimtaSpellbooks.ITEMS.put("player_statue",Registry.register(BuiltInRegistries.ITEM,itemKey,new StatueItem(BLOCK,new Item.Properties().setId(itemKey).useBlockDescriptionPrefix())));
        for(String kind:List.of("keeper","ice_spider")){
            var blockKey=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_spellbooks_"+kind+"_statue"));
            Block block=Registry.register(BuiltInRegistries.BLOCK,blockKey,new StatueBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).setId(blockKey).noOcclusion()));
            DECORATIVE.put(kind,block);var ik=ResourceKey.create(Registries.ITEM,blockKey.identifier());
            JimtaSpellbooks.ITEMS.put(kind+"_statue",Registry.register(BuiltInRegistries.ITEM,ik,new StatueItem(block,new Item.Properties().setId(ik).useBlockDescriptionPrefix())));
        }
        var supported=new ArrayList<Block>();supported.add(BLOCK);supported.addAll(DECORATIVE.values());
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id,FabricBlockEntityTypeBuilder.create(Node::new,supported.toArray(Block[]::new)).build());
        MENU=Registry.register(BuiltInRegistries.MENU,id,new MenuType<>(Menu::new,FeatureFlags.DEFAULT_FLAGS));
    }
    public static final class StatueBlock extends Block implements EntityBlock {
        StatueBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(ROTATION,0).setValue(Y,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(ROTATION,Y);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){
            var above=c.getClickedPos().above();var l=c.getLevel();
            if(above.getY()>l.getMaxY()||!l.hasChunkAt(above)||!l.getBlockState(above).canBeReplaced(c))return null;
            if(c.getPlayer() instanceof ServerPlayer p&&(!Authority.mayChange(p.level(),p,c.getClickedPos())||!Authority.mayChange(p.level(),p,above)))return null;
            return defaultBlockState().setValue(ROTATION,RotationSegment.convertToSegment(c.getRotation()+180));
        }
        @Override public void setPlacedBy(Level l,BlockPos pos,BlockState s,LivingEntity placer,ItemStack stack){
            l.setBlock(pos.above(),s.setValue(Y,1),Block.UPDATE_ALL);
            if(l.getBlockEntity(pos) instanceof Node n&&placer!=null){n.profile=placer.getUUID();n.changed();}
        }
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos pos,Direction side,BlockPos otherPos,BlockState other,RandomSource random){
            if(side==(s.getValue(Y)==0?Direction.UP:Direction.DOWN)&&(!other.is(this)||other.getValue(Y)==s.getValue(Y)))return Blocks.AIR.defaultBlockState();
            return super.updateShape(s,l,ticks,pos,side,otherPos,other,random);
        }
        @Override public BlockState playerWillDestroy(Level l,BlockPos pos,BlockState s,Player p){
            // Only the lower half owns loot. Breaking the upper half removes/drops that lower half once.
            if(!l.isClientSide()&&s.getValue(Y)==1&&l.getBlockState(pos.below()).is(this))l.destroyBlock(pos.below(),!p.getAbilities().instabuild,p);
            return super.playerWillDestroy(l,pos,s,p);
        }
        @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(ROTATION,r.rotate(s.getValue(ROTATION),16));}
        @Override protected BlockState mirror(BlockState s,Mirror m){return s.setValue(ROTATION,m.mirror(s.getValue(ROTATION),16));}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(Y)==0?Shapes.or(Block.box(1,0,1,15,2,15),Block.box(4,2,4,12,16,12)):Block.box(4,0,4,12,16,12);}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState s){return s.getValue(Y)==0?new Node(pos,s):null;}
        private Node node(Level l,BlockPos pos,BlockState s){return l.getBlockEntity(pos.below(s.getValue(Y))) instanceof Node n?n:null;}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){
            if(p instanceof ServerPlayer sp){var n=node(l,pos,s);if(n==null||!n.permitted(sp))return InteractionResult.FAIL;if(n.kind().equals("player"))sp.openMenu(n);}return InteractionResult.SUCCESS;
        }
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
            if(!stack.is(Items.NAME_TAG)||!stack.has(DataComponents.CUSTOM_NAME))return InteractionResult.TRY_WITH_EMPTY_HAND;
            if(p instanceof ServerPlayer sp){var n=node(l,pos,s);if(n==null||!n.permitted(sp))return InteractionResult.FAIL;
                var target=sp.level().getServer().getPlayerList().getPlayerByName(stack.get(DataComponents.CUSTOM_NAME).getString());
                if(!n.kind().equals("player"))return InteractionResult.PASS;
                if(target==null){sp.sendOverlayMessage(Component.literal("Name a player who is currently online to copy their appearance."));return InteractionResult.FAIL;}
                if(!target.getUUID().equals(n.profile)){n.profile=target.getUUID();n.changed();stack.consume(1,sp);}
            }return InteractionResult.SUCCESS;
        }
    }
    public static final class Node extends BlockEntity implements GeoBlockEntity,MenuProvider {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        public int pose;public boolean flipped;public UUID profile;
        public Node(BlockPos pos,BlockState s){super(TYPE,pos,s);}
        public String kind(){return DECORATIVE.entrySet().stream().filter(e->getBlockState().is(e.getValue())).map(Map.Entry::getKey).findFirst().orElse("player");}
        public boolean permitted(ServerPlayer p){return JimtaSpellbooks.enabled()&&!isRemoved()&&p.isAlive()&&!p.isSpectator()&&p.level()==level&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(Vec3.atCenterOf(worldPosition))<=64&&Authority.mayChange(p.level(),p,worldPosition)&&Authority.mayChange(p.level(),p,worldPosition.above());}
        public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putInt("pose",pose);out.putBoolean("flipped",flipped);if(profile!=null)out.store("profile",UUIDUtil.CODEC,profile);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);pose=Math.clamp(in.getIntOr("pose",0),0,POSES.size()-1);flipped=in.getBooleanOr("flipped",false);profile=in.read("profile",UUIDUtil.CODEC).orElse(null);}
        @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
        @Override public Component getDisplayName(){return Component.literal("Statue Pose");}
        @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new Menu(id,inv,this);}
    }
    public static final class Menu extends AbstractContainerMenu {
        public final ContainerData data;private final Node node;
        public Menu(int id,Inventory inv){super(MENU,id);node=null;data=new SimpleContainerData(2);addDataSlots(data);}
        Menu(int id,Inventory inv,Node n){super(MENU,id);node=n;data=new ContainerData(){public int get(int i){return i==0?n.pose:n.flipped?1:0;}public void set(int i,int v){}public int getCount(){return 2;}};addDataSlots(data);}
        @Override public boolean stillValid(Player p){return node==null||p instanceof ServerPlayer sp&&node.permitted(sp);}
        @Override public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}
        @Override public boolean clickMenuButton(Player p,int id){
            if(!(p instanceof ServerPlayer sp)||node==null||!node.permitted(sp))return false;
            if(id>=0&&id<POSES.size())node.pose=id;else if(id==POSES.size())node.flipped=!node.flipped;else return false;
            node.changed();broadcastChanges();return true;
        }
    }
    public static final class StatueItem extends BlockItem implements GeoItem {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        StatueItem(Block b,Properties p){super(b,p);}
        @Override public Object getRenderProvider(){for(var entry:DECORATIVE.entrySet())if(getBlock()==entry.getValue())return decorativeRenderer.apply(entry.getKey());return renderer.get();}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){}
    }
}

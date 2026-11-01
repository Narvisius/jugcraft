package io.github.jimbozoomer.jugcraft.arcane;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Portable single-essence storage. Contents survive harvesting and can feed the forge. */
public final class ArcaneJar extends BlockEntity {
    public static final int LIMIT=10000;
    public static DataComponentType<Integer> CONTENT;
    private int kind,amount;
    int storedKind(){return kind;}
    int storedAmount(){return amount;}
    void take(int value){amount=Math.max(0,amount-Math.max(0,value));changed();}
    public ArcaneJar(BlockPos p,BlockState s){super(ForbiddenArcane.JAR,p,s);}
    public static void register(){CONTENT=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("forbidden_essence_storage"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,40003)).networkSynchronized(ByteBufCodecs.VAR_INT).build());}
    public static boolean portable(ItemStack s){return s.is(ForbiddenArcane.item("essence_utrem_jar"));}
    public static int kind(ItemStack s){return Math.clamp(s.getOrDefault(CONTENT,0)/10001,0,3);}
    public static int amount(ItemStack s){return Math.clamp(s.getOrDefault(CONTENT,0)%10001,0,LIMIT);}
    public static ItemStack drain(ItemStack s,int count){int left=Math.max(0,amount(s)-count);if(left==0)return new ItemStack(ForbiddenArcane.item("utrem_jar"));s.set(CONTENT,kind(s)*10001+left);return s;}
    private void decode(int data){kind=Math.clamp(data/10001,0,3);amount=Math.clamp(data%10001,0,LIMIT);}
    public ItemStack asItem(){var s=new ItemStack(ForbiddenArcane.item(amount==0?"utrem_jar":"essence_utrem_jar"));if(amount>0)s.set(CONTENT,kind*10001+amount);return s;}
    private void changed(){setChanged();if(level!=null){var s=getBlockState();int fill=amount==0?0:Math.clamp((amount+624)/625,1,16);var next=s.setValue(JarBlock.FILL,fill).setValue(JarBlock.KIND,kind);if(!next.equals(s))level.setBlock(worldPosition,next,3);level.sendBlockUpdated(worldPosition,next,next,3);}}
    InteractionResult use(ServerPlayer p,InteractionHand hand){
        if(!Authority.mayChange(p.level(),p,worldPosition))return InteractionResult.FAIL;
        var held=p.getItemInHand(hand);int type=ArcaneEssences.kind(held),value=ArcaneEssences.amount(held),room=LIMIT-amount;
        if(type>=0&&value>0&&(amount==0||kind==type)&&room>0){
            if(!ArcaneEssences.divisible(held)&&room<value)return InteractionResult.FAIL;
            int added=Math.min(room,value);kind=type;amount+=added;ArcaneEssences.consume(p,hand,added);changed();
        }else if(held.isEmpty()&&p.isShiftKeyDown()){
            // Clear before removal; the block's loot path cannot duplicate the stored jar.
            ItemStack take=asItem();amount=0;setChanged();p.level().removeBlock(worldPosition,false);if(!p.addItem(take))p.drop(take,false,net.minecraft.util.Prediction.SERVER_ONLY);return InteractionResult.SUCCESS;
        }
        p.sendOverlayMessage(Component.literal((amount==0?"Empty jar":new String[]{"Aureal","Souls","Blood","Experience"}[kind]+": "+amount+" / "+LIMIT)+" — sneak with an empty hand to pick up"));return InteractionResult.SUCCESS;
    }
    @Override protected void saveAdditional(ValueOutput o){super.saveAdditional(o);o.putInt("essence",kind*10001+amount);}
    @Override protected void loadAdditional(ValueInput i){super.loadAdditional(i);decode(Math.clamp(i.getIntOr("essence",0),0,40003));}
    @Override protected void collectImplicitComponents(DataComponentMap.Builder c){super.collectImplicitComponents(c);c.set(CONTENT,kind*10001+amount);}
    @Override protected void applyImplicitComponents(DataComponentGetter c){super.applyImplicitComponents(c);decode(c.getOrDefault(CONTENT,0));}
    public static final class JarBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
        public static final IntegerProperty FILL=IntegerProperty.create("fill",0,16),KIND=IntegerProperty.create("essence",0,3);
        public JarBlock(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,false).setValue(FILL,0).setValue(KIND,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(BlockStateProperties.WATERLOGGED,FILL,KIND);}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ArcaneJar(p,s);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.or(Block.box(3,0,3,13,13,13),Block.box(5,13,5,11,15,11));}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){int amount=ArcaneJar.amount(c.getItemInHand());return defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).is(Fluids.WATER)).setValue(KIND,ArcaneJar.kind(c.getItemInHand())).setValue(FILL,amount==0?0:Math.clamp((amount+624)/625,1,16));}
        @Override protected FluidState getFluidState(BlockState s){return s.getValue(BlockStateProperties.WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(s);}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos n,BlockState other,RandomSource r){if(s.getValue(BlockStateProperties.WATERLOGGED))ticks.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));return super.updateShape(s,l,ticks,p,d,n,other,r);}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity actor,ItemStack stack){super.setPlacedBy(l,p,s,actor,stack);if(l.getBlockEntity(p) instanceof ArcaneJar jar){jar.decode(stack.getOrDefault(CONTENT,0));jar.changed();}}
        @Override protected ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s,boolean data){return l.getBlockEntity(p) instanceof ArcaneJar jar?jar.asItem():new ItemStack(ForbiddenArcane.item("utrem_jar"));}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return player instanceof ServerPlayer p&&l.getBlockEntity(pos) instanceof ArcaneJar jar?jar.use(p,hand):InteractionResult.SUCCESS;}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player player,BlockHitResult hit){return player instanceof ServerPlayer p&&l.getBlockEntity(pos) instanceof ArcaneJar jar?jar.use(p,InteractionHand.MAIN_HAND):InteractionResult.SUCCESS;}
    }
}

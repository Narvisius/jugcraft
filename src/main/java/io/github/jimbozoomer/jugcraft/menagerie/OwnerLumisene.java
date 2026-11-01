package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.Optional;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.*;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Supplied finite luminous fuel: sixteen conserved layers, four bottles per bucket.
 * Native adaptation deliberately burns only its own fuel, never neighboring builds. */
public final class OwnerLumisene {
    public static final IntegerProperty LAYERS=IntegerProperty.create("layers",1,16),AGE=IntegerProperty.create("age",0,15);
    public static Fluid FLUID;public static Block BLOCK;public static Item BOTTLE,BUCKET;
    public static Holder<MobEffect> FLAMMABLE;
    private OwnerLumisene(){}
    public static void register(){
        FLAMMABLE=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("supps_flammable"),new MobEffect(MobEffectCategory.HARMFUL,0xDD4400){
            @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){return true;}
            @Override public boolean applyEffectTick(ServerLevel level,LivingEntity entity,int amplifier){
                if(entity.getRemainingFireTicks()>0&&entity.getRemainingFireTicks()<=160)entity.setRemainingFireTicks(8);
                return true;
            }
        });
        FLUID=Registry.register(BuiltInRegistries.FLUID,Jugcraft.id("supps_lumisene"),new Fuel());
        var bk=ResourceKey.create(Registries.BLOCK,Jugcraft.id("supps_lumisene"));
        BLOCK=Registry.register(BuiltInRegistries.BLOCK,bk,new Puddle(BlockBehaviour.Properties.of().setId(bk).strength(0,100).noCollision().noOcclusion().noLootTable().lightLevel(s->s.getValue(AGE)>0?15:11).sound(SoundType.EMPTY)));
        BOTTLE=container("lumisene_bottle",4,Items.GLASS_BOTTLE);BUCKET=container("lumisene_bucket",16,Items.BUCKET);
        FluidStorage.ITEM.registerForItems((stack,context)->new FullItemFluidStorage(context,Items.GLASS_BOTTLE,FluidVariant.of(FLUID),FluidConstants.BUCKET/4),BOTTLE);
        FluidStorage.ITEM.registerForItems((stack,context)->new FullItemFluidStorage(context,Items.BUCKET,FluidVariant.of(FLUID),FluidConstants.BUCKET),BUCKET);
        FluidStorage.combinedItemApiProvider(Items.GLASS_BOTTLE).register(context->new EmptyItemFluidStorage(context,BOTTLE,FLUID,FluidConstants.BUCKET/4));
        FluidStorage.combinedItemApiProvider(Items.BUCKET).register(context->new EmptyItemFluidStorage(context,BUCKET,FLUID,FluidConstants.BUCKET));
        for(var item:new Item[]{BOTTLE,BUCKET})DispenserBlock.registerBehavior(item,new net.minecraft.core.dispenser.DefaultDispenseItemBehavior(){
            @Override protected ItemStack execute(net.minecraft.core.dispenser.BlockSource source,ItemStack stack){
                var level=source.level();var target=source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                if(!level.hasChunkAt(target)||!level.isInWorldBounds(target)||!level.getWorldBorder().isWithinBounds(target)
                    ||!ClaimTransfers.allowed(level,source.pos(),target)||io.github.jimbozoomer.jugcraft.town.TownProtection.shieldsBlock(level,target))return stack;
                var state=level.getBlockState(target);int stored=state.is(BLOCK)?state.getValue(LAYERS):0;
                int volume=stack.is(BOTTLE)?4:16;
                if(!state.isAir()&&!state.is(BLOCK)||stored+volume>16||state.is(BLOCK)&&state.getValue(AGE)>0)return stack;
                set(level,target,stored+volume,0);return new ItemStack(volume==4?Items.GLASS_BOTTLE:Items.BUCKET);
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(o->{o.accept(BOTTLE);o.accept(BUCKET);});
        UseItemCallback.EVENT.register((player,level,hand)->{
            var held=player.getItemInHand(hand);int amount=held.is(Items.GLASS_BOTTLE)?4:held.is(Items.BUCKET)?16:0;
            if(amount==0)return InteractionResult.PASS;var hit=ContainerItem.trace(level,player);
            if(hit.getType()!=HitResult.Type.BLOCK||!level.getBlockState(hit.getBlockPos()).is(BLOCK))return InteractionResult.PASS;
            if(!(player instanceof ServerPlayer actor))return InteractionResult.SUCCESS;
            var at=hit.getBlockPos();var state=level.getBlockState(at);
            if(!Authority.mayChange(actor.level(),actor,at)||state.getValue(AGE)>0||state.getValue(LAYERS)<amount)return InteractionResult.FAIL;
            set(actor.level(),at,state.getValue(LAYERS)-amount,0);
            exchange(player,hand,amount==4?BOTTLE:BUCKET);level.playSound(null,at,SoundEvents.BUCKET_FILL,SoundSource.PLAYERS,.6F,1);return InteractionResult.SUCCESS;
        });
    }
    private static Item container(String name,int layers,Item remainder){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("supps_"+name));var item=Registry.register(BuiltInRegistries.ITEM,key,new ContainerItem(new Item.Properties().setId(key).stacksTo(1).craftRemainder(remainder),layers,remainder));OwnerSupplementaries.ITEMS.put(name,item);return item;}
    static void exchange(Player p,InteractionHand hand,Item result){if(p.hasInfiniteMaterials())return;var held=p.getItemInHand(hand);held.shrink(1);var out=new ItemStack(result);if(held.isEmpty())p.setItemInHand(hand,out);else if(!p.addItem(out))p.drop(out,false,net.minecraft.util.Prediction.SERVER_ONLY);}
    static void set(ServerLevel l,BlockPos p,int amount,int age){if(amount<=0)l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());else{var next=BLOCK.defaultBlockState().setValue(LAYERS,Math.clamp(amount,1,16)).setValue(AGE,Math.clamp(age,0,15));if(l.getBlockState(p)!=next){l.setBlockAndUpdate(p,next);l.scheduleTick(p,BLOCK,10);}}}
    public static boolean place(ServerPlayer actor,BlockPos p,int amount){
        var l=actor.level();if(amount<=0||amount>16||!l.hasChunkAt(p)||!l.isInWorldBounds(p)||!l.getWorldBorder().isWithinBounds(p)||!Authority.mayChange(l,actor,p))return false;
        var old=l.getBlockState(p);int stored=old.is(BLOCK)?old.getValue(LAYERS):0;if(!old.isAir()&&!old.is(BLOCK)||stored+amount>16||old.is(BLOCK)&&old.getValue(AGE)>0)return false;
        set(l,p,stored+amount,0);return true;
    }
    public static boolean light(ServerLevel l,BlockPos p,ServerPlayer actor){if(!Authority.mayChange(l,actor,p)||!l.getBlockState(p).is(BLOCK))return false;var s=l.getBlockState(p);if(s.getValue(AGE)>0)return false;set(l,p,s.getValue(LAYERS),1);return true;}
    /** At most five neighboring positions per tick, and no chunk loads or source creation. */
    public static void spread(ServerLevel l,BlockPos p){
        var s=l.getBlockState(p);if(!s.is(BLOCK))return;int amount=s.getValue(LAYERS),age=s.getValue(AGE);
        if(age>0){if(age==15){amount--;age=1;}else age++;set(l,p,amount,age);return;}
        for(var direction:new Direction[]{Direction.DOWN,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}){
            var q=p.relative(direction);if(!l.hasChunkAt(q)||!l.isInWorldBounds(q)||!l.getWorldBorder().isWithinBounds(q)||!ClaimTransfers.allowed(l,p,q)||io.github.jimbozoomer.jugcraft.town.TownProtection.shieldsBlock(l,q))continue;
            var target=l.getBlockState(q);if(!target.isAir()&&!target.is(BLOCK))continue;if(target.is(BLOCK)&&target.getValue(AGE)>0)continue;
            int there=target.is(BLOCK)?target.getValue(LAYERS):0;
            int moved=direction==Direction.DOWN?Math.min(amount,16-there):Math.max(0,(amount-there)/2);
            if(moved==0)continue;set(l,q,there+moved,0);amount-=moved;if(amount==0)break;
        }
        set(l,p,amount,0);
    }
    private static final class Puddle extends Block {
        Puddle(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(LAYERS,16).setValue(AGE,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(LAYERS,AGE);}
        @Override protected FluidState getFluidState(BlockState s){return FLUID.defaultFluidState().setValue(LAYERS,s.getValue(LAYERS));}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.box(0,0,0,1,s.getValue(LAYERS)/16D,1);}
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){l.scheduleTick(p,this,10);}
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){spread(l,p);}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos q,BlockState other,RandomSource r){ticks.scheduleTick(p,this,10);return s;}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
            if(!stack.is(Items.FLINT_AND_STEEL)&&!stack.is(Items.FIRE_CHARGE))return InteractionResult.PASS;
            if(!(p instanceof ServerPlayer actor))return InteractionResult.SUCCESS;if(!light(actor.level(),pos,actor))return InteractionResult.FAIL;
            if(stack.is(Items.FLINT_AND_STEEL))stack.hurtAndBreak(1,p,hand.asEquipmentSlot());else stack.consume(1,p);return InteractionResult.SUCCESS;
        }
        @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity e,InsideBlockEffectApplier effects,boolean precise){
            if(l instanceof ServerLevel&&e.getBoundingBox().minY<p.getY()+s.getValue(LAYERS)/16D&&e instanceof LivingEntity living){living.addEffect(new MobEffectInstance(MobEffects.GLOWING,100,0,false,false));living.addEffect(new MobEffectInstance(FLAMMABLE,100,0,false,false));if(s.getValue(AGE)>0)e.igniteForSeconds(2);}
        }
    }
    private static final class Fuel extends Fluid {
        Fuel(){registerDefaultState(getStateDefinition().any().setValue(LAYERS,16));}
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid,FluidState> b){b.add(LAYERS);}
        @Override public Item getBucket(){return BUCKET;}
        @Override protected boolean canBeReplacedWith(FluidState s,BlockGetter l,BlockPos p,Fluid f,Direction d){return false;}
        @Override protected Vec3 getFlow(BlockGetter l,BlockPos p,FluidState s){return Vec3.ZERO;}
        @Override public int getTickDelay(LevelReader l){return 10;}
        @Override protected float getExplosionResistance(){return 100;}
        @Override public float getHeight(FluidState s,BlockGetter l,BlockPos p){return getOwnHeight(s);}
        @Override public float getOwnHeight(FluidState s){return s.getValue(LAYERS)/16F;}
        @Override protected BlockState createLegacyBlock(FluidState s){return BLOCK.defaultBlockState().setValue(LAYERS,s.getValue(LAYERS));}
        @Override public boolean isSource(FluidState s){return s.getValue(LAYERS)==16;}
        @Override public int getAmount(FluidState s){return s.getValue(LAYERS);}
        @Override public VoxelShape getShape(FluidState s,BlockGetter l,BlockPos p){return Shapes.box(0,0,0,1,getOwnHeight(s),1);}
        @Override public Optional<SoundEvent> getPickupSound(){return Optional.of(SoundEvents.BUCKET_FILL);}
    }
    private static final class ContainerItem extends Item {
        final int layers;final Item remainder;ContainerItem(Properties p,int layers,Item remainder){super(p);this.layers=layers;this.remainder=remainder;}
        static BlockHitResult trace(Level l,Player p){return getPlayerPOVHitResult(l,p,ClipContext.Fluid.ANY);}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer actor))return InteractionResult.SUCCESS;
            var at=c.getLevel().getBlockState(c.getClickedPos()).is(BLOCK)?c.getClickedPos():c.getClickedPos().relative(c.getClickedFace());
            if(!place(actor,at,layers))return InteractionResult.FAIL;exchange(actor,c.getHand(),remainder);return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){
            if(layers!=4)return InteractionResult.PASS;p.startUsingItem(hand);return InteractionResult.SUCCESS;
        }
        @Override public int getUseDuration(ItemStack s,LivingEntity e){return 32;}
        @Override public ItemUseAnimation getUseAnimation(ItemStack s){return ItemUseAnimation.DRINK;}
        @Override public ItemStack finishUsingItem(ItemStack s,Level l,LivingEntity e){
            if(!l.isClientSide()){e.removeEffect(MobEffects.POISON);e.addEffect(new MobEffectInstance(MobEffects.GLOWING,600));e.addEffect(new MobEffectInstance(FLAMMABLE,600));}
            if(e instanceof Player p&&p.hasInfiniteMaterials())return s;s.shrink(1);return s.isEmpty()?new ItemStack(remainder):s;
        }
    }
}

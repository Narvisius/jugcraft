package io.github.jimbozoomer.jugcraft.menagerie;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** One-seat, server-steered dive vehicle. Standard player input drives it without a second key-binding scheme. */
public final class CavernSubmarine extends Entity implements GeoEntity {
    public static EntityType<CavernSubmarine> TYPE,BOARD;
    public boolean board(){return getType()==BOARD;}
    public boolean hidesPilot(){return !board();}
    private static final EntityDataAccessor<Boolean> RUNNING=SynchedEntityData.defineId(CavernSubmarine.class,EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private UUID owner;
    private int fuel;
    private float damage;
    CavernSubmarine(EntityType<? extends CavernSubmarine> type,Level l){super(type,l);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("cavern_submarine"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<CavernSubmarine>of(CavernSubmarine::new,MobCategory.MISC).sized(2.5F,1.8F).noLootTable().clientTrackingRange(10).updateInterval(1).build(key));var boardKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("beast_straddleboard"));BOARD=Registry.register(BuiltInRegistries.ENTITY_TYPE,boardKey,EntityType.Builder.<CavernSubmarine>of(CavernSubmarine::new,MobCategory.MISC).sized(.9F,.25F).fireImmune().noLootTable().clientTrackingRange(10).updateInterval(1).build(boardKey));}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(RUNNING,false);}
    @Override public LinearInterpolationHandler createInterpolationHandler(){return io.github.jimbozoomer.jugcraft.SmoothFlight.handler(this);}
    @Override public boolean isPickable(){return !isRemoved();}
    @Override public boolean canBeCollidedWith(Entity other){return !isRemoved();}
    @Override public boolean canCollideWith(Entity e){return !e.isPassengerOfSameVehicle(this);}
    @Override protected boolean canAddPassenger(Entity e){return getPassengers().isEmpty()&&e instanceof Player;}
    @Override protected Vec3 getPassengerAttachmentPoint(Entity e,EntityDimensions d,float scale){return new Vec3(0,board()?.3:.15,0);}
    @Override public InteractionResult interact(Player p,InteractionHand hand,Vec3 location){
        if(owner!=null&&!owner.equals(p.getUUID())&&!p.isCreative()||!Menagerie.mayChange(p.level(),p,blockPosition()))return InteractionResult.FAIL;
        var held=p.getItemInHand(hand);
        if(!board()&&held.is(Items.REDSTONE)&&fuel<=2800){if(!level().isClientSide()){held.consume(1,p);fuel+=200;p.sendOverlayMessage(Component.literal("Submarine power: "+fuel/20+" seconds"));}return InteractionResult.SUCCESS;}
        if(level().isClientSide())return InteractionResult.SUCCESS;
        if(p.startRiding(this)){owner=p.getUUID();p.sendOverlayMessage(Component.literal(board()?"W/S: throttle · Look to steer · Jump: hop · Sneak: exit":"W/S: throttle · Look to steer/dive · Jump: rise · Sneak: exit · Redstone: refuel"));return InteractionResult.SUCCESS;}return InteractionResult.PASS;
    }
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel l))return;
        boolean lava=l.getFluidState(blockPosition()).is(FluidTags.LAVA)||l.getFluidState(blockPosition().below()).is(FluidTags.LAVA);boolean water=l.getFluidState(blockPosition()).is(FluidTags.WATER)||board()&&lava;var velocity=getDeltaMovement().scale(.85);boolean running=false;
        if(getFirstPassenger() instanceof ServerPlayer pilot){
            if(board()){pilot.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,40,0,true,false));}
            else if(tickCount%20==0){pilot.setAirSupply(pilot.getMaxAirSupply());pilot.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,60,0,true,false));}
            if((water||board()&&onGround())&&(board()||fuel>0)){var input=pilot.getLastClientInput();int throttle=(input.forward()?1:0)-(input.backward()?1:0);
                setYRot(net.minecraft.util.Mth.rotLerp(.15F,getYRot(),pilot.getYRot()));setXRot(board()?0:net.minecraft.util.Mth.clamp(pilot.getXRot(),-50,50));
                var heading=Vec3.directionFromRotation(getXRot(),getYRot());velocity=velocity.add(heading.scale(throttle*.035));if(input.jump()&&(!board()||onGround()||lava))velocity=velocity.add(0,board()?.06:.035,0);
                running=throttle!=0||input.jump();if(running&&!board())fuel--;
            }
        }
        if(!water)velocity=velocity.add(0,-.04,0);else if(board()&&lava)velocity=velocity.add(0,.015,0);else if(!isVehicle())velocity=velocity.add(0,.005,0);
        if(velocity.lengthSqr()>.09)velocity=velocity.normalize().scale(.3);
        var next=blockPosition().offset((int)Math.signum(velocity.x)*2,0,(int)Math.signum(velocity.z)*2);
        if(!l.hasChunkAt(next))velocity=Vec3.ZERO;
        entityData.set(RUNNING,running);setDeltaMovement(velocity);move(MoverType.SELF,velocity);
    }
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount){
        if(isRemoved()||isInvulnerableToBase(source)||amount<=0||!Float.isFinite(amount))return false;
        if(source.getEntity() instanceof Player p&&(owner!=null&&!owner.equals(p.getUUID())&&!p.isCreative()||!Menagerie.mayChange(l,p,blockPosition())))return false;
        damage+=amount;if(damage<40)return true;
        var item=new ItemStack(Menagerie.item(board()?"beast_straddleboard":"cavern_submarine"));var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("SubmarineFuel",fuel);item.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));ejectPassengers();spawnAtLocation(l,item);discard();return true;
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){if(owner!=null)out.putString("Owner",owner.toString());out.putInt("Fuel",fuel);out.putFloat("Damage",damage);}
    @Override protected void readAdditionalSaveData(ValueInput in){try{owner=UUID.fromString(in.getStringOr("Owner",""));}catch(IllegalArgumentException ignored){owner=null;}fuel=Math.clamp(in.getIntOr("Fuel",0),0,3000);damage=Math.clamp(in.getFloatOr("Damage",0),0,39);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<CavernSubmarine>("drive",4,s->s.setAndContinue(RawAnimation.begin().thenLoop(entityData.get(RUNNING)?"drive":"idle"))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    static final class Kit extends Item {
        private final boolean board;
        Kit(Properties p){this(p,false);}
        Kit(Properties p,boolean board){super(p.stacksTo(1).fireResistant());this.board=board;}
        @Override public InteractionResult use(Level l,Player who,InteractionHand hand){
            if(!(who instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var hit=p.pick(6,0,true);if(!(hit instanceof net.minecraft.world.phys.BlockHitResult block)||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)return InteractionResult.PASS;
            return place(p,p.getItemInHand(hand),block.getBlockPos());
        }
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var at=p.level().getFluidState(c.getClickedPos()).is(FluidTags.WATER)?c.getClickedPos():c.getClickedPos().relative(c.getClickedFace());return place(p,c.getItemInHand(),at);
        }
        private InteractionResult place(ServerPlayer p,ItemStack stack,BlockPos at){
            if(!Menagerie.mayChange(p.level(),p,at)||!board&&!p.level().getFluidState(at).is(FluidTags.WATER))return InteractionResult.FAIL;
            var vehicle=(board?BOARD:TYPE).create(p.level(),EntitySpawnReason.SPAWN_ITEM_USE);if(vehicle==null)return InteractionResult.FAIL;vehicle.owner=p.getUUID();vehicle.fuel=Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("SubmarineFuel",0),0,3000);vehicle.snapTo(at.getX()+.5,at.getY()+.1,at.getZ()+.5,p.getYRot(),0);
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(vehicle.getBoundingBox().minX,vehicle.getBoundingBox().minY,vehicle.getBoundingBox().minZ),BlockPos.containing(vehicle.getBoundingBox().maxX,vehicle.getBoundingBox().maxY,vehicle.getBoundingBox().maxZ)))if(!p.level().hasChunkAt(pos)||!Menagerie.mayChange(p.level(),p,pos))return InteractionResult.FAIL;
            if(!p.level().noCollision(vehicle)||!p.level().addFreshEntity(vehicle))return InteractionResult.FAIL;stack.consume(1,p);return InteractionResult.SUCCESS;
        }
    }
}

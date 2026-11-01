package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.world.level.ClipContext;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Supplied throwable charges; each splash/cloud target goes through the shared combat permissions. */
public final class OwnerAmendmentCharge extends ThrowableItemProjectile {
    public static EntityType<OwnerAmendmentCharge> TYPE;
    public static Item DRAGON;
    public static final Map<String,net.minecraft.core.particles.SimpleParticleType> PARTICLES=new LinkedHashMap<>();
    public static net.minecraft.sounds.SoundEvent IMPACT;
    private static final EntityDataAccessor<Boolean> CLOUD=SynchedEntityData.defineId(OwnerAmendmentCharge.class,EntityDataSerializers.BOOLEAN);
    private int age,cloudAge,lastDeflection=-20;private boolean extinguished;
    private final Map<UUID,Integer> struck=new HashMap<>();
    public OwnerAmendmentCharge(EntityType<? extends OwnerAmendmentCharge> type,Level level){super(type,level);}
    public static void register(){
        for(String name:List.of("fireball_trail","dragon_fireball_trail","fireball_explosion","boiling_bubble"))PARTICLES.put(name,Registry.register(BuiltInRegistries.PARTICLE_TYPE,Jugcraft.id("amends_"+name),net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple()));
        IMPACT=Registry.register(BuiltInRegistries.SOUND_EVENT,Jugcraft.id("amends_fireball_impact"),net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Jugcraft.id("amends_fireball_impact")));
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("amends_charge"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<OwnerAmendmentCharge>of(OwnerAmendmentCharge::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(8).updateInterval(1).build(key));
        var itemKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("amends_dragon_charge"));DRAGON=Registry.register(BuiltInRegistries.ITEM,itemKey,new Item(new Item.Properties().setId(itemKey)));
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(e->e.accept(DRAGON));
        var original=net.minecraft.world.level.block.DispenserBlock.DISPENSER_REGISTRY.get(Items.FIRE_CHARGE);
        net.minecraft.core.dispenser.DispenseItemBehavior dispenser=(source,stack)->{
            var component=io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialDynamite.DISPENSER_OWNER;
            if(!stack.has(component)&&stack.is(Items.FIRE_CHARGE)&&original!=null)return original.dispense(source,stack);
            var actor=io.github.jimbozoomer.jugcraft.concordance.Authority.present(source.level(),stack.get(component));
            var facing=source.state().getValue(net.minecraft.world.level.block.DispenserBlock.FACING);var target=source.pos().relative(facing);
            if(actor==null||!Menagerie.mayChange(source.level(),actor,source.pos())||!Menagerie.mayChange(source.level(),actor,target)||!source.level().hasChunkAt(target)||!source.level().getBlockState(target).getCollisionShape(source.level(),target).isEmpty())return stack;
            var shot=new OwnerAmendmentCharge(TYPE,source.level());shot.setOwner(actor);shot.setItem(stack.copyWithCount(1));shot.setPos(source.center().add(facing.getStepX()*.8,facing.getStepY()*.8,facing.getStepZ()*.8));shot.shoot(facing.getStepX(),facing.getStepY(),facing.getStepZ(),1.1F,1);
            if(source.level().addFreshEntity(shot)){stack.shrink(1);source.level().playSound(null,source.pos(),net.minecraft.sounds.SoundEvents.FIRECHARGE_USE,net.minecraft.sounds.SoundSource.BLOCKS,.5F,1);}return stack;
        };
        net.minecraft.world.level.block.DispenserBlock.registerBehavior(Items.FIRE_CHARGE,dispenser);net.minecraft.world.level.block.DispenserBlock.registerBehavior(DRAGON,dispenser);
        UseItemCallback.EVENT.register((player,level,hand)->{
            var stack=player.getItemInHand(hand);if(player.isSpectator()||!stack.is(DRAGON)&&!stack.is(Items.FIRE_CHARGE))return InteractionResult.PASS;
            if(player.isShiftKeyDown()){if(!level.isClientSide()){stack.set(io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialDynamite.DISPENSER_OWNER,player.getUUID());player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Charge bound for dispensers. You must be online; claims and combat protection apply."));}return InteractionResult.SUCCESS;}
            if(player.getCooldowns().isOnCooldown(stack))return InteractionResult.FAIL;
            if(level instanceof ServerLevel server){
                var shot=new OwnerAmendmentCharge(TYPE,server);shot.setOwner(player);shot.setItem(stack.copyWithCount(1));shot.setPos(player.getX(),player.getEyeY()-.1,player.getZ());shot.shootFromRotation(player,player.getXRot(),player.getYRot(),0,1.1F,1);
                if(!server.addFreshEntity(shot))return InteractionResult.FAIL;
                player.getCooldowns().addCooldown(stack,10);stack.consume(1,player);server.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.FIRECHARGE_USE,net.minecraft.sounds.SoundSource.PLAYERS,.5F,1);
            }return InteractionResult.SUCCESS;
        });
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(CLOUD,false);}
    public boolean cloud(){return entityData.get(CLOUD);}
    private boolean dragon(){return getItem().is(DRAGON);}
    @Override protected Item getDefaultItem(){return Items.FIRE_CHARGE;}
    @Override protected double getDefaultGravity(){return .03;}
    @Override public boolean isPickable(){return !cloud()&&!isRemoved();}
    @Override public boolean isAttackable(){return !cloud();}
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource damage,float amount){
        if(cloud()||age-lastDeflection<3||!(damage.getDirectEntity() instanceof ServerPlayer player)||!damage.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)||!Menagerie.mayChange(level,player,blockPosition()))return false;
        lastDeflection=age;setOwner(player);var look=player.getLookAngle();shoot(look.x,look.y,look.z,1.5F,0);needsSync=true;return true;
    }
    @Override public void tick(){
        if(cloud()){
            baseTick();if(!(level() instanceof ServerLevel server))return;
            if(++cloudAge>40){discard();return;}
            if(!(getOwner() instanceof ServerPlayer owner)||!owner.isAlive()||owner.level()!=server){discard();return;}
            float radius=1+5.5F*cloudAge/40;
            server.sendParticles(net.minecraft.core.particles.PowerParticleOption.create(ParticleTypes.DRAGON_BREATH,1),getX(),getY()+.1,getZ(),8,radius*.45,.2,radius*.45,.01);
            if(cloudAge%5==0){
                var targets=new ArrayList<LivingEntity>();server.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(radius,.5,radius),LivingEntity::isAlive,targets,32);
                struck.entrySet().removeIf(e->e.getValue()+20<=cloudAge);
                for(var target:targets){if(struck.size()>=256||target.distanceToSqr(this)>radius*radius||struck.getOrDefault(target.getUUID(),-100)+20>cloudAge||!allowed(server,owner,target))continue;
                    net.minecraft.world.effect.MobEffects.INSTANT_DAMAGE.value().applyInstantaneousEffect(server,this,owner,target,1,.5);struck.put(target.getUUID(),cloudAge);
                }
            }return;
        }
        super.tick();if(isRemoved())return;
        if(level() instanceof ServerLevel server){if(++age>200){discard();return;}if(isInWater())extinguished=true;if(age%2==0)server.sendParticles(dragon()?PARTICLES.get("dragon_fireball_trail"):extinguished?ParticleTypes.SMOKE:PARTICLES.get("fireball_trail"),getX(),getY(),getZ(),1,.02,.02,.02,0);}
    }
    private boolean allowed(ServerLevel level,ServerPlayer player,LivingEntity target){
        if(target==player||!TwoHanded.allowed(player,level,target))return false;
        return level.clip(new ClipContext(position(),target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS;
    }
    @Override protected void onHit(HitResult hit){
        if(cloud()||isRemoved()||!(level() instanceof ServerLevel level))return;
        if(!(getOwner() instanceof ServerPlayer player)||!player.isAlive()||player.level()!=level){discard();return;}
        setPos(hit.getLocation());
        if(hit instanceof EntityHitResult direct&&direct.getEntity() instanceof LivingEntity target&&allowed(level,player,target)){
            if(target.hurtServer(level,damageSources().thrown(this,player),dragon()?2:5)&&!dragon()&&!extinguished)target.igniteForSeconds(5);
        }
        if(dragon()){
            entityData.set(CLOUD,true);setDeltaMovement(Vec3.ZERO);setNoGravity(true);cloudAge=0;return;
        }
        if(!extinguished){
            var targets=new ArrayList<LivingEntity>();level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(1),LivingEntity::isAlive,targets,16);
            for(var target:targets)if(!(hit instanceof EntityHitResult direct&&direct.getEntity()==target)&&allowed(level,player,target)&&target.hurtServer(level,damageSources().thrown(this,player),2))target.igniteForSeconds(4);
            level.sendParticles(PARTICLES.get("fireball_explosion"),getX(),getY(),getZ(),1,0,0,0,0);
        }
        level.playSound(null,blockPosition(),IMPACT,net.minecraft.sounds.SoundSource.PLAYERS,.5F,1);discard();
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("Age",age);out.putInt("CloudAge",cloudAge);out.putBoolean("Cloud",cloud());out.putBoolean("Extinguished",extinguished);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);age=Math.clamp(in.getIntOr("Age",0),0,200);cloudAge=Math.clamp(in.getIntOr("CloudAge",0),0,40);entityData.set(CLOUD,in.getBooleanOr("Cloud",false));extinguished=in.getBooleanOr("Extinguished",false);if(cloud()){setNoGravity(true);cloudAge=40;}}
}

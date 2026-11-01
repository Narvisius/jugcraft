package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Twenty paid, original weapon projections; no block damage or recoverable/free items. */
public final class BotanicalRoyalWeapon extends ThrowableItemProjectile {
    public static EntityType<BotanicalRoyalWeapon> TYPE;
    private int age,delay,live=-1;private boolean launched;
    public BotanicalRoyalWeapon(EntityType<? extends BotanicalRoyalWeapon> type,Level level){super(type,level);setNoGravity(true);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("botanical_royal_weapon"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<BotanicalRoyalWeapon>of(BotanicalRoyalWeapon::new,MobCategory.MISC).sized(.4F,.4F).clientTrackingRange(10).updateInterval(1).build(key));}
    @Override protected Item getDefaultItem(){return Jugtania.item("king_key");}
    public boolean launched(){return launched;}
    static boolean summon(ServerPlayer p,ItemStack key,int index){
        if(index<0||index>=20||!BotanicalRelics.valid(p,key)||!Authority.mayChange(p.level(),p,p.blockPosition()))return false;
        var direction=p.getLookAngle().multiply(1,0,1).normalize();if(direction.lengthSqr()<.1)direction=new Vec3(0,0,1);var right=new Vec3(-direction.z,0,direction.x);
        double arc=(index%5)*Math.PI/4;var at=p.getEyePosition().subtract(direction.scale(2+index/5*.2)).add(right.scale(Math.cos(arc)*(3+index/5))).add(0,Math.sin(arc)*(2+index/5)+.5,0);
        if(!p.level().hasChunkAt(BlockPos.containing(at))||!Authority.mayChange(p.level(),p,BlockPos.containing(at))||!p.level().noCollision(new AABB(at.add(-.2,-.2,-.2),at.add(.2,.2,.2))))return false;
        var e=new BotanicalRoyalWeapon(TYPE,p.level());e.delay=index;e.setOwner(p);e.setPos(at);e.setYRot(p.getYRot());var display=key.copyWithCount(1);display.set(DataComponents.ITEM_MODEL,Jugcraft.id("botanical_gate_weapon_"+p.getRandom().nextInt(12)));e.setItem(display);
        if(!p.level().addFreshEntity(e))return false;p.level().playSound(null,e.blockPosition(),BotanicalSounds.get("babylon_spawn"),net.minecraft.sounds.SoundSource.PLAYERS,.25F,1+p.getRandom().nextFloat()*3);return true;
    }
    @Override public void tick(){
        if(!level().isClientSide()){
            if(++age>600||!(getOwner() instanceof ServerPlayer p)||!p.isAlive()||p.level()!=level()){discard();return;}
            if(live<0&&(!p.isUsingItem()||!p.getUseItem().is(Jugtania.item("king_key"))))live=0;
            if(live>=0&&!launched&&live++>=delay){launched=true;var eye=p.getEyePosition();var hit=p.pick(64,0,false);var target=hit.getType()==HitResult.Type.MISS?eye.add(p.getLookAngle().scale(64)):hit.getLocation();var v=target.subtract(position()).normalize().scale(2);setDeltaMovement(v);setYRot((float)Math.toDegrees(Math.atan2(v.x,v.z)));setXRot((float)Math.toDegrees(Math.atan2(v.y,Math.sqrt(v.x*v.x+v.z*v.z))));p.level().playSound(null,blockPosition(),BotanicalSounds.get("babylon_attack"),net.minecraft.sounds.SoundSource.PLAYERS,1,1);}
            if(!level().hasChunkAt(BlockPos.containing(position().add(getDeltaMovement())))||!Authority.mayChange(p.level(),p,blockPosition())){discard();return;}
        }
        var motion=getDeltaMovement();super.tick();setDeltaMovement(motion);
    }
    @Override protected void onHit(HitResult hit){
        if(!launched||!(level() instanceof ServerLevel level)||!(getOwner() instanceof ServerPlayer p))return;
        var at=BlockPos.containing(hit.getLocation());if(Authority.mayChange(level,p,at)){
            int checked=0;for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(at).inflate(3),e->e!=p&&e.isAlive())){
                if(++checked>64)break;if(!Authority.mayStrike(p,target)||!Authority.mayChange(level,p,target.blockPosition())||!p.hasLineOfSight(target))continue;
                double distance=target.position().distanceTo(hit.getLocation());if(distance>3)continue;float damage=hit instanceof EntityHitResult e&&e.getEntity()==target?20:(float)(12*(1-distance/3));if(damage>0)target.hurtServer(level,damageSources().thrown(this,p),damage);
            }level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,getX(),getY(),getZ(),1,0,0,0,0);
        }discard();
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("age",age);out.putInt("delay",delay);out.putInt("live",live);out.putBoolean("launched",launched);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);age=Math.clamp(in.getIntOr("age",0),0,600);delay=Math.clamp(in.getIntOr("delay",0),0,20);live=Math.clamp(in.getIntOr("live",0),-1,600);launched=in.getBooleanOr("launched",false);}
}

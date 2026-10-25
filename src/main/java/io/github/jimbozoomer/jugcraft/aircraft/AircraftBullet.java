package io.github.jimbozoomer.jugcraft.aircraft;

import io.github.jimbozoomer.jugcraft.concordance.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Native moving cannon round: swept collision, no terrain damage, impact-time authority. */
public final class AircraftBullet extends ThrowableItemProjectile {
    private Vec3 start;
    private int age;
    public AircraftBullet(EntityType<? extends AircraftBullet> type,Level level){super(type,level);}
    public AircraftBullet(ServerLevel level,ServerPlayer pilot){super(JugcraftAircraft.BULLET,pilot,level,new ItemStack(Items.GUNPOWDER));}
    @Override protected Item getDefaultItem(){return Items.GUNPOWDER;}
    @Override protected double getDefaultGravity(){return .005;}
    @Override protected boolean canHitEntity(Entity e){return super.canHitEntity(e)&&(getOwner()==null||e.getRootVehicle()!=getOwner().getRootVehicle());}
    @Override public void tick(){
        if(start==null)start=position();super.tick();
        if(level() instanceof ServerLevel l){
            if(++age>80||position().distanceToSqr(start)>250*250){discard();return;}
            l.sendParticles(ParticleTypes.CRIT,getX(),getY(),getZ(),1,0,0,0,0);
        }
    }
    @Override protected void onHit(HitResult hit){
        super.onHit(hit);
        if(level() instanceof ServerLevel l){
            if(hit instanceof EntityHitResult e&&e.getEntity() instanceof LivingEntity target&&getOwner() instanceof ServerPlayer pilot
                &&ConcordanceEffects.mayHarm(pilot,target)&&Authority.mayStrike(pilot,target))target.hurtServer(l,l.damageSources().thrown(this,pilot),5);
            discard();
        }
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("flight_age",age);if(start!=null){out.putDouble("launch_x",start.x);out.putDouble("launch_y",start.y);out.putDouble("launch_z",start.z);}}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);age=in.getIntOr("flight_age",0);start=new Vec3(in.getDoubleOr("launch_x",getX()),in.getDoubleOr("launch_y",getY()),in.getDoubleOr("launch_z",getZ()));}
}

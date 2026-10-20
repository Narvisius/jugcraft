package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.Set;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Visible cave ammunition with collision, finite lifetimes and conserved thrown equipment. */
public final class CavernProjectile extends ThrowableItemProjectile {
    public static EntityType<CavernProjectile> TYPE;
    private String kind="ink_bomb";
    private boolean recover,settled;
    private int bounces,life;
    private double speed;
    public CavernProjectile(EntityType<? extends CavernProjectile> t,Level l){super(t,l);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("cavern_projectile"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<CavernProjectile>of(CavernProjectile::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(8).updateInterval(1).build(key));}
    static boolean launch(ServerPlayer p,ItemStack item,String kind,boolean recover){
        var bolt=TYPE.create(p.level(),EntitySpawnReason.TRIGGERED);if(bolt==null)return false;
        bolt.setOwner(p);bolt.setItem(item.copyWithCount(1));bolt.kind=kind;bolt.recover=recover&&!p.isCreative();bolt.speed=kind.endsWith("spear")?1.8:kind.equals("shot_gum")?1.6:1.25;
        bolt.setPos(p.getX(),p.getEyeY()-.1,p.getZ());bolt.shootFromRotation(p,p.getXRot(),p.getYRot(),0,(float)bolt.speed,.5F);return p.level().addFreshEntity(bolt);
    }
    @Override protected Item getDefaultItem(){return Items.SNOWBALL;}
    @Override protected double getDefaultGravity(){return Set.of("sea_staff","sugar_staff","shot_gum").contains(kind)?0:.035;}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel l)||isRemoved())return;
        if(++life>200){finish(l);return;}
        if(kind.equals("sea_staff")&&isInWater()&&getDeltaMovement().lengthSqr()<speed*speed)setDeltaMovement(getDeltaMovement().normalize().scale(speed));
        if(life%3==0)l.sendParticles(kind.equals("sea_staff")?ParticleTypes.BUBBLE:kind.equals("sugar_staff")?NaturalCollections.particle("cavern_purple_witch_magic",ParticleTypes.WITCH):ParticleTypes.CRIT,getX(),getY(),getZ(),1,0,0,0,0);
    }
    @Override protected void onHit(HitResult hit){
        if(!(level() instanceof ServerLevel l)||settled||isRemoved())return;
        if(!(getOwner() instanceof ServerPlayer p)||!Authority.mayChange(l,p,BlockPos.containing(hit.getLocation()))){finish(l);return;}
        if(kind.equals("shot_gum")&&hit instanceof BlockHitResult block&&bounces++<3){
            Vec3 normal=new Vec3(block.getDirection().getStepX(),block.getDirection().getStepY(),block.getDirection().getStepZ()),v=getDeltaMovement();setPos(hit.getLocation().add(normal.scale(.05)));setDeltaMovement(v.subtract(normal.scale(2*v.dot(normal))).scale(.8));return;
        }
        if(hit instanceof EntityHitResult e&&e.getEntity() instanceof LivingEntity living)strike(l,p,living);
        if(Set.of("ink_bomb","glow_ink_bomb","depth_charge","purple_soda_bottle_rocket","frostmint_spear","extinction_spear").contains(kind)){
            for(var target:l.getEntitiesOfClass(LivingEntity.class,new AABB(hit.getLocation(),hit.getLocation()).inflate(3),LivingEntity::isAlive).stream().limit(16).toList())if(!(hit instanceof EntityHitResult direct&&direct.getEntity()==target))strike(l,p,target);
            l.sendParticles(kind.contains("ink")?ParticleTypes.SQUID_INK:NaturalCollections.particle(kind.equals("frostmint_spear")?"cavern_frostmint_explosion":"cavern_mine_explosion",ParticleTypes.POOF),getX(),getY(),getZ(),16,.8,.6,.8,.02);
        }
        finish(l);
    }
    private void strike(ServerLevel l,ServerPlayer p,LivingEntity target){
        if(!TwoHanded.allowed(p,l,target))return;
        var clip=l.clip(new net.minecraft.world.level.ClipContext(position(),target.getEyePosition(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this));if(clip.getType()!=HitResult.Type.MISS)return;
        if(kind.equals("ink_bomb"))target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,100,0),p);
        else if(kind.equals("glow_ink_bomb"))target.addEffect(new MobEffectInstance(MobEffects.GLOWING,200,0),p);
        else {target.hurtServer(l,p.damageSources().playerAttack(p),kind.equals("extinction_spear")?12:kind.endsWith("spear")?9:6);if(kind.equals("frostmint_spear")||kind.equals("sea_staff"))target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,80,1),p);}
    }
    private void finish(ServerLevel l){
        if(settled||isRemoved())return;settled=true;
        if(recover){var drop=new ItemEntity(l,getX(),getY(),getZ(),getItem().copy());drop.setUnlimitedLifetime();drop.setDefaultPickUpDelay();if(getOwner()!=null)drop.setTarget(getOwner().getUUID());l.addFreshEntity(drop);}discard();
    }
    @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.putString("CaveKind",kind);o.putBoolean("Recover",recover);o.putInt("Bounces",bounces);o.putInt("Life",life);o.putDouble("Speed",speed);}
    @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);kind=i.getStringOr("CaveKind","ink_bomb");recover=i.getBooleanOr("Recover",false);bounces=Math.clamp(i.getIntOr("Bounces",0),0,3);life=Math.clamp(i.getIntOr("Life",0),0,200);speed=Math.clamp(i.getDoubleOr("Speed",1.25),0,2);}
}

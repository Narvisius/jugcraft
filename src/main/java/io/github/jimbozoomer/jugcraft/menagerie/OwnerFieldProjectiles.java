package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.ClipContext;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
/** Paid block projectiles and original normal/blue cluster bomb behavior, using claim-safe bounded impacts. */
public final class OwnerFieldProjectiles extends ThrowableItemProjectile {
    public static EntityType<OwnerFieldProjectiles> TYPE;
    private int age,arming;private boolean baby,superCharged,recover=true;private Direction face=Direction.UP;
    public OwnerFieldProjectiles(EntityType<? extends OwnerFieldProjectiles> type,Level l){super(type,l);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("supps_field_projectile"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<OwnerFieldProjectiles>of(OwnerFieldProjectiles::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(8).updateInterval(1).build(key));}
    static ItemStack display(ItemStack ammo,boolean baby){var copy=ammo.copyWithCount(1);copy.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,new net.minecraft.world.item.component.CustomModelData(java.util.List.of(baby?2F:1F),java.util.List.of(),java.util.List.of(),java.util.List.of()));return copy;}
    @Override protected Item getDefaultItem(){return Items.COBBLESTONE;}
    boolean bomb(){return getItem().getItem() instanceof OwnerFieldTools.Bomb;}
    boolean blue(){return getItem().is(OwnerSupplementaries.ITEMS.get("bomb_blue"));}
    @Override protected double getDefaultGravity(){return .04;}
    @Override public void tick(){
        if(!level().isClientSide()){
            if(++age>300){if(bomb())explode();else drop();return;}
            var end=position().add(getDeltaMovement());for(int i=0;i<=8;i++){var p=BlockPos.containing(position().lerp(end,i/8D));if(!level().hasChunkAt(p)||!level().getWorldBorder().isWithinBounds(p)){discard();return;}}
            if(bomb()&&isInWater()&&!blue()){drop();return;}if(arming>0&&--arming==0){explode();return;}
        }super.tick();if(bomb()&&level() instanceof ServerLevel server&&age%3==0)server.sendParticles(ParticleTypes.SMOKE,getX(),getY()+.15,getZ(),1,0,0,0,.01);
    }
    private boolean confetti(){
        if(!(getItem().getItem() instanceof OwnerConfetti)||!(level() instanceof ServerLevel server))return false;
        OwnerConfetti.burst(server,new OwnerConfetti.Burst(position(),getDeltaMovement().normalize().scale(.6),OwnerConfetti.colors(getItem()),96,true));discard();return true;
    }
    void drop(){if(confetti())return;if(level() instanceof ServerLevel l&&recover&&!getItem().isEmpty()){var copy=getItem().copyWithCount(1);if(bomb())copy.remove(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);var dropped=new ItemEntity(l,getX(),getY(),getZ(),copy);if(getOwner()!=null)dropped.setThrower(getOwner());dropped.setDefaultPickUpDelay();l.addFreshEntity(dropped);}discard();}
    @Override protected void onHitEntity(EntityHitResult hit){
        if(!(level() instanceof ServerLevel server))return;if(confetti())return;if(bomb()){if(hit.getEntity() instanceof LargeFireball fireball&&getOwner() instanceof ServerPlayer actor&&Authority.mayChange(server,actor,fireball.blockPosition())){superCharged=true;fireball.discard();}if(baby&&age<10){arming=10-age;setDeltaMovement(Vec3.ZERO);}else explode();return;}
        if(getOwner() instanceof ServerPlayer actor&&hit.getEntity() instanceof LivingEntity target&&ConcordanceEffects.mayHarm(actor,target)&&TwoHanded.allowed(actor,server,target))target.hurtServer(server,damageSources().thrown(this,actor),(float)Math.clamp(getDeltaMovement().lengthSqr()*1.5,1,8));drop();
    }
    @Override protected void onHitBlock(BlockHitResult hit){
        if(!(level() instanceof ServerLevel server))return;if(confetti())return;face=hit.getDirection();
        if(bomb()){if(server.getBlockState(hit.getBlockPos()).is(Blocks.SLIME_BLOCK)&&face==Direction.UP){setDeltaMovement(getDeltaMovement().multiply(.8,-.8,.8));return;}if(baby&&age<10){arming=10-age;setDeltaMovement(Vec3.ZERO);}else explode();return;}
        if(getOwner() instanceof ServerPlayer actor&&getItem().getItem() instanceof DispensibleContainerItem bucket&&!actor.isSpectator()){
            var target=hit.getBlockPos().relative(face);boolean allowed=true;for(var at:BlockPos.betweenClosed(target.offset(-1,-1,-1),target.offset(1,1,1)))if(!server.hasChunkAt(at)||!server.isInWorldBounds(at)||!server.getWorldBorder().isWithinBounds(at)||!Authority.mayChange(server,actor,at)){allowed=false;break;}
            if(allowed&&bucket.emptyContents(actor,server,target,hit)){bucket.checkExtraContent(actor,server,getItem(),target);setItem(new ItemStack(Items.BUCKET));drop();return;}
        }
        if(getOwner() instanceof ServerPlayer actor&&getItem().getItem() instanceof BlockItem block&&!actor.isSpectator()){
            var target=hit.getBlockPos().relative(face);boolean allowed=true;
            // Covers the second half of doors/beds and any adjacent placement changes.
            for(var at:BlockPos.betweenClosed(target.offset(-1,-1,-1),target.offset(1,1,1)))if(!server.hasChunkAt(at)||!server.isInWorldBounds(at)||!server.getWorldBorder().isWithinBounds(at)||!Authority.mayChange(server,actor,at)){allowed=false;break;}
            if(allowed){var copy=getItem().copyWithCount(1);var result=block.place(new BlockPlaceContext(server,actor,net.minecraft.world.InteractionHand.MAIN_HAND,copy,hit));if(result.consumesAction()){discard();return;}}
        }drop();
    }
    void explode(){
        if(!(level() instanceof ServerLevel server)||isRemoved())return;
        if(!(getOwner() instanceof ServerPlayer actor)||!actor.isAlive()||!Authority.mayChange(server,actor,blockPosition())){discard();return;}
        double radius=baby?1.2:superCharged&&!blue()?6:blue()?3:2;var center=position().add(0,.15,0);
        // No native explosion flood-fill or chain ignition can cross protected land.
        for(var target:server.getEntitiesOfClass(LivingEntity.class,new AABB(center.subtract(radius*2,radius*2,radius*2),center.add(radius*2,radius*2,radius*2)))){
            double distance=target.position().distanceTo(center);if(distance>radius*2||!ConcordanceEffects.mayHarm(actor,target)||!TwoHanded.allowed(actor,server,target))continue;
            var ray=server.clip(new ClipContext(center,target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));if(ray.getType()!=HitResult.Type.MISS)continue;
            float damage=(float)((1-distance/(radius*2))*(baby?6:blue()?14:10));if(target.hurtServer(server,damageSources().thrown(this,actor),Math.max(1,damage))){target.push(target.position().subtract(center).normalize().scale(.6));if(blue())target.igniteForSeconds(baby?5:4);}
        }
        int extent=(int)Math.ceil(radius);for(var p:BlockPos.betweenClosed(blockPosition().offset(-extent,-extent,-extent),blockPosition().offset(extent,extent,extent))){
            if(p.distToCenterSqr(center)>radius*radius||!server.hasChunkAt(p)||!Authority.mayChange(server,actor,p))continue;var state=server.getBlockState(p);if(state.isAir()||state.hasBlockEntity()||!state.getFluidState().isEmpty()||state.getDestroySpeed(server,p)<0||state.getBlock().getExplosionResistance()>6)continue;
            server.destroyBlock(p,true,actor);
        }
        server.sendParticles(ParticleTypes.EXPLOSION,getX(),getY(),getZ(),baby?1:4,.3,.3,.3,0);server.playSound(null,blockPosition(),OwnerSupplementaries.SOUNDS.get("bomb_explode"),SoundSource.PLAYERS,baby?.7F:1.5F,baby?1.6F:1);
        if(blue()&&!baby){var n=Vec3.atLowerCornerOf(face.getUnitVec3i());var right=n.cross(Math.abs(n.y)>.5?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();var up=n.cross(right);int children=superCharged?10:5;for(int i=0;i<children;i++){double angle=(i/(double)children+random.nextDouble()*.15)*Math.PI*2;var child=new OwnerFieldProjectiles(TYPE,server);child.setOwner(actor);child.setItem(display(getItem(),true));child.baby=true;child.recover=false;child.setPos(center.add(n.scale(.3)));child.setDeltaMovement(n.scale(.45).add(right.scale(Math.cos(angle)*.35)).add(up.scale(Math.sin(angle)*.35)));if(server.noCollision(child))server.addFreshEntity(child);}}
        discard();
    }
    public static OwnerFieldProjectiles make(Level level,ItemStack ammo,LivingEntity owner){var p=new OwnerFieldProjectiles(TYPE,level);p.setItem(ammo.copyWithCount(1));p.setOwner(owner);if(p.bomb())p.setItem(display(ammo,false));p.recover=!(owner instanceof net.minecraft.world.entity.player.Player player&&player.hasInfiniteMaterials());return p;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("Age",age);out.putInt("Arming",arming);out.putBoolean("Baby",baby);out.putBoolean("SuperCharged",superCharged);out.putBoolean("Recover",recover);out.putString("HitFace",face.getName());}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);age=Math.clamp(in.getIntOr("Age",0),0,301);arming=Math.clamp(in.getIntOr("Arming",0),0,10);baby=in.getBooleanOr("Baby",false);superCharged=in.getBooleanOr("SuperCharged",false);recover=in.getBooleanOr("Recover",true);var d=Direction.byName(in.getStringOr("HitFace","up"));face=d==null?Direction.UP:d;}
}

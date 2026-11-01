package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Server-owned aiming, fuse and paid shot handoff. Unattended firing uses the placed owner's permissions. */
public final class OwnerCannon {
    public static EntityType<OwnerCannonPearl> PEARL;public static EntityType<OwnerCannonball> BALL;public static EntityType<OwnerCannonSeat> SEAT;
    private static final java.util.Map<java.util.UUID,Integer> FLIGHTS=new java.util.HashMap<>();
    public static final int FUSE=40,COOLDOWN=60;
    private OwnerCannon(){}
    static void register(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("supps_cannonball"));
        BALL=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<OwnerCannonball>of(OwnerCannonball::new,MobCategory.MISC).sized(.5F,.5F).clientTrackingRange(10).updateInterval(1).build(key));
        var seatKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("supps_cannon_seat"));SEAT=Registry.register(BuiltInRegistries.ENTITY_TYPE,seatKey,EntityType.Builder.<OwnerCannonSeat>of(OwnerCannonSeat::new,MobCategory.MISC).sized(.1F,.1F).clientTrackingRange(8).updateInterval(1).build(seatKey));
        var pearlKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("supps_cannon_pearl"));PEARL=Registry.register(BuiltInRegistries.ENTITY_TYPE,pearlKey,EntityType.Builder.<OwnerCannonPearl>of(OwnerCannonPearl::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(8).updateInterval(1).build(pearlKey));
        OwnerCannonMenu.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->FLIGHTS.entrySet().removeIf(entry->{var player=server.getPlayerList().getPlayer(entry.getKey());if(player==null||!player.isAlive()||entry.getValue()<=0||player.onGround()&&entry.getValue()<296||player.isInWater()||player.isPassenger())return true;player.resetFallDistance();entry.setValue(entry.getValue()-1);return false;}));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->FLIGHTS.clear());
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((target,source,amount)->{
            if(source.is(net.minecraft.world.damagesource.DamageTypes.FALL)&&FLIGHTS.containsKey(target.getUUID()))return false;
            var direct=source.getDirectEntity();if(direct==null||!direct.entityTags().contains("JugcraftHouseholdCannon"))return true;
            return source.getEntity() instanceof ServerPlayer actor&&target.level() instanceof ServerLevel server&&io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects.mayHarm(actor,target)&&io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(actor,server,target);
        });
    }
    public static Vec3 direction(float yaw,float pitch){return Vec3.directionFromRotation(pitch,yaw);}
    public static float velocity(int power){return (float)(.9*Math.pow(Math.clamp(power,1,4),.6));}
    public static Direction mount(net.minecraft.world.level.block.state.BlockState state){var direction=Direction.byName(HouseholdBlock.value(state,"facing"));return direction==null?Direction.UP:direction;}
    public static Vec3 mounted(Vec3 v,Direction mount,boolean inverse){return switch(mount){case UP->v;case DOWN->new Vec3(v.x,-v.y,-v.z);case NORTH->inverse?new Vec3(v.x,-v.z,v.y):new Vec3(v.x,v.z,-v.y);case SOUTH->inverse?new Vec3(v.x,v.z,-v.y):new Vec3(v.x,-v.z,v.y);case EAST->inverse?new Vec3(-v.y,v.x,v.z):new Vec3(v.y,-v.x,v.z);case WEST->inverse?new Vec3(v.y,-v.x,v.z):new Vec3(-v.y,v.x,v.z);};}
    public static float yaw(Vec3 v){return (float)Math.toDegrees(Math.atan2(-v.x,v.z));}
    public static float pitch(Vec3 v){return (float)-Math.toDegrees(Math.atan2(v.y,v.horizontalDistance()));}
    public static Vec3 mouth(OwnerSupplementData data){return Vec3.atCenterOf(data.getBlockPos()).add(Vec3.atLowerCornerOf(mount(data.getBlockState()).getUnitVec3i()).scale(.0625)).add(direction(data.cannonYaw,data.cannonPitch).scale(.7));}
    static boolean ammunition(ItemStack item){return !item.isEmpty()&&!item.is(net.minecraft.tags.TagKey.create(Registries.ITEM,Jugcraft.id("supps_cannon_blacklist")));}
    public static boolean loaded(OwnerSupplementData d){return d.getItem(0).is(Items.GUNPOWDER)&&d.getItem(0).getCount()>=d.cannonPower&&(ammunition(d.getItem(1))||rider(d)!=null);}
    public static boolean ignite(OwnerSupplementData d,ServerPlayer actor){
        if(!(d.getLevel() instanceof ServerLevel level)||!d.mayEdit(actor)||d.cannonFuse>0||d.cannonCooldown>0||!loaded(d)||HouseholdBlock.flag(d.getBlockState(),"waterlogged"))return false;
        d.cannonActor=actor.getUUID();d.cannonFuse=FUSE;d.changed();level.playSound(null,d.getBlockPos(),SoundEvents.TNT_PRIMED,SoundSource.BLOCKS,.6F,1.3F);return true;
    }
    public static boolean aim(OwnerSupplementData d,Player player){
        if(!d.mayEdit(player)||d.cannonFuse>0)return false;
        var mount=mount(d.getBlockState());var local=mounted(player.getLookAngle(),mount,true);var world=mounted(direction(yaw(local),Math.clamp(pitch(local),-85,40)),mount,false);d.cannonYaw=yaw(world);d.cannonPitch=pitch(world);d.changed();return true;
    }
    static InteractionResult use(OwnerSupplementData d,Player player,ItemStack held,InteractionHand hand){
        if(!(player instanceof ServerPlayer actor))return InteractionResult.SUCCESS;
        if(held.is(Items.FLINT_AND_STEEL)||held.is(Items.FIRE_CHARGE)){
            if(!ignite(d,actor))return InteractionResult.FAIL;
            if(held.is(Items.FLINT_AND_STEEL))held.hurtAndBreak(1,actor,hand.asEquipmentSlot());else held.consume(1,actor);return InteractionResult.SUCCESS;
        }
        if(held.getItem() instanceof OwnerSupplementWrench){if(!aim(d,actor))return InteractionResult.FAIL;held.hurtAndBreak(1,actor,hand.asEquipmentSlot());return InteractionResult.SUCCESS;}
        actor.openMenu(d);return InteractionResult.SUCCESS;
    }
    static void tick(ServerLevel level,OwnerSupplementData d){
        boolean signal=level.hasNeighborSignal(d.getBlockPos());
        if(signal&&!d.cannonPowered){var actor=Authority.present(level,d.owner);if(actor!=null)ignite(d,actor);}if(signal!=d.cannonPowered){d.cannonPowered=signal;d.setChanged();}
        if(d.cannonCooldown>0){d.cannonCooldown--;if(d.cannonCooldown==0)d.changed();else d.setChanged();}
        if(d.cannonFuse<=0)return;
        if(HouseholdBlock.flag(d.getBlockState(),"waterlogged")){d.cannonFuse=0;d.changed();return;}
        if(d.cannonFuse%3==0)level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,d.getBlockPos().getX()+.5,d.getBlockPos().getY()+.8,d.getBlockPos().getZ()+.5,1,.01,.01,.01,.01);
        d.cannonFuse--;d.setChanged();if(d.cannonFuse>0)return;
        var actor=Authority.present(level,d.cannonActor);
        if(actor==null||!d.mayEdit(actor)||!loaded(d)){d.changed();return;}
        var start=mouth(d);var pos=BlockPos.containing(start);
        if(!level.hasChunkAt(pos)||!Authority.mayChange(level,actor,pos)||!level.noCollision(new AABB(start.subtract(.25,.25,.25),start.add(.25,.25,.25)))){d.changed();return;}
        var rider=rider(d);if(rider!=null){
            var launch=start.add(direction(d.cannonYaw,d.cannonPitch).scale(.7));
            if(!level.noCollision(rider,rider.getBoundingBox().move(launch.subtract(rider.position())))){d.changed();return;}
            rider.stopRiding();rider.teleportTo(launch.x,launch.y,launch.z);rider.setDeltaMovement(direction(d.cannonYaw,d.cannonPitch).scale(velocity(d.cannonPower)));rider.resetFallDistance();rider.setOnGround(false);rider.tryToStartFallFlying();rider.needsSync=true;
            rider.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(rider));FLIGHTS.put(rider.getUUID(),300);d.cannonRider=null;d.getItem(0).shrink(d.cannonPower);d.cannonCooldown=COOLDOWN;d.changed();
            level.playSound(null,d.getBlockPos(),OwnerSupplementaries.SOUNDS.get("cannon_fire"),SoundSource.BLOCKS,3,1);return;
        }
        var ammo=d.getItem(1).copyWithCount(1);var facing=direction(d.cannonYaw,d.cannonPitch);Entity shot;
        shot=projectile(level,actor,ammo,start,facing,velocity(d.cannonPower));
        if(shot instanceof OwnerCannonPearl pearl)pearl.source(d);
        if(shot==null||!level.addFreshEntity(shot)){d.changed();return;}
        // Commit inventory consumption only after the actual projectile insertion succeeded.
        d.getItem(0).shrink(d.cannonPower);d.getItem(1).shrink(1);d.cannonCooldown=COOLDOWN;d.changed();
        level.playSound(null,d.getBlockPos(),OwnerSupplementaries.SOUNDS.get("cannon_fire"),SoundSource.BLOCKS,3,.9F+level.getRandom().nextFloat()*.2F);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,start.x,start.y,start.z,12,.15,.15,.15,.09);
    }
    public static ServerPlayer rider(OwnerSupplementData d){if(d.getLevel() instanceof ServerLevel level&&d.cannonRider!=null&&level.getEntity(d.cannonRider) instanceof ServerPlayer player&&player.getVehicle() instanceof OwnerCannonSeat seat&&seat.anchor.equals(d.getBlockPos()))return player;return null;}
    public static boolean board(OwnerSupplementData d,ServerPlayer player){
        if(!d.mayEdit(player)||player.isPassenger()||rider(d)!=null||!d.getItem(1).isEmpty()||d.cannonFuse>0)return false;
        var level=player.level();if(!level.getEntitiesOfClass(OwnerCannonSeat.class,new AABB(d.getBlockPos())).isEmpty())return false;var seat=new OwnerCannonSeat(SEAT,level);seat.anchor(d.getBlockPos());seat.setPos(Vec3.atCenterOf(d.getBlockPos()).add(Vec3.atLowerCornerOf(mount(d.getBlockState()).getUnitVec3i()).scale(.25)));seat.setYRot(d.cannonYaw);
        if(!level.addFreshEntity(seat))return false;if(!player.startRiding(seat,true,true)){seat.discard();return false;}d.cannonRider=player.getUUID();d.changed();return true;
    }
    static Entity projectile(ServerLevel level,ServerPlayer actor,ItemStack ammo,Vec3 start,Vec3 facing,float speed){
        Entity shot;
        if(ammo.is(OwnerSupplementaries.ITEMS.get("cannonball"))){var ball=new OwnerCannonball(BALL,level);ball.setItem(ammo);shot=ball;}
        else if(ammo.getItem() instanceof OwnerFieldTools.Bomb||ammo.getItem() instanceof OwnerConfetti){shot=OwnerFieldProjectiles.make(level,ammo,actor);}
        else if(ammo.is(Items.FIRE_CHARGE)||ammo.is(OwnerAmendmentCharge.DRAGON)){var charge=new OwnerAmendmentCharge(OwnerAmendmentCharge.TYPE,level);charge.setItem(ammo);shot=charge;}
        else if(ammo.is(Items.ENDER_PEARL))shot=new OwnerCannonPearl(PEARL,level);
        else if(ammo.is(Items.FIREWORK_ROCKET))shot=new net.minecraft.world.entity.projectile.FireworkRocketEntity(level,ammo,actor,start.x,start.y,start.z,true);
        else if(ammo.is(Items.TNT))shot=new net.minecraft.world.entity.item.PrimedTnt(level,start.x,start.y,start.z,actor);
        else if(ammo.getItem() instanceof SpawnEggItem){
            var type=SpawnEggItem.getType(ammo);if(type==null||((SpawnEggItem)ammo.getItem()).shouldPrintOpWarning(ammo,actor))return null;
            shot=type.create(level,EntitySpawnReason.DISPENSER);if(shot==null)return null;
            // Vanilla's item component application retains names and authored entity data, with native privilege checks.
            EntityType.createDefaultStackConfig(level,ammo,actor).apply(shot);
        }
        else if(ammo.is(Items.ARMOR_STAND)){shot=net.minecraft.world.entity.EntityTypes.ARMOR_STAND.create(level,EntitySpawnReason.DISPENSER);if(shot==null)return null;EntityType.createDefaultStackConfig(level,ammo,actor).apply(shot);}
        else if(ammo.getItem() instanceof ProjectileItem item){shot=item.asProjectile(level,start,ammo,Direction.getApproximateNearest(facing));}
        else {var cargo=new ItemEntity(level,start.x,start.y,start.z,ammo);cargo.setPickUpDelay(20);cargo.setThrower(actor);shot=cargo;}
        if(shot instanceof net.minecraft.world.entity.projectile.Projectile projectile)projectile.setOwner(actor);
        shot.entityTags().add("JugcraftHouseholdCannon");shot.setPos(start);shot.setDeltaMovement(facing.scale(speed));
        if(!level.noCollision(shot)||!level.getWorldBorder().isWithinBounds(shot.getBoundingBox()))return null;
        return shot;
    }

}

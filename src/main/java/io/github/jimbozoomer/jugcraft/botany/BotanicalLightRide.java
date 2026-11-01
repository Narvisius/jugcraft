package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** A real ride entity keeps passenger synchronization native, including logout and dismount. */
public final class BotanicalLightRide extends Entity {
    public static EntityType<BotanicalLightRide> TYPE;
    private BlockPos target=BlockPos.ZERO,source=BlockPos.ZERO;private UUID actor;private int hops;private final Set<BlockPos> visited=new HashSet<>();
    public BotanicalLightRide(EntityType<?> type,Level level){super(type,level);setNoGravity(true);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("botanical_light_ride"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<BotanicalLightRide>of(BotanicalLightRide::new,MobCategory.MISC).sized(.2F,.2F).clientTrackingRange(10).updateInterval(1).build(key));}
    static boolean begin(Entity rider,ServerPlayer p,BlockPos start,BlockPos target){
        var ride=new BotanicalLightRide(TYPE,p.level());ride.actor=p.getUUID();ride.source=start.immutable();ride.target=target.immutable();ride.visited.add(start);ride.setPos(rider.position());
        if(!p.level().addFreshEntity(ride))return false;if(!rider.startRiding(ride,true,true)){ride.discard();return false;}p.level().playSound(null,start,BotanicalSounds.get("light_relay"),net.minecraft.sounds.SoundSource.BLOCKS,.5F,1);return true;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
    @Override protected boolean canAddPassenger(Entity e){return getPassengers().isEmpty()&&(e instanceof LivingEntity||e instanceof ItemEntity);}
    @Override protected Vec3 getPassengerAttachmentPoint(Entity e,EntityDimensions dimensions,float scale){return Vec3.ZERO;}
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount){return false;}
    private void finish(){for(var rider:List.copyOf(getPassengers())){rider.fallDistance=0;if(rider instanceof ItemEntity item)item.setPickUpDelay(20);rider.stopRiding();}discard();}
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity rider){return position();}
    @Override public void tick(){
        super.tick();if(level().isClientSide()){for(int i=0;i<4;i++){double a=tickCount*.2+i*Math.PI/2;level().addParticle(new DustParticleOptions(net.minecraft.util.Mth.hsvToRgb((tickCount%72)/72F,1,1),.8F),getX()+Math.cos(a)*.35,getY()+.2,getZ()+Math.sin(a)*.35,0,0,0);}return;}
        var level=(ServerLevel)level();var p=Authority.answering(level,actor);var rider=getFirstPassenger();
        if(source.distSqr(target)>400||p==null||rider==null||!rider.isAlive()||tickCount>6000||hops>=128||!level.hasChunkAt(target)||!level.hasChunkAt(source)||!Authority.mayChange(level,p,source)||!Authority.mayChange(level,p,target)||!io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(level,source,target)){finish();return;}
        var end=Vec3.atCenterOf(target);var delta=end.subtract(position());if(delta.length()<.1){
            if(!(level.getBlockEntity(target) instanceof BotanicalWorkshop node)||!BotanicalLuminizer.TYPES.contains(node.kind())||!visited.add(target)){finish();return;}
            node.luminizer.arrived(p);var next=node.luminizer.next(p);if(next==null){finish();return;}source=target;target=next;hops++;end=Vec3.atCenterOf(target);delta=end.subtract(position());
        }
        var motion=delta.normalize().scale(Math.min(.5,delta.length()));var next=position().add(motion);
        for(int i=1;i<=2;i++){var point=position().lerp(next,i/2D);var at=BlockPos.containing(point);var box=rider.getBoundingBox().move(point.subtract(rider.position()));
            if(!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at)||!Authority.mayChange(level,p,at)||!level.noCollision(rider,box)){finish();return;}}
        setDeltaMovement(motion);setPos(next);rider.fallDistance=0;
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){out.store("target",BlockPos.CODEC,target);out.store("source",BlockPos.CODEC,source);if(actor!=null)out.store("actor",UUIDUtil.CODEC,actor);out.putInt("hops",hops);out.putInt("age",tickCount);out.store("visited",BlockPos.CODEC.listOf(),List.copyOf(visited));}
    @Override protected void readAdditionalSaveData(ValueInput in){target=in.read("target",BlockPos.CODEC).orElse(blockPosition());source=in.read("source",BlockPos.CODEC).orElse(blockPosition());actor=in.read("actor",UUIDUtil.CODEC).orElse(null);hops=Math.clamp(in.getIntOr("hops",128),0,128);tickCount=Math.clamp(in.getIntOr("age",6001),0,6001);visited.clear();in.read("visited",BlockPos.CODEC.listOf()).orElse(List.of()).stream().limit(128).forEach(visited::add);}
}

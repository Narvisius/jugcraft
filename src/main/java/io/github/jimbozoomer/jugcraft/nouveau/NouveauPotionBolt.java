package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Visible, colliding bottles; lingering doses retain the owner's claims/PvP checks. */
public final class NouveauPotionBolt extends ThrowableItemProjectile {
    public static EntityType<NouveauPotionBolt> TYPE;
    private record Cloud(UUID owner,ResourceKey<Level> dimension,Vec3 at,PotionContents potion,long until){}
    private static final List<Cloud> CLOUDS=new ArrayList<>();
    public NouveauPotionBolt(EntityType<? extends NouveauPotionBolt> t,Level l){super(t,l);}
    public static void register(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("nouveau_potion_bolt"));
        TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<NouveauPotionBolt>of(NouveauPotionBolt::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(8).updateInterval(2).build(key));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->CLOUDS.clear());
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTickCount()%20!=0)return;
            var iterator=CLOUDS.iterator();while(iterator.hasNext()){
                var cloud=iterator.next();var l=server.getLevel(cloud.dimension);var p=server.getPlayerList().getPlayer(cloud.owner);
                if(!JugNouveau.enabled()||l==null||p==null||!p.isAlive()||p.level()!=l||l.getGameTime()>=cloud.until||!l.hasChunkAt(BlockPos.containing(cloud.at))){iterator.remove();continue;}
                splash(l,p,cloud.at,cloud.potion,3,.25F);l.sendParticles(ParticleTypes.WITCH,cloud.at.x,cloud.at.y+.1,cloud.at.z,16,2,.2,2,.01);
            }
        });
    }
    public static boolean launch(ServerPlayer p,ItemStack flask,boolean lingering){
        if(lingering&&(CLOUDS.size()>=64||CLOUDS.stream().filter(c->c.owner.equals(p.getUUID())).count()>=4))return false;
        var bolt=TYPE.create(p.level(),EntitySpawnReason.TRIGGERED);if(bolt==null)return false;
        var payload=new ItemStack(lingering?Items.LINGERING_POTION:Items.SPLASH_POTION);
        payload.set(DataComponents.POTION_CONTENTS,flask.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY));
        bolt.setItem(payload);bolt.setOwner(p);bolt.setPos(p.getX(),p.getEyeY()-.1,p.getZ());bolt.shootFromRotation(p,p.getXRot(),p.getYRot(),-10,1.4F,.5F);
        return p.level().addFreshEntity(bolt);
    }
    @Override protected Item getDefaultItem(){return Items.SPLASH_POTION;}
    @Override public void tick(){super.tick();if(!level().isClientSide()&&tickCount>200)discard();}
    @Override protected void onHit(HitResult hit){
        if(!(level() instanceof ServerLevel l))return;
        if(getOwner() instanceof ServerPlayer p&&p.isAlive()&&JugNouveau.enabled()&&Authority.mayChange(l,p,BlockPos.containing(hit.getLocation()))){
            var potion=getItem().getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
            if(getItem().is(Items.LINGERING_POTION)&&CLOUDS.size()<64)CLOUDS.add(new Cloud(p.getUUID(),l.dimension(),hit.getLocation(),potion,l.getGameTime()+200));
            else splash(l,p,hit.getLocation(),potion,3,1);
            l.levelEvent(2002,BlockPos.containing(hit.getLocation()),potion.getColor());
        }discard();
    }
    private static void splash(ServerLevel l,ServerPlayer p,Vec3 at,PotionContents potion,double radius,float scale){
        for(var target:l.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(radius),e->e.isAlive()&&e.distanceToSqr(at)<=radius*radius).stream().limit(16).toList()){
            var clip=l.clip(new net.minecraft.world.level.ClipContext(at.add(0,.1,0),target.getEyePosition(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
            if(clip.getType()==HitResult.Type.MISS)NouveauPotions.apply(p,potion,target,scale);
        }
    }
}

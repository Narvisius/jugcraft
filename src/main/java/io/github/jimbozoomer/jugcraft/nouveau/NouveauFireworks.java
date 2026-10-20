package io.github.jimbozoomer.jugcraft.nouveau;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.phys.*;

/** Native rockets and star visuals; spell damage retains the paid cast's shared authority and ledger. */
public final class NouveauFireworks {
    private static AttachmentType<Boolean> SPELL;
    private record Shot(ConcordanceEffects.Context context,EffectSpec effect){}
    private static final Map<FireworkRocketEntity,Shot> SHOTS=new WeakHashMap<>();
    private NouveauFireworks(){}
    public static void register(){
        SPELL=AttachmentRegistry.<Boolean>builder().persistent(Codec.BOOL).buildAndRegister(Jugcraft.id("nouveau_spell_firework"));
        ServerTickEvents.END_SERVER_TICK.register(server->SHOTS.keySet().removeIf(e->e.isRemoved()||e.tickCount>240));
        ServerLifecycleEvents.SERVER_STOPPED.register(server->SHOTS.clear());
    }
    public static boolean create(ConcordanceEffects.Context c,EffectSpec e,ServerPlayer p,Vec3 point){
        if(SHOTS.size()>=128||!Authority.mayChange(c.level(),p,BlockPos.containing(point)))return false;
        ItemStack pattern=ItemStack.EMPTY;
        for(var ref:NouveauDevices.supplies(c,p))if(ref.stack().is(Items.FIREWORK_ROCKET)){pattern=ref.stack();break;}
        var original=pattern.getOrDefault(DataComponents.FIREWORKS,new Fireworks(1,List.of()));
        var stars=new ArrayList<>(original.explosions().stream().limit(8).toList());
        var color=new FireworkExplosion(FireworkExplosion.Shape.SMALL_BALL,it.unimi.dsi.fastutil.ints.IntList.of(NouveauControls.color(p.getMainHandItem())),it.unimi.dsi.fastutil.ints.IntList.of(),true,true);
        int amount=Math.clamp(e.magnitude(),1,8);while(stars.size()<amount)stars.add(stars.isEmpty()?color:stars.getFirst());
        int count=Math.clamp(1+e.augment("split"),1,5);boolean added=false;
        for(int i=0;i<count&&SHOTS.size()<128;i++){
            if(!c.ledger().admit(c.step()+"/launch","firework:"+i+":"+BlockPos.containing(point).asLong(),1).admitted())break;
            var stack=new ItemStack(Items.FIREWORK_ROCKET);stack.set(DataComponents.FIREWORKS,new Fireworks(Math.clamp(e.duration()/200,1,3),stars));
            var rocket=new FireworkRocketEntity(c.level(),p,point.x,point.y+.15,point.z,stack);rocket.setAttached(SPELL,true);
            rocket.setDeltaMovement((i-(count-1)/2.0)*.07,.12,0);
            if(c.level().addFreshEntity(rocket)){SHOTS.put(rocket,new Shot(c,e));added=true;}
        }return added;
    }
    /** Also cancels default damage after a save/reload, when an ephemeral spell ledger no longer exists. */
    public static boolean explode(FireworkRocketEntity rocket){
        if(SPELL==null||!rocket.getAttachedOrElse(SPELL,false))return false;
        Shot shot=SHOTS.remove(rocket);if(shot==null||!JugNouveau.enabled())return true;
        var c=shot.context();var p=ConcordanceEffects.person(c);if(p==null||!p.isAlive()||p.level()!=c.level())return true;
        var at=rocket.position();var e=shot.effect();int power=Math.clamp(e.magnitude()*2,1,16);
        if(e.augment("randomize")>0)power=1+c.level().getRandom().nextInt(power);
        for(var target:c.level().getEntitiesOfClass(LivingEntity.class,AABB.ofSize(at,8,8,8),LivingEntity::isAlive).stream().sorted(Comparator.comparingDouble(t->t.distanceToSqr(at))).limit(16).toList()){
            if(c.level().clip(new net.minecraft.world.level.ClipContext(at,target.getEyePosition(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,rocket)).getType()!=HitResult.Type.MISS)continue;
            int damage=Math.max(1,(int)(power*(1-Math.min(1,target.distanceToSqr(at)/32))));
            var impact=new ConcordanceEffects.Context(c.level(),c.cause(),p,c.ledger(),c.step()+"/rocket/"+rocket.getUUID(),at);
            ConcordanceEffects.apply(impact,EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,damage,0),target);
        }return true;
    }
}

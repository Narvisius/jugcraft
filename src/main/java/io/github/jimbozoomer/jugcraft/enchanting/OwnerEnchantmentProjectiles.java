package io.github.jimbozoomer.jugcraft.enchanting;

import java.util.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

public final class OwnerEnchantmentProjectiles {
    private OwnerEnchantmentProjectiles() {}
    public interface Flight { Vec3 jugcraft$start(); boolean jugcraft$redirected(); void jugcraft$redirect(); boolean jugcraft$salvaged(); void jugcraft$salvage(); }
    private record Mark(UUID target,int hits,int tick){}
    private static final Map<ServerPlayer,Mark> MARKS=new WeakHashMap<>();
    private static ItemStack weapon(AbstractArrow arrow){var stack=arrow.getWeaponItem();return stack==null?ItemStack.EMPTY:stack;}
    private static boolean eligible(AbstractArrow arrow,LivingEntity victim){
        return arrow instanceof Arrow&&arrow.getOwner() instanceof ServerPlayer p&&victim!=p&&!(victim instanceof Player)&&victim.isAlive()
            &&Authority.mayStrike(p.level(),p,p.getUUID(),victim);
    }
    public static float damage(AbstractArrow arrow,LivingEntity victim,float base){
        if(!eligible(arrow,victim))return base;var p=(ServerPlayer)arrow.getOwner();var stack=weapon(arrow);
        int hunt=OwnerEnchantments.level(p,stack,"hunters_mark"),longshot=OwnerEnchantments.level(p,stack,"longshot");
        var mark=MARKS.get(p);int previous=mark!=null&&mark.target.equals(victim.getUUID())&&p.tickCount-mark.tick<=100?mark.hits:0;
        double bonus=Math.min(.3,.035*hunt*previous);
        var start=((Flight)arrow).jugcraft$start();if(start!=null){double distance=start.distanceTo(arrow.position());if(distance<=192)bonus+=Math.min(.3,Math.max(0,Math.min(1,(distance-16)/32))*.1*longshot);}
        return (float)(base*(1+Math.min(.6,bonus)));
    }
    public static void hit(AbstractArrow arrow,LivingEntity victim){
        if(!eligible(arrow,victim))return;var p=(ServerPlayer)arrow.getOwner();var stack=weapon(arrow);var flight=(Flight)arrow;
        var mark=MARKS.get(p);int hits=mark!=null&&mark.target.equals(victim.getUUID())&&p.tickCount-mark.tick<=100?Math.min(4,mark.hits+1):1;MARKS.put(p,new Mark(victim.getUUID(),hits,p.tickCount));
        int frost=OwnerEnchantments.level(p,stack,"frostshot"),pin=OwnerEnchantments.level(p,stack,"pinning"),salvage=OwnerEnchantments.level(p,stack,"salvage");
        if(frost>0){victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,30+15*frost,0));victim.setTicksFrozen(Math.min(140,victim.getTicksFrozen()+20*frost));}
        if(pin>0&&arrow.getDeltaMovement().length()>=2.8&&p.getRandom().nextFloat()<.12*pin)victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,30+15*pin,1));
        // Paid ordinary arrows only. Conjured, infinite, piercing and creative arrows cannot mint ammunition.
        var ammo=arrow.getPickupItemStackOrigin();
        if(salvage>0&&!flight.jugcraft$salvaged()&&arrow.getPierceLevel()==0&&arrow.pickup==AbstractArrow.Pickup.ALLOWED&&ammo.is(Items.ARROW)
            &&!p.hasInfiniteMaterials()&&(io.github.jimbozoomer.jugcraft.nouveau.NouveauGear.CONJURED_ARROW==null||!ammo.has(io.github.jimbozoomer.jugcraft.nouveau.NouveauGear.CONJURED_ARROW))
            &&p.getRandom().nextFloat()<Math.min(.35,.08*salvage)){
            flight.jugcraft$salvage();arrow.pickup=AbstractArrow.Pickup.DISALLOWED;
            net.minecraft.world.level.block.Block.popResource(p.level(),victim.blockPosition(),ammo.copyWithCount(1));
        }
    }
    public static boolean redirect(AbstractArrow arrow,LivingEntity hit){
        if(!eligible(arrow,hit)||((Flight)arrow).jugcraft$redirected())return false;
        var p=(ServerPlayer)arrow.getOwner();int rank=OwnerEnchantments.level(p,weapon(arrow),"ricochet");
        // Spell-owned arrows keep their existing impact lifecycle.
        var ammo=arrow.getPickupItemStackOrigin();
        if(io.github.jimbozoomer.jugcraft.nouveau.NouveauGear.ARROW_SHOT!=null&&ammo.has(io.github.jimbozoomer.jugcraft.nouveau.NouveauGear.ARROW_SHOT))return false;
        if(rank==0||p.getRandom().nextFloat()>=Math.min(.65,.12*rank))return false;
        double radius=4+rank;
        var candidates=arrow.level().getEntitiesOfClass(LivingEntity.class,arrow.getBoundingBox().inflate(radius),e->e!=hit&&eligible(arrow,e)&&!e.isInvulnerable()&&e.distanceToSqr(arrow)<radius*radius);
        candidates.sort(Comparator.comparingDouble(arrow::distanceToSqr));
        for(int i=0;i<Math.min(32,candidates.size());i++){
            var target=candidates.get(i);if(!p.hasLineOfSight(target))continue;
            var direction=target.getBoundingBox().getCenter().subtract(arrow.position()).normalize();
            arrow.setPos(arrow.position().add(direction.scale(.6)));arrow.setDeltaMovement(direction.scale(Math.max(1,arrow.getDeltaMovement().length())));
            ((Flight)arrow).jugcraft$redirect();return true;
        }return false;
    }
}

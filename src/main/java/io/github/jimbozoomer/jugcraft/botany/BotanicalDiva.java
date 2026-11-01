package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.*;

/** Lethal charm interception happens before loot/XP death processing, once per creature. */
final class BotanicalDiva {
    private static final Map<Mob,Bout> BOUTS=new WeakHashMap<>();
    static void register(){
        ServerLivingEntityEvents.ALLOW_DEATH.register((victim,source,amount)->{
            if(!(victim instanceof Mob mob)||!(mob instanceof Enemy)||mob.getMaxHealth()>=200||mob.entityTags().contains("jugcraft:diva_redeemed")||!(source.getEntity() instanceof ServerPlayer p)||!BotanicalAccessories.has(p,"diva_charm")||mob.distanceToSqr(p)>400||p.getRandom().nextFloat()>=.6||!Authority.mayStrike(p,mob)||!Authority.mayChange(p.level(),p,mob.blockPosition()))return true;
            var rival=p.level().getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(20),e->e!=mob&&e instanceof Enemy&&!e.isAlliedTo(mob)&&BotanicalDefenses.eligible(p.level(),p,e)).stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            if(rival==null||!BotanicalEquipment.pay(p,250))return true;
            mob.addTag("jugcraft:diva_redeemed");mob.setHealth(mob.getMaxHealth());mob.setLastHurtByMob(null);if(mob instanceof Creeper creeper)creeper.setSwellDir(-1);
            var goal=new Bout(mob,rival,p);mob.getGoalSelector().addGoal(-1,goal);mob.setTarget(rival);BOUTS.put(mob,goal);p.level().playSound(null,mob.blockPosition(),BotanicalSounds.get("diva_charm"),net.minecraft.sounds.SoundSource.PLAYERS,1,1);return false;
        });
        ServerTickEvents.END_SERVER_TICK.register(s->{for(var entry:List.copyOf(BOUTS.entrySet()))if(entry.getValue().done||!entry.getKey().isAlive()){entry.getKey().getGoalSelector().removeGoal(entry.getValue());BOUTS.remove(entry.getKey());}});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->BOUTS.clear());
    }
    private static final class Bout extends Goal {
        final Mob mob,rival;final UUID owner;int left=600;boolean done;
        Bout(Mob mob,Mob rival,ServerPlayer p){this.mob=mob;this.rival=rival;owner=p.getUUID();setFlags(EnumSet.of(Flag.TARGET));}
        public boolean canUse(){return !done&&allowed();}public boolean canContinueToUse(){return !done&&left>0&&allowed();}
        private boolean allowed(){if(!(mob.level() instanceof ServerLevel l))return false;var p=Authority.answering(l,owner);return p!=null&&mob.distanceToSqr(rival)<=1024&&BotanicalDefenses.eligible(l,p,mob)&&BotanicalDefenses.eligible(l,p,rival);}
        public boolean requiresUpdateEveryTick(){return true;}public void tick(){left--;mob.setTarget(rival);}public void stop(){done=true;if(mob.getTarget()==rival)mob.setTarget(null);}
    }
}

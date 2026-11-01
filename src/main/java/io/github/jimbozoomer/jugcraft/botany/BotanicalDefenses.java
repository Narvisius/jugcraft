package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;

/** Flower defenses are attributed to their operator, with current claim checks at every target. */
final class BotanicalDefenses {
    static final Set<String> TYPES=Set.of("bellethorn","bellethorn_chibi","dreadthorn","hyacidus","medumone","tangleberrie","tangleberrie_chibi","jiyuulia","jiyuulia_chibi");
    static boolean eligible(ServerLevel level,ServerPlayer actor,LivingEntity target){
        return target.isAlive()&&!target.isSpectator()&&!(target instanceof Player)
            &&!(target instanceof TamableAnimal pet&&pet.isTame())
            &&Authority.mayChange(level,actor,target.blockPosition())&&Authority.mayStrike(actor,target);
    }
    static void tick(BotanicalWorkshop flower,ServerLevel level,ServerPlayer actor){
        String kind=flower.kind();boolean field=kind.startsWith("tangleberrie")||kind.startsWith("jiyuulia");
        if(flower.mana()==0)return;
        if(field){field(flower,level,actor);return;}
        boolean slow=kind.equals("medumone"),poison=kind.equals("hyacidus");
        if(!slow&&level.getGameTime()%5!=0)return;
        int cost=slow?1:poison?20:kind.equals("dreadthorn")?30:24,range=kind.endsWith("chibi")?1:6;
        int examined=0;
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(flower.effectivePos()).inflate(range))){
            if(++examined>32||flower.mana()<cost)break;
            if(!eligible(level,actor,target))continue;
            if(kind.equals("dreadthorn")&&(!(target instanceof Animal animal)||animal.isBaby()))continue;
            if(poison&&target.hasEffect(MobEffects.POISON)||!slow&&!poison&&target.getInvulnerableTime()>0)continue;
            if(!flower.draw(cost))return;
            boolean applied=slow?target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,2,100),actor)
                :poison?target.addEffect(new MobEffectInstance(MobEffects.POISON,60,0),actor)
                :target.hurtServer(level,level.damageSources().indirectMagic(actor,actor),target instanceof Witch?20:4);
            if(!applied)flower.fill(cost);
        }
    }
    private static void field(BotanicalWorkshop flower,ServerLevel level,ServerPlayer actor){
        boolean repel=flower.kind().startsWith("jiyuulia"),mini=flower.kind().endsWith("chibi");
        double radius=mini?3:repel?8:7,inside=repel?0:radius-1;
        var center=Vec3.atCenterOf(flower.effectivePos());int examined=0;boolean moved=false;
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(flower.effectivePos()).inflate(radius))){
            if(++examined>32)break;if(!eligible(level,actor,target))continue;
            var delta=center.subtract(target.position());double distance=delta.length();
            if(distance<=inside||distance>=radius||distance<.001)continue;
            double speed=Math.clamp(target.getSpeed()*.5,.05,.3)*(repel?-2:1);
            var motion=delta.scale(speed/distance);var end=BlockPos.containing(target.position().add(motion.scale(2)));
            if(!level.hasChunkAt(end)||!Authority.mayChange(level,actor,end)||!ClaimTransfers.allowed(level,target.blockPosition(),end))continue;
            target.setDeltaMovement(motion);target.needsSync=true;moved=true;
            if(level.getGameTime()%10==0)level.sendParticles(ParticleTypes.ENCHANT,target.getX(),target.getY()+.5,target.getZ(),1,.1,.1,.1,0);
        }
        if(moved&&level.getGameTime()%4==0)flower.draw(1);
    }
}

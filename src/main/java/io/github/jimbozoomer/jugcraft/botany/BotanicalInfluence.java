package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.phys.AABB;

/** Native mob AI, without rewriting a creature's permanent list of combat goals. */
public final class BotanicalInfluence {
    public static final String PACIFIED="jugcraft:botanical_tigerseye_pacified";
    static void tick(BotanicalWorkshop flower,ServerLevel level,ServerPlayer actor){
        if(level.getGameTime()%10!=0)return;
        if(flower.kind().equals("tigerseye")){
            int examined=0;
            for(var creeper:level.getEntitiesOfClass(Creeper.class,new AABB(flower.effectivePos()).inflate(10,4,10))){
                if(++examined>32||flower.mana()<70)break;
                if(creeper.entityTags().contains(PACIFIED)||!BotanicalDefenses.eligible(level,actor,creeper))continue;
                if(!flower.draw(70))return;
                if(!creeper.addTag(PACIFIED)){flower.fill(70);continue;}
                creeper.setTarget(null);creeper.setSwellDir(-1);creeper.getNavigation().stop();
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,creeper.getX(),creeper.getY()+1,creeper.getZ(),12,.4,.4,.4,0);
            }return;
        }
        if(flower.mana()<100)return;
        var mobs=level.getEntitiesOfClass(Mob.class,new AABB(flower.effectivePos()).inflate(5),m->m instanceof Enemy&&BotanicalDefenses.eligible(level,actor,m)).stream().limit(32).toList();
        for(var mob:mobs){
            prune(mob);
            if(mob.getTarget() instanceof Enemy||mob.getGoalSelector().getAvailableGoals().stream().anyMatch(g->g.getGoal() instanceof Rivalry))continue;
            var opponent=mobs.stream().filter(m->m!=mob&&!m.isAlliedTo(mob)).findFirst().orElse(null);if(opponent==null)continue;
            if(!flower.draw(100))return;
            mob.getGoalSelector().addGoal(-1,new Rivalry(mob,opponent,flower.getBlockPos(),actor.getUUID()));
            mob.setTarget(opponent);return;
        }
    }
    /** A paid bout lasts at most thirty seconds and rechecks both combatants' permissions. */
    private static final class Rivalry extends Goal {
        private final Mob mob,opponent;private final BlockPos source;private final java.util.UUID owner;
        private int remaining=600;private boolean finished;
        Rivalry(Mob mob,Mob opponent,BlockPos source,java.util.UUID owner){this.mob=mob;this.opponent=opponent;this.source=source.immutable();this.owner=owner;setFlags(EnumSet.of(Flag.TARGET));}
        @Override public boolean canUse(){return !finished&&allowed();}
        @Override public boolean canContinueToUse(){return !finished&&remaining>0&&allowed();}
        private boolean allowed(){
            if(!(mob.level() instanceof ServerLevel level)||!level.hasChunkAt(source)||!opponent.isAlive()||mob.distanceToSqr(opponent)>1024)return false;
            var actor=Authority.answering(level,owner);
            return actor!=null&&level.getBlockEntity(source) instanceof BotanicalWorkshop flower&&flower.kind().equals("heisei_dream")&&flower.permitted(actor)
                &&!level.hasNeighborSignal(flower.effectivePos())&&BotanicalDefenses.eligible(level,actor,mob)&&BotanicalDefenses.eligible(level,actor,opponent);
        }
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void tick(){remaining--;mob.setTarget(opponent);}
        @Override public void stop(){finished=true;if(mob.getTarget()==opponent)mob.setTarget(null);}
    }
    static void prune(Mob mob){mob.getGoalSelector().getAvailableGoals().stream().map(g->g.getGoal()).filter(g->g instanceof Rivalry r&&r.finished).toList().forEach(mob.getGoalSelector()::removeGoal);}
}

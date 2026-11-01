package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Weak, level-scoped live references: no chunk tickets and no stale saved location registry. */
public final class BotanicalFields {
    private static final Map<Level,Set<BotanicalWorkshop>> LIVE=new WeakHashMap<>();
    static void track(BotanicalWorkshop flower){
        if(flower.getLevel()==null||!Set.of("bergamute","solegnolia","solegnolia_chibi","vinculotus").contains(flower.kind()))return;
        synchronized(LIVE){LIVE.computeIfAbsent(flower.getLevel(),l->Collections.newSetFromMap(new WeakHashMap<>())).add(flower);}
    }
    static void forget(BotanicalWorkshop flower){synchronized(LIVE){var found=LIVE.get(flower.getLevel());if(found!=null)found.remove(flower);}}
    private static List<BotanicalWorkshop> live(Level level){synchronized(LIVE){return List.copyOf(LIVE.getOrDefault(level,Set.of()));}}
    private static boolean active(Level level,BotanicalWorkshop flower){return !flower.isRemoved()&&flower.getLevel()==level&&level.hasChunkAt(flower.getBlockPos())&&level.getBlockEntity(flower.getBlockPos())==flower&&level.hasChunkAt(flower.effectivePos())&&!level.hasNeighborSignal(flower.effectivePos());}
    public static boolean magnetBlocked(Entity entity){return magnetBlocked(entity.level(),entity.position());}
    public static boolean magnetBlocked(Level level,Vec3 position){
        for(var flower:live(level))if(flower.kind().startsWith("solegnolia")&&active(level,flower)&&position.distanceToSqr(Vec3.atCenterOf(flower.effectivePos()))<=(flower.kind().endsWith("chibi")?1:25))return true;return false;
    }
    public static int mufflers(Level level,Vec3 sound){int count=0;for(var flower:live(level))if(flower.kind().equals("bergamute")&&active(level,flower)&&sound.distanceToSqr(Vec3.atCenterOf(flower.effectivePos()))<=16&&++count==4)break;return count;}
    public static boolean mufflesVibration(Level level,Vec3 from,Vec3 to){
        var path=to.subtract(from);double squared=path.lengthSqr();
        for(var flower:live(level))if(flower.kind().equals("bergamute")&&active(level,flower)){
            var point=Vec3.atCenterOf(flower.effectivePos());double t=squared<.00001?0:Math.clamp(point.subtract(from).dot(path)/squared,0,1);
            if(point.distanceToSqr(from.add(path.scale(t)))<=16)return true;
        }return false;
    }
    public record Redirect(BotanicalWorkshop flower,Vec3 target){public void refund(){flower.fill(50);}}
    public static Redirect redirect(Enderman enderman,Vec3 requested){
        if(!(enderman.level() instanceof ServerLevel level))return null;
        for(var flower:live(level)){
            if(!flower.kind().equals("vinculotus")||!active(level,flower)||flower.mana()<50||requested.distanceToSqr(Vec3.atCenterOf(flower.effectivePos()))>4096)continue;
            var actor=flower.actor();if(actor==null||!flower.permitted(actor)||!BotanicalDefenses.eligible(level,actor,enderman))continue;
            for(int i=0;i<9;i++){
                var at=flower.effectivePos().offset(i%3-1,0,i/3-1);var to=Vec3.atBottomCenterOf(at);
                if(!level.hasChunkAt(at)||!ClaimTransfers.allowed(level,enderman.blockPosition(),at)||!level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP))continue;
                boolean allowed=true;for(int y=0;y<3;y++)if(!Authority.mayChange(level,actor,at.above(y))||!level.getFluidState(at.above(y)).isEmpty())allowed=false;
                if(!allowed||!level.noCollision(enderman,enderman.getBoundingBox().move(to.subtract(enderman.position()))))continue;
                if(flower.draw(50))return new Redirect(flower,to);
            }
        }return null;
    }
}

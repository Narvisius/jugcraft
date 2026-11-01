package io.github.jimbozoomer.jugcraft.wilds;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.*;

/** Supplied angle solver and surface selection, bounded for shared servers. */
final class WildElokosaLeaps {
    private static final int[] ANGLES={-20,-10,-5,0,5,20,35,50,65,70,75,80};
    private record Landing(Vec3 at,double score) {}
    private WildElokosaLeaps() {}

    static boolean loaded(WildElokosa mob,AABB box) {
        var level=mob.level();
        if(!level.getWorldBorder().isWithinBounds(BlockPos.containing(box.minX,box.minY,box.minZ))
                ||!level.getWorldBorder().isWithinBounds(BlockPos.containing(box.maxX,box.maxY,box.maxZ)))return false;
        for(var p:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))
            if(!level.isInWorldBounds(p)||!level.hasChunkAt(p))return false;
        return true;
    }
    private static boolean clear(WildElokosa mob,Vec3 at,boolean landing) {
        var box=mob.getBoundingBox().move(at.subtract(mob.position())).deflate(.001);
        return loaded(mob,box)&&mob.level().noCollision(mob,box)
                &&(!landing||!mob.level().containsAnyLiquid(box));
    }

    static Vec3 velocity(WildElokosa mob,Vec3 target) {return velocity(mob,target,new int[]{1024});}
    private static Vec3 velocity(WildElokosa mob,Vec3 target,int[] budget) {
        // The source aims half a block short and applies .95 launch compensation.
        Vec3 delta=target.subtract(mob.position());
        double horizontal=delta.horizontalDistance();
        if(horizontal<.5||horizontal>26||!clear(mob,target,false))return null;
        Vec3 offset=delta.subtract(new Vec3(delta.x,0,delta.z).normalize().scale(.5));
        double distance=offset.horizontalDistance(),gravity=mob.getAttributeValue(Attributes.GRAVITY);
        double maximum=mob.getAttributeValue(Attributes.JUMP_STRENGTH)*3.5;
        if(gravity<=0||!Double.isFinite(gravity))return null;
        int[] angles=ANGLES.clone();
        for(int i=angles.length-1;i>0;i--){int j=mob.getRandom().nextInt(i+1),swap=angles[i];angles[i]=angles[j];angles[j]=swap;}
        for(int degrees:angles){
            double angle=Math.toRadians(degrees),cos=Math.cos(angle),sin=Math.sin(angle);
            double denominator=distance*Math.sin(2*angle)-2*offset.y*cos*cos;
            if(denominator<=0)continue;
            double speedSquared=distance*distance*gravity/denominator;
            double speed=Math.sqrt(speedSquared),horizontalSpeed=speed*cos;
            if(!Double.isFinite(speed)||speed>maximum||horizontalSpeed<.01)continue;
            double time=distance/horizontalSpeed;
            if(time>70)continue;
            // Quarter-block samples also cover the first and last segments, which
            // the source skipped. No collision query may load an unchecked chunk.
            int samples=(int)Math.ceil(time*Math.max(speed,gravity*time)*4);
            if(samples>512)continue;
            boolean valid=true;
            for(int i=1;i<=samples;i++){
                if(--budget[0]<0)return null;
                double t=time*i/samples;
                var at=mob.position().add(offset.x/distance*horizontalSpeed*t,speed*sin*t-gravity*t*t/2,offset.z/distance*horizontalSpeed*t);
                if(!clear(mob,at,false)){valid=false;break;}
            }
            if(valid)return new Vec3(offset.x/distance*horizontalSpeed,speed*sin,offset.z/distance*horizontalSpeed).scale(.95);
        }
        return null;
    }

    static Vec3 surfaceVelocity(WildElokosa mob,Vec3 preferred) {
        if(!mob.onGround()&&mob.cling()==Direction.DOWN)return null;
        var origin=mob.blockPosition();var level=mob.level();var candidates=new ArrayList<Landing>();
        var seen=new HashSet<BlockPos>();
        // 96 samples per request, followed by at most 20 ranked trajectory solves.
        // Nearby columns are searched first; wider samples allow canopy/ceiling leaps.
        for(int i=0;i<96;i++){
            BlockPos p;
            if(i<48)p=BlockPos.containing(preferred).offset(i%3-1,(i/9)*3-6,(i/3)%3-1);
            else p=origin.offset(mob.getRandom().nextInt(53)-26,mob.getRandom().nextInt(41)-14,mob.getRandom().nextInt(53)-26);
            if(!seen.add(p)||p.distSqr(origin)>26*26||!level.hasChunkAt(p)||!level.isInWorldBounds(p))continue;
            var state=level.getBlockState(p);boolean climbable=state.is(BlockTags.CLIMBABLE);
            if(!climbable&&state.getCollisionShape(level,p).isEmpty())continue;
            for(var face:Direction.values()){
                if(climbable&&face!=Direction.UP)continue;
                Vec3 at=Vec3.atBottomCenterOf(p.relative(face));
                if(face==Direction.DOWN)at=Vec3.atBottomCenterOf(p).add(0,-mob.getBbHeight()-.02,0);
                if(mob.position().distanceToSqr(at)<6||!clear(mob,at,true))continue;
                double score=16+p.getY()-mob.getY()+(state.is(BlockTags.LOGS)||state.is(BlockTags.LEAVES)?6:0)
                        +mob.getRandom().nextInt(61)-30-at.distanceTo(preferred)*3;
                candidates.add(new Landing(at,score));
            }
        }
        candidates.sort(Comparator.comparingDouble(Landing::score).reversed());
        int[] budget={1024};
        for(int i=0;i<Math.min(20,candidates.size())&&budget[0]>0;i++){
            Vec3 velocity=velocity(mob,candidates.get(i).at(),budget);if(velocity!=null)return velocity;
        }
        return null;
    }
}

package io.github.jimbozoomer.jugcraft.wilds;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.*;

/** Incremental, non-destructive branching platform plan, checked before payment. */
final class WildTrialCourse {
    record Step(Vec3 at,boolean fragile) { AABB box(){return new AABB(at.x-1,at.y,at.z-1,at.x+1,at.y+1,at.z+1);} }
    private record Edge(Step from,Step to) {}
    private final List<Edge> branchesEdges=new ArrayList<>();
    private final List<Step> main=new ArrayList<>(),branches=new ArrayList<>();
    private final Vec3 origin;
    private boolean done,failed;
    private int work;
    WildTrialCourse(Vec3 origin){this.origin=origin;}
    boolean done(){return done;}
    boolean failed(){return failed;}
    List<Step> steps(){var all=new ArrayList<>(main);all.addAll(branches);return all;}
    static double radius(double y){return 6+Math.pow(Math.min(y/20,1),5)*6;}
    void tick(ServerLevel level,WildSculptor host,ServerPlayer player){
        if(done||failed)return;
        for(int count=0;count<2&&!done&&!failed;count++){
            Step prior=main.isEmpty()?new Step(origin.add(2.5,-1,0),false):main.getLast();
            double top=prior.at.y+1-origin.y;
            if(top>=60&&prior.at.subtract(origin).horizontalDistance()<3.4){done=true;break;}
            if(main.size()>=112||++work>512){failed=true;break;}
            Step selected=null;
            for(int attempt=0;attempt<12;attempt++){
                double gain=top>=60?0:main.isEmpty()?1:.7+host.getRandom().nextDouble()*.3;
                double nextTop=Math.min(60,top+gain);
                Vec3 toward=origin.subtract(prior.at).multiply(1,0,1).normalize();
                double distance=prior.at.subtract(origin).horizontalDistance();
                double spread=top>=59?0:Math.toRadians(55+host.getRandom().nextDouble()*100)*(1-Math.pow(top/60,5)*.75);
                if(host.getRandom().nextBoolean())spread=-spread;
                Vec3 direction=toward.yRot((float)spread);
                double horizontal=3.1+host.getRandom().nextDouble()*.8;
                Vec3 next=prior.at.add(direction.scale(horizontal));
                double maxRadius=radius(nextTop)-1.1;
                Vec3 offset=next.subtract(origin).multiply(1,0,1);
                if(offset.horizontalDistance()>maxRadius)next=origin.add(offset.normalize().scale(maxRadius));
                if(top>=59&&distance<5)next=origin.add(toward.scale(-2.8));
                next=new Vec3(next.x,origin.y+nextTop-1,next.z);
                var candidate=new Step(next,false);
                if(next.subtract(origin).horizontalDistance()<2.3||!clear(level,player,candidate,prior)||!jump(prior,candidate))continue;
                selected=candidate;break;
            }
            if(selected==null){for(int i=0;i<3&&!main.isEmpty();i++)main.removeLast();branches.clear();branchesEdges.clear();continue;}
            main.add(selected);
            // Source branching probability and upper-course crumbling threshold.
            // Alternate landings reconnect to the main route; it never requires
            // a temporary platform in order to remain completable.
            if(main.size()>2&&branches.size()<32&&host.getRandom().nextFloat()<.27F){
                Step before=main.get(main.size()-2);Vec3 span=selected.at.subtract(before.at);
                Vec3 side=new Vec3(-span.z,0,span.x).normalize().scale(host.getRandom().nextBoolean()?2.4:-2.4);
                var alternate=new Step(before.at.lerp(selected.at,.5).add(side),top>27&&host.getRandom().nextFloat()<.15F);
                if(alternate.at.subtract(origin).horizontalDistance()<radius(top)-1&&clear(level,player,alternate,before)&&jump(before,alternate)&&jump(alternate,selected)){branches.add(alternate);branchesEdges.add(new Edge(before,alternate));branchesEdges.add(new Edge(alternate,selected));}
            }
        }
    }
    private Step start(){return new Step(origin.add(2.5,-1,0),false);}
    boolean validate(ServerLevel level,ServerPlayer player){
        if(!done||failed||main.isEmpty())return false;
        var start=start();var footing=BlockPos.containing(start.at.add(0,.9,0));
        if(!level.hasChunkAt(footing)||!level.getBlockState(footing).isFaceSturdy(level,footing,net.minecraft.core.Direction.UP))return false;
        for(var step:steps())if(!worldClear(level,player,step.box().expandTowards(0,1.85,0)))return false;
        var prior=start;
        for(var step:main){if(!jump(prior,step)||!worldJumpClear(level,player,prior,step))return false;prior=step;}
        for(var edge:branchesEdges)if(!jump(edge.from,edge.to)||!worldJumpClear(level,player,edge.from,edge.to))return false;
        return true;
    }
    private boolean worldJumpClear(ServerLevel level,ServerPlayer player,Step from,Step to){
        var direction=to.at.subtract(from.at).multiply(1,0,1).normalize();
        var a=from.at.add(0,1,0).add(direction.scale(.72));var b=to.at.add(0,1,0).subtract(direction.scale(.72));
        double disc=.42*.42-2*.08*(b.y-a.y);if(disc<=0)return false;double duration=(.42+Math.sqrt(disc))/.08;
        for(int i=0;i<=20;i++){
            double t=duration*i/20D;var point=a.lerp(b,i/20D);double y=a.y+.42*t-.04*t*t;
            if(!worldClear(level,player,new AABB(point.x-.3,y+.02,point.z-.3,point.x+.3,y+1.8,point.z+.3)))return false;
        }return true;
    }
    private static boolean worldClear(ServerLevel level,ServerPlayer player,AABB clearance){
        for(var at:BlockPos.betweenClosed(BlockPos.containing(clearance.minX,clearance.minY,clearance.minZ),BlockPos.containing(clearance.maxX,clearance.maxY,clearance.maxZ)))
            if(!level.isInWorldBounds(at)||!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at)||!Authority.mayChange(level,player,at)||!level.getBlockState(at).getCollisionShape(level,at).isEmpty()||!level.getFluidState(at).isEmpty())return false;
        return true;
    }
    private boolean clear(ServerLevel level,ServerPlayer player,Step candidate,Step prior){
        AABB clearance=candidate.box().expandTowards(0,1.85,0);
        if(!worldClear(level,player,clearance))return false;
        // A newly placed landing must not block a previously accepted jump.
        var priorMain=start();for(var step:main){if(intersectsJump(priorMain,step,candidate))return false;priorMain=step;}
        for(var edge:branchesEdges)if(intersectsJump(edge.from,edge.to,candidate))return false;
        for(var existing:main)if(existing!=prior&&existing.box().inflate(.1,.1,.1).intersects(clearance))return false;
        for(var existing:branches)if(existing.box().inflate(.1,.1,.1).intersects(clearance))return false;
        return true;
    }
    private static boolean intersectsJump(Step from,Step to,Step obstacle){
        if(obstacle==from||obstacle==to)return false;
        var direction=to.at.subtract(from.at).multiply(1,0,1).normalize();
        var a=from.at.add(0,1,0).add(direction.scale(.72));var b=to.at.add(0,1,0).subtract(direction.scale(.72));
        double disc=.42*.42-2*.08*(b.y-a.y);if(disc<=0)return true;double duration=(.42+Math.sqrt(disc))/.08;
        for(int i=1;i<20;i++){
            double t=duration*i/20D;var point=a.lerp(b,i/20D);double y=a.y+.42*t-.04*t*t;
            if(new AABB(point.x-.3,y+.02,point.z-.3,point.x+.3,y+1.8,point.z+.3).intersects(obstacle.box()))return true;
        }return false;
    }
    private boolean jump(Step from,Step to){
        Vec3 direction=to.at.subtract(from.at).multiply(1,0,1).normalize();
        Vec3 start=from.at.add(0,1,0).add(direction.scale(.72)),end=to.at.add(0,1,0).subtract(direction.scale(.72));
        double height=end.y-start.y,discriminant=.42*.42-2*.08*height;
        if(discriminant<=0)return false;
        double duration=(.42+Math.sqrt(discriminant))/.08;
        if(end.subtract(start).horizontalDistance()/duration>.25)return false;
        for(int i=1;i<20;i++){
            double t=duration*i/20D;Vec3 at=start.lerp(end,i/20D);at=new Vec3(at.x,start.y+.42*t-.04*t*t,at.z);
            var body=new AABB(at.x-.3,at.y+.02,at.z-.3,at.x+.3,at.y+1.8,at.z+.3);
            for(var other:main)if(other!=from&&other!=to&&body.intersects(other.box()))return false;
            for(var other:branches)if(other!=from&&other!=to&&body.intersects(other.box()))return false;
            // Never send a route through the solid central pillar.
            if(body.intersects(new AABB(origin.x-1,origin.y,origin.z-1,origin.x+1,origin.y+60,origin.z+1)))return false;
        }
        return true;
    }
}

package local.peepo.client;

import local.peepo.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Client-only motions for real work/transfer events. No inventory lookup, particles or entity spawning. */
final class CompanionActionPose {
    static void extract(PeepoEntity npc,PeepoState s,float partial,ItemModelResolver items){
        s.actionPose=s.spatula=s.pressSeated=false;s.pressOffset=Vec3.ZERO;
        s.gesture=CompanionGesture.NONE;s.actionScale=.38F;
        var shown=ItemStack.EMPTY;var level=npc.level();
        if(!s.eating && !s.sleeping && !s.sitting && !s.wheelRunning && npc.isAlive()){
            if(s.work==WorkAnimation.NONE)s.gesture=npc.gesture();
            if(s.gesture!=CompanionGesture.NONE){
                var target=npc.gestureTarget();
                if(s.gesture==CompanionGesture.CARRY || level.hasChunkAt(target) && target.distToCenterSqr(npc.position())<16){
                    float t=Mth.clamp((level.getGameTime()-npc.gestureStarted()+partial)/CompanionGesture.TICKS,0,1);
                    var center=s.gesture==CompanionGesture.CARRY?npc.getPosition(partial).add(0,.35,0):nearSurface(npc,target,partial);
                    var point=relative(npc,center,partial);
                    handling(s,t,point.x,point.y,point.z);
                    s.actionPose=true;shown=npc.gestureItem();
                    if(s.gesture==CompanionGesture.PUT && t>.6F || s.spatula)shown=ItemStack.EMPTY;
                }
            }else if(s.work==WorkAnimation.PLANT || s.work==WorkAnimation.PICK || s.work==WorkAnimation.WATER_FILL){
                var target=npc.workTarget();
                if(level.hasChunkAt(target)){
                    s.workPhase=s.work.phase(level.getGameTime()-npc.workStarted(),partial,0);
                    float t=s.workPhase/Mth.TWO_PI;
                    boolean water=s.work==WorkAnimation.WATER_FILL,plant=s.work==WorkAnimation.PLANT;
                    var point=relative(npc,water?Vec3.atBottomCenterOf(target).add(0,.86,0):nearSurface(npc,target,partial),partial);
                    tending(s,t,point.x,point.y,point.z);
                    s.actionPose=true;
                    if(water || !plant || t<.58F)shown=npc.workPie();
                    if(plant)s.actionScale=.20F;
                }
            }else if(s.work==WorkAnimation.PRESS && level.hasChunkAt(npc.workTarget())
                    && level.getBlockEntity(npc.workTarget()) instanceof CiderPressBlockEntity press){
                CiderPressPose.prepare(npc,s,press,partial);
                s.actionPose=true;
            }
        }
        if(s.actionPose && !s.pressSeated){s.bodyRot=npc.getYRot();s.yRot=s.xRot=0;}
        items.updateForLiving(s.actionItem,shown,ItemDisplayContext.NONE,npc);
    }
    static Vec3 nearSurface(PeepoEntity npc,net.minecraft.core.BlockPos target,float partial){
        var level=npc.level();var shape=level.getBlockState(target).getShape(level,target);
        var bounds=(shape.isEmpty()?new net.minecraft.world.phys.AABB(0,0,0,1,1,1):shape.bounds()).move(target);
        var from=npc.getPosition(partial).add(0,.35,0);
        // A side face at hand height is sufficient; do not force tiny helpers to reach a block's center/top.
        return new Vec3(Mth.clamp(from.x,bounds.minX,bounds.maxX),Mth.clamp(from.y,bounds.minY,bounds.maxY),Mth.clamp(from.z,bounds.minZ,bounds.maxZ));
    }
    private static org.joml.Vector3f relative(PeepoEntity npc,Vec3 point,float partial){
        var d=point.subtract(npc.getPosition(partial));double yaw=Math.toRadians(npc.getYRot());
        return new org.joml.Vector3f((float)((d.x*Math.cos(yaw)+d.z*Math.sin(yaw))*16),(float)(24-d.y*16),(float)((d.x*Math.sin(yaw)-d.z*Math.cos(yaw))*16));
    }
    private static void reset(MachineWorkClip p){p.x=p.xRot=p.zRot=p.bodyPitch=p.bodyRoll=p.bodyX=p.bodyY=0;}
    static void handling(PeepoState s,float t,float targetX,float targetY,float targetZ){
        var p=s.toolPose;reset(p);
        float reach=s.gesture==CompanionGesture.CARRY?0:s.gesture==CompanionGesture.TAKE?1-smooth(t):Mth.sin(t*Mth.PI);
        float bob=s.gesture==CompanionGesture.CARRY?Mth.sin(s.walkAnimationPos*1.3F)*Math.min(s.walkAnimationSpeed,.5F)*.35F:0;
        p.x=Mth.lerp(reach,0,targetX);p.y=Mth.lerp(reach,19.8F+bob,targetY);p.z=Mth.lerp(reach,s.pumpkin?-5.6F:-4.5F,targetZ);
        p.bodyPitch=reach*.18F;p.bodyY=reach*.12F;
        if(s.gesture==CompanionGesture.POUR)p.zRot=-Mth.sin(t*Mth.PI)*1.15F;
        if(s.gesture==CompanionGesture.TURN_FOOD && t>.35F){
            s.spatula=true;float flip=Mth.sin((t-.35F)/.65F*Mth.PI);
            p.x=targetX;p.xRot=1.30F-flip*.48F;
            p.y=targetY+Mth.cos(p.xRot)*3.5F-flip*.7F;p.z=targetZ+Mth.sin(p.xRot)*3.5F;
            p.bodyPitch=.15F+flip*.12F;
            p.grips(.2F,.25F,.40F);BoundedActionReach.limit(s);return;
        }
        p.grips(.3F,1.5F,0);
        BoundedActionReach.limit(s);
    }
    static void tending(PeepoState s,float t,float targetX,float targetY,float targetZ){
        if(s.work==WorkAnimation.WATER_FILL){WaterScoopPose.prepare(s,t);return;}
        var p=s.toolPose;reset(p);
        boolean plant=s.work==WorkAnimation.PLANT;
        float reach=t<.45F?smooth(t/.45F):t<.65F?1:1-smooth((t-.65F)/.35F);
        float pat=plant && t>.45F && t<.75F?Mth.sin((t-.45F)/.30F*Mth.PI*2)*.28F:0;
        p.x=targetX*reach;p.y=Mth.lerp(reach,20,targetY)+pat;p.z=Mth.lerp(reach,s.pumpkin?-5.5F:-4.2F,targetZ);
        p.bodyPitch=reach*(targetY>16?.40F:-.08F);p.bodyY=reach*(targetY>16?.45F:-.20F);
        p.bodyRoll=Mth.sin(t*Mth.TWO_PI)*.025F;
        if(!plant)p.xRot=Mth.sin(t*Mth.PI)*.16F;
        p.grips(.3F,.55F,.15F);
        BoundedActionReach.limit(s);
    }
    private static float smooth(float t){return t*t*(3-2*t);}
}

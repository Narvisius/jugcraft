package local.peepo.client;

import java.util.function.BiConsumer;
import local.peepo.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Checks the actual rig/prop grip path on both appearances and costumes. */
public final class CompanionActionChecks {
    public static void run(BiConsumer<Boolean,String> check){
        for(boolean jughead:new boolean[]{false,true})for(boolean pumpkin:new boolean[]{false,true}){
            var root=PeepoGeometry.create().bakeRoot();var model=new PeepoModel(root);
            for(int action=0;action<8;action++){
                var s=new PeepoState();s.jughead=jughead;s.pumpkin=pumpkin;s.actionPose=true;
                s.walkAnimationPos=2;s.walkAnimationSpeed=.4F;
                if(action<5)s.gesture=CompanionGesture.values()[action+1];
                else s.work=new WorkAnimation[]{WorkAnimation.PLANT,WorkAnimation.PICK,WorkAnimation.WATER_FILL}[action-5];
                for(float t:new float[]{0,.25F,.5F,.75F,1}){
                    s.spatula=false;
                    if(action<5)CompanionActionPose.handling(s,t,0,23,-12);
                    else CompanionActionPose.tending(s,t,0,25,-12);
                    model.setupAnim(s);var p=s.toolPose;
                    for(boolean left:new boolean[]{false,true}){
                        var arm=root.getChild(left?"left_arm":"right_arm");var hand=new Vector3f(0,2.7F*arm.yScale,0);
                        new Quaternionf().rotationZYX(arm.zRot,arm.yRot,arm.xRot).transform(hand);hand.add(arm.x,arm.y,arm.z);
                        check.accept(hand.distance(left?p.leftX:p.rightX,left?p.leftY:p.rightY,left?p.leftZ:p.rightZ)<.001F,"hand follows action pose: "+action);
                        check.accept(arm.yScale<=BoundedActionReach.MAX_REACH/2.7F+.0001F,"action arm stays within comfortable reach: "+action);
                        if(action==7)check.accept(arm.xScale==1 && arm.yScale==1 && arm.zScale==1,"water scoop never stretches either arm");
                    }
                    check.accept(Float.isFinite(root.getChild("body").xRot),"finite action torso");
                    if(action==0)check.accept(root.getChild("left_leg").xRot!=0,"carrying preserves walking legs");
                    if(action==3 && t==.5F)check.accept(p.zRot<-.9F,"bucket tilts to pour");
                    if(action==4 && t==.5F)check.accept(s.spatula,"kitchen loading switches to spatula");
                    if(action==7){
                        check.accept(Math.abs(p.x-p.rightX)<.001F && Math.abs(p.y-p.rightY-1.45F*(float)Math.cos(p.xRot))<.001F
                            && Math.abs(p.z-p.rightZ-1.45F*(float)Math.sin(p.xRot))<.001F,"water container stays at right-hand rim grip");
                        check.accept(Math.abs(p.leftX-p.x)>5,"free hand stays clear of water container");
                        if(t==.5F)check.accept(root.getChild("body").xRot>.9F && p.rightZ<-5,"water scoop bends whole torso and reaches forward");
                        if(t==0 || t==1)check.accept(p.bodyPitch==0 && p.xRot==0,"water scoop starts and ends upright");
                        float handZ=p.rightZ;
                        CompanionActionPose.tending(s,t,8,40,-32);
                        check.accept(p.rightZ==handZ,"distant water does not lengthen scoop reach");
                    }
                }
                s.actionPose=false;s.work=WorkAnimation.NONE;s.gesture=CompanionGesture.NONE;
                model.setupAnim(s);check.accept(root.getChild("left_arm").yScale==1,"action arm stretch clears on idle");
            }
            var seated=new PeepoState();seated.jughead=jughead;seated.pumpkin=pumpkin;seated.pressSeated=seated.actionPose=true;seated.work=WorkAnimation.PRESS;
            for(var facing:net.minecraft.core.Direction.Plane.HORIZONTAL)for(int degrees=0;degrees<=360;degrees+=15){
                float angle=CiderPressPose.angle(degrees,facing);seated.workPhase=angle*(float)Math.PI/180;
                model.setupAnim(seated);
                check.accept(root.getChild("left_arm").yScale==1 && root.getChild("right_arm").yScale==1,"press rider keeps normal arms");
                check.accept(root.getChild("left_leg").xRot<-1 && root.getChild("right_leg").xRot<-1,"press rider sits with dangling legs");
                var seat=CiderPressPose.seat(net.minecraft.core.BlockPos.ZERO,angle);
                check.accept(Math.abs(seat.y-17.0/16)<.00001 && Math.abs(Math.hypot(seat.x-.5,seat.z-.5)-4.3/16)<.00001,"press seat stays on top of same handle end");
            }
            seated.pressSeated=seated.actionPose=false;seated.work=WorkAnimation.NONE;model.setupAnim(seated);
            check.accept(root.getChild("left_leg").xRot==0,"press sitting pose clears on idle");
        }
    }
    public static void press(PeepoEntity npc,PeepoState s,BiConsumer<Boolean,String> check){
        check.accept(s.pressSeated,"press renders seated rider");
        var position=npc.getPosition(0).add(s.pressOffset).add(0,1.5/16,0);
        var target=npc.workTarget();
        check.accept(Math.abs(position.y-target.getY()-17.0/16)<.00001,"rendered hips touch top of real handle");
        check.accept(Math.abs(Math.hypot(position.x-target.getX()-.5,position.z-target.getZ()-.5)-4.3/16)<.00001,"rendered rider follows handle radius");
        check.accept(s.actionItem.isEmpty(),"press rider has no floating item prop");
    }
}

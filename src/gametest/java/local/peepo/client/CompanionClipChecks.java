package local.peepo.client;

import java.util.HashSet;
import java.util.function.BiConsumer;
import local.peepo.WorkAnimation;

/** Uses the same clip evaluation and rig as rendering, without making production internals public. */
public final class CompanionClipChecks {
    public static void harvesting(BiConsumer<Boolean,String> check){
        for(boolean jughead:new boolean[]{false,true})for(boolean pumpkin:new boolean[]{false,true}){
            var root=PeepoGeometry.create().bakeRoot();var model=new PeepoModel(root);var state=new PeepoState();
            state.work=WorkAnimation.HARVEST;state.jughead=jughead;state.pumpkin=pumpkin;
            var bodies=new HashSet<String>();
            for(int tick:new int[]{0,10,22,30,38}){
                state.toolPose.harvest(tick*net.minecraft.util.Mth.TWO_PI/40,25,-16);model.setupAnim(state);var p=state.toolPose;
                for(boolean left:new boolean[]{false,true}){
                    var arm=root.getChild(left?"left_arm":"right_arm");var hand=new org.joml.Vector3f(0,2.7F*arm.yScale,0);
                    new org.joml.Quaternionf().rotationZYX(arm.zRot,arm.yRot,arm.xRot).transform(hand);hand.add(arm.x,arm.y,arm.z);
                    check.accept(hand.distance(left?p.leftX:p.rightX,left?p.leftY:p.rightY,left?p.leftZ:p.rightZ)<.001F,"both hands follow hoe handle");
                }
                var body=root.getChild("body");bodies.add(body.y+":"+body.z+":"+body.xRot);
                var pose=new com.mojang.blaze3d.vertex.PoseStack();PeepoRenderer.hoePose(pose,p);
                check.accept(pose.last().pose().getScale(new org.joml.Vector3f()).distance(.65F,.65F,.65F)<.00001F,"hoe uses reduced size independently of arm stretch");
                if(tick==38){var tip=pose.last().pose().transformPosition(new org.joml.Vector3f(.25F,.25F,0));
                    check.accept(Math.abs(tip.y-25F/16)<.001F && Math.abs(tip.z+1)<.001F,"hoe head reaches farmland at contact");
                    check.accept(body.xRot>.5F && root.getChild("left_leg").xRot>0,"torso and braced legs follow through");
                }
            }
            check.accept(bodies.size()==5,"whole-body wind-up and strike on every rig/costume");
            state.work=WorkAnimation.NONE;model.setupAnim(state);check.accept(root.getChild("left_arm").yScale==1,"hoe pose resets after work");
        }
    }
    public static void chopping(BiConsumer<Boolean,String> check){
        for(boolean jughead:new boolean[]{false,true})for(boolean pumpkin:new boolean[]{false,true}){
            var root=PeepoGeometry.create().bakeRoot();var model=new PeepoModel(root);var state=new PeepoState();
            state.work=WorkAnimation.CHOP;state.jughead=jughead;state.pumpkin=pumpkin;
            var bodies=new HashSet<String>();var arms=new HashSet<String>();
            for(int i=0;i<40;i++){
                state.workPhase=i*net.minecraft.util.Mth.TWO_PI/40;state.toolPose.chop(state.workPhase,22.95F,-12.8F);
                model.setupAnim(state);var p=state.toolPose;
                for(boolean left:new boolean[]{false,true}){
                    var arm=root.getChild(left?"left_arm":"right_arm");
                    var hand=new org.joml.Vector3f(0,2.7F*arm.yScale,0);
                    new org.joml.Quaternionf().rotationZYX(arm.zRot,arm.yRot,arm.xRot).transform(hand);hand.add(arm.x,arm.y,arm.z);
                    check.accept(hand.distance(left?p.leftX:p.rightX,left?p.leftY:p.rightY,left?p.leftZ:p.rightZ)<.001F,"both hands stay on knife grip");
                    arms.add(arm.xRot+":"+arm.yRot);
                }
                var body=root.getChild("body");bodies.add(body.y+":"+body.z+":"+body.xRot);
                var pose=new com.mojang.blaze3d.vertex.PoseStack();PeepoRenderer.knifePose(pose,p);
                var scale=pose.last().pose().getScale(new org.joml.Vector3f());
                check.accept(scale.distance(1,1,1)<.00001F,"knife keeps native scale independently of arm stretch");
            }
            check.accept(bodies.size()>10 && arms.size()>10,"whole-body chopping motion on both rigs/costumes");
            state.work=WorkAnimation.NONE;model.setupAnim(state);check.accept(root.getChild("left_arm").yScale==1,"chop clears on idle");
        }
    }
    public static void run(BiConsumer<Boolean,String> check){
        var root=PeepoGeometry.create().bakeRoot();var model=new PeepoModel(root);
        for(boolean jughead:new boolean[]{false,true})for(boolean pumpkin:new boolean[]{false,true})
        for(var action:new WorkAnimation[]{WorkAnimation.VALVE,WorkAnimation.LEVER,WorkAnimation.MALLET,WorkAnimation.WRENCH,WorkAnimation.CRANK}){
            var s=new PeepoState();s.jughead=jughead;s.pumpkin=pumpkin;s.work=action;
            var arms=new HashSet<String>();var bodies=new HashSet<String>();
            for(int i=0;i<8;i++){
                s.workPhase=(float)(i*Math.PI/4);s.toolPose.evaluate(action,s.workPhase,pumpkin);
                s.crankX=(float)Math.sin(s.workPhase)*4.5F;s.crankY=16-(float)Math.cos(s.workPhase)*4.5F;s.crankZ=-5;
                model.setupAnim(s);var arm=root.getChild("right_arm");var body=root.getChild("body");
                check.accept(Float.isFinite(arm.xRot) && Float.isFinite(arm.yScale) && Float.isFinite(body.y),"finite pose "+action);
                arms.add(arm.xRot+":"+arm.yRot);bodies.add(body.y+":"+body.z+":"+body.xRot+":"+body.zRot);
            }
            check.accept(arms.size()>2 && bodies.size()>2,"body and arms animate: "+action+", Jughead="+jughead+", pumpkin="+pumpkin);
            s.work=WorkAnimation.NONE;s.pumpkin=false;model.setupAnim(s);
            check.accept(root.getChild("right_arm").yScale==1,"work pose resets on idle: "+action);
        }
    }
}

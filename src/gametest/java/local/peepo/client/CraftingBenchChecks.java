package local.peepo.client;

import java.util.HashSet;
import java.util.function.BiConsumer;
import local.peepo.WorkAnimation;

/** Exercise the actual rendering rig for both companions and the existing pumpkin body. */
public final class CraftingBenchChecks {
    private CraftingBenchChecks(){}
    public static void run(BiConsumer<Boolean,String> check){
        for(boolean jughead:new boolean[]{false,true})for(boolean pumpkin:new boolean[]{false,true}){
            var root=PeepoGeometry.create().bakeRoot();var model=new PeepoModel(root);var state=new PeepoState();
            state.work=WorkAnimation.CRAFT;state.jughead=jughead;state.pumpkin=pumpkin;
            var arms=new HashSet<String>();var bodies=new HashSet<String>();
            for(int tick:new int[]{0,6,12,18,24,36,48,53,59}){
                state.craftPose.evaluate(tick,pumpkin);model.setupAnim(state);
                for(var name:new String[]{"left_arm","right_arm"}){
                    var arm=root.getChild(name);check.accept(Float.isFinite(arm.xRot) && Float.isFinite(arm.yRot) && Float.isFinite(arm.yScale),"finite crafting arm pose");
                    check.accept(arm.yScale<=3.1F/2.7F+.0001F,"crafting preserves short arms instead of stretching across table");arms.add(arm.xRot+":"+arm.yRot);
                }
                var body=root.getChild("body");bodies.add(body.xRot+":"+body.y+":"+body.zRot);
                for(var name:new String[]{"left_leg","right_leg"}){
                    var leg=root.getChild(name);
                    check.accept(leg.y+1.5F==24 && leg.z==0 && leg.xRot==0 && leg.yRot==0 && leg.zRot==0,
                        "feet remain planted at tabletop height directly under the body");
                    var hips=root.getChild("hips");
                    var joint=new org.joml.Vector3f(leg.x-hips.x,leg.y-hips.y,leg.z-hips.z);
                    new org.joml.Quaternionf().rotationZYX(hips.zRot,hips.yRot,hips.xRot).conjugate().transform(joint);
                    check.accept(joint.y>=21.6F && joint.y<=22.55F && Math.abs(joint.z)<1.935F && Math.abs(joint.x)<2.82F,
                        "each leg remains connected to the transformed hips throughout crafting");
                }
                var pose=state.craftPose;
                check.accept(Math.abs(pose.ingredient0X/16)<.18 && Math.abs(pose.ingredient1X/16)<.18
                    && Math.abs(local.peepo.CraftingTableJob.STANDING_OFFSET+pose.ingredient0Z/16)<.06
                    && Math.abs(local.peepo.CraftingTableJob.STANDING_OFFSET+pose.ingredient1Z/16)<.06,
                    "ingredients stay near the center of the tabletop");
                if(tick==12)check.accept(pose.leftGather==1 && pose.rightGather==0 && pose.resultScale==0,"gather alternates hands without showing a finished result");
                if(tick==36)check.accept(pose.press>.99F && pose.resultScale==0,"middle of paid cycle presses the ingredients together");
                if(tick==53)check.accept(pose.inspect==1 && pose.ingredientScale==0 && pose.resultScale>0,"last paid phase lifts only the result preview");
            }
            check.accept(arms.size()>6 && bodies.size()>5,"gather, press and inspect move arms and body on every rig");
            state.work=WorkAnimation.NONE;state.craftPose.clear();model.setupAnim(state);
            check.accept(root.getChild("left_arm").yScale==1 && root.getChild("right_arm").yScale==1
                && state.craftPose.ingredientScale==0 && state.craftPose.resultScale==0,"idle resets arm scale and recipe previews");
        }
    }
}

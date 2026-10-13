package local.peepo.client;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
public final class PeepoModel extends EntityModel<PeepoState> {
    private final ModelPart head,leftArm,rightArm,leftLeg,rightLeg,body,hips,pumpkin,hat,shorts,jug;
    private final ModelPart[] workTorso;
    public PeepoModel(ModelPart root) {
        super(root);head=root.getChild("head");
        body=root.getChild("body");hips=root.getChild("hips");pumpkin=root.getChild("pumpkin");hat=head.getChild("pumpkin_hat");
        shorts=root.getChild("jughead_shorts");jug=head.getChild("jughead_jug");
        leftArm=root.getChild("left_arm");rightArm=root.getChild("right_arm");
        leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");
        workTorso=new ModelPart[]{head,body,hips,pumpkin,shorts};
    }
    public void translateToHand(com.mojang.blaze3d.vertex.PoseStack pose){
        rightArm.translateAndRotate(pose);
        pose.translate(0,2.6F/16F,0);
    }
    public void translateToUprightHand(com.mojang.blaze3d.vertex.PoseStack pose){
        // Follow the animated hand position without inheriting the arm's tilt.
        var grip=new org.joml.Vector3f(0,2.6F*rightArm.yScale/16F,0);
        new org.joml.Quaternionf().rotationZYX(rightArm.zRot,rightArm.yRot,rightArm.xRot).transform(grip);
        pose.translate(rightArm.x/16F+grip.x,rightArm.y/16F+grip.y,rightArm.z/16F+grip.z);
    }
    @Override public void setupAnim(PeepoState s) {
        super.setupAnim(s);
        head.getChild("sleeping_eyes").visible=s.sleeping;
        for (String side : new String[]{"left", "right"}) {
            for (int i=0;i<4;i++) {
                head.getChild(side+"_eye"+i).visible=!s.sleeping;
                head.getChild(side+"_eye_rim"+i).visible=!s.sleeping;
            }
            for (String part : new String[]{"pupil", "shine", "upper_eyelid", "lower_eyelid"})
                head.getChild(side+"_"+part).visible=!s.sleeping;
        }
        boolean jugheadClothes=s.jughead && !s.pumpkin;
        shorts.visible=jugheadClothes;jug.visible=jugheadClothes;
        leftLeg.getChild("left_shorts_cuff").visible=jugheadClothes;
        rightLeg.getChild("right_shorts_cuff").visible=jugheadClothes;
        body.visible=!s.pumpkin;hips.visible=!s.pumpkin;
        pumpkin.visible=s.pumpkin;hat.visible=s.pumpkin;
        leftArm.getChild("left_sleeve").visible=!s.pumpkin && !s.jughead;
        rightArm.getChild("right_sleeve").visible=!s.pumpkin && !s.jughead;
        leftArm.getChild("left_bare_shoulder").visible=s.pumpkin || s.jughead;
        rightArm.getChild("right_bare_shoulder").visible=s.pumpkin || s.jughead;
        if(s.pumpkin) { leftArm.x=3.69F;rightArm.x=-3.69F; }
        head.yRot=Mth.clamp(s.yRot,-40,40)*Mth.DEG_TO_RAD;
        head.xRot=Mth.clamp(s.xRot,-20,20)*Mth.DEG_TO_RAD;
        float walk=s.walkAnimationPos*1.3F, speed=Math.min(s.walkAnimationSpeed,.7F);
        leftLeg.xRot=Mth.cos(walk)*speed;rightLeg.xRot=Mth.cos(walk+Mth.PI)*speed;
        leftArm.xRot=rightLeg.xRot*.65F;rightArm.xRot=leftLeg.xRot*.65F;
        leftArm.zRot=-.06F-Mth.sin(s.ageInTicks*.06F)*.025F;
        rightArm.zRot=.06F+Mth.sin(s.ageInTicks*.06F)*.025F;
        if(s.wheelRunning && !s.eating) {
            // Twelve ticks per stride; bob twice per stride. Position stays on the wheel.
            float phase=s.ageInTicks*Mth.TWO_PI/12F;
            float stride=Mth.sin(phase), bob=(1-Mth.cos(phase*2))*.12F;
            leftLeg.xRot=stride*.85F;rightLeg.xRot=-stride*.85F;
            leftArm.xRot=-.30F-stride*.60F;rightArm.xRot=-.30F+stride*.60F;
            leftArm.zRot=-.10F;rightArm.zRot=.10F;
            head.yRot=0;head.xRot=.10F+Mth.sin(phase*2)*.025F;
            for(ModelPart part:new ModelPart[]{head,body,hips,pumpkin,shorts,leftArm,rightArm,leftLeg,rightLeg})part.y-=bob;
        }
        if(s.eating) {
            float bite=Mth.sin(s.eatingTime*.8F)*.06F;
            leftArm.xRot=rightArm.xRot=-2.05F+bite;
            leftArm.yRot=s.pumpkin ? 1.05F : .8F;rightArm.yRot=-leftArm.yRot;
            leftArm.zRot=rightArm.zRot=0;
            leftArm.z=rightArm.z=s.pumpkin ? -2.2F : -1.5F;
            head.yRot=0;head.xRot=.08F+bite*.5F;
        }
        if(s.sitting && !s.eating) {
            float kick=Mth.sin(s.ageInTicks*.16F)*.30F;
            leftLeg.xRot=-1.05F+kick;rightLeg.xRot=-1.05F-kick;
            leftArm.xRot=rightArm.xRot=-.70F;
            leftArm.yRot=.35F;rightArm.yRot=-.35F;
            leftArm.zRot=rightArm.zRot=0;
            head.xRot=.04F;
        }
        if(s.work!=local.peepo.WorkAnimation.NONE && !s.eating && !s.sleeping && !s.sitting && !s.wheelRunning){
            if(s.work==local.peepo.WorkAnimation.STIR)CompanionWorkPose.stir(s,leftArm,rightArm,head,leftLeg,rightLeg);
            else if(s.work==local.peepo.WorkAnimation.CRANK)CompanionWorkPose.crank(s,leftArm,rightArm,head,leftLeg,rightLeg,workTorso);
            else if(s.work.isPie())CompanionWorkPose.pie(s,leftArm,rightArm,head,leftLeg,rightLeg,workTorso);
            else if(s.work.hasTool() || s.work==local.peepo.WorkAnimation.CHOP || s.work==local.peepo.WorkAnimation.HARVEST || s.work==local.peepo.WorkAnimation.SHEAR)CompanionWorkPose.tool(s,leftArm,rightArm,head,leftLeg,rightLeg,workTorso);
            else CompanionWorkPose.interact(s,leftArm,rightArm,head);
        }
        if(!s.held.isEmpty() && !s.eating && !s.wheelRunning && !s.sleeping && s.work==local.peepo.WorkAnimation.NONE)rightArm.xRot=-.65F;
        if(s.holdingLight && !s.held.isEmpty() && !s.eating && !s.sleeping && s.work==local.peepo.WorkAnimation.NONE){
            rightArm.xRot=-2.55F;rightArm.yRot=0;rightArm.zRot=.15F;
        }
        if(s.social!=local.peepo.CompanionSocial.NONE && !s.eating && !s.sleeping && !s.sitting && !s.wheelRunning && s.work==local.peepo.WorkAnimation.NONE){
            // The free left hand waves; held tools/lights stay in the existing right-hand pose.
            float t=s.socialTime;
            if(s.social==local.peepo.CompanionSocial.WAVE){
                float fade=Math.min(1,Math.min(t/5,(40-t)/5));fade=Math.max(0,fade);
                leftArm.xRot=-.35F*fade;leftArm.yRot=.2F*fade;
                leftArm.zRot=Mth.lerp(fade,leftArm.zRot,-2.15F+Mth.sin(t*.65F)*.35F);
                head.xRot+=Mth.sin(t*.18F)*.045F*fade;
            }else if(s.social==local.peepo.CompanionSocial.TALK){
                float gesture=Mth.sin(t*.3F),fade=Math.min(1,Math.min(t/4,(30-t)/4));fade=Math.max(0,fade);
                leftArm.xRot=(-.8F+gesture*.2F)*fade;leftArm.yRot=.3F*fade;leftArm.zRot=-.25F*fade;
                if(s.held.isEmpty()){rightArm.xRot=(-.6F-gesture*.18F)*fade;rightArm.yRot=-.25F*fade;rightArm.zRot=.2F*fade;}
                head.xRot+=Mth.sin(t*.28F)*.07F;
            }else head.xRot+=Mth.sin(t*.16F)*.045F;
        }
        if(s.sleeping) {
            head.xRot=head.yRot=0;
            leftLeg.xRot=rightLeg.xRot=0;
            leftArm.xRot=rightArm.xRot=0;
            leftArm.zRot=-.08F;rightArm.zRot=.08F;
        }
    }
}

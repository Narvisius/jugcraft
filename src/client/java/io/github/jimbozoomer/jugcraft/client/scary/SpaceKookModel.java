package io.github.jimbozoomer.jugcraft.client.scary;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
public final class SpaceKookModel extends EntityModel<SpaceKookState> {
    private final ModelPart head, leftArm, rightArm, leftLeg, rightLeg;
    public SpaceKookModel(ModelPart root) {
        super(root); head=root.getChild("head");
        leftArm=root.getChild("left_arm"); rightArm=root.getChild("right_arm");
        leftLeg=root.getChild("left_leg"); rightLeg=root.getChild("right_leg");
    }
    @Override public void setupAnim(SpaceKookState s) {
        super.setupAnim(s);
        head.yRot=Mth.clamp(s.yRot,-40,40)*Mth.DEG_TO_RAD;
        head.xRot=Mth.clamp(s.xRot,-18,22)*Mth.DEG_TO_RAD;
        float walk=s.walkAnimationPos*.6F, speed=Math.min(s.walkAnimationSpeed,.7F);
        leftLeg.xRot=Mth.cos(walk)*speed*.8F; rightLeg.xRot=-leftLeg.xRot;
        leftArm.xRot=-leftLeg.xRot*.65F; rightArm.xRot=-rightLeg.xRot*.65F;
        leftArm.zRot=-.09F-Mth.sin(s.ageInTicks*.07F)*.025F; rightArm.zRot=-leftArm.zRot;
        if(s.chasing) { leftArm.xRot-=.45F; rightArm.xRot-=.45F; }
        float swing=Mth.sin(Mth.sqrt(s.attackTime)*Mth.PI*2)*.9F;
        rightArm.xRot-=swing;
    }
}

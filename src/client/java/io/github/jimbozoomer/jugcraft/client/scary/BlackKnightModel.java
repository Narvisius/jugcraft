package io.github.jimbozoomer.jugcraft.client.scary;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
public final class BlackKnightModel extends EntityModel<SpaceKookState> {
    private final ModelPart head,left,right,ll,rl;
    public BlackKnightModel(ModelPart root) {super(root);head=root.getChild("head");left=root.getChild("left_arm");right=root.getChild("right_arm");ll=root.getChild("left_leg");rl=root.getChild("right_leg");}
    @Override public void setupAnim(SpaceKookState s) {
        super.setupAnim(s);head.yRot=Mth.clamp(s.yRot,-35,35)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(s.xRot,-15,20)*Mth.DEG_TO_RAD;
        float walk=Mth.cos(s.walkAnimationPos*.6F)*Math.min(.6F,s.walkAnimationSpeed);
        ll.xRot=walk;rl.xRot=-walk;left.xRot=-walk*.6F;left.zRot=-.08F;
        right.xRot=(s.chasing?.1F:.45F)+walk*.15F;right.zRot=.06F;
        right.xRot-=Mth.sin(s.attackTime*Mth.PI)*1.8F;
    }
}

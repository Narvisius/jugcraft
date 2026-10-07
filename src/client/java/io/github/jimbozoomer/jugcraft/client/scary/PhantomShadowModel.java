package io.github.jimbozoomer.jugcraft.client.scary;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
public final class PhantomShadowModel extends EntityModel<SpaceKookState> {
    private final ModelPart head,body,left,right,lc,rc;
    public PhantomShadowModel(ModelPart root) {
        super(root);head=root.getChild("head");body=root.getChild("body");left=root.getChild("left_arm");right=root.getChild("right_arm");
        lc=root.getChild("left_chain");rc=root.getChild("right_chain");
    }
    @Override public void setupAnim(SpaceKookState s) {
        super.setupAnim(s);
        float wave=Mth.sin(s.ageInTicks*.055F);
        head.yRot=Mth.clamp(s.yRot,-25,25)*Mth.DEG_TO_RAD;
        head.xRot=Mth.clamp(s.xRot,-12,12)*Mth.DEG_TO_RAD;
        body.zRot=wave*.025F;left.zRot=wave*.035F;right.zRot=-wave*.035F;
        lc.xRot=Mth.sin(s.ageInTicks*.07F)*.13F;rc.xRot=Mth.sin(s.ageInTicks*.07F+1)*.13F;
        lc.zRot=.07F+wave*.07F;rc.zRot=-.07F-wave*.07F;
        if(s.chasing) {left.xRot=-.10F;right.xRot=-.10F;}
    }
}

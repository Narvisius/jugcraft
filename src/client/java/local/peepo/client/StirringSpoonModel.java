package local.peepo.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

/** Wooden spoon and both hands use the same tilted stirring pose. */
final class StirringSpoonModel extends EntityModel<PeepoState> {
    StirringSpoonModel(ModelPart root){super(root);}
    @Override public void setupAnim(PeepoState state){
        super.setupAnim(state);
        var spoon=root().getChild("spoon");
        var p=state.toolPose;
        spoon.x=p.x;spoon.y=p.y;spoon.z=p.z;spoon.xRot=p.xRot;spoon.zRot=p.zRot;
    }
    static LayerDefinition create(){
        var mesh=new MeshDefinition();
        var spoon=mesh.getRoot().addOrReplaceChild("spoon",CubeListBuilder.create()
            .texOffs(0,0).addBox(-.32F,-.5F,-.32F,.64F,6.2F,.64F),
            PartPose.offsetAndRotation(0,20,-3.4F,-.2618F,0,0));
        spoon.addOrReplaceChild("bowl",CubeListBuilder.create().texOffs(4,0)
            .addBox(-.9F,5.2F,-.36F,1.8F,1.6F,.72F),PartPose.ZERO);
        spoon.addOrReplaceChild("bowl_tip",CubeListBuilder.create().texOffs(6,5)
            .addBox(-.60F,6.65F,-.26F,1.2F,.65F,.52F),PartPose.ZERO);
        return LayerDefinition.create(mesh,16,16);
    }
}

package local.peepo.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

/** Closed wooden paddle/spoon, leaning 15 degrees into the pot from the shared hand grip. */
final class StirringSpoonModel extends EntityModel<PeepoState> {
    StirringSpoonModel(ModelPart root){super(root);}
    @Override public void setupAnim(PeepoState state){
        super.setupAnim(state);
        var spoon=root().getChild("spoon");
        spoon.x=CompanionWorkPose.gripX(state);
        spoon.z=CompanionWorkPose.gripZ(state);
    }
    static LayerDefinition create(){
        var mesh=new MeshDefinition();
        var spoon=mesh.getRoot().addOrReplaceChild("spoon",CubeListBuilder.create()
            .texOffs(0,0).addBox(-.32F,-.5F,-.32F,.64F,6.2F,.64F),
            PartPose.offsetAndRotation(0,CompanionWorkPose.GRIP_Y,CompanionWorkPose.GRIP_Z,-.2618F,0,0));
        spoon.addOrReplaceChild("bowl",CubeListBuilder.create().texOffs(4,0)
            .addBox(-.9F,5.2F,-.36F,1.8F,1.6F,.72F),PartPose.ZERO);
        spoon.addOrReplaceChild("bowl_tip",CubeListBuilder.create().texOffs(6,5)
            .addBox(-.60F,6.65F,-.26F,1.2F,.65F,.52F),PartPose.ZERO);
        return LayerDefinition.create(mesh,16,16);
    }
}

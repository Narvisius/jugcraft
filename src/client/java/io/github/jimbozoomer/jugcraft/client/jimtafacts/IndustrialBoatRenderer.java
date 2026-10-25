package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialBoats;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.BoatRenderState;

/** Source RenderIC2Boat delegates to the classic 64×32 vanilla hull, not the modern paddle atlas. */
public final class IndustrialBoatRenderer extends AbstractBoatRenderer {
    private final EntityModel<BoatRenderState> hull=new HullModel(layer().bakeRoot());
    public IndustrialBoatRenderer(EntityRendererProvider.Context context,String material){super(context,Jugcraft.id("textures/entity/jimtafacts/boat_"+material+".png"));}
    public static void register(){IndustrialBoats.TYPES.forEach((material,type)->EntityRendererRegistry.register(type,ctx->new IndustrialBoatRenderer(ctx,material)));}
    @Override protected EntityModel<BoatRenderState> model(){return hull;}
    static final class HullModel extends EntityModel<BoatRenderState>{HullModel(ModelPart root){super(root);}}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();float half=(float)Math.PI/2;
        root.addOrReplaceChild("bottom",CubeListBuilder.create().texOffs(0,8).addBox(-12,-8,-3,24,16,4),PartPose.offsetAndRotation(0,4,0,half,0,0));
        root.addOrReplaceChild("left",CubeListBuilder.create().texOffs(0,0).addBox(-10,-7,-1,20,6,2),PartPose.offsetAndRotation(-11,4,0,0,3*half,0));
        root.addOrReplaceChild("right",CubeListBuilder.create().texOffs(0,0).addBox(-10,-7,-1,20,6,2),PartPose.offsetAndRotation(11,4,0,0,half,0));
        root.addOrReplaceChild("bow",CubeListBuilder.create().texOffs(0,0).addBox(-12,-7,-1,24,6,2),PartPose.offsetAndRotation(0,4,-9,0,2*half,0));
        root.addOrReplaceChild("stern",CubeListBuilder.create().texOffs(0,0).addBox(-12,-7,-1,24,6,2),PartPose.offset(0,4,9));
        return LayerDefinition.create(mesh,64,32);
    }
}

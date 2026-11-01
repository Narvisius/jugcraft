package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;

/** Original BuntingBlockTileRenderer mesh, UVs and directional sway equation. */
final class OwnerBuntingRenderer {
    private final ModelPart mesh,flag;
    OwnerBuntingRenderer(){
        var definition=new MeshDefinition();var root=definition.getRoot();
        root.addOrReplaceChild("flag",CubeListBuilder.create().texOffs(0,0).addBox(-3.5F,0,0,7,11,0),PartPose.offsetAndRotation(0,-2,0,0,-1.5708F,0));
        root.addOrReplaceChild("box",CubeListBuilder.create().texOffs(0,12).addBox(-4,-1,-1,8,2,2),PartPose.offsetAndRotation(0,-3,0,0,-1.5708F,0));
        mesh=LayerDefinition.create(definition,32,16).bakeRoot();flag=mesh.getChild("flag");mesh.getChild("box").yScale=1.1F;mesh.getChild("box").zScale=1.1F;
    }
    void draw(OwnerSupplementRenderer.State s,PoseStack pose,SubmitNodeCollector out){
        for(var direction:Direction.Plane.HORIZONTAL){int color=s.buntings[direction.get2DDataValue()];if(color<0)continue;
            float phase=(s.time+s.blockPos.getX()*7+s.blockPos.getY()*9+s.blockPos.getZ()*13+(direction.getStepX()*7+direction.getStepZ()*13)*.25F)/100;
            flag.zRot=(direction.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:-1)*.01F*(float)Math.cos(phase*Math.PI*2)*(float)Math.PI;
            pose.pushPose();pose.translate(.5,.5,.5);pose.rotateDegrees(Axis.YP,180-direction.toYRot());pose.translate(0,0,-.25);pose.scale(1,-1,-1);
            out.submitModelPart(mesh,pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/supps/block/buntings/bunting_"+DyeColor.byId(color).getName()+".png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null);
            pose.popPose();
        }
    }
}

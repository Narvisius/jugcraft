package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.serialization.MapCodec;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.*;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Source numeric head/orb cubes, original UVs, and the original floating/pulsing timing. */
public final class JimtaPyriumRenderer implements NoDataSpecialModelRenderer {
    private final Form head=new Form(head().bakeRoot()),orb=new Form(orb().bakeRoot());
    public static void register(){SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("jimta_pyrium"),Unbaked.CODEC);}
    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> CODEC=MapCodec.unit(new Unbaked());
        @Override public MapCodec<Unbaked> type(){return CODEC;}
        @Override public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context){return new JimtaPyriumRenderer();}
    }
    private static final class Form extends Model<Void>{Form(ModelPart root){super(root,RenderTypes::entityCutout);}}
    @Override public void submit(PoseStack pose,SubmitNodeCollector out,int light,int overlay,boolean foil,int outline){
        pose.pushPose();pose.translate(.5,.5,.5);pose.rotateDegrees(Axis.ZP,135);pose.rotateDegrees(Axis.YP,-90);pose.translate(0,-.3984375,0);pose.scale(.5F,.5F,.5F);
        out.order(0).submitModel(head,null,pose,RenderTypes.entityCutout(Jugcraft.id("textures/jimtafacts/spellbooks/item/pyrium_staff_head.png")),light,overlay,-1,null,outline);
        float f=Minecraft.getInstance().player==null?0:(Minecraft.getInstance().player.tickCount+Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true))*.75F;
        pose.translate(0,-.296875+Math.sin(f*.3)/32,0);float scale=(float)((Math.sin(f*.5)+Math.sin(f*3))/2*.04+1);pose.scale(scale,scale,scale);
        out.order(1).submitModel(orb,null,pose,RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/jimtafacts/spellbooks/item/pyrium_staff_orb.png")),0xf000f0,overlay,-1,null,outline);pose.popPose();
    }
    @Override public void getExtents(Consumer<Vector3fc> out){out.accept(new Vector3f(-.5F,-.5F,-.5F));out.accept(new Vector3f(1.5F,1.5F,1.5F));}
    private static LayerDefinition head(){var mesh=new MeshDefinition();var base=mesh.getRoot().addOrReplaceChild("bb_main",CubeListBuilder.create().texOffs(0,0).addBox(-1,-1,-1.5F,2,1,3),PartPose.ZERO);base.addOrReplaceChild("forespike_r1",CubeListBuilder.create().texOffs(13,10).addBox(0,-9,0,0,13,5),PartPose.offsetAndRotation(0,0,0,0,1.0472F,0));base.addOrReplaceChild("forespike_r2",CubeListBuilder.create().texOffs(13,10).addBox(0,-9,0,0,13,5),PartPose.offsetAndRotation(0,0,0,0,-1.0472F,0));base.addOrReplaceChild("forespike_r3",CubeListBuilder.create().texOffs(13,10).addBox(0,-9,0,0,13,5),PartPose.offsetAndRotation(0,0,-.5F,0,3.1416F,0));return LayerDefinition.create(mesh,32,32);}
    private static LayerDefinition orb(){var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("bb_main",CubeListBuilder.create().texOffs(12,16).addBox(1.5F,1.5F,1.5F,-3,-3,-3).texOffs(8,8).addBox(1,1,1,-2,-2,-2).texOffs(4,3).addBox(.5F,.5F,.5F,-1,-1,-1),PartPose.ZERO);return LayerDefinition.create(mesh,16,16);}
}

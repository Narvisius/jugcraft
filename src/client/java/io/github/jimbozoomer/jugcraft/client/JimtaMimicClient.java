package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.JimtaMimic;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.resources.Identifier;
/** Geometry, UVs and airborne lid/bottom animation recovered unchanged from supplied MimicModel.class. */
public final class JimtaMimicClient {
    private static final ModelLayerLocation INSIDE=new ModelLayerLocation(Jugcraft.id("jimta_mimic"),"main");
    private static final ModelLayerLocation CHEST=new ModelLayerLocation(Jugcraft.id("jimta_mimic"),"chest");
    public static void register(){ModelLayerRegistry.registerModelLayer(INSIDE,Model::inside);ModelLayerRegistry.registerModelLayer(CHEST,Model::chest);EntityRendererRegistry.register(JimtaMimic.TYPE,Renderer::new);}
    public static final class State extends LivingEntityRenderState {public float air;}
    public static final class Model extends EntityModel<State>{
        private final ModelPart bottom,lid;
        Model(ModelPart root){super(root);bottom=root.getChild("bottom");lid=root.getChild("lid");}
        @Override public void setupAnim(State s){super.setupAnim(s);lid.xRot=Math.max(-60,s.air*-6)*.0174533F;bottom.xRot=Math.min(30,s.air*3)*.0174533F;}
        static LayerDefinition inside(){
            var mesh=new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("bottom",CubeListBuilder.create().texOffs(0,15).addBox(-6,-4,-13,12,3,12).texOffs(36,15).addBox(-6,-1,-13,12,0,12,new CubeDeformation(.02F)),PartPose.offset(0,15,7));
            mesh.getRoot().addOrReplaceChild("lid",CubeListBuilder.create().texOffs(0,0).addBox(-6,0,-13,12,3,12).texOffs(24,0).addBox(-6,0,-13,12,0,12,new CubeDeformation(.02F)),PartPose.offset(0,15,7));
            return LayerDefinition.create(mesh,64,32);
        }
        static LayerDefinition chest(){
            var mesh=new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("bottom",CubeListBuilder.create().texOffs(0,19).addBox(1,-9,0,14,10,14),PartPose.offset(0,9,1));
            mesh.getRoot().addOrReplaceChild("lid",CubeListBuilder.create().texOffs(0,0).addBox(1,0,0,14,5,14).texOffs(0,0).addBox(7,-2,14,2,4,1),PartPose.offset(0,9,1));
            return LayerDefinition.create(mesh,64,64);
        }
    }
    private static final class Renderer extends MobRenderer<JimtaMimic,State,Model>{
        Renderer(EntityRendererProvider.Context context){super(context,new Model(context.bakeLayer(INSIDE)),.45F);addLayer(new Shell(this,context));}
        @Override public State createRenderState(){return new State();}
        @Override public Identifier getTextureLocation(State s){return Jugcraft.id("textures/entity/jimtafacts/mimic.png");}
        @Override public void extractRenderState(JimtaMimic e,State s,float partial){super.extractRenderState(e,s,partial);s.air=e.ticksInAir>0?e.ticksInAir-1+partial:0;}
    }
    private static final class Shell extends RenderLayer<State,Model>{
        private final Model model;private final SpriteGetter sprites;
        Shell(RenderLayerParent<State,Model> parent,EntityRendererProvider.Context context){super(parent);model=new Model(context.bakeLayer(CHEST));sprites=context.getSprites();}
        @Override public void submit(PoseStack poses,SubmitNodeCollector collector,int light,State s,float yaw,float pitch){
            if(s.isInvisible)return;poses.pushPose();poses.rotateDegrees(Axis.XP,180);poses.translate(-.5,-1.5,-.5);model.setupAnim(s);
            var material=net.minecraft.util.SpecialDates.isExtendedChristmas()?Sheets.CHEST_CHRISTMAS.single():Sheets.CHEST_REGULAR.single();
            collector.submitModel(model,s,poses,material.renderType(RenderTypes::entityCutout),light,LivingEntityRenderer.getOverlayCoords(s,0),-1,sprites.get(material),s.outlineColor);
            poses.popPose();
        }
    }
}

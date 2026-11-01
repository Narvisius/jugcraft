package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerPlunderer;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
/** Supplied plunderer geometry/UVs, native walking and sword attack, and spyglass pose. */
final class OwnerPlundererRenderer extends IllagerRenderer<OwnerPlunderer,OwnerPlundererRenderer.State> {
 OwnerPlundererRenderer(EntityRendererProvider.Context c){super(c,new Model(),.5F);addLayer(new ItemInHandLayer<>(this));}
 static final class State extends IllagerRenderState {boolean spy;}
 @Override public State createRenderState(){return new State();}
 @Override public void extractRenderState(OwnerPlunderer e,State s,float partial){super.extractRenderState(e,s,partial);s.spy=e.usingSpyglass();}
 @Override public net.minecraft.resources.Identifier getTextureLocation(State s){return Jugcraft.id("textures/newest/supps/entity/plunderer.png");}
 private static final class Model extends IllagerModel<State> {
  private final ModelPart skirt,left,right;
  Model(){super(createMesh().bakeRoot());getHat().visible=true;skirt=root().getChild("body").getChild("skirt");left=root().getChild("left_arm");right=root().getChild("right_arm");}
  @Override public void setupAnim(State s){super.setupAnim(s);getHat().visible=true;skirt.visible=!s.isRiding;if(s.spy){root().getChild("arms").visible=false;left.visible=right.visible=true;var arm=s.mainArm==net.minecraft.world.entity.HumanoidArm.RIGHT?left:right;arm.xRot=net.minecraft.util.Mth.clamp(getHead().xRot-1.9198622F,-2.4F,3.3F);arm.yRot=getHead().yRot+(arm==left?.2617994F:-.2617994F);}}
    public static LayerDefinition createMesh() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        PartDefinition partDefinition2 = partDefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -10.0f, -4.0f, 8.0f, 10.0f, 8.0f), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        partDefinition2.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, -10.0f, -4.0f, 8.0f, 10.0f, 8.0f, new CubeDeformation(0.45f)).texOffs(0, 72).addBox(-5.0f, -11.0f, -4.0f, 10.0f, 4.0f, 8.0f, new CubeDeformation(0.5f)), PartPose.ZERO);
        partDefinition2.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0f, -1.0f, -6.0f, 2.0f, 4.0f, 2.0f), PartPose.offset((float)0.0f, (float)-2.0f, (float)0.0f));
        PartDefinition bodyPart = partDefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 22).addBox(-4.0f, 0.0f, -3.0f, 8.0f, 12.0f, 6.0f).texOffs(0, 40).addBox(-4.0f, 0.0f, -3.0f, 8.0f, 14.0f, 6.0f, new CubeDeformation(0.5f)), PartPose.ZERO);
        bodyPart.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 60).addBox(-4.0f, 15.0f, -3.0f, 8.0f, 6.0f, 6.0f, new CubeDeformation(0.5f)), PartPose.ZERO);
        PartDefinition partDefinition3 = partDefinition.addOrReplaceChild("arms", CubeListBuilder.create().texOffs(44, 22).addBox(-8.0f, -2.0f, -2.0f, 4.0f, 8.0f, 4.0f).texOffs(40, 38).addBox(-4.0f, 2.0f, -2.0f, 8.0f, 4.0f, 4.0f), PartPose.offsetAndRotation((float)0.0f, (float)3.0f, (float)-1.0f, (float)-0.75f, (float)0.0f, (float)0.0f));
        partDefinition3.addOrReplaceChild("left_shoulder", CubeListBuilder.create().texOffs(44, 22).mirror().addBox(4.0f, -2.0f, -2.0f, 4.0f, 8.0f, 4.0f), PartPose.ZERO);
        partDefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 20).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f), PartPose.offset((float)-2.0f, (float)12.0f, (float)0.0f));
        partDefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 20).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f), PartPose.offset((float)2.0f, (float)12.0f, (float)0.0f));
        partDefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 46).addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f), PartPose.offset((float)-5.0f, (float)2.0f, (float)0.0f));
        partDefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 46).mirror().addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f), PartPose.offset((float)5.0f, (float)2.0f, (float)0.0f));
        return LayerDefinition.create((MeshDefinition)meshDefinition, (int)64, (int)128);
    }

 }
}

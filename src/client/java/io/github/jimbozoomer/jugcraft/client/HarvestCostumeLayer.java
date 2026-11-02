package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestFestival;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Cosmetic geometry only: never replaces a citizen's skin, armor, job or saved appearance. */
public final class HarvestCostumeLayer<S extends HumanoidRenderState,M extends HumanoidModel<S>> extends RenderLayer<S,M>{
    private static final net.minecraft.client.renderer.rendertype.RenderType MATERIAL=RenderTypes.entityCutout(Identifier.withDefaultNamespace("textures/block/white_concrete.png"));
    private record Piece(String part,float x,float y,float z,float X,float Y,float Z,int color){}
    private static final java.util.List<Piece> COAT=pilgrim(false),DRESS=pilgrim(true);
    private static final java.util.List<java.util.List<Piece>> RAIDERS=java.util.List.of(raider(0),raider(1),raider(2));
    public HarvestCostumeLayer(RenderLayerParent<S,M> parent){super(parent);}
    @SuppressWarnings({"rawtypes","unchecked"}) public static void register(){
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type,renderer,helper,context)->{
            if(renderer.getModel() instanceof HumanoidModel<?>)helper.register(new HarvestCostumeLayer((RenderLayerParent)renderer));
        });
    }
    @Override public void submit(PoseStack pose,SubmitNodeCollector out,int light,S state,float yRot,float xRot){
        java.util.List<Piece> pieces=null;
        if(state.headEquipment.is(HarvestFestival.PILGRIM_COAT))pieces=COAT;
        else if(state.headEquipment.is(HarvestFestival.PILGRIM_DRESS))pieces=DRESS;
        else if(HarvestFestival.clientCostumes()){
            if(state instanceof TownsfolkRenderer.State town && town.seasonalTownResident)pieces=((int)town.phase%2)==0?COAT:DRESS;
            else if(state instanceof RaiderRenderer.State raider)pieces=RAIDERS.get(raider.role.ordinal()%3);
        }
        if(pieces==null||state.isInvisible)return;
        for(var p:pieces){
            ModelPart part=switch(p.part){case "head"->getParentModel().head;case "body"->getParentModel().body;case "left_arm"->getParentModel().leftArm;case "right_arm"->getParentModel().rightArm;case "left_leg"->getParentModel().leftLeg;default->getParentModel().rightLeg;};
            if(!part.visible)continue;
            pose.pushPose();part.translateAndRotate(pose);
            out.submitCustomGeometry(pose,MATERIAL,(matrix,buffer)->TintedBoxes.box(buffer,matrix,p.x,p.y,p.z,p.X,p.Y,p.Z,p.color,light));
            pose.popPose();
        }
    }
    private static void box(java.util.List<Piece> a,String part,float x,float y,float z,float X,float Y,float Z,int color){a.add(new Piece(part,x,y,z,X,Y,Z,color));}
    private static java.util.List<Piece> pilgrim(boolean dress){
        var a=new java.util.ArrayList<Piece>();int cloth=dress?0xFF504239:0xFF30313A,white=0xFFF2E4CD,gold=0xFFC9A452;
        box(a,"body",-4.15F,0,-2.15F,4.15F,12,2.15F,cloth);
        box(a,"body",-4.2F,.1F,-2.3F,4.2F,2.4F,-2.16F,white);
        box(a,"body",-3.3F,2.4F,-2.31F,-.4F,3.6F,-2.16F,white);box(a,"body",.4F,2.4F,-2.31F,3.3F,3.6F,-2.16F,white);
        box(a,"body",-4.2F,8.5F,-2.3F,4.2F,10,2.3F,0xFF35251D);box(a,"body",-1,8.5F,-2.45F,1,10,-2.3F,gold);
        for(String limb:java.util.List.of("left_arm","right_arm")){
            float x=limb.equals("left_arm")?-1.15F:-3.15F;
            box(a,limb,x,-2.1F,-2.15F,x+4.3F,8,2.15F,cloth);box(a,limb,x-.05F,7,-2.2F,x+4.35F,9,2.2F,white);
        }
        for(String leg:java.util.List.of("left_leg","right_leg")){
            box(a,leg,-2.1F,0,-2.1F,2.1F,8,2.1F,cloth);box(a,leg,-2.15F,7,-2.15F,2.15F,10,2.15F,white);
            box(a,leg,-2.2F,10,-2.5F,2.2F,12.1F,2.2F,0xFF29262A);box(a,leg,-.7F,10.4F,-2.6F,.7F,11.5F,-2.5F,gold);
        }
        if(dress){
            box(a,"body",-4.6F,10,-2.7F,4.6F,18,2.7F,cloth);box(a,"body",-3.2F,10.1F,-2.85F,3.2F,17.8F,-2.7F,white);
            box(a,"head",-4.4F,-8.5F,-3.8F,4.4F,-7.7F,4.5F,white);
            box(a,"head",-4.4F,-7.7F,-3.8F,-3.9F,-.4F,4.5F,white);box(a,"head",3.9F,-7.7F,-3.8F,4.4F,-.4F,4.5F,white);
            box(a,"head",-4.4F,-7.7F,4,4.4F,-.4F,4.5F,white);
        }else{
            box(a,"head",-5.2F,-8.6F,-5.2F,5.2F,-8,5.2F,cloth);box(a,"head",-3.6F,-13,-3.6F,3.6F,-8.6F,3.6F,cloth);
            box(a,"head",-3.65F,-10,-3.65F,3.65F,-8.7F,3.65F,0xFF815B35);box(a,"head",-1,-10,-3.8F,1,-8.7F,-3.65F,gold);
        }
        return java.util.List.copyOf(a);
    }
    private static java.util.List<Piece> raider(int variant){
        var a=new java.util.ArrayList<Piece>();int leather=variant==1?0xFF956542:0xFFB98853,red=0xFF9E352D,blue=0xFF387B88,bone=0xFFE4D2A4;
        // Clothing layers preserve the existing head/face; paint is a separate thin overlay.
        box(a,"body",-4.15F,0,-2.15F,4.15F,12,2.15F,leather);
        box(a,"body",-4.2F,9,-2.3F,4.2F,10.5F,2.3F,0xFF563E2C);
        box(a,"body",-2,10.5F,-2.45F,2,17,-2.2F,leather);
        box(a,"body",-3.8F,.2F,-2.35F,3.8F,1.1F,-2.18F,bone);
        for(int i=0;i<4;i++){box(a,"body",-3+i*1.7F,3+i%2,-2.3F,-2.2F+i*1.7F,6+i%2,-2.16F,i%2==0?red:blue);}
        for(String leg:java.util.List.of("left_leg","right_leg")){
            box(a,leg,-2.1F,0,-2.1F,2.1F,10,2.1F,leather);box(a,leg,-2.2F,10,-2.5F,2.2F,12.1F,2.2F,0xFF704A31);
            box(a,leg,-2.25F,8,-2.25F,2.25F,8.6F,2.25F,blue);
        }
        for(String arm:java.util.List.of("left_arm","right_arm")){
            float x=arm.equals("left_arm")?-1.1F:-3.1F;
            box(a,arm,x,-2,-2.1F,x+4.2F,8,2.1F,leather);box(a,arm,x-.1F,3,-2.2F,x+4.3F,4,2.2F,bone);
        }
        // Costume hair/headwear covers the uniform cap while leaving the existing face visible.
        int hair=0xFF35271F;
        box(a,"head",-4.35F,-8.35F,-4.35F,4.35F,-6.8F,4.35F,hair);
        box(a,"head",-4.3F,-6.8F,4.05F,4.3F,1.8F,4.4F,hair);
        box(a,"head",-4.35F,-6.8F,-2.7F,-4.02F,1.2F,4.2F,hair);
        box(a,"head",4.02F,-6.8F,-2.7F,4.35F,1.2F,4.2F,hair);
        box(a,"head",-4.2F,-6.8F,-4.2F,4.2F,-5.8F,4.2F,blue);
        for(int i=0;i<7;i++)box(a,"head",-3.7F+i,-6.75F,-4.3F,-3.2F+i,-5.85F,-4.2F,i%2==0?bone:red);
        box(a,"head",-3.6F,-3.5F,-4.08F,-1.5F,-3,-4.01F,red);box(a,"head",1.5F,-3.5F,-4.08F,3.6F,-3,-4.01F,red);
        int count=variant==2?7:2;
        for(int i=0;i<count;i++){
            float x=variant==2?-4+i*1.2F:3+i*.6F;
            float top=-12-(variant==2?3-Math.abs(i-3):i);
            box(a,"head",x,top,3.3F,x+.9F,-6.8F,3.7F,bone);box(a,"head",x,top,3.25F,x+.9F,top+1.6F,3.75F,red);
        }
        return java.util.List.copyOf(a);
    }
}

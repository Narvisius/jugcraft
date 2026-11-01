package io.github.jimbozoomer.jugcraft.client.aquatic;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.aquatic.OwnerAquaculture;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Supplied seven shapes, original swimming/flopping animations and species skins. */
public final class OwnerFishRenderer extends MobRenderer<OwnerAquaculture.Fish,AquaFishRenderState,FishBaseModel> {
 public OwnerFishRenderer(EntityRendererProvider.Context context,OwnerAquaculture.Shape shape){super(context,mesh(shape),.35f);}
 private static FishBaseModel mesh(OwnerAquaculture.Shape shape){return switch(shape){
  case SMALL->new FishSmallModel(FishSmallModel.createBodyLayer().bakeRoot());
  case LARGE->new FishLargeModel(FishLargeModel.createBodyLayer().bakeRoot());
  case LONGNOSE->new FishLongnoseModel(FishLongnoseModel.createBodyLayer().bakeRoot());
  case CATFISH->new FishCathfishModel(FishCathfishModel.createBodyLayer().bakeRoot());
  case JELLYFISH->new JellyfishModel(JellyfishModel.createBodyLayer().bakeRoot());
  case HALIBUT->new AquaTropicalFishBModel(AquaTropicalFishBModel.createBodyLayer(CubeDeformation.NONE).bakeRoot());
  default->new FishMediumModel(FishMediumModel.createBodyLayer().bakeRoot());};}
 @Override public AquaFishRenderState createRenderState(){return new AquaFishRenderState();}
 @Override public Identifier getTextureLocation(AquaFishRenderState state){return Jugcraft.id("textures/newest/aquatic/entity/fish/"+state.name+".png");}
 @Override public void extractRenderState(OwnerAquaculture.Fish fish,AquaFishRenderState state,float partial){super.extractRenderState(fish,state,partial);state.name=fish.species.name();state.fishType=fish.species.shape();}
 @Override protected RenderType getRenderType(AquaFishRenderState state,boolean visible,boolean translucent,boolean glowing){return state.fishType==OwnerAquaculture.Shape.JELLYFISH&&visible?RenderTypes.entityTranslucent(getTextureLocation(state)):super.getRenderType(state,visible,translucent,glowing);}
 @Override protected void setupRotations(AquaFishRenderState s,PoseStack pose,float bodyRot,float scale){
  super.setupRotations(s,pose,bodyRot,scale);if(s.fishType==OwnerAquaculture.Shape.JELLYFISH)return;
  boolean longnose=s.fishType==OwnerAquaculture.Shape.LONGNOSE;
  float amount=longnose&&!s.isInWater?1.3f:1,frequency=longnose&&!s.isInWater?1.7f:1;
  pose.rotateDegrees(Axis.YP,amount*4.3f*Mth.sin(frequency*.6f*s.ageInTicks));
  if(longnose)pose.translate(0,0,-.4);
  if(s.fishType==OwnerAquaculture.Shape.HALIBUT){pose.translate(-.4,.1,0);pose.rotateDegrees(Axis.ZP,-90);}
  else if(!s.isInWater){pose.translate(.1,.1,-.1);pose.rotateDegrees(Axis.ZP,90);}
 }
 @Override protected void scale(AquaFishRenderState state,PoseStack pose){
  float size=switch(state.name){case "minnow"->.5f;case "synodontis"->.8f;case "brown_trout","piranha"->.9f;case "pollock"->1.1f;case "atlantic_cod","blackfish","catfish","tambaqui"->1.2f;case "pacific_halibut","atlantic_halibut","capitaine","largemouth_bass","gar","arapaima","tuna"->1.4f;default->1;};
  pose.scale(size,size,size);
 }
}

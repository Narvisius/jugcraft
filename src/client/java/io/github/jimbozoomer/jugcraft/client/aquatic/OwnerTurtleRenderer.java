package io.github.jimbozoomer.jugcraft.client.aquatic;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.aquatic.OwnerLandTurtles;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
public final class OwnerTurtleRenderer extends AgeableMobRenderer<OwnerLandTurtles.Turtle,LivingEntityRenderState,TurtleLandModel> {
    private final Identifier texture;
    public OwnerTurtleRenderer(EntityRendererProvider.Context context,String name){super(context,new TurtleLandModel(TurtleLandModel.createBodyLayer().bakeRoot()),new TurtleLandModel(TurtleLandModel.createBodyLayer().apply(TurtleLandModel.BABY_TRANSFORMER).bakeRoot()),.25f);texture=Jugcraft.id("textures/newest/aquatic/entity/turtle/"+name+".png");}
    @Override public LivingEntityRenderState createRenderState(){return new LivingEntityRenderState();}
    @Override public Identifier getTextureLocation(LivingEntityRenderState state){return texture;}
}

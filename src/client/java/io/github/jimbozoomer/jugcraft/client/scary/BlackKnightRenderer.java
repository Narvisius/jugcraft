package io.github.jimbozoomer.jugcraft.client.scary;
import io.github.jimbozoomer.jugcraft.creatures.scary.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.*;
public final class BlackKnightRenderer extends MobRenderer<BlackKnightEntity,SpaceKookState,BlackKnightModel> {
    public BlackKnightRenderer(EntityRendererProvider.Context context) {
        super(context,new BlackKnightModel(context.bakeLayer(ScaryClient.KNIGHT)),.45F);
        addLayer(new EyesLayer<SpaceKookState,BlackKnightModel>(this) {
            @Override public RenderType renderType() {return RenderTypes.eyes(ScaryMod.id("textures/entity/scary/black_knight_glow.png"));}
        });
    }
    @Override public SpaceKookState createRenderState() {return new SpaceKookState();}
    @Override public Identifier getTextureLocation(SpaceKookState s) {return ScaryMod.id("textures/entity/scary/black_knight.png");}
    @Override public void extractRenderState(BlackKnightEntity e,SpaceKookState s,float partial) {
        super.extractRenderState(e,s,partial);s.chasing=e.isChasing();s.attackTime=e.getSwingAnimation(partial);
    }
}

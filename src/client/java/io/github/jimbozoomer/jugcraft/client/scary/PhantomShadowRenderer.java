package io.github.jimbozoomer.jugcraft.client.scary;
import io.github.jimbozoomer.jugcraft.creatures.scary.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.*;
public final class PhantomShadowRenderer extends MobRenderer<PhantomShadowEntity,SpaceKookState,PhantomShadowModel> {
    public PhantomShadowRenderer(EntityRendererProvider.Context context) {
        super(context,new PhantomShadowModel(context.bakeLayer(ScaryClient.SHADOW)),.35F);
        addLayer(new EyesLayer<SpaceKookState,PhantomShadowModel>(this) {
            @Override public RenderType renderType() {return RenderTypes.eyes(ScaryMod.id("textures/entity/scary/phantom_shadow_glow.png"));}
        });
    }
    @Override public SpaceKookState createRenderState() {return new SpaceKookState();}
    @Override public Identifier getTextureLocation(SpaceKookState s) {return ScaryMod.id("textures/entity/scary/phantom_shadow.png");}
    @Override public void extractRenderState(PhantomShadowEntity e,SpaceKookState s,float partial) {
        super.extractRenderState(e,s,partial);s.chasing=e.isChasing();
    }
}

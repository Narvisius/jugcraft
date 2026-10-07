package io.github.jimbozoomer.jugcraft.client.scary;
import io.github.jimbozoomer.jugcraft.creatures.scary.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class SpaceKookRenderer extends MobRenderer<SpaceKookEntity,SpaceKookState,SpaceKookModel> {
    private static Identifier texture(String name) { return ScaryMod.id("textures/entity/scary/"+name+".png"); }
    public SpaceKookRenderer(EntityRendererProvider.Context context) {
        super(context,new SpaceKookModel(context.bakeLayer(ScaryClient.BODY)),.65F);
        addLayer(new HelmetLayer(this, new SpaceKookModel(context.bakeLayer(ScaryClient.GLASS))));
    }
    @Override public SpaceKookState createRenderState() { return new SpaceKookState(); }
    @Override public Identifier getTextureLocation(SpaceKookState state) { return texture(state.redPulse()?"space_kook_red":"space_kook"); }
    @Override public void extractRenderState(SpaceKookEntity e, SpaceKookState s, float partial) {
        super.extractRenderState(e,s,partial); s.chasing=e.isChasing(); s.attackTime=e.getSwingAnimation(partial);
    }
    private static final class HelmetLayer extends RenderLayer<SpaceKookState,SpaceKookModel> {
        private final SpaceKookModel glass;
        HelmetLayer(SpaceKookRenderer parent, SpaceKookModel glass) { super(parent); this.glass=glass; }
        @Override public void submit(PoseStack stack, SubmitNodeCollector collector, int light, SpaceKookState s, float yaw, float pitch) {
            if(s.isInvisible) return;
            collector.order(1).submitModel(getParentModel(),s,stack,
                RenderTypes.eyes(texture(s.redPulse()?"glow_red":"glow")),light,OverlayTexture.NO_OVERLAY,s.outlineColor);
            collector.order(2).submitModel(glass,s,stack,
                RenderTypes.entityTranslucent(texture(s.redPulse()?"glass_red":"glass")),
                s.redPulse()?15728880:light,OverlayTexture.NO_OVERLAY,s.outlineColor);
        }
    }
}

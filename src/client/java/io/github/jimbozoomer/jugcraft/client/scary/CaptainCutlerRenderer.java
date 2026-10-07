package io.github.jimbozoomer.jugcraft.client.scary;
import io.github.jimbozoomer.jugcraft.creatures.scary.*;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public final class CaptainCutlerRenderer extends MobRenderer<CaptainCutlerEntity,SpaceKookState,SpaceKookModel> {
    public CaptainCutlerRenderer(EntityRendererProvider.Context context) {
        super(context,new SpaceKookModel(context.bakeLayer(ScaryClient.CUTLER)),.4F);
        addLayer(new EyesLayer<SpaceKookState,SpaceKookModel>(this) {
            @Override public RenderType renderType() {return RenderTypes.eyes(ScaryMod.id("textures/entity/scary/captain_cutler.png"));}
        });
    }
    @Override public SpaceKookState createRenderState() {return new SpaceKookState();}
    @Override public Identifier getTextureLocation(SpaceKookState state) {return ScaryMod.id("textures/entity/scary/captain_cutler.png");}
    @Override protected int getBlockLightLevel(CaptainCutlerEntity e,BlockPos pos) {return 15;}
    @Override public void extractRenderState(CaptainCutlerEntity e,SpaceKookState s,float partial) {
        super.extractRenderState(e,s,partial);s.chasing=e.isChasing();s.attackTime=e.getSwingAnimation(partial);
    }
}

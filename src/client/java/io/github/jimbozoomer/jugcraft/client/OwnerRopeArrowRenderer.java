package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerRopeArrow;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;
final class OwnerRopeArrowRenderer extends ArrowRenderer<OwnerRopeArrow,ArrowRenderState>{
    OwnerRopeArrowRenderer(EntityRendererProvider.Context context){super(context);}
    static void register(){EntityRendererRegistry.register(OwnerRopeArrow.TYPE,OwnerRopeArrowRenderer::new);}
    @Override public ArrowRenderState createRenderState(){return new ArrowRenderState();}
    @Override protected Identifier getTextureLocation(ArrowRenderState state){return Jugcraft.id("textures/newest/supps/entity/rope_arrow.png");}
}

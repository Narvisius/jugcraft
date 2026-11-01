package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.WildWeapons;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;
final class WildDartRenderer extends ArrowRenderer<WildWeapons.Dart,ArrowRenderState> {
    WildDartRenderer(EntityRendererProvider.Context c){super(c);}
    static void register(){EntityRendererRegistry.register(WildWeapons.DART,WildDartRenderer::new);}
    @Override public ArrowRenderState createRenderState(){return new ArrowRenderState();}
    @Override protected Identifier getTextureLocation(ArrowRenderState s){return Jugcraft.id("textures/newest/wilds/entity/dart.png");}
}

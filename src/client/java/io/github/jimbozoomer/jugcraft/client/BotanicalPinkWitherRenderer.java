package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.resources.Identifier;
final class BotanicalPinkWitherRenderer extends WitherBossRenderer {
    BotanicalPinkWitherRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public Identifier getTextureLocation(WitherRenderState state){return Jugcraft.id("textures/newest/jugtania/model/wither_pink.png");}
}

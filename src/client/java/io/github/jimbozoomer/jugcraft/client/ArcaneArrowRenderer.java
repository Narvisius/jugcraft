package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.arcane.*;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
final class ArcaneArrowRenderer extends ArrowRenderer<ArcaneArrow,ArcaneArrowRenderer.State> {
    ArcaneArrowRenderer(EntityRendererProvider.Context c){super(c);}
    static void register(){ArcaneArrow.TYPES.values().forEach(t->EntityRendererRegistry.register(t,ArcaneArrowRenderer::new));EntityRendererRegistry.register(ArcaneSplash.TYPE,ThrownItemRenderer::new);}
    static final class State extends ArrowRenderState {String kind;boolean cloud;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ArcaneArrow e,State s,float partial){super.extractRenderState(e,s,partial);s.kind=e.kind();s.cloud=e.cloud();}
    @Override protected Identifier getTextureLocation(State s){return Jugcraft.id("textures/newest/forbidden/entity/projectiles/"+s.kind+".png");}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){if(!s.cloud)super.submit(s,pose,out,camera);}
}

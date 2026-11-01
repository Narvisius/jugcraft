package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.botany.BotanicalGaia;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;

/** Gaia mirrors the viewing player's real skin and arm width, as in the supplied renderer. */
final class BotanicalGaiaRenderer extends HumanoidMobRenderer<BotanicalGaia,AvatarRenderState,PlayerModel> {
    private final PlayerModel classic,slim;
    private static final com.mojang.renderpearl.api.pipeline.RenderPipeline[] DISTORTION=new com.mojang.renderpearl.api.pipeline.RenderPipeline[12];
    private record Key(Identifier skin,int intensity){}
    private final java.util.Map<Key,net.minecraft.client.renderer.rendertype.RenderType> types=new java.util.LinkedHashMap<>();
    static {for(int n=0;n<DISTORTION.length;n++){
        float grain=n==11?1:.05F+n*.085F,distortion=n==11?.3F:(.025F+n*.0425F)/2;
        DISTORTION[n]=net.minecraft.client.renderer.RenderPipelines.register(com.mojang.renderpearl.api.pipeline.RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.ENTITY_SNIPPET,net.minecraft.client.renderer.RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(io.github.jimbozoomer.jugcraft.Jugcraft.id("pipeline/botanical_gaia_"+n)).withVertexShader(io.github.jimbozoomer.jugcraft.Jugcraft.id("core/botanical_gaia")).withFragmentShader(io.github.jimbozoomer.jugcraft.Jugcraft.id("core/botanical_gaia"))
            .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER1).withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER2)
            .withShaderDefine("JUGCRAFT_GRAIN",grain).withShaderDefine("JUGCRAFT_DISFIGURATION",distortion).withCull(false)
            .withColorTargetState(com.mojang.renderpearl.api.pipeline.ColorTargetState.DEFAULT).withDepthStencilState(net.minecraft.client.renderer.RenderPipelines.ENTITY_CUTOUT.getDepthStencilState()).build());
    }}
    private static final class State extends AvatarRenderState {int intensity;}

    BotanicalGaiaRenderer(EntityRendererProvider.Context c){
        super(c,new PlayerModel(c.bakeLayer(ModelLayers.PLAYER),false),0);
        classic=model;slim=new PlayerModel(c.bakeLayer(ModelLayers.PLAYER_SLIM),true);
    }
    @Override public AvatarRenderState createRenderState(){return new State();}
    @Override public void extractRenderState(BotanicalGaia entity,AvatarRenderState s,float partial){
        super.extractRenderState(entity,s,partial);if(s instanceof State state)state.intensity=entity.phase()==0||entity.phase()==2?11:Math.clamp(entity.hurtTime,0,10);
        var viewer=Minecraft.getInstance().player;if(viewer!=null)s.skin=viewer.getSkin();
        s.showHat=s.showJacket=s.showLeftPants=s.showRightPants=s.showLeftSleeve=s.showRightSleeve=true;
    }
    @Override public Identifier getTextureLocation(AvatarRenderState s){return s.skin.body().texturePath();}
    @Override protected net.minecraft.client.renderer.rendertype.RenderType getRenderType(AvatarRenderState s,boolean body,boolean translucent,boolean outline){
        if(!body||!(s instanceof State state))return super.getRenderType(s,body,translucent,outline);
        var key=new Key(getTextureLocation(s),state.intensity);if(!types.containsKey(key)&&types.size()>=48)types.remove(types.keySet().iterator().next());
        return types.computeIfAbsent(key,k->net.minecraft.client.renderer.rendertype.RenderType.create("jugcraft_gaia_"+k.intensity(),net.minecraft.client.renderer.rendertype.RenderSetup.builder(DISTORTION[k.intensity()])
            .withTexture("Sampler0",k.skin()).useLightmap().useOverlay().setOutline(net.minecraft.client.renderer.rendertype.RenderSetup.OutlineProperty.AFFECTS_OUTLINE).createRenderSetup()));
    }
    @Override public void submit(AvatarRenderState s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        model=s.skin.model()==PlayerModelType.SLIM?slim:classic;super.submit(s,pose,out,camera);
    }
}

package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.cache.animation.*;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.*;
import com.geckolib.renderer.base.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaStatues;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.phys.Vec3;

/** Supplied wide/slim meshes and all original static pose clips; snapshot-based, no shared mutable bones. */
public final class JimtaStatueRenderer extends GeoBlockRenderer<JimtaStatues.Node,JimtaStatueRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        String model="player";Identifier texture=DefaultPlayerSkin.getDefaultTexture();int pose,rotation;boolean flipped;float scale=1;boolean decorative;
    }
    private static <T extends GeoAnimatable> GeoModel<T> model(){return model("player");}
    private static <T extends GeoAnimatable> GeoModel<T> model(String kind){return new GeoModel<>(){
        @Override public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("block/jimtafacts/statue/"+(s instanceof State state?state.model:kind.equals("player")?defaultAppearance().model():kind));}
        @Override public Identifier getTextureResource(GeoRenderState s){return s instanceof State state?state.texture:kind.equals("player")?defaultAppearance().texture():Jugcraft.id("textures/jimtafacts/statue/"+kind+".png");}
        @Override public Identifier getAnimationResource(T a){return Jugcraft.id("block/jimtafacts/statue_poses");}
    };}
    private static JimtaStatueSkins.Appearance defaultAppearance(){return JimtaStatueSkins.resolve(DefaultPlayerSkin.get(new java.util.UUID(0,0)));}
    public static void register(){
        JimtaStatueSkins.register();
        BlockEntityRenderers.register(JimtaStatues.TYPE,JimtaStatueRenderer::new);
        JimtaStatues.renderer=()->Provider.INSTANCE;
        java.util.Map<String,GeoRenderProvider> providers=new java.util.HashMap<>();
        JimtaStatues.decorativeRenderer=kind->providers.computeIfAbsent(kind,k->new GeoRenderProvider(){
            private GeoItemRenderer<JimtaStatues.StatueItem> item;
            @Override public GeoItemRenderer<?> getGeoItemRenderer(){if(item==null)item=new GeoItemRenderer<>(model(k));return item;}
        });
    }
    private static final class Provider implements GeoRenderProvider {
        static final Provider INSTANCE=new Provider();private GeoItemRenderer<JimtaStatues.StatueItem> item;
        @Override public GeoItemRenderer<?> getGeoItemRenderer(){if(item==null)item=new GeoItemRenderer<>(model());return item;}
    }
    public JimtaStatueRenderer(BlockEntityRendererProvider.Context c){super(c,model());}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(JimtaStatues.Node n,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay crumble){
        // Resolve the same vanilla skin data already available to the client; no supporter API or custom download URL.
        var uuid=n.profile==null?java.util.UUID.nameUUIDFromBytes(("jugcraft-statue:"+n.getBlockPos().asLong()).getBytes(java.nio.charset.StandardCharsets.UTF_8)):n.profile;
        var connection=Minecraft.getInstance().getConnection();var player=connection==null?null:connection.getPlayerInfo(uuid);
        var skin=player==null?DefaultPlayerSkin.get(uuid):player.getSkin();
        var appearance=JimtaStatueSkins.resolve(skin);s.texture=appearance.texture();s.model=appearance.model();
        s.decorative=!n.kind().equals("player");s.scale=1;
        if(s.decorative){s.model=n.kind();s.texture=Jugcraft.id("textures/jimtafacts/statue/"+n.kind()+".png");s.scale=n.kind().equals("ice_spider")?.12F:.8F;}
        s.pose=n.pose;s.flipped=n.flipped;s.rotation=n.getBlockState().getValue(JimtaStatues.ROTATION);
        super.extractRenderState(n,s,partial,camera,crumble);
    }
    @Override public void adjustRenderPose(RenderPassInfo<State> info){
        super.adjustRenderPose(info);info.poseStack().translate(0,.125,0);
        info.poseStack().rotateDegrees(com.mojang.math.Axis.YP,-info.renderState().rotation*22.5F);
        float scale=info.renderState().scale;info.poseStack().scale(scale,scale,scale);
    }
    @Override public void adjustModelBonesForRender(RenderPassInfo<State> info,BoneSnapshots bones){
        super.adjustModelBonesForRender(info,bones);
        State s=info.renderState();if(s.decorative)return;var animation=getGeoModel().getBakedAnimation(null,JimtaStatues.POSES.get(Math.clamp(s.pose,0,JimtaStatues.POSES.size()-1)));
        if(animation==null)return;
        for(var track:animation.boneAnimations()){
            String name=track.boneName();if(s.flipped)name=switch(name){case "arm_l"->"arm_r";case "arm_r"->"arm_l";case "leg_l"->"leg_r";case "leg_r"->"leg_l";default->name;};
            bones.ifPresent(name,b->{
                var r=track.rotationKeyFrames();var p=track.positionKeyFrames();
                b.setRotation(first(r.xKeyframes()),first(r.yKeyframes())*(s.flipped?-1:1),first(r.zKeyframes())*(s.flipped?-1:1));
                b.setTranslation(first(p.xKeyframes())*(s.flipped?-1:1),first(p.yKeyframes()),first(p.zKeyframes()));
            });
        }
    }
    private static float first(Keyframe[] frames){return frames.length==0?0:(float)frames[0].startValue().get(null);}
}

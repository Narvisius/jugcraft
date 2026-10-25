package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.*;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaWorkshops;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Original statue mesh, visible finite cauldron layers, and the actual item on the source pedestal. */
public final class JimtaWorkshopRenderer extends GeoBlockRenderer<JimtaWorkshops.Node,JimtaWorkshopRenderer.State> {
    private final ItemModelResolver resolver;
    public static void register(){BlockEntityRenderers.register(JimtaWorkshops.NODE,JimtaWorkshopRenderer::new);
        var provider=new GeoRenderProvider(){
            private GeoItemRenderer<JimtaWorkshops.StatueItem> renderer;
            @Override public GeoItemRenderer<?> getGeoItemRenderer(){
                // Client entrypoints run before Minecraft's model manager exists.
                // Gecko item renderers must be constructed on first rendering use.
                if(renderer==null)renderer=new GeoItemRenderer<>(statueModel());
                return renderer;
            }
        };
        JimtaWorkshops.statueRenderer=()->provider;
    }
    private static <T extends com.geckolib.animatable.GeoAnimatable> GeoModel<T> statueModel(){return new GeoModel<>(){
        @Override public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("block/jimtafacts/tyros_statue");}
        @Override public Identifier getTextureResource(GeoRenderState s){return Jugcraft.id("textures/jimtafacts/spellbooks/entity/statue/tyros.png");}
        @Override public Identifier getAnimationResource(T a){return Jugcraft.id("block/jimtafacts/tyros_statue");}
    };}
    public static final class State extends BlockEntityRenderState {String kind="";int amount,color;float time;final ItemStackRenderState item=new ItemStackRenderState();}
    public JimtaWorkshopRenderer(BlockEntityRendererProvider.Context c){super(c,statueModel());resolver=c.itemModelResolver();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(JimtaWorkshops.Node node,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay crumble){
        s.kind=node.kind();if(s.kind.equals("tyros_statue"))super.extractRenderState(node,s,partial,camera,crumble);else BlockEntityRenderState.extractBase(node,s,crumble);
        s.amount=node.amount();s.time=node.getLevel()==null?0:node.getLevel().getGameTime()+partial;s.item.clear();
        if(s.kind.equals("pedestal"))resolver.updateForTopItem(s.item,node.getItem(0),ItemDisplayContext.GROUND,node.getLevel(),null,0);
        String key=node.fluids.isEmpty()?"":new java.util.ArrayList<>(node.fluids.keySet()).getLast();
        s.color=key.contains("blood")?0xff8e1528:key.contains("legendary")?0xffffad27:key.contains("epic")?0xffab43e8:key.contains("rare")?0xff4375d6:key.contains("uncommon")?0xff59af66:key.contains("ink")?0xff312b3e:key.contains("potion")?0xffa37dd6:0xff3f76e4;
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        if(s.kind.equals("tyros_statue")){super.submit(s,pose,out,camera);return;}
        if(s.kind.equals("pedestal")){pose.pushPose();pose.translate(.5,1.15,.5);pose.rotateDegrees(Axis.YP,s.time*2);s.item.submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
        if(s.kind.contains("cauldron")&&s.amount>0){float y=.3F+.55F*s.amount/1000F;out.submitCustomGeometry(pose,RenderTypes.debugQuads(),(p,b)->{
            b.addVertex(p,.1875F,y,.1875F).setColor(s.color);b.addVertex(p,.1875F,y,.8125F).setColor(s.color);b.addVertex(p,.8125F,y,.8125F).setColor(s.color);b.addVertex(p,.8125F,y,.1875F).setColor(s.color);
        });}
    }
}

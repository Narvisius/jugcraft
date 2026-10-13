package local.peepo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.*;
import java.util.function.Consumer;
import local.peepo.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.special.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** The wooden shell is a normal baked model; this companion layer draws its actual miniature occupants. */
public final class TransportCrateRenderer implements SpecialModelRenderer<List<TransportCrateRenderer.Occupant>> {
    public record Occupant(EntityRenderState state,float scale,float x,float z){}
    public record Unbaked() implements SpecialModelRenderer.Unbaked<List<Occupant>> {
        public static final MapCodec<Unbaked> CODEC=MapCodec.unit(new Unbaked());
        @Override public SpecialModelRenderer<List<Occupant>> bake(BakingContext context){return new TransportCrateRenderer();}
        @Override public MapCodec<Unbaked> type(){return CODEC;}
    }
    public static void initialize(){SpecialModelRenderers.ID_MAPPER.put(PeepoMod.id("crate_occupants"),Unbaked.CODEC);}
    @Override public List<Occupant> extractArgument(ItemStack stack){
        var result=new ArrayList<Occupant>();var dispatcher=Minecraft.getInstance().getEntityRenderDispatcher();
        int count=MobTransportCrate.count(stack),cols=count>4?4:2,index=0;
        for(var entry:MobTransportCrate.occupants(stack)){
            var mob=TransportCratePreview.mob(entry);if(mob==null)continue;
            var state=dispatcher.extractEntity(mob,0);state.shadowRadius=0;state.outlineColor=0;
            // Models have snouts/tails outside their collision widths; leave room between rows and the slats.
            float scale=Math.min(.52F/Math.max(.1F,mob.getBbHeight()),(cols==4?.15F:.21F)/Math.max(.1F,mob.getBbWidth()));
            result.add(new Occupant(state,scale,.5F+(index%cols-(cols-1)/2F)*(cols==4?.19F:.36F),.5F+(index/cols-.5F)*.24F));index++;
        }
        return result;
    }
    @Override public void submit(List<Occupant> occupants,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline){
        if(occupants==null)return;var dispatcher=Minecraft.getInstance().getEntityRenderDispatcher();var camera=new CameraRenderState();camera.orientation=new Quaternionf();
        for(var mob:occupants){pose.pushPose();pose.translate(mob.x,.145,mob.z);pose.scale(mob.scale,mob.scale,mob.scale);mob.state.lightCoords=light;
            dispatcher.getRenderer(mob.state).submit(mob.state,pose,collector,camera);pose.popPose();}
    }
    @Override public void getExtents(Consumer<Vector3fc> out){out.accept(new Vector3f(0,0,0));out.accept(new Vector3f(1,1,1));}
}

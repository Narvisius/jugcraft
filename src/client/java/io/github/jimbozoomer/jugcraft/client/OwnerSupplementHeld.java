package io.github.jimbozoomer.jugcraft.client;
import com.mojang.serialization.*;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.special.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

/** Placed and held vessels share one content renderer; original empty-shell meshes remain intact. */
final class OwnerSupplementHeld implements SpecialModelRenderer<OwnerSupplementRenderer.State> {
    private record Key(Item item,DataComponentPatch components){}
    private final LinkedHashMap<Key,OwnerSupplementData> cache=new LinkedHashMap<>(32,.75F,true);
    private OwnerSupplementRenderer renderer;
    private net.minecraft.client.multiplayer.ClientLevel level;
    private final String name;
    OwnerSupplementHeld(String name){this.name=name;}
    static void register(){SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("supps_contents"),Unbaked.CODEC);}
    public record Unbaked(String name) implements SpecialModelRenderer.Unbaked<OwnerSupplementRenderer.State>{
        static final MapCodec<Unbaked> CODEC=Codec.STRING.fieldOf("name").xmap(Unbaked::new,Unbaked::name);
        @Override public MapCodec<Unbaked> type(){return CODEC;}
        @Override public SpecialModelRenderer<OwnerSupplementRenderer.State> bake(SpecialModelRenderer.BakingContext context){return new OwnerSupplementHeld(name);}
    }
    @Override public OwnerSupplementRenderer.State extractArgument(ItemStack stack){
        var mc=Minecraft.getInstance();if(renderer==null)renderer=new OwnerSupplementRenderer();if(level!=mc.level){level=mc.level;cache.clear();}
        var state=new OwnerSupplementRenderer.State();var block=OwnerSupplementaries.BLOCKS.get(name);if(block==null)return state;
        var key=new Key(stack.getItem(),stack.getComponentsPatch());var data=cache.get(key);
        if(data==null){data=new OwnerSupplementData(BlockPos.ZERO,block.defaultBlockState());data.restore(stack);data.setLevel(level);cache.put(key,data);if(cache.size()>32)cache.remove(cache.keySet().iterator().next());}
        if(level!=null)renderer.extractRenderState(data,state,0,Vec3.ZERO,null);
        else{state.name=name;if(data.softPortions>0){state.liquidFill=name.equals("goblet")?1:(float)data.softPortions*OwnerSoftLiquids.units(data.softDrink)/9;state.liquidColor=OwnerSoftLiquids.color(data.softDrink);}if(name.equals("cannon")){state.cannonPitch=-10;}if(name.startsWith("globe")){state.globe=OwnerGlobeRenderer.appearance(data);state.globeRotation=0;}if(name.equals("jar_boat")){var ship=new ItemStack(Items.STICK);ship.set(DataComponents.ITEM_MODEL,Jugcraft.id("supps_jar_boat_ship"));mc.getItemModelResolver().updateForTopItem(state.ship,ship,ItemDisplayContext.NONE,null,null,0);}if(name.startsWith("flag_"))state.flagPatterns=data.flagPatterns;state.direction=HouseholdBlock.facing(data.getBlockState());if(name.equals("blackboard")){state.board=data.boardPixels.clone();state.boardGlow=data.boardGlow;}}
        mc.getModelManager().getBlockStateModelSet().get(block.defaultBlockState()).collectParts(net.minecraft.util.RandomSource.create(0),state.shell);
        return state;
    }
    @Override public void submit(OwnerSupplementRenderer.State s,PoseStack pose,SubmitNodeCollector out,int light,int overlay,boolean foil,int outline){
        s.lightCoords=light;if(s.mob!=null)s.mob.lightCoords=light;
        var camera=new CameraRenderState();camera.pos=Vec3.ZERO;camera.blockPos=BlockPos.ZERO;camera.orientation=new Quaternionf();
        renderer.submit(s,pose,out,camera);
        out.submitBlockModel(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS),s.shell,new int[0],light,overlay,outline);
    }
    @Override public void getExtents(Consumer<Vector3fc> result){result.accept(new Vector3f(0,0,0));result.accept(new Vector3f(1,1,name.startsWith("flag_")?2.1F:1));}
}

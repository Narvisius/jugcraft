package io.github.jimbozoomer.jugcraft.client;
import java.lang.Math;

import java.util.*;
import java.util.function.Consumer;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.loot.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

/** Original supplied textures, native layers used by the supplied renderer, and recovered pot shards. */
final class OwnerLootRenderer implements BlockEntityRenderer<OwnerLootContainer,OwnerLootRenderer.State> {
    private final ChestModel chest;
    private final ModelPart shulker,potBase,potSides,potOpen;
    OwnerLootRenderer(EntityModelSet models){
        chest=new ChestModel(models.bakeLayer(ModelLayers.CHEST));shulker=models.bakeLayer(ModelLayers.SHULKER_BOX);
        potBase=models.bakeLayer(ModelLayers.DECORATED_POT_BASE);potSides=models.bakeLayer(ModelLayers.DECORATED_POT_SIDES);
        try(var in=Minecraft.getInstance().getResourceManager().getResourceOrThrow(Jugcraft.id("newest_loot_models/open_pot.json")).openAsReader()){potOpen=RecoveredWildlifeModel.bake(JsonParser.parseReader(in).getAsJsonObject());}
        catch(java.io.IOException e){throw new IllegalStateException("Missing supplied personal-loot pot model",e);}
    }
    static void register(){BlockEntityRenderers.register(OwnerLoot.TYPE,c->new OwnerLootRenderer(c.entityModelSet()));SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("personal_loot"),Unbaked.CODEC);OwnerLootEntityRenderers.register();}
    static final class State extends BlockEntityRenderState {
        String kind="chest";boolean seen;float open;Direction facing=Direction.NORTH;
        net.minecraft.world.level.block.entity.PotDecorations decorations=net.minecraft.world.level.block.entity.PotDecorations.EMPTY;
        final List<BlockStateModelPart> parts=new ArrayList<>();
    }
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(OwnerLootContainer be,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderState.extractBase(be,s,overlay);var block=be.getBlockState();s.kind=((OwnerLootBlock)block.getBlock()).kind;s.facing=block.getValue(OwnerLootBlock.FACING);
        var player=Minecraft.getInstance().player;s.seen=player!=null&&be.seen(player.getUUID());s.open=be.previousOpenness+(be.openness-be.previousOpenness)*partial;
        s.decorations=be.decorations();
        s.parts.clear();Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(block.setValue(OwnerLootBlock.MODEL,true).setValue(OwnerLootBlock.SEEN,s.seen)).collectParts(RandomSource.create(0),s.parts);
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){draw(s,pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);}
    private static Identifier texture(String path){return Jugcraft.id("textures/newest/personal_loot/entity/"+path+".png");}
    private static void part(ModelPart model,Identifier tex,PoseStack pose,SubmitNodeCollector out,int light,int overlay,int outline){out.order(0).submitModelPart(model,pose,RenderTypes.entityCutout(tex),light,overlay,null,-1,outline);}
    void draw(State s,PoseStack pose,SubmitNodeCollector out,int light,int overlay,int outline){
        pose.pushPose();
        if(s.kind.equals("barrel")||s.kind.startsWith("suspicious_")){out.submitBlockModel(pose,RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS),s.parts,new int[0],light,overlay,outline);}
        else if(s.kind.equals("shulker_box")){
            pose.mulPose(ShulkerBoxRenderer.modelTransform(s.facing));shulker.getAllParts().forEach(ModelPart::resetPose);
            var lid=shulker.getChild("lid");lid.y=24-s.open*8;lid.yRot=(float)Math.toRadians(270*s.open);
            part(shulker,texture("shulker_box/normal"+(s.seen?"_opened":"")),pose,out,light,overlay,outline);
        }else if(s.kind.equals("decorated_pot")){
            pose.translate(.5,0,.5);pose.rotateDegrees(Axis.YP,180-s.facing.toYRot());
            if(s.seen){pose.scale(1,-1,-1);part(potOpen,texture("pot/normal_opened"),pose,out,light,overlay,outline);}
            else{
                pose.translate(-.5,0,-.5);part(potBase,texture("pot/normal"),pose,out,light,overlay,outline);
                potSide("front",s.decorations.front(),pose,out,light,overlay,outline);
                potSide("back",s.decorations.back(),pose,out,light,overlay,outline);
                potSide("left",s.decorations.left(),pose,out,light,overlay,outline);
                potSide("right",s.decorations.right(),pose,out,light,overlay,outline);
            }
        }else{
            pose.mulPose(ChestRenderer.modelTransformation(s.facing));float openness=1-(float)Math.pow(1-s.open,3);chest.setupAnim(openness);
            String tex=s.kind.contains("copper")?switch(s.kind){case "exposed_copper_chest"->"copper_exposed";case "weathered_copper_chest"->"copper_weathered";case "oxidized_copper_chest"->"copper_oxidized";default->"copper";}:s.kind.equals("trapped_chest")?"trapped":"normal";
            part(chest.root(),texture("chest/"+tex+(s.seen?"_opened":"")),pose,out,light,overlay,outline);
        }pose.popPose();
    }
    private void potSide(String side,Optional<net.minecraft.world.item.ItemStackTemplate> sherd,PoseStack pose,SubmitNodeCollector out,int light,int overlay,int outline){
        var pattern=sherd.map(i->i.get(net.minecraft.core.component.DataComponents.PROVIDES_POTTERY_PATTERN)).orElse(null);
        var asset=pattern==null?Identifier.withDefaultNamespace("decorated_pot_side"):pattern.value().assetId();
        part(potSides.getChild(side),asset.withPath(p->"textures/entity/decorated_pot/"+p+".png"),pose,out,light,overlay,outline);
    }
    public record Unbaked(String design) implements SpecialModelRenderer.Unbaked<State>{
        static final MapCodec<Unbaked> CODEC=Codec.STRING.fieldOf("design").xmap(Unbaked::new,Unbaked::design);
        @Override public MapCodec<Unbaked> type(){return CODEC;}
        @Override public SpecialModelRenderer<State> bake(SpecialModelRenderer.BakingContext context){return new Held(design,new OwnerLootRenderer(context.entityModelSet()));}
    }
    private record Held(String design,OwnerLootRenderer renderer) implements SpecialModelRenderer<State>{
        @Override public State extractArgument(ItemStack stack){var s=new State();s.kind=design;s.facing=design.equals("shulker_box")?Direction.UP:Direction.SOUTH;return s;}
        @Override public void submit(State state,PoseStack pose,SubmitNodeCollector out,int light,int overlay,boolean foil,int outline){renderer.draw(state,pose,out,light,overlay,outline);}
        @Override public void getExtents(Consumer<Vector3fc> out){out.accept(new Vector3f(0,0,0));out.accept(new Vector3f(1,1,1));}
    }
}

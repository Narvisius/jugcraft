package io.github.jimbozoomer.jugcraft.client;
import java.lang.Math;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.*;
import net.minecraft.world.item.ItemStack;
import org.joml.*;

/** Recovered original pylon geometry, native ring rotation and counter-rotating crystals. */
final class BotanicalPylons {
    private final Map<String,ModelPart> models=new HashMap<>();
    private Object resourceManager;
    private static int generation;
    private int seenGeneration=-1;
    static void register(){SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("botanical_pylon"),Unbaked.CODEC);
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).registerReloadListener(Jugcraft.id("botanical_models"),(net.minecraft.server.packs.resources.ResourceManagerReloadListener)r->generation++);
    }
    private ModelPart model(String variant){var manager=Minecraft.getInstance().getResourceManager();if(manager!=resourceManager||seenGeneration!=generation){models.clear();resourceManager=manager;seenGeneration=generation;}return models.computeIfAbsent(variant,key->{
        String name=switch(key){case "gaia"->"GaiaPylonModel";case "natura"->"NaturaPylonModel";default->"ManaPylonModel";};
        try(var reader=Minecraft.getInstance().getResourceManager().getResourceOrThrow(Jugcraft.id("newest_jugtania_models/"+name+".json")).openAsReader()){
            return RecoveredWildlifeModel.bake(JsonParser.parseReader(reader).getAsJsonObject());
        }catch(java.io.IOException e){throw new IllegalStateException("Missing original pylon geometry "+name,e);}
    });}
    void draw(String variant,float ticks,boolean held,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,int outline){
        var root=model(variant);var texture=Jugcraft.id("textures/newest/jugtania/model/pylon_"+variant+".png");
        root.getAllParts().forEach(p->{p.resetPose();p.visible=true;});
        pose.pushPose();pose.translate(.5,held?1.35:1.5,.5);pose.scale(1,-1,-1);
        for(boolean crystal:new boolean[]{false,true}){
            pose.pushPose();if(!held){if(crystal)pose.translate(0,Math.sin(ticks/20.0)/17.5,0);pose.rotateDegrees(Axis.YP,crystal?-ticks:ticks*1.5F);}
            var names=crystal?(!variant.equals("mana")?List.of("shardrft","shardlbt","shardrbt","shardlft","shardrfb","shardlbb","shardrbb","shardlfb"):List.of("shardlf","shardrf","shardlb","shardrb")):List.of("platef","plateb","platel","plater");
            // Submit disjoint parts; queued ring draws must not observe a later crystal visibility mask.
            for(String name:names)collector.order(0).submitModelPart(root.getChild(name),pose,RenderTypes.entityCutout(texture),crystal?net.minecraft.util.LightCoordsUtil.FULL_BRIGHT:light,overlay,null,-1,outline);pose.popPose();
        }pose.popPose();
    }
    record Unbaked(String variant) implements SpecialModelRenderer.Unbaked<String> {
        static final MapCodec<Unbaked> CODEC=Codec.STRING.optionalFieldOf("variant","mana").xmap(Unbaked::new,Unbaked::variant);
        @Override public MapCodec<Unbaked> type(){return CODEC;}
        @Override public SpecialModelRenderer<String> bake(SpecialModelRenderer.BakingContext context){return new Held(variant);}
    }
    private static final class Held implements SpecialModelRenderer<String> {
        private final String variant;private final BotanicalPylons renderer=new BotanicalPylons();
        Held(String variant){this.variant=variant;}
        @Override public String extractArgument(ItemStack stack){return variant;}
        @Override public void submit(String value,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline){renderer.draw(variant,0,true,pose,collector,light,overlay,outline);}
        @Override public void getExtents(Consumer<Vector3fc> out){out.accept(new Vector3f(0,0,0));out.accept(new Vector3f(1,1.5F,1));}
    }
}

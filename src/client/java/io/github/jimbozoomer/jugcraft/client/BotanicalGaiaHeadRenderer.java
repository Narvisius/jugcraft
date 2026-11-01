package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.special.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SkullBlock;
import org.joml.*;

/** Source Gaia trophy reflects the viewer using native skull geometry and the viewer's skin. */
final class BotanicalGaiaHeadRenderer {
    private static final Map<SkullBlock.Type,SkullModelBase> MODELS=new HashMap<>();private static Object modelSet;
    static void register(){SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("botanical_gaia_head"),Unbaked.CODEC);}
    static void draw(PoseStack pose,SubmitNodeCollector out,int light,int outline){
        var c=Minecraft.getInstance();var models=c.getEntityModels();if(models!=modelSet){modelSet=models;MODELS.clear();}
        var viewer=c.getCameraEntity();var type=viewer==null?null:viewer.getType();
        SkullBlock.Type kind=type==net.minecraft.world.entity.EntityTypes.SKELETON?SkullBlock.Types.SKELETON:type==net.minecraft.world.entity.EntityTypes.WITHER_SKELETON||type==net.minecraft.world.entity.EntityTypes.WITHER?SkullBlock.Types.WITHER_SKELETON:type==net.minecraft.world.entity.EntityTypes.ZOMBIE?SkullBlock.Types.ZOMBIE:type==net.minecraft.world.entity.EntityTypes.CREEPER?SkullBlock.Types.CREEPER:type==net.minecraft.world.entity.EntityTypes.ENDER_DRAGON?SkullBlock.Types.DRAGON:SkullBlock.Types.PLAYER;
        var skull=MODELS.computeIfAbsent(kind,k->SkullBlockRenderer.createModel(models,k));
        var skin=viewer instanceof net.minecraft.client.player.AbstractClientPlayer player?player.getSkin().body().texturePath():c.player==null?null:c.player.getSkin().body().texturePath();
        var render=SkullBlockRenderer.getSkullRenderType(kind,kind==SkullBlock.Types.PLAYER?skin:null);
        SkullBlockRenderer.submitSkull(0,pose,out,light,skull,render,outline,null);
    }
    record Unbaked() implements SpecialModelRenderer.Unbaked<Boolean>{
        static final MapCodec<Unbaked> CODEC=MapCodec.unit(new Unbaked());
        public MapCodec<Unbaked> type(){return CODEC;}
        public SpecialModelRenderer<Boolean> bake(SpecialModelRenderer.BakingContext context){return new Held();}
    }
    private static final class Held implements SpecialModelRenderer<Boolean>{
        public Boolean extractArgument(ItemStack stack){return true;}
        public void submit(Boolean value,PoseStack pose,SubmitNodeCollector out,int light,int overlay,boolean foil,int outline){pose.pushPose();pose.translate(.5,0,.5);draw(pose,out,light,outline);pose.popPose();}
        public void getExtents(Consumer<Vector3fc> out){out.accept(new Vector3f(.25F,0,.25F));out.accept(new Vector3f(.75F,.5F,.75F));}
    }
}

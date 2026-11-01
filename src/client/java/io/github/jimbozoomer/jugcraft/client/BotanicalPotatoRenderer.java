package io.github.jimbozoomer.jugcraft.client;
import java.lang.Math;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.botany.Jugtania;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.special.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import org.joml.*;

/** Uses all supplied named potato meshes, not a replacement humanoid model. */
final class BotanicalPotatoRenderer {
    static void register(){SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("botanical_potato"),Unbaked.CODEC);}
    static ItemStack display(net.minecraft.network.chat.Component name){
        var mc=Minecraft.getInstance();String variant=name.getString().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]","_");
        if(variant.isEmpty()||!mc.getResourceManager().getResource(Jugcraft.id("items/jugtania_tater_"+variant+".json")).isPresent())variant=java.time.LocalDate.now().getMonthValue()==10?"halloween":"default";
        var stack=new ItemStack(Jugtania.item("tiny_potato"));stack.set(DataComponents.ITEM_MODEL,Jugcraft.id("jugtania_tater_"+variant));return stack;
    }
    static void render(ItemStackRenderState potato,List<ItemStackRenderState> items,List<Integer> slots,boolean[] blocks,boolean[] children,float yaw,float jump,PoseStack pose,SubmitNodeCollector collector,int light,int overlay){
        double wave=Math.sin(jump/10*Math.PI);pose.pushPose();pose.translate(.5+wave*.05,Math.abs(wave)*.2,.5);pose.rotateDegrees(Axis.YP,yaw-180);pose.rotateDegrees(Axis.ZP,(float)wave*2);
        pose.pushPose();pose.translate(0,.5,0);potato.submit(pose,collector,light,overlay,0);pose.popPose();
        pose.translate(0,.5,0);pose.scale(.2857143F,.2857143F,.2857143F);
        for(int n=0;n<items.size();n++){
            int i=slots.get(n);if(i>=6)continue;boolean block=blocks[i],child=children[i];pose.pushPose();
            // Source offsets, with Minecraft's item-model center retained.
            switch(i){
                case 1->{if(child)pose.translate(0,-.375,.5);else if(block)pose.translate(0,.3,.5);pose.translate(0,-.5,-.4);}
                case 0->{pose.translate(0,-2.3,-.88);if(child)pose.translate(0,1.25,.5);else if(block)pose.translate(0,1,.6);}
                case 2->{pose.translate(0,-1.9,.02);if(child)pose.translate(0,.2,.57);else if(block)pose.translate(0,1,.6);}
                case 3->{pose.translate(0,-1.6,-.89);if(child)pose.translate(0,-.59,.26);else if(block)pose.translate(0,1,.5);}
                case 5->{if(child)pose.translate(-.35,-.29,-.06);else if(block)pose.translate(-.4,.8,0);else pose.rotateDegrees(Axis.YP,-90);pose.translate(-.3,-1.9,.04);}
                case 4->{if(child)pose.translate(.95,-.29,.9);else if(block)pose.translate(1,.8,1);else pose.rotateDegrees(Axis.YP,-90);pose.translate(-.3,-1.9,-.92);}
            }
            if(child)pose.scale(1.1F,1.1F,1.1F);else if(block)pose.scale(.5F,.5F,.5F);if(block&&i==2)pose.rotateDegrees(Axis.YP,180);items.get(n).submit(pose,collector,light,overlay,0);pose.popPose();
        }pose.popPose();
    }
    record Unbaked() implements SpecialModelRenderer.Unbaked<ItemStackRenderState> {
        static final MapCodec<Unbaked> CODEC=MapCodec.unit(new Unbaked());
        @Override public MapCodec<Unbaked> type(){return CODEC;}
        @Override public SpecialModelRenderer<ItemStackRenderState> bake(SpecialModelRenderer.BakingContext context){return new Held();}
    }
    static final class Held implements SpecialModelRenderer<ItemStackRenderState> {
        @Override public ItemStackRenderState extractArgument(ItemStack stack){var mc=Minecraft.getInstance();var result=new ItemStackRenderState();mc.getItemModelResolver().updateForTopItem(result,display(stack.getOrDefault(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.empty())),ItemDisplayContext.NONE,mc.level,null,0);return result;}
        @Override public void submit(ItemStackRenderState state,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline){pose.pushPose();pose.translate(.5,.5,.5);state.submit(pose,collector,light,overlay,outline);pose.popPose();}
        @Override public void getExtents(Consumer<Vector3fc> out){out.accept(new Vector3f(0,0,0));out.accept(new Vector3f(1,1,1));}
    }
}

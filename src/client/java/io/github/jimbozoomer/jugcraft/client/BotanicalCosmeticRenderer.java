package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.botany.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomModelData;
import java.util.List;

/** Original supplied cosmetic offsets, paired elements and UV-bearing item models. */
final class BotanicalCosmeticRenderer {
    static ItemModelResolver resolver;
    static boolean render(String name,HumanoidModel<?> body,ItemStack original,LivingEntity living,PoseStack ms,SubmitNodeCollector collector,int light,int outline){
        String attached=BotanicalCosmetics.attached(original);if(!attached.isEmpty())name=attached;
        if(!BotanicalCosmetics.cosmetic(name))return false;
        ItemStack stack=new ItemStack(Jugtania.item(name));
        if(name.equals("monocle")){body.head.translateAndRotate(ms);ms.translate(.15,-.2,-.25);ms.scale(.3F,-.3F,-.3F);draw(stack,ms,collector,light,outline,living,-1);return true;}
        String variant=name.substring(9);
            if (BotanicalCosmetics.HEAD.contains(variant)) {
                body.head.translateAndRotate(ms);
                switch (variant) {
                    case "red_glasses": 
                    case "engineer_goggles": 
                    case "anaglyph_glasses": {
                        ms.translate(0.0, -0.225, -0.3);
                        ms.scale(0.7f, -0.7f, -0.7f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "eyepatch": {
                        ms.translate(0.125, -0.225, -0.3);
                        ms.rotateDegrees(Axis.YP,180.0f);
                        ms.scale(0.3f, -0.3f, -0.3f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "wicked_eyepatch": {
                        ms.translate(-0.125, -0.225, -0.3);
                        ms.scale(0.3f, -0.3f, -0.3f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "red_ribbons": {
                        ms.translate(0.0, -0.65, 0.2);
                        ms.rotateDegrees(Axis.YP,180.0f);
                        ms.scale(0.5f, -0.5f, -0.5f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "pink_flower_bud": {
                        ms.translate(0.275, -0.6, 0.0);
                        ms.rotateDegrees(Axis.YP,-90.0f);
                        ms.scale(0.5f, -0.5f, -0.5f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "polka_dotted_bows": {
                        ms.pushPose();
                        ms.translate(0.275, -0.4, 0.0);
                        ms.rotateDegrees(Axis.YP,-90.0f);
                        ms.scale(0.5f, -0.5f, -0.5f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        ms.popPose();
                        ms.translate(-0.275, -0.4, 0.0);
                        ms.rotateDegrees(Axis.YP,90.0f);
                        ms.scale(0.5f, -0.5f, -0.5f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "blue_butterfly": {
                        ms.pushPose();
                        ms.translate(0.275, -0.4, 0.0);
                        ms.rotateDegrees(Axis.YP,45.0f);
                        ms.scale(0.5f, -0.5f, -0.5f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        ms.popPose();
                        ms.translate(0.275, -0.4, 0.0);
                        ms.rotateDegrees(Axis.YP,-45.0f);
                        ms.scale(0.5f, -0.5f, -0.5f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "cat_ears": {
                        ms.translate(0.0f, -0.5f, -0.175f);
                        ms.scale(0.5f, -0.5f, -0.5f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "googly_eyes": {
                        ms.translate(0.0, -0.225, -0.3);
                        ms.scale(0.9f, -0.9f, -0.9f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "clock_eye": {
                        ms.translate(0.1, -0.225, (double)-0.3f);
                        ms.scale(0.4f, -0.4f, -0.4f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "unicorn_horn": {
                        ms.translate(0.0, -0.7, -0.3);
                        ms.rotateDegrees(Axis.YP,-90.0f);
                        ms.scale(0.6f, -0.6f, -0.6f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "devil_horns": {
                        ms.translate(0.0f, -0.4f, -0.175f);
                        ms.scale(0.5f, -0.5f, -0.5f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "hyper_plus": {
                        ms.translate(-0.15f, -0.45f, -0.3f);
                        ms.scale(0.2f, -0.2f, -0.2f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        ms.translate(1.45f, 0.0f, 0.0f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "ancient_mask": {
                        ms.translate(0.0, -0.3, -0.3);
                        ms.scale(0.7f, -0.7f, -0.7f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "eerie_mask": {
                        ms.translate(0.0, -0.25, -0.3);
                        ms.scale(0.75f, -0.75f, -0.75f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "alien_antenna": {
                        ms.translate(0.0, -0.65, 0.2);
                        ms.scale(0.5f, -0.5f, -0.5f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "orange_shades": {
                        ms.translate(0.0, -0.3, -0.3);
                        ms.scale(0.7f, -0.7f, -0.7f);
                        int color = -1291845633;
                        draw(stack,ms,collector,light,outline,living,color);
                        break;
                    }
                    case "groucho_glasses": {
                        ms.translate(0.0, -0.1, -0.3);
                        ms.scale(0.75f, -0.75f, -0.75f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "thick_eyebrows": {
                        ms.pushPose();
                        ms.translate(-0.1, -0.3, -0.3);
                        ms.scale(0.3f, -0.3f, -0.3f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        ms.popPose();
                        ms.translate(0.1, -0.3, -0.3);
                        ms.rotateDegrees(Axis.YP,180.0f);
                        ms.scale(0.3f, -0.3f, -0.3f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "tiny_potato_mask": {
                        ms.translate(0.0, -0.3, -0.3);
                        ms.scale(0.6f, -0.6f, -0.6f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "questgiver_mark": {
                        ms.translate(0.0, -0.8, -0.2);
                        ms.scale(0.5f, -0.5f, -0.5f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "thinking_hand": {
                        ms.translate(-0.1, 0.0, -0.3);
                        ms.rotateDegrees(Axis.ZP,-15.0f);
                        ms.scale(0.5f, -0.5f, -0.5f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                }
            } else {
                body.body.translateAndRotate(ms);
                switch (variant) {
                    case "black_bowtie": {
                        ms.translate(0.0, 0.1, -0.13);
                        ms.scale(0.6f, -0.6f, -0.6f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "black_tie": 
                    case "puffy_scarf": {
                        ms.translate(0.0, 0.25, -0.15);
                        ms.scale(0.5f, -0.5f, -0.5f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "witch_pin": {
                        ms.translate(-0.1, 0.15, -0.15);
                        ms.scale(0.2f, -0.2f, -0.2f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "devil_tail": {
                        ms.translate(0.0, 0.55, 0.2);
                        ms.rotateDegrees(Axis.YP,-90.0f);
                        ms.scale(0.6f, -0.6f, -0.6f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "kamui_eye": {
                        ms.pushPose();
                        ms.translate(0.4, 0.1, -0.2);
                        ms.scale(0.5f, -0.5f, -0.5f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        ms.popPose();
                        ms.translate(-0.4, 0.1, -0.2);
                        ms.rotateDegrees(Axis.YP,180.0f);
                        ms.scale(0.5f, -0.5f, -0.5f);
                        draw(stack,ms,collector,light,outline,living,-16777140);
                        break;
                    }
                    case "four_leaf_clover": {
                        ms.translate(0.1, 0.1, -0.13);
                        ms.scale(0.3f, -0.3f, -0.3f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "botanist_emblem": {
                        ms.translate(0.0, 0.375, -0.13);
                        ms.scale(0.3f, -0.3f, -0.3f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                    case "lusitanic_shield": {
                        ms.translate(0.0, 0.35, 0.13);
                        ms.rotateDegrees(Axis.ZP,8.0f);
                        ms.rotateDegrees(Axis.YP,180.0f);
                        ms.scale(0.6f, -0.6f, -0.6f);
                        draw(stack,ms,collector,light,outline,living,-1);
                        break;
                    }
                }
            }
        return true;
    }
    private static void draw(ItemStack stack,PoseStack pose,SubmitNodeCollector collector,int light,int outline,LivingEntity wearer,int color){
        var copy=stack.copy();copy.set(DataComponents.ITEM_MODEL,Jugcraft.id("jugtania_worn_"+net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().substring(9)));
        copy.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of(),List.of(),List.of(),List.of(color&0xFFFFFF)));
        var render=new ItemStackRenderState();resolver.updateForLiving(render,copy,ItemDisplayContext.NONE,wearer);render.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,outline);
    }
}

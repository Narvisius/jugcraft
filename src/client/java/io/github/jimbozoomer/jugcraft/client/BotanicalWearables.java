package io.github.jimbozoomer.jugcraft.client;
import com.google.gson.JsonParser;
import com.mojang.math.Axis;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.botany.*;
import io.github.jimbozoomer.jugcraft.concordance.trinket.WornDisplay;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/** Original cloak cuboids, original pendant/icon models and source wing articulation. */
final class BotanicalWearables {
    private static ItemModelResolver resolver;private static ModelPart cloak,belt;
    static void register(){
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((t,r,h,c)->{resolver=c.getItemModelResolver();BotanicalCosmeticRenderer.resolver=resolver;});
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).registerReloadListener(Jugcraft.id("botanical_wearables"),(net.minecraft.server.packs.resources.ResourceManagerReloadListener)r->{cloak=null;belt=null;});
        for(var name:Jugtania.ITEMS.keySet())if(Jugtania.item(name) instanceof eu.pb4.trinkets.api.callback.TrinketCallback)TrinketRendererRegistry.registerRenderer(Jugtania.item(name),c->(stack,access,source,pose,collector,light,state,yaw,pitch)->{
            var wearer=access.inventory().getAttachment().getEntity();if(resolver==null||!(source instanceof HumanoidModel<?> body)||state.isInvisible||wearer.isSpectator()||WornDisplay.hidden(wearer)||BotanicalArmor.invisible(stack))return;
            pose.pushPose();
            if(BotanicalCosmeticRenderer.render(name,body,stack,wearer,pose,collector,light,state.outlineColor)){}else if(name.equals("tiny_planet")){body.head.translateAndRotate(pose);pose.translate(0,-.65,.25);pose.scale(.5F,-.5F,-.5F);var render=new ItemStackRenderState();resolver.updateForLiving(render,new ItemStack(Jugtania.item("tiny_planet_block")),ItemDisplayContext.NONE,wearer);render.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);}else if(name.equals("itemfinder")){body.head.translateAndRotate(pose);pose.translate(-.35,-.2,wearer.getItemBySlot(EquipmentSlot.HEAD).isEmpty()?.1:.05);pose.scale(.75F,-.75F,-.75F);icon(stack,"itemfinder_gem",wearer).submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);}else if(name.endsWith("cloak")&&!name.equals("invisibility_cloak")){
                body.body.translateAndRotate(pose);pose.translate(0,wearer.getItemBySlot(EquipmentSlot.CHEST).isEmpty()?-.01:-.07,0);
                String skin=name.equals("holy_cloak")?"cloak_holy":name.equals("unholy_cloak")?"cloak_unholy":"cloak_balance";
                for(int layer=0;layer<2;layer++)collector.order(layer).submitModelPart(cloak(),pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/jugtania/model/"+skin+(layer==1?"_glow":"")+".png")),layer==1?net.minecraft.util.LightCoordsUtil.FULL_BRIGHT:light,OverlayTexture.NO_OVERLAY,null,-1,state.outlineColor);
            }else if(name.endsWith("belt")){
                body.body.translateAndRotate(pose);pose.scale(1.15F,1.15F,1.15F);String skin=switch(name){case "travel_belt"->"belt_travel";case "super_travel_belt"->"belt_travel_super";case "speed_up_belt"->"belt_speed";default->"belt_knockback";};
                collector.order(0).submitModelPart(belt(),pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/jugtania/model/"+skin+".png")),light,OverlayTexture.NO_OVERLAY,null,-1,state.outlineColor);
            }else if(name.equals("flight_tiara")){
                int style=stack.getOrDefault(BotanicalMovement.WINGS,0);if(style>0){
                    body.body.translateAndRotate(pose);boolean flying=wearer instanceof Player p&&p.getAbilities().flying;float flap=20+(float)(Math.sin(state.ageInTicks*(flying?.4:.2))+.5)*(flying?30:5);int color=style==7?0xFFB0DEFF:-1;
                    var wing=icon(stack,"tiara_wing_"+style,wearer);pose.translate(0,style==4?-.2:style==5?-.4:.5,.2);
                    for(int side=0;side<(style==2?1:2);side++){
                        pose.pushPose();pose.rotateDegrees(Axis.YP,side==0?flap:180-flap);
                        if(style==3){pose.translate(-.8,-.35,.05);pose.scale(1.6F,-1.6F,-1.6F);}
                        else if(style==5){pose.translate(-1.3,0,0);pose.scale(2.5F,-2.5F,-2.5F);}
                        else{pose.translate(style==2?-1.1:style==7?-.7:-1,0,0);if(style!=7)pose.rotateDegrees(Axis.ZP,-60);float scale=style==2?1.6F:1.5F;pose.scale(scale,-scale,-scale);}
                        wing.submit(pose,collector,style==4||style==5||style==7?net.minecraft.util.LightCoordsUtil.FULL_BRIGHT:light,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();
                    }
                }
            }else if(name.endsWith("pendant")){
                body.body.translateAndRotate(pose);pose.translate(-.3,.4,wearer.getItemBySlot(EquipmentSlot.CHEST).isEmpty()?.12:.05);pose.scale(.5F,-.5F,-.5F);
                icon(stack,name+"_gem",wearer).submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);
                if(name.equals("blood_pendant"))icon(stack,"blood_pendant_chain",wearer).submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);
            }else if(name.equals("third_eye")){
                body.body.translateAndRotate(pose);pose.translate(-.3,.6,wearer.getItemBySlot(EquipmentSlot.CHEST).isEmpty()?.15:.1);pose.scale(.5F,-.5F,-.5F);
                for(int i=0;i<3;i++){pose.pushPose();if(i==1)pose.translate(Math.sin(state.ageInTicks/30)*.025,Math.cos(state.ageInTicks/60)*.025,-.025);else if(i==2)pose.translate(0,0,-.05);icon(stack,"third_eye_"+i,wearer).submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();}
            }else if(name.equals("diva_charm")||name.equals("goddess_charm")){
                body.head.translateAndRotate(pose);pose.translate(name.equals("diva_charm")?.15:.275,-.4,name.equals("diva_charm")?-.35:0);pose.scale(.4F,-.4F,-.4F);var render=new ItemStackRenderState();resolver.updateForLiving(render,stack,ItemDisplayContext.NONE,wearer);render.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);
            }
            pose.popPose();
        });
    }
    private static ItemStackRenderState icon(ItemStack original,String name,LivingEntity wearer){var copy=original.copy();copy.set(DataComponents.ITEM_MODEL,Jugcraft.id("jugtania_worn_"+name));var state=new ItemStackRenderState();resolver.updateForLiving(state,copy,ItemDisplayContext.NONE,wearer);return state;}
    private static ModelPart cloak(){
        if(cloak==null)try(var reader=Minecraft.getInstance().getResourceManager().getResourceOrThrow(Jugcraft.id("newest_jugtania_models/CloakModel.json")).openAsReader()){cloak=RecoveredWildlifeModel.bake(JsonParser.parseReader(reader).getAsJsonObject());}catch(java.io.IOException e){throw new IllegalStateException("Missing supplied cloak model",e);}return cloak;
    }
    private static ModelPart belt(){if(belt==null){var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,16).addBox(-4,0,-2,8,12,4),PartPose.ZERO);belt=LayerDefinition.create(mesh,64,32).bakeRoot();}return belt;}
}

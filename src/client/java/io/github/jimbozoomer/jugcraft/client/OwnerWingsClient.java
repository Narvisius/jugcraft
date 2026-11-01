package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wings.OwnerWings;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;

/** Original six rigs, all 83 paired skins, and source idle/crouch/glide wing poses. */
public final class OwnerWingsClient {
    private OwnerWingsClient(){}
    private static String rig(String name){
        if(name.equals("flandres_wings"))return "flandres";
        if(name.equals("discords_wings"))return "discords";
        if(name.equals("zanzas_wings"))return "zanzas";
        if(name.endsWith("light_wings"))return "light";
        return name.endsWith("dragon_wings")||name.endsWith("mechanical_leather_wings")?"leather":"feathered";
    }
    private static ModelPart model(String kind){return switch(kind){
        case "leather"->OwnerWingMeshes.leather().bakeRoot();case "light"->OwnerWingMeshes.light().bakeRoot();
        case "flandres"->OwnerWingMeshes.flandres().bakeRoot();case "discords"->OwnerWingMeshes.discords().bakeRoot();
        case "zanzas"->OwnerWingMeshes.zanzas().bakeRoot();default->OwnerWingMeshes.feathered().bakeRoot();};}
    public static void register(){
        ClientTickEvents.END_CLIENT_TICK.register(client->{var player=client.player;
            if(player!=null&&client.gui.screen()==null&&!client.isPaused()&&client.options.keyUp.isDown())OwnerWings.boost(player);
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,Jugcraft.id("wing_stamina"),(g,delta)->{
            var client=Minecraft.getInstance();var p=client.player;
            if(p==null||client.gui.hud.isHidden()||OwnerWings.worn(p).isEmpty())return;
            int stamina=OwnerWings.stamina(p);if(stamina>=OwnerWings.MAX_STAMINA&&!p.isFallFlying())return;
            int x=g.guiWidth()/2-40,y=g.guiHeight()-65;
            g.fill(x-2,y-2,x+82,y+6,0xC0182638);g.fill(x,y,x+80,y+4,0xFF35475B);
            g.fill(x,y,x+stamina*80/OwnerWings.MAX_STAMINA,y+4,stamina<60?0xFFE8A06B:0xFFB0DEF2);
            g.text(client.font,"Wings",x,y-11,0xFFD9EEFC,false);
        });
        OwnerWings.ITEMS.forEach((name,item)->TrinketRendererRegistry.registerRenderer(item,context->{
            String kind=rig(name);var root=model(kind);
            var first=Jugcraft.id("textures/newest/skyweave/entity/icarus/wings/"+name+".png");
            var second=Jugcraft.id("textures/newest/skyweave/entity/icarus/wings/"+name+"_2.png");
            return (stack,access,source,pose,collector,light,state,yaw,pitch)->{
                var wearer=access.inventory().getAttachment().getEntity();
                if(!(source instanceof HumanoidModel<?> body)||state.isInvisible||wearer.isSpectator()||io.github.jimbozoomer.jugcraft.concordance.trinket.WornDisplay.hidden(wearer))return;
                root.getAllParts().forEach(ModelPart::resetPose);animate(root,wearer,kind);
                pose.pushPose();body.body.translateAndRotate(pose);pose.translate(0,0,.125);
                collector.order(0).submitModelPart(root,pose,RenderTypes.entityCutout(first),light,OverlayTexture.NO_OVERLAY,null);
                collector.order(1).submitModelPart(root,pose,RenderTypes.entityCutout(second),light,OverlayTexture.NO_OVERLAY,null);
                pose.popPose();
            };
        }));
    }
    private static void animate(ModelPart root,LivingEntity entity,String kind){
        boolean flying=entity.isFallFlying(),crouching=entity.isCrouching();
        float pitch=.4f,roll=-.5f,offset=-1,yaw=0,frequency=.125f,amplitude=.1f;
        if(flying){
            double downward=entity.getDeltaMovement().normalize().y;
            float t=downward<0?1-(float)Math.pow(-downward,1.5):1;
            pitch=t*.35f+(1-t)*pitch;roll=t*-1.6f+(.3f-t)*roll;
            if(entity.zza>0){frequency=.4f;amplitude=1;}
        }else if(crouching){pitch=.7f;offset=0;yaw=.09f;}
        pitch+=(float)Math.cos(entity.tickCount*frequency)*amplitude;
        var left=root.getChild("leftWing");var right=root.getChild("rightWing");
        left.x=7;right.x=-7;left.y=right.y=offset;left.xRot=right.xRot=pitch;
        left.yRot=yaw;right.yRot=-yaw;left.zRot=roll;right.zRot=-roll;
        if(!kind.equals("light")&&!kind.equals("zanzas")){
            float angle=(float)Math.toRadians(kind.equals("flandres")?(flying?45:-50):(flying?-32.5:-60));
            left.getChild("leftWing01").getChild("leftWing02").getChild("leftWing03").xRot=angle;
            right.getChild("rightWing01").getChild("rightWing02").getChild("rightWing03").xRot=angle;
        }
    }
}

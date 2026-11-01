package io.github.jimbozoomer.jugcraft.client;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.*;
import com.zigythebird.playeranimcore.enums.PlayState;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

/** Supplied gestures retargeted onto the approved Player Animation Library.
 * This layer owns only wilds axe/crystal/empty-hand solar actions, never ArmsMotion weapons. */
public final class WildPlayerAnimations {
    private static final Identifier LAYER=Jugcraft.id("wilds_equipment");
    private record Action(String clip,String item,long start,long end){}
    private static final Map<Player,Action> ACTIVE=new WeakHashMap<>();
    private static final Map<Player,String> ICE=new WeakHashMap<>();
    static void register(){
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER,960,avatar->{var c=new PlayerAnimationController(avatar,(controller,state,set)->PlayState.STOP);c.setFirstPersonMode(com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode.NONE);return c;});
        ClientPlayNetworking.registerGlobalReceiver(WildPlayerActionPayload.TYPE,(packet,context)->{
            var client=context.client();if(client.level==null)return;
            if(client.level.getEntity(packet.player()) instanceof Player p&&p.getMainHandItem().getItem() instanceof WildWroughtEquipment.Axe)
                play(p,packet.slam()?"axe_slam":p.getMainArm()==HumanoidArm.LEFT?"axe_sweep_left":"axe_sweep_right","axe",34);
        });
        ClientPlayNetworking.registerGlobalReceiver(WildSunPosePayload.TYPE,(packet,context)->{
            var client=context.client();if(client.level==null||packet.move()<0||packet.move()>3)return;
            if(client.level.getEntity(packet.player()) instanceof Player p&&p.getMainHandItem().isEmpty()){
                String clip=switch(packet.move()){case 0->"sunstrike";case 1->"solar_flare";case 2->"solar_beam_charge";default->"supernova";};
                play(p,clip,"sun",packet.move()==0?19:packet.move()==1?28:packet.move()==2?108:116);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{ACTIVE.clear();ICE.clear();});
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(client.level==null){ACTIVE.clear();ICE.clear();return;}
            for(var p:client.level.players()){
                if(!p.isAlive()||p.isSpectator()||WildCold.frozen(p)){stop(p);ICE.remove(p);continue;}
                boolean ice=p.isUsingItem()&&p.getUseItem().getItem() instanceof WildCold.Crystal;
                if(ice){String phase=p.getTicksUsingItem()<10?"ice_breath_start":"ice_breath_loop";
                    if(!phase.equals(ICE.put(p,phase)))play(p,phase,"ice",phase.endsWith("loop")?Integer.MAX_VALUE:11);
                }else if(ICE.remove(p)!=null)play(p,"ice_breath_end","ice",5);
                var action=ACTIVE.get(p);if(action==null)continue;
                boolean valid=switch(action.item){case "axe"->p.getMainHandItem().getItem() instanceof WildWroughtEquipment.Axe;case "sun"->p.getMainHandItem().isEmpty();default->p.getMainHandItem().getItem() instanceof WildCold.Crystal||p.getOffhandItem().getItem() instanceof WildCold.Crystal;};
                if(!valid||p.level().getGameTime()>action.end)stop(p);
            }
            ACTIVE.keySet().removeIf(p->p.isRemoved()||p.level()!=client.level);ICE.keySet().removeIf(p->p.isRemoved()||p.level()!=client.level);
        });
    }
    private static void play(Player p,String clip,String item,int duration){
        ACTIVE.put(p,new Action(clip,item,p.level().getGameTime(),p.level().getGameTime()+duration));
        if(PlayerAnimationAccess.getPlayerAnimationLayer(p,LAYER) instanceof PlayerAnimationController c)c.triggerAnimation(Jugcraft.id("wilds_"+clip));
    }
    private static void stop(Player p){if(ACTIVE.remove(p)!=null&&PlayerAnimationAccess.getPlayerAnimationLayer(p,LAYER) instanceof PlayerAnimationController c){c.stopTriggeredAnimation();c.stop();}}
    static String clip(Player p){var a=ACTIVE.get(p);return a==null?"":a.clip;}
    static float age(Player p,float partial){var a=ACTIVE.get(p);return a==null?0:p.level().getGameTime()-a.start+partial;}
}

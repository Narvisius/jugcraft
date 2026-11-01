package io.github.jimbozoomer.jugcraft.botany;
import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyArtifacts;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative jump/dodge limits and finite Tiara flight using the shared flight lease. */
public final class BotanicalMovement {
    public static DataComponentType<Integer> FLIGHT_TIME,WINGS;
    private static final Map<ServerPlayer,Motion> STATES=new WeakHashMap<>();
    private static final class Motion {int jumps;long nextJump,nextDodge,nextDash;boolean sprinting;}
    static void register(){
        FLIGHT_TIME=component("botanical_flight_time",1200);WINGS=component("botanical_wing_style",8);
        PayloadTypeRegistry.serverboundPlay().register(Input.TYPE,Input.CODEC);ServerPlayNetworking.registerGlobalReceiver(Input.TYPE,(data,c)->c.server().execute(()->input(c.player(),data.action)));
        ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:s.getPlayerList().getPlayers())tick(p);});ServerLifecycleEvents.SERVER_STOPPED.register(s->STATES.clear());
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->STATES.remove(h.getPlayer()));
    }
    private static DataComponentType<Integer> component(String id,int max){return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id(id),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,max)).networkSynchronized(ByteBufCodecs.VAR_INT).build());}
    public static boolean hasJump(Player p){return BotanicalAccessories.has(p,"cloud_pendant")||BotanicalAccessories.has(p,"super_cloud_pendant");}
    private static void input(ServerPlayer p,int action){
        if(action<0||action>4||!p.isAlive()||p.isSpectator()||p.isPassenger()||p.getAbilities().flying||p.isFallFlying())return;var state=STATES.computeIfAbsent(p,k->new Motion());long now=p.level().getGameTime();
        if(action==0){
            int allowed=BotanicalAccessories.has(p,"super_cloud_pendant")?2:BotanicalAccessories.has(p,"cloud_pendant")?1:0;
            if(p.onGround()||p.isInWater()||p.isInLava()||state.jumps>=allowed||now<state.nextJump)return;state.jumps++;state.nextJump=now+5;
            var velocity=p.getDeltaMovement();double boost=p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH);p.setDeltaMovement(velocity.x,Math.max(.42,boost),velocity.z);p.resetFallDistance();p.syncVelocity=true;
            p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,p.getX(),p.getY(),p.getZ(),8,.2,.05,.2,.02);
        }else{
            var ring=BotanicalAccessories.equipped(p).get("dodge_ring");if(ring==null||!p.onGround()||now<state.nextDodge)return;state.nextDodge=now+20;ring.set(BotanicalAccessories.COOLDOWN,20);
            double angle=Math.toRadians(p.getYRot()+(action==1?0:action==2?180:action==3?90:-90));p.setDeltaMovement(-Math.sin(angle)*1.25,p.getDeltaMovement().y,Math.cos(angle)*1.25);p.syncVelocity=true;
        }
    }
    private static void tick(ServerPlayer p){
        var state=STATES.computeIfAbsent(p,k->new Motion());if(p.onGround()||p.isInWater()||p.isInLava())state.jumps=0;
        var tiara=BotanicalAccessories.equipped(p).get("flight_tiara");if(tiara==null||!p.isAlive()||p.isSpectator()||p.isPassenger()){state.sprinting=false;return;}
        boolean relicFlight=BotanicalRelics.carries(p,"flugel_eye");int time=relicFlight?1200:tiara.getOrDefault(FLIGHT_TIME,1200);boolean flying=p.getAbilities().flying;boolean gliding=!flying&&p.isShiftKeyDown()&&!p.onGround()&&p.getDeltaMovement().y<-.15;
        boolean mana=BotanicalEquipment.available(p)>=BotanicalArmor.price(p,35);
        if(time>(flying?0:120)&&mana){
            if(!flying||p.isCreative()||BotanicalEquipment.pay(p,35))AlchemyArtifacts.grantRitualFlight(p,2);
            if(flying&&!p.isCreative()&&!relicFlight)time=Math.max(0,time-1);
        }
        if(!flying&&time<1200&&p.tickCount%(gliding?6:2)==0)time++;
        if(gliding){var forward=p.getLookAngle().multiply(1,0,1).normalize().scale(.6);p.setDeltaMovement(forward.x,Math.max(-.15,p.getDeltaMovement().y),forward.z);p.fallDistance=Math.min(p.fallDistance,2);p.syncVelocity=true;}
        long now=p.level().getGameTime();if(flying&&p.isSprinting()&&!state.sprinting&&now>=state.nextDash&&!BotanicalAdvancedTools.temperance(p)){
            state.nextDash=now+40;p.setDeltaMovement(p.getDeltaMovement().add(p.getLookAngle().multiply(1,0,1).normalize()));p.syncVelocity=true;p.level().playSound(null,p.blockPosition(),BotanicalSounds.get("dash"),net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        }
        state.sprinting=p.isSprinting();if(time!=tiara.getOrDefault(FLIGHT_TIME,1200))tiara.set(FLIGHT_TIME,time);
    }
    public record Input(int action) implements CustomPacketPayload {
        public static final Type<Input> TYPE=new Type<>(Jugcraft.id("botanical_movement"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Input> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Input::action,Input::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}

package io.github.jimbozoomer.jugcraft.wilds;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Empty-hand solar controls. Packets select a move, never a target, position, cost or cooldown. */
public final class WildSunCasting {
    public record Action(int move) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(Jugcraft.id("wilds_sun_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Action::move,Action::new);
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private record Cast(ServerLevel level,int move,long start,Vec3 origin){}
    private static final Map<UUID,Cast> CASTS=new HashMap<>();
    private static final Map<ServerPlayer,Long> COOLDOWN=new WeakHashMap<>();
    public static boolean ready(Player p){return WildSunEquipment.BLESSING!=null&&p.isAlive()&&!p.isSpectator()&&p.getMainHandItem().isEmpty()&&p.hasEffect(WildSunEquipment.BLESSING)&&!WildCold.frozen(p);}
    static void register(){PayloadTypeRegistry.clientboundPlay().register(WildSunPosePayload.TYPE,WildSunPosePayload.CODEC);PayloadTypeRegistry.serverboundPlay().register(Action.TYPE,Action.CODEC);ServerPlayNetworking.registerGlobalReceiver(Action.TYPE,(action,context)->context.server().execute(()->cast(context.player(),action.move())));
        UseItemCallback.EVENT.register((p,l,h)->ready(p)?InteractionResult.FAIL:InteractionResult.PASS);
        UseBlockCallback.EVENT.register((p,l,h,hit)->ready(p)?InteractionResult.FAIL:InteractionResult.PASS);
        AttackBlockCallback.EVENT.register((p,l,h,pos,face)->l.isClientSide()&&ready(p)&&p.isShiftKeyDown()?InteractionResult.FAIL:InteractionResult.PASS);
        AttackEntityCallback.EVENT.register((p,l,h,e,hit)->l.isClientSide()&&ready(p)&&p.isShiftKeyDown()?InteractionResult.FAIL:InteractionResult.PASS);
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{CASTS.remove(handler.player.getUUID());COOLDOWN.remove(handler.player);});
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{CASTS.clear();COOLDOWN.clear();});
        ServerTickEvents.END_SERVER_TICK.register(server->{var it=CASTS.entrySet().iterator();while(it.hasNext()){var entry=it.next();var p=server.getPlayerList().getPlayer(entry.getKey());var cast=entry.getValue();if(p==null||!p.isAlive()||p.isSpectator()||p.level()!=cast.level||!p.getMainHandItem().isEmpty()||WildCold.frozen(p)){it.remove();continue;}int age=(int)(p.level().getGameTime()-cast.start);if(cast.move==1&&age==12){var victims=new ArrayList<LivingEntity>();p.level().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),p.getBoundingBox().inflate(4),e->WildSolar.canHarm(p,e),victims,64);for(var e:victims)if(p.distanceToSqr(e)<=16&&p.hasLineOfSight(e)&&e.hurtServer(p.level(),p.damageSources().playerAttack(p),2)){e.knockback(1.5,p.getX()-e.getX(),p.getZ()-e.getZ(),p.damageSources().playerAttack(p),2);}p.playSound(OwnerWilds.sound("umvuthi.burst"),1.7F,1.5F);}
            if(cast.move==2&&age==22)WildSolar.spawn(p.level(),p,WildSolar.BEAM,p.getEyePosition(),55);
            if(cast.move==3&&age==44)WildSolar.spawn(p.level(),p,WildSolar.NOVA,p.position().add(0,1,0),40);
            if(age>(cast.move==1?30:100))it.remove();}});
    }
    private static void cast(ServerPlayer p,int move){if(move<0||move>3||!ready(p)||CASTS.containsKey(p.getUUID())||COOLDOWN.getOrDefault(p,Long.MIN_VALUE)>p.level().getGameTime()||move!=0&&!p.isShiftKeyDown())return;
        Vec3 strike=null;if(move==0){var start=p.getEyePosition();var end=start.add(p.getLookAngle().scale(20));for(int i=0;i<=20;i++)if(!p.level().hasChunkAt(net.minecraft.core.BlockPos.containing(start.add(p.getLookAngle().scale(i)))))return;var hit=p.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(hit.getType()!=HitResult.Type.BLOCK||!p.level().canSeeSky(hit.getBlockPos().above()))return;strike=net.minecraft.world.phys.Vec3.atBottomCenterOf(hit.getBlockPos().above());}
        var blessing=p.getEffect(WildSunEquipment.BLESSING);int remaining=blessing.isInfiniteDuration()?72000:blessing.getDuration(),cost=move==2?6000:move==3?remaining:0;if(remaining<cost)return;
        if(move==0&&!WildSolar.spawn(p.level(),p,WildSolar.STRIKE,strike,43))return;
        p.removeEffect(WildSunEquipment.BLESSING);if(remaining>cost)p.addEffect(new MobEffectInstance(WildSunEquipment.BLESSING,remaining-cost,0,false,false));
        long now=p.level().getGameTime();COOLDOWN.put(p,now+(move==0?30:move==1?30:100));if(move!=0)CASTS.put(p.getUUID(),new Cast(p.level(),move,now,p.position()));
        var pose=new WildSunPosePayload(p.getId(),move);for(var watcher:PlayerLookup.tracking(p))ServerPlayNetworking.send(watcher,pose);ServerPlayNetworking.send(p,pose);
        if(move==3)p.playSound(OwnerWilds.sound("supernova.start"),3,1);
    }
}

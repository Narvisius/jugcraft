package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;

/** Explicit armor actions. Boots share Swiftwolf's flight lease rather than owning a second flight flag. */
public final class GemAbilities {
    private GemAbilities(){}
    public static void register(){PayloadTypeRegistry.serverboundPlay().register(Action.TYPE,Action.CODEC);ServerPlayNetworking.registerGlobalReceiver(Action.TYPE,(a,c)->activate(c.player(),a.action()));}
    private static ItemStack piece(ServerPlayer p,EquipmentSlot slot,String id){var s=p.getItemBySlot(slot);return s.is(ExtendedAlchemyEquipment.ITEMS.get(id))&&s.has(AlchemicalGear.ACTIVE)?s:ItemStack.EMPTY;}
    public static boolean flight(ServerPlayer p){var boots=piece(p,EquipmentSlot.FEET,"gem_boots");return !boots.isEmpty()&&AlchemyArtifacts.mode(boots)==1;}
    public static void pulse(ServerPlayer p){
        var helmet=piece(p,EquipmentSlot.HEAD,"gem_helmet");
        if(!helmet.isEmpty()&&AlchemyArtifacts.mode(helmet)==1){
            var old=p.getEffect(MobEffects.NIGHT_VISION);
            if((old==null||old.getDuration()<=220)&&AlchemyPower.pay(p,4))p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,240,0,false,false));
        }
    }
    public static boolean activate(ServerPlayer p,int action){
        if(action<0||action>3||!AlchemicalGear.ready(p)||!RateGate.allow(p,"gem_ability",20))return false;
        var slot=new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}[action];
        var worn=p.getItemBySlot(slot);
        if(!worn.is(ExtendedAlchemyEquipment.ITEMS.get("gem_"+new String[]{"helmet","chestplate","leggings","boots"}[action])))return false;
        switch(action){
            case 0->{AlchemyArtifacts.setMode(worn,1-AlchemyArtifacts.mode(worn)%2);}
            case 1->{AlchemyArtifacts.affectMonsters(p,"wind",6,16);}
            case 2->{
                if(p.isPassenger()||!p.onGround())return false;
                var v=p.getLookAngle().multiply(1,0,1).normalize().scale(1.1);
                if(!p.level().hasChunkAt(p.blockPosition().offset((int)Math.ceil(v.x),0,(int)Math.ceil(v.z)))
                        ||!Authority.mayChange(p.level(),p,p.blockPosition())||!p.level().noCollision(p,p.getBoundingBox().move(v))||!AlchemyPower.pay(p,64))return false;
                p.setDeltaMovement(v.x,.18,v.z);p.syncVelocity=true;
            }
            case 3->{AlchemyArtifacts.setMode(worn,1-AlchemyArtifacts.mode(worn)%2);}
        }
        // Clicking an explicit ability is sufficient to enable equipped gear; no hidden held-item ritual.
        worn.set(AlchemicalGear.ACTIVE,net.minecraft.util.Unit.INSTANCE);
        if(action==3)AlchemyArtifacts.pulse(p);
        AlchemyTelemetry.cooldown(p);p.getInventory().setChanged();AlchemyArtifacts.sound(p,"pepower");return true;
    }
    public record Action(int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(Jugcraft.id("gem_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=new StreamCodec<>(){
            public Action decode(RegistryFriendlyByteBuf b){return new Action(b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,Action a){b.writeVarInt(a.action);}
        };
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}

package io.github.jimbozoomer.jugcraft.wings;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import io.github.jimbozoomer.jugcraft.concordance.trinket.Wayfaring;
import io.github.jimbozoomer.jugcraft.concordance.trinket.WornTrinketItem;
import com.mojang.serialization.Codec;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Native glide provider; stamina belongs to the wearer, not a swappable wing item. */
public final class OwnerWings {
    private OwnerWings(){}
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    public static AttachmentType<Integer> STAMINA;
    public static Holder<MobEffect> FLIGHTLESS,ENERGIZED,SECOND_WIND;
    public static final int MAX_STAMINA=400;
    public static void register(){
        STAMINA=AttachmentRegistry.<Integer>builder().initializer(()->MAX_STAMINA).persistent(Codec.intRange(0,MAX_STAMINA)).syncWith(ByteBufCodecs.VAR_INT,AttachmentSyncPredicate.targetOnly()).buildAndRegister(Jugcraft.id("skyweave_stamina"));
        FLIGHTLESS=effect("flightless",MobEffectCategory.HARMFUL,0x997F77);
        ENERGIZED=effect("energized",MobEffectCategory.BENEFICIAL,0xEDD775);
        SECOND_WIND=effect("second_wind",MobEffectCategory.BENEFICIAL,0xB9DCEA);
        for(var value:OwnerBackpacks.source("newest_wings.json").getAsJsonArray("items")){
            String name=value.getAsString();var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("skyweave_"+name));
            ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key,new Wing(new Item.Properties().setId(key).stacksTo(1).rarity(unique(name)?Rarity.EPIC:Rarity.RARE),name)));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->ITEMS.values().forEach(e::accept));
        EntityElytraEvents.ALLOW.register(entity->!entity.hasEffect(FLIGHTLESS));
        EntityElytraEvents.CUSTOM.register((entity,tick)->canGlide(entity));
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers()){
            int stamina=stamina(p);var wing=worn(p);
            if(!p.isAlive()||p.isSpectator())continue;
            if(p.onGround()||p.hasEffect(SECOND_WIND))stamina=Math.min(MAX_STAMINA,stamina+2);
            if(!wing.isEmpty()&&canGlide(p)){
                if(p.isFallFlying()&&p.getLastClientInput().forward()&&!p.isCreative()&&!unique(((Wing)wing.getItem()).name)&&!p.hasEffect(ENERGIZED)&&stamina>0&&belowCeiling(p))stamina--;
                if(p.isFallFlying()&&p.isShiftKeyDown()){p.stopFallFlying();p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,30,0,false,false));}
            }else if(!wing.isEmpty()&&p.isFallFlying()){p.stopFallFlying();p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,30,0,false,false));}
            if(stamina!=stamina(p))p.setAttached(STAMINA,stamina);
        }});
    }
    private static Holder<MobEffect> effect(String name,MobEffectCategory category,int color){return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("skyweave_"+name),new MobEffect(category,color){});}
    public static boolean unique(String name){return name.equals("flandres_wings")||name.equals("discords_wings")||name.equals("zanzas_wings");}
    public static ItemStack worn(LivingEntity entity){var access=Wayfaring.worn(entity,s->s.getItem() instanceof Wing);return access==null?ItemStack.EMPTY:access.get();}
    public static boolean canGlide(LivingEntity entity){
        return entity instanceof Player p&&p.isAlive()&&!p.isSpectator()&&!p.isPassenger()&&!p.isInWater()&&!p.hasEffect(FLIGHTLESS)&&!worn(p).isEmpty()
            &&!p.level().dimensionTypeRegistration().is(TagKey.create(Registries.DIMENSION_TYPE,Jugcraft.id("skyweave_no_flying_allowed")));
    }
    public static int stamina(Player p){return p.getAttachedOrCreate(STAMINA);}
    public static boolean belowCeiling(Player p){return p.getY()<=p.level().getMaxY()+64;}
    public static boolean mayBoost(Player p){var stack=worn(p);return canGlide(p)&&p.isFallFlying()&&belowCeiling(p)&&(p.isCreative()||unique(((Wing)stack.getItem()).name)||p.hasEffect(ENERGIZED)||stamina(p)>0);}
    /** Called by the player's local motion simulation, like vanilla elytra physics. Server owns stamina. */
    public static void boost(Player p){
        if(!mayBoost(p))return;double armor=Math.min(3,1+p.getArmorValue()/10.0);
        Vec3 movement=p.getDeltaMovement().add(p.getLookAngle().scale(.0125/armor));
        // Bounded acceleration; firework acceleration remains vanilla and is not overwritten.
        if(movement.lengthSqr()<=16)p.setDeltaMovement(movement);
    }
    public static final class Wing extends WornTrinketItem {
        public final String name;
        Wing(Item.Properties props,String name){super(props,"skyweave_"+name,Map.of());this.name=name;}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<net.minecraft.network.chat.Component> lines,TooltipFlag flag){

            lines.accept(net.minecraft.network.chat.Component.literal("Equip in Wings. Jump again in the air to glide; hold forward to flap."));
            lines.accept(net.minecraft.network.chat.Component.literal(unique(name)?"Free flight. Sneak to land gently.":"Flapping spends stamina; landing restores it. Sneak to land gently."));
        }
    }
}

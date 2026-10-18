package io.github.jimbozoomer.jugcraft.concordance.assay;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Bucket supply and short paid wards. World placement remains ordinary bucket gameplay and its protection hooks. */
public final class ElementalAmulets {
    public static final int WATER_COST=64,LAVA_COST=4096,WATER_WARD_COST=4,FIRE_WARD_COST=8,WARD_TICKS=60;
    public static Item EVERTIDE,VOLCANITE;
    private ElementalAmulets() { }
    public static void register(){
        EVERTIDE=register("evertide_amulet",true);VOLCANITE=register("volcanite_amulet",false);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->{e.accept(EVERTIDE);e.accept(VOLCANITE);});
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers())if(p.tickCount%20==0)pulse(p);});
    }
    private static Item register(String id,boolean water){
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(id));
        return Registry.register(BuiltInRegistries.ITEM,key,new Amulet(new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.RARE),water));
    }
    public static void pulse(ServerPlayer player){
        if(!AlchemicalGear.ready(player))return;
        if(player.isUnderWater()&&AlchemicalGear.active(player,EVERTIDE))ward(player,MobEffects.WATER_BREATHING,WATER_WARD_COST);
        if((player.isInLava()||player.isOnFire())&&AlchemicalGear.active(player,VOLCANITE))ward(player,MobEffects.FIRE_RESISTANCE,FIRE_WARD_COST);
    }
    private static void ward(ServerPlayer player,Holder<MobEffect> effect,int cost){
        var existing=player.getEffect(effect);
        if(existing!=null&&(existing.getDuration()>40||existing.getAmplifier()>0)||AlchemyPower.available(player)<cost)return;
        if(player.addEffect(new MobEffectInstance(effect,WARD_TICKS,0,true,false,true)))
            AlchemyPower.pay(player, cost);
    }
    /** Refuse before consuming anything when a stacked bucket has nowhere to put its filled output. */
    public static boolean fillBucket(ServerPlayer player,InteractionHand amuletHand,boolean water){
        Item expected=water?EVERTIDE:VOLCANITE;
        if(!player.getItemInHand(amuletHand).is(expected)||!AlchemicalGear.ready(player))return false;
        InteractionHand other=amuletHand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        ItemStack bucket=player.getItemInHand(other);
        if(!bucket.is(Items.BUCKET)||!ItemStack.isSameItemSameComponents(bucket,new ItemStack(Items.BUCKET)))return false;
        int cost=water?WATER_COST:LAVA_COST;
        if(AlchemyPower.available(player)<cost)return false;
        int free=-1;
        if(bucket.getCount()>1){
            for(int i=0;i<36;i++)if(player.getInventory().getItem(i).isEmpty()){free=i;break;}
            if(free<0)return false;
        }
        ItemStack filled=new ItemStack(water?Items.WATER_BUCKET:Items.LAVA_BUCKET);
        AlchemyPower.pay(player, cost);
        if(bucket.getCount()==1)player.setItemInHand(other,filled);
        else{bucket.shrink(1);player.getInventory().setItem(free,filled);}
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();return true;
    }
    private static final class Amulet extends Item implements TrinketCallback {
        private final boolean water;
        Amulet(Properties p,boolean water){super(p);this.water=water;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(player instanceof ServerPlayer server&&RateGate.allow(server,"elemental_amulet",10)){
                if(!AlchemicalGear.ready(server)){
                    server.sendOverlayMessage(Component.translatable("message.jugcraft.alchemy.unavailable"));return InteractionResult.FAIL;
                }
                var stack=player.getItemInHand(hand);
                if(player.isShiftKeyDown()){
                    boolean on=!stack.has(AlchemicalGear.ACTIVE);
                    if(on)stack.set(AlchemicalGear.ACTIVE,Unit.INSTANCE);else stack.remove(AlchemicalGear.ACTIVE);
                    server.sendOverlayMessage(Component.translatable("message.jugcraft.amulet.toggle",stack.getHoverName(),
                            Component.translatable(on?"screen.jugcraft.alchemy.on":"screen.jugcraft.alchemy.off")));
                    if(on)pulse(server);
                }else if(!fillBucket(server,hand,water))server.sendOverlayMessage(Component.translatable("message.jugcraft.amulet.bucket",water?WATER_COST:LAVA_COST));
                else server.sendOverlayMessage(Component.translatable("message.jugcraft.amulet.filled",Assaying.balance(server)));
            }
            return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult useOn(UseOnContext context){
            return context.getPlayer()==null?InteractionResult.PASS:use(context.getLevel(),context.getPlayer(),context.getHand());
        }
        @Override public boolean canEquip(ItemStack stack,TrinketSlotAccess slot,LivingEntity entity){return entity instanceof Player;}
        @Override public TrinketDropRule getDropRule(ItemStack stack,TrinketSlotAccess slot,LivingEntity entity){
            return slot.cosmetic()?TrinketDropRule.KEEP:TrinketCallback.super.getDropRule(stack,slot,entity);
        }
        @Override public boolean isFoil(ItemStack stack){return stack.has(AlchemicalGear.ACTIVE)||super.isFoil(stack);}
        @Override public void appendHoverText(ItemStack stack,TooltipContext c,TooltipDisplay display,Consumer<Component> text,TooltipFlag flag){
            text.accept(Component.translatable(water?"tooltip.jugcraft.evertide_amulet":"tooltip.jugcraft.volcanite_amulet").withStyle(ChatFormatting.GRAY));
            text.accept(Component.translatable("tooltip.jugcraft.amulet.controls").withStyle(ChatFormatting.GRAY));
            text.accept(Component.translatable(stack.has(AlchemicalGear.ACTIVE)?"screen.jugcraft.alchemy.on":"screen.jugcraft.alchemy.off").withStyle(ChatFormatting.AQUA));
        }
    }
}

package io.github.jimbozoomer.jugcraft.nouveau;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

public final class NouveauPotions {
    public static DataComponentType<Integer> DOSES,JAR_DOSES,SELECTED;
    private NouveauPotions(){}
    public static boolean consumeDose(ItemStack stack){int doses=stack.getOrDefault(DOSES,0);if(doses<=0)return false;stack.set(DOSES,doses-1);return true;}
    public static void register(){
        DOSES=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("nouveau_doses"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,8)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        JAR_DOSES=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("nouveau_jar_doses"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,100)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        SELECTED=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("nouveau_potion_selected"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,35)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        NouveauPotionMenu.register();
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("nouveau_fill_flask"),NouveauFlaskRecipe.SERIALIZER);
        NouveauPotionBolt.register();
        for(var e:NouveauCatalog.ITEM_ENTRIES.values())if(e.kind().equals("potion"))NouveauCatalog.registerItem(e,p->new Flask(e.source(),p.stacksTo(1)));
    }
    public static void apply(ServerPlayer actor,PotionContents contents,LivingEntity target,float scale){
        for(var effect:contents.getAllEffects()){
            if(target!=actor&&!effect.getEffect().value().isBeneficial()&&(!ConcordanceEffects.mayHarm(actor,target)||!Authority.mayStrike(actor,target)))continue;
            if(effect.getEffect().value().isInstantaneous())effect.getEffect().value().applyInstantaneousEffect(actor.level(),actor,actor,target,effect.getAmplifier(),scale);
            else target.addEffect(new MobEffectInstance(effect.getEffect(),Math.max(1,(int)(effect.getDuration()*scale)),effect.getAmplifier()),actor);
        }
    }
    public static boolean ammo(ItemStack s){return s.has(DataComponents.POTION_CONTENTS)&&(s.is(Items.POTION)||s.getItem() instanceof Flask f&&!f.name.endsWith("cannon")&&s.getOrDefault(DOSES,0)>0);}
    public static final class Flask extends Item implements NouveauInstruments.Visual {
        private final String name;
        public Flask(String name,Properties p){super(p);this.name=name;}
        @Override public String source(){return name;}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){
            if(!(p instanceof ServerPlayer actor))return InteractionResult.SUCCESS;
            if(!JugNouveau.enabled()||p.isSpectator()||!RateGate.allow(actor,"nouveau_potion",5))return InteractionResult.FAIL;
            var flask=p.getItemInHand(hand);
            if(name.endsWith("cannon")){
                if(p.isShiftKeyDown()){actor.openMenu(new SimpleMenuProvider((id,inv,who)->new NouveauPotionMenu(id,inv,hand),net.minecraft.network.chat.Component.literal("Flask Cannon · Ammunition")));return InteractionResult.SUCCESS;}
                int selected=flask.getOrDefault(SELECTED,0);var ammo=p.getInventory().getItem(selected);
                if(!ammo(ammo)){for(int i=0;i<36;i++)if(ammo(p.getInventory().getItem(i))){ammo=p.getInventory().getItem(i);flask.set(SELECTED,i);break;}}
                if(!ammo(ammo)||!NouveauPotionBolt.launch(actor,ammo,name.startsWith("lingering")))return InteractionResult.FAIL;
                if(ammo.getItem() instanceof Flask)consumeDose(ammo);else ammo.consume(1,p);return InteractionResult.SUCCESS;
            }
            if(p.isShiftKeyDown()&&hand==InteractionHand.MAIN_HAND){
                var bottle=p.getOffhandItem();var contents=bottle.get(DataComponents.POTION_CONTENTS);if(!bottle.is(Items.POTION)||contents==null)return InteractionResult.FAIL;
                int doses=flask.getOrDefault(DOSES,0);if(doses>=8||doses>0&&!Objects.equals(contents,flask.get(DataComponents.POTION_CONTENTS)))return InteractionResult.FAIL;
                flask.set(DataComponents.POTION_CONTENTS,contents);flask.set(DOSES,doses+1);bottle.shrink(1);
                var empty=new ItemStack(Items.GLASS_BOTTLE);if(!p.getInventory().add(empty))p.drop(empty,false,net.minecraft.util.Prediction.SERVER_ONLY);return InteractionResult.SUCCESS;
            }
            if(flask.getOrDefault(DOSES,0)==0)return InteractionResult.FAIL;
            p.startUsingItem(hand);return InteractionResult.CONSUME;
        }
        @Override public int getUseDuration(ItemStack s,LivingEntity e){return 32;}
        @Override public ItemUseAnimation getUseAnimation(ItemStack s){return ItemUseAnimation.DRINK;}
        @Override public ItemStack finishUsingItem(ItemStack s,Level l,LivingEntity e){
            if(e instanceof ServerPlayer p&&s.getOrDefault(DOSES,0)>0){
                var contents=s.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
                if(name.endsWith("amplify")){
                    var effects=new ArrayList<MobEffectInstance>();for(var effect:contents.getAllEffects())effects.add(new MobEffectInstance(effect.getEffect(),effect.getDuration()/2,Math.min(4,effect.getAmplifier()+1)));
                    contents=new PotionContents(Optional.empty(),Optional.empty(),effects,Optional.empty());
                }
                apply(p,contents,p,name.endsWith("extend_time")?1.5F:1);s.set(DOSES,s.get(DOSES)-1);
            }return s;
        }
    }
}

package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.*;

/** Supplied brew formulas, native effects and reusable multi-dose vessels. */
public final class BotanicalBrewing {
    public static DataComponentType<String> BREW;
    public static DataComponentType<Integer> DOSES;
    public static Holder<MobEffect> FEATHER,ALLURE,SOUL,EMPTY,BLOOD;
    public record Brew(int mana,int color,List<MobEffectInstance> effects){}
    public static final Map<String,Brew> BREWS=new LinkedHashMap<>();
    private BotanicalBrewing(){}
    private static Holder<MobEffect> effect(String id,int color){return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("jugtania_"+id),new NativeEffect(id,color));}
    static void register(){
        BREW=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_brew"),DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
        DOSES=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_doses"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,6)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        FEATHER=effect("feather_feet",2534911);ALLURE=effect("allure",13540);SOUL=effect("soul_cross",4670781);EMPTY=effect("emptiness",0xFACFFF);BLOOD=effect("bloodthirst",0xC30000);
        add("speed",4000,5879807,MobEffects.SPEED,1800,1);add("strength",4000,0xEE3F3F,MobEffects.STRENGTH,1800,1);add("haste",4000,16032818,MobEffects.HASTE,1800,1);
        add("healing",6000,16735948,MobEffects.INSTANT_HEALTH,1,1);add("jump_boost",4000,3339373,MobEffects.JUMP_BOOST,1800,1);add("regen",7000,16606344,MobEffects.REGENERATION,500,1);
        add("regen_weak",9000,16606344,MobEffects.REGENERATION,2400,0);add("resistance",4000,11816471,MobEffects.RESISTANCE,1800,1);add("fire_resistance",4000,16279808,MobEffects.FIRE_RESISTANCE,9600,0);
        add("water_breathing",4000,8693711,MobEffects.WATER_BREATHING,9600,0);add("invisibility",8000,0xAEAEAE,MobEffects.INVISIBILITY,9600,0);add("night_vision",4000,8145899,MobEffects.NIGHT_VISION,9600,0);add("absorption",7000,15919907,MobEffects.ABSORPTION,1800,3);
        add("allure",2000,13540,ALLURE,4800,0);add("soul_cross",10000,4670781,SOUL,1800,0);add("feather_feet",7000,2534911,FEATHER,1800,0);add("emptiness",30000,0xFACFFF,EMPTY,7200,0);add("bloodthirst",20000,0xC30000,BLOOD,7200,0);
        BREWS.put("overload",new Brew(12000,0x232323,List.of(new MobEffectInstance(MobEffects.STRENGTH,1800,3),new MobEffectInstance(MobEffects.SPEED,1800,2),new MobEffectInstance(MobEffects.HUNGER,3600,1),new MobEffectInstance(MobEffects.NAUSEA,200,2))));
        BREWS.put("clear",new Brew(4000,0xFFFFFF,List.of()));
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((dead,damage)->{if(damage.getEntity() instanceof LivingEntity killer&&killer.hasEffect(SOUL)&&killer.isAlive())killer.heal(dead.getMaxHealth()/20);});
    }
    private static void add(String id,int cost,int color,Holder<MobEffect> effect,int duration,int amplifier){BREWS.put(id,new Brew(cost,color,List.of(new MobEffectInstance(effect,duration,amplifier))));}
    public static boolean canInfuseEquipment(String id){var b=BREWS.get(id);return b!=null&&!id.equals("absorption")&&b.effects().size()==1&&!b.effects().get(0).getEffect().value().isInstantaneous();}
    static boolean vessel(ItemStack stack){return stack.is(Jugtania.item("blood_pendant"))&&!stack.has(BREW)||stack.is(Jugtania.item("incense_stick"))&&!stack.has(BREW)||stack.is(Jugtania.item("vial"))||stack.is(Jugtania.item("flask"));}
    public static ItemStack filled(String id,boolean flask){
        if(!BREWS.containsKey(id))return ItemStack.EMPTY;var stack=new ItemStack(Jugtania.item(flask?"brew_flask":"brew_vial"));stack.set(BREW,id);setDoses(stack,flask?6:4);return stack;
    }
    private static void setDoses(ItemStack stack,int doses){
        stack.set(DOSES,doses);var brew=BREWS.get(stack.getOrDefault(BREW,""));int total=stack.is(Jugtania.item("brew_flask"))?6:4;
        stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of((float)(total-doses)),List.of(),List.of(),List.of(brew==null?0xFFFFFF:brew.color())));
    }
    static void tick(BotanicalWorkshop station,ServerLevel level){
        if(!station.getItem(16).isEmpty())return;int container=-1;
        for(int i=0;i<16;i++)if(vessel(station.getItem(i))){if(container!=-1||station.getItem(i).getCount()!=1)return;container=i;}
        if(container<0)return;var offerings=new ArrayList<ItemStack>();for(int i=0;i<16;i++)offerings.add(i==container?ItemStack.EMPTY:station.getItem(i));
        for(var recipe:JugtaniaRecipes.of("brew")){
            String id=recipe.data().get("brew").getAsString();id=id.substring(id.indexOf(':')+1).replaceFirst("^jugtania_","");var brew=BREWS.get(id);if(brew==null)continue;
            var used=recipe.match(offerings);if(used==null)continue;boolean exact=true;for(int i=0;i<16;i++)if(used[i]!=offerings.get(i).getCount())exact=false;if(!exact)continue;
            boolean flask=station.getItem(container).is(Jugtania.item("flask"));boolean pendant=station.getItem(container).is(Jugtania.item("blood_pendant"));boolean incense=station.getItem(container).is(Jugtania.item("incense_stick"));if((incense||pendant)&&!BotanicalIncense.allowed(id))continue;int cost=brew.mana()*(incense||pendant?10:flask?2:1);if(station.mana()<cost)return;
            // Preserve crafting remainders rather than silently consuming their containers.
            var remainders=new ArrayList<ItemStack>();for(int i=0;i<16;i++)if(used[i]>0){var r=station.getItem(i).getItem().getCraftingRemainder();if(r!=null)remainders.add(r.create().copyWithCount(used[i]));}
            if(!station.draw(cost))return;for(int i=0;i<16;i++)station.setItem(i,ItemStack.EMPTY);for(int i=0;i<remainders.size();i++)station.setItem(i,remainders.get(i));var result=pendant?new ItemStack(Jugtania.item("blood_pendant")):incense?new ItemStack(Jugtania.item("incense_stick")):filled(id,flask);if(incense||pendant){result.set(BREW,id);result.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of(),List.of(),List.of(),List.of(brew.color())));}station.setItem(16,result);
            level.playSound(null,station.getBlockPos(),BotanicalSounds.get("potion_create"),net.minecraft.sounds.SoundSource.BLOCKS,1,1.5F);return;
        }
    }
    public static boolean preventsSpawn(ServerLevel level,net.minecraft.core.BlockPos pos){for(var p:level.players())if(!p.isSpectator()&&p.hasEffect(EMPTY)&&Math.abs(p.getX()-pos.getX())<=128&&Math.abs(p.getY()-pos.getY())<=128&&Math.abs(p.getZ()-pos.getZ())<=128)return true;return false;}
    public static boolean ignoresDarkness(ServerLevelAccessor level,net.minecraft.core.BlockPos pos){for(var p:level.getLevel().players())if(!p.isSpectator()&&p.hasEffect(BLOOD)&&!p.hasEffect(EMPTY)&&Math.abs(p.getX()-pos.getX())<=64&&Math.abs(p.getY()-pos.getY())<=64&&Math.abs(p.getZ()-pos.getZ())<=64)return true;return false;}
    private static final class NativeEffect extends MobEffect {
        private final String kind;NativeEffect(String kind,int color){super(MobEffectCategory.BENEFICIAL,color);this.kind=kind;}
        @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){return kind.equals("feather_feet");}
        @Override public boolean applyEffectTick(ServerLevel level,LivingEntity entity,int amplifier){entity.fallDistance=Math.min(entity.fallDistance,2.5);return true;}
    }
    public static final class Drink extends Item {
        private final boolean flask;public Drink(boolean flask,Properties p){super(p.stacksTo(1));this.flask=flask;}
        @Override public ItemUseAnimation getUseAnimation(ItemStack stack){return ItemUseAnimation.DRINK;}
        @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return flask?24:32;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);if(!BREWS.containsKey(stack.getOrDefault(BREW,""))||stack.getOrDefault(DOSES,0)<=0)return InteractionResult.FAIL;player.startUsingItem(hand);return InteractionResult.CONSUME;}
        @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){
            String id=stack.getOrDefault(BREW,"");var brew=BREWS.get(id);int doses=stack.getOrDefault(DOSES,0);if(!(level instanceof ServerLevel server)||brew==null||doses<=0)return stack;
            if(id.equals("clear"))user.removeAllEffects();else for(var effect:brew.effects()){
                if(effect.getEffect().value().isInstantaneous())effect.getEffect().value().applyInstantaneousEffect(server,user,user,user,effect.getAmplifier(),1);
                else user.addEffect(new MobEffectInstance(effect));
            }
            if(user instanceof Player p&&p.getAbilities().instabuild)return stack;if(doses==1)return new ItemStack(Jugtania.item(flask?"flask":"vial"));setDoses(stack,doses-1);return stack;
        }
        @Override public Component getName(ItemStack stack){String id=stack.getOrDefault(BREW,"");return Component.literal((flask?"Flask":"Vial")+" of "+id.replace('_',' ')+" ("+stack.getOrDefault(DOSES,0)+" doses)");}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,java.util.function.Consumer<Component> tooltip,TooltipFlag flag){
            var brew=BREWS.get(stack.getOrDefault(BREW,""));if(brew!=null)for(var effect:brew.effects())tooltip.accept(Component.literal(effect.getEffect().value().getDisplayName().getString()+" "+(effect.getAmplifier()+1)+" · "+effect.getDuration()/20+"s"));
        }
    }
}

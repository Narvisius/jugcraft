package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.level.ClipContext;

import com.mojang.serialization.Codec;
import eu.pb4.trinkets.api.*;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Equipped-only source accessories. Different systems own different modifier IDs. */
public final class BotanicalAccessories {
    public static final Set<String> ITEMS=Set.of("tiny_planet","itemfinder","dodge_ring","cloud_pendant","super_cloud_pendant","flight_tiara","mining_ring","water_ring","magnet_ring","magnet_ring_greater","swap_ring","reach_ring","pixie_ring","invisibility_cloak","holy_cloak","unholy_cloak","balance_cloak","travel_belt","super_travel_belt","speed_up_belt","knockback_belt","lava_pendant","super_lava_pendant","ice_pendant","third_eye","blood_pendant","goddess_charm","diva_charm");
    static DataComponentType<Integer> COOLDOWN;
    private static final Map<ServerPlayer,Motion> MOTION=new WeakHashMap<>();
    private static final Set<UUID> REFLECTING=new HashSet<>();
    private static final Map<Explosion,Boolean> EXPLOSIONS=new WeakHashMap<>();
    private record Motion(Vec3 position,float speed){}
    static void register(){
        COOLDOWN=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_accessory_cooldown"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,24000)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        AttackBlockCallback.EVENT.register((player,world,hand,pos,face)->{if(player instanceof ServerPlayer p&&hand==InteractionHand.MAIN_HAND&&has(p,"swap_ring")&&Authority.mayChange(p.level(),p,pos))correctTool(p,world.getBlockState(pos));return InteractionResult.PASS;});
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers())tick(p);});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{MOTION.clear();EXPLOSIONS.clear();REFLECTING.clear();});
    }
    static Item create(String name,Item.Properties properties){return new Accessory(name,properties.stacksTo(1));}
    public static Map<String,ItemStack> equipped(LivingEntity wearer){
        var result=new LinkedHashMap<String,ItemStack>();TrinketsApi.getAttachment(wearer).forEach((slot,stack)->{if(!slot.cosmetic()&&slot.canApplyEffects()&&stack.getItem() instanceof Accessory item)result.putIfAbsent(item.name,stack);});return result;
    }
    public static boolean has(LivingEntity wearer,String name){return !wearer.isSpectator()&&equipped(wearer).containsKey(name);}
    private static void modifier(ServerPlayer p,Holder<Attribute> attribute,String suffix,double amount){
        var instance=p.getAttribute(attribute);if(instance==null)return;var id=Jugcraft.id("botanical_"+suffix);var old=instance.getModifier(id);
        if(old!=null&&old.amount()==amount)return;if(old!=null)instance.removeModifier(id);if(amount!=0)instance.addTransientModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_VALUE));
    }
    private static void effect(ServerPlayer p,Holder<MobEffect> effect,int duration,int amplifier){
        var old=p.getEffect(effect);if(old==null||old.getAmplifier()<amplifier||old.getAmplifier()==amplifier&&old.getDuration()<duration/2)p.addEffect(new MobEffectInstance(effect,duration,amplifier,true,false));
    }
    private static void tick(ServerPlayer p){
        var worn=p.isAlive()&&!p.isSpectator()?equipped(p):Map.<String,ItemStack>of();
        for(var stack:worn.values()){int cd=stack.getOrDefault(COOLDOWN,0);if(cd>0)stack.set(COOLDOWN,cd-1);}
        String belt=worn.containsKey("super_travel_belt")?"super_travel_belt":worn.containsKey("speed_up_belt")?"speed_up_belt":worn.containsKey("travel_belt")?"travel_belt":"";
        boolean powered=!belt.isEmpty()&&BotanicalEquipment.available(p)>=BotanicalArmor.price(p,1)&&!p.isShiftKeyDown();var previous=MOTION.get(p);double traveled=previous==null?0:p.position().distanceToSqr(previous.position);boolean moving=traveled>.000001&&traveled<16;
        float speed=powered&&moving&&belt.equals("speed_up_belt")?Math.min(.25F,(previous==null?0:previous.speed)+.00035F):0;
        if(powered&&moving&&p.tickCount%10==0)powered=BotanicalEquipment.pay(p,1);
        modifier(p,Attributes.MOVEMENT_SPEED,"sash_speed",powered?(belt.equals("super_travel_belt")?.085:belt.equals("travel_belt")?.035:speed):0);
        modifier(p,Attributes.STEP_HEIGHT,"sash_step",powered?.65:0);modifier(p,Attributes.JUMP_STRENGTH,"sash_jump",powered?(belt.equals("super_travel_belt")?.3:.2):0);
        modifier(p,Attributes.SAFE_FALL_DISTANCE,"sash_fall",powered?(belt.equals("super_travel_belt")?4:2):0);MOTION.put(p,new Motion(p.position(),speed));
        if(worn.isEmpty())return;
        if(worn.containsKey("mining_ring")&&BotanicalEquipment.available(p)>=BotanicalArmor.price(p,5)){
            if(!p.isSwinging()||p.getSwingAnimation(0)>0||BotanicalEquipment.pay(p,5))effect(p,MobEffects.HASTE,4,1);
        }
        if(worn.containsKey("water_ring")&&p.isInWater()&&BotanicalEquipment.pay(p,3)){effect(p,MobEffects.CONDUIT_POWER,60,0);if(!BotanicalAdvancedTools.temperance(p))effect(p,MobEffects.DOLPHINS_GRACE,60,0);}
        if(worn.containsKey("invisibility_cloak")&&BotanicalEquipment.pay(p,2))effect(p,MobEffects.INVISIBILITY,4,0);
        if(worn.containsKey("lava_pendant")||worn.containsKey("super_lava_pendant"))p.clearFire();
        if(worn.containsKey("ice_pendant")&&!p.isShiftKeyDown()&&p.tickCount%5==0)snow(p);
        if(worn.containsKey("third_eye")&&p.tickCount%10==0){int examined=0;for(var target:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(24),e->e instanceof Enemy&&BotanicalDefenses.eligible(p.level(),p,e))){if(++examined>64)break;var glow=target.getEffect(MobEffects.GLOWING);if(glow!=null&&glow.getDuration()>2)continue;if(!BotanicalEquipment.pay(p,2))break;target.addEffect(new MobEffectInstance(MobEffects.GLOWING,12,0),p);}}
        if(worn.containsKey("blood_pendant"))blood(p,worn.get("blood_pendant"));
        var magnet=worn.getOrDefault("magnet_ring_greater",worn.get("magnet_ring"));if(magnet!=null&&magnet.getOrDefault(COOLDOWN,0)==0&&!p.isShiftKeyDown()&&!BotanicalFields.magnetBlocked(p))magnet(p,magnet,worn.containsKey("magnet_ring_greater")?12:6);
    }
    private static void blood(ServerPlayer p,ItemStack pendant){
        var brew=BotanicalBrewing.BREWS.get(pendant.getOrDefault(BotanicalBrewing.BREW,""));if(brew==null||brew.effects().size()!=1)return;var e=brew.effects().get(0);if(e.getEffect().value().isInstantaneous()||e.getDuration()<=0)return;
        float exact=(float)brew.mana()/e.getDuration()/(1+e.getAmplifier())*2.5F;int cost=(int)Math.ceil(exact);if(BotanicalEquipment.available(p)<BotanicalArmor.price(p,cost))return;
        if(exact>=1||p.getRandom().nextFloat()<exact)if(!BotanicalEquipment.pay(p,cost))return;effect(p,e.getEffect(),e.getEffect()==MobEffects.NIGHT_VISION?385:80,e.getAmplifier());
    }
    private static void correctTool(ServerPlayer p,net.minecraft.world.level.block.state.BlockState state){
        var held=p.getMainHandItem();if(!(held.getItem() instanceof BotanicalEquipment.ManaGear))return;int best=-1;float speed=held.getDestroySpeed(state);
        for(int i=0;i<p.getInventory().getContainerSize();i++){var tool=p.getInventory().getItem(i);if(tool==held||!(tool.getItem() instanceof BotanicalEquipment.ManaGear)||tool.getDestroySpeed(state)<=speed||!tool.isCorrectToolForDrops(state))continue;best=i;speed=tool.getDestroySpeed(state);}
        if(best>=0){var tool=p.getInventory().getItem(best);p.getInventory().setItem(best,held);p.setItemInHand(InteractionHand.MAIN_HAND,tool);p.getInventory().setChanged();}
    }
    private static void magnet(ServerPlayer p,ItemStack ring,int range){
        int examined=0;var itemTag=TagKey.create(Registries.ITEM,Jugcraft.id("jugtania_magnet_ring_blacklist"));var blockTag=TagKey.create(Registries.BLOCK,Jugcraft.id("jugtania_magnet_ring_blacklist"));
        for(var item:p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(range))){
            if(++examined>200)break;if(item.hasPickUpDelay()||!item.isAlive()||item.getItem().is(itemTag)||BotanicalManaItems.capacity(item.getItem())>0||BotanicalFields.magnetBlocked(item)||!Authority.mayChange(p.level(),p,item.blockPosition())||!ClaimTransfers.allowed(p.level(),item.blockPosition(),p.blockPosition()))continue;
            if(p.level().getBlockState(item.blockPosition()).is(blockTag)||p.level().getBlockState(item.blockPosition().below()).is(blockTag))continue;
            Vec3 delta=p.position().add(0,.75,0).subtract(item.position());var hit=p.level().clip(new ClipContext(item.position(),p.position().add(0,.75,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(hit.getType()!=HitResult.Type.MISS)continue;
            item.setDeltaMovement(delta.normalize().scale(Math.min(.45,delta.length())));item.syncVelocity=true;
        }
    }
    public static void tossed(Player p){for(var e:equipped(p).entrySet())if(e.getKey().startsWith("magnet_ring"))e.getValue().set(COOLDOWN,100);}
    private static void snow(ServerPlayer p){
        var level=p.level();var center=p.blockPosition();int changed=0;
        for(var at:BlockPos.betweenClosed(center.offset(-8,-1,-8),center.offset(8,-1,8))){
            if(changed>=64)break;if(at.distSqr(center)>81||!level.hasChunkAt(at)||!level.hasChunkAt(at.above())||!Authority.mayChange(level,p,at)||!level.getBlockState(at).is(Blocks.WATER)||!level.getFluidState(at).isSource()||!level.isEmptyBlock(at.above()))continue;
            var ice=Blocks.FROSTED_ICE.defaultBlockState();if(!level.isUnobstructed(ice,at,net.minecraft.world.phys.shapes.CollisionContext.empty()))continue;level.setBlockAndUpdate(at,ice);level.scheduleTick(at,Blocks.FROSTED_ICE,60+p.getRandom().nextInt(61));changed++;
        }
        for(int dx:new int[]{-1,1})for(int dz:new int[]{-1,1}){var at=BlockPos.containing(p.getX()+dx*.25,p.getY(),p.getZ()+dz*.25);var s=Blocks.SNOW.defaultBlockState();if(level.hasChunkAt(at)&&level.isEmptyBlock(at)&&s.canSurvive(level,at)&&level.getBiome(at).value().coldEnoughToSnow(at,level.getSeaLevel())&&Authority.mayChange(level,p,at))level.setBlockAndUpdate(at,s);}
    }
    public static float damage(LivingEntity victim,DamageSource source,float amount){
        if(!(victim instanceof ServerPlayer p)||p.isSpectator()||!p.isAlive()||amount<=0||source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;var worn=equipped(p);
        if(worn.containsKey("super_lava_pendant")&&source.is(DamageTypeTags.IS_FIRE))return 0;
        if(REFLECTING.contains(p.getUUID()))return amount;
        String key=worn.containsKey("holy_cloak")?"holy_cloak":worn.containsKey("balance_cloak")?"balance_cloak":worn.containsKey("unholy_cloak")?"unholy_cloak":"";var cloak=worn.get(key);
        if(cloak==null||cloak.getOrDefault(COOLDOWN,0)>0||source.is(DamageTypeTags.BYPASSES_EFFECTS))return amount;
        cloak.set(COOLDOWN,200);REFLECTING.add(p.getUUID());
        try{
            if(key.equals("holy_cloak"))amount=0;
            else if(key.equals("balance_cloak")){amount*=.5F;if(source.getEntity() instanceof LivingEntity attacker&&Authority.mayStrike(p,attacker)&&Authority.mayChange(p.level(),p,attacker.blockPosition()))attacker.hurtServer(p.level(),p.damageSources().indirectMagic(p,p),amount);amount=Math.min(amount,Math.max(0,p.getHealth()-1));}
            else{int n=0;for(var target:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(6),e->e instanceof Enemy&&BotanicalDefenses.eligible(p.level(),p,e))){if(++n>64)break;target.hurtServer(p.level(),p.damageSources().playerAttack(p),amount);}}
            p.level().playSound(null,p.blockPosition(),BotanicalSounds.get(key.equals("unholy_cloak")?"unholy_cloak":"holy_cloak"),net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        }finally{REFLECTING.remove(p.getUUID());}return amount;
    }
    /** One mana payment per explosion, not per ray or per affected block. Damage to entities is unchanged. */
    public static boolean protectExplosion(Explosion explosion,BlockGetter getter){
        if(!(getter instanceof ServerLevel level))return false;var cached=EXPLOSIONS.get(explosion);if(cached!=null)return cached;boolean protectedArea=false;var center=explosion.center();
        for(var p:level.players())if(p.isAlive()&&!p.isSpectator()&&p.position().distanceToSqr(center)<=64&&has(p,"goddess_charm")&&Authority.mayChange(level,p,BlockPos.containing(center))&&BotanicalEquipment.pay(p,1000)){protectedArea=true;break;}EXPLOSIONS.put(explosion,protectedArea);return protectedArea;
    }
    public static final class Accessory extends Item implements TrinketCallback {
        public final String name;Accessory(String name,Properties p){super(p);this.name=name;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(!name.equals("flight_tiara")||!player.isShiftKeyDown())return InteractionResult.PASS;
            if(player instanceof ServerPlayer p){var stack=p.getItemInHand(hand);int style=(stack.getOrDefault(BotanicalMovement.WINGS,0)+1)%9;stack.set(BotanicalMovement.WINGS,style);p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Tiara wing style: "+(style+1)));}return InteractionResult.SUCCESS;
        }
        @Override public boolean canEquip(ItemStack stack,TrinketSlotAccess slot,LivingEntity wearer){return wearer instanceof Player;}
        @Override public void forEachTrinketModifier(ItemStack s,TrinketSlotAccess slot,LivingEntity e,Identifier id,java.util.function.BiConsumer<Holder<Attribute>,AttributeModifier> out){
            if(slot.cosmetic()||!slot.canApplyEffects())return;
            if(name.equals("reach_ring"))out.accept(Attributes.BLOCK_INTERACTION_RANGE,new AttributeModifier(id.withSuffix("/botanical_reach"),3.5,AttributeModifier.Operation.ADD_VALUE));
            if(name.equals("knockback_belt")){out.accept(Attributes.KNOCKBACK_RESISTANCE,new AttributeModifier(id.withSuffix("/botanical_knockback"),1,AttributeModifier.Operation.ADD_VALUE));out.accept(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,new AttributeModifier(id.withSuffix("/botanical_blast"),1,AttributeModifier.Operation.ADD_VALUE));}
        }
    }
}

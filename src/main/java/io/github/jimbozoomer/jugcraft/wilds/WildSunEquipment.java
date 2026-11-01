package io.github.jimbozoomer.jugcraft.wilds;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.Level;

/** Original mask rewards and sun blessing. Equipment works immediately, with no research requirement. */
public final class WildSunEquipment {
    public static Holder<MobEffect> SUNBLOCK,BLESSING;
    public static java.util.function.Function<Mask,Object> renderer=item->null;
    static void register(){
        WildSunCasting.register();
        SUNBLOCK=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("wilds_sunblock"),new MobEffect(MobEffectCategory.BENEFICIAL,16768834){
            @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){return true;}
            @Override public boolean applyEffectTick(ServerLevel l,LivingEntity e,int amplifier){int interval=Math.max(1,50>>Math.min(5,amplifier));if(e.tickCount%interval==0)e.heal(1);return true;}
        });
        BLESSING=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("wilds_suns_blessing"),new MobEffect(MobEffectCategory.BENEFICIAL,0xFFD24A){});
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers()){if(!p.isAlive()||p.tickCount%20!=0)continue;var stack=p.getItemBySlot(EquipmentSlot.HEAD);if(stack.getItem() instanceof Mask mask&&!mask.sol){var effect=switch(mask.phase){case 0->MobEffects.STRENGTH;case 1->MobEffects.SPEED;case 2->MobEffects.HASTE;case 3->MobEffects.JUMP_BOOST;case 4->MobEffects.RESISTANCE;default->MobEffects.HEALTH_BOOST;};p.addEffect(new MobEffectInstance(effect,60,0,true,false));}}});
    }
    public static boolean masked(LivingEntity e){return e.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof Mask;}
    static void releaseFollower(ServerLevel l,UUID owner,UUID entity){if(owner!=null)WildFollowerLedger.get(l).release(owner,entity);}
    public static final class Mask extends Item implements GeoItem {
        public final int phase;public final boolean sol;
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        public Mask(int phase,boolean sol,Properties p){super(properties(p,sol));this.phase=phase;this.sol=sol;}
        private static Properties properties(Properties p,boolean sol){p=p.humanoidArmor(new ArmorMaterial(sol?25:5,Map.of(ArmorType.HELMET,sol?2:1,ArmorType.CHESTPLATE,3,ArmorType.LEGGINGS,2,ArmorType.BOOTS,1,ArmorType.BODY,3),15,SoundEvents.ARMOR_EQUIP_LEATHER,0,0,ItemTags.REPAIRS_LEATHER_ARMOR,ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("wilds_mask"))),ArmorType.HELMET);return sol?p.component(DataComponents.UNBREAKABLE,Unit.INSTANCE):p;}
        @Override public InteractionResult use(Level l,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);if(!sol&&player.getItemBySlot(EquipmentSlot.HEAD).is(OwnerWilds.item("sol_visage"))){if(player instanceof ServerPlayer p){var e=WildUmvuthana.FOLLOWER.create(l,EntitySpawnReason.MOB_SUMMONED);if(e==null)return InteractionResult.FAIL;var at=p.position().add(p.getLookAngle().multiply(1,0,1).normalize().scale(1.5)).add(0,.5,0);if(!l.hasChunkAt(BlockPos.containing(at))||!l.getWorldBorder().isWithinBounds(BlockPos.containing(at)))return InteractionResult.FAIL;e.setPos(at);if(!l.noCollision(e))return InteractionResult.FAIL;var ledger=WildFollowerLedger.get(p.level());if(!ledger.reserve(p.getUUID(),e.getUUID())){p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Recall one of your ten companions before summoning another."));return InteractionResult.FAIL;}e.bindPlayer(p,phase,stack);e.setYRot(p.getYRot());e.setDeltaMovement(p.getLookAngle().multiply(.5,0,.5).add(0,.5,0));if(!p.level().addFreshEntity(e)){ledger.release(p.getUUID(),e.getUUID());return InteractionResult.FAIL;}stack.consume(1,p);p.playSound(OwnerWilds.sound("umvuthi.belly"),1.5F,1);}return InteractionResult.SUCCESS;}
            var equipped=stack.get(DataComponents.EQUIPPABLE);return equipped==null?InteractionResult.PASS:equipped.swapWithEquipmentSlot(stack,player);
        }
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<Mask>("pose",0,s->s.setAndContinue(RawAnimation.begin().thenLoop(sol?"default":"player"))));}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override public Object getRenderProvider(){return renderer.apply(this);}
        public String model(){return sol?"sol_visage":"mask_"+WildUmvuthana.MASKS[phase];}
        public String texture(){return sol?"sol_visage":"umvuthana_mask_"+WildUmvuthana.MASKS[phase];}
    }
}

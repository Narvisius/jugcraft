package io.github.jimbozoomer.jugcraft.wilds;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauSpellState;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Source geomancy rewards: immediate equipment use, bounded world work and persistent temporary terrain. */
public final class WildEarthEquipment {
    private record Lift(ServerLevel level,BlockPos ground,ItemStack stack){}
    private static final Map<ServerPlayer,Lift> LIFTS=new WeakHashMap<>();
    private static final Map<ServerPlayer,UUID> PILLARS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Float> FALL=new WeakHashMap<>();
    private static final Map<ServerPlayer,Long> ROBE_READY=new WeakHashMap<>();
    public static java.util.function.Function<Item,Object> renderer=item->null;
    private static final TagKey<Block> EARTH=TagKey.create(Registries.BLOCK,Jugcraft.id("wilds_geomancy_useable"));
    private static final Identifier BEADS=Jugcraft.id("wilds_beads"),BELT_ARMOR=Jugcraft.id("wilds_belt_armor"),BELT_RESIST=Jugcraft.id("wilds_belt_resist");
    static void register(){
        ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:s.getPlayerList().getPlayers())tick(p);});
        ServerLivingEntityEvents.AFTER_DAMAGE.register((e,source,base,dealt,blocked)->{if(dealt<=0||blocked)return;if(e instanceof ServerPlayer p)robe(p);if(source.getEntity() instanceof ServerPlayer p&&p!=e)robe(p);});
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{LIFTS.remove(h.player);PILLARS.remove(h.player);FALL.remove(h.player);ROBE_READY.remove(h.player);});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{LIFTS.clear();PILLARS.clear();FALL.clear();ROBE_READY.clear();});
    }
    private static boolean usable(ServerLevel l,ServerPlayer p,BlockPos pos){if(!l.hasChunkAt(pos)||!Authority.mayChange(l,p,pos)||l.getBlockEntity(pos)!=null)return false;var state=l.getBlockState(pos);return state.is(EARTH)&&state.getDestroySpeed(l,pos)>=0&&state.getFluidState().isEmpty();}
    private static boolean cost(ItemStack stack,int amount){if(stack.getDamageValue()+amount>=stack.getMaxDamage())return false;stack.setDamageValue(stack.getDamageValue()+amount);return true;}
    private static void modifier(LivingEntity e,Holder<Attribute> attribute,Identifier id,double amount,boolean active){var a=e.getAttribute(attribute);if(a==null)return;if(active&&!a.hasModifier(id))a.addTransientModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_VALUE));if(!active&&a.hasModifier(id))a.removeModifier(id);}
    private static void tick(ServerPlayer p){
        boolean alive=p.isAlive()&&!p.isSpectator();var hand=p.getMainHandItem();modifier(p,Attributes.ATTACK_DAMAGE,BEADS,3,alive&&p.getItemBySlot(EquipmentSlot.HEAD).is(OwnerWilds.item("geomancer_beads"))&&(hand.isEmpty()||hand.is(OwnerWilds.item("earthrend_gauntlet"))));boolean slow=alive&&p.hasEffect(MobEffects.SLOWNESS)&&p.getItemBySlot(EquipmentSlot.LEGS).is(OwnerWilds.item("geomancer_belt"));modifier(p,Attributes.ARMOR,BELT_ARMOR,4,slow);modifier(p,Attributes.KNOCKBACK_RESISTANCE,BELT_RESIST,1,slow);
        float fall=FALL.getOrDefault(p,0F);if(p.onGround()){if(alive&&fall>4&&p.getItemBySlot(EquipmentSlot.FEET).is(OwnerWilds.item("geomancer_sandals")))p.addEffect(new MobEffectInstance(MobEffects.SPEED,60,0,false,false));FALL.remove(p);}else FALL.put(p,Math.max(fall,(float)p.fallDistance));
        if(!p.isUsingItem())LIFTS.remove(p);
        boolean rise=alive&&hand.getItem() instanceof Gauntlet&&p.isShiftKeyDown()&&p.getLastClientInput().jump()&&!WildCold.frozen(p)&&!p.isUsingItem();
        var pillar=PILLARS.containsKey(p)&&p.level().getEntity(PILLARS.get(p)) instanceof WildBoulder b?b:null;
        if(!rise){PILLARS.remove(p);return;}if(pillar==null){var below=p.blockPosition().below();if(!p.onGround()||!usable(p.level(),p,below)||hand.getDamageValue()+5>=hand.getMaxDamage())return;pillar=WildBoulder.create(p.level(),p,net.minecraft.world.phys.Vec3.atBottomCenterOf(below.above()),WildBoulder.PILLAR,1,1200);if(pillar==null)return;pillar.material(p.level().getBlockState(below));pillar.height(.05F);if(!p.level().addFreshEntity(pillar))return;PILLARS.put(p,pillar.getUUID());cost(hand,5);}
        var at=pillar.blockPosition();double nextY=pillar.getY()+pillar.height()+.2;if(pillar.height()>=24||p.position().subtract(pillar.position()).horizontalDistanceSqr()>4||!p.level().hasChunkAt(BlockPos.containing(p.getX(),nextY+2,p.getZ()))||!Authority.mayChange(p.level(),p,BlockPos.containing(p.getX(),nextY,p.getZ()))||!p.level().noCollision(p,p.getBoundingBox().move(0,.2,0))||!cost(hand,1)){PILLARS.remove(p);return;}pillar.height(pillar.height()+.2F);p.setPos(p.getX(),Math.max(p.getY(),nextY),p.getZ());p.fallDistance=0;p.setDeltaMovement(p.getDeltaMovement().multiply(1,0,1));p.needsSync=true;
    }
    private static void robe(ServerPlayer p){
        if(!p.isAlive()||!p.getItemBySlot(EquipmentSlot.CHEST).is(OwnerWilds.item("geomancer_robe"))||ROBE_READY.getOrDefault(p,0L)>p.level().getGameTime()||p.getRandom().nextFloat()>=.5F)return;
        // Source chance and search, with an explicit nearby entity budget.
        var nearby=new ArrayList<WildBoulder>();p.level().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(WildBoulder.class),p.getBoundingBox().inflate(24),e->e.isAlive(),nearby,32);
        if(nearby.size()>=32)return;ROBE_READY.put(p,p.level().getGameTime()+1);
        for(int i=0;i<10;i++){
            double radius=Math.sqrt(p.getRandom().nextFloat())*10+3,angle=p.getRandom().nextDouble()*Math.PI*2;
            var at=p.blockPosition().offset((int)(Math.cos(angle)*radius),p.getRandom().nextInt(16)*(p.getRandom().nextInt(3)-1),(int)(Math.sin(angle)*radius));
            if(!p.level().hasChunkAt(at))continue;
            for(int j=0;j<10&&p.level().getBlockState(at).isAir();j++)at=at.below();
            for(int j=0;j<10&&!p.level().getBlockState(at).isAir();j++)at=at.above();
            at=at.below();if(!usable(p.level(),p,at)||!Authority.mayChange(p.level(),p,at.above(2)))continue;
            var rock=WildBoulder.create(p.level(),p,net.minecraft.world.phys.Vec3.atBottomCenterOf(at.above(2)),WildBoulder.RESTING,1,600);
            if(rock!=null&&p.level().noCollision(rock)){rock.material(p.level().getBlockState(at));if(p.level().addFreshEntity(rock))return;}
        }
    }
    public static final class Gauntlet extends Item implements GeoItem {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        public Gauntlet(Properties p){super(p.pickaxe(ToolMaterial.STONE,5,-2.8F).durability(400).rarity(Rarity.RARE));}
        @Override public boolean mineBlock(ItemStack stack,Level level,net.minecraft.world.level.block.state.BlockState state,BlockPos at,LivingEntity user){if(!level.isClientSide()&&state.getDestroySpeed(level,at)!=0)stack.setDamageValue(Math.min(stack.getMaxDamage()-1,stack.getDamageValue()+1));return true;}
        @Override public void postHurtEnemy(ItemStack stack,LivingEntity target,LivingEntity user){stack.setDamageValue(Math.min(stack.getMaxDamage()-1,stack.getDamageValue()+2));}
        @Override public InteractionResult useOn(UseOnContext c){if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;var ground=c.getClickedPos();if(!usable(p.level(),p,ground)||c.getItemInHand().getDamageValue()+5>=400)return InteractionResult.FAIL;LIFTS.put(p,new Lift(p.level(),ground.immutable(),c.getItemInHand()));p.startUsingItem(c.getHand());return InteractionResult.CONSUME;}
        @Override public InteractionResult use(Level l,Player player,InteractionHand hand){if(player.getItemInHand(hand).getDamageValue()+5>=400)return InteractionResult.FAIL;if(player instanceof ServerPlayer p){LIFTS.remove(p);if(!cost(p.getItemInHand(hand),5))return InteractionResult.FAIL;}player.startUsingItem(hand);return InteractionResult.CONSUME;}
        @Override public int getUseDuration(ItemStack stack,LivingEntity e){return 72000;}
        @Override public void onUseTick(Level raw,LivingEntity user,ItemStack stack,int remaining){if(!(user instanceof ServerPlayer p))return;if(WildCold.frozen(p)){p.stopUsingItem();return;}var lift=LIFTS.get(p);if(lift!=null){if(lift.stack!=stack||lift.level!=p.level()||p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(lift.ground))>64)p.stopUsingItem();return;}if(!cost(stack,1)){p.stopUsingItem();return;}if(p.tickCount%2!=0)return;
            var next=p.position().add(p.getLookAngle().scale(.5));var box=p.getBoundingBox().move(next.subtract(p.position())).inflate(.1);var positions=new ArrayList<BlockPos>();for(var at:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))){if(!p.level().hasChunkAt(at)||!p.level().getWorldBorder().isWithinBounds(at)){p.stopUsingItem();return;}if(p.level().getBlockState(at).isAir())continue;if(!usable(p.level(),p,at)){p.stopUsingItem();return;}positions.add(at.immutable());if(positions.size()>18){p.stopUsingItem();return;}}
            for(var at:positions)if(!NouveauSpellState.replace(p.level(),at,Blocks.AIR.defaultBlockState(),80)){p.stopUsingItem();return;}if(p.level().noCollision(p,box)){p.setDeltaMovement(p.getLookAngle().scale(.5));p.needsSync=true;p.fallDistance=0;}
        }
        @Override public boolean releaseUsing(ItemStack stack,Level raw,LivingEntity e,int remaining){if(!(e instanceof ServerPlayer p))return false;var lift=LIFTS.remove(p);if(lift==null||lift.level!=p.level()||lift.stack!=stack||p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(lift.ground))>64||!usable(p.level(),p,lift.ground))return false;int time=getUseDuration(stack,e)-remaining,tier=time>=60?3:time>=30?2:1;var rock=WildBoulder.create(p.level(),p,net.minecraft.world.phys.Vec3.atBottomCenterOf(lift.ground.above()),WildBoulder.RESTING,tier,1200);if(rock==null||!p.level().noCollision(rock)||stack.getDamageValue()+5*tier>=400)return false;rock.material(p.level().getBlockState(lift.ground));if(!p.level().addFreshEntity(rock))return false;cost(stack,5*tier);p.playSound(OwnerWilds.sound("geomancy.bouldercharge"),1,1);return true;}
        @Override public void inventoryTick(ItemStack stack,ServerLevel l,Entity e,EquipmentSlot slot){if(e instanceof LivingEntity living&&living.getUseItem()==stack||e instanceof ServerPlayer p&&PILLARS.containsKey(p))return;if(stack.getDamageValue()>0)stack.setDamageValue(stack.getDamageValue()-1);}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<Gauntlet>("idle",2,s->s.setAndContinue(RawAnimation.begin().thenLoop("idle"))));}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}@Override public Object getRenderProvider(){return renderer.apply(this);}
    }
    public static final class Staff extends Item implements GeoItem {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        public Staff(Properties p){super(p.sword(ToolMaterial.STONE,2,-3).durability(131).repairable(TagKey.create(Registries.ITEM,Jugcraft.id("repairs_geomancer_armor"))).rarity(Rarity.RARE));}
        @Override public InteractionResult use(Level raw,Player user,InteractionHand hand){if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;var stack=p.getItemInHand(hand);if(p.getCooldowns().isOnCooldown(stack))return InteractionResult.FAIL;int added=0;for(int i=0;i<3;i++){var side=new Vec3(p.getLookAngle().z,0,-p.getLookAngle().x).normalize();var at=p.getEyePosition().add(side.scale((i-1)*1.1)).add(0,.3,0);if(!p.level().getBlockState(BlockPos.containing(at)).isAir())continue;var rock=WildBoulder.create(p.level(),p,at,WildBoulder.SLING,1,200);if(rock==null)continue;rock.sling(p.getLookAngle(),10+i*5);if(p.level().addFreshEntity(rock))added++;}if(added==0)return InteractionResult.FAIL;stack.hurtAndBreak(2,p,hand.asEquipmentSlot());p.getCooldowns().addCooldown(stack,25);p.playSound(OwnerWilds.sound("geomancy.bouldercharge"),1,1);return InteractionResult.SUCCESS;}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){}@Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}@Override public Object getRenderProvider(){return renderer.apply(this);}
    }
    public static final class Armor extends Item implements GeoItem {
        public final ArmorType slot;private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        public Armor(ArmorType slot,Properties p){super(p.humanoidArmor(new ArmorMaterial(33,Map.of(ArmorType.HELMET,2,ArmorType.CHESTPLATE,7,ArmorType.LEGGINGS,6,ArmorType.BOOTS,2,ArmorType.BODY,7),9,SoundEvents.ARMOR_EQUIP_IRON,1,0,TagKey.create(Registries.ITEM,Jugcraft.id("repairs_geomancer_armor")),ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("wilds_geomancer"))),slot).rarity(Rarity.UNCOMMON));this.slot=slot;}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){}@Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}@Override public Object getRenderProvider(){return renderer.apply(this);}
    }
}

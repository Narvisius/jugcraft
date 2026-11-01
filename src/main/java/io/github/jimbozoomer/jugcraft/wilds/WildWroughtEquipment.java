package io.github.jimbozoomer.jugcraft.wilds;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Original guardian rewards: unbreakable horned helmet and delayed sweep/slam axe. */
public final class WildWroughtEquipment {
    private static final Map<UUID,Swing> SWINGS=new HashMap<>();
    private static final class Swing {
        final ItemStack stack;final ServerLevel level;final boolean slam;final long start;
        Vec3 origin;float yaw;final Set<UUID> hit=new HashSet<>();
        Swing(ServerPlayer p,ItemStack s,boolean slam){stack=s;level=p.level();this.slam=slam;start=level.getGameTime();}
    }
    static void register(){
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(WildPlayerActionPayload.TYPE,WildPlayerActionPayload.CODEC);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->SWINGS.clear());
        ServerTickEvents.END_SERVER_TICK.register(server->{var it=SWINGS.entrySet().iterator();while(it.hasNext()){var e=it.next();var p=server.getPlayerList().getPlayer(e.getKey());var s=e.getValue();if(p==null||!p.isAlive()||p.isSpectator()||WildCold.frozen(p)||p.level()!=s.level||p.getMainHandItem()!=s.stack){it.remove();continue;}int age=(int)(s.level.getGameTime()-s.start);if(age>30){it.remove();continue;}if(age==7)p.playSound(OwnerWilds.sound("wroughtnaut.whoosh"),.7F,1.1F);if(age==(s.slam?14:11)){s.origin=p.position();s.yaw=p.getYRot();strike(p,s,s.slam?4.5:4,s.slam?40:160,s.slam?9:7,false);if(s.slam)s.level.playSound(null,p.blockPosition(),OwnerWilds.sound("wroughtnaut.axe_land"),SoundSource.PLAYERS,.3F,.5F);}if(s.slam&&s.origin!=null&&age>=16&&age<=30)strike(p,s,age-15,63,Math.max(1,6-(age-15)*5F/16),true);}});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->SWINGS.remove(handler.player.getUUID()));
        
    }
    private static void strike(ServerPlayer p,Swing s,double radius,float arc,float damage,boolean ring){
        var center=ring?s.origin:p.position();double angle=Math.toRadians(s.yaw);var forward=new Vec3(-Math.sin(angle),0,Math.cos(angle));var victims=new ArrayList<LivingEntity>();
        s.level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),new net.minecraft.world.phys.AABB(center,center).inflate(radius+1,2,radius+1),e->e!=p&&e.isAlive()&&!e.isSpectator(),victims,64);
        for(var e:victims){var v=e.position().subtract(center);double distance=v.horizontalDistance();if(distance>radius+1||ring&&(distance<radius-1||!e.onGround())||!ring&&distance>radius||distance>.001&&v.normalize().dot(forward)<Math.cos(Math.toRadians(arc/2))||!p.hasLineOfSight(e)||s.hit.contains(e.getUUID())||e.isAlliedTo(p)||e instanceof TamableAnimal pet&&p.getUUID().equals((pet.getOwnerReference()==null?null:pet.getOwnerReference().getUUID()))||!TwoHanded.allowed(p,s.level,e))continue;
            var source=p.damageSources().playerAttack(p);float enchanted=net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(s.level,s.stack,e,source,damage);
            if(e.hurtServer(s.level,source,enchanted)){net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffectsWithItemSource(s.level,e,source,s.stack);s.hit.add(e.getUUID());double resist=1-e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);var push=v.normalize().scale((ring?.6:s.slam?.8:1.2)*resist);e.setDeltaMovement(e.getDeltaMovement().add(push.x,ring?.15*resist:0,push.z));e.needsSync=true;}
        }
        if(ring){for(int i=0;i<8;i++){double a=angle+Math.PI/2+(i/7.0-.5)*Math.toRadians(63);var at=center.add(Math.cos(a)*radius,0,Math.sin(a)*radius);var pos=net.minecraft.core.BlockPos.containing(at).below();if(!s.level.hasChunkAt(pos))continue;var block=s.level.getBlockState(pos);if(!block.isAir())s.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,block),at.x,at.y+.1,at.z,3,.3,.1,.3,.1);}}
    }
    public static final class Axe extends Item {
        public Axe(Properties p){super(p.axe(ToolMaterial.IRON,6,-3.1F).component(DataComponents.UNBREAKABLE,Unit.INSTANCE));}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){var stack=p.getItemInHand(hand);if(hand!=InteractionHand.MAIN_HAND||p.getAttackStrengthScale(.5F)<1||p.getCooldowns().isOnCooldown(stack)||p.isSpectator())return InteractionResult.FAIL;if(p instanceof ServerPlayer sp){if(SWINGS.containsKey(p.getUUID()))return InteractionResult.FAIL;SWINGS.put(p.getUUID(),new Swing(sp,stack,p.isShiftKeyDown()&&p.onGround()));p.getCooldowns().addCooldown(stack,30);p.resetAttackStrengthTicker();p.swing(hand,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);var payload=new WildPlayerActionPayload(p.getId(),p.isShiftKeyDown()&&p.onGround());for(var watcher:net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(sp))net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(watcher,payload);net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sp,payload);}return InteractionResult.SUCCESS;}
    }
    public static final class Helmet extends Item {
        public Helmet(Properties p){super(p.humanoidArmor(new ArmorMaterial(15,Map.of(ArmorType.HELMET,2,ArmorType.CHESTPLATE,6,ArmorType.LEGGINGS,5,ArmorType.BOOTS,2,ArmorType.BODY,6),9,SoundEvents.ARMOR_EQUIP_IRON,0,.1F,ItemTags.REPAIRS_IRON_ARMOR,ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("wilds_wrought"))),ArmorType.HELMET).component(DataComponents.UNBREAKABLE,Unit.INSTANCE));}
    }
}

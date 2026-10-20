package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import java.util.Map;

/** Native actions for the goblin equipment bundled with the owner's landscapes. */
final class LandscapeRelics extends Item {
    private final String name;
    private LandscapeRelics(String name,Properties p){super(p);this.name=name;}
    static Item create(String name,JsonObject data,Properties p){
        if(data.has("species"))return new MenagerieItems.Spawn(data.get("species").getAsString(),p);
        if(name.equals("goblin_crown")){
            var material=new ArmorMaterial(16,Map.of(ArmorType.HELMET,2,ArmorType.CHESTPLATE,5,ArmorType.LEGGINGS,4,ArmorType.BOOTS,1,ArmorType.BODY,4),15,SoundEvents.ARMOR_EQUIP_GOLD,0,0,TagKey.create(Registries.ITEM,Jugcraft.id("landscape_crown_repair")),ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("landscape/goblin_crown")));
            return new LandscapeRelics(name,p.humanoidArmor(material,ArmorType.HELMET));
        }
        if(name.startsWith("ring_of_")||name.equals("staff_amethyst"))p.stacksTo(1);
        return new LandscapeRelics(name,p);
    }
    private static ItemStack fuel(ServerPlayer p,Item item){
        for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(stack.is(item))return stack;}
        return ItemStack.EMPTY;
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(!name.equals("goblin_crown")||context.getPlayer()==null||!context.getPlayer().isShiftKeyDown())return super.useOn(context);
        if(!(context.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var level=p.level();var at=context.getClickedPos().above();
        if(!level.getBlockState(context.getClickedPos()).is(Menagerie.BLOCKS.get("landscape_urn"))||!Menagerie.mayChange(level,p,context.getClickedPos())||!Menagerie.mayChange(level,p,at))return InteractionResult.FAIL;
        var ash=fuel(p,Menagerie.item("landscape_soul_ash"));if(!p.isCreative()&&ash.getCount()<8)return InteractionResult.FAIL;
        if(!level.getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(96),m->m.species().id().equals("landscape_goblin_king")).isEmpty())return InteractionResult.FAIL;
        var king=Menagerie.TYPES.get("landscape_goblin_king").create(level,EntitySpawnReason.TRIGGERED);if(king==null)return InteractionResult.FAIL;
        king.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);if(!level.noCollision(king)||!level.addFreshEntity(king))return InteractionResult.FAIL;
        king.setPersistenceRequired();king.setTarget(p);if(!p.isCreative())ash.shrink(8);return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(!name.startsWith("ring_of_")&&!name.equals("staff_amethyst"))return super.use(level,player,hand);
        if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var held=p.getItemInHand(hand);if(p.getCooldowns().isOnCooldown(held))return InteractionResult.FAIL;
        var cost=fuel(p,name.equals("staff_amethyst")?Menagerie.item("landscape_goblin_soul_bullet"):Items.AMETHYST_SHARD);
        if(!p.isCreative()&&cost.isEmpty())return InteractionResult.FAIL;
        if(name.equals("staff_amethyst")){
            var target=Caverns.aim(p,20);if(target==null||!TwoHanded.allowed(p,p.level(),target))return InteractionResult.FAIL;
            target.hurtServer(p.level(),p.damageSources().playerAttack(p),8);
            p.level().sendParticles(ParticleTypes.SOUL,target.getX(),target.getEyeY(),target.getZ(),16,.2,.3,.2,.02);
        }else if(name.equals("ring_of_experience"))p.giveExperiencePoints(4);
        else if(name.equals("ring_of_health")){if(p.getHealth()>=p.getMaxHealth())return InteractionResult.FAIL;p.heal(6);}
        else p.addEffect(new MobEffectInstance(name.equals("ring_of_stealth")?MobEffects.INVISIBILITY:MobEffects.STRENGTH,200,0));
        if(!p.isCreative())cost.shrink(1);
        p.getCooldowns().addCooldown(held,name.equals("staff_amethyst")?25:1200);
        return InteractionResult.SUCCESS;
    }
}

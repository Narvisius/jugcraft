package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;

/** Source catacomb equipment/rare mutations and the Sharpness V Magehunter vindicator. */
public final class JimtaDungeonMobs {
    public static EntityType<CatacombZombie> ZOMBIE;
    public static EntityType<Magehunter> MAGEHUNTER;
    public static void register(){
        var zombieKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_catacombs_zombie"));
        ZOMBIE=Registry.register(BuiltInRegistries.ENTITY_TYPE,zombieKey,EntityType.Builder.of(CatacombZombie::new,MobCategory.MONSTER).sized(.6F,1.95F).build(zombieKey));
        FabricDefaultAttributeRegistry.register(ZOMBIE,Zombie.createAttributes());
        var mageKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_magehunter_vindicator"));
        MAGEHUNTER=Registry.register(BuiltInRegistries.ENTITY_TYPE,mageKey,EntityType.Builder.of(Magehunter::new,MobCategory.MONSTER).sized(.6F,1.95F).build(mageKey));
        FabricDefaultAttributeRegistry.register(MAGEHUNTER,Vindicator.createAttributes());
    }
    public static final class CatacombZombie extends Zombie {
        private boolean sourceEquipment;
        CatacombZombie(EntityType<? extends Zombie> t,Level l){super(t,l);}
        @Override public void tick(){super.tick();if(!level().isClientSide()&&!sourceEquipment){sourceEquipment=true;
            if(random.nextFloat()<.2F){var effect=switch(random.nextInt(4)){case 0->MobEffects.INVISIBILITY;case 1->MobEffects.SPEED;case 2->MobEffects.RESISTANCE;default->MobEffects.STRENGTH;};addEffect(new MobEffectInstance(effect,Integer.MAX_VALUE,effect==MobEffects.SPEED||effect==MobEffects.RESISTANCE?1:0));}
            Item[][] choices={{Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET},{Items.CHAINMAIL_BOOTS,Items.CHAINMAIL_LEGGINGS,Items.CHAINMAIL_CHESTPLATE,Items.CHAINMAIL_HELMET},{Items.IRON_BOOTS,Items.IRON_LEGGINGS,Items.IRON_CHESTPLATE,Items.IRON_HELMET}};
            EquipmentSlot[] slots={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};float power=random.nextFloat();
            for(int i=0;i<4;i++){float chance=power+(random.nextFloat()-.5F)/3;if(getItemBySlot(slots[i]).isEmpty()&&random.nextFloat()<=.6F)setItemSlot(slots[i],new ItemStack(choices[chance>.85?2:chance>.45?1:0][i]));setDropChance(slots[i],0);}
            if(random.nextFloat()<.01F&&getItemBySlot(EquipmentSlot.HEAD).isEmpty())setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.BANNER.cyan()));
        }}
        @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.putBoolean("source_equipment",sourceEquipment);}
        @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);sourceEquipment=i.getBooleanOr("source_equipment",false);}
    }
    public static final class Magehunter extends Vindicator {
        private boolean sourceEquipment;
        Magehunter(EntityType<? extends Vindicator> t,Level l){super(t,l);}
        @Override public void tick(){super.tick();if(!level().isClientSide()&&!sourceEquipment){sourceEquipment=true;if(getMainHandItem().isEmpty()||getMainHandItem().is(Items.IRON_AXE)){var sword=new ItemStack(JimtaSpellbooks.ITEMS.get("magehunter"));registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SHARPNESS).ifPresent(e->sword.enchant(e,5));setItemSlot(EquipmentSlot.MAINHAND,sword);}}}
        @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.putBoolean("source_equipment",sourceEquipment);}
        @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);sourceEquipment=i.getBooleanOr("source_equipment",false);}
    }
}

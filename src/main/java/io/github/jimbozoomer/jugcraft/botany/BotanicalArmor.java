package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;

/** Source armor statistics and full-set mechanics, without research/equip gates. */
public final class BotanicalArmor {
    public static DataComponentType<Boolean> PHANTOM;
    public static DataComponentType<Integer> WILLS;
    private static final List<EquipmentSlot> SLOTS=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    public static final Set<String> FAMILIES=Set.of("manasteel","elementium","terrasteel","manaweave");
    static void register(){
        BotanicalPixie.register();
        PHANTOM=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_phantom_ink"),DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
        WILLS=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_ancient_wills"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,63)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("botanical_armor_customization"),BotanicalArmorRecipe.SERIALIZER);
    }
    static boolean handles(String name){int split=name.lastIndexOf('_');return split>0&&FAMILIES.contains(name.substring(0,split))&&Set.of("helmet","chestplate","leggings","boots").contains(name.substring(split+1));}
    static Item create(String name,Item.Properties p){
        String family=name.substring(0,name.lastIndexOf('_'));
        ArmorType type=name.endsWith("helmet")?ArmorType.HELMET:name.endsWith("chestplate")?ArmorType.CHESTPLATE:name.endsWith("leggings")?ArmorType.LEGGINGS:ArmorType.BOOTS;
        boolean terra=family.equals("terrasteel"),cloth=family.equals("manaweave");
        int head=terra?3:cloth?1:2,chest=terra?8:cloth?3:6,legs=terra?6:cloth?2:5,boots=terra?3:cloth?1:2;
        var material=new ArmorMaterial(terra?34:cloth?5:family.equals("elementium")?18:16,
            Map.of(ArmorType.HELMET,head,ArmorType.CHESTPLATE,chest,ArmorType.LEGGINGS,legs,ArmorType.BOOTS,boots,ArmorType.BODY,chest),
            terra?26:18,SoundEvents.ARMOR_EQUIP_IRON,terra?3:0,terra?(type==ArmorType.HELMET?head:type==ArmorType.CHESTPLATE?chest:type==ArmorType.LEGGINGS?legs:boots)/20F:0,
            TagKey.create(Registries.ITEM,Jugcraft.id("jugtania_"+family+"_repair")),ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("jugtania_"+family)));
        return new Armor(family,type,p.humanoidArmor(material,type));
    }
    public static String fullSet(Player p){
        String family=null;
        for(var slot:SLOTS){if(!(p.getItemBySlot(slot).getItem() instanceof Armor a))return "";if(family==null)family=a.family;else if(!family.equals(a.family))return "";}
        return family==null?"":family;
    }
    public static int price(Player p,int amount){if(amount<=0)return amount;int percent=switch(fullSet(p)){case "manaweave"->60;case "terrasteel"->80;case "manasteel","elementium"->90;default->100;};return (int)(((long)amount*percent+99)/100);}
    static void tick(ServerPlayer p){
        if(fullSet(p).equals("terrasteel")){
            int food=p.getFoodData().getFoodLevel();if(p.tickCount%80==0&&food>0&&food<18&&p.getHealth()<p.getMaxHealth())p.heal(1);
            if(p.tickCount%10==0)BotanicalManaItems.fill(p,10);
        }
    }
    public static boolean invisible(ItemStack s){return s.getOrDefault(PHANTOM,false);}
    public static boolean will(Player p,int bit){return fullSet(p).equals("terrasteel")&&(p.getItemBySlot(EquipmentSlot.HEAD).getOrDefault(WILLS,0)&(1<<bit))!=0;}
    public static final class Armor extends Item implements BotanicalEquipment.ManaGear {
        public final String family;public final ArmorType type;
        Armor(String family,ArmorType type,Properties p){super(p);this.family=family;this.type=type;}
        @Override public int manaCost(){return family.equals("manaweave")&&type==ArmorType.HELMET?30:70;}
    }
}

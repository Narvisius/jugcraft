package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;

/** Finite source orb upgrades, stored on the actual equipment instead of an external player unlock. */
public final class JimtaUpgrades {
    public static final List<String> TYPES=List.of("fire","ice","lightning","holy","ender","blood","evocation","nature","mana","cooldown","protection");
    public static final int LIMIT=3;
    public static DataComponentType<List<String>> UPGRADES;
    public static void register(){
        var codec=Codec.STRING.listOf(0,LIMIT);
        UPGRADES=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("jimtafacts_equipment_upgrades"),DataComponentType.<List<String>>builder().persistent(codec).networkSynchronized(ByteBufCodecs.fromCodec(codec)).build());
        for(String type:TYPES){String name=type+"_upgrade_orb";var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(JimtaSpellbooks.PREFIX+name));JimtaSpellbooks.ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key,new Orb(new Item.Properties().setId(key),type)));}
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(JimtaSpellbooks.PREFIX+"upgrade_orb"));JimtaSpellbooks.ITEMS.put("upgrade_orb",Registry.register(BuiltInRegistries.ITEM,key,new Item(new Item.Properties().setId(key))));
    }
    public static boolean accepts(ItemStack stack){return stack.getItem() instanceof JimtaSpellbooks.Armor||stack.getItem() instanceof JimtaSpellbooks.Book;}
    public static boolean apply(ItemStack stack,ItemStack material){
        if(!accepts(stack)||!(material.getItem() instanceof Orb orb))return false;
        var list=new ArrayList<>(stack.getOrDefault(UPGRADES,List.<String>of()));if(list.size()>=LIMIT)return false;
        list.add(orb.type);stack.set(UPGRADES,List.copyOf(list));return true;
    }
    public static int count(LivingEntity entity,String type){
        if(UPGRADES==null||!JimtaSpellbooks.enabled())return 0;int sum=0;
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND)){
            var stack=entity.getItemBySlot(slot);if(!accepts(stack))continue;
            if(slot==EquipmentSlot.MAINHAND&&!(stack.getItem() instanceof JimtaSpellbooks.Book))continue;
            if(slot!=EquipmentSlot.MAINHAND&&!(stack.getItem() instanceof JimtaSpellbooks.Armor))continue;
            sum+=Collections.frequency(stack.getOrDefault(UPGRADES,List.<String>of()),type);
        }return sum;
    }
    public static int focusBonus(Player player){return count(player,"mana")*10+JimtaSpecialArmor.focusBonus(player);}
    public static float cooldownFactor(Player player){return Math.max(.1F,1-count(player,"cooldown")*.05F)*JimtaSpecialArmor.cooldownFactor(player);}
    public static float resistance(LivingEntity entity){return 1F/(1+count(entity,"protection")*.05F-(entity instanceof Player p&&JimtaSpecialArmor.held(p).equals("rotten_spell_book")?.15F:0));}
    public static float schoolBonus(LivingEntity entity,String school){return count(entity,school)*.05F;}
    public static void tooltip(ItemStack stack,java.util.function.Consumer<Component> lines){
        var values=stack.getOrDefault(UPGRADES,List.<String>of());if(values.isEmpty())return;
        lines.accept(Component.literal("Orb upgrades: "+values.size()+" / "+LIMIT).withStyle(ChatFormatting.AQUA));
        values.stream().distinct().forEach(type->lines.accept(Component.literal(type+" × "+Collections.frequency(values,type)).withStyle(ChatFormatting.GRAY)));
    }
    public static final class Orb extends Item {
        public final String type;Orb(Properties p,String type){super(p);this.type=type;}
        @Override public void appendHoverText(ItemStack stack,TooltipContext c,TooltipDisplay d,java.util.function.Consumer<Component> lines,TooltipFlag flag){
            lines.accept(Component.literal("Apply to spellbook equipment at an Arcane Anvil. Maximum three orbs per item.").withStyle(ChatFormatting.AQUA));
            lines.accept(Component.literal(type.equals("mana")?"+10 maximum shared Focus":type.equals("cooldown")?"5% shorter spellbook cooldowns":type.equals("protection")?"+5% spell resistance":"+5% "+type+" spellbook power").withStyle(ChatFormatting.GRAY));
        }
    }
}

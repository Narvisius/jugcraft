package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.*;

/** Original special outfits share normal equipment slots, Focus and the authoritative spell selector. */
public final class JimtaSpecialArmor {
    public static void register(){
        for(var entry:JimtaSpellbooks.data("special_armor").entrySet()){
            String name=entry.getKey();var row=entry.getValue().getAsJsonObject();ArmorType type=ArmorType.valueOf(row.get("slot").getAsString().toUpperCase(Locale.ROOT));
            var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(JimtaSpellbooks.PREFIX+name));
            var material=new ArmorMaterial(30,Map.of(ArmorType.HELMET,3,ArmorType.CHESTPLATE,8,ArmorType.LEGGINGS,6,ArmorType.BOOTS,3,ArmorType.BODY,8),25,SoundEvents.ARMOR_EQUIP_LEATHER,2,0,TagKey.create(Registries.ITEM,Jugcraft.id("jimtafacts/spellbook_armor_repair")),ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("jimtafacts/"+name)));
            var attributes=material.createAttributes(type);
            if(name.equals("speed_boots"))attributes=attributes.withModifierAdded(Attributes.MOVEMENT_SPEED,new AttributeModifier(Jugcraft.id("jimtafacts/speed_boots"),.25,AttributeModifier.Operation.ADD_MULTIPLIED_BASE),EquipmentSlotGroup.FEET);
            if(name.equals("tarnished_helmet"))attributes=attributes.withModifierAdded(Attributes.ATTACK_DAMAGE,new AttributeModifier(Jugcraft.id("jimtafacts/tarnished"),-.15,AttributeModifier.Operation.ADD_MULTIPLIED_BASE),EquipmentSlotGroup.HEAD);
            var p=new Item.Properties().setId(key).humanoidArmor(material,type).attributes(attributes).rarity(Rarity.RARE);
            if(name.equals("infernal_sorcerer_chestplate"))p.fireResistant();
            JimtaSpellbooks.ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key,new Special(p,name,row.get("model").getAsString(),row.get("texture").getAsString())));
        }
    }
    public static boolean wearing(LivingEntity entity,String name){return equipped(entity).stream().anyMatch(s->s.is(JimtaSpellbooks.ITEMS.get(name)));}
    public static List<ItemStack> equipped(LivingEntity entity){return List.of(entity.getItemBySlot(EquipmentSlot.HEAD),entity.getItemBySlot(EquipmentSlot.CHEST),entity.getItemBySlot(EquipmentSlot.LEGS),entity.getItemBySlot(EquipmentSlot.FEET));}
    public static int focusBonus(Player p){int total=switch(held(p)){case "gold_spell_book"->10;case "diamond_spell_book","rotten_spell_book"->20;case "netherite_spell_book","blaze_spell_book","dragonskin_spell_book","druidic_spell_book","villager_spell_book","ice_spell_book","evoker_spell_book"->40;default->0;};for(var stack:equipped(p))if(stack.getItem() instanceof Special armor)total+=armor.family.equals("gold_crown")?100:armor.family.equals("wizard_hat")?10:30;return total;}
    public static String held(Player p){return p.getMainHandItem().getItem() instanceof JimtaSpellbooks.Book b?b.name:"";}
    public static float cooldownFactor(Player p){return (wearing(p,"gold_crown")?.1F:1)*(held(p).equals("netherite_spell_book")?.8F:1);}
    public static float castDurationFactor(Player p){return held(p).equals("gold_spell_book")?.85F:held(p).equals("villager_spell_book")?.9F:1;}
    public static float bookPower(Player p,String school){String expected=switch(held(p)){case "blaze_spell_book"->"fire";case "dragonskin_spell_book"->"ender";case "druidic_spell_book"->"nature";case "villager_spell_book"->"holy";case "ice_spell_book"->"ice";case "evoker_spell_book"->"evocation";default->"";};return expected.equals(school)?.1F:0;}
    public static float powerBonus(Player p){return (wearing(p,"paladin_chestplate")||wearing(p,"infernal_sorcerer_chestplate")?.1F:0)+(wearing(p,"speed_boots")?.1F:0);}
    public static boolean inscribable(ItemStack stack){return stack.getItem() instanceof JimtaSpellbooks.Armor armor&&(armor.family.equals("tarnished_helmet")||stack.getItem().getDescriptionId().endsWith("chestplate"));}
    public static final class Special extends JimtaSpellbooks.Armor {
        Special(Properties p,String name,String model,String texture){super(p,name,model,texture,name.equals("infernal_sorcerer_chestplate")?"fire":name.equals("paladin_chestplate")?"holy":"arcane");}
        @Override public void appendHoverText(ItemStack stack,TooltipContext c,TooltipDisplay d,java.util.function.Consumer<Component> lines,TooltipFlag flag){
            JimtaUpgrades.tooltip(stack,lines);
            lines.accept(Component.literal(family.equals("gold_crown")?"Creative developer crown · shared Focus remains capped":family.equals("wizard_hat")?"+10 maximum shared Focus":"+30 maximum shared Focus").withStyle(ChatFormatting.AQUA));
            if(inscribable(stack))lines.accept(Component.literal("One equipped spell slot. Inscribe at an Inscription Table.").withStyle(ChatFormatting.AQUA));
            if(family.equals("tarnished_helmet"))lines.accept(Component.literal("25% faster Focus recovery · 15% less melee damage").withStyle(ChatFormatting.GRAY));
            if(family.equals("speed_boots"))lines.accept(Component.literal("25% faster movement · 10% stronger spellbook magic").withStyle(ChatFormatting.GRAY));
            if(family.equals("paladin_chestplate")||family.equals("infernal_sorcerer_chestplate"))lines.accept(Component.literal("10% stronger spellbook magic").withStyle(ChatFormatting.GRAY));
        }
    }
}

package io.github.jimbozoomer.jugcraft.menagerie;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
/** Original music, dragon banner and blast trim with explicit native survival rewards. */
public final class OwnerSupplementTreasures {
 private OwnerSupplementTreasures(){}
 static void register(){
  for(String name:new String[]{"dragon_banner_pattern","blast_armor_trim_smithing_template","music_disc_heave_ho"}){
   var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("supps_"+name));var p=new Item.Properties().setId(key).rarity(Rarity.RARE);Item item;
   if(name.equals("blast_armor_trim_smithing_template"))item=SmithingTemplateItem.createArmorTrimTemplate(p);
   else if(name.equals("music_disc_heave_ho"))item=new Item(p.stacksTo(1).jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG,Jugcraft.id("supps_heave_ho"))));
   else item=new Item(p.stacksTo(1)){
    @Override public void inventoryTick(ItemStack s,net.minecraft.server.level.ServerLevel l,net.minecraft.world.entity.Entity e,net.minecraft.world.entity.EquipmentSlot slot){if(!s.has(net.minecraft.core.component.DataComponents.PROVIDES_BANNER_PATTERNS))l.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).get(TagKey.create(Registries.BANNER_PATTERN,Jugcraft.id("supps_pattern/dragon"))).ifPresent(patterns->s.set(net.minecraft.core.component.DataComponents.PROVIDES_BANNER_PATTERNS,patterns));}
   };Registry.register(BuiltInRegistries.ITEM,key,item);OwnerSupplementaries.ITEMS.put(name,item);net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(e->e.accept(item));
  }
  net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key,table,source,registries)->{
   if(!source.isBuiltin())return;
   String path=key.identifier().toString();String name;float chance;
   if(path.equals("minecraft:chests/shipwreck_treasure")){name="music_disc_heave_ho";chance=.06F;}
   else if(path.equals("minecraft:chests/desert_pyramid")){name="blast_armor_trim_smithing_template";chance=.15F;}
   else if(path.equals("minecraft:chests/end_city_treasure")){name="dragon_banner_pattern";chance=.1F;}else return;
   table.withPool(LootPool.lootPool().setRolls(Holder.direct(new net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue(1))).add(LootItem.lootTableItem(OwnerSupplementaries.ITEMS.get(name))).when(LootItemRandomChanceCondition.randomChance(chance)));
  });
 }
}

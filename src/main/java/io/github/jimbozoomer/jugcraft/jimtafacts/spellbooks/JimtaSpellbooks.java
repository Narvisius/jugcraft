package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;

/** Native spellbooks use Jugcraft Focus, Spell Engine's selector and the supplied item/armor models. */
public final class JimtaSpellbooks {
    public static final String PREFIX = "jimta_spellbooks_";
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static DataComponentType<List<String>> CONTENTS;
    public static DataComponentType<Integer> EXTRA_SLOTS;
    public static Function<Armor, Object> armorRenderer = armor -> null;
    private JimtaSpellbooks() {}

    public static JsonObject data(String name) {
        try (var stream = JimtaSpellbooks.class.getResourceAsStream("/jugcraft/jimtafacts_spellbooks/" + name + ".json")) {
            if (stream == null) throw new IllegalStateException("Missing Jimtafacts spellbook " + name);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    public static boolean enabled() { return JugcraftConfig.isFeatureEnabled("concordance"); }
    public static void register() {
        var contents = Codec.STRING.listOf(0, 16);
        CONTENTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimtafacts_spellbook_contents"),
            DataComponentType.<List<String>>builder().persistent(contents).networkSynchronized(ByteBufCodecs.fromCodec(contents)).build());
        EXTRA_SLOTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimtafacts_spellbook_slots"),
            DataComponentType.<Integer>builder().persistent(Codec.intRange(0,4)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        JimtaSpellcasting.register();
        JimtaSpellCreature.register();
        for (var entry : data("items").entrySet()) {
            String name = entry.getKey(); var row = entry.getValue().getAsJsonObject();
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id(PREFIX + name));
            Item.Properties properties = new Item.Properties().setId(key);
            Item item = switch (row.get("kind").getAsString()) {
                case "book" -> new Book(properties.stacksTo(1).rarity(Rarity.UNCOMMON), name, row.get("capacity").getAsInt(), row.has("fixed_spells") ? new Gson().fromJson(row.get("fixed_spells"), String[].class) : new String[0]);
                case "scroll" -> new Scroll(properties.stacksTo(16).rarity(Rarity.UNCOMMON), row.get("spell").getAsString());
                case "armor" -> armor(properties, name, row);
                default -> new Material(properties, name);
            };
            ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, item));
        }
        JimtaUpgrades.register();
        JimtaSpecialArmor.register();
        JimtaSpellbookCurios.register();
        JimtaScribing.register();
        JimtaWizard.register();
        JimtaSpellGear.register();
        JimtaEncounterItems.register();
        JimtaEncounterCreatures.register();
        JimtaWorkshops.register();
        JimtaStatues.register();
        JimtaDungeonMobs.register();
        JimtaSpellbookLore.register();
        JimtaExploration.register();
        JimtaSpellLoot.register();
        JimtaStructures.register();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out ->
            ITEMS.values().stream().filter(item -> !(item instanceof Armor)).forEach(out::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out ->
            ITEMS.values().stream().filter(item -> item instanceof Armor).forEach(out::accept));
        LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            if (!source.isBuiltin() || !enabled() || !Set.of("minecraft:chests/simple_dungeon", "minecraft:chests/stronghold_library",
                "minecraft:chests/woodland_mansion", "minecraft:chests/ancient_city").contains(key.identifier().toString())) return;
            var pool = LootPool.lootPool().setRolls(Holder.direct(new ConstantValue(1))).when(LootItemRandomChanceCondition.randomChance(.3f));
            ITEMS.forEach((name, item) -> { if (item instanceof Scroll) pool.add(LootItem.lootTableItem(item)); });
            table.withPool(pool);
        });
    }
    private static Armor armor(Item.Properties p, String name, JsonObject row) {
        String family = row.get("family").getAsString();
        ArmorType type = name.endsWith("helmet") ? ArmorType.HELMET : name.endsWith("chestplate") ? ArmorType.CHESTPLATE : name.endsWith("leggings") ? ArmorType.LEGGINGS : ArmorType.BOOTS;
        boolean metal = family.equals("netherite_mage");
        var material = new ArmorMaterial(metal ? 30 : 18,
            Map.of(ArmorType.HELMET, metal ? 3 : 2, ArmorType.CHESTPLATE, metal ? 7 : 5, ArmorType.LEGGINGS, metal ? 6 : 4,
                   ArmorType.BOOTS, metal ? 3 : 1, ArmorType.BODY, metal ? 7 : 5), 20, SoundEvents.ARMOR_EQUIP_LEATHER,
            metal ? 2 : 0, 0, TagKey.create(Registries.ITEM, Jugcraft.id("jimtafacts/spellbook_armor_repair")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id("jimtafacts/" + family)));
        return new Armor(p.humanoidArmor(material, type), family, row.get("model").getAsString(), row.get("texture").getAsString(), row.get("school").getAsString());
    }
    public static List<String> spells(ItemStack stack) {
        if (stack.getItem() instanceof Scroll scroll) return List.of(scroll.spell);
        if(JimtaSpecialArmor.inscribable(stack))return stack.getOrDefault(CONTENTS,List.<String>of()).stream().filter(JimtaSpellcasting.SPELLS::containsKey).limit(1).toList();
        if (!(stack.getItem() instanceof Book book)) return List.of();
        return java.util.stream.Stream.concat(book.fixed.stream(),stack.getOrDefault(CONTENTS,List.<String>of()).stream()).filter(JimtaSpellcasting.SPELLS::containsKey).distinct().limit(capacity(stack, book)).toList();
    }
    public static int capacity(ItemStack stack, Book book) { return Math.min(16, book.slots + stack.getOrDefault(EXTRA_SLOTS, 0)); }
    public static boolean inscribe(ItemStack bookStack, String spell) {
        if(JimtaSpecialArmor.inscribable(bookStack)&&JimtaSpellcasting.SPELLS.containsKey(spell)&&spells(bookStack).isEmpty()){bookStack.set(CONTENTS,List.of(spell));return true;}
        if (!(bookStack.getItem() instanceof Book book) || !JimtaSpellcasting.SPELLS.containsKey(spell)) return false;
        var list = new ArrayList<>(spells(bookStack));
        if (list.contains(spell) || list.size() >= capacity(bookStack, book)) return false;
        list.add(spell); list.removeAll(book.fixed); bookStack.set(CONTENTS, List.copyOf(list)); return true;
    }
    public static int focusCost(Player player, ItemStack held, JimtaSpellcasting.Working spell) {
        if (held.getItem() instanceof Scroll) return 0;
        // The supplied school outfits contribute without creating another mana pool.
        int matching = 0;
        for (var slot : List.of(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET))
            if (player.getItemBySlot(slot).getItem() instanceof Armor armor && armor.school.equals(spell.school())) matching++;
        return Math.max(1, spell.focus() - matching / 2);
    }
    public static class Book extends Item {
        public final String name; public final int slots; public final List<String> fixed;
        Book(Properties properties,String name,int slots,String... fixed){super(properties);this.name=name;this.slots=slots;this.fixed=List.of(fixed);}
        @Override public InteractionResult use(Level level, Player user, InteractionHand hand) {
            if (user instanceof ServerPlayer player) {
                if (!enabled() || player.isSpectator()) return InteractionResult.FAIL;
                ItemStack book = player.getItemInHand(hand), other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                if(player.isShiftKeyDown()&&other.isEmpty()){
                    boolean dismissed=false;for(String spell:spells(book))if(JimtaSummonSpells.NAMES.contains(spell))dismissed|=io.github.jimbozoomer.jugcraft.nouveau.NouveauSummons.dismissSpellbook(player,spell);
                    if(dismissed){player.sendOverlayMessage(Component.literal("Summoned companions dismissed"));return InteractionResult.SUCCESS;}
                }
                if (other.getItem() instanceof Scroll scroll && inscribe(book, scroll.spell)) {
                    other.consume(1, player); player.sendOverlayMessage(Component.literal("Spell inscribed · select it with the casting HUD"));
                } else player.sendOverlayMessage(Component.literal(spells(book).isEmpty() ? "Hold a scroll in your other hand and use this book to inscribe it." : "Select an inscribed spell with the casting HUD, then cast."));
            }
            return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
            JimtaUpgrades.tooltip(stack,out);
            out.accept(Component.literal(spells(stack).size() + " / " + capacity(stack, this) + " spell slots").withStyle(ChatFormatting.AQUA));
            for (String spell : spells(stack)) out.accept(Component.translatable("spell.jugcraft.jimtafacts/" + spell + ".name").withStyle(ChatFormatting.GRAY));
            out.accept(Component.literal("No research requirement · spells spend shared Focus").withStyle(ChatFormatting.DARK_GRAY));
            if(spells(stack).stream().anyMatch(JimtaSummonSpells.NAMES::contains))out.accept(Component.literal("Sneak-use with an empty other hand to dismiss companions.").withStyle(ChatFormatting.GRAY));
        }
    }
    public static final class Scroll extends Item {
        public final String spell;
        Scroll(Properties properties, String spell) { super(properties); this.spell = spell; }
        @Override public InteractionResult use(Level level, Player user, InteractionHand hand) {
            if (!(user instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
            if (!enabled() || player.isSpectator()) return InteractionResult.FAIL;
            var other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (inscribe(other, spell)) { player.getItemInHand(hand).consume(1, player); return InteractionResult.SUCCESS; }
            return JimtaSpellcasting.castScroll(player, hand, this) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
            out.accept(Component.literal(JimtaSpellcasting.SPELLS.get(spell).description()).withStyle(ChatFormatting.GRAY));
            out.accept(Component.literal("Use with a spellbook in the other hand to inscribe, or consume to cast once.").withStyle(ChatFormatting.AQUA));
        }
    }
    private static final class Material extends Item {
        private final String name;
        Material(Properties p, String name) { super(p); this.name = name; }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(player instanceof ServerPlayer) || !enabled() || player.isSpectator()) return InteractionResult.PASS;
            var book = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!(book.getItem() instanceof Book)) return InteractionResult.PASS;
            if (name.equals("lesser_spell_slot_upgrade")) {
                int extra = book.getOrDefault(EXTRA_SLOTS, 0); if (extra >= 4 || capacity(book, (Book)book.getItem()) >= 16) return InteractionResult.FAIL;
                book.set(EXTRA_SLOTS, extra + 1); player.getItemInHand(hand).consume(1, player); return InteractionResult.SUCCESS;
            }
            if (name.equals("shriving_stone") && player.isShiftKeyDown() && !book.getOrDefault(CONTENTS,List.<String>of()).isEmpty()) {
                // Erasing returns no scrolls or materials: cannot duplicate by repeatedly stripping a book.
                book.remove(CONTENTS); player.getItemInHand(hand).consume(1, player); return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
    }
    public static class Armor extends Item implements GeoItem {
        public final String family, model, texture, school;
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        Armor(Properties p, String family, String model, String texture, String school) { super(p); this.family=family; this.model=model; this.texture=texture; this.school=school; }
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            if (family.equals("wizard")) controllers.add(new AnimationController<Armor>("authored_idle", 4,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("idle"))));
        }
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
        @Override public Object getRenderProvider() { return armorRenderer.apply(this); }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
            JimtaUpgrades.tooltip(stack,out);
            out.accept(Component.literal("Every two matching pieces reduce this school's spellbook Focus cost by one.").withStyle(ChatFormatting.AQUA));
        }
    }
}

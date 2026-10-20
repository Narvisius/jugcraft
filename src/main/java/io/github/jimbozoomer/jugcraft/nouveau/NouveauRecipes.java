package io.github.jimbozoomer.jugcraft.nouveau;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jspecify.annotations.Nullable;

/** Server-reloaded workshop recipes. Matching reserves exact slots, including repeated ingredients. */
public final class NouveauRecipes extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final NouveauRecipes INSTANCE=new NouveauRecipes();
    private static volatile List<Working> recipes=List.of();
    private NouveauRecipes(){super(ExtraCodecs.JSON,FileToIdConverter.json("nouveau_recipe"));}
    public record Working(String id,String operation,List<JsonElement> ingredients,JsonObject data,int ley,int experience) {
        public boolean accepts(String station){return switch(operation){
            case "imbuement" -> station.equals("imbuement_chamber");
            case "enchanting_apparatus","enchantment","reactive_enchantment","spell_write","prestidigitation" -> station.equals("enchanting_apparatus");
            case "glyph","caster_tome" -> station.equals("scribes_table");
            case "armor_upgrade","dye","book_upgrade" -> station.equals("alteration_table");
            case "potion_flask" -> station.equals("potion_melder");
            case "crush" -> station.equals("crush");
            default -> false;
        };}
        public String label(){if(operation.equals("caster_tome"))return NouveauTomes.name(this);return id.substring(id.lastIndexOf('/')+1).replace('_',' ');}
    }
    public record Match(Working recipe,int[] used,List<ItemStack> after,ItemStack output) {}
    public static List<Working> forStation(String station){return recipes.stream().filter(r->r.accepts(station)).toList();}
    public static List<Working> all(){return recipes;}
    /** Crush resolves the supplied output rolls once; the caller consumes one input only after accepting the result. */
    public static java.util.Optional<List<ItemStack>> crush(net.minecraft.server.level.ServerLevel level,ItemStack input){
        for(var recipe:forStation("crush"))if(!recipe.ingredients().isEmpty()&&ingredient(recipe.ingredients().getFirst(),input)){
            List<ItemStack> results=new ArrayList<>();
            for(var roll:recipe.data().getAsJsonArray("output")){
                var value=roll.getAsJsonObject();float chance=value.has("chance")?value.get("chance").getAsFloat():1;
                if(level.getRandom().nextFloat()>chance)continue;var result=stack(value.get("stack"),level.registryAccess());
                int range=value.has("maxRange")?Math.clamp(value.get("maxRange").getAsInt(),1,64):1;
                if(range>1)result.grow(level.getRandom().nextInt(range));if(!result.isEmpty())results.add(result);
            }return java.util.Optional.of(List.copyOf(results));
        }return java.util.Optional.empty();
    }
    @Override protected void apply(Map<Identifier,JsonElement> input,ResourceManager manager,ProfilerFiller profiler){
        List<Working> loaded=new ArrayList<>();
        input.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString))).forEach(entry->{
            try{
                var d=entry.getValue().getAsJsonObject();String op=d.get("type").getAsString();List<JsonElement> ingredients=new ArrayList<>();
                if(op.equals("caster_tome")){ingredients.add(new JsonPrimitive("minecraft:book"));ingredients.add(new JsonPrimitive("minecraft:paper"));ingredients.add(new JsonPrimitive("jugcraft:nouveau_source_gem"));}
                if(d.has("input"))ingredients.add(d.get("input"));
                if(d.has("reagent"))ingredients.add(d.get("reagent"));
                for(String field:List.of("inputs","ingredients"))if(d.has(field))d.getAsJsonArray(field).forEach(ingredients::add);
                if(!op.equals("imbuement")&&d.has("pedestalItems"))d.getAsJsonArray("pedestalItems").forEach(ingredients::add);
                if(ingredients.size()>16)throw new IllegalArgumentException("More than 16 inputs");
                JsonElement charge=d.has("sourceCost")?d.get("sourceCost"):d.get("source");
                // Summon recipes use source for an entity-selection object, not a charge amount.
                int source=charge!=null&&charge.isJsonPrimitive()&&charge.getAsJsonPrimitive().isNumber()?charge.getAsInt():0;
                if(source<0||source>1000000)throw new IllegalArgumentException("Invalid source cost");
                loaded.add(new Working(entry.getKey().toString(),op,List.copyOf(ingredients),d,(source+99)/100,d.has("exp")?Math.clamp(d.get("exp").getAsInt(),0,10000):0));
            }catch(RuntimeException error){Jugcraft.LOGGER.error("Jug Nouveau recipe {} rejected: {}",entry.getKey(),error.getMessage());}
        });recipes=List.copyOf(loaded);NouveauCrafting.invalidate();
    }
    public static boolean ingredient(JsonElement e,ItemStack stack){
        if(stack.isEmpty()||e==null||e.isJsonNull())return false;
        if(e.isJsonArray()){for(var option:e.getAsJsonArray())if(ingredient(option,stack))return true;return false;}
        String id;
        if(e.isJsonObject()){
            var obj=e.getAsJsonObject();if(obj.has("item"))id=obj.get("item").getAsString();
            else if(obj.has("tag"))id="#"+obj.get("tag").getAsString();else return false;
        }else id=e.getAsString();
        if(id.startsWith("#"))return stack.is(TagKey.create(Registries.ITEM,Identifier.parse(id.substring(1))));
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(id);
    }
    public static ItemStack stack(JsonElement json){
        if(json==null||!json.isJsonObject())return ItemStack.EMPTY;
        var obj=json.getAsJsonObject();String id=obj.has("id")?obj.get("id").getAsString():obj.has("item")?obj.get("item").getAsString():"minecraft:air";
        Item item=BuiltInRegistries.ITEM.getValue(Identifier.parse(id));if(item==null||item==Items.AIR)return ItemStack.EMPTY;
        return new ItemStack(item,Math.clamp(obj.has("count")?obj.get("count").getAsInt():1,1,64));
    }
    public static ItemStack stack(JsonElement json,net.minecraft.core.HolderLookup.Provider registries){
        if(json==null||!json.isJsonObject())return ItemStack.EMPTY;
        var value=json.deepCopy().getAsJsonObject();if(!value.has("id")&&value.has("item"))value.add("id",value.remove("item"));
        return ItemStack.CODEC.parse(RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,registries),value)
            .resultOrPartial(error->Jugcraft.LOGGER.warn("Jug Nouveau recipe output rejected: {}",error)).orElse(ItemStack.EMPTY);
    }
    public static ItemStack preview(Working r,net.minecraft.core.HolderLookup.Provider registries){if(r.operation().equals("caster_tome"))return NouveauTomes.stack(r);return stack(r.data().has("result")?r.data().get("result"):r.data().get("output"),registries);}
    /** A bounded backtracking assignment handles overlapping tags without spending anything. */
    private static boolean assign(List<JsonElement> needs,int n,List<ItemStack> slots,int[] used,int[] budget){
        if(--budget[0]<0)return false;
        if(n==needs.size())return true;
        for(int s=0;s<slots.size();s++)if(used[s]<slots.get(s).getCount()&&ingredient(needs.get(n),slots.get(s))){
            used[s]++;if(assign(needs,n+1,slots,used,budget))return true;used[s]--;
        }return false;
    }
    public static @Nullable Match match(Working r,List<ItemStack> slots,ServerPlayer player){
        int[] used=new int[slots.size()];List<JsonElement> needs=new ArrayList<>(r.ingredients());
        boolean equipment=Set.of("enchantment","armor_upgrade","reactive_enchantment","spell_write","prestidigitation").contains(r.operation());
        ItemStack base=slots.isEmpty()?ItemStack.EMPTY:slots.getFirst();
        if(equipment){if(base.isEmpty()||base.getCount()!=1)return null;used[0]=1;}
        else if(r.data().has("reagent")){var reagent=r.data().get("reagent");if(!ingredient(reagent,base))return null;used[0]=1;needs.remove(reagent);}
        if(needs.isEmpty()&&!equipment)return null;
        if(!assign(needs,0,slots,used,new int[]{4096}))return null;
        if(r.operation().equals("imbuement")&&r.data().has("pedestalItems")){
            List<JsonElement> catalysts=new ArrayList<>();r.data().getAsJsonArray("pedestalItems").forEach(catalysts::add);
            if(!assign(catalysts,0,slots,used.clone(),new int[]{4096}))return null;
        }
        ItemStack output=preview(r,player.registryAccess());
        if(r.operation().equals("enchantment")){
            var key=ResourceKey.create(Registries.ENCHANTMENT,Identifier.parse(r.data().get("enchantment").getAsString()));
            var enchant=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key).orElse(null);
            int tier=r.data().get("level").getAsInt();
            if(enchant==null||tier<1||tier>enchant.value().getMaxLevel()||(!base.is(Items.BOOK)&&!base.is(Items.ENCHANTED_BOOK)&&!enchant.value().isSupportedItem(base))
                ||EnchantmentHelper.getItemEnchantmentLevel(enchant,base)>=tier)return null;
            if(tier>1&&EnchantmentHelper.getItemEnchantmentLevel(enchant,base)!=tier-1)return null;
            for(var existing:EnchantmentHelper.getEnchantmentsForCrafting(base).keySet())if(!existing.equals(enchant)&&!net.minecraft.world.item.enchantment.Enchantment.areCompatible(existing,enchant))return null;
            output=base.is(Items.BOOK)?new ItemStack(Items.ENCHANTED_BOOK):base.copyWithCount(1);
            EnchantmentHelper.updateEnchantments(output,e->e.set(enchant,tier));
        }else if(r.operation().equals("armor_upgrade")){
            int tier=r.data().get("tier").getAsInt();
            if(!(base.getItem() instanceof NouveauGear.Armor)||base.getOrDefault(NouveauGear.ARMOR_TIER,0)!=tier-1)return null;
            output=base.copyWithCount(1);output.set(NouveauGear.ARMOR_TIER,tier);
        }else if(r.operation().equals("reactive_enchantment")){
            if(base.getOrDefault(NouveauGear.REACTIVE,false)||NouveauEquipment.enchantment(player,base,"reactive")>0)return null;
            output=base.copyWithCount(1);output.set(NouveauGear.REACTIVE,true);var holder=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT,Jugcraft.id("nouveau_reactive"))).orElse(null);if(holder!=null)output.enchant(holder,1);
            for(int i=1;i<slots.size();i++)if(used[i]>0&&slots.get(i).has(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.INSCRIPTION)){output.set(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.INSCRIPTION,slots.get(i).get(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.INSCRIPTION));break;}
            if(!output.has(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.INSCRIPTION))return null;
        }else if(r.operation().equals("spell_write")){
            if(!base.getOrDefault(NouveauGear.REACTIVE,false)&&NouveauEquipment.enchantment(player,base,"reactive")==0)return null;output=base.copyWithCount(1);boolean written=false;
            for(int i=1;i<slots.size();i++)if(used[i]>0&&slots.get(i).has(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.INSCRIPTION)){output.set(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.INSCRIPTION,slots.get(i).get(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.INSCRIPTION));written=true;break;}if(!written)return null;
        }else if(r.operation().equals("prestidigitation")){
            boolean style=false;for(int i=1;i<slots.size();i++)if(used[i]>0&&slots.get(i).has(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.INSCRIPTION))style|=slots.get(i).get(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.INSCRIPTION).text().contains("nouveau_prestidigitation");if(!style)return null;output=base.copyWithCount(1);output.set(NouveauGear.GLAMOUR,true);
        }else if(r.operation().equals("dye")||r.operation().equals("book_upgrade")||(r.data().has("keepNbtOfReagent")&&r.data().get("keepNbtOfReagent").getAsBoolean())){
            if(output.isEmpty())return null;
            for(int i=0;i<slots.size();i++)if(used[i]>0&&slots.get(i).getMaxStackSize()==1){output.applyComponents(slots.get(i).getComponentsPatch());break;}
            if(r.operation().equals("dye"))for(var s:slots){var color=io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlock.dyeColor(s);if(color!=null){
                output.set(net.minecraft.core.component.DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(color.getTextureDiffuseColor()));break;}}
        }
        if(output.isEmpty()||(!player.getAbilities().instabuild&&player.totalExperience<r.experience()))return null;
        List<ItemStack> after=new ArrayList<>();
        for(int i=0;i<slots.size();i++){
            var old=slots.get(i);var left=old.copy();left.shrink(used[i]);
            if(used[i]>0&&!equipment&&old.getItem().getCraftingRemainder()!=null){
                var remainder=old.getItem().getCraftingRemainder().create();
                if(!left.isEmpty())return null;left=remainder.copyWithCount(used[i]);
            }after.add(left);
        }
        return new Match(r,used,List.copyOf(after),output);
    }
}

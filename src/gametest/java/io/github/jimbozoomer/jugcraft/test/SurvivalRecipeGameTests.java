package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CuttingRecipe;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotRecipe;
import io.github.jimbozoomer.jugcraft.menagerie.HouseholdConstruction;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;

/** Real data-reload, matching and output checks for the survival acquisition pass. */
public class SurvivalRecipeGameTests {
    @GameTest(maxTicks = 40)
    public void magicalWoodStrippingPreservesAxisAndDoesNotStripTwice(GameTestHelper h) {
        var player=TestPlayers.survivor(h);var pos=new net.minecraft.core.BlockPos(1,1,1);
        for(String name:List.of("jugtania_livingwood","jugtania_livingwood_log","jugtania_dreamwood","jugtania_dreamwood_log","forbidden_aurum_log","forbidden_aurum_wood")){
            var block=BuiltInRegistries.BLOCK.getValue(Jugcraft.id(name));
            String target=name.replace("jugtania_","jugtania_stripped_").replace("forbidden_","forbidden_stripped_");
            var stripped=BuiltInRegistries.BLOCK.getValue(Jugcraft.id(target));
            for(var axis:net.minecraft.core.Direction.Axis.values()){
                h.setBlock(pos,block.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,axis));
                var axe=new ItemStack(Items.IRON_AXE);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,axe);
                h.useBlock(pos,player);
                h.assertTrue(h.getBlockState(pos).is(stripped)&&h.getBlockState(pos).getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)==axis&&axe.getDamageValue()==1,"Strip once with axis: "+name);
                h.useBlock(pos,player);
                h.assertTrue(h.getBlockState(pos).is(stripped)&&axe.getDamageValue()==1,"Already stripped remains intact: "+name);
            }
        }
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void sunblockRewardRequiresPaymentAndCannotSpamHealing(GameTestHelper h) {
        var player=TestPlayers.survivor(h);
        var item=BuiltInRegistries.ITEM.getValue(Jugcraft.id("wilds_sunblock_staff"));
        var staff=new ItemStack(item);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,staff);
        item.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(!player.hasEffect(io.github.jimbozoomer.jugcraft.wilds.WildSunEquipment.SUNBLOCK),"No unpaid healing");
        var gold=new ItemStack(Items.GOLD_INGOT,2);player.getInventory().setItem(1,gold);
        item.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(gold.getCount()==1&&player.hasEffect(io.github.jimbozoomer.jugcraft.wilds.WildSunEquipment.SUNBLOCK),"One ingot buys the timed effect");
        h.assertTrue(player.getCooldowns().isOnCooldown(staff),"Paid cast starts cooldown");
        item.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(gold.getCount()==1,"Repeated use during cooldown neither charges nor refreshes");
        h.succeed();
    }

    private static JsonElement read(String name) {
        try (var in = SurvivalRecipeGameTests.class.getResourceAsStream(name)) {
            if (in == null) throw new IllegalStateException("Missing fixture " + name);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    private static ItemStack ingredient(GameTestHelper h, JsonElement json) {
        // Native ritual loaders intentionally keep the owner's {item}/{tag}
        // descriptors; vanilla 26.3 Ingredient.CODEC uses strings instead.
        if(json.isJsonObject()){
            var object=json.getAsJsonObject();
            if(object.has("item"))json=object.get("item");
            else if(object.has("tag"))json=new JsonPrimitive("#"+object.get("tag").getAsString());
        }
        var value = Ingredient.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, h.getLevel().registryAccess()), json).getOrThrow();
        h.assertTrue(!value.isEmpty(), "Ingredient resolves after the real reload: " + json);
        return new ItemStack(value.items().findFirst().orElseThrow());
    }
    private static void result(GameTestHelper h, ItemStack result, JsonObject expected, String name) {
        h.assertTrue(!result.isEmpty(), "Output is not air: " + name);
        h.assertTrue(BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals(expected.get("id").getAsString()), "Correct output: " + name);
        h.assertTrue(result.getCount() == (expected.has("count") ? expected.get("count").getAsInt() : 1), "Correct output quantity: " + name);
        h.assertTrue(result.getCount() <= result.getMaxStackSize(), "Output fits its real stack limit: " + name);
    }
    @GameTest(maxTicks = 40)
    public void newestAcquisitionRecipesAndPaidForgeUpgradesLoadAndMatch(GameTestHelper h) {
        var cases=read("/newest-acquisition-cases.json").getAsJsonObject();
        for(var entry:cases.getAsJsonArray("crafting")){
            String name=entry.getAsString();
            var data=read("/data/jugcraft/recipe/"+name+".json").getAsJsonObject();
            var key=ResourceKey.create(Registries.RECIPE,Jugcraft.id(name));
            var recipe=(CraftingRecipe)h.getLevel().recipeAccess().byKey(key).orElseThrow().value();
            var stacks=new ArrayList<ItemStack>();int width=3,height;
            if(data.has("pattern")){
                var pattern=data.getAsJsonArray("pattern");width=pattern.get(0).getAsString().length();height=pattern.size();
                for(var row:pattern)for(char c:row.getAsString().toCharArray())stacks.add(c==' '?ItemStack.EMPTY:ingredient(h,data.getAsJsonObject("key").get(Character.toString(c))));
            }else{
                for(var value:data.getAsJsonArray("ingredients"))stacks.add(ingredient(h,value));
                height=(stacks.size()+2)/3;while(stacks.size()<width*height)stacks.add(ItemStack.EMPTY);
            }
            if(data.get("type").getAsString().equals("jugcraft:botanical_water_shaped"))for(var stack:stacks)if(stack.is(Items.POTION))
                stack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.WATER));
            var input=CraftingInput.of(width,height,stacks);
            h.assertTrue(recipe.matches(input,h.getLevel()),"Restored recipe matches: "+name);
            result(h,recipe.assemble(input),data.getAsJsonObject("result"),name);
            h.assertTrue(!recipe.matches(CraftingInput.EMPTY,h.getLevel()),"No free crafting: "+name);
        }
        for(var entry:cases.getAsJsonArray("rituals")){
            String name=entry.getAsString();
            var ritual=io.github.jimbozoomer.jugcraft.arcane.ArcaneRituals.all().stream()
                    .filter(r->r.id().equals("jugcraft:acquisition_"+name)).findFirst().orElseThrow();
            var main=ingredient(h,ritual.data().get("main_ingredient"));var slots=new ArrayList<ItemStack>();
            for(var input:ritual.inputs())slots.add(ingredient(h,input));
            h.assertTrue(ritual.matches(main,slots,ritual.tier(),ritual.essence(),ritual.essence(),List.of()),"Paid ritual matches: "+name);
            h.assertTrue(!ritual.matches(main,slots,ritual.tier()-1,ritual.essence(),ritual.essence(),List.of()),"Cannot skip forge tier: "+name);
            h.assertTrue(!ritual.matches(main,slots,ritual.tier(),new int[4],ritual.essence(),List.of()),"Cannot skip essence payment: "+name);
            var output=ritual.output(h.getLevel().registryAccess(),main);
            h.assertTrue(!output.isEmpty()&&BuiltInRegistries.ITEM.getKey(output.getItem()).toString().equals("jugcraft:forbidden_"+name),"Valid ritual output: "+name);
        }
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void everyCompletedRecipeMatchesRealIngredientsAndProducesValidStacks(GameTestHelper h) {
        int checked = 0; var failures=new ArrayList<String>();
        for (var entry : read("/survival-recipe-cases.json").getAsJsonArray()) {
            String name = entry.getAsString();
            try {
            var data = read("/data/jugcraft/recipe/survival_completion/" + name + ".json").getAsJsonObject();
            var key = ResourceKey.create(Registries.RECIPE, Jugcraft.id("survival_completion/" + name));
            var recipe = h.getLevel().recipeAccess().byKey(key).orElseThrow(() -> new IllegalStateException("Recipe rejected by reload: " + name)).value();
            if (recipe instanceof CraftingRecipe crafting) {
                List<ItemStack> stacks = new ArrayList<>(); int width = 3, height;
                if (data.has("pattern")) {
                    var pattern = data.getAsJsonArray("pattern"); width = pattern.get(0).getAsString().length(); height = pattern.size();
                    for (var row : pattern) for (char c : row.getAsString().toCharArray())
                        stacks.add(c == ' ' ? ItemStack.EMPTY : ingredient(h, data.getAsJsonObject("key").get(Character.toString(c))));
                } else {
                    for (var i : data.getAsJsonArray("ingredients")) stacks.add(ingredient(h, i));
                    height = (stacks.size() + 2) / 3;
                    while (stacks.size() < width * height) stacks.add(ItemStack.EMPTY);
                }
                var grid = CraftingInput.of(width, height, stacks);
                h.assertTrue(crafting.matches(grid, h.getLevel()), "Actual grid matches " + name);
                result(h, crafting.assemble(grid), data.getAsJsonObject("result"), name);
                // Validate the default output or the actual conflicting-recipe picker.
                var selected = h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, grid, h.getLevel()).orElseThrow();
                if (selected.id().equals(key)) {
                    result(h, selected.value().assemble(grid), data.getAsJsonObject("result"), "selected grid " + name);
                } else {
                    // Shared layouts are intentionally supported by the owner's
                    // output picker; verify this recipe remains reachable there.
                    var player=TestPlayers.survivor(h);
                    var pos=h.absolutePos(net.minecraft.core.BlockPos.ZERO);
                    h.getLevel().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.CRAFTING_TABLE.defaultBlockState());
                    player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
                    var menu=new net.minecraft.world.inventory.CraftingMenu(63,player.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(h.getLevel(),pos));
                    player.containerMenu=menu;
                    var choiceGrid=io.github.jimbozoomer.jugcraft.crafting.CraftingChoices.grid(menu);
                    for(int y=0;y<height;y++)for(int x=0;x<width;x++)choiceGrid.inputs().setItem(y*3+x,stacks.get(y*width+x).copy());
                    var choices=io.github.jimbozoomer.jugcraft.crafting.CraftingChoices.state(choiceGrid.result());
                    h.assertTrue(io.github.jimbozoomer.jugcraft.crafting.CraftingChoices.select(player,63,choices.revision(),key.identifier()),"Completion recipe is selectable: "+name);
                    result(h,menu.getSlot(0).getItem(),data.getAsJsonObject("result"),"selected grid "+name);
                }
                h.assertTrue(!crafting.matches(CraftingInput.EMPTY, h.getLevel()), "No free empty-grid output: " + name);
            } else if (recipe instanceof CuttingRecipe cutting) {
                var input = new SingleRecipeInput(ingredient(h, data.get("ingredient")));
                h.assertTrue(cutting.matches(input, h.getLevel()) && !cutting.tool().isEmpty(), "Cutting input and knife exist: " + name);
                var results = cutting.cut(); var expected = data.getAsJsonArray("results");
                h.assertTrue(results.size() == expected.size(), "All cutting byproducts are retained: " + name);
                for (int i = 0; i < results.size(); i++) result(h, results.get(i), expected.get(i).getAsJsonObject(), name);
            } else if (recipe instanceof CookingPotRecipe cooking) {
                var stacks = new ArrayList<ItemStack>();
                for(var material:data.getAsJsonArray("ingredients")){
                    var part=material.getAsJsonObject();var stack=ingredient(h,part.get("ingredient"));
                    stack.setCount(part.has("count")?part.get("count").getAsInt():1);stacks.add(stack);
                }
                var input=new CookingPotRecipe.Input(stacks);
                h.assertTrue(cooking.matches(input,h.getLevel()),"Cooking pot accepts complete paid ingredients: "+name);
                result(h,cooking.assemble(input),data.getAsJsonObject("result"),name);
            } else if (recipe instanceof HouseholdConstruction construction) {
                for (var material : data.getAsJsonArray("ingredients")) ingredient(h, material.getAsJsonObject().get("ingredient"));
                result(h, construction.assemble(new SingleRecipeInput(ItemStack.EMPTY)), data.getAsJsonObject("result"), name);
            } else if (recipe instanceof SingleItemRecipe single) {
                var input = new SingleRecipeInput(ingredient(h, data.get("ingredient")));
                h.assertTrue(single.matches(input, h.getLevel()), "Cooking/stonecutting input matches: " + name);
                result(h, single.assemble(input), data.getAsJsonObject("result"), name);
            } else throw new IllegalStateException("Untested serializer " + name);
            checked++;
            } catch (RuntimeException error) { failures.add(name+": "+error.getMessage()); }
        }
        h.assertTrue(failures.isEmpty(), "Recipe failures: " + String.join("; ",failures));
        h.assertTrue(checked >= 124, "All completion recipes were exercised");
        h.succeed();
    }
    @GameTest(maxTicks = 20)
    public void exportActualItemRegistryForTheAcquisitionAudit(GameTestHelper h) {
        try {
            var path = java.nio.file.Path.of(System.getProperty("jugcraft.survivalRegistryOutput", "survival-item-registry.json"));
            var ids = BuiltInRegistries.ITEM.keySet().stream().map(Object::toString).sorted().toList();
            java.nio.file.Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(ids) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) { throw new UncheckedIOException(e); }
        h.assertTrue(!BuiltInRegistries.ITEM.containsKey(Jugcraft.id("companion_roly_pack")), "Animal overlay is not a survival item");
        h.succeed();
    }
    @GameTest(maxTicks = 20)
    public void capsidMusicDiscConversionHasRealSurvivalInputs(GameTestHelper h) {
        var root = read("/jugcraft/menagerie/beast.json").getAsJsonObject(); boolean found = false;
        for (var row : root.getAsJsonArray("capsid")) {
            var r = row.getAsJsonObject();
            for (var i : r.getAsJsonArray("ingredients")) ingredient(h, i);
            if (r.getAsJsonObject("result").get("id").getAsString().equals("jugcraft:beast_music_disc_daze")) {
                var disc = Ingredient.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, h.getLevel().registryAccess()), r.getAsJsonArray("ingredients").get(0)).getOrThrow();
                h.assertTrue(disc.test(new ItemStack(Items.MUSIC_DISC_CAT)), "A normal disc works in the Capsid");
                h.assertTrue(!disc.test(new ItemStack(Items.DIRT)), "An ordinary block cannot substitute for a disc"); found = true;
            }
        }
        h.assertTrue(found, "Daze conversion exists"); h.succeed();
    }
    @GameTest(maxTicks=40)
    public void sodaLakePlacesARealCollectibleSource(GameTestHelper h){
        var config=read("/data/jugcraft/worldgen/feature/cavern_survival_soda_lake.json");
        var lake=io.github.jimbozoomer.jugcraft.menagerie.LandscapeFeature.CODEC.codec().parse(RegistryOps.create(JsonOps.INSTANCE,h.getLevel().registryAccess()),config).getOrThrow();
        var origin=h.absolutePos(new net.minecraft.core.BlockPos(4,4,4));
        h.assertTrue(lake.place(h.getLevel(),h.getLevel().getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(7),origin),"Soda lake generates its bounded basin");
        var pos=origin.below();var state=h.getLevel().getBlockState(pos);
        h.assertTrue(BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(Jugcraft.id("cavern_purple_soda")),"Basin contains actual soda");
        h.assertTrue(state.getBlock() instanceof net.minecraft.world.level.block.BucketPickup,"Source is bucket-collectible");
        var bucket=((net.minecraft.world.level.block.BucketPickup)state.getBlock()).pickupBlock(null,h.getLevel(),pos,state);
        h.assertTrue(BuiltInRegistries.ITEM.getKey(bucket.getItem()).equals(Jugcraft.id("cavern_purple_soda_bucket")),"Collecting yields the real recipe ingredient");
        h.assertTrue(h.getLevel().getFluidState(pos).isEmpty(),"Collection consumes the source");h.succeed();
    }
    @GameTest(maxTicks=40)
    public void generatedContainersKeepTheirLootUntilOpenedAndNeverReroll(GameTestHelper h){
        var pos=new net.minecraft.core.BlockPos(1,1,1);int checked=0;
        for(var row:read("/survival-chest-cases.json").getAsJsonArray()){
            var d=row.getAsJsonObject();var coords=d.getAsJsonArray("pos");
            var target=new net.minecraft.core.BlockPos(coords.get(0).getAsInt(),coords.get(1).getAsInt(),coords.get(2).getAsInt());
            var template=h.getLevel().getStructureTemplateManager().get(Identifier.parse(d.get("template").getAsString())).orElseThrow();
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo found=null;
            for(String block:List.of("minecraft:chest","minecraft:barrel","jugcraft:cavern_metal_barrel","jugcraft:cavern_rusty_barrel","jugcraft:cavern_gingerbarrel")){
                var type=BuiltInRegistries.BLOCK.getValue(Identifier.parse(block));
                for(var info:template.filterBlocks(net.minecraft.core.BlockPos.ZERO,new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),type))
                    if(info.pos().equals(target))found=info;
            }
            h.assertTrue(found!=null&&found.nbt()!=null,"Generated template has the real container: "+d);
            h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);
            h.setBlock(pos,found.state());
            var container=h.getBlockEntity(pos,net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity.class);
            container.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),found.nbt()));
            h.assertTrue(container.getLootTable()!=null&&container.getLootTable().identifier().toString().equals(d.get("table").getAsString()),"Container owns intended loot table");
            var saved=container.saveWithoutMetadata(h.getLevel().registryAccess());
            container.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
            h.assertTrue(container.getLootTable()!=null,"Unopened loot survives chunk save/load");
            container.unpackLootTable(null);
            h.assertTrue(container.getLootTable()==null,"Opening consumes the loot assignment exactly once");
            container.clearContent();
            saved=container.saveWithoutMetadata(h.getLevel().registryAccess());
            container.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
            container.unpackLootTable(null);
            h.assertTrue(container.isEmpty(),"Loot cannot reroll after emptying and reloading");checked++;
        }
        h.assertTrue(checked>=21,"All connected generated containers checked");h.succeed();
    }
    @GameTest(maxTicks=40)
    public void everyNewSourcePoolLoadsAndProducesTheRegisteredReward(GameTestHelper h){
        for(String path:List.of("ribbits_chests/fisherman_main","ribbits_chests/fisherman_storage","ribbits_chests/gardener","ribbits_chests/merchant","ribbits_chests/nitwit","ribbits_chests/sorcerer",
                "botanical_gaia/normal_killer","botanical_gaia/hard_killer","entities/wilds_umvuthi","ars_additions_chests/arcane_library","ars_additions_chests/nexus_tower")){
            var key=ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id(path));
            h.assertTrue(h.getLevel().getServer().reloadableRegistries().getLootTable(key)!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"Restored collection reward table loads: "+path);
        }
        for(var row:read("/survival-source-cases.json").getAsJsonArray()){
            var d=row.getAsJsonObject();String id=d.get("table").getAsString(),item=d.get("item").getAsString();
            var key=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse(id));
            h.assertTrue(h.getLevel().getServer().reloadableRegistries().getLootTable(key)!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"Source table actually loaded: "+id);
            var table=read("/data/jugcraft/loot_table/"+id.substring(9)+".json").getAsJsonObject();JsonObject pool=null;
            for(var p:table.getAsJsonArray("pools"))if(p.getAsJsonObject().has("name")&&p.getAsJsonObject().get("name").getAsString().equals("jugcraft_source_"+item.substring(9)))pool=p.getAsJsonObject().deepCopy();
            h.assertTrue(pool!=null,"Named source pool exists: "+item);
            h.assertTrue(pool.getAsJsonObject("condition").get("chance").getAsDouble()==d.get("chance").getAsDouble(),"Configured chance retained");
            // Isolate the new pool and force only its random gate for a
            // deterministic codec/output check. Production chance is unchanged.
            pool.remove("condition");var isolated=new JsonObject();var pools=new JsonArray();pools.add(pool);isolated.add("pools",pools);
            var parsed=net.minecraft.world.level.storage.loot.LootTable.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE,h.getLevel().registryAccess()),isolated).getOrThrow();
            var fixture=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:pig")).create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.ZERO)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,fixture)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.GIFT);
            var drops=parsed.value().getRandomItems(params);
            h.assertTrue(drops.size()==1&&drops.get(0).getCount()==1&&BuiltInRegistries.ITEM.getKey(drops.get(0).getItem()).toString().equals(item),"Pool yields one real reward: "+item);
        }
        h.succeed();
    }
}

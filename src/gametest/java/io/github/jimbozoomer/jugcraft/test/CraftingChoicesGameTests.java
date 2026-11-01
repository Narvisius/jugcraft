package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.crafting.CraftingChoices;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

public final class CraftingChoicesGameTests {
    private static CraftingMenu table(GameTestHelper h, ServerPlayer p) {
        var pos=h.absolutePos(BlockPos.ZERO);h.getLevel().setBlockAndUpdate(pos,Blocks.CRAFTING_TABLE.defaultBlockState());
        p.setPos(pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5);
        var menu=new CraftingMenu(41,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),pos));p.containerMenu=menu;return menu;
    }
    private static void carrots(AbstractContainerMenu menu,int amount) {
        var grid=CraftingChoices.grid(menu);for(int i=0;i<9;i++)grid.inputs().setItem(i,new ItemStack(Items.CARROT,amount));
    }
    @GameTest public void selectedOutputConsumesOnlyNormalIngredientsAndShiftCrafts(GameTestHelper h) {
        var p=TestPlayers.survivor(h);var menu=table(h,p);carrots(menu,3);var grid=CraftingChoices.grid(menu);
        var state=CraftingChoices.state(grid.result());
        h.assertTrue(state.options().stream().anyMatch(o->o.recipe().equals(Jugcraft.id("giant_carrot"))),"Giant carrot is selectable");
        h.assertTrue(state.options().stream().anyMatch(o->o.recipe().equals(Jugcraft.id("carrot_crate"))),"Carrot crate is selectable");
        h.assertTrue(CraftingChoices.select(p,41,state.revision(),Jugcraft.id("giant_carrot")),"Choose giant carrot");
        menu.clicked(0,0,ContainerInput.PICKUP,p);
        h.assertTrue(menu.getCarried().is(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Jugcraft.id("giant_carrot"))),"Actual cursor output matches choice");
        for(int i=0;i<9;i++)h.assertValueEqual(grid.inputs().getItem(i).getCount(),2,"Exactly one ingredient consumed per slot");
        menu.setCarried(ItemStack.EMPTY);
        h.assertTrue(CraftingChoices.select(p,41,state.revision(),Jugcraft.id("carrot_crate")),"Can change remaining batch to crate");
        menu.clicked(0,0,ContainerInput.QUICK_MOVE,p);
        h.assertValueEqual(p.getInventory().countItem(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Jugcraft.id("carrot_crate"))),2,"Shift craft keeps the selected output");
        h.assertTrue(grid.inputs().isEmpty(),"All ingredients used once");h.succeed();
    }
    @GameTest public void staleForgedRemoteAndOtherPlayerSelectionsAreRejected(GameTestHelper h) {
        var p=TestPlayers.survivor(h);var menu=table(h,p);carrots(menu,2);var grid=CraftingChoices.grid(menu);
        long revision=CraftingChoices.state(grid.result()).revision();
        h.assertTrue(!CraftingChoices.select(p,41,revision,Identifier.withDefaultNamespace("diamond_block")),"Unmatched recipe refused");
        grid.inputs().setItem(0,ItemStack.EMPTY);
        h.assertTrue(!CraftingChoices.select(p,41,revision,Jugcraft.id("giant_carrot")),"Changed grid refuses stale choice");
        carrots(menu,2);revision=CraftingChoices.state(grid.result()).revision();
        h.assertTrue(!CraftingChoices.select(p,42,revision,Jugcraft.id("giant_carrot")),"Wrong menu refused");
        var other=TestPlayers.survivor(h);
        h.assertTrue(!CraftingChoices.select(other,41,revision,Jugcraft.id("giant_carrot")),"Another player cannot use this menu");
        p.setPos(p.position().add(100,0,0));
        h.assertTrue(!CraftingChoices.select(p,41,revision,Jugcraft.id("giant_carrot")),"Out of reach refused");
        for(int i=0;i<9;i++)h.assertValueEqual(grid.inputs().getItem(i).getCount(),2,"Selection never spends items");h.succeed();
    }
    @GameTest public void inventoryGridAndBucketsRetainVanillaConsumption(GameTestHelper h) {
        var p=TestPlayers.survivor(h);var menu=p.inventoryMenu;p.containerMenu=menu;var grid=CraftingChoices.grid(menu);
        grid.inputs().setItem(0,new ItemStack(Items.MILK_BUCKET));var state=CraftingChoices.state(grid.result());
        h.assertTrue(CraftingChoices.select(p,0,state.revision(),Identifier.fromNamespaceAndPath("jugcraft-test","choice_bucket_b")),"Inventory 2x2 choice");
        menu.clicked(0,0,ContainerInput.PICKUP,p);
        h.assertTrue(menu.getCarried().is(Items.CARROT),"Selected fixture output");
        h.assertTrue(grid.inputs().getItem(0).is(Items.BUCKET),"Bucket returned, not duplicated or lost");
        h.assertValueEqual(grid.inputs().getItem(0).getCount(),1,"One returned bucket");
        h.assertTrue(CraftingChoices.state(grid.result()).options().size()<=1,"Choices disappear after ingredients change");h.succeed();
    }
    @GameTest public void bothStorageCraftingGridsUseTheSameChoices(GameTestHelper h) {
        var p=TestPlayers.survivor(h);var pos=h.absolutePos(new BlockPos(1,1,1));p.setPos(pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5);
        h.getLevel().setBlockAndUpdate(pos,io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalStorage.BLOCKS.get("crafting_terminal").defaultBlockState());
        var node=(io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalNode)h.getLevel().getBlockEntity(pos);node.owner(p.getUUID());node.energy.setAmount(10000);
        var digital=new io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalTerminal.Menu(52,p.getInventory(),node);
        p.containerMenu=digital;carrots(digital,1);var dg=CraftingChoices.grid(digital);
        h.assertTrue(CraftingChoices.select(p,52,CraftingChoices.state(dg.result()).revision(),Jugcraft.id("giant_carrot")),"ME crafting terminal chooses a matching output");
        p.getInventory().add(new ItemStack(io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalStorage.ITEMS.get("blank_pattern")));
        h.assertTrue(digital.clickMenuButton(p,4),"Encode selected recipe");
        io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalPatterns.Pattern encoded=null;
        for(int i=0;i<36;i++){
            var candidate=p.getInventory().getItem(i).get(io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalPatterns.PATTERN);
            if(candidate!=null)encoded=candidate;
        }
        h.assertTrue(encoded!=null&&encoded.recipeId().equals("jugcraft:giant_carrot"),"Encoded pattern preserves chosen recipe ID");
        h.assertTrue(encoded.output(h.getLevel()).is(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Jugcraft.id("giant_carrot"))),"Encoded output stays selected");
        var missing=new io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalPatterns.Pattern(encoded.grid(),0,"jugcraft:missing_choice_recipe");
        h.assertTrue(missing.output(h.getLevel()).isEmpty(),"Missing pinned recipe cannot fall back to a different output");
        digital.clicked(dg.slot(),0,ContainerInput.PICKUP,p);
        h.assertTrue(dg.inputs().isEmpty(),"ME crafting consumes actual grid items");digital.setCarried(ItemStack.EMPTY);
        h.getLevel().setBlockAndUpdate(pos,io.github.jimbozoomer.jugcraft.nouveau.JugNouveau.LECTERN.defaultBlockState());
        var lectern=(io.github.jimbozoomer.jugcraft.nouveau.StorageLectern)h.getLevel().getBlockEntity(pos);lectern.placedBy(p);lectern.bind(p);
        var magical=new io.github.jimbozoomer.jugcraft.nouveau.NouveauStorageTerminal.Menu(53,p.getInventory(),lectern);
        p.containerMenu=magical;carrots(magical,1);var mg=CraftingChoices.grid(magical);
        h.assertTrue(CraftingChoices.select(p,53,CraftingChoices.state(mg.result()).revision(),Jugcraft.id("carrot_crate")),"Magical crafting terminal chooses a matching output");
        magical.clicked(mg.slot(),0,ContainerInput.PICKUP,p);
        h.assertTrue(mg.inputs().isEmpty(),"Magical crafting consumes actual grid items");h.succeed();
    }
    @GameTest public void limitedCraftingAndRecipeHintsStillApply(GameTestHelper h) {
        var p=TestPlayers.survivor(h);var menu=table(h,p);var grid=CraftingChoices.grid(menu);
        var rules=h.getLevel().getGameRules();var key=net.minecraft.world.level.gamerules.GameRules.LIMITED_CRAFTING;
        boolean old=rules.get(key);
        try {
            rules.set(key,true,h.getLevel().getServer());carrots(menu,1);
            h.assertTrue(grid.result().getItem(0).isEmpty(),"Limited crafting blocks unlearned results");
            var recipe=h.getLevel().getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,Jugcraft.id("giant_carrot"))).orElseThrow();
            p.awardRecipes(java.util.List.of(recipe));CraftingChoices.refresh(menu,p);
            h.assertValueEqual(CraftingChoices.state(grid.result()).options().size(),1,"Only unlocked result offered");
        } finally { rules.set(key,old,h.getLevel().getServer()); }
        carrots(menu,2);var chosen=CraftingChoices.recipes(h.getLevel().getServer().getRecipeManager()).stream().filter(r->r.id().identifier().equals(Jugcraft.id("giant_carrot"))).findFirst().orElseThrow();
        menu.finishPlacingRecipe(h.getLevel(),chosen);
        h.assertValueEqual(CraftingChoices.state(grid.result()).selected().id(),chosen.id(),"Recipe book hint wins over earlier choice");h.succeed();
    }
    @GameTest public void liveAuditWitnessesSelectEveryDifferentOutput(GameTestHelper h) throws Exception {
        var p=TestPlayers.survivor(h);var menu=table(h,p);var grid=CraftingChoices.grid(menu);
        var input=CraftingChoicesGameTests.class.getResourceAsStream("/jugcraft-test/recipe-overlap-witnesses.json");
        var pairs=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
        int verified=0;
        for(var entry:pairs){
            var pair=entry.getAsJsonObject();var witness=pair.getAsJsonObject("witness");int width=witness.get("width").getAsInt();
            grid.inputs().clearContent();var items=witness.getAsJsonArray("items");
            for(int i=0;i<items.size();i++)if(!items.get(i).isJsonNull())grid.inputs().setItem(i%width+(i/width)*3,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.parse(items.get(i).getAsString()))));
            CraftingChoices.refresh(menu,p);
            for(var id:pair.getAsJsonArray("recipes")){
                var recipe=h.getLevel().getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,Identifier.parse(id.getAsString())));
                if(recipe.isEmpty())continue;
                var r=(net.minecraft.world.item.crafting.CraftingRecipe)recipe.get().value();
                h.assertTrue(r.matches(grid.inputs().asCraftInput(),h.getLevel()),"Audit witness must match "+id);
                var expected=r.assemble(grid.inputs().asCraftInput());
                var state=CraftingChoices.state(grid.result());
                var option=state.options().stream().filter(o->o.output().getCount()==expected.getCount()&&ItemStack.isSameItemSameComponents(o.output(),expected)).findFirst().orElseThrow();
                h.assertTrue(CraftingChoices.select(p,41,state.revision(),option.recipe()),"Select actual overlapping result "+id);verified++;
            }
        }
        h.assertTrue(verified>=20,"Runtime verifies meaningful overlap coverage");
        Jugcraft.LOGGER.info("Crafting overlap audit: {} loaded witness outputs verified",verified);h.succeed();
    }
}

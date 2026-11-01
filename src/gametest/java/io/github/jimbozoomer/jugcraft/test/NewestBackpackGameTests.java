package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpackStacks;
import java.util.List;
import java.util.ArrayList;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.*;

public final class NewestBackpackGameTests {
    @GameTest public void dyeingEveryTierPreservesCargoAndModules(GameTestHelper h){
        for(var tier:List.of("backpack","copper_backpack","iron_backpack","gold_backpack","diamond_backpack","netherite_backpack")){
            var bag=new ItemStack(OwnerBackpacks.ITEMS.get(tier));
            bag.set(OwnerBackpacks.CONTENTS,List.of(new OwnerBackpackStacks.Entry(new ItemStack(Items.DIAMOND,23),23)));
            bag.set(OwnerBackpacks.UPGRADES,ItemContainerContents.fromItems(List.of(new ItemStack(OwnerBackpacks.ITEMS.get("pickup_upgrade")))));
            bag.set(DataComponents.CUSTOM_NAME,Component.literal("Keep my cargo"));
            var input=CraftingInput.of(2,1,List.of(bag,new ItemStack(Items.DYE.pick(DyeColor.RED))));
            var recipe=h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow();
            var dyed=recipe.value().assemble(input);
            h.assertTrue(dyed.is(bag.getItem())&&dyed.has(DataComponents.DYED_COLOR),"Dye preserves tier: "+tier);
            h.assertTrue(dyed.get(OwnerBackpacks.CONTENTS).equals(bag.get(OwnerBackpacks.CONTENTS)),"Dye preserves exact cargo");
            h.assertTrue(dyed.get(OwnerBackpacks.UPGRADES).equals(bag.get(OwnerBackpacks.UPGRADES)),"Dye preserves installed modules");
            h.assertTrue(dyed.getHoverName().equals(bag.getHoverName())&&!bag.has(DataComponents.DYED_COLOR),"Preview retains name and does not mutate original");
        }
        h.succeed();
    }
    @GameTest public void survivalTierUpgradePreservesCargoNameAndUpgradeModules(GameTestHelper h){
        var bag=new ItemStack(OwnerBackpacks.ITEMS.get("backpack"));var treasure=new ItemStack(Items.DIAMOND,27);treasure.set(DataComponents.CUSTOM_NAME,Component.literal("My gems"));
        bag.set(DataComponents.CUSTOM_NAME,Component.literal("Expedition"));
        bag.set(OwnerBackpacks.CONTENTS,List.of(new OwnerBackpackStacks.Entry(treasure,27)));
        bag.set(OwnerBackpacks.UPGRADES,ItemContainerContents.fromItems(List.of(new ItemStack(OwnerBackpacks.ITEMS.get("pickup_upgrade")))));
        var grid=new ArrayList<ItemStack>();for(int i=0;i<9;i++)grid.add(i==4?bag:new ItemStack(Items.IRON_INGOT));var input=CraftingInput.of(3,3,grid);
        var recipe=h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow();var result=recipe.value().assemble(input);
        h.assertTrue(result.is(OwnerBackpacks.ITEMS.get("iron_backpack")),"Native crafting finds actual paid tier upgrade");
        h.assertTrue(result.getHoverName().getString().equals("Expedition"),"Name survives tier change");
        h.assertTrue(ItemStack.matches(result.get(OwnerBackpacks.CONTENTS).getFirst().stack(),treasure),"Exact cargo survives tier change");
        h.assertTrue(result.get(OwnerBackpacks.UPGRADES).itemCopies().filter(s->s.is(OwnerBackpacks.ITEMS.get("pickup_upgrade"))).count()==1,"Installed module survives tier change");
        h.assertTrue(bag.is(OwnerBackpacks.ITEMS.get("backpack"))&&bag.get(OwnerBackpacks.CONTENTS).getFirst().count()==27,"Recipe preview never mutates input");h.succeed();
    }
    @GameTest public void stackUpgradesCannotCombineDistinctEquipment(GameTestHelper h){
        var modules=ItemContainerContents.fromItems(List.of(new ItemStack(OwnerBackpacks.ITEMS.get("stack_upgrade_omega_tier"))));
        h.assertTrue(OwnerBackpackStacks.limit(modules,new ItemStack(Items.DIAMOND_SWORD))==1,"Tool stacks remain individually addressable");
        h.assertTrue(OwnerBackpackStacks.limit(modules,new ItemStack(Items.COBBLESTONE))==Integer.MAX_VALUE,"Bulk stack count is bounded without overflow");h.succeed();
    }
}

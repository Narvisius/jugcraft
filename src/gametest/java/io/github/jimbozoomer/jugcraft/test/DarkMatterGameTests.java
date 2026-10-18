package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.phys.AABB;

public class DarkMatterGameTests {
    private static final BlockPos CENTRE = new BlockPos(3, 3, 3);
    private static Item item(String id) { return DarkMatter.ITEMS.get(id); }
    private static ServerPlayer player(GameTestHelper h, String tool, int mode) {
        var p = h.makeMockServerPlayerInLevel(); p.setGameMode(GameType.SURVIVAL);
        var at = h.absolutePos(new BlockPos(3, 2, 1)); p.snapTo(at.getX()+.5, at.getY(), at.getZ()+.5, 0, 0);
        ConcordanceProgress.grant(p, "jugcraft:first_light", ResearchState.UNDERSTOOD);
        ConcordanceProgress.grant(p, Assaying.RESEARCH, ResearchState.UNDERSTOOD);
        ItemStack stack = new ItemStack(item("dark_matter_"+tool)); stack.set(DarkMatter.MODE, mode);
        p.setItemInHand(InteractionHand.MAIN_HAND,stack); Assaying.setBalance(p,1000); return p;
    }
    private static void face(GameTestHelper h, Block block) {
        for(int x=2;x<=4;x++)for(int y=2;y<=4;y++)h.setBlock(new BlockPos(x,y,3),block);
    }
    private static long remainingFace(GameTestHelper h, Block block) {
        long count=0;for(int x=2;x<=4;x++)for(int y=2;y<=4;y++)if(h.getBlockState(new BlockPos(x,y,3)).is(block))count++;return count;
    }
    private static void mine(GameTestHelper h, ServerPlayer p, BlockPos pos) {
        h.assertTrue(p.gameMode.destroyBlock(h.absolutePos(pos)), "The ordinary starting block must break");
    }
    private static ItemStack craft(GameTestHelper h, Item... grid) {
        var stacks=new ArrayList<ItemStack>(); for(Item item:grid)stacks.add(item==null?ItemStack.EMPTY:new ItemStack(item));
        var input=CraftingInput.of(3,3,stacks);
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow().value().assemble(input);
    }
    private static void recipe(GameTestHelper h, String result, int count, Item... grid) {
        var made=craft(h,grid);h.assertTrue(made.is(item(result))&&made.getCount()==count,"Crafting route produces "+result);
    }

    @GameTest(maxTicks=20) public void completeCraftingRouteLoads(GameTestHelper h) {
        recipe(h,"low_covalence_dust",4,Items.COBBLESTONE,Items.CHARCOAL,null,null,null,null,null,null,null);
        recipe(h,"medium_covalence_dust",4,Items.IRON_INGOT,Items.REDSTONE,null,null,null,null,null,null,null);
        recipe(h,"high_covalence_dust",4,Items.DIAMOND,Items.LAPIS_LAZULI,null,null,null,null,null,null,null);
        recipe(h,"alchemical_coal",1,Items.COAL,Items.COAL,Items.COAL,Items.COAL,item("low_covalence_dust"),null,null,null,null);
        recipe(h,"mobius_fuel",1,item("alchemical_coal"),item("alchemical_coal"),item("medium_covalence_dust"),null,null,null,null,null,null);
        recipe(h,"aeternalis_fuel",1,item("mobius_fuel"),item("mobius_fuel"),item("high_covalence_dust"),null,null,null,null,null,null);
        Item f=item("aeternalis_fuel"),m=item("dark_matter");
        recipe(h,"dark_matter",1,f,f,f,f,Items.DIAMOND_BLOCK,f,f,f,f);
        recipe(h,"dark_matter_pickaxe",1,m,m,m,null,Items.STICK,null,null,Items.STICK,null);
        recipe(h,"dark_matter_hammer",1,m,m,m,m,Items.STICK,m,null,Items.STICK,null);
        recipe(h,"dark_matter_axe",1,m,m,null,m,Items.STICK,null,null,Items.STICK,null); h.succeed();
    }

    @GameTest(maxTicks=260) public void allThreeFuelsActuallySmelt(GameTestHelper h) {
        String[] names={"alchemical_coal","mobius_fuel","aeternalis_fuel"};
        for(int i=0;i<3;i++){
            var at=new BlockPos(1+i*2,2,3);h.setBlock(at,Blocks.FURNACE);
            var furnace=(FurnaceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(at));
            var fuel=new ItemStack(item(names[i]));
            h.assertTrue(fuel.get(DataComponents.COOKING_FUEL).burnTime().get(null,0)==DarkMatter.BURN_TICKS[i],"Exact bounded fuel duration");
            furnace.setItem(0,new ItemStack(Items.RAW_IRON));furnace.setItem(1,fuel);furnace.setChanged();
        }
        h.runAfterDelay(220,()->{
            for(int i=0;i<3;i++){
                var f=(FurnaceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1+i*2,2,3)));
                h.assertTrue(f.getItem(2).is(Items.IRON_INGOT)&&f.getItem(2).getCount()==1&&f.getItem(1).isEmpty(),names[i]+" smelted with one fuel");
            }h.succeed();
        });
    }
    @GameTest(maxTicks=20) public void hammerBreaksOnlyOneFaceAndPaysForExtras(GameTestHelper h) {
        var p=player(h,"hammer",1);face(h,Blocks.STONE);var stack=p.getMainHandItem();
        h.setBlock(CENTRE.south(),Blocks.STONE);mine(h,p,CENTRE);
        h.assertTrue(remainingFace(h,Blocks.STONE)==0&&h.getBlockState(CENTRE.south()).is(Blocks.STONE),"Only one 3x3 face, never recursive expansion");
        h.assertTrue(Assaying.balance(p)==872&&stack.getDamageValue()==9,"Eight paid extras and nine normal durability points");h.succeed();
    }
    @GameTest(maxTicks=20) public void insufficientEssenceStopsWithoutOverdraft(GameTestHelper h) {
        var p=player(h,"hammer",1);face(h,Blocks.STONE);Assaying.setBalance(p,20);mine(h,p,CENTRE);
        h.assertTrue(remainingFace(h,Blocks.STONE)==7&&Assaying.balance(p)==4&&p.getMainHandItem().getDamageValue()==2,"One affordable extra only");h.succeed();
    }
    @GameTest(maxTicks=20) public void sneakAndSingleModesArePrecise(GameTestHelper h) {
        var p=player(h,"hammer",1);face(h,Blocks.STONE);p.setShiftKeyDown(true);mine(h,p,CENTRE);
        h.assertTrue(remainingFace(h,Blocks.STONE)==8&&Assaying.balance(p)==1000,"Sneaking suppresses paid work");
        p.setShiftKeyDown(false);p.getMainHandItem().set(DarkMatter.MODE,0);h.setBlock(CENTRE,Blocks.STONE);mine(h,p,CENTRE);
        h.assertTrue(remainingFace(h,Blocks.STONE)==8&&Assaying.balance(p)==1000,"Single-block mode needs no essence");h.succeed();
    }
    @GameTest(maxTicks=20) public void protectedAndContainerBlocksAreKept(GameTestHelper h) {
        var p=player(h,"hammer",1);face(h,Blocks.IRON_BLOCK);BlockPos claimed=new BlockPos(2,2,3),container=new BlockPos(4,4,3);
        h.setBlock(container,Blocks.FURNACE);h.setBlock(new BlockPos(4,2,3),Blocks.OBSIDIAN);
        try(var claims=TestClaims.open()){
            claims.block(h.absolutePos(claimed),UUID.randomUUID());mine(h,p,CENTRE);
            h.assertTrue(h.getBlockState(claimed).is(Blocks.IRON_BLOCK)&&h.getBlockState(container).is(Blocks.FURNACE)
                    &&h.getBlockState(new BlockPos(4,2,3)).is(Blocks.OBSIDIAN),"Claim, block entity and harder neighbour survive");
            h.assertTrue(Assaying.balance(p)==920,"Only five eligible extras are charged");
        }h.succeed();
    }
    @GameTest(maxTicks=20) public void tunnelStopsAtTheFirstBarrier(GameTestHelper h) {
        var p=player(h,"pickaxe",2);h.setBlock(CENTRE,Blocks.STONE);h.setBlock(CENTRE.south(),Blocks.STONE);h.setBlock(CENTRE.south(2),Blocks.STONE);
        try(var claims=TestClaims.open()){
            claims.block(h.absolutePos(CENTRE.south()),UUID.randomUUID());mine(h,p,CENTRE);
            h.assertTrue(h.getBlockState(CENTRE.south(2)).is(Blocks.STONE)&&Assaying.balance(p)==1000,"Cannot tunnel through a claimed block");
        }h.succeed();
    }
    @GameTest(maxTicks=20) public void tunnelFollowsTheViewForOnlyTwoExtras(GameTestHelper h) {
        var p=player(h,"pickaxe",2);for(int d=0;d<4;d++)h.setBlock(CENTRE.south(d),Blocks.STONE);mine(h,p,CENTRE);
        h.assertTrue(h.getBlockState(CENTRE.south(2)).isAir()&&h.getBlockState(CENTRE.south(3)).is(Blocks.STONE)&&Assaying.balance(p)==984,"Three-block tunnel, two paid extras");h.succeed();
    }
    @GameTest(maxTicks=20) public void oreVeinIsBoundedAndKeepsOtherMaterials(GameTestHelper h) {
        var p=player(h,"pickaxe",1);
        for(int x=2;x<=4;x++)for(int y=2;y<=4;y++)for(int z=2;z<=4;z++)h.setBlock(new BlockPos(x,y,z),Blocks.IRON_ORE);
        h.setBlock(new BlockPos(5,3,3),Blocks.GOLD_ORE);mine(h,p,CENTRE);
        long left=0;for(int x=2;x<=4;x++)for(int y=2;y<=4;y++)for(int z=2;z<=4;z++)if(h.getBlockState(new BlockPos(x,y,z)).is(Blocks.IRON_ORE))left++;
        h.assertTrue(left==18&&Assaying.balance(p)==936&&h.getBlockState(new BlockPos(5,3,3)).is(Blocks.GOLD_ORE),"Eight same-ore extras, no mixed ore");h.succeed();
    }
    @GameTest(maxTicks=20) public void fellingIsBoundedAndLeavesRootsAndOtherTrees(GameTestHelper h) {
        var p=player(h,"axe",1);var base=new BlockPos(3,2,3);
        for(int x=2;x<=4;x++)for(int y=2;y<=4;y++)for(int z=2;z<=4;z++)h.setBlock(new BlockPos(x,y,z),Blocks.OAK_LOG);
        h.setBlock(base.below(),Blocks.OAK_LOG);h.setBlock(new BlockPos(5,3,3),Blocks.BIRCH_LOG);mine(h,p,base);
        long left=0;for(int x=2;x<=4;x++)for(int y=2;y<=4;y++)for(int z=2;z<=4;z++)if(h.getBlockState(new BlockPos(x,y,z)).is(Blocks.OAK_LOG))left++;
        h.assertTrue(left==10&&Assaying.balance(p)==872&&h.getBlockState(base.below()).is(Blocks.OAK_LOG)
                &&h.getBlockState(new BlockPos(5,3,3)).is(Blocks.BIRCH_LOG),"Sixteen extra logs; roots and other wood survive");h.succeed();
    }
    @GameTest(maxTicks=20) public void brokenToolCannotContinueMining(GameTestHelper h) {
        var p=player(h,"hammer",1);face(h,Blocks.STONE);p.getMainHandItem().setDamageValue(DarkMatter.DURABILITY-2);mine(h,p,CENTRE);
        h.assertTrue(p.getMainHandItem().isEmpty()&&remainingFace(h,Blocks.STONE)==7&&Assaying.balance(p)==984,"Tool breaks after one extra and stops");h.succeed();
    }
    @GameTest(maxTicks=20) public void anvilRepairPreservesModeAndName(GameTestHelper h) {
        var p=player(h,"pickaxe",2);var stack=p.getMainHandItem().copy();stack.setDamageValue(1500);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Old friend"));
        var menu=new AnvilMenu(1,p.getInventory());menu.getSlot(0).set(stack);
        // The vanilla client sends the input's displayed name. Leaving it blank requests removal of a custom name.
        menu.setItemName(stack.getHoverName().getString());
        menu.getSlot(1).set(new ItemStack(item("high_covalence_dust")));menu.createResult();
        var result=menu.getSlot(2).getItem();
        h.assertTrue(!result.isEmpty()&&result.getDamageValue()==476,"Normal anvil repairs 1024; remaining damage="+result.getDamageValue());
        h.assertTrue(result.get(DarkMatter.MODE)==2&&result.getHoverName().getString().equals("Old friend")&&menu.getCost()>0,
                "Normal anvil keeps mode/name and charges XP: "+result);h.succeed();
    }
    @GameTest(maxTicks=20) public void modesSurviveSavingAndUseCyclesThem(GameTestHelper h) {
        var p=player(h,"pickaxe",0);var stack=p.getMainHandItem();RateGate.forget(p.getUUID());stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(stack.get(DarkMatter.MODE)==1,"Normal use selects vein mode");
        var ops=h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var restored=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,stack).getOrThrow()).getOrThrow();
        h.assertTrue(restored.get(DarkMatter.MODE)==1,"Selected mode persists in a saved item");h.succeed();
    }
    @GameTest(maxTicks=20) public void disabledMagicStopsExtrasButTradedToolsNeedNoResearch(GameTestHelper h) {
        var p=player(h,"hammer",1);face(h,Blocks.STONE);boolean enabled=JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
        try{JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE,false);mine(h,p,CENTRE);}
        finally{JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE,enabled);}
        h.assertTrue(remainingFace(h,Blocks.STONE)==8&&Assaying.balance(p)==1000,"Disabled feature only allows ordinary mining");
        var novice=h.makeMockServerPlayerInLevel();novice.setGameMode(GameType.SURVIVAL);novice.snapTo(p.getX(),p.getY(),p.getZ(),0,0);
        novice.setItemInHand(InteractionHand.MAIN_HAND,p.getMainHandItem().copy());Assaying.setBalance(novice,1000);h.setBlock(CENTRE,Blocks.STONE);mine(h,novice,CENTRE);
        h.assertTrue(remainingFace(h,Blocks.STONE)==0&&Assaying.balance(novice)==872&&!Assaying.knows(novice),"Traded hammer buys eight extras without research");h.succeed();
    }
    @GameTest(maxTicks=20) public void silkTouchUsesNormalLootForEveryBlock(GameTestHelper h) {
        var p=player(h,"hammer",1);face(h,Blocks.STONE);
        p.getMainHandItem().enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
        mine(h,p,CENTRE);
        long stones=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(CENTRE)).inflate(3))
                .stream().filter(e->e.getItem().is(Items.STONE)).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(stones==9,"Every block uses Silk Touch, not raw destroyBlock loot: "+stones);h.succeed();
    }
    @GameTest(maxTicks=20) public void defaultMagicalMaterialsCanBeExchanged(GameTestHelper h) {
        for(Item item:DarkMatter.ITEMS.values())h.assertTrue(Assaying.eligibility(new ItemStack(item)).isEmpty(),"Default magical materials have essence values");
        h.succeed();
    }
}

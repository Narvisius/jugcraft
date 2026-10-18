package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Real recipes, player break callbacks, shared protection, equipment components and essence conservation. */
public class ExtendedAlchemyEquipmentGameTests {
    private static Item item(String id) { return ExtendedAlchemyEquipment.ITEMS.getOrDefault(id, DarkMatter.ITEMS.get(id)); }
    private static ServerPlayer player(GameTestHelper h, String tool, int mode) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        var at = h.absolutePos(new BlockPos(3, 2, 1));
        player.snapTo(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
        ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
        ConcordanceProgress.grant(player, Assaying.RESEARCH, ResearchState.UNDERSTOOD);
        if (tool != null) {
            var stack = new ItemStack(item(tool)); stack.set(DarkMatter.MODE, mode);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        }
        Assaying.setBalance(player, 1000);
        return player;
    }
    private static ItemStack craft(GameTestHelper h, Item... grid) {
        var list = new ArrayList<ItemStack>();
        for (Item i : grid) list.add(i == null ? ItemStack.EMPTY : new ItemStack(i));
        var input = CraftingInput.of(3, 3, list);
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow().value().assemble(input);
    }
    private static ItemStack upgrade(GameTestHelper h, ItemStack base, Item addition) {
        var input = new SmithingRecipeInput(new ItemStack(item("high_covalence_dust")), base, new ItemStack(addition));
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.SMITHING, input, h.getLevel()).orElseThrow().value().assemble(input);
    }
    private static void face(GameTestHelper h) { for (int x=2; x<=4; x++) for (int y=2; y<=4; y++) h.setBlock(new BlockPos(x,y,3), Blocks.STONE); }
    private static int remaining(GameTestHelper h) {
        int remaining=0; for (int x=2; x<=4; x++) for (int y=2; y<=4; y++) if (h.getBlockState(new BlockPos(x,y,3)).is(Blocks.STONE)) remaining++; return remaining;
    }
    private static void suit(ServerPlayer player, String tier) {
        var slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        var pieces = new String[]{"helmet", "chestplate", "leggings", "boots"};
        for (int i=0;i<4;i++) player.setItemSlot(slots[i], new ItemStack(item(tier + "_" + pieces[i])));
    }

    @GameTest(maxTicks=20) public void everyEquipmentEntryHasAReachableRecipe(GameTestHelper h) {
        h.assertTrue(ExtendedAlchemyEquipment.ITEMS.size()==32 && ExtendedAlchemyEquipment.BLOCKS.size()==5, "All 32 equipment/material entries registered");
        Item fuel=item("aeternalis_fuel"), dark=item("dark_matter"), red=item("red_matter"), stick=Items.STICK;
        h.assertTrue(craft(h,fuel,fuel,fuel,dark,dark,dark,fuel,fuel,fuel).is(red), "Red Matter route");
        h.assertTrue(craft(h,Items.IRON_INGOT,Items.IRON_INGOT,Items.IRON_INGOT,Items.IRON_INGOT,null,Items.IRON_INGOT,Items.IRON_INGOT,Items.IRON_INGOT,Items.IRON_INGOT).is(item("iron_band")), "Iron Band route");
        String[] kinds={"sword","shovel","hoe","shears"};
        Item[][] grids={{null,dark,null,null,dark,null,null,stick,null},{null,dark,null,null,stick,null,null,stick,null},{dark,dark,null,null,stick,null,null,stick,null},{null,dark,null,dark,null,null,null,null,null}};
        for (int i=0;i<kinds.length;i++) h.assertTrue(craft(h,grids[i]).is(item("dark_matter_"+kinds[i])), "Dark tool route: "+kinds[i]);
        for (String kind:List.of("pickaxe","axe","shovel","sword","hoe","shears","hammer")) h.assertTrue(upgrade(h,new ItemStack(item("dark_matter_"+kind)),red).is(item("red_matter_"+kind)), "Red tool route: "+kind);
        h.assertTrue(craft(h,item("red_matter_axe"),item("red_matter_sword"),item("red_matter_hoe"),item("red_matter_shears"),red,red,red,red,item("high_covalence_dust")).is(item("red_matter_katar")), "Katar combines four tools");
        h.assertTrue(craft(h,item("red_matter_pickaxe"),item("red_matter_shovel"),item("red_matter_hammer"),red,red,red,red,item("iron_band"),item("high_covalence_dust")).is(item("red_matter_morning_star")), "Morning Star combines three tools");
        String[] pieces={"helmet","chestplate","leggings","boots"};
        Item[][] armorGrids={{dark,dark,dark,dark,null,dark,null,null,null},{dark,null,dark,dark,dark,dark,dark,dark,dark},{dark,dark,dark,dark,null,dark,dark,null,dark},{dark,null,dark,dark,null,dark,null,null,null}};
        for (int i=0;i<pieces.length;i++) {
            var dm=craft(h,armorGrids[i]); h.assertTrue(dm.is(item("dark_matter_"+pieces[i])), "Dark armor route");
            var rm=upgrade(h,dm,red); h.assertTrue(rm.is(item("red_matter_"+pieces[i])), "Red armor route");
            h.assertTrue(upgrade(h,rm,Items.DIAMOND_BLOCK).is(item("gem_"+pieces[i])), "Gem armor route");
        }
        h.succeed();
    }

    @GameTest(maxTicks=20) public void storageRecipesAndFuelConserveExactly(GameTestHelper h) {
        for (String material:List.of("dark_matter","red_matter","alchemical_coal","mobius_fuel","aeternalis_fuel")) {
            Item m=item(material); var block=craft(h,m,m,m,m,m,m,m,m,m);
            h.assertTrue(block.is(item(material+"_block")), "Storage compression");
            var decompressed=craft(h,block.getItem(),null,null,null,null,null,null,null,null);
            h.assertTrue(decompressed.is(m)&&decompressed.getCount()==9, "Storage decompression conserves nine");
            var fuel= new ItemStack(m).get(DataComponents.COOKING_FUEL);
            if (fuel!=null) h.assertTrue(block.get(DataComponents.COOKING_FUEL).burnTime().get(null,0)==9*fuel.burnTime().get(null,0), "Fuel storage conserves burn ticks");
        } h.succeed();
    }

    @GameTest(maxTicks=20) public void smithingKeepsDamageNameAndSelectedMode(GameTestHelper h) {
        var source=new ItemStack(item("dark_matter_sword"));source.setDamageValue(2123);source.set(DarkMatter.MODE,1);
        source.set(DataComponents.CUSTOM_NAME,Component.literal("Silver Thorn"));
        var result=upgrade(h,source,item("red_matter"));
        h.assertTrue(result.getDamageValue()==2123 && result.getMaxDamage()==8192 && result.getOrDefault(DarkMatter.MODE,0)==1
                && result.getHoverName().getString().equals("Silver Thorn"), "Native smithing preserves durable equipment state");h.succeed();
    }

    @GameTest(maxTicks=20) public void morningStarPaysPerActualBreakAndRespectsClaims(GameTestHelper h) {
        var p=player(h,"red_matter_morning_star",1);face(h);
        try(var claims=TestClaims.open()) {
            claims.block(h.absolutePos(new BlockPos(2,2,3)),UUID.randomUUID());
            h.setBlock(new BlockPos(4,4,3),Blocks.FURNACE);
            h.assertTrue(p.gameMode.destroyBlock(h.absolutePos(new BlockPos(3,3,3))),"Starting block breaks normally");
            h.assertTrue(remaining(h)==1 && h.getBlockState(new BlockPos(4,4,3)).is(Blocks.FURNACE),"Only eligible face blocks break");
            h.assertTrue(Assaying.balance(p)==928 && p.getMainHandItem().getDamageValue()==7,"Six paid extras, seven durability");
        }h.succeed();
    }

    @GameTest(maxTicks=20) public void lowBalanceAndSneakKeepMiningPredictable(GameTestHelper h) {
        var p=player(h,"red_matter_hammer",1);face(h);Assaying.setBalance(p,15);
        p.gameMode.destroyBlock(h.absolutePos(new BlockPos(3,3,3)));
        h.assertTrue(remaining(h)==7 && Assaying.balance(p)==3,"One affordable extra, no overdraft");
        face(h);p.setShiftKeyDown(true);Assaying.setBalance(p,1000);p.gameMode.destroyBlock(h.absolutePos(new BlockPos(3,3,3)));
        h.assertTrue(remaining(h)==8 && Assaying.balance(p)==1000,"Sneaking is always precise");h.succeed();
    }

    @GameTest(maxTicks=20) public void hoeCultivatesOnlyPermittedTilesAndKeepsSeedBed(GameTestHelper h) {
        var p=player(h,"dark_matter_hoe",1);
        for(int x=2;x<=4;x++)for(int z=2;z<=4;z++)h.setBlock(new BlockPos(x,1,z),Blocks.DIRT);
        BlockPos centre=h.absolutePos(new BlockPos(3,1,3));
        try(var claims=TestClaims.open()) {
            claims.block(h.absolutePos(new BlockPos(2,1,2)),UUID.randomUUID());
            var context=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(centre).add(0,.5,0),Direction.UP,centre,false));
            h.assertTrue(p.getMainHandItem().getItem().useOn(context).consumesAction(),"Ordinary tilling works");
            int tilled=0;for(int x=2;x<=4;x++)for(int z=2;z<=4;z++)if(h.getBlockState(new BlockPos(x,1,z)).is(Blocks.FARMLAND))tilled++;
            h.assertTrue(tilled==8 && Assaying.balance(p)==944,"One normal and seven paid cultivated blocks, protected dirt remains");
        }h.succeed();
    }

    @GameTest(maxTicks=20) public void sheepShearingRetainsClaimedAnimalsAndSpendsOnlySuccessfulExtras(GameTestHelper h) {
        var p=player(h,"red_matter_shears",1);
        var first=h.spawn(EntityTypes.SHEEP,new BlockPos(3,2,3));first.setNoAi(true);
        var extra=h.spawn(EntityTypes.SHEEP,new BlockPos(4,2,3));extra.setNoAi(true);
        var protectedSheep=h.spawn(EntityTypes.SHEEP,new BlockPos(2,2,3));protectedSheep.setNoAi(true);
        try(var claims=TestClaims.open()) {
            claims.block(protectedSheep.blockPosition(),UUID.randomUUID());
            var stack=p.getMainHandItem(); stack.getItem().interactLivingEntity(stack,p,first,InteractionHand.MAIN_HAND);
            h.assertTrue(first.isSheared() && extra.isSheared() && !protectedSheep.isSheared(),"Real sheep loot and protected flock member");
            h.assertTrue(Assaying.balance(p)==992 && stack.getDamageValue()==2,"Only one extra sheep charged");
        }h.succeed();
    }

    @GameTest(maxTicks=20) public void sweepUsesHostileTargetsAndCannotSpendWithoutEssence(GameTestHelper h) {
        var p=player(h,"red_matter_sword",1);
        var original=h.spawn(EntityTypes.ZOMBIE,new BlockPos(3,2,3));original.setNoAi(true);
        var extra=h.spawn(EntityTypes.ZOMBIE,new BlockPos(4,2,3));extra.setNoAi(true);
        var pet=h.spawn(EntityTypes.SHEEP,new BlockPos(2,2,3));pet.setNoAi(true);float petHealth=pet.getHealth();
        var tool=(ExtendedMatterTool)p.getMainHandItem().getItem();float health=extra.getHealth();
        h.assertTrue(tool.sweep(p,original,p.getMainHandItem())==1 && extra.getHealth()<health && pet.getHealth()==petHealth && Assaying.balance(p)==984,"Only hostile target harmed and charged");
        Assaying.setBalance(p,0);extra.setInvulnerableTime(0);float after=extra.getHealth();
        h.assertTrue(tool.sweep(p,original,p.getMainHandItem())==0 && extra.getHealth()==after,"An empty ledger cannot cleave");h.succeed();
    }

    @GameTest(maxTicks=20) public void fullSuitWardsPayOnceAndPreserveStrongerEffects(GameTestHelper h) {
        var p=player(h,null,0);suit(p,"dark_matter");ExtendedAlchemyEquipment.pulseArmor(p);
        h.assertTrue(p.hasEffect(MobEffects.RESISTANCE)&&Assaying.balance(p)==992,"Complete suit pays eight for a short ward");
        ExtendedAlchemyEquipment.pulseArmor(p);h.assertTrue(Assaying.balance(p)==992,"Repeated calls do not double-charge a fresh ward");
        p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,600,2));ExtendedAlchemyEquipment.pulseArmor(p);
        h.assertTrue(p.getEffect(MobEffects.RESISTANCE).getAmplifier()==2 && Assaying.balance(p)==992,"Stronger ward untouched");
        p.removeEffect(MobEffects.RESISTANCE);p.getItemBySlot(EquipmentSlot.HEAD).remove(AlchemicalGear.ACTIVE);ExtendedAlchemyEquipment.pulseArmor(p);
        h.assertTrue(!p.hasEffect(MobEffects.RESISTANCE)&&Assaying.balance(p)==992,"One disabled piece stops set ward");h.succeed();
    }

    @GameTest(maxTicks=20) public void gemPiecesUseOnlyTheirOwnSituationalPerks(GameTestHelper h) {
        var p=player(h,null,0);p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(item("gem_leggings")));
        ExtendedAlchemyEquipment.pulseArmor(p);h.assertTrue(Assaying.balance(p)==1000,"Idle legs do not drain essence");
        p.setSprinting(true);ExtendedAlchemyEquipment.pulseArmor(p);
        h.assertTrue(p.hasEffect(MobEffects.SPEED)&&Assaying.balance(p)==996&&!p.hasEffect(MobEffects.RESISTANCE),"Single leggings give sprint ward, not full-set resistance");
        p.setSprinting(false);p.setItemSlot(EquipmentSlot.FEET,new ItemStack(item("gem_boots")));p.fallDistance=3;
        ExtendedAlchemyEquipment.pulseArmor(p);h.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING)&&Assaying.balance(p)==992,"Falling boots spend four");h.succeed();
    }

    @GameTest(maxTicks=20) public void gemBootsCatchFallsBetweenOrdinaryArmorPulses(GameTestHelper h) {
        var p=player(h,null,0);suit(p,"gem");p.tickCount=19;p.fallDistance=3;
        ExtendedAlchemyEquipment.tickArmor(p);
        h.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING)&&Assaying.balance(p)==996&&!p.hasEffect(MobEffects.RESISTANCE),"Boots activate on the actual per-tick entry point before the next full armor pulse");
        p.tickCount=20;ExtendedAlchemyEquipment.tickArmor(p);
        h.assertTrue(p.hasEffect(MobEffects.RESISTANCE)&&Assaying.balance(p)==984,"Full set renews on its scheduled pulse without recharging the fresh boots ward");
        p.tickCount=21;ExtendedAlchemyEquipment.tickArmor(p);
        h.assertTrue(Assaying.balance(p)==984,"Per-tick detection does not become per-tick essence consumption");h.succeed();
    }

    @GameTest(maxTicks=20) public void defaultEquipmentIsValuedAndModernArmorHasWornAssets(GameTestHelper h) {
        var p=player(h,null,0);
        for(var entry:ExtendedAlchemyEquipment.ITEMS.entrySet()) {
            var stack=new ItemStack(entry.getValue());h.assertTrue(Assaying.eligibility(stack).isEmpty(),"Default item is valued: "+entry.getKey());
            if(entry.getKey().endsWith("helmet")||entry.getKey().endsWith("chestplate")||entry.getKey().endsWith("leggings")||entry.getKey().endsWith("boots"))
                h.assertTrue(stack.has(DataComponents.EQUIPPABLE)&&stack.has(DataComponents.MAX_DAMAGE),"Real worn durable equipment: "+entry.getKey());
        }h.succeed();
    }
}

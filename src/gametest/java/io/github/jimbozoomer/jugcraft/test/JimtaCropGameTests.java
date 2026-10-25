package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialCrops;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

public final class JimtaCropGameTests {
    private static IndustrialCrops.Genes crop(GameTestHelper h,BlockPos p,String name,int age){h.setBlock(p.below(),Blocks.FARMLAND);h.setBlock(p,IndustrialCrops.CROPS.get(name).getStateForAge(age));return h.getBlockEntity(p, IndustrialCrops.Genes.class);}
    @GameTest public void metalPlantsRequireTheirActualNearbyOreAnchor(GameTestHelper h){
        var p=new BlockPos(2,4,2);crop(h,p,"ferru",6);var plant=IndustrialCrops.CROPS.get("ferru");
        h.assertTrue(!plant.canRipen(h.getLevel(),h.absolutePos(p)),"Metal crop cannot mature on plain soil");h.setBlock(p.below(2),Blocks.IRON_BLOCK);
        h.assertTrue(plant.canRipen(h.getLevel(),h.absolutePos(p)),"Physical iron block in root range permits growth");h.setBlock(p.below(2),Blocks.GOLD_BLOCK);
        h.assertTrue(!plant.canRipen(h.getLevel(),h.absolutePos(p)),"Different metal is not an iron anchor");h.succeed();
    }
    @GameTest public void crossbreedingChecksClaimsAndPreservesOwnerAndBoundedGenes(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var center=new BlockPos(3,2,3);var dest=crop(h,center,"cross_sticks",0);dest.owner=player.getUUID();
            var a=crop(h,center.north(),"reed",7);a.growth=31;a.gain=31;a.resistance=31;
            var b=crop(h,center.south(),"wheat",7);b.growth=31;b.gain=31;b.resistance=31;
            var absolute=h.absolutePos(center);
            try(var claims=TestClaims.open().block(h.absolutePos(center.north()),UUID.randomUUID())){
                h.assertTrue(!IndustrialCrops.crossbreed(h.getLevel(),absolute,dest,RandomSource.create(1)),"Protected parent's genes cannot be used across claim boundary");}
            h.assertTrue(IndustrialCrops.crossbreed(h.getLevel(),absolute,dest,RandomSource.create(1)),"Authorized crossbreeding creates hops from wheat and reed");
            h.assertTrue(h.getBlockState(center).is(IndustrialCrops.CROPS.get("hops")),"Explicit reachable breeding outcome");var child=h.getBlockEntity(center, IndustrialCrops.Genes.class);
            h.assertTrue(child.growth<=31&&child.gain<=31&&child.resistance<=31&&player.getUUID().equals(child.owner),"Genes remain bounded and retain plot owner");
            var seed=IndustrialCrops.seed(IndustrialCrops.CROPS.get("hops"),child);var data=seed.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            h.assertTrue(data.getIntOr("Growth",-1)==child.growth&&data.getIntOr("Gain",-1)==child.gain,"Recovered seed bag carries real inherited genes");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void drinksReturnOneMugAndTerraWartCuresOnlyBadEffects(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var drink=new ItemStack(IndustrialCrops.ITEMS.get("jimta_coffee_mug"));var left=drink.finishUsingItem(h.getLevel(),player);
            h.assertTrue(left.is(IndustrialCrops.ITEMS.get("jimta_empty_mug"))&&left.getCount()==1,"One consumed drink returns exactly one reusable mug");
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,200));player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,200));
            var wart=new ItemStack(IndustrialCrops.ITEMS.get("jimta_terra_wart"));player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wart);wart.getItem().use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.POISON)&&player.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)&&wart.isEmpty(),"Terra wart spends one item, cures poison and keeps unrelated beneficial effect");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
}

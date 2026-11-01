package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.test.TestPlayers;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public final class NewestLumiseneGameTests {
    @GameTest(structure="jugcraft-test:claim_combat_arena") public void fuelCannotCrossClaimBoundary(GameTestHelper h){
        var l=h.getLevel();var anchor=h.absolutePos(new BlockPos(16,2,16));
        var p=new BlockPos(Math.floorDiv(anchor.getX(),16)*16+15,anchor.getY(),anchor.getZ());var q=p.east();
        var owner=h.makeMockServerPlayerInLevel();var stranger=TestPlayers.survivor(h);
        var registry=io.github.jimbozoomer.jugcraft.finance.LandRegistry.get(l.getServer());
        var book=io.github.jimbozoomer.jugcraft.finance.Ledger.get(l.getServer()).book();
        long key=io.github.jimbozoomer.jugcraft.finance.LandRegistry.keyOf(q);
        registry.assign(book,key,book.account(owner.getUUID(),"LumiseneClaim"),0);
        try{
            l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
            h.assertTrue(!OwnerLumisene.place(stranger,q,4),"Foreign claim rejects placement");
            for(var d:new net.minecraft.core.Direction[]{net.minecraft.core.Direction.DOWN,net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.SOUTH,net.minecraft.core.Direction.WEST})l.setBlockAndUpdate(p.relative(d),Blocks.STONE.defaultBlockState());
            OwnerLumisene.set(l,p,16,0);OwnerLumisene.spread(l,p);
            h.assertTrue(l.getBlockState(q).isAir()&&l.getBlockState(p).getValue(OwnerLumisene.LAYERS)==16,"Unattended fuel cannot flow across ownership boundary");
        }finally{registry.abandon(owner,book,key,true,0);owner.discard();}
        h.succeed();
    }
    @GameTest public void finiteFuelNeverCreatesSourceVolume(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,2,2));
        l.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());
        OwnerLumisene.set(l,p,16,0);OwnerLumisene.spread(l,p);
        int total=0;
        for(var at:BlockPos.betweenClosed(p.offset(-1,0,-1),p.offset(1,0,1))){
            var state=l.getBlockState(at);if(state.is(OwnerLumisene.BLOCK))total+=state.getValue(OwnerLumisene.LAYERS);
        }
        h.assertTrue(total==16,"Horizontal flow preserves one bucket's sixteen layers");h.succeed();
    }
    @GameTest public void failedPlacementPreservesSolidBlockAndStoredFuel(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,2,2));var player=TestPlayers.survivor(h);
        l.setBlockAndUpdate(p,Blocks.DIAMOND_BLOCK.defaultBlockState());
        h.assertTrue(!OwnerLumisene.place(player,p,4)&&l.getBlockState(p).is(Blocks.DIAMOND_BLOCK),"Cannot replace a build with fuel");
        OwnerLumisene.set(l,p,16,0);
        h.assertTrue(!OwnerLumisene.place(player,p,4)&&l.getBlockState(p).getValue(OwnerLumisene.LAYERS)==16,"Overfill rejected without change");h.succeed();
    }
    @GameTest public void burningConsumesOnlyFiniteFuel(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,2,2));
        l.setBlockAndUpdate(p.east(),Blocks.OAK_PLANKS.defaultBlockState());OwnerLumisene.set(l,p,1,15);
        OwnerLumisene.spread(l,p);
        h.assertTrue(l.getBlockState(p).isAir()&&l.getBlockState(p.east()).is(Blocks.OAK_PLANKS),"Last layer burns away without wildfire");h.succeed();
    }
    @GameTest public void drinkingReturnsBottleAndAppliesOriginalEffects(GameTestHelper h){
        var player=TestPlayers.survivor(h);player.addEffect(new MobEffectInstance(MobEffects.POISON,200));
        var result=OwnerLumisene.BOTTLE.finishUsingItem(new ItemStack(OwnerLumisene.BOTTLE),h.getLevel(),player);
        h.assertTrue(result.is(Items.GLASS_BOTTLE)&&!player.hasEffect(MobEffects.POISON),"Fuel consumed and poison removed");
        h.assertTrue(player.hasEffect(MobEffects.GLOWING)&&player.hasEffect(OwnerLumisene.FLAMMABLE),"Both supplied effects applied");h.succeed();
    }
}

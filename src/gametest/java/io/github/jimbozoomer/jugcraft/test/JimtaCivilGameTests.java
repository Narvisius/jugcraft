package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.CivilWorkshops;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

public final class JimtaCivilGameTests {
    private CivilWorkshops.Node node(GameTestHelper h,String kind){var p=new BlockPos(2,2,2);h.setBlock(p,CivilWorkshops.BLOCKS.get(kind));return h.getBlockEntity(p, CivilWorkshops.Node.class);}
    @GameTest public void quarryChecksClaimAndOutputCapacityBeforeConsumingPowerOrTool(GameTestHelper h){var p=JimtaRobotGameTests.player(h);try{
        var n=node(h,"quarry");n.owner=p.getUUID();var at=new BlockPos(3,2,2);h.setBlock(at,Blocks.STONE);n.setArea(h.absolutePos(at),h.absolutePos(at));n.running=true;n.energy.setAmount(1000);n.setItem(0,new ItemStack(Items.IRON_PICKAXE));
        try(var claim=TestClaims.open().block(h.absolutePos(at),UUID.randomUUID())){n.step(h.getLevel());h.assertTrue(h.getBlockState(at).is(Blocks.STONE)&&n.energy.getAmount()==1000&&n.cursor==0,"Protected work waits without advancing or consuming");}
        for(int i=14;i<27;i++)n.setItem(i,new ItemStack(Items.DIRT,64));n.step(h.getLevel());h.assertTrue(h.getBlockState(at).is(Blocks.STONE)&&n.getItem(0).getDamageValue()==0&&n.energy.getAmount()==1000,"Blocked output cannot destroy the block or spend power/tool durability");
        n.setItem(14,ItemStack.EMPTY);n.step(h.getLevel());h.assertTrue(h.getBlockState(at).isAir()&&n.getItem(14).is(Items.COBBLESTONE)&&n.energy.getAmount()==744&&n.getItem(0).getDamageValue()==1,"Successful mine transfers one real drop and charges once");
    }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();}
    @GameTest public void replacingAndFillingConsumeRealMaterials(GameTestHelper h){var p=JimtaRobotGameTests.player(h);try{
        var n=node(h,"replacer");n.owner=p.getUUID();var at=new BlockPos(3,2,2);h.setBlock(at,Blocks.STONE);n.setArea(h.absolutePos(at),h.absolutePos(at));n.running=true;n.energy.setAmount(512);n.setItem(0,new ItemStack(Items.IRON_PICKAXE));n.setItem(1,new ItemStack(Items.OAK_PLANKS,2));n.step(h.getLevel());
        h.assertTrue(h.getBlockState(at).is(Blocks.OAK_PLANKS)&&n.getItem(1).getCount()==1&&n.getItem(14).is(Items.COBBLESTONE)&&n.energy.getAmount()==256,"Replacement conserves old block drops, consumes one material and power");
        n.cursor=0;n.step(h.getLevel());h.assertTrue(n.getItem(1).getCount()==1&&n.energy.getAmount()==256,"Identical desired block is skipped without charging");h.assertTrue(!n.setArea(h.absolutePos(at),h.absolutePos(at.offset(32,0,0))),"32-block maximum rejects oversized work volumes");
    }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();}
    @GameTest public void floodGateRequiresAnEnclosedReservoirAndFiniteFluid(GameTestHelper h){var p=JimtaRobotGameTests.player(h);try{
        var n=node(h,"flood_gate");n.owner=p.getUUID();var at=new BlockPos(4,2,4);h.setBlock(at,Blocks.AIR);n.setArea(h.absolutePos(at),h.absolutePos(at));n.running=true;n.energy.setAmount(512);n.fluid.fill(Fluids.WATER,1000);n.step(h.getLevel());
        h.assertTrue(h.getBlockState(at).isAir()&&n.fluid.millibuckets()==1000&&n.energy.getAmount()==512,"An open edge prevents unselected land from being flooded");
        for(var d:net.minecraft.core.Direction.values())if(d!=net.minecraft.core.Direction.UP)h.setBlock(at.relative(d),Blocks.STONE);n.step(h.getLevel());
        h.assertTrue(h.getBlockState(at).is(Blocks.WATER)&&n.fluid.millibuckets()==0&&n.energy.getAmount()==256,"One placed source consumes a bucket of water and one work charge");
    }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();}
}

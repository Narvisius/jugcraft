package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;

public final class JimtaMatterGameTests {
    private MatterWorkshops.Node node(GameTestHelper h,String kind){var pos=new BlockPos(2,2,2);h.setBlock(pos,MatterWorkshops.BLOCKS.get(kind));return h.getBlockEntity(pos, MatterWorkshops.Node.class);}
    @GameTest public void scannerConsumesOneRealSampleAndBlankMemory(GameTestHelper h){
        var n=node(h,"matter_scanner");n.setItem(0,new ItemStack(Items.COBBLESTONE,2));n.setItem(1,new ItemStack(MatterWorkshops.MEMORY));n.energy.setAmount(10000);
        for(int i=0;i<10;i++)n.step(h.getLevel());
        h.assertTrue(n.getItem(0).getCount()==1&&n.getItem(1).isEmpty()&&n.energy.getAmount()==0,"One scan consumes exactly one sample, one blank memory and 10,000 JE");
        h.assertTrue(MatterWorkshops.remembered(n.getItem(4)).is(Items.COBBLESTONE),"Real encoded memory identifies the consumed sample");h.succeed();
    }
    @GameTest public void replicationRejectsValuablesAndWaitsForOutputSpace(GameTestHelper h){
        var n=node(h,"matter_replicator");n.tank.fill(MatterWorkshops.MATTER.source(),1000);n.energy.setAmount(12000);n.setItem(0,MatterWorkshops.memory(new ItemStack(Items.DIAMOND)));n.step(h.getLevel());
        h.assertTrue(n.getItem(4).isEmpty()&&n.tank.millibuckets()==1000&&n.energy.getAmount()==12000,"Encoded diamonds do not bypass server economy restriction");
        n.setItem(0,MatterWorkshops.memory(new ItemStack(Items.COBBLESTONE)));n.setItem(4,new ItemStack(Items.COBBLESTONE,64));n.step(h.getLevel());
        h.assertTrue(n.tank.millibuckets()==1000&&n.energy.getAmount()==12000,"Blocked output preserves matter and energy");
        n.setItem(4,ItemStack.EMPTY);int cost=MatterWorkshops.cost(new ItemStack(Items.COBBLESTONE));n.step(h.getLevel());
        h.assertTrue(n.getItem(4).is(Items.COBBLESTONE)&&n.getItem(4).getCount()==1&&n.tank.millibuckets()==1000-cost&&n.energy.getAmount()==8000,"One permitted result debits exact matter price and JE once");h.succeed();
    }
    @GameTest public void fabricatorRequiresRealEnergyAndConservesAmplifierRemainder(GameTestHelper h){
        var n=node(h,"mass_fabricator");n.setItem(0,new ItemStack(IndustrialProduction.ITEMS.get("jimta_scrap"),2));n.step(h.getLevel());
        h.assertTrue(n.getItem(0).getCount()==2&&n.progress==0,"No power does not consume amplifier");n.energy.setAmount(4000);n.step(h.getLevel());
        h.assertTrue(n.energy.getAmount()==0&&n.progress==24000&&n.amplification==2000&&n.getItem(0).getCount()==1,"One scrap gives finite amplification credit; 4,000 real JE produces 24,000 progress");
        n.tank.fill(MatterWorkshops.MATTER.source(),16000);n.energy.setAmount(4000);n.step(h.getLevel());h.assertTrue(n.energy.getAmount()==4000&&n.progress==24000,"Full matter tank stops energy debit");h.succeed();
    }
    @GameTest public void canningPreservesFoodAndReturnsItsVessel(GameTestHelper h){
        var n=node(h,"solid_canner");var soup=new ItemStack(Items.MUSHROOM_STEW);n.setItem(0,soup.copy());n.setItem(1,new ItemStack(IndustrialProduction.ITEMS.get("jimta_tin_can"),2));n.energy.setAmount(800);n.setItem(5,new ItemStack(Items.STONE,64));n.step(h.getLevel());
        h.assertTrue(n.getItem(0).is(Items.MUSHROOM_STEW)&&n.energy.getAmount()==800,"Full remainder slot prevents consumption of soup, can and JE");
        n.setItem(5,ItemStack.EMPTY);n.step(h.getLevel());
        h.assertTrue(n.getItem(0).isEmpty()&&n.getItem(1).getCount()==1&&n.getItem(5).is(Items.BOWL)&&n.energy.getAmount()==400,"One recipe returns the existing bowl, uses one empty can and pays 400 JE");
        h.assertTrue(soup.get(net.minecraft.core.component.DataComponents.FOOD).equals(n.getItem(4).get(net.minecraft.core.component.DataComponents.FOOD)),"Canning retains exact nutrition and saturation");h.succeed();
    }
    @GameTest public void bottlerRollsBackTankWhenOutputIsBlocked(GameTestHelper h){
        var n=node(h,"fluid_bottler");n.tank.fill(net.minecraft.world.level.material.Fluids.WATER,1000);n.energy.setAmount(800);n.setItem(0,new ItemStack(Items.BUCKET));n.setItem(4,new ItemStack(Items.STONE,64));n.step(h.getLevel());
        h.assertTrue(n.tank.millibuckets()==1000&&n.getItem(0).is(Items.BUCKET)&&n.energy.getAmount()==800,"Blocked output rolls back both fluid storages before any item or JE consumption");
        n.setItem(4,ItemStack.EMPTY);n.step(h.getLevel());
        h.assertTrue(n.tank.millibuckets()==0&&n.getItem(4).is(Items.WATER_BUCKET)&&n.getItem(0).isEmpty()&&n.energy.getAmount()==400,"Fill operation conserves exactly one bucket of water");
        n.setItem(0,n.removeItem(4,1));n.mode=1;n.step(h.getLevel());
        h.assertTrue(n.tank.millibuckets()==1000&&n.getItem(4).is(Items.BUCKET)&&n.energy.getAmount()==0,"Empty mode returns the bucket and water, charging separately");h.succeed();
    }
}

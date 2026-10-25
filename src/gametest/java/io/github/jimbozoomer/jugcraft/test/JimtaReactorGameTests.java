package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.CompactReactor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

public final class JimtaReactorGameTests {
    private CompactReactor.Core reactor(GameTestHelper h){h.setBlock(new BlockPos(2,2,2),CompactReactor.CORE);return h.getBlockEntity(new BlockPos(2,2,2), CompactReactor.Core.class);}
    private ItemStack part(String name){return new ItemStack(CompactReactor.PARTS.get("jimta_"+name));}
    @GameTest public void fuelProducesFiniteEnergyAndBecomesSpentExactlyOnce(GameTestHelper h){
        var r=reactor(h);var rod=part("uranium_rod_simple");CustomData.update(DataComponents.CUSTOM_DATA,rod,t->t.putInt("Used",19999));r.setItem(0,rod);r.enabled=true;r.step(h.getLevel());
        h.assertTrue(r.getItem(0).is(CompactReactor.PARTS.get("jimta_uranium_rod_simple_depleted")),"Last fuel second replaces rod with exactly one spent rod");
        long energy=r.energy.getAmount();h.assertTrue(energy==100&&r.heat==4,"One isolated fuel cell generates 5 JE/t and four heat per reactor second");r.step(h.getLevel());
        h.assertTrue(r.energy.getAmount()==energy&&r.getItem(0).getCount()==1,"Spent rod cannot make more energy or duplicate itself");h.succeed();
    }
    @GameTest public void coolingContinuesStoppedAndChamberRemovalFailsClosed(GameTestHelper h){
        var r=reactor(h);r.heat=100;r.setItem(0,part("reactor_heat_vent"));r.step(h.getLevel());h.assertTrue(r.heat==95,"Core vent removes five hull heat while stopped");
        h.setBlock(new BlockPos(3,2,2),CompactReactor.CHAMBER);r.step(h.getLevel());h.assertTrue(r.columns==4,"Adjacent chamber unlocks one physical grid column");
        r.setItem(3,part("uranium_rod_simple"));r.enabled=true;h.setBlock(new BlockPos(3,2,2),Blocks.AIR);r.step(h.getLevel());
        h.assertTrue(r.columns==3&&!r.enabled&&r.fault&&CompactReactor.stored(r.getItem(3),"Used")==0,"Removal shuts down, preserving unsupported-column fuel for recovery");h.succeed();
    }
    @GameTest public void redstoneShutdownAndFullBufferNeverConsumeFuel(GameTestHelper h){
        var r=reactor(h);r.setItem(0,part("uranium_rod_simple"));r.enabled=true;h.setBlock(new BlockPos(2,2,3),Blocks.REDSTONE_BLOCK);r.step(h.getLevel());
        h.assertTrue(r.energy.getAmount()==0&&CompactReactor.stored(r.getItem(0),"Used")==0,"Redstone is an actual fuel pause");
        h.setBlock(new BlockPos(2,2,3),Blocks.AIR);r.energy.setAmount(r.energy.getCapacity());r.step(h.getLevel());
        h.assertTrue(CompactReactor.stored(r.getItem(0),"Used")==0,"Full JE buffer conserves unused fuel");h.succeed();
    }
}

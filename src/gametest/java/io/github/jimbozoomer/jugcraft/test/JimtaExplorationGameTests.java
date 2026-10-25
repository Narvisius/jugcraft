package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.network.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Authoring only; run at the combined implementation checkpoint. */
public final class JimtaExplorationGameTests {
    @GameTest public void waterReturnsExactlyOneUnprimedTinyTnt(GameTestHelper h) {
        h.setBlock(2,2,2,Blocks.WATER); var p=h.absolutePos(new BlockPos(2,2,2)); var tnt=DigitalTinyTnt.spawn(h.getLevel(),p,null,1);tnt.tick();
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,tnt.getBoundingBox().inflate(2),e->e.getItem().is(DigitalTinyTnt.BLOCK.asItem()));
        h.assertTrue(tnt.isRemoved()&&drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"Water defuses once, even on last fuse tick");h.succeed();
    }
    @GameTest public void tinyTntCannotBreakAnotherPlayersClaim(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        h.setBlock(3,2,2,Blocks.DIRT);var block=h.absolutePos(new BlockPos(3,2,2));
        try(var claim=TestClaims.open().block(block,java.util.UUID.randomUUID())) {
            var charge=DigitalTinyTnt.spawn(h.getLevel(),h.absolutePos(new BlockPos(2,2,2)),p.getUUID(),1);charge.tick();
            h.assertTrue(h.getLevel().getBlockState(block).is(Blocks.DIRT),"Explosion checks actual actor at each affected block");
        }h.succeed();
    }
    @GameTest public void compassFindsAndForgetsRealRewardCore(GameTestHelper h) {
        h.setBlock(3,2,3,DigitalMeteorites.CUBE);var pos=h.absolutePos(new BlockPos(3,2,3));var index=DigitalMeteorites.Index.get(h.getLevel());
        index.remember(pos);h.assertTrue(pos.equals(index.nearest(h.getLevel(),pos)),"Registered original core is navigable");
        h.setBlock(3,2,3,Blocks.AIR);h.assertTrue(!pos.equals(index.nearest(h.getLevel(),pos)),"A harvested core stops attracting compasses");h.succeed();
    }
}

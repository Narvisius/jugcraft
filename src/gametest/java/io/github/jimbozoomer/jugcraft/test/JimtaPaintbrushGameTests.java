package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.*;

/** Authored for the final combined batch; not executed during the implementation phase. */
public final class JimtaPaintbrushGameTests {
    @GameTest public void paintLoadsSpendOnlyOnChangesAndCleanWithoutRefund(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var brush=new ItemStack(FactoryPaintbrush.BRUSH);player.setItemInHand(InteractionHand.MAIN_HAND,brush);player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.DYE.red(),2));
            FactoryPaintbrush.BRUSH.use(h.getLevel(),player,InteractionHand.MAIN_HAND);
            h.assertTrue(FactoryPaintbrush.uses(brush)==64&&player.getOffhandItem().getCount()==1,"One dye loads 64 applications");
            FactoryPaintbrush.BRUSH.use(h.getLevel(),player,InteractionHand.MAIN_HAND);
            h.assertTrue(player.getOffhandItem().getCount()==1,"Refilling an unchanged full brush wastes no dye");
            var pos=new BlockPos(2,2,2);h.setBlock(pos,IndustrialTransport.BLOCKS.get("jimta_pipe_gold_item"));var at=h.absolutePos(pos);
            var use=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false));
            FactoryPaintbrush.BRUSH.useOn(use);FactoryPaintbrush.BRUSH.useOn(use);
            h.assertTrue(TransportAttachments.at(h.getLevel(),at).paint==14&&FactoryPaintbrush.uses(brush)==63,"Repainting an unchanged pipe costs nothing");
            player.setShiftKeyDown(true);FactoryPaintbrush.BRUSH.use(h.getLevel(),player,InteractionHand.MAIN_HAND);player.setShiftKeyDown(false);FactoryPaintbrush.BRUSH.useOn(use);
            h.assertTrue(TransportAttachments.at(h.getLevel(),at).paint==-1&&FactoryPaintbrush.uses(brush)==0&&player.getOffhandItem().getCount()==1,"Cleaning clears paint without generating dye");
            FactoryPaintbrush.set(brush,11,1);h.setBlock(pos,Blocks.WOOL.white());FactoryPaintbrush.BRUSH.useOn(use);
            h.assertTrue(h.getBlockState(pos).is(Blocks.WOOL.blue())&&FactoryPaintbrush.color(brush)==-1,"Last application paints the block and returns to the clean sprite");
            FactoryPaintbrush.set(brush,14,64);
            try(var denied=TestClaims.open().block(at,java.util.UUID.randomUUID())){
                FactoryPaintbrush.BRUSH.useOn(use);
                h.assertTrue(h.getBlockState(pos).is(Blocks.WOOL.blue())&&FactoryPaintbrush.uses(brush)==64,"Denied paint leaves the block and all uses unchanged");
            }
            h.succeed();
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}
    }
    @GameTest public void coloredEdgesRevokeCachedPortsAndPersistWithoutChangingCargo(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,IndustrialTransport.BLOCKS.get("jimta_pipe_diamond_wood_item"));h.setBlock(pos.east(),IndustrialTransport.BLOCKS.get("jimta_pipe_gold_item"));
        var node=h.getBlockEntity(pos,IndustrialTransportDevices.Node.class);node.setItem(9,new ItemStack(Items.IRON_INGOT,7));
        var first=node.attachments();var second=TransportAttachments.at(h.getLevel(),h.absolutePos(pos.east()));first.paint=14;second.paint=14;first.changed();second.changed();
        var cached=ItemStorage.SIDED.find(h.getLevel(),node.getBlockPos(),Direction.EAST);
        h.assertTrue(TransportAttachments.openEdge(h.getLevel(),node.getBlockPos(),Direction.EAST),"Same colors connect");
        second.paint=11;second.changed();
        h.assertTrue(!TransportAttachments.openEdge(h.getLevel(),node.getBlockPos(),Direction.EAST)&&!TransportAttachments.openEnergyEdge(h.getLevel(),node.getBlockPos(),Direction.EAST),"Different painted colors close all flow types");
        try(var tx=Transaction.openOuter()){h.assertTrue(cached.extract(ItemVariant.of(Items.IRON_INGOT),7,tx)==0,"Previously cached port rechecks new paint boundary");tx.commit();}
        h.assertTrue(node.getItem(9).getCount()==7,"Blocked transfer leaves the actual cargo intact");
        var saved=node.saveWithoutMetadata(h.getLevel().registryAccess());first.paint=-1;node.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(first.paint==14,"Pipe paint survives save/load");
        second.paint=-1;second.changed();try(var tx=Transaction.openOuter()){h.assertTrue(cached.extract(ItemVariant.of(Items.IRON_INGOT),7,tx)==7,"Cleaning a junction reopens it to any color");}
        h.assertTrue(node.getItem(9).getCount()==7,"Aborted extraction remains conserved");h.succeed();
    }
}

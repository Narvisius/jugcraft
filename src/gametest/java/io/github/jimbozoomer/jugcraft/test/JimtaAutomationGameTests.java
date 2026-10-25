package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialAutomation;
import io.github.jimbozoomer.jugcraft.control.LogicControllerBlockEntity;
import io.github.jimbozoomer.jugcraft.logistics.*;
import java.util.Arrays;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public final class JimtaAutomationGameTests {
    @GameTest public void modifierExtraConditionsFailClosedAndExtraActionsFollowTheRow(GameTestHelper h){
        var state=IndustrialAutomation.BLOCKS.get("jimta_gate_gold_and").defaultBlockState().setValue(IndustrialAutomation.Gate.MODIFIER,3);
        var gate=new IndustrialAutomation.GateEntity(BlockPos.ZERO,state);
        h.assertTrue(gate.ruleRows()==4&&!gate.click(4,0),"Diamond modifier halves eight rows and rejects hidden row edits");
        gate.setRule(0,true,DyeColor.RED,true,50,DyeColor.WHITE,true);
        gate.parameter(0,0,1);gate.parameter(0,1,DyeColor.BLUE.getId());gate.parameter(0,2,1);gate.parameter(0,3,25);
        gate.parameter(0,12,1);gate.parameter(0,13,DyeColor.GREEN.getId());gate.parameter(0,14,1);
        int[] readings=new int[16];Arrays.fill(readings,-1);readings[DyeColor.RED.getId()]=80;
        int outputs=(1<<DyeColor.WHITE.getId())|(1<<DyeColor.GREEN.getId());
        h.assertTrue((gate.evaluateRules(outputs,readings).states()&outputs)==0,"Unavailable extra condition turns off primary and extra outputs");
        readings[DyeColor.BLUE.getId()]=40;
        h.assertTrue((gate.evaluateRules(0,readings).states()&outputs)==outputs,"Both real matching readings activate both row outputs");h.succeed();
    }
    @GameTest public void copierRejectsMalformedDataAndPreservesTargetOwner(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var copier=io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialGateCopier.ITEM;
            h.assertTrue(!io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialGateCopier.valid(new int[]{1,16,0,50,0,1}),"Invalid channel is rejected");
            var a=new BlockPos(2,2,2);var b=new BlockPos(3,2,2);
            h.setBlock(a,IndustrialAutomation.BLOCKS.get("jimta_gate_gold_and"));h.setBlock(b,IndustrialAutomation.BLOCKS.get("jimta_gate_gold_and"));
            var source=h.getBlockEntity(a, IndustrialAutomation.GateEntity.class);var target=h.getBlockEntity(b, IndustrialAutomation.GateEntity.class);
            source.owner(p.getUUID());target.owner(java.util.UUID.randomUUID());source.setRule(0,true,DyeColor.BLUE,true,65,DyeColor.RED,false);
            var oldOwner=target.saveWithoutMetadata(h.getLevel().registryAccess()).get("GateOwner");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(copier));
            java.util.function.Function<BlockPos,net.minecraft.world.item.context.UseOnContext> context=pos->new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos)),Direction.UP,h.absolutePos(pos),false));
            copier.useOn(context.apply(a));copier.useOn(context.apply(b));
            h.assertTrue(target.rule(0,LogicControllerBlockEntity.THRESHOLD)==65&&target.rule(0,LogicControllerBlockEntity.SENSOR)==DyeColor.BLUE.ordinal(),"Copier applies bounded real rules");
            h.assertTrue(oldOwner.equals(target.saveWithoutMetadata(h.getLevel().registryAccess()).get("GateOwner")),"Target retains its own owner");
            h.setBlock(b,IndustrialAutomation.BLOCKS.get("jimta_gate_clay_brick_and"));var small=h.getBlockEntity(b, IndustrialAutomation.GateEntity.class);
            copier.useOn(context.apply(b));h.assertTrue(small.rule(0,LogicControllerBlockEntity.ENABLED)==0,"Eight saved rows cannot be silently truncated onto one-row gate");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    @GameTest public void andOrCombineConditionsAndAbsentSensorsFailClosed(GameTestHelper h){
        var and=new IndustrialAutomation.GateEntity(BlockPos.ZERO,IndustrialAutomation.BLOCKS.get("jimta_gate_gold_and").defaultBlockState());
        var or=new IndustrialAutomation.GateEntity(BlockPos.ZERO,IndustrialAutomation.BLOCKS.get("jimta_gate_gold_or").defaultBlockState());
        for(var gate:new IndustrialAutomation.GateEntity[]{and,or}){gate.setRule(0,true,DyeColor.RED,true,50,DyeColor.WHITE,true);gate.setRule(1,true,DyeColor.BLUE,false,30,DyeColor.WHITE,true);}
        int[] readings=new int[16];Arrays.fill(readings,-1);readings[DyeColor.RED.ordinal()]=80;readings[DyeColor.BLUE.ordinal()]=40;
        int bit=1<<DyeColor.WHITE.ordinal();h.assertTrue((and.evaluateRules(0,readings).states()&bit)==0,"AND needs both conditions");h.assertTrue((or.evaluateRules(0,readings).states()&bit)!=0,"OR needs one condition");
        readings[DyeColor.BLUE.ordinal()]=10;h.assertTrue((and.evaluateRules(0,readings).states()&bit)!=0,"Both matching sensors enable AND");
        Arrays.fill(readings,-1);and.setRule(1,true,DyeColor.BLUE,false,30,DyeColor.WHITE,false);
        h.assertTrue((and.evaluateRules(bit,readings).states()&bit)==0,"Absent sensors fail closed even for an inverted row");h.succeed();
    }
    @GameTest public void lowTierGateRefusesHiddenRuleEdits(GameTestHelper h){
        var gate=new IndustrialAutomation.GateEntity(BlockPos.ZERO,IndustrialAutomation.BLOCKS.get("jimta_gate_clay_brick_and").defaultBlockState());
        h.assertTrue(!gate.click(1,0)&&gate.click(0,0),"One-row gate rejects packet edits to hidden higher rows");h.succeed();
    }
    @GameTest public void sourceFilterUsesSamplesAndAtomicNativeTransfer(GameTestHelper h){
        h.setBlock(new BlockPos(2,2,2),IndustrialAutomation.BLOCKS.get("jimta_item_filter").defaultBlockState().setValue(ItemSorterBlock.FACING,Direction.EAST));h.setBlock(new BlockPos(3,2,2),Blocks.CHEST);
        var filter=h.getBlockEntity(new BlockPos(2,2,2), ItemSorterBlockEntity.class);filter.setItem(0,new ItemStack(Items.IRON_INGOT));var target=h.getBlockEntity(new BlockPos(3,2,2), ChestBlockEntity.class);
        try(var tx=Transaction.openOuter()){h.assertTrue(filter.itemsFor(Direction.WEST).insert(ItemVariant.of(Items.GOLD_INGOT),4,tx)==0,"Unlisted item never passes");h.assertTrue(filter.itemsFor(Direction.WEST).insert(ItemVariant.of(Items.IRON_INGOT),4,tx)==4,"Listed item reaches connected container");}
        h.assertTrue(target.isEmpty(),"Rollback preserves output when the transaction does not commit");
        try(var tx=Transaction.openOuter()){filter.itemsFor(Direction.WEST).insert(ItemVariant.of(Items.IRON_INGOT),4,tx);tx.commit();}
        h.assertTrue(target.getItem(0).getCount()==4&&filter.getItem(0).getCount()==1,"Transfer commits once without consuming or duplicating sample");h.succeed();
    }
}

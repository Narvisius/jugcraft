package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.*;
import io.github.jimbozoomer.jugcraft.tools.*;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Real inventory/world operations, claim callbacks and persisted robot cargo. Deferred until combined validation. */
public final class JimtaRobotGameTests {
    public static ServerPlayer player(GameTestHelper h){
        var id=UUID.randomUUID();var profile=new com.mojang.authlib.GameProfile(id,"jimta-"+id.toString().substring(0,8));
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false));
        p.setGameMode(GameType.SURVIVAL);p.snapTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(5,2,5))));return p;
    }
    private static WorkshopRobot robot(GameTestHelper h,ServerPlayer p,String program){
        var r=WorkshopRobots.TYPE.create(h.getLevel(),EntitySpawnReason.SPAWN_ITEM_USE);
        var home=h.absolutePos(new BlockPos(2,1,2));r.configure(p.getUUID(),home);r.program(program);r.snapTo(Vec3.atCenterOf(home.above()));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.REDSTONE));r.interact(p,InteractionHand.MAIN_HAND,net.minecraft.world.phys.Vec3.ZERO);
        h.assertTrue(r.energy()==1000&&p.getMainHandItem().isEmpty(),"One starter redstone consumed for finite charge");return r;
    }
    private static void ticks(WorkshopRobot r,int count){for(int i=0;i<count;i++)r.tick();}
    @GameTest public void robotCraftingPreservesInputsWhenOutputsAreFull(GameTestHelper h){
        var p=player(h);try{
            var r=robot(h,p,"crafter");r.cargo.setItem(0,new ItemStack(Items.OAK_LOG));
            for(int i=9;i<27;i++)r.cargo.setItem(i,new ItemStack(Items.STONE,64));ticks(r,40);
            h.assertTrue(r.cargo.getItem(0).is(Items.OAK_LOG)&&r.energy()==1000,"Full output neither consumes ingredients nor pays action energy");
            r.cargo.setItem(9,ItemStack.EMPTY);ticks(r,40);
            h.assertTrue(r.cargo.getItem(0).isEmpty()&&r.cargo.getItem(9).is(Items.OAK_PLANKS)&&r.cargo.getItem(9).getCount()==4,"One real crafting recipe yields four planks");
            h.assertTrue(r.energy()==1000-WorkshopRobot.STEP_COST,"Exactly one completed craft pays once");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    @GameTest public void miningChecksClaimsAndPoweredToolChargeBeforeChangingWorld(GameTestHelper h){
        var p=player(h);try{
            var r=robot(h,p,"miner");var target=new BlockPos(2,1,3);h.setBlock(target,Blocks.IRON_ORE);
            var drill=new ItemStack(JugcraftTools.MINING_DRILL);Chargeable.setEnergy(drill,120);r.cargo.setItem(0,drill);
            try(var claim=TestClaims.open().block(h.absolutePos(target),UUID.randomUUID())){
                ticks(r,200);h.assertTrue(h.getBlockState(target).is(Blocks.IRON_ORE)&&Chargeable.energy(r.cargo.getItem(0))==120,"Another player's claim prevents digging and tool debit");
            }
            Chargeable.setEnergy(r.cargo.getItem(0),0);ticks(r,200);h.assertTrue(h.getBlockState(target).is(Blocks.IRON_ORE),"Empty powered tool cannot mine");
            Chargeable.setEnergy(r.cargo.getItem(0),120);ticks(r,200);
            h.assertTrue(h.getBlockState(target).isAir()&&Chargeable.energy(r.cargo.getItem(0))==60,"Successful block pays one powered-tool charge");
            h.assertTrue(r.cargo.countItem(Items.RAW_IRON)==1,"The real block drop goes to finite cargo");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    @GameTest public void blockedCargoNeverDestroysOreOrDamagesTool(GameTestHelper h){
        var p=player(h);try{
            var r=robot(h,p,"miner");var target=new BlockPos(2,1,3);h.setBlock(target,Blocks.IRON_ORE);r.cargo.setItem(0,new ItemStack(Items.DIAMOND_PICKAXE));
            for(int i=1;i<27;i++)r.cargo.setItem(i,new ItemStack(Items.STONE,64));ticks(r,200);
            h.assertTrue(h.getBlockState(target).is(Blocks.IRON_ORE)&&r.cargo.getItem(0).getDamageValue()==0,"All drop slots must fit before the block and tool change");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    @GameTest public void ownerCargoFluidAndProgramSurviveReloadWithBoundedLinks(GameTestHelper h){
        var p=player(h);try{
            var r=robot(h,p,"fluid_carrier");r.cargo.setItem(3,new ItemStack(Items.IRON_INGOT,17));
            try(var tx=Transaction.openOuter()){r.fluid.insert(FluidVariant.of(Fluids.WATER),3*FluidConstants.BUCKET,tx);tx.commit();}
            h.assertTrue(!r.link(0,h.absolutePos(new BlockPos(100,1,2)),Direction.UP),"No distant or chunk-loading link");
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());r.saveWithoutId(saved);
            var copy=WorkshopRobots.TYPE.create(h.getLevel(),EntitySpawnReason.LOAD);copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved.buildResult()));
            h.assertTrue(copy.ownedBy(p)&&copy.program().equals("fluid_carrier")&&copy.energy()==1000,"Owner, program and charge persist");
            h.assertTrue(copy.cargo.getItem(3).getCount()==17&&copy.fluid.amount==3*FluidConstants.BUCKET&&copy.fluid.variant.isOf(Fluids.WATER),"Actual cargo and finite fluid persist");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
}

package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.artillery.*;
import io.github.jimbozoomer.jugcraft.building.*;
import io.github.jimbozoomer.jugcraft.fluid.*;
import io.github.jimbozoomer.jugcraft.guns.*;
import io.github.jimbozoomer.jugcraft.logistics.*;
import io.github.jimbozoomer.jugcraft.server.ViewerWorldRules;
import io.github.jimbozoomer.jugcraft.weapons.Blast;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;

/** Actual actions with two simulated server-player identities, not a live two-account network test. */
public final class ServerDefenseGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"defense-test");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false));
        p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();
        p.snapTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(2,3,2))));
        h.assertFalse(p.isCreative(),"Real survival fixture, not the permanently creative vanilla mock");return p;
    }
    private static void remove(ServerPlayer p){p.level().getServer().getPlayerList().remove(p);p.discard();}
    private static BlockPos border(GameTestHelper h, int offset) {
        var base=h.absolutePos(new BlockPos(offset,3,offset));
        return new BlockPos(Math.floorDiv(base.getX(),16)*16+15,base.getY(),Math.floorDiv(base.getZ(),16)*16+8);
    }
    private static void aim(ServerPlayer player, LivingEntity target) {
        var aim=target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        player.setYRot((float)Math.toDegrees(Math.atan2(-aim.x,aim.z)));
        player.setYHeadRot(player.getYRot());
        player.setXRot((float)Math.toDegrees(Math.atan2(-aim.y,Math.sqrt(aim.x*aim.x+aim.z*aim.z))));
        var gun=new ItemStack(JugcraftGuns.GUNS.get("patchwork_carbine"));GunItem.setLoaded(gun,10);
        player.setItemInHand(InteractionHand.MAIN_HAND,gun);
    }

    @GameTest
    public void twoPlayersClaimsStopBulletsBlastsBucketsAndRevokeTrust(GameTestHelper h) {
        var l=h.getLevel();var owner=player(h);var visitor=player(h);
        owner.setGameMode(GameType.SURVIVAL);visitor.setGameMode(GameType.SURVIVAL);
        var from=border(h,10240);var target=from.east(3);
        var land=LandRegistry.get(l.getServer());var book=Ledger.get(l.getServer()).book();long key=LandRegistry.keyOf(target);
        var victim=h.spawnWithNoFreeWill(EntityTypes.COW,new BlockPos(1,2,1));
        try {
            book.account(visitor.getUUID(),"ClaimVisitor");land.assign(book,key,book.account(owner.getUUID(),"WeaponsOwner"),0);
            for(int x=-2;x<=5;x++)for(int y=0;y<=3;y++) l.setBlockAndUpdate(from.offset(x,y,0),Blocks.AIR.defaultBlockState());
            victim.snapTo(Vec3.atCenterOf(target));visitor.snapTo(Vec3.atCenterOf(from));owner.snapTo(Vec3.atCenterOf(from.north(3)));
            float health=victim.getHealth();aim(visitor,victim);
            h.assertTrue(GunShots.fire(visitor),"Visitor fires an actual loaded gun");
            h.assertTrue(victim.getHealth()==health,"Bullets cannot damage someone else's claimed livestock");
            Blast.detonate(l,victim.position(),null,visitor,4,10);
            h.assertTrue(victim.getHealth()==health,"Indirect grenade/rocket blast cannot bypass the claim");
            var water=target.south(2);l.setBlockAndUpdate(water,Blocks.AIR.defaultBlockState());
            h.assertFalse(((BucketItem)Items.WATER_BUCKET).emptyContents(visitor,l,water,null),"Bucket placement denied before mutation");
            h.assertTrue(l.getFluidState(water).isEmpty(),"Denied bucket leaves the world alone");
            h.assertTrue(((BucketItem)Items.WATER_BUCKET).emptyContents(owner,l,water,null),"Owner can place water");
            visitor.snapTo(Vec3.atCenterOf(water.above()));visitor.setXRot(90);
            visitor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
            Items.BUCKET.use(l,visitor,InteractionHand.MAIN_HAND);
            h.assertTrue(l.getFluidState(water).isSource()&&visitor.getMainHandItem().is(Items.BUCKET),"Actual bucket raycast cannot steal a protected water source");
            owner.snapTo(Vec3.atCenterOf(water.above()));owner.setXRot(90);
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
            Items.BUCKET.use(l,owner,InteractionHand.MAIN_HAND);
            h.assertTrue(l.getFluidState(water).isEmpty(),"Owner's actual bucket raycast can collect the same source");
            owner.snapTo(Vec3.atCenterOf(from.north(3)));
            land.trust(owner,book,"ClaimVisitor",true);victim.damageCooldownTime=0;
            h.assertTrue(victim.hurtServer(l,l.damageSources().playerAttack(visitor),1),"Trusted player's actual damage succeeds");
            land.trust(owner,book,"ClaimVisitor",false);victim.damageCooldownTime=0;health=victim.getHealth();
            h.assertFalse(victim.hurtServer(l,l.damageSources().playerAttack(visitor),1),"Revocation applies to actual damage immediately");
            h.assertTrue(victim.getHealth()==health,"No damage after trust revoked");
            aim(owner,victim);victim.damageCooldownTime=0;
            h.assertTrue(GunShots.fire(owner)&&victim.getHealth()<health,"Claim owner can still shoot livestock");
        } finally { victim.discard();land.abandon(owner,book,key,true,0);remove(owner);remove(visitor); }
        h.succeed();
    }

    @GameTest
    public void twoPlayersClaimsPartitionCachedPipesFluidsHoppersAndRacks(GameTestHelper h) {
        var l=h.getLevel();var owner=player(h);var neighbor=player(h);
        var from=border(h,11264).west(2);var pipe=from.east();var out=from.east(3);
        var land=LandRegistry.get(l.getServer());var book=Ledger.get(l.getServer()).book();
        long a=LandRegistry.keyOf(from),b=LandRegistry.keyOf(out);
        try {
            var first=book.account(owner.getUUID(),"FactoryOwner");var second=book.account(neighbor.getUUID(),"FactoryNeighbor");
            land.assign(book,a,first,0);land.assign(book,b,first,0);
            l.setBlockAndUpdate(from,Blocks.BARREL.defaultBlockState());
            l.setBlockAndUpdate(pipe,JugcraftLogistics.BRASS_ITEM_PIPE.defaultBlockState());
            l.setBlockAndUpdate(pipe.east(),JugcraftLogistics.BRASS_ITEM_PIPE.defaultBlockState());
            l.setBlockAndUpdate(out,Blocks.BARREL.defaultBlockState());
            var input=(BarrelBlockEntity)l.getBlockEntity(from);var output=(BarrelBlockEntity)l.getBlockEntity(out);
            input.setItem(0,new ItemStack(Items.IRON_INGOT,16));
            var source=ItemStorage.SIDED.find(l,from,Direction.EAST);
            h.assertTrue(ItemNetworks.push(l,from,Direction.EAST,source,1,Set.of(from))==1,"One owner's pipes transfer across adjacent claimed chunks");
            land.assign(book,b,second,0);
            h.assertTrue(ItemNetworks.push(l,from,Direction.EAST,source,1,Set.of(from))==0,"Claim transfer invalidates a previously connected pipe network");
            h.assertTrue(output.getItem(0).getCount()==1&&input.getItem(0).getCount()==15,"No items lost or stolen");
            // Direct native hopper output on the same border.
            var hopperPos=out.west().north(2);var chest=hopperPos.east();
            l.setBlockAndUpdate(chest,Blocks.BARREL.defaultBlockState());
            var state=Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.EAST);
            l.setBlockAndUpdate(hopperPos,state);var hopper=(HopperBlockEntity)l.getBlockEntity(hopperPos);hopper.setItem(0,new ItemStack(Items.GOLD_INGOT,2));
            HopperBlockEntity.pushItemsTick(l,hopperPos,state,hopper);
            h.assertTrue(hopper.getItem(0).getCount()==2,"Hopper cannot inject items into a foreign claim");
            // Fluid source and destination use the same boundary, independent of item caches.
            var fluidFrom=from.south(2);var fluidOut=out.south(2);
            l.setBlockAndUpdate(fluidFrom,JugcraftFluids.FLUID_TANK.defaultBlockState());
            l.setBlockAndUpdate(fluidFrom.east(),JugcraftFluids.BRONZE_FLUID_PIPE.defaultBlockState());
            l.setBlockAndUpdate(fluidFrom.east(2),JugcraftFluids.BRONZE_FLUID_PIPE.defaultBlockState());
            l.setBlockAndUpdate(fluidOut,JugcraftFluids.FLUID_TANK.defaultBlockState());
            var tank=(FluidTankBlockEntity)l.getBlockEntity(fluidFrom);
            try(var tx=Transaction.openOuter()){tank.storage.insert(FluidVariant.of(Fluids.WATER),FluidConstants.BUCKET,tx);tx.commit();}
            h.assertTrue(FluidNetworks.pushToNeighbors(l,fluidFrom,tank.storage,1000,List.of(Direction.EAST))==0,"Fluid pipe respects ownership");
            var rackPos=out.above(2);l.setBlockAndUpdate(rackPos,Fortifications.BLOCKS.get("ready_rack").defaultBlockState());
            var rack=(ReadyRackBlock.Entity)l.getBlockEntity(rackPos);rack.shells.setItem(0,new ItemStack(JugcraftArtillery.HEAVY_SHELL_ITEM,4));
            var area=new AABB(rackPos.west()).inflate(2);
            h.assertFalse(ReadyRackBlock.take(l,area,JugcraftArtillery.HEAVY_SHELL_ITEM),"Gun cannot steal shells from a neighbor's ready rack");
            h.assertTrue(rack.count()==4,"Refused rack transfer preserves every shell");
            land.assign(book,b,first,0);
            h.assertTrue(ItemNetworks.push(l,from,Direction.EAST,source,1,Set.of(from))==1,"Pipe resumes when ownership matches again");
            h.assertTrue(FluidNetworks.pushToNeighbors(l,fluidFrom,tank.storage,1000,List.of(Direction.EAST))>0,"Friendly fluid plumbing works");
            h.assertTrue(ReadyRackBlock.take(l,area,JugcraftArtillery.HEAVY_SHELL_ITEM)&&rack.count()==3,"Friendly rack consumes exactly one shell");
        } finally {land.abandon(owner,book,a,true,0);land.abandon(owner,book,b,true,0);remove(owner);remove(neighbor);}
        h.succeed();
    }

    @GameTest
    public void villagersRejectZombieTargetAndDamageButPillagersStillAttack(GameTestHelper h) {
        var l=h.getLevel();var villager=h.spawnWithNoFreeWill(EntityTypes.VILLAGER,new BlockPos(1,2,1));
        var zombie=h.spawnWithNoFreeWill(EntityTypes.HUSK,new BlockPos(3,2,1));
        var pillager=h.spawnWithNoFreeWill(EntityTypes.PILLAGER,new BlockPos(5,2,1));
        var p=player(h);p.setGameMode(GameType.SURVIVAL);
        h.assertFalse(zombie.canAttack(villager),"Zombie targeting rejects villager");
        zombie.setTarget(villager);h.assertTrue(zombie.getTarget()==null,"Explicit mob targeting also rejects resident");
        float before=villager.getHealth();
        h.assertFalse(villager.hurtServer(l,l.damageSources().mobAttack(zombie),3),"Mob damage cannot bypass target policy");
        h.assertTrue(villager.getHealth()==before,"Zombie cannot injure resident");
        pillager.setTarget(villager);h.assertTrue(pillager.getTarget()==villager&&pillager.canAttack(villager),"Pillager remains a valid attacker");
        h.assertTrue(villager.hurtServer(l,l.damageSources().mobAttack(pillager),1),"Pillager damage still works");
        villager.damageCooldownTime=0;
        h.assertTrue(villager.hurtServer(l,l.damageSources().playerAttack(p),1),"Player rules remain unchanged on unclaimed land");
        villager.discard();zombie.discard();pillager.discard();remove(p);h.succeed();
    }

    @GameTest
    public void dayClockUsesNativeFractionalRatesAndLeavesOtherClocksAlone(GameTestHelper h) {
        var clock=new net.minecraft.world.clock.ServerClockManager.ServerClockInstance();
        // Exercise native fractional accumulation for a full cycle, including the phase transition.
        long elapsed=0;
        while(clock.totalTicks()<24000&&elapsed<33000) {
            var saved=clock.packState();
            clock.loadFrom(new net.minecraft.world.clock.ClockState(saved.totalTicks(),saved.partialTick(),ViewerWorldRules.daylightRate(clock.totalTicks()),false));
            clock.tick();elapsed++;
            if(clock.totalTicks()==12000) h.assertTrue(elapsed==24000,"Daylight takes 24,000 simulation ticks (20 minutes)");
        }
        h.assertTrue(elapsed==32000&&clock.totalTicks()==24000,"Night takes 8,000 ticks (6 minutes 40 seconds); calendar advances one day");
        var server=h.getLevel().getServer();var registry=server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK);
        var manager=server.clockManager();var overworld=registry.getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD);
        var saved=manager.getInstance(overworld).packState();
        var others=new HashMap<net.minecraft.core.Holder<net.minecraft.world.clock.WorldClock>,net.minecraft.world.clock.ClockState>();
        registry.listElements().filter(holder->!holder.equals(overworld)).forEach(holder->others.put(holder,manager.getInstance(holder).packState()));
        try {
            manager.getInstance(overworld).loadFrom(new net.minecraft.world.clock.ClockState(6000,0,.75F,true));ViewerWorldRules.updateClock(server);
            h.assertTrue(manager.getInstance(overworld).rate()==.5F&&manager.getInstance(overworld).packState().paused(),"Actual manager changes daylight rate and preserves pause");
            manager.getInstance(overworld).loadFrom(new net.minecraft.world.clock.ClockState(18000,0,.75F,false));ViewerWorldRules.updateClock(server);
            h.assertTrue(manager.getInstance(overworld).rate()==1.5F,"Actual manager chooses the faster night rate");
            others.forEach((holder,state)->h.assertTrue(manager.getInstance(holder).packState().equals(state),"Other dimension clocks unchanged"));
        } finally {manager.getInstance(overworld).loadFrom(saved);}

        h.succeed();
    }

    @GameTest
    public void automatedShellSpareBystandersEvenAfterFiring(GameTestHelper h) {
        var l=h.getLevel();var point=h.absoluteVec(new Vec3(3,3,3));
        var mob=h.spawnWithNoFreeWill(EntityTypes.HUSK,new BlockPos(3,3,3));
        var villager=h.spawnWithNoFreeWill(EntityTypes.VILLAGER,new BlockPos(4,3,3));
        var cow=h.spawnWithNoFreeWill(EntityTypes.COW,new BlockPos(3,3,4));
        var player=player(h);player.setGameMode(GameType.SURVIVAL);player.snapTo(point.add(1,0,1));
        var shell=new ArtilleryShell(JugcraftArtillery.HEAVY_SHELL,l);shell.setAutomatedDefense(true);
        h.assertFalse(AutomatedDefense.clearToFire(l,point,8),"Sentry holds fire around villagers, animals and players");
        float m=mob.getHealth(),v=villager.getHealth(),c=cow.getHealth(),p=player.getHealth();
        Blast.detonate(l,point,shell,null,6,4);
        h.assertTrue(mob.getHealth()<m,"Automated blast damages its hostile target");
        h.assertTrue(villager.getHealth()==v&&cow.getHealth()==c&&player.getHealth()==p,"Arriving bystanders are spared at impact");
        mob.discard();villager.discard();cow.discard();remove(player);shell.discard();h.succeed();
    }

    @GameTest
    public void fireControlRechecksBothPlayersAndOwnershipChanges(GameTestHelper h) {
        var l=h.getLevel();var owner=player(h);var visitor=player(h);
        var pos=border(h,12288).west(7);var key=LandRegistry.keyOf(pos);
        var book=Ledger.get(l.getServer()).book();var land=LandRegistry.get(l.getServer());
        var gun=h.spawn(JugcraftTowerGuns.type("bastion_autocannon"),new Vec3(1,2,1));gun.snapTo(Vec3.atCenterOf(pos.east(3)));
        try {
            l.setBlockAndUpdate(pos,FireControl.TABLE.defaultBlockState());var table=(FireControlTableBlock.Entity)l.getBlockEntity(pos);
            land.assign(book,key,book.account(owner.getUUID(),"GunnerOwner"),0);
            FireControl.startLink(visitor,l,pos);FireControl.link(visitor,gun);
            h.assertFalse(table.linked(gun.getUUID()),"Outsider cannot bind a claimed table or gun");
            FireControl.startLink(owner,l,pos);FireControl.link(owner,gun);
            h.assertTrue(table.linked(gun.getUUID())&&table.mayOperate(l,gun.blockPosition()),"Owner links a usable unattended defense");
            h.assertFalse(gun.hurtServer(l,l.damageSources().playerAttack(visitor),1000),"Remote damage cannot destroy claimed artillery");
            land.assign(book,key,book.account(visitor.getUUID(),"NewGunnerOwner"),0);
            h.assertFalse(table.mayOperate(l,gun.blockPosition()),"Old operator loses unattended permission after sale or reassignment");
            table.authorize(visitor);h.assertTrue(table.mayOperate(l,gun.blockPosition()),"New owner can recommission the defense");
        } finally {gun.discard();land.abandon(owner,book,key,true,0);remove(owner);remove(visitor);}
        h.succeed();
    }

    @GameTest(structure="jugcraft-test:drone_tower",maxTicks=320,skyAccess=true)
    public void automatedTurretFeedsFromRackAndObeysRedstoneStop(GameTestHelper h) {
        var l=h.getLevel();var tablePos=new BlockPos(8,1,2);var rackPos=new BlockPos(5,1,2);
        for(int x=0;x<10;x++)for(int z=0;z<36;z++) h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        h.setBlock(tablePos,FireControl.TABLE.defaultBlockState().setValue(FireControlTableBlock.MODE,FireControlTableBlock.Mode.SENTRY));
        h.setBlock(rackPos,Fortifications.BLOCKS.get("ready_rack"));
        var rack=h.getBlockEntity(rackPos,ReadyRackBlock.Entity.class);rack.shells.addItem(new ItemStack(JugcraftArtillery.HEAVY_SHELL_ITEM,6));
        var gun=h.spawn(JugcraftTowerGuns.type("triple_battery"),new Vec3(2.5,1,2.5));gun.face(0);
        var owner=player(h);FireControl.startLink(owner,l,h.absolutePos(tablePos));FireControl.link(owner,gun);remove(owner);
        h.spawnWithNoFreeWill(EntityTypes.HUSK,new Vec3(2.5,1,30.5));
        var resident=h.spawnWithNoFreeWill(EntityTypes.VILLAGER,new Vec3(3.5,1,30.5));
        h.runAfterDelay(70,()->{
            h.assertTrue(rack.count()==6,"Turret holds fire while a resident is near its target");
            h.setBlock(tablePos.east(),Blocks.REDSTONE_BLOCK);resident.discard();
        });
        h.runAfterDelay(140,()->{
            h.assertTrue(rack.count()==6,"Powered sentry table stops automatic fire even with a clear target");
            h.setBlock(tablePos.east(),Blocks.AIR);
        });
        h.runAfterDelay(300,()->{
            h.assertTrue(rack.count()<6,"Unpowered sentry fires using actual stocked ammunition");
            h.succeed();
        });
    }
}

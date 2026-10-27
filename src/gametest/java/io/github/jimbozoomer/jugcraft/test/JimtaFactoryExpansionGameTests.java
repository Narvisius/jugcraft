package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.*;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;

/** Combined-batch regressions. Authored now; gameplay execution deferred until B–H implementation is complete. */
public final class JimtaFactoryExpansionGameTests {
    @GameTest public void rubberTrampolineHasThinCollisionAndCrouchableFallProtection(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
            h.assertTrue(player.connection.hasClientLoaded(),"Fall-damage fixture completed the client loading handshake");
            var block=IndustrialConstruction.BLOCKS.get("rubber_trampoline");var pos=new BlockPos(2,2,2);h.setBlock(pos,block);var at=h.absolutePos(pos);
            h.assertTrue(block.defaultBlockState().getCollisionShape(h.getLevel(),at).max(Direction.Axis.Y)==.125,"Rubber sheet uses the original one-eighth-block height");
            float before=player.getHealth();block.fallOn(h.getLevel(),block.defaultBlockState(),at,player,8);
            h.assertTrue(player.getHealth()==before&&Math.abs(block.getBounceRestitution()-.8F)<.0001,"Normal landing absorbs damage and uses native 80-percent restitution");
            player.setShiftKeyDown(true);block.fallOn(h.getLevel(),block.defaultBlockState(),at,player,8);
            h.assertTrue(player.getHealth()<before,"Suppressing the bounce restores ordinary fall damage");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void storageUpgradePreservesEnergyAndPolicyWithoutStaleDuplicate(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var pos=new BlockPos(2,2,2);h.setBlock(pos,JimtaIndustry.MACHINES.get("jimta_mfe"));var at=h.absolutePos(pos);
            var old=(io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity)h.getLevel().getBlockEntity(at);
            var tag=old.saveWithoutMetadata(h.getLevel().registryAccess());tag.putLong("energy",1_234_567);tag.putInt("batteryReserve",70);tag.putInt("batteryOutput",50);
            old.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag));
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(IndustrialStorageUpgrade.ITEM));
            var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at),Direction.UP,at,false));
            IndustrialStorageUpgrade.ITEM.useOn(context);
            var replacement=(io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity)h.getLevel().getBlockEntity(at);
            h.assertTrue(replacement!=old&&replacement.getBlockState().is(JimtaIndustry.MACHINES.get("jimta_mfsu")),"Kit installs a fresh higher-tier entity");
            var saved=replacement.saveWithoutMetadata(h.getLevel().registryAccess());
            h.assertTrue(replacement.energyFor(null).getAmount()==1_234_567&&saved.getIntOr("batteryReserve",0)==70&&saved.getIntOr("batteryOutput",0)==50,"Charge and power policy survive the upgrade");
            h.assertTrue(old.isRemoved()&&old.energyFor(null).getAmount()==0&&player.getMainHandItem().isEmpty(),"Old handles contain no duplicate charge and exactly one kit was consumed");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void obscuratorConsumesPowerOnlyForPermittedFacadeChanges(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var pos=new BlockPos(2,2,2);h.setBlock(pos,IndustrialTransport.BLOCKS.get("jimta_pipe_stone_item"));var at=h.absolutePos(pos);var node=(TransportAttachments.Node)h.getLevel().getBlockEntity(at);
            node.attachments().faces.set(Direction.UP.ordinal(),Blocks.STONE.defaultBlockState());var item=new net.minecraft.world.item.ItemStack(IndustrialObscurator.ITEM);
            item.set(io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalFacades.MATERIAL,Blocks.BRICKS.defaultBlockState());io.github.jimbozoomer.jugcraft.tools.Chargeable.setEnergy(item,10_000);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,item);
            var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at),Direction.UP,at,false));
            try(var claim=TestClaims.open().block(at,java.util.UUID.randomUUID())){IndustrialObscurator.ITEM.useOn(context);h.assertTrue(node.attachments().faces.get(Direction.UP.ordinal()).is(Blocks.STONE)&&io.github.jimbozoomer.jugcraft.tools.Chargeable.energy(item)==10_000,"Denied reskin neither changes appearance nor consumes power");}
            IndustrialObscurator.ITEM.useOn(context);IndustrialObscurator.ITEM.useOn(context);
            h.assertTrue(node.attachments().faces.get(Direction.UP.ordinal()).is(Blocks.BRICKS)&&io.github.jimbozoomer.jugcraft.tools.Chargeable.energy(item)==5_000,"One actual reskin costs 5000 JE; repeating it cannot spend more or create a facade");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void waterGelRespectsClaimsAndRetainsItsOwner(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var a=new BlockPos(2,2,2);var b=a.east();h.setBlock(a,Blocks.WATER);h.setBlock(b,Blocks.WATER);
            var abs=h.absolutePos(a);var protectedPos=h.absolutePos(b);
            try(var claim=TestClaims.open().block(protectedPos,java.util.UUID.randomUUID())){
                h.assertTrue(!FactoryWaterGel.start(h.getLevel(),player,protectedPos),"Seed cannot modify protected water");
                h.assertTrue(FactoryWaterGel.start(h.getLevel(),player,abs),"Permitted source water accepts a seed");
                var node=(FactoryWaterGel.Node)h.getLevel().getBlockEntity(abs);node.step(h.getLevel());
                h.assertTrue(h.getLevel().getBlockState(protectedPos).is(Blocks.WATER),"Delayed spreading also respects claims");
                var saved=node.saveWithoutMetadata(h.getLevel().registryAccess());var copy=new FactoryWaterGel.Node(abs,node.getBlockState());copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
                h.assertTrue(copy.owner.equals(player.getUUID())&&copy.origin.equals(abs),"Reload preserves spreading authority and radius origin");
            }
            var node=(FactoryWaterGel.Node)h.getLevel().getBlockEntity(abs);node.step(h.getLevel());
            h.assertTrue(h.getLevel().getBlockState(protectedPos).is(FactoryWaterGel.BLOCK),"Released water can join the bounded spread");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void waterGelStopsForAbsentOwnerAndWaitsToSetUntilDry(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var a=new BlockPos(2,2,2);h.setBlock(a,FactoryWaterGel.BLOCK.defaultBlockState().setValue(FactoryWaterGel.STAGE,4));h.setBlock(a.east(),Blocks.WATER);
            var node=(FactoryWaterGel.Node)h.getLevel().getBlockEntity(h.absolutePos(a));node.owner=player.getUUID();node.step(h.getLevel());
            h.assertTrue(node.getBlockState().getValue(FactoryWaterGel.STAGE)==4,"Wet neighbors delay setting");
            h.setBlock(a.east(),Blocks.AIR);node.owner=java.util.UUID.randomUUID();node.step(h.getLevel());
            h.assertTrue(node.getBlockState().getValue(FactoryWaterGel.STAGE)==4,"Absent-owner automation is paused");
            node.owner=player.getUUID();node.step(h.getLevel());node.step(h.getLevel());
            h.assertTrue(node.getBlockState().getValue(FactoryWaterGel.STAGE)==6,"Dry gel completes its finite stages");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void foamFluidRefillRollsBackAndNeverRoundsUp(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var hand=net.minecraft.world.InteractionHand.MAIN_HAND;
            player.setItemInHand(hand,new net.minecraft.world.item.ItemStack(IndustrialGear.ITEMS.get("jimta_construction_foam_pack")));
            var storage=IndustrialBackpacks.foamStorage(net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.ofPlayerHand(player,hand));
            var fluid=FluidVariant.of(IndustrialHeat.CONSTRUCTION_FOAM.source());long unit=IndustrialBackpacks.FOAM_PER_BLOCK;
            try(var tx=Transaction.openOuter()){h.assertTrue(storage.insert(fluid,unit*12,tx)==unit*12,"Fluid refill reserves twelve whole charges");}
            h.assertTrue(IndustrialBackpacks.foam(player.getItemInHand(hand))==0,"Aborted refill leaves no free spray charges");
            try(var tx=Transaction.openOuter()){
                h.assertTrue(storage.insert(FluidVariant.of(Fluids.WATER),unit,tx)==0,"Water cannot refill foam");
                h.assertTrue(storage.insert(fluid,unit-1,tx)==0,"A partial dose is not rounded into a charge");
                h.assertTrue(storage.insert(fluid,unit*12+1,tx)==unit*12,"Only whole doses are consumed");tx.commit();
            }
            h.assertTrue(IndustrialBackpacks.foam(player.getItemInHand(hand))==12,"Committed refill becomes exactly twelve charges");
            try(var tx=Transaction.openOuter()){h.assertTrue(storage.extract(fluid,unit*5+1,tx)==unit*5,"Emptying preserves exact dose accounting");tx.commit();}
            var canister=new net.minecraft.world.item.ItemStack(io.github.jimbozoomer.jugcraft.chemistry.ConstructionChemistry.FOAM_CANISTER);canister.setDamageValue(30);
            h.assertTrue(IndustrialBackpacks.refillFoam(player.getItemInHand(hand),canister)==2&&canister.isEmpty()&&IndustrialBackpacks.foam(player.getItemInHand(hand))==9,"Part-used canisters remain compatible with the same balance");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void containmentPreservesInventoryAndAttenuatesStoredRadiation(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var uranium=new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id("uranium_ingot")),12);
            var box=new net.minecraft.world.item.ItemStack(IndustrialContainment.ITEM);
            h.assertTrue(IndustrialContainment.accepts(uranium)&&!IndustrialContainment.accepts(box)&&!IndustrialContainment.accepts(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND)),"Only actual radioactive non-container materials fit");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,box);
            IndustrialContainment.ITEM.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
            var menu=(IndustrialContainment.Menu)player.containerMenu;menu.getSlot(12).setByPlayer(uranium.copy());menu.quickMoveStack(player,12);
            int count=0;for(int i=0;i<12;i++)count+=menu.getSlot(i).getItem().getCount();
            h.assertTrue(count==12&&menu.getSlot(12).getItem().isEmpty(),"Shift insertion conserves all twelve ingots");
            double ordinary=io.github.jimbozoomer.jugcraft.nuclear.Radiation.strength(uranium),shielded=io.github.jimbozoomer.jugcraft.nuclear.Radiation.strength(box);
            h.assertTrue(ordinary>0&&Math.abs(shielded-ordinary*.01)<1e-8,"Stored radiation is measured with 99% attenuation");
            player.closeContainer();IndustrialContainment.ITEM.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);menu=(IndustrialContainment.Menu)player.containerMenu;
            h.assertTrue(menu.getSlot(0).getItem().getCount()==12,"Reopening retains contents");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            h.assertTrue(!menu.stillValid(player)&&menu.quickMoveStack(player,0).isEmpty(),"A stale held-container menu cannot extract a second copy");
        }finally{player.closeContainer();h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void statuePosePersistsAndChecksBothHalves(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var relative=new BlockPos(2,2,2);h.setBlock(relative,JimtaStatues.BLOCK);h.setBlock(relative.above(),JimtaStatues.BLOCK.defaultBlockState().setValue(JimtaStatues.Y,1));
            var pos=h.absolutePos(relative);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos.offset(1,0,0)));
            var node=(JimtaStatues.Node)h.getLevel().getBlockEntity(pos);node.pose=22;node.flipped=true;node.profile=player.getUUID();
            var saved=node.saveWithoutMetadata(h.getLevel().registryAccess());var loaded=new JimtaStatues.Node(pos,node.getBlockState());loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
            h.assertTrue(loaded.pose==22&&loaded.flipped&&player.getUUID().equals(loaded.profile),"Statue pose, mirror and profile survive a save/load");
            h.assertTrue(node.permitted(player),"A nearby unclaimed statue can be edited");
            try(var claim=TestClaims.open().block(pos.above(),java.util.UUID.randomUUID())){h.assertTrue(!node.permitted(player),"Protecting the upper half also protects pose edits");}
            h.getLevel().removeBlock(pos.above(),false);h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"Removing the upper half does not leave an orphaned pedestal");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    private FactoryHeatExchanger.Node part(GameTestHelper h,String kind,int x,java.util.UUID owner){
        var pos=new BlockPos(x,2,2);h.setBlock(pos,FactoryHeatExchanger.BLOCKS.get(kind).defaultBlockState().setValue(FactoryHeatExchanger.FACING,Direction.EAST));
        var node=(FactoryHeatExchanger.Node)h.getLevel().getBlockEntity(h.absolutePos(pos));node.owner=owner;return node;
    }
    @GameTest public void counterflowConservesBothCircuitsAndStopsOnFullOutput(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var start=part(h,"start",1,player.getUUID());part(h,"middle",2,player.getUUID());var end=part(h,"end",3,player.getUUID());
            start.input.fill(IndustrialHeat.HOT_COOLANT.source(),100);end.input.fill(Fluids.WATER,100);start.step(h.getLevel());
            h.assertTrue(start.input.millibuckets()==95&&start.output.has(IndustrialHeat.COOLANT.source(),5)&&end.input.millibuckets()==95&&end.output.has(IndustrialHeat.STEAM.fluid(),50),"One section exchanges five mB and creates fifty steam without fluid loss");
            end.output.fill(IndustrialHeat.STEAM.fluid(),15950);start.step(h.getLevel());
            h.assertTrue(start.input.millibuckets()==95&&end.input.millibuckets()==95,"Blocked second circuit consumes neither input");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void missingOrForeignMiddleStopsTheWholeExchanger(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var start=part(h,"start",1,player.getUUID());var middle=part(h,"middle",2,java.util.UUID.randomUUID());var end=part(h,"end",3,player.getUUID());
            start.input.fill(IndustrialHeat.HOT_COOLANT.source(),100);end.input.fill(Fluids.WATER,100);start.step(h.getLevel());
            h.assertTrue(start.input.millibuckets()==100&&end.input.millibuckets()==100,"Foreign owner cannot join circuits");
            middle.owner=player.getUUID();h.setBlock(new BlockPos(2,2,2),Blocks.AIR);start.step(h.getLevel());
            h.assertTrue(start.input.millibuckets()==100&&end.input.millibuckets()==100,"Broken assembly cannot operate");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void detachedFluidPortCannotDrainReplacedMachine(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var start=part(h,"start",1,player.getUUID());start.output.fill(IndustrialHeat.COOLANT.source(),100);var cached=start.port(Direction.DOWN);
            h.setBlock(new BlockPos(1,2,2),Blocks.AIR);
            try(var tx=Transaction.openOuter()){h.assertTrue(cached.extract(FluidVariant.of(IndustrialHeat.COOLANT.source()),8100,tx)==0,"Cached storage rejects detached block entity");}
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void plannerZonesSurviveReloadAndRejectOversize(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,FactoryPlanning.PLANNER);var node=(FactoryPlanning.Planner)h.getLevel().getBlockEntity(h.absolutePos(pos));
        node.owner=java.util.UUID.randomUUID();node.zones.add(new FactoryPlanning.Zone(new BlockPos(1,2,3),new BlockPos(20,20,20)));
        var saved=node.saveWithFullMetadata(h.getLevel().registryAccess());node.zones.clear();node.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(node.zones.size()==1&&node.zones.getFirst().b().equals(new BlockPos(20,20,20)),"Saved planning area retains exact corners");
        h.assertTrue(!FactoryPlanning.valid(BlockPos.ZERO,new BlockPos(Integer.MAX_VALUE,1,1)),"Coordinate overflow cannot make oversized areas valid");h.succeed();
    }
    @GameTest public void spiderPartsForwardDamageAndDisappearWithTheirParent(GameTestHelper h){
        var spider=h.spawn(JimtaEncounterCreatures.ICE_SPIDER,2,2,2);spider.setNoAi(true);var part=JimtaSpiderPart.create(spider,2);float before=spider.getHealth();
        part.hurtServer(h.getLevel(),h.getLevel().damageSources().generic(),3);
        h.assertTrue(spider.getHealth()<before,"Abdomen hit reaches parent health");spider.discard();part.tick();h.assertTrue(part.isRemoved(),"No orphan hitbox remains after parent removal");h.succeed();
    }
    @GameTest public void keeperRiseIsSynchronizedAndSurvivesReload(GameTestHelper h){
        var keeper=h.spawn(JimtaEncounterCreatures.KEEPER,2,2,2);keeper.setNoAi(true);keeper.rise();
        h.assertTrue(keeper.riseTicks()==25,"Client-visible rise duration starts at source length");keeper.tick();h.assertTrue(keeper.riseTicks()==24,"Server advances the client-visible fade");
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());
        keeper.saveWithoutId(out);
        var copy=JimtaEncounterCreatures.KEEPER.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.LOAD);
        copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        h.assertTrue(copy.riseTicks()==24,"Reload retains the synchronized rise progress");
        h.succeed();
    }

    @GameTest public void cachedFluidViewRechecksMachineIdentity(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var start=part(h,"start",1,player.getUUID());start.output.fill(IndustrialHeat.COOLANT.source(),100);
            var view=start.port(Direction.DOWN).iterator().next();h.setBlock(new BlockPos(1,2,2),Blocks.AIR);
            try(var tx=Transaction.openOuter()){h.assertTrue(view.extract(FluidVariant.of(IndustrialHeat.COOLANT.source()),8100,tx)==0&&view.getAmount()==0,"Cached views cannot bypass detached-device checks");}
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void spiderConsumesKilledPreyFoodWithoutStealingNearbyDrops(GameTestHelper h){
        var spider=h.spawn(JimtaEncounterCreatures.ICE_SPIDER,2,2,2);spider.setNoAi(true);
        var cow=h.spawn(net.minecraft.world.entity.EntityTypes.COW,3,2,2);cow.setNoAi(true);
        cow.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
        cow.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND,2);
        var bread=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),cow.getX(),cow.getY(),cow.getZ(),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD,3));h.getLevel().addFreshEntity(bread);
        cow.hurtServer(h.getLevel(),h.getLevel().damageSources().mobAttack(spider),1000);
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,cow.getBoundingBox().inflate(2));
        h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(net.minecraft.world.item.Items.BEEF)),"Fresh prey meat is eaten");
        h.assertTrue(drops.stream().anyMatch(e->e.getItem().is(net.minecraft.world.item.Items.STICK)),"Nonfood equipment remains loot");
        h.assertTrue(!bread.isRemoved()&&bread.getItem().getCount()==3,"Unrelated food on the ground is untouched");h.succeed();
    }
    @GameTest public void portableListMatchesMultipleItemsWithoutCreatingSamples(GameTestHelper h){
        var list=new net.minecraft.world.item.ItemStack(FactoryFieldTools.LIST);
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,list,t->{t.putString("filter0","minecraft:iron_ingot");t.putString("filter1","minecraft:copper_ingot");});
        h.assertTrue(FactoryFieldTools.matches(list,net.minecraft.world.item.Items.IRON_INGOT)&&FactoryFieldTools.matches(list,net.minecraft.world.item.Items.COPPER_INGOT)&&!FactoryFieldTools.matches(list,net.minecraft.world.item.Items.DIAMOND),"A list acts as an exact item whitelist");
        h.assertTrue(list.getCount()==1&&!list.has(net.minecraft.core.component.DataComponents.CONTAINER),"Sample identities are not spendable inventory");h.succeed();
    }
    @GameTest public void portalRefusesOtherPlayersPetsAndBlockedArrival(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var wolf=h.spawn(net.minecraft.world.entity.EntityTypes.WOLF,2,2,2);wolf.setNoAi(true);wolf.setTame(true,false);wolf.setOwnerReference(net.minecraft.world.entity.EntityReference.of(java.util.UUID.randomUUID()));
            h.assertTrue(!JimtaWardEntity.mayTransport(player,wolf),"Portal owner cannot take another player's tame animal");
            var at=h.absolutePos(new BlockPos(4,2,4));h.setBlock(new BlockPos(4,2,4),Blocks.STONE);h.setBlock(new BlockPos(4,3,4),Blocks.STONE);
            h.assertTrue(!JimtaWardEntity.safeArrival(player,wolf,h.getLevel(),net.minecraft.world.phys.Vec3.atBottomCenterOf(at)),"Portal refuses solid destination");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void heatRecoveryPreservesBlockedFluidsAndAccountsForHotWater(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var pos=new BlockPos(2,2,2);h.setBlock(pos,IndustrialHeat.BLOCKS.get("liquid_heat_exchanger"));
            var node=(IndustrialHeat.Node)h.getLevel().getBlockEntity(h.absolutePos(pos));node.owner=player.getUUID();
            node.input.fill(IndustrialHeat.HOT_WATER.source(),100);node.step(h.getLevel());
            h.assertTrue(node.input.millibuckets()==36&&node.output.has(Fluids.WATER,64)&&node.heat==64,"64 hot-water mB conserve volume and yield 64 heat");
            node.input.drain(36);node.input.fill(Fluids.LAVA,100);node.step(h.getLevel());
            h.assertTrue(node.input.millibuckets()==100&&node.heat==64,"Wrong output fluid blocks lava conversion without consuming heat or lava");
            node.output.drain(64);node.step(h.getLevel());
            h.assertTrue(node.input.millibuckets()==36&&node.output.has(IndustrialHeat.PAHOEHOE_LAVA.source(),64)&&node.heat==320,"Lava recovery conserves 64 mB and adds 256 heat");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void surveyedRobotRoutesPersistAndRejectForeignClaims(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var robot=WorkshopRobots.TYPE.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
            var home=h.absolutePos(new BlockPos(2,1,2));robot.configure(player.getUUID(),home);robot.program("carrier");
            var survey=new net.minecraft.world.item.ItemStack(FactoryFieldTools.MAP);var a=home.offset(1,0,0);var b=home.offset(2,0,1);
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,survey,t->{t.putInt("mode",2);t.putInt("points",2);t.putString("Dimension",h.getLevel().dimension().identifier().toString());t.putLong("point0",a.asLong());t.putLong("point1",b.asLong());});
            try(var claim=TestClaims.open().block(b.above(),java.util.UUID.randomUUID())){h.assertTrue(!robot.surveyRoute(player,survey),"Survey cannot route through someone else's claim");}
            h.assertTrue(robot.surveyRoute(player,survey),"A permitted two-point route installs");
            var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());robot.saveWithoutId(out);
            var saved=out.buildResult();var route=BlockPos.CODEC.listOf().parse(net.minecraft.nbt.NbtOps.INSTANCE,saved.get("SurveyRoute")).getOrThrow();
            h.assertTrue(route.equals(java.util.List.of(a.above(),b.above())),"Saved waypoints retain order and flight height");
            var copy=WorkshopRobots.TYPE.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.LOAD);copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
            var next=net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());copy.saveWithoutId(next);
            h.assertTrue(saved.get("SurveyRoute").equals(next.buildResult().get("SurveyRoute")),"Reload retains the repeating route");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
}

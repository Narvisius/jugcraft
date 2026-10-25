package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.*;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.TagValueInput;

/** Deferred with the combined collection tests: actual inventories, JE and saved ownership. */
public final class JimtaWorkshopTableGameTests {
    @GameTest public void engineOutputQuotaSurvivesSimulationAndFuelWaitsForCapacity(GameTestHelper h){
        var p=JimtafactsComputerGameTests.player(h);var pos=new BlockPos(2,2,2);h.setBlock(pos,WorkshopEngines.BLOCKS.get("stirling_engine"));h.setBlock(pos.west(),net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        var node=h.getBlockEntity(pos, WorkshopEngines.Node.class);node.owner=p.getUUID();node.energy.setAmount(32000);node.setItem(0,new ItemStack(Items.COAL));node.step();
        h.assertTrue(node.burn==0&&node.getItem(0).getCount()==1&&node.getItem(1).isEmpty(),"Full engine does not consume fuel or create ash");
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){h.assertTrue(node.powerPort(Direction.UP).extract(1000,tx)==256,"Engine output is limited per tick");}
        h.assertTrue(node.energy.getAmount()==32000,"Simulation leaves stored energy intact");
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){h.assertTrue(node.powerPort(Direction.UP).extract(1000,tx)==256&&node.powerPort(Direction.UP).extract(1000,tx)==0,"Aborted quota restores; repeated same-tick calls cannot exceed it");tx.commit();}
        h.assertTrue(node.energy.getAmount()==31744,"Only committed energy leaves");h.succeed();
    }
    @GameTest public void chuteKeepsFailedDeliveriesAndChargesOnlyWhenItemsMove(GameTestHelper h){
        var p=JimtafactsComputerGameTests.player(h);var pos=new BlockPos(2,3,2);h.setBlock(pos,WorkshopChute.BLOCK);var node=h.getBlockEntity(pos, WorkshopChute.Node.class);node.owner=p.getUUID();node.energy.setAmount(20);node.setItem(0,new ItemStack(Items.IRON_INGOT,20));
        node.step();h.assertTrue(node.getItem(0).getCount()==20&&node.energy.getAmount()==20,"No receiver preserves cargo and boost energy");
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            h.assertTrue(node.port(Direction.NORTH).extract(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.IRON_INGOT),64,tx)==0,"Disabled outlet refuses extraction");
            h.assertTrue(node.port(Direction.DOWN).insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.GOLD_INGOT),1,tx)==0,"Outlet refuses incoming cargo");
        }h.succeed();
    }
    @GameTest public void transformerQuotasRollbackAndAreSharedAcrossFaces(GameTestHelper h){
        var p=JimtafactsComputerGameTests.player(h);var pos=new BlockPos(2,2,2);h.setBlock(pos,IndustrialTransformers.BLOCKS.get("lv"));var node=h.getBlockEntity(pos, IndustrialTransformers.Node.class);node.owner=p.getUUID();
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){h.assertTrue(node.port(Direction.NORTH).insert(500,tx)==128,"LV front accepts only its 128-JE tick budget");}
        h.assertTrue(node.energy.getAmount()==0,"Aborted insertion restores energy");
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){h.assertTrue(node.port(Direction.NORTH).insert(500,tx)==128,"Aborted insertion also restores throughput");tx.commit();}
        int moved=0;for(Direction side:new Direction[]{Direction.SOUTH,Direction.EAST,Direction.WEST,Direction.UP,Direction.DOWN})try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){long first=node.port(side).extract(500,tx);h.assertTrue(first<=32&&node.port(side).extract(500,tx)==0,"Each low face has a shared 32-JE tick cap, including repeated queries");moved+=first;tx.commit();}
        h.assertTrue(moved==128&&node.energy.getAmount()==0,"All five outputs together cannot exceed or duplicate the 128 JE received");h.succeed();
    }
    @GameTest public void regulatorPaysOnlyAfterRealDeliveryAndCannotBeDrainedAroundItsLimit(GameTestHelper h){
        var p=JimtafactsComputerGameTests.player(h);var pos=new BlockPos(3,2,3);h.setBlock(pos,IndustrialFluidServices.BLOCKS.get("fluid_regulator"));h.setBlock(pos.north(),IndustrialFluidServices.BLOCKS.get("solar_distiller"));
        var regulator=h.getBlockEntity(pos, IndustrialFluidServices.Node.class);var target=h.getBlockEntity(pos.north(), IndustrialFluidServices.Node.class);regulator.owner=target.owner=p.getUUID();regulator.alternate=true;
        regulator.input.fill(net.minecraft.world.level.material.Fluids.WATER,500);regulator.energy.setAmount(10);regulator.step();
        h.assertTrue(regulator.input.millibuckets()==400&&target.input.millibuckets()==100&&regulator.energy.getAmount()==0,"One 100-mB delivery pays exactly 10 JE");
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            var water=net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER);
            for(Direction side:Direction.values())h.assertTrue(regulator.port(side).extract(water,81000,tx)==0,"External extraction cannot bypass configured deliveries");}
        regulator.step();h.assertTrue(regulator.input.millibuckets()==400,"Repeated same-tick request does not deliver twice");h.succeed();
    }
    @GameTest(maxTicks=60) public void steamRecoveryMatchesTheNativeBoilerExpansion(GameTestHelper h){
        var p=JimtafactsComputerGameTests.player(h);var pos=new BlockPos(2,2,2);h.setBlock(pos,IndustrialFluidServices.BLOCKS.get("steam_condenser"));var node=h.getBlockEntity(pos, IndustrialFluidServices.Node.class);node.owner=p.getUUID();node.input.fill(IndustrialHeat.STEAM.fluid(),1000);
        h.runAfterDelay(21,()->{h.assertTrue(node.input.millibuckets()==0&&node.output.has(IndustrialFluidServices.DISTILLED.source(),100)&&node.output.millibuckets()==100,"1000 steam recovers exactly the 100 water used to create it");h.succeed();});
    }
    @GameTest public void barterChecksStockPaymentAndCapacityBeforeCommitting(GameTestHelper h){
        var owner=JimtafactsComputerGameTests.player(h);var buyer=JimtafactsComputerGameTests.player(h);var pos=new BlockPos(2,2,2);h.setBlock(pos,PersonalWorkshops.BLOCKS.get("barter_station"));var node=h.getBlockEntity(pos, PersonalWorkshops.Node.class);node.owner=owner.getUUID();node.published=true;node.price=new ItemStack(Items.EMERALD,2);node.offer=new ItemStack(Items.DIAMOND);node.setItem(0,new ItemStack(Items.DIAMOND,3));
        for(int i=0;i<36;i++)buyer.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));buyer.getInventory().setItem(0,new ItemStack(Items.EMERALD,3));
        h.assertTrue(!node.trade(buyer)&&buyer.getInventory().getItem(0).getCount()==3&&node.getItem(0).getCount()==3&&node.getItem(9).isEmpty(),"Full inventory leaves payment and stock intact");
        buyer.getInventory().setItem(1,ItemStack.EMPTY);h.assertTrue(node.trade(buyer),"One complete trade fits");
        h.assertTrue(buyer.getInventory().getItem(0).getCount()==1&&buyer.getInventory().getItem(1).is(Items.DIAMOND)&&node.getItem(0).getCount()==2&&node.getItem(9).getCount()==2,"Exactly two emeralds and one diamond move between the correct owners");
        h.assertTrue(node.getSlotsForFace(Direction.DOWN).length==0&&!node.template(buyer,0,ItemStack.EMPTY)&&!node.trade(buyer),"Public trading never exposes stock or configuration; same-tick repeats are refused");h.succeed();
    }
    @GameTest public void chargingVendorAndPrivateSafeKeepTheirInventoriesPrivate(GameTestHelper h){
        var owner=JimtafactsComputerGameTests.player(h);var buyer=JimtafactsComputerGameTests.player(h);var pos=new BlockPos(2,2,2);h.setBlock(pos,PersonalWorkshops.BLOCKS.get("energy_vendor"));var node=h.getBlockEntity(pos, PersonalWorkshops.Node.class);node.owner=owner.getUUID();node.published=true;node.price=new ItemStack(Items.EMERALD);node.energy.setAmount(1000);
        var battery=new ItemStack(IndustrialGear.ITEMS.get("jimta_re_battery"));buyer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,battery);buyer.getInventory().setItem(10,new ItemStack(Items.EMERALD));
        h.assertTrue(node.trade(buyer)&&node.energy.getAmount()==0&&Chargeable.energy(buyer.getMainHandItem())==1000&&node.getItem(9).is(Items.EMERALD),"One real payment buys exactly 1000 stored JE in the selected held battery");
        var safePos=new BlockPos(3,2,2);h.setBlock(safePos,PersonalWorkshops.BLOCKS.get("personal_safe"));var safe=h.getBlockEntity(safePos, PersonalWorkshops.Node.class);safe.owner=owner.getUUID();safe.setItem(0,new ItemStack(Items.DIAMOND));
        h.assertTrue(!safe.canOpen(buyer)&&safe.canOpen(owner)&&safe.getSlotsForFace(Direction.DOWN).length==0,"Unrelated buyers and hoppers cannot open or empty the personal safe");h.succeed();
    }
    @GameTest public void electricBoatUsesEquippedPowerAndOwnerAccess(GameTestHelper h){
        var player=JimtafactsComputerGameTests.player(h);var chest=new ItemStack(IndustrialGear.ITEMS.get("jimta_battery_pack"));Chargeable.setEnergy(chest,12);player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,chest);
        h.assertTrue(IndustrialBoats.Hull.payBoost(player)&&IndustrialBoats.Hull.payBoost(player)&&IndustrialBoats.Hull.payBoost(player)&&!IndustrialBoats.Hull.payBoost(player)&&Chargeable.energy(chest)==0,"Exactly three four-JE boosts; no free fourth acceleration");
        var hull=IndustrialBoats.TYPES.get("electric").create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);h.assertTrue(hull!=null,"Electric hull is registered");hull.owner=player.getUUID();
        h.assertTrue(hull.permitted(player)&&!hull.permitted(JimtafactsComputerGameTests.player(h)),"Unrelated player cannot drive or collect a private hull");h.assertTrue(hull.lavaHull(),"Only the electric variant enables lava flotation");h.succeed();
    }
    @GameTest(maxTicks = 60) public void electricBoatFloatsOnContainedLava(GameTestHelper h){
        for(int x=1;x<=5;x++)for(int z=1;z<=5;z++){h.setBlock(new BlockPos(x,0,z),net.minecraft.world.level.block.Blocks.OBSIDIAN);h.setBlock(new BlockPos(x,1,z),x==1||x==5||z==1||z==5?net.minecraft.world.level.block.Blocks.OBSIDIAN:net.minecraft.world.level.block.Blocks.LAVA);}
        var boat=IndustrialBoats.TYPES.get("electric").create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);var at=h.absolutePos(new BlockPos(3,2,3));boat.setInitialPos(at.getX()+.5,at.getY(),at.getZ()+.5);h.getLevel().addFreshEntity(boat);
        h.runAtTickTime(40,()->{h.assertTrue(boat.isAlive()&&boat.getY()>h.absolutePos(new BlockPos(3,1,3)).getY()+.3,"Electric boat remains afloat and intact in the bounded lava pool");boat.discard();h.succeed();});
    }
    @GameTest public void latheRefusesUnpaidOrBlockedCuts(GameTestHelper h){
        var player=JimtafactsComputerGameTests.player(h);var pos=new BlockPos(2,2,2);h.setBlock(pos,IndustrialLathe.BLOCK);
        var node=h.getBlockEntity(pos, IndustrialLathe.Node.class);node.owner=player.getUUID();
        var blank=new ItemStack(JimtaIndustry.MATERIALS.get("jimta_iron_turning_blank"));var tool=new ItemStack(IndustrialUtilityItems.ITEMS.get("lathing_tool"));node.setItem(1,blank);node.setItem(0,tool);
        h.assertTrue(!node.cut(0,player)&&IndustrialLathe.profile(blank)[0]==5&&tool.getDamageValue()==0,"No power means no changed profile or tool wear");
        node.acceptKinetic(Direction.WEST,10000);
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,blank,t->t.putInt("JimtaLatheCuts",1));
        node.setItem(2,new ItemStack(Items.COBBLESTONE,64));
        h.assertTrue(!node.cut(0,player)&&node.kinetic==10000&&tool.getDamageValue()==0,"A shaving that cannot fit pauses before consuming anything");
        node.setItem(2,ItemStack.EMPTY);h.assertTrue(node.cut(0,player)&&node.kinetic==9000&&IndustrialLathe.profile(blank)[0]==4&&tool.getDamageValue()==1&&node.getItem(2).is(Items.IRON_NUGGET),"One cut pays 1000 KE, one wear and emits exactly one due shaving");
        h.assertTrue(!node.cut(0,player)&&!node.cut(5,player)&&!node.cut(-1,player),"Same-tick repeat and out-of-range segment requests are rejected");
        var stranger=JimtafactsComputerGameTests.player(h);h.assertTrue(!node.permitted(stranger),"Lathe content is private to owner or party");
        int[] profile={4,3,4,3,4};net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,blank,t->{for(int i=0;i<5;i++)t.putInt("JimtaLathe"+i,profile[i]);});
        h.assertTrue(node.finish(player)&&net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(node.getItem(1).getItem()).getPath().equals("iron_shaft"),"Exact original fluted profile finishes one real shaft");
        h.assertTrue(!node.finish(player),"A completed part cannot be finished again");h.succeed();
    }
    @GameTest public void luminatorPaysForItsLight(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos.below(),net.minecraft.world.level.block.Blocks.IRON_BLOCK);h.setBlock(pos,IndustrialLighting.LUMINATOR);
        var node=h.getBlockEntity(pos, IndustrialLighting.Node.class);node.energy.setAmount(2);node.step();h.assertTrue(h.getBlockState(pos).getValue(IndustrialLighting.LIT)&&node.energy.getAmount()==1,"First four-tick light interval pays one JE");
        node.step();node.step();h.assertTrue(!h.getBlockState(pos).getValue(IndustrialLighting.LIT)&&node.energy.getAmount()==0,"Exhausted luminator switches off without negative storage");
        node.energy.setAmount(2);node.inverted=true;node.step();h.assertTrue(node.energy.getAmount()==2&&!h.getBlockState(pos).getValue(IndustrialLighting.LIT),"Inverted lamp waits for redstone without spending JE");h.succeed();
    }
    @GameTest public void backpacksSpendTheirOwnResources(GameTestHelper h) {
        var player=JimtafactsComputerGameTests.player(h);
        var pack=new ItemStack(IndustrialGear.ITEMS.get("jimta_biogas_jetpack"));
        var jet=(io.github.jimbozoomer.jugcraft.tools.Jetpack)pack.getItem();
        var biogas=net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(io.github.jimbozoomer.jugcraft.chemistry.PetroFluids.BIOGAS.fluid());
        pack.set(io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids.STORED_FLUID,new io.github.jimbozoomer.jugcraft.fluid.StoredFluid(biogas,IndustrialBackpacks.FUEL_PER_TICK*2));
        h.assertTrue(jet.hasThrustFuel(pack)&&jet.payThrust(pack)&&jet.payThrust(pack)&&!jet.payThrust(pack),"Exactly two paid fuel ticks; no phantom JE or third thrust");
        h.assertTrue(!pack.has(io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids.STORED_FLUID)&&!(pack.getItem() instanceof Chargeable),"Empty fuel pack cannot charge from a JE charger");
        var foam=new ItemStack(IndustrialGear.ITEMS.get("jimta_construction_foam_pack"));
        var canister=new ItemStack(io.github.jimbozoomer.jugcraft.chemistry.ConstructionChemistry.FOAM_CANISTER);
        canister.setDamageValue(canister.getMaxDamage()-7);
        h.assertTrue(IndustrialBackpacks.refillFoam(foam,canister)==7&&canister.isEmpty(),"Only seven remaining canister charges enter the backpack");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,foam);
        h.assertTrue(IndustrialBackpacks.consumeWornFoam(player,3)==3&&IndustrialBackpacks.wornFoam(player)==4,"Worn foam pack pays only actual blocks");
        h.assertTrue(IndustrialBackpacks.consumeWornFoam(player,100)==4&&IndustrialBackpacks.wornFoam(player)==0,"Foam consumption clamps to the saved supply");h.succeed();
    }
    @GameTest public void irrigationPaysOnlyForActualWorkAndNeverConsumesThePlan(GameTestHelper h) {
        var player=JimtafactsComputerGameTests.player(h);var machine=new BlockPos(2,2,2);var soil=new BlockPos(3,2,2);
        h.setBlock(machine,CivilWorkshops.BLOCKS.get("terraformer"));h.setBlock(soil,net.minecraft.world.level.block.Blocks.DIRT);
        h.setBlock(soil.above(),net.minecraft.world.level.block.Blocks.AIR);
        var node=h.getBlockEntity(machine, CivilWorkshops.Node.class);node.owner=player.getUUID();node.setArea(h.absolutePos(soil),h.absolutePos(soil));
        node.setItem(0,new ItemStack(Terraforming.PLANS.get("irrigation")));node.energy.setAmount(8192);node.running=true;
        node.step(h.getLevel());h.assertTrue(h.getBlockState(soil).is(net.minecraft.world.level.block.Blocks.DIRT)&&node.energy.getAmount()==8192,"Missing water pauses the machine before soil or power changes");
        node.fluid.fill(net.minecraft.world.level.material.Fluids.WATER,500);node.step(h.getLevel());
        h.assertTrue(h.getBlockState(soil).is(net.minecraft.world.level.block.Blocks.FARMLAND)&&node.fluid.millibuckets()==250&&node.energy.getAmount()==4096,"Preparing one block consumes 250 mB and 4096 JE");
        h.assertTrue(node.getItem(0).is(Terraforming.PLANS.get("irrigation"))&&node.getItem(0).getCount()==1,"Terraforming plans are reusable");
        node.cursor=0;node.running=true;node.step(h.getLevel());h.assertTrue(node.energy.getAmount()==4096&&node.fluid.millibuckets()==250,"Already moist soil costs no further resources");h.succeed();
    }
    @GameTest public void closedSplitterDoesNotStallTheLocalBranch(GameTestHelper h) {
        var level=h.getLevel();
        h.setBlock(new BlockPos(1,2,1),IndustrialCables.BLOCKS.get("copper"));
        h.setBlock(new BlockPos(2,3,1),net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        h.setBlock(new BlockPos(2,2,1),IndustrialCables.BLOCKS.get("splitter"));
        h.setBlock(new BlockPos(3,2,1),WorkshopTables.BLOCKS.get("charging"));
        h.setBlock(new BlockPos(1,2,2),WorkshopTables.BLOCKS.get("charging"));
        var remote=h.getBlockEntity(new BlockPos(3,2,1), WorkshopTables.Node.class);
        var local=h.getBlockEntity(new BlockPos(1,2,2), WorkshopTables.Node.class);
        var source=new io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage(2000,0,2000,()->{});source.setAmount(2000);
        long moved=io.github.jimbozoomer.jugcraft.energy.EnergyNetworks.pushToNeighbors(level,h.absolutePos(new BlockPos(0,2,1)),source,1000,java.util.List.of(Direction.EAST));
        h.assertTrue(moved>0&&local.energy.getAmount()==moved&&remote.energy.getAmount()==0&&source.getAmount()+moved==2000,"Closed splitter isolates only the far branch and preserves actual JE");
        h.setBlock(new BlockPos(2,3,1),net.minecraft.world.level.block.Blocks.AIR);
        io.github.jimbozoomer.jugcraft.energy.EnergyNetworks.pushToNeighbors(level,h.absolutePos(new BlockPos(0,2,1)),source,1000,java.util.List.of(Direction.EAST));
        h.assertTrue(remote.energy.getAmount()>0,"Removing redstone reconnects the remote table without rebuilding cables");h.succeed();
    }
    @GameTest(maxTicks=30) public void detectorReportsCommittedTrafficThenResets(GameTestHelper h) {
        var at=new BlockPos(2,2,2);h.setBlock(at,IndustrialCables.BLOCKS.get("detector"));
        h.setBlock(at.east(),WorkshopTables.BLOCKS.get("charging"));
        var source=new io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage(1000,0,1000,()->{});source.setAmount(1000);
        long moved=io.github.jimbozoomer.jugcraft.energy.EnergyNetworks.pushToNeighbors(h.getLevel(),h.absolutePos(at.west()),source,1000,java.util.List.of(Direction.EAST));
        h.assertTrue(moved>0&&h.getBlockState(at).getValue(IndustrialCables.ACTIVE),"Only committed JE transfer activates the detector");
        h.runAfterDelay(12,()->{h.assertTrue(!h.getBlockState(at).getValue(IndustrialCables.ACTIVE),"Idle detectors reset after ten ticks");h.succeed();});
    }
    @GameTest public void barrelAgeAndPortableContentsStayFinite(GameTestHelper h) {
        var pos = new BlockPos(2,2,2); h.setBlock(pos, BreweryBarrel.BLOCK);
        var barrel = h.getBlockEntity(pos, BreweryBarrel.Node.class);
        barrel.kind = 1; barrel.portions = 4; barrel.wheat = 4; barrel.hops = 2;
        barrel.age(24000); h.assertTrue(barrel.stage == 1 && barrel.age == 0, "Beer advances at the recovered loaded-time threshold");
        barrel.tap = true; barrel.locked = true; barrel.body = 3; barrel.bitterness = 3;
        barrel.age(24000); h.assertTrue(barrel.stage == 1 && barrel.age == 0, "A fitted tap stops further ageing");
        var restored = new BreweryBarrel.Node(barrel.getBlockPos(), barrel.getBlockState()); restored.restore(barrel.contents());
        h.assertTrue(restored.tap && restored.locked && restored.portions == 4 && restored.wheat == 4 && restored.hops == 2, "A moved barrel keeps its exact batch and fitted tap");
        for (int i = 0; i < 4; i++) h.assertTrue(restored.draw().is(BreweryBarrel.DRINKS.get("youngster")), "Every finite portion is an original-art drink");
        h.assertTrue(restored.draw().isEmpty() && restored.portions == 0 && restored.wheat == 0, "A fifth draw cannot duplicate the batch");
        restored.kind = 2; restored.portions = 8; restored.age = BreweryBarrel.Node.rumTime(8); restored.tap = true;
        restored.draw(); h.assertTrue(restored.age == BreweryBarrel.Node.rumTime(7), "Drawing a fully mature rum keeps the remaining seven portions mature");
        h.succeed();
    }
    @GameTest public void handToolsUseSharedMaterialsAndToolboxRejectsContainers(GameTestHelper h) {
        var plate = IndustrialUtilityItems.product("forge_hammer", new ItemStack(Items.IRON_INGOT,2));
        h.assertTrue(plate.getCount() == 1 && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(plate.getItem()).getPath().equals("iron_plate"), "Manual hammering feeds existing factory plates");
        h.assertTrue(IndustrialUtilityItems.product("forge_hammer",new ItemStack(Items.DIAMOND)).isEmpty(), "Unlisted inputs cannot create arbitrary resources");
        h.assertTrue(IndustrialUtilityItems.storable(new ItemStack(Items.IRON_PICKAXE)), "Ordinary tools fit in the toolbox");
        h.assertTrue(!IndustrialUtilityItems.storable(new ItemStack(IndustrialUtilityItems.ITEMS.get("toolbox"))) && !IndustrialUtilityItems.storable(new ItemStack(Items.SHULKER_BOX)), "Nested storage is refused");
        h.succeed();
    }
    @GameTest public void programmingConservesCardsAndWaitsForOutput(GameTestHelper h) {
        var player = JimtafactsComputerGameTests.player(h); var pos = new BlockPos(2,2,2);
        h.setBlock(pos, WorkshopTables.BLOCKS.get("programming"));
        var table = h.getBlockEntity(pos, WorkshopTables.Node.class); table.owner = player.getUUID();
        table.selected = WorkshopRobots.PROGRAMS.indexOf("planter");
        table.setItem(0, new ItemStack(WorkshopTables.BLANK_BOARD, 2));
        table.energy.setAmount(1999); table.step();
        h.assertTrue(table.getItem(0).getCount() == 2 && table.getItem(1).isEmpty(), "No partial card consumption before the full JE cost is available");
        table.energy.setAmount(4000); table.step();
        h.assertTrue(table.getItem(0).getCount() == 1 && table.getItem(1).is(WorkshopRobots.BOARDS.get("planter")) && table.energy.getAmount() == 2000, "One paid card becomes one selected program");
        table.selected = WorkshopRobots.PROGRAMS.indexOf("carrier"); table.step();
        h.assertTrue(table.getItem(0).getCount() == 1 && table.energy.getAmount() == 2000, "Different finished card blocks processing without loss");
        var save = table.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored = new WorkshopTables.Node(table.getBlockPos(), table.getBlockState());
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), save));
        h.assertTrue(restored.selected == table.selected && restored.owner.equals(player.getUUID()) && restored.getItem(0).getCount() == 1 && restored.energy.getAmount() == 2000, "Selection, real inventory, owner and JE survive saving");
        table.removeItem(1, 64); table.step();
        h.assertTrue(table.getItem(0).isEmpty() && table.getItem(1).is(WorkshopRobots.BOARDS.get("carrier")) && table.energy.getAmount() == 0, "Clearing the output resumes exactly one operation");
        h.succeed();
    }
    @GameTest public void chargingConservesJeAndOnlyAutomatesFinishedEquipment(GameTestHelper h) {
        var player = JimtafactsComputerGameTests.player(h); var pos = new BlockPos(2,2,2);
        h.setBlock(pos, WorkshopTables.BLOCKS.get("charging"));
        var table = h.getBlockEntity(pos, WorkshopTables.Node.class); table.owner = player.getUUID();
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().filter(i -> i instanceof Chargeable && Chargeable.capacity(new ItemStack(i)) >= 100).findFirst().orElseThrow();
        var tool = new ItemStack(item); Chargeable.setEnergy(tool, Math.max(0, Chargeable.capacity(tool) - 100));
        table.setItem(0, tool); table.energy.setAmount(1000);
        h.assertTrue(!table.canTakeItemThroughFace(0, tool, Direction.DOWN), "Automation cannot extract equipment still charging");
        long before = Chargeable.energy(tool); table.step();
        h.assertTrue(Chargeable.energy(tool) == before + 100 && table.energy.getAmount() == 900, "Only actual accepted JE is removed from the table");
        h.assertTrue(table.canTakeItemThroughFace(0, tool, Direction.DOWN), "Finished equipment may leave through the bottom");
        table.step(); h.assertTrue(table.energy.getAmount() == 900, "Full equipment never consumes standby JE");
        var stranger = JimtafactsComputerGameTests.player(h);
        h.assertTrue(!table.permitted(stranger), "An unrelated player cannot access the equipment");
        table.setRemoved();
        h.assertTrue(!table.canTakeItemThroughFace(0, tool, Direction.DOWN), "Cached automation stops when the physical table is removed"); h.succeed();
    }
}

package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.vehiclebay.*;
import io.github.jimbozoomer.jugcraft.machine.*;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;

public class VehicleBayGameTests {
    private static final String ARENA="jugcraft-test:arms_arena";
    private static VehicleBayBlockEntity place(GameTestHelper h,VehicleBayBlock block,Direction direction) {
        var pos=h.absolutePos(new BlockPos(9,2,9));var state=block.defaultBlockState().setValue(MachineBlock.FACING,direction);
        for(int x=-6;x<=6;x++)for(int y=0;y<7;y++)for(int z=-6;z<=6;z++)h.getLevel().setBlockAndUpdate(pos.offset(x,y,z),Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos,state);block.setPlacedBy(h.getLevel(),pos,state,null,ItemStack.EMPTY);
        return (VehicleBayBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    public static void supply(VehicleBayBlockEntity bay) {
        int slot=0;for(var need:bay.needs())bay.setItem(slot++,need.preview());bay.energy.setAmount(64000);
    }
    @GameTest(maxTicks=40)
    public void bothWorkbenchRecipesMatchAndWinTheRealRecipeLookup(GameTestHelper h) {
        for(var block:new VehicleBayBlock[]{VehicleBays.MECHA,VehicleBays.TANK}){
            var stacks=new java.util.ArrayList<ItemStack>();
            for(String id:new String[]{"jugcraft:steel_plate","minecraft:piston","jugcraft:steel_plate",
                    "jugcraft:diesel_engine","jugcraft:basic_circuit","jugcraft:diesel_engine",
                    "jugcraft:steel_block",block==VehicleBays.MECHA?"jugcraft:steel_block":"jugcraft:steel_plate","jugcraft:steel_block"})
                stacks.add(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id))));
            var grid=net.minecraft.world.item.crafting.CraftingInput.of(3,3,stacks);
            var selected=h.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,h.getLevel()).orElseThrow();
            h.assertTrue(selected.value().assemble(grid).is(block.asItem()),"Workbench recipe reachable without a colliding grid");
        }h.succeed();
    }
    @GameTest(structure=ARENA,maxTicks=40)
    public void everyVehicleAssemblesWithExactCostsAndRealFitout(GameTestHelper h) {
        for(int plan=0;plan<4;plan++){
            var bay=place(h,plan==3?VehicleBays.TANK:VehicleBays.MECHA,Direction.NORTH);
            bay.select(plan);bay.select(11);bay.select(21);supply(bay);
            h.assertTrue(bay.start(),"Starts vehicle "+plan);
            h.assertTrue(!bay.select(10)&&!bay.canPlaceItem(0,new ItemStack(Items.DIRT)),"Controls and inputs lock");
            for(int i=0;i<200;i++)bay.tick();
            var kit=bay.getItem(10);h.assertTrue(kit.is(VehiclePlans.product(plan).getItem())&&kit.getCount()==1,"One correct vehicle");
            h.assertTrue(VehicleFitout.read(kit).equals(new VehicleFitout(1,1)),"Paid upgrades present");
            h.assertTrue(bay.energy.getAmount()==51200,"Exactly 12,800 JE");
            for(int i=0;i<9;i++)h.assertTrue(bay.getItem(i).isEmpty(),"Exact material consumption");
            h.assertTrue(!bay.start(),"Output cannot be overwritten");
        }h.succeed();
    }
    @GameTest(structure=ARENA,maxTicks=40)
    public void powerPauseCancelAndSaveDoNotLoseOrDuplicateInputs(GameTestHelper h) {
        var bay=place(h,VehicleBays.MECHA,Direction.NORTH);supply(bay);bay.energy.setAmount(0);
        h.assertTrue(!bay.start()&&bay.status()==VehicleBayBlockEntity.POWER,"No power means no work");
        bay.energy.setAmount(640);h.assertTrue(bay.start(),"Can start with partial buffer");
        for(int i=0;i<30;i++)bay.tick();h.assertTrue(bay.progress()==10&&bay.getItem(10).isEmpty(),"Paid ticks only");
        var saved=bay.saveWithoutMetadata(h.getLevel().registryAccess());
        bay.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(bay.progress()==10&&bay.busy(),"Progress and lock survive reload");
        bay.cancel();h.assertTrue(!bay.busy()&&bay.getItem(0).getCount()==2&&bay.getItem(10).isEmpty(),"Cancel returns actual ingredients");
        bay.energy.setAmount(64000);h.assertTrue(bay.start(),"Restart");
        for(int i=0;i<200;i++)bay.tick();h.assertTrue(bay.getItem(10).getCount()==1&&bay.getItem(0).isEmpty(),"No duplicate output after reload/cancel");h.succeed();
    }
    @GameTest(structure=ARENA,maxTicks=40)
    public void refitPreservesKitFuelAndNameAndCannotCreateFreeVehicles(GameTestHelper h) {
        var bay=place(h,VehicleBays.TANK,Direction.WEST);
        var kit=new VehicleFitout(1,1).write(VehiclePlans.product(3),150);
        kit.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Owner tank"));
        bay.setItem(9,kit);bay.select(11);bay.select(22);supply(bay);
        h.assertTrue(bay.needs().stream().noneMatch(n->n.id().equals("#c:plates/steel")),"Existing armor not charged twice");
        h.assertTrue(bay.start(),"Refit starts");for(int i=0;i<100;i++)bay.tick();
        var out=bay.getItem(10);h.assertTrue(out.getHoverName().getString().equals("Owner tank")&&VehicleFitout.fuel(out)==150,"Fuel and custom name retained");
        h.assertTrue(VehicleFitout.read(out).equals(new VehicleFitout(1,2))&&bay.getItem(9).isEmpty(),"One modified kit");
        h.assertTrue(bay.energy.getAmount()==57600,"Refit costs 6,400 JE");
        bay.setItem(10,ItemStack.EMPTY);h.assertTrue(!bay.start(),"No kit no free refit");h.succeed();
    }
    @GameTest(structure=ARENA,maxTicks=40)
    public void rotatedFootprintsAndOnlyVisiblePowerPortConnect(GameTestHelper h) {
        for(var block:new VehicleBayBlock[]{VehicleBays.MECHA,VehicleBays.TANK})for(var facing:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}){
            var bay=place(h,block,facing);h.assertTrue(bay.structureIntact(),"Intact rotated bay");
            var fp=block.footprint(bay.getBlockState());
            h.assertTrue(fp.size()==(block==VehicleBays.MECHA?64:52),"Sparse footprint");
            h.assertTrue(fp.offsets().stream().mapToInt(Vec3i::getY).max().orElseThrow()+1==block.bayHeight,"Correct height");
            for(var side:Direction.values()){
                var port=EnergyStorage.SIDED.find(h.getLevel(),bay.getBlockPos(),side);
                h.assertTrue((port!=null)==(side==Footprint.rotate(Direction.EAST,facing)),"Only shown connector accepts power");
                if(port!=null)try(var tx=Transaction.openOuter()){h.assertTrue(port.insert(512,tx)==512,"Shared transactional power");tx.commit();}
            }
            h.assertTrue(EnergyStorage.SIDED.find(h.getLevel(),fp.partPos(bay.getBlockPos(),facing,1),Direction.UP)==null,"No phantom ports");
        }h.succeed();
    }
    @GameTest(structure=ARENA,maxTicks=40)
    public void missingMaterialsObstructionAndDisableNeverConsume(GameTestHelper h) {
        var bay=place(h,VehicleBays.MECHA,Direction.NORTH);bay.energy.setAmount(64000);
        h.assertTrue(!bay.start()&&bay.status()==VehicleBayBlockEntity.MATERIALS,"No empty assembly");supply(bay);
        var obstruct=bay.getBlockPos().offset(-2,2,2);h.getLevel().setBlockAndUpdate(obstruct,Blocks.STONE.defaultBlockState());
        h.assertTrue(!bay.start()&&bay.status()==VehicleBayBlockEntity.STRUCTURE,"Clearance blocked");h.getLevel().removeBlock(obstruct,false);
        JugcraftConfig.setFeatureEnabled("machines",false);
        try{h.assertTrue(!bay.start()&&bay.status()==VehicleBayBlockEntity.DISABLED,"Disabled acquisitions stop");}
        finally{JugcraftConfig.setFeatureEnabled("machines",true);}
        h.assertTrue(bay.energy.getAmount()==64000&&bay.getItem(0).getCount()==2,"No costs lost");h.succeed();
    }
    @GameTest(structure=ARENA,maxTicks=40)
    public void breakingPeripheralPartReturnsOneBayAndInputs(GameTestHelper h) {
        var bay=place(h,VehicleBays.MECHA,Direction.NORTH);supply(bay);bay.start();bay.tick();
        var pos=bay.getBlockPos();h.getLevel().destroyBlock(pos.offset(-5,3,5),true);
        h.assertTrue(h.getLevel().getBlockEntity(pos)==null,"Controller removed");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(7));
        int workbenches=0,plates=0,vehicles=0;
        for(var e:drops){var s=e.getItem();if(s.is(VehicleBays.MECHA.asItem()))workbenches+=s.getCount();
            if(s.is(VehiclePlans.needs(0,0,0,false,VehicleFitout.STANDARD).getFirst().preview().getItem()))plates+=s.getCount();
            if(VehiclePlans.plan(s)>=0)vehicles+=s.getCount();}
        h.assertTrue(workbenches==1&&plates==2&&vehicles==0,"Exactly one bay, actual inputs, no unearned vehicles: "+workbenches+"/"+plates+"/"+vehicles);h.succeed();
    }
    @GameTest(structure=ARENA,maxTicks=40)
    public void realPlacementRejectsBlockedEnvelopeWithoutPartialStructure(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.CREATIVE);player.setYRot(0);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(VehicleBays.TANK));
        var floor=new BlockPos(9,1,9);var absolute=h.absolutePos(floor);h.setBlock(floor,Blocks.STONE);
        h.setBlock(floor.offset(-2,2,2),Blocks.STONE);
        var hit=new BlockHitResult(Vec3.atCenterOf(absolute).add(0,.5,0),Direction.UP,absolute,false);
        h.useBlock(floor,player,hit);h.assertTrue(h.getBlockState(floor.above()).isAir(),"No partial placement");
        h.setBlock(floor.offset(-2,2,2),Blocks.AIR);h.useBlock(floor,player,hit);
        h.assertTrue(h.getBlockState(floor.above()).is(VehicleBays.TANK),"Clear bay places from its item");h.succeed();
    }
    @GameTest(structure=ARENA,maxTicks=40)
    public void kitPlacementInstallsActualVehicleStats(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var floor=new BlockPos(9,1,9);h.setBlock(floor,Blocks.STONE);var at=h.absolutePos(floor);
        for(int plan=0;plan<4;plan++) {
            for(var old:h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(7)))old.discard();
            var kit=new VehicleFitout(1,2).write(VehiclePlans.product(plan),123);player.setItemInHand(InteractionHand.MAIN_HAND,kit);
            h.useBlock(floor,player,new BlockHitResult(Vec3.atCenterOf(at).add(0,.5,0),Direction.UP,at,false));
            var vehicles=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class,new AABB(at).inflate(5),
                e->e instanceof io.github.jimbozoomer.jugcraft.walker.DieselWalker || e instanceof io.github.jimbozoomer.jugcraft.landship.Landship);
            h.assertTrue(vehicles.size()==1,"Kit really deploys vehicle "+plan);var vehicle=vehicles.getFirst();
            var save=net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());
            vehicle.saveWithoutId(save);
            if(vehicle instanceof io.github.jimbozoomer.jugcraft.walker.DieselWalker w){w.fitout=VehicleFitout.STANDARD;w.setFuel(0);}
            else {var tank=(io.github.jimbozoomer.jugcraft.landship.Landship)vehicle;tank.fitout=VehicleFitout.STANDARD;tank.setFuel(0);}
            vehicle.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),save.buildResult()));
            var fitout=vehicle instanceof io.github.jimbozoomer.jugcraft.walker.DieselWalker w?w.fitout:((io.github.jimbozoomer.jugcraft.landship.Landship)vehicle).fitout;
            int fuel=vehicle instanceof io.github.jimbozoomer.jugcraft.walker.DieselWalker w?w.fuel():((io.github.jimbozoomer.jugcraft.landship.Landship)vehicle).fuel();
            h.assertTrue(fitout.equals(new VehicleFitout(1,2))&&fuel==123,"Entity reload retains fuel and upgrades");
            h.assertTrue(Math.abs(fitout.speed()-.765)<.001&&fitout.toughness()==1.5,"Balanced real stat multipliers");
            h.assertTrue(fitout.fuelCost(1)+fitout.fuelCost(2)==1,"Economy fuel accounting");
            vehicle.hurtServer(h.getLevel(),h.getLevel().damageSources().playerAttack(player),10000);
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(7));
            h.assertTrue(drops.size()==1&&VehicleFitout.read(drops.getFirst().getItem()).equals(fitout)
                &&VehicleFitout.fuel(drops.getFirst().getItem())==123,"Survival recovery keeps paid fitout and fuel");
        }h.succeed();
    }
}

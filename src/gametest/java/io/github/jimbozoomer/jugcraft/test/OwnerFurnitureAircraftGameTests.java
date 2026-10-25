package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.aircraft.*;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.Vec3;

public class OwnerFurnitureAircraftGameTests {
    @GameTest public void sheetsUseThreadAndDoNotReplaceBackpackSleepingBags(GameTestHelper h){
        for(var color:net.minecraft.world.item.DyeColor.values()){
        var wool=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("minecraft:"+color.getName()+"_wool"));
        var grid=java.util.List.of(new ItemStack(wool),new ItemStack(wool),new ItemStack(wool),new ItemStack(Items.STRING),ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY);
        var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,grid);
        var result=h.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,h.getLevel()).orElseThrow().value().assemble(input);
        h.assertTrue(result.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id("crafted_"+color.getName()+"_sheet")))&&result.getCount()==8,"Wool and thread produces eight "+color+" furniture sheets");}
        h.succeed();
    }
    @GameTest public void everyAircraftFliesOnPaidPowerAndUnpoweredCraftDescends(GameTestHelper h){
        var level=h.getLevel();
        for(var spec:AircraftSpec.ALL.values()){
            var p=DeathGraveGameTests.player(h);var a=JugcraftAircraft.TYPES.get(spec.name()).create(level,EntitySpawnReason.COMMAND);var start=p.position().add(0,60,0);
            a.snapTo(start);a.setOwner(p.getUUID());level.addFreshEntity(a);p.startRiding(a);
            for(int i=0;i<spec.sockets().size();i++)if(spec.sockets().get(i).type().equals("boiler"))a.inventory().setItem(i,new ItemStack(Items.COAL,16));
            for(int i=0;i<80;i++){a.steer(p,new AircraftInput(1,0,1,false,false));a.tickCount++;a.tick();}
            h.assertTrue(a.power()>.5&&a.fuel()>0&&a.position().subtract(start).horizontalDistance()>.1,"Powered motion for "+spec.name());
            h.assertTrue(Double.isFinite(a.getX())&&Double.isFinite(a.getY())&&Double.isFinite(a.getZ()),"Finite flight for "+spec.name());p.stopRiding();a.discard();
            if(!spec.name().equals("gyrodyne")){
                var empty=JugcraftAircraft.TYPES.get(spec.name()).create(level,EntitySpawnReason.COMMAND);empty.snapTo(start);empty.setOwner(p.getUUID());level.addFreshEntity(empty);p.startRiding(empty);
                for(int i=0;i<20;i++){empty.steer(p,new AircraftInput(1,0,1,false,false));empty.tickCount++;empty.tick();}
                h.assertTrue(empty.power()==0&&empty.getY()<start.y,"No fuel means no free flight for "+spec.name());p.stopRiding();empty.discard();
            }
        }h.succeed();
    }
    @GameTest public void aircraftRoundsRespectWallsAndHaveFiniteLifetime(GameTestHelper h){
        var l=h.getLevel();var p=DeathGraveGameTests.player(h);var start=h.absoluteVec(new Vec3(2.5,12,2.5));
        for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)l.setBlock(net.minecraft.core.BlockPos.containing(start).offset(x,y,6),Blocks.STONE.defaultBlockState(),2);
        var round=new AircraftBullet(l,p);round.setPos(start);round.setDeltaMovement(0,0,4);l.addFreshEntity(round);round.tick();round.tick();
        h.assertTrue(round.isRemoved(),"Swept native collision stops a fast round at the wall");
        for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)h.assertTrue(l.getBlockState(net.minecraft.core.BlockPos.containing(start).offset(x,y,6)).is(Blocks.STONE),"Round does not damage terrain");
        var expired=new AircraftBullet(l,p);expired.setPos(start.add(0,30,0));expired.setNoGravity(true);l.addFreshEntity(expired);
        for(int i=0;i<81;i++)expired.tick();h.assertTrue(expired.isRemoved(),"Idle projectile expires");h.succeed();
    }
    @GameTest public void aircraftRepairsAndAmmunitionPersist(GameTestHelper h){
        var p=DeathGraveGameTests.player(h);var l=h.getLevel();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var a=JugcraftAircraft.TYPES.get("airship").create(l,EntitySpawnReason.COMMAND);a.snapTo(p.position().add(0,4,0));a.setOwner(p.getUUID());l.addFreshEntity(a);
        h.assertTrue(a.hurtServer(l,l.damageSources().playerAttack(p),4),"Owner may damage unoccupied aircraft");float before=a.healthFraction();
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);a.use(p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(a.healthFraction()>before&&!p.isPassenger(),"Empty-hand repair precedes mounting");
        int weapon=-1,cargo=-1;
        for(int i=0;i<a.spec().sockets().size();i++){String type=a.spec().sockets().get(i).type();if(type.equals("weapon"))weapon=i;if(type.equals("inventory"))cargo=i;}
        a.inventory().setItem(weapon,new ItemStack(JugcraftAircraft.ITEMS.get("rotary_cannon")));a.inventory().setItem(cargo,new ItemStack(Items.GUNPOWDER,2));p.startRiding(a);
        a.steer(p,new AircraftInput(0,0,0,true,false));a.tick();
        h.assertTrue(a.loadedRounds(weapon)==90&&a.inventory().getItem(cargo).getCount()==1,"One gunpowder loads ten cannon shots");
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());a.save(out);
        var copy=JugcraftAircraft.TYPES.get("airship").create(l,EntitySpawnReason.COMMAND);copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),out.buildResult()));
        h.assertTrue(copy.loadedRounds(weapon)==90,"Remaining ammunition survives restart");p.stopRiding();a.discard();h.succeed();
    }
    @GameTest public void aircraftCrossbowFiresNativeSpectralArrows(GameTestHelper h){
        var p=DeathGraveGameTests.player(h);var l=h.getLevel();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var a=JugcraftAircraft.TYPES.get("airship").create(l,EntitySpawnReason.COMMAND);a.snapTo(p.position().add(0,6,0));a.setOwner(p.getUUID());l.addFreshEntity(a);
        int weapon=-1,cargo=-1;
        for(int i=0;i<a.spec().sockets().size();i++){String t=a.spec().sockets().get(i).type();if(t.equals("weapon"))weapon=i;if(t.equals("inventory"))cargo=i;}
        a.inventory().setItem(weapon,new ItemStack(JugcraftAircraft.ITEMS.get("heavy_crossbow")));a.inventory().setItem(cargo,new ItemStack(Items.SPECTRAL_ARROW));p.startRiding(a);
        a.steer(p,new AircraftInput(0,0,0,true,false));a.tick();
        var arrows=l.getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.SpectralArrow.class,a.getBoundingBox().inflate(6),e->e.getOwner()==p);
        h.assertTrue(arrows.size()==2&&a.loadedRounds(weapon)==50,"Both authored barrels fire native spectral arrows; half an ammo item remains");
        for(var arrow:arrows){h.assertTrue(arrow.entityTags().contains("jugcraft_aircraft_arrow")&&arrow.pickup==net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.DISALLOWED,"Projectile permissions marker and no free recoverable ammo");arrow.discard();}
        p.stopRiding();a.discard();h.succeed();
    }
    @GameTest public void gyrodyneUsesPedalingAndAircraftSaveKeepsCargo(GameTestHelper h){
        var p=DeathGraveGameTests.player(h);var a=JugcraftAircraft.TYPES.get("gyrodyne").create(h.getLevel(),EntitySpawnReason.COMMAND);
        a.snapTo(p.position().add(0,3,0));a.setOwner(p.getUUID());h.getLevel().addFreshEntity(a);p.startRiding(a);
        for(int i=0;i<30;i++){a.steer(p,new AircraftInput(1,0,1,false,false));a.tickCount++;a.tick();}
        h.assertTrue(a.power()>.5,"Pedaling drives the Gyrodyne without a nonexistent boiler slot");
        p.getFoodData().setFoodLevel(0);a.tick();h.assertTrue(a.fuel()==0,"Hungry pilot cannot power the rotor");p.stopRiding();a.discard();
        var cargo=JugcraftAircraft.TYPES.get("cargo_airship").create(h.getLevel(),EntitySpawnReason.COMMAND);cargo.setOwner(p.getUUID());cargo.paint(0xAA3355);
        int slot=0;while(!cargo.spec().sockets().get(slot).type().equals("inventory"))slot++;
        var diamonds=new ItemStack(Items.DIAMOND,19);diamonds.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Saved cargo"));cargo.inventory().setItem(slot,diamonds);
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess());cargo.saveWithoutId(out);
        var restored=JugcraftAircraft.TYPES.get("cargo_airship").create(h.getLevel(),EntitySpawnReason.COMMAND);
        restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        h.assertTrue(ItemStack.matches(diamonds,restored.inventory().getItem(slot))&&p.getUUID().equals(restored.owner())&&restored.paint()==0xFFAA3355,"Cargo components, owner and paint survive save/load");h.succeed();
    }
    @GameTest public void stackedDecorAndBedHalvesDropCorrectly(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(2,2,2));
        var bowl=CraftedFurniture.BLOCKS.get("crafted_wood_bowl");
        var state=bowl.defaultBlockState();var prop=(net.minecraft.world.level.block.state.properties.Property)state.getBlock().getStateDefinition().getProperty("pieces");
        state=state.setValue(prop,(Comparable)prop.getValue("6").orElseThrow());
        var drops=Block.getDrops(state,h.getLevel(),p,null);
        h.assertTrue(drops.stream().filter(s->s.is(bowl.asItem())).mapToInt(ItemStack::getCount).sum()==6,"Six stacked bowls return six items");
        var bed=CraftedFurniture.BLOCKS.get("crafted_oak_fancy_bed").defaultBlockState();
        h.assertTrue(Block.getDrops(bed.setValue(BedBlock.PART,BedPart.FOOT),h.getLevel(),p,null).isEmpty(),"Foot does not duplicate bed item");
        h.assertTrue(Block.getDrops(bed.setValue(BedBlock.PART,BedPart.HEAD),h.getLevel(),p,null).stream().mapToInt(ItemStack::getCount).sum()==1,"Head drops one bed");h.succeed();
    }
    static String value(BlockState s,String key){var p=s.getBlock().getStateDefinition().getProperty(key);return s.getValue(p).toString();}
    @GameTest public void furnitureCatalogStorageAndConnectedTables(GameTestHelper h){
        h.assertTrue(CraftedFurniture.BLOCKS.size()==267,"All 267 supplied furniture blocks registered");
        var block=CraftedFurniture.BLOCKS.get("crafted_oak_table");var p=h.absolutePos(new BlockPos(2,2,2));
        h.getLevel().setBlockAndUpdate(p,block.defaultBlockState());h.getLevel().setBlockAndUpdate(p.east(),block.defaultBlockState());
        h.assertTrue(!value(h.getLevel().getBlockState(p),"shape").equals("single"),"Adjacent tables connect");
        h.getLevel().removeBlock(p.east(),false);h.assertTrue(value(h.getLevel().getBlockState(p),"shape").equals("single"),"Table disconnects when neighbor removed");
        h.getLevel().setBlockAndUpdate(p,CraftedFurniture.BLOCKS.get("crafted_oak_drawer").defaultBlockState());
        var be=h.getLevel().getBlockEntity(p);h.assertTrue(be instanceof net.minecraft.world.Container,"Drawers are usable storage");
        ((net.minecraft.world.Container)be).setItem(0,new ItemStack(Items.DIAMOND,7));
        h.assertTrue(((net.minecraft.world.Container)be).getItem(0).getCount()==7,"Furniture retains inventory");
        h.getLevel().setBlockAndUpdate(p,CraftedFurniture.BLOCKS.get("crafted_oven").defaultBlockState());
        h.assertTrue(h.getLevel().getBlockEntity(p) instanceof FurnaceBlockEntity,"Oven has native cooking logic");h.succeed();
    }
    @GameTest public void bedsAreRealBedsAndSeatsUseSharedSeating(GameTestHelper h){
        var bed=CraftedFurniture.BLOCKS.get("crafted_oak_fancy_bed");
        h.assertTrue(bed instanceof BedBlock,"Fancy beds use the real vanilla sleep/respawn implementation");
        h.assertTrue(CraftedFurniture.BLOCKS.get("crafted_oak_chair") instanceof io.github.jimbozoomer.jugcraft.tower.Sittable,"Chair uses shared sitting entity");
        var p=h.absolutePos(new BlockPos(2,2,2));
        var foot=bed.defaultBlockState().setValue(BedBlock.PART,BedPart.FOOT).setValue(BedBlock.FACING,Direction.NORTH);
        h.getLevel().setBlock(p,foot,2);h.getLevel().setBlock(p.north(),foot.setValue(BedBlock.PART,BedPart.HEAD),3);
        h.assertTrue(h.getLevel().getBlockState(p).is(bed),"Bed pair remains intact");h.succeed();
    }
    @GameTest public void aircraftLayoutsKeepEveryCargoAndEquipmentSlot(GameTestHelper h){
        h.assertTrue(JugcraftAircraft.TYPES.size()==7&&JugcraftAircraft.ITEMS.size()==27,"Every aircraft and component registered");
        for(var spec:AircraftSpec.ALL.values()){
            var a=JugcraftAircraft.TYPES.get(spec.name()).create(h.getLevel(),EntitySpawnReason.COMMAND);
            h.assertTrue(a.inventory().getContainerSize()==spec.sockets().size(),"Saved inventory matches supplied layout "+spec.name());
            for(var s:spec.sockets())h.assertTrue(s.x()>=0&&s.y()>=0&&s.x()+16<spec.width()&&s.y()+16<spec.inventoryY(),"Slot fits menu: "+spec.name());
        }
        h.assertTrue(AircraftSpec.ALL.get("cargo_airship").sockets().stream().filter(s->s.type().equals("inventory")).count()==80,"Full 80-slot cargo airship");h.succeed();
    }
    @GameTest public void aircraftOwnerFuelUpgradesAndStaleControls(GameTestHelper h){
        var level=h.getLevel();var p=DeathGraveGameTests.player(h);var other=DeathGraveGameTests.player(h);
        var a=JugcraftAircraft.TYPES.get("quadrocopter").create(level,EntitySpawnReason.COMMAND);
        a.snapTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(3,4,3))));a.setOwner(p.getUUID());level.addFreshEntity(a);p.startRiding(a);
        h.assertTrue(!a.mayUse(other),"Another player cannot open or pilot the craft");
        for(int i=0;i<a.spec().sockets().size();i++)if(a.spec().sockets().get(i).type().equals("boiler"))a.inventory().setItem(i,new ItemStack(Items.COAL,2));
        a.steer(other,new AircraftInput(1,1,1,false,false));a.tick();h.assertTrue(a.control(0)==0,"Passenger control spoof rejected");
        a.steer(p,new AircraftInput(1,0,1,false,false));for(int i=0;i<10;i++){a.tickCount++;a.tick();}
        h.assertTrue(a.fuel()>0&&a.power()>0,"Boiler consumes real fuel and spins engine");
        for(int i=0;i<12;i++){a.tickCount++;a.tick();}h.assertTrue(a.control(0)==0&&a.control(2)==0,"Lost input expires on server");
        h.assertTrue(!Aircraft.accepts("upgrade",new ItemStack(Items.DIAMOND)),"Upgrade slot rejects arbitrary items");
        p.stopRiding();a.discard();h.succeed();
    }
}

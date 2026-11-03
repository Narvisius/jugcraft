package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.FeastTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import io.github.jimbozoomer.jugcraft.raiders.JugcraftRaiders;
import io.github.jimbozoomer.jugcraft.raiders.RaiderRaids;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import java.time.MonthDay;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Focused festival suite; run separately because the raid test previews the server calendar. */
public final class FestivalRegressionGameTests {
    @GameTest
    public void exportFoodPlacementInventory(GameTestHelper h) {
        var out=System.getProperty("jugcraft.foodAudit");
        if(out==null) {h.succeed();return;}
        var entries=new com.google.gson.JsonArray();
        for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var stack=item.getDefaultInstance();
            var food=stack.get(net.minecraft.core.component.DataComponents.FOOD);
            if(food==null) continue;
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            var row=new com.google.gson.JsonObject();
            row.addProperty("id",id.toString());
            row.addProperty("class",item.getClass().getName());
            row.addProperty("nutrition",food.nutrition());
            if(item instanceof net.minecraft.world.item.BlockItem bi) {
                row.addProperty("item_block",net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(bi.getBlock()).toString());
                row.addProperty("block_class",bi.getBlock().getClass().getName());
            }
            row.addProperty("menu_dish",id.getNamespace().equals("jugcraft") && io.github.jimbozoomer.jugcraft.agriculture.MenuDishes.PLACED.stream().anyMatch(d->d.id().equals(id.getPath())));
            entries.add(row);
        }
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(out),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(entries));
        } catch(java.io.IOException e) {throw new RuntimeException(e);}
        h.succeed();
    }

    @GameTest
    public void holidayCalendar(GameTestHelper h) {
        new SeasonGameTests().thanksgivingTruceIsOnlyTheHoliday(h);
    }

    @GameTest(maxTicks=40)
    public void clothAndFoodSurviveChanges(GameTestHelper h) {
        new PartyTableGameTests().linenClothSwapsWithoutLosingFoodOrDuplicatingCloth(h);
    }

    @GameTest(maxTicks=30)
    public void foodPlacementAndPickup(GameTestHelper h) {
        new MenuGameTests().dishesSetDownAndTakenBack(h);
    }

    @GameTest(structure="jugcraft-test:drone_tower",maxTicks=40)
    public void longLinenTablesJoinAndSplitOnBothAxes(GameTestHelper h) {
        var block=JugcraftAgriculture.block("feast_table");
        for(var axis:java.util.List.of(Direction.Axis.X,Direction.Axis.Z)) {
            var start=new BlockPos(axis==Direction.Axis.X?1:20,2,axis==Direction.Axis.X?5:1);
            var state=block.defaultBlockState()
                .setValue(FeastTableBlock.AXIS,axis)
                .setValue(FeastTableBlock.RUNNER,FeastTableBlock.Runner.LINEN);
            for(int i=0;i<15;i++) {
                var at=start.offset(axis==Direction.Axis.X?i:0,0,axis==Direction.Axis.Z?i:0);
                h.setBlock(at.below(),Blocks.STONE);
                h.setBlock(at,state);
            }
            for(int i=0;i<15;i++) {
                var at=h.absolutePos(start.offset(axis==Direction.Axis.X?i:0,0,axis==Direction.Axis.Z?i:0));
                h.getLevel().setBlock(at,Block.updateFromNeighbourShapes(h.getLevel().getBlockState(at),h.getLevel(),at),Block.UPDATE_ALL);
            }
            var part=FeastTableBlock.PART;
            for(int i=0;i<15;i++) {
                var at=start.offset(axis==Direction.Axis.X?i:0,0,axis==Direction.Axis.Z?i:0);
                var expected=i==0?FeastTableBlock.Part.START:i==14?FeastTableBlock.Part.END:FeastTableBlock.Part.MIDDLE;
                h.assertTrue(h.getBlockState(at).getValue(part)==expected,"Connected linen table "+axis+" segment "+i);
            }
            var middle=start.offset(axis==Direction.Axis.X?7:0,0,axis==Direction.Axis.Z?7:0);
            h.getLevel().destroyBlock(h.absolutePos(middle),false);
            var direction=axis==Direction.Axis.X?Direction.EAST:Direction.SOUTH;
            h.assertTrue(h.getBlockState(middle.relative(direction.getOpposite())).getValue(part)==FeastTableBlock.Part.END,"Split creates new end");
            h.assertTrue(h.getBlockState(middle.relative(direction)).getValue(part)==FeastTableBlock.Part.START,"Split creates new start");
        }
        h.succeed();
    }

    @GameTest(maxTicks=80, environment="jugcraft-test:holiday_truce")
    public void holidayWithdrawsExistingRaidAndKeepsHorn(GameTestHelper h) {
        var level=h.getLevel();
        var server=level.getServer();
        var previous=JugcraftSeasons.settings().fixedDate();
        JugcraftSeasons.setFixedDate(server, MonthDay.of(11,1));
        var pos=h.absolutePos(new BlockPos(3,2,3));
        int before=RaiderRaids.raidLevel(level);
        var raid=RaiderRaids.start(level,pos,pos,1,RandomSource.create(9));
        h.assertTrue(raid!=null && !raid.members().isEmpty(), "Raid starts before the holiday");
        var members=raid.members();
        var holiday=SeasonCalendar.harvestFeastDay(JugcraftSeasons.settings().feast(), JugcraftSeasons.date().getYear());
        JugcraftSeasons.setFixedDate(server, MonthDay.from(holiday));
        try {
            h.assertTrue(!RaiderRaids.enabled(), "Automatic raids disabled on holiday");
            h.assertTrue(RaiderRaids.start(level,pos,1,RandomSource.create(9))==null, "Ordinary start refused");
            h.assertTrue(RaiderRaids.start(level,pos,pos,1,RandomSource.create(9))==null, "Explicit gather start refused");
            var player=h.makeMockServerPlayerInLevel();
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(JugcraftRaiders.RAID_HORN));
            JugcraftRaiders.RAID_HORN.use(level,player,InteractionHand.MAIN_HAND);
            h.assertTrue(player.getMainHandItem().is(JugcraftRaiders.RAID_HORN) && player.getMainHandItem().getCount()==1,"Truce does not consume the horn");
        } catch (RuntimeException | Error error) {
            RaiderRaids.withdraw(level,raid);
            JugcraftSeasons.setFixedDate(server,previous);
            throw error;
        }
        h.runAfterDelay(25,()->{
            try {
                h.assertTrue(!RaiderRaids.active(level,raid.id()), "Holiday ends existing raid");
                for(var id:members) h.assertTrue(level.getEntity(id)==null || level.getEntity(id).isRemoved(),"Loaded raider withdraws");
                h.assertTrue(RaiderRaids.raidLevel(level)==before,"Truce is not a victory or difficulty increase");
            } finally {
                RaiderRaids.withdraw(level,raid);
                JugcraftSeasons.setFixedDate(server,previous);
            }
            h.succeed();
        });
    }
}

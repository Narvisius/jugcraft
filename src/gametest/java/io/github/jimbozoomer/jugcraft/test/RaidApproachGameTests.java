package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.raiders.*;
import io.github.jimbozoomer.jugcraft.town.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class RaidApproachGameTests {
    @GameTest
    public void allApproachesUseRealGatesAndStayOutsideTheWall(GameTestHelper h) {
        var data = TownData.get();
        var centre = Town.centre(BlockPos.ZERO);
        for (var gate : CityRaidRoute.gates(BlockPos.ZERO)) {
            for (int d = -9; d <= 3; d++) for (int y = 1; y <= 3; y++) {
                var p = gate.passage().offset(gate.dx()*d, y-1, gate.dz()*d);
                var state = data.state(data.index(p.getX(), p.getY(), p.getZ()));
                h.assertTrue(state.getCollisionShape(h.getLevel(), p).isEmpty(), "Real gate passage is open: " + p);
            }
        }
        for (int degrees = 0; degrees < 360; degrees += 15) {
            double a = Math.toRadians(degrees);
            var from = new Vec3(centre.getX()+Math.cos(a)*130, 1, centre.getZ()+Math.sin(a)*130);
            var route = CityRaidRoute.approach(BlockPos.ZERO, from);
            var gate = CityRaidRoute.nearest(BlockPos.ZERO, from);
            h.assertTrue(route.getLast().equals(gate.inside()), "Route reaches a gate's inner road");
            for (int i=0; i<route.size()-3; i++) {
                var p = route.get(i);
                h.assertTrue(data.mask(p.getX(), p.getZ()) != TownData.INSIDE, "Outside approach cuts the wall: " + p);
            }
        }
        h.succeed();
    }

    @GameTest(structure="jugcraft-test:drone_tower", maxTicks=350, skyAccess=true)
    public void infantryMarchesUnderGateRoofInsteadOfOntoIt(GameTestHelper h) {
        var data = TownData.get();
        for(int x=0;x<40;x++) for(int z=0;z<40;z++) {
            h.setBlock(new BlockPos(x,0,z), Blocks.STONE);
            for(int y=1;y<22;y++) {
                int index = data.index(x+152,y,z+80);
                h.setBlock(new BlockPos(x,y,z), data.state(index));
            }
        }
        var objective = h.absolutePos(new BlockPos(6,1,16));
        var raid = RaiderRaids.start(h.getLevel(), objective, h.absolutePos(new BlockPos(34,1,16)), 1, RandomSource.create(1));
        RaiderInfantry officer = null;
        for(var id : raid.members()) {
            var entity=h.getLevel().getEntity(id);
            if(entity instanceof RaiderInfantry r && r.getType()==JugcraftRaiders.OFFICER) officer=r;
            else if(entity!=null) entity.discard();
        }
        h.assertTrue(officer!=null,"Raid includes an officer");
        var marching=officer;
        marching.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(34,1,16))));
        h.succeedWhen(()->{
            h.assertTrue(marching.getX()<h.absolutePos(new BlockPos(13,1,16)).getX(),"Officer crosses the authored east gate");
            h.assertTrue(marching.getY()<objective.getY()+2,"Officer remains on the street");
            RaiderRaids.withdraw(h.getLevel(),raid);
        });
    }
}

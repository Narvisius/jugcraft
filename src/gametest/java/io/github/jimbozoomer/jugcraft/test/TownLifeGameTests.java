package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.menagerie.*;
import io.github.jimbozoomer.jugcraft.town.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

public class TownLifeGameTests {
    @GameTest(maxTicks=180, padding=16)
    public void civiliansWalkToBedLateAndWakeWhileGuardsStayUp(GameTestHelper h) {
        var level=h.getLevel();
        var foot=h.absolutePos(new BlockPos(2,2,2));
        var facing=net.minecraft.core.Direction.SOUTH;
        var head=foot.relative(facing);
        for(int x=0;x<7;x++)for(int z=0;z<7;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
        level.setBlockAndUpdate(foot,TownData.parse("minecraft:blue_bed").setValue(BedBlock.FACING,facing).setValue(BedBlock.PART,BedPart.FOOT));
        level.setBlockAndUpdate(head,TownData.parse("minecraft:blue_bed").setValue(BedBlock.FACING,facing).setValue(BedBlock.PART,BedPart.HEAD));
        long[] clock={13000};
        var resident=new Townsfolk(JugcraftTown.TOWNSFOLK,level) {
            @Override protected long scheduleTime(){return clock[0];}
            @Override public BlockPos bed(){return head;}
        };
        resident.setUp(new TownData.Spot(-1,"townsfolk",null,"Test resident","townsfolk_1",BlockPos.ZERO),foot.offset(3,0,0));
        resident.snapTo(foot.getX()+3.5,foot.getY(),foot.getZ()+.5,0,0);level.addFreshEntity(resident);
        var guard=new Townsfolk(JugcraftTown.TOWNSFOLK,level) {
            @Override protected long scheduleTime(){return clock[0];}
            @Override public BlockPos bed(){return head;}
        };
        guard.setUp(new TownData.Spot(-1,"guard",null,"Test guard","guard_blue",BlockPos.ZERO),foot.offset(4,0,0));
        guard.snapTo(foot.getX()+4.5,foot.getY(),foot.getZ()+.5,0,0);level.addFreshEntity(guard);
        h.runAfterDelay(10,()->{h.assertTrue(!resident.isSleeping(),"Resident stays awake at sunset");clock[0]=18000;});
        h.runAfterDelay(120,()->{
            h.assertTrue(resident.isSleeping(),"Resident walks to and sleeps in the bed at midnight");
            h.assertTrue(!guard.isSleeping(),"Guard remains on duty");
            var resting=resident.position();
            resident.setDeltaMovement(new Vec3(.5,0,.5));resident.travel(new Vec3(1,0,1));
            h.assertTrue(resident.position().distanceToSqr(resting)<.0001 && resident.getDeltaMovement().lengthSqr()==0,"Sleeping residents cannot travel even with stale walking input");
            clock[0]=0;
        });
        h.runAfterDelay(150,()->{h.assertTrue(!resident.isSleeping(),"Resident wakes at dawn");resident.discard();guard.discard();h.succeed();});
    }

    @GameTest(maxTicks=2200)
    public void birdActuallyTakesOffCruisesAndLands(GameTestHelper h) {
        var level=h.getLevel();int x=92000,z=92000,cx=x>>4,cz=z>>4;
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)level.setChunkForced(cx+dx,cz+dz,true);
        level.getChunk(cx,cz);
        int floor=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,x,z);
        var bird=Menagerie.TYPES.get("wildlife_bird").create(level,EntitySpawnReason.COMMAND);
        bird.getRandom().setSeed(731);bird.setPersistenceRequired();bird.snapTo(x+.5,floor,z+.5,0,0);bird.setOnGround(true);level.addFreshEntity(bird);
        double[] high={floor};boolean[] done={false};
        Runnable clean=()->{bird.discard();for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)level.setChunkForced(cx+dx,cz+dz,false);};
        h.onEachTick(()->{
            if(done[0])return;
            high[0]=Math.max(high[0],bird.getY());
            if(high[0]>=floor+8 && bird.onGround() && bird.tickCount>200) {
                done[0]=true;clean.run();h.succeed();
            }
        });
        h.runAfterDelay(2100,()->{
            if(done[0])return;done[0]=true;String detail="peak="+(high[0]-floor)+", birdTicks="+bird.tickCount+", ground="+bird.onGround()+", position="+bird.blockPosition();clean.run();
            h.assertTrue(false,"Bird must take off, cruise overhead and return to ground: "+detail);
        });
    }

    @GameTest
    public void idleBirdsDescendInsteadOfHovering(GameTestHelper h) {
        for(String id:new String[]{"wildlife_bird","wildlife_vulture","beast_blue_jay","beast_bald_eagle","beast_seagull"}) {
            var bird=Menagerie.TYPES.get(id).create(h.getLevel(),EntitySpawnReason.COMMAND);
            var at=h.absolutePos(new BlockPos(2,25,2));bird.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
            bird.setNoGravity(true);bird.setOnGround(false);
            double before=bird.getY();
            for(int i=0;i<12;i++)bird.travel(Vec3.ZERO);
            h.assertTrue(!bird.isNoGravity() && bird.getY()<before-2,"Idle "+id+" descends, including old saved hover state");
            h.assertTrue(BirdFlightGoal.cruisingHeight(bird.species())>=10,"Bird cruises above head height");
            bird.discard();
        }
        h.assertTrue(!BirdFlightGoal.applies(Menagerie.SPECIES.get("wildlife_butterfly")) && !BirdFlightGoal.applies(Menagerie.SPECIES.get("fable_wisp")),"Insects and magical hovering creatures retain their behavior");
        h.succeed();
    }
}

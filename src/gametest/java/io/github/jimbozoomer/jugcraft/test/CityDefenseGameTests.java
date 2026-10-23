package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.artillery.*;
import io.github.jimbozoomer.jugcraft.raiders.*;
import io.github.jimbozoomer.jugcraft.town.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class CityDefenseGameTests {
    static Townsfolk guard(GameTestHelper h, Vec3 at) {
        var guard = h.spawnWithNoFreeWill(JugcraftTown.TOWNSFOLK, at);
        guard.setUp(new TownData.Spot(-1, "guard", null, "Test Guard", "guard_blue", BlockPos.ZERO), guard.blockPosition());
        return guard;
    }

    @GameTest
    public void raidersIgnorePlayersPetsAndCivilianRetaliation(GameTestHelper h) {
        var level=h.getLevel(); var player=h.makeMockServerPlayerInLevel();
        var civilian=h.spawnWithNoFreeWill(JugcraftTown.TOWNSFOLK, new BlockPos(2,2,2));
        var pig=h.spawnWithNoFreeWill(EntityTypes.PIG,new BlockPos(3,2,2));
        var guard=guard(h,new Vec3(4,2,2));
        for(var type:java.util.List.of(JugcraftRaiders.GRUNT,JugcraftRaiders.GUNNER,JugcraftRaiders.GRENADIER,JugcraftRaiders.WALKER,JugcraftRaiders.BLIMP)) {
            var raider=h.spawnWithNoFreeWill(type,new BlockPos(1,2,1));
            for(var safe:java.util.List.of(player,civilian,pig)) {
                h.assertTrue(!raider.canAttack(safe),"Raider cannot select a civilian/player/pet");
                raider.setTarget(safe); h.assertTrue(raider.getTarget()==null,"Manual/retaliation targets are filtered too");
                float before=safe.getHealth();
                safe.hurtServer(level,level.damageSources().mobAttack(raider),100);
                h.assertTrue(safe.getHealth()==before,"Raider damage cannot hurt a bystander");
            }
            h.assertTrue(raider.canAttack(guard),"City guard is a valid opponent");
            raider.setTarget(guard);h.assertTrue(raider.getTarget()==guard,"Guard combat is allowed");
            guard.hurtServer(level,level.damageSources().mobAttack(raider),10000);
            h.assertTrue(guard.getHealth()==guard.getMaxHealth(),"Guard cannot lose combat health");
        }
        h.succeed();
    }

    @GameTest(structure="jugcraft-test:drone_tower",maxTicks=180,skyAccess=true)
    public void cityFlakActuallyFiresWithoutInventoryAndSparesBystanders(GameTestHelper h) {
        for(int x=0;x<40;x++)for(int z=0;z<40;z++)h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        var guard=guard(h,new Vec3(8.5,1,8.5));
        guard.setUp(new TownData.Spot(9999,"flak_guard",null,"Crew","guard_blue",BlockPos.ZERO),guard.blockPosition());
        TownDefense.crew(h.getLevel(),guard);
        h.assertTrue(guard.getVehicle() instanceof FlakGun,"Guard boards an actual flak gun");
        var gun=(FlakGun)guard.getVehicle();gun.face(0);
        var raider=h.spawnWithNoFreeWill(JugcraftRaiders.BLIMP,new Vec3(8.5,10,28.5));
        raider.setNoGravity(true);
        var pig=h.spawnWithNoFreeWill(EntityTypes.PIG,new Vec3(9.5,10,28.5));pig.setNoGravity(true);
        var player=h.makeMockServerPlayerInLevel();
        h.assertTrue(!gun.hurtServer(h.getLevel(),h.getLevel().damageSources().playerAttack(player),10000),"Town gun cannot be salvaged/destroyed");
        h.assertTrue(AutomatedDefense.hostile(raider), "Raider airship is a hostile defense target");
        h.runAfterDelay(150, () -> io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.info("City flak regression: health={}, pitch={}, sinceShot={}, crew={}",
                raider.getHealth(), gun.aimPitch(), gun.sinceShot(), guard.getVehicle() == gun));
        h.succeedWhen(()->{
            h.assertTrue(raider.getHealth()<raider.getMaxHealth(),"Crew fires live shells and damages the raider");
            h.assertTrue(pig.getHealth()==pig.getMaxHealth(),"Nearby wildlife survives city flak");
            h.assertTrue(guard.getHealth()==guard.getMaxHealth(),"Crew remains healthy");
            raider.discard();pig.discard();guard.discard();gun.discard();
        });
    }

    @GameTest(structure="jugcraft-test:drone_tower",maxTicks=180,skyAccess=true)
    public void guardWalkerFiresAndCannotBeKilledInCombat(GameTestHelper h) {
        for(int x=0;x<40;x++)for(int z=0;z<40;z++)h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        var mecha=h.spawn(JugcraftTown.GUARD_MECHA,new Vec3(10.5,1,8.5));
        mecha.setUp(new TownData.Spot(-1,"mecha_guard",null,"Walker","guard_blue",BlockPos.ZERO),mecha.blockPosition());
        var raider=h.spawnWithNoFreeWill(JugcraftRaiders.BLIMP,new Vec3(10.5,9,28.5));raider.setNoGravity(true);
        mecha.hurtServer(h.getLevel(),h.getLevel().damageSources().mobAttack(raider),100000);
        h.assertTrue(mecha.getHealth()==mecha.getMaxHealth(),"Guard walker is combat invulnerable");
        h.runAfterDelay(150, () -> io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.info("City walker regression: health={}, pitch={}, sinceShot={}",
                raider.getHealth(), mecha.gunPitch(), mecha.sinceShot()));
        h.succeedWhen(()->{h.assertTrue(raider.getHealth()<raider.getMaxHealth(),"Walker fires functional cannon");mecha.discard();raider.discard();});
    }

    @GameTest
    public void towerFlakRejectsSteepFallbackAndWalkerTracksNetworkMovement(GameTestHelper h) {
        var gun=h.spawn(JugcraftArtillery.FLAK_GUN,new BlockPos(2,2,2));
        Vec3 pivot=gun.position().add(0,20,0);
        h.assertTrue(gun.solveDirect(pivot.add(0,-20,10),pivot)==null,"Nearby ground target cannot cause near-vertical fallback fire");
        h.assertTrue(gun.solveDirect(pivot.add(0,15,60),pivot)!=null,"Elevated airship has a direct firing solution");
        gun.discard();
        var walker=h.spawnWithNoFreeWill(JugcraftTown.GUARD_MECHA,new BlockPos(2,2,2));walker.setNoGravity(true);
        walker.tick();float before=walker.stride(1);
        walker.setPos(walker.getX()+.25,walker.getY(),walker.getZ());walker.tick();
        h.assertTrue(walker.stride(1)>before && walker.strideWeight(1)>0,"Movement between ticks animates the walker");
        before=walker.stride(1);walker.setPos(walker.getX()+100,walker.getY(),walker.getZ());walker.tick();
        h.assertTrue(walker.stride(1)==before,"Teleports do not create a giant stride");walker.discard();h.succeed();
    }

    @GameTest
    public void artilleryCanReach250AndBankHasUsableEnderChest(GameTestHelper h) {
        h.assertTrue(Ballistics.solve(JugcraftArtillery.HEAVY_SPEED,JugcraftArtillery.HEAVY_GRAVITY,245,0,true,45,85)!=null,"Heavy shells can reach 250 including barrel");
        h.assertTrue(Ballistics.solve(JugcraftArtillery.FLAK_SPEED,JugcraftArtillery.FLAK_GRAVITY,247,0,false,-5,85)!=null,"Flak has a 250-block solution");
        double travel=0,speed=JugcraftArtillery.FLAK_SPEED;
        for(int t=0;t<JugcraftArtillery.FLAK_FUSE;t++){travel+=speed;speed*=.99;}
        h.assertTrue(travel>250,"Flak fuse permits flight to maximum targeting range");
        var tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,io.github.jimbozoomer.jugcraft.Jugcraft.id("town_usable"));
        h.assertTrue(Blocks.ENDER_CHEST.defaultBlockState().is(tag),"Town visitors can use the bank ender chest");
        var data=TownData.get();boolean found=false;
        for(int x=0;x<data.size;x++)for(int z=0;z<data.size;z++)if(data.state(data.index(x,1,z)).is(Blocks.ENDER_CHEST))found=true;
        h.assertTrue(found,"Bank blueprint includes the ender chest");
        h.assertTrue(data.spots.stream().filter(s->s.role().equals("flak_guard")).count()==6,"Six staffed tower guns");
        h.assertTrue(data.spots.stream().filter(s->s.role().equals("mecha_guard")).count()==4,"Four guard walkers");
        h.succeed();
    }
}

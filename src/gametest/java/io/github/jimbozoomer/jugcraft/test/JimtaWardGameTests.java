package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Written for the final combined suite; not executed during content authoring. */
public final class JimtaWardGameTests {
    @GameTest(maxTicks = 120) public void heartstopAccumulatesAndRepaysOnceWhenCleansed(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);p.setHealth(20);p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(2,2,2))));
        // Newly created players retain vanilla loading invulnerability for 60 ticks.
        h.runAfterDelay(80,()->{
        p.addEffect(new MobEffectInstance(JimtaWardSpells.EFFECTS.get("heartstop"),200));
        p.hurtServer(h.getLevel(),p.damageSources().generic(),6);
        h.assertTrue(p.getHealth()==20&&p.getAttachedOrElse(JimtaWardSpells.HEART_DEBT,0F)==6,"Damage is held as persistent debt");
        p.removeEffect(JimtaWardSpells.EFFECTS.get("heartstop"));JimtaWardSpells.settleDebt(p);
        h.assertTrue(p.getHealth()==14&&p.getAttachedOrElse(JimtaWardSpells.HEART_DEBT,0F)==0,"Cleanse repays the entire debt");
        JimtaWardSpells.settleDebt(p);h.assertTrue(p.getHealth()==14,"Debt cannot be replayed");h.succeed();});
    }
    @GameTest public void invisibilityClearsAndRejectsEnemyTargetThenReleases(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(3,2,2))));var zombie=h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,2,2,2);zombie.setTarget(p);
        JimtaWardSpells.trueInvisibility(p,200);h.assertTrue(zombie.getTarget()==null&&JimtaWardSpells.concealed(p),"Caster and equipment are concealed, target cleared");
        zombie.setTarget(p);h.assertTrue(zombie.getTarget()==null,"A target cannot be immediately reacquired");
        p.removeEffect(JimtaWardSpells.EFFECTS.get("true_invisibility"));JimtaWardSpells.refreshConcealment(p);zombie.setTarget(p);
        h.assertTrue(!JimtaWardSpells.concealed(p),"Concealment clears when effect ends");h.assertTrue(zombie.getTarget()==p,"Normal targeting restores when effect ends");h.succeed();}finally{h.getLevel().getServer().getPlayerList().remove(p);}
    }
    @GameTest public void shieldPlaneUsesRotationAndDoesNotBlockUncrossedAttacks(GameTestHelper h){
        var e=JimtaWardEntity.TYPES.get("shield").create(h.getLevel(),EntitySpawnReason.MOB_SUMMONED);var center=Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(2,2,2)));e.snapTo(center.x,center.y,center.z,0,0);
        h.assertTrue(e.intersects(center.add(0,0,-2),center.add(0,0,2)),"Crossing the shield is blocked");
        h.assertTrue(!e.intersects(center.add(2,0,-2),center.add(2,0,2)),"Passing beside shield is allowed");
        h.assertTrue(!e.intersects(center.add(0,0,1),center.add(0,0,2)),"Same-side attacks are allowed");
        e.setYRot(90);h.assertTrue(e.intersects(center.add(-2,0,0),center.add(2,0,0)),"Yaw rotates the blocking plane");h.succeed();
    }
    @GameTest public void telekinesisRefusesPartyAndProtectedTargets(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();var animal=h.spawn(net.minecraft.world.entity.EntityTypes.COW,2,2,2);
        var protectedClaim=TestClaims.open().block(animal.blockPosition(),java.util.UUID.randomUUID());
        try{h.assertTrue(!JimtaWardSpells.permitted(p,animal),"Protected occupants cannot be forcibly moved");}finally{protectedClaim.close();}
        h.assertTrue(!JimtaWardSpells.permitted(p,p),"Caster is not a hostile target");h.succeed();
    }
    @GameTest public void originalPortalFramesBindPersistentlyWithoutSyncingDestinations(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var first=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));var second=h.absolutePos(new net.minecraft.core.BlockPos(2,2,6));
        for(var at:java.util.List.of(first,second)){var state=JimtaPortalFrames.BLOCK.defaultBlockState();h.getLevel().setBlock(at,state,3);JimtaPortalFrames.BLOCK.setPlacedBy(h.getLevel(),at,state,p,new net.minecraft.world.item.ItemStack(JimtaPortalFrames.ITEM));}
        p.snapTo(first.getX()+.5,first.getY(),first.getZ()-2,0,0);h.assertTrue(JimtaPortalFrames.cast(p),"First frame selected");
        p.snapTo(second.getX()+.5,second.getY(),second.getZ()-2,0,0);h.assertTrue(JimtaPortalFrames.cast(p),"Second frame bound");
        var a=JimtaPortalFrames.at(h.getLevel(),first);var b=JimtaPortalFrames.at(h.getLevel(),second);h.assertTrue(a.connected()&&b.connected(),"Both ends linked");
        var visual=a.getUpdateTag(h.getLevel().registryAccess());h.assertTrue(!visual.contains("owner")&&!visual.contains("identity")&&!visual.contains("link")&&!visual.contains("partner"),"Only connected/colour rendering data is public");
        a.unlink();h.assertTrue(!a.connected()&&!b.connected(),"Unlinking clears both loaded endpoints");h.succeed();
    }
}

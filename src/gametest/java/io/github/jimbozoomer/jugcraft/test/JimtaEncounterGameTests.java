package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;

/** Deferred until the final combined suite, as requested by the owner. */
public final class JimtaEncounterGameTests {
    @GameTest public void chargedCreeperBottlingConsumesChargeAndRespectsClaims(GameTestHelper h){
        var c=h.spawn(EntityTypes.CREEPER,2,2,2);c.setNoAi(true);
        var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);p.setPos(c.position());
        var hand=net.minecraft.world.InteractionHand.MAIN_HAND;
        p.setItemInHand(hand,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
        c.getEntityData().set(io.github.jimbozoomer.jugcraft.mixin.JimtaCreeperAccessor.jugcraft$charge(),true);
        try(var claim=TestClaims.open().block(c.blockPosition(),java.util.UUID.randomUUID())){
            JimtaWorkshops.captureLightning(p,hand,c);
            h.assertTrue(c.isPowered()&&p.getItemInHand(hand).is(net.minecraft.world.item.Items.GLASS_BOTTLE),"Claim denial preserves both charge and bottle");
        }
        JimtaWorkshops.captureLightning(p,hand,c);
        h.assertTrue(!c.isPowered()&&p.getItemInHand(hand).is(JimtaSpellbooks.ITEMS.get("lightning_bottle")),"One bottle captures one lightning charge");
        p.setItemInHand(hand,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
        JimtaWorkshops.captureLightning(p,hand,c);
        h.assertTrue(p.getItemInHand(hand).is(net.minecraft.world.item.Items.GLASS_BOTTLE),"Discharged creeper cannot be bottled twice");h.succeed();
    }
    @GameTest public void predatorsRespectVillagersAndClaimedAnimals(GameTestHelper h){
        var spider=h.spawn(JimtaEncounterCreatures.ICE_SPIDER,2,2,2);var keeper=h.spawn(JimtaEncounterCreatures.KEEPER,4,2,2);
        var villager=h.spawn(net.minecraft.world.entity.EntityTypes.VILLAGER,2,2,4);var cow=h.spawn(net.minecraft.world.entity.EntityTypes.COW,4,2,4);
        h.assertTrue(!spider.canAttack(villager)&&!keeper.canAttack(villager),"Source enemies retain Jugcraft villager protection");
        try(var claim=new NativeTestClaim(h,cow.blockPosition())){h.assertTrue(!spider.canAttack(cow),"Claimed livestock cannot be hunted");}
        h.succeed();
    }
    @GameTest public void hostileTombReleasesWhenItsSpiderDies(GameTestHelper h){
        var spider=h.spawn(JimtaEncounterCreatures.ICE_SPIDER,2,3,2);var p=JimtaRobotGameTests.player(h);try{
        p.setPos(spider.position().add(2,0,2));p.setHealth(10);JimtaSpiderTomb.entomb(spider,p);
        h.assertTrue(p.getVehicle() instanceof JimtaSpiderTomb,"Spider captures the player");var tomb=(JimtaSpiderTomb)p.getVehicle();
        spider.discard();tomb.tick();h.assertTrue(!p.isPassenger()&&tomb.isRemoved(),"Losing the owner releases the victim");h.assertTrue(p.getHealth()==10,"Hostile tomb never heals");h.succeed();}finally{h.getLevel().getServer().getPlayerList().remove(p);}
    }
    @GameTest public void restoredKeeperUsesTheOriginalRestoredWeapon(GameTestHelper h){
        var keeper=h.spawn(JimtaEncounterCreatures.KEEPER,2,2,2);keeper.setRestored(true);
        h.assertTrue(keeper.getMainHandItem().is(JimtaEncounterItems.ITEMS.get("legionnaire_flamberge")),"Restored state equips its source weapon");
        keeper.setRestored(false);h.assertTrue(keeper.getMainHandItem().is(JimtaEncounterItems.ITEMS.get("keeper_flamberge")),"Ordinary state equips the weathered weapon");h.succeed();
    }
}

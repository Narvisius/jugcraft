package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaWardEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Deferred portal cargo conservation and rejection scenarios for the combined batch. */
public final class JimtaTransitGameTests {
    @GameTest public void mountedOwnerTravelsWithoutLosingMountOrAllowingOtherOwners(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);var other=JimtaRobotGameTests.player(h);
        try{
            var from=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2,2,2)));var to=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5,2,2)));
            var type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("horse"));
            var horse=(net.minecraft.world.entity.animal.equine.AbstractHorse)type.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            horse.snapTo(from);horse.setTamed(true);horse.setOwner(other);h.getLevel().addFreshEntity(horse);
            h.assertTrue(player.startRiding(horse,true,true),"Player mounts the test horse");
            h.assertTrue(!JimtaWardEntity.transfer(player,horse,h.getLevel(),to,90),"Riding does not override another player's mount ownership");
            horse.setOwner(player);
            h.assertTrue(JimtaWardEntity.transfer(player,horse,h.getLevel(),to,90),"Owner and mount travel together");
            h.assertTrue(player.getRootVehicle()==horse&&horse.position().distanceToSqr(to)<.01,"Native movement retains riding relationship");
            h.assertTrue(!JimtaWardEntity.transfer(player,horse,h.getLevel(),from,0),"Mounted group shares portal cooldown");
            player.stopRiding();horse.discard();h.succeed();
        }finally{h.getLevel().getServer().getPlayerList().remove(player);h.getLevel().getServer().getPlayerList().remove(other);}
    }
    @GameTest public void ownerCargoMovesOnceAndCannotImmediatelyReturn(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var from=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2,2,2)));var to=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4,2,2)));
            var stack=new ItemStack(Items.DIAMOND,7);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Portal cargo"));
            var cargo=new ItemEntity(h.getLevel(),from.x,from.y,from.z,stack.copy());cargo.setThrower(player);cargo.setPickUpDelay(100);h.getLevel().addFreshEntity(cargo);
            var identity=cargo.getUUID();
            h.assertTrue(JimtaWardEntity.transfer(player,cargo,h.getLevel(),to,90),"Owner-thrown cargo can use a permitted portal");
            var moved=(ItemEntity)h.getLevel().getEntity(identity);
            h.assertTrue(moved!=null&&moved.position().distanceToSqr(to)<.01&&ItemStack.matches(stack,moved.getItem())&&moved.hasPickUpDelay(),"Identity, stack components and pickup delay survive the native move");
            h.assertTrue(!JimtaWardEntity.transfer(player,moved,h.getLevel(),from,0),"Shared cooldown prevents an immediate portal bounce");
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(from,to).inflate(1),e->e.getUUID().equals(identity)).size()==1,"Transit creates no second cargo entity");h.succeed();
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}
    }
    @GameTest public void unattributedCargoClaimsAndBlockedDestinationsNeverMoveItems(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var from=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2,2,2)));var at=h.absolutePos(new BlockPos(4,2,2));var to=Vec3.atBottomCenterOf(at);
            var cargo=new ItemEntity(h.getLevel(),from.x,from.y,from.z,new ItemStack(Items.EMERALD,3));h.getLevel().addFreshEntity(cargo);
            h.assertTrue(!JimtaWardEntity.transfer(player,cargo,h.getLevel(),to,0),"Unattributed world loot is not claimed by a portal owner");
            cargo.setThrower(player);
            try(var claim=TestClaims.open().block(at,java.util.UUID.randomUUID())){h.assertTrue(!JimtaWardEntity.transfer(player,cargo,h.getLevel(),to,0),"Destination claims reject even owner-thrown cargo");}
            h.setBlock(new BlockPos(4,2,2),Blocks.STONE);
            h.assertTrue(!JimtaWardEntity.transfer(player,cargo,h.getLevel(),to,0),"Blocked arrival rejects cargo");
            h.assertTrue(cargo.position().distanceToSqr(from)<.01&&cargo.getItem().getCount()==3&&!cargo.isOnPortalCooldown(),"All rejected moves preserve position, contents and cooldown");h.succeed();
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}
    }
}

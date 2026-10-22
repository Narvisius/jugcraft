package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.party.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.material.Fluids;

public final class ClaimProtectionGameTests {
    @GameTest
    public void starterAllowanceIsFreeSavedReusableAndCannotBeResold(GameTestHelper h) {
        var l=h.getLevel();var p=h.makeMockServerPlayerInLevel();
        p.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(8192,2,8192))));
        var land=new LandRegistry();var book=Ledger.get(l.getServer()).book();book.account(p.getUUID(),"StarterOwner");
        var cash=book.cash(p.getUUID());var first=LandRegistry.keyOf(p.blockPosition());
        int x=LandRegistry.keyX(first),z=LandRegistry.keyZ(first);
        for(int i=0;i<4;i++) {
            h.assertTrue(land.nextPrice(p.getUUID(),book)==0,"Starter land is shown as free");
            h.assertTrue(land.buy(p,book,LandRegistry.key(x+i,z),0,0).ok(),"Each of four starter chunks is free");
        }
        h.assertTrue(book.cash(p.getUUID())==cash&&land.freeRemaining(p.getUUID())==0,"No currency or loan required");
        h.assertTrue(land.nextPrice(p.getUUID(),book)==land.basePrice(),"Fifth chunk uses the ordinary paid base price");
        h.assertFalse(land.buy(p,book,LandRegistry.key(x+4,z),0,0).ok(),"A stale free quote cannot buy the fifth chunk");
        h.assertFalse(land.sell(p,book,first,10,0).ok(),"Starter claims cannot be flipped for money");
        h.assertFalse(land.auction(p,book,first,10,0,0).ok(),"Starter claims cannot be auctioned");
        h.assertFalse(land.zone(p,book,first,0,0).ok(),"Zoning cannot recycle a free allowance");
        var restored=LandRegistry.CODEC.parse(NbtOps.INSTANCE,LandRegistry.CODEC.encodeStart(NbtOps.INSTANCE,land).getOrThrow()).getOrThrow();
        h.assertTrue(restored.freeRemaining(p.getUUID())==0&&restored.claim(first).starter,"Allowance survives save/load");
        h.assertTrue(restored.abandon(p,book,first,false,0).ok(),"Owner can move their free claim");
        h.assertTrue(restored.freeRemaining(p.getUUID())==1&&restored.buy(p,book,LandRegistry.key(x+4,z),0,0).ok(),"Abandoning returns exactly one reusable slot");
        h.succeed();
    }

    @GameTest
    public void partySharingIsExplicitRevocableAndSaved(GameTestHelper h) {
        var l=h.getLevel();var owner=h.makeMockServerPlayerInLevel();var friend=h.makeMockServerPlayerInLevel();var stranger=h.makeMockServerPlayerInLevel();
        var p=h.absolutePos(new BlockPos(4096,2,4096));owner.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(p));
        var land=LandRegistry.get(l.getServer());var key=LandRegistry.keyOf(p);var book=Ledger.get(l.getServer()).book();
        var manager=JugcraftParties.manager();
        try {
            land.assign(book,key,book.account(owner.getUUID(),"ClaimOwner"),0);
            h.assertTrue(manager.create(owner.getUUID())==PartyManager.Result.OK,"Create party");
            manager.invite(owner.getUUID(),friend.getUUID(),System.currentTimeMillis());manager.accept(friend.getUUID(),System.currentTimeMillis());
            h.assertFalse(LandRegistry.mayBuild(friend,l,p),"Joining a party does not silently share private land");
            h.assertTrue(land.shareParty(owner,true).ok(),"Owner opts this claim into sharing");
            h.assertTrue(LandRegistry.mayBuild(friend,l,p),"Current party member can build");
            h.assertFalse(LandRegistry.mayBuild(stranger,l,p),"Outsider cannot build");
            h.assertFalse(land.abandon(friend,book,key,false,0).ok(),"Party member cannot release owner's land");
            var restored=LandRegistry.CODEC.parse(NbtOps.INSTANCE,LandRegistry.CODEC.encodeStart(NbtOps.INSTANCE,land).getOrThrow()).getOrThrow();
            h.assertTrue(Objects.equals(restored.claim(key).sharedParty,JugcraftParties.partyId(owner.getUUID()).orElseThrow()),"Party binding survives save/load");
            manager.kick(owner.getUUID(),friend.getUUID());
            h.assertFalse(LandRegistry.mayBuild(friend,l,p),"Kick revokes access immediately");
            land.assign(book,key,book.account(stranger.getUUID(),"NewOwner"),0);
            h.assertTrue(land.claim(key).sharedParty==null,"Ownership transfer clears old sharing");
        } finally { land.abandon(owner,book,key,true,0);manager.disband(owner.getUUID()); }
        h.succeed();
    }
    @GameTest
    public void claimsBlockExplosionsAndCrossBorderFluidsAndPistons(GameTestHelper h) {
        var l=h.getLevel();var p=h.makeMockServerPlayerInLevel();var book=Ledger.get(l.getServer()).book();var land=LandRegistry.get(l.getServer());
        var base=h.absolutePos(new BlockPos(6144,3,6144));
        var from=new BlockPos(Math.floorDiv(base.getX(),16)*16+15,base.getY(),Math.floorDiv(base.getZ(),16)*16+8);var to=from.east();
        long first=LandRegistry.keyOf(from),second=LandRegistry.keyOf(to);var account=book.account(p.getUUID(),"BoundaryOwner");
        try {
            land.assign(book,first,account,0);
            h.assertFalse(PistonBaseBlock.isPushable(Blocks.STONE.defaultBlockState(),l,from,Direction.EAST,false,Direction.EAST),"Piston cannot pull/push blocks over claim boundary");
            for(int dx=-1;dx<=2;dx++)for(int dz=-1;dz<=1;dz++) {
                var at=from.offset(dx,0,dz);l.setBlockAndUpdate(at.below(),Blocks.STONE.defaultBlockState());
                l.setBlockAndUpdate(at, dz!=0||dx<0||dx>1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            }
            l.setBlockAndUpdate(from,Blocks.WATER.defaultBlockState());
            Fluids.WATER.tick(l,from,l.getBlockState(from),l.getFluidState(from));
            h.assertTrue(l.getFluidState(to).isEmpty(),"Fluid cannot cross the boundary");
            land.assign(book,second,account,0);
            h.assertTrue(PistonBaseBlock.isPushable(Blocks.STONE.defaultBlockState(),l,from,Direction.EAST,false,Direction.EAST),"Same-owner neighbouring claims allow piston movement");
            Fluids.WATER.tick(l,from,l.getBlockState(from),l.getFluidState(from));
            h.assertFalse(l.getFluidState(to).isEmpty(),"Same-owner fluid plumbing still works");
            var protectedBlock=from.west(5).above(2);l.setBlockAndUpdate(protectedBlock,Blocks.OAK_PLANKS.defaultBlockState());
            l.explode(null,protectedBlock.getX()+.5,protectedBlock.getY()+1,protectedBlock.getZ()+.5,4,Level.ExplosionInteraction.TNT);
            h.assertTrue(l.getBlockState(protectedBlock).is(Blocks.OAK_PLANKS),"Explosion leaves claimed blocks intact");
            var fire=protectedBlock.above();l.setBlockAndUpdate(fire,Blocks.FIRE.defaultBlockState());
            l.getBlockState(fire).tick(l,fire,net.minecraft.util.RandomSource.create(42));
            h.assertTrue(l.getBlockState(fire).isAir()&&l.getBlockState(protectedBlock).is(Blocks.OAK_PLANKS),"Claim fire extinguishes before burning its support");
        } finally { land.abandon(p,book,first,true,0);land.abandon(p,book,second,true,0); }
        h.succeed();
    }
}

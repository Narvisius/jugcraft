package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialDynamite;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Deferred combined-batch coverage: remote identity/authority and bounded excavation. */
public final class JimtaDynamiteGameTests {
    @GameTest public void plasmaPreservesEnergyOnDeniedAndNonBlockOutputs(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var pos=new BlockPos(2,2,2);var at=h.absolutePos(pos);h.setBlock(pos,Blocks.SAND);
            var tool=new ItemStack(io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialGear.ITEMS.get("jimta_plasma_launcher"));io.github.jimbozoomer.jugcraft.tools.Chargeable.setEnergy(tool,3000);
            try(var claim=TestClaims.open().block(at,UUID.randomUUID())){h.assertTrue(!io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialPlasmaLauncher.heat(player,tool,at)&&io.github.jimbozoomer.jugcraft.tools.Chargeable.energy(tool)==3000,"Denied thermal change neither spends power nor alters the block");}
            h.assertTrue(io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialPlasmaLauncher.heat(player,tool,at)&&h.getBlockState(pos).is(Blocks.GLASS)&&io.github.jimbozoomer.jugcraft.tools.Chargeable.energy(tool)==2000,"One sand becomes one glass in place for exactly 1000 JE");
            h.setBlock(pos,Blocks.IRON_ORE);h.assertTrue(!io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialPlasmaLauncher.heat(player,tool,at)&&h.getBlockState(pos).is(Blocks.IRON_ORE),"Item-only furnace output cannot create a block or extra ore drops");
            h.setBlock(pos,Blocks.CHEST);h.assertTrue(!io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialPlasmaLauncher.heat(player,tool,at)&&io.github.jimbozoomer.jugcraft.tools.Chargeable.energy(tool)==2000,"Inventories cannot be heated and invalid targets do not spend power");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void remoteRechecksOwnerClaimAndChargeIdentity(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var pos=new BlockPos(2,2,2);h.setBlock(pos,IndustrialDynamite.BLOCK);var at=h.absolutePos(pos);player.setPos(Vec3.atCenterOf(at).add(0,0,3));
            var charge=h.getBlockEntity(pos,IndustrialDynamite.Charge.class);charge.owner=player.getUUID();charge.linked=true;
            var remote=new ItemStack(IndustrialDynamite.REMOTE);var link=new IndustrialDynamite.Link(GlobalPos.of(h.getLevel().dimension(),at),charge.identity);remote.set(IndustrialDynamite.LINKS,List.of(link));
            try(var claim=TestClaims.open().block(at,UUID.randomUUID())){h.assertTrue(IndustrialDynamite.fire(player,remote)==0&&charge.fuse==-1&&remote.get(IndustrialDynamite.LINKS).size()==1,"Denied charge remains unlit and its link is retained");}
            charge.owner=UUID.randomUUID();h.assertTrue(IndustrialDynamite.fire(player,remote)==0,"Possession of a linked remote does not grant another owner's charge");charge.owner=player.getUUID();
            var snapshot=charge.saveWithoutMetadata(h.getLevel().registryAccess());charge.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),snapshot));
            h.assertTrue(charge.identity.equals(link.charge())&&charge.linked,"Remote identity and ownership survive save/load");
            h.assertTrue(IndustrialDynamite.fire(player,remote)==1&&charge.fuse==40&&remote.get(IndustrialDynamite.LINKS).isEmpty(),"Authorized remote primes once with a two-second fuse");
            charge.fuse=-1;charge.linked=true;charge.identity=UUID.randomUUID();remote.set(IndustrialDynamite.LINKS,List.of(link));
            h.assertTrue(IndustrialDynamite.fire(player,remote)==0&&charge.fuse==-1&&remote.get(IndustrialDynamite.LINKS).isEmpty(),"Stale coordinates cannot ignite a replacement charge");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void blastSkipsContainersClaimsAndResistantBlocks(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var center=new BlockPos(3,3,3);var at=h.absolutePos(center);var chest=center.east();var protectedBlock=center.west();var stone=center.south();var obsidian=center.north();
            h.setBlock(chest,Blocks.CHEST);h.setBlock(protectedBlock,Blocks.STONE);h.setBlock(stone,Blocks.STONE);h.setBlock(obsidian,Blocks.OBSIDIAN);
            var inventory=h.getBlockEntity(chest,net.minecraft.world.level.block.entity.ChestBlockEntity.class);inventory.setItem(0,new ItemStack(Items.DIAMOND,7));
            player.setPos(Vec3.atCenterOf(at).add(0,5,0));
            try(var claim=TestClaims.open().block(h.absolutePos(protectedBlock),UUID.randomUUID())){IndustrialDynamite.blast(h.getLevel(),Vec3.atCenterOf(at),player.getUUID(),null);}
            h.assertTrue(h.getBlockState(stone).isAir(),"Exposed ordinary stone is excavated");
            h.assertTrue(h.getBlockState(protectedBlock).is(Blocks.STONE)&&h.getBlockState(obsidian).is(Blocks.OBSIDIAN),"Claimed and highly resistant blocks remain intact");
            h.assertTrue(h.getBlockState(chest).is(Blocks.CHEST)&&inventory.getItem(0).getCount()==7,"Container and its real cargo remain intact");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void nuclearChargeRequiresCompleteRingAndProtectsFuseCargo(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);
        try{
            var pos=new BlockPos(2,2,2);var at=h.absolutePos(pos);
            h.setBlock(pos,io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialNuclearCharge.BLOCK);
            var node=h.getBlockEntity(pos,io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialNuclearCharge.Node.class);
            node.owner=player.getUUID();player.setPos(Vec3.atCenterOf(at).add(0,0,3));
            node.setItem(0,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id("jimta_uranium_235"))));
            for(int slot=1;slot<8;slot++)node.setItem(slot,new ItemStack(IndustrialDynamite.INDUSTRIAL_TNT));
            h.assertTrue(!node.arm(player)&&node.fuse<0,"Seven charges cannot arm the ring");
            node.setItem(8,new ItemStack(IndustrialDynamite.INDUSTRIAL_TNT));
            try(var claim=TestClaims.open().block(at,UUID.randomUUID())){h.assertTrue(!node.arm(player),"Claim denial prevents arming");}
            h.assertTrue(node.arm(player)&&node.fuse==200,"Complete authorized charge arms for ten seconds");
            h.assertTrue(node.getSlotsForFace(Direction.DOWN).length==0&&!node.canTakeItemThroughFace(0,node.getItem(0),Direction.DOWN),"Hoppers cannot extract active core");
            h.assertTrue(node.disarm(player)&&node.loaded(),"Disarming keeps all nine ingredients");
            h.assertTrue(node.arm(player),"Disarmed charge can be rearmed");
            node.owner=UUID.randomUUID();node.tick();
            h.assertTrue(node.fuse<0&&node.loaded(),"Absent owner safely disarms without consuming inventory");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
}

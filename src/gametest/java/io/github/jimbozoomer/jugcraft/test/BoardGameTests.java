package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.boardgames.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
public final class BoardGameTests {
 private static TableEntity table(GameTestHelper h,boolean chess){var p=new BlockPos(2,2,2);h.setBlock(p,(chess?BoardGames.CHESS:BoardGames.CHECKERS).defaultBlockState());return h.getBlockEntity(p,TableEntity.class);}
 @GameTest(maxTicks=40) public void twoSeatsTurnsDrawAndConsensualRematch(GameTestHelper h){var t=table(h,true);var a=UUID.randomUUID();var b=UUID.randomUUID();var c=UUID.randomUUID();
  h.assertTrue(t.change(a,"Alice",0,0,0,0)&&t.change(b,"Bob",0,0,0,0),"Two players take seats");h.assertTrue(!t.change(c,"Third",0,0,0,0)&&!t.change(a,"Alice",0,0,0,0),"No third seat or double seating");
  h.assertTrue(!t.change(b,"Bob",1,52,36,0),"Wrong turn rejected");h.assertTrue(!t.change(c,"Third",1,12,28,0),"Spectator rejected");h.assertTrue(t.change(a,"Alice",1,12,28,0),"Legal e4 accepted");h.assertTrue(!t.change(a,"Alice",1,11,27,0),"Cannot move twice");
  h.assertTrue(t.change(a,"Alice",3,0,0,0)&&!t.change(a,"Alice",4,0,0,0),"Cannot accept own draw offer");h.assertTrue(t.change(b,"Bob",4,0,0,0)&&!t.active(),"Opponent accepts draw");
  h.assertTrue(t.change(a,"Alice",5,0,0,0)&&!t.active(),"One rematch vote insufficient");h.assertTrue(t.change(b,"Bob",5,0,0,0)&&t.active()&&t.game.ply==0,"Both votes reset board");
  h.assertTrue(t.change(a,"Alice",6,0,0,0)&&t.game.result.contains("Black wins"),"Leaving active match forfeits");h.succeed();
 }
 @GameTest(maxTicks=40) public void savedIdentityPositionAndJumpChainSurviveReload(GameTestHelper h){var t=table(h,true);var a=UUID.randomUUID();var b=UUID.randomUUID();t.change(a,"Alice",0,0,0,0);t.change(b,"Bob",0,0,0,0);t.change(a,"Alice",1,12,28,0);
  var tag=t.saveWithFullMetadata(h.getLevel().registryAccess());var restored=new TableEntity(t.getBlockPos(),t.getBlockState());restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag));
  h.assertTrue(restored.seat(a)==0&&restored.seat(b)==1&&restored.game.squares[28]==1&&restored.game.turn==-1&&restored.game.ep==20,"Seats and en passant survive reload");h.assertTrue(restored.instance.equals(t.instance),"Table identity stable on restart");h.assertTrue(restored.change(b,"Bob",1,52,36,0),"Reconnected player continues");
  var checkers=new TableEntity(t.getBlockPos(),BoardGames.CHECKERS.defaultBlockState());checkers.change(a,"Alice",0,0,0,0);checkers.change(b,"Bob",0,0,0,0);Arrays.fill(checkers.game.squares,0);checkers.game.squares[9]=1;checkers.game.squares[18]=-1;checkers.game.squares[36]=-1;checkers.game.squares[63]=-2;
  h.assertTrue(checkers.change(a,"Alice",1,9,27,0),"First chain capture");var saved=checkers.saveWithFullMetadata(h.getLevel().registryAccess());var resumed=new TableEntity(t.getBlockPos(),BoardGames.CHECKERS.defaultBlockState());resumed.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
  h.assertTrue(resumed.game.chain==27&&resumed.game.turn==1&&resumed.change(a,"Alice",1,27,45,0),"Saved chain resumes same player and piece");h.succeed();
 }
 @GameTest(maxTicks=40) public void networkRejectsUnopenedRemoteStaleAndForgedActions(GameTestHelper h){var t=table(h,true);var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);p.setPos(t.getBlockPos().getX()+2,t.getBlockPos().getY(),t.getBlockPos().getZ());
  var join=new BoardGames.Action(t.getBlockPos().asLong(),t.instance,0,0,0,0,0);t.act(p,join);h.assertTrue(t.seat(p.getUUID())==-1,"Unopened table refused");t.open(p);t.act(p,new BoardGames.Action(join.pos(),"wrong-instance",0,0,0,0,0));h.assertTrue(t.seat(p.getUUID())==-1,"Wrong instance refused");t.act(p,new BoardGames.Action(join.pos(),t.instance,99,0,0,0,0));h.assertTrue(t.seat(p.getUUID())==-1,"Stale revision refused");p.setPos(t.getBlockPos().getX()+30,t.getBlockPos().getY(),t.getBlockPos().getZ());t.act(p,join);h.assertTrue(t.seat(p.getUUID())==-1,"Remote request refused");p.setPos(t.getBlockPos().getX()+2,t.getBlockPos().getY(),t.getBlockPos().getZ());t.act(p,join);h.assertTrue(t.seat(p.getUUID())==0&&t.revision==1,"Nearby opened current request accepted");h.succeed();
 }
 @GameTest(maxTicks=40) public void tablesArePublicAndBreakDropsOnlyOneTable(GameTestHelper h){var t=table(h,false);h.assertTrue(io.github.jimbozoomer.jugcraft.town.TownProtection.publicBlock(h.getLevel(),t.getBlockPos(),t.getBlockState()),"Public town board has no shared inventory");var drops=Block.getDrops(t.getBlockState(),h.getLevel(),t.getBlockPos(),t);h.assertTrue(drops.size()==1&&drops.getFirst().is(BoardGames.CHECKERS.asItem())&&drops.getFirst().getCount()==1,"One table drop, no pieces/currency duplication");h.getLevel().destroyBlock(t.getBlockPos(),false);h.assertTrue(t.isRemoved(),"Broken match removed");h.succeed();}
}


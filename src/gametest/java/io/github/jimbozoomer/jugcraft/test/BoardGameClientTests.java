package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.boardgames.*;
import io.github.jimbozoomer.jugcraft.client.BoardGameScreen;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
public final class BoardGameClientTests implements FabricClientGameTest {
 private static void click(ClientGameTestContext c,double x,double y){c.runOnClient(m->m.gui.screen().mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(0,0)),false));c.waitTicks(10);}
 private static void button(ClientGameTestContext c,String label){c.clickScreenButton(label);c.waitTicks(10);}
 private static void square(ClientGameTestContext c,int q){var xy=c.computeOnClient(m->{var s=m.gui.screen();int cell=Math.max(12,Math.min(28,Math.min((s.height-70)/8,(s.width-180)/8)));int left=(s.width-8*cell-154)/2+8,top=(s.height-Math.max(186,8*cell)-60)/2+28;return new double[]{left+(q%8+.5)*cell,top+(7-q/8+.5)*cell};});click(c,xy[0],xy[1]);}
 public void runTest(ClientGameTestContext c){try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()){
  world.getConnection().waitForChunksRender();var server=world.getServer();var origin=c.computeOnClient(m->BlockPos.containing(m.player.position()));var chess=origin.offset(0,1,2);var checkers=origin.offset(2,1,2);var opponent=UUID.randomUUID();
  server.runCommand("gamerule minecraft:send_command_feedback false");server.runCommand("time set noon");server.runCommand("weather clear");
  server.runOnServer(s->{var l=s.overworld();for(int x=-4;x<6;x++)for(int z=-4;z<7;z++){l.setBlock(origin.offset(x,0,z),Blocks.SPRUCE_PLANKS.defaultBlockState(),3);for(int y=1;y<6;y++)l.removeBlock(origin.offset(x,y,z),false);}l.setBlock(chess,BoardGames.CHESS.defaultBlockState(),3);l.setBlock(checkers,BoardGames.CHECKERS.defaultBlockState(),3);});
  server.runCommand("tp @p %s %s %s 0 35".formatted(origin.getX()+1.5,origin.getY()+2,origin.getZ()-1.5));c.waitTicks(20);world.getConnection().waitForChunksRender();c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_F1);c.waitTicks(2);c.takeScreenshot("board_tables_world");c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_F1);
  server.runOnServer(s->((TableEntity)s.overworld().getBlockEntity(chess)).open(s.getPlayerList().getPlayers().getFirst()));c.waitForScreen(BoardGameScreen.class);c.waitTicks(5);c.takeScreenshot("chess_open_seats");button(c,"Take an open seat");c.takeScreenshot("chess_join_result");
  server.runOnServer(s->{var t=(TableEntity)s.overworld().getBlockEntity(chess);if(t.seat(s.getPlayerList().getPlayers().getFirst().getUUID())!=0)throw new AssertionError("Real UI join failed");t.change(opponent,"Test opponent",0,0,0,0);t.revision++;t.open(s.getPlayerList().getPlayers().getFirst());});c.waitTicks(5);
  button(c,"Optional wager");button(c,"Stake: 0 Jugs each");button(c,"Stake: 1 Jugs each");button(c,"Confirm 5 Jugs");c.takeScreenshot("chess_wager_confirmation");server.runOnServer(s->{var t=(TableEntity)s.overworld().getBlockEntity(chess);if(t.stake!=5||!t.agreed[0]||!t.betId.isEmpty())throw new AssertionError("Wager consent UI failed");});button(c,"Cancel / free play");button(c,"Back to game");
  square(c,12);c.takeScreenshot("chess_legal_destinations");square(c,28);
  server.runOnServer(s->{var t=(TableEntity)s.overworld().getBlockEntity(chess);if(t.game.squares[28]!=1||t.game.turn!=-1)throw new AssertionError("Real UI e4 move failed");});c.runOnClient(m->{if(((TableEntity)m.level.getBlockEntity(chess)).game.squares[28]!=1)throw new AssertionError("World pieces did not sync");});c.takeScreenshot("chess_after_e4");button(c,"Rules & help");c.takeScreenshot("chess_rules");button(c,"Back to game");
  server.runOnServer(s->{var t=(TableEntity)s.overworld().getBlockEntity(chess);t.game.reset();Arrays.fill(t.game.squares,0);t.game.squares[4]=6;t.game.squares[63]=-6;t.game.squares[48]=1;t.game.rights=0;t.revision++;t.open(s.getPlayerList().getPlayers().getFirst());});c.waitTicks(5);square(c,48);square(c,56);c.takeScreenshot("chess_promotion_choices");button(c,"Knight");server.runOnServer(s->{if(((TableEntity)s.overworld().getBlockEntity(chess)).game.squares[56]!=2)throw new AssertionError("Promotion UI failed");});
  c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);c.waitForScreen(null);
  server.runOnServer(s->{var t=(TableEntity)s.overworld().getBlockEntity(checkers);t.change(s.getPlayerList().getPlayers().getFirst().getUUID(),"Test player",0,0,0,0);t.change(opponent,"Test opponent",0,0,0,0);Arrays.fill(t.game.squares,0);t.game.squares[8]=1;t.game.squares[17]=-1;t.game.squares[35]=-1;t.game.squares[62]=-2;t.open(s.getPlayerList().getPlayers().getFirst());});c.waitForScreen(BoardGameScreen.class);c.waitTicks(5);square(c,8);square(c,26);c.takeScreenshot("checkers_forced_chain");square(c,26);square(c,44);server.runOnServer(s->{var t=(TableEntity)s.overworld().getBlockEntity(checkers);if(t.game.squares[44]!=1||t.game.turn!=-1||t.game.chain!=-1)throw new AssertionError("Checkers chain UI failed");});c.takeScreenshot("checkers_completed_chain");c.getInput().resizeWindow(640,480);c.waitTicks(10);button(c,"Rules & help");c.takeScreenshot("checkers_compact_rules");button(c,"Back to game");c.takeScreenshot("checkers_compact_board");button(c,"Optional wager");c.takeScreenshot("checkers_compact_wager");button(c,"Back to game");
  c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);c.waitForScreen(null);
 }}
}

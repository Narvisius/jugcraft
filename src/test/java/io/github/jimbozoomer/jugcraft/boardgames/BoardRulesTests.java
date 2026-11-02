package io.github.jimbozoomer.jugcraft.boardgames;
import java.util.*;
public final class BoardRulesTests {
 static int checks;
 static void ok(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static BoardPosition chess(String fen){var b=new BoardPosition(BoardPosition.Kind.CHESS);Arrays.fill(b.squares,0);String[] rows=fen.split("/");for(int r=0;r<8;r++){int x=0;for(char c:rows[r].toCharArray()){if(Character.isDigit(c)){x+=c-'0';continue;}int p=" pnbrqk".indexOf(Character.toLowerCase(c));b.squares[(7-r)*8+x++]=Character.isUpperCase(c)?p:-p;}}b.rights=0;b.ep=-1;b.repetitions.clear();return b;}
 static int sq(String s){return s.charAt(0)-'a'+8*(s.charAt(1)-'1');}
 static boolean move(BoardPosition b,String f,String t){return b.move(new BoardPosition.Move(sq(f),sq(t),0));}
 static long perft(BoardPosition b,int d){if(d==0)return 1;long total=0;for(var m:b.legal()){var c=b.copy();if(!c.move(m))throw new AssertionError();total+=perft(c,d-1);}return total;}
 public static void main(String[] args){
  var b=new BoardPosition(BoardPosition.Kind.CHESS);ok(b.legal().size()==20,"20 initial chess moves");ok(perft(b,2)==400,"perft 2");ok(perft(b,3)==8902,"perft 3 / king safety");
  ok(move(b,"f2","f3")&&move(b,"e7","e5")&&move(b,"g2","g4")&&move(b,"d8","h4"),"Fool's mate sequence");ok(b.result.contains("Black wins")&&b.result.contains("checkmate"),"Checkmate");ok(!move(b,"a2","a3"),"No move after game over");
  b=chess("4k3/8/8/8/8/8/8/R3K2R");b.rights=3;ok(b.legal().contains(new BoardPosition.Move(4,6,0))&&b.legal().contains(new BoardPosition.Move(4,2,0)),"Both castles legal");ok(move(b,"e1","g1")&&b.squares[5]==4&&b.squares[7]==0&&(b.rights&3)==0,"Castle moves rook and consumes rights");
  b=chess("4kr2/8/8/8/8/8/8/R3K2R");b.rights=3;ok(!b.legal().contains(new BoardPosition.Move(4,6,0)),"Cannot castle through attacked f1");
  b=chess("4k3/8/8/3pP3/8/8/8/4K3");b.ep=sq("d6");ok(move(b,"e5","d6")&&b.squares[sq("d5")]==0,"En passant removes passed pawn");
  b=chess("4r1k1/8/8/3pP3/8/8/8/4K3");b.ep=sq("d6");ok(!move(b,"e5","d6"),"Pinned en passant illegal");
  b=chess("7k/P7/8/8/8/8/8/4K3");ok(b.legal().stream().filter(m->m.from()==sq("a7")&&m.to()==sq("a8")).count()==4,"All promotion choices");ok(b.move(new BoardPosition.Move(sq("a7"),sq("a8"),2))&&b.squares[sq("a8")]==2,"Knight underpromotion");
  b=chess("7k/5K2/6Q1/8/8/8/8/8");b.turn=-1;ok(!b.inCheck(-1)&&b.legal().isEmpty(),"Stalemate not checkmate");
  b=new BoardPosition(BoardPosition.Kind.CHESS);for(int r=0;r<2;r++){ok(move(b,"g1","f3"),"Repetition knight out");ok(move(b,"g8","f6"),"Repetition knight out");ok(move(b,"f3","g1"),"Repetition knight back");ok(move(b,"f6","g8"),"Repetition knight back");}ok(b.result.contains("threefold"),"Threefold draw");
  b=new BoardPosition(BoardPosition.Kind.CHECKERS);ok(b.legal().size()==7,"Initial checkers seven moves");Arrays.fill(b.squares,0);b.squares[sq("b2")]=1;b.squares[sq("c3")]=-1;b.squares[sq("e5")]=-1;b.squares[sq("h8")]=-2;b.squares[sq("h2")]=1;
  ok(b.legal().equals(List.of(new BoardPosition.Move(sq("b2"),sq("d4"),0))),"Global compulsory capture");ok(move(b,"b2","d4")&&b.turn==1&&b.chain==sq("d4"),"Chain retains player and piece");ok(!move(b,"h2","g3"),"Cannot switch pieces during chain");ok(move(b,"d4","f6")&&b.turn==-1&&b.chain==-1&&b.squares[sq("e5")]==0,"Chain completion");
  b=new BoardPosition(BoardPosition.Kind.CHECKERS);Arrays.fill(b.squares,0);b.squares[sq("b6")]=1;b.squares[sq("c7")]=-1;b.squares[sq("e7")]=-1;ok(move(b,"b6","d8")&&b.squares[sq("d8")]==2&&b.turn==-1&&b.chain==-1,"Crowning ends turn even with backward jump");
  b=new BoardPosition(BoardPosition.Kind.CHECKERS);Arrays.fill(b.squares,0);b.squares[sq("d4")]=1;b.squares[sq("c3")]=-1;ok(!b.legal().contains(new BoardPosition.Move(sq("d4"),sq("b2"),0)),"Men cannot capture backwards");b.squares[sq("d4")]=2;ok(b.legal().contains(new BoardPosition.Move(sq("d4"),sq("b2"),0)),"Kings capture backwards");
  System.out.println("Board rules: "+checks+" assertions passed; chess perft 20 / 400 / 8902.");
 }
}

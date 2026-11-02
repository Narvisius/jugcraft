package io.github.jimbozoomer.jugcraft.boardgames;

import java.util.*;

/** Deterministic, server-owned chess and English draughts. Squares: a1=0, h8=63. */
public final class BoardPosition {
 public enum Kind { CHECKERS, CHESS }
 public record Move(int from,int to,int promote) {}
 public Kind kind;
 public int[] squares=new int[64];
 public int turn=1,rights=15,ep=-1,chain=-1,quiet=0,ply=0,lastFrom=-1,lastTo=-1;
 public String result="";
 public Map<String,Integer> repetitions=new HashMap<>();
 public BoardPosition(){this(Kind.CHECKERS);}
 public BoardPosition(Kind k){kind=k;reset();}
 public void reset(){Arrays.fill(squares,0);turn=1;rights=15;ep=chain=lastFrom=lastTo=-1;quiet=ply=0;result="";repetitions.clear();
  if(kind==Kind.CHESS){int[] back={4,2,3,5,6,3,2,4};for(int x=0;x<8;x++){squares[x]=back[x];squares[x+8]=1;squares[x+48]=-1;squares[x+56]=-back[x];}}
  else for(int y=0;y<8;y++)for(int x=0;x<8;x++)if((x+y)%2==1&&(y<3||y>4))squares[y*8+x]=y<3?1:-1;
  repetitions.put(key(),1);
 }
 public BoardPosition copy(){var b=new BoardPosition(kind);b.squares=squares.clone();b.turn=turn;b.rights=rights;b.ep=ep;b.chain=chain;b.quiet=quiet;b.ply=ply;b.lastFrom=lastFrom;b.lastTo=lastTo;b.result=result;b.repetitions=new HashMap<>(repetitions);return b;}
 public List<Move> legal(){if(!result.isEmpty())return List.of();return kind==Kind.CHECKERS?draughts():chess();}
 public boolean move(Move m){if(!legal().contains(m))return false;int moving=squares[m.from],captured=squares[m.to];int side=turn;
  if(kind==Kind.CHECKERS){boolean jump=Math.abs(m.to%8-m.from%8)==2;boolean crown=Math.abs(moving)==1&&(m.to/8==0||m.to/8==7);squares[m.from]=0;squares[m.to]=crown?side*2:moving;
   if(jump)squares[(m.from/8+m.to/8)/2*8+(m.from%8+m.to%8)/2]=0;
   quiet=jump||Math.abs(moving)==1?0:quiet+1;lastFrom=m.from;lastTo=m.to;ply++;
   chain=jump&&!crown?m.to:-1;
   if(chain>=0&&!jumps(chain).isEmpty())return true;
   chain=-1;turn=-turn;
  }else{raw(m);quiet=(Math.abs(moving)==1||captured!=0)?0:quiet+1;ply++;}
  if(legal().isEmpty())result=kind==Kind.CHESS&&!inCheck(turn)?"Draw — stalemate":(side==1?"White":"Black")+" wins"+(kind==Kind.CHESS?" — checkmate":" — no legal reply");
  if(result.isEmpty()&&kind==Kind.CHESS&&insufficient())result="Draw — insufficient material";
  if(result.isEmpty()&&quiet>=(kind==Kind.CHESS?100:80))result=kind==Kind.CHESS?"Draw — 50-move rule":"Draw — 40 moves without capture or man advance";
  if(quiet==0)repetitions.clear();
  if(result.isEmpty()&&repetitions.merge(key(),1,Integer::sum)>=3)result="Draw — threefold repetition";
  return true;
 }
 private List<Move> draughts(){var captures=new ArrayList<Move>();for(int i=0;i<64;i++)if(squares[i]*turn>0&&(chain<0||chain==i))captures.addAll(jumps(i));if(!captures.isEmpty()||chain>=0)return captures;
  var moves=new ArrayList<Move>();for(int i=0;i<64;i++)if(squares[i]*turn>0)for(int dy:steps(squares[i]))for(int dx:new int[]{-1,1}){int x=i%8+dx,y=i/8+dy;if(inside(x,y)&&squares[y*8+x]==0)moves.add(new Move(i,y*8+x,0));}return moves;
 }
 private int[] steps(int p){return Math.abs(p)==2?new int[]{-1,1}:new int[]{Integer.signum(p)};}
 private List<Move> jumps(int i){var out=new ArrayList<Move>();for(int dy:steps(squares[i]))for(int dx:new int[]{-1,1}){int x=i%8+dx,y=i/8+dy,X=x+dx,Y=y+dy;if(inside(X,Y)&&squares[y*8+x]*squares[i]<0&&squares[Y*8+X]==0)out.add(new Move(i,Y*8+X,0));}return out;}
 private List<Move> chess(){var out=new ArrayList<Move>();for(int i=0;i<64;i++)if(squares[i]*turn>0){int p=Math.abs(squares[i]),x=i%8,y=i/8;
  if(p==1){int Y=y+turn;if(inside(x,Y)&&squares[Y*8+x]==0){pawn(out,i,Y*8+x);int yy=y+turn*2;if(y==(turn==1?1:6)&&squares[yy*8+x]==0)pawn(out,i,yy*8+x);}
   for(int dx:new int[]{-1,1})if(inside(x+dx,Y)){int to=Y*8+x+dx;if(squares[to]*turn<0&&Math.abs(squares[to])!=6||to==ep&&squares[to]==0&&squares[to-turn*8]==-turn)pawn(out,i,to);}}
  else if(p==2){for(int[] d:KNIGHT)add(out,i,x+d[0],y+d[1]);}
  else{int[][] dirs=p==3?DIAGONAL:p==4?ORTHOGONAL:ALL;for(int[] d:dirs)for(int n=1;n<8;n++){int X=x+d[0]*n,Y=y+d[1]*n;if(!inside(X,Y))break;int q=squares[Y*8+X];add(out,i,X,Y);if(q!=0||p==6)break;}
   if(p==6&&i==(turn==1?4:60)&&!inCheck(turn)){int home=turn==1?0:56,mask=turn==1?1:4;
    if((rights&mask)!=0&&squares[home+7]==turn*4&&squares[home+5]==0&&squares[home+6]==0&&!attacked(home+5,-turn)&&!attacked(home+6,-turn))out.add(new Move(i,home+6,0));
    if((rights&(mask*2))!=0&&squares[home]==turn*4&&squares[home+1]==0&&squares[home+2]==0&&squares[home+3]==0&&!attacked(home+3,-turn)&&!attacked(home+2,-turn))out.add(new Move(i,home+2,0));
   }
  }
 }
 out.removeIf(m->{var b=copyWithoutHistory();b.raw(m);return b.inCheck(turn);});return out;}
 private BoardPosition copyWithoutHistory(){var b=new BoardPosition(Kind.CHECKERS);b.kind=kind;b.squares=squares.clone();b.turn=turn;b.rights=rights;b.ep=ep;return b;}
 private void add(List<Move> out,int from,int x,int y){if(inside(x,y)&&squares[y*8+x]*turn<=0&&Math.abs(squares[y*8+x])!=6)out.add(new Move(from,y*8+x,0));}
 private void pawn(List<Move> out,int from,int to){if(to/8==0||to/8==7)for(int p:new int[]{5,4,3,2})out.add(new Move(from,to,p));else out.add(new Move(from,to,0));}
 private void raw(Move m){int p=squares[m.from],side=Integer.signum(p),target=squares[m.to];squares[m.from]=0;
  if(Math.abs(p)==1&&m.to==ep&&target==0)squares[m.to-side*8]=0;
  squares[m.to]=m.promote!=0?side*m.promote:p;
  if(Math.abs(p)==6){rights&=side==1?12:3;if(Math.abs(m.to-m.from)==2){int rook=m.to>m.from?m.from+3:m.from-4;int to=m.to>m.from?m.from+1:m.from-1;squares[to]=squares[rook];squares[rook]=0;}}
  for(int sq:new int[]{0,7,56,63})if(m.from==sq||m.to==sq)rights&=~(sq==0?2:sq==7?1:sq==56?8:4);
  ep=Math.abs(p)==1&&Math.abs(m.to-m.from)==16?(m.from+m.to)/2:-1;lastFrom=m.from;lastTo=m.to;turn=-turn;
 }
 public boolean inCheck(int side){for(int i=0;i<64;i++)if(squares[i]==side*6)return attacked(i,-side);return true;}
 private boolean attacked(int at,int side){int X=at%8,Y=at/8;for(int i=0;i<64;i++)if(squares[i]*side>0){int p=Math.abs(squares[i]),dx=X-i%8,dy=Y-i/8;
  if(p==1&&dy==side&&Math.abs(dx)==1||p==2&&Math.abs(dx)*Math.abs(dy)==2||p==6&&Math.max(Math.abs(dx),Math.abs(dy))==1)return true;
  if((p==3||p==5)&&Math.abs(dx)==Math.abs(dy)&&dx!=0||(p==4||p==5)&&(dx==0^dy==0)){int sx=Integer.signum(dx),sy=Integer.signum(dy),x=i%8+sx,y=i/8+sy;boolean clear=true;while(x!=X||y!=Y){if(squares[y*8+x]!=0){clear=false;break;}x+=sx;y+=sy;}if(clear)return true;}
 }return false;}
 private boolean insufficient(){int count=0,bishops=0,color=-1;for(int i=0;i<64;i++){int p=Math.abs(squares[i]);if(p==0||p==6)continue;if(p==1||p==4||p==5)return false;count++;if(p==3){bishops++;int c=(i%8+i/8)%2;if(color<0)color=c;else if(color!=c)return false;}}
  return count<=1||bishops==count;
 }
 private String key(){int effective=-1;if(kind==Kind.CHESS&&ep>=0&&chess().stream().anyMatch(m->m.to==ep&&Math.abs(squares[m.from])==1))effective=ep;return Arrays.toString(squares)+":"+turn+":"+(kind==Kind.CHESS?rights:0)+":"+effective;}
 private static boolean inside(int x,int y){return x>=0&&x<8&&y>=0&&y<8;}
 private static final int[][] KNIGHT={{1,2},{2,1},{-1,2},{-2,1},{1,-2},{2,-1},{-1,-2},{-2,-1}},DIAGONAL={{1,1},{1,-1},{-1,1},{-1,-1}},ORTHOGONAL={{1,0},{-1,0},{0,1},{0,-1}},ALL={{1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,1},{0,-1}};
}

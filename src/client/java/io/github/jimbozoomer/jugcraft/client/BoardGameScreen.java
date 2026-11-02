package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.boardgames.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Walnut/brass board, clickable sculpted pixel pieces, server-provided legal destinations. */
public final class BoardGameScreen extends Screen {
 private final long pos;private BoardGames.Snapshot data;private int selected=-1,promoteTo=-1,cell,left,top,side,panelHeight;private boolean confirmResign,confirmLeave,showRules,showWager;
 public BoardGameScreen(long p,BoardGames.Snapshot d){super(Component.literal(d.position().kind==BoardPosition.Kind.CHESS?"Chess":"Checkers"));pos=p;data=d;}
 public static void register(){ClientPlayNetworking.registerGlobalReceiver(BoardGames.View.TYPE,(p,c)->{var mc=Minecraft.getInstance();if(p.json().isEmpty()){if(mc.gui.screen() instanceof BoardGameScreen s&&s.pos==p.pos())mc.gui.setScreen(null);return;}var d=BoardGames.JSON.fromJson(p.json(),BoardGames.Snapshot.class);if(p.open())mc.gui.setScreen(new BoardGameScreen(p.pos(),d));else if(mc.gui.screen() instanceof BoardGameScreen s&&s.pos==p.pos()&&s.data.instance().equals(d.instance())){boolean changed=s.data.revision()!=d.revision();s.data=d;if(changed){s.selected=s.promoteTo=-1;s.confirmLeave=s.confirmResign=false;s.rebuildWidgets();}}});}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){send(7,-1,-1,0);super.onClose();}
 private void send(int action,int from,int to,int promotion){ClientPlayNetworking.send(new BoardGames.Action(pos,data.instance(),data.revision(),action,from,to,promotion));}
 private boolean mine(){return data.active()&&data.seat()==(data.position().turn==1?0:1);}
 @Override protected void init(){cell=Math.max(12,Math.min(28,Math.min((height-70)/8,(width-180)/8)));panelHeight=Math.max(186,8*cell);left=(width-8*cell-154)/2+8;top=(height-panelHeight-60)/2+28;side=left+8*cell+16;
  int y=top+34;
  if(showRules){button("Back to game",top+panelHeight-20,()->{showRules=false;rebuildWidgets();},true);return;}
  if(showWager){boolean editable=data.seat()>=0&&!data.funded()&&data.position().ply==0&&data.position().result.isEmpty();
   button("Stake: "+data.stake()+" Jugs each",y,()->send(8,0,0,0),editable);
   button(data.funded()?"Stake funded":data.stake()==0?"Free game":data.seat()>=0&&data.agreed()[data.seat()]?"Confirmed":"Confirm "+data.stake()+" Jugs",y+22,()->send(9,0,0,0),editable&&data.stake()>0&&!data.agreed()[data.seat()]);
   button("Cancel / free play",y+44,()->send(10,0,0,0),editable);
   button("Back to game",y+66,()->{showWager=false;rebuildWidgets();},true);return;}
  if(promoteTo>=0){int[] pieces={5,4,3,2};String[] labels={"Queen","Rook","Bishop","Knight"};for(int i=0;i<4;i++){final int piece=pieces[i];button(labels[i],y+i*22,()->send(1,selected,promoteTo,piece),true);}button("Cancel",y+90,()->{promoteTo=-1;rebuildWidgets();},true);return;}
  button(data.seat()<0?"Take an open seat":"Seated: "+(data.seat()==0?"White":"Black"),y,()->send(0,-1,-1,0),data.seat()<0&&(data.names()[0].equals("Open seat")||data.names()[1].equals("Open seat")));
  button(confirmResign?"Confirm resignation":"Resign",y+22,()->{if(confirmResign)send(2,-1,-1,0);else{confirmResign=true;rebuildWidgets();}},data.seat()>=0&&data.active());
  boolean offered=data.drawOffer()==1-data.seat()&&data.seat()>=0;
  button(offered?"Accept draw":data.drawOffer()==data.seat()&&data.seat()>=0?"Draw offered":"Offer draw",y+44,()->send(offered?4:3,-1,-1,0),data.seat()>=0&&data.active()&&data.drawOffer()!=data.seat());
  button(data.seat()>=0&&data.ready()[data.seat()]?"Rematch requested":"Request rematch",y+66,()->send(5,-1,-1,0),data.seat()>=0&&!data.position().result.isEmpty()&&!data.ready()[data.seat()]);
  button(confirmLeave?"Confirm leave / forfeit":"Leave seat",y+88,()->{if(!data.active()||confirmLeave)send(6,-1,-1,0);else{confirmLeave=true;rebuildWidgets();}},data.seat()>=0);
  button("Rules & help",y+110,()->{showRules=true;rebuildWidgets();},true);
  button(data.stake()==0?"Optional wager":"Wager: "+data.stake()+" Jugs each",y+132,()->{showWager=true;rebuildWidgets();},true);
 }
 private void button(String text,int y,Runnable action,boolean enabled){var b=addRenderableWidget(Button.builder(Component.literal(text),v->action.run()).bounds(side,y,128,20).build());b.active=enabled;}
 private int square(int col,int row){return data.seat()==1?row*8+7-col:(7-row)*8+col;}
 @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){if(e.button()==0&&e.x()>=left&&e.x()<left+cell*8&&e.y()>=top&&e.y()<top+cell*8){if(showRules||showWager||!mine()||promoteTo>=0)return true;int q=square((int)(e.x()-left)/cell,(int)(e.y()-top)/cell);var candidates=data.moves().stream().filter(m->m.from()==selected&&m.to()==q).toList();if(!candidates.isEmpty()){if(candidates.getFirst().promote()!=0){promoteTo=q;rebuildWidgets();}else send(1,selected,q,0);}else selected=data.position().squares[q]*data.position().turn>0?q:-1;return true;}return super.mouseClicked(e,twice);}
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float d){super.extractBackground(g,mx,my,d);g.fill(left-10,top-25,side+138,top+panelHeight+28,0xFF2C1B16);g.fill(left-8,top-23,side+136,top+panelHeight+26,0xFFB88A43);g.fill(left-6,top-21,side+134,top+panelHeight+24,0xFF493226);if(showRules||showWager)return;g.fill(left-2,top-2,left+8*cell+2,top+8*cell+2,0xFF211713);
  for(int row=0;row<8;row++)for(int col=0;col<8;col++){int q=square(col,row),x=left+col*cell,y=top+row*cell;int color=(q%8+q/8)%2==(data.position().kind==BoardPosition.Kind.CHESS?1:0)?0xFFE4CDA3:0xFF77583F;g.fill(x,y,x+cell,y+cell,color);
   if(q==data.position().lastFrom||q==data.position().lastTo)g.fill(x+1,y+1,x+cell-1,y+cell-1,0x60409EA0);
   if(q==selected){g.fill(x,y,x+cell,y+2,0xFFFFD76B);g.fill(x,y+cell-2,x+cell,y+cell,0xFFFFD76B);g.fill(x,y,x+2,y+cell,0xFFFFD76B);g.fill(x+cell-2,y,x+cell,y+cell,0xFFFFD76B);}
   if(selected>=0&&data.moves().stream().anyMatch(m->m.from()==selected&&m.to()==q)){g.fill(x+1,y+1,x+cell-1,y+3,0xFF3C9858);g.fill(x+1,y+cell-3,x+cell-1,y+cell-1,0xFF3C9858);g.fill(x+1,y+1,x+3,y+cell-1,0xFF3C9858);g.fill(x+cell-3,y+1,x+cell-1,y+cell-1,0xFF3C9858);}
   int piece=data.position().squares[q];if(piece!=0)drawPiece(g,x,y,cell,piece,data.position().kind==BoardPosition.Kind.CHECKERS);
  }
 }
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){super.extractRenderState(g,mx,my,delta);g.text(font,title,left,top-17,0xFFFFE5AF,false);
  if(!showRules&&!showWager){for(int i=0;i<8;i++){int q=square(i,7);g.text(font,""+(char)('a'+q%8),left+i*cell+cell/2-2,top+cell*8+3,0xFFEAD8B8,false);}
  for(int row=0;row<8;row++)g.text(font,""+(square(0,row)/8+1),left-7,top+row*cell+cell/2-4,0xFFEAD8B8,false);
  for(int i=0;i<2;i++)g.text(font,(i==0?"W: ":"B: ")+font.plainSubstrByWidth(data.names()[i],100),side,top+i*12,0xFFF1E4C9,false);
  }
  String status=!data.position().result.isEmpty()?data.position().result:!data.active()?(data.stake()>0?"Confirm wager or choose free play":"Waiting for two players"):data.position().chain>=0?"Continue the jump":(data.position().turn==1?"White":"Black")+" to move"+(data.position().kind==BoardPosition.Kind.CHESS&&data.position().inCheck(data.position().turn)?" — check!":"");
  g.text(font,font.plainSubstrByWidth(status,8*cell+142),left,top+panelHeight+15,0xFFFFE5AF,false);
  if(showWager){String info="Optional Jugs wager\n\n"+"White: "+data.names()[0]+(data.agreed()[0]?" (confirmed)":"")+"\nBlack: "+data.names()[1]+(data.agreed()[1]?" (confirmed)":"")+"\n\n"+data.wagerMessage()+"\n\nBoth confirm before paying. Winner takes pot. Draw: refund. Leave/resign: lose stake. Disconnect: resume. Refund after 7 days. Rematch: free.";int y=top+4;for(var line:font.split(Component.literal(info),8*cell)){g.text(font,line,left,y,0xFFEAD8B8,false);y+=9;}}
  if(showRules){String rules=data.position().kind==BoardPosition.Kind.CHESS?"Click a piece and a green destination. Includes castling, en passant and promotion. Automatic draws: repetition (3), 50 moves, dead material.":"English checkers: captures compulsory. Continue all jumps with the same piece. Men move/capture forward. Crowning ends the turn. Draw: repetition (3) or 40 quiet moves each.";
   rules+=" Closing keeps your seat. Leaving forfeits. Both players must agree to rematch.";
   g.fill(left,top,side+128,top+panelHeight-24,0xFF493226);int y=top+10;for(var line:font.split(Component.literal(rules),8*cell+120)){g.text(font,line,left+6,y,0xFFEAD8B8,false);y+=12;}
  }
 }
 private static final String[] PIECES={"","00011000/00111100/00111100/00011000/00011000/00111100/01111110","00111000/01111100/11101100/11111100/00011100/00111100/01111110","00011000/00111100/00110100/00011000/00011000/00111100/01111110","11011011/11111111/01111110/00111100/00111100/01111110/11111111","10100101/11111111/01111110/00111100/00111100/01111110/11111111","00011000/00111100/00011000/00111100/00111100/01111110/11111111"};
 static void drawPiece(GuiGraphicsExtractor g,int x,int y,int cell,int p,boolean checkers){int light=p>0?0xFFFFF0CD:0xFF433B3A,shade=p>0?0xFFBDA77E:0xFF17191D;int unit=Math.max(1,(cell-4)/8),size=unit*8,X=x+(cell-size)/2,Y=y+(cell-unit*7)/2;
  String[] rows=(checkers?"00111100/01111110/11111111/11111111/11111111/01111110/00111100":PIECES[Math.abs(p)]).split("/");
  for(int r=0;r<rows.length;r++)for(int c=0;c<8;c++)if(rows[r].charAt(c)=='1')g.fill(X+c*unit+1,Y+r*unit+2,X+(c+1)*unit+1,Y+(r+1)*unit+2,shade);
  for(int r=0;r<rows.length;r++)for(int c=0;c<8;c++)if(rows[r].charAt(c)=='1')g.fill(X+c*unit,Y+r*unit,X+(c+1)*unit,Y+(r+1)*unit,light);
  if(checkers&&Math.abs(p)==2){g.fill(X+2*unit,Y+2*unit,X+6*unit,Y+4*unit,0xFFC99034);for(int c:new int[]{2,4,5})g.fill(X+c*unit,Y+unit,X+(c+1)*unit,Y+2*unit,0xFFC99034);}
 }
}

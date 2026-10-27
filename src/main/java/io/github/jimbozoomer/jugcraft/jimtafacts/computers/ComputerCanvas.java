package io.github.jimbozoomer.jugcraft.jimtafacts.computers;

import java.util.Arrays;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Fixed-size, saved terminal cells. Every operation is bounded and has no host/runtime dependencies. */
public final class ComputerCanvas {
    public static final int WIDTH=48, HEIGHT=24, SIZE=WIDTH*HEIGHT;
    private final char[] text=new char[SIZE],foreground=new char[SIZE],background=new char[SIZE];
    private int x,y,ink=0,paper=15;
    private boolean active;
    public ComputerCanvas(){Arrays.fill(text,' ');Arrays.fill(foreground,'0');Arrays.fill(background,'f');}
    public boolean active(){return active;}
    public int cursorX(){return x+1;}
    public int cursorY(){return y+1;}
    public int foregroundColor(){return ink;}
    public int backgroundColor(){return paper;}
    public String text(){return new String(text);}
    public String foreground(){return new String(foreground);}
    public String background(){return new String(background);}
    public void deactivate(){active=false;}
    /** Lua terminals may put their cursor outside the screen; drawing clips instead of wrapping. */
    public void terminalCursor(int x,int y){this.x=Math.clamp(x,-32767,32767)-1;this.y=Math.clamp(y,-32767,32767)-1;active=true;}
    public void cursor(int x,int y){if(x<1||x>WIDTH||y<1||y>HEIGHT)throw new IllegalArgumentException("Cursor: 1..48 columns, 1..24 rows.");this.x=x-1;this.y=y-1;active=true;}
    public void ink(int foreground,int background){ink=Math.clamp(foreground,0,15);paper=Math.clamp(background,0,15);}
    public void clear(){Arrays.fill(text,' ');Arrays.fill(foreground,Character.forDigit(ink,16));Arrays.fill(background,Character.forDigit(paper,16));x=y=0;active=true;}
    public void clearLine(){if(y<0||y>=HEIGHT)return;int start=y*WIDTH;Arrays.fill(text,start,start+WIDTH,' ');Arrays.fill(foreground,start,start+WIDTH,Character.forDigit(ink,16));Arrays.fill(background,start,start+WIDTH,Character.forDigit(paper,16));active=true;}
    public void terminalWrite(String value){
        if(value.length()>512)throw new IllegalArgumentException("Write at most 512 characters.");
        for(char c:value.toCharArray()){
            if(x>=0&&x<WIDTH&&y>=0&&y<HEIGHT){int i=y*WIDTH+x;text[i]=c>=32&&c<=126?c:'?';foreground[i]=Character.forDigit(ink,16);background[i]=Character.forDigit(paper,16);}
            x=Math.min(32767,x+1);
        }active=true;
    }
    public void scroll(int rows){
        if(rows<-HEIGHT||rows>HEIGHT)throw new IllegalArgumentException("Scroll: -24..24 rows.");
        if(rows==0)return;
        int delta=Math.abs(rows)*WIDTH,remain=SIZE-delta,from=rows>0?delta:0,to=rows>0?0:delta;
        for(char[] cells:new char[][]{text,foreground,background})System.arraycopy(cells,from,cells,to,remain);
        int first=rows>0?remain:0,last=rows>0?SIZE:delta;
        Arrays.fill(text,first,last,' ');Arrays.fill(foreground,first,last,Character.forDigit(ink,16));Arrays.fill(background,first,last,Character.forDigit(paper,16));active=true;
    }
    /** ASCII cell grid avoids variable-width/surrogate glyphs overflowing a neighboring cell. */
    public void write(String value){
        if(value.length()>512)throw new IllegalArgumentException("Write at most 512 characters.");
        for(char c:value.toCharArray()){
            if(c=='\n'){x=0;if(++y>=HEIGHT){scroll(1);y=HEIGHT-1;}continue;}
            if(x>=WIDTH){x=0;if(++y>=HEIGHT){scroll(1);y=HEIGHT-1;}}
            int i=y*WIDTH+x++;text[i]=c>=32&&c<=126?c:'?';foreground[i]=Character.forDigit(ink,16);background[i]=Character.forDigit(paper,16);
        }active=true;
    }
    public void blit(String value,String front,String back){
        if(value.length()>WIDTH||front.length()!=value.length()||back.length()!=value.length())throw new IllegalArgumentException("Blit needs text and two equal-length hexadecimal color strings (max 48).");
        for(int i=0;i<value.length();i++)if(Character.digit(front.charAt(i),16)<0||Character.digit(back.charAt(i),16)<0)throw new IllegalArgumentException("Blit colors must be hexadecimal 0..f.");
        for(int i=0;i<value.length();i++){if(x>=0&&x<WIDTH&&y>=0&&y<HEIGHT){int cell=y*WIDTH+x;char c=value.charAt(i);text[cell]=c>=32&&c<=126?c:'?';foreground[cell]=Character.toLowerCase(front.charAt(i));background[cell]=Character.toLowerCase(back.charAt(i));}x=Math.min(32767,x+1);}active=true;
    }
    public void save(ValueOutput out){out.putBoolean("canvas_active",active);out.putString("canvas_text",text());out.putString("canvas_fore",foreground());out.putString("canvas_back",background());out.putInt("canvas_x",x);out.putInt("canvas_y",y);out.putInt("canvas_ink",ink);out.putInt("canvas_paper",paper);}
    public void load(ValueInput in){
        copy(in.getStringOr("canvas_text",""),text,' ',false);copy(in.getStringOr("canvas_fore",""),foreground,'0',true);copy(in.getStringOr("canvas_back",""),background,'f',true);
        active=in.getBooleanOr("canvas_active",false);x=Math.clamp(in.getIntOr("canvas_x",0),0,WIDTH);y=Math.clamp(in.getIntOr("canvas_y",0),0,HEIGHT-1);ink=Math.clamp(in.getIntOr("canvas_ink",0),0,15);paper=Math.clamp(in.getIntOr("canvas_paper",15),0,15);
    }
    private static void copy(String source,char[] target,char fallback,boolean colors){
        Arrays.fill(target,fallback);for(int i=0;i<Math.min(source.length(),target.length);i++){char c=source.charAt(i);target[i]=colors?Character.digit(c,16)>=0?Character.toLowerCase(c):fallback:c>=32&&c<=126?c:'?';}
    }
}

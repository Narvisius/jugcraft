package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/** Engraved equipment panels around unchanged owner atlas details. All geometry snaps to GUI pixels. */
final class OwnerAlchemyArt {
    static final int SILVER=0xFFAABAC6, INK=0xFF10191F, TEAL=0xFF74CEC2, VIOLET=0xFFB8A0D8, GOLD=0xFFD5B67B;
    private OwnerAlchemyArt(){}
    static void crop(GuiGraphicsExtractor g,String atlas,int x,int y,int u,int v,int w,int h){
        g.blit(RenderPipelines.GUI_TEXTURED,Jugcraft.id("textures/gui/alchemy/"+atlas+".png"),x,y,u,v,w,h,w,h,256,256);
    }
    static void slot(GuiGraphicsExtractor g,String atlas,int x,int y,int u,int v){
        crop(g,atlas,x-1,y-1,u,v,18,18);
        // Preserve the source atlas's recessed shape under a quiet smoked-metal tint.
        g.fill(x-1,y-1,x+17,y+17,0xA0101C24);
        g.fill(x,y,x+16,y+1,0xFF0F171E);g.fill(x,y,x+1,y+16,0xFF111B23);
        g.fill(x,y+16,x+17,y+17,0xFF73828C);g.fill(x+16,y,x+17,y+16,0xFF485C65);
    }
    static void frame(GuiGraphicsExtractor g,int x,int y,int w,int h){
        TransmutationScreen.slab(g,x+3,y+5,w,h,0x80060A10);
        TransmutationScreen.slab(g,x,y,w,h,INK);
        TransmutationScreen.slab(g,x+1,y+1,w-2,h-2,0xFF899AA5);
        TransmutationScreen.slab(g,x+2,y+3,w-4,h-5,0xFF3D4D59);
        TransmutationScreen.slab(g,x+5,y+5,w-10,h-10,0xFF17232D);
        g.fill(x+9,y+6,x+w-9,y+7,0xFF536775);
        g.fill(x+9,y+h-8,x+w-9,y+h-7,0xFF0C131B);
        // Sparse engraved diagonal marks in the side rails, away from text and hit targets.
        for(int yy=32;yy<h-24;yy+=18){
            g.fill(x+3,y+yy,x+5,y+yy+5,0xFF91A3AA);
            g.fill(x+w-5,y+yy,x+w-3,y+yy+5,0xFF526873);
        }
        for(int xx:new int[]{8,w-9})for(int yy:new int[]{8,h-9}){
            g.fill(x+xx-2,y+yy-2,x+xx+3,y+yy+3,INK);
            BookArt.star(g,x+xx,y+yy,2,GOLD);
            g.fill(x+xx,y+yy,x+xx+1,y+yy+1,0xFFF3DEB4);
        }
        // Split metal inlay and a central cut jewel distinguish these panels from a book.
        int cx=x+w/2;
        g.fill(cx-31,y+2,cx-6,y+3,0xFFCDDBDC);g.fill(cx+7,y+2,cx+32,y+3,0xFFCDDBDC);
        BookArt.star(g,cx,y+4,4,0xFF101820);BookArt.star(g,cx,y+4,2,TEAL);
    }
    static void panel(GuiGraphicsExtractor g,int x,int y,int w,int h,int accent){
        g.fill(x,y,x+w,y+h,0xFF0B141C);
        g.fill(x+1,y+1,x+w-1,y+h-1,0xFF1F303C);
        g.fill(x+2,y+2,x+w-2,y+h-2,0xFF18262F);
        g.fill(x+3,y+1,x+w-3,y+2,0xFF4C626D);
        g.fill(x+5,y+1,x+Math.min(28,w-5),y+2,accent);
        g.fill(x+3,y+h-2,x+w-3,y+h-1,0xFF0F1A22);
    }
    static void socket(GuiGraphicsExtractor g,int x,int y,int accent){
        g.outline(x-3,y-3,22,22,0xFF0B121B);g.outline(x-2,y-2,20,20,accent);
        slot(g,"alchchest",x,y,11,3);
    }
    static void sigil(GuiGraphicsExtractor g,int cx,int cy,int radius,int accent,boolean working,float delta){
        // Fixed octagonal engraving; only the tiny energy marker moves, and obeys reduced motion.
        int cut=radius*2/5;
        int[] dx={-cut,cut,radius,radius,cut,-cut,-radius,-radius};
        int[] dy={-radius,-radius,-cut,cut,radius,radius,cut,-cut};
        for(int i=0;i<8;i++)BookArt.line(g,cx+dx[i],cy+dy[i],cx+dx[(i+1)%8],cy+dy[(i+1)%8],0xFF425963);
        BookArt.star(g,cx-radius,cy,2,accent);BookArt.star(g,cx+radius,cy,2,accent);
        if(working){
            var mc=net.minecraft.client.Minecraft.getInstance();
            double phase=ConcordanceClientOptions.reducedMotion()||mc.level==null?0:(mc.level.getGameTime()+delta)*.035;
            BookArt.star(g,cx+(int)(Math.cos(phase)*radius),cy+(int)(Math.sin(phase)*radius),1,accent);
        }
    }
    static void meter(GuiGraphicsExtractor g,int x,int y,int width,long value,long capacity,int accent){
        g.fill(x,y,x+width,y+7,0xFF09121A);g.fill(x+1,y+1,x+width-1,y+6,0xFF33444E);
        int amount=capacity<=0?0:(int)((width-2)*Math.clamp((double)value/capacity,0,1));
        g.fill(x+1,y+2,x+1+amount,y+5,accent);
        for(int i=16;i<width-2;i+=16)g.fill(x+i,y+1,x+i+1,y+6,0x800D1820);
    }
    static void flow(GuiGraphicsExtractor g,int from,int to,int y,int accent,boolean working,float delta){
        BookArt.line(g,from,y,to,y,0xFF52616F);
        BookArt.line(g,to-3,y-3,to,y,accent);BookArt.line(g,to-3,y+3,to,y,accent);
        if(working&&to-from>8){
            var mc=net.minecraft.client.Minecraft.getInstance();
            int step=ConcordanceClientOptions.reducedMotion()||mc.level==null?(to-from)/2:(int)((mc.level.getGameTime()+delta)*.7)%(to-from-3);
            g.fill(from+step,y-1,from+step+3,y+2,accent);
        }
    }
}

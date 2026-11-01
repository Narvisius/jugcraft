package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.tooltip.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.item.*;

/** Supplied frame atlas, native item preview and centered titles, without replacing tooltip text/data. */
public final class OwnerTooltipFrames {
    private static final net.minecraft.resources.Identifier BORDER=Jugcraft.id("textures/gui/newest/tooltip_borders.png");
    private OwnerTooltipFrames(){}
    public static List<ClientTooltipComponent> components(List<ClientTooltipComponent> original,ItemStack stack,Font font){
        if(stack.isEmpty()||original.isEmpty())return original;
        var result=new ArrayList<>(original);int width=Math.max(48,result.stream().mapToInt(c->c.getWidth(font)).max().orElse(0));
        var title=result.getFirst();final int span=width;
        result.set(0,new ClientTooltipComponent(){
            public int getHeight(Font f){return title.getHeight(f);}
            public int getWidth(Font f){return span;}
            public void extractText(GuiGraphicsExtractor g,Font f,int x,int y){title.extractText(g,f,x+(span-title.getWidth(f))/2,y);}
            public void extractImage(Font f,int x,int y,int w,int h,GuiGraphicsExtractor g){title.extractImage(f,x+(span-title.getWidth(f))/2,y,w,h,g);}
        });
        if(stack.isDamageableItem())result.add(new ClientTooltipComponent(){
            public int getHeight(Font f){return 38;}
            public int getWidth(Font f){return 38;}
            public void extractImage(Font f,int x,int y,int w,int h,GuiGraphicsExtractor g){
                var mc=net.minecraft.client.Minecraft.getInstance();
                if(mc.level==null){g.item(stack,x+(w-16)/2,y+8);return;}
                var item=new net.minecraft.world.entity.item.ItemEntity(mc.level,0,0,0,stack.copyWithCount(1));
                var state=mc.getEntityRenderDispatcher().extractEntity(item,0);
                if(state instanceof net.minecraft.client.renderer.entity.state.ItemEntityRenderState dropped)dropped.bobOffset=0;
                float angle=ConcordanceClientOptions.reducedMotion()?0:(System.nanoTime()%12_000_000_000L)/12_000_000_000F*(float)(Math.PI*2);
                g.entity(state,38,new org.joml.Vector3f(0,-.1F,0),new org.joml.Quaternionf().rotationXYZ(.2F,angle,(float)Math.PI),null,x+(w-38)/2,y,x+(w+38)/2,y+38);
            }
        });return result;
    }
    public static void decorate(GuiGraphicsExtractor g,Font font,List<ClientTooltipComponent> parts,ItemStack stack,int mouseX,int mouseY,ClientTooltipPositioner positioner,boolean extra){
        if(stack.isEmpty()||parts.isEmpty())return;
        int width=parts.stream().mapToInt(c->c.getWidth(font)).max().orElse(0),height=parts.stream().mapToInt(c->c.getHeight(font)).sum()+(!extra||parts.size()==1?-2:0);
        var pos=positioner.positionTooltip(g.guiWidth(),g.guiHeight(),mouseX,mouseY,width,height);int x=pos.x(),y=pos.y();
        var style=OwnerTooltipStyles.of(stack);
        int color=style!=null?style.start():switch(stack.getRarity()){case COMMON->0xFF81728F;case UNCOMMON->0xFFDDD077;case RARE->0xFF6EC5EC;case EPIC->0xFFC482EC;};
        g.nextStratum();g.fill(x-3,y-3,x+width+3,y-2,color);g.fill(x-3,y+height+2,x+width+3,y+height+3,style==null?color:style.end());
        g.fill(x-3,y-2,x-2,y+height+2,color);g.fill(x+width+2,y-2,x+width+3,y+height+2,color);
        if(parts.size()>1)g.fill(x,y+parts.getFirst().getHeight(font)+1,x+width,y+parts.getFirst().getHeight(font)+2,(color&0xFFFFFF)|0x55000000);
        if(style==null&&stack.getRarity()!=Rarity.EPIC)return;
        blit(g,style,x-6,y-6,0,0,8,8);blit(g,style,x+width-2,y-6,56,0,8,8);
        blit(g,style,x-6,y+height-2,0,8,8,8);blit(g,style,x+width-2,y+height-2,56,8,8,8);
        if(width>=48){blit(g,style,x+width/2-24,y-9,8,0,48,8);blit(g,style,x+width/2-24,y+height+1,8,8,48,8);}
        if(!ConcordanceClientOptions.reducedMotion()){
            int shine=(int)((System.nanoTime()/15_000_000L)%Math.max(1,width+24))-12;
            if(shine>=0&&shine<width)g.fill(x+shine,y-3,x+Math.min(width,shine+5),y-2,0xFFDCE6EF);
        }
    }
    private static void blit(GuiGraphicsExtractor g,OwnerTooltipStyles.Style style,int x,int y,int u,int v,int w,int h){
        int tw=style==null?128:style.width(),th=style==null?128:style.height(),index=style==null?0:style.index();
        u+=index/(th/16)*64;v+=(index%(th/16))*16;
        g.blit(RenderPipelines.GUI_TEXTURED,style==null?BORDER:style.image(),x,y,u,v,w,h,w,h,tw,th);
    }
}

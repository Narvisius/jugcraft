package local.peepo.client;

import local.peepo.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class TransportCrateScreen extends AbstractContainerScreen<TransportCrateMenu> {
    private final Button[] release=new Button[8];
    public TransportCrateScreen(TransportCrateMenu menu,Inventory inventory,Component title){super(menu,inventory,title,304,226);}
    @Override protected void init(){
        super.init();
        for(int i=0;i<8;i++){final int slot=i;release[i]=addRenderableWidget(Button.builder(Component.literal("Release"),b->{
            if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,slot);
        }).bounds(leftPos+10+(i%4)*72,topPos+100+(i/4)*92,66,18).build());}
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        var entries=MobTransportCrate.occupants(menu.crate());int capacity=MobTransportCrate.capacity(menu.crate());
        for(int i=0;i<8;i++){release[i].visible=i<capacity;release[i].active=i<entries.size() && !entries.get(i).getStringOr("Claim","").isEmpty();}
        extractBackground(g,mouseX,mouseY,delta);super.extractRenderState(g,mouseX,mouseY,delta);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF37302A);g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+imageHeight-2,0xFFC6C6C6);
        var entries=MobTransportCrate.occupants(menu.crate());
        for(int i=0;i<MobTransportCrate.capacity(menu.crate());i++){
            int x=leftPos+9+i%4*72,y=topPos+28+i/4*92;g.fill(x,y,x+68,y+69,0xFF8B8B8B);g.fill(x+1,y+1,x+67,y+68,0xFF55504A);
            if(i>=entries.size() || entries.get(i).getStringOr("Claim","").isEmpty()){g.text(font,Component.literal("Empty"),x+17,y+31,0xFFBBBBBB,false);continue;}
            var entry=entries.get(i);var mob=TransportCratePreview.mob(entry);
            if(mob!=null){
                int size=(int)Math.min(60,Math.min(39/Math.max(.1F,mob.getBbHeight()),45/Math.max(.1F,mob.getBbWidth())));
                InventoryScreen.extractEntityInInventoryFollowsMouse(g,x+2,y+2,x+66,y+53,size,.1F,x+34,y+26,mob);
            }
            var name=Component.literal(font.plainSubstrByWidth(entry.getStringOr("Name","Mob"),62));g.text(font,name,x+3,y+57,0xFFFFFFFF,false);
            if(mouseX>=x && mouseX<x+68 && mouseY>=y && mouseY<y+69)g.setTooltipForNextFrame(font,Component.literal(entry.getStringOr("Name","Mob")),mouseX,mouseY);
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){
        g.text(font,title,8,8,0xFF303030,false);g.text(font,Component.literal(MobTransportCrate.count(menu.crate())+" / "+MobTransportCrate.capacity(menu.crate())),258,8,0xFF303030,false);
        g.text(font,Component.literal("Release places a mob safely beside you."),8,213,0xFF404040,false);
    }
}

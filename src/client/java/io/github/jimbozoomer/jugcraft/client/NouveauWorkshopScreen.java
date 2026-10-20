package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.nouveau.NouveauWorkshopMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Inlaid workshop controls, recipe ingredient/output previews and explicit recipe locking. */
public final class NouveauWorkshopScreen extends AbstractContainerScreen<NouveauWorkshopMenu> {
    private net.minecraft.client.gui.components.EditBox search;private String pending="";private int quiet;
    @Override protected void containerTick(){super.containerTick();if(quiet>0&&--quiet==0)net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new io.github.jimbozoomer.jugcraft.nouveau.NouveauWorkshops.Search(menu.containerId,pending));}
    private TransmutationScreen.ActionButton previous,next,choose,lock,pause,auto,minus,plus,invert,sample,clear;
    public NouveauWorkshopScreen(NouveauWorkshopMenu m,Inventory inv,Component title){super(m,inv,title,352,240);}
    @Override protected void init(){super.init();
        search=addRenderableWidget(new net.minecraft.client.gui.components.EditBox(font,leftPos+210,topPos+20,120,18,Component.literal("Find a recipe")));search.setMaxLength(48);search.setHint(Component.literal("Find recipe…"));search.setValue(pending);search.setResponder(text->{pending=text;quiet=4;});
        previous=button(0,210,43,55,"Previous");next=button(1,275,43,55,"Next");
        choose=button(2,210,143,120,"Use this recipe");lock=button(3,210,166,120,"Lock recipe");
        minus=button(6,210,43,55,"− 1");plus=button(7,275,43,55,"+ 1");invert=button(8,210,143,120,"Invert signal");sample=button(9,210,166,120,"Match input item");clear=button(10,210,189,120,"Clear filter");
        pause=button(4,210,212,120,"Pause");auto=button(5,210,189,120,"Automatic recipe");
    }
    private TransmutationScreen.ActionButton button(int action,int x,int y,int width,String label){return addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+x,topPos+y,width,Component.literal(label),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}));}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        boolean config=menu.controls()>0;search.visible=!config&&!menu.storage();previous.visible=next.visible=choose.visible=lock.visible=auto.visible=!config&&!menu.storage();minus.visible=plus.visible=config&&menu.controls()!=3;invert.visible=config&&menu.controls()!=2;sample.visible=clear.visible=config&&menu.controls()!=2;invert.setMessage(Component.literal(menu.controls()==3?(menu.inverted()?"Sense: impact":"Sense: cast"):(menu.inverted()?"Output: inverted":"Output: normal")));
        previous.active=menu.cursor()>0;next.active=menu.cursor()+1<menu.recipes();choose.active=menu.recipes()>0&&!menu.locked();auto.active=!menu.locked();
        lock.setMessage(Component.literal(menu.locked()?"Unlock recipe":"Lock recipe"));pause.setMessage(Component.literal(menu.paused()?"Resume":"Pause"));
        extractBackground(g,mx,my,delta);super.extractRenderState(g,mx,my,delta);extractTooltip(g,mx,my);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        int x=leftPos,y=topPos;g.fill(0,0,width,height,0xB0080814);OwnerAlchemyArt.frame(g,x,y,imageWidth,imageHeight);
        OwnerAlchemyArt.panel(g,x+12,y+34,174,103,OwnerAlchemyArt.VIOLET);OwnerAlchemyArt.panel(g,x+12,y+148,174,86,OwnerAlchemyArt.GOLD);
        OwnerAlchemyArt.panel(g,x+204,y+78,130,61,OwnerAlchemyArt.VIOLET);
        for(int i=0;i<menu.slots.size();i++){var slot=menu.slots.get(i);if(slot.isActive()&&slot.y>=0)OwnerAlchemyArt.slot(g,"alchchest",x+slot.x,y+slot.y,11,3);}
        if(!menu.storage()){
            g.fill(x+18,y+89,x+180,y+92,0xFF272338);g.fill(x+18,y+89,x+18+162*menu.progress()/100,y+92,OwnerAlchemyArt.VIOLET);
            var lines=font.split(Component.literal("Inputs above · Results below\nFind and select a recipe.\nDominion Wand links Ley Charge."),156);
            for(int i=0;i<lines.size();i++)g.text(font,lines.get(i),x+20,y+101+i*9,OwnerAlchemyArt.SILVER,false);
        }
        OwnerAlchemyArt.sigil(g,x+325,y+22,10,OwnerAlchemyArt.VIOLET,menu.progress()>0,delta);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,BookArt.fit(font,title.getString(),184),18,10,0xFFE9DFF5,false);
        g.text(font,"Ley Charge: "+menu.ley()+" · "+(menu.automatic()?"Automatic":"Selected recipe"),18,24,OwnerAlchemyArt.SILVER,false);
        g.text(font,BookArt.fit(font,menu.controls()==2?"Every "+menu.threshold()+" seconds":menu.controls()==1?"Threshold: "+menu.threshold():menu.details().getString(),124),210,66,OwnerAlchemyArt.GOLD,false);
        g.text(font,"Your inventory",18,140,OwnerAlchemyArt.SILVER,false);
        if(!menu.storage()&&menu.controls()==0)g.text(font,menu.recipes()==0?"No recipes":(menu.cursor()+1)+" / "+menu.recipes(),292,11,OwnerAlchemyArt.SILVER,false);
    }
}

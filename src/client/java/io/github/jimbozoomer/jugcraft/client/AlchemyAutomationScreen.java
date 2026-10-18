package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyAutomationMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AlchemyAutomationScreen extends AbstractContainerScreen<AlchemyAutomationMenu> {
    private TransmutationScreen.ActionButton active,target,budget,reserve;
    public AlchemyAutomationScreen(AlchemyAutomationMenu menu,Inventory inv,Component title){super(menu,inv,title,330,238);}
    private static Component ui(String s,Object... args){return Component.translatable("screen.jugcraft.automation."+s,args);}
    private void press(int id){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init(){super.init();
        active=addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+91,topPos+43,108,ui("start"),b->press(0)));
        target=addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+91,topPos+68,108,ui("target_2"),b->press(1)));
        addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+91,topPos+93,108,ui("clear"),b->press(2)));
        budget=addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+211,topPos+43,102,ui("budget",16),b->press(3)));
        reserve=addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+211,topPos+68,102,ui("reserve",0),b->press(4)));
        reserve.setTooltip(Tooltip.create(ui("reserve_help")));
        active.setTooltip(Tooltip.create(ui("help")));target.setTooltip(Tooltip.create(ui("output_help")));refresh();
    }
    private void refresh(){budget.setMessage(ui("budget",menu.budget()));reserve.setMessage(ui("reserve",menu.reserve()));active.setMessage(ui(menu.active()?"stop":"start"));active.selected=menu.active();target.visible=!menu.voiding();target.setMessage(ui("target_"+Math.max(0,menu.target())));}
    @Override protected void containerTick(){super.containerTick();refresh();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){extractBackground(g,mx,my,delta);super.extractRenderState(g,mx,my,delta);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        int x=leftPos,y=topPos;
        g.fill(0,0,width,height,0xB0081018);
        OwnerAlchemyArt.frame(g,x,y,imageWidth,imageHeight);

        OwnerAlchemyArt.panel(g,x+17,y+39,66,70,OwnerAlchemyArt.VIOLET);
        OwnerAlchemyArt.panel(g,x+205,y+35,113,76,OwnerAlchemyArt.GOLD);
        OwnerAlchemyArt.panel(g,x+12,y+115,306,31,OwnerAlchemyArt.TEAL);
        OwnerAlchemyArt.panel(g,x+82,y+148,168,82,OwnerAlchemyArt.SILVER);
        for(var slot:menu.slots)OwnerAlchemyArt.slot(g,"eternal_density",x+slot.x,y+slot.y,61,25);
        OwnerAlchemyArt.sigil(g,x+43,y+188,17,OwnerAlchemyArt.VIOLET,menu.active(),delta);
        BookArt.icon(g,menu.voiding()?"jugcraft:void_ring":"jugcraft:gem_of_eternal_density",x+35,y+180);
        if(!menu.voiding())BookArt.icon(g,AlchemyAutomationMenu.TARGETS.get(Math.clamp(menu.target(),0,2)),x+281,y+179);

    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,BookArt.fit(font,title.getString(),300),13,12,0xFFE5DDEB,false);
        g.text(font,ui("filters"),16,29,0xFFE4C786,false);
        g.text(font,BookArt.fit(font,ui("preview",menu.outputPrice()).getString(),103),211,96,0xFFBBCBD7,false);
        g.text(font,BookArt.fit(font,ui("status_"+menu.status()).getString(),295),16,120,0xFFE4C786,false);
        g.text(font,BookArt.fit(font,ui("menu_pause").getString(),295),16,136,0xFFBBCBD7,false);
    }
}

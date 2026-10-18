package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Existing silver/stone tablet controls, clear slot purposes and actual server status. */
public final class AlchemyMachinesScreen extends AbstractContainerScreen<AlchemyMachinesMenu> {
    private TransmutationScreen.ActionButton range;
    private final TransmutationScreen.ActionButton[] buttons=new TransmutationScreen.ActionButton[3];
    public AlchemyMachinesScreen(AlchemyMachinesMenu menu,Inventory inv,Component title){super(menu,inv,title,348,240);}
    private static Component ui(String key,Object... args){return Component.translatable("screen.jugcraft.alchemy_machine."+key,args);}
    private static String n(long v){return String.format("%,d",v);}
    @Override protected void init(){
        super.init();for(int i=0;i<3;i++){final int action=i;buttons[i]=addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+21+i*103,topPos+124,100,ui(i==0?"pause":i==1?"deposit":"withdraw"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,menu.kind()==AlchemyMachines.Kind.NOVA?3:action);}));}range=addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+206,topPos+66,112,ui("range",6),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,4);}));refresh();
    }
    private void refresh(){
        range.visible=menu.kind()==AlchemyMachines.Kind.PEDESTAL;range.setMessage(ui("range",menu.range()));range.setTooltip(Tooltip.create(ui("range_help")));
        boolean nova=menu.kind()==AlchemyMachines.Kind.NOVA;
        buttons[0].setMessage(ui(nova?"detonate":menu.paused()?"resume":"pause"));buttons[0].active=menu.canOperate();
        buttons[0].setTooltip(Tooltip.create(ui(nova?"nova_help":"pause_help")));
        buttons[1].active=!nova&&menu.canOperate()&&menu.wallet()>0&&menu.essence()<menu.capacity();buttons[2].active=!nova&&menu.canOperate()&&menu.essence()>0&&menu.wallet()<io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay.MAX_BALANCE;
        buttons[1].visible=!nova;buttons[2].visible=!nova;
    }
    @Override protected void containerTick(){super.containerTick();refresh();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){extractBackground(g,mx,my,delta);super.extractRenderState(g,mx,my,delta);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        int x=leftPos,y=topPos;
        g.fill(0,0,width,height,0xB0081018);
        OwnerAlchemyArt.frame(g,x,y,imageWidth,imageHeight);

        OwnerAlchemyArt.panel(g,x+16,y+34,316,88,OwnerAlchemyArt.TEAL);
        OwnerAlchemyArt.meter(g,x+23,y+38,302,menu.essence(),menu.capacity(),OwnerAlchemyArt.TEAL);
        OwnerAlchemyArt.panel(g,x+89,y+156,170,81,OwnerAlchemyArt.SILVER);

        if(menu.kind()==AlchemyMachines.Kind.FURNACE){
            OwnerAlchemyArt.flow(g,x+185,x+225,y+78,OwnerAlchemyArt.GOLD,menu.status()==7,delta);
            OwnerAlchemyArt.meter(g,x+184,y+87,42,menu.progress(),40,OwnerAlchemyArt.GOLD);
        }else if(menu.kind()==AlchemyMachines.Kind.RELAY){
            OwnerAlchemyArt.sigil(g,x+262,y+78,17,OwnerAlchemyArt.TEAL,menu.status()==7,delta);
            BookArt.icon(g,"jugcraft:relay_mk"+menu.tier(),x+254,y+70);
        }
        for(var slot:menu.slots){
            if(!slot.isActive())continue;
            OwnerAlchemyArt.slot(g,"alchchest",x+slot.x,y+slot.y,11,3);
            if(slot.index==0||slot.index==1)OwnerAlchemyArt.socket(g,x+slot.x,y+slot.y,slot.index==0?OwnerAlchemyArt.VIOLET:OwnerAlchemyArt.GOLD);
        }
        OwnerAlchemyArt.sigil(g,x+44,y+189,20,OwnerAlchemyArt.VIOLET,menu.status()==7,delta);
        String icon=switch(menu.kind()){
            case FURNACE -> menu.tier()==1?"dark_matter_furnace":"red_matter_furnace";
            case RELAY -> "relay_mk"+menu.tier();
            case PEDESTAL -> "dark_matter_pedestal";
            case TORCH -> "interdiction_torch";
            case NOVA -> menu.tier()==1?"nova_catalyst":"nova_cataclysm";
        };
        BookArt.icon(g,"jugcraft:"+icon,x+36,y+181);

    }
    private void line(GuiGraphicsExtractor g,Component text,int x,int y,int width,int color){g.text(font,BookArt.fit(font,text.getString(),width),x,y,color,false);}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        line(g,title,19,13,311,0xFFE4DCEC);line(g,ui("buffer",n(menu.essence()),n(menu.capacity())),19,25,312,0xFFB5DFE2);
        var kind=menu.kind();if(kind!=AlchemyMachines.Kind.NOVA)line(g,ui(kind==AlchemyMachines.Kind.RELAY?"star_charge":"star_supply"),24,54,91,0xFFBDAED3);
        if(kind==AlchemyMachines.Kind.RELAY||kind==AlchemyMachines.Kind.FURNACE)line(g,ui(kind==AlchemyMachines.Kind.RELAY?"dissolve":"smelt"),115,54,102,0xFFE1C39F);
        if(kind==AlchemyMachines.Kind.FURNACE)line(g,ui("output"),239,54,80,0xFFB6E4D0);
        if(kind==AlchemyMachines.Kind.PEDESTAL)line(g,ui("artifact"),115,54,130,0xFFE4C786);
        if(kind==AlchemyMachines.Kind.NOVA)line(g,ui("nova_cost",n(menu.tier()==1?4096:16384)),24,64,295,0xFFE4C786);
        if(kind==AlchemyMachines.Kind.PEDESTAL){
            String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(menu.getSlot(1).getItem().getItem()).getPath();
            line(g,ui("effect_"+id),24,95,296,0xFFB5C3D3);
            line(g,ui("pulse",menu.lastTargets(),menu.lastSpent()),24,105,296,0xFFB5C3D3);
        }else line(g,ui("hint_"+kind.name().toLowerCase(),menu.tier()==1?1:4),24,95,296,0xFFB5C3D3);
        line(g,ui("status_"+menu.status()),19,112,311,menu.status()==7?0xFF9CE2BC:0xFFE2C99D);line(g,ui("wallet",n(menu.wallet())),93,146,237,0xFFCEC5DF);
    }
}
